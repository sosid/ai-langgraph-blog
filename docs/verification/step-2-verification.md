# Step 2 Verification: Service-Owned Records

## Findings and acceptance status

The implemented records, role isolation, migrations, versioned contracts,
request hashes and interval constraints pass the source/test checks below.
The legacy fixture behavior remains covered. The article describes the
demonstrated system and its limits; it does not deliver Step 3 or Step 4 early.

**Overall chapter acceptance is qualified, not an unrestricted deployment
pass.** Host-published Compose traffic still times out. Normal kind
pod-to-database traffic and kube-proxy Service routing fail in this host kernel.
The separately approved single-node `hostNetwork`/port-forward fallback passes,
but is not evidence that those normal paths work.

Review found and corrected related gaps before freeze: runtime temporary-table
privileges, strict booking JSON types, booking schema guard ordering, qualified
digest imports, and local rendering of Kubernetes JSON streams. The HTTP
walkthrough also demonstrated that JAX-RS resolves `Location` to an absolute
URL; the contract now describes the observed response rather than promising a
relative header. Independent code review also found that re-importing rebuilt
`:step-2` images did not restart existing Deployments or rerun completed
migration Jobs, and that a changed database pod IP could leave a pod-mode Job
template immutable under its old name. The kind apply path now fingerprints
image IDs in pod templates and the service/database image IDs plus database pod
address in migration Job names; focused deployment evidence is recorded below.
No independent security audit is claimed.

## Original PR candidate (historical)

| Field | Identifier |
| --- | --- |
| Step / branch | Approved Step 2, local `features/step-2` |
| Accepted base | `daed623a6c32383b96b402fe4bf27f22778b060a` |
| Original PR head | `082ea6bb51886ab0af6bcfd31458afa730ebf40a` |
| Original runtime/test/config SHA-256 | `dca61ba2de2c9bb5b5563bd89aa3bd6423ba5f142094e08ba5eafdbab63837aa` |
| Original runtime manifest | 101 files; source/tests, deployment and scripts excluding Markdown, root POM/wrapper/configuration |
| Original full tree | `636e5de81c6d94600b7438c2b6ed4e0cf6ec62aa` |
| Working tree at review start | Clean at the original PR head; current review changes are described below |

The runtime manifest is sorted by path. Each row contains `path`, executable
mode (`100644` or `100755`) and the file's SHA-256. The fingerprint is SHA-256
of the UTF-8 JSON row array with sorted keys and compact separators. The
fingerprint above applies only to the original PR head. The review correction
changes tracked deployment scripts, so that fingerprint does not identify the
updated candidate. Build output, downloaded tools, generated
credentials/certificates and live database data remain ignored.

The final full Git tree includes this record and all chapter documentation.
It cannot be embedded in its own contents without changing its identity.
Computing that tree with a temporary index does not stage the user's index or
create a commit. The full changed-file inventory appears below.

### Original PR preparation (historical)

The user subsequently authorized a PR to `development` and requested an
explanation if its already integrated predecessor could not be safely amended.
The target was fetched and still points to the exact accepted base above.
Published target history is preserved; no merge is amended or replaced.
The new article and deployment-guide sections explain the Maven correction's
purpose, its actual failed kind attempt, and why the later hostNetwork
observation does not erase those earlier gaps.

That preparation changed documentation only relative to the previously
verified full tree `bb53c7ea8f7ed1fad091061c572eb33ab6d8e6dc`.
All 101 runtime/test/config input hashes and executable modes are rechecked
against the manifest above. Consequently the recorded 39 unit and 34
integration passes remain applicable to unchanged inputs; they are not claimed
as a new runtime run. The revised narrative is checked against the actual
published commit, predecessor blueprint and Step 1 evidence, with local
links/anchors, credential-pattern scanning and whitespace checks repeated.
The original PR head/tree identifies that revised documentation snapshot. This
historical check does not cover the runtime script changes in the review
correction below. No PR template exists. The draft remains explicitly
non-accepting of blocked normal deployment gates.

## Review correction candidate

The current review candidate is based on the accepted base above and adds
image-revision handling to `scripts/step-2-kind-apply.sh` and
`scripts/step-2-kind-render.py`. The final commit and tree identifiers are in
the PR head and review handoff; the old runtime fingerprint is not reused for
these changes.

| Field | Value |
| --- | --- |
| Runtime/config fingerprint | `21fa32f5674002544f4be8c731cef44a330d76117537bfe9e0c52825794d71c5` |
| Manifest | 101 tracked non-Markdown files; sorted path, Git mode, and file SHA-256 rows encoded as compact sorted-key JSON before SHA-256 |

