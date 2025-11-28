# Phase 1: Gradle + CI Infrastructure Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace NetBeans Ant build system with Gradle, establish GitHub Actions CI pipeline.

**Architecture:** Multi-project Gradle build with Kotlin DSL. Each existing NetBeans module becomes a Gradle subproject. Dependencies mapped from `nbproject/project.properties`. External JARs replaced with Maven Central coordinates where possible.

**Tech Stack:** Gradle 8.5+, Kotlin DSL, GitHub Actions, Java 7 (initially)

---

## Module Dependency Map

Based on analysis of all 34 `nbproject/project.properties` files:

### Tier 0: No internal dependencies
- `API` - depends only on `org-openide-util-lookup.jar`
- `Localization` - no dependencies

### Tier 1: Depends on Tier 0
- `Resources` - depends on `API`

### Tier 2: Depends on Tier 0-1
- `Lib` - depends on `API`, `Resources`
- `Domain` - depends on `API`, `Lib`, `Resources`

### Tier 3: Depends on Tier 0-2
- `Image` - depends on `API`, `Lib`, `Resources`
- `XMP` - depends on `API`, `Domain`, `Lib`, `Resources`
- `Exif` - depends on `API`, `Domain`, `Lib`, `Resources`
- `Iptc` - depends on `API`, `Domain`, `Lib`, `Resources`
- `LookAndFeels` - depends on `API`, `Lib`, `Resources`
- `KML` - depends on `Resources`

### Tier 4: Repository layer
- `Repositories/HSQLDB` - depends on `API`, `Domain`, `Image`, `Lib`, `Resources`, `XMP`

### Tier 5: Modules (plugins)
- `Modules/DisplayFilesWithoutMetaData`
- `Modules/Exif`
- `Modules/ExifToolXmpToImageWriter`
- `Modules/FileEventHooks`
- `Modules/FindDuplicates`
- `Modules/ImportFiles`
- `Modules/Iptc`
- `Modules/Maintainance`
- `Modules/RepositoryFileBrowser`
- `Modules/Synonyms`
- `Modules/UserDefinedFileFilters`
- `Modules/UserDefinedFileTypes`
- `Modules/Xmp`

### Tier 6: Plugins
- `Plugins/CopyFilenamesToClipboard`
- `Plugins/HtmlReports`
- `Plugins/IrfanViewSlideshow`

### Tier 7: Exporters/Importers
- `ExportersImporters/JPhotoTaggerExportersImporters`

### Tier 8: External commands
- `ExternalThumbnailCreationCommands/DefaultExternalThumbnailCreationCommands`

### Tier 9: User services
- `UserServices`

### Tier 10: Main application
- `Program` - depends on all modules

### Development support (not in main build)
- `DeveloperSupport/LibraryTest`
- `DeveloperSupport/scripts/JavaDeveloperSupport`

---

## External Dependencies (Maven Central Coordinates)

| Local JAR | Maven Central | Notes |
|-----------|---------------|-------|
| `hsqldb.jar` | `org.hsqldb:hsqldb:2.4.1` | |
| `metadata-extractor.jar` | `com.drewnoakes:metadata-extractor:2.6.4` | |
| `jgoodies-common.jar` | `com.jgoodies:jgoodies-common:1.6.0` | |
| `jgoodies-looks.jar` | `com.jgoodies:jgoodies-looks:2.5.3` | |
| `lucene-core.jar` | `org.apache.lucene:lucene-core:3.4.0` | |
| `jaxb-api.jar` | `javax.xml.bind:jaxb-api:2.2.11` | |
| `jaxb-core.jar` | `com.sun.xml.bind:jaxb-core:2.2.11` | |
| `jaxb-impl.jar` | `com.sun.xml.bind:jaxb-impl:2.2.11` | |
| `jaxb-activation.jar` | `javax.activation:activation:1.1.1` | |

### Local JARs (not on Maven Central)
| Local JAR | Notes |
|-----------|-------|
| `eventbus.jar` | EventBus 1.4 - keep as local JAR |
| `swingx-core.jar` | SwingX 1.6.2 - keep as local JAR |
| `beansbinding.jar` | BeansBinding 1.2.1 - keep as local JAR |
| `ImgrRdr.jar` | Imagero Reader - keep as local JAR |
| `org-openide-util-lookup.jar` | NetBeans Lookup - keep as local JAR |
| `mapdb.jar` | MapDB 0.9.9-SNAPSHOT - keep as local JAR |
| `XMPCore.jar` | Adobe XMP Core - keep as local JAR |

