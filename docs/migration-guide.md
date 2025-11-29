# JPhotoTagger Database Migration Guide: HSQLDB to SQLite

## Overview

JPhotoTagger now supports SQLite as an alternative database backend to HSQLDB. This guide explains how to migrate your existing HSQLDB database to SQLite, switch between database backends, and what to expect during the migration process.

## Why SQLite?

SQLite offers several advantages over HSQLDB:

- **Better Performance**: SQLite shows 4-11% faster performance on filtered queries and point lookups
- **Lower Variance**: More consistent query performance with lower variance
- **Single File Database**: Simplifies backup and portability (single .db file vs multiple HSQLDB files)
- **Industry Standard**: Widely used, battle-tested database engine
- **Active Development**: Regular updates and security patches

## Performance Characteristics

Based on benchmark testing with 1000 records:

| Operation Type | Performance vs HSQLDB | Notes |
|----------------|----------------------|-------|
| Point Lookups (SELECT with WHERE) | 11% faster | More consistent performance |
| Filtered Queries | 4-9% faster | Better index utilization |
| Single Inserts | 2.35x slower | Still very fast (<20 microseconds) |
| Full Table Scans | 6.5x slower | Optimization recommended for large datasets |

**Overall**: SQLite provides better consistency and comparable or improved performance for typical application workflows. Full table scans are slower but unlikely to impact normal usage.

## Migration Methods

### Method 1: Using System Property (Recommended for Testing)

This method allows you to test SQLite without permanently changing your configuration:

1. **Start JPhotoTagger with SQLite backend**:
   ```bash
   java -Djphototagger.database.backend=sqlite -jar jphototagger.jar
   ```

2. **Verify SQLite is active**: Check the application logs or database file location

3. **Return to HSQLDB**: Simply restart without the system property
   ```bash
   java -jar jphototagger.jar
   ```

**Note**: The system property creates a new empty SQLite database. To migrate your existing data, use Method 3.

### Method 2: Using Programmatic Preference

You can set the database backend preference programmatically (this persists across sessions):

```java
import org.jphototagger.domain.repository.DatabaseBackend;
import org.jphototagger.domain.repository.DatabaseBackendPreference;

// Switch to SQLite
DatabaseBackendPreference.setPreference(DatabaseBackend.SQLITE);

// Switch back to HSQLDB
DatabaseBackendPreference.setPreference(DatabaseBackend.HSQLDB);

// Clear preference (reverts to default HSQLDB)
DatabaseBackendPreference.clearPreference();
```

**Priority**: System property (`-Djphototagger.database.backend`) always takes precedence over stored preferences.

### Method 3: Migrating Existing Data (Production Migration)

To migrate your existing HSQLDB database to SQLite:

#### Step 1: Locate Your HSQLDB Database

Default locations:
- **Linux**: `~/.jphototagger/database/jphototagger`
- **Windows**: `%USERPROFILE%\.jphototagger\database\jphototagger`
- **macOS**: `~/Library/Application Support/jphototagger/database/jphototagger`

You should see files like:
- `jphototagger.script`
- `jphototagger.data`
- `jphototagger.properties`

#### Step 2: Backup Your Database

**CRITICAL**: Always backup before migration!

```bash
# Linux/macOS
cp -r ~/.jphototagger/database ~/.jphototagger/database.backup

# Windows
xcopy %USERPROFILE%\.jphototagger\database %USERPROFILE%\.jphototagger\database.backup /E /I
```

#### Step 3: Run Migration Tool

Using Gradle (from source):
```bash
./gradlew :Tools:MigrationTool:run --args="~/.jphototagger/database/jphototagger ~/.jphototagger/database/jphototagger.db"
```

Using standalone JAR:
```bash
java -jar MigrationTool.jar /path/to/hsqldb/jphototagger /path/to/output/jphototagger.db
```

**Parameters**:
- First argument: HSQLDB database path (without .script/.data extension)
- Second argument: Output SQLite database file path

#### Step 4: Verify Migration

The migration tool will display:
```
JPhotoTagger Database Migration Tool
=====================================
Source HSQLDB: /home/user/.jphototagger/database/jphototagger
Target SQLite: /home/user/.jphototagger/database/jphototagger.db

Starting migration...
Migrating table: files
  Completed: files (1234 rows)
Migrating table: xmp
  Completed: xmp (1234 rows)
...

Migration completed successfully!
  Tables migrated: 35
  Total rows: 12345

SQLite database created at: /home/user/.jphototagger/database/jphototagger.db
```

#### Step 5: Switch to SQLite Backend

Add system property or set preference:
```bash
java -Djphototagger.database.backend=sqlite -jar jphototagger.jar
```

Or programmatically:
```java
DatabaseBackendPreference.setPreference(DatabaseBackend.SQLITE);
```

#### Step 6: Verify Data Integrity

