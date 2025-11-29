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
 * SQLite implementation for external programs database operations.
 * Manages external programs and actions that can be launched from the application,
 * including default program associations and actions after database insertion.
 */
public class SqliteProgramsDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteProgramsDatabase.class.getName());

    public SqliteProgramsDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    /**
     * Inserts a new program into the database.
     * Sets the ID and sequence number on the program object after insertion.
     *
     * @param program the program to insert
     * @return true if inserted successfully
     * @throws NullPointerException if program is null
     */
    public boolean insertProgram(ProgramRecord program) {
        if (program == null) {
            throw new NullPointerException("program == null");
        }

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Set ID
            setId(con, program);

            // Ensure sequence number
            ensureSequenceNumber(con, program);

            String sql = "INSERT INTO programs (id, action, filename, alias, "
                    + "parameters_before_filename, parameters_after_filename, "
                    + "input_before_execute, input_before_execute_per_file, "
                    + "single_file_processing, change_file, sequence_number, "
                    + "use_pattern, pattern) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            stmt = con.prepareStatement(sql);
            setValuesInsert(stmt, program);

            LOGGER.log(Level.FINER, stmt.toString());
            int count = stmt.executeUpdate();
            con.commit();

            return count == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting program", e);
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

    private void setValuesInsert(PreparedStatement stmt, ProgramRecord program) throws SQLException {
        stmt.setLong(1, program.id());
        stmt.setBoolean(2, program.action());
        stmt.setString(3, program.filename());
        stmt.setString(4, program.alias());
        setBlob(stmt, 5, program.parametersBeforeFilename());
        setBlob(stmt, 6, program.parametersAfterFilename());
        stmt.setBoolean(7, program.inputBeforeExecute());
        stmt.setBoolean(8, program.inputBeforeExecutePerFile());
        stmt.setBoolean(9, program.singleFileProcessing());
        stmt.setBoolean(10, program.changeFile());
        stmt.setInt(11, program.sequenceNumber());
        stmt.setBoolean(12, program.usePattern());
        setBlob(stmt, 13, program.pattern());
    }

    private void setId(Connection con, ProgramRecord program) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.createStatement();
            String sql = "SELECT MAX(id) FROM programs";
            LOGGER.log(Level.FINEST, sql);
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                long maxId = rs.getLong(1);
                program.setId(rs.wasNull() ? 1 : maxId + 1);
            } else {
                program.setId(1);
            }
        } finally {
            close(rs, stmt);
        }
    }

    private void ensureSequenceNumber(Connection con, ProgramRecord program) throws SQLException {
        if (program.sequenceNumber() >= 0) {
            return;
        }

        int count = getProgramCount(con, program.action());
        if (count <= 0) {
            program.setSequenceNumber(0);
            return;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT MAX(sequence_number) FROM programs WHERE action = ?";
            stmt = con.prepareStatement(sql);
            stmt.setBoolean(1, program.action());
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                int max = rs.getInt(1);
                program.setSequenceNumber((max < 0) ? 0 : max + 1);
            }
        } finally {
            close(rs, stmt);
        }
    }

    /**
     * Updates an existing program in the database.
     *
     * @param program the program to update (must have valid ID)
     * @return true if updated successfully
     * @throws NullPointerException if program is null
     */
    public boolean updateProgram(ProgramRecord program) {
        if (program == null) {
            throw new NullPointerException("program == null");
        }

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // Ensure sequence number if needed
            ensureSequenceNumber(con, program);

            String sql = "UPDATE programs SET action = ?, filename = ?, alias = ?, "
                    + "parameters_before_filename = ?, parameters_after_filename = ?, "
                    + "input_before_execute = ?, input_before_execute_per_file = ?, "
                    + "single_file_processing = ?, change_file = ?, sequence_number = ?, "
                    + "use_pattern = ?, pattern = ? "
                    + "WHERE id = ?";

            stmt = con.prepareStatement(sql);
            setValuesUpdate(stmt, program);
            stmt.setLong(13, program.id());

            LOGGER.log(Level.FINER, stmt.toString());
            int count = stmt.executeUpdate();
            con.commit();

            return count == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating program", e);
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

    private void setValuesUpdate(PreparedStatement stmt, ProgramRecord program) throws SQLException {
        stmt.setBoolean(1, program.action());
        stmt.setString(2, program.filename());
        stmt.setString(3, program.alias());
        setBlob(stmt, 4, program.parametersBeforeFilename());
        setBlob(stmt, 5, program.parametersAfterFilename());
        stmt.setBoolean(6, program.inputBeforeExecute());
        stmt.setBoolean(7, program.inputBeforeExecutePerFile());
        stmt.setBoolean(8, program.singleFileProcessing());
        stmt.setBoolean(9, program.changeFile());
        stmt.setInt(10, program.sequenceNumber());
        stmt.setBoolean(11, program.usePattern());
        setBlob(stmt, 12, program.pattern());
    }

    /**
     * Deletes a program from the database.
     * Also removes the program from default_programs and actions_after_db_insertion.
     *
     * @param program the program to delete
     * @return true if deleted successfully
     * @throws NullPointerException if program is null
     */
    public boolean deleteProgram(ProgramRecord program) {
        if (program == null) {
            throw new NullPointerException("program == null");
        }

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            stmt = con.prepareStatement("DELETE FROM programs WHERE id = ?");
            stmt.setLong(1, program.id());
            LOGGER.log(Level.FINER, stmt.toString());
            int count = stmt.executeUpdate();

            // Also delete from default_programs and actions_after_db_insertion
            deleteProgramFromDefaultPrograms(con, program.id());
            deleteProgramFromActionsAfterDbInsertion(con, program.id());

            con.commit();
            return count == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting program", e);
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

    private void deleteProgramFromDefaultPrograms(Connection con, long idProgram) throws SQLException {
        PreparedStatement stmt = null;
        try {
            String sql = "DELETE FROM default_programs WHERE id_program = ?";
            stmt = con.prepareStatement(sql);
            stmt.setLong(1, idProgram);
            LOGGER.log(Level.FINER, stmt.toString());
            stmt.executeUpdate();
        } finally {
            close(stmt);
        }
    }

    private void deleteProgramFromActionsAfterDbInsertion(Connection con, long idProgram) throws SQLException {
        PreparedStatement stmt = null;
        try {
            String sql = "DELETE FROM actions_after_db_insertion WHERE id_program = ?";
            stmt = con.prepareStatement(sql);
            stmt.setLong(1, idProgram);
            LOGGER.log(Level.FINER, stmt.toString());
            stmt.executeUpdate();
        } finally {
            close(stmt);
        }
    }

    /**
     * Returns all programs (programs and actions) ordered by sequence number and alias.
     *
     * @param actions true to get actions only, false to get programs only
     * @return list of programs
     */
    public List<ProgramRecord> getAllPrograms(boolean actions) {
        List<ProgramRecord> programs = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = getSelectProgramSql(true);
            stmt = con.prepareStatement(sql);
            stmt.setBoolean(1, actions);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                programs.add(createProgramFromResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all programs", e);
        } finally {
            close(rs, stmt);
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        return programs;
    }

    /**
     * Finds a program by its ID.
     *
     * @param id the program ID
     * @return the program, or null if not found
     */
    public ProgramRecord findProgram(long id) {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = getSelectProgramSql(false, false) + " WHERE id = ?";
            stmt = con.prepareStatement(sql);
            stmt.setLong(1, id);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                return createProgramFromResultSet(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding program", e);
        } finally {
            close(rs, stmt);
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        return null;
    }

    private String getSelectProgramSql(boolean withActionFilter) {
        return getSelectProgramSql(withActionFilter, true);
    }

    private String getSelectProgramSql(boolean withActionFilter, boolean withOrderBy) {
        String sql = "SELECT id, action, filename, alias, "
                + "parameters_before_filename, parameters_after_filename, "
                + "input_before_execute, input_before_execute_per_file, "
                + "single_file_processing, change_file, sequence_number, "
                + "use_pattern, pattern "
                + "FROM programs";

        if (withActionFilter) {
            sql += " WHERE action = ?";
        }

        if (withOrderBy) {
            sql += " ORDER BY sequence_number, alias";
        }

        return sql;
    }

    private ProgramRecord createProgramFromResultSet(ResultSet rs) throws SQLException {
        byte[] parametersBeforeFilename = rs.getBytes(5);
        byte[] parametersAfterFilename = rs.getBytes(6);
        byte[] pattern = rs.getBytes(13);

        return new ProgramRecord(
            rs.getLong(1),
            rs.getBoolean(2),
            rs.getString(3),
            rs.getString(4),
            (parametersBeforeFilename == null) ? null : new String(parametersBeforeFilename),
            (parametersAfterFilename == null) ? null : new String(parametersAfterFilename),
            rs.getBoolean(7),
            rs.getBoolean(8),
            rs.getBoolean(9),
            rs.getBoolean(10),
            rs.getInt(11),
            rs.getBoolean(12),
            (pattern == null) ? null : new String(pattern)
        );
    }

    /**
     * Checks if a program exists by alias and filename.
     *
     * @param program the program to check
     * @return true if exists
     * @throws NullPointerException if program is null
     */
    public boolean existsProgram(ProgramRecord program) {
        if (program == null) {
            throw new NullPointerException("program == null");
        }

        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = "SELECT COUNT(*) FROM programs WHERE alias = ? AND filename = ?";
            stmt = con.prepareStatement(sql);
            setString(program.alias(), stmt, 1);
            setString(program.filename(), stmt, 2);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking program existence", e);
        } finally {
            close(rs, stmt);
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        return false;
    }

    /**
     * Returns the count of programs or actions.
     *
     * @param actions true for action count, false for program count
     * @return the count
     */
    public int getProgramCount(boolean actions) {
        Connection con = null;
        try {
            con = getConnection();
            return getProgramCount(con, actions);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting program count", e);
            return 0;
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
    }

    int getProgramCount(Connection con, boolean actions) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT COUNT(*) FROM programs WHERE action = ?";
            stmt = con.prepareStatement(sql);
            stmt.setBoolean(1, actions);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting program count", e);
        } finally {
            close(rs, stmt);
        }
        return 0;
    }

    /**
     * Returns whether at least one program exists.
     *
     * @return true if at least one program exists
     */
    public boolean hasProgram() {
        return getProgramCount(false) > 0;
    }

    /**
     * Returns whether at least one action exists.
     *
     * @return true if at least one action exists
     */
    public boolean hasAction() {
        return getProgramCount(true) > 0;
    }

    /**
     * Returns the default image open program (sequence number 0, not an action).
     *
     * @return the default program, or null if none exists
     */
    public ProgramRecord getDefaultImageOpenProgram() {
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = "SELECT id, action, filename, alias, "
                    + "parameters_before_filename, parameters_after_filename, "
                    + "input_before_execute, input_before_execute_per_file, "
                    + "single_file_processing, change_file, sequence_number, "
                    + "use_pattern, pattern "
                    + "FROM programs WHERE action = FALSE AND sequence_number = 0";
            stmt = con.createStatement();
            LOGGER.log(Level.FINEST, sql);
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return createProgramFromResultSet(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting default image open program", e);
        } finally {
            close(rs, stmt);
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        return null;
    }

    /**
     * Sets or updates a default program for a file suffix.
     *
     * @param filenameSuffix the file suffix
     * @param idProgram the program ID
     * @return true if successful
     * @throws NullPointerException if filenameSuffix is null
     */
    public boolean setDefaultProgram(String filenameSuffix, long idProgram) {
        if (filenameSuffix == null) {
            throw new NullPointerException("filenameSuffix == null");
        }

        return existsDefaultProgram(filenameSuffix)
                ? updateDefaultProgram(filenameSuffix, idProgram) == 1
                : insertDefaultProgram(filenameSuffix, idProgram) == 1;
    }

    private int insertDefaultProgram(String filenameSuffix, long idProgram) {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            String sql = "INSERT INTO default_programs (id_program, filename_suffix) VALUES (?, ?)";
            stmt = con.prepareStatement(sql);
            stmt.setLong(1, idProgram);
            stmt.setString(2, filenameSuffix);
            LOGGER.log(Level.FINER, stmt.toString());
            int count = stmt.executeUpdate();
            con.commit();
            return count;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting default program", e);
            rollback(con);
            return 0;
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

    private int updateDefaultProgram(String filenameSuffix, long idProgram) {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            String sql = "UPDATE default_programs SET id_program = ? WHERE filename_suffix = ?";
            stmt = con.prepareStatement(sql);
            stmt.setLong(1, idProgram);
            stmt.setString(2, filenameSuffix);
            LOGGER.log(Level.FINER, stmt.toString());
            int count = stmt.executeUpdate();
            con.commit();
            return count;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating default program", e);
            rollback(con);
            return 0;
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
     * Finds the default program for a file suffix.
     *
     * @param filenameSuffix the file suffix
     * @return the program, or null if not found
     * @throws NullPointerException if filenameSuffix is null
     */
    public ProgramRecord findDefaultProgram(String filenameSuffix) {
        if (filenameSuffix == null) {
            throw new NullPointerException("filenameSuffix == null");
        }

        Connection con = null;
        try {
            con = getConnection();
            long id = findDefaultProgramId(con, filenameSuffix);
            if (id > 0) {
                return findProgram(id);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding default program", e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        return null;
    }

    private long findDefaultProgramId(Connection con, String filenameSuffix) throws SQLException {
        long id = Long.MIN_VALUE;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT id_program FROM default_programs WHERE filename_suffix = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, filenameSuffix);
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
     * Deletes a default program association.
     *
     * @param filenameSuffix the file suffix
     * @return true if deleted
     * @throws NullPointerException if filenameSuffix is null
     */
    public boolean deleteDefaultProgram(String filenameSuffix) {
        if (filenameSuffix == null) {
            throw new NullPointerException("filenameSuffix == null");
        }

        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);
            String sql = "DELETE FROM default_programs WHERE filename_suffix = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, filenameSuffix);
            LOGGER.log(Level.FINER, stmt.toString());
            int count = stmt.executeUpdate();
            con.commit();
            return count == 1;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting default program", e);
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
     * Checks if a default program exists for a suffix.
     *
     * @param filenameSuffix the file suffix
     * @return true if exists
     * @throws NullPointerException if filenameSuffix is null
     */
    public boolean existsDefaultProgram(String filenameSuffix) {
        if (filenameSuffix == null) {
            throw new NullPointerException("filenameSuffix == null");
        }

        long count = 0;
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = "SELECT COUNT(*) FROM default_programs WHERE filename_suffix = ?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, filenameSuffix);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking default program existence", e);
        } finally {
            close(rs, stmt);
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        return count > 0;
    }

    /**
     * Returns all default programs.
     *
     * @return list of default programs
     */
    public List<DefaultProgramRecord> findAllDefaultPrograms() {
        List<DefaultProgramRecord> defaults = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = "SELECT d.id_program, d.filename_suffix, p.alias "
                    + "FROM default_programs d INNER JOIN programs p "
                    + "ON d.id_program = p.id";
            stmt = con.prepareStatement(sql);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                defaults.add(new DefaultProgramRecord(
                    rs.getLong(1),
                    rs.getString(2),
                    rs.getString(3)
                ));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding all default programs", e);
        } finally {
            close(rs, stmt);
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        return defaults;
    }

    /**
     * Returns programs that should run after database insertion.
     * Ordered by action_order.
     *
     * @return list of programs
     */
    public List<ProgramRecord> getActionsAfterDbInsertion() {
        List<ProgramRecord> programs = new ArrayList<>();
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            String sql = "SELECT p.id, p.action, p.filename, p.alias, "
                    + "p.parameters_before_filename, p.parameters_after_filename, "
                    + "p.input_before_execute, p.input_before_execute_per_file, "
                    + "p.single_file_processing, p.change_file, p.sequence_number, "
                    + "p.use_pattern, p.pattern "
                    + "FROM programs p "
                    + "INNER JOIN actions_after_db_insertion a ON p.id = a.id_program "
                    + "ORDER BY a.action_order";
            stmt = con.prepareStatement(sql);
            LOGGER.log(Level.FINEST, stmt.toString());
            rs = stmt.executeQuery();
            while (rs.next()) {
                programs.add(createProgramFromResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting actions after DB insertion", e);
        } finally {
            close(rs, stmt);
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        return programs;
    }

    // Helper method to set BLOB fields
    private void setBlob(PreparedStatement stmt, int paramIndex, String value) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, java.sql.Types.BLOB);
        } else {
            stmt.setBytes(paramIndex, value.getBytes());
        }
    }
}
