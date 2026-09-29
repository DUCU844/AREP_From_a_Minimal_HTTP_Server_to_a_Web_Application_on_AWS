package edu.eci.arem.lab2.config;

import java.util.Map;

public final class ServerConfig {
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_STATIC_FILES_PATH = "webroot";
    public static final String DEFAULT_APP_ENV = "development";
    public static final String DEFAULT_GREETING_PREFIX = "Hello";
    public static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 10;

    private final int port;
    private final String staticFilesPath;
    private final String appEnvironment;
    private final String greetingPrefix;
    private final int poolSize;
    private final int shutdownTimeoutSeconds;

    private ServerConfig(int port, String staticFilesPath, String appEnvironment,
                         String greetingPrefix, int poolSize, int shutdownTimeoutSeconds) {
        this.port = port;
        this.staticFilesPath = staticFilesPath;
        this.appEnvironment = appEnvironment;
        this.greetingPrefix = greetingPrefix;
        this.poolSize = poolSize;
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }

    public static ServerConfig fromEnvironment(Map<String, String> environment) {
        int defaultPoolSize = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
        return new ServerConfig(
                positiveInt(environment, "PORT", DEFAULT_PORT, 1, 65535),
                text(environment, "STATIC_FILES_PATH", DEFAULT_STATIC_FILES_PATH),
                text(environment, "APP_ENV", DEFAULT_APP_ENV),
                text(environment, "GREETING_PREFIX", DEFAULT_GREETING_PREFIX),
                positiveInt(environment, "POOL_SIZE", defaultPoolSize, 1, Integer.MAX_VALUE),
                positiveInt(environment, "SHUTDOWN_TIMEOUT_SECONDS", DEFAULT_SHUTDOWN_TIMEOUT_SECONDS, 1,
                        Integer.MAX_VALUE));
    }

    private static String text(Map<String, String> environment, String name, String defaultValue) {
        String value = environment.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int positiveInt(Map<String, String> environment, String name, int defaultValue,
                                   int minimum, int maximum) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed >= minimum && parsed <= maximum) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to the safe default and clear diagnostic.
        }
        System.err.printf("Invalid %s='%s'; using default %d.%n", name, value, defaultValue);
        return defaultValue;
    }

    public int getPort() {
        return port;
    }

    public String getStaticFilesPath() {
        return staticFilesPath;
    }

    public String getAppEnvironment() {
        return appEnvironment;
    }

    public String getGreetingPrefix() {
        return greetingPrefix;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }
}