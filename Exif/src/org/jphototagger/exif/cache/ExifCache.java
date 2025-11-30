package org.jphototagger.exif.cache;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.jphototagger.api.storage.CacheDirectoryProvider;
import org.jphototagger.cachedb.CacheConnectionFactory;
import org.jphototagger.domain.metadata.exif.event.ExifCacheClearedEvent;
import org.jphototagger.domain.metadata.exif.event.ExifCacheFileDeletedEvent;
import org.jphototagger.domain.repository.event.imagefiles.ImageFileDeletedEvent;
import org.jphototagger.domain.repository.event.imagefiles.ImageFileMovedEvent;
import org.jphototagger.exif.ExifTags;
import org.openide.util.Lookup;

/**
 * SQLite-backed EXIF cache.
 * Delegates to SqliteExifCache.
 *
 * @author Elmar Baumann
 */
public final class ExifCache {

    private static final Logger LOGGER = Logger.getLogger(ExifCache.class.getName());
    public static final ExifCache INSTANCE = new ExifCache();
    private final File cacheDir;
    private final SqliteExifCache sqliteCache;

    private ExifCache() {
        CacheDirectoryProvider provider = Lookup.getDefault().lookup(CacheDirectoryProvider.class);
        cacheDir = provider.getCacheDirectory("ExifCache");
        LOGGER.log(Level.INFO, "Opening SQLite EXIF cache in ''{0}''", cacheDir);
        File cacheDbFile = new File(cacheDir, "cache.db");
        CacheConnectionFactory connectionFactory = new CacheConnectionFactory(cacheDbFile);
        sqliteCache = new SqliteExifCache(connectionFactory);
    }

    public synchronized void cacheExifTags(File imageFile, ExifTags exifTags) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        if (exifTags == null) {
            throw new NullPointerException("exifTags == null");
        }
        LOGGER.log(Level.FINEST, "Caching EXIF metadata of image file ''{0}''", imageFile);
        sqliteCache.cacheExifTags(imageFile, exifTags);
    }

    public synchronized boolean containsUpToDateExifTags(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        return sqliteCache.containsUpToDateExifTags(imageFile);
    }

    public synchronized ExifTags getCachedExifTags(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        LOGGER.log(Level.FINEST, "Reading cached EXIF metadata of image file ''{0}''", imageFile);
        return sqliteCache.getCachedExifTags(imageFile);
    }

    private void deleteCachedExifTags(File imageFile) {
        sqliteCache.deleteCachedExifTags(imageFile);
        LOGGER.log(Level.FINEST, "Deleted cached EXIF metadata of image file ''{0}''", imageFile);
        EventBus.publish(new ExifCacheFileDeletedEvent(this, imageFile));
    }

    private synchronized void renameCachedExifTags(File oldImageFile, File newImageFile) {
        sqliteCache.renameCachedExifTags(oldImageFile, newImageFile);
        LOGGER.log(Level.FINEST, "Renamed cached EXIF metadata from ''{0}'' to ''{1}''",
                new Object[]{oldImageFile, newImageFile});
    }

    int clear() {
        LOGGER.log(Level.INFO, "Deleting all cached EXIF metadata");
        int count = sqliteCache.clear();
        EventBus.publish(new ExifCacheClearedEvent(this, count));
        return count;
    }

    @EventSubscriber(eventClass = ImageFileMovedEvent.class)
    public void imageFileMoved(ImageFileMovedEvent event) {
        File oldImageFile = event.getOldImageFile();
        File newImageFile = event.getNewImageFile();
        renameCachedExifTags(oldImageFile, newImageFile);
    }

    @EventSubscriber(eventClass = ImageFileDeletedEvent.class)
    public void imageFileRemoved(ImageFileDeletedEvent event) {
        File deletedImageFile = event.getImageFile();
        deleteCachedExifTags(deletedImageFile);
    }

    void init() {
        AnnotationProcessor.process(this);
    }

    File getCacheDir() {
        return cacheDir;
    }
}
