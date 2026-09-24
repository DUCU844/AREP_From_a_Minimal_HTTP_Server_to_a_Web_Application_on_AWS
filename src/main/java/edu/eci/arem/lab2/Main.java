package edu.eci.arem.lab2;

import static edu.eci.arem.lab2.framework.WebFramework.*;

import edu.eci.arem.lab2.server.SimpleHttpServer;
import edu.eci.arem.lab2.util.JsonUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

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

    public static void main(String[] args) throws Exception {
        staticfiles(System.getenv().getOrDefault("STATIC_FILES_PATH", "webroot"));

        get("/api/greeting", (req, resp) -> {
            resp.setContentType("application/json; charset=UTF-8");
            String name = req.getQueryParams().get("name");
            if (name == null || name.isBlank()) name = "world";
            String prefix = System.getenv().getOrDefault("GREETING_PREFIX", "Hello");
            return "{\"greeting\":\"" + prefix + ", " + JsonUtil.escape(name) + "!\"}";
        });

        get("/api/square", (req, resp) -> {
            resp.setContentType("application/json; charset=UTF-8");
            String raw = req.getQueryParams().get("value");
            double value = (raw == null || raw.isBlank()) ? 0 : Double.parseDouble(raw);
            return "{\"input\":" + value + ",\"square\":" + (value * value) + "}";
        });

        get("/api/time", (req, resp) -> {
            resp.setContentType("application/json; charset=UTF-8");
            return "{\"serverTime\":\"" + JsonUtil.escape(Instant.now().toString()) + "\"}";
        });

        get("/api/health", (req, resp) -> {
            resp.setContentType("application/json; charset=UTF-8");
            return "{\"status\":\"UP\"}";
        });

        String environment = System.getenv().getOrDefault("APP_ENV", "development");
        if (environment.equals("development")) {
            get("/shutdown", (req, resp) -> {
                stop();
                return "Server will stop after this response.";
            });
        }

        start();
    }
}
