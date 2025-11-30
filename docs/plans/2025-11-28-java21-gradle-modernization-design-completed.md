# JPhotoTagger Modernization Design

**Date:** 2025-11-28
**Status:** Approved
**Scope:** Java 7 → Java 21, NetBeans Ant → Gradle, HSQLDB/MapDB → SQLite

---

## Phase 1 Completion Summary

**Completed:** 2025-11-28
**Status:** ✅ COMPLETE

### Deliverables Achieved

- [x] All 38 modules building with Gradle (34 original + TestSupport, Benchmarks, DeveloperSupport modules)
- [x] `./gradlew build` works
- [x] `./gradlew :Program:run` launches the app
- [x] GitHub Actions workflow passing (`.github/workflows/build.yml`)

### Implementation Details

| Component | Status | Notes |
|-----------|--------|-------|
| Gradle Wrapper | ✅ | Gradle 8.5 |
| Root `build.gradle.kts` | ✅ | Shared configuration for all subprojects |
| `settings.gradle.kts` | ✅ | 38 subproject definitions with proper naming |
| Version Catalog | ✅ | `gradle/libs.versions.toml` for centralized dependency management |
| Module Build Files | ✅ | 38 `build.gradle.kts` files created |
| GitHub Actions CI | ✅ | Build on push/PR to master |
| `.gitignore` | ✅ | Updated for Gradle artifacts |

### Module Structure

```
jphototagger/
├── settings.gradle.kts          # 38 subproject definitions
├── build.gradle.kts             # Shared config (Java 21, UTF-8, test deps)
├── gradle/
│   ├── wrapper/                 # Gradle 8.5 wrapper
│   └── libs.versions.toml       # Version catalog
├── API/build.gradle.kts
├── Benchmarks/build.gradle.kts  # JMH performance benchmarks
├── Domain/build.gradle.kts
├── Exif/build.gradle.kts
├── ExportersImporters/JPhotoTaggerExportersImporters/build.gradle.kts
├── ExternalThumbnailCreationCommands/DefaultExternalThumbnailCreationCommands/build.gradle.kts
├── Image/build.gradle.kts
├── Iptc/build.gradle.kts
├── KML/build.gradle.kts
├── Lib/build.gradle.kts
├── Localization/build.gradle.kts
├── LookAndFeels/build.gradle.kts
├── Modules/*/build.gradle.kts   # 13 module plugins
├── Plugins/*/build.gradle.kts   # 3 plugins
├── Program/build.gradle.kts     # Main application
├── Repositories/HSQLDB/build.gradle.kts
├── Resources/build.gradle.kts
├── TestSupport/build.gradle.kts # Test utilities
├── UserServices/build.gradle.kts
└── XMP/build.gradle.kts
```

### Key Decisions Made

1. **Kotlin DSL** (`.kts`) - Type safety and IDE support
2. **Version catalog** - Centralized dependency management in `libs.versions.toml`
3. **Preserved module structure** - Minimal disruption to existing codebase
4. **Java 21** - Upgraded directly to Java 21 (skipped intermediate Java 7 step)
5. **Local JARs preserved** - Non-Maven dependencies kept in `Libraries/Jars/`

### Verification Commands

```bash
# Build all modules
./gradlew build

# Run the application
./gradlew :Program:run

# Run tests
./gradlew test

# Check Gradle version
./gradlew --version
```

### Files Created/Modified

- `gradlew`, `gradlew.bat` - Gradle wrapper scripts
- `gradle/wrapper/gradle-wrapper.properties` - Wrapper config
- `gradle/wrapper/gradle-wrapper.jar` - Wrapper JAR
- `gradle/libs.versions.toml` - Version catalog
- `settings.gradle.kts` - Project settings
- `build.gradle.kts` - Root build file
- 37 module `build.gradle.kts` files
- `.github/workflows/build.yml` - CI workflow
- `.gitignore` - Updated for Gradle

