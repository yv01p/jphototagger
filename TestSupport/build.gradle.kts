plugins {
    `java-library`
}

dependencies {
    api(libs.hsqldb)
    api(libs.junit5.api)
    api(libs.assertj)
}

// This module is for test utilities - no main source, but consumed by other test source sets
java {
    sourceSets {
        main {
            java.srcDir("src/main/java")
        }
    }
}
