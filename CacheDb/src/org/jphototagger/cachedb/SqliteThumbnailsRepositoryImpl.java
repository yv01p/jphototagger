package org.jphototagger.cachedb;

import java.awt.Image;
import java.io.File;
import java.util.Set;
import org.jphototagger.domain.repository.ThumbnailsRepository;

/**
 * SQLite-backed implementation of ThumbnailsRepository.
 */
public final class SqliteThumbnailsRepositoryImpl implements ThumbnailsRepository {

    private final SqliteThumbnailCache cache;

    public SqliteThumbnailsRepositoryImpl(SqliteThumbnailCache cache) {
        this.cache = cache;
    }

    @Override
    public void insertThumbnail(Image thumbnail, File imageFile) {
        cache.insertThumbnail(thumbnail, imageFile);
    }

    @Override
    public Image findThumbnail(File imageFile) {
        return cache.findThumbnail(imageFile);
    }

    @Override
    public boolean existsThumbnail(File imageFile) {
        return cache.existsThumbnail(imageFile);
    }

    @Override
    public boolean hasUpToDateThumbnail(File imageFile) {
        return cache.hasUpToDateThumbnail(imageFile);
    }

    @Override
    public boolean renameThumbnail(File fromImageFile, File toImageFile) {
        return cache.renameThumbnail(fromImageFile, toImageFile);
    }

    @Override
    public boolean deleteThumbnail(File imageFile) {
        return cache.deleteThumbnail(imageFile);
    }

    @Override
    public void compact() {
        cache.compact();
    }

    @Override
    public Set<String> getImageFilenames() {
        return cache.getImageFilenames();
    }
}
