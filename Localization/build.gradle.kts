plugins {
    `java-library`
}

// No dependencies - pure resource module

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
