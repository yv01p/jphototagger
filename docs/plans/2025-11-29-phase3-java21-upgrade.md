# Phase 3: Java 21 Upgrade Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Upgrade JPhotoTagger from Java 8 to Java 21, migrating JAXB from javax to jakarta namespace, fixing Java version parsing, removing Lucene, and integrating FlatLaf for modern UI.

**Architecture:** The upgrade follows a staged approach: (1) change Java target version, (2) migrate JAXB imports, (3) fix version parsing for Java 9+ format, (4) replace Lucene with simple string search, (5) test SwingX compatibility, (6) add FlatLaf look-and-feel option.

**Tech Stack:** Java 21, Jakarta XML Binding 4.0, FlatLaf 3.4+, Gradle Kotlin DSL

---

## Summary of Changes

| Component | Files Affected | Change Type |
|-----------|----------------|-------------|
| Java version | `build.gradle.kts`, `gradle/libs.versions.toml` | Configuration |
| JAXB migration | 24 Java files | Import replacement |
| Version parsing | 2 files (`SystemUtil.java`, `Main.java`) | Code fix |
| Lucene removal | 1 file (`HelpSearch.java`) | Rewrite |
| FlatLaf integration | 2 new files in LookAndFeels module | New feature |

---

## Task 1: Update Java Version Configuration

**Files:**
- Modify: `gradle/libs.versions.toml:2`
- Modify: `build.gradle.kts:19-20`

**Step 1: Update version catalog**

In `gradle/libs.versions.toml`, change line 2:

```toml
# OLD
java = "7"

# NEW
java = "21"
```

And add Jakarta XML Binding and FlatLaf versions after line 10:

```toml
# After activation = "1.1.1"
jakarta-xml-bind = "4.0.2"
jakarta-activation = "2.1.3"
flatlaf = "3.4.1"
```

And add library definitions after line 27:

```toml
# After activation = { module = "javax.activation:activation", version.ref = "activation" }
jakarta-xml-bind-api = { module = "jakarta.xml.bind:jakarta.xml.bind-api", version.ref = "jakarta-xml-bind" }
jakarta-xml-bind-impl = { module = "org.glassfish.jaxb:jaxb-runtime", version.ref = "jakarta-xml-bind" }
jakarta-activation = { module = "jakarta.activation:jakarta.activation-api", version.ref = "jakarta-activation" }
flatlaf = { module = "com.formdev:flatlaf", version.ref = "flatlaf" }
```

**Step 2: Run test to verify build fails**

Run: `./gradlew build 2>&1 | head -50`
Expected: Build should fail with Java 21 incompatibility errors (JAXB imports)

**Step 3: Update root build.gradle.kts**

In `build.gradle.kts`, change lines 19-20:

```kotlin
// OLD
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// NEW
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

**Step 4: Commit configuration changes**

```bash
git add gradle/libs.versions.toml build.gradle.kts
git commit -m "chore: update Java target to 21, add Jakarta dependencies to version catalog"
```

---

## Task 2: Migrate JAXB Dependencies in Lib Module

**Files:**
- Modify: `Lib/build.gradle.kts:10-14`

**Step 1: Update Lib/build.gradle.kts dependencies**

Replace the old JAXB dependencies:

```kotlin
// OLD (lines 10-14)
    api(libs.jaxb.api)
    api(libs.jaxb.core)
    api(libs.jaxb.impl)
    api(libs.activation)

// NEW
    api(libs.jakarta.xml.bind.api)
    api(libs.jakarta.xml.bind.impl)
    api(libs.jakarta.activation)
```

**Step 2: Verify build still fails (import errors)**

Run: `./gradlew :Lib:compileJava 2>&1 | head -20`
Expected: FAIL with "package javax.xml.bind does not exist"

**Step 3: Commit dependency change**

```bash
git add Lib/build.gradle.kts
git commit -m "build(Lib): switch from javax.xml.bind to jakarta.xml.bind dependencies"
```

---

## Task 3: Migrate JAXB Imports in Lib Module (4 files)

**Files:**
- Modify: `Lib/src/org/jphototagger/lib/xml/bind/XmlObjectExporter.java:12-14`
- Modify: `Lib/src/org/jphototagger/lib/xml/bind/XmlObjectImporter.java:13-16`
- Modify: `Lib/src/org/jphototagger/lib/xml/bind/StringWrapper.java:6-8`
- Modify: `Lib/src/org/jphototagger/lib/xml/bind/Base64ByteStringXmlAdapter.java:3`
- Modify: `Lib/src/org/jphototagger/lib/xml/bind/Base64ByteArrayXmlAdapter.java:3`

**Step 1: Update XmlObjectExporter.java imports**

Replace lines 12-14:

```java
// OLD
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

