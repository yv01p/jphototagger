package org.jphototagger.repository.sqlite;

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
 * Factory for SQLite database connections with WAL mode and foreign key support.
 */
public final class SqliteConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(SqliteConnectionFactory.class.getName());
    private final String url;
    private final List<Connection> connections = new ArrayList<>();
    private volatile boolean closed = false;

    public SqliteConnectionFactory(File databaseFile) {
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
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA synchronous=NORMAL");
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
