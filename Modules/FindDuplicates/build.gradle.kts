plugins {
    `java-library`
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Image"))
    implementation(project(":Lib"))
    implementation(project(":Resources"))

    implementation(files("../../Libraries/eventbus.jar"))
    implementation(files("../../Libraries/org-openide-util-lookup.jar"))
}

sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("src")
    }
}
