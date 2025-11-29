package org.jphototagger.tools.migration;

import java.io.File;

/**
 * Command-line tool for migrating HSQLDB database to SQLite.
 *
 * Usage: java -jar MigrationTool.jar <hsqldb-file> <sqlite-file>
 *
 * Example:
 *   java -jar MigrationTool.jar /path/to/jphototagger /path/to/jphototagger.db
 */
public class MigrationMain {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: MigrationMain <hsqldb-file> <sqlite-file>");
            System.err.println();
            System.err.println("Arguments:");
            System.err.println("  hsqldb-file  Path to HSQLDB database file (without .script/.data extension)");
            System.err.println("  sqlite-file  Path to output SQLite database file");
            System.err.println();
            System.err.println("Example:");
            System.err.println("  MigrationMain /home/user/.jphototagger/database/jphototagger /home/user/.jphototagger/database/jphototagger.db");
            System.exit(1);
        }

        File hsqldbFile = new File(args[0]);
        File sqliteFile = new File(args[1]);

        // Validate input
        if (!hsqldbFile.getParentFile().exists()) {
            System.err.println("Error: HSQLDB directory does not exist: " + hsqldbFile.getParentFile());
            System.exit(1);
        }

        // Check for HSQLDB files
        File scriptFile = new File(hsqldbFile.getAbsolutePath() + ".script");
        File dataFile = new File(hsqldbFile.getAbsolutePath() + ".data");
        if (!scriptFile.exists() && !dataFile.exists()) {
            System.err.println("Error: HSQLDB database files not found at: " + hsqldbFile.getAbsolutePath());
            System.err.println("Expected files: " + scriptFile.getName() + " or " + dataFile.getName());
            System.exit(1);
        }

        // Ensure output directory exists
        File sqliteDir = sqliteFile.getParentFile();
        if (sqliteDir != null && !sqliteDir.exists()) {
            if (!sqliteDir.mkdirs()) {
                System.err.println("Error: Could not create output directory: " + sqliteDir);
                System.exit(1);
            }
        }

        // Check if output file already exists
        if (sqliteFile.exists()) {
            System.err.println("Warning: Output file already exists and will be overwritten: " + sqliteFile);
        }

        System.out.println("JPhotoTagger Database Migration Tool");
        System.out.println("=====================================");
        System.out.println("Source HSQLDB: " + hsqldbFile.getAbsolutePath());
        System.out.println("Target SQLite: " + sqliteFile.getAbsolutePath());
        System.out.println();

        // Create migrator with progress listener
        HsqldbToSqliteMigrator migrator = new HsqldbToSqliteMigrator(hsqldbFile, sqliteFile);

        HsqldbToSqliteMigrator.MigrationListener listener = new HsqldbToSqliteMigrator.MigrationListener() {
            private String currentTable = null;

            @Override
            public void onProgress(String tableName, int current, int total) {
                if (!tableName.equals(currentTable)) {
                    System.out.println("Migrating table: " + tableName);
                    currentTable = tableName;
                }
                if (total > 0 && current % 1000 == 0) {
                    int percentage = (current * 100) / total;
                    System.out.printf("  Progress: %d/%d rows (%d%%)\r", current, total, percentage);
                }
            }

            @Override
            public void onTableComplete(String tableName, int rowCount) {
                System.out.printf("  Completed: %s (%d rows)\n", tableName, rowCount);
            }
        };

        // Execute migration
        System.out.println("Starting migration...");
        HsqldbToSqliteMigrator.MigrationResult result = migrator.migrate(listener);

        System.out.println();
        if (result.success()) {
            System.out.println("Migration completed successfully!");
            System.out.println("  Tables migrated: " + result.tablesProcessed());
            System.out.println("  Total rows: " + result.rowsMigrated());
            System.out.println();
            System.out.println("SQLite database created at: " + sqliteFile.getAbsolutePath());
            System.exit(0);
        } else {
            System.err.println("Migration failed!");
            System.err.println("  Error: " + result.errorMessage());
            System.err.println("  Tables processed: " + result.tablesProcessed());
            System.err.println("  Rows migrated: " + result.rowsMigrated());
            System.exit(1);
        }
    }
}
