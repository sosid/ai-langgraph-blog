# Step 2 - A Record Has an Owner

Alex can now submit a synthetic booking request and see the same stored request
again after a retry. We can also store a draft rental referencing that request
and read a fleet-owned compact vehicle. Alex still has no reservation:
references do not allocate a car, a draft is not pickup, and neither service has
permission to invent a price or charge a card.

[Step 1](step-1-article.md) gave us one stable PostgreSQL fixture and observable
failure behavior. Its initialization code also created a table using the
application's database identity. That was an explicit baseline limitation.
Before adding business transitions, we need to answer a more basic question:
which process is allowed to change which records?

This chapter starts from accepted `development` at
`daed623a6c32383b96b402fe4bf27f22778b060a`, including the Maven layout correction.
The [approved blueprint](../plans/step-2-plan.md) defines this increment.
The [verification record](../verification/step-2-verification.md) identifies the
candidate and separates demonstrated behavior from blocked deployment paths.
No release tag is implied.

## Why the earlier correction remains in history

The preceding integration, `daed623a6c32383b96b402fe4bf27f22778b060a`
(`fix: organize service projects as Maven modules`), is a build-layout
correction between the original baseline and this chapter. It moved the
existing booking application into `source/booking-service`, retained root
Maven commands, and adjusted container inputs and documentation. It did not
introduce rental or fleet behavior.

That integration also records a fresh kind attempt: a Ready node and an imported
image were observed, but the host kernel's missing iptables comment match
blocked deployment. It is therefore not a chapter with kind verification
silently omitted, nor a successful runtime milestone. Keeping the failure
visible explains why this chapter later tries direct pod traffic and, after
that also fails, the separately approved node-local port-forward fallback.

