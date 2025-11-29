package org.jphototagger.tools.migration;

import org.jphototagger.repository.sqlite.SqliteConnectionFactory;
import org.jphototagger.repository.sqlite.SqliteTables;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Migrates data from HSQLDB to SQLite database.
 */
public class HsqldbToSqliteMigrator {

    private static final Logger LOGGER = Logger.getLogger(HsqldbToSqliteMigrator.class.getName());
    private static final int BATCH_SIZE = 100;

    private final File hsqldbFile;
    private final File sqliteFile;

    // Tables in dependency order (respecting foreign keys)
    private static final String[] TABLE_ORDER = {
        // No FK dependencies
        "application",
        "files",
        // 1:N lookup tables
        "dc_creators",
        "dc_rights",
        "iptc4xmpcore_locations",
        "photoshop_authorspositions",
        "photoshop_captionwriters",
        "photoshop_cities",
        "photoshop_countries",
        "photoshop_credits",
        "photoshop_sources",
        "photoshop_states",
        "dc_subjects",
        "exif_recording_equipment",
        "exif_lenses",
        // References files
        "xmp",
        "exif",
        // Junction table
        "xmp_dc_subject",
        // Collections
        "collection_names",
        "collections",
        // Saved searches
        "saved_searches",
        "saved_searches_panels",
        "saved_searches_keywords",
        // Programs
        "programs",
        "actions_after_db_insertion",
        "default_programs",
        // Other tables
        "autoscan_directories",
        "metadata_edit_templates",
        "favorite_directories",
        "file_exclude_patterns",
        "hierarchical_subjects",
        "synonyms",
        "rename_templates",
        "user_defined_file_filters",
        "user_defined_file_types",
        "wordsets",
        "wordsets_words"
    };

    public HsqldbToSqliteMigrator(File hsqldbFile, File sqliteFile) {
        this.hsqldbFile = hsqldbFile;
        this.sqliteFile = sqliteFile;
    }

    public MigrationResult migrate(MigrationListener listener) {
        int tablesProcessed = 0;
        int rowsMigrated = 0;
        Connection sourceConn = null;
        SqliteConnectionFactory targetFactory = null;

        try {
            // Connect to source HSQLDB
            String hsqlUrl = "jdbc:hsqldb:file:" + hsqldbFile.getAbsolutePath() + ";shutdown=true";
            sourceConn = DriverManager.getConnection(hsqlUrl, "sa", "");
            LOGGER.info("Connected to source HSQLDB: " + hsqldbFile.getAbsolutePath());

            // Create target SQLite database with schema
            targetFactory = new SqliteConnectionFactory(sqliteFile);
            SqliteTables tables = new SqliteTables(targetFactory);
            tables.createTables();
            LOGGER.info("Created SQLite database schema: " + sqliteFile.getAbsolutePath());

            // Get list of tables that exist in source
            List<String> existingTables = getExistingTables(sourceConn);
            LOGGER.info("Found " + existingTables.size() + " tables in source database");

            // Copy each table in order
            for (String tableName : TABLE_ORDER) {
                if (!existingTables.contains(tableName.toLowerCase())) {
                    LOGGER.fine("Skipping table " + tableName + " (not found in source)");
                    continue;
                }

                int rowCount = copyTable(sourceConn, targetFactory, tableName, listener);
                tablesProcessed++;
                rowsMigrated += rowCount;

                if (listener != null) {
                    listener.onTableComplete(tableName, rowCount);
                }

                LOGGER.info("Migrated table " + tableName + ": " + rowCount + " rows");
            }

            LOGGER.info("Migration completed successfully: " + tablesProcessed + " tables, " + rowsMigrated + " rows");
            return new MigrationResult(true, tablesProcessed, rowsMigrated, null);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Migration failed", e);
            return new MigrationResult(false, tablesProcessed, rowsMigrated, e.getMessage());
        } finally {
            if (sourceConn != null) {
                try {
                    sourceConn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error closing source connection", e);
                }
            }
            if (targetFactory != null) {
                targetFactory.close();
            }
        }
    }

    private List<String> getExistingTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (!tableName.startsWith("SYSTEM_")) {
                    tables.add(tableName.toLowerCase());
                }
            }
        }
        return tables;
    }

    private int copyTable(Connection source, SqliteConnectionFactory targetFactory,
                         String tableName, MigrationListener listener) throws SQLException {
        int rowCount = 0;

        // Get table metadata
        List<ColumnInfo> columns = getTableColumns(source, tableName);
        if (columns.isEmpty()) {
            LOGGER.warning("No columns found for table " + tableName);
            return 0;
        }

        // Read all rows from source
        String selectSql = "SELECT * FROM " + tableName;
        try (Statement sourceStmt = source.createStatement();
             ResultSet rs = sourceStmt.executeQuery(selectSql)) {

            // Get total count for progress reporting
            int totalRows = getTotalRowCount(source, tableName);

            // Prepare insert statement for target
            String insertSql = buildInsertStatement(tableName, columns);

            try (Connection target = targetFactory.getConnection();
                 PreparedStatement insertStmt = target.prepareStatement(insertSql)) {

                target.setAutoCommit(false);

                // Copy rows in batches
                while (rs.next()) {
                    rowCount++;

                    // Set all column values
                    for (int i = 0; i < columns.size(); i++) {
                        ColumnInfo col = columns.get(i);
                        Object value = rs.getObject(i + 1);

                        if (value == null) {
                            insertStmt.setNull(i + 1, col.sqlType);
                        } else {
                            insertStmt.setObject(i + 1, value, col.sqlType);
                        }
                    }

                    insertStmt.addBatch();

                    // Execute batch periodically
                    if (rowCount % BATCH_SIZE == 0) {
                        insertStmt.executeBatch();
                        target.commit();

                        if (listener != null) {
                            listener.onProgress(tableName, rowCount, totalRows);
                        }
                    }
                }

                // Execute remaining batch
                if (rowCount % BATCH_SIZE != 0) {
                    insertStmt.executeBatch();
                }

                target.commit();
            }
        }

        return rowCount;
    }

    private List<ColumnInfo> getTableColumns(Connection conn, String tableName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();

        try (ResultSet rs = meta.getColumns(null, null, tableName.toUpperCase(), "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                int sqlType = rs.getInt("DATA_TYPE");
                columns.add(new ColumnInfo(columnName, sqlType));
            }
        }

        return columns;
    }

    private String buildInsertStatement(String tableName, List<ColumnInfo> columns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName);
        sql.append(" (");

        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(columns.get(i).name);
        }

        sql.append(") VALUES (");

        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }

        sql.append(")");
        return sql.toString();
    }

    private int getTotalRowCount(Connection conn, String tableName) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not get row count for " + tableName, e);
        }
        return 0;
    }

    private static class ColumnInfo {
        final String name;
        final int sqlType;

        ColumnInfo(String name, int sqlType) {
            this.name = name;
            this.sqlType = sqlType;
        }
    }

    public interface MigrationListener {
        void onProgress(String tableName, int current, int total);
        void onTableComplete(String tableName, int rowCount);
    }

    public record MigrationResult(boolean success, int tablesProcessed, int rowsMigrated, String errorMessage) {}
}
