# JPhotoTagger SaaS Multi-User Design

**Date:** 2026-02-23
**Status:** Approved
**Scope:** Convert JPhotoTagger from a desktop Swing application into a SaaS multi-user web application supporting up to 800 users.

---

## Context

JPhotoTagger is a mature Java 21 photo management desktop application with ~368,000 lines of code. It handles EXIF, IPTC, and XMP metadata across JPEG, TIFF, and a wide range of RAW formats (Canon CR2, Nikon NEF, Sony ARW, Fuji RAF, etc.). The application has recently completed a 7-phase modernization to Java 21 + Gradle + SQLite.

This design describes a **clean rewrite** as a SaaS web application, preserving key metadata library choices from the existing codebase.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    React Frontend                    │
│         (Browser - photo browse, tag, search)        │
└──────────────────────┬──────────────────────────────┘
                       │ HTTPS / REST
┌──────────────────────▼──────────────────────────────┐
│            Spring Boot 3 API (Java 21)               │
│  - metadata-extractor (EXIF/RAW reading on upload)  │
│  - ExifTool subprocess (metadata writing to files)  │
└────────────┬──────────────────────┬─────────────────┘
             │                      │
┌────────────▼───┐        ┌─────────▼──────┐
│  PostgreSQL    │        │  S3-compatible  │
│  (metadata,    │        │  (photo files + │
│   users,       │        │   thumbnails)   │
│   keywords,    │        └────────────────┘
│   search)      │
└────────────────┘
```

### Key decisions

| Concern | Decision | Rationale |
|---|---|---|
| Frontend | React (static, CDN-served) | Standard SaaS web UI |
| Backend | Spring Boot 3 / Java 21 | Team knows Java; strongest RAW metadata library ecosystem |
| Database | PostgreSQL | Multi-user capable; full-text search built in |
| File storage | S3-compatible object storage | Photos never stored in DB; pre-signed URLs for direct browser delivery |
| Metadata reading | metadata-extractor + Adobe XMP SDK | Best RAW format support of any language ecosystem |
| Metadata writing | ExifTool subprocess | Same approach as existing app; language-neutral |
| Auth | Managed service (Auth0 / Cognito) + JWT | Avoids building login/registration/MFA from scratch |
| Multi-tenancy | Shared DB, user_id on all tables | Simple and correct for 800 users |
| Search | PostgreSQL full-text + indexed EXIF columns | Sufficient for 800 users; no Elasticsearch needed |
| Testing | JUnit 5 + Testcontainers + Playwright | Consistent with existing project testing culture |

---

## Multi-Tenancy & User Data Isolation

A shared PostgreSQL database with `user_id` discrimination on every user-owned table. No per-tenant schemas or databases — unnecessary complexity for 800 users.

### Schema pattern

```sql
photos        (id, user_id, filename, s3_key, uploaded_at, ...)
exif_data     (id, photo_id, tag_name, tag_value, ...)
keywords      (id, user_id, name, parent_id, ...)
photo_keywords(photo_id, keyword_id)
saved_searches(id, user_id, name, criteria_json, ...)
```

### S3 key namespace

```
users/{user_id}/originals/{photo_id}.{ext}
users/{user_id}/thumbnails/{photo_id}_thumb.jpg
```

### Enforcement

- Every SQL query includes `WHERE user_id = ?`
- Spring Security extracts `user_id` from JWT and makes it available to all service calls
- PostgreSQL row-level security as an optional second enforcement layer

---

## Photo Upload & Metadata Extraction Pipeline

```
Browser → multipart POST → Spring Boot → S3 (original)
                                       → metadata-extractor (EXIF/RAW parsing)
                                       → thumbnail generation
                                       → S3 (thumbnail)
                                       → PostgreSQL (all extracted metadata)
```

1. **Upload** — file streamed directly to S3; does not fully land on API server disk
2. **EXIF extraction** — `metadata-extractor` parses EXIF, IPTC, and XMP tags; results written to PostgreSQL
3. **Thumbnail generation** — background job extracts embedded JPEG preview from RAW (fast, no full decode) or generates from JPEG/TIFF; stored in S3
4. **XMP sidecar handling** — `.xmp` sidecars uploaded alongside RAW files are paired by filename and merged into the database record
5. **ExifTool** — invoked only when writing metadata back to a file after user edits; not in the upload path
6. **Batch uploads** — accepted asynchronously; API returns a job ID immediately and client polls for completion

**RAW format note:** Some RAW formats require full file availability (not a stream) for parsing. For these, the pipeline downloads to a temp file, extracts, then deletes. Handled transparently by `metadata-extractor`'s format detection.

---

## Search

All search handled in PostgreSQL — no Elasticsearch.

- **Metadata search:** Standard SQL with indexed `exif_data`, `keywords`, and `photo_keywords` joins, filtered by `user_id`
- **Full-text search:** PostgreSQL `tsvector` on titles, descriptions, and captions with a GIN index
- **Saved searches:** Stored as JSON in PostgreSQL, reconstructed into SQL queries server-side
- **Performance:** Well-indexed PostgreSQL handles millions of metadata rows at sub-100ms for this user scale

---

## Frontend

React static build served from CDN or S3. Communicates with Spring Boot API over HTTPS.

| Desktop panel | Web equivalent |
|---|---|
| Thumbnail browser | Virtualized grid (react-window) for 10,000+ photos |
| EXIF/IPTC/XMP panel | Tabbed side panel |
| Keyword tree | Collapsible tree component |
| Advanced search | Filter sidebar (field / operator / value rows) |
| Full-size view | Lightbox overlay |

- **Photo delivery:** API issues short-lived S3 pre-signed URLs; browser fetches photos directly from S3 — API server carries no image traffic
- **State management:** React Query for server state; no Redux needed at this scale

---

## Testing Strategy

| Layer | Tools |
|---|---|
| Backend unit | JUnit 5 + Mockito |
| Backend integration | `@SpringBootTest` + Testcontainers (PostgreSQL + LocalStack for S3) |
| Frontend components | Vitest + React Testing Library |
| E2E | Playwright — critical journeys: upload → metadata extraction → search → keyword edit |
| CI | GitHub Actions (existing pipeline pattern) |

Key backend areas requiring thorough test coverage: metadata extraction pipeline, search query construction, S3 upload/download, JWT auth enforcement.

---

## Deployment Considerations (Deferred)

Deployment target (AWS, GCP, self-hosted) and specific S3-compatible provider (AWS S3, Cloudflare R2, MinIO) to be decided in the implementation plan. The architecture is provider-neutral.
