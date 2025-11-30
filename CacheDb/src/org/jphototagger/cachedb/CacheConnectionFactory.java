package org.jphototagger.cachedb;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for SQLite cache database connections with WAL mode.
 * Separate from main database - cache can be deleted without data loss.
 */
public final class CacheConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(CacheConnectionFactory.class.getName());
    private final String url;
    private final List<Connection> connections = new ArrayList<>();
    private volatile boolean closed = false;

    public CacheConnectionFactory(File databaseFile) {
        this.url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
    }

    public synchronized Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Connection factory is closed");
        }
        Connection con = DriverManager.getConnection(url);
        configureConnection(con);
        connections.add(con);
        return con;
    }

    private void configureConnection(Connection con) throws SQLException {
        try (Statement stmt = con.createStatement()) {
            // Enable WAL mode for better concurrent read performance
            stmt.execute("PRAGMA journal_mode=WAL");
            // NORMAL synchronous - good balance of safety and performance
            stmt.execute("PRAGMA synchronous=NORMAL");
            // Enable foreign keys
            stmt.execute("PRAGMA foreign_keys=ON");
        }
    }

    public synchronized void close() {
        closed = true;
        for (Connection con : connections) {
            try {
                if (!con.isClosed()) {
                    con.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        connections.clear();
    }
}
