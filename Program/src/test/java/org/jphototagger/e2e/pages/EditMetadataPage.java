package org.jphototagger.e2e.pages;

import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.fixture.JListFixture;
import org.assertj.swing.fixture.JTextComponentFixture;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

/**
 * Page object for the metadata edit panel on the right side of the main window.
 * Provides access to keyword editing functionality for selected images.
 */
public class EditMetadataPage {

    private final FrameFixture window;

    public EditMetadataPage(FrameFixture window) {
        this.window = window;
    }

    /**
     * Selects the Keywords tab in the metadata tabbed pane.
     * This makes the keywords edit panel visible.
     *
     * @return this page object for fluent chaining
     */
    public EditMetadataPage selectKeywordsTab() {
        window.robot().waitForIdle();
        window.panel("panelEditKeywords").requireVisible();
        window.tabbedPane("tabbedPaneMetadata").selectTab("panelEditKeywords");
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Returns the keyword input text field fixture.
     * This is where users type keywords before adding them.
     *
     * @return JTextComponentFixture for the keyword input field
     */
    public JTextComponentFixture getKeywordInput() {
        return window.textBox("dc_subjects.textAreaInput");
    }

    /**
     * Returns the keywords list fixture.
     * This list shows all keywords applied to the selected image(s).
     *
     * @return JListFixture for the keywords list
     */
    public JListFixture getKeywordsList() {
        return window.list("dc_subjects.list");
    }

    /**
     * Returns the add button fixture.
     *
     * @return JButtonFixture for the add keyword button
     */
    public JButtonFixture getAddButton() {
        return window.button("dc_subjects.buttonAddInput");
    }

    /**
     * Returns the remove button fixture.
     *
     * @return JButtonFixture for the remove keyword button
     */
    public JButtonFixture getRemoveButton() {
        return window.button("dc_subjects.buttonRemoveSelection");
    }

    /**
     * Returns the suggestion button fixture.
     *
     * @return JButtonFixture for the keyword suggestions button
     */
    public JButtonFixture getSuggestionButton() {
        return window.button("dc_subjects.buttonSuggestion");
    }

    /**
     * Types a keyword into the input field and presses Enter to add it.
     * This simulates the most common user workflow.
     *
     * @param keyword the keyword to enter and add
     * @return this page object for fluent chaining
     */
    public EditMetadataPage enterKeyword(String keyword) {
        window.robot().waitForIdle();
        JTextComponentFixture input = getKeywordInput();
        input.deleteText();
        input.enterText(keyword);
        input.pressAndReleaseKeys(KeyEvent.VK_ENTER);
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Types a keyword into the input field and clicks the add button.
     * This is an alternative workflow to enterKeyword().
     *
     * @param keyword the keyword to add
     * @return this page object for fluent chaining
     */
    public EditMetadataPage addKeyword(String keyword) {
        window.robot().waitForIdle();
        JTextComponentFixture input = getKeywordInput();
        input.deleteText();
        input.enterText(keyword);
        getAddButton().click();
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Clicks the remove button to remove the currently selected keyword from the list.
     * A keyword must be selected in the list first.
     *
     * @return this page object for fluent chaining
     */
    public EditMetadataPage removeSelectedKeyword() {
        window.robot().waitForIdle();
        getRemoveButton().click();
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Selects a keyword in the keywords list by its text.
     *
     * @param keyword the keyword text to select
     * @return this page object for fluent chaining
     */
    public EditMetadataPage selectKeywordInList(String keyword) {
        window.robot().waitForIdle();
        getKeywordsList().selectItem(keyword);
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Returns all keywords currently in the keywords list.
     *
     * @return list of keyword strings
     */
    public List<String> getKeywords() {
        window.robot().waitForIdle();
        String[] contents = getKeywordsList().contents();
        return Arrays.asList(contents);
    }

    /**
     * Checks if a specific keyword exists in the keywords list.
     *
     * @param keyword the keyword to check for
     * @return true if the keyword is in the list, false otherwise
     */
    public boolean hasKeyword(String keyword) {
        return getKeywords().contains(keyword);
    }

    /**
     * Clears the keyword input field.
     *
     * @return this page object for fluent chaining
     */
    public EditMetadataPage clearKeywordInput() {
        window.robot().waitForIdle();
        getKeywordInput().deleteText();
        window.robot().waitForIdle();
        return this;
    }

    /**
     * Verifies the keywords panel is visible.
     *
     * @return this page object for fluent chaining
     */
    public EditMetadataPage requireVisible() {
        window.panel("panelEditKeywords").requireVisible();
        return this;
    }
}
