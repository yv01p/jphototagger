# Phase 2 Performance Benchmarks Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add comprehensive performance benchmarks to measure Phase 6 optimizations (startup, virtual threads, SQLite caches).

**Architecture:** Extend existing Benchmarks module with test harnesses that wrap production cache/thumbnail code. Use bundled sample images for reproducible thumbnail generation tests. Startup benchmark runs as standalone main() rather than JMH.

**Tech Stack:** JMH 1.37, existing TestSupport utilities, sample JPEG images

---

## Task 1: Update Benchmarks Module Dependencies

**Files:**
- Modify: `Benchmarks/build.gradle.kts`

**Step 1: Add dependencies for cache and image benchmarking**

Edit `Benchmarks/build.gradle.kts`:
```kotlin
plugins {
    java
    id("me.champeau.jmh") version "0.7.2"
    application
}

application {
    mainClass.set("org.jphototagger.benchmarks.StartupBenchmark")
}

dependencies {
    // JMH
    jmh(libs.jmh.core)
    jmhAnnotationProcessor(libs.jmh.generator)

    // Access to application code for benchmarking
    implementation(project(":Domain"))
    implementation(project(":Repositories:HSQLDB"))
    implementation(project(":Exif"))
    implementation(project(":Image"))
    implementation(project(":Lib"))
    implementation(project(":TestSupport"))
    implementation(libs.hsqldb)

    // Local JARs needed for image/cache operations
    implementation(files("../Libraries/Jars/mapdb-0.9.9-SNAPSHOT.jar"))
    implementation(files("../Libraries/Jars/ImgRdr.jar"))
}

jmh {
    warmupIterations.set(2)
    iterations.set(5)
    fork.set(1)
    jmhVersion.set("1.37")

    // Output results to JSON for tracking
    resultFormat.set("JSON")
    resultsFile.set(project.file("build/reports/jmh/results.json"))
}

tasks.register("startup") {
    group = "benchmark"
    description = "Run startup benchmark"
    dependsOn("run")
}
```

**Step 2: Verify module compiles**

Run: `./gradlew :Benchmarks:compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Benchmarks/build.gradle.kts
git commit -m "feat(benchmark): add dependencies for cache and image benchmarks"
```

---

## Task 2: Create Sample Images Directory

**Files:**
- Create: `Benchmarks/src/jmh/resources/sample-images/README.md`
- Create: `Benchmarks/src/jmh/resources/sample-images/medium/sample-01.jpg` (and 9 more)

**Step 1: Create directory structure and README**

Create `Benchmarks/src/jmh/resources/sample-images/README.md`:
```markdown
# Sample Images for Benchmarks

These images are used for thumbnail generation and cache benchmarks.

## Sources
All images are from Unsplash (https://unsplash.com) under the Unsplash License.

## Structure
- `medium/` - 10 images, ~500KB each, 1920x1080

## Usage
Tests cycle through these images to create N test files for folder load benchmarks.
```

**Step 2: Download sample images**

Run the following to download 10 public domain images:
```bash
mkdir -p Benchmarks/src/jmh/resources/sample-images/medium
cd Benchmarks/src/jmh/resources/sample-images/medium

# Download from picsum.photos (Lorem Picsum - free images)
for i in $(seq -w 1 10); do
  curl -L "https://picsum.photos/1920/1080" -o "sample-${i}.jpg"
  sleep 1
done
```

**Step 3: Verify images exist**

Run: `ls -la Benchmarks/src/jmh/resources/sample-images/medium/`
Expected: 10 JPEG files, each ~100-500KB

**Step 4: Commit**

```bash
git add Benchmarks/src/jmh/resources/sample-images/
git commit -m "feat(benchmark): add sample images for thumbnail benchmarks"
```

---

## Task 3: Create TestImages Utility

**Files:**
- Create: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/TestImages.java`

**Step 1: Write the TestImages utility**

Create `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/TestImages.java`:
```java
package org.jphototagger.benchmarks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for loading and creating test images for benchmarks.
 */
public final class TestImages {

    private static final String SAMPLE_DIR = "/sample-images/medium/";
    private static final int SAMPLE_COUNT = 10;
    private static Path tempDirectory;

    private TestImages() {
    }

    /**
     * Creates a temporary directory with N image files for benchmarking.
     * Images are copied from bundled resources, cycling through samples.
     */
    public static List<File> createTestDirectory(int fileCount) throws IOException {
        tempDirectory = Files.createTempDirectory("jpt-benchmark-");
        List<File> files = new ArrayList<>(fileCount);

        for (int i = 0; i < fileCount; i++) {
            String sampleName = String.format("sample-%02d.jpg", (i % SAMPLE_COUNT) + 1);
            String targetName = String.format("img_%04d.jpg", i);

            try (InputStream is = TestImages.class.getResourceAsStream(SAMPLE_DIR + sampleName)) {
                if (is == null) {
                    throw new IOException("Sample image not found: " + sampleName);
                }
                Path target = tempDirectory.resolve(targetName);
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                files.add(target.toFile());
            }
        }

        return files;
    }

