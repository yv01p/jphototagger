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
