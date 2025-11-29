package org.jphototagger.repository.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation for XMP metadata database operations.
 * Handles XMP table and all its lookup tables (dc_creators, dc_rights,
 * iptc4xmpcore_locations, photoshop_* tables) and the xmp_dc_subject junction table.
 */
public class SqliteXmpDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteXmpDatabase.class.getName());

    private static final Set<String> VALID_XMP_FIELDS = Set.of(
        "dc_description", "dc_title", "photoshop_headline", "photoshop_instructions",
        "photoshop_transmissionReference", "iptc4xmpcore_datecreated", "rating"
    );

    public SqliteXmpDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    /**
     * Checks if XMP metadata exists for a given file ID.
     *
     * @param fileId the file ID
     * @return true if XMP exists, false otherwise
     */
    public boolean existsXmp(long fileId) {
        String sql = "SELECT COUNT(*) FROM xmp WHERE id_file = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking XMP existence for file ID: " + fileId, e);
            return false;
        }
    }

    /**
     * Gets the XMP ID for a given file ID.
     *
     * @param fileId the file ID
     * @return the XMP ID, or -1 if not found
     */
    public long getXmpId(long fileId) {
        String sql = "SELECT id FROM xmp WHERE id_file = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting XMP ID for file ID: " + fileId, e);
        }
        return -1;
    }

    /**
     * Inserts XMP metadata for a file.
     * This is a simplified version that handles basic XMP fields.
     *
     * @param fileId the file ID
     * @param dcCreator Dublin Core creator
     * @param dcDescription Dublin Core description
     * @param dcRights Dublin Core rights
     * @param dcTitle Dublin Core title
     * @param location IPTC4XMP Core location
     * @param city Photoshop city
     * @param country Photoshop country
     * @param headline Photoshop headline
     * @param rating rating (0-5)
     * @return true if inserted successfully
     */
    public boolean insertXmp(long fileId, String dcCreator, String dcDescription,
                            String dcRights, String dcTitle, String location,
                            String city, String country, String headline, Integer rating) {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Get or create lookup IDs
            Long creatorId = ensureValueExists("dc_creators", "creator", dcCreator);
            Long rightsId = ensureValueExists("dc_rights", "rights", dcRights);
            Long locationId = ensureValueExists("iptc4xmpcore_locations", "location", location);
            Long cityId = ensureValueExists("photoshop_cities", "city", city);
            Long countryId = ensureValueExists("photoshop_countries", "country", country);

            // Insert XMP
            String sql = "INSERT INTO xmp (id_file, id_dc_creator, dc_description, "
                    + "id_dc_rights, dc_title, id_iptc4xmpcore_location, id_photoshop_city, "
                    + "id_photoshop_country, photoshop_headline, rating) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            stmt = con.prepareStatement(sql);
            stmt.setLong(1, fileId);
            setLong(creatorId, stmt, 2);
            setString(dcDescription, stmt, 3);
            setLong(rightsId, stmt, 4);
            setString(dcTitle, stmt, 5);
            setLong(locationId, stmt, 6);
            setLong(cityId, stmt, 7);
            setLong(countryId, stmt, 8);
            setString(headline, stmt, 9);
            if (rating != null) {
                stmt.setInt(10, rating);
            } else {
                stmt.setNull(10, java.sql.Types.INTEGER);
            }

            int count = stmt.executeUpdate();
            con.commit();
            return count > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting XMP for file ID: " + fileId, e);
            rollback(con);
            return false;
        } finally {
            close(stmt);
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    /**
     * Updates XMP metadata for a file.
     *
     * @param fileId the file ID
     * @param dcCreator Dublin Core creator
     * @param dcDescription Dublin Core description
     * @param dcRights Dublin Core rights
     * @param dcTitle Dublin Core title
     * @param location IPTC4XMP Core location
     * @param city Photoshop city
     * @param country Photoshop country
     * @param headline Photoshop headline
     * @param rating rating (0-5)
     * @return true if updated successfully
     */
    public boolean updateXmp(long fileId, String dcCreator, String dcDescription,
                            String dcRights, String dcTitle, String location,
                            String city, String country, String headline, Integer rating) {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Get or create lookup IDs
            Long creatorId = ensureValueExists("dc_creators", "creator", dcCreator);
            Long rightsId = ensureValueExists("dc_rights", "rights", dcRights);
            Long locationId = ensureValueExists("iptc4xmpcore_locations", "location", location);
            Long cityId = ensureValueExists("photoshop_cities", "city", city);
            Long countryId = ensureValueExists("photoshop_countries", "country", country);

            // Update XMP
            String sql = "UPDATE xmp SET id_dc_creator = ?, dc_description = ?, "
                    + "id_dc_rights = ?, dc_title = ?, id_iptc4xmpcore_location = ?, "
                    + "id_photoshop_city = ?, id_photoshop_country = ?, "
                    + "photoshop_headline = ?, rating = ? WHERE id_file = ?";

            stmt = con.prepareStatement(sql);
            setLong(creatorId, stmt, 1);
            setString(dcDescription, stmt, 2);
            setLong(rightsId, stmt, 3);
            setString(dcTitle, stmt, 4);
            setLong(locationId, stmt, 5);
            setLong(cityId, stmt, 6);
            setLong(countryId, stmt, 7);
            setString(headline, stmt, 8);
            if (rating != null) {
                stmt.setInt(9, rating);
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }
            stmt.setLong(10, fileId);

            int count = stmt.executeUpdate();
            con.commit();
            return count > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating XMP for file ID: " + fileId, e);
            rollback(con);
            return false;
        } finally {
            close(stmt);
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    /**
     * Deletes XMP metadata for a file.
     *
     * @param fileId the file ID
     * @return the number of rows deleted
     */
    public int deleteXmp(long fileId) {
        String sql = "DELETE FROM xmp WHERE id_file = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting XMP for file ID: " + fileId, e);
            return 0;
        }
    }

    /**
     * Inserts a DC subject (keyword) and links it to an XMP record.
     *
     * @param xmpId the XMP ID
     * @param subject the subject/keyword
     * @return true if inserted successfully
     */
    public boolean insertXmpDcSubject(long xmpId, String subject) {
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Ensure subject exists
            Long subjectId = ensureValueExists("dc_subjects", "subject", subject);
            if (subjectId == null) {
                return false;
            }

            // Check if link already exists
            if (existsXmpDcSubjectLink(con, xmpId, subjectId)) {
                con.commit();
                return true;
            }

            // Insert link
            String sql = "INSERT INTO xmp_dc_subject (id_xmp, id_dc_subject) VALUES (?, ?)";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setLong(1, xmpId);
                stmt.setLong(2, subjectId);
                int count = stmt.executeUpdate();
                con.commit();
                return count > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting XMP DC subject for XMP ID: " + xmpId, e);
            rollback(con);
            return false;
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    /**
     * Checks if a link between XMP and DC subject exists.
     *
     * @param con the database connection
     * @param xmpId the XMP ID
     * @param subjectId the subject ID
     * @return true if the link exists
     */
    private boolean existsXmpDcSubjectLink(Connection con, long xmpId, long subjectId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM xmp_dc_subject WHERE id_xmp = ? AND id_dc_subject = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, xmpId);
            stmt.setLong(2, subjectId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Deletes all DC subjects (keywords) for an XMP record.
     *
     * @param xmpId the XMP ID
     * @return the number of links deleted
     */
    public int deleteXmpDcSubjects(long xmpId) {
        String sql = "DELETE FROM xmp_dc_subject WHERE id_xmp = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, xmpId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting XMP DC subjects for XMP ID: " + xmpId, e);
            return 0;
        }
    }

    /**
     * Gets all DC subjects (keywords) for an XMP record.
     *
     * @param xmpId the XMP ID
     * @return list of subjects
     */
    public List<String> getXmpDcSubjects(long xmpId) {
        List<String> subjects = new ArrayList<>();
        String sql = "SELECT s.subject FROM xmp_dc_subject x "
                + "JOIN dc_subjects s ON x.id_dc_subject = s.id "
                + "WHERE x.id_xmp = ? ORDER BY s.subject";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, xmpId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    subjects.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting XMP DC subjects for XMP ID: " + xmpId, e);
        }
        return subjects;
    }

    /**
     * Gets a specific XMP field value.
     *
     * @param fileId the file ID
     * @param fieldName the XMP field name
     * @return the field value, or null if not found
     */
    public String getXmpField(long fileId, String fieldName) {
        if (fieldName == null || !VALID_XMP_FIELDS.contains(fieldName)) {
            throw new IllegalArgumentException("Invalid field name: " + fieldName);
        }
        String sql = "SELECT " + fieldName + " FROM xmp WHERE id_file = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting XMP field " + fieldName + " for file ID: " + fileId, e);
        }
        return null;
    }

    /**
     * Gets the rating for a file.
     *
     * @param fileId the file ID
     * @return the rating, or null if not found
     */
    public Integer getRating(long fileId) {
        String sql = "SELECT rating FROM xmp WHERE id_file = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, fileId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int rating = rs.getInt(1);
                    return rs.wasNull() ? null : rating;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting rating for file ID: " + fileId, e);
        }
        return null;
    }

    /**
     * Updates the rating for a file.
     *
     * @param fileId the file ID
     * @param rating the new rating (0-5), or null to clear
     * @return true if updated successfully
     */
    public boolean updateRating(long fileId, Integer rating) {
        String sql = "UPDATE xmp SET rating = ? WHERE id_file = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            if (rating != null) {
                stmt.setInt(1, rating);
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }
            stmt.setLong(2, fileId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating rating for file ID: " + fileId, e);
            return false;
        }
    }
}