The correction is already published on `development`. Amending a new feature
commit cannot erase it from that target's ancestry; replacing it would require
rewriting the shared branch and invalidating recorded predecessor identifiers.
This PR preserves the integration and its evidence instead. See the
[deployment-history explanation](../../deploy/step-2/README.md#why-the-published-correction-is-retained)
for the distinction between the build correction and deployment acceptance.

## What the lab demonstrates

The Java 25 reactor has mock-based unit tests and PostgreSQL Testcontainers
tests for every owner. They cover schema upgrade, role denial, canonical request
identity, concurrent duplicates, and allocation boundaries. The legacy fixture
API remains covered.

Compose's internal network path works; host-published Compose access still times
out in this sandbox. A normal kind direct-pod path also fails: PostgreSQL is
ready, but migration Jobs cannot reach it. A separately approved single-node
`hostNetwork` fallback works through verified HTTPS port-forwards, including
application/database replacement and retained data. That is not a CNI,
ClusterIP, DNS, HAProxy, cert-manager or cross-node networking pass.

There is no model, graph, checkpoint, telemetry pipeline, or payment call. The
new records are business state, not graph progress, ephemeral model context, or
immutable audit evidence.

## Three owners, one local failure domain

A Maven module organizes a build. It does not enforce a data boundary.
The running service's database credentials do.

```text
Versioned booking API --> booking_app --> booking schema
Versioned draft API   --> rental_app  --> rental schema
Vehicle read API      --> fleet_app   --> fleet schema
                                          |
                         one local PostgreSQL instance

Separate migrator process per schema --> owning schema DDL + Flyway history
Runtime identities                  -X-> other owners' records / DDL / history
```

Booking owns the original fixture, new requests, request keys and hashes.
Rental owns a `DRAFT` and references to booking/vehicle IDs. Fleet owns vehicles
and allocation interval storage. There are no cross-schema foreign keys or
shared domain entities. Rental does not query booking's table to validate its
reference; that reference is deliberately not a confirmation of remote facts.

One PostgreSQL instance keeps the lab manageable but shares an administrator
and an outage boundary. Separate schemas are useful for proving grants, not
for pretending these services have independent infrastructure availability.
In a smaller system, modules within one service could be a better trade-off.
The series uses these owners to teach the later cross-service effect boundaries,
not because every three-table application needs microservices.

The root reactor now includes
[`booking-service`](../../source/booking-service/pom.xml),
[`rental-service`](../../source/rental-service/pom.xml), and
[`fleet-service`](../../source/fleet-service/pom.xml).
Application dependencies and tests stay in their owning modules.

## Migration authority is not runtime authority

The [bootstrap](../../deploy/step-2/bootstrap.sql) runs once with an administrator
identity. It creates an owner/migrator and a different runtime role per schema,
revokes public schema creation and database temporary-table privileges, and
installs PostgreSQL's `btree_gist` extension for fleet's constraint.

The migrator owns tables and its Flyway history. The runtime receives schema
usage, explicitly granted record DML, and read-only schema-version access.
It cannot create tables, assume its owner role, or inspect migration history.
The database administrator remains powerful; these are least-privilege lab
credentials, not protection against a malicious administrator.

[`BookingMigrationMain`](../../source/booking-service/src/main/java/com/milesofsmiles/booking/BookingMigrationMain.java)
uses the maintained Flyway API in a separate owner-credential process. Rental
and fleet have equivalent independent entry points. We intentionally do not
run migrations at application startup: doing so would put DDL credentials in
the service whose authority we are trying to limit.
The [release-pinned Quarkus guide](https://github.com/quarkusio/quarkus/blob/3.40.1.1/docs/src/main/asciidoc/flyway.adoc)
describes the PostgreSQL-specific Flyway module and programmatic migration
option. This implementation uses Flyway directly, not the Quarkus Flyway
extension's startup automation.

The [booking migration](../../source/booking-service/src/main/resources/db/migration/V1__booking_records.sql)
adopts the exact Step 1 fixture table after an explicit administrator ownership
transfer. It moves the table into booking's schema; it does not recreate its
rows. A clean installation instead creates it there. The injected clock still
supplies the fixture's initial time, and `ON CONFLICT DO NOTHING` still
preserves an existing fixture.

[`BookingBootstrapIT`](../../source/booking-service/src/test/java/com/milesofsmiles/booking/BookingBootstrapIT.java)
exercises the actual deployment bootstrap against a legacy table with changed
synthetic values. [`BookingUpgradeIT`](../../source/booking-service/src/test/java/com/milesofsmiles/booking/BookingUpgradeIT.java)
also proves the original row survives repeat initialization and that checksum
drift fails. We do not enable baseline-on-migrate, automatic repair, or clean.
Runtime guards reject a missing or incompatible schema version rather than
attempting DDL. Booking's guard runs before fixture seeding.

## A retry must identify the same request

A connection can fail after a database commit. Returning 503 does not establish
that nothing was stored. A caller can safely resubmit the **same request key
and payload** because the key, hash and record are committed together.
There is no remote effect or distributed transaction in this chapter.

[`BookingSubmission`](../../source/booking-service/src/main/java/com/milesofsmiles/booking/BookingSubmission.java)
validates the typed body and hashes a specified ordered UTF-8 representation.
Instants normalize to UTC before hashing. Rental normalizes canonical UUIDs to
lowercase. JSON property order, insignificant timestamp formatting, and UUID
letter case therefore do not create a new semantic request.
The [contract reference](../contracts/step-2-http.md) shows exact canonical
inputs, field bounds, response shapes and error outcomes.

Hashing raw JSON would make transport formatting part of business identity.
Ignoring payload differences would be worse: a reused key could silently refer
to different dates. The database unique key resolves competing writers, not an
in-memory "exists" check. A same-hash replay returns the original ID and time;
a different hash returns 409 without overwriting the original.

The `Idempotency-Replayed` header makes first creation (201/false) and replay
(200/true) observable. Keys are service-local; the same key in rental does not
coordinate booking's transaction. SHA-256 is a payload comparison mechanism,
not caller authentication, a tenant boundary, or tamper-evident audit storage.
Changing canonicalization after accepting requests requires an explicit
compatibility design.

## Adjacent is not overlapping

Fleet stores half-open UTC windows: `[start, end)` includes the start but excludes
the end. A return at 12:00 and another allocation starting at 12:00 can be
adjacent at the storage layer. Whether a future maintenance buffer should make
that business operation invalid is a later rule, not hidden inside this chapter.

The [fleet migration](../../source/fleet-service/src/main/resources/db/migration/V1__fleet_records.sql)
uses PostgreSQL's exclusion constraint:

```sql
CONSTRAINT allocation_interval_no_overlap EXCLUDE USING gist (
    vehicle_id WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
) WHERE (status = 'BLOCKING')
```

`btree_gist` supplies equality support for the vehicle UUID alongside range
overlap. The constraint rejects competing blocking windows for one vehicle,
including concurrent inserts and an invalid released-to-blocking update.
Different vehicles and released rows do not conflict.
See the pinned
[PostgreSQL 17.6 range reference](https://github.com/postgres/postgres/blob/REL_17_6/doc/src/sgml/rangetypes.sgml)
and [extension reference](https://github.com/postgres/postgres/blob/REL_17_6/doc/src/sgml/btree-gist.sgml).

[`AllocationInterval`](../../source/fleet-service/src/main/java/com/milesofsmiles/fleet/AllocationInterval.java)
rejects empty/reversed windows and unsupported timestamp precision before JDBC.
[`FleetRepository`](../../source/fleet-service/src/main/java/com/milesofsmiles/fleet/FleetRepository.java)
maps only SQLSTATE `23P01` to an interval conflict; a foreign-key or connection
failure is not mislabeled as "car unavailable." Rollback leaves no partial row.
There is no public hold-write endpoint, expiry loop, quote or reservation
confirmation. Storage integrity is necessary for Step 4, not its early delivery.

## Run the candidate

Use Temurin 25.0.5+8 without preview flags, Docker and the repository wrapper.
The observed lab is Linux amd64 with approximately 8 GiB RAM; no measured
minimum, latency, throughput, or recovery SLA is claimed.

| Component | Pin / observation |
| --- | --- |
| Java / Quarkus | Temurin 25.0.5+8 / BOM and plugin 3.40.1.1 |
| Maven / wrapper | 3.9.11 / 3.3.4, distribution checksum pinned |
| Flyway core and PostgreSQL module | 12.0.0, managed by the pinned Quarkus BOM |
| PostgreSQL / `btree_gist` | 17.6-alpine, image digest pinned / 1.7 |
| JDBC / Testcontainers | 42.7.13 / 2.0.5 |
| JUnit / Mockito / REST Assured | 6.1.3 / 5.21.0 / 6.0.1 |
| Compiler / Surefire+Failsafe / Enforcer | 3.14.1 / 3.5.4 / 3.6.2 |
| Docker Engine / Compose | 29.1.3 / 5.6.1 |
| kind / Kubernetes+kubectl | 0.33.0 / 1.35.0 |
| Application base | Java 25 JRE; exact digest in the chapter Dockerfile |

From the repository root:

```bash
./mvnw clean compile
./mvnw verify
scripts/check-pki.sh
```

Standard verification uses mocks and Testcontainers, not kind or live model
credentials. It includes the original fixture, outage and trust checks.
The [deployment guide](../../deploy/step-2/README.md) contains build/private-input
preparation, Compose, kind fallback, foreground port-forwards, isolation and
restart exercises. Do not use Step 1 manifests to start the newly migrated
applications. On kind, applying rebuilt images fingerprints their content into
the service pod templates and migration Job names, so the fixed `:step-2` tags
cannot leave an older process serving requests after an image reload.

With the kind fallback forwarded as documented:

```bash
curl --noproxy '*' --cacert .local/step-2/ca.crt \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: alex-weekend-v1' \
  -d '{"contractVersion":1,"vehicleClass":"COMPACT","startsAt":"2026-11-01T10:00:00Z","endsAt":"2026-11-03T10:00:00Z"}' \
  -i https://localhost:8643/api/v1/booking-requests
```

Observe 201 and a generated record ID on the first use of that key, then 200
with the same ID/time and the replay header when repeating it. Changing only
the end date under that key produces 409. If the key was used earlier, the first
observation is already a replay. Do not hardcode an invented generated UUID.

## Failure lab and evidence limits

Run the smoke, isolation and persistence scripts from the deployment guide.
The smoke script verifies statuses, JSON, headers and stable identities.
The isolation script checks real SQLSTATE `42501` permission denials; a connection
timeout does not count as an access-control pass.
The persistence script adds a database-only marker and compares it with stored
record identities/hashes after restarting each service and then PostgreSQL.
A regenerated deterministic fixture cannot recreate that marker.

Unit tests mock JDBC/clock/resource boundaries. Testcontainers establishes real
constraints, migrations, transactions and runtime-role behavior on the pinned
server, including adjacent/overlapping windows and concurrent contenders.
Neither substitutes for the deployed TLS/port-forward observations. The kind
fallback preserves server trust but shares one network namespace and one node;
it says nothing about production service isolation or cross-node recovery.

Use synthetic data only. APIs are loopback lab surfaces without authenticated
customer scope. Keep keys and credentials out of logs; safe storage errors expose
no provider details. The shared application certificate and privileged database
bootstrap are explicit lab compromises. No model account or inference cost is
required, and no benchmark or production security certification is claimed.

Alex now has durable, owner-scoped request and draft records with tested failure
outcomes. Next, the planned rules chapter will decide eligibility and exact
pricing and record its grounds. Those decisions, audit evidence and telemetry
are not implemented here.