The renderer places a 32-character digest of the sorted local service image
IDs in each service Deployment pod template. The migration Job name also
includes a digest derived from those service IDs, the PostgreSQL image ID, and
the current PostgreSQL pod address. This causes changed images to produce a new
Deployment template and migration Jobs, and changed pod addresses to avoid
reapplying an immutable old Job. Existing Jobs and persistent database state
are retained.

| Check | Result |
| --- | --- |
| Temurin 25.0.4.1 `./mvnw clean compile` and `./mvnw verify` | Passed; 39 mock-based unit and 34 PostgreSQL Testcontainers integration tests, zero failures/errors/skips. Java sources and dependencies are unchanged by this review fix. |
| `bash -n scripts/step-2-kind-apply.sh`; renderer AST and pod/host render checks | Passed; pod-mode Job names vary with database pod address, host-mode names include `-host`, and both modes include revision annotations. |
| `scripts/check-pki.sh` | Passed. |
| Three pinned service Docker builds | Passed. |
| Dedicated fresh `kind-mos-step-2`, host-network apply | Passed on kind 0.33.0 / Kubernetes 1.35.0 / Docker 28.0.4; trusted local port-forwards exercised health, fixture, booking/rental writes, replay/conflict, fleet read, invalid/missing IDs, and runtime role denials. |
| Rebuild under the same `:step-2` tag with a review-only image label and reapply | Passed; changed Docker image ID changed the Deployment fingerprint, running pod image ID, and migration Job names. The original image was restored and re-applied; prior Jobs were preserved. |
| Same-image reapply | Passed; Deployment fingerprints and migration Job count remained unchanged. |
| `scripts/step-2-persistence.sh kind` | Passed after separate booking, rental, fleet, and PostgreSQL restarts; marker and record IDs/hashes remained stable. Final trusted smoke passed after reopening the forwards. |
| Unrelated system CA against forwarded HTTPS | Rejected as expected (curl exit 60). |

This run verifies only the approved single-node host-network/port-forward
fallback. It does not verify pod-network/CNI, ClusterIP/DNS, ingress,
cross-node networking, or HA. Compose was not rerun for this kind-only script
change. The original blocked network-gate evidence remains in force.

## Environment and prerequisites

Observed Linux amd64, kernel `6.12.8+`, approximately 8 GiB RAM; this is not a
minimum-resource, throughput, latency or recovery-time result.

The initial shell provided Temurin 27, so `./mvnw clean compile -q` correctly
failed the existing Java enforcer. Temurin 25.0.5+8 was restored from Adoptium's
release with package SHA-256
`68982529869257d0ee6b824ec5aa270ff8b118bb438670da0b97ebdab178d7fb`.
The downloaded JDK did not trust the environment's Maven proxy CA. Verification
used the existing approved environment truststore, not disabled validation:

```bash
export JAVA_HOME="$PWD/.local/toolchains/jdk-25.0.5+8"
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_OPTS="-Djavax.net.ssl.trustStore=/usr/local/sdkman/candidates/java/current/lib/security/cacerts"
./mvnw clean compile -q
./mvnw verify -q
```

No Java baseline, preview flags or dependency pins were weakened. The unchanged
accepted predecessor first passed root compile and all 7 original unit plus
5 integration tests under Java 25.

The final matrix is Quarkus 3.40.1.1, Maven 3.9.11/wrapper 3.3.4, Flyway
core/PostgreSQL module 12.0.0, PostgreSQL 17.6 and `btree_gist` 1.7, JDBC
42.7.13, Testcontainers 2.0.5, JUnit 6.1.3, Mockito 5.21.0, REST Assured
6.0.1, compiler 3.14.1, Surefire/Failsafe 3.5.4 and Enforcer 3.6.2.
Flyway is invoked programmatically in separate owner processes, not through
Quarkus extension startup automation. Pinned artifacts actually executed on
Java 25; release documentation alone was not treated as compatibility proof.

Docker 29.1.3 was already running. Missing Compose 5.6.1, kind 0.33.0,
kubectl 1.35.0, conntrack 1.4.8 and kmod were restored after missing-command
observations. iptables userspace was already installed. Kernel module dry runs
could not find `ip_tables`, `xt_comment` or `nf_conntrack` under
`/lib/modules/6.12.8+`; some conntrack matches are already built in. No host
firewall rules were flushed, security controls disabled or arbitrary modules
downloaded.

