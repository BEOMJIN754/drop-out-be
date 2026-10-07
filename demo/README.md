# DROP backend

Java 21 + PostgreSQL. The local database has been migrated to generated numeric IDs.
See [migration notes](migrations/README.md) for the schema, removed columns and recovery.

## Run

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
./gradlew bootRun
```

Configure `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` as needed.
Defaults: `jdbc:postgresql://localhost:5432/dropout`, user `beom`.
Hibernate uses `ddl-auto=validate`; an existing schema is required.
For a fresh empty local database only, initialize with
`./gradlew bootRun --args='--spring.jpa.hibernate.ddl-auto=update'`, then use validation normally.

Frontend: run `npm run dev` in `../../drop-out`. Vite proxies `/api` to localhost:8080.
Brands and releases come from the database; failures show a message instead of demo records.

## Authentication and API

Login uses a server session. `POST /api/auth/login` takes username/password and returns
`{id, username, admin}`. `GET /api/auth/me` restores frontend authentication state.
Write requests other than login require the token/header returned by `GET /api/auth/csrf`.
`POST /api/auth/logout` ends the session. User-specific follows are stored in the DB.
Roles are stored in `users.role` as `USER` or `ADMIN`; `ADMIN_USERNAMES` is no longer used.
Login and `/me` return `role` (and the compatible `admin` boolean).
New accounts default to `USER`. Role changes take effect on the next login.

Optional `LOGIN_SEED_USERNAME` and `LOGIN_SEED_PASSWORD` create an account if absent.
There is no signup API; existing passwords are not overwritten.

Use `GET /api/brands` to obtain numeric IDs, then `GET /api/releases?brand=ID`.
Brand/release detail routes use `/{id}`. Create requests omit ID and slug.
See migration notes for the full ID contract. Brand requests use
`/api/brand/requests`; admins list/review through `/api/brand/admin/requests`
and `PATCH /api/brand/admin/requests/{id}/status`.

## Enums

Java enums are persisted with `EnumType.STRING` and PostgreSQL CHECK constraints, never ordinal numbers.
Frontend values and display labels are centralized in `drop-out/src/enums.js`.

| Domain | Enum | Values |
| --- | --- | --- |
| User role | UserRole | USER, ADMIN |
| Brand category | BrandCategory | STREETWEAR, SKATE_STREETWEAR, TECHNICAL_STREETWEAR, CASUAL_COLLABORATION, SPORTSWEAR, CONTEMPORARY, OTHER |
| Release type | ReleaseType | DROP, COLLAB, CAPSULE, SEASONAL, LIMITED, OTHER |
| Release status | ReleaseStatus | UPCOMING, RELEASED, UNKNOWN |
| Brand request | RequestStatus | PENDING, APPROVED, REJECTED |

Release status is calculated from release timestamps/dates on read, not persisted.
Brand category requests/responses use enum codes; the UI renders readable labels.
Release `COLLAB` remains the collaboration code for compatibility.
New enum values require a Java/frontend update and a migration of the corresponding CHECK constraint.

`migrations/20260910_enums.sql` was applied to the local database after the numeric-ID migration.
It preserves existing records, maps brand category labels, and gives the existing `admin` account
the ADMIN role. Do not rerun it. Other installations must adapt that one-time account mapping
to their previously configured admin usernames.
Pre-change backup: `../../backups/dropout-before-enums-20260910.dump`.

## Crawlers

API startup no longer runs crawlers or schedules hourly collection.
Run `CrawlerApplication.main()` from your IDE, or run `./gradlew crawl` from `demo`.
The standalone application calls `CrawlerManager.start()`, runs all enabled crawlers
once in parallel, waits for them to finish, then closes its database connections and exits.
It does not bind an HTTP port, so the API server can stay running on 8080.
Each crawler logs its own success/failure; completion does not imply every source succeeded.
Disable individual crawlers with
`SUPREME_CRAWLER_ENABLED=false`, `PALACE_CRAWLER_ENABLED=false`,
`SANSAN_CRAWLER_ENABLED=false`, or `SPAO_CRAWLER_ENABLED=false`.
Successful recollection preserves numeric IDs and creation timestamps;
failed collection preserves existing rows.

- Supreme: official news first page, Asia release dates. CDN dataset is configurable
  with `SUPREME_DATASET`. Missing release dates remain null.
- Palace: up to eight linked launch announcements, Korean in-store time. Year is inferred
  from the current homepage season and checked against the announced weekday.
- SAN SAN GEAR: current homepage collection and official JSON-LD logo. New collection titles
  at the same source URL create separate records. No invented release dates.
- SPAO: official collaboration banners. Banner image dates are not treated as release dates.
  All collaboration dates remain unconfirmed; no item-specific date overrides are applied.

The detail screen displays `dateLabel` as date provenance. Null dates remain unconfirmed;
calendar download is only available when a date is known. These are announcement/collection
crawlers, not a complete product or weekly-drop catalogue.

## Verify

`./gradlew test` requires PostgreSQL. Set all crawler-enabled environment variables to
false for tests. Numeric-ID integration tests roll back their database writes.
`npm run build` and `npm run test:e2e` run frontend checks.

Optional captured-payload tests accept `SUPREME_CAPTURE`, `PALACE_CAPTURE_DIR`,
`SANSAN_CAPTURE`, and `SPAO_CAPTURE`. Explicit SPAO import additionally requires
`SPAO_IMPORT=true`.
