package org.jphototagger.exif;

import static org.assertj.core.api.Assertions.assertThat;

import org.jphototagger.lib.xml.bind.XmlObjectExporter;
import org.jphototagger.lib.xml.bind.XmlObjectImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests JAXB serialization of ExifTags.
 * These tests ensure round-trip serialization works correctly,
 * critical for the EXIF cache which stores XML in MapDB.
 */
class ExifTagsSerializationTest {

    @Nested
    @DisplayName("round-trip serialization")
    class RoundTrip {

        @Test
        @DisplayName("empty ExifTags serializes and deserializes correctly")
        void emptyExifTags() throws Exception {
            ExifTags original = new ExifTags();
            original.setLastModified(123456789L);

            String xml = XmlObjectExporter.marshal(original);
            ExifTags restored = XmlObjectImporter.unmarshal(xml, ExifTags.class);

            assertThat(restored.getLastModified()).isEqualTo(123456789L);
            assertThat(restored.getExifTags()).isEmpty();
            assertThat(restored.getGpsTags()).isEmpty();
            assertThat(restored.getMakerNoteTags()).isEmpty();
        }

        @Test
        @DisplayName("ExifTags with tags serializes and deserializes correctly")
        void exifTagsWithTags() throws Exception {
            ExifTags original = new ExifTags();
            original.setLastModified(987654321L);

            // Create EXIF tag using constructor with all parameters
            ExifTag cameraTag = new ExifTag(
                271,  // Make tag ID
                2,    // ASCII type
                1,    // value count
                0L,   // value offset
                "Canon".getBytes("UTF-8"),  // raw value
                "Canon",  // string value
                18761,  // byte order (little endian)
                "Make",  // name
                ExifIfd.EXIF
            );
            original.addExifTag(cameraTag);

            // Create GPS tag
            ExifTag gpsTag = new ExifTag(
                1,    // GPSLatitudeRef tag ID
                2,    // ASCII type
                1,    // value count
                0L,   // value offset
                "N".getBytes("UTF-8"),  // raw value
                "N",  // string value
                18761,  // byte order (little endian)
                "GPS Latitude Ref",  // name
                ExifIfd.GPS
            );
            original.addGpsTag(gpsTag);

            String xml = XmlObjectExporter.marshal(original);
            ExifTags restored = XmlObjectImporter.unmarshal(xml, ExifTags.class);

            assertThat(restored.getLastModified()).isEqualTo(987654321L);
            assertThat(restored.getExifTags()).hasSize(1);
            assertThat(restored.getGpsTags()).hasSize(1);

            ExifTag restoredCamera = restored.findExifTagByTagId(271);
            assertThat(restoredCamera).isNotNull();
            assertThat(restoredCamera.getStringValue()).isEqualTo("Canon");
        }

        @Test
        @DisplayName("ExifTags with maker note description preserved")
        void makerNoteDescription() throws Exception {
            ExifTags original = new ExifTags();
            original.setMakerNoteDescription("Nikon Type 3");

            String xml = XmlObjectExporter.marshal(original);
            ExifTags restored = XmlObjectImporter.unmarshal(xml, ExifTags.class);

            assertThat(restored.getMakerNoteDescription()).isEqualTo("Nikon Type 3");
        }
    }

    @Nested
    @DisplayName("XML format")
    class XmlFormat {

        @Test
        @DisplayName("produces valid UTF-8 encoded XML")
        void producesUtf8Xml() throws Exception {
            ExifTags original = new ExifTags();

            // Create tag with German umlaut
            ExifTag tag = new ExifTag(
                271,  // Make tag ID
                2,    // ASCII type
                1,    // value count
                0L,   // value offset
                "Kamera-Hersteller".getBytes("UTF-8"),  // raw value with umlaut
                "Nikon",  // string value
                18761,  // byte order (little endian)
                "Kamera-Hersteller",  // name with umlaut
                ExifIfd.EXIF
            );
            original.addExifTag(tag);

            String xml = XmlObjectExporter.marshal(original);

            assertThat(xml).contains("UTF-8");
            assertThat(xml).contains("Kamera-Hersteller");
        }
    }
}
