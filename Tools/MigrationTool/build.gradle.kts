plugins {
    id("java")
    id("application")
}

application {
    mainClass.set("org.jphototagger.tools.migration.MigrationMain")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
    }
    test {
        java {
            setSrcDirs(listOf("test"))
        }
    }
}

dependencies {
    implementation(project(":Repositories:SQLite"))
    implementation(libs.hsqldb)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.bundles.junit5)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.test {
    useJUnitPlatform()
}
