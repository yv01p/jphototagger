package org.jphototagger.cachedb;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initializes the SQLite cache database.
 * Creates a single cache.db file containing thumbnail cache.
 */
public final class CacheDbInit {

    private static final Logger LOGGER = Logger.getLogger(CacheDbInit.class.getName());
    private static final String CACHE_DB_FILENAME = "cache.db";

    private final File cacheDbFile;
    private final CacheConnectionFactory connectionFactory;
    private final SqliteThumbnailCache thumbnailCache;

    private CacheDbInit(File cacheDirectory) {
        this.cacheDbFile = new File(cacheDirectory, CACHE_DB_FILENAME);
        LOGGER.log(Level.INFO, "Initializing SQLite cache database: {0}", cacheDbFile);

        this.connectionFactory = new CacheConnectionFactory(cacheDbFile);
        this.thumbnailCache = new SqliteThumbnailCache(connectionFactory);
    }

    /**
     * Creates cache database in the specified directory.
     *
     * @param cacheDirectory directory to store cache.db
     * @return initialized cache database
     */
    public static CacheDbInit createForDirectory(File cacheDirectory) {
        if (cacheDirectory == null) {
            throw new NullPointerException("cacheDirectory == null");
        }
        if (!cacheDirectory.isDirectory()) {
            if (!cacheDirectory.mkdirs()) {
                throw new RuntimeException("Failed to create cache directory: " + cacheDirectory);
            }
        }
        return new CacheDbInit(cacheDirectory);
    }

    public SqliteThumbnailCache getThumbnailCache() {
        return thumbnailCache;
    }

    public File getCacheDbFile() {
        return cacheDbFile;
    }

    public void close() {
        connectionFactory.close();
    }
}
