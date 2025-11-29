package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jphototagger.repository.sqlite.SqliteSavedSearchesDatabase.SavedSearchData;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SqliteSavedSearchesDatabase.
 * Uses direct SQL operations to avoid GUI dependencies in domain classes.
 */
class SqliteSavedSearchesDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteSavedSearchesDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteSavedSearchesDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getCount_returnsZeroWhenEmpty() {
        int count = database.getCount();

        assertThat(count).isZero();
    }

    @Test
    void getCount_returnsCorrectCount() {
        insertTestSearch("Search1", "SELECT * FROM files", (short) 1);
        insertTestSearch("Search2", "SELECT * FROM files WHERE 1=1", (short) 1);

        int count = database.getCount();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void exists_returnsTrueWhenExists() {
        insertTestSearch("MySearch", "SELECT * FROM files", (short) 1);

        boolean exists = database.exists("MySearch");

        assertThat(exists).isTrue();
    }

    @Test
    void exists_returnsFalseWhenNotExists() {
        boolean exists = database.exists("NonExistent");

        assertThat(exists).isFalse();
    }

    @Test
    void exists_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.exists(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("name == null");
    }

    @Test
    void insert_insertsSavedSearch() {
        boolean inserted = database.insert("TestSearch", "SELECT * FROM files", (short) 1);

        assertThat(inserted).isTrue();
        assertThat(database.exists("TestSearch")).isTrue();
        assertThat(getCustomSql("TestSearch")).isEqualTo("SELECT * FROM files");
        assertThat(getSearchType("TestSearch")).isEqualTo((short) 1);
    }

    @Test
    void insert_insertsWithPanels() {
        boolean inserted = database.insert("SearchWithPanels", null, (short) 0);
        assertThat(inserted).isTrue();

        long searchId = getSearchId("SearchWithPanels");

        // Insert test panels
        insertTestPanel(searchId, 0, false, 1, false, 5, 2, "test value", false);
        insertTestPanel(searchId, 1, true, 2, true, 6, 3, "another value", true);

        // Verify panels were inserted
        int panelCount = getPanelCount(searchId);
        assertThat(panelCount).isEqualTo(2);
    }

    @Test
    void insert_insertsWithKeywords() {
        boolean inserted = database.insert("SearchWithKeywords", null, (short) 0);
        assertThat(inserted).isTrue();

        long searchId = getSearchId("SearchWithKeywords");

        // Insert test keywords
        insertTestKeyword(searchId, "nature");
        insertTestKeyword(searchId, "wildlife");

        // Verify keywords were inserted
        List<String> keywords = getKeywords(searchId);
        assertThat(keywords).containsExactlyInAnyOrder("nature", "wildlife");
    }

    @Test
    void insert_throwsNullPointerExceptionForNullName() {
        assertThatThrownBy(() -> database.insert(null, "SELECT * FROM files", (short) 1))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("name == null");
    }

    @Test
    void delete_removesSavedSearch() {
        insertTestSearch("ToDelete", "SELECT * FROM files", (short) 1);

        boolean deleted = database.delete("ToDelete");

        assertThat(deleted).isTrue();
        assertThat(database.exists("ToDelete")).isFalse();
    }

    @Test
    void delete_returnsFalseWhenNotExists() {
        boolean deleted = database.delete("NonExistent");

        assertThat(deleted).isFalse();
    }

    @Test
    void delete_cascadesToPanelsAndKeywords() {
        insertTestSearch("SearchWithChildren", null, (short) 0);
        long searchId = getSearchId("SearchWithChildren");

        // Add panels and keywords
        insertTestPanel(searchId, 0, false, 1, false, 5, 2, "test", false);
        insertTestKeyword(searchId, "keyword1");

        // Verify they exist
        assertThat(getPanelCount(searchId)).isEqualTo(1);
        assertThat(getKeywords(searchId)).hasSize(1);

        // Delete the search
        database.delete("SearchWithChildren");

        // Verify cascade delete worked
        assertThat(getPanelCount(searchId)).isZero();
        assertThat(getKeywords(searchId)).isEmpty();
    }

    @Test
    void delete_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.delete(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("name == null");
    }

    @Test
    void updateRename_changesName() {
        insertTestSearch("OldName", "SELECT * FROM files", (short) 1);

        boolean renamed = database.updateRename("OldName", "NewName");

        assertThat(renamed).isTrue();
        assertThat(database.exists("OldName")).isFalse();
        assertThat(database.exists("NewName")).isTrue();
        assertThat(getCustomSql("NewName")).isEqualTo("SELECT * FROM files");
    }

    @Test
    void updateRename_returnsFalseWhenNotExists() {
        boolean renamed = database.updateRename("NonExistent", "NewName");

        assertThat(renamed).isFalse();
    }

    @Test
    void updateRename_throwsNullPointerExceptionForNullFromName() {
        assertThatThrownBy(() -> database.updateRename(null, "NewName"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("fromName == null");
    }

    @Test
    void updateRename_throwsNullPointerExceptionForNullToName() {
        assertThatThrownBy(() -> database.updateRename("OldName", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("toName == null");
    }

    @Test
    void find_returnsSavedSearchByName() {
        insertTestSearch("FindMe", "SELECT * FROM files WHERE 1=1", (short) 1);

        SavedSearchData found = database.find("FindMe");

        assertThat(found).isNotNull();
        assertThat(found.name()).isEqualTo("FindMe");
        assertThat(found.customSql()).isEqualTo("SELECT * FROM files WHERE 1=1");
        assertThat(found.searchType()).isEqualTo((short) 1);
    }

    @Test
    void find_returnsNullWhenNotExists() {
        SavedSearchData found = database.find("NonExistent");

        assertThat(found).isNull();
    }

    @Test
    void find_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.find(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("name == null");
    }

    @Test
    void getAll_returnsEmptyListWhenEmpty() {
        List<SavedSearchData> searches = database.getAll();

        assertThat(searches).isEmpty();
    }

    @Test
    void getAll_returnsAllSortedByName() {
        insertTestSearch("Zebra", "SELECT * FROM files", (short) 1);
        insertTestSearch("Apple", "SELECT * FROM files", (short) 0);
        insertTestSearch("Mountain", "SELECT * FROM files", (short) 1);

        List<SavedSearchData> searches = database.getAll();

        assertThat(searches).hasSize(3);
        assertThat(searches.get(0).name()).isEqualTo("Apple");
        assertThat(searches.get(1).name()).isEqualTo("Mountain");
        assertThat(searches.get(2).name()).isEqualTo("Zebra");
    }

    // Helper methods for direct SQL testing

    private void insertTestSearch(String name, String customSql, short searchType) {
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "INSERT INTO saved_searches (name, custom_sql, search_type) VALUES (?, ?, ?)")) {
            stmt.setString(1, name);
            if (customSql != null) {
                stmt.setBytes(2, customSql.getBytes());
            } else {
                stmt.setNull(2, java.sql.Types.BLOB);
            }
            stmt.setShort(3, searchType);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long getSearchId(String name) {
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT id FROM saved_searches WHERE name = ?")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    private String getCustomSql(String name) {
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT custom_sql FROM saved_searches WHERE name = ?")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes(1);
                    return bytes != null ? new String(bytes) : null;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private short getSearchType(String name) {
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT search_type FROM saved_searches WHERE name = ?")) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getShort(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    private void insertTestPanel(long idSavedSearch, int panelIndex, boolean bracketLeft1,
                                  int operatorId, boolean bracketLeft2, int columnId,
                                  int comparatorId, String value, boolean bracketRight) {
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "INSERT INTO saved_searches_panels (id_saved_search, panel_index, bracket_left_1, " +
                     "operator_id, bracket_left_2, column_id, comparator_id, value, bracket_right) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setLong(1, idSavedSearch);
            stmt.setInt(2, panelIndex);
            stmt.setBoolean(3, bracketLeft1);
            stmt.setInt(4, operatorId);
            stmt.setBoolean(5, bracketLeft2);
            stmt.setInt(6, columnId);
            stmt.setInt(7, comparatorId);
            stmt.setString(8, value);
            stmt.setBoolean(9, bracketRight);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getPanelCount(long idSavedSearch) {
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "SELECT COUNT(*) FROM saved_searches_panels WHERE id_saved_search = ?")) {
            stmt.setLong(1, idSavedSearch);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private void insertTestKeyword(long idSavedSearch, String keyword) {
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "INSERT INTO saved_searches_keywords (id_saved_search, keyword) VALUES (?, ?)")) {
            stmt.setLong(1, idSavedSearch);
            stmt.setString(2, keyword);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> getKeywords(long idSavedSearch) {
        List<String> keywords = new ArrayList<>();
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "SELECT keyword FROM saved_searches_keywords WHERE id_saved_search = ? ORDER BY keyword")) {
            stmt.setLong(1, idSavedSearch);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    keywords.add(rs.getString(1));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return keywords;
    }
}
