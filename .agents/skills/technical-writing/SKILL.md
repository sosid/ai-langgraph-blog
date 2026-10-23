---
name: technical-writing
description: "Use when drafting or revising an article in the Miles of Smiles LangGraph4j series. Write source-anchored technical teaching for experienced software engineers, continue the rental lifecycle, and keep claims aligned with the approved chapter and candidate code."
---

# Technical Writing

Write chapter articles for **Miles of Smiles: Building Auditable AI Systems with LangGraph4j**. The readers are experienced software engineers learning from a principal architect's practical decisions. The story follows Alex Morgan's rental from search and reservation through pickup, return, forgotten cellphone, scratch assessment, settlement, and making the car safe for the next customer. Continue the story that the code actually supports; do not force every event into every chapter.

Use the Quarkus LangChain4j docs and workshop as references for incremental teaching, source navigation, and observable exercises. The workshop is not an authority for this repository's APIs, architecture, version matrix, or demonstrated behavior. Do not copy its prose. Consult `plan.md`, the approved `docs/plans/step-N-plan.md`, the actual source/tests/deployment examples for that step, and the prior article before drafting.

## Workflow

1. Confirm the step is authorized and identify the accepted starting snapshot and intended final snapshot. Read the approved blueprint, preceding chapter, relevant code, tests, configuration, and deployment files. If the approved increment or evidence is missing, request clarification instead of inventing behavior.
2. State what the customer or operator could do in the prior snapshot, where that capability ends, and what this step adds. Keep business state, graph progress, ephemeral context, and audit evidence distinct. Name each authoritative owner and decision-maker.
3. Explain the problem and mechanism in plain language before introducing APIs, class names, protocols, or annotations. Define unfamiliar terms before relying on them. Connect the mechanism to a real trade-off and explain when simpler deterministic code or a workflow engine is more appropriate.
4. Anchor edits to real file paths and exact candidate code. Explain why a change is needed before showing it. Keep snippets small and complete enough to apply; distinguish additions from existing context and do not fabricate source, output, screenshots, logs, or success.
5. Give the reader a reproducible path: starting snapshot, prerequisites, exact working directory and commands, required resources, expected observation, and how to inspect it. Keep live model credentials optional unless explicitly needed and label variable model text as illustrative.
6. Include the relevant failure lab, safe recovery/reconciliation path, and evidence proving the intended behavior. Say what a check establishes and what it cannot establish. Include privacy, security, cost, latency, and operational limitations that materially affect the exercise.
7. Close with what the reader can now do and a short handoff to the next planned increment. Do not imply that later work exists or is verified unless the repository snapshot proves it.

## Required Article Content

Each code chapter covers:

- A concrete continuation of the rental and the limitation of the previous state.
- Learning outcomes, exact starting snapshot, prerequisites, pinned version matrix, and local resource requirements.
- Before/after architecture or graph diagrams where they clarify ownership, authority, state, or event sequence. Label planned versus implemented components accurately.
- Focused source changes and a completed-solution reading path.
- Reproducible interaction with observed output or observation instructions. Clearly label illustrative or stochastic model output.
- A failure lab, recovery behavior, and evidence for the stated guarantee.
- Tests and their boundaries, limitations, privacy/cost/latency trade-offs, concise recap, and next-step handoff.

Step 0 is documentation-only: use scenarios, ownership, threat/failure analysis, and reviewable acceptance definitions instead of runnable code claims. Keep all articles consistent with the series contract and promise ledger once chapter work is authorized.

## Claim Discipline

- Deterministic code owns eligibility, price, inventory, payment, permission, liability, and safety. Describe model results as typed proposals. Explain abstention and authorized review where appropriate.
- Do not claim that citations prove correctness, multiple agreeing models are calibrated, a timeout proves cancellation, a checkpoint is atomic with an external effect, or telemetry is an immutable audit store.
- Never use “production-ready,” “self-healing,” “secure,” or “exactly-once” without narrow, directly relevant evidence and scope.
- Distinguish planned, implemented-but-unverified, verified-at-revision, failed, blocked, and deferred behavior. Do not convert targets, mock-only observations, or unavailable runtime checks into demonstrated results.
- Keep examples synthetic and concise. Exclude secrets, real PII, and private chain-of-thought from articles, prompts, checkpoints, logs, and traces.
- Keep library versions, commands, images, Java/Quarkus support, and preview flags anchored to the exact candidate. A floating docs page is not a version matrix.

## Editorial Review

Read once for the explanation and once as a reader following the exercise. Ensure each step tells one connected story, headings describe concepts or reader tasks, lists are used only when they improve scanning, and diagrams add information not repeated in prose. Verify every path and identifier against the candidate. Prefer direct, natural prose over boilerplate recaps, unsupported assurances, or API inventories. Hand unresolved claims and verification gaps to the article-to-code verification skill; do not silently smooth them over.