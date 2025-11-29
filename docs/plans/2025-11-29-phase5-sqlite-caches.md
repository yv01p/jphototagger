# Phase 5: SQLite Caches Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace MapDB thumbnail and EXIF caches with SQLite, creating a separate cache.db file that can be safely deleted without data loss.

**Architecture:** Create a new `CacheDb` module providing SQLite-backed implementations of thumbnail and EXIF caching. The caches use a single `cache.db` file with WAL mode for concurrent reads. Each cache tracks file modification time to auto-invalidate stale entries.

**Tech Stack:** SQLite JDBC (xerial), existing SqliteConnectionFactory pattern from Phase 4, JUnit 5 + AssertJ for tests, JMH for benchmarks.

---

## Pre-Phase Checkpoint

### Task 0: Run Baseline Benchmarks

**Files:**
- Read: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheBenchmark.java`
- Read: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheBenchmark.java`
- Create: `docs/benchmarks/pre-phase5-cache.json`

**Step 1: Run existing cache benchmarks**

Run:
```bash
./gradlew :Benchmarks:jmh -Pjmh.includes="ThumbnailCacheBenchmark|ExifCacheBenchmark"
```
Expected: Benchmark results showing MapDB-simulated performance baselines

**Step 2: Save baseline results**

Run:
```bash
cp Benchmarks/build/results/jmh/results.json docs/benchmarks/pre-phase5-cache.json
```
Expected: File copied successfully

**Step 3: Commit baseline**

```bash
git add docs/benchmarks/pre-phase5-cache.json
git commit -m "docs: add pre-phase5 cache benchmark baseline"
```

---

## Part 1: Cache Database Infrastructure

### Task 1: Create CacheDb Module Structure

**Files:**
- Create: `CacheDb/build.gradle.kts`
- Modify: `settings.gradle.kts` (add include)

**Step 1: Write the failing test - module exists**

Run:
```bash
./gradlew :CacheDb:build
```
Expected: FAIL - project ':CacheDb' not found

**Step 2: Create build.gradle.kts**

Create `CacheDb/build.gradle.kts`:
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
    implementation(project(":Exif"))

    implementation(libs.sqlite.jdbc)

    compileOnly(files("../Libraries/org-openide-util-lookup-8.6.jar"))

    testImplementation(project(":TestSupport"))
    testImplementation(libs.bundles.junit5)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}
```

**Step 3: Add to settings.gradle.kts**

Add to `settings.gradle.kts` in the include block:
```kotlin
include("CacheDb")
```

**Step 4: Create source directories**

Run:
```bash
mkdir -p CacheDb/src/org/jphototagger/cachedb
mkdir -p CacheDb/test/org/jphototagger/cachedb
```
Expected: Directories created

**Step 5: Run build to verify module setup**

Run:
```bash
./gradlew :CacheDb:build
```
Expected: BUILD SUCCESSFUL (empty module compiles)

**Step 6: Commit**

```bash
git add CacheDb settings.gradle.kts
git commit -m "feat(cachedb): create CacheDb module skeleton"
```

---

### Task 2: Create CacheConnectionFactory

**Files:**
- Create: `CacheDb/src/org/jphototagger/cachedb/CacheConnectionFactory.java`
- Test: `CacheDb/test/org/jphototagger/cachedb/CacheConnectionFactoryTest.java`

**Step 1: Write the failing test**

Create `CacheDb/test/org/jphototagger/cachedb/CacheConnectionFactoryTest.java`:
```java
package org.jphototagger.cachedb;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class CacheConnectionFactoryTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private File dbFile;

    @BeforeEach
    void setUp() {
        dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getConnection_createsDatabase() throws Exception {
        Connection con = factory.getConnection();
        assertThat(con).isNotNull();
        assertThat(dbFile).exists();
        con.close();
    }

    @Test
    void getConnection_enablesWalMode() throws Exception {
        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA journal_mode")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualToIgnoringCase("wal");
        }
    }

    @Test
    void close_closesAllConnections() throws Exception {
        Connection con1 = factory.getConnection();
        Connection con2 = factory.getConnection();
        factory.close();
        assertThat(con1.isClosed()).isTrue();
        assertThat(con2.isClosed()).isTrue();
    }
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :CacheDb:test --tests "CacheConnectionFactoryTest"
```
Expected: FAIL - CacheConnectionFactory class not found

**Step 3: Write minimal implementation**

Create `CacheDb/src/org/jphototagger/cachedb/CacheConnectionFactory.java`:
```java
package org.jphototagger.cachedb;

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
 * Factory for SQLite cache database connections with WAL mode.
 * Separate from main database - cache can be deleted without data loss.
 */