// NEW
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
```

**Step 2: Update XmlObjectImporter.java imports**

Replace lines 13-16:

```java
// OLD
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

// NEW
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
```

**Step 3: Update StringWrapper.java imports**

Replace lines 6-8:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 4: Update Base64ByteStringXmlAdapter.java import**

Replace line 3:

```java
// OLD
import javax.xml.bind.annotation.adapters.XmlAdapter;

// NEW
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
```

**Step 5: Update Base64ByteArrayXmlAdapter.java import**

Replace line 3:

```java
// OLD
import javax.xml.bind.annotation.adapters.XmlAdapter;

// NEW
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
```

**Step 6: Verify Lib module compiles**

Run: `./gradlew :Lib:compileJava`
Expected: PASS (Lib module should now compile)

**Step 7: Commit Lib JAXB migration**

```bash
git add Lib/src/org/jphototagger/lib/xml/bind/
git commit -m "refactor(Lib): migrate JAXB imports from javax to jakarta namespace"
```

---

## Task 4: Migrate JAXB Imports in Domain Module (9 files)

**Files:**
- Modify: `Domain/src/org/jphototagger/domain/favorites/Favorite.java:4-6`
- Modify: `Domain/src/org/jphototagger/domain/metadata/exif/Exif.java:8`
- Modify: `Domain/src/org/jphototagger/domain/metadata/search/SavedSearch.java:8-12`
- Modify: `Domain/src/org/jphototagger/domain/metadata/search/SavedSearchPanel.java:3-5`
- Modify: `Domain/src/org/jphototagger/domain/filefilter/UserDefinedFileFilter.java:8-10`
- Modify: `Domain/src/org/jphototagger/domain/programs/Program.java:5-8`
- Modify: `Domain/src/org/jphototagger/domain/wordsets/Wordset.java:10-15`
- Modify: `Domain/src/org/jphototagger/domain/templates/RenameTemplate.java:3-6`
- Modify: `Domain/src/org/jphototagger/domain/filetypes/UserDefinedFileType.java:3-5`
- Modify: `Domain/src/org/jphototagger/domain/imagecollections/ImageCollection.java:11-15`

**Step 1: Update Favorite.java imports**

Replace lines 4-6:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 2: Update Exif.java import**

Replace line 8:

```java
// OLD
import javax.xml.bind.annotation.XmlTransient;

// NEW
import jakarta.xml.bind.annotation.XmlTransient;
```

**Step 3: Update SavedSearch.java imports**

Replace lines 8-12:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 4: Update SavedSearchPanel.java imports**

Replace lines 3-5:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 5: Update UserDefinedFileFilter.java imports**

Replace lines 8-10:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 6: Update Program.java imports**

Replace lines 5-8:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
```

**Step 7: Update Wordset.java imports**

Replace lines 10-15:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
```

**Step 8: Update RenameTemplate.java imports**

Replace lines 3-6:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
```

**Step 9: Update UserDefinedFileType.java imports**

Replace lines 3-5:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 10: Update ImageCollection.java imports**

Replace lines 11-15:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 11: Verify Domain module compiles**

Run: `./gradlew :Domain:compileJava`
Expected: PASS

**Step 12: Commit Domain JAXB migration**

```bash
git add Domain/src/
git commit -m "refactor(Domain): migrate JAXB imports from javax to jakarta namespace"
```

---

## Task 5: Migrate JAXB Imports in Exif Module (2 files)

**Files:**
- Modify: `Exif/src/org/jphototagger/exif/ExifTags.java:10-13`
- Modify: `Exif/src/org/jphototagger/exif/ExifTag.java:10-14,17`

**Step 1: Update ExifTags.java imports**

