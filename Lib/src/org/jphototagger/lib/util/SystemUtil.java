package org.jphototagger.lib.util;

import java.awt.Desktop;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Elmar Baumann
 */
public final class SystemUtil {

    /**
     * Returns the Version of the JVM.
     *
     * @return Version or null if not found
     */
    public static Version getJavaVersion() {
        String versionProperty = System.getProperty("java.version");
        return parseJavaVersion(versionProperty);
    }

    /**
     * Parses a Java version string into a Version object.
     * Handles both pre-Java 9 format (1.7.0_80) and Java 9+ format (21.0.1).
     *
     * @param versionString the version string to parse
     * @return Version or null if parsing fails
     */
    public static Version parseJavaVersion(String versionString) {
        if (versionString == null || versionString.isEmpty()) {
            return null;
        }

        try {
            // Remove any suffix like "-ea" or "+build"
            String cleanVersion = versionString.split("[-+]")[0];

            // Handle Java 9+ format: "21" or "21.0.1"
            // Handle pre-Java 9 format: "1.8.0_292"
            String[] parts = cleanVersion.split("[._]");

            if (parts.length == 0) {
                return null;
            }

            int major = Integer.parseInt(parts[0]);

            // For pre-Java 9: "1.8.0" -> major=1, but we want major=8 semantically
            // However, the existing code expects major=1, minor=8 for Java 8
            // So we keep the literal parsing

            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            return new Version(major, minor, patch);
        } catch (NumberFormatException e) {
            Logger.getLogger(SystemUtil.class.getName()).log(Level.SEVERE,
                "Failed to parse Java version: " + versionString, e);
            return null;
        }
    }

    /**
     * Returns whether {@code Desktop#mail(java.net.URI)} can be called. <p> Shorthand for {@code Desktop#isDesktopSupported()}
     * &amp;&amp;
     * {@code Desktop#isSupported(Desktop.Action)}.
     *
     * @return true, if mailing is possible
     */
    public static boolean canMail() {
        return isSupported(Desktop.Action.MAIL);
    }

    /**
     * Returns whether {@code Desktop#browse(java.net.URI)} can be called. <p> Shorthand for {@code Desktop#isDesktopSupported()}
     * &amp;&amp;
     * {@code Desktop#isSupported(Desktop.Action)}.
     *
     * @return true, if browsing is possible
     */
    public static boolean canBrowse() {
        return isSupported(Desktop.Action.BROWSE);
    }

    /**
     * Returns whether {@code Desktop#open(java.io.File)} can be called. <p> Shorthand for {@code Desktop#isDesktopSupported()}
     * &amp;&amp;
     * {@code Desktop#isSupported(Desktop.Action)}.
     *
     * @return true, if opening is possible
     */
    public static boolean canOpen() {
        return isSupported(Desktop.Action.OPEN);
    }

    /**
     * Returns whether {@code Desktop#edit(java.io.File)} can be called. <p> Shorthand for {@code Desktop#isDesktopSupported()}
     * &amp;&amp;
     * {@code Desktop#isSupported(Desktop.Action)}.
     *
     * @return true, if editing is possible
     */
    public static boolean canEdit() {
        return isSupported(Desktop.Action.EDIT);
    }

    /**
     * Returns whether {@code Desktop#print(java.io.File)} can be called. <p> Shorthand for {@code Desktop#isDesktopSupported()}
     * &amp;&amp;
     * {@code Desktop#isSupported(Desktop.Action)}.
     *
     * @return true, if editing is possible
     */
    public static boolean canPrint() {
        return isSupported(Desktop.Action.PRINT);
    }

    private static boolean isSupported(Desktop.Action action) {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action);
    }

    /**
     * Returns whether the VM runs on a Windows operating system.
     *
     * @return true if the VM runs on a Windows operating system
     */
    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("windows");
    }

    public static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("linux");
    }

    /**
     * Returns whether the VM runs on a Macintosh operating system.
     *
     * @return true if the VM runs on a Windows operating system
     */
    public static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    /**
     * @return Empty if unknown
     */
    public static String getDefaultProgramDirPath() {
        if (isWindows()) {
            String programFiles = System.getenv("ProgramFiles");
            return programFiles == null
                    ? ""
                    : programFiles;
        } else if (isLinux()) {
            return "/usr/bin";
        }
        return "";
    }

    /**
     * @return Currently "32" or "64" or empty string on errors
     */
    public static String guessVmArchitecture() {
        try {
            String arch = System.getProperty("sun.arch.data.model");
            if (arch == null) {
                arch = System.getProperty("os.arch");
            }
            return arch == null
                    ? ""
                    : arch.contains("64")
                    ? "64"
                    : "32";
        } catch (Throwable t) {
            Logger.getLogger(SystemUtil.class.getName()).log(Level.SEVERE, null, t);
            return "";
        }
    }

    private SystemUtil() {
    }
}
