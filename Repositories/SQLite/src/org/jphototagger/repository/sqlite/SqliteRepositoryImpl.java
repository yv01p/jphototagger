package org.jphototagger.repository.sqlite;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jphototagger.api.file.FilenameTokens;
import org.jphototagger.domain.repository.FileRepositoryProvider;
import org.jphototagger.domain.repository.Repository;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 * SQLite implementation of the Repository interface.
 * Provides the main entry point for the SQLite database backend.
 * Uses a lower position than HSQLDB to remain as secondary option during migration.
 */
@ServiceProvider(service = Repository.class, position = 200)
public final class SqliteRepositoryImpl implements Repository {

    private static final Logger LOGGER = Logger.getLogger(SqliteRepositoryImpl.class.getName());
    private static SqliteConnectionFactory connectionFactory;
    private volatile boolean initialized = false;

    @Override
    public synchronized void init() {
        if (initialized) {
            return;
        }

        try {
            File dbFile = getDatabaseFile();
            connectionFactory = new SqliteConnectionFactory(dbFile);

            // Create tables if they don't exist
            SqliteTables tables = new SqliteTables(connectionFactory);
            tables.createTables();

            initialized = true;
            LOGGER.info("SQLite repository initialized successfully at: " + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SQLite repository", e);
            throw new RuntimeException("Failed to initialize SQLite repository", e);
        }
    }

    @Override
    public boolean isInit() {
        return initialized;
    }

    @Override
    public synchronized void shutdown() {
        if (connectionFactory != null) {
            connectionFactory.close();
            initialized = false;
            LOGGER.info("SQLite repository shut down successfully");
        }
    }

    private File getDatabaseFile() {
        FileRepositoryProvider provider = Lookup.getDefault().lookup(FileRepositoryProvider.class);
        String fileName = provider.getFileRepositoryFileName(FilenameTokens.FULL_PATH);
        // Change extension from .script (HSQLDB) to .db (SQLite)
        String sqliteFileName = fileName.replace(".script", ".sqlite.db");
        return new File(sqliteFileName);
    }

    /**
     * Package-private access to connection factory for repository implementations.
     */
    static SqliteConnectionFactory getConnectionFactory() {
        if (connectionFactory == null) {
            throw new IllegalStateException("SQLite repository not initialized");
        }
        return connectionFactory;
    }
}