Replace lines 10-13:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
```

**Step 2: Update ExifTag.java imports**

Replace lines 10-14:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
```

**Step 3: Verify Exif module compiles**

Run: `./gradlew :Exif:compileJava`
Expected: PASS

**Step 4: Commit Exif JAXB migration**

```bash
git add Exif/src/
git commit -m "refactor(Exif): migrate JAXB imports from javax to jakarta namespace"
```

---

## Task 6: Migrate JAXB Imports in ExportersImporters Module (8 files)

**Files:**
- Modify: `ExportersImporters/JPhotoTaggerExportersImporters/src/org/jphototagger/eximport/jpt/exporter/ProgramsExporter.java:12-14`
- Modify: `ExportersImporters/JPhotoTaggerExportersImporters/src/org/jphototagger/eximport/jpt/exporter/SavedSearchesExporter.java:12-14`
- Modify: `ExportersImporters/JPhotoTaggerExportersImporters/src/org/jphototagger/eximport/jpt/exporter/FileExcludePatternsExporter.java:12-14`
- Modify: `ExportersImporters/JPhotoTaggerExportersImporters/src/org/jphototagger/eximport/jpt/exporter/AutoscanDirectoriesExporter.java:12-14`
- Modify: `ExportersImporters/JPhotoTaggerExportersImporters/src/org/jphototagger/eximport/jpt/exporter/WordsetsExporter.java:12-14`
- Modify: `ExportersImporters/JPhotoTaggerExportersImporters/src/org/jphototagger/eximport/jpt/exporter/RenameTemplatesExporter.java:13-15`
- Modify: `ExportersImporters/JPhotoTaggerExportersImporters/src/org/jphototagger/eximport/jpt/exporter/FavoritesExporter.java:12-14`
- Modify: `ExportersImporters/JPhotoTaggerExportersImporters/src/org/jphototagger/eximport/jpt/exporter/UserDefinedFileTypesExporter.java:12-14`
- Modify: `ExportersImporters/JPhotoTaggerExportersImporters/src/org/jphototagger/eximport/jpt/exporter/UserDefinedFileFilterExporter.java:13-15`

**Step 1: Update all exporter files**

For each file, replace the JAXB imports with jakarta equivalents. The pattern is the same for all:

```java
// OLD
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 2: Verify ExportersImporters module compiles**

Run: `./gradlew :ExportersImporters:JPhotoTaggerExportersImporters:compileJava`
Expected: PASS

**Step 3: Commit ExportersImporters JAXB migration**

```bash
git add ExportersImporters/
git commit -m "refactor(ExportersImporters): migrate JAXB imports from javax to jakarta namespace"
```

---

## Task 7: Migrate JAXB Imports in Remaining Modules (4 files)

**Files:**
- Modify: `Modules/Maintainance/src/org/jphototagger/maintainance/browse/SqlCommand.java:3-5`
- Modify: `Modules/Maintainance/src/org/jphototagger/maintainance/browse/SqlCommands.java:5-9`
- Modify: `Modules/ImportFiles/src/org/jphototagger/importfiles/subdircreators/templates/SubdirectoryTemplate.java:3-5`
- Modify: `Modules/ImportFiles/src/org/jphototagger/importfiles/subdircreators/templates/SubdirectoryTemplates.java:5-9`
- Modify: `Program/src/org/jphototagger/program/module/exportimport/exporter/ImageCollectionsExporter.java:12-14`

**Step 1: Update SqlCommand.java imports**

Replace lines 3-5:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
```

**Step 2: Update SqlCommands.java imports**

Replace lines 5-9:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 3: Update SubdirectoryTemplate.java imports**

Replace lines 3-5:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
```

**Step 4: Update SubdirectoryTemplates.java imports**

Replace lines 5-9:

```java
// OLD
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 5: Update ImageCollectionsExporter.java imports**

Replace lines 12-14:

```java
// OLD
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

// NEW
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
```

**Step 6: Verify all modules compile**

Run: `./gradlew compileJava`
Expected: PASS (all modules should compile with Jakarta JAXB)

**Step 7: Commit remaining JAXB migrations**

```bash
git add Modules/ Program/
git commit -m "refactor(Modules,Program): migrate JAXB imports from javax to jakarta namespace"
```

