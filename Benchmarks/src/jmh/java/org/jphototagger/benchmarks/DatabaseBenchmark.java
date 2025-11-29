package org.jphototagger.benchmarks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Baseline benchmarks for database operations.
 * Run before and after SQLite migration to compare performance.
 *
 * Results are saved to build/reports/jmh/results.json
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class DatabaseBenchmark {

    private Connection connection;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(
                "jdbc:hsqldb:mem:benchmark;shutdown=true", "sa", "");

        // Create schema
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE hierarchical_subjects (" +
                "id BIGINT NOT NULL PRIMARY KEY, " +
                "id_parent BIGINT, " +
                "subject VARCHAR(256) NOT NULL, " +
                "real BOOLEAN)");

            // Insert test data - 1000 keywords
            for (int i = 0; i < 1000; i++) {
                Long parentId = i > 0 ? (long) (i / 10) : null;
                String sql = String.format(
                    "INSERT INTO hierarchical_subjects VALUES (%d, %s, 'Keyword%d', TRUE)",
                    i, parentId == null ? "NULL" : parentId.toString(), i);
                stmt.execute(sql);
            }
        }
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        if (connection != null) {
            connection.createStatement().execute("SHUTDOWN");
            connection.close();
        }
    }

    @Benchmark
    public void selectAllKeywords(Blackhole bh) throws Exception {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, id_parent, subject, real FROM hierarchical_subjects")) {
            while (rs.next()) {
                bh.consume(rs.getLong(1));
                bh.consume(rs.getString(3));
            }
        }
    }

    @Benchmark
    public void selectRootKeywords(Blackhole bh) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, id_parent, subject, real FROM hierarchical_subjects WHERE id_parent IS NULL ORDER BY subject")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bh.consume(rs.getLong(1));
                    bh.consume(rs.getString(3));
                }
            }
        }
    }

    @Benchmark
    public void selectChildKeywords(Blackhole bh) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, id_parent, subject, real FROM hierarchical_subjects WHERE id_parent = ? ORDER BY subject")) {
            stmt.setLong(1, 50); // Arbitrary parent ID
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bh.consume(rs.getLong(1));
                    bh.consume(rs.getString(3));
                }
            }
        }
    }

    @Benchmark
    public void keywordExists(Blackhole bh) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM hierarchical_subjects WHERE subject = ?")) {
            stmt.setString(1, "Keyword500");
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                bh.consume(rs.getInt(1));
            }
        }
    }

    @Benchmark
    public void insertKeyword(Blackhole bh) throws Exception {
        long id = System.nanoTime();
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO hierarchical_subjects VALUES (?, NULL, ?, TRUE)")) {
            stmt.setLong(1, id);
            stmt.setString(2, "NewKeyword" + id);
            bh.consume(stmt.executeUpdate());
        }
        // Clean up
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM hierarchical_subjects WHERE id = ?")) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }
}
