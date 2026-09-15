package edu.eci.arem.lab2.handler;

import edu.eci.arem.lab2.http.HttpRequest;
import edu.eci.arem.lab2.http.HttpResponse;
import edu.eci.arem.lab2.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;

/**
 * Recognizes exactly four hardcoded paths using direct if/else conditions,
 * on purpose (see section 4 of the lab guide): a general routing framework,
 * reflection-based dispatch, or annotations would hide the mechanism this
 * lab exists to teach.
 *
 * Routes:
 *   GET /api/greeting?name=X   -> {"greeting":"Hello, X"}
 *   GET /api/square?value=N    -> {"input":N,"square":N*N}
 *   GET /api/time              -> {"serverTime":"..."}
 *   GET /api/health            -> {"status":"UP"}
 */
public final class ApiHandler {

    public static final String GREETING_PATH = "/api/greeting";
    public static final String SQUARE_PATH = "/api/square";
    public static final String TIME_PATH = "/api/time";
    public static final String HEALTH_PATH = "/api/health";

    /** Returns true if this handler recognizes the given path as a service route. */
    public boolean canHandle(String path) {
        return GREETING_PATH.equals(path)
                || SQUARE_PATH.equals(path)
                || TIME_PATH.equals(path)
                || HEALTH_PATH.equals(path);
    }

    public void handle(HttpRequest request, OutputStream out) throws IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            sendError(out, 405, "Method Not Allowed", "Only GET is supported for service endpoints.");
            return;
        }

        String path = request.getPath();

        if (GREETING_PATH.equals(path)) {
            handleGreeting(request, out);
        } else if (SQUARE_PATH.equals(path)) {
            handleSquare(request, out);
        } else if (TIME_PATH.equals(path)) {
            handleTime(out);
        } else if (HEALTH_PATH.equals(path)) {
            handleHealth(out);
        } else {
            // Should not happen if canHandle() was checked first, but keep it safe.
            sendError(out, 404, "Not Found", "Unknown service route: " + path);
        }
    }

    private void handleGreeting(HttpRequest request, OutputStream out) throws IOException {
        String name = request.getQueryParams().get("name");
        if (name == null || name.isBlank()) {
            sendError(out, 400, "Bad Request", "Missing required query parameter 'name'.");
            return;
        }
        String json = "{\"greeting\":\"Hello, " + JsonUtil.escape(name) + "!\"}";
        HttpResponse.sendText(out, 200, "OK", "application/json; charset=UTF-8", json);
    }

    private void handleSquare(HttpRequest request, OutputStream out) throws IOException {
        String rawValue = request.getQueryParams().get("value");
        if (rawValue == null || rawValue.isBlank()) {
            sendError(out, 400, "Bad Request", "Missing required query parameter 'value'.");
            return;
        }
        double value;
        try {
            value = Double.parseDouble(rawValue);
        } catch (NumberFormatException e) {
            sendError(out, 400, "Bad Request", "'value' must be a valid number.");
            return;
        }
        double square = value * value;
        String json = "{\"input\":" + formatNumber(value) + ",\"square\":" + formatNumber(square) + "}";
        HttpResponse.sendText(out, 200, "OK", "application/json; charset=UTF-8", json);
    }

    private void handleTime(OutputStream out) throws IOException {
        String json = "{\"serverTime\":\"" + JsonUtil.escape(Instant.now().toString()) + "\"}";
        HttpResponse.sendText(out, 200, "OK", "application/json; charset=UTF-8", json);
    }

    private void handleHealth(OutputStream out) throws IOException {
        String json = "{\"status\":\"UP\"}";
        HttpResponse.sendText(out, 200, "OK", "application/json; charset=UTF-8", json);
    }

    private void sendError(OutputStream out, int statusCode, String reason, String message) throws IOException {
        String json = "{\"error\":\"" + JsonUtil.escape(message) + "\"}";
        HttpResponse.sendText(out, statusCode, reason, "application/json; charset=UTF-8", json);
    }

    /** Avoids printing "4.0" for whole numbers, while still supporting decimals. */
    private String formatNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
