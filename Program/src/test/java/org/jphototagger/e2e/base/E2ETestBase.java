package org.jphototagger.e2e.base;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.jphototagger.program.app.AppInit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Base class for all E2E GUI tests.
 * Handles app launch, robot creation, and test data management.
 * Requires DISPLAY environment variable (set automatically by xvfb-run).
 */
@Tag("e2e")
@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".+")
public abstract class E2ETestBase {

    protected static FrameFixture window;
    protected static Robot robot;
    protected TestDataManager testData;

    private static final long FRAME_TIMEOUT_MS = 30000; // 30 seconds for app startup

    @BeforeAll
    static void launchApp() {
        robot = BasicRobot.robotWithNewAwtHierarchy();

        // Configure robot timeouts for CI environment
        robot.settings().delayBetweenEvents(50);
        robot.settings().eventPostingDelay(100);

        // Launch JPhotoTagger on the EDT
        // Use -nosplash for faster startup in tests
        GuiActionRunner.execute(() -> {
            AppInit.INSTANCE.init(new String[]{"-nosplash"});
            return null;
        });

        // Wait for main frame with timeout
        Frame frame = findMainFrame();
        window = new FrameFixture(robot, frame);
        window.show();

        // Wait a moment for any startup dialogs to appear
        robot.waitForIdle();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Dismiss any dialogs that appeared after startup
        dismissAllDialogs();

        // Restore focus to main window after dismissing dialogs
        ensureMainWindowFocus();
    }

    /**
     * Ensures main window has focus (robust for headless Xvfb environment).
     */
    private static void ensureMainWindowFocus() {
        GuiActionRunner.execute(() -> {
            window.target().toFront();
            window.target().requestFocus();
            return null;
        });
        robot.waitForIdle();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        robot.click(window.target());
        robot.waitForIdle();
    }

    /**
     * Dismisses all visible dialogs by pressing Escape or clicking close buttons.
     * This handles unexpected dialogs that may appear after app startup.
     */
    private static void dismissAllDialogs() {
        robot.waitForIdle();
        int maxAttempts = 5;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            boolean foundDialog = false;

            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isVisible()) {
                    JDialog dialog = (JDialog) w;
                    System.err.println("[E2E-DEBUG] Dismissing unexpected dialog: " + dialog.getTitle());

                    // Try to find and click a close button (Yes, OK, Cancel, etc.)
                    JButton closeButton = findCloseButton(dialog);
                    if (closeButton != null) {
                        System.err.println("[E2E-DEBUG] Clicking button: " + closeButton.getText());
                        GuiActionRunner.execute(() -> {
                            closeButton.doClick();
                            return null;
                        });
                    } else {
                        // Try pressing Escape to close
                        System.err.println("[E2E-DEBUG] Pressing Escape to close dialog");
                        GuiActionRunner.execute(() -> {
                            dialog.dispose();
                            return null;
                        });
                    }

                    foundDialog = true;
                    robot.waitForIdle();
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    break;
                }
            }

            if (!foundDialog) {
                break;
            }
        }
    }

    /**
     * Finds a close button in a container (Yes, OK, Cancel, Close, etc.)
     */
    private static JButton findCloseButton(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton) {
                JButton button = (JButton) c;
                String text = button.getText();
                if (text != null && (text.equalsIgnoreCase("Yes")
                        || text.equalsIgnoreCase("OK")
                        || text.equalsIgnoreCase("Cancel")
                        || text.equalsIgnoreCase("Close")
                        || text.equalsIgnoreCase("No"))) {
                    return button;
                }
            }
            if (c instanceof Container) {
                JButton found = findCloseButton((Container) c);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Frame findMainFrame() {
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() - start < FRAME_TIMEOUT_MS) {
            Frame[] frames = Frame.getFrames();
            // First, look for frame with JPhotoTagger title
            for (Frame frame : frames) {
                if (frame.isVisible() && frame.getTitle() != null
                        && frame.getTitle().contains("JPhotoTagger")) {
                    return frame;
                }
            }
            // Fallback to first visible frame
            for (Frame frame : frames) {
                if (frame.isVisible()) {
                    return frame;
                }
            }
            // Wait before retrying
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("Main frame not found after " + FRAME_TIMEOUT_MS + "ms");
    }

    @BeforeEach
    void setupTestData() throws Exception {
        // Dismiss any dialogs that may have appeared
        dismissAllDialogs();

        // Ensure main window has focus before each test (robust for headless Xvfb)
        ensureMainWindowFocus();

        testData = new TestDataManager();
        testData.createTempDirectory();
        testData.copyTestPhotos();
    }

    @AfterEach
    void cleanupTestData() {
        if (testData != null) {
            testData.cleanup();
        }
    }

    @AfterAll
    static void tearDown() {
        if (window != null) {
            window.cleanUp();
        }
    }
}
