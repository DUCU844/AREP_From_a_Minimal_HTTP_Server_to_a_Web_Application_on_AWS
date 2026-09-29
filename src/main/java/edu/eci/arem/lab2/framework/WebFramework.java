package edu.eci.arem.lab2.framework;

import java.io.IOException;
import java.nio.file.Path;

import edu.eci.arem.lab2.config.ServerConfig;
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
        start(ServerConfig.fromEnvironment(System.getenv()));
    }

    public static void start(int port) throws IOException {
        serverInstance = new SimpleHttpServer(port, staticFilesRoot, router);
        serverInstance.start();
    }

    public static void start(ServerConfig config) throws IOException {
        serverInstance = new SimpleHttpServer(config.getPort(), Path.of(config.getStaticFilesPath()), router,
                config.getPoolSize(), config.getShutdownTimeoutSeconds());
        serverInstance.start();
    }

    public static void stop() {
        if (serverInstance != null) serverInstance.stop();
    }
    
}
