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
| Step | 1 - Reproducible booking baseline |
| Base revision | `93c882b27a44779bbf17bbf9fb4057ce09149a5f` (integrated Step 1; original chapter starts at accepted Step 0 `5e0c173a11d57b64d0864379e03824c282eb4543`) |
| Candidate | Maven-layout correction on `fix/maven-service-modules`; current source fingerprint and evidence in `docs/verification/step-1-verification.md` |
| Type | Corrective refactor, not a new chapter; deployment acceptance remains blocked |

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
| P-RECORDS | Service-owned records with isolated roles, versioned contracts, request hashes, interval constraints | Booking / Fleet / Rental | 2 | planned |
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
| P1-KIND | Existing/dedicated authorized kind path with HAProxy, cert-manager Issuer, nip.io/sslip.io routes, retained local volume | Local operator | `deploy/step-1/kind/`, kind scripts | blocked (no confirmed existing context for correction; earlier networking failure evidence is historical) |
| P1-ROTATION | Renew deployed server certificates and prove workloads use them after restart | Local operator | Compose renewal walkthrough; kind Certificate resources | blocked overall (correction isolated PKI check passes; deployed renewal unrun; earlier internal Compose observation is historical) |
