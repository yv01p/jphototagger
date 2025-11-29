---
date: 2025-11-28T22:32:58+00:00
researcher: Claude
git_commit: 6e5eb843f31ce9c204a2d6011ecef21776cbe6ba
branch: master
repository: jphototagger
topic: "Java 7 to Java 21 Upgrade and Gradle Migration Analysis"
tags: [java-upgrade, gradle-migration, jaxb, hsqldb, mapdb, lucene, swing, modernization, performance]
status: complete
last_updated: 2025-11-28
last_updated_by: Claude
type: implementation_strategy
---

# Handoff: Java 21 Upgrade & Gradle Migration Analysis

## Task(s)

**Status: Analysis Complete, Implementation Not Started**

Comprehensive analysis of JPhotoTagger codebase for modernization from Java 7 to Java 21, including:

1. **Java 21 Upgrade Analysis** - COMPLETE
   - Build configuration (34 NetBeans project.properties files)
   - JAXB migration strategy (31 files → Jakarta XML Binding)
   - HSQLDB upgrade path (1.8.0.10 → 2.7.x)
   - Lucene replacement strategy (remove, use simple string search)
   - Reflection usage analysis (safe, no changes needed)

2. **Third-Party Library Analysis** - COMPLETE
   - MapDB (HIGH risk - API breaks between 0.9.x → 3.x)
   - JGoodies (LOW risk - test compatibility)
   - SwingX (MEDIUM risk - 72 files, unmaintained library)
   - EventBus, metadata-extractor, BeansBinding, XMPCore, NetBeans Lookup

3. **NetBeans to Gradle Migration Analysis** - COMPLETE
   - 34 subprojects mapped
   - Dependency structure documented
   - Gradle multi-project build structure proposed
   - No NetBeans GUI forms (all UI hand-coded - good)

4. **Additional Modernization Opportunities** - COMPLETE
   - Code modernization (lambdas, streams, records)
   - Testing infrastructure gaps (33 test files, JUnit 4)
   - CI/CD pipeline recommendations
   - jpackage native installer strategy

5. **Performance Improvements** - COMPLETE
   - Database optimizations (indexes, batching, HikariCP)
   - Thumbnail loading parallelization
   - Virtual threads opportunities
   - Startup time optimizations (CDS, lazy init)

## Critical References

- `thoughts/shared/handoffs/general/2025-11-28_21-46-26_java21-upgrade-analysis.md` - Initial upgrade analysis with JAXB/HSQLDB/Lucene decisions
- `Libraries/README.txt` - Third-party JAR versions and sources
- `Program/nbproject/project.properties` - Main module build config showing all 30+ dependencies

## Recent changes

No code changes made - this was a pure analysis session building on prior research.

## Learnings

### Build System
- 34 NetBeans Ant modules with complex inter-dependencies
- 0 .form files (all UI hand-coded Swing) - makes migration easier
- 205 @ServiceProvider annotations (NetBeans Lookup for DI)
- 1,379 Java source files total

### Critical Library Risks
- **MapDB 0.9.9-SNAPSHOT** (`Program/.../ThumbnailsDb.java`, `Exif/.../ExifCache.java`): Uses deprecated APIs (`DBMaker.newFileDB()`, `db.getHashMap()`). MapDB 3.x has completely different API and incompatible serialization format.
- **SwingX 1.6.2**: Used in 72 files for `JXList`, `JXTree`, `JXLabel`. Unmaintained since 2012. Core usage in `Resources/src/org/jphototagger/resources/UiFactory.java`.
- **HSQLDB driver class**: `org.hsqldb.jdbcDriver` → `org.hsqldb.jdbc.JDBCDriver` in `Repositories/HSQLDB/src/org/jphototagger/repository/hsqldb/ConnectionPool.java:70`

### Performance Bottlenecks Identified
- Single-threaded thumbnail fetcher: `Program/.../ThumbnailCache.java:131-160`
- Custom connection pool could be replaced with HikariCP: `Repositories/HSQLDB/.../ConnectionPool.java`
- Sequential startup: `Program/src/org/jphototagger/program/app/AppInit.java:48-70`
- `SystemUtil.getJavaVersion()` broken for Java 9+: `Lib/src/org/jphototagger/lib/util/SystemUtil.java:18-35`

### Code Patterns
- Heavy use of `synchronized` (38+ occurrences) - opportunity for fine-grained locking
- Very few try-with-resources usages (~6 found) - many JDBC resources not properly closed
- Only 33 test files - low test coverage

## Artifacts

Analysis produced in this session (documented in conversation, not written to files):
- Third-party library compatibility matrix with risk levels
- Gradle multi-project build structure proposal
- Performance optimization priority matrix
- Modernization roadmap with phases

Prior analysis document:
- `thoughts/shared/handoffs/general/2025-11-28_21-46-26_java21-upgrade-analysis.md`

## Action Items & Next Steps

### Phase 1: Build System Migration (Recommended First)
1. Create `settings.gradle.kts` with 34 subproject definitions
2. Create root `build.gradle.kts` with shared configuration
3. Create individual module `build.gradle.kts` files
4. Map dependencies from `project.properties` to Gradle format
5. Replace local JARs with Maven Central where available

### Phase 2: Java 21 Upgrade
1. Update source/target to 21 in Gradle configs
2. Fix `SystemUtil.getJavaVersion()` for Java 9+ version format
3. Replace `javax.xml.bind.*` → `jakarta.xml.bind.*` in 31 files
4. Update HSQLDB driver class name in ConnectionPool.java
5. Replace Lucene with simple string search in `HelpSearch.java`

### Phase 3: MapDB Migration
1. Rewrite `ThumbnailsDb.java` for MapDB 3.x API
2. Rewrite `ExifCache.java` for MapDB 3.x API
3. Handle cache file format migration (or clear caches on upgrade)

### Phase 4: Performance Quick Wins
1. Add database indexes for common queries
2. Replace single ThumbnailFetcher thread with virtual thread pool
3. Enable CDS for faster startup
4. Add JVM flags: `-XX:+UseZGC -XX:+UseStringDeduplication`

### Phase 5: CI/CD & Testing
1. Create GitHub Actions workflow
2. Upgrade to JUnit 5
3. Add integration tests for database and file operations

## Other Notes

### Key File Locations
- Build configs: `*/nbproject/project.properties` (34 files)
- Third-party JARs: `Libraries/` (16 JARs)
- Database layer: `Repositories/HSQLDB/src/org/jphototagger/repository/hsqldb/`
- Thumbnail caching: `Program/src/org/jphototagger/program/module/thumbnails/cache/`
- Image utilities: `Image/src/org/jphototagger/image/`
- JAXB utilities: `Lib/src/org/jphototagger/lib/xml/bind/`

### Useful Commands
```bash
# Find all project.properties
find . -name "project.properties" -path "*/nbproject/*"

# Find JAXB imports
grep -r "javax.xml.bind" --include="*.java"

# Find @ServiceProvider usage
grep -r "@ServiceProvider" --include="*.java" | wc -l
```

### Dependencies Not on Maven Central (need local JARs)
- eventbus.jar (EventBus 1.4)
- swingx-core.jar (SwingX 1.6.2)
- beansbinding.jar (BeansBinding 1.2.1)
- ImgrRdr.jar (Imagero Reader)

### Test Considerations
- SwingX compatibility unknown with Java 21 - test early
- BeansBinding uses reflection - may need `--add-opens` flags
- MapDB cache files will be unreadable after upgrade - plan for cache clearing
