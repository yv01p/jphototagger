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
}
