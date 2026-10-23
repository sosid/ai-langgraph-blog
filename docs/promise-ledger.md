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
| Step | 0 — Rental and Evidence Contract |
| Base revision | `f5fecbac8a063b9b59bacb22e87ead17a144dcc7` (empty `development` root) |
| Candidate | Step 0 documentation set on `features/step-0` (see Step 0 verification record) |
| Type | Documentation only — no code, build, deployment, or runtime claim |

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
| P-BOOK-BASELINE | Reproducible booking baseline: clean compile/verify, both deployment paths, restart/persistence | Booking | 1 | planned |
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
