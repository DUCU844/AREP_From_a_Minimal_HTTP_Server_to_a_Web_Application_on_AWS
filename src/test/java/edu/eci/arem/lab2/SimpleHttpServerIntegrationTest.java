package edu.eci.arem.lab2;

import edu.eci.arem.lab2.framework.Router;
import edu.eci.arem.lab2.server.SimpleHttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleHttpServerIntegrationTest {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Test
    void handlesSlowRequestsConcurrently() throws Exception {
        Router router = new Router();
        router.addGet("/api/slow", (request, response) -> {
            response.setContentType("application/json; charset=UTF-8");
            sleep(300);
            return "{\"sleptMs\":300}";
        });
        int port = findFreePort();
        SimpleHttpServer server = startServer(router, port, 4);
        ExecutorService callers = Executors.newFixedThreadPool(4);

        try {
            long startedAt = System.nanoTime();
            List<java.util.concurrent.Future<HttpResponse<String>>> responses = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                responses.add(callers.submit(() -> get(port, "/api/slow?ms=300")));
            }
            for (var response : responses) {
                assertEquals(200, response.get(2, TimeUnit.SECONDS).statusCode());
            }
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            assertTrue(elapsedMillis < 900,
                    "four concurrent requests took " + elapsedMillis + " ms");
        } finally {
            callers.shutdownNow();
            server.stop();
        }
    }

    @Test
    void gracefulShutdownLetsActiveRequestFinishAndRejectsNewConnections() throws Exception {
        Router router = new Router();
        CountDownLatch requestStarted = new CountDownLatch(1);
        router.addGet("/api/slow", (request, response) -> {
            requestStarted.countDown();
            sleep(350);
            response.setContentType("application/json; charset=UTF-8");
            return "{\"status\":\"completed\"}";
        });
        int port = findFreePort();
        SimpleHttpServer server = startServer(router, port, 2);

        try {
            var responseFuture = client.sendAsync(request(port, "/api/slow?ms=350"),
                    HttpResponse.BodyHandlers.ofString());
            assertTrue(requestStarted.await(2, TimeUnit.SECONDS));

            server.stop();

            assertEquals(200, responseFuture.get(2, TimeUnit.SECONDS).statusCode());
            assertThrows(IOException.class, () -> get(port, "/api/slow?ms=1"));
        } finally {
            server.stop();
        }
    }

    private SimpleHttpServer startServer(Router router, int port, int poolSize) throws Exception {
        SimpleHttpServer server = new SimpleHttpServer(port, Path.of("webroot"), router, poolSize, 2);
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();
        waitUntilListening(port);
        return server;
    }

    private HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
        return client.send(request(port, path), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest request(int port, String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
    }

    private void waitUntilListening(int port) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            try (var socket = new java.net.Socket("localhost", port)) {
                return;
            } catch (IOException ignored) {
                Thread.sleep(10);
            }
        }
        throw new IOException("Server did not start on port " + port);
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}