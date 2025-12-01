package org.jphototagger.e2e.pages;

import org.assertj.swing.fixture.DialogFixture;

import java.io.File;

/**
 * Page object for the Import Images dialog.
 */
public class ImportDialogPage {

    private final DialogFixture dialog;

    public ImportDialogPage(DialogFixture dialog) {
        this.dialog = dialog;
    }

    /**
     * Sets the source directory by clicking browse and selecting folder.
     *
     * @param folder the source directory containing photos to import
     * @return this page object for fluent chaining
     * @throws UnsupportedOperationException directory chooser handling not yet implemented
     */
    public ImportDialogPage withSourceDirectory(File folder) {
        throw new UnsupportedOperationException(
            "Directory chooser handling not yet implemented. See Phase 4 of implementation plan.");
    }

    /**
     * Clicks the Import (OK) button.
     */
    public void clickImport() {
        dialog.button("dialog.import.btnStart").click();
    }

    /**
     * Clicks the Cancel button.
     */
    public void clickCancel() {
        dialog.button("dialog.import.btnCancel").click();
        // Wait for dialog to fully close before returning
        dialog.robot().waitForIdle();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Verifies the dialog is visible.
     */
    public ImportDialogPage requireVisible() {
        dialog.requireVisible();
        return this;
    }

    /**
     * Verifies the dialog is not visible (closed).
     */
    public void requireNotVisible() {
        dialog.requireNotVisible();
    }

    /**
     * Returns the underlying dialog fixture for advanced assertions.
     */
    public DialogFixture dialog() {
        return dialog;
    }
}
