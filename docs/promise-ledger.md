# Promise Ledger

This ledger distinguishes documented design intent from runtime evidence.
Step 0 is documentation-only: no runtime capability is verified by the
article. Future steps must update affected promises against an exact candidate
revision and record the actual commands, environment, and results.

| ID | Promise | Owner | Step | Article section | Source/evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| S0-DOC-01 | The happy-path walkthrough identifies the owner, authority, evidence, and unresolved outcome for consequential transitions. | Booking, fleet, rental, payment | 0 | [Happy path](blog/step-0-article.md#walk-the-happy-path-without-giving-the-model-authority) | `docs/plans/step-0-plan.md`; article walkthrough table | Verified at revision `90ce17d` — documentation coverage only |
| S0-DOC-02 | The ambiguous-return walkthrough separates lost-property handling, damage evidence, liability, settlement, and vehicle readiness. | Lost property/disputes, rental, payment, fleet | 0 | [Ambiguous return](blog/step-0-article.md#treat-the-ambiguous-return-as-a-separate-business-problem) | `docs/plans/step-0-plan.md`; article scenarios table | Verified at revision `90ce17d` — documentation coverage only |
| S0-DOC-03 | Business state, durable graph progress, ephemeral context, and decision evidence are not conflated. | Domain owners; graph/harness | 0 | [State boundaries](blog/step-0-article.md#keep-four-kinds-of-state-separate) | `plan.md` state and audit boundaries; article state table | Verified at revision `90ce17d` — documentation coverage only |
| S0-DOC-04 | A model proposal, checkpoint, or telemetry trace cannot authorize a consequential business decision. | Domain owners; graph/harness | 0 | [Decision ownership](blog/step-0-article.md#start-with-decision-ownership) | `plan.md` technical boundaries; article owner table | Verified at revision `90ce17d` — documentation coverage only |
| S0-DOC-05 | Unknown payment outcomes are reconciled, not blindly replayed. | Payment | 0 | [Failure exercise](blog/step-0-article.md#failure-exercise-stop-at-uncertainty) | `plan.md` effects and retries; article failure table | Planned — runtime behavior belongs to later steps |
| S0-DOC-06 | Changed or conflicting evidence cannot silently inherit an earlier approval or establish liability. | Rental; staff review | 0 | [Ambiguous return](blog/step-0-article.md#treat-the-ambiguous-return-as-a-separate-business-problem) | `plan.md` business authority; article scenarios table | Planned — runtime behavior belongs to later steps |
| S0-DOC-07 | Lost-property lookup does not itself authorize disclosure or outreach. | Lost property/disputes | 0 | [Ambiguous return](blog/step-0-article.md#treat-the-ambiguous-return-as-a-separate-business-problem) | `docs/plans/step-0-plan.md`; article scenarios table | Planned — runtime behavior belongs to later steps |
| S0-DOC-08 | Unresolved safety concerns keep a vehicle unavailable until authoritative readiness checks pass. | Fleet | 0 | [Ambiguous return](blog/step-0-article.md#treat-the-ambiguous-return-as-a-separate-business-problem) | `plan.md` business authority; article scenarios table | Planned — runtime behavior belongs to later steps |
| S0-DOC-09 | Required decision evidence survives telemetry sampling, export failure, or collector outage and can be correlated to traces when available. | Owning domain service; telemetry | 0 (boundary), 3 (initial implementation) | [State boundaries](blog/step-0-article.md#keep-four-kinds-of-state-separate); [failure exercise](blog/step-0-article.md#failure-exercise-stop-at-uncertainty) | `plan.md` audit and telemetry boundary; article state/failure tables | Planned — runtime behavior belongs to Step 3 and later |
| S0-DOC-10 | Runtime benchmark targets are not reported as measured outcomes without a named environment, workload, and results. | Chapter author/reviewer | 0 | [Benchmark worksheet](blog/step-0-article.md#proposed-acceptance-and-benchmark-worksheet) | `docs/plans/step-0-plan.md`; article benchmark section | Verified at revision `90ce17d` — documentation coverage only |

## Status vocabulary

- **Planned:** intended but not yet implemented.
- **Implemented but unverified:** present in the candidate, with required
  verification or review still outstanding.
- **Verified at revision:** demonstrated by named evidence at an exact
  candidate revision and environment.
- **Failed:** an applicable check ran and failed.
- **Blocked:** a required prerequisite or gate is unavailable.
- **Deferred:** explicitly outside the current step.

Documentation coverage does not verify the runtime promises in this ledger.
When a row becomes implemented or verified, replace the generic evidence
reference with exact source symbols, tests/commands, candidate revision,
observed result, and environment.
