plugins {
    java
    `jvm-test-suite`
}

allprojects {
    group = "org.jphototagger"
    version = "2.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jvm-test-suite")

    // Java toolchain ensures consistent JDK version across all environments.
    // To upgrade to a newer Java version, change the number below (e.g., 22, 23).
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    // NetBeans Lookup annotation processor for @ServiceProvider
    dependencies {
        "annotationProcessor"(files("${rootProject.projectDir}/Libraries/org-openide-util-lookup.jar"))
    }

    // Common test dependencies
    dependencies {
        // JUnit 5
        "testImplementation"(rootProject.libs.junit5.api)
        "testImplementation"(rootProject.libs.junit5.params)
        // Legacy JUnit 4 (for existing tests during migration)
        "testImplementation"(rootProject.libs.junit4)
        "testImplementation"(rootProject.libs.hamcrest)
        // AssertJ
        "testImplementation"(rootProject.libs.assertj)
        // Mockito
        "testImplementation"(rootProject.libs.mockito.core)
        "testImplementation"(rootProject.libs.mockito.junit5)
    }

    // Configure the default test suite to use JUnit 5 with Vintage engine
    @Suppress("UnstableApiUsage")
    testing {
        suites {
            val test by getting(JvmTestSuite::class) {
                useJUnitJupiter()
                dependencies {
                    // Vintage engine for existing JUnit 4 tests
                    runtimeOnly(rootProject.libs.junit5.vintage)
                }
            }
        }
    }

    tasks.test {
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
}

// Convenience task to build everything
tasks.register("buildAll") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}

// OS detection for jpackage
val currentOs: String = System.getProperty("os.name").lowercase().let { osName ->
    when {
        osName.contains("linux") -> "linux"
        osName.contains("windows") -> "windows"
        osName.contains("mac") || osName.contains("darwin") -> "macos"
        else -> error("Unsupported OS: $osName")
    }
}

// jpackage task for creating native app-images
tasks.register<Exec>("jpackage") {
    description = "Creates a native app-image using jpackage"
    group = "distribution"
    dependsOn(":Program:installDist")

    val jpackageVersion = project.findProperty("version")?.toString() ?: project.version.toString()
    val inputDir = project.file("Program/build/install/Program/lib")
    val outputDir = project.file("build/jpackage")
    val resourceDir = project.file("packaging")

    val mainJar = "Program-${project.version}.jar"
    val javaOptions = listOf(
        "-XX:+UseZGC",
        "-XX:+UseStringDeduplication",
        "-Xmx1g",
        "-Xms256m"
    )

    doFirst {
        outputDir.mkdirs()
        // Clean previous build
        project.file("$outputDir/JPhotoTagger").deleteRecursively()
        if (currentOs == "macos") {
            project.file("$outputDir/JPhotoTagger.app").deleteRecursively()
        }
    }

    workingDir(project.projectDir)

    val jpackageArgs = mutableListOf(
        "jpackage",
        "--type", "app-image",
        "--name", "JPhotoTagger",
        "--app-version", jpackageVersion.removePrefix("v"),
        "--vendor", "JPhotoTagger",
        "--description", "Photo metadata management application",
        "--input", inputDir.absolutePath,
        "--main-jar", mainJar,
        "--main-class", "org.jphototagger.program.Main",
        "--dest", outputDir.absolutePath
    )

    // Add Java options
    javaOptions.forEach { opt ->
        jpackageArgs.addAll(listOf("--java-options", opt))
    }

    // Add platform-specific icon if it exists
    val iconFile = when (currentOs) {
        "linux" -> resourceDir.resolve("JPhotoTagger.png")
        "windows" -> resourceDir.resolve("JPhotoTagger.ico")
        "macos" -> resourceDir.resolve("JPhotoTagger.icns")
        else -> null
    }
    if (iconFile?.exists() == true) {
        jpackageArgs.addAll(listOf("--icon", iconFile.absolutePath))
    }

    commandLine(jpackageArgs)

    doLast {
        val appDir = if (currentOs == "macos") {
            outputDir.resolve("JPhotoTagger.app")
        } else {
            outputDir.resolve("JPhotoTagger")
        }
        println("App-image created at: ${appDir.absolutePath}")
    }
}