public final class CacheConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(CacheConnectionFactory.class.getName());
    private final String url;
    private final List<Connection> connections = new ArrayList<>();
    private volatile boolean closed = false;

    public CacheConnectionFactory(File databaseFile) {
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

Run:
```bash
./gradlew :CacheDb:test --tests "CacheConnectionFactoryTest"
```
Expected: PASS - all 3 tests pass

**Step 5: Commit**

```bash
git add CacheDb/src CacheDb/test
git commit -m "feat(cachedb): add CacheConnectionFactory with WAL mode"
```

---

### Task 3: Create CacheDatabase Base Class

**Files:**
- Create: `CacheDb/src/org/jphototagger/cachedb/CacheDatabase.java`
- Test: `CacheDb/test/org/jphototagger/cachedb/CacheDatabaseTest.java`

**Step 1: Write the failing test**

Create `CacheDb/test/org/jphototagger/cachedb/CacheDatabaseTest.java`:
```java
package org.jphototagger.cachedb;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class CacheDatabaseTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private TestCacheDatabase database;

    @BeforeEach
    void setUp() throws SQLException {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        database = new TestCacheDatabase(factory);
        database.createTestTable();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void getConnection_returnsConnection() throws SQLException {
        try (Connection con = database.testGetConnection()) {
            assertThat(con).isNotNull();
            assertThat(con.isClosed()).isFalse();
        }
    }

    @Test
    void setBytes_writesAndReadsBlob() throws SQLException {
        byte[] data = {1, 2, 3, 4, 5};
        database.insertBytes("test-key", data);

        byte[] result = database.selectBytes("test-key");
        assertThat(result).isEqualTo(data);
    }

    @Test
    void setBytes_nullValueWritesNull() throws SQLException {
        database.insertBytes("null-key", null);

        byte[] result = database.selectBytes("null-key");
        assertThat(result).isNull();
    }

    // Test implementation to expose protected methods
    private static class TestCacheDatabase extends CacheDatabase {
        TestCacheDatabase(CacheConnectionFactory factory) {
            super(factory);
        }

        Connection testGetConnection() throws SQLException {
            return getConnection();
        }

        void createTestTable() throws SQLException {
            try (Connection con = getConnection();
                 PreparedStatement stmt = con.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS test (key TEXT PRIMARY KEY, data BLOB)")) {
                stmt.executeUpdate();
            }
        }

        void insertBytes(String key, byte[] data) throws SQLException {
            try (Connection con = getConnection();
                 PreparedStatement stmt = con.prepareStatement(
                     "INSERT OR REPLACE INTO test (key, data) VALUES (?, ?)")) {
                stmt.setString(1, key);
                setBytes(data, stmt, 2);
                stmt.executeUpdate();
            }
        }

        byte[] selectBytes(String key) throws SQLException {
            try (Connection con = getConnection();
                 PreparedStatement stmt = con.prepareStatement(
                     "SELECT data FROM test WHERE key = ?")) {
                stmt.setString(1, key);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return getBytes(rs, 1);
                    }
                }
            }
            return null;
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :CacheDb:test --tests "CacheDatabaseTest"
```
Expected: FAIL - CacheDatabase class not found

**Step 3: Write minimal implementation**

Create `CacheDb/src/org/jphototagger/cachedb/CacheDatabase.java`:
```java
package org.jphototagger.cachedb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for SQLite cache database operations.
 */
public abstract class CacheDatabase {

    private static final Logger LOGGER = Logger.getLogger(CacheDatabase.class.getName());
    private final CacheConnectionFactory connectionFactory;

    protected CacheDatabase(CacheConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    protected Connection getConnection() throws SQLException {
        return connectionFactory.getConnection();
    }

    protected void setBytes(byte[] value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, Types.BLOB);
        } else {
            stmt.setBytes(paramIndex, value);
        }
    }

    protected byte[] getBytes(ResultSet rs, int colIndex) throws SQLException {
        byte[] value = rs.getBytes(colIndex);
        return rs.wasNull() ? null : value;
    }

    protected void setString(String value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, Types.VARCHAR);
        } else {
            stmt.setString(paramIndex, value);
        }
    }

    protected String getString(ResultSet rs, int colIndex) throws SQLException {
        String value = rs.getString(colIndex);
        return rs.wasNull() ? null : value;
    }

    protected void setLong(Long value, PreparedStatement stmt, int paramIndex) throws SQLException {
        if (value == null) {
            stmt.setNull(paramIndex, Types.BIGINT);
        } else {
            stmt.setLong(paramIndex, value);
        }
    }

    protected Long getLong(ResultSet rs, int colIndex) throws SQLException {
        long value = rs.getLong(colIndex);
        return rs.wasNull() ? null : value;
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

    public static void rollback(Connection con) {
        if (con == null) return;
        try {
            con.rollback();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error rolling back", e);
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :CacheDb:test --tests "CacheDatabaseTest"
```
Expected: PASS - all 3 tests pass

**Step 5: Commit**

```bash
git add CacheDb/src CacheDb/test
git commit -m "feat(cachedb): add CacheDatabase base class"
```

---

## Part 2: SQLite Thumbnail Cache

### Task 4: Create SQLite Thumbnail Cache Schema

**Files:**
- Create: `CacheDb/src/org/jphototagger/cachedb/SqliteThumbnailCache.java`
- Test: `CacheDb/test/org/jphototagger/cachedb/SqliteThumbnailCacheTest.java`

**Step 1: Write the failing test**

Create `CacheDb/test/org/jphototagger/cachedb/SqliteThumbnailCacheTest.java`:
```java
package org.jphototagger.cachedb;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class SqliteThumbnailCacheTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private SqliteThumbnailCache cache;
    private File imageFile;

    @BeforeEach
    void setUp() {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        cache = new SqliteThumbnailCache(factory);

        imageFile = new File(tempDir, "test.jpg");
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void constructor_createsTable() throws Exception {
        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name='thumbnails'")) {
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void existsThumbnail_returnsFalseForMissing() {
        assertThat(cache.existsThumbnail(imageFile)).isFalse();
    }

    @Test
    void insertAndFind_roundTrip() throws Exception {
        // Create test file for lastModified
        imageFile.createNewFile();

        BufferedImage thumbnail = createTestThumbnail();
        cache.insertThumbnail(thumbnail, imageFile);

        assertThat(cache.existsThumbnail(imageFile)).isTrue();

        Image result = cache.findThumbnail(imageFile);
        assertThat(result).isNotNull();
        assertThat(result.getWidth(null)).isEqualTo(100);
        assertThat(result.getHeight(null)).isEqualTo(100);
    }

    @Test
    void hasUpToDateThumbnail_returnsTrueWhenCurrent() throws Exception {
        imageFile.createNewFile();

        cache.insertThumbnail(createTestThumbnail(), imageFile);

        assertThat(cache.hasUpToDateThumbnail(imageFile)).isTrue();
    }

    @Test
    void hasUpToDateThumbnail_returnsFalseWhenStale() throws Exception {
        imageFile.createNewFile();
        long originalModified = imageFile.lastModified();

        cache.insertThumbnail(createTestThumbnail(), imageFile);

        // Simulate file modification
        imageFile.setLastModified(originalModified + 1000);

        assertThat(cache.hasUpToDateThumbnail(imageFile)).isFalse();
    }

    @Test
    void deleteThumbnail_removesEntry() throws Exception {
        imageFile.createNewFile();
        cache.insertThumbnail(createTestThumbnail(), imageFile);
        assertThat(cache.existsThumbnail(imageFile)).isTrue();

        boolean deleted = cache.deleteThumbnail(imageFile);

        assertThat(deleted).isTrue();
        assertThat(cache.existsThumbnail(imageFile)).isFalse();
    }

    @Test
    void renameThumbnail_movesEntry() throws Exception {
        imageFile.createNewFile();
        cache.insertThumbnail(createTestThumbnail(), imageFile);

        File newFile = new File(tempDir, "renamed.jpg");
        boolean renamed = cache.renameThumbnail(imageFile, newFile);

        assertThat(renamed).isTrue();
        assertThat(cache.existsThumbnail(imageFile)).isFalse();
        assertThat(cache.existsThumbnail(newFile)).isTrue();
    }

    private BufferedImage createTestThumbnail() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        return img;
    }
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :CacheDb:test --tests "SqliteThumbnailCacheTest"
```
Expected: FAIL - SqliteThumbnailCache class not found

**Step 3: Write minimal implementation**

Create `CacheDb/src/org/jphototagger/cachedb/SqliteThumbnailCache.java`:
```java
package org.jphototagger.cachedb;

import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * SQLite-backed thumbnail cache.
 * Replaces MapDB-based ThumbnailsDb.
 */
public final class SqliteThumbnailCache extends CacheDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteThumbnailCache.class.getName());

    private static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS thumbnails (
            file_path TEXT PRIMARY KEY,
            modified_time INTEGER NOT NULL,
            file_length INTEGER NOT NULL,
            thumbnail BLOB NOT NULL
        )
        """;

    private static final String SELECT_EXISTS = "SELECT 1 FROM thumbnails WHERE file_path = ?";
    private static final String SELECT_THUMBNAIL = "SELECT thumbnail FROM thumbnails WHERE file_path = ?";
    private static final String SELECT_FOR_VALIDATION = "SELECT modified_time, file_length FROM thumbnails WHERE file_path = ?";
    private static final String INSERT_THUMBNAIL = "INSERT OR REPLACE INTO thumbnails (file_path, modified_time, file_length, thumbnail) VALUES (?, ?, ?, ?)";
    private static final String DELETE_THUMBNAIL = "DELETE FROM thumbnails WHERE file_path = ?";
    private static final String SELECT_ALL_PATHS = "SELECT file_path FROM thumbnails";

    public SqliteThumbnailCache(CacheConnectionFactory connectionFactory) {
        super(connectionFactory);
        createTable();
    }

    private void createTable() {
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(CREATE_TABLE);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating thumbnails table", e);
            throw new RuntimeException("Failed to create thumbnails table", e);
        }
    }

    public boolean existsThumbnail(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_EXISTS)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking thumbnail existence", e);
            return false;
        }
    }

    public Image findThumbnail(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_THUMBNAIL)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = getBytes(rs, 1);
                    if (bytes != null) {
                        return new ImageIcon(bytes).getImage();
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding thumbnail", e);
        }
        return null;
    }

    public boolean hasUpToDateThumbnail(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_FOR_VALIDATION)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long storedModified = rs.getLong(1);
                    long storedLength = rs.getLong(2);
                    return storedModified == imageFile.lastModified()
                        && storedLength == imageFile.length();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking thumbnail freshness", e);
        }
        return false;
    }

    public void insertThumbnail(Image thumbnail, File imageFile) {
        byte[] bytes = toJpegBytes(thumbnail);
        if (bytes == null) {
            LOGGER.log(Level.WARNING, "Failed to convert thumbnail to bytes for {0}", imageFile);
            return;
        }

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_THUMBNAIL)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            stmt.setLong(2, imageFile.lastModified());
            stmt.setLong(3, imageFile.length());
            stmt.setBytes(4, bytes);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error inserting thumbnail", e);
        }
    }

    public boolean deleteThumbnail(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_THUMBNAIL)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting thumbnail", e);
            return false;
        }
    }

    public boolean renameThumbnail(File fromImageFile, File toImageFile) {
        try (Connection con = getConnection()) {
            con.setAutoCommit(false);
            try {
                // Get existing thumbnail
                byte[] bytes;
                long modifiedTime;
                long fileLength;
                try (PreparedStatement selectStmt = con.prepareStatement(
                        "SELECT thumbnail, modified_time, file_length FROM thumbnails WHERE file_path = ?")) {
                    selectStmt.setString(1, fromImageFile.getAbsolutePath());
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            return false;
                        }
                        bytes = rs.getBytes(1);
                        modifiedTime = rs.getLong(2);
                        fileLength = rs.getLong(3);
                    }
                }

                // Delete old entry
                try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_THUMBNAIL)) {
                    deleteStmt.setString(1, fromImageFile.getAbsolutePath());
                    deleteStmt.executeUpdate();
                }

                // Insert with new path
                try (PreparedStatement insertStmt = con.prepareStatement(INSERT_THUMBNAIL)) {
                    insertStmt.setString(1, toImageFile.getAbsolutePath());
                    insertStmt.setLong(2, modifiedTime);
                    insertStmt.setLong(3, fileLength);
                    insertStmt.setBytes(4, bytes);
                    insertStmt.executeUpdate();
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error renaming thumbnail", e);
            return false;
        }
    }

    public Set<String> getImageFilenames() {
        Set<String> filenames = new HashSet<>();
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ALL_PATHS);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                filenames.add(rs.getString(1));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting image filenames", e);
        }
        return filenames;
    }

    public void compact() {
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("VACUUM");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error compacting database", e);
        }
    }

    private byte[] toJpegBytes(Image image) {
        try {
            BufferedImage buffered;
            if (image instanceof BufferedImage) {
                buffered = (BufferedImage) image;
            } else {
                int w = image.getWidth(null);
                int h = image.getHeight(null);
                if (w <= 0 || h <= 0) {
                    return null;
                }
                buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = buffered.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(buffered, "jpeg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error converting image to bytes", e);
            return null;
        }
    }
}
```

**Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :CacheDb:test --tests "SqliteThumbnailCacheTest"
```
Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add CacheDb/src CacheDb/test
git commit -m "feat(cachedb): add SqliteThumbnailCache implementation"
```

---

### Task 5: Create Thumbnail Cache Provider

**Files:**
- Create: `CacheDb/src/org/jphototagger/cachedb/SqliteThumbnailsRepositoryImpl.java`
- Create: `CacheDb/src/org/jphototagger/cachedb/CacheDbInit.java`
- Test: `CacheDb/test/org/jphototagger/cachedb/SqliteThumbnailsRepositoryImplTest.java`

**Step 1: Write the failing test**

Create `CacheDb/test/org/jphototagger/cachedb/SqliteThumbnailsRepositoryImplTest.java`:
```java
package org.jphototagger.cachedb;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import org.jphototagger.domain.repository.ThumbnailsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class SqliteThumbnailsRepositoryImplTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private SqliteThumbnailsRepositoryImpl repository;
    private File imageFile;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        SqliteThumbnailCache cache = new SqliteThumbnailCache(factory);
        repository = new SqliteThumbnailsRepositoryImpl(cache);

        imageFile = new File(tempDir, "test.jpg");
        imageFile.createNewFile();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void implementsThumbnailsRepository() {
        assertThat(repository).isInstanceOf(ThumbnailsRepository.class);
    }

    @Test
    void insertAndFindThumbnail() {
        BufferedImage thumbnail = createTestThumbnail();
        repository.insertThumbnail(thumbnail, imageFile);

        Image result = repository.findThumbnail(imageFile);
        assertThat(result).isNotNull();
    }

    @Test
    void existsThumbnail() {
        assertThat(repository.existsThumbnail(imageFile)).isFalse();

        repository.insertThumbnail(createTestThumbnail(), imageFile);

        assertThat(repository.existsThumbnail(imageFile)).isTrue();
    }

    @Test
    void deleteThumbnail() {
        repository.insertThumbnail(createTestThumbnail(), imageFile);
        assertThat(repository.existsThumbnail(imageFile)).isTrue();

        repository.deleteThumbnail(imageFile);

        assertThat(repository.existsThumbnail(imageFile)).isFalse();
    }

    private BufferedImage createTestThumbnail() {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.RED);
        g.fillRect(0, 0, 100, 100);
        g.dispose();
        return img;
    }
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :CacheDb:test --tests "SqliteThumbnailsRepositoryImplTest"
```
Expected: FAIL - SqliteThumbnailsRepositoryImpl class not found

**Step 3: Write minimal implementation**

Create `CacheDb/src/org/jphototagger/cachedb/SqliteThumbnailsRepositoryImpl.java`:
```java
package org.jphototagger.cachedb;

import java.awt.Image;
import java.io.File;
import java.util.Set;
import org.jphototagger.domain.repository.ThumbnailsRepository;

/**
 * SQLite-backed implementation of ThumbnailsRepository.
 */
public final class SqliteThumbnailsRepositoryImpl implements ThumbnailsRepository {

    private final SqliteThumbnailCache cache;

    public SqliteThumbnailsRepositoryImpl(SqliteThumbnailCache cache) {
        this.cache = cache;
    }

    @Override
    public void insertThumbnail(Image thumbnail, File imageFile) {
        cache.insertThumbnail(thumbnail, imageFile);
    }

    @Override
    public Image findThumbnail(File imageFile) {
        return cache.findThumbnail(imageFile);
    }

    @Override
    public boolean existsThumbnail(File imageFile) {
        return cache.existsThumbnail(imageFile);
    }

    @Override
    public boolean hasUpToDateThumbnail(File imageFile) {
        return cache.hasUpToDateThumbnail(imageFile);
    }

    @Override
    public boolean renameThumbnail(File fromImageFile, File toImageFile) {
        return cache.renameThumbnail(fromImageFile, toImageFile);
    }

    @Override
    public boolean deleteThumbnail(File imageFile) {
        return cache.deleteThumbnail(imageFile);
    }

    @Override
    public void compact() {
        cache.compact();
    }

    @Override
    public Set<String> getImageFilenames() {
        return cache.getImageFilenames();
    }
}
```

**Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :CacheDb:test --tests "SqliteThumbnailsRepositoryImplTest"
```
Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add CacheDb/src CacheDb/test
git commit -m "feat(cachedb): add SqliteThumbnailsRepositoryImpl"
```

---

## Part 3: SQLite EXIF Cache

### Task 6: Create SQLite EXIF Cache

**Files:**
- Create: `CacheDb/src/org/jphototagger/cachedb/SqliteExifCache.java`
- Test: `CacheDb/test/org/jphototagger/cachedb/SqliteExifCacheTest.java`

**Step 1: Write the failing test**

Create `CacheDb/test/org/jphototagger/cachedb/SqliteExifCacheTest.java`:
```java
package org.jphototagger.cachedb;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.jphototagger.exif.ExifIfd;
import org.jphototagger.exif.ExifTag;
import org.jphototagger.exif.ExifTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class SqliteExifCacheTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private SqliteExifCache cache;
    private File imageFile;

    @BeforeEach
    void setUp() throws Exception {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        cache = new SqliteExifCache(factory);

        imageFile = new File(tempDir, "test.jpg");
        imageFile.createNewFile();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void constructor_createsTable() throws Exception {
        try (Connection con = factory.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name='exif_cache'")) {
            assertThat(rs.next()).isTrue();
        }
    }

    @Test
    void containsUpToDateExifTags_returnsFalseForMissing() {
        assertThat(cache.containsUpToDateExifTags(imageFile)).isFalse();
    }

    @Test
    void cacheAndRetrieve_roundTrip() {
        ExifTags tags = createSampleExifTags();
        cache.cacheExifTags(imageFile, tags);

        ExifTags result = cache.getCachedExifTags(imageFile);

        assertThat(result).isNotNull();
        assertThat(result.getExifTags()).hasSize(1);
        ExifTag makeTag = result.findExifTagByTagId(271);
        assertThat(makeTag).isNotNull();
        assertThat(makeTag.getStringValue()).isEqualTo("TestCamera");
    }

    @Test
    void containsUpToDateExifTags_returnsTrueWhenCurrent() {
        cache.cacheExifTags(imageFile, createSampleExifTags());

        assertThat(cache.containsUpToDateExifTags(imageFile)).isTrue();
    }

    @Test
    void containsUpToDateExifTags_returnsFalseWhenStale() throws Exception {
        cache.cacheExifTags(imageFile, createSampleExifTags());
        long originalModified = imageFile.lastModified();

        // Simulate file modification
        Thread.sleep(100);
        imageFile.setLastModified(originalModified + 1000);

        assertThat(cache.containsUpToDateExifTags(imageFile)).isFalse();
    }

    @Test
    void deleteCachedExifTags_removesEntry() {
        cache.cacheExifTags(imageFile, createSampleExifTags());
        assertThat(cache.containsUpToDateExifTags(imageFile)).isTrue();

        cache.deleteCachedExifTags(imageFile);

        assertThat(cache.containsUpToDateExifTags(imageFile)).isFalse();
    }

    @Test
    void renameCachedExifTags_movesEntry() {
        cache.cacheExifTags(imageFile, createSampleExifTags());

        File newFile = new File(tempDir, "renamed.jpg");
        cache.renameCachedExifTags(imageFile, newFile);

        assertThat(cache.containsUpToDateExifTags(imageFile)).isFalse();
        assertThat(cache.getCachedExifTags(newFile)).isNotNull();
    }

    @Test
    void clear_removesAllEntries() throws Exception {
        File file1 = new File(tempDir, "test1.jpg");
        File file2 = new File(tempDir, "test2.jpg");
        file1.createNewFile();
        file2.createNewFile();

        cache.cacheExifTags(file1, createSampleExifTags());
        cache.cacheExifTags(file2, createSampleExifTags());

        int deleted = cache.clear();

        assertThat(deleted).isEqualTo(2);
        assertThat(cache.getCachedExifTags(file1)).isNull();
        assertThat(cache.getCachedExifTags(file2)).isNull();
    }

    private ExifTags createSampleExifTags() {
        ExifTags tags = new ExifTags();
        tags.setLastModified(imageFile.lastModified());

        ExifTag makeTag = new ExifTag(
            271,  // tagId for Make
            2,    // ASCII type
            11,   // valueCount
            0,    // valueOffset
            "TestCamera".getBytes(),
            "TestCamera",
            18761,  // little endian
            "Make",
            ExifIfd.EXIF
        );
        tags.addExifTag(makeTag);

        return tags;
    }
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :CacheDb:test --tests "SqliteExifCacheTest"
```
Expected: FAIL - SqliteExifCache class not found

**Step 3: Write minimal implementation**

Create `CacheDb/src/org/jphototagger/cachedb/SqliteExifCache.java`:
```java
package org.jphototagger.cachedb;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jphototagger.exif.ExifTags;
import org.jphototagger.lib.xml.bind.XmlObjectExporter;
import org.jphototagger.lib.xml.bind.XmlObjectImporter;

/**
 * SQLite-backed EXIF metadata cache.
 * Replaces MapDB-based ExifCache.
 */
public final class SqliteExifCache extends CacheDatabase {

    private static final Logger LOGGER = Logger.getLogger(SqliteExifCache.class.getName());

    private static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS exif_cache (
            file_path TEXT PRIMARY KEY,
            modified_time INTEGER NOT NULL,
            exif_xml TEXT NOT NULL
        )
        """;

    private static final String SELECT_EXISTS = "SELECT modified_time FROM exif_cache WHERE file_path = ?";
    private static final String SELECT_EXIF = "SELECT exif_xml FROM exif_cache WHERE file_path = ?";
    private static final String INSERT_EXIF = "INSERT OR REPLACE INTO exif_cache (file_path, modified_time, exif_xml) VALUES (?, ?, ?)";
    private static final String DELETE_EXIF = "DELETE FROM exif_cache WHERE file_path = ?";
    private static final String SELECT_COUNT = "SELECT COUNT(*) FROM exif_cache";
    private static final String DELETE_ALL = "DELETE FROM exif_cache";

    public SqliteExifCache(CacheConnectionFactory connectionFactory) {
        super(connectionFactory);
        createTable();
    }

    private void createTable() {
        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute(CREATE_TABLE);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating exif_cache table", e);
            throw new RuntimeException("Failed to create exif_cache table", e);
        }
    }

    public synchronized void cacheExifTags(File imageFile, ExifTags exifTags) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        if (exifTags == null) {
            throw new NullPointerException("exifTags == null");
        }

        exifTags.setLastModified(imageFile.lastModified());

        try {
            String xml = XmlObjectExporter.marshal(exifTags);

            try (Connection con = getConnection();
                 PreparedStatement stmt = con.prepareStatement(INSERT_EXIF)) {
                stmt.setString(1, imageFile.getAbsolutePath());
                stmt.setLong(2, imageFile.lastModified());
                stmt.setString(3, xml);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error caching EXIF tags", e);
        }
    }

    public synchronized boolean containsUpToDateExifTags(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_EXISTS)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long storedModified = rs.getLong(1);
                    return storedModified == imageFile.lastModified();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking EXIF cache", e);
        }
        return false;
    }

    public synchronized ExifTags getCachedExifTags(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }

        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_EXIF)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String xml = rs.getString(1);
                    return XmlObjectImporter.unmarshal(xml, ExifTags.class);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting cached EXIF tags", e);
        }
        return null;
    }

    public void deleteCachedExifTags(File imageFile) {
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXIF)) {
            stmt.setString(1, imageFile.getAbsolutePath());
            stmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting cached EXIF tags", e);
        }
    }

    public synchronized void renameCachedExifTags(File oldImageFile, File newImageFile) {
        ExifTags tags = getCachedExifTags(oldImageFile);
        if (tags != null) {
            deleteCachedExifTags(oldImageFile);
            cacheExifTags(newImageFile, tags);
        }
    }

    public synchronized int clear() {
        int count = 0;
        try (Connection con = getConnection()) {
            try (PreparedStatement countStmt = con.prepareStatement(SELECT_COUNT);
                 ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }

            try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_ALL)) {
                deleteStmt.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error clearing EXIF cache", e);
            return 0;
        }
        return count;
    }
}
```

**Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :CacheDb:test --tests "SqliteExifCacheTest"
```
Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add CacheDb/src CacheDb/test
git commit -m "feat(cachedb): add SqliteExifCache implementation"
```

---

### Task 7: Create EXIF Cache Provider

**Files:**
- Create: `CacheDb/src/org/jphototagger/cachedb/SqliteExifCacheProviderImpl.java`
- Test: `CacheDb/test/org/jphototagger/cachedb/SqliteExifCacheProviderImplTest.java`

**Step 1: Write the failing test**

Create `CacheDb/test/org/jphototagger/cachedb/SqliteExifCacheProviderImplTest.java`:
```java
package org.jphototagger.cachedb;

import java.io.File;
import org.jphototagger.domain.metadata.exif.ExifCacheProvider;
import org.jphototagger.exif.ExifIfd;
import org.jphototagger.exif.ExifTag;
import org.jphototagger.exif.ExifTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class SqliteExifCacheProviderImplTest {

    @TempDir
    File tempDir;

    private CacheConnectionFactory factory;
    private SqliteExifCache cache;
    private SqliteExifCacheProviderImpl provider;

    @BeforeEach
    void setUp() {
        File dbFile = new File(tempDir, "cache.db");
        factory = new CacheConnectionFactory(dbFile);
        cache = new SqliteExifCache(factory);
        provider = new SqliteExifCacheProviderImpl(cache);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void implementsExifCacheProvider() {
        assertThat(provider).isInstanceOf(ExifCacheProvider.class);
    }

    @Test
    void clear_returnsDeletedCount() throws Exception {
        File file = new File(tempDir, "test.jpg");
        file.createNewFile();

        ExifTags tags = new ExifTags();
        tags.setLastModified(file.lastModified());
        ExifTag tag = new ExifTag(271, 2, 5, 0, "Test".getBytes(), "Test", 18761, "Make", ExifIfd.EXIF);
        tags.addExifTag(tag);
        cache.cacheExifTags(file, tags);

        int cleared = provider.clear();

        assertThat(cleared).isEqualTo(1);
    }
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :CacheDb:test --tests "SqliteExifCacheProviderImplTest"
```
Expected: FAIL - SqliteExifCacheProviderImpl class not found

**Step 3: Write minimal implementation**

Create `CacheDb/src/org/jphototagger/cachedb/SqliteExifCacheProviderImpl.java`:
```java
package org.jphototagger.cachedb;

import org.jphototagger.domain.metadata.exif.ExifCacheProvider;

/**
 * SQLite-backed implementation of ExifCacheProvider.
 */
public final class SqliteExifCacheProviderImpl implements ExifCacheProvider {

    private final SqliteExifCache cache;

    public SqliteExifCacheProviderImpl(SqliteExifCache cache) {
        this.cache = cache;
    }

    @Override
    public void init() {
        // SQLite cache is initialized in constructor
    }

    @Override
    public int clear() {
        return cache.clear();
    }

    public SqliteExifCache getCache() {
        return cache;
    }
}
```

**Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :CacheDb:test --tests "SqliteExifCacheProviderImplTest"
```
Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add CacheDb/src CacheDb/test
git commit -m "feat(cachedb): add SqliteExifCacheProviderImpl"
```

---

## Part 4: Cache Initialization and Integration

### Task 8: Create Cache Database Initialization

**Files:**
- Create: `CacheDb/src/org/jphototagger/cachedb/CacheDbInit.java`
- Test: `CacheDb/test/org/jphototagger/cachedb/CacheDbInitTest.java`

**Step 1: Write the failing test**

Create `CacheDb/test/org/jphototagger/cachedb/CacheDbInitTest.java`:
```java
package org.jphototagger.cachedb;

import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;

class CacheDbInitTest {

    @TempDir
    File tempDir;

    private CacheDbInit cacheDbInit;

    @BeforeEach
    void setUp() {
        cacheDbInit = CacheDbInit.createForDirectory(tempDir);
    }

    @AfterEach
    void tearDown() {
        cacheDbInit.close();
    }

    @Test
    void createForDirectory_createsCacheDbFile() {
        File expectedDbFile = new File(tempDir, "cache.db");
        assertThat(expectedDbFile).exists();
    }

    @Test
    void getThumbnailCache_returnsCache() {
        SqliteThumbnailCache cache = cacheDbInit.getThumbnailCache();
        assertThat(cache).isNotNull();
    }

    @Test
    void getExifCache_returnsCache() {
        SqliteExifCache cache = cacheDbInit.getExifCache();
        assertThat(cache).isNotNull();
    }

    @Test
    void getCacheDbFile_returnsCorrectFile() {
        File cacheFile = cacheDbInit.getCacheDbFile();
        assertThat(cacheFile.getName()).isEqualTo("cache.db");
        assertThat(cacheFile.getParentFile()).isEqualTo(tempDir);
    }
}
```

**Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :CacheDb:test --tests "CacheDbInitTest"
```
Expected: FAIL - CacheDbInit class not found

**Step 3: Write minimal implementation**

Create `CacheDb/src/org/jphototagger/cachedb/CacheDbInit.java`:
```java
package org.jphototagger.cachedb;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initializes the SQLite cache database.
 * Creates a single cache.db file containing both thumbnail and EXIF caches.
 */
public final class CacheDbInit {

    private static final Logger LOGGER = Logger.getLogger(CacheDbInit.class.getName());
    private static final String CACHE_DB_FILENAME = "cache.db";

    private final File cacheDbFile;
    private final CacheConnectionFactory connectionFactory;
    private final SqliteThumbnailCache thumbnailCache;
    private final SqliteExifCache exifCache;

    private CacheDbInit(File cacheDirectory) {
        this.cacheDbFile = new File(cacheDirectory, CACHE_DB_FILENAME);
        LOGGER.log(Level.INFO, "Initializing SQLite cache database: {0}", cacheDbFile);

        this.connectionFactory = new CacheConnectionFactory(cacheDbFile);
        this.thumbnailCache = new SqliteThumbnailCache(connectionFactory);
        this.exifCache = new SqliteExifCache(connectionFactory);
    }

    /**
     * Creates cache database in the specified directory.
     *
     * @param cacheDirectory directory to store cache.db
     * @return initialized cache database
     */
    public static CacheDbInit createForDirectory(File cacheDirectory) {
        if (cacheDirectory == null) {
            throw new NullPointerException("cacheDirectory == null");
        }
        if (!cacheDirectory.isDirectory()) {
            if (!cacheDirectory.mkdirs()) {
                throw new RuntimeException("Failed to create cache directory: " + cacheDirectory);
            }
        }
        return new CacheDbInit(cacheDirectory);
    }

    public SqliteThumbnailCache getThumbnailCache() {
        return thumbnailCache;
    }

    public SqliteExifCache getExifCache() {
        return exifCache;
    }

    public File getCacheDbFile() {
        return cacheDbFile;
    }

    public void close() {
        connectionFactory.close();
    }
}
```

**Step 4: Run test to verify it passes**

Run:
```bash
./gradlew :CacheDb:test --tests "CacheDbInitTest"
```
Expected: PASS - all tests pass

**Step 5: Commit**

```bash
git add CacheDb/src CacheDb/test
git commit -m "feat(cachedb): add CacheDbInit for database initialization"
```

---

### Task 9: Update Program Module to Use SQLite Cache

**Files:**
- Modify: `Program/build.gradle.kts` (add CacheDb dependency)
- Modify: `Program/src/org/jphototagger/program/module/thumbnails/cache/ThumbnailsDb.java` (switch to SQLite)
- Modify: `Exif/src/org/jphototagger/exif/cache/ExifCache.java` (switch to SQLite)

**Step 1: Verify current tests pass**

Run:
```bash
./gradlew :Program:test
```
Expected: PASS - current tests pass (baseline)

**Step 2: Add CacheDb dependency to Program**

Modify `Program/build.gradle.kts`, add to dependencies:
```kotlin
implementation(project(":CacheDb"))
```

Remove from dependencies:
```kotlin
implementation(files("../Libraries/mapdb.jar"))
```

**Step 3: Update ThumbnailsDb to use SQLite**

Replace contents of `Program/src/org/jphototagger/program/module/thumbnails/cache/ThumbnailsDb.java`:
```java
package org.jphototagger.program.module.thumbnails.cache;

import java.awt.Image;
import java.io.File;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jphototagger.api.storage.CacheDirectoryProvider;
import org.jphototagger.cachedb.CacheDbInit;
import org.jphototagger.cachedb.SqliteThumbnailCache;
import org.openide.util.Lookup;

/**
 * SQLite-backed thumbnail database.
 * Delegates to SqliteThumbnailCache.
 *
 * @author Elmar Baumann
 */
public final class ThumbnailsDb {

    private static final Logger LOGGER = Logger.getLogger(ThumbnailsDb.class.getName());
    private static final CacheDbInit CACHE_DB;
    private static final SqliteThumbnailCache THUMBNAILS;

    static {
        CacheDirectoryProvider provider = Lookup.getDefault().lookup(CacheDirectoryProvider.class);
        File cacheDirectory = provider.getCacheDirectory("ThumbnailCache");
        LOGGER.log(Level.INFO, "Opening SQLite thumbnail cache in ''{0}''", cacheDirectory);
        CACHE_DB = CacheDbInit.createForDirectory(cacheDirectory);
        THUMBNAILS = CACHE_DB.getThumbnailCache();
    }

    static boolean existsThumbnail(File imageFile) {
        return THUMBNAILS.existsThumbnail(imageFile);
    }

    static Image findThumbnail(File imageFile) {
        return THUMBNAILS.findThumbnail(imageFile);
    }

    static boolean deleteThumbnail(File imageFile) {
        return THUMBNAILS.deleteThumbnail(imageFile);
    }

    static void insertThumbnail(Image thumbnail, File imageFile) {
        THUMBNAILS.insertThumbnail(thumbnail, imageFile);
    }

    static boolean hasUpToDateThumbnail(File imageFile) {
        return THUMBNAILS.hasUpToDateThumbnail(imageFile);
    }

    static boolean renameThumbnail(File fromImageFile, File toImageFile) {
        return THUMBNAILS.renameThumbnail(fromImageFile, toImageFile);
    }

    static Set<String> getImageFilenames() {
        return THUMBNAILS.getImageFilenames();
    }

    static void compact() {
        THUMBNAILS.compact();
    }

    private ThumbnailsDb() {
    }
}
```

**Step 4: Run tests to verify they pass**

Run:
```bash
./gradlew :Program:test
```
Expected: PASS

**Step 5: Commit**

```bash
git add Program/build.gradle.kts Program/src
git commit -m "feat(program): switch ThumbnailsDb to SQLite backend"
```

---

### Task 10: Update Exif Module to Use SQLite Cache

**Files:**
- Modify: `Exif/build.gradle.kts` (add CacheDb dependency, remove mapdb)
- Modify: `Exif/src/org/jphototagger/exif/cache/ExifCache.java` (switch to SQLite)

**Step 1: Update Exif dependencies**

Modify `Exif/build.gradle.kts`:

Add to dependencies:
```kotlin
implementation(project(":CacheDb"))
```

Remove from dependencies:
```kotlin
api(files("../Libraries/mapdb.jar"))
```

**Step 2: Update ExifCache to use SQLite**

Replace contents of `Exif/src/org/jphototagger/exif/cache/ExifCache.java`:
```java
package org.jphototagger.exif.cache;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.jphototagger.api.storage.CacheDirectoryProvider;
import org.jphototagger.cachedb.CacheDbInit;
import org.jphototagger.cachedb.SqliteExifCache;
import org.jphototagger.domain.metadata.exif.event.ExifCacheClearedEvent;
import org.jphototagger.domain.metadata.exif.event.ExifCacheFileDeletedEvent;
import org.jphototagger.domain.repository.event.imagefiles.ImageFileDeletedEvent;
import org.jphototagger.domain.repository.event.imagefiles.ImageFileMovedEvent;
import org.jphototagger.exif.ExifTags;
import org.openide.util.Lookup;

/**
 * SQLite-backed EXIF cache.
 * Delegates to SqliteExifCache.
 *
 * @author Elmar Baumann
 */
public final class ExifCache {

    public static final ExifCache INSTANCE = new ExifCache();
    private static final Logger LOGGER = Logger.getLogger(ExifCache.class.getName());
    private final File cacheDir;
    private final SqliteExifCache sqliteCache;

    private ExifCache() {
        CacheDirectoryProvider provider = Lookup.getDefault().lookup(CacheDirectoryProvider.class);
        cacheDir = provider.getCacheDirectory("ExifCache");
        LOGGER.log(Level.INFO, "Opening SQLite EXIF cache in ''{0}''", cacheDir);
        CacheDbInit cacheDb = CacheDbInit.createForDirectory(cacheDir);
        sqliteCache = cacheDb.getExifCache();
    }

    public synchronized void cacheExifTags(File imageFile, ExifTags exifTags) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        if (exifTags == null) {
            throw new NullPointerException("exifTags == null");
        }
        LOGGER.log(Level.FINEST, "Caching EXIF metadata of image file ''{0}''", imageFile);
        sqliteCache.cacheExifTags(imageFile, exifTags);
    }

    public synchronized boolean containsUpToDateExifTags(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        return sqliteCache.containsUpToDateExifTags(imageFile);
    }

    public synchronized ExifTags getCachedExifTags(File imageFile) {
        if (imageFile == null) {
            throw new NullPointerException("imageFile == null");
        }
        LOGGER.log(Level.FINEST, "Reading cached EXIF metadata of image file ''{0}''", imageFile);
        return sqliteCache.getCachedExifTags(imageFile);
    }

    private void deleteCachedExifTags(File imageFile) {
        sqliteCache.deleteCachedExifTags(imageFile);
        LOGGER.log(Level.FINEST, "Deleted cached EXIF metadata of image file ''{0}''", imageFile);
        EventBus.publish(new ExifCacheFileDeletedEvent(this, imageFile));
    }

    private synchronized void renameCachedExifTags(File oldImageFile, File newImageFile) {
        sqliteCache.renameCachedExifTags(oldImageFile, newImageFile);
        LOGGER.log(Level.FINEST, "Renamed cached EXIF metadata from ''{0}'' to ''{1}''",
                new Object[]{oldImageFile, newImageFile});
    }

    int clear() {
        LOGGER.log(Level.INFO, "Deleting all cached EXIF metadata");
        int count = sqliteCache.clear();
        EventBus.publish(new ExifCacheClearedEvent(this, count));
        return count;
    }

    @EventSubscriber(eventClass = ImageFileMovedEvent.class)
    public void imageFileMoved(ImageFileMovedEvent event) {
        File oldImageFile = event.getOldImageFile();
        File newImageFile = event.getNewImageFile();
        renameCachedExifTags(oldImageFile, newImageFile);
    }

    @EventSubscriber(eventClass = ImageFileDeletedEvent.class)
    public void imageFileRemoved(ImageFileDeletedEvent event) {
        File deletedImageFile = event.getImageFile();
        deleteCachedExifTags(deletedImageFile);
    }

    void init() {
        AnnotationProcessor.process(this);
    }

    File getCacheDir() {
        return cacheDir;
    }
}
```

**Step 3: Run tests**

Run:
```bash
./gradlew :Exif:test
```
Expected: PASS

**Step 4: Commit**

```bash
git add Exif/build.gradle.kts Exif/src
git commit -m "feat(exif): switch ExifCache to SQLite backend"
```

---

### Task 11: Remove MapDB Dependency from Project

**Files:**
- Modify: `build.gradle.kts` (root - if MapDB is there)
- Delete: `Libraries/mapdb.jar` (if applicable, or document removal)
- Modify: `gradle/libs.versions.toml` (remove mapdb if present)

**Step 1: Search for remaining MapDB references**

Run:
```bash
grep -r "mapdb" --include="*.kts" --include="*.gradle" .
grep -r "MapDB\|mapdb" --include="*.java" .
```
Expected: No more MapDB references in production code

**Step 2: Remove mapdb.jar from any remaining build files**

Check and update any build.gradle.kts files that still reference mapdb.jar.

**Step 3: Run full build to verify**

Run:
```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add -A
git commit -m "chore: remove MapDB dependency"
```

---

## Part 5: Benchmarks and Verification

### Task 12: Update Benchmark Test Harnesses

**Files:**
- Modify: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheTestHarness.java`
- Modify: `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheTestHarness.java`
- Modify: `Benchmarks/build.gradle.kts`

