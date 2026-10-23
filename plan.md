# Miles of Smiles: Building Auditable AI Systems with LangGraph4j

## Status and Purpose

This document is the initial series plan, not authorization to implement, merge, tag, publish, create branches, or establish a runtime compatibility claim. The series is a learning opportunity for experienced engineers, told by a principal architect through one complete car-rental lifecycle. The repository is initially being populated with planning and agent guidance only.

For a reader-first narrative of Alex's complete rental before the technical roadmap, see [`story.md`](story.md).

Alex Morgan wants a compact car for a weekend. Reservation is only the opening: the system must allocate capacity, authorize payment, verify pickup, record return evidence, settle approved charges, and make the car safe for the next customer. On return, Alex forgets his cellphone in the car; cleaning finds a scratch on the driver's door. These are connected business events, not permission for an agent to decide ownership, liability, or payment.

The series builds a Java system that retrieves applicable policy, uses separately scoped MCP servers, coordinates bounded parallel agents and subagents, and revises typed proposals when evidence is missing. Deterministic rules and authorized people retain control of consequential decisions.

## Series Contract

Build one complete rental lifecycle rather than disconnected agent demos. Introduce independently deployable Quarkus services with service-owned data only when justified. Every feature has unit tests using mocks and integration tests using Testcontainers. Backend artifacts support a local existing kind cluster and Docker Compose, introduced progressively so readers can build the working system incrementally.

Use LangGraph4j for graph coordination. Pin versions and verify actual capabilities rather than assuming parity with official LangGraph or the Quarkus workshop. Build a well-designed execution harness, not a job orchestrator. Reuse maintained libraries for graph persistence, HTTP/MCP, resilience, tracing, and testing. Treat an unsupported capability as a documented spike or blocker, not a home-grown substitute without approval.

### Technical Boundaries

- **Java and concurrency:** Java 25 is the planned baseline through Step 16. Step 17 is a conditional migration to Java 27. Before it starts, verify actual JDK/toolchain availability, Quarkus support, APIs, and preview requirements; if blocked, stop for a scope decision. Use virtual threads for suitable blocking work and retain a bounded-executor alternative. Introduce `StructuredTaskScope` only at Step 17 after compatibility verification, including preview flags where required. `ScopedValue` is bounded JVM context, not network propagation or durable state.
- **Business authority:** deterministic code owns eligibility, exact-decimal prices, inventory, payment, permissions, liability, legal transitions, and safety. Models make typed proposals. Uncertain or high-risk outcomes abstain or require authorized review.
- **Harness controls:** enforce tool allowlists and per-task permissions, authenticated tenant/actor scope, context selection/provenance/size/redaction/retention, schema validation, cancellation, escalation, and per-call/workflow turn, token, cost, and time budgets. Apply limits at the first model call. Version graph, model, prompt, policy, tool, embedding, and evaluator references. These controls stay outside model authority.
- **RAG:** filter by tenant/access scope, date, jurisdiction, and policy version. Track provenance and validate citations. A citation is an evidence reference, not proof of proposal correctness. Missing, conflicting, stale, or malicious evidence is an explicit outcome.
- **MCP:** MCP means Model Context Protocol. Use separate identities/scopes for each server, allowlisted servers/tools, authorization, deadlines, and schema validation. Never trust retrieved content, descriptions, or tool results as instructions.
- **Parallel work and correction:** parallel agents and nested subagents use isolated inputs, typed handoffs, deterministic reducers, bounded fan-out, and explicit partial-failure handling. A model supervisor cannot grant authority. Self-correction revises a proposal within fixed budgets; it cannot alter policy, grant tools, train itself, or replay irreversible effects. Model agreement is not calibrated confidence.
- **State and persistence:** distinguish service business state, durable graph progress, ephemeral context, and audit evidence. Durable PostgreSQL-backed graph checkpoints begin at Step 5. A checkpoint is neither atomic with remote effects nor a complete immutable audit log. Bounded Caffeine/Valkey read caching begins at Step 11; cache loss must not lose business state or graph progress. Verify tenant/version/freshness keys, invalidation, restart recovery, and unknown-effect reconciliation.
- **Effects and retries:** use stable operation IDs, outcome queries, idempotency, and reconciliation. Never blindly retry unknown payment or write outcomes. Resilience4j belongs at outbound dependency boundaries with a total deadline and one retry owner; do not stack competing retries. Timeouts do not prove downstream cancellation.
- **Audit and telemetry:** introduce OpenTelemetry correlation together with durable decision evidence at Step 3 and extend it through graph, checkpoint/resume, retrieval, MCP, parallel tasks, correction, and approval. Link durable audit events to trace/span IDs. Sampling, exporter failure, or collector outage must not erase required audit records. Telemetry is correlation/transport, not an immutable audit store. Step 27 completes the collector/backend walkthrough and reconstruction lab.
- **Privacy:** use synthetic data and concise decision grounds only. No secrets, real PII, or private chain-of-thought in articles, prompts, checkpoints, logs, or traces.

