package org.jphototagger.benchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.jphototagger.cachedb.CacheConnectionFactory;
import org.jphototagger.exif.cache.SqliteExifCache;
import org.jphototagger.exif.ExifIfd;
import org.jphototagger.exif.ExifTag;
import org.jphototagger.exif.ExifTags;

/**
 * Test harness for EXIF cache benchmarking using SQLite backend.
 */
public final class ExifCacheTestHarness {

    private final File tempDir;
    private final CacheConnectionFactory factory;
    private final SqliteExifCache cache;

    private ExifCacheTestHarness(File tempDir) {
        this.tempDir = tempDir;
        File dbFile = new File(tempDir, "benchmark-exif-cache.db");
        this.factory = new CacheConnectionFactory(dbFile);
        this.cache = new SqliteExifCache(factory);
    }

    public static ExifCacheTestHarness create() {
        try {
            File tempDir = Files.createTempDirectory("exif-benchmark").toFile();
            return new ExifCacheTestHarness(tempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ExifTags[] generateSampleTags(int count) {
        ExifTags[] tags = new ExifTags[count];
        for (int i = 0; i < count; i++) {
            ExifTags exifTags = new ExifTags();
            exifTags.setLastModified(System.currentTimeMillis() - i * 1000);

            String makeValue = "Camera" + (i % 5);
            ExifTag makeTag = new ExifTag(
                271, 2, makeValue.length() + 1, 0,
                makeValue.getBytes(), makeValue, 18761, "Make", ExifIfd.EXIF
            );
            exifTags.addExifTag(makeTag);

            String modelValue = "Model" + i;
            ExifTag modelTag = new ExifTag(
                272, 2, modelValue.length() + 1, 0,
                modelValue.getBytes(), modelValue, 18761, "Model", ExifIfd.EXIF
            );
            exifTags.addExifTag(modelTag);

            tags[i] = exifTags;
        }
        return tags;
    }

    public void cacheExifTags(File imageFile, ExifTags exifTags) {
        cache.cacheExifTags(imageFile, exifTags);
    }

    public ExifTags getCachedExifTags(File imageFile) {
        return cache.getCachedExifTags(imageFile);
    }

    public boolean containsUpToDateExifTags(File imageFile) {
        return cache.containsUpToDateExifTags(imageFile);
    }

    public void clear() {
        cache.clear();
    }

    public void close() {
        factory.close();
        deleteRecursively(tempDir);
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
