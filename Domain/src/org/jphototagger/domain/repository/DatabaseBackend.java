package org.jphototagger.domain.repository;

/**
 * Enumeration of available database backends for JPhotoTagger.
 * Allows switching between HSQLDB and SQLite implementations.
 *
 * @author Claude
 */
public enum DatabaseBackend {
    /**
     * HSQLDB backend (default, legacy implementation)
     */
    HSQLDB,

    /**
     * SQLite backend (new implementation)
     */
    SQLITE;

    /**
     * Parses a string value to a DatabaseBackend enum.
     * Case-insensitive.
     *
     * @param value the string value to parse
     * @return the corresponding DatabaseBackend, or HSQLDB if parsing fails
     */
    public static DatabaseBackend fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return HSQLDB;
        }
        try {
            return DatabaseBackend.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HSQLDB;
        }
    }
}