## Build, unit and integration evidence

Both final root commands above exited 0. Standard verification required Docker,
not kind, credentials for a live model, or paid services.

| Module | Mock-based unit tests | PostgreSQL integration tests | Failures / errors / skips |
| --- | --- | --- | --- |
| Booking | 11 | 12 | 0 / 0 / 0 |
| Rental | 18 | 11 | 0 / 0 / 0 |
| Fleet | 10 | 11 | 0 / 0 / 0 |
| Total | 39 | 34 | 0 / 0 / 0 |

Surefire and Failsafe XML reports are in each module's ignored `target/`.
The build packages every Quarkus module. Dependency warnings concerning
`sun.misc.Unsafe`, Mockito dynamic-agent loading and native access are not
presented as safety or preview-API guarantees.

## Claim inventory and trace

| Article / contract claim | Source/configuration | Discriminating evidence / limit |
| --- | --- | --- |
| Request and draft are not reservation/activation | `BookingRecord`, `DraftRental`, `RentalResource`, DRAFT database check | Unit/API tests and deployed body; no ACTIVE/hold/payment endpoint |
| Three schema owners, one infrastructure failure domain | Root module list, owner POMs, `bootstrap.sql` | Actual owner/runtime roles; one PostgreSQL container per deployment path |
| Runtime is not migrator | Owner grant SQL and separate `*MigrationMain` processes; Compose secrets and kind mounts | All six deployed cross-owner read/insert/delete pairs deny SQLSTATE 42501; runtime table/temp DDL, role assumption and history reads also denied |
| Runtime has useful own access | Owner repositories and explicit DML grants | Positive API writes/reads plus isolation script's own-table query |
| Actual deployment bootstrap works | `bootstrap.sh`, `bootstrap.sql` | `BookingBootstrapIT` runs the production files, checks extension 1.7, denied TEMP/owner assumption and adopted fixture |
| Exact legacy table upgrade preserves data | Booking V1 conditional move and administrator ownership transfer | `BookingBootstrapIT` and `BookingUpgradeIT` start with original columns and altered synthetic reference/time; row retained |
| No-op migration and checksum drift are distinct | Flyway 12 and per-schema immutable V1/history | Owner integration tests; repeated migrate executes zero new migrations, drift throws validation failure |
| Incompatible schema is rejected, not repaired | Three schema guards; booking guard priority before initialization | Mock and real version-mismatch tests; runtime cannot modify version/history |
| Original fixture/API/clock behavior remains | `BookingRepository`, `FixtureInitializer`, `ClockProducer`, original resource | Original 7+5 tests still pass; no-overwrite upgrade check and deployed legacy fixture smoke |
| Canonical hash input is typed and versioned | `BookingSubmission`, `RentalRequest` | Unit semantic-normalization checks and strict JSON tests; exact canonical formats in contract |
| Same key/same payload replays; changed payload conflicts | Owner unique keys and transactional insert/replay repositories | Unit/API tests, concurrent requests with one row, deployed stable IDs/header and 409 checks |
| Validated/unsupported/unknown/coerced input outcomes | HTTP body limit, Jackson customizers/mappers, DTO validation | Real 400/413 tests for versions, fields, types, duplicates, trailing JSON and bounded fields |
| Read 400/404 differs from unavailable storage | Resources and safe exception mappers | Mock mapping, real API failure/outage tests; no provider-detail response |
| Fleet half-open windows and conditional blocking | Fleet exclusion constraint, `AllocationInterval`, `FleetRepository` | Adjacent success, overlap rejection, different-vehicle success, released status predicate, invalid reactivation, invalid windows, concurrent winner and rollback tests |
| Only exclusion violations mean interval conflict | SQLSTATE 23P01 mapping | Foreign-key SQLSTATE 23503 remains explicit storage failure; no false capacity result |
| No early public hold lifecycle | Fleet read-only public resource and repository-only insert | POST rejection test; source/route inventory; expiry/quotes/compensation deferred |
| Health distinguishes dependency from process | Datasource readiness, liveness, original outage test | Paused database yields live 200, ready/API 503; unpause restores access |
| HTTPS and JDBC preserve server trust | TLS profiles, CA mounts, JDBC verify-full, `HealthProbe` | Original TLS tests, deployed HTTPS smoke, unrelated-trust curl rejection (60), deployed pg_stat_ssl shows all three runtime users with ssl=true in both labs |
| Private inputs are not runtime owner authority | Ignored `.local`, preparation script, migration/runtime secret mounts | File/configuration scan and actual role denial; bootstrap remains explicitly privileged |
| Internal Compose path works, host path does not | Chapter Compose and smoke script | Internal trusted assertions pass; host localhost:8543 curl times out (28); not conflated |
| Direct pod database path needs actual connectivity | Normal manifests plus hostAliases | Ready database, failed owner migration Jobs with connect timeout; normal gate blocked |
| Single-node fallback really works | Host-mode renderer, Recreate workloads, loopback DB/HTTPS, port-forward | All three forwarded APIs and trust rejection demonstrated after final image load and replacement |
| Stored state survives separate restarts | Compose named volume; kind node-local hostPath; persistence script | Database-only comment and record IDs/hash snapshots unchanged after booking, rental, fleet and PostgreSQL restarts in both labs |
| Fallback is not HA, cross-node or production isolation | One node/shared network namespace, node-local directory, common app certificate | Explicit deployment/article limits; no inference from a Ready node or probe |
| Article snippets, paths, versions and commands match candidate | Chapter, contract, guide and source reading path | Exact SQL excerpt inspected; article curl observed 201 then 200 and unchanged ID/time/header; Location resolved absolute and docs corrected |
| Diagrams preserve ownership and state distinctions | Plain-text before/after ownership diagram | Manual inspection; no Mermaid/browser-rendering claim or graph-progress implementation |
| Cumulative promises remain honest | Ledger P2 rows and historical P1 rows | Legacy behavior reverified; historical Step 1 ingress/rotation gaps retained; Step 3+ deferred |

