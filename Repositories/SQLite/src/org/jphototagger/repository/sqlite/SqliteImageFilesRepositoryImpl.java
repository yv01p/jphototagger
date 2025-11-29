package org.jphototagger.repository.sqlite;

import java.awt.Image;
import java.io.File;
import java.sql.Date;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jphototagger.api.applifecycle.generics.Functor;
import org.jphototagger.api.progress.ProgressListener;
import org.jphototagger.domain.image.ImageFile;
import org.jphototagger.domain.metadata.MetaDataValue;
import org.jphototagger.domain.metadata.exif.Exif;
import org.jphototagger.domain.metadata.xmp.FileXmp;
import org.jphototagger.domain.metadata.xmp.Xmp;
import org.jphototagger.domain.repository.ImageFilesRepository;
import org.jphototagger.domain.timeline.Timeline;
import org.openide.util.lookup.ServiceProvider;

/**
 * SQLite implementation of ImageFilesRepository.
 * Currently implements only the basic methods supported by SqliteImageFilesDatabase.
 * Other methods throw UnsupportedOperationException until fully implemented.
 */
@ServiceProvider(service = ImageFilesRepository.class, position = 200)
public final class SqliteImageFilesRepositoryImpl implements ImageFilesRepository {

    private SqliteImageFilesDatabase getDatabase() {
        return new SqliteImageFilesDatabase(SqliteRepositoryImpl.getConnectionFactory());
    }

    @Override
    public long getFileCount() {
        return getDatabase().getFileCount();
    }

    @Override
    public List<File> findAllImageFiles() {
        return getDatabase().getAllImageFiles();
    }

    @Override
    public void eachImage(Functor<File> functor) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public long findImageFilesLastModifiedTimestamp(File imageFile) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public long findImageFilesSizeInBytes(File file) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public long findXmpFilesLastModifiedTimestamp(File imageFile) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int deleteImageFiles(List<File> imageFiles) {
        int count = 0;
        for (File file : imageFiles) {
            count += getDatabase().deleteImageFile(file);
        }
        return count;
    }

    @Override
    public void deleteDcSubject(String dcSubject) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int deleteAbsentImageFiles(ProgressListener listener) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int deleteAbsentXmp(ProgressListener listener) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void deleteValueOfJoinedMetaDataValue(MetaDataValue mdValue, String value) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsImageFile(File imageFile) {
        return getDatabase().existsImageFile(imageFile);
    }

    @Override
    public boolean existsDcSubject(String dcSubject) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsExifDate(Date date) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsMetaDataValue(Object value, MetaDataValue mdValue) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsXMPDateCreated(String date) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Set<String> findAllDcSubjects() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Set<String> findAllDistinctMetaDataValues(MetaDataValue mdValue) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<String> findDcSubjectsOfImageFile(File imageFile) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Exif findExifOfImageFile(File imageFile) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Set<File> findImageFilesContainingAllDcSubjects(List<? extends String> dcSubjects) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Set<File> findImageFilesContainingDcSubject(String dcSubject, boolean includeSynonyms) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Set<File> findImageFilesContainingSomeOfDcSubjects(List<? extends String> dcSubjects) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Set<File> findImageFilesContainingAllWordsInMetaDataValue(List<? extends String> words, MetaDataValue mdValue) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<File> findImageFilesContainingAVauleInMetaDataValue(MetaDataValue mdValue) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Set<File> findImageFilesOfDateTaken(int year, int month, int day) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<File> findImageFilesOfUnknownDateTaken() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<File> findImageFilesWhereMetaDataValueHasExactValue(MetaDataValue mdValue, String exactValue) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<File> findImageFilesWithoutDataValue(MetaDataValue mdValue) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Set<String> findNotReferencedDcSubjects() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Timeline findTimeline() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Xmp findXmpOfImageFile(File imageFile) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<FileXmp> findXmpOfImageFiles(Collection<? extends File> imageFiles) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean saveDcSubject(String dcSubject) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean saveOrUpdateExif(File imageFile, Exif exif) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean saveOrUpdateImageFile(ImageFile imageFile) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean isDcSubjectReferenced(String dcSubject) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean setLastModifiedToXmpSidecarFileOfImageFile(File imageFile, long time) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean updateImageFile(ImageFile imageFile) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int updateRenameImageFile(File fromImageFile, File toImageFile) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean updateThumbnail(File imageFile, Image thumbnail) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Long findIdDcSubject(String dcSubject) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsXmpDcSubjectsLink(long idXmp, long idDcSubject) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int updateAllThumbnails(ProgressListener listener) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int updateRenameFilenamesStartingWith(String before, String after, ProgressListener progressListener) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean saveOrUpdateXmpOfImageFile(File imageFile, Xmp xmp) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsXmpForFile(File file) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int deleteXmpOfFile(File file) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String findXmpIptc4CoreDateCreated(File file) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public long findExifDateTimeOriginalTimestamp(File file) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int updateRenameAllDcSubjects(String fromName, String toName) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
