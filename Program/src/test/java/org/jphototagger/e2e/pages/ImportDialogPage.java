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
     * Note: This is a simplified version that sets the label directly.
     * For full testing, would need to handle the directory chooser.
     */
    public ImportDialogPage withSourceDirectory(File folder) {
        // Click browse button to open directory chooser
        dialog.button("dialog.import.btnBrowseSource").click();

        // For now, we'll need to handle the native file chooser
        // This may require robot.keyPress to type the path
        // TODO: Implement directory chooser handling

        return this;
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