---

## Task 8: Fix Java Version Parsing for Java 9+

**Files:**
- Modify: `Lib/src/org/jphototagger/lib/util/SystemUtil.java:18-35`
- Modify: `Program/src/org/jphototagger/program/Main.java:17-18,28-50`

**Step 1: Write the failing test**

Create test file `Lib/test/org/jphototagger/lib/util/SystemUtilTest.java`:

```java
package org.jphototagger.lib.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SystemUtilTest {

    @ParameterizedTest
    @CsvSource({
        "1.7.0_80, 1, 7",
        "1.8.0_292, 1, 8",
        "9.0.4, 9, 0",
        "11.0.11, 11, 0",
        "17.0.1, 17, 0",
        "21, 21, 0",
        "21.0.1, 21, 0"
    })
    void parseJavaVersion_handlesAllFormats(String versionString, int expectedMajor, int expectedMinor) {
        Version version = SystemUtil.parseJavaVersion(versionString);

        assertThat(version).isNotNull();
        assertThat(version.getMajor()).isEqualTo(expectedMajor);
        assertThat(version.getMinor1()).isEqualTo(expectedMinor);
    }

    @Test
    void getJavaVersion_returnsNonNull() {
        Version version = SystemUtil.getJavaVersion();
        assertThat(version).isNotNull();
        assertThat(version.getMajor()).isGreaterThanOrEqualTo(1);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :Lib:test --tests "SystemUtilTest" -i`
Expected: FAIL with "parseJavaVersion method not found" or incorrect parsing

**Step 3: Update SystemUtil.java with new version parsing**

Replace the `getJavaVersion()` method (lines 18-35) with:

```java
    /**
     * Returns the Version of the JVM.
     *
     * @return Version or null if not found
     */
    public static Version getJavaVersion() {
        String versionProperty = System.getProperty("java.version");
        return parseJavaVersion(versionProperty);
    }

    /**
     * Parses a Java version string into a Version object.
     * Handles both pre-Java 9 format (1.7.0_80) and Java 9+ format (21.0.1).
     *
     * @param versionString the version string to parse
     * @return Version or null if parsing fails
     */
    public static Version parseJavaVersion(String versionString) {
        if (versionString == null || versionString.isEmpty()) {
            return null;
        }

        try {
            // Remove any suffix like "-ea" or "+build"
            String cleanVersion = versionString.split("[-+]")[0];

            // Handle Java 9+ format: "21" or "21.0.1"
            // Handle pre-Java 9 format: "1.8.0_292"
            String[] parts = cleanVersion.split("[._]");

            if (parts.length == 0) {
                return null;
            }

            int major = Integer.parseInt(parts[0]);

            // For pre-Java 9: "1.8.0" -> major=1, but we want major=8 semantically
            // However, the existing code expects major=1, minor=8 for Java 8
            // So we keep the literal parsing

            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            return new Version(major, minor, patch);
        } catch (NumberFormatException e) {
            Logger.getLogger(SystemUtil.class.getName()).log(Level.SEVERE,
                "Failed to parse Java version: " + versionString, e);
            return null;
        }
    }
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :Lib:test --tests "SystemUtilTest" -i`
Expected: PASS

**Step 5: Update Main.java version check**

Replace `Main.java` lines 17-18 and method `checkJavaVersion()`:

```java
    // For Java 21+, we only need major version >= 21
    private static final int MIN_JAVA_MAJOR_VERSION = 21;
```

And update `checkJavaVersion()` method (lines 28-50):

```java
    private static boolean checkJavaVersion() {
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.info("Checking Java version");
        String version = System.getProperty("java.version");

        try {
            // Handle both "1.8.0_292" and "21.0.1" formats
            String cleanVersion = version.split("[-+]")[0];
            String[] parts = cleanVersion.split("[._]");

            if (parts.length == 0) {
                logger.log(Level.SEVERE, "Can''t get valid Java Version! Got: ''{0}''", version);
                return true; // Allow to proceed on parse failure
            }

            int major = Integer.parseInt(parts[0]);

            // For Java 9+, the major version is the first number (e.g., "21" -> 21)
            // For Java 8 and earlier, format is "1.x" (e.g., "1.8" -> we check second number)
            int effectiveMajor = (major == 1 && parts.length > 1)
                ? Integer.parseInt(parts[1])
                : major;

            if (effectiveMajor < MIN_JAVA_MAJOR_VERSION) {
                errorMessageJavaVersion(version);
                return false;
            }
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Failed to parse Java version: " + version, e);
            return true; // Allow to proceed on parse failure
        }

        return true;
    }
```

