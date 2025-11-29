# Performance Benchmarks Design

**Date:** 2025-11-29
**Status:** Approved
**Purpose:** Measure Phase 6 optimizations (startup, virtual threads, SQLite caches)

## Overview

Benchmarks to run before and after Phase 6 changes to prove optimizations work. These establish baselines in Phase 2 and verify improvements after Phase 6.

## Benchmark Suite

| Benchmark | Type | Measures |
|-----------|------|----------|
| StartupBenchmark | Standalone | LnF, caches, DB, UI init times |
| ThumbnailCacheBenchmark | JMH | Cache hit (single/concurrent) |
| ThumbnailGenerationBenchmark | JMH | Cache miss, generate + store |
| FolderLoadBenchmark | JMH | Cold/warm load for 100/500/1000 images |
| ExifCacheBenchmark | JMH | Read/write/upToDate check |
| DatabaseBenchmark | JMH | (existing) SQL query performance |

## Phase 6 Optimizations Being Measured

| Optimization | Benchmark That Measures It |
|--------------|---------------------------|
| CDS archive | StartupBenchmark |
| Lazy initialization | StartupBenchmark |
| Parallel init (virtual threads) | StartupBenchmark |
| ZGC | All (latency variance) |
| Virtual thread pool for thumbnails | FolderLoadBenchmark, ThumbnailCacheBenchmark (concurrent) |
| SQLite vs MapDB | ThumbnailCacheBenchmark, ExifCacheBenchmark |
| SQLite batching | ThumbnailGenerationBenchmark (generate + store) |

---

## Benchmark 1: StartupBenchmark

**Type:** Standalone (not JMH - needs to launch app components)

**Location:** `Benchmarks/src/main/java/org/jphototagger/benchmarks/StartupBenchmark.java`

```java
package org.jphototagger.benchmarks;

/**
 * Measures application startup time broken down by phase.
 * Run before and after Phase 6 to compare.
 *
 * Usage: java -cp ... org.jphototagger.benchmarks.StartupBenchmark
 * Output: JSON with timing for each phase
 */
public class StartupBenchmark {

    public static void main(String[] args) {
        long start = System.nanoTime();

        // Phase 1: Initialize logging, LnF
        AppLoggingSystem.init();
        AppLookAndFeel.set();
        long afterLnf = System.nanoTime();

        // Phase 2: Cache initialization (MapDB/SQLite)
        CacheUtil.initCaches();
        long afterCaches = System.nanoTime();

        // Phase 3: Database connection (HSQLDB/SQLite)
        Repository repo = Lookup.getDefault().lookup(Repository.class);
        repo.init();
        long afterDb = System.nanoTime();

        // Phase 4: UI frame creation (without showing)
        AppFrame frame = new AppFrame();
        long afterUi = System.nanoTime();

        // Output JSON for comparison
        System.out.printf(
            "{\"lnf_ms\": %.2f, \"caches_ms\": %.2f, \"db_ms\": %.2f, \"ui_ms\": %.2f, \"total_ms\": %.2f}%n",
            (afterLnf - start) / 1_000_000.0,
            (afterCaches - afterLnf) / 1_000_000.0,
            (afterDb - afterCaches) / 1_000_000.0,
            (afterUi - afterDb) / 1_000_000.0,
            (afterUi - start) / 1_000_000.0);

        System.exit(0);
    }
}
```

**What Phase 6 changes should improve:**
- `caches_ms`: SQLite init vs MapDB init
- `total_ms`: Parallel init with virtual threads
- All phases: CDS archive reduces class loading time

---

## Benchmark 2: ThumbnailCacheBenchmark

**Type:** JMH

**Location:** `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheBenchmark.java`

```java
package org.jphototagger.benchmarks;

import java.awt.Image;
import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

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
    public void setup() throws Exception {
        // Initialize cache with 1000 pre-stored thumbnails
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
```

**What Phase 6 changes should improve:**
- `cacheHit_concurrent`: Virtual thread pool scales better
- All operations: SQLite may be faster than MapDB

---

## Benchmark 3: ThumbnailGenerationBenchmark

**Type:** JMH

**Location:** `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailGenerationBenchmark.java`

```java
package org.jphototagger.benchmarks;

import java.awt.Image;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ThumbnailGenerationBenchmark {

    private File testImageFile;
    private ThumbnailCacheTestHarness cache;

    @Param({"small", "medium", "large"})
    private String imageSize;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        testImageFile = TestImageLoader.loadSampleImage(imageSize);
        cache = ThumbnailCacheTestHarness.createEmpty();
    }

    @TearDown(Level.Iteration)
    public void clearCache() {
        cache.clear();
    }

    @Benchmark
    public void generateThumbnail(Blackhole bh) {
        Image thumbnail = ThumbnailCreatorTestHarness.createThumbnail(testImageFile);
        bh.consume(thumbnail);
    }

    @Benchmark
    public void generateAndStore(Blackhole bh) {
        Image thumbnail = ThumbnailCreatorTestHarness.createThumbnail(testImageFile);
        cache.insertThumbnail(thumbnail, testImageFile);
        bh.consume(thumbnail);
    }
}
```