The agent harness is an application boundary around model execution, not a new framework to build from scratch. It enforces typed task/result schemas, allowlisted tools, scoped identity, context budgets, workflow budgets, version references, durable progress, and safe effect coordination. Models cannot expand those boundaries.

### Self-Correction, Not Self-Authorization

A reviewer can identify missing evidence or an invalid proposal; a reviser can retrieve approved evidence or produce another typed proposal. Each cycle rechecks deterministic constraints and records its revision reason, evidence changes, and outcome. Bound attempts, time, tokens, and cost. Stop on exhaustion, repeated proposals, or unmet quality. This is not online training, self-modifying policy, new permissions, or autonomous financial/safety decision-making. Irreversible effects never run inside speculative correction loops.

## Research and Compatibility Gates

Candidate library choices are not a tested version matrix. Before a chapter relies on them, record exact versions, source/API references, minimal executable compatibility evidence, limitations, and the chapter introducing them.

- Verify LangGraph4j state/reducer behavior, nested graph support, streaming, maintained PostgreSQL saver, serialization, durable wait/resume, and telemetry against pinned release artifacts.
- Verify Quarkus, JDK distribution/build, model libraries, images, and build tools. For Java 25/27, consult release pages and actual structured-concurrency JEP/API status at that time.
- Verify LangChain4j RAG facilities, PostgreSQL/pgvector, synthetic versioned policy filters, citation validation, and embedding compatibility.
- Verify MCP contracts and scoped identities, NATS JetStream/outbox/inbox and effect reconciliation, cache invalidation, Resilience4j decoration order, and OpenTelemetry export/audit behavior at the step introducing each capability.
- Do not infer TLS/mTLS or storage support across different protocols/components. Do not claim local single-instance kind or Compose services provide production HA.

## Roadmap

Each row is one increment and names a discriminating acceptance/failure check. A chapter blueprint expands it into measurable promises, service ownership, graph/state delta, prerequisites, deployment changes, tests, and exclusions. No later row's feature is delivered implicitly by an earlier one.

### Section 1: Build a Trustworthy AI Foundation (0–9)

| Step | Increment and teaching focus | Acceptance and failure evidence |
| --- | --- | --- |
| 0 | **Rental and evidence contract.** Lifecycle, owners, state distinctions, threat model, rules/agent/human/workflow-engine alternatives, local benchmark targets. No code. | Walk happy path and ambiguous return; every consequential decision has an owner, authority, evidence source, and future acceptance check. Targets are not results. |
| 1 | **Reproducible booking baseline.** Quarkus/Java 25, minimal API, PostgreSQL, deterministic fixture/clock, tests, progressive kind/Compose resources, local development PKI bootstrap. | Pin compatibility; clean compile/verify; run both deployment paths, health/readiness, restart/persistence. No graph/model. |
| 2 | **Service-owned records.** Booking, rental, fleet persistence, migrations, isolated roles, versioned contracts, request hashes, interval constraints. | Deny cross-owner DB access; test duplicate request/payload conflict, schema upgrade, adjacent windows, and overlapping holds. |
| 3 | **Rules before agents.** Eligibility, pricing, versioned policy rules, durable decision records, initial OpenTelemetry trace/span correlation and privacy classification. | Replay same evidence/rule version to same outcome; correlate decision to request; sampling/export failure retains audit evidence; model cannot weaken a rule. |
| 4 | **Search, quote, reserve.** Fleet atomic holds, expiry, quote validity, idempotency, compensation, outcome queries. | Test concurrent contenders, stale quote, lost successful response, restart across expiry, maintenance buffers, UTC/branch time, adjacent windows. |
| 5 | **Durable graph harness.** Orchestrator state, deterministic reducers/routing, maintained PostgreSQL saver, bounded references, durable wait, trace-linked resume, single-owner recovery. | Crash A/resume B; inspect saved state/serializer behavior, tenant isolation, incompatible-state rejection; correlate progress across restart without credentials. Saver alone is not distributed fencing. |
| 6 | **Models make typed proposals.** LangChain4j, deterministic fake model, optional local/cloud profiles, initial tool/context/budget gates and outbound resilience. | Malformed output, refusal, injection, timeout, and budget exhaustion safely fail/abstain without changing business authority. |
| 7 | **Progress is not success.** Virtual-thread SSE and small browser client distinguish model tokens, persisted progress, and committed facts. | Authorized reconnect to another process, disconnect handling, replay/deduplication, no repeated business effect. |
| 8 | **Versioned policy RAG.** Retrieval service, ingestion/chunking, embeddings/pgvector, access/applicability filters, citations, retrieval abstention. | Wrong tenant/date/jurisdiction, missing/conflicting/stale policy, poisoned text, embedding mismatch; citation resolves to exact evidence version. |
| 9 | **Confidence is measured.** Calibrated Confidence Evaluator (CCE), held-out synthetic task data, coverage/error/abstention and risk/authority gates. | Publish split and limitations; high confidence cannot bypass required review; compare deterministic/no-model baselines. |