### NetBeans Files

NetBeans Ant build files (`nbproject/`, `build.xml`) were preserved for reference but are no longer used.

---

## Overview

Modernize JPhotoTagger from Java 7 + NetBeans Ant to Java 21 + Gradle, consolidating all data storage on SQLite. This addresses security (Java 7 EOL), performance, and developer experience concerns.

### Current State

- **Build:** 34 NetBeans Ant modules
- **Runtime:** Java 7
- **Database:** HSQLDB 1.8.0.10 (main), MapDB 0.9.9-SNAPSHOT (caches)
- **UI:** Swing + SwingX 1.6.2
- **Tests:** 33 test files, low coverage

### Target State

- **Build:** Gradle with Kotlin DSL
- **Runtime:** Java 21
- **Database:** SQLite (unified - main DB and caches)
- **UI:** Swing + FlatLaf (modern look and feel)
- **Tests:** Comprehensive coverage with performance benchmarks

## Phase 1: Gradle + CI Infrastructure

**Goal:** Replace NetBeans Ant with Gradle, establish CI pipeline.

**Status:** ✅ COMPLETE (see summary above)

### Gradle Structure

```
jphototagger/
├── settings.gradle.kts          # 34 subproject definitions
├── build.gradle.kts              # Shared config
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── API/build.gradle.kts
├── Domain/build.gradle.kts
├── Lib/build.gradle.kts
├── Program/build.gradle.kts      # Main application
└── ... (30 more modules)
```

### Key Decisions

- **Kotlin DSL** (`.kts`) for type safety and IDE support
- **Version catalog** for centralized dependency management
- **Stay on Java 7** initially - only change the build system
- **Preserve module structure** - minimize disruption

### CI Pipeline (GitHub Actions)

- Build on every push/PR
- Run existing tests
- Cache Gradle dependencies

### Deliverables

- [x] All 34 modules building with Gradle
- [x] `./gradlew build` works
- [x] `./gradlew run` launches the app
- [x] GitHub Actions workflow passing

## Phase 2: Testing Foundation

**Goal:** Build comprehensive test infrastructure before risky changes.

### Testing Stack

| Library | Purpose |
|---------|---------|
| JUnit 5 | Test framework (upgrade from JUnit 4) |
| AssertJ | Fluent assertions |
| Mockito | Mocking |
| JMH | Performance benchmarks |

### Test Categories

| Category | Focus | Priority |
|----------|-------|----------|
| Database layer | Repository classes, SQL queries | High |
| File operations | Image reading, metadata extraction | High |
| Cache layer | Thumbnail/EXIF cache operations | High |
| XML binding | JAXB serialization | Medium |
| UI utilities | Non-visual helper classes | Medium |
| Performance benchmarks | Baseline measurements | High |

### Performance Benchmarks

| Benchmark | What it measures |
|-----------|------------------|
| Startup time | Time from `main()` to UI visible |
| Folder load | Time to load thumbnails for 100/500/1000 images |
| Search query | Time for keyword/metadata searches |
| Cache hit | Thumbnail retrieval from warm cache |
| Cache miss | Thumbnail generation from source image |

### Strategy

1. Characterization tests first - capture current behavior
2. Focus on code we're about to modify (DB, caches, JAXB)
3. Establish baseline performance measurements

### Deliverables

- [ ] JUnit 5 infrastructure
- [ ] Tests for database repository layer
- [ ] Tests for cache read/write operations
- [ ] Tests for JAXB serialization/deserialization
- [ ] JMH benchmark suite with baseline measurements
- [ ] CI running all tests with coverage reporting

## Phase 3: Java 21 Upgrade + UI Compatibility

**Goal:** Upgrade to Java 21, handle library migrations, resolve UI compatibility.

### Step 3a: Core Java 21 Changes

