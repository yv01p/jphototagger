plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Domain"))
    api(project(":Image"))
    api(project(":Lib"))
    api(project(":Resources"))
    api(project(":XMP"))

    // Maven Central
    api(libs.hsqldb)

    // Local JARs
    api(files("../../Libraries/org-openide-util-lookup.jar"))
    api(files("../../Libraries/eventbus.jar"))

    // Test dependencies
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
