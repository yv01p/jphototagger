package org.jphototagger.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite database for the application's properties/settings storage.
 *
 * Consider it as a registry ("INI" file). The name is not e.g.
 * <code>DatabaseRegistry</code> because future releases could use it in a
 * different way too.
 */
public final class SqliteApplicationPropertiesDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteApplicationPropertiesDatabase.class.getName());
    private static final String VALUE_FALSE = "0";    // Never change that!
    private static final String VALUE_TRUE = "1";    // Never change that!

    public SqliteApplicationPropertiesDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    /**
     * Returns whether a key exists.
     *
     * @param  key key
     * @return     true if the key exists
     */
    public boolean existsKey(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT COUNT(*) FROM application WHERE key = ?")) {
            stmt.setString(1, key);
            LOGGER.log(Level.FINEST, stmt.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if key exists: " + key, e);
        }
        return false;
    }

    /**
     * Deletes a key and its value.
     *
     * @param key key to delete
     */
    public void deleteKey(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement("DELETE FROM application WHERE key = ?")) {
            con.setAutoCommit(true);
            stmt.setString(1, key);
            LOGGER.log(Level.FINER, stmt.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting key: " + key, e);
        }
    }

    /**
     * Returns whether a value is true.
     *
     * @param  key key
     * @return     true if the value is true or false if the value is false or
     *             the key does not exist. You can check for the existence of
     *             a key with {@code #existsKey(String)}.
     */
    public boolean getBoolean(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        String value = getString(key);
        return value != null && value.equals(VALUE_TRUE);
    }

    /**
     * Inserts a boolean value or updates it if the key exists.
     *
     * @param key   key
     * @param value value to set
     */
    public void setBoolean(String key, boolean value) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        setString(key, value ? VALUE_TRUE : VALUE_FALSE);
    }

    private String getInsertOrUpdateStmt(String key) {
        if (existsKey(key)) {
            return "UPDATE application SET value = ? WHERE key = ?";
        } else {
            return "INSERT INTO application (value, key) VALUES (?, ?)";
        }
    }

    /**
     * Sets a string value for the given key.
     * Inserts a new entry if the key doesn't exist, updates if it does.
     *
     * @param key    key
     * @param value  value to set
     */
    public void setString(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        if (value == null) {
            throw new NullPointerException("value == null");
        }
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(getInsertOrUpdateStmt(key))) {
            con.setAutoCommit(true);
            stmt.setBytes(1, value.getBytes());
            stmt.setString(2, key);
            LOGGER.log(Level.FINER, stmt.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error setting string for key: " + key, e);
        }
    }

    /**
     * Returns a string value for the given key.
     *
     * @param  key key
     * @return     string or null if there is no such key in the database,
     *             the inserted string was null or on database errors
     */
    public String getString(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT value FROM application WHERE key = ?")) {
            stmt.setString(1, key);
            LOGGER.log(Level.FINEST, stmt.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes(1);
                    if (rs.wasNull() || bytes == null) {
                        return null;
                    }
                    return new String(bytes);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting string for key: " + key, e);
        }
        return null;
    }

    /**
     * Returns all keys stored in the application properties.
     *
     * @return list of all keys, empty if none exist
     */
    public List<String> getAllKeys() {
        List<String> keys = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT key FROM application ORDER BY key")) {
            LOGGER.log(Level.FINEST, stmt.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    keys.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all keys", e);
        }
        return keys;
    }

    /**
     * Returns the count of stored properties.
     *
     * @return number of key-value pairs stored
     */
    public int getPropertiesCount() {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT COUNT(*) FROM application")) {
            LOGGER.log(Level.FINEST, stmt.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting properties count", e);
        }
        return 0;
    }
}
