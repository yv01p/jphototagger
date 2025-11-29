plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Maven Central
    api(libs.jgoodies.common)
    api(libs.jgoodies.looks)
    api(libs.flatlaf)

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
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
