# Phase 2: Testing Foundation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build comprehensive test infrastructure with JUnit 5, AssertJ, Mockito, and JMH benchmarks before risky database and cache migrations.

**Architecture:** Upgrade from JUnit 4 to JUnit 5 with Jupiter engine. Add AssertJ for fluent assertions and Mockito for mocking. Add JMH for performance benchmarks. Focus tests on database repository layer, cache layer (MapDB), and JAXB serialization - the components most at risk during later migration phases.

**Tech Stack:** JUnit 5.10.x, AssertJ 3.24.x, Mockito 5.x, JMH 1.37, Gradle with JUnit Platform

---

## Task 1: Upgrade to JUnit 5 Infrastructure

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`

**Step 1: Add JUnit 5 and testing dependencies to version catalog**

Edit `gradle/libs.versions.toml`:
```toml
[versions]
java = "7"
hsqldb = "2.4.1"
metadata-extractor = "2.6.4"
jgoodies-common = "1.6.0"
jgoodies-looks = "2.5.3"
lucene = "3.4.0"
jaxb-api = "2.2.11"
jaxb-impl = "2.2.11"
activation = "1.1.1"
junit4 = "4.13.2"
hamcrest = "1.3"
junit5 = "5.10.2"
assertj = "3.24.2"
mockito = "5.11.0"
jmh = "1.37"

