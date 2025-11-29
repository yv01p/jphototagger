package org.jphototagger.cachedb;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class CacheConnectionFactoryTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private File dbFile;

    @BeforeEach
    void setUp() {
        dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getConnection_createsDatabase() throws Exception {
        Connection con = factory.getConnection();
        assertThat(con).isNotNull();
        assertThat(dbFile).exists();
        con.close();
    }

    @Test
    void getConnection_enablesWalMode() throws Exception {
        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualToIgnoringCase("wal");
        }
    }

    @Test
    void getConnection_setsSynchronousNormal() throws Exception {
        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA synchronous")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1); // NORMAL=1
        }
    }

    @Test
    void close_closesAllConnections() throws Exception {
        Connection con1 = factory.getConnection();
        Connection con2 = factory.getConnection();
        factory.close();
        assertThat(con1.isClosed()).isTrue();
        assertThat(con2.isClosed()).isTrue();
    }
}
