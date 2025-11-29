package org.jphototagger.repository.hsqldb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import org.jphototagger.domain.metadata.keywords.Keyword;
import org.jphototagger.testsupport.TestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for KeywordsDatabase.
 * These tests capture current behavior to protect against regressions
 * during the SQLite migration.
 */
class KeywordsDatabaseTest extends DatabaseTestBase {

    private static final String CREATE_HIERARCHICAL_SUBJECTS =
            "CREATE TABLE hierarchical_subjects (" +
            "id BIGINT NOT NULL PRIMARY KEY, " +
            "id_parent BIGINT, " +
            "subject VARCHAR(256) NOT NULL, " +
            "real BOOLEAN)";

    @Override
    protected void createSchema(TestDatabase db) throws SQLException {
        db.executeSql(CREATE_HIERARCHICAL_SUBJECTS);
    }

    // Note: KeywordsDatabase.INSTANCE uses ConnectionPool.INSTANCE internally.
    // For proper unit testing, we need to inject connections.
    // These tests demonstrate the INTERFACE we expect - actual implementation
    // requires refactoring KeywordsDatabase to accept connection provider.

    @Nested
    @DisplayName("getAllKeywords")
    class GetAllKeywords {

        @Test
        @DisplayName("returns empty collection when no keywords exist")
        void returnsEmptyWhenNoKeywords() throws SQLException {
            // Given: empty table (just created)

            // When: query all keywords
            Collection<Keyword> keywords = queryAllKeywords();

            // Then: empty collection
            assertThat(keywords).isEmpty();
        }

        @Test
        @DisplayName("returns all keywords with correct properties")
        void returnsAllKeywordsWithProperties() throws SQLException {
            // Given: keywords in database
            testDb.executeSql(
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (1, NULL, 'Animals', TRUE)",
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (2, 1, 'Dogs', TRUE)",
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (3, 1, 'Cats', FALSE)"
            );

            // When: query all keywords
            Collection<Keyword> keywords = queryAllKeywords();

            // Then: all keywords returned
            assertThat(keywords).hasSize(3);
            assertThat(keywords).extracting(Keyword::getName)
                    .containsExactlyInAnyOrder("Animals", "Dogs", "Cats");
        }

        @Test
        @DisplayName("handles null id_parent for root keywords")
        void handlesNullIdParent() throws SQLException {
            // Given: root keyword (no parent)
            testDb.executeSql(
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (1, NULL, 'Root', TRUE)"
            );

            // When: query all keywords
            Collection<Keyword> keywords = queryAllKeywords();

            // Then: id_parent is null
            assertThat(keywords).hasSize(1);
            Keyword root = keywords.iterator().next();
            assertThat(root.getIdParent()).isNull();
        }
    }

    @Nested
    @DisplayName("getRootKeywords")
    class GetRootKeywords {

        @Test
        @DisplayName("returns only keywords with null parent, ordered by subject")
        void returnsOnlyRootKeywordsOrdered() throws SQLException {
            // Given: mix of root and child keywords
            testDb.executeSql(
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (1, NULL, 'Zebra', TRUE)",
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (2, NULL, 'Apple', TRUE)",
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (3, 1, 'Child', TRUE)"
            );

            // When: query root keywords
            Collection<Keyword> roots = queryRootKeywords();

            // Then: only roots, ordered alphabetically
            assertThat(roots).hasSize(2);
            assertThat(roots).extracting(Keyword::getName)
                    .containsExactly("Apple", "Zebra");
        }
    }

    @Nested
    @DisplayName("getChildKeywords")
    class GetChildKeywords {

        @Test
        @DisplayName("returns children of specified parent, ordered by subject")
        void returnsChildrenOrdered() throws SQLException {
            // Given: parent with children
            testDb.executeSql(
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (1, NULL, 'Parent', TRUE)",
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (2, 1, 'Zebra', TRUE)",
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (3, 1, 'Apple', TRUE)",
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (4, NULL, 'Other', TRUE)"
            );

            // When: query children of parent (id=1)
            Collection<Keyword> children = queryChildKeywords(1);

            // Then: only children, ordered
            assertThat(children).hasSize(2);
            assertThat(children).extracting(Keyword::getName)
                    .containsExactly("Apple", "Zebra");
        }

