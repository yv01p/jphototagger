// Auto-provisioning: Gradle will automatically download the required JDK if not installed.
// Uses Foojay Disco API (https://github.com/foojayio/discoapi) to find and download JDKs.
// This ensures all developers use the exact same JDK version without manual installation.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "jphototagger"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Tier 0: No internal dependencies
include("API")
include("Localization")
include("TestSupport")
include("Benchmarks")

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
include("Repositories:SQLite")

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

include("Tools:MigrationTool")

// Tier 10: Main application
include("Program")