And update `errorMessageJavaVersion()`:

```java
    private static void errorMessageJavaVersion(final String version) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE,
            "Java version ''{0}'' is too old! The required minimum Java version is ''{1}''.",
            new Object[]{version, MIN_JAVA_MAJOR_VERSION});
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/jphototagger/program/Bundle");
                String message = MessageFormat.format(
                    "Java version {0} is too old. JPhotoTagger requires Java {1} or newer.",
                    version, MIN_JAVA_MAJOR_VERSION);
                String title = bundle.getString("Main.Error.JavaVersion.MessageTitle");
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }
```

**Step 6: Commit version parsing fix**

```bash
git add Lib/src/org/jphototagger/lib/util/SystemUtil.java Lib/test/org/jphototagger/lib/util/SystemUtilTest.java Program/src/org/jphototagger/program/Main.java
git commit -m "fix: update Java version parsing to handle Java 9+ format (21.0.1)"
```

---

## Task 9: Replace Lucene with Simple String Search

**Files:**
- Modify: `Lib/build.gradle.kts:10` (remove lucene dependency)
- Modify: `gradle/libs.versions.toml` (remove lucene entries)
- Rewrite: `Lib/src/org/jphototagger/lib/help/HelpSearch.java`

**Step 1: Write the failing test for new search**

Create test file `Lib/test/org/jphototagger/lib/help/HelpSearchTest.java`:

```java
package org.jphototagger.lib.help;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HelpSearchTest {

    private HelpSearch helpSearch;
    private HelpNode rootNode;

    @BeforeEach
    void setUp() {
        rootNode = createTestHelpTree();
        helpSearch = new HelpSearch(rootNode);
        helpSearch.startIndexing();
    }

    @Test
    void findHelpPagesMatching_findsExactMatch() {
        List<HelpPage> results = helpSearch.findHelpPagesMatching("keyword");
        assertThat(results).isNotEmpty();
    }

    @Test
    void findHelpPagesMatching_caseInsensitive() {
        List<HelpPage> results = helpSearch.findHelpPagesMatching("KEYWORD");
        assertThat(results).isNotEmpty();
    }

    @Test
    void findHelpPagesMatching_multipleTerms() {
        List<HelpPage> results = helpSearch.findHelpPagesMatching("keyword search");
        assertThat(results).isNotEmpty();
    }

    @Test
    void findHelpPagesMatching_noResults() {
        List<HelpPage> results = helpSearch.findHelpPagesMatching("xyznonexistent");
        assertThat(results).isEmpty();
    }

    private HelpNode createTestHelpTree() {
        // Create minimal test structure
        HelpNode root = new HelpNode();
        root.setTitle("Root");
        // Add test pages with content containing "keyword"
        return root;
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :Lib:test --tests "HelpSearchTest" -i`
Expected: May fail or pass depending on implementation

**Step 3: Remove Lucene from build.gradle.kts**

In `Lib/build.gradle.kts`, remove line 10:

```kotlin
// REMOVE THIS LINE
    api(libs.lucene.core)
```

**Step 4: Remove Lucene from version catalog**

In `gradle/libs.versions.toml`, remove:
- Line 7: `lucene = "3.4.0"`
- Line 23: `lucene-core = { module = "org.apache.lucene:lucene-core", version.ref = "lucene" }`

**Step 5: Rewrite HelpSearch.java without Lucene**

Replace entire content of `Lib/src/org/jphototagger/lib/help/HelpSearch.java`:

