package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jphototagger.domain.favorites.Favorite;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SqliteFavoritesDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteFavoritesDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteFavoritesDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getAllFavorites_returnsEmptyListWhenNoFavorites() {
        List<Favorite> favorites = database.getAllFavorites();

        assertThat(favorites).isEmpty();
    }

    @Test
    void getAllFavorites_returnsFavoritesSortedByIndex() throws Exception {
        // Insert favorites with different indices
        insertFavoriteDirectly("Fav1", "/dir1", 2);
        insertFavoriteDirectly("Fav2", "/dir2", 0);
        insertFavoriteDirectly("Fav3", "/dir3", 1);

        List<Favorite> favorites = database.getAllFavorites();

        assertThat(favorites).hasSize(3);
        assertThat(favorites.get(0).getName()).isEqualTo("Fav2");
        assertThat(favorites.get(0).getIndex()).isEqualTo(0);
        assertThat(favorites.get(1).getName()).isEqualTo("Fav3");
        assertThat(favorites.get(1).getIndex()).isEqualTo(1);
        assertThat(favorites.get(2).getName()).isEqualTo("Fav1");
        assertThat(favorites.get(2).getIndex()).isEqualTo(2);
    }

    @Test
    void existsFavorite_returnsTrueWhenExists() {
        Favorite favorite = new Favorite();
        favorite.setName("TestFavorite");
        favorite.setDirectory(new File("/test/dir"));
        favorite.setIndex(0);
        database.insertOrUpdateFavorite(favorite);

        boolean exists = database.existsFavorite("TestFavorite");

        assertThat(exists).isTrue();
    }

    @Test
    void existsFavorite_returnsFalseWhenNotExists() {
        boolean exists = database.existsFavorite("NonExistent");

        assertThat(exists).isFalse();
    }

    @Test
    void insertFavorite_insertsNewFavorite() throws Exception {
        Favorite favorite = new Favorite();
        favorite.setName("NewFavorite");
        favorite.setDirectory(new File("/new/dir"));
        favorite.setIndex(5);

        boolean inserted = database.insertOrUpdateFavorite(favorite);

        assertThat(inserted).isTrue();
        assertThat(database.existsFavorite("NewFavorite")).isTrue();

        // Verify in database
        String sql = "SELECT favorite_name, directory_name, favorite_index FROM favorite_directories WHERE favorite_name = ?";
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, "NewFavorite");
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("favorite_name")).isEqualTo("NewFavorite");
                assertThat(rs.getString("directory_name")).isEqualTo("/new/dir");
                assertThat(rs.getInt("favorite_index")).isEqualTo(5);
            }
        }
    }

    @Test
    void insertFavorite_updatesExistingFavorite() throws Exception {
        // Insert initial favorite
        Favorite favorite = new Favorite();
        favorite.setName("ExistingFavorite");
        favorite.setDirectory(new File("/old/dir"));
        favorite.setIndex(1);
        database.insertOrUpdateFavorite(favorite);

        // Update the same favorite with new directory
        Favorite updated = new Favorite();
        updated.setName("ExistingFavorite");
        updated.setDirectory(new File("/new/dir"));
        updated.setIndex(2);
        boolean result = database.insertOrUpdateFavorite(updated);

        assertThat(result).isTrue();

        // Verify only one entry exists with updated values
        String sql = "SELECT COUNT(*), directory_name, favorite_index FROM favorite_directories WHERE favorite_name = ?";
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, "ExistingFavorite");
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1); // Only one entry
                assertThat(rs.getString("directory_name")).isEqualTo("/new/dir");
                assertThat(rs.getInt("favorite_index")).isEqualTo(2);
            }
        }
    }

    @Test
    void deleteFavorite_removesFavorite() {
        Favorite favorite = new Favorite();
        favorite.setName("ToDelete");
        favorite.setDirectory(new File("/test/dir"));
        favorite.setIndex(0);
        database.insertOrUpdateFavorite(favorite);

        boolean deleted = database.deleteFavorite("ToDelete");

        assertThat(deleted).isTrue();
        assertThat(database.existsFavorite("ToDelete")).isFalse();
    }

    @Test
    void deleteFavorite_returnsFalseWhenNotExists() {
        boolean deleted = database.deleteFavorite("NonExistent");

        assertThat(deleted).isFalse();
    }

    @Test
    void updateFavorite_updatesExistingById() throws Exception {
        // Insert initial favorite
        Favorite favorite = new Favorite();
        favorite.setName("OriginalName");
        favorite.setDirectory(new File("/original/dir"));
        favorite.setIndex(1);
        database.insertOrUpdateFavorite(favorite);

        // Get the ID
        List<Favorite> favorites = database.getAllFavorites();
        assertThat(favorites).hasSize(1);
        Long id = favorites.get(0).getId();

        // Update by ID
        Favorite updated = new Favorite();
        updated.setId(id);
        updated.setName("UpdatedName");
        updated.setDirectory(new File("/updated/dir"));
        updated.setIndex(3);

        boolean result = database.updateFavorite(updated);

        assertThat(result).isTrue();

        // Verify update
        String sql = "SELECT favorite_name, directory_name, favorite_index FROM favorite_directories WHERE id = ?";
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("favorite_name")).isEqualTo("UpdatedName");
                assertThat(rs.getString("directory_name")).isEqualTo("/updated/dir");
                assertThat(rs.getInt("favorite_index")).isEqualTo(3);
            }
        }
    }

    @Test
    void renameFavorite_changesName() throws Exception {
        Favorite favorite = new Favorite();
        favorite.setName("OldName");
        favorite.setDirectory(new File("/test/dir"));
        favorite.setIndex(0);
        database.insertOrUpdateFavorite(favorite);

        boolean renamed = database.updateRenameFavorite("OldName", "NewName");

        assertThat(renamed).isTrue();
        assertThat(database.existsFavorite("OldName")).isFalse();
        assertThat(database.existsFavorite("NewName")).isTrue();

        // Verify directory and index are preserved
        String sql = "SELECT directory_name, favorite_index FROM favorite_directories WHERE favorite_name = ?";
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, "NewName");
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("directory_name")).isEqualTo("/test/dir");
                assertThat(rs.getInt("favorite_index")).isEqualTo(0);
            }
        }
    }

    @Test
    void findById_returnsFavorite() {
        Favorite favorite = new Favorite();
        favorite.setName("FindMe");
        favorite.setDirectory(new File("/find/dir"));
        favorite.setIndex(7);
        database.insertOrUpdateFavorite(favorite);

        List<Favorite> favorites = database.getAllFavorites();
        assertThat(favorites).hasSize(1);
        Long id = favorites.get(0).getId();

        Favorite found = database.findById(id);

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getName()).isEqualTo("FindMe");
        assertThat(found.getDirectory()).isEqualTo(new File("/find/dir"));
        assertThat(found.getIndex()).isEqualTo(7);
    }

    @Test
    void findById_returnsNullWhenNotFound() {
        Favorite found = database.findById(999L);

        assertThat(found).isNull();
    }

    @Test
    void findByName_returnsFavorite() {
        Favorite favorite = new Favorite();
        favorite.setName("FindByName");
        favorite.setDirectory(new File("/find/dir"));
        favorite.setIndex(3);
        database.insertOrUpdateFavorite(favorite);

        Favorite found = database.findByName("FindByName");

        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("FindByName");
        assertThat(found.getDirectory()).isEqualTo(new File("/find/dir"));
        assertThat(found.getIndex()).isEqualTo(3);
    }

    @Test
    void findByName_returnsNullWhenNotFound() {
        Favorite found = database.findByName("NonExistent");

        assertThat(found).isNull();
    }

    @Test
    void insertOrUpdateFavorite_setsIdOnInsert() {
        Favorite favorite = new Favorite();
        favorite.setName("NewWithId");
        favorite.setDirectory(new File("/test/dir"));
        favorite.setIndex(0);

        database.insertOrUpdateFavorite(favorite);

        assertThat(favorite.getId()).isNotNull();
        assertThat(favorite.getId()).isGreaterThan(0L);
    }

    @Test
    void existsFavorite_throwsNullPointerExceptionWhenNameIsNull() {
        assertThatThrownBy(() -> database.existsFavorite(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("favoriteName == null");
    }

    @Test
    void deleteFavorite_throwsNullPointerExceptionWhenNameIsNull() {
        assertThatThrownBy(() -> database.deleteFavorite(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("favoriteName == null");
    }

    @Test
    void updateRenameFavorite_throwsNullPointerExceptionWhenFromNameIsNull() {
        assertThatThrownBy(() -> database.updateRenameFavorite(null, "ToName"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("fromFavoriteName == null");
    }

    @Test
    void updateRenameFavorite_throwsNullPointerExceptionWhenToNameIsNull() {
        assertThatThrownBy(() -> database.updateRenameFavorite("FromName", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("toFavoriteName == null");
    }

    @Test
    void insertOrUpdateFavorite_throwsNullPointerExceptionWhenFavoriteIsNull() {
        assertThatThrownBy(() -> database.insertOrUpdateFavorite(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("favorite == null");
    }

    @Test
    void updateFavorite_throwsNullPointerExceptionWhenFavoriteIsNull() {
        assertThatThrownBy(() -> database.updateFavorite(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("favorite == null");
    }

    // Helper method to insert favorites directly via SQL for testing
    private void insertFavoriteDirectly(String name, String directory, int index) throws Exception {
        String sql = "INSERT INTO favorite_directories (favorite_name, directory_name, favorite_index) VALUES (?, ?, ?)";
        try (Connection con = factory.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, directory);
            stmt.setInt(3, index);
            stmt.executeUpdate();
        }
    }
}
