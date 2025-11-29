package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SqliteApplicationPropertiesDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteApplicationPropertiesDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteApplicationPropertiesDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void existsKey_returnsFalseWhenKeyDoesNotExist() {
        boolean exists = database.existsKey("nonexistent.key");

        assertThat(exists).isFalse();
    }

    @Test
    void existsKey_returnsTrueWhenKeyExists() {
        database.setString("test.key", "test.value");

        boolean exists = database.existsKey("test.key");

        assertThat(exists).isTrue();
    }

    @Test
    void existsKey_throwsNullPointerExceptionWhenKeyIsNull() {
        assertThatThrownBy(() -> database.existsKey(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("key == null");
    }

    @Test
    void setString_insertsNewKeyValue() throws Exception {
        database.setString("new.key", "new.value");

        assertThat(database.existsKey("new.key")).isTrue();
        assertThat(database.getString("new.key")).isEqualTo("new.value");

        // Verify in database
        String sql = "SELECT value FROM application WHERE key = ?";
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, "new.key");
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                byte[] bytes = rs.getBytes("value");
                assertThat(new String(bytes)).isEqualTo("new.value");
            }
        }
    }

    @Test
    void setString_updatesExistingKeyValue() throws Exception {
        database.setString("update.key", "old.value");
        database.setString("update.key", "new.value");

        assertThat(database.getString("update.key")).isEqualTo("new.value");

        // Verify only one entry exists
        String sql = "SELECT COUNT(*) FROM application WHERE key = ?";
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, "update.key");
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void setString_throwsNullPointerExceptionWhenKeyIsNull() {
        assertThatThrownBy(() -> database.setString(null, "value"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("key == null");
    }

    @Test
    void setString_throwsNullPointerExceptionWhenValueIsNull() {
        assertThatThrownBy(() -> database.setString("key", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value == null");
    }

    @Test
    void getString_returnsValueForExistingKey() {
        database.setString("get.key", "get.value");

        String value = database.getString("get.key");

        assertThat(value).isEqualTo("get.value");
    }

    @Test
    void getString_returnsNullForNonExistentKey() {
        String value = database.getString("nonexistent.key");

        assertThat(value).isNull();
    }

    @Test
    void getString_throwsNullPointerExceptionWhenKeyIsNull() {
        assertThatThrownBy(() -> database.getString(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("key == null");
    }

    @Test
    void deleteKey_removesExistingKey() {
        database.setString("delete.key", "delete.value");
        assertThat(database.existsKey("delete.key")).isTrue();

        database.deleteKey("delete.key");

        assertThat(database.existsKey("delete.key")).isFalse();
        assertThat(database.getString("delete.key")).isNull();
    }

    @Test
    void deleteKey_doesNotFailWhenKeyDoesNotExist() {
        // Should not throw an exception
        database.deleteKey("nonexistent.key");

        assertThat(database.existsKey("nonexistent.key")).isFalse();
    }

    @Test
    void deleteKey_throwsNullPointerExceptionWhenKeyIsNull() {
        assertThatThrownBy(() -> database.deleteKey(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("key == null");
    }

    @Test
    void getBoolean_returnsFalseWhenKeyDoesNotExist() {
        boolean value = database.getBoolean("nonexistent.key");

        assertThat(value).isFalse();
    }

    @Test
    void getBoolean_returnsTrueWhenValueIsOne() {
        database.setBoolean("bool.key", true);

        boolean value = database.getBoolean("bool.key");

        assertThat(value).isTrue();
    }

    @Test
    void getBoolean_returnsFalseWhenValueIsZero() {
        database.setBoolean("bool.key", false);

        boolean value = database.getBoolean("bool.key");

        assertThat(value).isFalse();
    }

    @Test
    void getBoolean_throwsNullPointerExceptionWhenKeyIsNull() {
        assertThatThrownBy(() -> database.getBoolean(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("key == null");
    }

    @Test
    void setBoolean_storesOneForTrue() {
        database.setBoolean("bool.true.key", true);

        assertThat(database.getString("bool.true.key")).isEqualTo("1");
        assertThat(database.getBoolean("bool.true.key")).isTrue();
    }

    @Test
    void setBoolean_storesZeroForFalse() {
        database.setBoolean("bool.false.key", false);

        assertThat(database.getString("bool.false.key")).isEqualTo("0");
        assertThat(database.getBoolean("bool.false.key")).isFalse();
    }

    @Test
    void setBoolean_throwsNullPointerExceptionWhenKeyIsNull() {
        assertThatThrownBy(() -> database.setBoolean(null, true))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("key == null");
    }

    @Test
    void getAllKeys_returnsEmptyListWhenNoKeys() {
        List<String> keys = database.getAllKeys();

        assertThat(keys).isEmpty();
    }

    @Test
    void getAllKeys_returnsAllStoredKeys() {
        database.setString("key1", "value1");
        database.setString("key2", "value2");
        database.setString("key3", "value3");

        List<String> keys = database.getAllKeys();

        assertThat(keys).hasSize(3);
        assertThat(keys).containsExactlyInAnyOrder("key1", "key2", "key3");
    }

    @Test
    void getPropertiesCount_returnsZeroWhenNoProperties() {
        int count = database.getPropertiesCount();

        assertThat(count).isZero();
    }

    @Test
    void getPropertiesCount_returnsCorrectCount() {
        database.setString("prop1", "value1");
        database.setString("prop2", "value2");
        database.setString("prop3", "value3");

        int count = database.getPropertiesCount();

        assertThat(count).isEqualTo(3);
    }

    @Test
    void getPropertiesCount_updatesAfterDeletion() {
        database.setString("prop1", "value1");
        database.setString("prop2", "value2");
        assertThat(database.getPropertiesCount()).isEqualTo(2);

        database.deleteKey("prop1");

        assertThat(database.getPropertiesCount()).isEqualTo(1);
    }

    @Test
    void setString_handlesBlobStorageCorrectly() throws Exception {
        String testValue = "Value with special chars: \n\t\r";
        database.setString("special.key", testValue);

        String retrieved = database.getString("special.key");

        assertThat(retrieved).isEqualTo(testValue);
    }

    @Test
    void getString_handlesNullBytesInDatabase() throws Exception {
        // Insert a null value directly into the database
        String sql = "INSERT INTO application (key, value) VALUES (?, ?)";
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, "null.value.key");
            stmt.setBytes(2, null);
            stmt.executeUpdate();
        }

        String value = database.getString("null.value.key");

        assertThat(value).isNull();
    }
}