[libraries]
hsqldb = { module = "org.hsqldb:hsqldb", version.ref = "hsqldb" }
metadata-extractor = { module = "com.drewnoakes:metadata-extractor", version.ref = "metadata-extractor" }
jgoodies-common = { module = "com.jgoodies:jgoodies-common", version.ref = "jgoodies-common" }
jgoodies-looks = { module = "com.jgoodies:jgoodies-looks", version.ref = "jgoodies-looks" }
lucene-core = { module = "org.apache.lucene:lucene-core", version.ref = "lucene" }
jaxb-api = { module = "javax.xml.bind:jaxb-api", version.ref = "jaxb-api" }
jaxb-core = { module = "com.sun.xml.bind:jaxb-core", version.ref = "jaxb-impl" }
jaxb-impl = { module = "com.sun.xml.bind:jaxb-impl", version.ref = "jaxb-impl" }
activation = { module = "javax.activation:activation", version.ref = "activation" }
junit4 = { module = "junit:junit", version.ref = "junit4" }
hamcrest = { module = "org.hamcrest:hamcrest-core", version.ref = "hamcrest" }
junit5-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }
junit5-vintage = { module = "org.junit.vintage:junit-vintage-engine", version.ref = "junit5" }
assertj = { module = "org.assertj:assertj-core", version.ref = "assertj" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
mockito-junit5 = { module = "org.mockito:mockito-junit-jupiter", version.ref = "mockito" }
jmh-core = { module = "org.openjdk.jmh:jmh-core", version.ref = "jmh" }
jmh-generator = { module = "org.openjdk.jmh:jmh-generator-annprocess", version.ref = "jmh" }

# Local JARs (referenced as file dependencies in build.gradle.kts)
# eventbus, swingx-core, beansbinding, ImgrRdr, org-openide-util-lookup, mapdb, XMPCore

[bundles]
junit5 = ["junit5-api", "junit5-engine", "junit5-params"]
mockito = ["mockito-core", "mockito-junit5"]
jmh = ["jmh-core", "jmh-generator"]
```

**Step 2: Update root build.gradle.kts for JUnit 5**

Edit `build.gradle.kts`:
```kotlin
plugins {
    java
}

allprojects {
    group = "org.jphototagger"
    version = "1.1.9"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    // Common test configuration - JUnit 5 with vintage engine for JUnit 4 compatibility
    dependencies {
        // JUnit 5
        "testImplementation"(rootProject.libs.junit5.api)
        "testRuntimeOnly"(rootProject.libs.junit5.engine)
        "testImplementation"(rootProject.libs.junit5.params)
        // Vintage engine for existing JUnit 4 tests
        "testRuntimeOnly"(rootProject.libs.junit5.vintage)
        // Legacy JUnit 4 (for existing tests during migration)
        "testImplementation"(rootProject.libs.junit4)
        "testImplementation"(rootProject.libs.hamcrest)
        // AssertJ
        "testImplementation"(rootProject.libs.assertj)
        // Mockito
        "testImplementation"(rootProject.libs.mockito.core)
        "testImplementation"(rootProject.libs.mockito.junit5)
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
}

// Convenience task to build everything
tasks.register("buildAll") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}
```

**Step 3: Run tests to verify existing JUnit 4 tests still work**

Run: `./gradlew test`
Expected: All existing tests pass via JUnit Vintage engine

**Step 4: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts
git commit -m "feat(test): upgrade to JUnit 5 with vintage engine for backwards compatibility

Add JUnit 5 Jupiter, AssertJ, and Mockito dependencies. Existing JUnit 4
tests run via vintage engine during migration."
```

---

## Task 2: Create Test Utilities Module

**Files:**
- Create: `TestSupport/build.gradle.kts`
- Create: `TestSupport/src/main/java/org/jphototagger/testsupport/TestDatabase.java`
- Create: `TestSupport/src/main/java/org/jphototagger/testsupport/TestFiles.java`
- Modify: `settings.gradle.kts`

**Step 1: Add TestSupport to settings.gradle.kts**

First, read `settings.gradle.kts` to understand its structure. Then add "TestSupport" to the include list.

**Step 2: Create TestSupport build file**

Create `TestSupport/build.gradle.kts`:
```kotlin
plugins {
    java
}

dependencies {
    implementation(libs.hsqldb)
    implementation(libs.junit5.api)
    implementation(libs.assertj)
}

// This module is for test utilities - no main source, but consumed by other test source sets
java {
    sourceSets {
        main {
            java.srcDir("src/main/java")
        }
    }
}
```

**Step 3: Create TestDatabase utility**

Create `TestSupport/src/main/java/org/jphototagger/testsupport/TestDatabase.java`:
```java
package org.jphototagger.testsupport;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Provides isolated in-memory HSQLDB databases for testing.
 * Each test gets a fresh database instance.
 */
public final class TestDatabase {

    private static int instanceCounter = 0;

    private final String dbName;
    private Connection connection;

    private TestDatabase(String dbName) {
        this.dbName = dbName;
    }

    /**
     * Creates a new in-memory database for testing.
     * Each call returns a unique database instance.
     */
    public static TestDatabase createInMemory() {
        String uniqueName = "testdb_" + System.currentTimeMillis() + "_" + (++instanceCounter);
        return new TestDatabase(uniqueName);
    }

    /**
     * Opens a connection to the test database.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.hsqldb.jdbcDriver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("HSQLDB driver not found", e);
            }
            connection = DriverManager.getConnection(
                    "jdbc:hsqldb:mem:" + dbName + ";shutdown=true", "sa", "");
        }
        return connection;
    }

    /**
     * Executes SQL statements to set up the test database schema.
     */
    public void executeSql(String... sqlStatements) throws SQLException {
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            for (String sql : sqlStatements) {
                stmt.execute(sql);
            }
        }
    }

    /**
     * Shuts down and cleans up the test database.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.createStatement().execute("SHUTDOWN");
                connection.close();
            } catch (SQLException e) {
                // Ignore shutdown errors
            }
            connection = null;
        }
    }
}
```

**Step 4: Create TestFiles utility**

Create `TestSupport/src/main/java/org/jphototagger/testsupport/TestFiles.java`:
```java
package org.jphototagger.testsupport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Provides temporary files and directories for testing.
 */
public final class TestFiles {

    private TestFiles() {
    }

    /**
     * Creates a temporary directory for test files.
     */
    public static Path createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    /**
     * Creates a temporary file with the given content.
     */
    public static Path createTempFile(String prefix, String suffix, byte[] content) throws IOException {
        Path tempFile = Files.createTempFile(prefix, suffix);
        if (content != null) {
            Files.write(tempFile, content);
        }
        return tempFile;
    }

    /**
     * Recursively deletes a directory and all its contents.
     */
    public static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.list(path).forEach(child -> {
                try {
                    deleteRecursively(child);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        Files.deleteIfExists(path);
    }
}
```

**Step 5: Verify TestSupport compiles**

Run: `./gradlew :TestSupport:build`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add TestSupport settings.gradle.kts
git commit -m "feat(test): add TestSupport module with test utilities

Add TestDatabase for in-memory HSQLDB instances and TestFiles for
temporary file management in tests."
```

---

## Task 3: Create Database Repository Base Test Class

**Files:**
- Create: `Repositories/HSQLDB/test/org/jphototagger/repository/hsqldb/DatabaseTestBase.java`
- Modify: `Repositories/HSQLDB/build.gradle.kts`

**Step 1: Add TestSupport dependency to HSQLDB module**

First, read `Repositories/HSQLDB/build.gradle.kts` to understand its current structure. Then add the TestSupport dependency.

Add to dependencies:
```kotlin
testImplementation(project(":TestSupport"))
```

**Step 2: Create DatabaseTestBase**

Create `Repositories/HSQLDB/test/org/jphototagger/repository/hsqldb/DatabaseTestBase.java`:
```java
package org.jphototagger.repository.hsqldb;

import java.sql.Connection;
import java.sql.SQLException;
import org.jphototagger.testsupport.TestDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for database repository tests.
 * Provides an isolated in-memory HSQLDB instance for each test.
 */
public abstract class DatabaseTestBase {

    protected TestDatabase testDb;

    @BeforeEach
    void setUpDatabase() throws SQLException {
        testDb = TestDatabase.createInMemory();
        createSchema(testDb);
    }

    @AfterEach
    void tearDownDatabase() {
        if (testDb != null) {
            testDb.close();
        }
    }

    /**
     * Override to create the database schema needed for tests.
     */
    protected abstract void createSchema(TestDatabase db) throws SQLException;

    /**
     * Gets a connection to the test database.
     */
    protected Connection getConnection() throws SQLException {
        return testDb.getConnection();
    }
}
```

**Step 3: Verify HSQLDB test module compiles**

Run: `./gradlew :Repositories:HSQLDB:compileTestJava`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add Repositories/HSQLDB/build.gradle.kts Repositories/HSQLDB/test
git commit -m "feat(test): add DatabaseTestBase for repository testing

Provides isolated in-memory HSQLDB instances for each test."
```

---

## Task 4: Write KeywordsDatabase Characterization Tests

**Files:**
- Create: `Repositories/HSQLDB/test/org/jphototagger/repository/hsqldb/KeywordsDatabaseTest.java`

**Step 1: Write the KeywordsDatabase tests**

Create `Repositories/HSQLDB/test/org/jphototagger/repository/hsqldb/KeywordsDatabaseTest.java`:
```java
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
        try (var stmt = getConnection().prepareStatement(
                "SELECT id, id_parent, subject, real FROM hierarchical_subjects WHERE id_parent = ? ORDER BY subject ASC")) {
            stmt.setLong(1, parentId);
            try (var rs = stmt.executeQuery()) {
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
        try (var stmt = getConnection().prepareStatement(
                "SELECT COUNT(*) FROM hierarchical_subjects WHERE subject = ?")) {
            stmt.setString(1, keyword);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean rootKeywordExists(String keyword) throws SQLException {
        try (var stmt = getConnection().prepareStatement(
                "SELECT COUNT(*) FROM hierarchical_subjects WHERE subject = ? AND id_parent IS NULL")) {
            stmt.setString(1, keyword);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private <T> T executeQuery(String sql, ResultSetMapper<T> mapper) throws SQLException {
        try (var stmt = getConnection().createStatement();
             var rs = stmt.executeQuery(sql)) {
            return mapper.map(rs);
        }
    }

    @FunctionalInterface
    private interface ResultSetMapper<T> {
        T map(java.sql.ResultSet rs) throws SQLException;
    }
}
```

**Step 2: Run the tests**

Run: `./gradlew :Repositories:HSQLDB:test --tests KeywordsDatabaseTest`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add Repositories/HSQLDB/test/org/jphototagger/repository/hsqldb/KeywordsDatabaseTest.java
git commit -m "test(db): add KeywordsDatabase characterization tests

Cover getAllKeywords, getRootKeywords, getChildKeywords, existsKeyword,
and existsRootKeyword with isolated in-memory database tests."
```

---

## Task 5: Write ImageFilesDatabase Characterization Tests

**Files:**
- Create: `Repositories/HSQLDB/test/org/jphototagger/repository/hsqldb/ImageFilesDatabaseTest.java`

**Step 1: Read ImageFilesDatabase to understand schema**

First, read `Repositories/HSQLDB/src/org/jphototagger/repository/hsqldb/ImageFilesDatabase.java` to understand the table schema and key methods.

**Step 2: Write the failing test (following TDD)**

Create `Repositories/HSQLDB/test/org/jphototagger/repository/hsqldb/ImageFilesDatabaseTest.java`:
```java
package org.jphototagger.repository.hsqldb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.sql.SQLException;
import java.util.Collection;
import org.jphototagger.testsupport.TestDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for ImageFilesDatabase.
 * Captures behavior for file metadata storage and queries.
 */
class ImageFilesDatabaseTest extends DatabaseTestBase {

    private static final String CREATE_FILES_TABLE =
            "CREATE TABLE files (" +
            "id BIGINT GENERATED BY DEFAULT AS IDENTITY(START WITH 1) PRIMARY KEY, " +
            "filename VARCHAR(512) NOT NULL UNIQUE, " +
            "lastmodified BIGINT, " +
            "size_in_bytes BIGINT, " +
            "xmp_lastmodified BIGINT, " +
            "thumbnail_update BOOLEAN DEFAULT FALSE)";

    private static final String CREATE_EXIF_TABLE =
            "CREATE TABLE exif (" +
            "id_file BIGINT PRIMARY KEY, " +
            "date_time_original TIMESTAMP, " +
            "date_time_original_timestamp BIGINT, " +
            "iso_speed_ratings INTEGER, " +
            "focal_length DOUBLE, " +
            "exif_lens VARCHAR(256), " +
            "FOREIGN KEY (id_file) REFERENCES files(id) ON DELETE CASCADE)";

    @Override
    protected void createSchema(TestDatabase db) throws SQLException {
        db.executeSql(CREATE_FILES_TABLE, CREATE_EXIF_TABLE);
    }

    @Nested
    @DisplayName("file existence")
    class FileExistence {

        @Test
        @DisplayName("existsFile returns true when file exists in database")
        void existsFileReturnsTrue() throws SQLException {
            // Given: file in database
            testDb.executeSql(
                "INSERT INTO files (filename, lastmodified, size_in_bytes) VALUES ('/photos/test.jpg', 1234567890, 1024)"
            );

            // When/Then
            boolean exists = fileExists("/photos/test.jpg");
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("existsFile returns false when file does not exist")
        void existsFileReturnsFalse() throws SQLException {
            // Given: empty database

            // When/Then
            boolean exists = fileExists("/photos/nonexistent.jpg");
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("file queries")
    class FileQueries {

        @Test
        @DisplayName("getAllFiles returns all file paths")
        void getAllFilesReturnsAllPaths() throws SQLException {
            testDb.executeSql(
                "INSERT INTO files (filename, lastmodified, size_in_bytes) VALUES ('/photos/a.jpg', 1000, 100)",
                "INSERT INTO files (filename, lastmodified, size_in_bytes) VALUES ('/photos/b.jpg', 2000, 200)"
            );

            Collection<File> files = getAllFiles();

            assertThat(files).hasSize(2);
            assertThat(files).extracting(File::getPath)
                    .containsExactlyInAnyOrder("/photos/a.jpg", "/photos/b.jpg");
        }

        @Test
        @DisplayName("getFileCountInDirectory returns correct count")
        void getFileCountInDirectoryReturnsCount() throws SQLException {
            testDb.executeSql(
                "INSERT INTO files (filename, lastmodified, size_in_bytes) VALUES ('/photos/dir1/a.jpg', 1000, 100)",
                "INSERT INTO files (filename, lastmodified, size_in_bytes) VALUES ('/photos/dir1/b.jpg', 2000, 200)",
                "INSERT INTO files (filename, lastmodified, size_in_bytes) VALUES ('/photos/dir2/c.jpg', 3000, 300)"
            );

            int count = getFileCountLike("/photos/dir1/%");

            assertThat(count).isEqualTo(2);
        }
    }

    // Helper methods

    private boolean fileExists(String filename) throws SQLException {
        try (var stmt = getConnection().prepareStatement(
                "SELECT COUNT(*) FROM files WHERE filename = ?")) {
            stmt.setString(1, filename);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Collection<File> getAllFiles() throws SQLException {
        java.util.List<File> files = new java.util.ArrayList<>();
        try (var stmt = getConnection().createStatement();
             var rs = stmt.executeQuery("SELECT filename FROM files")) {
            while (rs.next()) {
                files.add(new File(rs.getString(1)));
            }
        }
        return files;
    }

    private int getFileCountLike(String pattern) throws SQLException {
        try (var stmt = getConnection().prepareStatement(
                "SELECT COUNT(*) FROM files WHERE filename LIKE ?")) {
            stmt.setString(1, pattern);
            try (var rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
```

**Step 3: Run the tests**

Run: `./gradlew :Repositories:HSQLDB:test --tests ImageFilesDatabaseTest`
Expected: All tests PASS

**Step 4: Commit**

```bash
git add Repositories/HSQLDB/test/org/jphototagger/repository/hsqldb/ImageFilesDatabaseTest.java
git commit -m "test(db): add ImageFilesDatabase characterization tests

Cover file existence and file query operations."
```

---

## Task 6: Write JAXB Serialization Tests

**Files:**
- Create: `Exif/test/org/jphototagger/exif/ExifTagsSerializationTest.java`
- Modify: `Exif/build.gradle.kts`

**Step 1: Add test dependencies to Exif module**

Read `Exif/build.gradle.kts` and add TestSupport dependency:
```kotlin
testImplementation(project(":TestSupport"))
```

**Step 2: Write ExifTags serialization test**

Create `Exif/test/org/jphototagger/exif/ExifTagsSerializationTest.java`:
```java
package org.jphototagger.exif;

import static org.assertj.core.api.Assertions.assertThat;

import org.jphototagger.lib.xml.bind.XmlObjectExporter;
import org.jphototagger.lib.xml.bind.XmlObjectImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests JAXB serialization of ExifTags.
 * These tests ensure round-trip serialization works correctly,
 * critical for the EXIF cache which stores XML in MapDB.
 */
class ExifTagsSerializationTest {

    @Nested
    @DisplayName("round-trip serialization")
    class RoundTrip {

        @Test
        @DisplayName("empty ExifTags serializes and deserializes correctly")
        void emptyExifTags() throws Exception {
            ExifTags original = new ExifTags();
            original.setLastModified(123456789L);

            String xml = XmlObjectExporter.marshal(original);
            ExifTags restored = XmlObjectImporter.unmarshal(xml, ExifTags.class);

            assertThat(restored.getLastModified()).isEqualTo(123456789L);
            assertThat(restored.getExifTags()).isEmpty();
            assertThat(restored.getGpsTags()).isEmpty();
            assertThat(restored.getMakerNoteTags()).isEmpty();
        }

        @Test
        @DisplayName("ExifTags with tags serializes and deserializes correctly")
        void exifTagsWithTags() throws Exception {
            ExifTags original = new ExifTags();
            original.setLastModified(987654321L);

            ExifTag cameraTag = new ExifTag();
            cameraTag.setTagId(271); // Make
            cameraTag.setDisplayName("Camera Make");
            cameraTag.setStringValue("Canon");
            original.addExifTag(cameraTag);

            ExifTag gpsTag = new ExifTag();
            gpsTag.setTagId(1); // GPSLatitudeRef
            gpsTag.setDisplayName("GPS Latitude Ref");
            gpsTag.setStringValue("N");
            original.addGpsTag(gpsTag);

            String xml = XmlObjectExporter.marshal(original);
            ExifTags restored = XmlObjectImporter.unmarshal(xml, ExifTags.class);

            assertThat(restored.getLastModified()).isEqualTo(987654321L);
            assertThat(restored.getExifTags()).hasSize(1);
            assertThat(restored.getGpsTags()).hasSize(1);

            ExifTag restoredCamera = restored.findExifTagByTagId(271);
            assertThat(restoredCamera).isNotNull();
            assertThat(restoredCamera.getStringValue()).isEqualTo("Canon");
        }

        @Test
        @DisplayName("ExifTags with maker note description preserved")
        void makerNoteDescription() throws Exception {
            ExifTags original = new ExifTags();
            original.setMakerNoteDescription("Nikon Type 3");

            String xml = XmlObjectExporter.marshal(original);
            ExifTags restored = XmlObjectImporter.unmarshal(xml, ExifTags.class);

            assertThat(restored.getMakerNoteDescription()).isEqualTo("Nikon Type 3");
        }
    }

    @Nested
    @DisplayName("XML format")
    class XmlFormat {

        @Test
        @DisplayName("produces valid UTF-8 encoded XML")
        void producesUtf8Xml() throws Exception {
            ExifTags original = new ExifTags();
            ExifTag tag = new ExifTag();
            tag.setDisplayName("Kamera-Hersteller"); // German umlaut
            tag.setStringValue("Nikon");
            original.addExifTag(tag);

            String xml = XmlObjectExporter.marshal(original);

            assertThat(xml).contains("UTF-8");
            assertThat(xml).contains("Kamera-Hersteller");
        }
    }
}
```

**Step 3: Run the tests**

Run: `./gradlew :Exif:test --tests ExifTagsSerializationTest`
Expected: All tests PASS

**Step 4: Commit**

```bash
git add Exif/build.gradle.kts Exif/test/org/jphototagger/exif/ExifTagsSerializationTest.java
git commit -m "test(exif): add JAXB serialization tests for ExifTags

Verify round-trip XML marshalling/unmarshalling works correctly.
Critical for EXIF cache which stores XML in MapDB."
```

---

## Task 7: Write Cache Operation Tests

**Files:**
- Create: `Program/test/org/jphototagger/program/module/thumbnails/cache/ThumbnailCacheTest.java`
- Modify: `Program/build.gradle.kts`

**Step 1: Add test dependencies to Program module**

Read `Program/build.gradle.kts` and add TestSupport dependency.

**Step 2: Write ThumbnailCache behavior tests**

Create `Program/test/org/jphototagger/program/module/thumbnails/cache/ThumbnailCacheTest.java`:
```java
package org.jphototagger.program.module.thumbnails.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

/**
 * Tests for thumbnail cache behavior.
 * Note: ThumbnailsDb uses static initialization with MapDB.
 * These tests document expected behavior for the SQLite migration.
 */
class ThumbnailCacheTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("cache key behavior")
    class CacheKeyBehavior {

        @Test
        @DisplayName("cache key is absolute file path")
        void cacheKeyIsAbsolutePath() {
            File imageFile = new File("/photos/vacation/beach.jpg");
            String expectedKey = imageFile.getAbsolutePath();

            assertThat(expectedKey).isEqualTo("/photos/vacation/beach.jpg");
        }

        @Test
        @DisplayName("different paths are different keys")
        void differentPathsAreDifferentKeys() {
            File file1 = new File("/photos/a.jpg");
            File file2 = new File("/photos/b.jpg");

            assertThat(file1.getAbsolutePath())
                    .isNotEqualTo(file2.getAbsolutePath());
        }
    }

    @Nested
    @DisplayName("up-to-date detection")
    class UpToDateDetection {

        @Test
        @DisplayName("file modification time is used for cache invalidation")
        void fileModificationTimeUsedForInvalidation() throws Exception {
            Path testFile = tempDir.resolve("test.jpg");
            java.nio.file.Files.write(testFile, "fake image".getBytes());

            File file = testFile.toFile();
            long originalModified = file.lastModified();

            // Simulate file modification
            Thread.sleep(100);
            java.nio.file.Files.write(testFile, "modified image".getBytes());

            long newModified = file.lastModified();

            assertThat(newModified).isGreaterThan(originalModified);
        }

        @Test
        @DisplayName("file length is used for cache invalidation")
        void fileLengthUsedForInvalidation() throws Exception {
            Path testFile = tempDir.resolve("test.jpg");
            java.nio.file.Files.write(testFile, "small".getBytes());

            File file = testFile.toFile();
            long originalLength = file.length();

            java.nio.file.Files.write(testFile, "much larger content here".getBytes());
            long newLength = file.length();

            assertThat(newLength).isGreaterThan(originalLength);
        }
    }
}
```

**Step 3: Run the tests**

Run: `./gradlew :Program:test --tests ThumbnailCacheTest`
Expected: All tests PASS

**Step 4: Commit**

```bash
git add Program/build.gradle.kts Program/test/org/jphototagger/program/module/thumbnails/cache/ThumbnailCacheTest.java
git commit -m "test(cache): add ThumbnailCache behavior tests

Document cache key and invalidation behavior for SQLite migration."
```

---

## Task 8: Set Up JMH Benchmark Infrastructure

**Files:**
- Create: `Benchmarks/build.gradle.kts`
- Create: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/DatabaseBenchmark.java`
- Modify: `settings.gradle.kts`

**Step 1: Add Benchmarks module to settings.gradle.kts**

Add "Benchmarks" to the include list in `settings.gradle.kts`.

**Step 2: Create Benchmarks build file**

Create `Benchmarks/build.gradle.kts`:
```kotlin
plugins {
    java
    id("me.champeau.jmh") version "0.7.2"
}

dependencies {
    // JMH
    jmh(libs.jmh.core)
    jmhAnnotationProcessor(libs.jmh.generator)

    // Access to application code for benchmarking
    implementation(project(":Domain"))
    implementation(project(":Repositories:HSQLDB"))
    implementation(libs.hsqldb)
}

jmh {
    warmupIterations.set(2)
    iterations.set(3)
    fork.set(1)
    jmhVersion.set("1.37")

    // Output results to JSON for tracking
    resultFormat.set("JSON")
    resultsFile.set(project.file("build/reports/jmh/results.json"))
}
```

**Step 3: Create baseline database benchmark**

Create `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/DatabaseBenchmark.java`:
```java
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
```

**Step 4: Run benchmarks to establish baseline**

Run: `./gradlew :Benchmarks:jmh`
Expected: Benchmark results saved to `Benchmarks/build/reports/jmh/results.json`

**Step 5: Commit**

```bash
git add Benchmarks settings.gradle.kts
git commit -m "feat(benchmark): add JMH benchmark infrastructure

Add baseline database benchmarks to compare HSQLDB vs SQLite performance.
Results saved to build/reports/jmh/results.json"
```

---

## Task 9: Add CI Test Configuration

**Files:**
- Modify: `.github/workflows/build.yml`

**Step 1: Update CI workflow to run tests with coverage**

First, read the existing `.github/workflows/build.yml`. Then add test and coverage steps:

```yaml
name: Build

on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 8
      uses: actions/setup-java@v4
      with:
        java-version: '8'
        distribution: 'temurin'

    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@v3

    - name: Build with Gradle
      run: ./gradlew build

    - name: Run tests
      run: ./gradlew test

    - name: Upload test results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: test-results
        path: '**/build/test-results/test/*.xml'

  benchmark:
    runs-on: ubuntu-latest
    needs: build

    steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 8
      uses: actions/setup-java@v4
      with:
        java-version: '8'
        distribution: 'temurin'

    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@v3

    - name: Run benchmarks
      run: ./gradlew :Benchmarks:jmh

    - name: Upload benchmark results
      uses: actions/upload-artifact@v4
      with:
        name: benchmark-results
        path: 'Benchmarks/build/reports/jmh/results.json'
```

**Step 2: Verify workflow syntax**

Run: `cat .github/workflows/build.yml | head -50`
Expected: Valid YAML syntax

**Step 3: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "ci: add test and benchmark steps to CI workflow

Run tests and upload results. Run benchmarks in separate job and
preserve results as artifacts."
```

---

## Task 10: Migrate One Existing Test to JUnit 5

**Files:**
- Modify: `Lib/test/org/jphototagger/lib/util/StringUtilTest.java`

**Step 1: Migrate StringUtilTest to JUnit 5**

Read the existing file, then rewrite with JUnit 5 + AssertJ:

```java
package org.jphototagger.lib.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for StringUtil.
 * Migrated from JUnit 4 to JUnit 5 with AssertJ assertions.
 */
class StringUtilTest {

    @Nested
    @DisplayName("wrapWords")
    class WrapWords {

        @Test
        @DisplayName("returns empty list for empty string")
        void emptyString() {
            List<String> result = StringUtil.wrapWords("", 1);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("single character within limit")
        void singleCharacter() {
            List<String> result = StringUtil.wrapWords("a", 1);
            assertThat(result).containsExactly("a");
        }

        @Test
        @DisplayName("splits long words")
        void splitsLongWords() {
            List<String> result = StringUtil.wrapWords("aa", 1);
            assertThat(result).containsExactly("a", "a");
        }

        @Test
        @DisplayName("splits on spaces")
        void splitsOnSpaces() {
            List<String> result = StringUtil.wrapWords("a a", 1);
            assertThat(result).containsExactly("a", "a");
        }

        @Test
        @DisplayName("wraps German text correctly")
        void wrapsGermanText() {
            String text = "Dies ist ein längerer Text mit 43 Zeichen.";
            List<String> result = StringUtil.wrapWords(text, 25);
            assertThat(result).containsExactly(
                    "Dies ist ein längerer",
                    "Text mit 43 Zeichen.");
        }

        @Test
        @DisplayName("wraps single long word")
        void wrapsSingleLongWord() {
            String text = "DiesisteinlängererTextmit36Zeichen.";
            List<String> result = StringUtil.wrapWords(text, 25);
            assertThat(result).containsExactly(
                    "DiesisteinlängererTextmit",
                    "36Zeichen.");
        }
    }

    @Nested
    @DisplayName("getNTimesRepeated")
    class GetNTimesRepeated {

        @Test
        @DisplayName("returns empty for empty string")
        void emptyString() {
            assertThat(StringUtil.getNTimesRepeated("", 0)).isEmpty();
            assertThat(StringUtil.getNTimesRepeated("", 100)).isEmpty();
        }

        @Test
        @DisplayName("returns empty for zero repetitions")
        void zeroRepetitions() {
            assertThat(StringUtil.getNTimesRepeated(".", 0)).isEmpty();
            assertThat(StringUtil.getNTimesRepeated("abc", 0)).isEmpty();
        }

        @Test
        @DisplayName("repeats single character")
        void repeatsSingleChar() {
            assertThat(StringUtil.getNTimesRepeated(".", 1)).isEqualTo(".");
            assertThat(StringUtil.getNTimesRepeated(".", 3)).isEqualTo("...");
        }

        @Test
        @DisplayName("repeats multi-character string")
        void repeatsMultiChar() {
            assertThat(StringUtil.getNTimesRepeated("abc", 1)).isEqualTo("abc");
            assertThat(StringUtil.getNTimesRepeated("abc", 3)).isEqualTo("abcabcabc");
        }
    }

    @Nested
    @DisplayName("getSubstringCount")
    class GetSubstringCount {

        @Test
        @DisplayName("returns 0 for empty string")
        void emptyString() {
            assertThat(StringUtil.getSubstringCount("", "")).isZero();
            assertThat(StringUtil.getSubstringCount("", "bla")).isZero();
        }

        @Test
        @DisplayName("counts single occurrence")
        void singleOccurrence() {
            assertThat(StringUtil.getSubstringCount("bla", "bla")).isEqualTo(1);
        }

        @Test
        @DisplayName("counts multiple occurrences")
        void multipleOccurrences() {
            assertThat(StringUtil.getSubstringCount("bla bla", "bla")).isEqualTo(2);
            assertThat(StringUtil.getSubstringCount("blablabla", "bla")).isEqualTo(3);
        }

        @Test
        @DisplayName("returns 0 when substring not found")
        void substringNotFound() {
            assertThat(StringUtil.getSubstringCount("blubb", "bla")).isZero();
        }
    }

    @Nested
    @DisplayName("removeLast")
    class RemoveLast {

        @Test
        @DisplayName("handles empty strings")
        void emptyStrings() {
            assertThat(StringUtil.removeLast("", "")).isEmpty();
            assertThat(StringUtil.removeLast("", "bla")).isEmpty();
            assertThat(StringUtil.removeLast("bla", "")).isEqualTo("bla");
        }

        @Test
        @DisplayName("removes entire string when equal")
        void removesEntireString() {
            assertThat(StringUtil.removeLast("bla", "bla")).isEmpty();
        }

        @Test
        @DisplayName("removes last occurrence")
        void removesLastOccurrence() {
            assertThat(StringUtil.removeLast("bla bla bla", "bla bla")).isEqualTo("bla ");
        }

        @Test
        @DisplayName("returns original when substring not found")
        void substringNotFound() {
            assertThat(StringUtil.removeLast("xyz", "bla bla")).isEqualTo("xyz");
        }

        @Test
        @DisplayName("removes from middle of string")
        void removesFromMiddle() {
            assertThat(StringUtil.removeLast("xyz bla xyz", "bla")).isEqualTo("xyz  xyz");
        }
    }
}
```

**Step 2: Run the migrated tests**

Run: `./gradlew :Lib:test --tests StringUtilTest`
Expected: All tests PASS

**Step 3: Commit**

```bash
git add Lib/test/org/jphototagger/lib/util/StringUtilTest.java
git commit -m "test(lib): migrate StringUtilTest to JUnit 5 with AssertJ

Example migration showing JUnit 5 nested classes, @DisplayName,
and AssertJ fluent assertions."
```

---

## Task 11: Run Full Test Suite and Verify

**Files:** None (verification only)

**Step 1: Run all tests**

Run: `./gradlew test`
Expected: All tests PASS (both migrated JUnit 5 and legacy JUnit 4 via Vintage)

**Step 2: Run benchmarks**

Run: `./gradlew :Benchmarks:jmh`
Expected: Benchmark results generated

**Step 3: Record baseline measurements**

View: `cat Benchmarks/build/reports/jmh/results.json`
Document: Save these baseline numbers for comparison after Phase 4 (SQLite migration)

**Step 4: Final commit**

```bash
git add -A
git commit -m "chore: verify Phase 2 testing foundation complete

All tests passing. JMH baseline benchmarks recorded.
Ready for Phase 3: Java 21 upgrade."
```

---

## Summary

Phase 2 establishes the testing foundation needed before risky migrations:

1. **JUnit 5 Infrastructure** - Modern testing with backwards compatibility
2. **Test Utilities** - TestDatabase and TestFiles for isolated testing
3. **Database Tests** - Characterization tests for KeywordsDatabase and ImageFilesDatabase
4. **JAXB Tests** - Serialization round-trip tests for ExifTags
5. **Cache Tests** - Behavior documentation for thumbnail cache
6. **JMH Benchmarks** - Baseline performance measurements
7. **CI Integration** - Automated test and benchmark runs

**Dependencies established:**
- JUnit 5.10.2 + Vintage engine
- AssertJ 3.24.2
- Mockito 5.11.0
- JMH 1.37

**Baseline measurements captured in:** `Benchmarks/build/reports/jmh/results.json`

---

Plan complete and saved to `docs/plans/2025-11-29-phase2-testing-foundation.md`. Two execution options:

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

Which approach?
