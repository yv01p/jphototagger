package org.jphototagger.domain.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseBackend enum.
 *
 * @author Claude
 */
class DatabaseBackendTest {

    @Test
    void fromString_parsesHSQLDB() {
        assertEquals(DatabaseBackend.HSQLDB, DatabaseBackend.fromString("HSQLDB"));
        assertEquals(DatabaseBackend.HSQLDB, DatabaseBackend.fromString("hsqldb"));
        assertEquals(DatabaseBackend.HSQLDB, DatabaseBackend.fromString("HsQlDb"));
    }

    @Test
    void fromString_parsesSQLite() {
        assertEquals(DatabaseBackend.SQLITE, DatabaseBackend.fromString("SQLITE"));
        assertEquals(DatabaseBackend.SQLITE, DatabaseBackend.fromString("sqlite"));
        assertEquals(DatabaseBackend.SQLITE, DatabaseBackend.fromString("SqLiTe"));
    }

    @Test
    void fromString_handlesNullAsDefault() {
        assertEquals(DatabaseBackend.HSQLDB, DatabaseBackend.fromString(null));
    }

    @Test
    void fromString_handlesEmptyAsDefault() {
        assertEquals(DatabaseBackend.HSQLDB, DatabaseBackend.fromString(""));
        assertEquals(DatabaseBackend.HSQLDB, DatabaseBackend.fromString("   "));
    }

    @Test
    void fromString_handlesInvalidAsDefault() {
        assertEquals(DatabaseBackend.HSQLDB, DatabaseBackend.fromString("invalid"));
        assertEquals(DatabaseBackend.HSQLDB, DatabaseBackend.fromString("postgresql"));
    }
}