```java
package org.jphototagger.lib.help;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jphototagger.lib.util.StringUtil;

/**
 * Simple string-based help search (replaces Lucene-based implementation).
 *
 * @author Elmar Baumann
 */
final class HelpSearch {

    private final HelpNode rootNode;
    private final List<IndexedPage> indexedPages = new ArrayList<>();
    private boolean indexed = false;

    HelpSearch(HelpNode rootNode) {
        if (rootNode == null) {
            throw new NullPointerException("rootNode == null");
        }
        this.rootNode = rootNode;
    }

    void startIndexing() {
        if (indexed) {
            return;
        }

        try {
            Collection<HelpPage> helpPages = HelpUtil.findHelpPagesRecursive(rootNode);
            for (HelpPage helpPage : helpPages) {
                indexPage(helpPage);
            }
            indexed = true;
        } catch (Throwable t) {
            Logger.getLogger(HelpSearch.class.getName()).log(Level.SEVERE, null, t);
        }
    }

    private void indexPage(HelpPage helpPage) {
        try {
            String content = getHelpPageContentAsString(helpPage);
            String normalizedContent = normalizeForSearch(content);
            String normalizedTitle = normalizeForSearch(
                StringUtil.emptyStringIfNull(helpPage.getTitle()));

            indexedPages.add(new IndexedPage(
                helpPage.getUrl(),
                helpPage.getTitle(),
                normalizedTitle + " " + normalizedContent
            ));
        } catch (IOException e) {
            Logger.getLogger(HelpSearch.class.getName()).log(Level.WARNING,
                "Failed to index help page: " + helpPage.getUrl(), e);
        }
    }

    private String getHelpPageContentAsString(HelpPage helpPage) throws IOException {
        InputStream helpPageContent = HelpUtil.class.getResourceAsStream(helpPage.getUrl());
        if (helpPageContent == null) {
            return "";
        }
        String content = StringUtil.convertStreamToString(helpPageContent, "UTF-8");
        return removeHtmlTags(content);
    }

    private String removeHtmlTags(String stringWithHtmlTags) {
        String result = stringWithHtmlTags;
        result = result.replaceAll("<[^>]*>", " ");
        result = result.replaceAll("&nbsp;", " ");
        result = result.replaceAll("&amp;", "&");
        result = result.replaceAll("&quot;", "\"");
        result = result.replaceAll("&lt;", "<");
        result = result.replaceAll("&gt;", ">");
        result = result.replaceAll("\\s+", " ");
        return result.trim();
    }

    private String normalizeForSearch(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    List<HelpPage> findHelpPagesMatching(String queryString) {
        if (queryString == null) {
            throw new NullPointerException("queryString == null");
        }

        if (!indexed) {
            throw new IllegalStateException("startIndexing was not called");
        }

        String normalizedQuery = normalizeForSearch(queryString.trim());
        if (normalizedQuery.isEmpty()) {
            return Collections.emptyList();
        }

        // Split query into terms for AND matching
        String[] queryTerms = normalizedQuery.split("\\s+");

        List<HelpPage> matchingPages = new ArrayList<>();

        for (IndexedPage page : indexedPages) {
            if (matchesAllTerms(page.normalizedContent, queryTerms)) {
                HelpPage helpPage = new HelpPage();
                helpPage.setUrl(page.url);
                helpPage.setTitle(page.title);
                matchingPages.add(helpPage);
            }
        }

        return matchingPages;
    }

    private boolean matchesAllTerms(String content, String[] terms) {
        for (String term : terms) {
            if (!content.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private static class IndexedPage {
        final String url;
        final String title;
        final String normalizedContent;

        IndexedPage(String url, String title, String normalizedContent) {
            this.url = url;
            this.title = title;
            this.normalizedContent = normalizedContent;
        }
    }
}
```

**Step 6: Verify Lib module compiles and tests pass**

Run: `./gradlew :Lib:compileJava :Lib:test`
Expected: PASS

**Step 7: Commit Lucene removal**

```bash
git add gradle/libs.versions.toml Lib/build.gradle.kts Lib/src/org/jphototagger/lib/help/HelpSearch.java Lib/test/org/jphototagger/lib/help/HelpSearchTest.java
git commit -m "refactor(Lib): replace Lucene with simple string-based help search"
```

---

## Task 10: Add FlatLaf Look and Feel Provider

