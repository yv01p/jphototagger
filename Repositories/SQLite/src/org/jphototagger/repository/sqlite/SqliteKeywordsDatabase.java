package org.jphototagger.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SQLite implementation for dc_subjects (keywords) database operations.
 * Handles Dublin Core subject metadata.
 */
public class SqliteKeywordsDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteKeywordsDatabase.class.getName());

    public SqliteKeywordsDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    /**
     * Returns all Dublin core subjects ordered ascending.
     *
     * @return all subjects
     */
    public Set<String> getAllDcSubjects() {
        Set<String> dcSubjects = new LinkedHashSet<>();
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = "SELECT subject FROM dc_subjects ORDER BY subject ASC";
            stmt = con.createStatement();
            LOGGER.log(Level.FINEST, sql);
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                dcSubjects.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
        } finally {
            close(rs, stmt);
        }
        return dcSubjects;
    }

    /**
     * Inserts a Dublin core subject.
     * Does not check whether it already exists. In that case
     * an SQLException will be caught and false returned.
     *
     * @param dcSubject subject
     * @return true if inserted
     */
    public boolean insertDcSubject(String dcSubject) {
        if (dcSubject == null) {
            throw new NullPointerException("dcSubject == null");
        }
        boolean inserted = false;
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            String sql = "INSERT INTO dc_subjects (subject) VALUES (?)";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, dcSubject);
            LOGGER.log(Level.FINER, stmt.toString());
            int count = stmt.executeUpdate();
            inserted = count == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
        } finally {
            close(stmt);
        }
        return inserted;
    }

    /**
     * Deletes a Dublin core subject.
     *
     * @param dcSubject subject to delete
     */
    public void deleteDcSubject(String dcSubject) {
        if (dcSubject == null) {
            throw new NullPointerException("dcSubject == null");
        }
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            String sql = "DELETE FROM dc_subjects WHERE subject = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, dcSubject);
            LOGGER.log(Level.FINER, stmt.toString());
            stmt.executeUpdate();
            con.commit();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            rollback(con);
        } finally {
            close(stmt);
        }
    }

    /**
     * Returns whether a Dublin core subject exists.
     *
     * @param subject subject
     * @return true if exists
     */
    public boolean existsDcSubject(String subject) {
        if (subject == null) {
            throw new NullPointerException("subject == null");
        }
        boolean exists = false;
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = "SELECT COUNT(*) FROM dc_subjects WHERE subject = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, subject);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                exists = rs.getInt(1) == 1;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
        } finally {
            close(rs, stmt);
        }
        return exists;
    }

    /**
     * Returns the ID of a Dublin core subject.
     *
     * @param dcSubject subject
     * @return ID or null if not exists
     */
    public Long findIdDcSubject(String dcSubject) {
        if (dcSubject == null) {
            throw new NullPointerException("dcSubject == null");
        }
        Long id = null;
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = "SELECT id FROM dc_subjects WHERE subject = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, dcSubject);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                id = rs.getLong(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
        } finally {
            close(rs, stmt);
        }
        return id;
    }
}
