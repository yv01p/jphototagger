package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jphototagger.repository.sqlite.SqliteSynonymsDatabase.SynonymPair;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SqliteSynonymsDatabase.
 * Uses direct SQL operations for verification.
 */
class SqliteSynonymsDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteSynonymsDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteSynonymsDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void insertSynonym_insertsNewPair() {
        int count = database.insertSynonym("car", "automobile");

        assertThat(count).isEqualTo(1);
        assertThat(existsInDb("car", "automobile")).isTrue();
    }

    @Test
    void insertSynonym_returnsZeroWhenAlreadyExists() {
        database.insertSynonym("car", "automobile");

        int count = database.insertSynonym("car", "automobile");

        assertThat(count).isZero();
    }

    @Test
    void insertSynonym_throwsNullPointerExceptionForNullWord() {
        assertThatThrownBy(() -> database.insertSynonym(null, "synonym"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("word == null");
    }

    @Test
    void insertSynonym_throwsNullPointerExceptionForNullSynonym() {
        assertThatThrownBy(() -> database.insertSynonym("word", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("synonym == null");
    }

    @Test
    void deleteSynonym_removesSpecificPair() {
        insertIntoDb("car", "automobile");
        insertIntoDb("car", "vehicle");

        int count = database.deleteSynonym("car", "automobile");

        assertThat(count).isEqualTo(1);
        assertThat(existsInDb("car", "automobile")).isFalse();
        assertThat(existsInDb("car", "vehicle")).isTrue();
    }

    @Test
    void deleteSynonym_returnsZeroWhenNotExists() {
        int count = database.deleteSynonym("car", "automobile");

        assertThat(count).isZero();
    }

    @Test
    void deleteSynonym_throwsNullPointerExceptionForNullWord() {
        assertThatThrownBy(() -> database.deleteSynonym(null, "synonym"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("word == null");
    }

    @Test
    void deleteSynonym_throwsNullPointerExceptionForNullSynonym() {
        assertThatThrownBy(() -> database.deleteSynonym("word", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("synonym == null");
    }

    @Test
    void deleteWord_removesAllSynonymsForWord() {
        insertIntoDb("car", "automobile");
        insertIntoDb("car", "vehicle");
        insertIntoDb("car", "motorcar");
        insertIntoDb("bike", "bicycle");

        int count = database.deleteWord("car");

        assertThat(count).isEqualTo(3);
        assertThat(existsInDb("car", "automobile")).isFalse();
        assertThat(existsInDb("car", "vehicle")).isFalse();
        assertThat(existsInDb("car", "motorcar")).isFalse();
        assertThat(existsInDb("bike", "bicycle")).isTrue();
    }

    @Test
    void deleteWord_returnsZeroWhenWordNotExists() {
        int count = database.deleteWord("nonexistent");

        assertThat(count).isZero();
    }

    @Test
    void deleteWord_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.deleteWord(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("word == null");
    }

    @Test
    void existsSynonym_returnsTrueWhenExists() {
        insertIntoDb("car", "automobile");

        boolean exists = database.existsSynonym("car", "automobile");

        assertThat(exists).isTrue();
    }

    @Test
    void existsSynonym_returnsFalseWhenNotExists() {
        boolean exists = database.existsSynonym("car", "automobile");

        assertThat(exists).isFalse();
    }

    @Test
    void existsSynonym_throwsNullPointerExceptionForNullWord() {
        assertThatThrownBy(() -> database.existsSynonym(null, "synonym"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("word == null");
    }

    @Test
    void existsSynonym_throwsNullPointerExceptionForNullSynonym() {
        assertThatThrownBy(() -> database.existsSynonym("word", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("synonym == null");
    }

    @Test
    void getAllSynonymsOfWord_returnsAllSynonymsIncludingReverse() {
        // Direct synonyms: car -> automobile, car -> vehicle
        insertIntoDb("car", "automobile");
        insertIntoDb("car", "vehicle");
        // Reverse synonym: motorcar -> car (so car is a synonym of motorcar)
        insertIntoDb("motorcar", "car");

        Set<String> synonyms = database.getAllSynonymsOfWord("car");

        // Should return: automobile, vehicle (direct) + motorcar (reverse)
        assertThat(synonyms).containsExactlyInAnyOrder("automobile", "vehicle", "motorcar");
    }

    @Test
    void getAllSynonymsOfWord_returnsEmptySetWhenNoSynonyms() {
        Set<String> synonyms = database.getAllSynonymsOfWord("car");

        assertThat(synonyms).isEmpty();
    }

    @Test
    void getAllSynonymsOfWord_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.getAllSynonymsOfWord(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("word == null");
    }

    @Test
    void getAllWords_returnsUniqueWordsSorted() {
        insertIntoDb("car", "automobile");
        insertIntoDb("car", "vehicle");
        insertIntoDb("bike", "bicycle");
        insertIntoDb("zebra", "horse");

        Set<String> words = database.getAllWords();

        assertThat(words).containsExactly("bike", "car", "zebra");
    }

    @Test
    void getAllWords_returnsEmptySetWhenEmpty() {
        Set<String> words = database.getAllWords();

        assertThat(words).isEmpty();
    }

    @Test
    void getAllSynonyms_returnsAllPairs() {
        insertIntoDb("car", "automobile");
        insertIntoDb("car", "vehicle");
        insertIntoDb("bike", "bicycle");

        Set<SynonymPair> synonyms = database.getAllSynonyms();

        assertThat(synonyms).hasSize(3);
        assertThat(synonyms).anyMatch(p -> p.word().equals("car") && p.synonym().equals("automobile"));
        assertThat(synonyms).anyMatch(p -> p.word().equals("car") && p.synonym().equals("vehicle"));
        assertThat(synonyms).anyMatch(p -> p.word().equals("bike") && p.synonym().equals("bicycle"));
    }

    @Test
    void getAllSynonyms_returnsEmptySetWhenEmpty() {
        Set<SynonymPair> synonyms = database.getAllSynonyms();

        assertThat(synonyms).isEmpty();
    }

    @Test
    void getWordCount_returnsCountOfUniqueWords() {
        insertIntoDb("car", "automobile");
        insertIntoDb("car", "vehicle");
        insertIntoDb("bike", "bicycle");

        int count = database.getWordCount();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void getWordCount_returnsZeroWhenEmpty() {
        int count = database.getWordCount();

        assertThat(count).isZero();
    }

    @Test
    void getSynonymCount_returnsCountForWord() {
        insertIntoDb("car", "automobile");
        insertIntoDb("car", "vehicle");
        insertIntoDb("car", "motorcar");
        insertIntoDb("bike", "bicycle");

        int count = database.getSynonymCount("car");

        assertThat(count).isEqualTo(3);
    }

    @Test
    void getSynonymCount_returnsZeroWhenWordNotExists() {
        int count = database.getSynonymCount("nonexistent");

        assertThat(count).isZero();
    }

    @Test
    void getSynonymCount_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.getSynonymCount(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("word == null");
    }

    // Helper methods for direct SQL testing

    private void insertIntoDb(String word, String synonym) {
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "INSERT INTO synonyms (word, synonym) VALUES (?, ?)")) {
            stmt.setString(1, word);
            stmt.setString(2, synonym);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean existsInDb(String word, String synonym) {
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "SELECT COUNT(*) FROM synonyms WHERE word = ? AND synonym = ?")) {
            stmt.setString(1, word);
            stmt.setString(2, synonym);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