**Files:**
- Modify: `LookAndFeels/build.gradle.kts` (add flatlaf dependency)
- Create: `LookAndFeels/src/org/jphototagger/laf/flatlaf/FlatLafLightLookAndFeelProvider.java`
- Create: `LookAndFeels/src/org/jphototagger/laf/flatlaf/FlatLafDarkLookAndFeelProvider.java`
- Create: `LookAndFeels/src/org/jphototagger/laf/flatlaf/META-INF/services/org.jphototagger.api.windows.LookAndFeelProvider`

**Step 1: Add FlatLaf dependency**

In `LookAndFeels/build.gradle.kts`, add after line 12:

```kotlin
    // FlatLaf modern look and feel
    api(libs.flatlaf)
```

**Step 2: Create FlatLafLightLookAndFeelProvider.java**

Create file `LookAndFeels/src/org/jphototagger/laf/flatlaf/FlatLafLightLookAndFeelProvider.java`:

```java
package org.jphototagger.laf.flatlaf;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import org.jphototagger.api.windows.LookAndFeelProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 * FlatLaf Light look and feel provider.
 */
@ServiceProvider(service = LookAndFeelProvider.class)
public final class FlatLafLightLookAndFeelProvider implements LookAndFeelProvider {

    private static final Logger LOGGER = Logger.getLogger(FlatLafLightLookAndFeelProvider.class.getName());

    @Override
    public String getDisplayname() {
        return "FlatLaf Light";
    }

    @Override
    public String getDescription() {
        return "Modern flat light theme";
    }

    @Override
    public Component getPreferencesComponent() {
        return null;
    }

    @Override
    public String getPreferencesKey() {
        return "FlatLafLight";
    }

    @Override
    public boolean canInstall() {
        return true;
    }

    @Override
    public void setLookAndFeel() {
        LOGGER.info("Setting FlatLaf Light Look and Feel");
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
    }

    @Override
    public int getPosition() {
        return 100; // Position in the list of available LaFs
    }
}
```

**Step 3: Create FlatLafDarkLookAndFeelProvider.java**

Create file `LookAndFeels/src/org/jphototagger/laf/flatlaf/FlatLafDarkLookAndFeelProvider.java`:

```java
package org.jphototagger.laf.flatlaf;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import org.jphototagger.api.windows.LookAndFeelProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 * FlatLaf Dark look and feel provider.
 */
@ServiceProvider(service = LookAndFeelProvider.class)
public final class FlatLafDarkLookAndFeelProvider implements LookAndFeelProvider {

    private static final Logger LOGGER = Logger.getLogger(FlatLafDarkLookAndFeelProvider.class.getName());

    @Override
    public String getDisplayname() {
        return "FlatLaf Dark";
    }

    @Override
    public String getDescription() {
        return "Modern flat dark theme";
    }

    @Override
    public Component getPreferencesComponent() {
        return null;
    }

    @Override
    public String getPreferencesKey() {
        return "FlatLafDark";
    }

    @Override
    public boolean canInstall() {
        return true;
    }

    @Override
    public void setLookAndFeel() {
        LOGGER.info("Setting FlatLaf Dark Look and Feel");
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
    }

    @Override
    public int getPosition() {
        return 101; // Position in the list of available LaFs
    }
}
```

**Step 4: Create service provider file**

Create directory and file `LookAndFeels/src/META-INF/services/org.jphototagger.api.windows.LookAndFeelProvider`:

```
org.jphototagger.laf.flatlaf.FlatLafLightLookAndFeelProvider
org.jphototagger.laf.flatlaf.FlatLafDarkLookAndFeelProvider
```

**Step 5: Verify LookAndFeels module compiles**

Run: `./gradlew :LookAndFeels:compileJava`
Expected: PASS

**Step 6: Commit FlatLaf integration**

```bash
git add LookAndFeels/
git commit -m "feat(LookAndFeels): add FlatLaf light and dark themes"
```

---

## Task 11: Test SwingX Compatibility with Java 21

**Files:**
- No file changes (manual testing)

**Step 1: Build the entire project**

Run: `./gradlew build`
Expected: PASS

**Step 2: Run all tests**

Run: `./gradlew test`
Expected: PASS

**Step 3: Launch the application**

Run: `./gradlew run`
Expected: Application should launch without crashes

**Step 4: Document SwingX compatibility**

