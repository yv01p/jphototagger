plugins {
    java
    id("me.champeau.jmh") version "0.7.2"
    application
}

application {
    mainClass.set("org.jphototagger.benchmarks.StartupBenchmark")
}

dependencies {
    // JMH
    jmh(libs.jmh.core)
    jmhAnnotationProcessor(libs.jmh.generator)

    // Access to application code for benchmarking
    implementation(project(":Domain"))
    implementation(project(":Repositories:HSQLDB"))
    implementation(project(":Exif"))
    implementation(project(":Image"))
    implementation(project(":Lib"))
    implementation(project(":TestSupport"))
    implementation(libs.hsqldb)

    // Local JARs needed for image/cache operations
    implementation(files("../Libraries/mapdb.jar"))
    implementation(files("../Libraries/ImgrRdr.jar"))
}

jmh {
    warmupIterations.set(2)
    iterations.set(5)
    fork.set(1)
    jmhVersion.set("1.37")

    // Output results to JSON for tracking
    resultFormat.set("JSON")
    resultsFile.set(project.file("build/reports/jmh/results.json"))
}

tasks.register("startup") {
    group = "benchmark"
    description = "Run startup benchmark"
    dependsOn("run")
}