---

## Task 1: Create Gradle Wrapper

**Files:**
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradlew`
- Create: `gradlew.bat`

**Step 1: Generate Gradle wrapper**

Run (requires Gradle installed):
```bash
cd /home/yv01p/jphototagger
gradle wrapper --gradle-version 8.5
```

**Step 2: Verify wrapper created**

Run: `ls -la gradlew gradle/wrapper/`

Expected: `gradlew` executable, `gradle-wrapper.jar` and `gradle-wrapper.properties` exist

**Step 3: Test wrapper**

Run: `./gradlew --version`

Expected: Gradle 8.5 info displayed

**Step 4: Commit**

```bash
git add gradlew gradlew.bat gradle/
git commit -m "chore: add Gradle 8.5 wrapper"
```

---

## Task 2: Create Version Catalog

**Files:**
- Create: `gradle/libs.versions.toml`

**Step 1: Create version catalog file**

```toml
[versions]
java = "7"
hsqldb = "2.4.1"
metadata-extractor = "2.6.4"
jgoodies-common = "1.6.0"
jgoodies-looks = "2.5.3"
lucene = "3.4.0"
jaxb-api = "2.2.11"
jaxb-impl = "2.2.11"
activation = "1.1.1"
junit4 = "4.13.2"
hamcrest = "1.3"

[libraries]
hsqldb = { module = "org.hsqldb:hsqldb", version.ref = "hsqldb" }
metadata-extractor = { module = "com.drewnoakes:metadata-extractor", version.ref = "metadata-extractor" }
jgoodies-common = { module = "com.jgoodies:jgoodies-common", version.ref = "jgoodies-common" }
jgoodies-looks = { module = "com.jgoodies:jgoodies-looks", version.ref = "jgoodies-looks" }
lucene-core = { module = "org.apache.lucene:lucene-core", version.ref = "lucene" }
jaxb-api = { module = "javax.xml.bind:jaxb-api", version.ref = "jaxb-api" }
jaxb-core = { module = "com.sun.xml.bind:jaxb-core", version.ref = "jaxb-impl" }
jaxb-impl = { module = "com.sun.xml.bind:jaxb-impl", version.ref = "jaxb-impl" }
activation = { module = "javax.activation:activation", version.ref = "activation" }
junit4 = { module = "junit:junit", version.ref = "junit4" }
hamcrest = { module = "org.hamcrest:hamcrest-core", version.ref = "hamcrest" }

# Local JARs (referenced as file dependencies in build.gradle.kts)
# eventbus, swingx-core, beansbinding, ImgrRdr, org-openide-util-lookup, mapdb, XMPCore
```

**Step 2: Verify file created**

Run: `cat gradle/libs.versions.toml`

Expected: Contents match above

**Step 3: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "chore: add Gradle version catalog"
```

---

## Task 3: Create Root build.gradle.kts

**Files:**
- Create: `build.gradle.kts`

**Step 1: Create root build file**

```kotlin
plugins {
    java
}

allprojects {
    group = "org.jphototagger"
    version = "1.1.9"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_7
        targetCompatibility = JavaVersion.VERSION_1_7
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    // Common test configuration
    dependencies {
        "testImplementation"(rootProject.libs.junit4)
        "testImplementation"(rootProject.libs.hamcrest)
    }

    tasks.test {
        useJUnit()
    }
}

// Convenience task to build everything
tasks.register("buildAll") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}
```

**Step 2: Verify file created**

Run: `cat build.gradle.kts`

Expected: Contents match above

**Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "chore: add root build.gradle.kts"
```

---

## Task 4: Create settings.gradle.kts

**Files:**
- Create: `settings.gradle.kts`

**Step 1: Create settings file**

```kotlin
rootProject.name = "jphototagger"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Tier 0: No internal dependencies
include("API")
include("Localization")

// Tier 1: Depends on Tier 0
include("Resources")

// Tier 2: Depends on Tier 0-1
include("Lib")
include("Domain")

