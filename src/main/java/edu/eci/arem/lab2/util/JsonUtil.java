package edu.eci.arem.lab2.util;

/**
 * Deliberately tiny JSON helper. We are not pulling in a JSON library for
 * four fixed-shape responses; we just need to escape untrusted values
 * safely before interpolating them into a JSON string.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    /** Escapes a string so it can be safely placed inside a JSON string literal. */
    public static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
