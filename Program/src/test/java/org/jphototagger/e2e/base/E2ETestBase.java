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

    @BeforeAll
    static void launchApp() {
        robot = BasicRobot.robotWithNewAwtHierarchy();

        // Launch JPhotoTagger on the EDT
        Frame frame = GuiActionRunner.execute(() -> {
            AppInit.INSTANCE.init(new String[]{});
            return findMainFrame();
        });

        window = new FrameFixture(robot, frame);
        window.show();
    }

    private static Frame findMainFrame() {
        // Wait for main frame to be visible
        Frame[] frames = Frame.getFrames();
        for (Frame frame : frames) {
            if (frame.isVisible() && frame.getTitle().contains("JPhotoTagger")) {
                return frame;
            }
        }
        // If not found by title, return the first visible frame
        for (Frame frame : frames) {
            if (frame.isVisible()) {
                return frame;
            }
        }
        throw new IllegalStateException("Main frame not found");
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
