package edu.eci.arem.lab2.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Writes a single HTTP/1.1 response to an OutputStream. Body length is
 * always computed from the actual byte array, never from a String's
 * character count, so binary resources (images) and text resources share
 * the exact same response path without corruption.
 */
public final class HttpResponse {

    public static void send(OutputStream out, int statusCode, String reasonPhrase,
                             String contentType, byte[] body) throws IOException {
        StringBuilder headers = new StringBuilder();
        headers.append("HTTP/1.1 ").append(statusCode).append(' ').append(reasonPhrase).append("\r\n");
        headers.append("Content-Type: ").append(contentType).append("\r\n");
        headers.append("Content-Length: ").append(body.length).append("\r\n");
        headers.append("Connection: close\r\n");
        headers.append("\r\n");

        out.write(headers.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    public static void sendText(OutputStream out, int statusCode, String reasonPhrase,
                                 String contentType, String body) throws IOException {
        send(out, statusCode, reasonPhrase, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponse() {
    }
}
