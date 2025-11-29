package org.jphototagger.program.module.thumbnails.cache;

import java.awt.Image;
import java.io.File;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jphototagger.api.storage.CacheDirectoryProvider;
import org.jphototagger.cachedb.CacheDbInit;
import org.jphototagger.cachedb.SqliteThumbnailCache;
import org.openide.util.Lookup;

/**
 * SQLite-backed thumbnail database.
 * Delegates to SqliteThumbnailCache.
 *
 * @author Elmar Baumann
 */
public final class ThumbnailsDb {

    private static final Logger LOGGER = Logger.getLogger(ThumbnailsDb.class.getName());
    private static final CacheDbInit CACHE_DB;
    private static final SqliteThumbnailCache THUMBNAILS;

    static {
        CacheDirectoryProvider provider = Lookup.getDefault().lookup(CacheDirectoryProvider.class);
        File cacheDirectory = provider.getCacheDirectory("ThumbnailCache");
        LOGGER.log(Level.INFO, "Opening SQLite thumbnail cache in ''{0}''", cacheDirectory);
        CACHE_DB = CacheDbInit.createForDirectory(cacheDirectory);
        THUMBNAILS = CACHE_DB.getThumbnailCache();
    }

    static boolean existsThumbnail(File imageFile) {
        return THUMBNAILS.existsThumbnail(imageFile);
    }

    static Image findThumbnail(File imageFile) {
        return THUMBNAILS.findThumbnail(imageFile);
    }

    static boolean deleteThumbnail(File imageFile) {
        return THUMBNAILS.deleteThumbnail(imageFile);
    }

    static void insertThumbnail(Image thumbnail, File imageFile) {
        THUMBNAILS.insertThumbnail(thumbnail, imageFile);
    }

    static boolean hasUpToDateThumbnail(File imageFile) {
        return THUMBNAILS.hasUpToDateThumbnail(imageFile);
    }

    static boolean renameThumbnail(File fromImageFile, File toImageFile) {
        return THUMBNAILS.renameThumbnail(fromImageFile, toImageFile);
    }

    static Set<String> getImageFilenames() {
        return THUMBNAILS.getImageFilenames();
    }

    static void compact() {
        THUMBNAILS.compact();
    }

    private ThumbnailsDb() {
    }
}
