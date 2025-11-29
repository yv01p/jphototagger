package org.jphototagger.repository.sqlite;

import org.jphototagger.domain.metadata.exif.Exif;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation for EXIF metadata database operations.
 */
public class SqliteExifDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteExifDatabase.class.getName());

    public SqliteExifDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    /**
     * Gets EXIF metadata for an image file.
     *
     * @param imageFile the image file
     * @return EXIF metadata, or null if not found
     */
    public Exif getExif(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }

        String sql = "SELECT"
                + " exif_recording_equipment.equipment" // -- 1 --
                + ", exif.exif_date_time_original" // -- 2 --
                + ", exif.exif_focal_length" // -- 3 --
                + ", exif.exif_iso_speed_ratings" // -- 4 --
                + ", exif_lenses.lens" // -- 5 --
                + ", exif.exif_date_time_original_timestamp" // -- 6 --
                + ", exif.exif_gps_latitude" // -- 7 --
                + ", exif.exif_gps_longitude" // -- 8 --
                + " FROM files INNER JOIN exif ON files.id = exif.id_file"
                + " LEFT JOIN exif_recording_equipment ON"
                + " exif.id_exif_recording_equipment = exif_recording_equipment.id"
                + " LEFT JOIN exif_lenses ON exif.id_exif_lens = exif_lenses.id"
                + " WHERE files.filename = ?";

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Exif exif = new Exif();
                    exif.setRecordingEquipment(rs.getString(1));
                    exif.setDateTimeOriginal(rs.getDate(2));
                    exif.setFocalLength(rs.getDouble(3));
                    exif.setIsoSpeedRatings(rs.getShort(4));
                    exif.setLens(rs.getString(5));
                    exif.setDateTimeOriginalTimestamp(rs.getLong(6));

                    // GPS coordinates - only set if both are not null
                    double gpsLatitude = rs.getDouble(7);
                    boolean hasGpsLatitude = !rs.wasNull();
                    double gpsLongitude = rs.getDouble(8);
                    boolean hasGpsLongitude = !rs.wasNull();
                    if (hasGpsLatitude && hasGpsLongitude) {
                        exif.setGpsLatitude(gpsLatitude);
                        exif.setGpsLongitude(gpsLongitude);
                    }

                    return exif;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting EXIF for " + imageFile, e);
        }
        return null;
    }

    /**
     * Inserts EXIF metadata for an image file.
     *
     * @param imageFile the image file
     * @param exif the EXIF metadata
     * @return true if inserted, false otherwise
     */
    public boolean insertExif(File imageFile, Exif exif) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        if (exif == null || exif.isEmpty()) {
            return false;
        }

        // Find the file ID
        long idFile = findIdImageFile(imageFile);
        if (idFile < 0) {
            LOGGER.log(Level.WARNING, "Image file not found: " + imageFile);
            return false;
        }

        String sql = "INSERT INTO exif"
                + " (id_file, id_exif_recording_equipment, exif_date_time_original,"
                + " exif_focal_length, exif_iso_speed_ratings, id_exif_lens,"
                + " exif_date_time_original_timestamp, exif_gps_latitude, exif_gps_longitude)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            setExifValues(stmt, idFile, exif);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting EXIF for " + imageFile, e);
            return false;
        }
    }

    /**
     * Updates EXIF metadata for an image file.
     *
     * @param imageFile the image file
     * @param exif the EXIF metadata
     * @return true if updated, false otherwise
     */
    public boolean updateExif(File imageFile, Exif exif) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        if (exif == null) {
            return false;
        }

        // Find the file ID
        long idFile = findIdImageFile(imageFile);
        if (idFile < 0) {
            LOGGER.log(Level.WARNING, "Image file not found: " + imageFile);
            return false;
        }

        String sql = "UPDATE exif SET"
                + " id_file = ?" // -- 1 --
                + ", id_exif_recording_equipment = ?" // -- 2 --
                + ", exif_date_time_original = ?" // -- 3 --
                + ", exif_focal_length = ?" // -- 4 --
                + ", exif_iso_speed_ratings = ?" // -- 5 --
                + ", id_exif_lens = ?" // -- 6 --
                + ", exif_date_time_original_timestamp = ?" // -- 7 --
                + ", exif_gps_latitude = ?" // -- 8 --
                + ", exif_gps_longitude = ?" // -- 9 --
                + " WHERE id_file = ?"; // -- 10 --

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            setExifValues(stmt, idFile, exif);
            stmt.setLong(10, idFile);
            int count = stmt.executeUpdate();
            return count > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating EXIF for " + imageFile, e);
            return false;
        }
    }

    /**
     * Deletes EXIF metadata for an image file.
     *
     * @param imageFile the image file
     * @return number of rows deleted (0 or 1)
     */
    public int deleteExif(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }

        // Find the file ID
        long idFile = findIdImageFile(imageFile);
        if (idFile < 0) {
            return 0;
        }

        String sql = "DELETE FROM exif WHERE id_file = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, idFile);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting EXIF for " + imageFile, e);
            return 0;
        }
    }

    /**
     * Checks if EXIF metadata exists for an image file.
     *
     * @param imageFile the image file
     * @return true if EXIF exists, false otherwise
     */
    public boolean existsExif(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }

        String sql = "SELECT COUNT(*) FROM exif"
                + " INNER JOIN files ON exif.id_file = files.id"
                + " WHERE files.filename = ?";

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking EXIF existence for " + imageFile, e);
            return false;
        }
    }

    /**
     * Sets EXIF values in a prepared statement.
     *
     * @param stmt the prepared statement
     * @param idFile the file ID
     * @param exif the EXIF metadata
     */
    private void setExifValues(PreparedStatement stmt, long idFile, Exif exif) throws SQLException {
        stmt.setLong(1, idFile);
        setLong(ensureValueExists("exif_recording_equipment", "equipment", exif.getRecordingEquipment()), stmt, 2);
        setDate(exif.getDateTimeOriginal(), stmt, 3);
        setDouble(exif.getFocalLengthGreaterZeroOrNull(), stmt, 4);
        setShort(exif.getIsoSpeedRatingsGreaterZeroOrNull(), stmt, 5);
        setLong(ensureValueExists("exif_lenses", "lens", exif.getLens()), stmt, 6);
        setLong(exif.getDateTimeOriginalTimestampGreaterZeroOrNull(), stmt, 7);

        boolean hasGpsCoordinates = exif.hasGpsCoordinates();
        setDouble(hasGpsCoordinates ? exif.getGpsLatitude() : null, stmt, 8);
        setDouble(hasGpsCoordinates ? exif.getGpsLongitude() : null, stmt, 9);
    }

    /**
     * Finds the ID of an image file.
     *
     * @param file the image file
     * @return the file ID, or -1 if not found
     */
    private long findIdImageFile(File file) {
        String sql = "SELECT id FROM files WHERE filename = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, file.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding file ID for " + file, e);
            return -1;
        }
    }
}
