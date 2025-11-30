package org.jphototagger.benchmarks;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.jphototagger.cachedb.CacheConnectionFactory;
import org.jphototagger.cachedb.SqliteThumbnailCache;

/**
 * Test harness for thumbnail cache benchmarking using SQLite backend.
 */
public final class ThumbnailCacheTestHarness {

    private final File tempDir;
    private final CacheConnectionFactory factory;
    private final SqliteThumbnailCache cache;
    private File[] storedFiles;

    private ThumbnailCacheTestHarness(File tempDir) {
        this.tempDir = tempDir;
        File dbFile = new File(tempDir, "benchmark-cache.db");
        this.factory = new CacheConnectionFactory(dbFile);
        this.cache = new SqliteThumbnailCache(factory);
    }

    public static ThumbnailCacheTestHarness createEmpty() {
        try {
            File tempDir = Files.createTempDirectory("thumbnail-benchmark").toFile();
            return new ThumbnailCacheTestHarness(tempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ThumbnailCacheTestHarness createWithSampleData(int count) {
        ThumbnailCacheTestHarness harness = createEmpty();
        harness.storedFiles = new File[count];

        for (int i = 0; i < count; i++) {
            File file = new File("/photos/image_" + i + ".jpg");
            harness.storedFiles[i] = file;

            BufferedImage thumbnail = createSampleThumbnail(i);
            harness.cache.insertThumbnail(thumbnail, file);
        }

        return harness;
    }

    private static BufferedImage createSampleThumbnail(int seed) {
        BufferedImage img = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(seed % 256, (seed * 7) % 256, (seed * 13) % 256));
        g.fillRect(0, 0, 150, 150);
        g.dispose();
        return img;
    }

    public File[] getStoredFiles() {
        return storedFiles;
    }

    public boolean existsThumbnail(File imageFile) {
        return cache.existsThumbnail(imageFile);
    }

    public Image findThumbnail(File imageFile) {
        return cache.findThumbnail(imageFile);
    }

    public boolean hasUpToDateThumbnail(File imageFile) {
        return cache.hasUpToDateThumbnail(imageFile);
    }

    public void insertThumbnail(Image thumbnail, File imageFile) {
        cache.insertThumbnail(thumbnail, imageFile);
    }

    public void clear() {
        // No-op for SQLite - drop table would be too expensive per benchmark
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
