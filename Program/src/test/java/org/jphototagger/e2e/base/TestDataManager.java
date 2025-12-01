package org.jphototagger.e2e.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

/**
 * Manages test photo files for E2E tests.
 * Creates temp directories, copies test photos from resources,
 * and cleans up after tests.
 */
public class TestDataManager {

    private static final String[] TEST_PHOTOS = {
        "test-photo-01.jpg",
        "test-photo-02.jpg",
        "test-photo-03.jpg"
    };

    private Path tempDirectory;

    /**
     * Creates a new temporary directory for this test.
     */
    public void createTempDirectory() throws IOException {
        tempDirectory = Files.createTempDirectory("jphototagger-e2e-");
    }

    /**
     * Copies test photos from resources to the temp directory.
     */
    public void copyTestPhotos() throws IOException {
        if (tempDirectory == null) {
            throw new IllegalStateException("Call createTempDirectory() first");
        }

        for (String photoName : TEST_PHOTOS) {
            String resourcePath = "/e2e/photos/" + photoName;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + resourcePath);
                }
                Path targetPath = tempDirectory.resolve(photoName);
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Returns the temp directory containing test photos.
     */
    public File getTestPhotosFolder() {
        if (tempDirectory == null) {
            throw new IllegalStateException("Call createTempDirectory() first");
        }
        return tempDirectory.toFile();
    }

    /**
     * Returns a specific test photo file.
     */
    public File getTestPhoto(int index) {
        if (index < 0 || index >= TEST_PHOTOS.length) {
            throw new IllegalArgumentException("Invalid photo index: " + index);
        }
        return tempDirectory.resolve(TEST_PHOTOS[index]).toFile();
    }

    /**
     * Returns the number of test photos available.
     */
    public int getTestPhotoCount() {
        return TEST_PHOTOS.length;
    }

    /**
     * Deletes the temp directory and all contents.
     */
    public void cleanup() {
        if (tempDirectory == null) {
            return;
        }

        try {
            Files.walk(tempDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Best effort cleanup
                    }
                });
        } catch (IOException e) {
            // Best effort cleanup
        }

        tempDirectory = null;
    }
}
