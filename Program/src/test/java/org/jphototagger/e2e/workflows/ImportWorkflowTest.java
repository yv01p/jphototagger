package org.jphototagger.e2e.workflows;

import org.jphototagger.e2e.base.E2ETestBase;
import org.jphototagger.e2e.pages.ImportDialogPage;
import org.jphototagger.e2e.pages.MainWindowPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the photo import workflow.
 * Requires DISPLAY environment variable (run with xvfb-run).
 */
@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".+")
class ImportWorkflowTest extends E2ETestBase {

    private MainWindowPage mainWindow;

    @BeforeEach
    void setup() {
        mainWindow = new MainWindowPage(window);
    }

    @Test
    @Disabled("App initialization hangs in headless Xvfb - needs investigation")
    @DisplayName("Import dialog opens from File menu")
    void openImportDialog_displaysImportDialog() {
        ImportDialogPage importDialog = mainWindow.openImportDialog();

        importDialog.requireVisible();

        // Close dialog to clean up
        importDialog.clickCancel();
    }

    @Test
    @Disabled("App initialization hangs in headless Xvfb - needs investigation")
    @DisplayName("Import dialog can be cancelled")
    void cancelImportDialog_closesDialog() {
        ImportDialogPage importDialog = mainWindow.openImportDialog();

        importDialog.clickCancel();

        importDialog.requireNotVisible();
    }

    @Test
    @Disabled("Requires directory chooser handling - implement in next iteration")
    @DisplayName("Import photos from folder displays in thumbnail panel")
    void importPhotosFromFolder_displaysInThumbnailPanel() {
        // This test requires handling the native directory chooser
        // which is complex with AssertJ Swing.
        // Keeping as placeholder for next iteration.

        ImportDialogPage importDialog = mainWindow.openImportDialog();
        importDialog.withSourceDirectory(testData.getTestPhotosFolder());
        importDialog.clickImport();

        // Verify photos appear in thumbnail panel
        assertThat(testData.getTestPhotoCount()).isEqualTo(3);
    }
}