**What Phase 6 changes should improve:**
- `generateAndStore`: SQLite batching reduces per-write overhead

---

## Benchmark 4: FolderLoadBenchmark

**Type:** JMH

**Location:** `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/FolderLoadBenchmark.java`

```java
package org.jphototagger.benchmarks;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 5)
@Measurement(iterations = 3, time = 5)
@Fork(1)
public class FolderLoadBenchmark {

    @Param({"100", "500", "1000"})
    private int fileCount;

    private List<File> testImageFiles;
    private ThumbnailPipelineTestHarness pipeline;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        testImageFiles = TestImageLoader.createTestDirectory(fileCount);
        pipeline = new ThumbnailPipelineTestHarness();
    }

    @TearDown(Level.Trial)
    public void teardown() {
        TestImageLoader.cleanupTestDirectory();
        pipeline.close();
    }

    @Benchmark
    public void folderLoad_coldCache(Blackhole bh) throws Exception {
        pipeline.clearCache();

        CountDownLatch latch = new CountDownLatch(testImageFiles.size());

        for (File file : testImageFiles) {
            pipeline.requestThumbnail(file, thumbnail -> {
                bh.consume(thumbnail);
                latch.countDown();
            });
        }

        latch.await();
    }

    @Benchmark
    public void folderLoad_warmCache(Blackhole bh) {
        // Cache populated from previous iteration
        for (File file : testImageFiles) {
            bh.consume(pipeline.getThumbnailSync(file));
        }
    }
}
```

**What Phase 6 changes should improve:**
- `folderLoad_coldCache`: Virtual thread pool parallelizes generation
- Expected: Sub-linear scaling with file count after Phase 6

---

## Benchmark 5: ExifCacheBenchmark

**Type:** JMH

**Location:** `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheBenchmark.java`

```java
package org.jphototagger.benchmarks;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.jphototagger.exif.ExifTags;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

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
        testFiles = TestFiles.generateFilePaths(1000);
        sampleExifTags = TestExifData.generateSampleTags(1000);

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
```

**What Phase 6 changes should improve:**
- All operations: SQLite vs MapDB
- `exifCache_read`: JSON deserialization vs XML
- `exifCache_write`: SQLite batching

---

## Test Images

**Location:** `Benchmarks/src/jmh/resources/sample-images/`

```
sample-images/
├── small/          # 10 images, ~100KB each (640x480)
├── medium/         # 10 images, ~500KB each (1920x1080)
└── large/          # 10 images, ~2MB each (4000x3000)
```

**Requirements:**
- Public domain images (Unsplash/Pexels license)
- Variety of orientations (portrait, landscape)
- Include EXIF metadata for realistic testing
- Total size: ~25MB

**File generation for N-file tests:**
```java
public static List<File> createTestDirectory(int fileCount) {
    Path resourceDir = getResourcePath("/sample-images/medium");
    File[] sourceImages = resourceDir.toFile().listFiles();

    List<File> testFiles = new ArrayList<>(fileCount);
    for (int i = 0; i < fileCount; i++) {
        File source = sourceImages[i % sourceImages.length];
        Path copy = tempDir.resolve("img_" + i + ".jpg");
        Files.copy(source.toPath(), copy);
        testFiles.add(copy.toFile());
    }
    return testFiles;
}
```

---

## Running Benchmarks

**All JMH benchmarks:**
```bash
./gradlew :Benchmarks:jmh
```

**Specific benchmark:**
```bash
./gradlew :Benchmarks:jmh -Pjmh.includes="ThumbnailCacheBenchmark"
```

**Startup benchmark:**
```bash
./gradlew :Benchmarks:run --args="startup"
```

**Results location:**
- JMH: `Benchmarks/build/reports/jmh/results.json`
- Startup: stdout (JSON format)

---

## Baseline vs Post-Optimization Comparison

Save baseline results before Phase 6:
```bash
./gradlew :Benchmarks:jmh
cp Benchmarks/build/reports/jmh/results.json docs/benchmarks/baseline-phase2.json
```

After Phase 6, compare:
```bash
./gradlew :Benchmarks:jmh
# Compare results.json with baseline-phase2.json
```

**Key metrics to compare:**

| Benchmark | Metric | Expected Improvement |
|-----------|--------|---------------------|
| StartupBenchmark | total_ms | 30-50% faster (CDS + parallel init) |
| FolderLoadBenchmark (cold, 1000) | avg time | 2-4x faster (virtual threads) |
| ThumbnailCacheBenchmark (concurrent) | throughput | 2-3x higher (virtual threads) |
| ExifCacheBenchmark | read/write | 10-30% faster (SQLite vs MapDB) |

---

## Implementation Tasks

1. Add test harness classes that wrap production code for benchmarking
2. Create sample image resources
3. Implement StartupBenchmark
4. Implement ThumbnailCacheBenchmark
5. Implement ThumbnailGenerationBenchmark
6. Implement FolderLoadBenchmark
7. Implement ExifCacheBenchmark
8. Run baseline measurements
9. Save baseline results to docs/benchmarks/