### Section 2: Coordinate Agents, Tools, and People (10–19)

| Step | Increment and teaching focus | Acceptance and failure evidence |
| --- | --- | --- |
| 10 | **Confirm without double charging.** Payment service, tokenized mock gateway, authorization, compensation, minimal NATS outbox/inbox, durable outcome wait/effect records. | Decline, expiry, unknown result, crash before checkpoint, duplicate command/event, failed compensation; reconcile unknowns rather than replay. |
| 11 | **Cache only what is safe.** Bounded Caffeine/Valkey read caches, tenant/access/version/freshness keys, TTL/invalidation, shared workflow budgets, stampede control. | Stale policy, cross-user access, eviction/outage, budget exhaustion; domain/checkpoint state survives cache loss; never cache writes, final inventory authority, payment commands. Measure warm/cold cost and hit/miss telemetry. |
| 12 | **Consume CRM over MCP.** Dummy CRM server with typed read tools, scoped identity, tool inventory, explicit trace/context propagation. | Unauthorized scope, schema drift, poisoned descriptions/results, timeout, server substitution, budget checks. Discovery is not trust. |
| 13 | **Expose legacy lost property over a second MCP server.** Restricted facade over synthetic legacy system; separate scopes/permissions from CRM. | Wrong server/tool, confused deputy, forbidden writes/outreach, injection, unknown write outcome; only authorized idempotent operations. |
| 14 | **Pickup is a business transition.** Coordinate identity/licence result, terms, ready assignment, deposit, and condition references through owning APIs. | Wrong identity, unsafe/unavailable car, duplicate pickup, partial failure; ACTIVE only after authoritative prerequisites. |
| 15 | **Supervisor, parallel specialists, nested subagents.** Return-assessment worker with separate graph history; local specialist subgraphs and remote NATS task contract. Parallel policy, evidence-quality, and condition analyses. | Typed joins/reducers, bounded fan-out, partial failure/cancellation, duplicate/out-of-order results, crash recovery, schema mismatch, state isolation. Add ownership/fencing before concurrent worker execution. |
| 16 | **Review, revise, or stop.** Bounded reviewer/reviser loop over return proposals; retrieve missing authorized evidence; coordinate fleet inspection/cleaning/quarantine. | Compare initial/revised proposals on held-out cases; no-progress loop, contradictory reviewers, injection, exhausted budget abstain/escalate. Consensus/revision cannot release unsafe vehicle or impose liability. |
| 17 | **Conditional Java 27 concurrency migration.** Verify actual release/APIs first; structured fan-out/cancellation, scoped context, dependency bulkheads, bounded-executor equivalent. | Prior-snapshot migration, compile/test/runtime preview setup if required, max calls, cooperative interruption, deadlines, no tenant leakage. Unavailable prerequisites block migration. |
| 18 | **Human review survives deployment.** Durable review queue/API, authenticated roles/limits, evidence-version-bound decisions. | Restart while waiting, alternate replica resume, reviewer race, duplicate approval, changed evidence, unauthorized reviewer. Approval does not imply blind effect replay. |
| 19 | **Resilience and adaptive routing.** Harden dependency policies, global workflow budgets, allowlisted model selection by measured need. | Provider 429/outage, saturation, retry multiplication, partial results, recovery; compare quality/latency/cost. Local breakers/limiters are not global; routing cannot weaken controls. |

