package edu.eci.arem.lab2.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses the request line (and skips the header block) from the raw socket
 * stream. Only the request line matters for this lab (method, path, query
 * string); headers are read and discarded so the connection stays in a
 * consistent state, but their values are not used.
 */
public final class HttpRequestParser {

    private HttpRequestParser() {
    }

    public static HttpRequest parse(BufferedReader reader) throws IOException, MalformedRequestException {
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            throw new MalformedRequestException("Empty request line");
        }

        // Consume and discard the header lines up to the blank line that
        // separates headers from the body. We don't need header values for
        // this lab, but we must read them off the socket so the stream is
        // left in a clean state.
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            // intentionally ignored
        }

        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new MalformedRequestException("Malformed request line: " + requestLine);
        }

        String method = parts[0];
        String target = parts[1];

        String rawPath;
        Map<String, String> queryParams = new LinkedHashMap<>();
        int queryIndex = target.indexOf('?');
        if (queryIndex >= 0) {
            rawPath = target.substring(0, queryIndex);
            String queryString = target.substring(queryIndex + 1);
            parseQueryString(queryString, queryParams);
        } else {
            rawPath = target;
        }

        String decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);

        return new HttpRequest(method, decodedPath, queryParams, requestLine);
    }

    private static void parseQueryString(String queryString, Map<String, String> out) {
        if (queryString.isEmpty()) {
            return;
        }
        for (String pair : queryString.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key;
            String value;
            if (eq >= 0) {
                key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            } else {
                key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                value = "";
            }
            out.put(key, value);
        }
    }

    public static final class MalformedRequestException extends Exception {
        public MalformedRequestException(String message) {
            super(message);
        }
    }
}
