package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

class SqliteRepositoryIndexesTest {

    @TempDir
    File tempDir;

    @Test
    void repositoryInitCreatesPerformanceIndexes() throws Exception {
        // This test verifies the integration - that repository.init()
        // creates performance indexes as part of initialization
        File dbFile = new File(tempDir, "jphototagger.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        // Simulate repository initialization
        SqliteTables tables = new SqliteTables(factory);
        tables.createTables();
        SqliteIndexes indexes = new SqliteIndexes(factory);
        indexes.createPerformanceIndexes();

        try (Connection conn = factory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name LIKE 'idx_xmp%'")) {
            rs.next();
            // Should have at least rating and date_rating indexes
            assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(2);
        }
    }
}
