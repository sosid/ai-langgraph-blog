---
name: article-to-code-verification
description: "Use when verifying a Miles of Smiles chapter article against its implementation, especially before acceptance or PR. Check every claim, snippet, command, diagram, link, prerequisite, expected output, and promise against one frozen candidate revision and report evidence and gaps."
---

# Article-to-Code Verification

Verify that one chapter's article describes what its exact candidate snapshot implements and demonstrates. Read `AGENTS.md`, `plan.md`, the approved step blueprint, `skills.md`, the article, source, tests, deployment resources, and promise ledger entries affected by the step. Do not edit the article or code during verification unless the user explicitly requests that; report findings and evidence first.

## Freeze the Candidate

Record the step, base revision, candidate revision, and working-tree status before checking claims. Prefer a commit SHA with a clean tree. If committing is not authorized, identify the exact candidate by its base commit plus a stable patch/tree identifier and enumerate changed files. Never create a commit just to satisfy this workflow. If the candidate changes during review, discard affected conclusions and rerun them against the new exact snapshot.

## Verification Workflow

1. **Build the claim inventory.** Split the article into verifiable claims: business behavior, ownership/authority, architecture/state, versions, prerequisites, snippets, commands, outputs, tests, failure handling, security/privacy, diagrams, links, and cumulative promises.
2. **Trace each claim.** Record the source path and symbol/configuration, test, command, deployment resource, or captured observation that supports it. Check excerpts against exact candidate lines and ensure snippets can be applied in the described starting state. Check command working directories, flags, profiles, environment variables, and expected outputs against the repository.
3. **Classify evidence.** Use the promise-ledger status vocabulary: planned; implemented but unverified; verified at revision; failed; blocked; deferred. A passing unit test supports only its tested behavior. A mock does not establish external-provider behavior. A target is not a result. Live-model evaluations are stochastic and must include exact configuration, data, run count, tolerance, and limitations.
4. **Run applicable gates.** For code chapters with Maven, run and record `./mvnw clean compile` and `./mvnw verify` from the documented build root, plus mock-based unit tests and Testcontainers integration tests for every feature, and applicable system, contract, security, concurrency, recovery, and failure checks. Keep standard verification independent of live-model credentials and kind. Do not add build/lint/test tooling solely for prose validation.
5. **Check deployment claims when affected.** If resources or workflows changed, follow the chapter's Compose and existing-kind walkthroughs. Before applying to kind, inspect and confirm the current context and chapter namespace with the user, apply only that namespace, wait for readiness, call the documented endpoint, and test stated recovery/persistence. Never create/reset a cluster, silently switch context, delete PVCs, or remove user data. If authorization or prerequisites are absent, report the gate blocked; do not claim it passed.
6. **Check documentation artifacts.** Verify external and local links, diagram syntax with an appropriate renderer, labels and state ownership in diagrams, prerequisites, screenshots/alt text, documented starting and final snapshots, expected output, and consistency with the prior/next step. A docs build alone does not prove browser rendering, diagram rendering, runtime behavior, or a live model result.
7. **Review safety and privacy.** Confirm there are no secrets, real PII, or private chain-of-thought. Check that model proposals cannot override deterministic controls, retrieved/MCP content is treated as untrusted, unknown remote effects are reconciled rather than blindly retried, and audit records do not rely on telemetry sampling/export availability.
8. **Report the result.** Provide the candidate identifier; findings first, with file/path and claim; evidence and exact commands/results; failed, blocked, deferred, and unverified claims; limitations; and affected promise-ledger updates. Do not label a chapter verified unless all applicable required gates pass.

## Invalidation Rules

Any change to source, tests, article, dependencies, images, configuration, prompts, policies, schemas, deployment resources, or integration base invalidates the evidence that depends on it. Identify the affected claim/check set and rerun it. A new frozen candidate is required for final acceptance. Review/security findings must be resolved or explicitly accepted by an authorized reviewer; the verifier cannot waive a blocker.

## Required Distinctions

- Every feature has deterministic mock-based unit tests and Testcontainers integration tests. Kind and Compose are separate local deployment paths, not substitutes for one another.
- Business state, durable graph progress, ephemeral context, and audit evidence are distinct. Checkpoint persistence does not prove atomicity with payment or other remote effects.
- OpenTelemetry spans correlate execution with durable audit evidence; sampling, collector outage, or export failure must not erase required audit records.
- Citations point to evidence and do not prove the proposal is correct. Model agreement does not establish calibrated confidence.
- For documentation-only planning changes, check consistency, links, secrets, and editorial quality only. Do not claim nonexistent build or runtime evidence.