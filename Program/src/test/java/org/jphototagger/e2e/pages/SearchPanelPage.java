package org.jphototagger.e2e.pages;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;

import javax.swing.JDialog;

/**
 * Page object for the search panel in JPhotoTagger.
 * Provides access to both fast search and advanced search functionality.
 */
public class SearchPanelPage {

    private final FrameFixture window;

    public SearchPanelPage(FrameFixture window) {
        this.window = window;
    }

    // ========== Fast Search Methods ==========

    /**
     * Enters text into the fast search text area.
     *
     * @param text the search text to enter
     * @return this page object for fluent chaining
     */
    public SearchPanelPage enterFastSearchText(String text) {
        window.robot().waitForIdle();
        JTextComponentFixture searchField = window.textBox("textAreaSearch");
        searchField.deleteText();
        searchField.enterText(text);
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Clicks the fast search button to execute the search.
     *
     * @return this page object for fluent chaining
     */
    public SearchPanelPage clickFastSearch() {
        window.robot().waitForIdle();
        window.button("searchButton").click();
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Selects a search type from the fast search combo box.
     *
     * @param index the index of the search type to select
     * @return this page object for fluent chaining
     */
    public SearchPanelPage selectFastSearchType(int index) {
        window.robot().waitForIdle();
        JComboBoxFixture comboBox = window.comboBox("fastSearchComboBox");
        comboBox.selectItem(index);
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Selects a search type from the fast search combo box by text.
     *
     * @param text the text of the search type to select
     * @return this page object for fluent chaining
     */
    public SearchPanelPage selectFastSearchType(String text) {
        window.robot().waitForIdle();
        JComboBoxFixture comboBox = window.comboBox("fastSearchComboBox");
        comboBox.selectItem(text);
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Clicks the restore selection button to restore the previous selection.
     *
     * @return this page object for fluent chaining
     */
    public SearchPanelPage clickRestoreSelection() {
        window.robot().waitForIdle();
        window.button("buttonRestoreSelection").click();
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Returns the fast search combo box fixture.
     *
     * @return JComboBoxFixture for the fast search combo box
     */
    public JComboBoxFixture getFastSearchComboBox() {
        return window.comboBox("fastSearchComboBox");
    }

    /**
     * Returns the fast search text area fixture.
     *
     * @return JTextComponentFixture for the search text area
     */
    public JTextComponentFixture getFastSearchTextArea() {
        return window.textBox("textAreaSearch");
    }

    /**
     * Returns the search button fixture.
     *
     * @return JButtonFixture for the search button
     */
    public JButtonFixture getSearchButton() {
        return window.button("searchButton");
    }

    // ========== Advanced Search Methods ==========

    /**
     * Opens the advanced search dialog.
     * Note: This assumes there's a menu or button to open the advanced search.
     * The implementation may need to be adjusted based on how the dialog is opened.
     *
     * @return DialogFixture for the advanced search dialog
     */
    public DialogFixture openAdvancedSearch() {
        window.robot().waitForIdle();

        // Open advanced search via menu (adjust path as needed)
        window.menuItem("menu.search.itemAdvanced").click();

        window.robot().waitForIdle();

        // Wait for and find the advanced search dialog
        DialogFixture dialog = window.dialog(new GenericTypeMatcher<JDialog>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog d) {
                return "advancedSearchPanel".equals(d.getName()) ||
                       (d.isVisible() && d.getTitle() != null &&
                        d.getTitle().toLowerCase().contains("advanced search"));
            }
        });

        return dialog;
    }

    /**
     * Enters text into the advanced search keywords input.
     * Note: This method assumes the advanced search dialog is already open.
     *
     * @param dialog the advanced search dialog fixture
     * @param keywords the keywords to enter
     * @return this page object for fluent chaining
     */
    public SearchPanelPage enterAdvancedSearchKeywords(DialogFixture dialog, String keywords) {
        dialog.robot().waitForIdle();
        // The keywords input panel has its own input area
        JTextComponentFixture keywordsInput = dialog.textBox("advancedSearch.panelKeywordsInput");
        keywordsInput.deleteText();
        keywordsInput.enterText(keywords);
        dialog.robot().waitForIdle();
        return this;
    }

    /**
     * Selects a tab in the advanced search dialog.
     *
     * @param dialog the advanced search dialog fixture
     * @param tabIndex the index of the tab to select (0=Keywords, 1=SimpleSql, 2=CustomSql)
     * @return this page object for fluent chaining
     */
    public SearchPanelPage selectAdvancedSearchTab(DialogFixture dialog, int tabIndex) {
        dialog.robot().waitForIdle();
        JTabbedPaneFixture tabbedPane = dialog.tabbedPane("advancedSearch.tabbedPane");
        tabbedPane.selectTab(tabIndex);
        dialog.robot().waitForIdle();
        return this;
    }

    /**
     * Enters custom SQL in the advanced search dialog.
     * Note: This method assumes the advanced search dialog is already open
     * and the Custom SQL tab is selected.
     *
     * @param dialog the advanced search dialog fixture
     * @param sql the SQL query to enter
     * @return this page object for fluent chaining
     */
    public SearchPanelPage enterAdvancedSearchCustomSql(DialogFixture dialog, String sql) {
        dialog.robot().waitForIdle();
        JTextComponentFixture sqlArea = dialog.textBox("advancedSearch.textAreaCustomSql");
        sqlArea.deleteText();
        sqlArea.enterText(sql);
        dialog.robot().waitForIdle();
        return this;
    }

    /**
     * Clicks the search button in the advanced search dialog.
     *
     * @param dialog the advanced search dialog fixture
     * @return this page object for fluent chaining
     */
    public SearchPanelPage clickAdvancedSearchButton(DialogFixture dialog) {
        dialog.robot().waitForIdle();
        dialog.button("advancedSearch.btnSearch").click();
        dialog.robot().waitForIdle();
        return this;
    }

    /**
     * Clicks the save button in the advanced search dialog.
     *
     * @param dialog the advanced search dialog fixture
     * @return this page object for fluent chaining
     */
    public SearchPanelPage clickAdvancedSearchSave(DialogFixture dialog) {
        dialog.robot().waitForIdle();
        dialog.button("advancedSearch.btnSave").click();
        dialog.robot().waitForIdle();
        return this;
    }

    /**
     * Clicks the save as button in the advanced search dialog.
     *
     * @param dialog the advanced search dialog fixture
     * @return this page object for fluent chaining
     */
    public SearchPanelPage clickAdvancedSearchSaveAs(DialogFixture dialog) {
        dialog.robot().waitForIdle();
        dialog.button("advancedSearch.btnSaveAs").click();
        dialog.robot().waitForIdle();
        return this;
    }

    /**
     * Clicks the reset button in the advanced search dialog.
     *
     * @param dialog the advanced search dialog fixture
     * @return this page object for fluent chaining
     */
    public SearchPanelPage clickAdvancedSearchReset(DialogFixture dialog) {
        dialog.robot().waitForIdle();
        dialog.button("advancedSearch.btnReset").click();
        dialog.robot().waitForIdle();
        return this;
    }

    /**
     * Returns the advanced search tabbed pane fixture.
     *
     * @param dialog the advanced search dialog fixture
     * @return JTabbedPaneFixture for the advanced search tabs
     */
    public JTabbedPaneFixture getAdvancedSearchTabbedPane(DialogFixture dialog) {
        return dialog.tabbedPane("advancedSearch.tabbedPane");
    }
}
