plugins {
    `java-library`
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Lib"))
    implementation(project(":Resources"))
    implementation(project(":XMP"))

    implementation(libs.jaxb.api)
    implementation(libs.jaxb.core)
    implementation(libs.jaxb.impl)
    implementation(libs.activation)

    implementation(files("../../Libraries/org-openide-util-lookup.jar"))
    implementation(files("../../Libraries/eventbus.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
