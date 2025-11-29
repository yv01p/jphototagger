package org.jphototagger.cachedb;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class SqliteThumbnailCacheTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private SqliteThumbnailCache cache;
    private File imageFile;

    @BeforeEach
    void setUp() {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        cache = new SqliteThumbnailCache(factory);

        imageFile = new File(tempDir, "test.jpg");
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void constructor_createsTable() throws Exception {
        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name='thumbnails'")) {
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void existsThumbnail_returnsFalseForMissing() {
        assertThat(cache.existsThumbnail(imageFile)).isFalse();
    }

    @Test
    void insertAndFind_roundTrip() throws Exception {
        // Create test file for lastModified
        imageFile.createNewFile();

        BufferedImage thumbnail = createTestThumbnail();
        cache.insertThumbnail(thumbnail, imageFile);

        assertThat(cache.existsThumbnail(imageFile)).isTrue();

        Image result = cache.findThumbnail(imageFile);
        assertThat(result).isNotNull();
        assertThat(result.getWidth(null)).isEqualTo(100);
        assertThat(result.getHeight(null)).isEqualTo(100);
    }

    @Test
    void hasUpToDateThumbnail_returnsTrueWhenCurrent() throws Exception {
        imageFile.createNewFile();

        cache.insertThumbnail(createTestThumbnail(), imageFile);

        assertThat(cache.hasUpToDateThumbnail(imageFile)).isTrue();
    }

    @Test
    void hasUpToDateThumbnail_returnsFalseWhenStale() throws Exception {
        imageFile.createNewFile();
        long originalModified = imageFile.lastModified();

        cache.insertThumbnail(createTestThumbnail(), imageFile);

        // Simulate file modification
        imageFile.setLastModified(originalModified + 1000);

        assertThat(cache.hasUpToDateThumbnail(imageFile)).isFalse();
    }

    @Test
    void deleteThumbnail_removesEntry() throws Exception {
        imageFile.createNewFile();
        cache.insertThumbnail(createTestThumbnail(), imageFile);
        assertThat(cache.existsThumbnail(imageFile)).isTrue();

        boolean deleted = cache.deleteThumbnail(imageFile);

        assertThat(deleted).isTrue();
        assertThat(cache.existsThumbnail(imageFile)).isFalse();
    }

    @Test
    void renameThumbnail_movesEntry() throws Exception {
        imageFile.createNewFile();
        cache.insertThumbnail(createTestThumbnail(), imageFile);

        File newFile = new File(tempDir, "renamed.jpg");
        boolean renamed = cache.renameThumbnail(imageFile, newFile);

        assertThat(renamed).isTrue();
        assertThat(cache.existsThumbnail(imageFile)).isFalse();
        assertThat(cache.existsThumbnail(newFile)).isTrue();
    }

    @Test
    void getImageFilenames_returnsAllCachedPaths() throws Exception {
        File file1 = new File(tempDir, "test1.jpg");
        File file2 = new File(tempDir, "test2.jpg");
        file1.createNewFile();
        file2.createNewFile();

        cache.insertThumbnail(createTestThumbnail(), file1);
        cache.insertThumbnail(createTestThumbnail(), file2);

        Set<String> filenames = cache.getImageFilenames();
        assertThat(filenames).hasSize(2);
        assertThat(filenames).contains(file1.getAbsolutePath(), file2.getAbsolutePath());
    }

    @Test
    void compact_doesNotBreakExistingData() throws Exception {
        imageFile.createNewFile();
        cache.insertThumbnail(createTestThumbnail(), imageFile);

        cache.compact();

        assertThat(cache.existsThumbnail(imageFile)).isTrue();
    }

    private BufferedImage createTestThumbnail() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        return img;
    }
}
