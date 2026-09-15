package edu.eci.arem.lab2.http;

import java.util.Locale;
import java.util.Map;

/**
 * Associates supported file extensions with their HTTP response content
 * type. Anything not in this table is treated as an unsupported / unknown
 * resource type.
 */
public final class ContentTypeResolver {

    private static final Map<String, String> EXTENSION_TO_TYPE = Map.of(
            "html", "text/html; charset=UTF-8",
            "htm", "text/html; charset=UTF-8",
            "js", "application/javascript; charset=UTF-8",
            "css", "text/css; charset=UTF-8",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "svg", "image/svg+xml",
            "ico", "image/x-icon",
            "txt", "text/plain; charset=UTF-8"
    );

    private ContentTypeResolver() {
    }

    /** Returns the content type for the given file name, or empty if the extension is not supported. */
    public static java.util.Optional<String> resolve(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return java.util.Optional.empty();
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return java.util.Optional.ofNullable(EXTENSION_TO_TYPE.get(extension));
    }
}
