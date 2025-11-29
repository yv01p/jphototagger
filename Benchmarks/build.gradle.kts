plugins {
    java
    id("me.champeau.jmh") version "0.7.2"
}

dependencies {
    // JMH
    jmh(libs.jmh.core)
    jmhAnnotationProcessor(libs.jmh.generator)

    // Access to application code for benchmarking
    implementation(project(":Domain"))
    implementation(project(":Repositories:HSQLDB"))
    implementation(libs.hsqldb)
}

jmh {
    warmupIterations.set(2)
    iterations.set(3)
    fork.set(1)
    jmhVersion.set("1.37")

    // Output results to JSON for tracking
    resultFormat.set("JSON")
    resultsFile.set(project.file("build/reports/jmh/results.json"))
}