1. Start JPhotoTagger with SQLite backend
2. Verify your images, keywords, collections are present
3. Test search functionality
4. Check EXIF metadata displays correctly
5. Verify saved searches work

## What to Expect During Migration

### Migration Process

- **Duration**: Depends on database size. Approximately 1-2 minutes per 10,000 images
- **Progress**: The tool shows real-time progress with table names and row counts
- **Tables Migrated**: All 35+ tables including:
  - Image files and metadata
  - XMP data (keywords, descriptions, ratings)
  - EXIF data (camera, lens, GPS, timestamps)
  - Collections and favorites
  - Saved searches
  - User preferences and templates
  - Programs and actions
  - Hierarchical keywords and synonyms

### Data Preserved

- All image file references
- Complete XMP metadata (keywords, descriptions, creator, rights, etc.)
- EXIF data (camera model, lens, ISO, focal length, GPS coordinates, timestamps)
- Collections and collection memberships
- Favorite directories
- Saved searches and search panels
- Programs and default program associations
- Metadata edit templates
- Hierarchical keyword structures
- Synonyms and word sets
- Rename templates
- User-defined file filters and file types

### Schema Changes

The SQLite schema is semantically equivalent to HSQLDB but uses SQLite-specific syntax:
- `BIGINT GENERATED BY DEFAULT AS IDENTITY` → `INTEGER PRIMARY KEY AUTOINCREMENT`
- `VARCHAR_IGNORECASE` → `TEXT COLLATE NOCASE`
- `VARBINARY` → `BLOB`
- Foreign key constraints are preserved
- All indexes are recreated

### Known Differences

1. **Auto-increment**: SQLite uses `INTEGER PRIMARY KEY AUTOINCREMENT` which may generate different IDs than HSQLDB (but referential integrity is maintained)
2. **Case Sensitivity**: SQLite uses `COLLATE NOCASE` for case-insensitive text matching
3. **File Location**: SQLite creates a single `.db` file plus `.db-wal` and `.db-shm` files (WAL mode)

## Rollback Instructions

If you need to return to HSQLDB:

### Option 1: System Property Override

Simply remove the system property:
```bash
# Instead of:
java -Djphototagger.database.backend=sqlite -jar jphototagger.jar

# Use:
java -jar jphototagger.jar
```

### Option 2: Clear Stored Preference

```java
DatabaseBackendPreference.clearPreference();
```

### Option 3: Restore from Backup

If you experience issues:

1. **Stop JPhotoTagger**

2. **Delete or rename the SQLite database**:
   ```bash
   rm ~/.jphototagger/database/jphototagger.db
   rm ~/.jphototagger/database/jphototagger.db-wal
   rm ~/.jphototagger/database/jphototagger.db-shm
   ```

3. **Restore HSQLDB from backup** (if needed):
   ```bash
   cp -r ~/.jphototagger/database.backup/* ~/.jphototagger/database/
   ```

4. **Clear preference or system property**

5. **Restart JPhotoTagger**

**Note**: Your HSQLDB database is never modified by the migration tool. It only reads from HSQLDB and writes to a new SQLite file.

## Troubleshooting

### Migration Tool Errors

**Error: HSQLDB database files not found**
- Verify the path is correct (without .script extension)
- Check that `.script` or `.data` files exist
- Ensure you have read permissions

**Error: Could not create output directory**
- Verify you have write permissions
- Check disk space availability
- Ensure parent directory exists

**Migration failed with "Table not found"**
- Your HSQLDB version may have a different schema
- Check if you're running a very old or modified version
- Contact support with error details

### Runtime Errors

**"Connection factory is closed"**
- Database was shut down
- Restart the application

**Slow performance on large datasets**
- Run `VACUUM` on SQLite database to optimize
- Check available disk space
- Consider adding indexes for frequently queried columns

**Missing data after migration**
- Verify migration completed successfully (check row counts)
- Ensure you're pointing to the correct database file
- Check system property and preference settings

### Verification Queries

To verify data integrity, you can run SQL queries:

```sql
-- Count files
SELECT COUNT(*) FROM files;

-- Count XMP records
SELECT COUNT(*) FROM xmp;

-- Count keywords
SELECT COUNT(*) FROM dc_subjects;

-- Check for orphaned records (should be 0)
SELECT COUNT(*) FROM xmp WHERE id_file NOT IN (SELECT id FROM files);
```

## Best Practices

### Before Migration

1. **Backup everything**: Database, images, configuration
2. **Test on a copy**: Migrate a copy of your database first
3. **Close JPhotoTagger**: Ensure HSQLDB is not locked
4. **Check disk space**: Ensure 2x database size available
5. **Note database size**: Record file count for verification

### During Migration

1. **Don't interrupt**: Let the migration complete
2. **Monitor progress**: Watch for error messages
3. **Note table/row counts**: Verify against original database

### After Migration

