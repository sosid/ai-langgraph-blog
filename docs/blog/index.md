# Miles of Smiles — Chapter Index

*Building Auditable AI Systems with LangGraph4j.* This index is cumulative: it
lists only chapters that have been delivered in the repository. Planned steps from
the [roadmap](../../plan.md#roadmap) appear here as they are authorized, written,
and verified — not before.

Read [`story.md`](../../story.md) for the customer journey and
[`plan.md`](../../plan.md) for the full series contract.

## Delivered chapters

| Step | Title | Type | Article | Status |
| --- | --- | --- | --- | --- |
| 0 | The Rental and Evidence Contract | Documentation only | [`step-0-article.md`](step-0-article.md) | Accepted by the user |
| 1 | A Booking Request You Can Reproduce | Java / PostgreSQL baseline | [`step-1-article.md`](step-1-article.md) | Accepted by the user; historical deployment gaps retained |
| 2 | A Record Has an Owner | Service records / migration / constraints | [`step-2-article.md`](step-2-article.md) | Implementation candidate; node-local kind fallback demonstrated; normal deployment gates blocked |

## How to read a chapter

Each chapter article names its exact starting snapshot, prerequisites, and — from
Step 1 onward — a pinned version matrix and reproducible commands. Promises opened
by each chapter are tracked in [`../promise-ledger.md`](../promise-ledger.md), and
each chapter's claim-to-evidence record lives under
[`../verification/`](../verification/).

Step 0 is documentation only: it defines ownership, authority, evidence, and
failure outcomes for the rental lifecycle. It contains no code, no build, and no
runtime claims.

Executable projects use Maven modules under `source/<service-name>/`. Step 1
introduced [`booking-service`](../../source/booking-service/pom.xml); Step 2 adds
[`rental-service`](../../source/rental-service/pom.xml) and
[`fleet-service`](../../source/fleet-service/pom.xml).
The [root build guide](../../README.md#maven-service-layout) explains the reactor,
module selection, and generated artifact paths.
