plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Domain"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/eventbus.jar"))
    api(files("../Libraries/XMPCore.jar"))
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
