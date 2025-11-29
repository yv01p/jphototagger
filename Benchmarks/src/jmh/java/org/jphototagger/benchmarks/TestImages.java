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
