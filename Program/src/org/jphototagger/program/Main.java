package org.jphototagger.program;

import java.awt.EventQueue;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jphototagger.program.app.AppInit;


/**
 * @author Elmar Baumann
 */
public final class Main {

    // For Java 21+, we only need major version >= 21
    private static final int MIN_JAVA_MAJOR_VERSION = 21;

    public static void main(String[] args) {
        if (checkJavaVersion()) {
            AppInit.INSTANCE.init(args);
        }
    }

    // Version check code does not use other classes from project to minimize the risk of incompatibility
    // (due unknown build computers, JPhotoTagger does not use the javac option "-bootclasspath")
    private static boolean checkJavaVersion() {
        Logger logger = Logger.getLogger(Main.class.getName());
        logger.info("Checking Java version");
        String version = System.getProperty("java.version");

        try {
            // Handle both "1.8.0_292" and "21.0.1" formats
            String cleanVersion = version.split("[-+]")[0];
            String[] parts = cleanVersion.split("[._]");

            if (parts.length == 0) {
                logger.log(Level.SEVERE, "Can''t get valid Java Version! Got: ''{0}''", version);
                return true; // Allow to proceed on parse failure
            }

            int major = Integer.parseInt(parts[0]);

            // For Java 9+, the major version is the first number (e.g., "21" -> 21)
            // For Java 8 and earlier, format is "1.x" (e.g., "1.8" -> we check second number)
            int effectiveMajor = (major == 1 && parts.length > 1)
                ? Integer.parseInt(parts[1])
                : major;

            if (effectiveMajor < MIN_JAVA_MAJOR_VERSION) {
                errorMessageJavaVersion(version);
                return false;
            }
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Failed to parse Java version: " + version, e);
            return true; // Allow to proceed on parse failure
        }

        return true;
    }

    private static void errorMessageJavaVersion(final String version) {
        Logger.getLogger(Main.class.getName()).log(Level.SEVERE,
            "Java version ''{0}'' is too old! The required minimum Java version is ''{1}''.",
            new Object[]{version, MIN_JAVA_MAJOR_VERSION});
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/jphototagger/program/Bundle");
                String message = MessageFormat.format(
                    "Java version {0} is too old. JPhotoTagger requires Java {1} or newer.",
                    version, MIN_JAVA_MAJOR_VERSION);
                String title = bundle.getString("Main.Error.JavaVersion.MessageTitle");
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private Main() {
    }
}
