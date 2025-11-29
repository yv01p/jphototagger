package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import static org.assertj.core.api.Assertions.*;

class SqliteDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private TestDatabase database;

    @BeforeEach
    void setUp() {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        database = new TestDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getConnection_returnsValidConnection() throws Exception {
        try (Connection con = database.getTestConnection()) {
            assertThat(con).isNotNull();
            assertThat(con.isClosed()).isFalse();
        }
    }

    @Test
    void close_closesResourcesProperly() throws Exception {
        Connection con = database.getTestConnection();
        PreparedStatement stmt = con.prepareStatement("SELECT 1");
        ResultSet rs = stmt.executeQuery();

        SqliteDatabase.close(rs, stmt);

        assertThat(stmt.isClosed()).isTrue();
    }

    // Test subclass to access protected methods
    private static class TestDatabase extends SqliteDatabase {
        TestDatabase(SqliteConnectionFactory factory) {
            super(factory);
        }

        Connection getTestConnection() throws Exception {
            return getConnection();
        }
    }
}
