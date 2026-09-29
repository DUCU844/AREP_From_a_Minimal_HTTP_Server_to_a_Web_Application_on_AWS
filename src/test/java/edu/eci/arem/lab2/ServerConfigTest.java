package edu.eci.arem.lab2;

import edu.eci.arem.lab2.config.ServerConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigTest {

    @Test
    void usesSafeDefaultsWhenEnvironmentIsEmpty() {
        ServerConfig config = ServerConfig.fromEnvironment(Map.of());

        assertEquals(8080, config.getPort());
        assertEquals("webroot", config.getStaticFilesPath());
        assertEquals("development", config.getAppEnvironment());
        assertEquals("Hello", config.getGreetingPrefix());
        assertTrue(config.getPoolSize() >= 1);
        assertEquals(10, config.getShutdownTimeoutSeconds());
    }

    @Test
    void rejectsInvalidNumericValuesUsingDefaults() {
        ServerConfig config = ServerConfig.fromEnvironment(Map.of(
                "PORT", "not-a-port",
                "POOL_SIZE", "0",
                "SHUTDOWN_TIMEOUT_SECONDS", "-4"));

        assertEquals(8080, config.getPort());
        assertTrue(config.getPoolSize() >= 1);
        assertEquals(10, config.getShutdownTimeoutSeconds());
    }

    @Test
    void readsAllConfiguredValues() {
        ServerConfig config = ServerConfig.fromEnvironment(Map.of(
                "PORT", "9090",
                "STATIC_FILES_PATH", "public",
                "APP_ENV", "production",
                "GREETING_PREFIX", "Hola",
                "POOL_SIZE", "4",
                "SHUTDOWN_TIMEOUT_SECONDS", "3"));

        assertEquals(9090, config.getPort());
        assertEquals("public", config.getStaticFilesPath());
        assertEquals("production", config.getAppEnvironment());
        assertEquals("Hola", config.getGreetingPrefix());
        assertEquals(4, config.getPoolSize());
        assertEquals(3, config.getShutdownTimeoutSeconds());
    }
}