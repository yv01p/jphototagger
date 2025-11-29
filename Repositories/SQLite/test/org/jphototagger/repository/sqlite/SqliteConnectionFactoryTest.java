package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.assertj.core.api.Assertions.*;

class SqliteConnectionFactoryTest {

    @TempDir
    File tempDir;

    @Test
    void getConnection_createsNewDatabase() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        try (Connection con = factory.getConnection()) {
            assertThat(con).isNotNull();
            assertThat(con.isClosed()).isFalse();
            assertThat(dbFile).exists();
        }
    }

    @Test
    void getConnection_enablesWalMode() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1).toLowerCase()).isEqualTo("wal");
        }
    }

    @Test
    void getConnection_enablesForeignKeys() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void close_closesAllConnections() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);
        Connection con = factory.getConnection();

        factory.close();

        assertThat(con.isClosed()).isTrue();
    }
}
