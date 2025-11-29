package org.jphototagger.benchmarks;

/**
 * Standalone startup time benchmark.
 * Measures initialization phases without launching full UI.
 *
 * Run with: ./gradlew :Benchmarks:run
 *
 * Note: This is a simplified version that measures class loading
 * and basic initialization. Full startup benchmarking requires
 * the complete application context.
 */
public class StartupBenchmark {

    public static void main(String[] args) {
        System.out.println("JPhotoTagger Startup Benchmark");
        System.out.println("==============================");
        System.out.println();

        long totalStart = System.nanoTime();

        // Phase 1: Class loading (load key classes)
        long phase1Start = System.nanoTime();
        loadClasses();
        long phase1End = System.nanoTime();

        // Phase 2: JAXB initialization
        long phase2Start = System.nanoTime();
        initJaxb();
        long phase2End = System.nanoTime();

        // Phase 3: Image I/O initialization
        long phase3Start = System.nanoTime();
        initImageIO();
        long phase3End = System.nanoTime();

        long totalEnd = System.nanoTime();

        // Output results
        double phase1Ms = (phase1End - phase1Start) / 1_000_000.0;
        double phase2Ms = (phase2End - phase2Start) / 1_000_000.0;
        double phase3Ms = (phase3End - phase3Start) / 1_000_000.0;
        double totalMs = (totalEnd - totalStart) / 1_000_000.0;

        System.out.printf("Phase 1 (Class Loading):    %8.2f ms%n", phase1Ms);
        System.out.printf("Phase 2 (JAXB Init):        %8.2f ms%n", phase2Ms);
        System.out.printf("Phase 3 (ImageIO Init):     %8.2f ms%n", phase3Ms);
        System.out.println("------------------------------");
        System.out.printf("Total:                      %8.2f ms%n", totalMs);
        System.out.println();

        // JSON output for automated comparison
        System.out.printf("{\"class_loading_ms\": %.2f, \"jaxb_ms\": %.2f, \"imageio_ms\": %.2f, \"total_ms\": %.2f}%n",
                phase1Ms, phase2Ms, phase3Ms, totalMs);
    }

    private static void loadClasses() {
        try {
            // Load key domain classes
            Class.forName("org.jphototagger.domain.metadata.keywords.Keyword");
            Class.forName("org.jphototagger.domain.image.ImageFile");
            Class.forName("org.jphototagger.exif.ExifTags");
            Class.forName("org.jphototagger.exif.ExifTag");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: Could not load class - " + e.getMessage());
        }
    }

    private static void initJaxb() {
        try {
            // Initialize JAXB context (expensive first-time operation)
            org.jphototagger.exif.ExifTags tags = new org.jphototagger.exif.ExifTags();
            org.jphototagger.lib.xml.bind.XmlObjectExporter.marshal(tags);
        } catch (Exception e) {
            System.err.println("Warning: JAXB init failed - " + e.getMessage());
        }
    }

    private static void initImageIO() {
        // Initialize ImageIO (scans for plugins)
        javax.imageio.ImageIO.getReaderFormatNames();
        javax.imageio.ImageIO.getWriterFormatNames();
    }
}
