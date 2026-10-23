# Step 0 Blueprint: Rental and Evidence Contract

**Status:** Proposed; awaiting human approval before chapter implementation.

## Starting Snapshot and Scope

The accepted starting point is the content-empty orphan `development` root
`f5fecbac8a063b9b59bacb22e87ead17a144dcc7`. It has no parents and no tracked
files. Work takes place on `features/step-0`.

The user authorized establishing this baseline and using feature branches with
pull requests targeting `development`. Those operations do not approve this
blueprint, chapter acceptance, merging, tagging, or proceeding to Step 1.

This proposal restores the current planning and skill documents byte-for-byte
from planning snapshot `91cd1eddabc7c716518117eb0d15fb5d420c8bd6`. It does not
inherit that snapshot's history or older drafts. In particular, `AGENTS.md`,
`plan.md`, `skills.md`, and `story.md` remain unchanged.

The approved roadmap's [Step 0](../../plan.md#roadmap) is documentation only:
define the rental lifecycle, decision ownership, evidence boundaries, threat
model, architecture alternatives, and proposed local benchmark targets.
There is no runnable application or runtime compatibility claim.

## Business Increment and Reader Outcomes

Continue [Alex's rental](../../story.md) as a reviewable contract rather than a
new feature demo. Follow search, quote, hold, reservation, pickup, return,
inspection, settlement, and vehicle readiness. Keep lost-property handling and
disputes distinct from damage assessment and payment authority.

After the chapter, a reader should be able to:

- Identify the owner, decision authority, evidence source, and failure outcome
  for every consequential transition in the two walkthroughs.
- Distinguish business records, workflow progress, temporary model context,
  and durable decision evidence.
- Explain why a model proposal, checkpoint, or trace cannot authorize payment,
  establish liability, or release an unsafe vehicle.
- Choose between plain rules, authorized human review, bounded model assistance,
  and a workflow engine based on the problem rather than novelty.
- Read benchmark targets as proposed acceptance criteria, not measured results.

## Ownership and Authority Contract to Document

These are logical boundaries, not a commitment to deploy one service per row.
Later chapters introduce concrete services only when justified.

| Boundary | Owns | Decision authority and evidence |
| --- | --- | --- |
| Booking | Requests, quotes, reservation status | Deterministic eligibility and versioned quote checks; fleet hold and payment outcome references |
| Fleet | Capacity, holds, allocation, readiness | Atomic availability rules and authorized inspection/maintenance records |
| Rental | Agreement, pickup, active rental, return, billing facts | Verified prerequisites, accepted agreement, condition evidence, authorized assessments |
| Payment | Authorization, capture, release, refund, outcome queries | Explicit authorized operation and provider outcome; stable operation identity |
| Policy and evidence | Applicable policy versions and inspection provenance | Access-controlled, dated evidence; conflicting evidence remains explicit |
| Staff review | Review decisions within authenticated roles and limits | Evidence-version-bound approval; changed evidence requires re-evaluation |
| Lost property and disputes | Separate cases linked to the rental | Verified ownership/contact permissions or approved adjustment; no automatic disclosure or charge |
| Graph and harness | Coordination and bounded proposals | Cannot override domain authority; budgets, schemas, permissions, and escalation remain external to models |

For each walkthrough decision, the article will record: synthetic case ID,
owning boundary, authorized actor or deterministic rule, prerequisites,
versioned evidence references, outcome or abstention, and a future acceptance
check. Concise grounds are appropriate; private chain-of-thought is not.

## Graph and State Delta

**Before:** An empty integration baseline; planning guidance describes the
destination but delivers no rental capability.

**After:** A documented lifecycle and authority model, not a LangGraph4j graph.
Any architecture diagram is explicitly conceptual. No graph nodes, reducers,
checkpoint schemas, APIs, migrations, or repositories are implemented.

The article will distinguish:

| State category | Purpose | Boundary to preserve |
| --- | --- | --- |
| Business state | Authoritative rental and operation facts | A graph or model cannot invent or overwrite a committed business outcome |
| Durable graph progress | Coordination position and references | Introduced at Step 5; not atomic with a remote effect |
| Ephemeral context | Bounded input for the current model task | Not a business record or reliable recovery mechanism |
| Audit evidence | Decision basis, versions, actor, outcome | Not replaced by sampled telemetry; correlation begins at Step 3 |

## Walkthroughs and Threat/Failure Boundaries

The happy path follows Alex from an eligible request through return, authorized
settlement, and independently verified readiness for the next renter. Each
transition names its authoritative prerequisite instead of claiming that AI
completes the transaction.

The ambiguous return follows the forgotten cellphone and discovered scratch.
An observation does not establish liability. Missing or conflicting condition
evidence goes to authorized review; lost-property access does not grant contact
or disclosure permission; an unresolved safety concern keeps the car unavailable.

The failure exercise will include an unknown payment outcome, changed evidence
after approval, malicious text in retrieved/tool content, and lost telemetry.
For each, document the prohibited inference, safe stop or reconciliation path,
owner, and future check. Step 0 demonstrates reasoning through these scenarios,
not executed recovery.

All cases use synthetic data. No credentials, real identity or licence data,
payment details, or private reasoning enter examples. Models and tool results
cannot grant permissions; specialist agreement is not calibrated confidence.

## Alternatives and Dependencies

Compare deterministic rules for eligibility and money; human review for
uncertain liability and safety; bounded model proposals for evidence summaries;
and a workflow engine for durable coordination when it fits better. Explain
LangGraph4j's intended coordination role without assuming feature parity with
other graph frameworks or claiming a tested library version.

Step 0 needs only Git and a Markdown reader. No JDK, Maven, Docker, kind,
database, model download, live credentials, or paid inference is required.
Java 25 remains the planned code baseline, not a verified Step 0 dependency.

Hardware, provider, synthetic policy details, and runtime performance thresholds
remain open decisions from [the series plan](../../plan.md#open-decisions).
They do not block a documentation proposal but must be resolved before affected
runtime work or achievement claims.

## Local Benchmark Proposal

The article will define a proposed acceptance worksheet, not fabricate numbers.
Its documentation-review targets are complete owner/authority/evidence/check
coverage for both walkthroughs, zero model-authorized consequential decisions,
and explicit safe outcomes for all listed failure scenarios.

The runtime worksheet will require an agreed named environment and workload,
versions, warm-up/repetition policy, deterministic or stochastic classification,
and measurements for latency percentiles, throughput/concurrency, recovery time,
cost/budgets, task quality, false approvals, and abstention. Numerical runtime
thresholds and machine budgets remain pending human agreement. No runtime target
can be reported as achieved in this chapter.

## Planned Files After Blueprint Approval

| Path | Purpose |
| --- | --- |
| `docs/blog/step-0-article.md` | Original teaching narrative, conceptual diagrams, decision contract, walkthroughs, alternatives, failure exercise, and limitations |
| `docs/blog/index.md` | Cumulative index containing only the delivered Step 0 article |
| `docs/promise-ledger.md` | Stable promise IDs, ownership, article/evidence references, candidate and status; clearly separate design review from future runtime delivery |
| `docs/verification/step-0-verification.md` | Frozen-candidate claim-to-evidence record, commands/results, gaps, and review status |

Use the [technical-writing skill](../../.agents/skills/technical-writing/SKILL.md)
when drafting the article and the
[article-to-code-verification skill](../../.agents/skills/article-to-code-verification/SKILL.md)
for final candidate review. Recheck affected evidence after any relevant change.

## Verification and Acceptance

For this proposed blueprint, inspect consistency with the protected guidance,
check local Markdown links and whitespace, scan the candidate documents for
credential patterns, and review wording for unsupported implementation claims.
Compare the restored guidance bytes with the planning snapshot before committing.
Record the exact committed candidate in the draft PR.

After approval and article work, freeze an exact candidate and verify every
claim, snippet, link, diagram, prerequisite, scenario outcome, and ledger entry.
Both walkthroughs must meet the documentation coverage targets. Runtime promises
remain planned or deferred, never verified by prose. Human approval and any
unavailable verification remain explicitly pending.

There are no unit or Testcontainers tests in Step 0 because there is no code.
Maven compile/verify, deployment checks, runtime security tests, and model
evaluations are not applicable; do not add tooling merely to validate prose.
Document-level credential scanning is not a runtime security audit.

## Deployment Changes and Explicit Exclusions

No deployment changes: no clusters, namespaces, containers, secrets, persistent
data, storage, certificates, ports, or external effects are created or modified.

Exclude all Step 1+ code and dependencies, executable policy, actual API and
database contracts, deployment manifests, live models, retrieval, MCP servers,
graph persistence, caches, telemetry infrastructure, and performance claims.
Do not invent policy amounts, legal rules, liability decisions, or model choices.

## Approval and Release Boundary

The draft PR requests approval of this blueprint only; it is not a completed
Step 0 delivery or a request to merge the planning-only candidate. After explicit
blueprint approval, complete Step 0 on this feature branch and repeat its gates
before marking the PR ready. Do not begin Step 1 automatically.

Merge, tag, and publication require separate authorization. If later authorized,
the Step 0 milestone name is `step-00`, only on the verified integration merge
commit; do not create or move a tag during blueprint preparation.
