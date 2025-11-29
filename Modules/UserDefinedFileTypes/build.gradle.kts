plugins {
    `java-library`
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Lib"))
    implementation(project(":Resources"))

    implementation(files("../../Libraries/eventbus.jar"))
    implementation(files("../../Libraries/org-openide-util-lookup.jar"))
    implementation(files("../../Libraries/swingx-core.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
