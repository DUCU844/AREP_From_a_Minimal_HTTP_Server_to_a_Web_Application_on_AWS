package edu.eci.arem.lab2.framework;

import java.io.IOException;
import java.nio.file.Path;

import edu.eci.arem.lab2.server.SimpleHttpServer;

public class WebFramework {
    private static final Router router = new Router();
    private static Path staticFilesRoot = Path.of("webroot");
    private static SimpleHttpServer serverInstance;

    public static void staticfiles(String root) {
        staticFilesRoot = Path.of(root);
    }

    public static void get(String path, Service service) {
        router.addGet(path, service);
    }

    public static void start() throws IOException {
        start(resolvePort());
    }

    public static void start(int port) throws IOException {
        serverInstance = new SimpleHttpServer(port, staticFilesRoot, router);
        serverInstance.start();
    }

    public static void stop() {
        if (serverInstance != null) serverInstance.stop();
    }
    
    private static int resolvePort() {
        String portValue = System.getenv("PORT");
        return (portValue == null || portValue.isBlank()) ? 8080 : Integer.parseInt(portValue);
    }
}