**Step 1: Update Benchmarks build.gradle.kts**

Add to dependencies:
```kotlin
implementation(project(":CacheDb"))
```

**Step 2: Update ThumbnailCacheTestHarness to use SQLite**

Replace `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ThumbnailCacheTestHarness.java`:
```java
package org.jphototagger.benchmarks;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import org.jphototagger.cachedb.CacheConnectionFactory;
import org.jphototagger.cachedb.SqliteThumbnailCache;

/**
 * Test harness for thumbnail cache benchmarking using SQLite backend.
 */
public final class ThumbnailCacheTestHarness {

    private final File tempDir;
    private final CacheConnectionFactory factory;
    private final SqliteThumbnailCache cache;
    private File[] storedFiles;

    private ThumbnailCacheTestHarness(File tempDir) {
        this.tempDir = tempDir;
        File dbFile = new File(tempDir, "benchmark-cache.db");
        this.factory = new CacheConnectionFactory(dbFile);
        this.cache = new SqliteThumbnailCache(factory);
    }

    public static ThumbnailCacheTestHarness createEmpty() {
        try {
            File tempDir = Files.createTempDirectory("thumbnail-benchmark").toFile();
            return new ThumbnailCacheTestHarness(tempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ThumbnailCacheTestHarness createWithSampleData(int count) {
        ThumbnailCacheTestHarness harness = createEmpty();
        harness.storedFiles = new File[count];

        for (int i = 0; i < count; i++) {
            File file = new File("/photos/image_" + i + ".jpg");
            harness.storedFiles[i] = file;

            BufferedImage thumbnail = createSampleThumbnail(i);
            harness.cache.insertThumbnail(thumbnail, file);
        }

        return harness;
    }

    private static BufferedImage createSampleThumbnail(int seed) {
        BufferedImage img = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(seed % 256, (seed * 7) % 256, (seed * 13) % 256));
        g.fillRect(0, 0, 150, 150);
        g.dispose();
        return img;
    }

    public File[] getStoredFiles() {
        return storedFiles;
    }

    public boolean existsThumbnail(File imageFile) {
        return cache.existsThumbnail(imageFile);
    }

    public Image findThumbnail(File imageFile) {
        return cache.findThumbnail(imageFile);
    }

    public boolean hasUpToDateThumbnail(File imageFile) {
        return cache.hasUpToDateThumbnail(imageFile);
    }

    public void insertThumbnail(Image thumbnail, File imageFile) {
        cache.insertThumbnail(thumbnail, imageFile);
    }

    public void clear() {
        // No-op for SQLite - drop table would be too expensive per benchmark
    }

    public void close() {
        factory.close();
        deleteRecursively(tempDir);
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
```