// Tier 3: Core libraries
include("Image")
include("XMP")
include("Exif")
include("Iptc")
include("LookAndFeels")
include("KML")

// Tier 4: Repository layer
include("Repositories:HSQLDB")

// Tier 5: Modules
include("Modules:DisplayFilesWithoutMetaData")
include("Modules:Exif")
project(":Modules:Exif").name = "ExifModule"
include("Modules:ExifToolXmpToImageWriter")
include("Modules:FileEventHooks")
include("Modules:FindDuplicates")
include("Modules:ImportFiles")
include("Modules:Iptc")
project(":Modules:Iptc").name = "IptcModule"
include("Modules:Maintainance")
include("Modules:RepositoryFileBrowser")
include("Modules:Synonyms")
include("Modules:UserDefinedFileFilters")
include("Modules:UserDefinedFileTypes")
include("Modules:Xmp")
project(":Modules:Xmp").name = "XmpModule"

// Tier 6: Plugins
include("Plugins:CopyFilenamesToClipboard")
include("Plugins:HtmlReports")
include("Plugins:IrfanViewSlideshow")

// Tier 7: Exporters/Importers
include("ExportersImporters:JPhotoTaggerExportersImporters")

// Tier 8: External thumbnail commands
include("ExternalThumbnailCreationCommands:DefaultExternalThumbnailCreationCommands")

// Tier 9: User services
include("UserServices")

// Tier 10: Main application
include("Program")
```

**Step 2: Verify file created**

Run: `cat settings.gradle.kts`

Expected: Contents match above

**Step 3: Commit**

```bash
git add settings.gradle.kts
git commit -m "chore: add settings.gradle.kts with all 34 modules"
```

---

## Task 5: Create API module build.gradle.kts

**Files:**
- Create: `API/build.gradle.kts`

**Step 1: Create API build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(files("../Libraries/org-openide-util-lookup.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :API:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add API/build.gradle.kts
git commit -m "chore: add API module Gradle build"
```

---

## Task 6: Create Localization module build.gradle.kts

**Files:**
- Create: `Localization/build.gradle.kts`

**Step 1: Create Localization build file**

```kotlin
plugins {
    `java-library`
}

// No dependencies - pure resource module

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Localization:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Localization/build.gradle.kts
git commit -m "chore: add Localization module Gradle build"
```

---

## Task 7: Create Resources module build.gradle.kts

**Files:**
- Create: `Resources/build.gradle.kts`

**Step 1: Create Resources build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/swingx-core.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Resources:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Resources/build.gradle.kts
git commit -m "chore: add Resources module Gradle build"
```

---

## Task 8: Create Lib module build.gradle.kts

**Files:**
- Create: `Lib/build.gradle.kts`

**Step 1: Create Lib build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Resources"))

    // Maven Central dependencies
    api(libs.lucene.core)
    api(libs.jaxb.api)
    api(libs.jaxb.core)
    api(libs.jaxb.impl)
    api(libs.activation)

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/swingx-core.jar"))
    api(files("../Libraries/beansbinding.jar"))
    api(files("../Libraries/eventbus.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Lib:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Lib/build.gradle.kts
git commit -m "chore: add Lib module Gradle build"
```

---

## Task 9: Create Domain module build.gradle.kts

**Files:**
- Create: `Domain/build.gradle.kts`

**Step 1: Create Domain build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/eventbus.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Domain:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Domain/build.gradle.kts
git commit -m "chore: add Domain module Gradle build"
```

---

## Task 10: Create Image module build.gradle.kts

**Files:**
- Create: `Image/build.gradle.kts`

**Step 1: Create Image build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Maven Central
    api(libs.metadata.extractor)

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/eventbus.jar"))
    api(files("../Libraries/ImgrRdr.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Image:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Image/build.gradle.kts
git commit -m "chore: add Image module Gradle build"
```

---

## Task 11: Create XMP module build.gradle.kts

**Files:**
- Create: `XMP/build.gradle.kts`

