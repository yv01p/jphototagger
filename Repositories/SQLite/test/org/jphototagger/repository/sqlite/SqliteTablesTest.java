package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class SqliteTablesTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void createTables_createsFilesTable() throws Exception {
        SqliteTables tables = new SqliteTables(factory);

        tables.createTables();

        Set<String> tableNames = getTableNames();
        assertThat(tableNames).contains("files");
    }

    @Test
    void createTables_createsXmpTables() throws Exception {
        SqliteTables tables = new SqliteTables(factory);

        tables.createTables();

        Set<String> tableNames = getTableNames();
        assertThat(tableNames).contains("xmp", "dc_subjects", "xmp_dc_subject");
    }

    @Test
    void createTables_createsExifTables() throws Exception {
        SqliteTables tables = new SqliteTables(factory);

        tables.createTables();

        Set<String> tableNames = getTableNames();
        assertThat(tableNames).contains("exif", "exif_recording_equipment", "exif_lenses");
    }

    @Test
    void createTables_isIdempotent() throws Exception {
        SqliteTables tables = new SqliteTables(factory);

        tables.createTables();
        tables.createTables(); // Should not throw

        Set<String> tableNames = getTableNames();
        assertThat(tableNames).contains("files");
    }

    @Test
    void filesTable_hasAutoIncrementId() throws Exception {
        SqliteTables tables = new SqliteTables(factory);
        tables.createTables();

        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("INSERT INTO files (filename) VALUES ('/test/image.jpg')");
            try (ResultSet rs = stmt.executeQuery("SELECT id FROM files")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong("id")).isEqualTo(1);
            }
        }
    }

    private Set<String> getTableNames() throws Exception {
        Set<String> names = new HashSet<>();
        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }
}
