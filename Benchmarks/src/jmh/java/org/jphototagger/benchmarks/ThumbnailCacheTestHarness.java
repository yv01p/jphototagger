package org.jphototagger.benchmarks;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;

/**
 * Test harness for thumbnail cache benchmarking.
 * Simulates ThumbnailsDb behavior without requiring MapDB initialization.
 */
public final class ThumbnailCacheTestHarness {

    private final Map<String, ThumbnailEntry> cache = new HashMap<>();
    private File[] storedFiles;

    private static class ThumbnailEntry {
        final byte[] imageBytes;
        final long fileLength;
        final long lastModified;

        ThumbnailEntry(byte[] imageBytes, long fileLength, long lastModified) {
            this.imageBytes = imageBytes;
            this.fileLength = fileLength;
            this.lastModified = lastModified;
        }
    }

    private ThumbnailCacheTestHarness() {
    }

    /**
     * Creates an empty cache for testing inserts.
     */
    public static ThumbnailCacheTestHarness createEmpty() {
        return new ThumbnailCacheTestHarness();
    }

    /**
     * Creates a cache pre-populated with sample thumbnails.
     */
    public static ThumbnailCacheTestHarness createWithSampleData(int count) {
        ThumbnailCacheTestHarness harness = new ThumbnailCacheTestHarness();
        harness.storedFiles = new File[count];

        // Generate fake thumbnails (small colored rectangles)
        for (int i = 0; i < count; i++) {
            File file = new File("/photos/image_" + i + ".jpg");
            harness.storedFiles[i] = file;

            BufferedImage thumbnail = createSampleThumbnail(i);
            byte[] bytes = imageToBytes(thumbnail);

            harness.cache.put(file.getAbsolutePath(),
                    new ThumbnailEntry(bytes, 1024 * (i + 1), System.currentTimeMillis()));
        }

        return harness;
    }

    private static BufferedImage createSampleThumbnail(int seed) {
        BufferedImage img = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        // Different color per thumbnail
        g.setColor(new java.awt.Color(seed % 256, (seed * 7) % 256, (seed * 13) % 256));
        g.fillRect(0, 0, 150, 150);
        g.dispose();
        return img;
    }

    private static byte[] imageToBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpeg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File[] getStoredFiles() {
        return storedFiles;
    }

    public boolean existsThumbnail(File imageFile) {
        return cache.containsKey(imageFile.getAbsolutePath());
    }

    public Image findThumbnail(File imageFile) {
        ThumbnailEntry entry = cache.get(imageFile.getAbsolutePath());
        if (entry == null) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(entry.imageBytes));
        } catch (IOException e) {
            return null;
        }
    }

    public boolean hasUpToDateThumbnail(File imageFile) {
        ThumbnailEntry entry = cache.get(imageFile.getAbsolutePath());
        if (entry == null) {
            return false;
        }
        // In real code, compares with file.length() and file.lastModified()
        // Here we just check existence
        return true;
    }

    public void insertThumbnail(Image thumbnail, File imageFile) {
        BufferedImage buffered;
        if (thumbnail instanceof BufferedImage) {
            buffered = (BufferedImage) thumbnail;
        } else {
            buffered = new BufferedImage(
                    thumbnail.getWidth(null),
                    thumbnail.getHeight(null),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = buffered.createGraphics();
            g.drawImage(thumbnail, 0, 0, null);
            g.dispose();
        }

        byte[] bytes = imageToBytes(buffered);
        cache.put(imageFile.getAbsolutePath(),
                new ThumbnailEntry(bytes, imageFile.length(), imageFile.lastModified()));
    }

    public void clear() {
        cache.clear();
    }

    public void close() {
        cache.clear();
    }
}