**Step 1: Create XMP build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Domain"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/eventbus.jar"))
    api(files("../Libraries/XMPCore.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :XMP:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add XMP/build.gradle.kts
git commit -m "chore: add XMP module Gradle build"
```

---

## Task 12: Create Exif module build.gradle.kts

**Files:**
- Create: `Exif/build.gradle.kts`

**Step 1: Create Exif build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Domain"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Maven Central
    api(libs.metadata.extractor)

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/eventbus.jar"))
    api(files("../Libraries/mapdb.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Exif:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Exif/build.gradle.kts
git commit -m "chore: add Exif module Gradle build"
```

---

## Task 13: Create Iptc module build.gradle.kts

**Files:**
- Create: `Iptc/build.gradle.kts`

**Step 1: Create Iptc build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Domain"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Maven Central
    api(libs.metadata.extractor)

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/eventbus.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Iptc:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Iptc/build.gradle.kts
git commit -m "chore: add Iptc module Gradle build"
```

---

## Task 14: Create LookAndFeels module build.gradle.kts

**Files:**
- Create: `LookAndFeels/build.gradle.kts`

**Step 1: Create LookAndFeels build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Maven Central
    api(libs.jgoodies.common)
    api(libs.jgoodies.looks)

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :LookAndFeels:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add LookAndFeels/build.gradle.kts
git commit -m "chore: add LookAndFeels module Gradle build"
```

---

## Task 15: Create KML module build.gradle.kts

**Files:**
- Create: `KML/build.gradle.kts`

**Step 1: Create KML build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":Resources"))

    // JAXB for KML file generation
    api(libs.jaxb.api)
    api(libs.jaxb.core)
    api(libs.jaxb.impl)
    api(libs.activation)
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :KML:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add KML/build.gradle.kts
git commit -m "chore: add KML module Gradle build"
```

---

## Task 16: Create HSQLDB Repository module build.gradle.kts

**Files:**
- Create: `Repositories/HSQLDB/build.gradle.kts`

**Step 1: Create HSQLDB build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Domain"))
    api(project(":Image"))
    api(project(":Lib"))
    api(project(":Resources"))
    api(project(":XMP"))

    // Maven Central
    api(libs.hsqldb)

    // Local JARs
    api(files("../../Libraries/org-openide-util-lookup.jar"))
    api(files("../../Libraries/eventbus.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Repositories:HSQLDB:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Repositories/HSQLDB/build.gradle.kts
git commit -m "chore: add HSQLDB Repository module Gradle build"
```

---

## Task 17: Create remaining Modules build.gradle.kts files

This task creates build files for all modules in the Modules/ directory.

**Files:**
- Create: `Modules/DisplayFilesWithoutMetaData/build.gradle.kts`
- Create: `Modules/Exif/build.gradle.kts`
- Create: `Modules/ExifToolXmpToImageWriter/build.gradle.kts`
- Create: `Modules/FileEventHooks/build.gradle.kts`
- Create: `Modules/FindDuplicates/build.gradle.kts`
- Create: `Modules/ImportFiles/build.gradle.kts`
- Create: `Modules/Iptc/build.gradle.kts`
- Create: `Modules/Maintainance/build.gradle.kts`
- Create: `Modules/RepositoryFileBrowser/build.gradle.kts`
- Create: `Modules/Synonyms/build.gradle.kts`
- Create: `Modules/UserDefinedFileFilters/build.gradle.kts`
- Create: `Modules/UserDefinedFileTypes/build.gradle.kts`
- Create: `Modules/Xmp/build.gradle.kts`

**Step 1: Create common module template**

Each module follows a similar pattern. Example for DisplayFilesWithoutMetaData:

```kotlin
plugins {
    `java-library`
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Lib"))
    implementation(project(":Resources"))

    implementation(files("../../Libraries/org-openide-util-lookup.jar"))
    implementation(files("../../Libraries/eventbus.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
```

**Step 2: Read each module's project.properties for specific dependencies**

For each module, check the `nbproject/project.properties` for additional dependencies beyond the common ones.

**Step 3: Create all 13 module build files**

Create each file with appropriate dependencies.

**Step 4: Test compilation**

Run: `./gradlew :Modules:DisplayFilesWithoutMetaData:compileJava`

Expected: BUILD SUCCESSFUL for all modules

**Step 5: Commit**

```bash
git add Modules/*/build.gradle.kts
git commit -m "chore: add all Modules Gradle builds"
```

---

## Task 18: Create Plugins build.gradle.kts files

