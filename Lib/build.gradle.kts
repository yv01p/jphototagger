plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Resources"))

    // Maven Central dependencies
    api(libs.lucene.core)
    api(libs.jaxb.api)
    api(libs.jaxb.core)
    api(libs.jaxb.impl)
    api(libs.activation)

    // Local JARs
    api(files("../Libraries/org-openide-util-lookup.jar"))
    api(files("../Libraries/swingx-core.jar"))
    api(files("../Libraries/beansbinding.jar"))
    api(files("../Libraries/eventbus.jar"))
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
    enabled = false  // TODO: Requires infrastructure setup - external resources, test fixtures
}
