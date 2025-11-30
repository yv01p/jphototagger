package org.jphototagger.repository.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates performance-optimized indexes for SQLite database.
 * These indexes improve query performance for common operations:
 * - Filtering by rating
 * - Sorting by date
 * - Keyword lookups
 */
public final class SqliteIndexes {

    private static final Logger LOGGER = Logger.getLogger(SqliteIndexes.class.getName());
    private final SqliteConnectionFactory connectionFactory;

    public SqliteIndexes(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Creates additional indexes for performance optimization.
     * These complement the basic indexes created by SqliteTables.
     */
    public void createPerformanceIndexes() throws SQLException {
        try (Connection conn = connectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(false);

            // Rating filter index - speeds up "show 4+ star images"
            createIndexIfNotExists(stmt, "idx_xmp_rating", "xmp", "rating");

            // Composite index for date range + rating queries
            createCompositeIndexIfNotExists(stmt, "idx_xmp_date_rating",
                "xmp", "iptc4xmpcore_datecreated", "rating");

            // Covering index for keyword existence checks
            createIndexIfNotExists(stmt, "idx_dc_subjects_id_subject",
                "dc_subjects", "id, subject");

            // Index for hierarchical keyword parent lookups
            createIndexIfNotExists(stmt, "idx_hierarchical_subjects_id_parent",
                "hierarchical_subjects", "id_parent");

            conn.commit();
            LOGGER.info("Performance indexes created successfully");
        }
    }

    private void createIndexIfNotExists(Statement stmt, String indexName,
            String tableName, String columns) throws SQLException {
        String sql = String.format(
            "CREATE INDEX IF NOT EXISTS %s ON %s (%s)",
            indexName, tableName, columns);
        stmt.execute(sql);
        LOGGER.log(Level.FINE, "Created index: {0}", indexName);
    }

    private void createCompositeIndexIfNotExists(Statement stmt, String indexName,
            String tableName, String... columns) throws SQLException {
        String columnList = String.join(", ", columns);
        createIndexIfNotExists(stmt, indexName, tableName, columnList);
    }
}
