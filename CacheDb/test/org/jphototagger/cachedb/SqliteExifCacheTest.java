package org.jphototagger.cachedb;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.jphototagger.exif.ExifIfd;
import org.jphototagger.exif.ExifTag;
import org.jphototagger.exif.ExifTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class SqliteExifCacheTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private SqliteExifCache cache;
    private File imageFile;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        cache = new SqliteExifCache(factory);

        imageFile = new File(tempDir, "test.jpg");
        imageFile.createNewFile();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void constructor_createsTable() throws Exception {
        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name='exif_cache'")) {
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void containsUpToDateExifTags_returnsFalseForMissing() {
        assertThat(cache.containsUpToDateExifTags(imageFile)).isFalse();
    }

    @Test
    void cacheAndRetrieve_roundTrip() {
        ExifTags tags = createSampleExifTags();
        cache.cacheExifTags(imageFile, tags);

        ExifTags result = cache.getCachedExifTags(imageFile);

        assertThat(result).isNotNull();
        assertThat(result.getExifTags()).hasSize(1);
        ExifTag makeTag = result.findExifTagByTagId(271);
        assertThat(makeTag).isNotNull();
        assertThat(makeTag.getStringValue()).isEqualTo("TestCamera");
    }

    @Test
    void containsUpToDateExifTags_returnsTrueWhenCurrent() {
        cache.cacheExifTags(imageFile, createSampleExifTags());

        assertThat(cache.containsUpToDateExifTags(imageFile)).isTrue();
    }

    @Test
    void containsUpToDateExifTags_returnsFalseWhenStale() throws Exception {
        cache.cacheExifTags(imageFile, createSampleExifTags());
        long originalModified = imageFile.lastModified();

        // Simulate file modification
        Thread.sleep(100);
        imageFile.setLastModified(originalModified + 1000);

        assertThat(cache.containsUpToDateExifTags(imageFile)).isFalse();
    }

    @Test
    void deleteCachedExifTags_removesEntry() {
        cache.cacheExifTags(imageFile, createSampleExifTags());
        assertThat(cache.containsUpToDateExifTags(imageFile)).isTrue();

        cache.deleteCachedExifTags(imageFile);

        assertThat(cache.containsUpToDateExifTags(imageFile)).isFalse();
    }

    @Test
    void renameCachedExifTags_movesEntry() {
        cache.cacheExifTags(imageFile, createSampleExifTags());

        File newFile = new File(tempDir, "renamed.jpg");
        cache.renameCachedExifTags(imageFile, newFile);

        assertThat(cache.containsUpToDateExifTags(imageFile)).isFalse();
        assertThat(cache.getCachedExifTags(newFile)).isNotNull();
    }

    @Test
    void clear_removesAllEntries() throws Exception {
        File file1 = new File(tempDir, "test1.jpg");
        File file2 = new File(tempDir, "test2.jpg");
        file1.createNewFile();
        file2.createNewFile();

        cache.cacheExifTags(file1, createSampleExifTags());
        cache.cacheExifTags(file2, createSampleExifTags());

        int deleted = cache.clear();

        assertThat(deleted).isEqualTo(2);
        assertThat(cache.getCachedExifTags(file1)).isNull();
        assertThat(cache.getCachedExifTags(file2)).isNull();
    }

    private ExifTags createSampleExifTags() {
        ExifTags tags = new ExifTags();
        tags.setLastModified(imageFile.lastModified());

        ExifTag makeTag = new ExifTag(
            271,  // tagId for Make
            2,    // ASCII type
            11,   // valueCount
            0,    // valueOffset
            "TestCamera".getBytes(),
            "TestCamera",
            18761,  // little endian
            "Make",
            ExifIfd.EXIF
        );
        tags.addExifTag(makeTag);

        return tags;
    }
}
