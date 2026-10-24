# Promise-to-Evidence Ledger

This ledger tracks the promises the series makes, who owns them, where they are
introduced, and their current evidence status. It is maintained during authorized
chapter work per [`plan.md`](../plan.md#promise-to-evidence-contract).

## Status vocabulary

- **planned** — described as intended behavior; no implementation yet.
- **implemented but unverified** — code exists; required checks not yet run/passed.
- **verified at revision** — demonstrated against a named frozen candidate revision.
- **failed** — a required check ran and did not pass.
- **blocked** — a prerequisite or decision is unresolved.
- **deferred** — intentionally postponed to a later authorized step.

Targets, design reviews, mock-only observations, and unavailable live checks are
**not** demonstrated runtime guarantees. Each chapter reverifies affected
cumulative promises, not only its own new behavior. Any change to prompts,
policies, dependencies, schemas, or base invalidates relevant evidence.

## Candidate

| Field | Value |
| --- | --- |
| Step | 2 - Service-owned records |
| Base revision | `daed623a6c32383b96b402fe4bf27f22778b060a` (accepted Step 1 plus Maven-module correction) |
| Candidate | Local `features/step-2`; runtime fingerprint and evidence in `docs/verification/step-2-verification.md`; final full-tree identifier supplied in handoff |
| Type | Approved chapter implementation; normal networking deployment gates remain blocked |

## Step 0 promises (delivered in this chapter)

These are documentation-review promises. Their evidence is the Step 0 article and
its verification record, not runtime behavior.

| ID | Promise | Owning boundary | Step | Article section | Evidence reference | Status |
| --- | --- | --- | --- | --- | --- | --- |
| P0-CONTRACT | Every consequential transition in both walkthroughs has a named owner, authority, evidence source, and failure outcome | Graph and harness (documentation) | 0 | The cast; Walkthrough 1; Walkthrough 2 | `docs/blog/step-0-article.md`; `docs/verification/step-0-verification.md` | verified at revision (documentation review) |
| P0-STATE | Four kinds of memory (business state, durable graph progress, ephemeral context, audit evidence) are distinguished | Graph and harness (documentation) | 0 | Four kinds of memory | `docs/blog/step-0-article.md` | verified at revision (documentation review) |
| P0-NO-MODEL-AUTH | No model proposal, checkpoint, or trace authorizes payment, liability, or vehicle release | All boundaries (documentation) | 0 | The cast; Walkthrough 2; Privacy, safety, and claim discipline | `docs/blog/step-0-article.md` | verified at revision (documentation review) |
| P0-FAILURE | Each listed failure scenario has a prohibited inference and a safe stop/reconciliation path | Owner per scenario (documentation) | 0 | The failure exercise | `docs/blog/step-0-article.md` | verified at revision (documentation review) |
| P0-SYNTHETIC | Examples are synthetic; no secrets, real PII, or private chain-of-thought | Privacy (documentation) | 0 | Privacy, safety, and claim discipline | `docs/verification/step-0-verification.md` (credential scan) | verified at revision (documentation review) |
| P0-BENCHMARK | Benchmark worksheet is defined as proposed acceptance criteria, not reported as achieved | Graph and harness (documentation) | 0 | The benchmark worksheet | `docs/blog/step-0-article.md` | verified at revision (documentation review) |

## Future runtime promises (opened, not yet delivered)

These are the consequential runtime promises Step 0 names so later chapters can be
held to them. All are **planned** or **deferred** until their introducing step is
authorized, implemented, and verified. Step 0 prose does not and cannot verify
them.

| ID | Promise | Owning boundary | Introducing step | Status |
| --- | --- | --- | --- | --- |
| P-BOOK-BASELINE | Reproducible booking baseline: clean compile/verify, both deployment paths, restart/persistence | Booking | 1 | blocked (build/tests pass; host-published Compose and kind end-to-end gates unavailable) |
| P-RECORDS | Service-owned records with isolated roles, versioned contracts, request hashes, interval constraints | Booking / Fleet / Rental | 2 | verified at revision for source/test and node-local fallback scope; normal deployment coverage separately blocked below |
| P-RULES | Deterministic eligibility/pricing replay same evidence+rule version to same outcome; model cannot weaken a rule | Booking / Policy and evidence | 3 | planned |
| P-AUDIT-CORRELATION | OpenTelemetry correlation with durable decision evidence; audit survives sampling/export loss | Audit evidence | 3 (extended through 27) | planned |
| P-HOLDS | Atomic holds, expiry, quote validity, idempotency, compensation, outcome queries | Fleet / Booking | 4 | planned |
| P-GRAPH-DURABLE | Durable PostgreSQL graph checkpoints; crash/resume; saver is not distributed fencing | Graph and harness | 5 | planned |
| P-TYPED-PROPOSALS | Models produce typed proposals; malformed/refusal/injection/timeout/budget fail safely | Graph and harness | 6 | planned |
| P-PROGRESS | Progress is distinct from committed facts; reconnect/replay without repeated business effect | Graph and harness | 7 | planned |
| P-RAG | Versioned policy RAG filtered by tenant/date/jurisdiction/version; citations resolve to exact evidence | Policy and evidence | 8 | planned |
| P-CONFIDENCE | Calibrated confidence evaluation; high confidence cannot bypass required review | Graph and harness | 9 | planned |
| P-PAYMENT | Confirm without double charging; reconcile unknown outcomes rather than replay | Payment | 10 | planned |
| P-CACHE | Bounded read caches; cache loss never loses business state or graph progress; writes never cached | Graph and harness | 11 | planned |
| P-MCP-CRM | CRM over MCP with scoped identity; descriptions/results are untrusted data | Lost property and disputes | 12 | planned |
| P-MCP-LOSTPROP | Legacy lost-property over a second MCP server; separate scopes; only authorized idempotent ops | Lost property and disputes | 13 | planned |
| P-PICKUP | Pickup becomes ACTIVE only after authoritative prerequisites | Rental / Fleet | 14 | planned |
| P-PARALLEL | Supervisor/parallel specialists/nested subagents with typed joins, bounded fan-out, isolation | Graph and harness | 15 | planned |
| P-REVISE | Bounded reviewer/reviser loop; cannot release unsafe vehicle or impose liability | Staff review / Graph and harness | 16 | planned |
| P-JAVA27 | Conditional Java 27 concurrency migration; unavailable prerequisites block it | Graph and harness | 17 | deferred |
| P-HUMAN-REVIEW | Durable review queue survives deployment; evidence-version-bound decisions | Staff review | 18 | planned |
| P-ROUTING | Resilience and adaptive routing; routing cannot weaken controls | Graph and harness | 19 | planned |
| P-SETTLEMENT | Close the money loop with exact-decimal totals and reconciled ledger/invoice | Rental / Payment | 24 | planned |
| P-DISPUTES | Disputes and late evidence as separate cases preserving original history | Lost property and disputes | 25 | planned |
| P-OBSERVE | Observe and reconstruct failure across services/restarts; audit survives collector outage | Audit evidence | 27 | planned |

New promises are added as later steps are authorized. Statuses move only when a
named frozen candidate provides the corresponding evidence.

## Step 1 promise breakdown

These historical observations describe the accepted predecessor and its named
fingerprints, not current implementation details. Step 2 reverifies the legacy
API/clock/failure/trust behavior while replacing runtime schema creation.

Step 0 remains accepted by the user. No Step 0 runtime promise is retroactively
verified by this increment. The fixture is a synthetic request, not a confirmed
reservation. The Step 1 source fingerprint, observed toolchain, commands, and
limitations are recorded in
[`verification/step-1-verification.md`](verification/step-1-verification.md).

| ID | Promise | Owner | Source / check | Status |
| --- | --- | --- | --- | --- |
| P1-BUILD | Java 25 pinned baseline compiles and passes standard verification without model credentials or kind | Booking | Root parent/aggregator `pom.xml`, `source/booking-service/pom.xml`, Maven wrapper; root and selected-module verification | verified at revision (current correction fingerprint in Step 1 record) |
| P1-FIXTURE | Read a stable synthetic request; deterministic initialization preserves existing fixture | Booking | `BookingRepository`, `FixtureInitializer`, `BookingBaselineTest`, `BookingApiIT` | verified at revision (source fingerprint in Step 1 record) |
| P1-FAILURE | Malformed/missing requests are explicit; dependency failure is 503, readiness down and liveness up; recovery restores reads | Booking | `BookingResource`, exception mapper, datasource health; mock and PostgreSQL outage tests | verified at revision (source fingerprint in Step 1 record) |
| P1-TLS | Verify application HTTPS and JDBC server TLS; reject untrusted certificates | Booking | `BookingApiIT`, `HealthProbe`, `scripts/check-pki.sh` | verified at revision (current correction tests; internal Compose evidence is historical only) |
| P1-PKI | Local development CA/leaf bootstrap, private-file permissions, hostname/expiry rejection, CA-preserving renewal | Local operator | `scripts/pki.sh`, `scripts/check-pki.sh` | verified at revision (isolated local checks; deployed rotation not yet verified) |
| P1-COMPOSE | Documented host endpoint and restart/persistence walkthrough | Local operator | `deploy/step-1/compose.yaml`, smoke/persistence scripts | blocked (correction config/image build pass; runtime unrun without authorization to create persistent lab state; earlier internal observations are historical) |
| P1-KIND | Existing/dedicated authorized kind path with HAProxy, cert-manager Issuer, nip.io/sslip.io routes, retained local volume | Local operator | `deploy/step-1/kind/`, kind scripts | blocked (newly authorized correction cluster Ready and booking image imported; networking preflight and kube-proxy fail on missing kernel comment match before chapter deployment) |
| P1-ROTATION | Renew deployed server certificates and prove workloads use them after restart | Local operator | Compose renewal walkthrough; kind Certificate resources | blocked overall (correction isolated PKI check passes; deployed renewal unrun; earlier internal Compose observation is historical) |

## Step 2 promise breakdown

All references below are tied to the runtime fingerprint and command evidence
in [the Step 2 verification record](verification/step-2-verification.md).
The Step 2 article sections are the claim inventory; the contract document
defines versioned JSON, canonical hash input, bounds and error outcomes.
No model, graph, audit or Step 3 behavior is delivered.

| ID | Promise / owning boundary | Article section | Source / evidence | Status |
| --- | --- | --- | --- | --- |
| P2-OWNERS | Only owner-scoped record DML; runtime cannot DDL, assume migrator or read history / each service | Three owners; Migration authority | `deploy/step-2/bootstrap.sql`, grants in owner migrations; `BookingRecordsIT`; deployed `step-2-isolation.sh` across all six ordered pairs | verified at revision (actual runtime credentials in tests, Compose and kind fallback) |
| P2-MIGRATIONS | Clean installation, exact Step 1 upgrade preserving altered fixture, no-op repeat, drift/mismatch rejection / owning migrator | Migration authority | `BookingBootstrapIT`, `BookingUpgradeIT`, owner migration/schema guard tests | verified at revision (Testcontainers; no live upgrade of user-owned Step 1 data claimed) |
| P2-CONTRACTS | Bounded, strict, versioned requests and reads; stable safe outcomes / HTTP owners | A retry; Run the candidate | `docs/contracts/step-2-http.md`; owner unit/API tests; deployed smoke | verified at revision (synthetic unauthenticated local scope) |
| P2-HASHES | Canonical typed hashes; same-key replay, changed-payload conflict, concurrent exactly-one row / Booking and Rental | A retry | `BookingSubmission`, `BookingRecordRepository`, `RentalRequest`, `RentalRepository`; mock/PostgreSQL concurrent tests and deployed replay checks | verified at revision (single database transaction per owner, no distributed effect guarantee) |
| P2-INTERVALS | Half-open adjacency and blocking overlap integrity / Fleet | Adjacent is not overlapping | `V1__fleet_records.sql`, `AllocationInterval`, `FleetRepository`; mock and Testcontainers boundary/concurrency/status/rollback tests | verified at revision (repository storage contract; public hold lifecycle deferred to Step 4) |
| P2-LEGACY | Existing fixture/API/clock/trust/outage behavior preserved / Booking | Migration authority; Failure lab | Original `BookingBaselineTest`, `BookingApiIT`; upgrade tests; deployed fixture smoke | verified at revision (startup DDL intentionally replaced by owner migrations) |
| P2-COMPOSE-INTERNAL | Trusted internal endpoints, distinct roles, record/marker persistence / Local operator | Run the candidate; Failure lab | Internal `step-2-smoke.sh`, `step-2-isolation.sh compose`, `step-2-persistence.sh compose` | verified at revision (internal network only) |
| P2-COMPOSE-HOST | Normal host-published Compose walkthrough / Local operator | What the lab demonstrates | `curl`/normal smoke against loopback 8543 | blocked (published transport times out despite healthy services) |
| P2-KIND-HOST | Explicitly approved single-node loopback hostNetwork+port-forward fallback / Local operator | What the lab demonstrates; Failure lab | Chapter renderer/apply script, trusted endpoint/role checks and separate app/database replacement with retained marker/records | verified at revision (not CNI/ClusterIP/ingress/cross-node evidence) |
| P2-KIND-POD | Normal direct pod-to-database connectivity / Local operator | What the lab demonstrates | Ready database; normal migration Jobs fail with connection timeouts; kube-proxy comment-match failure | blocked (missing host kernel support; failed Jobs retained) |
| P2-FUTURE | Rules, audit correlation, public hold lifecycle, graph/model authority | Closing handoff | Roadmap Steps 3 onward | deferred (not implemented in this chapter) |
