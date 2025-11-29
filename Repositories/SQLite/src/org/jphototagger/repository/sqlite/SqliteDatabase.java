package org.jphototagger.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for SQLite database operations.
 * Mirrors the HSQLDB Database class structure for consistency.
 */
public abstract class SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteDatabase.class.getName());
    private final SqliteConnectionFactory connectionFactory;

    protected SqliteDatabase(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    protected Connection getConnection() throws SQLException {
        return connectionFactory.getConnection();
    }

    public static void close(Statement stmt) {
        if (stmt == null) return;
        try {
            stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing statement", e);
        }
    }

    public static void close(ResultSet rs, Statement stmt) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing resources", e);
        }
    }

    public static void close(ResultSet rs, PreparedStatement stmt) {
        close(rs, (Statement) stmt);
    }

    public static void rollback(Connection con) {
        if (con == null) return;
        try {
            con.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error rolling back", e);
        }
    }

    protected Long getLong(ResultSet rs, int colIndex) throws SQLException {
        long value = rs.getLong(colIndex);
        return rs.wasNull() ? null : value;
    }

    protected String getString(ResultSet rs, int colIndex) throws SQLException {
        String value = rs.getString(colIndex);
        return rs.wasNull() ? null : value;
    }

    protected void setLong(Long value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(paramIndex, value);
        }
    }

    protected void setString(Object value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, java.sql.Types.VARCHAR);
        } else {
            stmt.setString(paramIndex, value.toString());
        }
    }

    protected void setDouble(Double value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(paramIndex, value);
        }
    }

    protected void setShort(Short value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, java.sql.Types.SMALLINT);
        } else {
            stmt.setShort(paramIndex, value);
        }
    }

    protected void setDate(java.sql.Date value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, java.sql.Types.DATE);
        } else {
            stmt.setDate(paramIndex, value);
        }
    }

    /**
     * Ensures a value exists in a lookup table and returns its ID.
     * If the value doesn't exist, it's inserted and the new ID is returned.
     *
     * @param tableName the name of the lookup table
     * @param columnName the name of the value column
     * @param value the value to ensure exists
     * @return the ID of the value, or null if value is null
     */
    protected Long ensureValueExists(String tableName, String columnName, String value) throws SQLException {
        if (tableName == null) {
            throw new NullPointerException("tableName == null");
        }
        if (columnName == null) {
            throw new NullPointerException("columnName == null");
        }
        if (value == null) {
            return null;
        }

        // First, try to get the existing ID
        Long id = getId(tableName, columnName, value);
        if (id == null) {
            // Insert the value and get the new ID
            String sql = "INSERT INTO " + tableName + " (" + columnName + ") VALUES (?)";
            try (Connection con = getConnection();
                 PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, value);
                stmt.executeUpdate();
                id = getId(tableName, columnName, value);
            }
        }
        return id;
    }

    /**
     * Gets the ID of a value from a lookup table.
     *
     * @param tableName the name of the lookup table
     * @param columnName the name of the value column
     * @param value the value to find
     * @return the ID of the value, or null if not found
     */
    protected Long getId(String tableName, String columnName, String value) throws SQLException {
        if (tableName == null) {
            throw new NullPointerException("tableName == null");
        }
        if (columnName == null) {
            throw new NullPointerException("columnName == null");
        }
        if (value == null) {
            return null;
        }

        String sql = "SELECT id FROM " + tableName + " WHERE " + columnName + " = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, value);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }
}