### Section 3: Make the System Auditable Under Failure (20–31)

| Step | Increment and teaching focus | Acceptance and failure evidence |
| --- | --- | --- |
| 20 | **TLS/mTLS you can inspect.** Extend development PKI into verified per-hop config, workload identities, SAN/EKU checks, trust bundles, rotation. | Wrong hostname, missing/untrusted/expired client cert, rollover; document unsupported transports/revocation. Public model TLS does not imply provider mTLS. |
| 21 | **Authorization and defensible evidence.** OIDC/Keycloak, ingress tenant validation, least-privilege tools, separation of duties, retention, tamper-evident records. | Deny/detect forged task completion, confused deputy, cross-tenant access, unauthorized refund, sensitive-data leak. Hash chains without external anchors do not defeat privileged rewriting. |
| 22 | **Distributed failure without fiction.** Harden leases/fencing, optimistic versions, event ordering, inbox/outbox, DLQ/replay, reconciliation. | Stale paused owner, lease expiry, duplicate/out-of-order/poison events, checkpoint contention, multi-worker crash windows; no duplicate irreversible outcome. |
| 23 | **The rental changes.** Extensions, cancellation/no-show, late return, breakdown; evaluate maintained durable scheduler for idempotent due-time commands. | Downtime across expiry, cancellation/payment race, extension conflict, unsafe continued-driving escalation; no sleeping graph threads as durable timers. |
| 24 | **Close the money loop.** Approved billing facts/invoices remain rental-owned; payment ledger and capture/void/refund/release remain payment-owned. | Exact-decimal totals, partial/unknown payment, duplicate settlement/refund, failed compensation, reconciled ledger/invoice. No real card data or compliance claim. |
| 25 | **Disputes and late evidence.** Separate cases and approved adjustments linked to original invoices/evidence. | Contradictory late evidence, appealed model proposal, repeated refund request, redacted bundle preserving original history. |
| 26 | **Customer support with boundaries.** Support service uses scoped tools, consent-aware notices, ownership-verified lost-property coordination. | Wrong owner, revoked consent, failed delivery, duplicate events, attempts to obtain privileged chat actions. |
| 27 | **Observe and reconstruct.** OpenTelemetry Collector, traces/metrics/logs and Grafana; correlate durable audit with workflow/checkpoint/task/operation/evidence/approval/revision. | Reconstruct failure across services/restarts, including cache/RAG/MCP/subagent/correction; check privacy and cost/latency/budgets. Audit survives sampling, dropped exports, or collector outage and recovers delivery under stated retention. Trace is not immutable ledger. |
| 28 | **Evaluate the whole AI system.** Golden retrieval/tool/agent datasets, correction ablations, calibration, adversarial/security tests, versioned CI gates. | Regressed prompt/retriever/tool schema is blocked; report quality, false approvals, abstention, loop benefit, correlated-review failure, stochastic tolerance, judge limits. |
| 29 | **Operate evolving state.** Compatible service/graph/tool/event/schema upgrades, backup/restore, reindex cutover, rotation, rollback. | Old durable wait resumes or explicitly rejects incompatibility; restore domain/checkpoint then reconcile effects; document PVC/storage and rollback limits. |
| 30 | **Break the stack deliberately.** Full Testcontainers and separate kind/Compose lifecycle/fault labs, deterministic defaults, optional live inference. | Split-brain rejection, lost outcomes, malicious MCP content, stale approval, cache outage, cert failure, worker failover; record reproducible observations and cleanup/persistence. |
| 31 | **One rental, every consequential decision accounted for.** Reservation to settlement/readiness, optional dispute/lost item; publish evidence and promise scorecard. | Reproduce from released snapshot using both documented paths; measured quality/cost/latency and limits; explain when plain code, fewer services, or workflow engine is better. Native-image work is separately qualified. |

