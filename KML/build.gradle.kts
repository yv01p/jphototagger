plugins {
    `java-library`
}

dependencies {
    api(project(":Resources"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
