package org.jphototagger.benchmarks;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for thumbnail generation (cache miss scenario).
 * Measures image scaling performance for Phase 6 comparison.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class ThumbnailGenerationBenchmark {

    private static final int THUMBNAIL_SIZE = 150;

    private File testImageFile;
    private ThumbnailCacheTestHarness cache;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        testImageFile = TestImages.loadSampleImage(0);
        cache = ThumbnailCacheTestHarness.createEmpty();
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        TestImages.cleanup();
        cache.close();
    }

    @TearDown(Level.Iteration)
    public void clearCache() {
        cache.clear();
    }

    @Benchmark
    public void generateThumbnail(Blackhole bh) throws Exception {
        BufferedImage original = ImageIO.read(testImageFile);
        Image thumbnail = scaleThumbnail(original, THUMBNAIL_SIZE);
        bh.consume(thumbnail);
    }

    @Benchmark
    public void generateAndStore(Blackhole bh) throws Exception {
        BufferedImage original = ImageIO.read(testImageFile);
        Image thumbnail = scaleThumbnail(original, THUMBNAIL_SIZE);
        cache.insertThumbnail(thumbnail, testImageFile);
        bh.consume(thumbnail);
    }

    /**
     * Simplified thumbnail scaling (mirrors ThumbnailUtil logic).
     */
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