**Files:**
- Create: `Plugins/CopyFilenamesToClipboard/build.gradle.kts`
- Create: `Plugins/HtmlReports/build.gradle.kts`
- Create: `Plugins/IrfanViewSlideshow/build.gradle.kts`

**Step 1: Create plugin build files**

Example for CopyFilenamesToClipboard:

```kotlin
plugins {
    `java-library`
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Lib"))
    implementation(project(":Resources"))

    implementation(files("../../Libraries/org-openide-util-lookup.jar"))
    implementation(files("../../Libraries/eventbus.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Plugins:CopyFilenamesToClipboard:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add Plugins/*/build.gradle.kts
git commit -m "chore: add Plugins Gradle builds"
```

---

## Task 19: Create ExportersImporters build.gradle.kts

**Files:**
- Create: `ExportersImporters/JPhotoTaggerExportersImporters/build.gradle.kts`

**Step 1: Create build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Lib"))
    implementation(project(":Resources"))
    implementation(project(":XMP"))

    implementation(libs.jaxb.api)
    implementation(libs.jaxb.core)
    implementation(libs.jaxb.impl)
    implementation(libs.activation)

    implementation(files("../../Libraries/org-openide-util-lookup.jar"))
    implementation(files("../../Libraries/eventbus.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :ExportersImporters:JPhotoTaggerExportersImporters:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add ExportersImporters/JPhotoTaggerExportersImporters/build.gradle.kts
git commit -m "chore: add ExportersImporters Gradle build"
```

---

## Task 20: Create ExternalThumbnailCreationCommands build.gradle.kts

**Files:**
- Create: `ExternalThumbnailCreationCommands/DefaultExternalThumbnailCreationCommands/build.gradle.kts`

**Step 1: Create build file**

```kotlin
plugins {
    `java-library`
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Lib"))
    implementation(project(":Resources"))

    implementation(files("../../Libraries/org-openide-util-lookup.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :ExternalThumbnailCreationCommands:DefaultExternalThumbnailCreationCommands:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add ExternalThumbnailCreationCommands/DefaultExternalThumbnailCreationCommands/build.gradle.kts
git commit -m "chore: add ExternalThumbnailCreationCommands Gradle build"
```

---

## Task 21: Create UserServices build.gradle.kts

**Files:**
- Create: `UserServices/build.gradle.kts`

**Step 1: Create build file**

```kotlin
plugins {
    `java-library`
}

// UserServices is an empty placeholder module for user extensions
// No dependencies needed

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :UserServices:compileJava`

Expected: BUILD SUCCESSFUL (or no sources to compile)

**Step 3: Commit**

```bash
git add UserServices/build.gradle.kts
git commit -m "chore: add UserServices Gradle build"
```

---

## Task 22: Create Program (main application) build.gradle.kts

**Files:**
- Create: `Program/build.gradle.kts`

**Step 1: Create Program build file**

```kotlin
plugins {
    `java-library`
    application
}

application {
    mainClass.set("org.jphototagger.program.Main")
}

dependencies {
    // All internal modules
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Exif"))
    implementation(project(":ExportersImporters:JPhotoTaggerExportersImporters"))
    implementation(project(":ExternalThumbnailCreationCommands:DefaultExternalThumbnailCreationCommands"))
    implementation(project(":Image"))
    implementation(project(":Iptc"))
    implementation(project(":KML"))
    implementation(project(":Lib"))
    implementation(project(":Localization"))
    implementation(project(":LookAndFeels"))
    implementation(project(":Modules:DisplayFilesWithoutMetaData"))
    implementation(project(":Modules:ExifModule"))
    implementation(project(":Modules:ExifToolXmpToImageWriter"))
    implementation(project(":Modules:FileEventHooks"))
    implementation(project(":Modules:FindDuplicates"))
    implementation(project(":Modules:ImportFiles"))
    implementation(project(":Modules:IptcModule"))
    implementation(project(":Modules:Maintainance"))
    implementation(project(":Modules:RepositoryFileBrowser"))
    implementation(project(":Modules:Synonyms"))
    implementation(project(":Modules:UserDefinedFileFilters"))
    implementation(project(":Modules:UserDefinedFileTypes"))
    implementation(project(":Modules:XmpModule"))
    implementation(project(":Plugins:CopyFilenamesToClipboard"))
    implementation(project(":Plugins:HtmlReports"))
    implementation(project(":Plugins:IrfanViewSlideshow"))
    implementation(project(":Repositories:HSQLDB"))
    implementation(project(":Resources"))
    implementation(project(":UserServices"))
    implementation(project(":XMP"))

    // Maven Central dependencies
    implementation(libs.hsqldb)
    implementation(libs.metadata.extractor)
    implementation(libs.jgoodies.common)
    implementation(libs.jgoodies.looks)
    implementation(libs.lucene.core)
    implementation(libs.jaxb.api)
    implementation(libs.jaxb.core)
    implementation(libs.jaxb.impl)
    implementation(libs.activation)

    // Local JARs
    implementation(files("../Libraries/org-openide-util-lookup.jar"))
    implementation(files("../Libraries/swingx-core.jar"))
    implementation(files("../Libraries/beansbinding.jar"))
    implementation(files("../Libraries/eventbus.jar"))
    implementation(files("../Libraries/ImgrRdr.jar"))
    implementation(files("../Libraries/mapdb.jar"))
    implementation(files("../Libraries/XMPCore.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.jphototagger.program.Main"
        )
    }
}
```

**Step 2: Test compilation**

Run: `./gradlew :Program:compileJava`

Expected: BUILD SUCCESSFUL

**Step 3: Test application run**

Run: `./gradlew :Program:run`

Expected: Application starts (may fail on headless server, that's OK)

**Step 4: Commit**

```bash
git add Program/build.gradle.kts
git commit -m "chore: add Program (main app) Gradle build"
```

---

## Task 23: Full Build Verification

**Step 1: Clean and build all**

Run: `./gradlew clean build`

Expected: BUILD SUCCESSFUL

**Step 2: Run tests**

Run: `./gradlew test`

Expected: Tests pass (or skip if none)

**Step 3: Verify JAR creation**

Run: `find . -name "*.jar" -path "*/build/libs/*" | head -10`

Expected: JAR files created for each module

**Step 4: Commit any fixes**

```bash
git add -A
git commit -m "fix: resolve build issues across all modules"
```

---

## Task 24: Create GitHub Actions CI Workflow

**Files:**
- Create: `.github/workflows/build.yml`

**Step 1: Create workflow file**

```yaml
name: Build

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 7
      uses: actions/setup-java@v4
      with:
        java-version: '7'
        distribution: 'zulu'

    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@v3

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build with Gradle
      run: ./gradlew build

    - name: Run tests
      run: ./gradlew test

    - name: Upload test results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: test-results
        path: '**/build/test-results/test/*.xml'
```

**Step 2: Create directory and verify**

Run: `mkdir -p .github/workflows && cat .github/workflows/build.yml`

Expected: File exists with correct content

**Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add GitHub Actions build workflow"
```

---

## Task 25: Update .gitignore for Gradle

**Files:**
- Modify: `.gitignore`

**Step 1: Add Gradle entries to .gitignore**

Append to `.gitignore`:

```
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
.project
.classpath
.settings/
```

**Step 2: Verify changes**

Run: `tail -20 .gitignore`

Expected: Gradle entries present

**Step 3: Commit**

```bash
git add .gitignore
git commit -m "chore: update .gitignore for Gradle"
```

---

## Task 26: Final Verification and Summary Commit

**Step 1: Full clean build**

Run: `./gradlew clean build`

Expected: BUILD SUCCESSFUL

**Step 2: Verify all modules compile**

Run: `./gradlew compileJava --info | grep "compileJava"`

Expected: All 34 modules shown as compiled

**Step 3: Create summary commit if needed**

```bash
git status
# If there are uncommitted changes:
git add -A
git commit -m "chore: complete Phase 1 Gradle migration"
```

**Step 4: Tag the milestone**

```bash
git tag -a v1.2.0-gradle -m "Phase 1: Gradle migration complete"
```

---

## Verification Checklist

After completing all tasks, verify:

- [ ] `./gradlew build` succeeds
- [ ] `./gradlew :Program:run` starts the application
- [ ] `./gradlew test` runs existing tests
- [ ] GitHub Actions workflow file exists
- [ ] All 34 modules have `build.gradle.kts` files
- [ ] NetBeans files remain (not deleted) for reference