**Step 3: Update ExifCacheTestHarness to use SQLite**

Replace `Benchmarks/src/jmh/java/org/jphototagger/benchmarks/ExifCacheTestHarness.java`:
```java
package org.jphototagger.benchmarks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.jphototagger.cachedb.CacheConnectionFactory;
import org.jphototagger.cachedb.SqliteExifCache;
import org.jphototagger.exif.ExifIfd;
import org.jphototagger.exif.ExifTag;
import org.jphototagger.exif.ExifTags;

/**
 * Test harness for EXIF cache benchmarking using SQLite backend.
 */
public final class ExifCacheTestHarness {

    private final File tempDir;
    private final CacheConnectionFactory factory;
    private final SqliteExifCache cache;

    private ExifCacheTestHarness(File tempDir) {
        this.tempDir = tempDir;
        File dbFile = new File(tempDir, "benchmark-exif-cache.db");
        this.factory = new CacheConnectionFactory(dbFile);
        this.cache = new SqliteExifCache(factory);
    }

    public static ExifCacheTestHarness create() {
        try {
            File tempDir = Files.createTempDirectory("exif-benchmark").toFile();
            return new ExifCacheTestHarness(tempDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ExifTags[] generateSampleTags(int count) {
        ExifTags[] tags = new ExifTags[count];
        for (int i = 0; i < count; i++) {
            ExifTags exifTags = new ExifTags();
            exifTags.setLastModified(System.currentTimeMillis() - i * 1000);

            String makeValue = "Camera" + (i % 5);
            ExifTag makeTag = new ExifTag(
                271, 2, makeValue.length() + 1, 0,
                makeValue.getBytes(), makeValue, 18761, "Make", ExifIfd.EXIF
            );
            exifTags.addExifTag(makeTag);

            String modelValue = "Model" + i;
            ExifTag modelTag = new ExifTag(
                272, 2, modelValue.length() + 1, 0,
                modelValue.getBytes(), modelValue, 18761, "Model", ExifIfd.EXIF
            );
            exifTags.addExifTag(modelTag);

            tags[i] = exifTags;
        }
        return tags;
    }

    public void cacheExifTags(File imageFile, ExifTags exifTags) {
        cache.cacheExifTags(imageFile, exifTags);
    }

    public ExifTags getCachedExifTags(File imageFile) {
        return cache.getCachedExifTags(imageFile);
    }

    public boolean containsUpToDateExifTags(File imageFile) {
        return cache.containsUpToDateExifTags(imageFile);
    }

    public void clear() {
        cache.clear();
    }

    public void close() {
        factory.close();
        deleteRecursively(tempDir);
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
```

