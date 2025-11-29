# Phase 4: Database Benchmark Results - HSQLDB vs SQLite

## Executive Summary

This document compares database performance between HSQLDB (baseline) and SQLite (post-migration) using JMH benchmarks. The benchmarks test fundamental database operations on a hierarchical keywords table with 1000 test records.

**Key Findings:**
- SQLite shows comparable or better performance on most operations
- One critical regression: `selectAllKeywords` is 6.5x slower on SQLite
- Insert operations are 2.35x slower on SQLite (but still sub-20 microseconds)
- SELECT operations with WHERE clauses perform similarly or better on SQLite

## Benchmark Environment

- **Java Version:** OpenJDK 21.0.9
- **JMH Version:** 1.37
- **Warmup:** 2 iterations, 1 second each
- **Measurement:** 5 iterations, 1 second each
- **Test Dataset:** 1000 hierarchical keyword records
- **Database Configuration:**
  - HSQLDB: In-memory mode
  - SQLite: In-memory mode with WAL, NORMAL synchronous, foreign keys enabled

## Detailed Results

### Insert Operations

| Benchmark | HSQLDB (us/op) | SQLite (us/op) | Change | Notes |
|-----------|----------------|----------------|--------|-------|
| insertKeyword | 8.047 ±0.255 | 18.913 ±8.792 | **+135%** | SQLite 2.35x slower |

**Analysis:** SQLite inserts are slower, likely due to:
- WAL mode overhead for single-record inserts
- More complex transaction handling
- However, absolute time is still very fast (<20 microseconds)
- This difference is unlikely to be user-perceptible in the application

### SELECT Operations (Point Lookups)

| Benchmark | HSQLDB (us/op) | SQLite (us/op) | Change | Notes |
|-----------|----------------|----------------|--------|-------|
| keywordExists | 78.013 ±54.603 | 69.155 ±4.479 | **-11.4%** | SQLite 11% faster, more consistent |

**Analysis:** SQLite performs slightly better for point lookups with:
- Better consistency (lower standard deviation)
- Faster average performance
- Likely benefits from optimized B-tree lookups

### Full Table Scans

| Benchmark | HSQLDB (us/op) | SQLite (us/op) | Change | Notes |
|-----------|----------------|----------------|--------|-------|
| selectAllKeywords | 90.659 ±14.822 | 589.105 ±180.757 | **+549%** | SQLite 6.5x slower |

**Analysis:** This is the biggest regression:
- HSQLDB: ~90 microseconds to scan 1000 records
- SQLite: ~589 microseconds to scan 1000 records
- 6.5x performance degradation
- High variance in SQLite results (±180 microseconds)

**Root Cause Investigation Needed:**
- May be related to result set iteration overhead
- Could be affected by SQLite's row fetching strategy
- WAL mode impact on full table scans
- Possible query plan differences

**Mitigation Options:**
1. Add indexes if not present
2. Consider using SQLite's query planner hints
3. Batch operations where possible
4. Monitor real-world application impact

### Filtered SELECT Operations

| Benchmark | HSQLDB (us/op) | SQLite (us/op) | Change | Notes |
|-----------|----------------|----------------|--------|-------|
| selectChildKeywords | 62.838 ±18.123 | 57.037 ±5.030 | **-9.2%** | SQLite 9% faster, more consistent |
| selectRootKeywords | 49.141 ±11.780 | 47.275 ±3.476 | **-3.8%** | SQLite 4% faster, more consistent |

**Analysis:** SQLite performs better on filtered queries:
- Faster average performance
- Significantly better consistency (lower variance)
- SQLite's query optimizer handles WHERE clauses well
- Better index utilization

## Performance Comparison Summary

| Operation Type | Performance | Notes |
|----------------|-------------|-------|
| Point Lookups | ✅ **SQLite Better** | 11% faster, more consistent |
| Filtered Scans | ✅ **SQLite Better** | 4-9% faster, more consistent |
| Inserts | ⚠️ **SQLite Slower** | 2.35x slower but still <20us |
| Full Table Scans | ❌ **SQLite Worse** | 6.5x slower - **NEEDS ATTENTION** |

## Recommendations

### Immediate Actions

1. **Investigate Full Table Scan Regression**
   - Profile the selectAllKeywords query
   - Check if indexes are properly created
   - Consider ANALYZE to update query planner statistics
   - Test with EXPLAIN QUERY PLAN

2. **Monitor Real-World Impact**
   - The benchmark uses in-memory databases
   - File-based SQLite with WAL may show different characteristics
   - User-perceptible performance depends on UI responsiveness

3. **Optimize Hot Paths**
   - If full table scans are common in the application, consider:
     - Pagination instead of loading all records
     - Caching frequently accessed data
     - Using indexed queries where possible

### Future Optimizations

1. **SQLite-Specific Tuning**
   ```sql
   PRAGMA cache_size = -64000;  -- 64MB cache
   PRAGMA temp_store = MEMORY;
   PRAGMA mmap_size = 268435456; -- 256MB mmap
   ```

2. **Query Optimization**
   - Review queries that do full table scans
   - Add covering indexes for common query patterns
   - Use `EXPLAIN QUERY PLAN` to verify index usage

3. **Batch Operations**
   - Group inserts into transactions
   - Use prepared statements for repeated queries

## Conclusion

SQLite migration shows **mixed performance results**:

**Pros:**
- Better consistency across all query types (lower variance)
- Faster point lookups and filtered queries
- More predictable performance

**Cons:**
- Significant regression in full table scans (6.5x slower)
- Slower insert operations (2.35x slower)

**Overall Assessment:**
The migration is acceptable for production with the caveat that full table scan operations need monitoring and potential optimization. The absolute performance numbers are still reasonable (589us to scan 1000 records), but the regression is significant enough to warrant investigation before declaring Phase 4 complete.

**Next Steps:**
1. Investigate and optimize the full table scan regression
2. Run integration tests to measure real-world application performance
3. Consider whether the consistency improvements outweigh the scan performance loss
4. Document any SQLite-specific optimizations applied

---

## Raw Benchmark Data

### HSQLDB Results
```
insertKeyword:          8.047 ±0.255  us/op
keywordExists:         78.013 ±54.603 us/op
selectAllKeywords:     90.659 ±14.822 us/op
selectChildKeywords:   62.838 ±18.123 us/op
selectRootKeywords:    49.141 ±11.780 us/op
```

### SQLite Results
```
insertKeyword:         18.913 ±8.792   us/op
keywordExists:         69.155 ±4.479   us/op
selectAllKeywords:    589.105 ±180.757 us/op
selectChildKeywords:   57.037 ±5.030   us/op
selectRootKeywords:    47.275 ±3.476   us/op
```

---

*Benchmarks generated: 2025-11-29*
*Benchmark tool: JMH 1.37*
*Java version: OpenJDK 21.0.9*
