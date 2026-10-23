# Step 0 Verification

## Candidate and scope

- **Step:** 0, documentation-only.
- **Base:** `6bd4aefe59e82954f5b38ce099be315072e3ed60`, the merged Step 0
  blueprint on `development`.
- **Article/evidence candidate:** `90ce17dfed6ff5115f71f3209da8d80f2c2cb706`.
- **Candidate working tree:** clean when frozen.
- **Files reviewed at that candidate:** `docs/blog/step-0-article.md`,
  `docs/blog/index.md`, and `docs/promise-ledger.md`.
- **Scope:** verify the article's design claims against `plan.md` and the
  approved `docs/plans/step-0-plan.md`. No runtime behavior is claimed.

## Findings and claim evidence

| Article claim | Evidence in candidate | Result and limit |
| --- | --- | --- |
| Domain owners retain eligibility, pricing, inventory, payment, liability, permission, and safety authority; models make proposals only. | `plan.md:21-26`; `docs/plans/step-0-plan.md:49-63`; article section [Start with decision ownership](../blog/step-0-article.md#start-with-decision-ownership) | Consistent as a design contract. No implementation is present or tested. |
| The article distinguishes business state, graph progress, temporary context, and decision evidence. | `plan.md:27,29`; `docs/plans/step-0-plan.md:74-81`; article section [Keep four kinds of state separate](../blog/step-0-article.md#keep-four-kinds-of-state-separate) | Consistent. The diagram is conceptual, not a deployed topology. |
| Happy-path decisions and ambiguous-return scenarios name synthetic cases, prerequisites, authority, versioned evidence references, outcome/abstention, and future checks. | `docs/plans/step-0-plan.md:60-63,85-99`; article sections [Walk the happy path](../blog/step-0-article.md#walk-the-happy-path-without-giving-the-model-authority) and [Treat the ambiguous return](../blog/step-0-article.md#treat-the-ambiguous-return-as-a-separate-business-problem) | Documentation coverage is present. The case IDs and evidence references are explanatory placeholders, not implemented schemas or real records. |
| Unknown payment effects are reconciled; changed evidence, malicious tool/retrieved content, and lost telemetry do not grant authority or erase evidence. | `plan.md:23-30`; `docs/plans/step-0-plan.md:90-103`; article section [Failure exercise](../blog/step-0-article.md#failure-exercise-stop-at-uncertainty) | Correctly stated as future design/test expectations. No recovery or security test ran. |
| Step 0 has no runtime prerequisites, version matrix, or measured benchmark result. | `docs/plans/step-0-plan.md:105-134,164-167`; `plan.md:56`; article sections [Starting point and prerequisites](../blog/step-0-article.md#starting-point-and-prerequisites) and [Proposed acceptance and benchmark worksheet](../blog/step-0-article.md#proposed-acceptance-and-benchmark-worksheet) | Consistent. Numerical runtime thresholds remain open; no benchmark was run. |
| Step 1 is the next planned increment, not part of Step 0 delivery. | `plan.md:56-57,144-153`; `docs/plans/step-0-plan.md:174-188`; article [Handoff](../blog/step-0-article.md#handoff) | Consistent. Step 1 implementation remains gated on its own approved blueprint and compatibility evidence. |

## Checks and results

- `git diff --check` — passed for the candidate changes.
- Local Markdown path/anchor and table-column validation using a one-off
  Python standard-library check — passed for all three candidate documents.
- Credential-pattern secret scan — no secrets detected in the candidate
  documents.
- Restored guidance comparison — passed. Git blob IDs for `AGENTS.md`,
  `plan.md`, `skills.md`, `story.md`, and both skill files match between
  planning snapshot `91cd1eddabc7c716518117eb0d15fb5d420c8bd6` and blueprint
  merge `6bd4aefe59e82954f5b38ce099be315072e3ed60`.
- Editorial/source review — performed against the technical-writing and
  article-verification guidance; no unsupported runtime-success claim found.
- Mermaid rendering — not run; a Mermaid renderer was unavailable. The diagram
  is marked conceptual and its syntax was inspected but not rendered.
- Unit, Testcontainers, Maven, deployment, model, and performance checks — not
  applicable to this documentation-only step; no such result is claimed.

## Ledger and acceptance status

`S0-DOC-01` through `S0-DOC-04` and `S0-DOC-10` are marked verified at the
article/evidence candidate revision for documentation coverage only.
`S0-DOC-05` through `S0-DOC-09` remain planned runtime promises.

This verification establishes consistency and coverage of the Step 0
documentation, not runtime safety, security, resilience, correctness, or
performance. Final human chapter acceptance and any release action remain
separate. Do not begin Step 1 until Step 0 is accepted and a Step 1 blueprint is
approved.
