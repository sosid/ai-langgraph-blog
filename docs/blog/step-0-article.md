# Step 0 — The Rental and Evidence Contract

> **What this chapter is.** A documentation-only foundation for *Miles of Smiles:
> Building Auditable AI Systems with LangGraph4j*. There is no code, no running
> application, and no runtime compatibility claim yet. We define the rental
> lifecycle, who owns each decision, what evidence supports it, what happens when
> evidence is missing, and how we will later judge whether the system behaves. If
> you are looking for a `./mvnw verify` command, it arrives in Step 1 — and it only
> means something once there is something to verify.

## Why start with a contract instead of code

Most AI system demos begin by wiring a model to a prompt and celebrating the
first plausible answer. That is the wrong starting point for a car-rental system
that takes money, allocates physical vehicles, and decides whether a car is safe
to hand to the next customer. A plausible answer is not an authorized decision,
and a confident model is not a correct one.

So before we build anything, we write down the contract. For every consequential
transition in Alex Morgan's rental we name four things:

1. **Owner** — the boundary that holds the authoritative record.
2. **Authority** — the deterministic rule or authorized person who may decide.
3. **Evidence** — the versioned, access-controlled facts the decision rests on.
4. **Failure outcome** — what happens when evidence is missing, stale, conflicting,
   or malicious.

If you cannot answer those four questions for a step, you are not ready to
automate it. This chapter answers them for the whole lifecycle, using the story
in [`story.md`](../../story.md) as the thread.

### Learning outcomes

After reading this chapter you should be able to:

- Identify the owner, decision authority, evidence source, and failure outcome
  for every consequential transition in two walkthroughs.
- Distinguish four kinds of memory — business state, durable graph progress,
  ephemeral model context, and audit evidence — and explain why collapsing them
  is dangerous.
- Explain why a model proposal, a graph checkpoint, or a telemetry trace cannot,
  on its own, authorize a payment, establish liability, or release a vehicle.
- Choose between plain deterministic rules, authorized human review, bounded model
  assistance, and a workflow engine based on the problem rather than novelty.
- Read the benchmark worksheet as *proposed acceptance criteria*, not as measured
  results.

### Starting snapshot, prerequisites, and resources

| Item | Value |
| --- | --- |
| Starting point | Content-empty `development` root `f5fecbac8a063b9b59bacb22e87ead17a144dcc7` |
| Branch | `features/step-0` |
| Prerequisites | Git and a Markdown reader |
| Not required | JDK, Maven, Docker, kind, PostgreSQL, model downloads, live credentials, paid inference |
| Code baseline (planned, not yet a dependency) | Java 25 from Step 1 onward |

