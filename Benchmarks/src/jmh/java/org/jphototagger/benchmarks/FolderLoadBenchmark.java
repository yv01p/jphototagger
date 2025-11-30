package org.jphototagger.benchmarks;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for folder loading (browsing a directory of images).
 * Measures end-to-end thumbnail loading for Phase 6 comparison.
 *
 * Phase 6 should show significant improvement due to virtual threads.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 10)
@Measurement(iterations = 3, time = 10)
@Fork(1)
public class FolderLoadBenchmark {

    private static final int THUMBNAIL_SIZE = 150;

    @Param({"10", "50", "100"})
    private int fileCount;

    private List<File> testImageFiles;
    private ThumbnailCacheTestHarness cache;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        testImageFiles = TestImages.createTestDirectory(fileCount);
        cache = ThumbnailCacheTestHarness.createEmpty();
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        TestImages.cleanup();
        cache.close();
    }

    @Setup(Level.Iteration)
    public void clearCacheBeforeIteration() {
        cache.clear();
    }

    @Benchmark
    public void folderLoad_coldCache(Blackhole bh) throws Exception {
        // Simulate loading a folder with no cached thumbnails
        for (File file : testImageFiles) {
            BufferedImage original = ImageIO.read(file);
            if (original != null) {
                Image thumbnail = scaleThumbnail(original, THUMBNAIL_SIZE);
                cache.insertThumbnail(thumbnail, file);
                bh.consume(thumbnail);
            }
        }
    }

    @Benchmark
    public void folderLoad_warmCache(Blackhole bh) {
        // Pre-populate cache first
        try {
            for (File file : testImageFiles) {
                if (!cache.existsThumbnail(file)) {
                    BufferedImage original = ImageIO.read(file);
                    if (original != null) {
                        Image thumbnail = scaleThumbnail(original, THUMBNAIL_SIZE);
                        cache.insertThumbnail(thumbnail, file);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Now measure cache hits
        for (File file : testImageFiles) {
            Image thumbnail = cache.findThumbnail(file);
            bh.consume(thumbnail);
        }
    }

    @Benchmark
    public void folderLoad_virtualThreads(Blackhole bh) throws Exception {
        // Test parallel loading using virtual threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Image>> futures = new ArrayList<>();

            for (File file : testImageFiles) {
                futures.add(executor.submit(() -> {
                    BufferedImage original = ImageIO.read(file);
                    if (original != null) {
                        Image thumbnail = scaleThumbnail(original, THUMBNAIL_SIZE);
                        cache.insertThumbnail(thumbnail, file);
                        return thumbnail;
                    }
                    return null;
                }));
            }

            // Wait for all to complete
            for (Future<Image> future : futures) {
                bh.consume(future.get());
            }
        }
    }

    private static Image scaleThumbnail(BufferedImage original, int maxSize) {
        int width = original.getWidth();
        int height = original.getHeight();
        double scale = (double) maxSize / Math.max(width, height);

        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
        g2.dispose();

        return scaled;
    }
}
