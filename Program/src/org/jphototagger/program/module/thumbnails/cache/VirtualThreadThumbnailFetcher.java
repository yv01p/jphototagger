package org.jphototagger.program.module.thumbnails.cache;

import java.awt.Image;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parallel thumbnail fetcher using Java 21 virtual threads.
 * Replaces single-threaded ThumbnailFetcher for improved throughput.
 */
public final class VirtualThreadThumbnailFetcher {

    private static final Logger LOGGER = Logger.getLogger(VirtualThreadThumbnailFetcher.class.getName());
    private final ExecutorService executor;
    private final Consumer<File> onComplete;

    public VirtualThreadThumbnailFetcher(Consumer<File> onComplete) {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.onComplete = onComplete;
    }

    /**
     * Submit a file for thumbnail fetching.
     * The work is executed on a virtual thread.
     */
    public void submit(File imageFile) {
        executor.submit(() -> {
            try {
                fetchThumbnail(imageFile);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to fetch thumbnail for: " + imageFile, e);
            } finally {
                // Always call the callback, even if fetching fails
                if (onComplete != null) {
                    try {
                        onComplete.accept(imageFile);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Callback failed for: " + imageFile, e);
                    }
                }
            }
        });
    }

    private void fetchThumbnail(File imageFile) {
        if (imageFile == null) {
            LOGGER.log(Level.WARNING, "Image file is null");
            return;
        }
        // Thumbnail fetching logic - delegated to ThumbnailsDb
        Image thumbnail = ThumbnailsDb.findThumbnail(imageFile);
        if (thumbnail == null) {
            LOGGER.log(Level.FINE, "No thumbnail found for: {0}", imageFile);
        }
    }

    /**
     * Shutdown the executor gracefully.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