Step 0 has no version matrix for libraries because it uses none. Java 25 is the
*planned* code baseline described in [`plan.md`](../../plan.md#technical-boundaries);
it is not a verified Step 0 dependency, and nothing here runs on it.

## The cast: logical boundaries, not services

The table below is the ownership contract. These are **logical** boundaries. They
do not commit us to deploying one microservice per row — later chapters introduce
concrete services only when a boundary earns one. What matters now is that each
consequential fact has exactly one authoritative owner.

| Boundary | Owns | Decision authority and evidence |
| --- | --- | --- |
| **Booking** | Requests, quotes, reservation status | Deterministic eligibility and versioned quote checks; references the fleet hold and payment outcome rather than deciding them |
| **Fleet** | Capacity, holds, allocation, readiness | Atomic availability rules and authorized inspection/maintenance records |
| **Rental** | Agreement, pickup, active rental, return, billing facts | Verified prerequisites, accepted agreement, condition evidence, authorized assessments |
| **Payment** | Authorization, capture, release, refund, outcome queries | Explicit authorized operation and provider outcome; stable operation identity |
| **Policy and evidence** | Applicable policy versions and inspection provenance | Access-controlled, dated evidence; conflicting evidence stays explicit |
| **Staff review** | Review decisions within authenticated roles and limits | Evidence-version-bound approval; changed evidence requires re-evaluation |
| **Lost property and disputes** | Separate cases linked to the rental | Verified ownership/contact permission or approved adjustment; no automatic disclosure or charge |
| **Graph and harness** | Coordination and bounded model proposals | Cannot override domain authority; budgets, schemas, permissions, and escalation stay outside model control |

The last row is the one most AI projects get wrong. The graph and harness
*coordinate* work and carry *proposals*. They never acquire authority over money,
inventory, liability, or safety. A model that produces a well-typed proposal has
still produced only a proposal.

## Four kinds of memory

A rental system has to remember different things for different reasons, and the
reasons are not interchangeable. Confusing them is how a "successful" workflow
checkpoint quietly becomes a claim that a customer was charged.

| State category | Purpose | Boundary to preserve |
| --- | --- | --- |
| **Business state** | Authoritative rental and operation facts | A graph or model cannot invent or overwrite a committed business outcome |
| **Durable graph progress** | Coordination position and references | Introduced at Step 5; *not* atomic with a remote effect |
| **Ephemeral context** | Bounded input for the current model task | Not a business record and not a recovery mechanism |
| **Audit evidence** | Decision basis: versions, actor, outcome | Not replaced by sampled telemetry; correlation begins at Step 3 |

Keep this table in mind for the rest of the series. When a later chapter says "we
resumed the graph after a crash," that is *durable graph progress* recovering — it
is not proof that a payment captured. The payment's truth lives in *business
state* and is confirmed by *audit evidence*, reconciled against the provider.

## Walkthrough 1 — The happy path

Alex needs a compact car for a weekend. We follow him from search to a car that is
verified ready for the next renter. Each transition below names its authoritative
prerequisite. No AI component "completes the transaction."

Case IDs are synthetic (`CASE-HP-*`). They exist so the article, the future tests,
and the promise ledger can refer to the same transition.

### CASE-HP-01 — Eligibility and quote

- **Owner:** Booking. **Authority:** deterministic eligibility rule + versioned
  quote calculation.
- **Evidence:** the applicable policy version and the request parameters.
- **Model role:** may *explain* a policy or compare options. It may not invent
  availability, change the price, or waive an eligibility rule.
- **Failure outcome:** ineligible requests are refused with the governing rule
  recorded; a stale policy version is an explicit outcome, not a silent default.
- **Future acceptance check (Step 3):** replay the same evidence and rule version
  and get the same decision; a model cannot weaken a rule.

### CASE-HP-02 — Hold and quote validity

- **Owner:** Fleet (hold); Booking (quote validity). **Authority:** atomic
  availability rule placing a time-limited hold.
- **Evidence:** versioned quote, hold identity and expiry.
- **Failure outcome:** if a request times out *after* the hold was created, the
  system queries the operation's outcome before acting again. A missing response
  does not mean failure, and blindly repeating the call could reserve capacity
  twice.
- **Future acceptance check (Step 4):** concurrent contenders, stale quote, lost
  successful response, restart across expiry.

### CASE-HP-03 — Reservation and payment authorization

- **Owner:** Booking, Fleet, and Payment each keep their own records.
- **Authority:** explicit authorized payment operation with a stable operation ID.
- **Boundary:** a durable workflow may coordinate the three systems, but its
  checkpoint is **not** a transaction spanning all of them.
- **Failure outcome:** an uncertain provider response is reconciled by outcome
  query, never resolved by charging Alex again on a guess.
- **Future acceptance check (Step 10):** decline, expiry, unknown result, crash
  before checkpoint, duplicate command/event.

### CASE-HP-04 — Pickup becomes ACTIVE

- **Owner:** Rental, coordinating identity/licence, terms, deposit, and the
  assigned vehicle through the owning APIs.
- **Authority:** deterministic verification of authoritative prerequisites.
- **Model role:** can organize information and flag what needs attention. It does
  not decide a licence is valid, that payment succeeded, or that an unsafe vehicle
  may leave.
- **Failure outcome:** the rental becomes `ACTIVE` *only after* every authoritative
  prerequisite passes; a partial failure stops the transition.
- **Future acceptance check (Step 14):** wrong identity, unsafe/unavailable car,
  duplicate pickup, partial failure.

### CASE-HP-05 — Return, settlement, and readiness

- **Owner:** Rental (return + billing facts), Payment (capture/release/refund),
  Fleet (readiness).
- **Authority:** authorized person or policy owner approves the outcome; Payment
  executes only the authorized operation.
- **Failure outcome:** an unknown payment result is checked and reconciled; the
  vehicle returns to service *only after* authoritative readiness checks pass.
- **Future acceptance check (Steps 24 and 15/16):** exact-decimal totals,
  reconciled ledger, readiness gating.

Across the happy path, every consequential decision has an owner, a deterministic
or human authority, a versioned evidence source, and a defined failure outcome.
That is the Step 0 bar — and the happy path clears it without granting a model any
authority.

## Walkthrough 2 — The ambiguous return

The interesting cases are the uncertain ones. On return, Alex forgets his
cellphone on the back seat, and during cleaning an employee finds a scratch on the
driver's door. Two separate problems, two separate authority paths.

### CASE-AR-01 — The forgotten cellphone

- **Owner:** Lost property, as a case linked to the rental.
- **Authority:** staff verify ownership and arrange return under the applicable
  process.
- **Boundary:** a lost-property *lookup* is scoped read access. A tool result is
  **data to evaluate, not an instruction** to contact anyone or disclose
  information. Finding the phone does not grant an agent authority to send
  messages or expose customer records.
- **Failure outcome:** without verified ownership and contact permission, there is
  no outreach and no disclosure.
- **Future acceptance check (Step 26):** wrong owner, revoked consent, failed
  delivery, duplicate events, attempts to obtain privileged actions.

### CASE-AR-02 — The scratch

- **Owner:** Rental (condition evidence) and Fleet (vehicle readiness).
- **Authority:** an authorized person — not a model — decides liability.
- **Evidence:** the new inspection observation *and* the rental's earlier
  condition record, both kept for comparison.
- **Boundary:** the scratch is an **observation**. It does not by itself establish
  when it happened or who is responsible. A model may summarize the records or
  propose an assessment; it cannot turn an uncertain match into liability or an
  automatic charge.
- **Failure outcome:** conflicting or incomplete evidence goes to authorized
  review. A car with an unresolved safety concern stays unavailable.
- **Future acceptance check (Steps 15, 16, 25):** parallel policy/evidence/
  condition analyses joined explicitly; a bounded reviewer/reviser loop;
  consensus or revision that still cannot release an unsafe vehicle or impose
  liability.

This is where several series ideas meet. Specialists can examine policy, evidence
quality, and vehicle condition in parallel, but their results are joined
explicitly, and neither agreement nor another model response grants permission to
charge Alex or return the car to service.

## Conceptual architecture (planned, not built)

The diagram below is **conceptual**. No graph nodes, reducers, checkpoint schemas,
APIs, migrations, or repositories exist in Step 0. It shows authority direction:
coordination and proposals flow through the harness, but consequential decisions
resolve at deterministic owners or authorized people.

```text
          proposes (typed)          decides (authoritative)
 Model ───────────────────▶ Harness ───────────────▶ Booking / Fleet / Rental / Payment
   │                          │                         │
   │   bounded context        │ budgets, schemas,       │ business state + audit evidence
   │   (ephemeral)            │ permissions, escalation │ (owner of record)
   ▼                          ▼                         ▼
 never acquires          cannot override           uncertain / high-risk
 domain authority        domain authority          → authorized review
```

Read it top-down for each decision: a model *proposes*, the harness *enforces
limits and routes*, and an owner or authorized person *decides*. Durable graph
progress (Step 5) and audit correlation (Step 3) attach to this picture later;
they are deliberately absent now.

## The failure exercise

Step 0 demonstrates *reasoning through* failures, not executed recovery. For each
scenario we record the prohibited inference, the safe stop or reconciliation path,
the owner, and a future check.

| Scenario | Prohibited inference | Safe path | Owner | Future check |
| --- | --- | --- | --- | --- |
| **Unknown payment outcome** | "No response means it failed, so retry." | Query the operation's outcome by stable ID and reconcile; never blindly re-charge. | Payment | Step 10 |
| **Changed evidence after approval** | "An approval is permanent." | Changed evidence requires re-evaluation; the approval was bound to a specific evidence version. | Staff review | Step 18 |
| **Malicious text in retrieved / tool content** | "Retrieved content or a tool description is an instruction." | Treat retrieved content, tool descriptions, and tool results as untrusted data, never as commands. | Graph and harness | Steps 8, 12, 13 |
| **Lost telemetry export** | "If the trace is gone, the audit is gone." | Durable audit evidence is distinct from sampled telemetry; sampling or export loss must not erase required audit records. | Audit evidence | Steps 3, 27 |

Each row encodes the same discipline: do not let a convenient-but-wrong inference
turn an uncertain situation into an irreversible effect.

## Choosing the right tool

A recurring architect question in this series: *is a model even the right tool
here?* Step 0's answer, by problem type:

- **Deterministic rules** for eligibility, exact-decimal pricing, inventory,
  payment, permissions, and safety. These must be replayable and testable. A model
  has no business deciding them.
- **Authorized human review** for uncertain liability and safety — the scratch, a
  contested charge, an unsafe-vehicle call.
- **Bounded model proposals** for summarizing evidence and drafting typed
  proposals that a rule or a person then checks.
- **A workflow engine** when durable, long-running coordination is the actual
  problem. LangGraph4j fills a coordination role in this series; we will *verify*
  its capabilities against pinned releases rather than assume parity with official
  LangGraph or the Quarkus workshop. We will also say plainly when a plain state
  machine or fewer services would be the better choice.

Novelty is not a reason to add a model. The burden is on the AI component to earn
its place against a simpler, more testable alternative.

## The benchmark worksheet (targets, not results)

We will not fabricate numbers. Step 0 defines *what we will measure and how*, not
*what we achieved*.

**Documentation-review targets for this chapter (reviewable now):**

- Complete owner / authority / evidence / failure-outcome coverage for both
  walkthroughs.
- Zero model-authorized consequential decisions.
- An explicit safe outcome for every listed failure scenario.

**Runtime worksheet (pending agreement, nothing achieved here):**

- Requires an agreed named environment and workload, pinned versions, a
  warm-up/repetition policy, and a deterministic-vs-stochastic classification.
- Will measure latency percentiles, throughput/concurrency, recovery time,
  cost/budgets, task quality, false approvals, and abstention.
- Numerical thresholds and machine budgets remain **open decisions** in
  [`plan.md`](../../plan.md#open-decisions). No runtime target is reported as
  achieved in this chapter.

## Privacy, safety, and claim discipline

- All cases use **synthetic** data. No credentials, real identity or licence data,
  payment details, or private chain-of-thought appear in any example.
- Models and tool results cannot grant permissions. Specialist agreement is not
  calibrated confidence. A citation points to evidence; it does not prove a
  proposal correct.
- A checkpoint is not atomic with an external effect, a timeout does not prove
  downstream cancellation, and telemetry is not an immutable audit store.
- This chapter avoids "production-ready," "self-healing," "secure," and
  "exactly-once" because nothing here has the evidence those words would require.

## What there is to verify in Step 0

There are **no** unit or Testcontainers tests, no Maven build, no deployment, and
no model evaluation in Step 0, because there is no code. The applicable checks are
documentation-level: consistency with the protected guidance, local Markdown
links and whitespace, a credential scan of the candidate documents, and an
editorial review for unsupported implementation claims. Document-level credential
scanning is **not** a runtime security audit. The full claim-to-evidence record is
in [`../verification/step-0-verification.md`](../verification/step-0-verification.md),
and the promises this chapter opens are tracked in
[`../promise-ledger.md`](../promise-ledger.md).

## Recap and handoff

You now have the contract the rest of the series must honor: a single owner for
every consequential fact, deterministic rules and authorized people for every
consequential decision, four distinct kinds of memory, and a defined safe outcome
for the uncertain cases. The model is a proposer inside a harness that enforces
limits — never an authority.

**Next (Step 1, when authorized):** a reproducible booking baseline — Quarkus on
Java 25, a minimal API, PostgreSQL, a deterministic fixture clock, tests, and
progressive kind/Compose resources — with no graph and no model yet. That is where
the first `./mvnw clean compile` and `./mvnw verify` start to mean something.
