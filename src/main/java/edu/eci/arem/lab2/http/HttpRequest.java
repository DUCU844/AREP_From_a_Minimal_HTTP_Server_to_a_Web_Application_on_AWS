package edu.eci.arem.lab2.http;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable representation of a parsed HTTP request line + query string.
 * We deliberately do not model headers or body: this lab only needs GET
 * requests with an optional query string.
 */
public final class HttpRequest {

    private final String method;
    private final String path;          // normalized, decoded, without query string
    private final Map<String, String> queryParams;
    private final String rawRequestLine;

    public HttpRequest(String method, String path, Map<String, String> queryParams, String rawRequestLine) {
        this.method = method;
        this.path = path;
        this.queryParams = Collections.unmodifiableMap(queryParams);
        this.rawRequestLine = rawRequestLine;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getRawRequestLine() {
        return rawRequestLine;
    }
}
