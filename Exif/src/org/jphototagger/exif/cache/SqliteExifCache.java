package org.jphototagger.exif.cache;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jphototagger.cachedb.CacheConnectionFactory;
import org.jphototagger.cachedb.CacheDatabase;
import org.jphototagger.exif.ExifTags;
import org.jphototagger.lib.xml.bind.XmlObjectExporter;
import org.jphototagger.lib.xml.bind.XmlObjectImporter;

/**
 * SQLite-backed EXIF metadata cache.
 * Replaces MapDB-based ExifCache.
 */
public final class SqliteExifCache extends CacheDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteExifCache.class.getName());

    private static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS exif_cache (
            file_path TEXT PRIMARY KEY,
            modified_time INTEGER NOT NULL,
            exif_xml TEXT NOT NULL
        )
        """;

    private static final String SELECT_EXISTS = "SELECT modified_time FROM exif_cache WHERE file_path = ?";
    private static final String SELECT_EXIF = "SELECT exif_xml FROM exif_cache WHERE file_path = ?";
    private static final String INSERT_EXIF = "INSERT OR REPLACE INTO exif_cache (file_path, modified_time, exif_xml) VALUES (?, ?, ?)";
    private static final String DELETE_EXIF = "DELETE FROM exif_cache WHERE file_path = ?";
    private static final String SELECT_COUNT = "SELECT COUNT(*) FROM exif_cache";
    private static final String DELETE_ALL = "DELETE FROM exif_cache";

    public SqliteExifCache(CacheConnectionFactory connectionFactory) {
        super(connectionFactory);
        createTable();
    }

    private void createTable() {
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(CREATE_TABLE);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating exif_cache table", e);
            throw new RuntimeException("Failed to create exif_cache table", e);
        }
    }

    public synchronized void cacheExifTags(File imageFile, ExifTags exifTags) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        if (exifTags == null) {
            throw new NullPointerException("exifTags == null");
        }

        exifTags.setLastModified(imageFile.lastModified());

        try {
            String xml = XmlObjectExporter.marshal(exifTags);

            try (Connection con = getConnection();
                 PreparedStatement stmt = con.prepareStatement(INSERT_EXIF)) {
                stmt.setString(1, imageFile.getAbsolutePath());
                stmt.setLong(2, imageFile.lastModified());
                stmt.setString(3, xml);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error caching EXIF tags", e);
        }
    }

    public synchronized boolean containsUpToDateExifTags(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_EXISTS)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long storedModified = rs.getLong(1);
                    return storedModified == imageFile.lastModified();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking EXIF cache", e);
        }
        return false;
    }

    public synchronized ExifTags getCachedExifTags(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_EXIF)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String xml = rs.getString(1);
                    return XmlObjectImporter.unmarshal(xml, ExifTags.class);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting cached EXIF tags", e);
        }
        return null;
    }

    public synchronized void deleteCachedExifTags(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXIF)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting cached EXIF tags", e);
        }
    }

    public synchronized void renameCachedExifTags(File oldImageFile, File newImageFile) {
        try (Connection con = getConnection()) {
            boolean originalAutoCommit = con.getAutoCommit();
            try {
                con.setAutoCommit(false);

                // Get existing EXIF data
                String xml;
                long modifiedTime;
                try (PreparedStatement selectStmt = con.prepareStatement(
                        "SELECT exif_xml, modified_time FROM exif_cache WHERE file_path = ?")) {
                    selectStmt.setString(1, oldImageFile.getAbsolutePath());
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            return;
                        }
                        xml = rs.getString(1);
                        modifiedTime = rs.getLong(2);
                    }
                }

                // Delete old entry
                try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_EXIF)) {
                    deleteStmt.setString(1, oldImageFile.getAbsolutePath());
                    deleteStmt.executeUpdate();
                }

                // Insert with new path
                try (PreparedStatement insertStmt = con.prepareStatement(INSERT_EXIF)) {
                    insertStmt.setString(1, newImageFile.getAbsolutePath());
                    insertStmt.setLong(2, modifiedTime);
                    insertStmt.setString(3, xml);
                    insertStmt.executeUpdate();
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error renaming cached EXIF tags", e);
        }
    }

    public synchronized int clear() {
        int count = 0;
        try (Connection con = getConnection()) {
            try (PreparedStatement countStmt = con.prepareStatement(SELECT_COUNT);
                 ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }

            try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_ALL)) {
                deleteStmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error clearing EXIF cache", e);
            return 0;
        }
        return count;
    }
}
