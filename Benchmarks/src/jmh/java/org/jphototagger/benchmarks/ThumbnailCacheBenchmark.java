package org.jphototagger.benchmarks;

import java.awt.Image;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks for thumbnail cache operations.
 * Measures cache hit performance for Phase 6 comparison.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ThumbnailCacheBenchmark {

    private File[] testFiles;
    private ThumbnailCacheTestHarness cache;

    @Setup(Level.Trial)
    public void setup() {
        cache = ThumbnailCacheTestHarness.createWithSampleData(1000);
        testFiles = cache.getStoredFiles();
    }

    @TearDown(Level.Trial)
    public void teardown() {
        cache.close();
    }

    @Benchmark
    public void cacheHit_single(Blackhole bh) {
        File file = testFiles[ThreadLocalRandom.current().nextInt(testFiles.length)];
        Image thumbnail = cache.findThumbnail(file);
        bh.consume(thumbnail);
    }

    @Benchmark
    @Threads(10)
    public void cacheHit_concurrent(Blackhole bh) {
        File file = testFiles[ThreadLocalRandom.current().nextInt(testFiles.length)];
        Image thumbnail = cache.findThumbnail(file);
        bh.consume(thumbnail);
    }

    @Benchmark
    public void cacheExists_single(Blackhole bh) {
        File file = testFiles[ThreadLocalRandom.current().nextInt(testFiles.length)];
        bh.consume(cache.existsThumbnail(file));
    }

    @Benchmark
    public void cacheUpToDate_single(Blackhole bh) {
        File file = testFiles[ThreadLocalRandom.current().nextInt(testFiles.length)];
        bh.consume(cache.hasUpToDateThumbnail(file));
    }
}
