# Step 2 Blueprint: Service-Owned Records

**Status:** Approved by the user in this session before implementation.

## Accepted Predecessor and Authorization

The user declared Step 1 complete and requested Step 2 on a new branch, with
source/article verification and preservation of existing behavior.
`features/step-2` starts from accepted `development` at
`daed623a6c32383b96b402fe4bf27f22778b060a`, including the Maven-module correction.
The predecessor was accepted by the user; that does not turn its recorded
blocked deployment gates into verified results.

This blueprint expands [roadmap Step 2](../../plan.md#roadmap). Creating the local
branch is authorized. The user explicitly approved this blueprint and proceeding
with implementation under [AGENTS.md](../../AGENTS.md#chapter-workflow).
Initially no commit, push or PR was authorized. After reviewing the completed
candidate, the user requested a PR targeting `development` and, if the published
predecessor could not be amended safely, documentation and a blog explanation
for retaining it. This authorizes committing and pushing the Step 2 branch and
opening a draft PR with the remaining gates explicit; it does not authorize
rewriting `development`, merging, tagging, publication, cluster replacement or
persistent-data deletion.

The user permits attempting missing networking prerequisites and, if necessary,
using internal Kubernetes traffic and direct port-forwarding without HAProxy.
Treat this as permission to investigate a supported local path, not to disable
TLS verification, flush firewall rules, alter shared-host policy, or claim that
direct access verifies ingress. Confirm an existing context and chapter namespace
before applying resources. A new cluster requires explicit creation approval.

## Business Increment and Learning Outcomes

Alex's synthetic booking request gains durable, explicitly owned records.
Booking owns requests and their duplicate-request identity. Rental owns a
draft rental record, not an active rental. Fleet owns vehicle records and the
storage constraint preventing incompatible allocation intervals.

Readers will learn to separate service ownership from Maven modules, distinguish
migration identities from runtime identities, evolve the existing fixture
without data loss, version storage-facing HTTP contracts, and make duplicate
requests and interval conflicts explicit database-backed outcomes.

This is not search, quote, reserve, pickup, or a distributed transaction.
Persisting a draft or an allocation test record cannot establish eligibility,
inventory availability for sale, payment success, or permission to drive.

## Ownership and Module Delta

Retain the root Maven parent, wrapper, Java baseline, and existing booking
module. Add `source/rental-service/` and `source/fleet-service/`, each with its
own application dependencies, configuration, sources, tests, and container
artifact. Register both in the root reactor. Do not create a shared domain-model
module that allows one service to write another's records.

| Owner | Records | Authority and limits |
| --- | --- | --- |
| Booking | Existing fixture; versioned booking requests; request-key/hash association | Owns request persistence, not price, allocation, payment, or rental activation |
| Rental | Versioned draft rental with stable booking/vehicle references | Stores references without cross-database foreign keys; cannot become ACTIVE in this chapter |
| Fleet | Synthetic vehicle; allocation interval records with explicit blocking status | Enforces interval storage integrity; public hold acquisition/expiry/compensation remain Step 4 |

Use separate owner schemas and identities in one local PostgreSQL instance.
This is logical ownership isolation, not separate database-host failure domains.
Use an owner/migrator identity per schema and a different non-owner runtime
identity per service. Revoke default cross-owner schema/table access and PUBLIC
creation privileges in the lab database. Runtime identities receive only the
required DML and sequence privileges in their own schema, with no DDL, role
creation, superuser, or migration credentials.

Bootstrap credentials come from ignored, non-committed inputs. Migration jobs
use owner credentials and finish before runtime workloads start; application
containers must not receive those credentials. Bootstrap's elevated identity
is not a service identity. Test all ordered cross-owner access combinations
using actual runtime credentials, including attempted reads, writes, and DDL.

No LangGraph4j, graph state, checkpoints, model context, decision ledger,
OpenTelemetry, or caches are introduced. Persistent business records are not
graph progress or immutable audit evidence.

## Versioned Contracts and Duplicate Requests

Preserve `GET /api/bookings/{id}`, its fixture JSON, canonical UUID validation,
404 behavior, explicit storage-failure response, and health endpoints. Add
versioned contracts without silently moving or removing that reader path.

Proposed new public lab contracts:

| Surface | Outcome |
| --- | --- |
| `POST /api/v1/booking-requests` | Persist a validated synthetic request with a required request key; first creation returns 201 |
| `GET /api/v1/booking-requests/{id}` | Read the stored request, including its contract version |
| `POST /api/v1/rentals` | Persist a synthetic DRAFT referencing a booking and vehicle; never perform pickup or assert remote confirmation |
| `GET /api/v1/rentals/{id}` | Read the stored DRAFT and reference versions |
| `GET /api/v1/vehicles/{id}` | Read the seeded fleet-owned vehicle |

Define bounded fields, canonical UUIDs, UTC instants, explicit schema version,
stable error codes, and example JSON in checked-in contract documentation.
Required values, unsupported versions, unknown fields, and invalid intervals
must have explicit tested outcomes. These are unauthenticated loopback lab
contracts, not production tenant authentication. Do not accept a caller-supplied
tenant header and present it as authenticated scope.

Hash the canonical validated typed request, not the raw JSON byte sequence.
Specify field ordering, normalized UUID/instant representation, included
contract version, UTF-8 encoding, and SHA-256. Exclude transport-only metadata
and the request key itself. Do not log raw payloads or credentials.

Use a unique service-local request key and persist its hash with the record in
one database transaction. A duplicate key with the same semantic payload
returns the original record and an explicit replay response (200), without
inserting another row or changing its creation time. A different payload for the
same key returns 409. Database uniqueness, not an in-memory precheck, resolves
concurrent duplicate requests. Apply the same semantics to both new write
contracts, but keep schemas and ownership independent.

No cross-service call or orchestrated booking-to-rental write belongs in this
increment. References alone do not prove remote existence, current state, or
authorization; document that limit rather than inventing a consistency claim.

## Fleet Interval Storage Contract

Store UTC half-open windows `[start, end)` with `start < end`, a vehicle ID,
record ID, and an explicit blocking status. Use a PostgreSQL exclusion
constraint combining vehicle equality and range overlap, with the required
pinned-server extension installed by the fleet migrator.

Adjacent windows are allowed; overlapping blocking windows for the same
vehicle are rejected even under concurrent transactions. Different vehicles
do not conflict. Nonblocking historical/released records must not reserve
capacity. Test the exact status predicate and boundary instants.

Exercise allocation insertion through the owning repository and real database
tests; do not expose a public hold-acquisition API early. The chapter teaches
storage constraints, not the Step 4 hold lifecycle. There is no expiry timer,
maintenance-buffer policy, quote validity, confirmation, compensation, or
unknown-effect reconciliation.

## Migrations and Upgrade Safety

Introduce a maintained, exactly pinned migration library after checking its
PostgreSQL 17, Java 25, and Quarkus compatibility against release-specific
primary sources. Prefer the supported Flyway integration if compatibility
evidence passes; do not write a replacement migration framework.

Provide independent, immutable versioned migrations and history for each
schema. Do not run ad hoc `CREATE TABLE` under runtime credentials. Separate
fixture seeding through the booking runtime identity from schema creation, and
retain the injected clock and insert-only fixture semantics.

Test both clean installation and upgrade from the exact Step 1 table layout.
The upgrade must preserve every existing fixture row, including an intentionally
nondefault creation time and reference. Handle the legacy public-schema table
through an explicit owner-only migration path; do not silently enable
baseline-on-migrate, drop/recreate tables, overwrite existing fixtures, or
grant runtime access to public-schema bootstrap objects.

Repeat migration without data changes; reject checksum drift and an incompatible
schema version. Document migration failure and recovery without deleting
volumes. Do not promise automatic downgrade or distributed zero-downtime rollout.

## Dependencies and Compatibility

Keep Java 25 without preview APIs and the accepted Quarkus/Maven/PostgreSQL
pins unless a demonstrated incompatibility requires an approved change.
Record exact migration-library and extension versions, source/API references,
and executable observations before asserting compatibility.

Initial environment inspection found Java 27 on PATH and a running Docker
daemon. `kind`, `kubectl`, and `modprobe` were not found on PATH; `/lib/modules`
was absent. No running Docker containers were listed. These observations do not
prove kernel capability or the absence of clusters in other environments.

`./mvnw clean compile -q` failed at the Java enforcer because Java 27 is outside
`[25,26)`. Restore Java 25 before running the baseline and candidate gates;
never bypass the enforcer or change the series baseline to make a build pass.

Use mocks and Testcontainers as the standard verification path, independent of
kind, model credentials, cloud accounts, and paid inference.

## Threat and Failure Boundaries

Use synthetic data only. Bound request size and fields, validate before
hashing/persistence, parameterize SQL, and return explicit safe errors without
provider stack traces. Differentiate validation, missing rows, duplicate-payload
conflicts, interval conflicts, and storage unavailability; never turn dependency
failure into an empty result or fabricated success.

Runtime role tests establish the tested grants, not a production security
certification. A shared PostgreSQL administrator can still access all schemas.
Do not claim tenant isolation, authenticated ingress, mTLS, NetworkPolicy
enforcement, HA, payment safety, or immutable evidence from schema isolation.

Preserve the inherited HTTPS and JDBC server-trust checks. No `trust-all`,
insecure curl flags, or plaintext fallback is allowed merely to unblock a lab.

## Local Deployment Delta and Fallback

Add chapter-scoped `deploy/step-2/` resources, without replacing Step 1's
walkthrough or reusing its persistent database volume. Compose runs the three
non-root service containers, one non-HA PostgreSQL instance, and explicit
owner-scoped migration tasks. Pin images, bound resources, mount private
inputs read-only, publish only loopback service ports, and do not publish the
database port.

For an explicitly confirmed existing kind context, use only `mos-step-2`
workloads and namespaced inputs. Follow the existing plain-YAML/direct-kubectl
pattern, with startup sequencing, probes, scoped identities, disabled runtime
API-token mounts, and persistent database storage. Any cluster-scoped resource
or controller installation requires separate explicit authorization.

Investigate `ip_tables`, `iptable_nat`, `nf_conntrack`, and `xt_comment` support
only through available, compatible host facilities. Installing userspace
iptables/conntrack tools does not supply missing host kernel modules. Do not
download arbitrary kernel modules, simulate a passing preflight, flush rules,
or change the host security configuration.

If ingress/controller prerequisites fail but the pod network and application
dependencies work, exercise the application directly with foreground
`kubectl --context <confirmed-context> -n mos-step-2 port-forward` and a client
that still verifies the certificate hostname and CA. Use a direct database
endpoint only when needed to bypass broken Service routing, document its
certificate SAN and restart/address-change limitations, and demonstrate
application-to-database TLS and connectivity rather than assuming them.

Port-forwarding can bypass ingress and Service routing; it cannot by itself
repair pod-to-pod connectivity, database connectivity, or certificate issuance.
Record direct access, ClusterIP routing, and HAProxy/cert-manager separately.
A working fallback does not retroactively verify the blocked Step 1 ingress
promise. If neither safe deployment path works, retain the blocked status.

**Approved lab amendment:** After confirming there was no existing kind cluster
or Kubernetes context, the user explicitly authorized creation of a dedicated
`mos-step-2` cluster with an isolated kubeconfig to attempt direct port-forwarding
without HAProxy. This does not authorize host firewall changes or deletion of
existing cluster/persistent state.

After observing direct pod-to-database connection timeouts, the user explicitly
approved an additional single-node `hostNetwork` fallback: bind PostgreSQL and
service HTTPS listeners to node loopback, assign distinct service ports, and
use `kubectl port-forward`. Preserve normal pod-network and ingress gates as
blocked rather than presenting this fallback as their verification.

For each working path, show readiness, exact contract outcomes, rejection
cases, and a nonseeded marker/record retained across separate application and
database restarts. Keep volumes/PVCs and existing user data; never use
volume-removal cleanup. Local storage remains single-instance and non-HA.

## Tests and Acceptance Gates

Every feature requires mock-based unit tests and real PostgreSQL Testcontainers
integration tests, not just manifest review or SQL inspection.

| Gate | Required discriminating evidence |
| --- | --- |
| Baseline | Java 25 root `./mvnw clean compile` and `./mvnw verify` before changes; preserve original fixture/API/health/TLS behavior |
| Candidate reactor | The same two root commands build all three modules; report unit/integration counts and no silent skips |
| Owner isolation | All six ordered cross-owner pairs deny reads and writes; each runtime role rejects DDL and cannot assume a migrator role; own permitted operations succeed |
| Contracts | Versioned JSON, bounded validation, unsupported versions/unknown fields, 201/200 replay/409 conflict, 400/404/503 outcomes |
| Request identity | Canonically equivalent payloads replay; changed payload conflicts; concurrent same-key requests persist exactly one row; restart preserves identity |
| Migrations | Clean install; exact Step 1 upgrade preserving modified fixture values; repeat migration; checksum/incompatible-version failure |
| Intervals | Adjacent success; same-vehicle overlap rejection; different-vehicle success; invalid/zero windows rejected; status predicate and concurrent contenders |
| Failure handling | Dependency outage remains explicit; liveness/readiness distinction; failed transaction rollback; no raw provider-detail exposure |
| Compose | Pinned/non-root configuration, trusted endpoints, all new record paths, separate restarts and persistence without volume deletion |
| kind | Confirmed context/namespace; readiness, trusted direct endpoint and dependency connectivity, restart/persistence; distinguish fallback from ingress evidence |
| Documentation | Article claims/snippets/commands/outputs/links/diagrams and cumulative promises traced to one frozen candidate |
| Safety/review | Credential-pattern scan, input/role/secret/TLS/data-preservation review; report independent-review status honestly |

Missing prerequisites are blockers, not passes. Do not weaken an acceptance
check because a mocked test or internal-only observation is easier to obtain.
No load, performance, recovery-time, cross-platform, or live-model acceptance
target is inferred from the roadmap.

## Documentation, Promises, and Candidate Freeze

After approval, author `docs/blog/step-2-article.md` using the technical-writing
skill. Continue Alex's request-to-draft story and explain why records and
references are not confirmed business transitions. Show the ownership boundary,
schema upgrade, canonical hash input, duplicate/conflict examples, and adjacent
versus overlapping intervals with observed output only.

Update root deployment/build instructions, rebuild `docs/blog/index.md`
cumulatively, and expand `P-RECORDS` in `docs/promise-ledger.md` into stable Step 2
promises for ownership, migrations, contracts/hashes, and interval integrity.
Reverify affected Step 1 behavior while retaining its honest blocked ingress
status unless fresh evidence resolves it. Do not rewrite historical evidence
as though a Step 2 direct-port-forward observation occurred at Step 1.

Freeze base plus stable patch/tree identifier if committing is still
unauthorized, enumerate changed files, and record article-to-code evidence in
`docs/verification/step-2-verification.md`. Recheck affected gates after any
source, documentation, dependency, configuration, schema, or base change.

For this proposed blueprint alone, perform consistency/local-link checks,
credential-pattern scanning, whitespace checks, and manual editorial review.
These checks do not constitute implementation or runtime verification.

## Explicit Exclusions

No eligibility/pricing/policy rules, audit/telemetry implementation, public hold
lifecycle, search/quote/reserve orchestration, payment, pickup/return activation,
distributed transactions, graph/checkpoint/model dependencies, MCP, RAG,
caches, production authentication/authorization platform, HA/security
certification, Java 27 migration, cluster reset, deletion of persistent data,
release operations, or automatic Step 3 work.
