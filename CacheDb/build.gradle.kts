plugins {
    id("java-library")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
        resources {
            setSrcDirs(listOf("src"))
            exclude("**/*.java")
        }
    }
    test {
        java {
            setSrcDirs(listOf("test"))
        }
    }
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Lib"))
    implementation(project(":Exif"))

    implementation(libs.sqlite.jdbc)

    compileOnly(files("../Libraries/org-openide-util-lookup-8.6.jar"))

    testImplementation(project(":TestSupport"))
    testImplementation(libs.bundles.junit5)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}
