package org.jphototagger.repository.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation for image files database operations.
 */
public class SqliteImageFilesDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteImageFilesDatabase.class.getName());

    public SqliteImageFilesDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public boolean existsImageFile(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        String sql = "SELECT COUNT(*) FROM files WHERE filename = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return false;
        }
    }

    public boolean insertImageFile(File file, long sizeInBytes, long lastModified) {
        if (file == null) {
            throw new NullPointerException("file == null");
        }
        String sql = "INSERT INTO files (filename, size_in_bytes, lastmodified) VALUES (?, ?, ?)";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, file.getAbsolutePath());
            stmt.setLong(2, sizeInBytes);
            stmt.setLong(3, lastModified);
            int count = stmt.executeUpdate();
            return count > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return false;
        }
    }

    public long getFileCount() {
        String sql = "SELECT COUNT(*) FROM files";
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return 0;
        }
    }

    public List<File> getAllImageFiles() {
        List<File> files = new ArrayList<>();
        String sql = "SELECT filename FROM files";
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                files.add(new File(rs.getString(1)));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
        return files;
    }

    public int deleteImageFile(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        String sql = "DELETE FROM files WHERE filename = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return 0;
        }
    }

    public long findIdImageFile(File file) {
        if (file == null) {
            throw new NullPointerException("file == null");
        }
        String sql = "SELECT id FROM files WHERE filename = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, file.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return -1;
        }
    }
}
