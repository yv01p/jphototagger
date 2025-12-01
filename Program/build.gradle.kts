plugins {
    `java-library`
    application
}

application {
    mainClass.set("org.jphototagger.program.Main")
    applicationDefaultJvmArgs = listOf(
        "-XX:+UseZGC",
        "-XX:+UseStringDeduplication",
        "-Xmx1g",
        "-Xms256m"
    )
}

dependencies {
    // All internal modules
    implementation(project(":API"))
    implementation(project(":CacheDb"))
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
    implementation(libs.jgoodies.common)
    implementation(libs.jgoodies.looks)

    // Local JARs
    implementation(files("../Libraries/org-openide-util-lookup.jar"))
    implementation(files("../Libraries/swingx-core.jar"))
    implementation(files("../Libraries/beansbinding.jar"))
    implementation(files("../Libraries/eventbus.jar"))
    implementation(files("../Libraries/ImgrRdr.jar"))
    implementation(files("../Libraries/metadata-extractor.jar"))
    implementation(files("../Libraries/XMPCore.jar"))

    // Test dependencies
    testImplementation(project(":TestSupport"))
    testImplementation(libs.assertj.swing)
    testImplementation(libs.bundles.junit5)
    testImplementation(libs.assertj)
}

sourceSets {
    main {
        java {
            srcDirs("src")
            exclude("test/**")
        }
        resources {
            srcDirs("src")
            exclude("test/**")
        }
    }
    test {
        java.srcDirs("test", "src/test/java")
        resources.srcDirs("src/test/resources")
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Suppress specific warnings for legacy code
    options.compilerArgs.addAll(listOf("-Xlint:-unchecked,-rawtypes,-deprecation"))
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "org.jphototagger.program.Main"
        )
    }
}

tasks.test {
    // Enable tests for new JUnit 5 tests (non-GUI tests)
    // Exclude E2E tests - those run separately via e2eTest task
    useJUnitPlatform {
        excludeTags("e2e")
    }
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Test>("e2eTest") {
    description = "Runs E2E GUI tests (requires DISPLAY, use xvfb-run on Linux)"
    group = "verification"

    // Use isolated data directory for E2E tests to avoid:
    // 1. Lock file conflicts with running instances
    // 2. Modal dialogs blocking on stale lock files
    // 3. Database state conflicts between test runs
    val e2eDataDir = layout.buildDirectory.dir("e2e-data").get().asFile

    // Clean up test data directory before each run
    doFirst {
        if (e2eDataDir.exists()) {
            e2eDataDir.deleteRecursively()
        }
        e2eDataDir.mkdirs()
        logger.lifecycle("E2E tests using isolated data directory: ${e2eDataDir.absolutePath}")
    }

    // Skip if no DISPLAY available (prevents AWTError on headless systems)
    onlyIf {
        val display = System.getenv("DISPLAY")
        if (display.isNullOrBlank()) {
            logger.lifecycle("Skipping e2eTest: DISPLAY not set. Run with xvfb-run on Linux.")
            false
        } else {
            true
        }
    }

    useJUnitPlatform {
        includeTags("e2e")
    }

    failFast = true

    // E2E tests need more memory for GUI
    maxHeapSize = "512m"

    // Pass DISPLAY environment variable for Xvfb
    environment("DISPLAY", System.getenv("DISPLAY") ?: ":0")

    // Add JVM arguments for Java 21 module system compatibility with AssertJ Swing
    // Also set jpt.home to isolated test directory to avoid lock file and state conflicts
    jvmArgs(
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "--add-opens", "java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
        "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
        "-Djpt.home=${e2eDataDir.absolutePath}"
    )
}

tasks.register<Exec>("generateCdsArchive") {
    description = "Generates Class Data Sharing archive for faster startup"
    group = "distribution"
    dependsOn("jar")

    workingDir(rootProject.projectDir)
    commandLine("bash", "scripts/generate-cds-archive.sh")
}
