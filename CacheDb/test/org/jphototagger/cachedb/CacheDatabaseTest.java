package org.jphototagger.cachedb;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class CacheDatabaseTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private TestCacheDatabase database;

    @BeforeEach
    void setUp() throws SQLException {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        database = new TestCacheDatabase(factory);
        database.createTestTable();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getConnection_returnsConnection() throws SQLException {
        try (Connection con = database.testGetConnection()) {
            assertThat(con).isNotNull();
            assertThat(con.isClosed()).isFalse();
        }
    }

    @Test
    void setBytes_writesAndReadsBlob() throws SQLException {
        byte[] data = {1, 2, 3, 4, 5};
        database.insertBytes("test-key", data);

        byte[] result = database.selectBytes("test-key");
        assertThat(result).isEqualTo(data);
    }

    @Test
    void setBytes_nullValueWritesNull() throws SQLException {
        database.insertBytes("null-key", null);

        byte[] result = database.selectBytes("null-key");
        assertThat(result).isNull();
    }

    // Test implementation to expose protected methods
    private static class TestCacheDatabase extends CacheDatabase {
        TestCacheDatabase(CacheConnectionFactory factory) {
            super(factory);
        }

        Connection testGetConnection() throws SQLException {
            return getConnection();
        }

        void createTestTable() throws SQLException {
            try (Connection con = getConnection();
                 PreparedStatement stmt = con.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS test (key TEXT PRIMARY KEY, data BLOB)")) {
                stmt.executeUpdate();
            }
        }

        void insertBytes(String key, byte[] data) throws SQLException {
            try (Connection con = getConnection();
                 PreparedStatement stmt = con.prepareStatement(
                     "INSERT OR REPLACE INTO test (key, data) VALUES (?, ?)")) {
                stmt.setString(1, key);
                setBytes(data, stmt, 2);
                stmt.executeUpdate();
            }
        }

        byte[] selectBytes(String key) throws SQLException {
            try (Connection con = getConnection();
                 PreparedStatement stmt = con.prepareStatement(
                     "SELECT data FROM test WHERE key = ?")) {
                stmt.setString(1, key);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return getBytes(rs, 1);
                    }
                }
            }
            return null;
        }
    }
}
