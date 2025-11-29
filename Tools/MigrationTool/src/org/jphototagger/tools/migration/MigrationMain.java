package org.jphototagger.tools.migration;

import java.io.File;

public class MigrationMain {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: MigrationMain <hsqldb-file> <sqlite-file>");
            System.exit(1);
        }
        // Implementation in Task 18
    }
}
