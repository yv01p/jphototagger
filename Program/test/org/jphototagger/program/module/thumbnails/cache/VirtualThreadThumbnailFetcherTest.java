package org.jphototagger.program.module.thumbnails.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;

class VirtualThreadThumbnailFetcherTest {

    @TempDir
    File tempDir;

    @Test
    void fetchesThumbnailsInParallel() throws Exception {
        // Create test images
        int imageCount = 10;
        File[] files = new File[imageCount];
        for (int i = 0; i < imageCount; i++) {
            files[i] = new File(tempDir, "test" + i + ".jpg");
            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(img, "jpg", files[i]);
        }

        CountDownLatch latch = new CountDownLatch(imageCount);
        AtomicInteger completed = new AtomicInteger(0);

        VirtualThreadThumbnailFetcher fetcher = new VirtualThreadThumbnailFetcher(
            file -> {
                completed.incrementAndGet();
                latch.countDown();
            }
        );

        // Submit all files
        for (File file : files) {
            fetcher.submit(file);
        }

        // Wait for completion with timeout
        boolean allCompleted = latch.await(30, TimeUnit.SECONDS);
        fetcher.shutdown();

        assertThat(allCompleted).isTrue();
        assertThat(completed.get()).isEqualTo(imageCount);
    }
}
