plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Domain"))
    api(project(":Image"))
    api(project(":KML"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Local JARs
    api(files("../Libraries/metadata-extractor.jar"))
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/eventbus.jar"))
    api(files("../Libraries/mapdb.jar"))

    testImplementation(project(":TestSupport"))
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
