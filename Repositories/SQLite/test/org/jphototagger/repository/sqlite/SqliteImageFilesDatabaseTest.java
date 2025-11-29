package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class SqliteImageFilesDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteImageFilesDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteImageFilesDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void existsImageFile_returnsFalseWhenNotExists() {
        File file = new File("/nonexistent/image.jpg");

        boolean exists = database.existsImageFile(file);

        assertThat(exists).isFalse();
    }

    @Test
    void insertImageFile_insertsAndReturnsTrue() {
        File file = new File("/test/image.jpg");

        boolean inserted = database.insertImageFile(file, 1000L, System.currentTimeMillis());

        assertThat(inserted).isTrue();
        assertThat(database.existsImageFile(file)).isTrue();
    }

    @Test
    void getFileCount_returnsCorrectCount() {
        database.insertImageFile(new File("/test/image1.jpg"), 1000L, System.currentTimeMillis());
        database.insertImageFile(new File("/test/image2.jpg"), 2000L, System.currentTimeMillis());

        long count = database.getFileCount();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void getAllImageFiles_returnsAllFiles() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        database.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertImageFile(file2, 2000L, System.currentTimeMillis());

        List<File> files = database.getAllImageFiles();

        assertThat(files).containsExactlyInAnyOrder(file1, file2);
    }

    @Test
    void deleteImageFile_deletesAndReturnsOne() {
        File file = new File("/test/image.jpg");
        database.insertImageFile(file, 1000L, System.currentTimeMillis());

        int deleted = database.deleteImageFile(file);

        assertThat(deleted).isEqualTo(1);
        assertThat(database.existsImageFile(file)).isFalse();
    }

    @Test
    void findIdImageFile_returnsIdWhenExists() {
        File file = new File("/test/image.jpg");
        database.insertImageFile(file, 1000L, System.currentTimeMillis());

        long id = database.findIdImageFile(file);

        assertThat(id).isGreaterThan(0);
    }

    @Test
    void findIdImageFile_returnsMinusOneWhenNotExists() {
        File file = new File("/nonexistent/image.jpg");

        long id = database.findIdImageFile(file);

        assertThat(id).isEqualTo(-1);
    }
}
