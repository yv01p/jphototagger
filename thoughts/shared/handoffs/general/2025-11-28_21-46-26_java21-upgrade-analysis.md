---
date: 2025-11-28T21:46:26+00:00
researcher: Claude
git_commit: 6e5eb843f31ce9c204a2d6011ecef21776cbe6ba
branch: master
repository: jphototagger
topic: "Java 7 to Java 21 Upgrade Analysis"
tags: [java-upgrade, jaxb, hsqldb, lucene, swing, migration]
status: in_progress
last_updated: 2025-11-28
last_updated_by: Claude
type: implementation_strategy
---

# Handoff: Java 7 to Java 21 Upgrade Analysis

## Task(s)

**Status: Analysis Complete, Implementation Not Started**

Analyzed the JPhotoTagger codebase for Java 7 → Java 21 upgrade compatibility. This is a mature Java Swing desktop application for photo tagging and organization.

### Completed Analysis:
1. Build configuration analysis (34 NetBeans project.properties files need updating)
2. JAXB usage analysis (31 files) - decision: migrate to Jakarta XML Binding
3. Reflection usage analysis (3 files) - safe, no changes needed
4. HSQLDB analysis (73 files) - decision: upgrade from 1.8.0.10 to 2.7.x
5. Lucene analysis (1 file) - decision: replace with simple string search
6. Swing compatibility confirmed (still supported in Java 21)

## Critical References

- `Repositories/HSQLDB/src/org/jphototagger/repository/hsqldb/ConnectionPool.java:70-79` - HSQLDB driver and URL configuration
- `Lib/src/org/jphototagger/lib/xml/bind/XmlObjectExporter.java` - Core JAXB marshalling utility
- `Lib/src/org/jphototagger/lib/help/HelpSearch.java` - Only Lucene usage (142 lines)

## Recent changes

No code changes made - this session was analysis only.

## Learnings

### JAXB (javax.xml.bind)
- 31 files use JAXB for XML export/import of domain objects
- Already has JAXB JARs bundled (jaxb-api.jar, jaxb-core.jar, jaxb-impl.jar)
- Uses annotations: `@XmlRootElement`, `@XmlAccessorType(FIELD)`, `@XmlElement`, `@XmlElementWrapper`, `@XmlTransient`
- Core utilities: `Lib/src/org/jphototagger/lib/xml/bind/XmlObjectExporter.java` and `XmlObjectImporter.java`
- **Decision:** Migrate `javax.xml.bind.*` → `jakarta.xml.bind.*` (Option 1 - lowest risk)

### HSQLDB
- Version 1.8.0.10 from 2008, current is 2.7.x
- 73 files in `Repositories/HSQLDB/` module
- Uses **only standard JDBC** - no HSQLDB-specific APIs
- Embedded file-based: `jdbc:hsqldb:file:...`
- **Decision:** Upgrade JAR only, minimal code changes (driver class name)

### Lucene
- Version 3.4.0 from 2011, current is 9.x
- Only 1 file: `Lib/src/org/jphototagger/lib/help/HelpSearch.java`
- Used for in-app help search (in-memory RAMDirectory)
- Lucene 3→9 has massive API breaks
- **Decision:** Replace with simple string search (removes 1.5MB dependency)

### Reflection Usage
- 3 files use reflection: `Settings.java`, `ListItemTempSelectionRowSetter.java`, `TreeItemTempSelectionRowSetter.java`
- All reflect on application's own classes, not JDK internals
- **Safe for Java 21** - no changes needed

### Swing
- Swing is NOT deprecated/removed in Java 21 (common misconception)
- Still in `java.desktop` module, actively maintained
- No Applet usage found (good - Applets were removed)

### Third-Party Libraries (in Libraries/ folder)
| Library | Current | Status |
|---------|---------|--------|
| HSQLDB | 1.8.0.10 (2008) | Upgrade to 2.7.x |
| Lucene | 3.4.0 (2011) | Remove/replace |
| SwingX | unknown (~2009) | Test compatibility |
| MapDB | 0.9.9-SNAPSHOT (2013) | Consider upgrade to 3.x |
| JGoodies | 2.5.3 (2013) | Test compatibility |
| JAXB | 2.2.11 | Replace with Jakarta 4.0 |

### Build Configuration
- 34 `nbproject/project.properties` files with `javac.source=1.7` and `javac.target=1.7`
- All need updating to `21`

## Artifacts

No artifacts produced - analysis session only. Findings documented in this handoff.

## Action Items & Next Steps

### Phase 1: Build Configuration
1. Update all 34 `project.properties` files: change `javac.source` and `javac.target` from `1.7` to `21`

### Phase 2: JAXB Migration
1. Replace bundled JAXB JARs with Jakarta XML Binding 4.0+:
   - Remove: `jaxb-api.jar`, `jaxb-core.jar`, `jaxb-impl.jar`, `jaxb-activation.jar`
   - Add: `jakarta.xml.bind-api-4.0.x.jar`, `jaxb-impl` (GlassFish), `jakarta.activation-api`
2. Search/replace in 31 files: `javax.xml.bind` → `jakarta.xml.bind`

### Phase 3: HSQLDB Upgrade
1. Replace `Libraries/hsqldb.jar` (1.8.0.10) with `hsqldb-2.7.2.jar`
2. Update driver class in `ConnectionPool.java:70`:
   - From: `org.hsqldb.jdbcDriver`
   - To: `org.hsqldb.jdbc.JDBCDriver`
3. Test database operations

### Phase 4: Lucene Replacement
1. Remove `Libraries/lucene-core.jar`
2. Rewrite `Lib/src/org/jphototagger/lib/help/HelpSearch.java` (~142 lines) to use simple `String.contains()` search

### Phase 5: Testing
1. Build with Java 21
2. Test all export/import functionality (JAXB)
3. Test database operations (HSQLDB)
4. Test help search (Lucene replacement)
5. Test UI (Swing)

## Other Notes

### Key File Locations
- Build configs: `*/nbproject/project.properties` (34 files)
- JAXB utilities: `Lib/src/org/jphototagger/lib/xml/bind/`
- JAXB domain objects: `Domain/src/org/jphototagger/domain/`
- Database layer: `Repositories/HSQLDB/src/org/jphototagger/repository/hsqldb/`
- Help search: `Lib/src/org/jphototagger/lib/help/HelpSearch.java`
- Third-party JARs: `Libraries/`

### Useful Commands
Find all project.properties: `find . -name "project.properties" -path "*/nbproject/*"`
Find JAXB imports: `grep -r "javax.xml.bind" --include="*.java"`

### SystemUtil.getJavaVersion() Note
`Lib/src/org/jphototagger/lib/util/SystemUtil.java:18-35` parses version strings like `1.7.0_80`.
Java 9+ uses format `21.0.1`. This method may need updating to handle new format.
