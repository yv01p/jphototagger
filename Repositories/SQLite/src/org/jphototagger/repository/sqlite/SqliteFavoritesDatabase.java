package org.jphototagger.repository.sqlite;

import org.jphototagger.domain.favorites.Favorite;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation for favorite directories database operations.
 * Manages favorite directories with their aliases and ordering.
 */
public class SqliteFavoritesDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteFavoritesDatabase.class.getName());

    public SqliteFavoritesDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    /**
     * Returns all favorites sorted by index.
     *
     * @return list of favorites in index order, empty if none exist
     */
    public List<Favorite> getAllFavorites() {
        List<Favorite> favorites = new ArrayList<>();
        String sql = "SELECT id, favorite_name, directory_name, favorite_index"
                + " FROM favorite_directories ORDER BY favorite_index ASC";
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Favorite favorite = new Favorite();
                favorite.setId(rs.getLong(1));
                favorite.setName(rs.getString(2));
                favorite.setDirectory(new File(rs.getString(3)));
                favorite.setIndex(rs.getInt(4));
                favorites.add(favorite);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all favorites", e);
            favorites.clear();
        }
        return favorites;
    }

    /**
     * Checks if a favorite exists by name.
     *
     * @param favoriteName name of the favorite
     * @return true if the favorite exists
     * @throws NullPointerException if favoriteName is null
     */
    public boolean existsFavorite(String favoriteName) {
        if (favoriteName == null) {
            throw new NullPointerException("favoriteName == null");
        }
        String sql = "SELECT COUNT(*) FROM favorite_directories WHERE favorite_name = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, favoriteName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking favorite existence", e);
            return false;
        }
    }

    /**
     * Inserts a new favorite or updates an existing one by name.
     * If a favorite with the same name exists, it will be updated.
     * Sets the ID on the favorite object after insertion.
     *
     * @param favorite the favorite to insert or update
     * @return true if successfully inserted or updated
     * @throws NullPointerException if favorite is null
     */
    public boolean insertOrUpdateFavorite(Favorite favorite) {
        if (favorite == null) {
            throw new NullPointerException("favorite == null");
        }

        if (existsFavorite(favorite.getName())) {
            return updateFavoriteByName(favorite);
        }

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            stmt = con.prepareStatement(
                    "INSERT INTO favorite_directories"
                            + " (favorite_name, directory_name, favorite_index)"
                            + " VALUES (?, ?, ?)");
            stmt.setString(1, favorite.getName());
            stmt.setString(2, favorite.getDirectory().getAbsolutePath());
            stmt.setInt(3, favorite.getIndex());
            int count = stmt.executeUpdate();
            con.commit();

            if (count > 0) {
                // Set the ID on the favorite object
                Long id = findIdByFavoriteName(favorite.getName());
                favorite.setId(id);
                return true;
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting or updating favorite", e);
            rollback(con);
            return false;
        } finally {
            close(stmt);
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    /**
     * Deletes a favorite by name.
     *
     * @param favoriteName name of the favorite to delete
     * @return true if a favorite was deleted
     * @throws NullPointerException if favoriteName is null
     */
    public boolean deleteFavorite(String favoriteName) {
        if (favoriteName == null) {
            throw new NullPointerException("favoriteName == null");
        }
        String sql = "DELETE FROM favorite_directories WHERE favorite_name = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, favoriteName);
            int deletedRows = stmt.executeUpdate();
            return deletedRows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting favorite", e);
            return false;
        }
    }

    /**
     * Updates a favorite by ID.
     * Updates the name, directory, and index for the favorite with the given ID.
     *
     * @param favorite the favorite to update (must have a valid ID)
     * @return true if successfully updated
     * @throws NullPointerException if favorite is null
     */
    public boolean updateFavorite(Favorite favorite) {
        if (favorite == null) {
            throw new NullPointerException("favorite == null");
        }

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            stmt = con.prepareStatement(
                    "UPDATE favorite_directories SET"
                            + " favorite_name = ?, directory_name = ?, favorite_index = ?"
                            + " WHERE id = ?");
            stmt.setString(1, favorite.getName());
            stmt.setString(2, favorite.getDirectory().getAbsolutePath());
            stmt.setInt(3, favorite.getIndex());
            stmt.setLong(4, favorite.getId());
            int count = stmt.executeUpdate();
            con.commit();
            return count > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating favorite", e);
            rollback(con);
            return false;
        } finally {
            close(stmt);
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    /**
     * Renames a favorite.
     * Changes the favorite name from one value to another.
     *
     * @param fromFavoriteName current name
     * @param toFavoriteName new name
     * @return true if successfully renamed
     * @throws NullPointerException if either parameter is null
     */
    public boolean updateRenameFavorite(String fromFavoriteName, String toFavoriteName) {
        if (fromFavoriteName == null) {
            throw new NullPointerException("fromFavoriteName == null");
        }
        if (toFavoriteName == null) {
            throw new NullPointerException("toFavoriteName == null");
        }

        String sql = "UPDATE favorite_directories SET favorite_name = ? WHERE favorite_name = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, toFavoriteName);
            stmt.setString(2, fromFavoriteName);
            int count = stmt.executeUpdate();
            return count > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error renaming favorite", e);
            return false;
        }
    }

    /**
     * Finds a favorite by its ID.
     *
     * @param id the ID to search for
     * @return the favorite if found, null otherwise
     */
    public Favorite findById(Long id) {
        if (id == null) {
            return null;
        }

        String sql = "SELECT id, favorite_name, directory_name, favorite_index"
                + " FROM favorite_directories WHERE id = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Favorite favorite = new Favorite();
                    favorite.setId(rs.getLong(1));
                    favorite.setName(rs.getString(2));
                    favorite.setDirectory(new File(rs.getString(3)));
                    favorite.setIndex(rs.getInt(4));
                    return favorite;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding favorite by ID", e);
        }
        return null;
    }

    /**
     * Finds a favorite by its name.
     *
     * @param favoriteName the name to search for
     * @return the favorite if found, null otherwise
     */
    public Favorite findByName(String favoriteName) {
        if (favoriteName == null) {
            return null;
        }

        String sql = "SELECT id, favorite_name, directory_name, favorite_index"
                + " FROM favorite_directories WHERE favorite_name = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, favoriteName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Favorite favorite = new Favorite();
                    favorite.setId(rs.getLong(1));
                    favorite.setName(rs.getString(2));
                    favorite.setDirectory(new File(rs.getString(3)));
                    favorite.setIndex(rs.getInt(4));
                    return favorite;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding favorite by name", e);
        }
        return null;
    }

    // Private helper methods

    /**
     * Updates a favorite by name (used internally by insertOrUpdateFavorite).
     */
    private boolean updateFavoriteByName(Favorite favorite) {
        String sql = "UPDATE favorite_directories SET"
                + " directory_name = ?, favorite_index = ?"
                + " WHERE favorite_name = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, favorite.getDirectory().getAbsolutePath());
            stmt.setInt(2, favorite.getIndex());
            stmt.setString(3, favorite.getName());
            int count = stmt.executeUpdate();
            if (count > 0) {
                // Set the ID on the favorite object
                Long id = findIdByFavoriteName(favorite.getName());
                favorite.setId(id);
                return true;
            }
            return false;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating favorite by name", e);
            return false;
        }
    }

    /**
     * Finds the ID of a favorite by its name.
     */
    private Long findIdByFavoriteName(String favoriteName) {
        String sql = "SELECT id FROM favorite_directories WHERE favorite_name = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, favoriteName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding favorite ID by name", e);
        }
        return null;
    }
}
