package org.jphototagger.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation for synonyms database operations.
 * Mirrors the HSQLDB SynonymsDatabase functionality but without EventBus notifications.
 */
public final class SqliteSynonymsDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteSynonymsDatabase.class.getName());

    public SqliteSynonymsDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    /**
     * Inserts a word-synonym pair.
     *
     * @param word the word
     * @param synonym the synonym
     * @return count of inserted rows (1 if inserted, 0 if already exists)
     */
    public int insertSynonym(String word, String synonym) {
        if (word == null) {
            throw new NullPointerException("word == null");
        }
        if (synonym == null) {
            throw new NullPointerException("synonym == null");
        }
        if (existsSynonym(word, synonym)) {
            return 0;
        }
        int count = 0;
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement stmt = con.prepareStatement(
                    "INSERT INTO synonyms (word, synonym) VALUES (?, ?)")) {
                stmt.setString(1, word);
                stmt.setString(2, synonym);
                LOGGER.log(Level.FINER, stmt.toString());
                count = stmt.executeUpdate();
                con.commit();
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, null, t);
                count = 0;
                rollback(con);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return count;
    }

    /**
     * Deletes a specific word-synonym pair.
     *
     * @param word the word
     * @param synonym the synonym
     * @return count of deleted rows
     */
    public int deleteSynonym(String word, String synonym) {
        if (word == null) {
            throw new NullPointerException("word == null");
        }
        if (synonym == null) {
            throw new NullPointerException("synonym == null");
        }
        int count = 0;
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement stmt = con.prepareStatement(
                    "DELETE FROM synonyms WHERE word = ? AND synonym = ?")) {
                stmt.setString(1, word);
                stmt.setString(2, synonym);
                LOGGER.log(Level.FINER, stmt.toString());
                count = stmt.executeUpdate();
                con.commit();
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, null, t);
                count = 0;
                rollback(con);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return count;
    }

    /**
     * Deletes a word and all its synonyms.
     *
     * @param word the word
     * @return count of deleted word-synonym pairs
     */
    public int deleteWord(String word) {
        if (word == null) {
            throw new NullPointerException("word == null");
        }
        int count = 0;
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement stmt = con.prepareStatement(
                    "DELETE FROM synonyms WHERE word = ?")) {
                stmt.setString(1, word);
                LOGGER.log(Level.FINER, stmt.toString());
                count = stmt.executeUpdate();
                con.commit();
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, null, t);
                count = 0;
                rollback(con);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return count;
    }

    /**
     * Checks if a word-synonym pair exists.
     *
     * @param word the word
     * @param synonym the synonym
     * @return true if exists
     */
    public boolean existsSynonym(String word, String synonym) {
        if (word == null) {
            throw new NullPointerException("word == null");
        }
        if (synonym == null) {
            throw new NullPointerException("synonym == null");
        }
        long count = 0;
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "SELECT COUNT(*) FROM synonyms WHERE word = ? AND synonym = ?")) {
            stmt.setString(1, word);
            stmt.setString(2, synonym);
            LOGGER.log(Level.FINEST, stmt.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getLong(1);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
            count = 0;
        }
        return count == 1;
    }

    /**
     * Returns all synonyms of a word.
     * Includes both direct synonyms (word -> synonym) and reverse synonyms (synonym -> word).
     *
     * @param word the word
     * @return synonyms or empty set
     */
    public Set<String> getAllSynonymsOfWord(String word) {
        if (word == null) {
            throw new NullPointerException("word == null");
        }
        Set<String> synonyms = new LinkedHashSet<>();
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(getGetSynonymsOfSql())) {
            stmt.setString(1, word);
            stmt.setString(2, word);
            LOGGER.log(Level.FINEST, stmt.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    synonyms.add(rs.getString(1));
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return synonyms;
    }

    /**
     * Returns all unique words that have synonyms.
     *
     * @return words sorted alphabetically
     */
    public Set<String> getAllWords() {
        Set<String> words = new LinkedHashSet<>();
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            String sql = "SELECT DISTINCT word FROM synonyms ORDER BY word";
            LOGGER.log(Level.FINEST, sql);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    words.add(rs.getString(1));
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
            words.clear();
        }
        return words;
    }

    /**
     * Returns all word-synonym pairs.
     *
     * @return all synonym pairs
     */
    public Set<SynonymPair> getAllSynonyms() {
        Set<SynonymPair> synonyms = new LinkedHashSet<>();
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            String sql = "SELECT word, synonym FROM synonyms ORDER BY word, synonym";
            LOGGER.log(Level.FINEST, sql);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String word = rs.getString(1);
                    String synonym = rs.getString(2);
                    synonyms.add(new SynonymPair(word, synonym));
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
            synonyms.clear();
        }
        return synonyms;
    }

    /**
     * Returns the count of unique words that have synonyms.
     *
     * @return count of words
     */
    public int getWordCount() {
        int count = 0;
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            String sql = "SELECT COUNT(DISTINCT word) FROM synonyms";
            LOGGER.log(Level.FINEST, sql);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return count;
    }

    /**
     * Returns the count of synonyms for a specific word.
     *
     * @param word the word
     * @return count of synonyms
     */
    public int getSynonymCount(String word) {
        if (word == null) {
            throw new NullPointerException("word == null");
        }
        int count = 0;
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(
                     "SELECT COUNT(*) FROM synonyms WHERE word = ?")) {
            stmt.setString(1, word);
            LOGGER.log(Level.FINEST, stmt.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return count;
    }

    // Private helper methods

    private String getGetSynonymsOfSql() {
        return "SELECT synonym FROM synonyms WHERE word = ?"
                + " UNION SELECT word FROM synonyms WHERE synonym = ?"
                + " ORDER BY 1";
    }

    /**
     * Simple data holder for word-synonym pairs.
     */
    public record SynonymPair(String word, String synonym) {}
}
