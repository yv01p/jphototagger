package org.jphototagger.cachedb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for SQLite cache database operations.
 */
public abstract class CacheDatabase {

    private static final Logger LOGGER = Logger.getLogger(CacheDatabase.class.getName());
    private final CacheConnectionFactory connectionFactory;

    protected CacheDatabase(CacheConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    protected Connection getConnection() throws SQLException {
        return connectionFactory.getConnection();
    }

    protected void setBytes(byte[] value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, Types.BLOB);
        } else {
            stmt.setBytes(paramIndex, value);
        }
    }

    protected byte[] getBytes(ResultSet rs, int colIndex) throws SQLException {
        byte[] value = rs.getBytes(colIndex);
        return rs.wasNull() ? null : value;
    }

    protected void setString(String value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, Types.VARCHAR);
        } else {
            stmt.setString(paramIndex, value);
        }
    }

    protected String getString(ResultSet rs, int colIndex) throws SQLException {
        String value = rs.getString(colIndex);
        return rs.wasNull() ? null : value;
    }

    protected void setLong(Long value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, Types.BIGINT);
        } else {
            stmt.setLong(paramIndex, value);
        }
    }

    protected Long getLong(ResultSet rs, int colIndex) throws SQLException {
        long value = rs.getLong(colIndex);
        return rs.wasNull() ? null : value;
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

    public static void rollback(Connection con) {
        if (con == null) return;
        try {
            con.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error rolling back", e);
        }
    }
}
