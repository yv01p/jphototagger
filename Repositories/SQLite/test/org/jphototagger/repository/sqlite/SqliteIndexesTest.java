package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

class SqliteIndexesTest {

    @TempDir
    File tempDir;

    @Test
    void createsPerformanceIndexes() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        try (Connection conn = factory.getConnection()) {
            SqliteTables tables = new SqliteTables(factory);
            tables.createTables();

            SqliteIndexes indexes = new SqliteIndexes(factory);
            indexes.createPerformanceIndexes();

            // Verify rating index exists
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_xmp_rating'")) {
                assertThat(rs.next()).isTrue();
            }
        }
    }
}
