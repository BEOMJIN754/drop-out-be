# Numeric ID schema — 2026-09-10

The local `dropout` database has been migrated. Do not rerun `20260910_numeric_ids.sql` on it.
For another database with the previous schema, stop the app, make a `pg_dump -Fc` backup,
then run `psql -v ON_ERROR_STOP=1 -d dropout -f migrations/20260910_numeric_ids.sql`.
The migration is transactional and keeps users, requests, releases, and follow relationships.
Hibernate now uses `ddl-auto=validate` so it cannot silently recreate removed columns.

## API contract

- Brand and release IDs are generated Java `Long` / PostgreSQL `bigint` / JSON numbers.
- Creation requests do not take `id` or `slug`; the database assigns the ID.
- `GET /api/brands/{id}` and `GET /api/releases/{id}` use numeric IDs.
- `GET /api/releases?brand={brandId}` filters by numeric brand ID.
- Follow responses are number arrays; PUT/DELETE follow paths use numeric brand IDs.
- `GET /api/brands` provides the IDs to the frontend. No frontend ID mapping is hardcoded.
- Brand names have a unique database constraint.
- Crawlers find brands by name and releases by `(brand_id, source_url, title)` to preserve
  IDs and creation timestamps on repeated imports. A changed title is a distinct record;
  SAN SAN GEAR uses one homepage URL for successive collection titles.
- External-site slugs in parser payloads are still read to construct official source URLs;
  they are not stored as application slugs or used as API IDs.

## Removed columns

| Table | Columns | Reason |
| --- | --- | --- |
| brands | slug | Numeric ID lookup replaces it |
| releases | slug | Numeric ID lookup replaces it |
| releases | confidence, pieces_summary | All 26 rows were empty |
| releases | short_title | Equal to title for all 26 rows; UI uses title |
| releases | published_at, release_url, source_timezone, announcement_status, date_precision | No current application consumer; release dates and official source URL remain |
| brand_requests | requested_by | Response derives the name through user_id |

`date_label` remains and is now displayed as the actual date provenance in the detail modal.
`created_at`, `updated_at`, and `crawled_at` retain lifecycle/recollection timestamps.
Partially empty fields such as logo, founding year, and release time remain because they
have real values and application consumers. No entirely empty column remains in populated tables.

## Verification

All four live crawlers ran after migration: Palace 2, SAN SAN GEAR 1, SPAO 12, Supreme 11.
The 26 existing releases retained their numeric IDs. Existing 3 follows, 4 requests and 2 users
were preserved. Backend integration tests cover generated numeric IDs, API responses,
follow isolation, and repeated import without duplicate rows.

Pre-migration backup: `../../../backups/dropout-before-numeric-20260910.dump`.
It contains user data and must not be committed or shared. Restore it to a separate database
with `pg_restore --no-owner --no-privileges -d RECOVERY_DATABASE BACKUP_FILE` for inspection.
