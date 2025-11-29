package org.jphototagger.testsupport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides temporary files and directories for testing.
 */
public final class TestFiles {

    private TestFiles() {
    }

    /**
     * Creates a temporary directory for test files.
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    /**
     * Creates a temporary file with the given content.
     */
    public static Path createTempFile(String prefix, String suffix, byte[] content) throws IOException {
        Path tempFile = Files.createTempFile(prefix, suffix);
        if (content != null) {
            Files.write(tempFile, content);
        }
        return tempFile;
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    public static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.list(path).forEach(child -> {
                try {
                    deleteRecursively(child);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Files.deleteIfExists(path);
    }
}
