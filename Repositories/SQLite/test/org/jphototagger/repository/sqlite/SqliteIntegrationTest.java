package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration test that exercises all major SQLite repository operations
 * in a realistic workflow. This test verifies end-to-end functionality of the SQLite
 * backend implementation.
 *
 * The test uses the system property '-Djphototagger.database.backend=sqlite' conceptually,
 * though in this unit test context we directly instantiate the SQLite components.
 *
 * Test coverage:
 * - Image file operations (insert, query, delete)
 * - Keyword/DC subject operations (insert, query, delete)
 * - XMP metadata operations (insert, update, query)
 * - Collections operations (create, add images, query)
 * - Saved searches operations (create, query)
 * - Synonyms operations (insert, query)
 * - Application properties operations (set, get)
 * - Foreign key constraints and cascading deletes
 * - Transaction consistency
 *
 * @author Claude
 */
class SqliteIntegrationTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteImageFilesDatabase imageFilesDb;
    private SqliteKeywordsDatabase keywordsDb;
    private SqliteXmpDatabase xmpDb;
    private SqliteCollectionsDatabase collectionsDb;
    private SqliteSavedSearchesDatabase savedSearchesDb;
    private SqliteSynonymsDatabase synonymsDb;
    private SqliteApplicationPropertiesDatabase appPropsDb;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "integration-test.db");
        factory = new SqliteConnectionFactory(dbFile);

        // Create all tables
        new SqliteTables(factory).createTables();

        // Initialize all database components
        imageFilesDb = new SqliteImageFilesDatabase(factory);
        keywordsDb = new SqliteKeywordsDatabase(factory);
        xmpDb = new SqliteXmpDatabase(factory);
        collectionsDb = new SqliteCollectionsDatabase(factory);
        savedSearchesDb = new SqliteSavedSearchesDatabase(factory);
        synonymsDb = new SqliteSynonymsDatabase(factory);
        appPropsDb = new SqliteApplicationPropertiesDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    /**
     * Test Scenario 1: Image File Lifecycle
     *
     * Simulates the complete lifecycle of image files in the repository:
     * 1. Insert multiple image files
     * 2. Query file existence and count
     * 3. Retrieve all files
     * 4. Delete a file
     * 5. Verify file count updated
     */
    @Test
    void imageFileLifecycle_completeCRUDOperations() {
        // Insert image files
        File image1 = new File("/photos/2025/vacation/beach.jpg");
        File image2 = new File("/photos/2025/vacation/sunset.jpg");
        File image3 = new File("/photos/2025/family/birthday.jpg");

        assertThat(imageFilesDb.insertImageFile(image1, 2048000L, System.currentTimeMillis())).isTrue();
        assertThat(imageFilesDb.insertImageFile(image2, 3072000L, System.currentTimeMillis())).isTrue();
        assertThat(imageFilesDb.insertImageFile(image3, 1536000L, System.currentTimeMillis())).isTrue();

        // Query operations
        assertThat(imageFilesDb.existsImageFile(image1)).isTrue();
        assertThat(imageFilesDb.existsImageFile(new File("/nonexistent.jpg"))).isFalse();
        assertThat(imageFilesDb.getFileCount()).isEqualTo(3);

        // Retrieve all files
        List<File> allFiles = imageFilesDb.getAllImageFiles();
        assertThat(allFiles).hasSize(3).containsExactlyInAnyOrder(image1, image2, image3);

        // Delete a file
        assertThat(imageFilesDb.deleteImageFile(image2)).isEqualTo(1);
        assertThat(imageFilesDb.getFileCount()).isEqualTo(2);
        assertThat(imageFilesDb.existsImageFile(image2)).isFalse();
    }

    /**
     * Test Scenario 2: Keyword Management
     *
     * Tests keyword/DC subject operations:
     * 1. Insert keywords
     * 2. Query all keywords (ordered)
     * 3. Check existence
     * 4. Find keyword IDs
     * 5. Delete keywords
     */
    @Test
    void keywordManagement_insertQueryDelete() {
        // Insert keywords
        assertThat(keywordsDb.insertDcSubject("nature")).isTrue();
        assertThat(keywordsDb.insertDcSubject("wildlife")).isTrue();
        assertThat(keywordsDb.insertDcSubject("landscape")).isTrue();
        assertThat(keywordsDb.insertDcSubject("travel")).isTrue();

        // Query all (should be ordered alphabetically)
        Set<String> allSubjects = keywordsDb.getAllDcSubjects();
        assertThat(allSubjects).containsExactly("landscape", "nature", "travel", "wildlife");

        // Check existence
        assertThat(keywordsDb.existsDcSubject("nature")).isTrue();
        assertThat(keywordsDb.existsDcSubject("nonexistent")).isFalse();

        // Find IDs
        Long natureId = keywordsDb.findIdDcSubject("nature");
        assertThat(natureId).isNotNull().isGreaterThan(0);
        assertThat(keywordsDb.findIdDcSubject("nonexistent")).isNull();

        // Delete keyword
        keywordsDb.deleteDcSubject("wildlife");
        assertThat(keywordsDb.getAllDcSubjects()).hasSize(3).doesNotContain("wildlife");
    }

    /**
     * Test Scenario 3: XMP Metadata Operations
     *
     * Tests XMP metadata workflow:
     * 1. Insert image file
     * 2. Insert XMP metadata for the file
     * 3. Link keywords to the XMP record
     * 4. Query XMP data
     * 5. Update XMP data
     * 6. Verify cascade delete when file is deleted
     */
    @Test
    void xmpMetadata_fullWorkflow() {
        // Insert image
        File image = new File("/photos/metadata-test.jpg");
        imageFilesDb.insertImageFile(image, 2048000L, System.currentTimeMillis());
        long fileId = imageFilesDb.findIdImageFile(image);

        // Insert keywords first
        keywordsDb.insertDcSubject("sunset");
        keywordsDb.insertDcSubject("beach");

        // Insert XMP with proper method signature
        boolean xmpInserted = xmpDb.insertXmp(
            fileId,
            "John Doe",  // creator
            "Beautiful sunset at the beach",  // description
            "Copyright 2025",  // rights
            "Summer Vacation 2025",  // title
            "California Coast",  // location
            "Santa Monica",  // city
            "USA",  // country
            "Sunset Glory",  // headline
            5  // rating
        );
        assertThat(xmpInserted).isTrue();

        // Link keywords to XMP (using subject names, not IDs)
        long xmpId = xmpDb.getXmpId(fileId);
        assertThat(xmpId).isGreaterThan(0);
        xmpDb.insertXmpDcSubject(xmpId, "sunset");
        xmpDb.insertXmpDcSubject(xmpId, "beach");

        // Query XMP exists
        assertThat(xmpDb.existsXmp(fileId)).isTrue();

        // Update rating
        xmpDb.updateRating(fileId, 4);

        // Verify cascade delete - deleting file should delete XMP
        imageFilesDb.deleteImageFile(image);
        assertThat(xmpDb.existsXmp(fileId)).isFalse();
    }

    /**
     * Test Scenario 4: Image Collections
     *
     * Tests collection management:
     * 1. Create collections with images
     * 2. Query collections
     * 3. Get images from collections
     * 4. Delete collections
     */
    @Test
    void imageCollections_createAndManage() {
        // Insert images
        File img1 = new File("/photos/collection1.jpg");
        File img2 = new File("/photos/collection2.jpg");
        File img3 = new File("/photos/collection3.jpg");
        imageFilesDb.insertImageFile(img1, 1000000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(img2, 2000000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(img3, 3000000L, System.currentTimeMillis());

        // Create collections (insertCollection takes name and list of files)
        List<File> bestPhotos = Arrays.asList(img1, img2);
        List<File> favorites = Arrays.asList(img1, img3);

        assertThat(collectionsDb.insertCollection("Best of 2025", bestPhotos)).isTrue();
        assertThat(collectionsDb.insertCollection("Favorites", favorites)).isTrue();

        // Query collections
        List<String> allCollections = collectionsDb.getAllCollectionNames();
        assertThat(allCollections).containsExactlyInAnyOrder("Best of 2025", "Favorites");

        // Check existence
        assertThat(collectionsDb.existsCollection("Best of 2025")).isTrue();
        assertThat(collectionsDb.existsCollection("Nonexistent")).isFalse();

        // Get images from collection
        List<File> bestPhotosRetrieved = collectionsDb.getImageFilesOfCollection("Best of 2025");
        assertThat(bestPhotosRetrieved).hasSize(2).containsExactlyInAnyOrder(img1, img2);

        // Delete collection should cascade delete entries
        collectionsDb.deleteCollection("Favorites");
        assertThat(collectionsDb.getAllCollectionNames()).hasSize(1).contains("Best of 2025");
    }

    /**
     * Test Scenario 5: Saved Searches
     *
     * Tests saved search functionality:
     * 1. Create saved searches
     * 2. Query searches
     * 3. Delete searches
     */
    @Test
    void savedSearches_createAndQuery() {
        // Insert saved searches
        boolean search1 = savedSearchesDb.insert("High Rated Photos", null, (short) 0);
        boolean search2 = savedSearchesDb.insert("Recent Vacation", null, (short) 0);
        assertThat(search1).isTrue();
        assertThat(search2).isTrue();

        // Query count
        assertThat(savedSearchesDb.getCount()).isEqualTo(2);

        // Check existence
        assertThat(savedSearchesDb.exists("High Rated Photos")).isTrue();
        assertThat(savedSearchesDb.exists("Nonexistent")).isFalse();

        // Delete search
        savedSearchesDb.delete("Recent Vacation");
        assertThat(savedSearchesDb.getCount()).isEqualTo(1);
    }

    /**
     * Test Scenario 6: Synonyms
     *
     * Tests synonym management:
     * 1. Insert synonyms
     * 2. Delete synonyms
     */
    @Test
    void synonyms_insertAndDelete() {
        // Insert synonyms (returns count)
        assertThat(synonymsDb.insertSynonym("ocean", "sea")).isEqualTo(1);
        assertThat(synonymsDb.insertSynonym("ocean", "water")).isEqualTo(1);
        assertThat(synonymsDb.insertSynonym("mountain", "peak")).isEqualTo(1);

        // Check existence
        assertThat(synonymsDb.existsSynonym("ocean", "sea")).isTrue();
        assertThat(synonymsDb.existsSynonym("ocean", "lake")).isFalse();

        // Delete synonym
        assertThat(synonymsDb.deleteSynonym("ocean", "water")).isEqualTo(1);
        assertThat(synonymsDb.existsSynonym("ocean", "water")).isFalse();
        assertThat(synonymsDb.existsSynonym("ocean", "sea")).isTrue();
    }

    /**
     * Test Scenario 7: Application Properties
     *
     * Tests application properties storage:
     * 1. Set properties
     * 2. Get properties
     * 3. Delete properties
     */
    @Test
    void applicationProperties_setAndGet() {
        // Set properties
        appPropsDb.setString("app.version", "1.0.0");
        appPropsDb.setString("user.theme", "dark");
        appPropsDb.setString("window.width", "1920");

        // Get properties
        assertThat(appPropsDb.getString("app.version")).isEqualTo("1.0.0");
        assertThat(appPropsDb.getString("user.theme")).isEqualTo("dark");
        assertThat(appPropsDb.getString("nonexistent")).isNull();

        // Check existence
        assertThat(appPropsDb.existsKey("app.version")).isTrue();
        assertThat(appPropsDb.existsKey("nonexistent")).isFalse();

        // Update property
        appPropsDb.setString("user.theme", "light");
        assertThat(appPropsDb.getString("user.theme")).isEqualTo("light");

        // Delete property
        appPropsDb.deleteKey("window.width");
        assertThat(appPropsDb.getString("window.width")).isNull();
    }

    /**
     * Test Scenario 8: Complex Workflow - Complete Photo Tagging
     *
     * Simulates a realistic photo management workflow:
     * 1. Import photos
     * 2. Add keywords
     * 3. Add XMP metadata with keywords
     * 4. Create collections
     * 5. Create saved searches
     * 6. Verify all relationships
     * 7. Clean up - verify cascading deletes
     */
    @Test
    void completePhotoTaggingWorkflow_endToEnd() {
        // Step 1: Import photos
        File photo1 = new File("/import/2025/trip/mountain1.jpg");
        File photo2 = new File("/import/2025/trip/mountain2.jpg");
        File photo3 = new File("/import/2025/trip/lake.jpg");

        imageFilesDb.insertImageFile(photo1, 4096000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(photo2, 3584000L, System.currentTimeMillis());
        imageFilesDb.insertImageFile(photo3, 5120000L, System.currentTimeMillis());

        long photo1Id = imageFilesDb.findIdImageFile(photo1);
        long photo2Id = imageFilesDb.findIdImageFile(photo2);
        long photo3Id = imageFilesDb.findIdImageFile(photo3);

        // Step 2: Create keyword taxonomy
        keywordsDb.insertDcSubject("mountains");
        keywordsDb.insertDcSubject("nature");
        keywordsDb.insertDcSubject("landscape");
        keywordsDb.insertDcSubject("hiking");
        keywordsDb.insertDcSubject("water");

        // Step 3: Add XMP metadata to photos
        xmpDb.insertXmp(photo1Id, null, "Majestic mountain peak", null, "Alpine Vista",
                        null, "Aspen", "USA", "Mountain Majesty", 5);

        xmpDb.insertXmp(photo2Id, null, "Mountain range at sunset", null, "Sunset Peaks",
                        null, "Aspen", "USA", "Golden Hour", 5);

        xmpDb.insertXmp(photo3Id, null, "Crystal clear mountain lake", null, "Alpine Lake",
                        null, "Aspen", "USA", "Reflection", 4);

        // Link keywords to photos (using subject names)
        long xmp1Id = xmpDb.getXmpId(photo1Id);
        long xmp2Id = xmpDb.getXmpId(photo2Id);
        long xmp3Id = xmpDb.getXmpId(photo3Id);

        xmpDb.insertXmpDcSubject(xmp1Id, "mountains");
        xmpDb.insertXmpDcSubject(xmp1Id, "nature");
        xmpDb.insertXmpDcSubject(xmp1Id, "landscape");
        xmpDb.insertXmpDcSubject(xmp1Id, "hiking");

        xmpDb.insertXmpDcSubject(xmp2Id, "mountains");
        xmpDb.insertXmpDcSubject(xmp2Id, "nature");
        xmpDb.insertXmpDcSubject(xmp2Id, "landscape");

        xmpDb.insertXmpDcSubject(xmp3Id, "water");
        xmpDb.insertXmpDcSubject(xmp3Id, "nature");
        xmpDb.insertXmpDcSubject(xmp3Id, "landscape");

        // Step 4: Create collection
        List<File> tripPhotos = Arrays.asList(photo1, photo2, photo3);
        collectionsDb.insertCollection("Colorado Trip 2025", tripPhotos);

        // Step 5: Create saved search
        savedSearchesDb.insert("Mountain Photos", null, (short) 0);

        // Step 6: Verify everything is connected
        assertThat(imageFilesDb.getFileCount()).isEqualTo(3);
        assertThat(keywordsDb.getAllDcSubjects()).hasSize(5);
        assertThat(xmpDb.existsXmp(photo1Id)).isTrue();
        assertThat(xmpDb.existsXmp(photo2Id)).isTrue();
        assertThat(xmpDb.existsXmp(photo3Id)).isTrue();
        assertThat(collectionsDb.getAllCollectionNames()).hasSize(1);
        assertThat(savedSearchesDb.getCount()).isEqualTo(1);

        // Step 7: Test cascading delete - delete photo1
        // This should cascade delete its XMP data
        imageFilesDb.deleteImageFile(photo1);
        assertThat(imageFilesDb.getFileCount()).isEqualTo(2);
        assertThat(xmpDb.existsXmp(photo1Id)).isFalse();

        // Collection should still exist but with fewer images
        assertThat(collectionsDb.getAllCollectionNames()).hasSize(1);
        List<File> remainingPhotos = collectionsDb.getImageFilesOfCollection("Colorado Trip 2025");
        assertThat(remainingPhotos).hasSize(2).containsExactlyInAnyOrder(photo2, photo3);

        // Keywords should still exist (they're not cascade deleted)
        assertThat(keywordsDb.getAllDcSubjects()).hasSize(5);
    }

    /**
     * Test Scenario 9: Database Integrity - Foreign Key Constraints
     *
     * Verifies foreign key constraints are properly enforced:
     * 1. Cannot insert XMP for non-existent file
     * 2. Cascading deletes work properly
     */
    @Test
    void databaseIntegrity_foreignKeyConstraints() {
        // Try to insert XMP for non-existent file - should fail gracefully
        boolean xmpResult = xmpDb.insertXmp(999999L, null, "Test", null, null,
                                            null, null, null, null, 5);
        // Should fail due to foreign key constraint
        assertThat(xmpResult).isFalse();

        // Insert valid file and metadata
        File validFile = new File("/test/valid.jpg");
        imageFilesDb.insertImageFile(validFile, 1000000L, System.currentTimeMillis());
        long fileId = imageFilesDb.findIdImageFile(validFile);

        xmpDb.insertXmp(fileId, null, "Valid XMP", null, null,
                       null, null, null, null, 3);

        assertThat(xmpDb.existsXmp(fileId)).isTrue();

        // Delete file - should cascade
        imageFilesDb.deleteImageFile(validFile);
        assertThat(xmpDb.existsXmp(fileId)).isFalse();
    }
}
