package org.jphototagger.testsupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides isolated in-memory HSQLDB databases for testing.
 * Each test gets a fresh database instance.
 */
public final class TestDatabase implements AutoCloseable {

    private static final AtomicInteger instanceCounter = new AtomicInteger(0);

    private final String dbName;
    private Connection connection;

    private TestDatabase(String dbName) {
        this.dbName = dbName;
    }

    /**
     * Creates a new in-memory database for testing.
     * Each call returns a unique database instance.
     */
    public static TestDatabase createInMemory() {
        String uniqueName = "testdb_" + System.currentTimeMillis() + "_" + instanceCounter.incrementAndGet();
        return new TestDatabase(uniqueName);
    }

    /**
     * Opens a connection to the test database.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.hsqldb.jdbcDriver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("HSQLDB driver not found", e);
            }
            connection = DriverManager.getConnection(
                    "jdbc:hsqldb:mem:" + dbName + ";shutdown=true", "sa", "");
        }
        return connection;
    }

    /**
     * Executes SQL statements to set up the test database schema.
     */
    public void executeSql(String... sqlStatements) throws SQLException {
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            for (String sql : sqlStatements) {
                stmt.execute(sql);
            }
        }
    }

    /**
     * Shuts down and cleans up the test database.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.createStatement().execute("SHUTDOWN");
                connection.close();
            } catch (SQLException e) {
                // Ignore shutdown errors
            }
            connection = null;
        }
    }
}
