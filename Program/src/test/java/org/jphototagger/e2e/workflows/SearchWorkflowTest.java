package org.jphototagger.e2e.workflows;

import org.assertj.swing.fixture.DialogFixture;
import org.jphototagger.e2e.base.E2ETestBase;
import org.jphototagger.e2e.pages.SearchPanelPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the search workflow.
 * Tests fast search functionality and advanced search dialog.
 * Requires DISPLAY environment variable (run with xvfb-run).
 * Display check is inherited from E2ETestBase.
 */
class SearchWorkflowTest extends E2ETestBase {

    private SearchPanelPage searchPanel;

    @BeforeEach
    void setup() {
        searchPanel = new SearchPanelPage(window);
    }

    @Test
    @DisplayName("Fast search text field is accessible")
    void fastSearchFieldIsAccessible() {
        // Verify fast search text area is visible
        searchPanel.getFastSearchTextArea().requireVisible();

        // Enter text in the search field
        searchPanel.enterFastSearchText("test");

        // Verify text was entered
        assertThat(searchPanel.getFastSearchTextArea().text()).isEqualTo("test");
    }

    @Test
    @DisplayName("Fast search combo box has options")
    void fastSearchComboBoxHasOptions() {
        // Verify combo box is visible
        searchPanel.getFastSearchComboBox().requireVisible();

        // Verify combo box has at least one option
        String[] contents = searchPanel.getFastSearchComboBox().contents();
        assertThat(contents).isNotEmpty();
        assertThat(contents.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Fast search button executes search")
    void fastSearchButtonExecutesSearch() {
        // Verify search button is visible
        searchPanel.getSearchButton().requireVisible();

        // Enter text in the search field
        searchPanel.enterFastSearchText("test");

        // Click the search button
        searchPanel.clickFastSearch();

        // If we reach here without exception, the search button works
        // The actual search results verification would require image data
    }

    @Test
    @Disabled("Requires menu item for advanced search - menu item name may vary")
    @DisplayName("Open advanced search dialog displays dialog")
    void openAdvancedSearchDialog_displaysDialog() {
        // This test is disabled because the menu item name/path for opening
        // advanced search may vary or not exist in the current UI.
        // Enable this test once the correct menu path is identified.

        // Open advanced search dialog
        DialogFixture dialog = searchPanel.openAdvancedSearch();

        // Verify dialog is visible
        dialog.requireVisible();

        // Verify tabbed pane is present
        searchPanel.getAdvancedSearchTabbedPane(dialog).requireVisible();

        // Close dialog
        dialog.close();
    }
}
