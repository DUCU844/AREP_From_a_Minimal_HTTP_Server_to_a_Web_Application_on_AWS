package edu.eci.arem.lab2;

import edu.eci.arem.lab2.server.SimpleHttpServer;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Entry point.
 *
 * Usage:
 *   java -jar networking-lab2.jar [port] [webRootPath]
 *
 * Both arguments are optional:
 *   - port defaults to 8080
 *   - webRootPath defaults to "webroot" (relative to the working directory),
 *     which is what lets the exact same jar run locally and on EC2: you
 *     just place the jar next to the webroot/ folder in both places.
 */
public final class Main {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_WEBROOT = "webroot";

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        String webRootArg = DEFAULT_WEBROOT;

        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port '" + args[0] + "', falling back to " + DEFAULT_PORT);
            }
        }
        if (args.length >= 2) {
            webRootArg = args[1];
        }

        Path webRoot = Path.of(webRootArg);
        SimpleHttpServer server = new SimpleHttpServer(port, webRoot);
        server.start();
    }
}