1. **Verify data**: Check images, keywords, collections
2. **Test workflows**: Perform typical tasks
3. **Monitor performance**: Note any slow operations
4. **Keep HSQLDB backup**: Don't delete for at least a week
5. **Report issues**: If you find problems, switch back and report

## Performance Optimization

### SQLite Configuration

The SQLite implementation uses these optimizations by default:
- **WAL Mode**: Write-Ahead Logging for concurrent reads
- **Synchronous Normal**: Balance between safety and performance
- **Foreign Keys Enabled**: Referential integrity enforcement

### Additional Tuning (Advanced)

If you experience performance issues, you can tune SQLite pragmas:

```sql
PRAGMA cache_size = -64000;      -- 64MB cache
PRAGMA temp_store = MEMORY;       -- Use memory for temp tables
PRAGMA mmap_size = 268435456;     -- 256MB memory-mapped I/O
```

**Caution**: These settings require manual modification and may impact stability.

### Query Optimization

For slow queries:
1. Run `EXPLAIN QUERY PLAN` to check index usage
2. Consider adding covering indexes
3. Use filtered queries instead of full table scans
4. Paginate large result sets

## Frequently Asked Questions

### Q: Will migration delete my HSQLDB database?
**A**: No. The migration tool only reads from HSQLDB and creates a new SQLite file. Your original database is never modified.

### Q: Can I switch back to HSQLDB after migrating?
**A**: Yes. Simply remove the system property or clear the preference. Your HSQLDB database remains intact.

### Q: How long does migration take?
**A**: Approximately 1-2 minutes per 10,000 images. A typical 5,000 image database migrates in under a minute.

### Q: What happens to my images?
**A**: Nothing. The database stores file paths, not images. Migration only moves metadata and references.

### Q: Is SQLite faster than HSQLDB?
**A**: For most operations (point lookups, filtered queries), yes. Full table scans are slower but rarely used in normal workflows.

### Q: Can I use both databases simultaneously?
**A**: No. JPhotoTagger uses one database backend at a time, selected by system property or preference.

### Q: What if migration fails midway?
**A**: The migration tool creates a new SQLite file. If it fails, simply delete the incomplete .db file and try again. Your HSQLDB database is unaffected.

### Q: Do I need to migrate images?
**A**: No. Only the database is migrated. Images remain in their original locations.

### Q: Will plugins still work?
**A**: Yes. Plugins use the repository interface, which works identically with both backends.

### Q: How do I verify the migration was successful?
**A**: The migration tool reports table and row counts. Compare these numbers with your HSQLDB database size. Also verify data in the application.

### Q: Can I migrate back from SQLite to HSQLDB?
**A**: Not directly. If you need to go back, use your HSQLDB backup. There's currently no SQLite-to-HSQLDB migration tool.

### Q: What are the .db-wal and .db-shm files?
**A**: These are SQLite Write-Ahead Log and Shared Memory files. They're created automatically in WAL mode and should not be deleted while the database is in use.

## Support

If you encounter issues during migration:

1. **Check this guide**: Review troubleshooting section
2. **Check logs**: Look for error messages in application logs
3. **Restore backup**: If stuck, restore HSQLDB backup and retry
4. **Report issues**: Provide migration tool output and error messages

## Technical Details

### Database File Locations

**HSQLDB** (multiple files):
- `jphototagger.script` - Schema and small tables
- `jphototagger.data` - Large table data (cached tables)
- `jphototagger.properties` - Database properties
- `jphototagger.log` - Transaction log

**SQLite** (single file + WAL):
- `jphototagger.db` - Main database file
- `jphototagger.db-wal` - Write-Ahead Log (transient)
- `jphototagger.db-shm` - Shared memory (transient)

### Feature Flag Implementation

The database backend selection uses a three-tier priority system:

1. **System Property** (highest): `-Djphototagger.database.backend=sqlite`
2. **Stored Preference**: `DatabaseBackendPreference.setPreference(DatabaseBackend.SQLITE)`
3. **Default**: HSQLDB (for backwards compatibility)

### Migration Algorithm

The migration tool:
1. Opens source HSQLDB database (read-only)
2. Creates target SQLite database
3. Creates SQLite schema (all tables, indexes, foreign keys)
4. Migrates tables in dependency order:
   - Core tables first (files, application)
   - 1:N reference tables (creators, rights, locations, etc.)
   - Metadata tables (xmp, exif)
   - Junction tables (xmp_dc_subject)
   - Configuration tables (collections, searches, programs)
5. Validates row counts match source
6. Reports success or failure with detailed counts

### Compatibility

- **Java Version**: Requires Java 21+
- **Database Versions**: HSQLDB 2.x, SQLite 3.45+
- **Schema Version**: Phase 4 schema (post-Java 21 upgrade)
- **Data Formats**: Preserves all BLOB and TEXT data as-is

---

**Document Version**: 1.0
**Last Updated**: 2025-11-29
**Applicable to**: JPhotoTagger Phase 4 (SQLite Migration)
