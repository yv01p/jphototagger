package org.jphototagger.lib.help;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jphototagger.lib.util.StringUtil;

/**
 * Simple string-based help search (replaces Lucene-based implementation).
 *
 * @author Elmar Baumann
 */
final class HelpSearch {

    private final HelpNode rootNode;
    private final List<IndexedPage> indexedPages = new ArrayList<>();
    private boolean indexed = false;

    HelpSearch(HelpNode rootNode) {
        if (rootNode == null) {
            throw new NullPointerException("rootNode == null");
        }
        this.rootNode = rootNode;
    }

    void startIndexing() {
        if (indexed) {
            return;
        }

        try {
            Collection<HelpPage> helpPages = HelpUtil.findHelpPagesRecursive(rootNode);
            for (HelpPage helpPage : helpPages) {
                indexPage(helpPage);
            }
            indexed = true;
        } catch (Throwable t) {
            Logger.getLogger(HelpSearch.class.getName()).log(Level.SEVERE, null, t);
        }
    }

    private void indexPage(HelpPage helpPage) {
        try {
            String content = getHelpPageContentAsString(helpPage);
            String normalizedContent = normalizeForSearch(content);
            String normalizedTitle = normalizeForSearch(
                StringUtil.emptyStringIfNull(helpPage.getTitle()));

            indexedPages.add(new IndexedPage(
                helpPage.getUrl(),
                helpPage.getTitle(),
                normalizedTitle + " " + normalizedContent
            ));
        } catch (IOException e) {
            Logger.getLogger(HelpSearch.class.getName()).log(Level.WARNING,
                "Failed to index help page: " + helpPage.getUrl(), e);
        }
    }

    private String getHelpPageContentAsString(HelpPage helpPage) throws IOException {
        InputStream helpPageContent = HelpUtil.class.getResourceAsStream(helpPage.getUrl());
        if (helpPageContent == null) {
            return "";
        }
        String content = StringUtil.convertStreamToString(helpPageContent, "UTF-8");
        return removeHtmlTags(content);
    }

    private String removeHtmlTags(String stringWithHtmlTags) {
        String result = stringWithHtmlTags;
        result = result.replaceAll("<[^>]*>", " ");
        result = result.replaceAll("&nbsp;", " ");
        result = result.replaceAll("&amp;", "&");
        result = result.replaceAll("&quot;", "\"");
        result = result.replaceAll("&lt;", "<");
        result = result.replaceAll("&gt;", ">");
        result = result.replaceAll("\\s+", " ");
        return result.trim();
    }

    private String normalizeForSearch(String text) {
        return text.toLowerCase(Locale.ROOT);
    }

    List<HelpPage> findHelpPagesMatching(String queryString) {
        if (queryString == null) {
            throw new NullPointerException("queryString == null");
        }

        if (!indexed) {
            throw new IllegalStateException("startIndexing was not called");
        }

        String normalizedQuery = normalizeForSearch(queryString.trim());
        if (normalizedQuery.isEmpty()) {
            return Collections.emptyList();
        }

        // Split query into terms for AND matching
        String[] queryTerms = normalizedQuery.split("\\s+");

        List<HelpPage> matchingPages = new ArrayList<>();

        for (IndexedPage page : indexedPages) {
            if (matchesAllTerms(page.normalizedContent, queryTerms)) {
                HelpPage helpPage = new HelpPage();
                helpPage.setUrl(page.url);
                helpPage.setTitle(page.title);
                matchingPages.add(helpPage);
            }
        }

        return matchingPages;
    }

    private boolean matchesAllTerms(String content, String[] terms) {
        for (String term : terms) {
            if (!content.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private static class IndexedPage {
        final String url;
        final String title;
        final String normalizedContent;

        IndexedPage(String url, String title, String normalizedContent) {
            this.url = url;
            this.title = title;
            this.normalizedContent = normalizedContent;
        }
    }
}
