# Fleet service — Step 2

Java 25 / Quarkus service with one public business endpoint:
`GET /api/v1/vehicles/{canonical-uuid}`. The synthetic vehicle is
`00000000-0000-4000-8000-000000000002`. Its JSON contains `id`, `reference`,
and `contractVersion: 1`. Invalid IDs return 400, absent vehicles 404, and
storage failures 503 with a safe `STORAGE_UNAVAILABLE` code. SQLSTATE is logged,
not provider messages. `/q/health/live` and `/q/health/ready` remain available.

Allocation writes are repository-only: `FleetRepository.insertInterval` takes
an `AllocationInterval` with UUIDs, microsecond-resolution UTC instants bounded
to years 0001–9999, and typed `BLOCKING` or `RELEASED` status.
Windows are half-open `[start,end)` and require `start < end`. Only PostgreSQL
SQLSTATE `23P01` becomes `IntervalConflictException`; other database failures
are explicit `FleetStorageException`s. Failed writes roll back. There is no
public allocation/hold API, expiry, compensation, request hash, or distributed
confirmation.

## Ownership and migration

Bootstrap must precreate schema `fleet` owned by `fleet_migrator`, install
`btree_gist` in `public` as administrator, and grant `USAGE` on `public` to
`fleet_migrator`, **not** `fleet_app`. Revoke PUBLIC schema/database creation
privileges. Keep both identities non-superuser, without role inheritance from
the owner. Runtime gets only schema usage, SELECT/INSERT/UPDATE on the two
business tables and SELECT on `schema_version`; never migration history or DDL.

Run `com.milesofsmiles.fleet.FleetMigrationMain` as a standalone Java main with
the packaged `target/quarkus-app/app/*`, `target/quarkus-app/lib/main/*`,
and `target/quarkus-app/lib/boot/*` on the classpath. Supply `DB_URL`, owner
`DB_USERNAME`, and `DB_PASSWORD_FILE`.
Only the migration container receives this owner input; its entrypoint and Java
main read it. Flyway (BOM-managed 12.0.0)
uses `classpath:db/migration`, schema/default schema `fleet`, and disabled clean.
V1 creates and seeds records; subsequent runs validate checksums and do not
overwrite rows. Never repair checksum drift or delete data just to pass startup:
restore the immutable migration, investigate the failure, then rerun it.

Application startup only checks the single supported schema version, 1; it never
runs migrations or reads the migration password file. Runtime configuration
uses `DB_URL`, `DB_USERNAME=fleet_app`, and `DB_PASSWORD`. Use JDBC server
verification (`sslmode=verify-full` and a trusted CA) in deployment. The `tls`
profile disables plaintext HTTP, uses port 8443 and `${TLS_DIR}/server.crt`
and `server.key`; default binding is loopback. Follow the chapter deployment
bootstrap rather than assigning owner credentials to the application.

## Verification

From the repository root: `./mvnw -f source/fleet-service/pom.xml verify -q`.
Tests require Docker (no automatic skips), create a pinned PostgreSQL 17.6
container with trusted TLS, bootstrap distinct identities and the extension,
then run the actual migrations as owner and application as runtime.
Unit tests use mocks; integration tests cover HTTP, interval boundaries/status,
concurrency, rollback, role restrictions, repeated migration/checksum drift,
and schema incompatibility. This module does not establish cross-service
isolation or deployment/restart evidence by itself.
