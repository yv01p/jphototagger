package org.jphototagger.cachedb;

import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * SQLite-backed thumbnail cache.
 * Replaces MapDB-based ThumbnailsDb.
 */
public final class SqliteThumbnailCache extends CacheDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteThumbnailCache.class.getName());

    private static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS thumbnails (
            file_path TEXT PRIMARY KEY,
            modified_time INTEGER NOT NULL,
            file_length INTEGER NOT NULL,
            thumbnail BLOB NOT NULL
        )
        """;

    private static final String SELECT_EXISTS = "SELECT 1 FROM thumbnails WHERE file_path = ?";
    private static final String SELECT_THUMBNAIL = "SELECT thumbnail FROM thumbnails WHERE file_path = ?";
    private static final String SELECT_FOR_VALIDATION = "SELECT modified_time, file_length FROM thumbnails WHERE file_path = ?";
    private static final String INSERT_THUMBNAIL = "INSERT OR REPLACE INTO thumbnails (file_path, modified_time, file_length, thumbnail) VALUES (?, ?, ?, ?)";
    private static final String DELETE_THUMBNAIL = "DELETE FROM thumbnails WHERE file_path = ?";
    private static final String SELECT_ALL_PATHS = "SELECT file_path FROM thumbnails";

    public SqliteThumbnailCache(CacheConnectionFactory connectionFactory) {
        super(connectionFactory);
        createTable();
    }

    private void createTable() {
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(CREATE_TABLE);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating thumbnails table", e);
            throw new RuntimeException("Failed to create thumbnails table", e);
        }
    }

    public boolean existsThumbnail(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_EXISTS)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking thumbnail existence", e);
            return false;
        }
    }

    public Image findThumbnail(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_THUMBNAIL)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = getBytes(rs, 1);
                    if (bytes != null) {
                        return new ImageIcon(bytes).getImage();
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding thumbnail", e);
        }
        return null;
    }

    public boolean hasUpToDateThumbnail(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_FOR_VALIDATION)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long storedModified = rs.getLong(1);
                    long storedLength = rs.getLong(2);
                    return storedModified == imageFile.lastModified()
                        && storedLength == imageFile.length();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking thumbnail freshness", e);
        }
        return false;
    }

    public void insertThumbnail(Image thumbnail, File imageFile) {
        byte[] bytes = toJpegBytes(thumbnail);
        if (bytes == null) {
            LOGGER.log(Level.WARNING, "Failed to convert thumbnail to bytes for {0}", imageFile);
            return;
        }

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_THUMBNAIL)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            stmt.setLong(2, imageFile.lastModified());
            stmt.setLong(3, imageFile.length());
            stmt.setBytes(4, bytes);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting thumbnail", e);
        }
    }

    public boolean deleteThumbnail(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_THUMBNAIL)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting thumbnail", e);
            return false;
        }
    }

    public boolean renameThumbnail(File fromImageFile, File toImageFile) {
        try (Connection con = getConnection()) {
            boolean originalAutoCommit = con.getAutoCommit();
            try {
                con.setAutoCommit(false);
                try {
                    // Get existing thumbnail
                    byte[] bytes;
                    long modifiedTime;
                    long fileLength;
                    try (PreparedStatement selectStmt = con.prepareStatement(
                            "SELECT thumbnail, modified_time, file_length FROM thumbnails WHERE file_path = ?")) {
                        selectStmt.setString(1, fromImageFile.getAbsolutePath());
                        try (ResultSet rs = selectStmt.executeQuery()) {
                            if (!rs.next()) {
                                return false;
                            }
                            bytes = rs.getBytes(1);
                            modifiedTime = rs.getLong(2);
                            fileLength = rs.getLong(3);
                        }
                    }

                    // Delete old entry
                    try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_THUMBNAIL)) {
                        deleteStmt.setString(1, fromImageFile.getAbsolutePath());
                        deleteStmt.executeUpdate();
                    }

                    // Insert with new path
                    try (PreparedStatement insertStmt = con.prepareStatement(INSERT_THUMBNAIL)) {
                        insertStmt.setString(1, toImageFile.getAbsolutePath());
                        insertStmt.setLong(2, modifiedTime);
                        insertStmt.setLong(3, fileLength);
                        insertStmt.setBytes(4, bytes);
                        insertStmt.executeUpdate();
                    }

                    con.commit();
                    return true;
                } catch (SQLException e) {
                    con.rollback();
                    throw e;
                }
            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error renaming thumbnail", e);
            return false;
        }
    }

    public Set<String> getImageFilenames() {
        Set<String> filenames = new HashSet<>();
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ALL_PATHS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                filenames.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting image filenames", e);
        }
        return filenames;
    }

    public void compact() {
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("VACUUM");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error compacting database", e);
        }
    }

    private byte[] toJpegBytes(Image image) {
        try {
            BufferedImage buffered;
            if (image instanceof BufferedImage) {
                buffered = (BufferedImage) image;
            } else {
                int w = image.getWidth(null);
                int h = image.getHeight(null);
                if (w <= 0 || h <= 0) {
                    return null;
                }
                buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = buffered.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "jpeg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error converting image to bytes", e);
            return null;
        }
    }
}
