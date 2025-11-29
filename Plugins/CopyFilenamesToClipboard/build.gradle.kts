plugins {
    `java-library`
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Lib"))
    implementation(project(":Resources"))

    implementation(files("../../Libraries/org-openide-util-lookup.jar"))
    implementation(files("../../Libraries/eventbus.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
