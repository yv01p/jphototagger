package org.jphototagger.repository.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation for image collections database operations.
 * Manages collections of images with sequence ordering.
 */
public class SqliteCollectionsDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteCollectionsDatabase.class.getName());

    public SqliteCollectionsDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    /**
     * Returns all collection names in alphabetical order.
     *
     * @return list of collection names, empty if none exist
     */
    public List<String> getAllCollectionNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM collection_names ORDER BY name";
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all collection names", e);
            names.clear();
        }
        return names;
    }

    /**
     * Checks if a collection exists.
     *
     * @param collectionName name of the collection
     * @return true if the collection exists
     */
    public boolean existsCollection(String collectionName) {
        if (collectionName == null) {
            throw new NullPointerException("collectionName == null");
        }
        String sql = "SELECT COUNT(*) FROM collection_names WHERE name = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, collectionName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking collection existence", e);
            return false;
        }
    }

    /**
     * Returns the image files of a collection in sequence order.
     *
     * @param collectionName name of the collection
     * @return list of files in sequence order, empty if collection doesn't exist
     */
    public List<File> getImageFilesOfCollection(String collectionName) {
        if (collectionName == null) {
            throw new NullPointerException("collectionName == null");
        }
        List<File> imageFiles = new ArrayList<>();
        String sql = "SELECT files.filename FROM"
                + " collections INNER JOIN collection_names"
                + " ON collections.id_collectionnname = collection_names.id"
                + " INNER JOIN files ON collections.id_file = files.id"
                + " WHERE collection_names.name = ?"
                + " ORDER BY collections.sequence_number ASC";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, collectionName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    imageFiles.add(new File(rs.getString(1)));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting image files of collection", e);
            imageFiles.clear();
        }
        return imageFiles;
    }

    /**
     * Inserts an image collection into the database.
     * If a collection with that name already exists, it will be deleted first.
     *
     * @param collectionName name of the collection
     * @param imageFiles ordered list of image files
     * @return true if successfully inserted
     */
    public boolean insertCollection(String collectionName, List<File> imageFiles) {
        if (collectionName == null) {
            throw new NullPointerException("collectionName == null");
        }
        if (imageFiles == null) {
            throw new NullPointerException("imageFiles == null");
        }

        // If collection exists, delete it first (without notification)
        if (existsCollection(collectionName)) {
            deleteCollection(collectionName);
        }

        Connection con = null;
        PreparedStatement stmtName = null;
        PreparedStatement stmtColl = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Insert collection name
            stmtName = con.prepareStatement("INSERT INTO collection_names (name) VALUES (?)");
            stmtName.setString(1, collectionName);
            stmtName.executeUpdate();
            close(stmtName);

            // Get the collection ID
            long idCollectionName = findCollectionId(con, collectionName);

            // Verify all files exist in database
            for (File imageFile : imageFiles) {
                if (!existsImageFile(con, imageFile)) {
                    LOGGER.log(Level.WARNING, "File ''{0}'' is not in the database! No collection will be created!", imageFile);
                    rollback(con);
                    return false;
                }
            }

            // Insert collection entries
            stmtColl = con.prepareStatement(
                    "INSERT INTO collections (id_collectionnname, id_file, sequence_number) VALUES (?, ?, ?)");

            int sequenceNumber = 0;
            for (File imageFile : imageFiles) {
                long idImageFile = findImageFileId(con, imageFile);
                stmtColl.setLong(1, idCollectionName);
                stmtColl.setLong(2, idImageFile);
                stmtColl.setInt(3, sequenceNumber);
                stmtColl.executeUpdate();
                sequenceNumber++;
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting collection", e);
            rollback(con);
            return false;
        } finally {
            close(stmtColl);
            close(stmtName);
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    /**
     * Deletes a collection.
     *
     * @param collectionName name of the collection
     * @return true if a collection was deleted, false if no collection with that name exists
     */
    public boolean deleteCollection(String collectionName) {
        if (collectionName == null) {
            throw new NullPointerException("collectionName == null");
        }
        String sql = "DELETE FROM collection_names WHERE name = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, collectionName);
            int deletedRows = stmt.executeUpdate();
            return deletedRows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting collection", e);
            return false;
        }
    }

    /**
     * Returns the count of collections.
     *
     * @return count of collections, or -1 on error
     */
    public int getCollectionCount() {
        String sql = "SELECT COUNT(*) FROM collection_names";
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting collection count", e);
            return -1;
        }
    }

    /**
     * Returns the total count of images across all collections.
     *
     * @return total image count, or -1 on error
     */
    public int getImageCountOfAllCollections() {
        String sql = "SELECT COUNT(*) FROM collections";
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting image count of all collections", e);
            return -1;
        }
    }

    /**
     * Adds images to an existing collection or creates a new collection if it doesn't exist.
     * Duplicates are skipped.
     *
     * @param collectionName name of the collection
     * @param imageFiles images to add
     * @return true if successfully added
     */
    public boolean insertImagesIntoCollection(String collectionName, List<File> imageFiles) {
        if (collectionName == null) {
            throw new NullPointerException("collectionName == null");
        }
        if (imageFiles == null) {
            throw new NullPointerException("imageFiles == null");
        }

        // If collection doesn't exist, create it
        if (!existsCollection(collectionName)) {
            return insertCollection(collectionName, imageFiles);
        }

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            long idCollectionName = findCollectionId(con, collectionName);
            int sequenceNumber = getMaxSequenceNumber(con, collectionName) + 1;

            stmt = con.prepareStatement(
                    "INSERT INTO collections (id_file, id_collectionnname, sequence_number) VALUES (?, ?, ?)");

            for (File imageFile : imageFiles) {
                // Skip if already in collection
                if (!isImageInCollection(con, collectionName, imageFile)) {
                    long idFile = findImageFileId(con, imageFile);
                    stmt.setLong(1, idFile);
                    stmt.setLong(2, idCollectionName);
                    stmt.setInt(3, sequenceNumber);
                    stmt.executeUpdate();
                    sequenceNumber++;
                }
            }

            reorderSequenceNumbers(con, collectionName);
            con.commit();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting images into collection", e);
            rollback(con);
            return false;
        } finally {
            close(stmt);
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    /**
     * Deletes images from a collection.
     *
     * @param collectionName name of the collection
     * @param imageFiles images to delete
     * @return count of deleted images
     */
    public int deleteImagesFromCollection(String collectionName, List<File> imageFiles) {
        if (collectionName == null) {
            throw new NullPointerException("collectionName == null");
        }
        if (imageFiles == null) {
            throw new NullPointerException("imageFiles == null");
        }

        int deleteCount = 0;
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            long idCollectionName = findCollectionId(con, collectionName);
            stmt = con.prepareStatement("DELETE FROM collections WHERE id_collectionnname = ? AND id_file = ?");

            for (File imageFile : imageFiles) {
                long idFile = findImageFileId(con, imageFile);
                if (idFile != -1) {
                    stmt.setLong(1, idCollectionName);
                    stmt.setLong(2, idFile);
                    deleteCount += stmt.executeUpdate();
                }
            }

            reorderSequenceNumbers(con, collectionName);
            con.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting images from collection", e);
            rollback(con);
        } finally {
            close(stmt);
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        return deleteCount;
    }

    /**
     * Renames a collection.
     *
     * @param fromName old name
     * @param toName new name
     * @return count of renamed collections (0 or 1)
     */
    public int renameCollection(String fromName, String toName) {
        if (fromName == null) {
            throw new NullPointerException("fromName == null");
        }
        if (toName == null) {
            throw new NullPointerException("toName == null");
        }
        if (existsCollection(toName)) {
            return 0;
        }

        String sql = "UPDATE collection_names SET name = ? WHERE name = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, toName);
            stmt.setString(2, fromName);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error renaming collection", e);
            return 0;
        }
    }

    /**
     * Checks if a file is in a collection.
     *
     * @param collectionName name of the collection
     * @param filePathname absolute path of the file
     * @return true if the file is in the collection
     */
    public boolean containsFile(String collectionName, String filePathname) {
        Objects.requireNonNull(collectionName, "collectionName == null");
        if (filePathname == null || filePathname.trim().isEmpty()) {
            return true; // Empty string is considered in any set
        }

        String sql = "SELECT COUNT(*) FROM"
                + " collections INNER JOIN collection_names"
                + " ON collections.id_collectionnname = collection_names.id"
                + " INNER JOIN files ON collections.id_file = files.id"
                + " WHERE collection_names.name = ?"
                + " AND files.filename = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, collectionName);
            stmt.setString(2, filePathname);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if file is in collection", e);
            return false;
        }
    }

    // Helper methods

    private long findCollectionId(Connection con, String collectionName) throws SQLException {
        String sql = "SELECT id FROM collection_names WHERE name = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, collectionName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    private long findImageFileId(Connection con, File file) throws SQLException {
        String sql = "SELECT id FROM files WHERE filename = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, file.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    private boolean existsImageFile(Connection con, File imageFile) throws SQLException {
        String sql = "SELECT COUNT(*) FROM files WHERE filename = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean isImageInCollection(Connection con, String collectionName, File imageFile) throws SQLException {
        String sql = "SELECT COUNT(*) FROM"
                + " collections INNER JOIN collection_names"
                + " ON collections.id_collectionnname = collection_names.id"
                + " INNER JOIN files on collections.id_file = files.id"
                + " WHERE collection_names.name = ? AND files.filename = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, collectionName);
            stmt.setString(2, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private int getMaxSequenceNumber(Connection con, String collectionName) throws SQLException {
        String sql = "SELECT MAX(collections.sequence_number)"
                + " FROM collections INNER JOIN collection_names"
                + " ON collections.id_collectionnname = collection_names.id"
                + " AND collection_names.name = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, collectionName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private void reorderSequenceNumbers(Connection con, String collectionName) throws SQLException {
        long idCollectionName = findCollectionId(con, collectionName);

        // Get all file IDs in current sequence order
        List<Long> idFiles = new ArrayList<>();
        String selectSql = "SELECT id_file FROM collections WHERE id_collectionnname = ?"
                + " ORDER BY sequence_number ASC";
        try (PreparedStatement stmt = con.prepareStatement(selectSql)) {
            stmt.setLong(1, idCollectionName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    idFiles.add(rs.getLong(1));
                }
            }
        }

        // Update sequence numbers to be consecutive
        String updateSql = "UPDATE collections SET sequence_number = ?"
                + " WHERE id_collectionnname = ? AND id_file = ?";
        try (PreparedStatement stmt = con.prepareStatement(updateSql)) {
            int sequenceNumber = 0;
            for (Long idFile : idFiles) {
                stmt.setInt(1, sequenceNumber);
                stmt.setLong(2, idCollectionName);
                stmt.setLong(3, idFile);
                stmt.executeUpdate();
                sequenceNumber++;
            }
        }
    }
}
