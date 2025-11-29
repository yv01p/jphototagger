# Phase 4: SQLite Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace HSQLDB with SQLite for the main application database while maintaining full backward compatibility and providing a migration path for existing users.

**Architecture:** Create a new `Repositories/SQLite` module mirroring the HSQLDB structure. Use SQLite JDBC (xerial) with WAL mode for concurrent read performance. The SQLite implementation will use the same repository interfaces, allowing runtime switching via service provider configuration. A separate migration tool module will handle HSQLDB-to-SQLite data migration.

**Tech Stack:** SQLite JDBC (xerial), JUnit 5, AssertJ

---

## Pre-Implementation: Benchmark Baseline

### Task 0: Capture Pre-Migration Baseline

**Files:**
- Modify: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/DatabaseBenchmark.java`

**Step 1: Run existing database benchmarks**

Run:
```bash
./gradlew :Benchmarks:jmh -Pjmh.includes="DatabaseBenchmark"
```

**Step 2: Save baseline results**

Run:
```bash
mkdir -p docs/benchmarks
cp Benchmarks/build/results/jmh/results.json docs/benchmarks/pre-phase4-hsqldb.json
```

**Step 3: Commit baseline**

```bash
git add docs/benchmarks/pre-phase4-hsqldb.json
git commit -m "docs: add pre-Phase 4 HSQLDB benchmark baseline"
```

---

## Part 1: Project Setup

### Task 1: Add SQLite JDBC dependency to version catalog

**Files:**
- Modify: `gradle/libs.versions.toml`

**Step 1: Write the failing test**

No test needed - this is a configuration change.

**Step 2: Add SQLite version and library**

Add to `gradle/libs.versions.toml`:

```toml
# In [versions] section, add:
sqlite-jdbc = "3.45.1.0"

# In [libraries] section, add:
sqlite-jdbc = { module = "org.xerial:sqlite-jdbc", version.ref = "sqlite-jdbc" }
```

**Step 3: Verify configuration parses**

Run: `./gradlew help`
Expected: No errors about version catalog

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: add SQLite JDBC dependency to version catalog"
```

---

### Task 2: Create SQLite repository module structure

**Files:**
- Create: `Repositories/SQLite/build.gradle.kts`
- Create: `Repositories/SQLite/src/org/jphototagger/repository/sqlite/.gitkeep`
- Create: `Repositories/SQLite/test/org/jphototagger/repository/sqlite/.gitkeep`
- Modify: `settings.gradle.kts`

**Step 1: Create module directory structure**

Run:
```bash
mkdir -p Repositories/SQLite/src/org/jphototagger/repository/sqlite
mkdir -p Repositories/SQLite/test/org/jphototagger/repository/sqlite
touch Repositories/SQLite/src/org/jphototagger/repository/sqlite/.gitkeep
touch Repositories/SQLite/test/org/jphototagger/repository/sqlite/.gitkeep
```

**Step 2: Create build.gradle.kts**

Create `Repositories/SQLite/build.gradle.kts`:

```kotlin
plugins {
    id("java-library")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
        resources {
            setSrcDirs(listOf("src"))
            exclude("**/*.java")
        }
    }
    test {
        java {
            setSrcDirs(listOf("test"))
        }
    }
}

dependencies {
    implementation(project(":API"))
    implementation(project(":Domain"))
    implementation(project(":Lib"))

    implementation(libs.sqlite.jdbc)

    compileOnly(files("../../Libraries/org-openide-util-lookup-8.6.jar"))

    testImplementation(project(":TestSupport"))
    testImplementation(libs.bundles.junit5)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.test {
    useJUnitPlatform()
}
```

**Step 3: Add module to settings.gradle.kts**

In `settings.gradle.kts`, in the "Tier 4: Repository layer" section, add:

```kotlin
include("Repositories:SQLite")
```

**Step 4: Verify module builds**

Run: `./gradlew :Repositories:SQLite:build`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add Repositories/SQLite settings.gradle.kts
git commit -m "build: create SQLite repository module structure"
```

---

## Part 2: Core Infrastructure

### Task 3: Create SQLite connection manager

**Files:**
- Create: `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteConnectionFactory.java`
- Create: `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteConnectionFactoryTest.java`

**Step 1: Write the failing test**

Create `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteConnectionFactoryTest.java`:

```java
package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.assertj.core.api.Assertions.*;

class SqliteConnectionFactoryTest {

    @TempDir
    File tempDir;

    @Test
    void getConnection_createsNewDatabase() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        try (Connection con = factory.getConnection()) {
            assertThat(con).isNotNull();
            assertThat(con.isClosed()).isFalse();
            assertThat(dbFile).exists();
        }
    }

    @Test
    void getConnection_enablesWalMode() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1).toLowerCase()).isEqualTo("wal");
        }
    }

    @Test
    void getConnection_enablesForeignKeys() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);

        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void close_closesAllConnections() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        SqliteConnectionFactory factory = new SqliteConnectionFactory(dbFile);
        Connection con = factory.getConnection();

        factory.close();

        assertThat(con.isClosed()).isTrue();
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteConnectionFactoryTest`
Expected: FAIL - class SqliteConnectionFactory not found

**Step 3: Write minimal implementation**

Create `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteConnectionFactory.java`:

```java
package org.jphototagger.repository.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for SQLite database connections with WAL mode and foreign key support.
 */