## Promise-to-Evidence Contract

During authorized chapter work, maintain `docs/promise-ledger.md` with stable promise IDs, owning boundary, introducing step, article section, source symbol/path, test/command, candidate revision, observed result, environment, and status. Do not create chapter artifacts during this initial planning reset.

Cover booking/holds, pickup/return/readiness, payment/settlement, disputes/contact, graph checkpoint durability, cache isolation/invalidation/outage, harness budgets, RAG grounding, MCP isolation, parallel/subagent joins, bounded correction, authorization, privacy, recovery, deployment, evaluations, and OpenTelemetry-linked audit reconstruction despite sampling/export loss.

Statuses are **planned**, **implemented but unverified**, **verified at revision**, **failed**, **blocked**, or **deferred**. Targets, design reviews, mock-only observations, and unavailable live checks are not demonstrated runtime guarantees. Each chapter verifies affected cumulative promises, not only its new happy path. Changes to prompts, policies, dependencies, schemas, or base invalidate relevant evidence.

## Article Structure and Review

Follow `.agents/skills/technical-writing/SKILL.md`. Each article includes:

1. A concrete continuation of the rental and the problem with the prior state.
2. Learning outcomes, exact starting snapshot, prerequisites, version matrix, and resource requirements.
3. Before/after architecture or graph with state and authority boundaries.
4. Small explained changes tied to real files and a completed-solution reading path.
5. Reproducible interaction and observed output; stochastic text is illustrative, not an exact assertion.
6. Failure lab, safe recovery, and evidence for the behavior.
7. Tests, limitations, privacy/cost/latency trade-offs, recap, and handoff.

Define terms before relying on them. Explain the junior mental model and architect trade-offs in the flow rather than burying them under boilerplate. Never use “production-ready,” “self-healing,” “secure,” or “exactly-once” without narrowly scoped evidence. Step 0 substitutes scenarios and reviewable acceptance definitions for runnable changes. All code steps include deterministic tests, relevant integration/system checks, and source-aligned articles.

## Deployment Contract

Use the reader's existing kind cluster as the primary path, plain YAML, and direct `kubectl`; no cluster creation/reset, silent context switching, Helm, or Kustomize. Confirm selected context and isolated chapter namespace before applying anything. Document versioned image build/load, apply order, resource needs, readiness, port-forward, and endpoint checks.

Compose is a supported alternative with progressive profiles and tested commands. Introduce only current-chapter components; model downloads/cloud credentials remain optional. Explain differences from kind rather than claiming parity. Use health probes, resource limits, non-root service containers, scoped identities, non-committed secret inputs, explicit image versions, and namespaced resources. Do not assume NetworkPolicy enforcement without checking CNI. Preserve persistent data unless deletion is authorized. Stateful local defaults may be single-instance, not production HA. Verify TLS/mTLS, certificate rotation, and storage per component/protocol.

## Verification Gates

For every code chapter:

- Record exact source revision, toolchain/dependency/image/model/prompt/policy versions, and environment.
- Once Maven exists, run `./mvnw clean compile` and `./mvnw verify` from the documented root, plus mock-based unit and Testcontainers integration tests for each feature and focused/broader checks required by the step.
- Standard unit and Testcontainers checks are deterministic and independent of kind and external model credentials.
- Run applicable authorization, adversarial, concurrency, recovery, failure, and migration checks.
- When deployment resources/workflows change, run documented kind and Compose smoke paths, including restart/persistence. Do not destructively clean cluster/PVC state.
- Repeat article-to-code verification after step completion against the frozen exact revision. Check snippets, links, diagrams, prerequisites, outputs, and cumulative promises.
- Preserve commands, expected behavior, actual result, exit status, evidence references, and limitations. Redact sensitive output.
- Missing prerequisites or failed checks block a “verified” status and PR unless scope is explicitly changed and the revised promises are reverified.

Live-model evaluations are separately labeled with exact provider/model configuration, dataset, repeated runs, and stochastic tolerance. Synthetic calibration is not evidence of production safety. Agree quality, false-approval, abstention, budget, concurrency, latency, and recovery targets for a named environment before claiming achievement.

This initial documentation reset needs consistency/link checks, secret scanning, and editorial review, not invented build results. Do not add build/lint/test tooling merely to validate prose.

## Chapter Lifecycle After Approval

