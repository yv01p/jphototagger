package org.jphototagger.e2e.base;

import java.awt.Frame;
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
        GuiActionRunner.execute(() -> {
            AppInit.INSTANCE.init(new String[]{});
            return null;
        });

        // Wait for main frame with timeout
        Frame frame = findMainFrame();
        window = new FrameFixture(robot, frame);
        window.show();
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