public final class SqliteConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(SqliteConnectionFactory.class.getName());
    private final String url;
    private final List<Connection> connections = new ArrayList<>();
    private volatile boolean closed = false;

    public SqliteConnectionFactory(File databaseFile) {
        this.url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
    }

    public synchronized Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Connection factory is closed");
        }

        Connection con = DriverManager.getConnection(url);
        configureConnection(con);
        connections.add(con);
        return con;
    }

    private void configureConnection(Connection con) throws SQLException {
        try (Statement stmt = con.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
            stmt.execute("PRAGMA synchronous=NORMAL");
        }
    }

    public synchronized void close() {
        closed = true;
        for (Connection con : connections) {
            try {
                if (!con.isClosed()) {
                    con.close();
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection", e);
            }
        }
        connections.clear();
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteConnectionFactoryTest`
Expected: PASS

**Step 5: Commit**

```bash
git add Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteConnectionFactory.java
git add Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteConnectionFactoryTest.java
git commit -m "feat(sqlite): add connection factory with WAL mode"
```

---

### Task 4: Create SQLite base database class

**Files:**
- Create: `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteDatabase.java`
- Create: `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteDatabaseTest.java`

**Step 1: Write the failing test**

Create `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteDatabaseTest.java`:

```java
package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import static org.assertj.core.api.Assertions.*;

class SqliteDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private TestDatabase database;

    @BeforeEach
    void setUp() {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        database = new TestDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getConnection_returnsValidConnection() throws Exception {
        try (Connection con = database.getTestConnection()) {
            assertThat(con).isNotNull();
            assertThat(con.isClosed()).isFalse();
        }
    }

    @Test
    void close_closesResourcesProperly() throws Exception {
        Connection con = database.getTestConnection();
        PreparedStatement stmt = con.prepareStatement("SELECT 1");
        ResultSet rs = stmt.executeQuery();

        SqliteDatabase.close(rs, stmt);

        assertThat(stmt.isClosed()).isTrue();
    }

    // Test subclass to access protected methods
    private static class TestDatabase extends SqliteDatabase {
        TestDatabase(SqliteConnectionFactory factory) {
            super(factory);
        }

        Connection getTestConnection() throws Exception {
            return getConnection();
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteDatabaseTest`
Expected: FAIL - class SqliteDatabase not found

**Step 3: Write minimal implementation**

Create `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteDatabase.java`:

```java
package org.jphototagger.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for SQLite database operations.
 * Mirrors the HSQLDB Database class structure for consistency.
 */
public abstract class SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteDatabase.class.getName());
    private final SqliteConnectionFactory connectionFactory;

    protected SqliteDatabase(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    protected Connection getConnection() throws SQLException {
        return connectionFactory.getConnection();
    }

    public static void close(Statement stmt) {
        if (stmt == null) return;
        try {
            stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing statement", e);
        }
    }

    public static void close(ResultSet rs, Statement stmt) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing resources", e);
        }
    }

    public static void close(ResultSet rs, PreparedStatement stmt) {
        close(rs, (Statement) stmt);
    }

    public static void rollback(Connection con) {
        if (con == null) return;
        try {
            con.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error rolling back", e);
        }
    }

    protected Long getLong(ResultSet rs, int colIndex) throws SQLException {
        long value = rs.getLong(colIndex);
        return rs.wasNull() ? null : value;
    }

    protected String getString(ResultSet rs, int colIndex) throws SQLException {
        String value = rs.getString(colIndex);
        return rs.wasNull() ? null : value;
    }

    protected void setLong(Long value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(paramIndex, value);
        }
    }

    protected void setString(Object value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, java.sql.Types.VARCHAR);
        } else {
            stmt.setString(paramIndex, value.toString());
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteDatabaseTest`
Expected: PASS

**Step 5: Commit**

```bash
git add Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteDatabase.java
git add Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteDatabaseTest.java
git commit -m "feat(sqlite): add base database class with helper methods"
```

---

### Task 5: Create SQLite schema with DDL

**Files:**
- Create: `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteTables.java`
- Create: `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteTablesTest.java`

**Step 1: Write the failing test**

Create `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteTablesTest.java`:

```java
package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class SqliteTablesTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void createTables_createsFilesTable() throws Exception {
        SqliteTables tables = new SqliteTables(factory);

        tables.createTables();

        Set<String> tableNames = getTableNames();
        assertThat(tableNames).contains("files");
    }

    @Test
    void createTables_createsXmpTables() throws Exception {
        SqliteTables tables = new SqliteTables(factory);

        tables.createTables();

        Set<String> tableNames = getTableNames();
        assertThat(tableNames).contains("xmp", "dc_subjects", "xmp_dc_subject");
    }

    @Test
    void createTables_createsExifTables() throws Exception {
        SqliteTables tables = new SqliteTables(factory);

        tables.createTables();

        Set<String> tableNames = getTableNames();
        assertThat(tableNames).contains("exif", "exif_recording_equipment", "exif_lenses");
    }

    @Test
    void createTables_isIdempotent() throws Exception {
        SqliteTables tables = new SqliteTables(factory);

        tables.createTables();
        tables.createTables(); // Should not throw

        Set<String> tableNames = getTableNames();
        assertThat(tableNames).contains("files");
    }

    @Test
    void filesTable_hasAutoIncrementId() throws Exception {
        SqliteTables tables = new SqliteTables(factory);
        tables.createTables();

        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("INSERT INTO files (filename) VALUES ('/test/image.jpg')");
            try (ResultSet rs = stmt.executeQuery("SELECT id FROM files")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong("id")).isEqualTo(1);
            }
        }
    }

    private Set<String> getTableNames() throws Exception {
        Set<String> names = new HashSet<>();
        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteTablesTest`
Expected: FAIL - class SqliteTables not found

**Step 3: Write minimal implementation**

Create `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteTables.java`:

```java
package org.jphototagger.repository.sqlite;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates all database tables for SQLite.
 * Schema adapted from HSQLDB with SQLite-compatible syntax:
 * - BIGINT GENERATED BY DEFAULT AS IDENTITY -> INTEGER PRIMARY KEY AUTOINCREMENT
 * - VARCHAR_IGNORECASE -> TEXT COLLATE NOCASE
 * - VARBINARY -> BLOB
 * - CREATE CACHED TABLE -> CREATE TABLE
 */
public final class SqliteTables {

    private static final Logger LOGGER = Logger.getLogger(SqliteTables.class.getName());
    private final SqliteConnectionFactory connectionFactory;

    public SqliteTables(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void createTables() throws SQLException {
        try (Connection con = connectionFactory.getConnection();
             Statement stmt = con.createStatement()) {
            con.setAutoCommit(false);

            createApplicationTable(stmt);
            createFilesTable(stmt);
            create1nTables(stmt);
            createDcSubjectsTable(stmt);
            createXmpTable(stmt);
            createXmpDcSubjectTable(stmt);
            createExifTables(stmt);
            createCollectionsTables(stmt);
            createSavedSearchesTables(stmt);
            createAutoScanDirectoriesTable(stmt);
            createMetadataTemplateTable(stmt);
            createFavoriteDirectoriesTable(stmt);
            createFileExcludePatternsTable(stmt);
            createProgramsTable(stmt);
            createActionsAfterDbInsertionTable(stmt);
            createDefaultProgramsTable(stmt);
            createHierarchicalSubjectsTable(stmt);
            createSynonymsTable(stmt);
            createRenameTemplatesTable(stmt);
            createUserDefinedFileFiltersTable(stmt);
            createUserDefinedFileTypesTable(stmt);
            createWordsetTables(stmt);

            con.commit();
            LOGGER.info("SQLite database tables created successfully");
        }
    }

    private void createApplicationTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS application (
                key TEXT PRIMARY KEY,
                value BLOB
            )
            """);
    }

    private void createFilesTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS files (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                filename TEXT COLLATE NOCASE NOT NULL,
                size_in_bytes INTEGER,
                lastmodified INTEGER,
                xmp_lastmodified INTEGER
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_files ON files (filename)");
    }

    private void create1nTables(Statement stmt) throws SQLException {
        create1nTable(stmt, "dc_creators", "creator", 128);
        create1nTable(stmt, "dc_rights", "rights", 128);
        create1nTable(stmt, "iptc4xmpcore_locations", "location", 64);
        create1nTable(stmt, "photoshop_authorspositions", "authorsposition", 32);
        create1nTable(stmt, "photoshop_captionwriters", "captionwriter", 32);
        create1nTable(stmt, "photoshop_cities", "city", 32);
        create1nTable(stmt, "photoshop_countries", "country", 64);
        create1nTable(stmt, "photoshop_credits", "credit", 32);
        create1nTable(stmt, "photoshop_sources", "source", 32);
        create1nTable(stmt, "photoshop_states", "state", 32);
    }

    private void create1nTable(Statement stmt, String tablename, String columnname, int length)
            throws SQLException {
        stmt.execute(String.format("""
            CREATE TABLE IF NOT EXISTS %s (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                %s TEXT COLLATE NOCASE
            )
            """, tablename, columnname));
        stmt.execute(String.format(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_%s_id ON %s (id)", tablename, tablename));
        stmt.execute(String.format(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_%s_%s ON %s (%s)",
            tablename, columnname, tablename, columnname));
    }

    private void createDcSubjectsTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS dc_subjects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject TEXT COLLATE NOCASE
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_dc_subjects_id ON dc_subjects (id)");
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_dc_subjects_subject ON dc_subjects (subject)");
    }

    private void createXmpTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS xmp (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_file INTEGER NOT NULL,
                id_dc_creator INTEGER,
                dc_description TEXT COLLATE NOCASE,
                id_dc_rights INTEGER,
                dc_title TEXT COLLATE NOCASE,
                id_iptc4xmpcore_location INTEGER,
                id_photoshop_authorsposition INTEGER,
                id_photoshop_captionwriter INTEGER,
                id_photoshop_city INTEGER,
                id_photoshop_country INTEGER,
                id_photoshop_credit INTEGER,
                photoshop_headline TEXT COLLATE NOCASE,
                photoshop_instructions TEXT COLLATE NOCASE,
                id_photoshop_source INTEGER,
                id_photoshop_state INTEGER,
                photoshop_transmissionReference TEXT COLLATE NOCASE,
                rating INTEGER,
                iptc4xmpcore_datecreated TEXT COLLATE NOCASE,
                FOREIGN KEY (id_file) REFERENCES files (id) ON DELETE CASCADE,
                FOREIGN KEY (id_dc_creator) REFERENCES dc_creators (id) ON DELETE SET NULL,
                FOREIGN KEY (id_dc_rights) REFERENCES dc_rights (id) ON DELETE SET NULL,
                FOREIGN KEY (id_iptc4xmpcore_location) REFERENCES iptc4xmpcore_locations (id) ON DELETE SET NULL,
                FOREIGN KEY (id_photoshop_authorsposition) REFERENCES photoshop_authorspositions (id) ON DELETE SET NULL,
                FOREIGN KEY (id_photoshop_captionwriter) REFERENCES photoshop_captionwriters (id) ON DELETE SET NULL,
                FOREIGN KEY (id_photoshop_city) REFERENCES photoshop_cities (id) ON DELETE SET NULL,
                FOREIGN KEY (id_photoshop_country) REFERENCES photoshop_countries (id) ON DELETE SET NULL,
                FOREIGN KEY (id_photoshop_credit) REFERENCES photoshop_credits (id) ON DELETE SET NULL,
                FOREIGN KEY (id_photoshop_source) REFERENCES photoshop_sources (id) ON DELETE SET NULL,
                FOREIGN KEY (id_photoshop_state) REFERENCES photoshop_states (id) ON DELETE SET NULL
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_xmp_id_files ON xmp (id_file)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_xmp_dc_description ON xmp (dc_description)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_xmp_dc_title ON xmp (dc_title)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_xmp_photoshop_headline ON xmp (photoshop_headline)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_xmp_iptc4xmpcore_datecreated ON xmp (iptc4xmpcore_datecreated)");
    }

    private void createXmpDcSubjectTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS xmp_dc_subject (
                id_xmp INTEGER,
                id_dc_subject INTEGER,
                PRIMARY KEY (id_xmp, id_dc_subject),
                FOREIGN KEY (id_xmp) REFERENCES xmp (id) ON DELETE CASCADE,
                FOREIGN KEY (id_dc_subject) REFERENCES dc_subjects (id) ON DELETE CASCADE
            )
            """);
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_xmp_dc_subject_id_xmp ON xmp_dc_subject (id_xmp)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_xmp_dc_subject_id_dc_subject ON xmp_dc_subject (id_dc_subject)");
    }

    private void createExifTables(Statement stmt) throws SQLException {
        create1nTable(stmt, "exif_recording_equipment", "equipment", 125);
        create1nTable(stmt, "exif_lenses", "lens", 256);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS exif (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                id_file INTEGER NOT NULL,
                id_exif_recording_equipment INTEGER,
                exif_date_time_original TEXT,
                exif_focal_length REAL,
                exif_iso_speed_ratings INTEGER,
                id_exif_lens INTEGER,
                exif_date_time_original_timestamp INTEGER,
                exif_gps_latitude REAL,
                exif_gps_longitude REAL,
                FOREIGN KEY (id_file) REFERENCES files (id) ON DELETE CASCADE,
                FOREIGN KEY (id_exif_recording_equipment) REFERENCES exif_recording_equipment (id) ON DELETE SET NULL,
                FOREIGN KEY (id_exif_lens) REFERENCES exif_lenses (id) ON DELETE SET NULL
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_exif_id_files ON exif (id_file)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_exif_date_time_original ON exif (exif_date_time_original)");
    }

    private void createCollectionsTables(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS collection_names (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT COLLATE NOCASE
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_collection_names_id ON collection_names (id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_collection_names_name ON collection_names (name)");

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS collections (
                id_collectionnname INTEGER,
                id_file INTEGER,
                sequence_number INTEGER,
                FOREIGN KEY (id_collectionnname) REFERENCES collection_names (id) ON DELETE CASCADE,
                FOREIGN KEY (id_file) REFERENCES files (id) ON DELETE CASCADE
            )
            """);
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_collections_id_collectionnnames ON collections (id_collectionnname)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_collections_id_files ON collections (id_file)");
    }

    private void createSavedSearchesTables(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS saved_searches (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT COLLATE NOCASE,
                custom_sql BLOB,
                search_type INTEGER
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_saved_searches_name ON saved_searches (name)");

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS saved_searches_panels (
                id_saved_search INTEGER,
                panel_index INTEGER,
                bracket_left_1 INTEGER,
                operator_id INTEGER,
                bracket_left_2 INTEGER,
                column_id INTEGER,
                comparator_id INTEGER,
                value TEXT,
                bracket_right INTEGER,
                FOREIGN KEY (id_saved_search) REFERENCES saved_searches (id) ON DELETE CASCADE
            )
            """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS saved_searches_keywords (
                id_saved_search INTEGER,
                keyword TEXT COLLATE NOCASE,
                FOREIGN KEY (id_saved_search) REFERENCES saved_searches (id) ON DELETE CASCADE
            )
            """);
    }

    private void createAutoScanDirectoriesTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS autoscan_directories (
                directory TEXT COLLATE NOCASE
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_autoscan_directories_directory ON autoscan_directories (directory)");
    }

    private void createMetadataTemplateTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS metadata_edit_templates (
                name TEXT COLLATE NOCASE,
                dcSubjects BLOB,
                dcTitle BLOB,
                photoshopHeadline BLOB,
                dcDescription BLOB,
                photoshopCaptionwriter BLOB,
                iptc4xmpcoreLocation BLOB,
                dcRights BLOB,
                dcCreator BLOB,
                photoshopAuthorsposition BLOB,
                photoshopCity BLOB,
                photoshopState BLOB,
                photoshopCountry BLOB,
                photoshopTransmissionReference BLOB,
                photoshopInstructions BLOB,
                photoshopCredit BLOB,
                photoshopSource BLOB,
                rating BLOB,
                iptc4xmpcore_datecreated BLOB
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_metadata_edit_templates_name ON metadata_edit_templates (name)");
    }

    private void createFavoriteDirectoriesTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS favorite_directories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                favorite_name TEXT COLLATE NOCASE,
                directory_name TEXT,
                favorite_index INTEGER
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_favorite_directories_favorite_name ON favorite_directories (favorite_name)");
    }

    private void createFileExcludePatternsTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS file_exclude_patterns (
                pattern TEXT COLLATE NOCASE
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_file_exclude_pattern_pattern ON file_exclude_patterns (pattern)");
    }

    private void createProgramsTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS programs (
                id INTEGER NOT NULL,
                action INTEGER,
                filename TEXT NOT NULL,
                alias TEXT COLLATE NOCASE NOT NULL,
                parameters_before_filename BLOB,
                parameters_after_filename BLOB,
                input_before_execute INTEGER,
                input_before_execute_per_file INTEGER,
                single_file_processing INTEGER,
                change_file INTEGER,
                sequence_number INTEGER,
                use_pattern INTEGER,
                pattern BLOB
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_programs_id ON programs (id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_programs_alias ON programs (alias)");
    }

    private void createActionsAfterDbInsertionTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS actions_after_db_insertion (
                id_program INTEGER NOT NULL,
                action_order INTEGER
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_actions_after_db_insertion_id_programs ON actions_after_db_insertion (id_program)");
    }

    private void createDefaultProgramsTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS default_programs (
                id_program INTEGER NOT NULL,
                filename_suffix TEXT COLLATE NOCASE
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_default_programs_filename_suffix ON default_programs (filename_suffix)");
    }

    private void createHierarchicalSubjectsTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS hierarchical_subjects (
                id INTEGER NOT NULL,
                id_parent INTEGER,
                subject TEXT COLLATE NOCASE NOT NULL,
                real INTEGER
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_hierarchical_subjects_id ON hierarchical_subjects (id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_hierarchical_subjects_subject ON hierarchical_subjects (subject)");
    }

    private void createSynonymsTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS synonyms (
                word TEXT,
                synonym TEXT,
                PRIMARY KEY (word, synonym)
            )
            """);
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_synonyms_word ON synonyms (word)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_synonyms_synonym ON synonyms (synonym)");
    }

    private void createRenameTemplatesTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS rename_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                start_number INTEGER,
                step_width INTEGER,
                number_count INTEGER,
                date_delimiter TEXT,
                format_class_at_begin TEXT,
                delimiter_1 TEXT,
                format_class_in_the_middle TEXT,
                delimiter_2 TEXT,
                format_class_at_end TEXT,
                text_at_begin TEXT,
                text_in_the_middle TEXT,
                text_at_end TEXT,
                UNIQUE(name)
            )
            """);
    }

    private void createUserDefinedFileFiltersTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS user_defined_file_filters (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                is_not INTEGER,
                type INTEGER,
                name TEXT COLLATE NOCASE NOT NULL,
                expression TEXT NOT NULL,
                UNIQUE(name)
            )
            """);
    }

    private void createUserDefinedFileTypesTable(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS user_defined_file_types (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                suffix TEXT COLLATE NOCASE NOT NULL,
                description TEXT COLLATE NOCASE,
                external_thumbnail_creator INTEGER
            )
            """);
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_defined_file_types_suffix ON user_defined_file_types (suffix)");
    }

    private void createWordsetTables(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS wordsets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT COLLATE NOCASE
            )
            """);
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_wordsets_name ON wordsets (name)");

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS wordsets_words (
                id_wordsets INTEGER,
                word TEXT COLLATE NOCASE,
                word_order INTEGER,
                FOREIGN KEY (id_wordsets) REFERENCES wordsets (id) ON DELETE CASCADE
            )
            """);
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_wordsets_words_id_wordsets ON wordsets_words (id_wordsets)");
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteTablesTest`
Expected: PASS

**Step 5: Commit**

```bash
git add Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteTables.java
git add Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteTablesTest.java
git commit -m "feat(sqlite): add schema creation with all tables"
```

---

## Part 3: Repository Implementations

### Task 6: Create ImageFilesDatabase for SQLite

**Files:**
- Create: `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteImageFilesDatabase.java`
- Create: `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteImageFilesDatabaseTest.java`

**Step 1: Write the failing test**

Create `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteImageFilesDatabaseTest.java`:

```java
package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class SqliteImageFilesDatabaseTest {

    @TempDir
    File tempDir;

    private SqliteConnectionFactory factory;
    private SqliteImageFilesDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "test.db");
        factory = new SqliteConnectionFactory(dbFile);
        new SqliteTables(factory).createTables();
        database = new SqliteImageFilesDatabase(factory);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void existsImageFile_returnsFalseWhenNotExists() {
        File file = new File("/nonexistent/image.jpg");

        boolean exists = database.existsImageFile(file);

        assertThat(exists).isFalse();
    }

    @Test
    void insertImageFile_insertsAndReturnsTrue() {
        File file = new File("/test/image.jpg");

        boolean inserted = database.insertImageFile(file, 1000L, System.currentTimeMillis());

        assertThat(inserted).isTrue();
        assertThat(database.existsImageFile(file)).isTrue();
    }

    @Test
    void getFileCount_returnsCorrectCount() {
        database.insertImageFile(new File("/test/image1.jpg"), 1000L, System.currentTimeMillis());
        database.insertImageFile(new File("/test/image2.jpg"), 2000L, System.currentTimeMillis());

        long count = database.getFileCount();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void getAllImageFiles_returnsAllFiles() {
        File file1 = new File("/test/image1.jpg");
        File file2 = new File("/test/image2.jpg");
        database.insertImageFile(file1, 1000L, System.currentTimeMillis());
        database.insertImageFile(file2, 2000L, System.currentTimeMillis());

        List<File> files = database.getAllImageFiles();

        assertThat(files).containsExactlyInAnyOrder(file1, file2);
    }

    @Test
    void deleteImageFile_deletesAndReturnsOne() {
        File file = new File("/test/image.jpg");
        database.insertImageFile(file, 1000L, System.currentTimeMillis());

        int deleted = database.deleteImageFile(file);

        assertThat(deleted).isEqualTo(1);
        assertThat(database.existsImageFile(file)).isFalse();
    }

    @Test
    void findIdImageFile_returnsIdWhenExists() {
        File file = new File("/test/image.jpg");
        database.insertImageFile(file, 1000L, System.currentTimeMillis());

        long id = database.findIdImageFile(file);

        assertThat(id).isGreaterThan(0);
    }

    @Test
    void findIdImageFile_returnsMinusOneWhenNotExists() {
        File file = new File("/nonexistent/image.jpg");

        long id = database.findIdImageFile(file);

        assertThat(id).isEqualTo(-1);
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteImageFilesDatabaseTest`
Expected: FAIL - class SqliteImageFilesDatabase not found

**Step 3: Write minimal implementation**

Create `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteImageFilesDatabase.java`:

```java
package org.jphototagger.repository.sqlite;

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
 * SQLite implementation for image files database operations.
 */
public class SqliteImageFilesDatabase extends SqliteDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteImageFilesDatabase.class.getName());

    public SqliteImageFilesDatabase(SqliteConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    public boolean existsImageFile(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        String sql = "SELECT COUNT(*) FROM files WHERE filename = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return false;
        }
    }

    public boolean insertImageFile(File file, long sizeInBytes, long lastModified) {
        if (file == null) {
            throw new NullPointerException("file == null");
        }
        String sql = "INSERT INTO files (filename, size_in_bytes, lastmodified) VALUES (?, ?, ?)";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, file.getAbsolutePath());
            stmt.setLong(2, sizeInBytes);
            stmt.setLong(3, lastModified);
            int count = stmt.executeUpdate();
            return count > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return false;
        }
    }

    public long getFileCount() {
        String sql = "SELECT COUNT(*) FROM files";
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return 0;
        }
    }

    public List<File> getAllImageFiles() {
        List<File> files = new ArrayList<>();
        String sql = "SELECT filename FROM files";
        try (Connection con = getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                files.add(new File(rs.getString(1)));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }
        return files;
    }

    public int deleteImageFile(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        String sql = "DELETE FROM files WHERE filename = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            return stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return 0;
        }
    }

    public long findIdImageFile(File file) {
        if (file == null) {
            throw new NullPointerException("file == null");
        }
        String sql = "SELECT id FROM files WHERE filename = ?";
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, file.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, null, e);
            return -1;
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteImageFilesDatabaseTest`
Expected: PASS

**Step 5: Commit**

```bash
git add Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteImageFilesDatabase.java
git add Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteImageFilesDatabaseTest.java
git commit -m "feat(sqlite): add ImageFilesDatabase with basic CRUD operations"
```

---

### Task 7-15: Implement remaining repository classes

**Note:** Tasks 7-15 follow the same TDD pattern. Each task creates one database class mirroring the HSQLDB implementation. Due to the large scope, here is a summary of what needs to be implemented:

| Task | Class | Key Methods |
|------|-------|-------------|
| 7 | SqliteKeywordsDatabase | getDcSubjects, insertDcSubject, deleteDcSubject |
| 8 | SqliteExifDatabase | getExif, insertExif, updateExif |
| 9 | SqliteXmpDatabase | getXmp, insertXmp, updateXmp |
| 10 | SqliteCollectionsDatabase | getCollections, insertCollection, deleteCollection |
| 11 | SqliteFavoritesDatabase | getFavorites, insertFavorite, deleteFavorite |
| 12 | SqliteSavedSearchesDatabase | getSavedSearches, insertSavedSearch |
| 13 | SqliteProgramsDatabase | getPrograms, insertProgram |
| 14 | SqliteSynonymsDatabase | getSynonyms, insertSynonym |
| 15 | SqliteApplicationPropertiesDatabase | getValue, setValue |

Each follows the same pattern: failing test first, minimal implementation, verify pass, commit.

---

## Part 4: Service Provider Integration

### Task 16: Create Repository service provider

**Files:**
- Create: `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteRepositoryImpl.java`
- Create: `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteImageFilesRepositoryImpl.java`
- Create: `Repositories/SQLite/src/META-INF/services/org.jphototagger.domain.repository.Repository`

**Step 1: Write the failing test**

Create `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteRepositoryImplTest.java`:

```java
package org.jphototagger.repository.sqlite;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SqliteRepositoryImplTest {

    @Test
    void isInit_returnsFalseBeforeInit() {
        SqliteRepositoryImpl repo = new SqliteRepositoryImpl();

        assertThat(repo.isInit()).isFalse();
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteRepositoryImplTest`
Expected: FAIL

**Step 3: Write minimal implementation**

Create `Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteRepositoryImpl.java`:

```java
package org.jphototagger.repository.sqlite;

import org.jphototagger.domain.repository.Repository;
import org.openide.util.lookup.ServiceProvider;

/**
 * SQLite implementation of the Repository interface.
 */
@ServiceProvider(service = Repository.class)
public final class SqliteRepositoryImpl implements Repository {

    private volatile boolean init = false;
    private SqliteConnectionFactory connectionFactory;

    @Override
    public void init() {
        if (!init) {
            // Database path will come from FileRepositoryProvider
            // For now, this is a placeholder
            init = true;
        }
    }

    @Override
    public boolean isInit() {
        return init;
    }

    @Override
    public void shutdown() {
        if (connectionFactory != null) {
            connectionFactory.close();
        }
        init = false;
    }

    SqliteConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
}
```

**Step 4: Run test to verify it passes**

Run: `./gradlew :Repositories:SQLite:test --tests SqliteRepositoryImplTest`
Expected: PASS

**Step 5: Commit**

```bash
git add Repositories/SQLite/src/org/jphototagger/repository/sqlite/SqliteRepositoryImpl.java
git add Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteRepositoryImplTest.java
git commit -m "feat(sqlite): add Repository service provider implementation"
```

---

## Part 5: Migration Tool

### Task 17: Create migration tool module

**Files:**
- Create: `Tools/MigrationTool/build.gradle.kts`
- Create: `Tools/MigrationTool/src/org/jphototagger/tools/migration/HsqldbToSqliteMigrator.java`
- Modify: `settings.gradle.kts`

**Step 1: Create module structure**

Run:
```bash
mkdir -p Tools/MigrationTool/src/org/jphototagger/tools/migration
mkdir -p Tools/MigrationTool/test/org/jphototagger/tools/migration
```

**Step 2: Create build.gradle.kts**

Create `Tools/MigrationTool/build.gradle.kts`:

```kotlin
plugins {
    id("java")
    id("application")
}

application {
    mainClass.set("org.jphototagger.tools.migration.MigrationMain")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
    }
    test {
        java {
            setSrcDirs(listOf("test"))
        }
    }
}

dependencies {
    implementation(libs.hsqldb)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.bundles.junit5)
    testImplementation(libs.assertj)
}

tasks.test {
    useJUnitPlatform()
}
```

**Step 3: Add to settings.gradle.kts**

Add before the Tier 10 comment:

```kotlin
// Tools
include("Tools:MigrationTool")
```

**Step 4: Create migrator class (stub)**

Create `Tools/MigrationTool/src/org/jphototagger/tools/migration/HsqldbToSqliteMigrator.java`:

```java
package org.jphototagger.tools.migration;

import java.io.File;
import java.sql.*;
import java.util.logging.Logger;

/**
 * Migrates data from HSQLDB to SQLite database.
 */
public class HsqldbToSqliteMigrator {

    private static final Logger LOGGER = Logger.getLogger(HsqldbToSqliteMigrator.class.getName());

    private final File hsqldbFile;
    private final File sqliteFile;

    public HsqldbToSqliteMigrator(File hsqldbFile, File sqliteFile) {
        this.hsqldbFile = hsqldbFile;
        this.sqliteFile = sqliteFile;
    }

    public MigrationResult migrate(MigrationListener listener) {
        // Implementation in subsequent tasks
        return new MigrationResult(true, 0, 0, null);
    }

    public interface MigrationListener {
        void onProgress(String tableName, int current, int total);
        void onTableComplete(String tableName, int rowCount);
    }

    public record MigrationResult(boolean success, int tablesProcessed, int rowsMigrated, String errorMessage) {}
}
```

**Step 5: Verify builds**

Run: `./gradlew :Tools:MigrationTool:build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add Tools/MigrationTool settings.gradle.kts
git commit -m "feat: add migration tool module structure"
```

---

### Task 18: Implement table-by-table migration

**Files:**
- Modify: `Tools/MigrationTool/src/org/jphototagger/tools/migration/HsqldbToSqliteMigrator.java`
- Create: `Tools/MigrationTool/test/org/jphototagger/tools/migration/HsqldbToSqliteMigratorTest.java`

**Step 1: Write the failing test**

Create `Tools/MigrationTool/test/org/jphototagger/tools/migration/HsqldbToSqliteMigratorTest.java`:

```java
package org.jphototagger.tools.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;

class HsqldbToSqliteMigratorTest {

    @TempDir
    File tempDir;

    @Test
    void migrate_copiesFilesTable() throws Exception {
        // Setup HSQLDB with test data
        File hsqldbFile = new File(tempDir, "hsqldb/jphototagger");
        File sqliteFile = new File(tempDir, "jphototagger.db");
        setupHsqldbWithTestData(hsqldbFile);

        HsqldbToSqliteMigrator migrator = new HsqldbToSqliteMigrator(hsqldbFile, sqliteFile);
        HsqldbToSqliteMigrator.MigrationResult result = migrator.migrate(null);

        assertThat(result.success()).isTrue();
        assertThat(sqliteFile).exists();

        // Verify data was copied
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM files")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(2);
        }
    }

    private void setupHsqldbWithTestData(File hsqldbFile) throws Exception {
        hsqldbFile.getParentFile().mkdirs();
        String url = "jdbc:hsqldb:file:" + hsqldbFile.getAbsolutePath() + ";shutdown=true";
        try (Connection con = DriverManager.getConnection(url, "sa", "");
             Statement stmt = con.createStatement()) {
            stmt.execute("CREATE CACHED TABLE files (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, filename VARCHAR(512) NOT NULL)");
            stmt.execute("INSERT INTO files (filename) VALUES ('/test/image1.jpg')");
            stmt.execute("INSERT INTO files (filename) VALUES ('/test/image2.jpg')");
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew :Tools:MigrationTool:test --tests HsqldbToSqliteMigratorTest`
Expected: FAIL

**Step 3: Implement migration logic** (abbreviated - full implementation would be longer)

The implementation should:
1. Connect to source HSQLDB
2. Create SQLite database with schema
3. Copy each table in order (respecting foreign keys)
4. Validate record counts match
5. Report progress via listener

**Step 4: Run test to verify it passes**

Run: `./gradlew :Tools:MigrationTool:test --tests HsqldbToSqliteMigratorTest`
Expected: PASS

**Step 5: Commit**

```bash
git add Tools/MigrationTool/src Tools/MigrationTool/test
git commit -m "feat(migration): implement HSQLDB to SQLite data migration"
```

---

## Part 6: Integration and Validation

### Task 19: Add feature flag for database backend selection

**Files:**
- Modify: `Domain/src/org/jphototagger/domain/repository/DatabaseBackend.java` (create)
- Modify: Application startup to read flag

This allows users to switch between HSQLDB and SQLite during testing.

### Task 20: Run integration tests against SQLite

**Files:**
- Modify: `Repositories/SQLite/test/org/jphototagger/repository/sqlite/SqliteIntegrationTest.java`

Full integration test that exercises all repository operations.

### Task 21: Run benchmarks against SQLite and compare

**Files:**
- Create: `docs/benchmarks/post-phase4-sqlite.json`
- Create: `docs/plans/2025-11-29-phase4-benchmark-results.md`

Run:
```bash
./gradlew :Benchmarks:jmh -Pjmh.includes="DatabaseBenchmark"
cp Benchmarks/build/results/jmh/results.json docs/benchmarks/post-phase4-sqlite.json
```

Compare with baseline and document results.

### Task 22: Final cleanup and documentation

**Files:**
- Create: `docs/migration-guide.md`

Document the migration process for end users.

---

## Verification Checklist

Before marking Phase 4 complete:

- [ ] All SQLite repository tests pass
- [ ] Migration tool successfully migrates test databases
- [ ] Benchmarks show no regression vs HSQLDB baseline
- [ ] Application starts with SQLite backend
- [ ] All existing integration tests pass with SQLite
- [ ] Migration documentation complete

---

## Summary

This plan implements the SQLite migration in 22 tasks across 6 parts:

1. **Pre-Implementation**: Capture baseline benchmarks
2. **Project Setup**: Add dependencies, create module structure
3. **Core Infrastructure**: Connection factory, base class, schema
4. **Repository Implementations**: All database operation classes
5. **Service Provider**: Integration with existing interfaces
6. **Migration Tool**: Standalone utility for user migration
7. **Integration**: Feature flags, testing, validation

Each task follows TDD: failing test first, minimal implementation, verify, commit.