1. Start `features/step-N` from accepted `development`, sequentially. Work only on the approved chapter.
2. Write and approve `docs/plans/step-N-plan.md`: business increment, learning outcomes, ownership, graph/state delta, dependencies, threat/failure boundaries, deployment changes, tests, and exclusions.
3. Implement only that increment with deterministic doubles, focused tests, relevant Testcontainers/system tests, and progressive deployment examples.
4. Write `docs/blog/step-N-article.md` using the technical-writing skill, keeping code, article, and commands aligned during development.
5. Rebuild cumulative `docs/blog/index.md` and update `docs/promise-ledger.md` when chapter work is authorized. Record delivered, deferred, failed, and unverified promises separately.
6. Freeze the candidate and repeat final technical/editorial verification against that exact revision. Produce claim-to-evidence record.
7. Open a PR targeting `development` only after applicable gates pass. Include revision, promise coverage, commands/results, tested matrix, limitations, and review evidence. Missing prerequisites are blockers.
8. Any relevant source, article, dependency, configuration, prompt, policy, schema, or base change invalidates affected evidence. Rerun checks before acceptance; a changed base requires fresh integration check.
9. Merge to `development`, push, and publish only with applicable authorization. Preserve immutable tags. An authorized correction uses a feature branch and propagates to development.

## Branch, PR, and Snapshot Policy

The planned baseline is a content-empty orphan `development` root commit with no parents and an empty tree. Git needs a commit to publish a branch. Creating/publishing it is a separate authorized operation. Do not populate that root with planning documents, overwrite an existing `development`, or delete prior branches/history/tags. Leave `main` untouched. The planning set belongs on the assigned task branch and does not claim that `development` has been created.

After chapter approval, start `features/step-N` from accepted `development` containing the preceding verified step. Step 0 starts from the empty root commit and adds approved planning documents alongside its authorized documentation increment; do not inherit historical drafts. Finish, freeze, verify, and review a step before PR. On authorized integration, preserve a visible merge commit and recheck the resulting snapshot before creating an authorized annotated `step-N` tag.

Canonical milestone tags are `step-00` through `step-31`, created only on an authorized, verified `development` merge commit. Each tag must contain cumulative articles through that step and matching code/tests/infrastructure. Never move or reuse a published or unrelated historical tag. If a canonical name is occupied by a different sequence, stop for a naming decision. Corrections use an authorized immutable tag such as `step-5-patch1`. Push, PR, merge, tag, and publication are separate gated actions. Planned names do not prove snapshots exist. Once snapshots do exist, readers should be able to check out `step-5`, read its matching article, and compare `step-4..step-5`.

Keep conventional `README.md`, `AGENTS.md`, `plan.md`, and `skills.md` casing. Future chapter paths are `docs/plans/step-N-plan.md` and `docs/blog/step-N-article.md`; rebuild the historical index/ledger only when that chapter is authorized.

## Open Decisions

These planning defaults do not authorize implementation. Resolve them before affected work:

- Reader hardware/budget: CPU/RAM, optional GPU, operating systems, local model downloads, and cloud spend ceiling.
- Model baseline: local/cloud providers, model/license choices, embedding model, and reproducibility/availability policy. Default tests remain offline fakes.
- Data/tools: approve synthetic policies and CRM/lost-property contracts; real enterprise integrations require separate scope.
- Success criteria: task quality, false approvals/abstention, cost/latency, load, and recovery targets for a named lab environment.
- Release cadence: human reviewer, chapter implementation authorization, and separate merge/tag/publication approval.
- Future Java: retain conditional Step 17 Java 27 target; decide revised schedule/scope if availability or compatibility gate fails.
- Baseline establishment: content-empty orphan interpretation is confirmed, but creating/publishing the branch and later adding planning docs are separate operations. If `development` already exists, stop for a decision rather than replace it.

## Verification References

The Quarkus LangChain4j workshop is a teaching-structure reference, not executed or certified for this stack. See [`skills.md`](skills.md) for its sources and current repository paths. Before implementation, consult release-specific Quarkus, LangChain4j, LangGraph4j/saver, Resilience4j, MCP, NATS, PostgreSQL/pgvector, OpenTelemetry, and JDK documentation/artifacts. Replace assumptions with executable compatibility checks and record unsupported behavior honestly.