| Change | Files | Approach |
|--------|-------|----------|
| Source/target → 21 | `build.gradle.kts` | Configuration change |
| `javax.xml.bind` → `jakarta.xml.bind` | 31 files | Find/replace + add dependency |
| `SystemUtil.getJavaVersion()` | 1 file | Fix for Java 9+ version format |
| Remove Lucene | ~3 files | Replace with simple string search |

### Step 3b: SwingX Compatibility Test

Before any UI changes:

1. Build with Java 21
2. Run the application
3. Test all major UI components (lists, trees, panels)
4. Document any visual glitches or crashes

### Step 3c: FlatLaf Integration

If SwingX works on Java 21:

- Add `com.formdev:flatlaf` dependency
- Set look-and-feel at startup
- Test appearance across all dialogs

### Step 3d: SwingX Replacement

Only if SwingX is broken on Java 21:

| SwingX Component | Standard Replacement |
|------------------|---------------------|
| `JXList` | `JList` with custom renderer |
| `JXTree` | `JTree` with custom renderer |
| `JXLabel` | `JLabel` |

### Deliverables

- [ ] App running on Java 21
- [ ] JAXB migrated to Jakarta XML Binding
- [ ] Lucene removed, simple string search in place
- [ ] SwingX compatibility documented
- [ ] FlatLaf integrated (or SwingX replaced if broken)

## Phase 4: SQLite Migration (Main Database)

**Goal:** Replace HSQLDB with SQLite for the main application database.

### Components

1. **SQLite Repository Layer**
   - Add `org.xerial:sqlite-jdbc` dependency
   - New repository implementations with SQLite-compatible SQL

2. **Schema Adaptation**
   - `IDENTITY` → `INTEGER PRIMARY KEY AUTOINCREMENT`
   - `LONGVARCHAR` → `TEXT`
   - Create clean SQLite schema

3. **Connection Handling**
   - Replace custom `ConnectionPool.java` with direct connections
   - Enable WAL mode for concurrent read performance

### Migration Tool

Separate utility for users to migrate existing data:

- Standalone JAR
- Reads HSQLDB database
- Writes to new SQLite database
- Progress reporting
- Validation step (compare record counts)

**User workflow:** Try new version with fresh database → Decide to migrate → Run migration tool

### Rollback Strategy

- Keep HSQLDB code until SQLite is proven stable
- Feature flag to switch between backends during testing

### Deliverables

- [ ] SQLite-backed repositories
- [ ] All tests passing against SQLite
- [ ] Migration tool (separate module)
- [ ] User documentation for migration

## Phase 5: SQLite Caches (Replace MapDB)

**Goal:** Replace MapDB thumbnail and EXIF caches with SQLite.

### Current MapDB Usage

| File | Purpose |
|------|---------|
| `ThumbnailsDb.java` | File path → JPEG thumbnail bytes |
| `ExifCache.java` | File path → EXIF metadata |

### SQLite Cache Schema

```sql
-- Separate cache.db file (can be deleted without data loss)

CREATE TABLE thumbnails (
    file_path TEXT PRIMARY KEY,
    modified_time INTEGER,
    thumbnail BLOB
);

CREATE TABLE exif_cache (
    file_path TEXT PRIMARY KEY,
    modified_time INTEGER,
    exif_json TEXT
);
```

### Design Decisions

- **Separate database file** (`cache.db`) - can be deleted without losing user data
- **Modification time tracking** - invalidate when source file changes
- **No migration needed** - caches rebuild automatically as user browses

### Benchmark Checkpoint

**Before starting Phase 5:**
```bash
./gradlew :Benchmarks:jmh -Pjmh.includes="ThumbnailCacheBenchmark|ExifCacheBenchmark"
cp Benchmarks/build/reports/jmh/results.json docs/benchmarks/pre-phase5-cache.json
```

**After completing Phase 5:**
```bash
./gradlew :Benchmarks:jmh -Pjmh.includes="ThumbnailCacheBenchmark|ExifCacheBenchmark"
# Compare results.json with pre-phase5-cache.json
# SQLite should be equal or faster than MapDB
```

