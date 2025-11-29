plugins {
    java
}

dependencies {
    implementation(libs.hsqldb)
    implementation(libs.junit5.api)
    implementation(libs.assertj)
}

// This module is for test utilities - no main source, but consumed by other test source sets
java {
    sourceSets {
        main {
            java.srcDir("src/main/java")
        }
    }
}