The article's unverified/future statements are exclusions, not delivered
promises. No real PII, secrets, private chain-of-thought, model authority,
payment, cache, graph/checkpoint or audit/telemetry implementation is introduced.

## Deployment commands and observations

All chapter commands run from the repository root. Exact reusable commands,
environment inputs and failure boundaries are in
[`deploy/step-2/README.md`](../../deploy/step-2/README.md).

| Command / exercise | Observation |
| --- | --- |
| `scripts/step-2-prepare.sh` | Passed once, separate ignored inputs; refuses overwrite |
| `bash -n scripts/*.sh deploy/step-2/*.sh` | Exit 0 |
| `scripts/check-pki.sh` | Exit 0; permissions, hostname/expiry rejection and CA-preserving renewal; not deployed rotation qualification |
| `docker compose -f deploy/step-2/compose.yaml config --quiet` | Exit 0 |
| Three chapter Docker builds with SERVICE argument | Exit 0; pinned base, non-root runtime and standalone migration entry points |
| Compose `up -d --wait --wait-timeout 150` | Three migration processes complete; database and runtime services healthy |
| Normal host `curl --noproxy '*' --max-time 12 --cacert ... https://localhost:8543/q/health/ready` | Exit 28, connection timeout; remains blocked |
| Internal `nsenter` + three `--connect-to` mappings + smoke script | Exit 0; transport mapping preserves CA and localhost hostname verification |
| `scripts/step-2-isolation.sh compose` | Exit 0; six ordered pairs, positive own access, DDL/TEMP/owner/history denial |
| `scripts/step-2-persistence.sh compose` | Exit 0; snapshots unchanged across all four separate restarts; internal smoke rerun passed |
| User-authorized kind creation, isolated kubeconfig | Initial failure: missing node-local /dev/kmsg; retained node repaired without reset; interrupted kubeadm initialization completed |
| Dedicated kind bootstrap | Shipped CNI/storage templates applied; kindnet directed to actual API endpoint; node Ready, but normal networking still fails |
| Isolated iptables comment-match probe | Fails, chain cleaned up; kube-proxy independently reports missing comment match |
| Normal kind chapter apply | Database Ready; all three owner migration Jobs connect-timeout; failed Jobs retained |
| `STEP2_NETWORK=host` chapter apply | Exit 0 after renderer/import fixes; owner Jobs complete and three runtimes Ready |
| Kind port-forwards 8643:8443, 8644:8444, 8645:8445 | All explicitly bound to 127.0.0.1, no HAProxy; smoke passed |
| `scripts/step-2-isolation.sh kind` | Exit 0; same permission checks as Compose using actual credentials |
| `scripts/step-2-persistence.sh kind` | Exit 0 after final image import; all four workload replacements retain marker and IDs/hashes |
| Reopened forwards + final smoke | Exit 0; actual requests, DRAFT, replay headers/IDs, payload conflicts, vehicle, invalid/missing IDs and health |
| Unrelated CA against forwarded booking HTTPS | curl exit 60, expected TLS trust rejection |
| `pg_stat_ssl` joined to deployed runtime sessions | booking_app, rental_app and fleet_app all ssl=true, in both Compose and kind |
| Article's exact key/payload curl | Observed 201 then 200, same record/time, replay false then true; generated UUID not fabricated |

