package org.jphototagger.exif.cache;

import org.jphototagger.domain.metadata.exif.ExifCacheProvider;

/**
 * SQLite-backed implementation of ExifCacheProvider.
 */
public final class SqliteExifCacheProviderImpl implements ExifCacheProvider {

    private final SqliteExifCache cache;

    public SqliteExifCacheProviderImpl(SqliteExifCache cache) {
        this.cache = cache;
    }

    @Override
    public void init() {
        // SQLite cache is initialized in constructor
    }

    @Override
    public int clear() {
        return cache.clear();
    }

    public SqliteExifCache getCache() {
        return cache;
    }
}
