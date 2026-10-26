package ru.mirea.cloud.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {
    private static Properties properties(String... pairs) {
        Properties result = new Properties();
        for (int i = 0; i < pairs.length; i += 2) result.setProperty(pairs[i], pairs[i + 1]);
        return result;
    }

    private static Map<String, String> environment() {
        return new HashMap<>(Map.of("DB_URL", "jdbc:postgresql://test-host/test-db",
                "DB_USER", "test-user", "DB_PASSWORD", "test-password"));
    }

    @Test void readsRequiredSettingsFromEnvironmentWithoutPackagedDefaults() {
        Properties result = DatabaseManager.connectionSettings(new Properties(), environment());
        assertEquals("jdbc:postgresql://test-host/test-db", result.getProperty("db.url"));
        assertEquals("test-user", result.getProperty("db.user"));
        assertEquals("test-password", result.getProperty("db.password"));
        IllegalStateException absent = assertThrows(IllegalStateException.class,
                () -> DatabaseManager.connectionSettings(new Properties(), Map.of()));
        assertTrue(absent.getMessage().contains("db.url"));
    }

    @Test void systemPropertiesTakePrecedenceAndCanSupplyAllSettings() {
        Properties system = properties("db.url", " jdbc:postgresql://system/test-db ",
                "db.user", " system-user ", "db.password", " exact password ");
        Properties withEnvironment = DatabaseManager.connectionSettings(system, environment());
        Properties withoutEnvironment = DatabaseManager.connectionSettings(system, Map.of());
        assertEquals(withEnvironment, withoutEnvironment);
        assertEquals("jdbc:postgresql://system/test-db", withEnvironment.getProperty("db.url"));
        assertEquals("system-user", withEnvironment.getProperty("db.user"));
        assertEquals(" exact password ", withEnvironment.getProperty("db.password"));
    }

    @Test void acceptsMixedSourcesAndPreservesEnvironmentPasswordWhitespace() {
        Map<String, String> environment = environment();
        environment.put("DB_PASSWORD", " environment password ");
        Properties result = DatabaseManager.connectionSettings(properties("db.user", "system-user"), environment);
        assertEquals("jdbc:postgresql://test-host/test-db", result.getProperty("db.url"));
        assertEquals("system-user", result.getProperty("db.user"));
        assertEquals(" environment password ", result.getProperty("db.password"));
    }

    @ParameterizedTest @ValueSource(strings = {"url", "user", "password"})
    void rejectsEachMissingSettingWithoutLeakingOtherValues(String key) {
        Map<String, String> environment = environment();
        environment.remove("DB_" + key.toUpperCase(Locale.ROOT));
        IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> DatabaseManager.connectionSettings(new Properties(), environment));
        assertTrue(missing.getMessage().contains("db." + key));
        assertTrue(missing.getMessage().contains("DB_" + key.toUpperCase(Locale.ROOT)));
        environment.values().forEach(value -> assertFalse(missing.getMessage().contains(value)));
    }

    @ParameterizedTest @ValueSource(strings = {"url", "user", "password"})
    void rejectsBlankEnvironmentSettingsAndDoesNotFallBackFromBlankSystemProperties(String key) {
        for (String blank : List.of("", " ", "\t\n")) {
            Map<String, String> environment = environment();
            IllegalStateException systemFailure = assertThrows(IllegalStateException.class,
                    () -> DatabaseManager.connectionSettings(properties("db." + key, blank), environment));
            assertTrue(systemFailure.getMessage().contains("db." + key));
            environment.values().forEach(value -> assertFalse(systemFailure.getMessage().contains(value)));

            environment.put("DB_" + key.toUpperCase(Locale.ROOT), blank);
            IllegalStateException environmentFailure = assertThrows(IllegalStateException.class,
                    () -> DatabaseManager.connectionSettings(new Properties(), environment));
            assertTrue(environmentFailure.getMessage().contains("db." + key));
        }
    }

    @Test void invalidUrlMessageDoesNotRevealAnyConnectionValues() {
        Map<String, String> environment = environment();
        environment.put("DB_URL", "invalid-test-url");
        IllegalStateException invalidUrl = assertThrows(IllegalStateException.class,
                () -> DatabaseManager.connectionSettings(new Properties(), environment));
        assertTrue(invalidUrl.getMessage().contains("JDBC"));
        environment.values().forEach(value -> assertFalse(invalidUrl.getMessage().contains(value)));
    }
}
