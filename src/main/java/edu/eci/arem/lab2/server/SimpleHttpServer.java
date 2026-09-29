package edu.eci.arem.lab2.server;

import edu.eci.arem.lab2.framework.Response;
import edu.eci.arem.lab2.framework.Router;
import edu.eci.arem.lab2.framework.Service;
import edu.eci.arem.lab2.handler.StaticFileHandler;
import edu.eci.arem.lab2.http.HttpRequest;
import edu.eci.arem.lab2.http.HttpRequestParser;
import edu.eci.arem.lab2.http.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Small HTTP server with one acceptor thread and a fixed worker pool.
 */
public final class SimpleHttpServer {

    private static final int SOCKET_READ_TIMEOUT_MILLIS = 10_000;
    private final int port;
    private final StaticFileHandler staticFileHandler;
    private final Router router;
    private final int poolSize;
    private final int shutdownTimeoutSeconds;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shutdownStarted = new AtomicBoolean(false);
    private final AtomicBoolean shutdownLogged = new AtomicBoolean(false);
    private volatile ServerSocket serverSocket;
    private volatile ExecutorService pool;
    private volatile Thread shutdownHook;

    public SimpleHttpServer(int port, Path webRoot, Router router) {
        this(port, webRoot, router,
                Math.max(1, Runtime.getRuntime().availableProcessors() * 2), 10);
    }

    public SimpleHttpServer(int port, Path webRoot, Router router, int poolSize,
                            int shutdownTimeoutSeconds) {
        this.port = port;
        this.staticFileHandler = new StaticFileHandler(webRoot);
        this.router = router;
        this.poolSize = Math.max(1, poolSize);
        this.shutdownTimeoutSeconds = Math.max(1, shutdownTimeoutSeconds);
    }

    public void stop() {
        shutdown();
    }

    /** Binds on all interfaces so the server is reachable remotely, including from EC2. */
    public void start() throws IOException {
        ServerSocket listeningSocket = new ServerSocket();
        listeningSocket.bind(new InetSocketAddress(port));
        serverSocket = listeningSocket;
        pool = Executors.newFixedThreadPool(poolSize, workerThreadFactory());
        running.set(true);
        shutdownHook = new Thread(this::shutdown, "networking-lab2-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        System.out.println("Server listening on port " + port + " with " + poolSize + " worker threads");

        try {
            while (running.get()) {
                try {
                    Socket clientSocket = listeningSocket.accept();
                    try {
                        pool.execute(() -> handleClient(clientSocket));
                    } catch (RejectedExecutionException e) {
                        closeQuietly(clientSocket);
                        if (running.get()) {
                            System.err.println("Request rejected while server is running: " + e.getMessage());
                        }
                    }
                } catch (SocketException e) {
                    if (running.get()) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        } finally {
            shutdown();
            removeShutdownHook();
        }
    }

    private ThreadFactory workerThreadFactory() {
        return runnable -> {
            Thread worker = new Thread(runnable, "networking-lab2-worker");
            worker.setDaemon(false);
            return worker;
        };
    }

    private void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket) {
            handleConnection(socket);
        } catch (Exception e) {
            System.err.println("Error handling connection: " + e.getMessage());
        }
    }

    private void handleConnection(Socket clientSocket) throws IOException {
        clientSocket.setSoTimeout(SOCKET_READ_TIMEOUT_MILLIS);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = clientSocket.getOutputStream()) {

            HttpRequest request;
            try {
                request = HttpRequestParser.parse(reader);
            } catch (HttpRequestParser.MalformedRequestException e) {
                HttpResponse.sendText(out, 400, "Bad Request", "text/plain; charset=UTF-8",
                        "400 Bad Request: " + e.getMessage());
                return;
            }

            logRequest(request);

            Service service = router.resolve(request.getPath());
            if (service != null) {
                if (!"GET".equalsIgnoreCase(request.getMethod())) {
                    HttpResponse.sendText(out, 405, "Method Not Allowed", "text/plain; charset=UTF-8",
                            "405 Method Not Allowed: only GET is supported by this server.");
                    return;
                }
                Response frameworkResponse = new Response();
                String body = service.handle(request, frameworkResponse);
                HttpResponse.sendText(out, 200, "OK", frameworkResponse.getContentType(), body);
            } else {
                staticFileHandler.handle(request, out);
            }
        }
    }

    private void logRequest(HttpRequest request) {
        System.out.printf("%s %s%n", request.getMethod(), request.getPath());
    }

    private void shutdown() {
        if (shutdownStarted.compareAndSet(false, true)) {
            System.out.println("Server shutdown started.");
            running.set(false);
            closeQuietly(serverSocket);
            ExecutorService currentPool = pool;
            if (currentPool != null) {
                currentPool.shutdown();
            }
        }

        if (Thread.currentThread().getName().startsWith("networking-lab2-worker")) {
            return;
        }
        completeShutdown();
    }

    private void completeShutdown() {
        ExecutorService currentPool = pool;
        if (currentPool != null) {
            try {
                if (!currentPool.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                    System.err.println("Shutdown timeout reached; interrupting remaining workers.");
                    currentPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                currentPool.shutdownNow();
                System.err.println("Shutdown interrupted; remaining workers were interrupted.");
            }
        }
        if (shutdownLogged.compareAndSet(false, true)) {
            System.out.println("Server shutdown completed.");
        }
    }

    private void removeShutdownHook() {
        Thread hook = shutdownHook;
        if (hook != null && hook != Thread.currentThread()) {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException ignored) {
                // The JVM is already shutting down.
            }
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Closing during shutdown is best effort.
            }
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing a rejected connection is best effort.
        }
    }
}
