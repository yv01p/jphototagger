plugins {
    `java-library`
}

dependencies {
    api(project(":API"))
    api(project(":Resources"))

    // Maven Central dependencies
    api(libs.lucene.core)
    api(libs.jakarta.xml.bind.api)
    api(libs.jakarta.xml.bind.impl)
    api(libs.jakarta.activation)

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
        resources.srcDirs("test")
    }
}
