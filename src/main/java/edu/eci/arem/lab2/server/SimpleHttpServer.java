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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Deliberately sequential HTTP server.
 *
 * The ServerSocket (listening socket) is opened once and stays open for the
 * whole process lifetime. For every accepted connection, the request is
 * fully read, handled, and responded to -- and the client Socket is closed
 * -- BEFORE accept() is called again. There is no thread pool, no executor,
 * no concurrent handling of any kind: this is intentional (see the lab's
 * "why scalability" motivation). A malformed request from one client must
 * never take down the listening loop.
 */
public final class SimpleHttpServer {

    private final int port;
    private final StaticFileHandler staticFileHandler;
    private final Router router;
    private boolean running = true;

    public SimpleHttpServer(int port, Path webRoot, Router router) {
    this.port = port;
    this.staticFileHandler = new StaticFileHandler(webRoot);
    this.router = router;
    }

    public void stop() {
        running = false;
    }

    /** Binds on all interfaces (0.0.0.0), not just loopback, so it is reachable remotely (e.g. from EC2). */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress(port));
            System.out.println("Server listening on port " + port + " (sequential, single connection at a time)");

            while (running) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleConnection(clientSocket);
                } catch (IOException e) {
                    // A single bad connection must not crash the listening loop.
                    System.err.println("Error handling connection: " + e.getMessage());
                }
            }
            System.out.println("Server stopped gracefully.");
        }
    }

    private void handleConnection(Socket clientSocket) throws IOException {
        clientSocket.setSoTimeout(10_000);

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
}