**Step 4: Run benchmarks**

Run:
```bash
./gradlew :Benchmarks:jmh -Pjmh.includes="ThumbnailCacheBenchmark|ExifCacheBenchmark"
```
Expected: Benchmarks run successfully

**Step 5: Commit**

```bash
git add Benchmarks
git commit -m "feat(benchmarks): update test harnesses to use SQLite backend"
```

---

### Task 13: Run Post-Phase Benchmarks and Compare

**Files:**
- Create: `docs/benchmarks/post-phase5-cache.json`
- Create: `docs/benchmarks/phase5-comparison.md`

**Step 1: Run post-implementation benchmarks**

Run:
```bash
./gradlew :Benchmarks:jmh -Pjmh.includes="ThumbnailCacheBenchmark|ExifCacheBenchmark"
cp Benchmarks/build/results/jmh/results.json docs/benchmarks/post-phase5-cache.json
```

**Step 2: Create comparison document**

Create `docs/benchmarks/phase5-comparison.md`:
```markdown
# Phase 5: SQLite Cache Performance Comparison

## Methodology

Benchmarks run using JMH with:
- Warmup: 2 iterations, 1 second each
- Measurement: 5 iterations, 1 second each
- 1 fork

## Results

### Thumbnail Cache

| Benchmark | Pre-Phase5 (MapDB) | Post-Phase5 (SQLite) | Change |
|-----------|-------------------|---------------------|--------|
| cacheHit_single | TBD μs | TBD μs | TBD% |
| cacheHit_concurrent | TBD μs | TBD μs | TBD% |
| cacheExists_single | TBD μs | TBD μs | TBD% |
| cacheUpToDate_single | TBD μs | TBD μs | TBD% |

### EXIF Cache

| Benchmark | Pre-Phase5 (MapDB) | Post-Phase5 (SQLite) | Change |
|-----------|-------------------|---------------------|--------|
| exifCache_read | TBD μs | TBD μs | TBD% |
| exifCache_write | TBD μs | TBD μs | TBD% |
| exifCache_containsUpToDate | TBD μs | TBD μs | TBD% |
| exifCache_read_concurrent | TBD μs | TBD μs | TBD% |

## Conclusion

[Fill in after running benchmarks]
```

