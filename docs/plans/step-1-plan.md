# Step 1 Blueprint: Reproducible Booking Baseline

**Status:** Approved by the user in this session before implementation.

## Accepted Predecessor and Authorization

The user confirmed Step 0 complete and authorized moving to Step 1. The accepted
starting snapshot is `development` at
`5e0c173a11d57b64d0864379e03824c282eb4543`. Work takes place on
`features/step-1`, created locally from that snapshot. Step 0 review findings are
not reopened or treated as blockers to this increment.

This blueprint expands [roadmap Step 1](../../plan.md#roadmap), not Step 2.
Branch creation and implementation authorization do not authorize commits,
pushes, pull requests, merges, tags, publication, cluster creation, or deletion
of persistent data. The user approved this blueprint and its implementation.

## Business Increment and Learning Outcomes

Alex can retrieve a synthetic booking-request fixture from a minimal API backed
by PostgreSQL. This is a reproducible technical baseline, not a reservation:
it does not establish eligibility, quote a price, allocate inventory, authorize
payment, or confirm a booking.

Readers will learn to build and run a Quarkus application on Java 25, distinguish
liveness from database-dependent readiness, use an injected clock and stable
synthetic fixture, test without live credentials, and demonstrate that the
fixture survives application and database restarts on local persistent storage.
They will also bootstrap a development-only certificate authority without
committing private keys or changing the machine's trust store.

## Ownership, API, and State Delta

One application owns a deliberately minimal, read-only booking-request fixture.
PostgreSQL holds its stable identifier, synthetic reference, and creation time.
Initialization is repeatable and never overwrites an existing fixture.

| Surface | Proposed contract |
| --- | --- |
| `GET /api/bookings/{id}` | Return the stored fixture as JSON; unknown identifiers return 404; malformed identifiers return 400 |
| `GET /q/health/live` | Report application liveness independently of database availability |
| `GET /q/health/ready` | Report readiness only when the database is usable |
| Clock | Inject `Clock`; deterministic fixtures/tests use a fixed UTC instant; ordinary execution uses UTC |
| Failure boundary | A database outage is an explicit unavailable/error outcome, never a fabricated successful fixture response |

Before this chapter there is no executable system. After it, business state is a
single synthetic fixture in PostgreSQL. No durable graph progress, model context,
decision ledger, telemetry pipeline, workflow, or LangGraph4j dependency is
introduced. Service ownership schemas, migrations, request hashes, interval
constraints, and public write APIs remain Step 2 or later.

## Dependencies and Compatibility Evidence

Use Java 25 without preview APIs, Quarkus REST JSON, JDBC PostgreSQL, application
health, JUnit, Mockito, REST API assertions, and Testcontainers PostgreSQL. A
Maven wrapper and all dependencies/plugins must have exact versions.

Before implementation relies on a component, consult its release-specific
primary documentation and artifacts. Record the exact JDK distribution/build,
Quarkus BOM, Maven/wrapper, test libraries, PostgreSQL image, application base
image, Compose, kind, and Kubernetes versions in the article and verification
record. Do not turn a guessed version into a compatibility claim.

The current shell exposes Temurin 27, not the required Java 25 runtime, and has
Docker and OpenSSL but no `mvn`, `kind`, or `kubectl` on PATH. These observations
are prerequisites to resolve, not permission to change the baseline to Java 27
or to skip deployment checks. Install/restore required tools only when the
corresponding command demonstrates they are missing.

No model providers, GPU, cloud account, paid inference, or external application
credentials are needed. Measure local resource use and record the actual lab
environment; do not invent performance or hardware acceptance results.

## Threat and Failure Boundaries

Use synthetic identifiers and data only. The local baseline is not an
authenticated customer-facing service and must be documented as such. Bind
published development ports to loopback and avoid exposing PostgreSQL publicly.
Reject malformed input, parameterize database access, and avoid leaking database
credentials or stack traces through API errors.

Credentials enter through non-committed local inputs; no secret defaults in
tracked files. Ignore generated secrets, certificates, and build output.
Run service containers as non-root with resource limits, health probes, and
minimal scoped identities.

Development PKI provides a local CA and server certificates with explicit SANs,
expiry, permissions, and documented trust inputs. Do not install the CA globally.
Wire and test TLS separately for HTTP and JDBC/PostgreSQL using hostname and CA
verification, not insecure trust-all flags. Test untrusted/expired or wrong-name
certificates and document renewal/restart behavior. Do not claim mTLS or
production PKI, and never commit generated private material.

## Local Deployment Changes

Provide a versioned application container and chapter-scoped resources for:

- Docker Compose: application and one PostgreSQL instance, loopback exposure,
  health/readiness, explicit secret/certificate inputs, and persistent volume.
- Existing kind: plain YAML and direct `kubectl`, isolated `mos-step-1`
  namespace, scoped service account, Services, Deployment, PostgreSQL storage,
  probes, resources, and externally supplied credentials/certificates.

Document build/load, apply order, readiness, port-forwarding, API invocation,
restart/persistence checks, certificate renewal, and data-preserving stop steps.
Keep application and PostgreSQL configuration consistent across the two paths
without claiming equivalent networking, storage, or availability.

Before any kind apply, confirm the user's selected existing kind context and
chapter namespace. Do not create/reset a cluster, switch contexts, delete PVCs,
or assume NetworkPolicy enforcement. Both local stateful paths are
single-instance and non-HA. Missing cluster access blocks the kind gate and
chapter verification; it is not a pass.

**Approved deployment amendment:** The user authorized creating a dedicated kind
cluster named `mos-step-1` and required HAProxy ingress, a cert-manager certificate
issuer, and allowed node-local persistent volumes. Use context
`kind-mos-step-1` explicitly without changing other contexts. Install the pinned
cert-manager controller/CRDs and chapter-scoped HAProxy controller permissions
needed for this dedicated lab. Issue certificates from a namespaced development
CA issuer. Use a retained local PV with node affinity; it survives pod restarts,
not deletion of the kind node or cluster. Cluster/controller installation is an
explicit exception to the otherwise existing-cluster, chapter-namespace-only
deployment rule for this authorized lab. No cleanup or data deletion is
authorized.

## Tests and Acceptance Gates

| Gate | Required evidence |
| --- | --- |
| Compatibility | Exact pinned matrix plus Java 25 compile and runtime observations |
| Build | `./mvnw clean compile` and `./mvnw verify` from repository root |
| Mock-based unit tests | Fixture mapping, injected clock, missing/invalid identifiers, initialization behavior, explicit dependency failures |
| Testcontainers integration | Real PostgreSQL fixture initialization and reads, repeated initialization, HTTP success/400/404, readiness and outage behavior, TLS verification |
| Compose | Health/readiness and documented endpoint; same stored fixture before/after application and PostgreSQL restarts without volume deletion |
| Existing kind | Confirmed context/namespace, ready workloads, documented endpoint, application/database restart persistence without deleting storage |
| PKI | Key permissions, SANs/validity, verified HTTP and JDBC TLS, invalid-certificate rejection, renewal/restart walkthrough |
| Documentation | Every command, snippet, output, link, diagram, prerequisite, and affected promise traced to a frozen candidate |
| Safety review | Credential-pattern scan and review of secret handling, local exposure, input validation, non-root execution, and storage preservation |

Standard Maven verification uses mocks and Testcontainers and requires neither
kind nor live model credentials. Deployment and PKI walkthroughs are separate
gates, not substitutes for those tests. Capture commands, exit statuses, actual
outputs, environment, and limitations. Do not label unavailable gates passed.

## Documentation and Candidate Freeze

After blueprint approval, add the application, Maven wrapper, tests, container
and chapter deployment resources, PKI tooling, and root `README.md` reader path.
Author `docs/blog/step-1-article.md` with the technical-writing skill. Update
`docs/blog/index.md` cumulatively without rewriting the accepted Step 0 article.
Update `docs/promise-ledger.md` for `P-BOOK-BASELINE` and new stable Step 1 promise
IDs, distinguishing delivered, blocked, failed, and unverified behavior.

Freeze the implementation using its base revision and a stable patch/tree
identifier if committing remains unauthorized. Enumerate changed files and
record claim-to-evidence results in
`docs/verification/step-1-verification.md` using article-to-code verification.
Rerun affected checks whenever the candidate or integration base changes.
Human approval, release operations, and any unavailable gates remain explicit.

## Explicit Exclusions

No Step 2 service records/migrations, eligibility/pricing rules, fleet holds,
payment operations, actual reservation confirmation, graph/model dependencies,
RAG, MCP, caches, audit/telemetry infrastructure, distributed coordination,
authentication platform, production HA/security claims, or Step 17 concurrency
APIs. No cluster or persistent-data reset and no automatic next chapter.
