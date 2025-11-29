package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SqliteXmpDatabase.
 * Tests database operations directly without using the Xmp domain class
 * to avoid GUI initialization issues in headless environment.
 */
class SqliteXmpDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private Connection testConnection;
    private SqliteXmpDatabase xmpDatabase;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        testConnection = factory.getConnection();
        xmpDatabase = new SqliteXmpDatabase(factory);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (testConnection != null) {
            testConnection.close();
        }
        factory.close();
    }

    @Test
    void xmpTablesExist() throws Exception {
        // Verify xmp table exists
        try (PreparedStatement stmt = testConnection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='xmp'");
             ResultSet rs = stmt.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("xmp");
        }

        // Verify junction table exists
        try (PreparedStatement stmt = testConnection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='xmp_dc_subject'");
             ResultSet rs = stmt.executeQuery()) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("xmp_dc_subject");
        }

        // Verify lookup tables exist
        String[] lookupTables = {
            "dc_creators", "dc_rights", "dc_subjects", "iptc4xmpcore_locations",
            "photoshop_authorspositions", "photoshop_captionwriters", "photoshop_cities",
            "photoshop_countries", "photoshop_credits", "photoshop_sources", "photoshop_states"
        };

        for (String table : lookupTables) {
            try (PreparedStatement stmt = testConnection.prepareStatement(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?")) {
                stmt.setString(1, table);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).as("Table %s should exist", table).isTrue();
                }
            }
        }
    }

    @Test
    void insertXmp_insertsRecord() throws Exception {
        // Insert file first
        long fileId = insertTestFile("/test/image.jpg");

        // Insert lookup values
        long creatorId = insertLookupValue("dc_creators", "creator", "John Doe");
        long rightsId = insertLookupValue("dc_rights", "rights", "Copyright 2025");
        long locationId = insertLookupValue("iptc4xmpcore_locations", "location", "New York");
        long cityId = insertLookupValue("photoshop_cities", "city", "NYC");
        long countryId = insertLookupValue("photoshop_countries", "country", "USA");

        // Insert XMP data
        String sql = "INSERT INTO xmp (id_file, id_dc_creator, dc_description, id_dc_rights, "
                + "dc_title, id_iptc4xmpcore_location, id_photoshop_city, id_photoshop_country, "
                + "photoshop_headline, rating) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            stmt.setLong(2, creatorId);
            stmt.setString(3, "A beautiful landscape");
            stmt.setLong(4, rightsId);
            stmt.setString(5, "Sunset");
            stmt.setLong(6, locationId);
            stmt.setLong(7, cityId);
            stmt.setLong(8, countryId);
            stmt.setString(9, "Amazing Sunset");
            stmt.setInt(10, 5);
            int count = stmt.executeUpdate();
            assertThat(count).isEqualTo(1);
        }

        // Get XMP ID
        long xmpId = getXmpIdForFile(fileId);

        // Insert keywords (dc_subjects)
        List<String> keywords = List.of("sunset", "landscape", "nature");
        for (String keyword : keywords) {
            long subjectId = insertLookupValue("dc_subjects", "subject", keyword);
            insertXmpDcSubjectLink(xmpId, subjectId);
        }

        // Verify data was inserted
        String selectSql = "SELECT x.dc_description, x.dc_title, x.photoshop_headline, x.rating, "
                + "c.creator, r.rights, l.location, ci.city, co.country "
                + "FROM xmp x "
                + "LEFT JOIN dc_creators c ON x.id_dc_creator = c.id "
                + "LEFT JOIN dc_rights r ON x.id_dc_rights = r.id "
                + "LEFT JOIN iptc4xmpcore_locations l ON x.id_iptc4xmpcore_location = l.id "
                + "LEFT JOIN photoshop_cities ci ON x.id_photoshop_city = ci.id "
                + "LEFT JOIN photoshop_countries co ON x.id_photoshop_country = co.id "
                + "WHERE x.id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("A beautiful landscape");
                assertThat(rs.getString(2)).isEqualTo("Sunset");
                assertThat(rs.getString(3)).isEqualTo("Amazing Sunset");
                assertThat(rs.getInt(4)).isEqualTo(5);
                assertThat(rs.getString(5)).isEqualTo("John Doe");
                assertThat(rs.getString(6)).isEqualTo("Copyright 2025");
                assertThat(rs.getString(7)).isEqualTo("New York");
                assertThat(rs.getString(8)).isEqualTo("NYC");
                assertThat(rs.getString(9)).isEqualTo("USA");
            }
        }

        // Verify keywords
        List<String> storedKeywords = getKeywordsForXmp(xmpId);
        assertThat(storedKeywords).containsExactlyInAnyOrder("sunset", "landscape", "nature");
    }

    @Test
    void updateXmp_updatesRecord() throws Exception {
        // Insert file and initial XMP
        long fileId = insertTestFile("/test/image.jpg");
        long creatorId1 = insertLookupValue("dc_creators", "creator", "John Doe");

        String insertSql = "INSERT INTO xmp (id_file, id_dc_creator, dc_title, rating) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(insertSql)) {
            stmt.setLong(1, fileId);
            stmt.setLong(2, creatorId1);
            stmt.setString(3, "Original Title");
            stmt.setInt(4, 3);
            stmt.executeUpdate();
        }

        // Update XMP with different creator
        long creatorId2 = insertLookupValue("dc_creators", "creator", "Jane Smith");
        String updateSql = "UPDATE xmp SET id_dc_creator = ?, dc_title = ?, rating = ? "
                + "WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(updateSql)) {
            stmt.setLong(1, creatorId2);
            stmt.setString(2, "Updated Title");
            stmt.setInt(3, 5);
            stmt.setLong(4, fileId);
            int count = stmt.executeUpdate();
            assertThat(count).isEqualTo(1);
        }

        // Verify update
        String selectSql = "SELECT c.creator, x.dc_title, x.rating "
                + "FROM xmp x "
                + "LEFT JOIN dc_creators c ON x.id_dc_creator = c.id "
                + "WHERE x.id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("Jane Smith");
                assertThat(rs.getString(2)).isEqualTo("Updated Title");
                assertThat(rs.getInt(3)).isEqualTo(5);
            }
        }
    }

    @Test
    void deleteXmp_deletesRecord() throws Exception {
        // Insert file and XMP
        long fileId = insertTestFile("/test/image.jpg");
        long creatorId = insertLookupValue("dc_creators", "creator", "John Doe");

        String insertSql = "INSERT INTO xmp (id_file, id_dc_creator) VALUES (?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(insertSql)) {
            stmt.setLong(1, fileId);
            stmt.setLong(2, creatorId);
            stmt.executeUpdate();
        }

        // Delete XMP
        String deleteSql = "DELETE FROM xmp WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(deleteSql)) {
            stmt.setLong(1, fileId);
            int count = stmt.executeUpdate();
            assertThat(count).isEqualTo(1);
        }

        // Verify deletion
        String selectSql = "SELECT COUNT(*) FROM xmp WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }
    }

    @Test
    void existsXmp_checksForRecord() throws Exception {
        // Insert file
        long fileId = insertTestFile("/test/image.jpg");

        // Verify XMP doesn't exist
        assertThat(xmpDatabase.existsXmp(fileId)).isFalse();

        // Insert XMP using database class
        boolean inserted = xmpDatabase.insertXmp(fileId, "John Doe", "Description",
                "Copyright", "Title", "Location", "City", "Country", "Headline", 5);
        assertThat(inserted).isTrue();

        // Verify XMP exists
        assertThat(xmpDatabase.existsXmp(fileId)).isTrue();
    }

    @Test
    void foreignKey_cascadeDelete() throws Exception {
        // Insert file and XMP
        long fileId = insertTestFile("/test/image.jpg");
        long creatorId = insertLookupValue("dc_creators", "creator", "John Doe");

        String insertSql = "INSERT INTO xmp (id_file, id_dc_creator) VALUES (?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(insertSql)) {
            stmt.setLong(1, fileId);
            stmt.setLong(2, creatorId);
            stmt.executeUpdate();
        }

        long xmpId = getXmpIdForFile(fileId);

        // Insert keywords
        long subjectId = insertLookupValue("dc_subjects", "subject", "test");
        insertXmpDcSubjectLink(xmpId, subjectId);

        // Delete file (should cascade delete XMP and junction table entries)
        String deleteSql = "DELETE FROM files WHERE id = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(deleteSql)) {
            stmt.setLong(1, fileId);
            stmt.executeUpdate();
        }

        // Verify XMP was also deleted
        String selectSql = "SELECT COUNT(*) FROM xmp WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(selectSql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }

        // Verify junction table entries were deleted
        String junctionSql = "SELECT COUNT(*) FROM xmp_dc_subject WHERE id_xmp = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(junctionSql)) {
            stmt.setLong(1, xmpId);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }
        }
    }

    @Test
    void xmpDatabase_insertAndGetXmp() throws Exception {
        // Insert file
        long fileId = insertTestFile("/test/image.jpg");

        // Insert XMP using database API
        boolean inserted = xmpDatabase.insertXmp(fileId, "John Doe", "A beautiful landscape",
                "Copyright 2025", "Sunset", "New York", "NYC", "USA", "Amazing Sunset", 5);
        assertThat(inserted).isTrue();

        // Get XMP ID
        long xmpId = xmpDatabase.getXmpId(fileId);
        assertThat(xmpId).isGreaterThan(0);

        // Get rating
        Integer rating = xmpDatabase.getRating(fileId);
        assertThat(rating).isEqualTo(5);

        // Get specific fields
        assertThat(xmpDatabase.getXmpField(fileId, "dc_description")).isEqualTo("A beautiful landscape");
        assertThat(xmpDatabase.getXmpField(fileId, "dc_title")).isEqualTo("Sunset");
        assertThat(xmpDatabase.getXmpField(fileId, "photoshop_headline")).isEqualTo("Amazing Sunset");
    }

    @Test
    void xmpDatabase_updateXmp() throws Exception {
        // Insert file and XMP
        long fileId = insertTestFile("/test/image.jpg");
        xmpDatabase.insertXmp(fileId, "John Doe", "Original description",
                "Copyright 2025", "Original Title", null, null, null, null, 3);

        // Update XMP
        boolean updated = xmpDatabase.updateXmp(fileId, "Jane Smith", "Updated description",
                "Copyright 2026", "Updated Title", "Paris", "Paris", "France", "New Headline", 5);
        assertThat(updated).isTrue();

        // Verify updates
        assertThat(xmpDatabase.getXmpField(fileId, "dc_description")).isEqualTo("Updated description");
        assertThat(xmpDatabase.getXmpField(fileId, "dc_title")).isEqualTo("Updated Title");
        assertThat(xmpDatabase.getXmpField(fileId, "photoshop_headline")).isEqualTo("New Headline");
        assertThat(xmpDatabase.getRating(fileId)).isEqualTo(5);
    }

    @Test
    void xmpDatabase_deleteXmp() throws Exception {
        // Insert file and XMP
        long fileId = insertTestFile("/test/image.jpg");
        xmpDatabase.insertXmp(fileId, "John Doe", "Description", null, null, null, null, null, null, null);

        // Verify XMP exists
        assertThat(xmpDatabase.existsXmp(fileId)).isTrue();

        // Delete XMP
        int deleted = xmpDatabase.deleteXmp(fileId);
        assertThat(deleted).isEqualTo(1);

        // Verify XMP doesn't exist
        assertThat(xmpDatabase.existsXmp(fileId)).isFalse();
    }

    @Test
    void xmpDatabase_manageKeywords() throws Exception {
        // Insert file and XMP
        long fileId = insertTestFile("/test/image.jpg");
        xmpDatabase.insertXmp(fileId, null, null, null, null, null, null, null, null, null);
        long xmpId = xmpDatabase.getXmpId(fileId);

        // Insert keywords
        assertThat(xmpDatabase.insertXmpDcSubject(xmpId, "nature")).isTrue();
        assertThat(xmpDatabase.insertXmpDcSubject(xmpId, "landscape")).isTrue();
        assertThat(xmpDatabase.insertXmpDcSubject(xmpId, "sunset")).isTrue();

        // Get keywords
        List<String> keywords = xmpDatabase.getXmpDcSubjects(xmpId);
        assertThat(keywords).containsExactly("landscape", "nature", "sunset"); // Sorted

        // Delete keywords
        int deleted = xmpDatabase.deleteXmpDcSubjects(xmpId);
        assertThat(deleted).isEqualTo(3);

        // Verify keywords deleted
        keywords = xmpDatabase.getXmpDcSubjects(xmpId);
        assertThat(keywords).isEmpty();
    }

    @Test
    void xmpDatabase_updateRating() throws Exception {
        // Insert file and XMP
        long fileId = insertTestFile("/test/image.jpg");
        xmpDatabase.insertXmp(fileId, null, null, null, null, null, null, null, null, 3);

        // Verify initial rating
        assertThat(xmpDatabase.getRating(fileId)).isEqualTo(3);

        // Update rating
        assertThat(xmpDatabase.updateRating(fileId, 5)).isTrue();
        assertThat(xmpDatabase.getRating(fileId)).isEqualTo(5);

        // Clear rating
        assertThat(xmpDatabase.updateRating(fileId, null)).isTrue();
        assertThat(xmpDatabase.getRating(fileId)).isNull();
    }

    @Test
    void xmpDcSubject_manyToMany() throws Exception {
        // Test that multiple XMP records can share the same keywords
        long fileId1 = insertTestFile("/test/image1.jpg");
        long fileId2 = insertTestFile("/test/image2.jpg");

        // Insert XMP for both files
        String insertSql = "INSERT INTO xmp (id_file, dc_title) VALUES (?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(insertSql)) {
            stmt.setLong(1, fileId1);
            stmt.setString(2, "Image 1");
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = testConnection.prepareStatement(insertSql)) {
            stmt.setLong(1, fileId2);
            stmt.setString(2, "Image 2");
            stmt.executeUpdate();
        }

        long xmpId1 = getXmpIdForFile(fileId1);
        long xmpId2 = getXmpIdForFile(fileId2);

        // Insert shared keyword
        long subjectId = insertLookupValue("dc_subjects", "subject", "nature");
        insertXmpDcSubjectLink(xmpId1, subjectId);
        insertXmpDcSubjectLink(xmpId2, subjectId);

        // Verify both XMP records have the keyword
        List<String> keywords1 = getKeywordsForXmp(xmpId1);
        List<String> keywords2 = getKeywordsForXmp(xmpId2);
        assertThat(keywords1).containsExactly("nature");
        assertThat(keywords2).containsExactly("nature");
    }

    @Test
    void getXmpField_rejectsInvalidFieldName() throws Exception {
        // Insert test file and XMP
        long fileId = insertTestFile("/test/image.jpg");
        xmpDatabase.insertXmp(fileId, "John Doe", "A description",
                "Copyright 2025", "Title", null, null, null, "Headline", 3);

        // Test null field name
        assertThatThrownBy(() -> xmpDatabase.getXmpField(fileId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid field name");

        // Test invalid field name (SQL injection attempt)
        assertThatThrownBy(() -> xmpDatabase.getXmpField(fileId, "id FROM xmp; DROP TABLE xmp; --"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid field name");

        // Test valid field names still work
        assertThat(xmpDatabase.getXmpField(fileId, "dc_description")).isEqualTo("A description");
        assertThat(xmpDatabase.getXmpField(fileId, "dc_title")).isEqualTo("Title");
        assertThat(xmpDatabase.getXmpField(fileId, "photoshop_headline")).isEqualTo("Headline");
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

    private long getXmpIdForFile(long fileId) throws Exception {
        String sql = "SELECT id FROM xmp WHERE id_file = ?";
        try (PreparedStatement stmt = testConnection.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new AssertionError("XMP not found for file ID: " + fileId);
    }

    private void insertXmpDcSubjectLink(long xmpId, long subjectId) throws Exception {
        String sql = "INSERT INTO xmp_dc_subject (id_xmp, id_dc_subject) VALUES (?, ?)";
        try (PreparedStatement stmt = testConnection.prepareStatement(sql)) {
            stmt.setLong(1, xmpId);
            stmt.setLong(2, subjectId);
            stmt.executeUpdate();
        }
    }

    private List<String> getKeywordsForXmp(long xmpId) throws Exception {
        List<String> keywords = new ArrayList<>();
        String sql = "SELECT s.subject FROM xmp_dc_subject x "
                + "JOIN dc_subjects s ON x.id_dc_subject = s.id "
                + "WHERE x.id_xmp = ? ORDER BY s.subject";
        try (PreparedStatement stmt = testConnection.prepareStatement(sql)) {
            stmt.setLong(1, xmpId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    keywords.add(rs.getString(1));
                }
            }
        }
        return keywords;
    }
}
