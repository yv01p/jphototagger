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
    useJUnitPlatform()
}
