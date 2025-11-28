plugins {
    `java-library`
}

dependencies {
    api(files("../Libraries/org-openide-util-lookup.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
    }
    test {
        java.srcDirs("test")
    }
}
