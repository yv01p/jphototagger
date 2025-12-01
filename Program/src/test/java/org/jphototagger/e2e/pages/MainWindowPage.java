package org.jphototagger.e2e.pages;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JPanelFixture;
import org.assertj.swing.fixture.JTreeFixture;

import javax.swing.JDialog;

/**
 * Page object for the main JPhotoTagger window.
 * Provides access to main UI components and navigation to dialogs.
 */
public class MainWindowPage {

    private final FrameFixture window;

    public MainWindowPage(FrameFixture window) {
        this.window = window;
    }

    /**
     * Opens the Import dialog via File menu.
     */
    public ImportDialogPage openImportDialog() {
        window.menuItem("menu.file.itemImport").click();

        // Wait for and find the import dialog
        DialogFixture dialog = window.dialog(new GenericTypeMatcher<JDialog>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog d) {
                return "dialog.import".equals(d.getName()) ||
                       (d.isVisible() && d.getTitle() != null &&
                        d.getTitle().toLowerCase().contains("import"));
            }
        });

        return new ImportDialogPage(dialog);
    }

    /**
     * Returns the directory tree component.
     */
    public JTreeFixture directoryTree() {
        return window.tree("treeDirectories");
    }

    /**
     * Returns the thumbnails panel.
     */
    public JPanelFixture thumbnailsPanel() {
        return window.panel("panelThumbnailsMetadata");
    }

    /**
     * Returns the search panel.
     */
    public JPanelFixture searchPanel() {
        return window.panel("panelSearch");
    }
}
