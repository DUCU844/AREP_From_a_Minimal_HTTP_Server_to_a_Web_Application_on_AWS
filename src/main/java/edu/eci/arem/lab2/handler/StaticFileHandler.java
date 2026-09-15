package edu.eci.arem.lab2.handler;

import edu.eci.arem.lab2.http.ContentTypeResolver;
import edu.eci.arem.lab2.http.HttpRequest;
import edu.eci.arem.lab2.http.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Serves static resources (HTML, JS, CSS, images) from a single "public
 * resources" directory (webroot).
 *
 * Security note: the requested path is resolved against webroot and then
 * normalized; if the normalized, absolute path is not still inside webroot,
 * the request is rejected with 404 instead of ever touching the filesystem
 * outside the intended area. This is what stops "../../etc/passwd"-style
 * requests.
 */
public final class StaticFileHandler {

    private final Path webRoot;
    private final Path defaultDocument;

    public StaticFileHandler(Path webRoot) {
        this.webRoot = webRoot.toAbsolutePath().normalize();
        this.defaultDocument = this.webRoot.resolve("index.html");
    }

    public void handle(HttpRequest request, OutputStream out) throws IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            HttpResponse.sendText(out, 405, "Method Not Allowed", "text/plain; charset=UTF-8",
                    "405 Method Not Allowed: only GET is supported by this server.");
            return;
        }

        String requestedPath = request.getPath();
        Path target = requestedPath.equals("/")
                ? defaultDocument
                : resolveWithinWebRoot(requestedPath);

        if (target == null) {
            // Path traversal attempt or otherwise unsafe path.
            HttpResponse.sendText(out, 404, "Not Found", "text/plain; charset=UTF-8",
                    "404 Not Found");
            return;
        }

        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            HttpResponse.sendText(out, 404, "Not Found", "text/plain; charset=UTF-8",
                    "404 Not Found: " + requestedPath);
            return;
        }

        Optional<String> contentType = ContentTypeResolver.resolve(target.getFileName().toString());
        if (contentType.isEmpty()) {
            HttpResponse.sendText(out, 404, "Not Found", "text/plain; charset=UTF-8",
                    "404 Not Found: unsupported resource type");
            return;
        }

        byte[] bytes = Files.readAllBytes(target); // read as bytes: text and binary share one path
        HttpResponse.send(out, 200, "OK", contentType.get(), bytes);
    }

    /**
     * Resolves a request path against webRoot and returns null if the
     * normalized result escapes webRoot (e.g. via "..").
     */
    private Path resolveWithinWebRoot(String requestedPath) {
        // Strip the leading '/' so resolve() treats it as relative to webRoot.
        String relative = requestedPath.startsWith("/") ? requestedPath.substring(1) : requestedPath;
        Path candidate = webRoot.resolve(relative).normalize();
        if (!candidate.startsWith(webRoot)) {
            return null;
        }
        return candidate;
    }
}