The first forward attempt reused Compose's 8543-8545 ports and correctly failed
to bind. The documented kind ports are 8643-8645 and were exercised. Restarting
pods ends forwards; they were reopened before the final smoke.

The normal path's default-provisioner PVC never bound. It and the failed normal
Jobs were preserved, not deleted to manufacture a result. The final manifests
use the approved node-local directory instead; no user data, PVC, cluster or
volume was removed. The default provisioner is not part of the claimed fallback.

Final Docker artifact indexes:

| Service | Index digest | Running kind config digest |
| --- | --- | --- |
| Booking | `088004ea64cc639fd41317d1a38e12be4a6034fbf630afe40d8f41b68e8c19ad` | `7f1826d83616a4e0fdf4f8f03aa94fb2fdc84acfc089f90793bb908574d542c3` |
| Rental | `ab622965de0d2d37059de568263927cfb2e0f625d002c4019ab5f056f03f5e87` | `d932fd1527843577cb589f432e58bc8d8187972b437eeecde80f0edaef0e7a82` |
| Fleet | `80b13ffe4d9feff975fa1a3d4df00ddd1d4640044cabf17d4204f71976d88aa3` | `37e935e49774909d1973b788b5bb146e9cececb52e1e70c1b7e3349d47a260d5` |

All digests are SHA-256. The index's linux/amd64 manifest config was read from
containerd and matched the running Pod image ID for every service; different
index/config digest representations were not mistaken for different content.
Completed migration Jobs were from earlier builds with unchanged migration SQL
and entry-point source; final packaged migrations were also rerun by Compose,
and the final source/test candidate reran production bootstrap in Testcontainers.

## Documentation, safety and limitations

At the original PR head, documentation checks covered 21 Markdown files, 90
local links/anchors, 122 candidate files scanned for credential patterns, its
77-file changed-path inventory, its unchanged runtime fingerprint, renderer
syntax, shell syntax, Compose configuration and Git whitespace. These are
historical results, not checks against the current review candidate.

For the current review candidate, the local Markdown link/anchor check passed
for 21 files and 93 links. Its 101-file non-Markdown runtime/config fingerprint
is recorded above; this fingerprint differs from the original PR candidate.
Source-backed editorial consistency was reviewed manually. External
release-specific Quarkus/Flyway and PostgreSQL source references were retrieved.
The PostgreSQL HTML documentation host could not resolve in this environment,
so the article links the accessible exact `REL_17_6` source documentation.
No browser, screenshot or rendered diagram result is claimed.

Manual review covered role grants, canonicalization, duplicate/conflict
transactions, safe errors, explicit rollback, no runtime DDL, startup guards,
non-root/resources/probes, TLS identity/trust, ignored private inputs,
loopback exposure and data preservation. This is not independent review or a
comprehensive runtime security audit. Generated synthetic credentials are
ignored. Standard tests do not require model/provider credentials.

Required normal deployment gates remain blocked. Live upgrade of a user's
Step 1 database, automatic downgrade, production authentication, distributed
availability, deployed certificate rotation, HAProxy/cert-manager, cross-node
networking, performance/load targets, ARM and native images are not claimed.
Merge/tag/publication and moving to Step 3 remain separate decisions.

## Changed-file inventory

Paths below are relative to the accepted base. This record itself is also new.

