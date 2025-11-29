package org.jphototagger.exif.cache;

import java.io.File;
import org.jphototagger.cachedb.CacheConnectionFactory;
import org.jphototagger.domain.metadata.exif.ExifCacheProvider;
import org.jphototagger.exif.ExifIfd;
import org.jphototagger.exif.ExifTag;
import org.jphototagger.exif.ExifTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class SqliteExifCacheProviderImplTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private SqliteExifCache cache;
    private SqliteExifCacheProviderImpl provider;

    @BeforeEach
    void setUp() {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        cache = new SqliteExifCache(factory);
        provider = new SqliteExifCacheProviderImpl(cache);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void implementsExifCacheProvider() {
        assertThat(provider).isInstanceOf(ExifCacheProvider.class);
    }

    @Test
    void clear_returnsDeletedCount() throws Exception {
        File file = new File(tempDir, "test.jpg");
        file.createNewFile();

        ExifTags tags = new ExifTags();
        tags.setLastModified(file.lastModified());
        ExifTag tag = new ExifTag(271, 2, 5, 0, "Test".getBytes(), "Test", 18761, "Make", ExifIfd.EXIF);
        tags.addExifTag(tag);
        cache.cacheExifTags(file, tags);

        int cleared = provider.clear();

        assertThat(cleared).isEqualTo(1);
    }
}