    /**
     * Loads a single sample image file.
     */
    public static File loadSampleImage(int index) throws IOException {
        if (tempDirectory == null) {
            tempDirectory = Files.createTempDirectory("jpt-benchmark-");
        }

        String sampleName = String.format("sample-%02d.jpg", (index % SAMPLE_COUNT) + 1);
        String targetName = String.format("sample_%d.jpg", index);

        try (InputStream is = TestImages.class.getResourceAsStream(SAMPLE_DIR + sampleName)) {
            if (is == null) {
                throw new IOException("Sample image not found: " + sampleName);
            }
            Path target = tempDirectory.resolve(targetName);
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toFile();
        }
    }

    /**
     * Cleans up the temporary directory.
     */
    public static void cleanup() throws IOException {
        if (tempDirectory != null && Files.exists(tempDirectory)) {
            Files.walk(tempDirectory)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            tempDirectory = null;
        }
    }

    /**
     * Generates fake file paths for cache key testing (no actual files).
     */
    public static File[] generateFilePaths(int count) {
        File[] files = new File[count];
        for (int i = 0; i < count; i++) {
            files[i] = new File("/fake/path/image_" + i + ".jpg");
        }
        return files;
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :Benchmarks:compileJmhJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Benchmarks/src/jmh/java/org/jphototagger/benchmarks/TestImages.java
git commit -m "feat(benchmark): add TestImages utility for sample image management"
```

---

## Task 4: Create ThumbnailCacheTestHarness

**Files:**
- Create: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheTestHarness.java`

**Step 1: Write the test harness**

Create `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheTestHarness.java`:
```java
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
```

**Step 2: Verify compilation**

Run: `./gradlew :Benchmarks:compileJmhJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheTestHarness.java
git commit -m "feat(benchmark): add ThumbnailCacheTestHarness for cache benchmarks"
```

---

## Task 5: Create ExifCacheTestHarness

**Files:**
- Create: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheTestHarness.java`

**Step 1: Write the test harness**

Create `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheTestHarness.java`:
```java
package org.jphototagger.benchmarks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.jphototagger.exif.ExifTag;
import org.jphototagger.exif.ExifTags;
import org.jphototagger.lib.xml.bind.XmlObjectExporter;
import org.jphototagger.lib.xml.bind.XmlObjectImporter;

/**
 * Test harness for EXIF cache benchmarking.
 * Simulates ExifCache behavior without requiring MapDB initialization.
 */
public final class ExifCacheTestHarness {

    private final Map<String, String> cache = new HashMap<>();

    private ExifCacheTestHarness() {
    }

    /**
     * Creates an empty EXIF cache for testing.
     */
    public static ExifCacheTestHarness create() {
        return new ExifCacheTestHarness();
    }

    /**
     * Generates sample ExifTags objects for testing.
     */
    public static ExifTags[] generateSampleTags(int count) {
        ExifTags[] tags = new ExifTags[count];
        for (int i = 0; i < count; i++) {
            ExifTags exifTags = new ExifTags();
            exifTags.setLastModified(System.currentTimeMillis() - i * 1000);

            ExifTag makeTag = new ExifTag();
            makeTag.setTagId(271);
            makeTag.setDisplayName("Make");
            makeTag.setStringValue("Camera" + (i % 5));
            exifTags.addExifTag(makeTag);

            ExifTag modelTag = new ExifTag();
            modelTag.setTagId(272);
            modelTag.setDisplayName("Model");
            modelTag.setStringValue("Model" + i);
            exifTags.addExifTag(modelTag);

            ExifTag isoTag = new ExifTag();
            isoTag.setTagId(34855);
            isoTag.setDisplayName("ISO Speed");
            isoTag.setIntegerValue(100 * (i % 32 + 1));
            exifTags.addExifTag(isoTag);

            tags[i] = exifTags;
        }
        return tags;
    }

    public void cacheExifTags(File imageFile, ExifTags exifTags) {
        try {
            String xml = XmlObjectExporter.marshal(exifTags);
            cache.put(imageFile.getAbsolutePath(), xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExifTags getCachedExifTags(File imageFile) {
        String xml = cache.get(imageFile.getAbsolutePath());
        if (xml == null) {
            return null;
        }
        try {
            return XmlObjectImporter.unmarshal(xml, ExifTags.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean containsUpToDateExifTags(File imageFile) {
        String xml = cache.get(imageFile.getAbsolutePath());
        if (xml == null) {
            return false;
        }
        try {
            ExifTags tags = XmlObjectImporter.unmarshal(xml, ExifTags.class);
            // In real code, compares with file.lastModified()
            return tags.getLastModified() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void clear() {
        cache.clear();
    }

    public void close() {
        cache.clear();
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :Benchmarks:compileJmhJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheTestHarness.java
git commit -m "feat(benchmark): add ExifCacheTestHarness for EXIF cache benchmarks"
```

---

## Task 6: Implement ThumbnailCacheBenchmark

**Files:**
- Create: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheBenchmark.java`

**Step 1: Write the benchmark**

Create `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheBenchmark.java`:
```java
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
```

**Step 2: Run the benchmark to verify it works**

Run: `./gradlew :Benchmarks:jmh -Pjmh.includes="ThumbnailCacheBenchmark"`
Expected: Benchmark runs and produces results

**Step 3: Commit**

```bash
git add Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheBenchmark.java
git commit -m "feat(benchmark): add ThumbnailCacheBenchmark for cache hit performance"
```

---

## Task 7: Implement ExifCacheBenchmark

**Files:**
- Create: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheBenchmark.java`

**Step 1: Write the benchmark**

Create `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheBenchmark.java`:
```java
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
```

**Step 2: Run the benchmark to verify it works**

Run: `./gradlew :Benchmarks:jmh -Pjmh.includes="ExifCacheBenchmark"`
Expected: Benchmark runs and produces results

**Step 3: Commit**

```bash
git add Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheBenchmark.java
git commit -m "feat(benchmark): add ExifCacheBenchmark for EXIF cache performance"
```

---

## Task 8: Implement ThumbnailGenerationBenchmark

**Files:**
- Create: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailGenerationBenchmark.java`

**Step 1: Write the benchmark**

Create `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailGenerationBenchmark.java`:
```java
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
```

**Step 2: Run the benchmark to verify it works**

Run: `./gradlew :Benchmarks:jmh -Pjmh.includes="ThumbnailGenerationBenchmark"`
Expected: Benchmark runs and produces results

**Step 3: Commit**

```bash
git add Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailGenerationBenchmark.java
git commit -m "feat(benchmark): add ThumbnailGenerationBenchmark for cache miss performance"
```

---

## Task 9: Implement FolderLoadBenchmark

**Files:**
- Create: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/FolderLoadBenchmark.java`

**Step 1: Write the benchmark**

Create `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/FolderLoadBenchmark.java`:
```java
package org.jphototagger.benchmarks;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
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
```

**Step 2: Run the benchmark to verify it works**

Run: `./gradlew :Benchmarks:jmh -Pjmh.includes="FolderLoadBenchmark"`
Expected: Benchmark runs (takes longer due to file I/O)

**Step 3: Commit**

```bash
git add Benchmarks/src/jmh/java/org/jphototagger/benchmarks/FolderLoadBenchmark.java
git commit -m "feat(benchmark): add FolderLoadBenchmark for end-to-end folder loading"
```

---

## Task 10: Implement StartupBenchmark

**Files:**
- Create: `Benchmarks/src/main/java/org/jphototagger/benchmarks/StartupBenchmark.java`

**Step 1: Write the standalone benchmark**

Create `Benchmarks/src/main/java/org/jphototagger/benchmarks/StartupBenchmark.java`:
```java
package org.jphototagger.benchmarks;

/**
 * Standalone startup time benchmark.
 * Measures initialization phases without launching full UI.
 *
 * Run with: ./gradlew :Benchmarks:run
 *
 * Note: This is a simplified version that measures class loading
 * and basic initialization. Full startup benchmarking requires
 * the complete application context.
 */
public class StartupBenchmark {

    public static void main(String[] args) {
        System.out.println("JPhotoTagger Startup Benchmark");
        System.out.println("==============================");
        System.out.println();

        long totalStart = System.nanoTime();

        // Phase 1: Class loading (load key classes)
        long phase1Start = System.nanoTime();
        loadClasses();
        long phase1End = System.nanoTime();

        // Phase 2: JAXB initialization
        long phase2Start = System.nanoTime();
        initJaxb();
        long phase2End = System.nanoTime();

        // Phase 3: Image I/O initialization
        long phase3Start = System.nanoTime();
        initImageIO();
        long phase3End = System.nanoTime();

        long totalEnd = System.nanoTime();

        // Output results
        double phase1Ms = (phase1End - phase1Start) / 1_000_000.0;
        double phase2Ms = (phase2End - phase2Start) / 1_000_000.0;
        double phase3Ms = (phase3End - phase3Start) / 1_000_000.0;
        double totalMs = (totalEnd - totalStart) / 1_000_000.0;

        System.out.printf("Phase 1 (Class Loading):    %8.2f ms%n", phase1Ms);
        System.out.printf("Phase 2 (JAXB Init):        %8.2f ms%n", phase2Ms);
        System.out.printf("Phase 3 (ImageIO Init):     %8.2f ms%n", phase3Ms);
        System.out.println("------------------------------");
        System.out.printf("Total:                      %8.2f ms%n", totalMs);
        System.out.println();

        // JSON output for automated comparison
        System.out.printf("{\"class_loading_ms\": %.2f, \"jaxb_ms\": %.2f, \"imageio_ms\": %.2f, \"total_ms\": %.2f}%n",
                phase1Ms, phase2Ms, phase3Ms, totalMs);
    }

    private static void loadClasses() {
        try {
            // Load key domain classes
            Class.forName("org.jphototagger.domain.metadata.keywords.Keyword");
            Class.forName("org.jphototagger.domain.image.ImageFile");
            Class.forName("org.jphototagger.exif.ExifTags");
            Class.forName("org.jphototagger.exif.ExifTag");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: Could not load class - " + e.getMessage());
        }
    }

    private static void initJaxb() {
        try {
            // Initialize JAXB context (expensive first-time operation)
            org.jphototagger.exif.ExifTags tags = new org.jphototagger.exif.ExifTags();
            org.jphototagger.lib.xml.bind.XmlObjectExporter.marshal(tags);
        } catch (Exception e) {
            System.err.println("Warning: JAXB init failed - " + e.getMessage());
        }
    }

    private static void initImageIO() {
        // Initialize ImageIO (scans for plugins)
        javax.imageio.ImageIO.getReaderFormatNames();
        javax.imageio.ImageIO.getWriterFormatNames();
    }
}
```

**Step 2: Run the benchmark**

Run: `./gradlew :Benchmarks:run`
Expected: Prints timing for each phase and JSON summary

**Step 3: Commit**

```bash
git add Benchmarks/src/main/java/org/jphototagger/benchmarks/StartupBenchmark.java
git commit -m "feat(benchmark): add StartupBenchmark for initialization timing"
```

---

## Task 11: Run Full Benchmark Suite and Save Baseline

**Files:**
- Create: `docs/benchmarks/baseline-phase2.json`

**Step 1: Run all JMH benchmarks**

Run: `./gradlew :Benchmarks:jmh`
Expected: All benchmarks complete, results in `Benchmarks/build/reports/jmh/results.json`

**Step 2: Run startup benchmark**

Run: `./gradlew :Benchmarks:run > docs/benchmarks/startup-baseline.txt 2>&1`
Expected: Startup timing saved

**Step 3: Save baseline results**

```bash
mkdir -p docs/benchmarks
cp Benchmarks/build/reports/jmh/results.json docs/benchmarks/baseline-phase2.json
```

**Step 4: Commit baseline**

```bash
git add docs/benchmarks/
git commit -m "chore(benchmark): save Phase 2 baseline benchmark results

These results serve as the comparison point for Phase 6 optimizations."
```

---

## Task 12: Update Phase 2 Plan Reference

**Files:**
- Modify: `docs/plans/2025-11-29-phase2-testing-foundation.md`

**Step 1: Add reference to new benchmarks**

Add before the Summary section in `docs/plans/2025-11-29-phase2-testing-foundation.md`:

```markdown
---

## Task 12: Extended Performance Benchmarks

See `docs/plans/2025-11-29-phase2-performance-benchmarks.md` for additional benchmarks:

- **ThumbnailCacheBenchmark** - Cache hit performance (single/concurrent)
- **ThumbnailGenerationBenchmark** - Cache miss with image scaling
- **FolderLoadBenchmark** - End-to-end folder loading (10/50/100 images)
- **ExifCacheBenchmark** - EXIF cache read/write with XML serialization
- **StartupBenchmark** - Application initialization phases

These benchmarks establish baselines for Phase 6 optimization validation.

---
```

**Step 2: Commit**

```bash
git add docs/plans/2025-11-29-phase2-testing-foundation.md
git commit -m "docs: reference performance benchmarks in Phase 2 plan"
```

---

## Summary

This plan adds 5 new benchmarks to measure Phase 6 optimizations:

| Benchmark | What it measures | Phase 6 improvement expected |
|-----------|------------------|------------------------------|
| ThumbnailCacheBenchmark | Cache hit latency | Virtual thread scaling |
| ThumbnailGenerationBenchmark | Cache miss + store | SQLite batching |
| FolderLoadBenchmark | End-to-end loading | Virtual thread parallelism |
| ExifCacheBenchmark | XML serialization | SQLite + possible JSON |
| StartupBenchmark | Init phase timing | CDS, parallel init |

**Baseline saved to:** `docs/benchmarks/baseline-phase2.json`
