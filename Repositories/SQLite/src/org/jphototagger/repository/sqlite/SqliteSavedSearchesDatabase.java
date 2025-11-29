package org.jphototagger.repository.sqlite;

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
 * SQLite implementation for saved searches database operations.
 * Mirrors the HSQLDB SavedSearchesDatabase functionality but without EventBus notifications.
 */
public final class SqliteSavedSearchesDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteSavedSearchesDatabase.class.getName());

    public SqliteSavedSearchesDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    /**
     * Returns the count of saved searches.
     *
     * @return count or -1 on error
     */
    public int getCount() {
        int count = -1;
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            String sql = "SELECT COUNT(*) FROM saved_searches";
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
     * Checks if a saved search exists by name.
     *
     * @param name the search name
     * @return true if exists
     */
    public boolean exists(String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        try (Connection con = getConnection()) {
            long id = findId(con, name);
            return id > 0;
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return false;
    }

    /**
     * Inserts a saved search.
     *
     * @param name the search name
     * @param customSql the custom SQL (can be null)
     * @param searchType the search type (0 = KEYWORDS_AND_PANELS, 1 = CUSTOM_SQL)
     * @return true if inserted
     */
    public boolean insert(String name, String customSql, short searchType) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        boolean inserted = false;
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement stmt = con.prepareStatement(getInsertSql())) {
                stmt.setString(1, name);
                if (customSql != null) {
                    stmt.setBytes(2, customSql.getBytes());
                } else {
                    stmt.setNull(2, java.sql.Types.BLOB);
                }
                stmt.setShort(3, searchType);
                LOGGER.log(Level.FINER, stmt.toString());
                stmt.executeUpdate();
                con.commit();
                inserted = true;
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, null, t);
                rollback(con);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return inserted;
    }

    /**
     * Deletes a saved search by name.
     * Cascade deletes will remove associated panels and keywords via foreign keys.
     *
     * @param name the search name
     * @return true if deleted
     */
    public boolean delete(String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        boolean deleted = false;
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement stmt = con.prepareStatement("DELETE FROM saved_searches WHERE name = ?")) {
                stmt.setString(1, name);
                LOGGER.log(Level.FINER, stmt.toString());
                int count = stmt.executeUpdate();
                con.commit();
                deleted = count > 0;
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, null, t);
                rollback(con);
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return deleted;
    }

    /**
     * Renames a saved search.
     *
     * @param fromName the current name
     * @param toName the new name
     * @return true if renamed
     */
    public boolean updateRename(String fromName, String toName) {
        if (fromName == null) {
            throw new NullPointerException("fromName == null");
        }
        if (toName == null) {
            throw new NullPointerException("toName == null");
        }
        boolean renamed = false;
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement("UPDATE saved_searches SET name = ? WHERE name = ?")) {
            con.setAutoCommit(true);
            stmt.setString(1, toName);
            stmt.setString(2, fromName);
            LOGGER.log(Level.FINER, stmt.toString());
            int count = stmt.executeUpdate();
            renamed = count > 0;
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
        }
        return renamed;
    }

    /**
     * Finds a saved search by name.
     *
     * @param name the search name
     * @return the saved search data or null if not found
     */
    public SavedSearchData find(String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        SavedSearchData savedSearch = null;
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(getFindSql())) {
            stmt.setString(1, name);
            LOGGER.log(Level.FINEST, stmt.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String searchName = rs.getString(1);
                    byte[] customSqlBytes = rs.getBytes(2);
                    String customSql = customSqlBytes != null ? new String(customSqlBytes) : null;
                    short searchType = rs.getShort(3);
                    savedSearch = new SavedSearchData(searchName, customSql, searchType);
                }
            }
        } catch (Throwable t) {
            savedSearch = null;
            LOGGER.log(Level.SEVERE, null, t);
        }
        return savedSearch;
    }

    /**
     * Returns all saved searches sorted by name.
     *
     * @return list of saved searches
     */
    public List<SavedSearchData> getAll() {
        List<SavedSearchData> searches = new ArrayList<>();
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            String sql = getGetAllSql();
            LOGGER.log(Level.FINEST, sql);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String name = rs.getString(1);
                    byte[] customSqlBytes = rs.getBytes(2);
                    String customSql = customSqlBytes != null ? new String(customSqlBytes) : null;
                    short searchType = rs.getShort(3);
                    SavedSearchData savedSearch = new SavedSearchData(name, customSql, searchType);
                    searches.add(savedSearch);
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, null, t);
            searches.clear();
        }
        return searches;
    }

    // Private helper methods

    private String getInsertSql() {
        return "INSERT INTO saved_searches (name, custom_sql, search_type) VALUES (?, ?, ?)";
    }

    private String getFindSql() {
        return "SELECT name, custom_sql, search_type FROM saved_searches WHERE name = ?";
    }

    private String getGetAllSql() {
        return "SELECT name, custom_sql, search_type FROM saved_searches ORDER BY name";
    }

    private long findId(Connection con, String name) throws SQLException {
        long id = -1;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT id FROM saved_searches WHERE name = ?");
            stmt.setString(1, name);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                id = rs.getLong(1);
            }
        } finally {
            close(rs, stmt);
        }
        return id;
    }

    /**
     * Simple data holder for saved search data.
     * Avoids using domain classes that may have GUI dependencies.
     */
    public record SavedSearchData(String name, String customSql, short searchType) {}
}
