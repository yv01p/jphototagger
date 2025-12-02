package org.jphototagger.e2e.pages;

import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JListFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.fixture.JTreeFixture;

/**
 * Page object for the keywords selection panel in the left sidebar.
 * Provides access to keyword tree/list views and filtering capabilities.
 */
public class KeywordsPanelPage {

    private final FrameFixture window;

    public KeywordsPanelPage(FrameFixture window) {
        this.window = window;
    }

    /**
     * Selects the Keywords tab in the selection tabbed pane.
     * This makes the keywords panel visible.
     *
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage selectKeywordsTab() {
        window.robot().waitForIdle();
        window.tabbedPane("tabbedPaneSelection").selectTab(4); // Keywords tab index
        window.robot().waitForIdle();
        window.panel("panelSelKeywords").requireVisible();
        return this;
    }

    /**
     * Returns the keywords tree fixture for the hierarchical view.
     * Note: The tree must be visible (tree view must be active).
     *
     * @return JTreeFixture for the keywords tree
     */
    public JTreeFixture getKeywordsTree() {
        return window.tree("treeSelKeywords");
    }

    /**
     * Returns the keywords list fixture for the flat view.
     * Note: The list must be visible (list view must be active).
     *
     * @return JListFixture for the keywords list
     */
    public JListFixture getKeywordsList() {
        return window.list("listSelKeywords");
    }

    /**
     * Switches to the list view of keywords (flat view).
     *
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage switchToListView() {
        window.robot().waitForIdle();
        var button = window.button("buttonDisplaySelKeywordsList");
        var target = button.target();
        // Use doClick() instead of robot click to avoid screen boundary issues in headless
        org.assertj.swing.edt.GuiActionRunner.execute(() -> {
            target.doClick();
            return null;
        });
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Switches to the tree view of keywords (hierarchical view).
     *
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage switchToTreeView() {
        window.robot().waitForIdle();
        var button = window.button("buttonDisplaySelKeywordsTree");
        var target = button.target();
        // Use doClick() instead of robot click to avoid screen boundary issues in headless
        org.assertj.swing.edt.GuiActionRunner.execute(() -> {
            target.doClick();
            return null;
        });
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Types text into the filter field for the list view.
     * Note: This only affects the list view, not the tree view.
     *
     * @param text the filter text to type
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage filterKeywords(String text) {
        window.robot().waitForIdle();
        JTextComponentFixture filterField = window.textBox("textFieldListSelKeywordsFilter");
        filterField.deleteText();
        filterField.enterText(text);
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Selects a keyword in the tree view by path.
     *
     * @param path the tree path to select (e.g., "Root/Category/Keyword")
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage selectKeywordInTree(String path) {
        window.robot().waitForIdle();
        getKeywordsTree().selectPath(path);
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Selects a keyword in the list view by its text.
     *
     * @param keyword the keyword text to select
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage selectKeywordInList(String keyword) {
        window.robot().waitForIdle();
        getKeywordsList().selectItem(keyword);
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Double-clicks a keyword in the tree view to add it to the edit panel.
     *
     * @param path the tree path to double-click (e.g., "Root/Category/Keyword")
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage doubleClickKeywordInTree(String path) {
        window.robot().waitForIdle();
        getKeywordsTree().doubleClickPath(path);
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Double-clicks a keyword in the list view to add it to the edit panel.
     *
     * @param keyword the keyword text to double-click
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage doubleClickKeywordInList(String keyword) {
        window.robot().waitForIdle();
        getKeywordsList().item(keyword).doubleClick();
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Toggles the expand/collapse state of all tree nodes.
     *
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage toggleExpandAllTreeNodes() {
        window.robot().waitForIdle();
        window.button("toggleButtonExpandAllNodesSelKeywords").click();
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Clicks the search button for the tree view.
     *
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage searchInTree() {
        window.robot().waitForIdle();
        window.button("buttonSearchInTreeSelKeywords").click();
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Clicks the search button for the list view.
     *
     * @return this page object for fluent chaining
     */
    public KeywordsPanelPage searchInList() {
        window.robot().waitForIdle();
        window.button("buttonSearchInListSelKeywords").click();
        window.robot().waitForIdle();
        return this;
    }
}
