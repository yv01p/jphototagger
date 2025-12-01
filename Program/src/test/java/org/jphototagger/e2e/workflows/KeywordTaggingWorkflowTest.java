package org.jphototagger.e2e.workflows;

import org.jphototagger.e2e.base.E2ETestBase;
import org.jphototagger.e2e.pages.EditMetadataPage;
import org.jphototagger.e2e.pages.KeywordsPanelPage;
import org.jphototagger.e2e.pages.MainWindowPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the keyword tagging workflow.
 * Tests keyword editing, selection panel interaction, and view switching.
 * Requires DISPLAY environment variable (run with xvfb-run).
 * Display check is inherited from E2ETestBase.
 */
class KeywordTaggingWorkflowTest extends E2ETestBase {

    private MainWindowPage mainWindow;
    private KeywordsPanelPage keywordsPanel;
    private EditMetadataPage editMetadata;

    @BeforeEach
    void setup() {
        mainWindow = new MainWindowPage(window);
        keywordsPanel = new KeywordsPanelPage(window);
        editMetadata = new EditMetadataPage(window);
    }

    @Test
    @DisplayName("Select keywords tab displays keywords panel")
    void selectKeywordsTab_displaysKeywordsPanel() {
        // Navigate to keywords tab in selection panel
        keywordsPanel.selectKeywordsTab();

        // Verify keywords tree is visible (default view is tree)
        keywordsPanel.getKeywordsTree().requireVisible();
    }

    @Test
    @Disabled("Requires selected image - keywords can only be edited when image is selected")
    @DisplayName("Enter keyword in edit panel adds keyword to list")
    void enterKeywordInEditPanel_addsKeywordToList() {
        // This test requires an image to be selected before keywords can be edited.
        // Without an image selection, the keyword input may be disabled or non-functional.
        // Implementation will be completed once image selection is implemented.

        // Navigate to keywords edit panel
        editMetadata.selectKeywordsTab();
        editMetadata.requireVisible();

        // Enter a keyword
        editMetadata.enterKeyword("TestKeyword");

        // Verify keyword appears in the list
        assertThat(editMetadata.hasKeyword("TestKeyword")).isTrue();
    }

    @Test
    @Disabled("Requires selected image - keywords can only be edited when image is selected")
    @DisplayName("Remove keyword from edit panel removes from list")
    void removeKeywordFromEditPanel_removesFromList() {
        // This test requires an image to be selected before keywords can be edited.
        // Without an image selection, the keyword input may be disabled or non-functional.
        // Implementation will be completed once image selection is implemented.

        // Navigate to keywords edit panel
        editMetadata.selectKeywordsTab();
        editMetadata.requireVisible();

        // Add a keyword first
        editMetadata.addKeyword("TestKeyword");
        assertThat(editMetadata.hasKeyword("TestKeyword")).isTrue();

        // Select the keyword in the list
        editMetadata.selectKeywordInList("TestKeyword");

        // Click remove button
        editMetadata.removeSelectedKeyword();

        // Verify keyword is no longer in list
        assertThat(editMetadata.hasKeyword("TestKeyword")).isFalse();
    }

    @Test
    @DisplayName("Switch between tree and list view works correctly")
    void switchBetweenTreeAndListView_worksCorrectly() {
        // Navigate to keywords selection panel
        keywordsPanel.selectKeywordsTab();

        // Verify tree view is showing initially (default)
        keywordsPanel.getKeywordsTree().requireVisible();

        // Switch to list view
        keywordsPanel.switchToListView();

        // Verify list is showing
        keywordsPanel.getKeywordsList().requireVisible();

        // Switch back to tree view
        keywordsPanel.switchToTreeView();

        // Verify tree is showing again
        keywordsPanel.getKeywordsTree().requireVisible();
    }
}
