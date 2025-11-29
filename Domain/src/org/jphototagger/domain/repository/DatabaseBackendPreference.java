package org.jphototagger.domain.repository;

import java.util.prefs.Preferences;

/**
 * Manages user preference for database backend selection.
 * Allows switching between HSQLDB and SQLite at runtime.
 *
 * The preference can be set via:
 * 1. System property: -Djphototagger.database.backend=sqlite
 * 2. User preference stored in Java Preferences API
 * 3. Default: HSQLDB (for backwards compatibility)
 *
 * System property takes precedence over stored preference.
 *
 * @author Claude
 */
public final class DatabaseBackendPreference {

    private static final String SYSTEM_PROPERTY = "jphototagger.database.backend";
    private static final String PREFERENCE_KEY = "database.backend";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(DatabaseBackendPreference.class);

    private DatabaseBackendPreference() {
        // Utility class
    }

    /**
     * Gets the current database backend preference.
     * Checks system property first, then stored preference, then defaults to HSQLDB.
     *
     * @return the preferred database backend
     */
    public static DatabaseBackend getPreference() {
        // Check system property first (highest priority)
        String systemProperty = System.getProperty(SYSTEM_PROPERTY);
        if (systemProperty != null && !systemProperty.trim().isEmpty()) {
            return DatabaseBackend.fromString(systemProperty);
        }

        // Check stored preference
        String storedPreference = PREFERENCES.get(PREFERENCE_KEY, null);
        if (storedPreference != null) {
            return DatabaseBackend.fromString(storedPreference);
        }

        // Default to HSQLDB for backwards compatibility
        return DatabaseBackend.HSQLDB;
    }

    /**
     * Sets the database backend preference.
     * This stores the preference in Java Preferences API for future sessions.
     * Note: System property will override this if set.
     *
     * @param backend the database backend to use
     */
    public static void setPreference(DatabaseBackend backend) {
        if (backend == null) {
            throw new NullPointerException("backend == null");
        }
        PREFERENCES.put(PREFERENCE_KEY, backend.name());
    }

    /**
     * Clears the stored preference, reverting to default (HSQLDB).
     * Note: System property will still take precedence if set.
     */
    public static void clearPreference() {
        PREFERENCES.remove(PREFERENCE_KEY);
    }

    /**
     * Checks if a system property override is currently active.
     *
     * @return true if system property is set, false otherwise
     */
    public static boolean hasSystemPropertyOverride() {
        String systemProperty = System.getProperty(SYSTEM_PROPERTY);
        return systemProperty != null && !systemProperty.trim().isEmpty();
    }
}
