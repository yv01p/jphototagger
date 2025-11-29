package org.jphototagger.cachedb;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import org.jphototagger.domain.repository.ThumbnailsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class SqliteThumbnailsRepositoryImplTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private SqliteThumbnailsRepositoryImpl repository;
    private File imageFile;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        SqliteThumbnailCache cache = new SqliteThumbnailCache(factory);
        repository = new SqliteThumbnailsRepositoryImpl(cache);

        imageFile = new File(tempDir, "test.jpg");
        imageFile.createNewFile();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void implementsThumbnailsRepository() {
        assertThat(repository).isInstanceOf(ThumbnailsRepository.class);
    }

    @Test
    void insertAndFindThumbnail() {
        BufferedImage thumbnail = createTestThumbnail();
        repository.insertThumbnail(thumbnail, imageFile);

        Image result = repository.findThumbnail(imageFile);
        assertThat(result).isNotNull();
    }

    @Test
    void existsThumbnail() {
        assertThat(repository.existsThumbnail(imageFile)).isFalse();

        repository.insertThumbnail(createTestThumbnail(), imageFile);

        assertThat(repository.existsThumbnail(imageFile)).isTrue();
    }

    @Test
    void deleteThumbnail() {
        repository.insertThumbnail(createTestThumbnail(), imageFile);
        assertThat(repository.existsThumbnail(imageFile)).isTrue();

        repository.deleteThumbnail(imageFile);

        assertThat(repository.existsThumbnail(imageFile)).isFalse();
    }

    private BufferedImage createTestThumbnail() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.RED);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        return img;
    }
}
