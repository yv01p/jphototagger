package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

class SqliteConnectionFactoryWalTest {

    @TempDir
    File tempDir;

    @Test
    void enablesWalMode() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        try (Connection conn = factory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
            rs.next();
            assertThat(rs.getString(1).toLowerCase()).isEqualTo("wal");
        }
    }

    @Test
    void setsSynchronousNormal() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        try (Connection conn = factory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA synchronous")) {
            rs.next();
            // NORMAL = 1
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