Test the following UI components manually:
- [ ] JXList displays items correctly
- [ ] JXTree displays and expands correctly
- [ ] JXLabel renders correctly
- [ ] JXBusyLabel shows busy indicator
- [ ] List filtering works (ListTextFilter)
- [ ] Tree searching works (SearchInJxTreeAction)
- [ ] Keyword highlighting works (KeywordHighlightPredicate)

**Step 5: Create compatibility report**

Create file `docs/phase3-swingx-compatibility.md`:

```markdown
# SwingX Compatibility Report (Java 21)

**Date:** YYYY-MM-DD
**Java Version:** 21.0.x
**SwingX Version:** (from Libraries/swingx-core.jar)

## Test Results

| Component | Status | Notes |
|-----------|--------|-------|
| JXList | PASS/FAIL | |
| JXTree | PASS/FAIL | |
| JXLabel | PASS/FAIL | |
| JXBusyLabel | PASS/FAIL | |
| Highlighters | PASS/FAIL | |

## Issues Found

(Document any issues here)

## Conclusion

SwingX is [compatible/incompatible] with Java 21.
```

**Step 6: Commit compatibility report**

```bash
git add docs/phase3-swingx-compatibility.md
git commit -m "docs: add SwingX Java 21 compatibility report"
```

---

## Task 12: Run Full Test Suite and Benchmarks

**Files:**
- No file changes

**Step 1: Run full test suite**

Run: `./gradlew test`
Expected: All tests PASS

**Step 2: Run benchmarks and compare to Phase 2 baseline**

Run: `./gradlew :Benchmarks:jmh`

**Step 3: Run startup benchmark**

Run: `./gradlew :Benchmarks:run`

**Step 4: Document Phase 3 results**

Create `docs/benchmarks/phase3-results.md`:

```markdown
# Phase 3 Benchmark Results

**Date:** YYYY-MM-DD
**Java Version:** 21
**Commit:** (git sha)

## JMH Benchmarks

(Copy results from Benchmarks/build/reports/jmh/results.json)

## Startup Time

(Copy results from startup benchmark)

## Comparison with Phase 2 Baseline

| Benchmark | Phase 2 | Phase 3 | Change |
|-----------|---------|---------|--------|
| StartupBenchmark | X ms | Y ms | +/-% |
| ThumbnailGeneration | X ms | Y ms | +/-% |
| FolderLoad | X ms | Y ms | +/-% |
```

**Step 5: Commit benchmark results**

```bash
git add docs/benchmarks/phase3-results.md
git commit -m "docs: add Phase 3 benchmark results"
```

---

## Task 13: Final Verification and Cleanup

**Files:**
- Modify: `gradle/libs.versions.toml` (remove old JAXB entries)

**Step 1: Remove deprecated dependencies from version catalog**

In `gradle/libs.versions.toml`, remove these lines:
- `jaxb-api = "2.2.11"`
- `jaxb-impl = "2.2.11"`
- `activation = "1.1.1"`
- `jaxb-api = { module = "javax.xml.bind:jaxb-api", version.ref = "jaxb-api" }`
- `jaxb-core = { module = "com.sun.xml.bind:jaxb-core", version.ref = "jaxb-impl" }`
- `jaxb-impl = { module = "com.sun.xml.bind:jaxb-impl", version.ref = "jaxb-impl" }`
- `activation = { module = "javax.activation:activation", version.ref = "activation" }`

**Step 2: Verify build still works**

Run: `./gradlew clean build`
Expected: PASS

**Step 3: Run the application one final time**

Run: `./gradlew run`
Expected: Application launches and works correctly

**Step 4: Create Phase 3 completion commit**

```bash
git add gradle/libs.versions.toml
git commit -m "chore: remove deprecated javax JAXB dependencies from version catalog"
```

---

## Phase 3 Deliverables Checklist

- [ ] App running on Java 21
- [ ] JAXB migrated to Jakarta XML Binding (24 files)
- [ ] Java version parsing fixed for Java 9+ format
- [ ] Lucene removed, simple string search in place
- [ ] SwingX compatibility documented
- [ ] FlatLaf integrated (light and dark themes)
- [ ] All tests passing
- [ ] Benchmarks compared to Phase 2 baseline
