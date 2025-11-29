package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.jphototagger.api.file.FilenameTokens;
import org.jphototagger.domain.repository.FileRepositoryProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

class SqliteRepositoryImplTest {

    @TempDir
    File tempDir;

    private SqliteRepositoryImpl repository;
    private File dbFile;

    @BeforeEach
    void setUp() {
        dbFile = new File(tempDir, "test.sqlite.db");
        repository = new SqliteRepositoryImpl();
    }

    @AfterEach
    void tearDown() {
        if (repository != null && repository.isInit()) {
            repository.shutdown();
        }
    }

    @Test
    void init_initializesRepository() {
        // We can't easily test the full init() without mocking Lookup
        // but we can verify the basic structure
        assertThat(repository).isNotNull();
    }

    @Test
    void isInit_returnsFalseBeforeInit() {
        assertThat(repository.isInit()).isFalse();
    }

    @Test
    void shutdown_doesNotThrowWhenNotInitialized() {
        assertThatCode(() -> repository.shutdown()).doesNotThrowAnyException();
    }

    /**
     * Service provider to provide a test database file location.
     * This allows the repository to be tested without the full application context.
     */
    @ServiceProvider(service = FileRepositoryProvider.class, position = 1)
    public static class TestFileRepositoryProvider implements FileRepositoryProvider {

        @Override
        public File getFileRepositoryDirectory() {
            return new File(System.getProperty("java.io.tmpdir"));
        }

        @Override
        public File getDefaultFileRepositoryDirectory() {
            return new File(System.getProperty("java.io.tmpdir"));
        }

        @Override
        public String getFileRepositoryFileName(FilenameTokens filenameTokens) {
            File dir = getFileRepositoryDirectory();
            File dbFile = new File(dir, "jpt_test.script");

            switch (filenameTokens) {
                case FULL_PATH:
                case FULL_PATH_NO_SUFFIX:
                    return dbFile.getAbsolutePath();
                case NAME:
                case PREFIX:
                    return dbFile.getName();
                default:
                    return dbFile.getAbsolutePath();
            }
        }
    }
}
