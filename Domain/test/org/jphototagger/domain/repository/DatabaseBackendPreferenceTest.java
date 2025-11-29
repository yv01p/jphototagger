package org.jphototagger.domain.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseBackendPreference.
 * Note: These tests manipulate Java Preferences and system properties,
 * so they clean up after themselves.
 *
 * @author Claude
 */
class DatabaseBackendPreferenceTest {

    private String originalSystemProperty;

    @BeforeEach
    void setUp() {
        // Save original system property
        originalSystemProperty = System.getProperty("jphototagger.database.backend");
        // Clear system property for clean tests
        System.clearProperty("jphototagger.database.backend");
        // Clear stored preference
        DatabaseBackendPreference.clearPreference();
    }

    @AfterEach
    void tearDown() {
        // Restore original system property
        if (originalSystemProperty != null) {
            System.setProperty("jphototagger.database.backend", originalSystemProperty);
        } else {
            System.clearProperty("jphototagger.database.backend");
        }
        // Clear stored preference
        DatabaseBackendPreference.clearPreference();
    }

    @Test
    void getPreference_defaultsToHSQLDB() {
        DatabaseBackend backend = DatabaseBackendPreference.getPreference();
        assertEquals(DatabaseBackend.HSQLDB, backend, "Default should be HSQLDB");
    }

    @Test
    void setPreference_storesAndRetrievesBackend() {
        DatabaseBackendPreference.setPreference(DatabaseBackend.SQLITE);
        DatabaseBackend backend = DatabaseBackendPreference.getPreference();
        assertEquals(DatabaseBackend.SQLITE, backend, "Should retrieve stored preference");
    }

    @Test
    void clearPreference_revertsToDefault() {
        DatabaseBackendPreference.setPreference(DatabaseBackend.SQLITE);
        DatabaseBackendPreference.clearPreference();
        DatabaseBackend backend = DatabaseBackendPreference.getPreference();
        assertEquals(DatabaseBackend.HSQLDB, backend, "Should revert to HSQLDB after clear");
    }

    @Test
    void systemProperty_overridesStoredPreference() {
        // Set stored preference to HSQLDB
        DatabaseBackendPreference.setPreference(DatabaseBackend.HSQLDB);

        // Set system property to SQLite
        System.setProperty("jphototagger.database.backend", "sqlite");

        DatabaseBackend backend = DatabaseBackendPreference.getPreference();
        assertEquals(DatabaseBackend.SQLITE, backend, "System property should override stored preference");
    }

    @Test
    void systemProperty_caseInsensitive() {
        System.setProperty("jphototagger.database.backend", "SqLiTe");
        DatabaseBackend backend = DatabaseBackendPreference.getPreference();
        assertEquals(DatabaseBackend.SQLITE, backend, "System property should be case-insensitive");
    }

    @Test
    void systemProperty_invalidValueUsesDefault() {
        System.setProperty("jphototagger.database.backend", "invalid");
        DatabaseBackend backend = DatabaseBackendPreference.getPreference();
        assertEquals(DatabaseBackend.HSQLDB, backend, "Invalid system property should use default");
    }

    @Test
    void hasSystemPropertyOverride_detectsOverride() {
        assertFalse(DatabaseBackendPreference.hasSystemPropertyOverride(), "Should be false initially");

        System.setProperty("jphototagger.database.backend", "sqlite");
        assertTrue(DatabaseBackendPreference.hasSystemPropertyOverride(), "Should be true when property set");
    }

    @Test
    void setPreference_throwsOnNull() {
        assertThrows(NullPointerException.class, () -> {
            DatabaseBackendPreference.setPreference(null);
        }, "Should throw on null backend");
    }
}
