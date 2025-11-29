package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class SqliteCollectionsDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteCollectionsDatabase database;
    private SqliteImageFilesDatabase imageFilesDb;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteCollectionsDatabase(factory);
        imageFilesDb = new SqliteImageFilesDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getAllCollectionNames_returnsEmptyListWhenNoCollections() {
        List<String> names = database.getAllCollectionNames();

        assertThat(names).isEmpty();
    }

    @Test
    void insertCollection_createsCollectionWithFiles() {
        // Prepare test files in the database
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());

        List<File> files = Arrays.asList(file1, file2);
        boolean inserted = database.insertCollection("MyCollection", files);

        assertThat(inserted).isTrue();
        assertThat(database.existsCollection("MyCollection")).isTrue();
    }

    @Test
    void insertCollection_returnsFalseWhenFileNotInDatabase() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/missing.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());

        List<File> files = Arrays.asList(file1, file2);
        boolean inserted = database.insertCollection("MyCollection", files);

        assertThat(inserted).isFalse();
        assertThat(database.existsCollection("MyCollection")).isFalse();
    }

    @Test
    void existsCollection_returnsTrueWhenExists() {
        File file1 = new File("/test/image1.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertCollection("MyCollection", Arrays.asList(file1));

        boolean exists = database.existsCollection("MyCollection");

        assertThat(exists).isTrue();
    }

    @Test
    void existsCollection_returnsFalseWhenNotExists() {
        boolean exists = database.existsCollection("NonExistent");

        assertThat(exists).isFalse();
    }

    @Test
    void getImageFilesOfCollection_returnsFilesInOrder() {
        // Prepare test files
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        File file3 = new File("/test/image3.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file3, 3000L, System.currentTimeMillis());

        List<File> files = Arrays.asList(file1, file2, file3);
        database.insertCollection("OrderedCollection", files);

        List<File> retrieved = database.getImageFilesOfCollection("OrderedCollection");

        assertThat(retrieved).containsExactly(file1, file2, file3);
    }

    @Test
    void getImageFilesOfCollection_returnsEmptyListWhenCollectionNotExists() {
        List<File> files = database.getImageFilesOfCollection("NonExistent");

        assertThat(files).isEmpty();
    }

    @Test
    void deleteCollection_removesCollection() {
        File file1 = new File("/test/image1.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertCollection("ToDelete", Arrays.asList(file1));

        boolean deleted = database.deleteCollection("ToDelete");

        assertThat(deleted).isTrue();
        assertThat(database.existsCollection("ToDelete")).isFalse();
    }

    @Test
    void deleteCollection_returnsFalseWhenNotExists() {
        boolean deleted = database.deleteCollection("NonExistent");

        assertThat(deleted).isFalse();
    }

    @Test
    void getCollectionCount_returnsCorrectCount() {
        File file1 = new File("/test/image1.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertCollection("Collection1", Arrays.asList(file1));
        database.insertCollection("Collection2", Arrays.asList(file1));

        int count = database.getCollectionCount();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void getCollectionCount_returnsZeroWhenEmpty() {
        int count = database.getCollectionCount();

        assertThat(count).isEqualTo(0);
    }

    @Test
    void getImageCountOfAllCollections_returnsCorrectCount() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        File file3 = new File("/test/image3.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file3, 3000L, System.currentTimeMillis());

        database.insertCollection("Collection1", Arrays.asList(file1, file2));
        database.insertCollection("Collection2", Arrays.asList(file2, file3));

        int count = database.getImageCountOfAllCollections();

        assertThat(count).isEqualTo(4); // Total entries across all collections
    }

    @Test
    void insertImagesIntoCollection_addsToExisting() {
        // Create initial collection
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        File file3 = new File("/test/image3.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file3, 3000L, System.currentTimeMillis());

        database.insertCollection("MyCollection", Arrays.asList(file1));

        // Add more images
        boolean added = database.insertImagesIntoCollection("MyCollection", Arrays.asList(file2, file3));

        assertThat(added).isTrue();
        List<File> files = database.getImageFilesOfCollection("MyCollection");
        assertThat(files).containsExactly(file1, file2, file3);
    }

    @Test
    void insertImagesIntoCollection_createsNewCollectionIfNotExists() {
        File file1 = new File("/test/image1.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());

        boolean added = database.insertImagesIntoCollection("NewCollection", Arrays.asList(file1));

        assertThat(added).isTrue();
        assertThat(database.existsCollection("NewCollection")).isTrue();
    }

    @Test
    void insertImagesIntoCollection_skipsDuplicates() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());

        database.insertCollection("MyCollection", Arrays.asList(file1, file2));

        // Try to add file1 again
        boolean added = database.insertImagesIntoCollection("MyCollection", Arrays.asList(file1));

        assertThat(added).isTrue();
        List<File> files = database.getImageFilesOfCollection("MyCollection");
        assertThat(files).containsExactly(file1, file2); // No duplicates
    }

    @Test
    void deleteImagesFromCollection_removesSpecificImages() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        File file3 = new File("/test/image3.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file3, 3000L, System.currentTimeMillis());

        database.insertCollection("MyCollection", Arrays.asList(file1, file2, file3));

        int deleted = database.deleteImagesFromCollection("MyCollection", Arrays.asList(file2));

        assertThat(deleted).isEqualTo(1);
        List<File> files = database.getImageFilesOfCollection("MyCollection");
        assertThat(files).containsExactly(file1, file3);
    }

    @Test
    void deleteImagesFromCollection_maintainsSequenceOrder() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        File file3 = new File("/test/image3.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file3, 3000L, System.currentTimeMillis());

        database.insertCollection("MyCollection", Arrays.asList(file1, file2, file3));
        database.deleteImagesFromCollection("MyCollection", Arrays.asList(file2));

        // Verify sequence numbers are reordered correctly
        List<File> files = database.getImageFilesOfCollection("MyCollection");
        assertThat(files).containsExactly(file1, file3);
    }

    @Test
    void renameCollection_changesName() {
        File file1 = new File("/test/image1.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertCollection("OldName", Arrays.asList(file1));

        int renamed = database.renameCollection("OldName", "NewName");

        assertThat(renamed).isEqualTo(1);
        assertThat(database.existsCollection("OldName")).isFalse();
        assertThat(database.existsCollection("NewName")).isTrue();
        List<File> files = database.getImageFilesOfCollection("NewName");
        assertThat(files).containsExactly(file1);
    }

    @Test
    void renameCollection_returnsZeroWhenTargetExists() {
        File file1 = new File("/test/image1.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertCollection("Collection1", Arrays.asList(file1));
        database.insertCollection("Collection2", Arrays.asList(file1));

        int renamed = database.renameCollection("Collection1", "Collection2");

        assertThat(renamed).isEqualTo(0);
        assertThat(database.existsCollection("Collection1")).isTrue();
        assertThat(database.existsCollection("Collection2")).isTrue();
    }

    @Test
    void containsFile_returnsTrueWhenFileInCollection() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());
        database.insertCollection("MyCollection", Arrays.asList(file1, file2));

        boolean contains = database.containsFile("MyCollection", file1.getAbsolutePath());

        assertThat(contains).isTrue();
    }

    @Test
    void containsFile_returnsFalseWhenFileNotInCollection() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());
        database.insertCollection("MyCollection", Arrays.asList(file1));

        boolean contains = database.containsFile("MyCollection", file2.getAbsolutePath());

        assertThat(contains).isFalse();
    }

    @Test
    void getAllCollectionNames_returnsSortedNames() {
        File file1 = new File("/test/image1.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertCollection("Zebra", Arrays.asList(file1));
        database.insertCollection("Apple", Arrays.asList(file1));
        database.insertCollection("Mango", Arrays.asList(file1));

        List<String> names = database.getAllCollectionNames();

        assertThat(names).containsExactly("Apple", "Mango", "Zebra");
    }

    @Test
    void insertCollection_replacesExistingCollection() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        imageFilesDb.insertImageFile(file1, 1000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(file2, 2000L, System.currentTimeMillis());

        database.insertCollection("MyCollection", Arrays.asList(file1));
        boolean inserted = database.insertCollection("MyCollection", Arrays.asList(file2));

        assertThat(inserted).isTrue();
        List<File> files = database.getImageFilesOfCollection("MyCollection");
        assertThat(files).containsExactly(file2);
    }
}