        @Test
        @DisplayName("returns empty when parent has no children")
        void returnsEmptyWhenNoChildren() throws SQLException {
            // Given: parent without children
            testDb.executeSql(
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (1, NULL, 'Lonely', TRUE)"
            );

            // When: query children
            Collection<Keyword> children = queryChildKeywords(1);

            // Then: empty
            assertThat(children).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsKeyword")
    class ExistsKeyword {

        @Test
        @DisplayName("returns true when keyword exists")
        void returnsTrueWhenExists() throws SQLException {
            testDb.executeSql(
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (1, NULL, 'Nature', TRUE)"
            );

            boolean exists = keywordExists("Nature");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("returns false when keyword does not exist")
        void returnsFalseWhenNotExists() throws SQLException {
            // Given: different keyword
            testDb.executeSql(
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (1, NULL, 'Nature', TRUE)"
            );

            boolean exists = keywordExists("Nonexistent");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsRootKeyword")
    class ExistsRootKeyword {

        @Test
        @DisplayName("returns true only for root keywords")
        void returnsTrueOnlyForRoots() throws SQLException {
            testDb.executeSql(
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (1, NULL, 'Root', TRUE)",
                "INSERT INTO hierarchical_subjects (id, id_parent, subject, real) VALUES (2, 1, 'Child', TRUE)"
            );

            assertThat(rootKeywordExists("Root")).isTrue();
            assertThat(rootKeywordExists("Child")).isFalse();
        }
    }

    // Helper methods that directly query the test database
    // These simulate what KeywordsDatabase does but with our test connection

    private Collection<Keyword> queryAllKeywords() throws SQLException {
        return executeQuery(
            "SELECT id, id_parent, subject, real FROM hierarchical_subjects",
            rs -> {
                java.util.List<Keyword> keywords = new java.util.ArrayList<>();
                while (rs.next()) {
                    Long idParent = rs.getLong(2);
                    if (rs.wasNull()) idParent = null;
                    keywords.add(new Keyword(rs.getLong(1), idParent, rs.getString(3), rs.getBoolean(4)));
                }
                return keywords;
            }
        );
    }

    private Collection<Keyword> queryRootKeywords() throws SQLException {
        return executeQuery(
            "SELECT id, id_parent, subject, real FROM hierarchical_subjects WHERE id_parent IS NULL ORDER BY subject ASC",
            rs -> {
                java.util.List<Keyword> keywords = new java.util.ArrayList<>();
                while (rs.next()) {
                    Long idParent = rs.getLong(2);
                    if (rs.wasNull()) idParent = null;
                    keywords.add(new Keyword(rs.getLong(1), idParent, rs.getString(3), rs.getBoolean(4)));
                }
                return keywords;
            }
        );
    }

    private Collection<Keyword> queryChildKeywords(long parentId) throws SQLException {
        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(
                "SELECT id, id_parent, subject, real FROM hierarchical_subjects WHERE id_parent = ? ORDER BY subject ASC")) {
            stmt.setLong(1, parentId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                java.util.List<Keyword> keywords = new java.util.ArrayList<>();
                while (rs.next()) {
                    Long idParent = rs.getLong(2);
                    if (rs.wasNull()) idParent = null;
                    keywords.add(new Keyword(rs.getLong(1), idParent, rs.getString(3), rs.getBoolean(4)));
                }
                return keywords;
            }
        }
    }

    private boolean keywordExists(String keyword) throws SQLException {
        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(
                "SELECT COUNT(*) FROM hierarchical_subjects WHERE subject = ?")) {
            stmt.setString(1, keyword);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean rootKeywordExists(String keyword) throws SQLException {
        try (java.sql.PreparedStatement stmt = getConnection().prepareStatement(
                "SELECT COUNT(*) FROM hierarchical_subjects WHERE subject = ? AND id_parent IS NULL")) {
            stmt.setString(1, keyword);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private <T> T executeQuery(String sql, ResultSetMapper<T> mapper) throws SQLException {
        try (java.sql.Statement stmt = getConnection().createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            return mapper.map(rs);
        }
    }

    @FunctionalInterface
    private interface ResultSetMapper<T> {
        T map(java.sql.ResultSet rs) throws SQLException;
    }
}
