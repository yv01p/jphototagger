package org.jphototagger.cachedb;

import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class CacheDbInitTest {

    @TempDir
    File tempDir;

    private CacheDbInit cacheDbInit;

    @BeforeEach
    void setUp() {
        cacheDbInit = CacheDbInit.createForDirectory(tempDir);
    }

    @AfterEach
    void tearDown() {
        cacheDbInit.close();
    }

    @Test
    void createForDirectory_createsCacheDbFile() {
        File expectedDbFile = new File(tempDir, "cache.db");
        assertThat(expectedDbFile).exists();
    }

    @Test
    void getThumbnailCache_returnsCache() {
        SqliteThumbnailCache cache = cacheDbInit.getThumbnailCache();
        assertThat(cache).isNotNull();
    }

    @Test
    void getCacheDbFile_returnsCorrectFile() {
        File cacheFile = cacheDbInit.getCacheDbFile();
        assertThat(cacheFile.getName()).isEqualTo("cache.db");
        assertThat(cacheFile.getParentFile()).isEqualTo(tempDir);
    }
}
