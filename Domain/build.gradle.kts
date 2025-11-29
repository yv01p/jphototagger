plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Lib"))
    api(project(":Resources"))

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/eventbus.jar"))
    api(files("../Libraries/ImgrRdr.jar"))
    api(files("../Libraries/jaxb-activation.jar"))
    api(files("../Libraries/jaxb-api.jar"))
    api(files("../Libraries/jaxb-core.jar"))
    api(files("../Libraries/jaxb-impl.jar"))
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

tasks.test {
    enabled = false  // TODO: Requires infrastructure setup - Preferences, Lookup services
}
