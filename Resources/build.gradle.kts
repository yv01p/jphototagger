plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/swingx-core.jar"))
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
