package org.jphototagger.benchmarks;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.jphototagger.exif.ExifTags;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for EXIF cache operations.
 * Measures XML serialization overhead for Phase 5/6 comparison.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ExifCacheBenchmark {

    private File[] testFiles;
    private ExifTags[] sampleExifTags;
    private ExifCacheTestHarness cache;

    @Setup(Level.Trial)
    public void setup() {
        cache = ExifCacheTestHarness.create();
        testFiles = TestImages.generateFilePaths(1000);
        sampleExifTags = ExifCacheTestHarness.generateSampleTags(1000);

        // Pre-populate cache
        for (int i = 0; i < testFiles.length; i++) {
            cache.cacheExifTags(testFiles[i], sampleExifTags[i]);
        }
    }

    @TearDown(Level.Trial)
    public void teardown() {
        cache.close();
    }

    @Benchmark
    public void exifCache_read(Blackhole bh) {
        File file = testFiles[ThreadLocalRandom.current().nextInt(testFiles.length)];
        bh.consume(cache.getCachedExifTags(file));
    }

    @Benchmark
    public void exifCache_write(Blackhole bh) {
        int i = ThreadLocalRandom.current().nextInt(testFiles.length);
        cache.cacheExifTags(testFiles[i], sampleExifTags[i]);
    }

    @Benchmark
    public void exifCache_containsUpToDate(Blackhole bh) {
        File file = testFiles[ThreadLocalRandom.current().nextInt(testFiles.length)];
        bh.consume(cache.containsUpToDateExifTags(file));
    }

    @Benchmark
    @Threads(10)
    public void exifCache_read_concurrent(Blackhole bh) {
        File file = testFiles[ThreadLocalRandom.current().nextInt(testFiles.length)];
        bh.consume(cache.getCachedExifTags(file));
    }
}
