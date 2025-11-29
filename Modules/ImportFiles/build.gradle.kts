plugins {
    `java-library`
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Image"))
    implementation(project(":Lib"))
    implementation(project(":Resources"))

    implementation(files("../../Libraries/org-openide-util-lookup.jar"))
    implementation(files("../../Libraries/swingx-core.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