**Step 3: Commit**

```bash
git add docs/benchmarks
git commit -m "docs: add phase5 benchmark comparison"
```

---

### Task 14: Integration Test - Full Application

**Step 1: Build and run application**

Run:
```bash
./gradlew :Program:run
```
Expected: Application launches successfully

**Step 2: Manual testing checklist**

- [ ] Browse a folder with images - thumbnails load correctly
- [ ] View EXIF data for an image
- [ ] Close and reopen app - thumbnails load from cache
- [ ] Modify an image file - thumbnail regenerates
- [ ] Use maintenance panel to clear caches

**Step 3: Verify cache file exists**

Run:
```bash
ls -la ~/.jphototagger/cache/ThumbnailCache/
ls -la ~/.jphototagger/cache/ExifCache/
```
Expected: cache.db files exist in both directories

**Step 4: Final commit**

```bash
git add -A
git commit -m "feat(phase5): complete SQLite cache migration"
```

---

## Post-Phase Checklist

- [ ] All CacheDb module tests pass
- [ ] Program module tests pass
- [ ] Exif module tests pass
- [ ] Benchmarks run successfully
- [ ] Application launches and caches work
- [ ] MapDB dependency removed
- [ ] Performance is equal or better than MapDB baseline

## Rollback Plan

If issues are discovered:

1. Revert the Program/build.gradle.kts changes to use MapDB
2. Revert ThumbnailsDb.java to MapDB implementation
3. Revert ExifCache.java to MapDB implementation
4. Keep CacheDb module for future use

The CacheDb module is additive and doesn't break existing code until explicitly enabled.
