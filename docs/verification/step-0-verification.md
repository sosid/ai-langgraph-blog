# Step 0 Verification Record — Rental and Evidence Contract

This is the claim-to-evidence record for Step 0, produced with the
[article-to-code-verification skill](../../.agents/skills/article-to-code-verification/SKILL.md).
Step 0 is documentation only, so the applicable gates are consistency, local
links, whitespace, credential scanning, and editorial review — **not** build,
runtime, deployment, or model-evaluation gates.

## Frozen candidate

| Field | Value |
| --- | --- |
| Step | 0 — Rental and Evidence Contract |
| Base revision | `f5fecbac8a063b9b59bacb22e87ead17a144dcc7` (content-empty `development` root) |
| Approved blueprint | `docs/plans/step-0-plan.md` |
| Candidate files | `docs/blog/step-0-article.md`, `docs/blog/index.md`, `docs/promise-ledger.md`, `docs/verification/step-0-verification.md` |
| Candidate identity | The commit that adds the four files above on `features/step-0`; recorded in the PR targeting `development`. Rerun these checks if any candidate file changes. |
| Type | Documentation only — no code, build, deployment, or runtime claim |

Per the verification skill, a commit never gets created solely to satisfy this
workflow; the candidate is identified by its base commit plus the enumerated
changed files, and finalized by the integration commit referenced in the PR.

## Applicable vs. not-applicable gates

| Gate | Applicable? | Reason |
| --- | --- | --- |
| Consistency with protected guidance | Yes | Article/ledger must match `plan.md`, blueprint, `story.md` |
| Local Markdown links and anchors | Yes | New files reference `plan.md`, `story.md`, and each other |
| Whitespace hygiene | Yes | Trailing whitespace / tab check |
| Credential / secret scan | Yes | Document-level scan of candidate files |
| Editorial review | Yes | No unsupported implementation claims |
| `./mvnw clean compile` / `./mvnw verify` | No | No Maven project exists in Step 0 |
| Unit (mock) / Testcontainers tests | No | No code exists |
| kind / Compose deployment smoke | No | No deployment resources exist |
| Live-model evaluation | No | No model is used |

## Checks run and results

### 1. Consistency with protected guidance

- Ownership table in the article matches the blueprint's "Ownership and Authority
  Contract" (`docs/plans/step-0-plan.md`) row for row (Booking, Fleet, Rental,
  Payment, Policy and evidence, Staff review, Lost property and disputes, Graph and
  harness). **Consistent.**
- Four-state memory table matches the blueprint's "Graph and State Delta" table
  (business state, durable graph progress, ephemeral context, audit evidence),
  including "durable graph progress introduced at Step 5" and "audit correlation
  begins at Step 3." **Consistent.**
- Walkthroughs follow `story.md`: eligibility/quote, hold, reservation + payment,
  pickup→ACTIVE, return/settlement/readiness (happy path); forgotten cellphone and
  scratch (ambiguous return). **Consistent.**
- Future acceptance checks cite the correct introducing steps from the
  [roadmap](../../plan.md#roadmap) (Steps 3, 4, 10, 14, 24, 15/16, 26, 18, 8/12/13,
  27). **Consistent.**
- Benchmark section reports targets only and defers numeric thresholds to the
  [open decisions](../../plan.md#open-decisions). **Consistent.**

### 2. Local links and anchors

All local link targets resolve from their file locations:

- `docs/blog/step-0-article.md` → `../../story.md`, `../../plan.md`,
  `../verification/step-0-verification.md`, `../promise-ledger.md` — **resolve.**
- `docs/blog/index.md` → `step-0-article.md`, `../../story.md`, `../../plan.md`,
  `../promise-ledger.md`, `../verification/` — **resolve.**
- `docs/promise-ledger.md` → `../plan.md` — **resolves.**

Anchor targets verified against `plan.md` headings: `#roadmap` (line 48),
`#technical-boundaries` (line 19), `#open-decisions` (line 166),
`#promise-to-evidence-contract` (line 99). **All resolve.**

### 3. Whitespace hygiene

Trailing-whitespace and tab scan over the candidate files: **none found.**

### 4. Credential / secret scan

Secret scan over `docs/blog/step-0-article.md`, `docs/blog/index.md`, and
`docs/promise-ledger.md`: **no secrets detected.** This is a document-level scan,
not a runtime security audit.

### 5. Editorial review

- No use of "production-ready," "self-healing," "secure," or "exactly-once" as
  unqualified claims.
- All scenarios use synthetic case IDs (`CASE-HP-*`, `CASE-AR-*`); no real PII,
  credentials, or private chain-of-thought.
- Model is consistently described as a proposer that cannot acquire authority;
  checkpoints are described as non-atomic with remote effects; telemetry is
  distinguished from audit evidence; citations are described as evidence pointers,
  not proofs. **No unsupported implementation claims found.**

## Promise coverage

Step 0 promises `P0-CONTRACT`, `P0-STATE`, `P0-NO-MODEL-AUTH`, `P0-FAILURE`,
`P0-SYNTHETIC`, and `P0-BENCHMARK` are **verified at revision** at the
documentation-review level (see `../promise-ledger.md`). All future runtime
promises remain **planned** or **deferred**; no prose in this chapter verifies any
runtime behavior.

## Limitations and gaps

- This record establishes documentation quality only. It does not establish any
  runtime, build, deployment, or model behavior, because none exists in Step 0.
- Human reviewer approval and merge/tag/publication authorization are separate
  actions tracked in the PR, not granted by this record.
- Any change to the candidate files, `plan.md`, the blueprint, or `story.md`
  invalidates the affected checks above and requires a rerun against the new
  frozen candidate.

## Status

Documentation gates applicable to Step 0: **passed.** Runtime/build/deployment/
model gates: **not applicable.** Candidate is ready for human editorial review and
the authorized PR to `development`.