```text
README.md
deploy/step-2/Dockerfile
deploy/step-2/README.md
deploy/step-2/bootstrap.sh
deploy/step-2/bootstrap.sql
deploy/step-2/compose.yaml
deploy/step-2/entrypoint.sh
deploy/step-2/kind/cluster.yaml
deploy/step-2/kind/database.yaml
deploy/step-2/kind/migrations.yaml
deploy/step-2/kind/services.yaml
docs/blog/index.md
docs/blog/step-1-article.md
docs/blog/step-2-article.md
docs/contracts/step-2-http.md
docs/plans/step-2-plan.md
docs/promise-ledger.md
docs/verification/step-2-verification.md
pom.xml
scripts/step-2-isolation.sh
scripts/step-2-kind-apply.sh
scripts/step-2-kind-render.py
scripts/step-2-persistence.sh
scripts/step-2-prepare.sh
scripts/step-2-smoke.sh
source/booking-service/pom.xml
source/booking-service/src/main/java/com/milesofsmiles/booking/BookingMigrationMain.java
source/booking-service/src/main/java/com/milesofsmiles/booking/BookingRecord.java
source/booking-service/src/main/java/com/milesofsmiles/booking/BookingRecordRepository.java
source/booking-service/src/main/java/com/milesofsmiles/booking/BookingRecordResource.java
source/booking-service/src/main/java/com/milesofsmiles/booking/BookingRepository.java
source/booking-service/src/main/java/com/milesofsmiles/booking/BookingSubmission.java
source/booking-service/src/main/java/com/milesofsmiles/booking/InvalidJsonMapper.java
source/booking-service/src/main/java/com/milesofsmiles/booking/SchemaGuard.java
source/booking-service/src/main/java/com/milesofsmiles/booking/StrictJsonCustomizer.java
source/booking-service/src/main/resources/application.properties
source/booking-service/src/main/resources/db/migration/V1__booking_records.sql
source/booking-service/src/test/java/com/milesofsmiles/booking/BookingBootstrapIT.java
source/booking-service/src/test/java/com/milesofsmiles/booking/BookingRecordsIT.java
source/booking-service/src/test/java/com/milesofsmiles/booking/BookingRecordsTest.java
source/booking-service/src/test/java/com/milesofsmiles/booking/BookingUpgradeIT.java
source/booking-service/src/test/java/com/milesofsmiles/booking/PostgresResource.java
source/fleet-service/README.md
source/fleet-service/pom.xml
source/fleet-service/src/main/java/com/milesofsmiles/fleet/AllocationInterval.java
source/fleet-service/src/main/java/com/milesofsmiles/fleet/FleetMigrationMain.java
source/fleet-service/src/main/java/com/milesofsmiles/fleet/FleetRepository.java
source/fleet-service/src/main/java/com/milesofsmiles/fleet/FleetStorageException.java
source/fleet-service/src/main/java/com/milesofsmiles/fleet/FleetStorageExceptionMapper.java
source/fleet-service/src/main/java/com/milesofsmiles/fleet/IntervalConflictException.java
source/fleet-service/src/main/java/com/milesofsmiles/fleet/SchemaGuard.java
source/fleet-service/src/main/java/com/milesofsmiles/fleet/Vehicle.java
source/fleet-service/src/main/java/com/milesofsmiles/fleet/VehicleResource.java
source/fleet-service/src/main/resources/application.properties
source/fleet-service/src/main/resources/db/migration/V1__fleet_records.sql
source/fleet-service/src/test/java/com/milesofsmiles/fleet/FleetApiIT.java
source/fleet-service/src/test/java/com/milesofsmiles/fleet/FleetUnitTest.java
source/fleet-service/src/test/java/com/milesofsmiles/fleet/PostgresResource.java
source/rental-service/pom.xml
source/rental-service/src/main/java/com/milesofsmiles/rental/ClockProducer.java
source/rental-service/src/main/java/com/milesofsmiles/rental/DraftRental.java
source/rental-service/src/main/java/com/milesofsmiles/rental/InvalidJsonMapper.java
source/rental-service/src/main/java/com/milesofsmiles/rental/RentalConflictException.java
source/rental-service/src/main/java/com/milesofsmiles/rental/RentalMigrationMain.java
source/rental-service/src/main/java/com/milesofsmiles/rental/RentalRepository.java
source/rental-service/src/main/java/com/milesofsmiles/rental/RentalRequest.java
source/rental-service/src/main/java/com/milesofsmiles/rental/RentalResource.java
source/rental-service/src/main/java/com/milesofsmiles/rental/RentalSchemaGuard.java
source/rental-service/src/main/java/com/milesofsmiles/rental/RentalStorageException.java
source/rental-service/src/main/java/com/milesofsmiles/rental/RentalStorageExceptionMapper.java
source/rental-service/src/main/java/com/milesofsmiles/rental/RequestExceptionMapper.java
source/rental-service/src/main/java/com/milesofsmiles/rental/StrictJsonCustomizer.java
source/rental-service/src/main/resources/application.properties
source/rental-service/src/main/resources/db/migration/V1__draft_rental.sql
source/rental-service/src/test/java/com/milesofsmiles/rental/PostgresResource.java
source/rental-service/src/test/java/com/milesofsmiles/rental/RentalApiIT.java
source/rental-service/src/test/java/com/milesofsmiles/rental/RentalUnitTest.java
```
