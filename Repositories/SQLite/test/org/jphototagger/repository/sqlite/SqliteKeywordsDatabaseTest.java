package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class SqliteKeywordsDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteKeywordsDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteKeywordsDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getAllDcSubjects_returnsEmptySetWhenNone() {
        Set<String> subjects = database.getAllDcSubjects();

        assertThat(subjects).isEmpty();
    }

    @Test
    void insertDcSubject_insertsAndReturnsTrue() {
        String subject = "nature";

        boolean inserted = database.insertDcSubject(subject);

        assertThat(inserted).isTrue();
    }

    @Test
    void insertDcSubject_insertsMultipleSubjects() {
        database.insertDcSubject("nature");
        database.insertDcSubject("wildlife");

        Set<String> subjects = database.getAllDcSubjects();

        assertThat(subjects).containsExactlyInAnyOrder("nature", "wildlife");
    }

    @Test
    void getAllDcSubjects_returnsAllSubjectsOrderedAscending() {
        database.insertDcSubject("zebra");
        database.insertDcSubject("apple");
        database.insertDcSubject("mountain");

        Set<String> subjects = database.getAllDcSubjects();

        // LinkedHashSet preserves insertion order from ORDER BY
        assertThat(subjects).containsExactly("apple", "mountain", "zebra");
    }

    @Test
    void deleteDcSubject_deletesExistingSubject() {
        database.insertDcSubject("nature");

        database.deleteDcSubject("nature");

        assertThat(database.getAllDcSubjects()).isEmpty();
    }

    @Test
    void deleteDcSubject_doesNothingWhenNotExists() {
        database.deleteDcSubject("nonexistent");

        assertThat(database.getAllDcSubjects()).isEmpty();
    }

    @Test
    void existsDcSubject_returnsTrueWhenExists() {
        database.insertDcSubject("nature");

        boolean exists = database.existsDcSubject("nature");

        assertThat(exists).isTrue();
    }

    @Test
    void existsDcSubject_returnsFalseWhenNotExists() {
        boolean exists = database.existsDcSubject("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    void findIdDcSubject_returnsIdWhenExists() {
        database.insertDcSubject("nature");

        Long id = database.findIdDcSubject("nature");

        assertThat(id).isNotNull();
        assertThat(id).isGreaterThan(0);
    }

    @Test
    void findIdDcSubject_returnsNullWhenNotExists() {
        Long id = database.findIdDcSubject("nonexistent");

        assertThat(id).isNull();
    }

    @Test
    void insertDcSubject_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.insertDcSubject(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dcSubject == null");
    }

    @Test
    void deleteDcSubject_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.deleteDcSubject(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dcSubject == null");
    }

    @Test
    void existsDcSubject_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.existsDcSubject(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("subject == null");
    }

    @Test
    void findIdDcSubject_throwsNullPointerExceptionForNull() {
        assertThatThrownBy(() -> database.findIdDcSubject(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dcSubject == null");
    }
}
