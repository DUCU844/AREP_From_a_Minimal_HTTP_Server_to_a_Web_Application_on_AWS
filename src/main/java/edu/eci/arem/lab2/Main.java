package edu.eci.arem.lab2;

import edu.eci.arem.lab2.config.ServerConfig;
import static edu.eci.arem.lab2.framework.WebFramework.*;
import edu.eci.arem.lab2.util.JsonUtil;

import java.time.Instant;

/**
 * Entry point.
 *
 * Configuration is read from environment variables so the same jar runs locally
 * and in a container without command-line-specific deployment scripts.
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.fromEnvironment(System.getenv());
        staticfiles(config.getStaticFilesPath());

        get("/api/greeting", (req, resp) -> {
            resp.setContentType("application/json; charset=UTF-8");
            String name = req.getQueryParams().get("name");
            if (name == null || name.isBlank()) name = "world";
                return "{\"greeting\":\"" + config.getGreetingPrefix() + ", "
                    + JsonUtil.escape(name) + "!\"}";
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

        if ("development".equals(config.getAppEnvironment())) {
            get("/shutdown", (req, resp) -> {
                stop();
                return "Server will stop after this response.";
            });
            get("/api/slow", (req, resp) -> {
                resp.setContentType("application/json; charset=UTF-8");
                String rawMilliseconds = req.getQueryParams().get("ms");
                long milliseconds;
                try {
                    milliseconds = Long.parseLong(rawMilliseconds == null ? "" : rawMilliseconds);
                } catch (NumberFormatException e) {
                    return "{\"error\":\"ms must be an integer between 0 and 10000\"}";
                }
                if (milliseconds < 0 || milliseconds > 10_000) {
                    return "{\"error\":\"ms must be an integer between 0 and 10000\"}";
                }
                try {
                    Thread.sleep(milliseconds);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "{\"sleptMs\":" + milliseconds + "}";
            });
        }

        start(config);
    }
}