### Deliverables

- [ ] `ThumbnailsDb` reimplemented with SQLite
- [ ] `ExifCache` reimplemented with SQLite
- [ ] MapDB dependency removed
- [ ] Cache rebuild tested and working
- [ ] Cache benchmarks show no regression vs MapDB

## Phase 6: Performance Optimizations

**Goal:** Improve startup time, thumbnail loading, and database queries.

### Startup Time

| Optimization | Approach |
|--------------|----------|
| Class Data Sharing (CDS) | Create CDS archive for faster class loading |
| Lazy initialization | Defer non-essential modules |
| Parallel init | Virtual threads for independent startup tasks |
| ZGC | `-XX:+UseZGC` for low-pause GC |

### Thumbnail Loading

- **Current:** Single-threaded fetcher
- **Target:** Virtual thread pool (`Executors.newVirtualThreadPerTaskExecutor()`)
- **Benefit:** Parallel thumbnail generation, saturate I/O and CPU

### Database Performance

| Optimization | Approach |
|--------------|----------|
| Indexes | Add indexes on frequently-queried columns |
| Query optimization | Identify and optimize slow queries |
| Batch operations | Batch inserts/updates |
| SQLite tuning | WAL mode, synchronous=NORMAL |

### JVM Flags

```
-XX:+UseZGC
-XX:+UseStringDeduplication
```

### Benchmark Validation

**Run full benchmark suite and compare to Phase 2 baseline:**
```bash
# Run all benchmarks
./gradlew :Benchmarks:jmh
./gradlew :Benchmarks:run > docs/benchmarks/phase6-startup.txt

# Compare with baseline
# docs/benchmarks/baseline-phase2.json (from Phase 2)
# Benchmarks/build/reports/jmh/results.json (current)
```

**Expected improvements:**

| Benchmark | Metric | Target |
|-----------|--------|--------|
| StartupBenchmark | total_ms | 30-50% faster (CDS + parallel init) |
| FolderLoadBenchmark (cold, 100) | avg time | 2-4x faster (virtual threads) |
| ThumbnailCacheBenchmark (concurrent) | throughput | 2-3x higher (virtual threads) |
| DatabaseBenchmark | all queries | 10-20% faster (indexes + WAL) |

### Deliverables

- [ ] Measurable startup time improvement (compare to Phase 2 baseline)
- [ ] Faster folder browsing (compare to Phase 2 baseline)
- [ ] Database indexes in place
- [ ] Optimized JVM launch script
- [ ] Benchmark comparison report showing improvements

## Dependency Changes

### Removed

| Dependency | Reason |
|------------|--------|
| HSQLDB 1.8.0.10 | Replaced by SQLite |
| MapDB 0.9.9-SNAPSHOT | Replaced by SQLite |
| Lucene | Replaced by simple string search |

### Added

| Dependency | Purpose |
|------------|---------|
| SQLite JDBC (xerial) | Database |
| FlatLaf | Modern look and feel |
| Jakarta XML Binding | JAXB replacement |
| JUnit 5 | Testing |
| AssertJ | Assertions |
| JMH | Performance benchmarks |

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Low test coverage | Build comprehensive tests in Phase 2 before risky changes |
| SQLite migration breaks things | Feature flag to switch backends during testing |
| SwingX incompatible with Java 21 | Test first, replace only if broken |
| User data loss | Separate migration tool, non-destructive approach |
| Performance regression | Baseline benchmarks in Phase 2, compare after Phase 6 |

## References

- `thoughts/shared/handoffs/general/2025-11-28_22-32-58_java21-gradle-modernization.md` - Original analysis
- `thoughts/shared/handoffs/general/2025-11-28_21-46-26_java21-upgrade-analysis.md` - Initial upgrade analysis
- `Libraries/README.txt` - Third-party JAR versions
