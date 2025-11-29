package org.jphototagger.benchmarks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.jphototagger.exif.ExifIfd;
import org.jphototagger.exif.ExifTag;
import org.jphototagger.exif.ExifTags;
import org.jphototagger.lib.xml.bind.XmlObjectExporter;
import org.jphototagger.lib.xml.bind.XmlObjectImporter;

/**
 * Test harness for EXIF cache benchmarking.
 * Simulates ExifCache behavior without requiring MapDB initialization.
 */
public final class ExifCacheTestHarness {

    private final Map<String, String> cache = new HashMap<>();

    private ExifCacheTestHarness() {
    }

    /**
     * Creates an empty EXIF cache for testing.
     */
    public static ExifCacheTestHarness create() {
        return new ExifCacheTestHarness();
    }

    /**
     * Generates sample ExifTags objects for testing.
     */
    public static ExifTags[] generateSampleTags(int count) {
        ExifTags[] tags = new ExifTags[count];
        for (int i = 0; i < count; i++) {
            ExifTags exifTags = new ExifTags();
            exifTags.setLastModified(System.currentTimeMillis() - i * 1000);

            // Create Make tag (tag ID 271)
            String makeValue = "Camera" + (i % 5);
            ExifTag makeTag = new ExifTag(
                271,  // tagId
                2,    // ASCII type
                makeValue.length() + 1,  // valueCount (string length + null terminator)
                0,    // valueOffset
                makeValue.getBytes(),  // rawValue
                makeValue,  // stringValue
                18761,  // little endian
                "Make",  // name
                ExifIfd.EXIF
            );
            exifTags.addExifTag(makeTag);

            // Create Model tag (tag ID 272)
            String modelValue = "Model" + i;
            ExifTag modelTag = new ExifTag(
                272,  // tagId
                2,    // ASCII type
                modelValue.length() + 1,  // valueCount
                0,    // valueOffset
                modelValue.getBytes(),  // rawValue
                modelValue,  // stringValue
                18761,  // little endian
                "Model",  // name
                ExifIfd.EXIF
            );
            exifTags.addExifTag(modelTag);

            // Create ISO tag (tag ID 34855)
            String isoValue = String.valueOf(100 * (i % 32 + 1));
            ExifTag isoTag = new ExifTag(
                34855,  // tagId
                3,      // SHORT type
                1,      // valueCount
                0,      // valueOffset
                new byte[]{(byte)(100 * (i % 32 + 1))},  // rawValue
                isoValue,  // stringValue
                18761,  // little endian
                "ISO Speed Ratings",  // name
                ExifIfd.EXIF
            );
            exifTags.addExifTag(isoTag);

            tags[i] = exifTags;
        }
        return tags;
    }

    public void cacheExifTags(File imageFile, ExifTags exifTags) {
        try {
            String xml = XmlObjectExporter.marshal(exifTags);
            cache.put(imageFile.getAbsolutePath(), xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExifTags getCachedExifTags(File imageFile) {
        String xml = cache.get(imageFile.getAbsolutePath());
        if (xml == null) {
            return null;
        }
        try {
            return XmlObjectImporter.unmarshal(xml, ExifTags.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean containsUpToDateExifTags(File imageFile) {
        String xml = cache.get(imageFile.getAbsolutePath());
        if (xml == null) {
            return false;
        }
        try {
            ExifTags tags = XmlObjectImporter.unmarshal(xml, ExifTags.class);
            // In real code, compares with file.lastModified()
            return tags.getLastModified() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void clear() {
        cache.clear();
    }

    public void close() {
        cache.clear();
    }
}
