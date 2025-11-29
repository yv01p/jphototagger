package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class SqliteImageFilesRepositoryImplTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteImageFilesDatabase database;
    private SqliteImageFilesRepositoryImpl repository;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteImageFilesDatabase(factory);

        // We need to set up a static factory for the repository to use
        // This is a bit of a hack for testing, but matches the pattern
        java.lang.reflect.Field field = SqliteRepositoryImpl.class.getDeclaredField("connectionFactory");
        field.setAccessible(true);
        field.set(null, factory);

        repository = new SqliteImageFilesRepositoryImpl();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getFileCount_returnsZeroWhenEmpty() {
        long count = repository.getFileCount();

        assertThat(count).isEqualTo(0);
    }

    @Test
    void getFileCount_returnsCorrectCount() {
        database.insertImageFile(new File("/test/image1.jpg"), 1000L, System.currentTimeMillis());
        database.insertImageFile(new File("/test/image2.jpg"), 2000L, System.currentTimeMillis());

        long count = repository.getFileCount();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findAllImageFiles_returnsEmptyListWhenEmpty() {
        List<File> files = repository.findAllImageFiles();

        assertThat(files).isEmpty();
    }

    @Test
    void findAllImageFiles_returnsAllFiles() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        database.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertImageFile(file2, 2000L, System.currentTimeMillis());

        List<File> files = repository.findAllImageFiles();

        assertThat(files).containsExactlyInAnyOrder(file1, file2);
    }

    @Test
    void existsImageFile_returnsFalseWhenNotExists() {
        File file = new File("/nonexistent/image.jpg");

        boolean exists = repository.existsImageFile(file);

        assertThat(exists).isFalse();
    }

    @Test
    void existsImageFile_returnsTrueWhenExists() {
        File file = new File("/test/image.jpg");
        database.insertImageFile(file, 1000L, System.currentTimeMillis());

        boolean exists = repository.existsImageFile(file);

        assertThat(exists).isTrue();
    }

    @Test
    void deleteImageFiles_deletesMultipleFiles() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        File file3 = new File("/test/image3.jpg");
        database.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertImageFile(file2, 2000L, System.currentTimeMillis());
        database.insertImageFile(file3, 3000L, System.currentTimeMillis());

        List<File> filesToDelete = new ArrayList<>();
        filesToDelete.add(file1);
        filesToDelete.add(file2);

        int deleted = repository.deleteImageFiles(filesToDelete);

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.existsImageFile(file1)).isFalse();
        assertThat(repository.existsImageFile(file2)).isFalse();
        assertThat(repository.existsImageFile(file3)).isTrue();
    }

    @Test
    void deleteImageFiles_handlesNonExistentFiles() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/nonexistent.jpg");
        database.insertImageFile(file1, 1000L, System.currentTimeMillis());

        List<File> filesToDelete = new ArrayList<>();
        filesToDelete.add(file1);
        filesToDelete.add(file2);

        int deleted = repository.deleteImageFiles(filesToDelete);

        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void unimplementedMethods_throwUnsupportedOperationException() {
        assertThatThrownBy(() -> repository.eachImage(null))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> repository.findImageFilesLastModifiedTimestamp(null))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> repository.deleteDcSubject("test"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> repository.findTimeline())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
