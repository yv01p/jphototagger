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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    // Common test configuration - JUnit 5 with vintage engine for JUnit 4 compatibility
    dependencies {
        // JUnit 5
        "testImplementation"(rootProject.libs.junit5.api)
        "testRuntimeOnly"(rootProject.libs.junit5.engine)
        "testImplementation"(rootProject.libs.junit5.params)
        // Vintage engine for existing JUnit 4 tests
        "testRuntimeOnly"(rootProject.libs.junit5.vintage)
        // Legacy JUnit 4 (for existing tests during migration)
        "testImplementation"(rootProject.libs.junit4)
        "testImplementation"(rootProject.libs.hamcrest)
        // AssertJ
        "testImplementation"(rootProject.libs.assertj)
        // Mockito
        "testImplementation"(rootProject.libs.mockito.core)
        "testImplementation"(rootProject.libs.mockito.junit5)
    }

    tasks.test {
        useJUnitPlatform()
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
