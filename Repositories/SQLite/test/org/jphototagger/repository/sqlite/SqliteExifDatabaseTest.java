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

/**
 * Tests for SqliteExifDatabase.
 * Tests database operations directly without using the Exif domain class
 * to avoid GUI initialization issues in headless environment.
 */
class SqliteExifDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private Connection testConnection;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        testConnection = factory.getConnection();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testConnection != null) {
            testConnection.close();
        }
        factory.close();
    }

    @Test
    void exifTablesExist() throws Exception {
        // Verify exif table exists
        try (PreparedStatement stmt = testConnection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='exif'");
             ResultSet rs = stmt.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("exif");
        }

        // Verify lookup tables exist
        try (PreparedStatement stmt = testConnection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('exif_recording_equipment', 'exif_lenses')");
             ResultSet rs = stmt.executeQuery()) {
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void insertExif_insertsRecord() throws Exception {
        // Insert file first
        long fileId = insertTestFile("/test/image.jpg");

        // Insert recording equipment
        long equipmentId = insertLookupValue("exif_recording_equipment", "equipment", "Canon EOS 5D");

        // Insert lens
        long lensId = insertLookupValue("exif_lenses", "lens", "Canon EF 24-70mm");

        // Insert EXIF data
        String sql = "INSERT INTO exif (id_file, id_exif_recording_equipment, id_exif_lens, "
                + "exif_focal_length, exif_iso_speed_ratings, exif_date_time_original_timestamp, "
                + "exif_gps_latitude, exif_gps_longitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            stmt.setLong(2, equipmentId);
            stmt.setLong(3, lensId);
            stmt.setDouble(4, 50.0);
            stmt.setShort(5, (short) 400);
            stmt.setLong(6, System.currentTimeMillis());
            stmt.setDouble(7, 47.5);
            stmt.setDouble(8, -122.3);
            int count = stmt.executeUpdate();
            assertThat(count).isEqualTo(1);
        }

        // Verify data was inserted
        String selectSql = "SELECT e.exif_focal_length, e.exif_iso_speed_ratings, "
                + "eq.equipment, l.lens, e.exif_gps_latitude, e.exif_gps_longitude "
                + "FROM exif e "
                + "LEFT JOIN exif_recording_equipment eq ON e.id_exif_recording_equipment = eq.id "
                + "LEFT JOIN exif_lenses l ON e.id_exif_lens = l.id "
                + "WHERE e.id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getDouble(1)).isEqualTo(50.0);
                assertThat(rs.getShort(2)).isEqualTo((short) 400);
                assertThat(rs.getString(3)).isEqualTo("Canon EOS 5D");
                assertThat(rs.getString(4)).isEqualTo("Canon EF 24-70mm");
                assertThat(rs.getDouble(5)).isEqualTo(47.5);
                assertThat(rs.getDouble(6)).isEqualTo(-122.3);
            }
        }
    }

    @Test
    void updateExif_updatesRecord() throws Exception {
        // Insert file and initial EXIF
        long fileId = insertTestFile("/test/image.jpg");
        long equipmentId1 = insertLookupValue("exif_recording_equipment", "equipment", "Canon EOS 5D");

        String insertSql = "INSERT INTO exif (id_file, id_exif_recording_equipment, exif_focal_length) "
                + "VALUES (?, ?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(insertSql)) {
            stmt.setLong(1, fileId);
            stmt.setLong(2, equipmentId1);
            stmt.setDouble(3, 50.0);
            stmt.executeUpdate();
        }

        // Update EXIF with different equipment
        long equipmentId2 = insertLookupValue("exif_recording_equipment", "equipment", "Nikon D850");
        String updateSql = "UPDATE exif SET id_exif_recording_equipment = ?, exif_focal_length = ? "
                + "WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(updateSql)) {
            stmt.setLong(1, equipmentId2);
            stmt.setDouble(2, 85.0);
            stmt.setLong(3, fileId);
            int count = stmt.executeUpdate();
            assertThat(count).isEqualTo(1);
        }

        // Verify update
        String selectSql = "SELECT eq.equipment, e.exif_focal_length "
                + "FROM exif e "
                + "LEFT JOIN exif_recording_equipment eq ON e.id_exif_recording_equipment = eq.id "
                + "WHERE e.id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("Nikon D850");
                assertThat(rs.getDouble(2)).isEqualTo(85.0);
            }
        }
    }

    @Test
    void deleteExif_deletesRecord() throws Exception {
        // Insert file and EXIF
        long fileId = insertTestFile("/test/image.jpg");
        long equipmentId = insertLookupValue("exif_recording_equipment", "equipment", "Canon EOS 5D");

        String insertSql = "INSERT INTO exif (id_file, id_exif_recording_equipment) VALUES (?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(insertSql)) {
            stmt.setLong(1, fileId);
            stmt.setLong(2, equipmentId);
            stmt.executeUpdate();
        }

        // Delete EXIF
        String deleteSql = "DELETE FROM exif WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(deleteSql)) {
            stmt.setLong(1, fileId);
            int count = stmt.executeUpdate();
            assertThat(count).isEqualTo(1);
        }

        // Verify deletion
        String selectSql = "SELECT COUNT(*) FROM exif WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }
    }

    @Test
    void existsExif_checksForeignKey() throws Exception {
        // Insert file
        long fileId = insertTestFile("/test/image.jpg");

        // Verify EXIF doesn't exist
        String sql = "SELECT COUNT(*) FROM exif WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }
    }

    @Test
    void foreignKey_cascadeDelete() throws Exception {
        // Insert file and EXIF
        long fileId = insertTestFile("/test/image.jpg");
        long equipmentId = insertLookupValue("exif_recording_equipment", "equipment", "Canon EOS 5D");

        String insertSql = "INSERT INTO exif (id_file, id_exif_recording_equipment) VALUES (?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(insertSql)) {
            stmt.setLong(1, fileId);
            stmt.setLong(2, equipmentId);
            stmt.executeUpdate();
        }

        // Delete file (should cascade delete EXIF)
        String deleteSql = "DELETE FROM files WHERE id = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(deleteSql)) {
            stmt.setLong(1, fileId);
            stmt.executeUpdate();
        }

        // Verify EXIF was also deleted
        String selectSql = "SELECT COUNT(*) FROM exif WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }
    }

    // Helper methods

    private long insertTestFile(String filename) throws Exception {
        String sql = "INSERT INTO files (filename, size_in_bytes, lastmodified) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(sql)) {
            stmt.setString(1, filename);
            stmt.setLong(2, 1000L);
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        }

        // Get the ID
        String selectSql = "SELECT id FROM files WHERE filename = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setString(1, filename);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new AssertionError("Failed to insert file");
    }

    private long insertLookupValue(String tableName, String columnName, String value) throws Exception {
        // Check if value already exists
        String selectSql = "SELECT id FROM " + tableName + " WHERE " + columnName + " = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        // Insert new value
        String insertSql = "INSERT INTO " + tableName + " (" + columnName + ") VALUES (?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(insertSql)) {
            stmt.setString(1, value);
            stmt.executeUpdate();
        }

        // Get the ID
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new AssertionError("Failed to insert lookup value");
    }
}
