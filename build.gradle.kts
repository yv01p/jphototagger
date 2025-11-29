plugins {
    java
    `jvm-test-suite`
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
    apply(plugin = "jvm-test-suite")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
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
