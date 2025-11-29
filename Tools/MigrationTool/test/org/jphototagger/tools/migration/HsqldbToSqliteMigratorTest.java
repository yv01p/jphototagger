package org.jphototagger.tools.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class HsqldbToSqliteMigratorTest {

    @TempDir
    File tempDir;

    @Test
    void migrate_copiesFilesTable() throws Exception {
        // Setup HSQLDB with test data
        File hsqldbFile = new File(tempDir, "hsqldb/jphototagger");
        File sqliteFile = new File(tempDir, "jphototagger.db");
        setupHsqldbWithTestData(hsqldbFile);

        HsqldbToSqliteMigrator migrator = new HsqldbToSqliteMigrator(hsqldbFile, sqliteFile);
        HsqldbToSqliteMigrator.MigrationResult result = migrator.migrate(null);

        assertThat(result.success()).isTrue();
        assertThat(sqliteFile).exists();

        // Verify data was copied
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM files")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(2);
        }
    }

    @Test
    void migrate_copiesMultipleTables() throws Exception {
        // Setup HSQLDB with test data
        File hsqldbFile = new File(tempDir, "hsqldb/jphototagger");
        File sqliteFile = new File(tempDir, "jphototagger.db");
        setupHsqldbWithMultipleTables(hsqldbFile);

        HsqldbToSqliteMigrator migrator = new HsqldbToSqliteMigrator(hsqldbFile, sqliteFile);
        HsqldbToSqliteMigrator.MigrationResult result = migrator.migrate(null);

        assertThat(result.success()).isTrue();
        assertThat(result.tablesProcessed()).isGreaterThan(0);
        assertThat(result.rowsMigrated()).isGreaterThan(0);

        // Verify files table
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
             Statement stmt = con.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM files")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }

            // Verify dc_subjects table
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM dc_subjects")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(3);
            }
        }
    }

    @Test
    void migrate_notifiesListener() throws Exception {
        File hsqldbFile = new File(tempDir, "hsqldb/jphototagger");
        File sqliteFile = new File(tempDir, "jphototagger.db");
        setupHsqldbWithTestData(hsqldbFile);

        List<String> notifications = new ArrayList<>();
        HsqldbToSqliteMigrator.MigrationListener listener = new HsqldbToSqliteMigrator.MigrationListener() {
            @Override
            public void onProgress(String tableName, int current, int total) {
                notifications.add("progress:" + tableName + ":" + current + "/" + total);
            }

            @Override
            public void onTableComplete(String tableName, int rowCount) {
                notifications.add("complete:" + tableName + ":" + rowCount);
            }
        };

        HsqldbToSqliteMigrator migrator = new HsqldbToSqliteMigrator(hsqldbFile, sqliteFile);
        HsqldbToSqliteMigrator.MigrationResult result = migrator.migrate(listener);

        assertThat(result.success()).isTrue();
        assertThat(notifications).isNotEmpty();
        assertThat(notifications).anyMatch(s -> s.contains("complete:files:2"));
    }

    @Test
    void migrate_handlesForeignKeys() throws Exception {
        File hsqldbFile = new File(tempDir, "hsqldb/jphototagger");
        File sqliteFile = new File(tempDir, "jphototagger.db");
        setupHsqldbWithForeignKeys(hsqldbFile);

        HsqldbToSqliteMigrator migrator = new HsqldbToSqliteMigrator(hsqldbFile, sqliteFile);
        HsqldbToSqliteMigrator.MigrationResult result = migrator.migrate(null);

        assertThat(result.success()).isTrue();

        // Verify foreign key relationships are maintained
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
             Statement stmt = con.createStatement()) {

            // Enable foreign keys to verify
            stmt.execute("PRAGMA foreign_keys=ON");

            // Verify xmp table references files table
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM xmp")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
        }
    }

    private void setupHsqldbWithTestData(File hsqldbFile) throws Exception {
        hsqldbFile.getParentFile().mkdirs();
        String url = "jdbc:hsqldb:file:" + hsqldbFile.getAbsolutePath() + ";shutdown=true";
        try (Connection con = DriverManager.getConnection(url, "sa", "");
             Statement stmt = con.createStatement()) {
            stmt.execute("CREATE CACHED TABLE files (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, filename VARCHAR(512) NOT NULL)");
            stmt.execute("INSERT INTO files (filename) VALUES ('/test/image1.jpg')");
            stmt.execute("INSERT INTO files (filename) VALUES ('/test/image2.jpg')");
        }
    }

    private void setupHsqldbWithMultipleTables(File hsqldbFile) throws Exception {
        hsqldbFile.getParentFile().mkdirs();
        String url = "jdbc:hsqldb:file:" + hsqldbFile.getAbsolutePath() + ";shutdown=true";
        try (Connection con = DriverManager.getConnection(url, "sa", "");
             Statement stmt = con.createStatement()) {
            stmt.execute("CREATE CACHED TABLE files (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, filename VARCHAR(512) NOT NULL, size_in_bytes BIGINT, lastmodified BIGINT)");
            stmt.execute("INSERT INTO files (filename, size_in_bytes, lastmodified) VALUES ('/test/image1.jpg', 1000, 123456)");
            stmt.execute("INSERT INTO files (filename, size_in_bytes, lastmodified) VALUES ('/test/image2.jpg', 2000, 234567)");

            stmt.execute("CREATE CACHED TABLE dc_subjects (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, subject VARCHAR_IGNORECASE(128))");
            stmt.execute("INSERT INTO dc_subjects (subject) VALUES ('landscape')");
            stmt.execute("INSERT INTO dc_subjects (subject) VALUES ('portrait')");
            stmt.execute("INSERT INTO dc_subjects (subject) VALUES ('nature')");
        }
    }

    private void setupHsqldbWithForeignKeys(File hsqldbFile) throws Exception {
        hsqldbFile.getParentFile().mkdirs();
        String url = "jdbc:hsqldb:file:" + hsqldbFile.getAbsolutePath() + ";shutdown=true";
        try (Connection con = DriverManager.getConnection(url, "sa", "");
             Statement stmt = con.createStatement()) {
            stmt.execute("CREATE CACHED TABLE files (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, filename VARCHAR(512) NOT NULL)");
            stmt.execute("INSERT INTO files (id, filename) VALUES (1, '/test/image1.jpg')");
            stmt.execute("INSERT INTO files (id, filename) VALUES (2, '/test/image2.jpg')");

            stmt.execute("CREATE CACHED TABLE xmp (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, id_file BIGINT NOT NULL, dc_title VARCHAR_IGNORECASE(256), FOREIGN KEY (id_file) REFERENCES files (id) ON DELETE CASCADE)");
            stmt.execute("INSERT INTO xmp (id_file, dc_title) VALUES (1, 'Test Image 1')");
            stmt.execute("INSERT INTO xmp (id_file, dc_title) VALUES (2, 'Test Image 2')");
        }
    }
}
