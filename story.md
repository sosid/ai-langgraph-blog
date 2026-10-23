# Alex Morgan's Rental

This is the story behind **Miles of Smiles: Building Auditable AI Systems with LangGraph4j**. Read it before the roadmap if you want to see the customer journey before meeting the services, agents, and protocols that support it.

Alex Morgan needs a compact car for a weekend. He wants a clear price, a car that will actually be ready when he arrives, and a straightforward way to resolve anything unexpected. The request sounds simple. Completing it responsibly means keeping several promises across different teams and systems, even when a response is late, evidence is incomplete, or the application restarts.

The series follows Alex from his first search through the car's return to its preparation for the next renter. It is one connected business story, not a collection of unrelated agent demonstrations. The chapters build the system gradually, so the full journey here describes the destination rather than claiming that every capability already exists in the repository.

## Finding a Car

Alex enters his dates and asks Miles of Smiles for a compact car. The system checks whether he is eligible to rent, finds vehicles that can cover the requested interval, and calculates a price from the applicable rules. These decisions belong to ordinary, testable business logic. An AI assistant may help Alex compare options or explain a policy, but it cannot invent availability, change the price, or waive an eligibility rule.

When Alex chooses a car, the fleet system places a time-limited hold. The quote and hold have clear owners and versions, so the booking can tell whether the offer is still valid. If a request times out after the hold was created, the system checks the operation's outcome before doing anything again. A missing response does not mean the operation failed, and repeating it blindly could reserve capacity twice.

Alex confirms the reservation and authorizes payment. The booking, fleet, and payment records remain with the services responsible for them. A durable workflow can coordinate their progress, but its checkpoint is not a transaction spanning all three systems. If a payment provider's response is uncertain, the system reconciles the result rather than charging Alex again on a guess.

## Picking Up the Car

On pickup day, Alex presents his identity and licence, reviews the rental terms, and receives the assigned vehicle. The system checks the authoritative results: the renter is eligible, the agreement is accepted, the deposit is handled as required, and the vehicle is ready for this rental. Only then does the rental become active.

The assistant can organize information and propose what needs attention. It does not decide that a licence is valid, that a payment succeeded, or that an unsafe vehicle can leave the lot. Those transitions belong to deterministic rules and the services that own the underlying records.

## Returning the Car

After the weekend, Alex returns the car. He is tired and leaves his cellphone on the back seat without noticing. The return process records what happened: when the car came back, its condition, and the evidence needed for inspection. A lost-property lookup may help staff locate the phone, but access is scoped. A tool result is data to evaluate, not an instruction to contact someone or disclose information.

During cleaning, an employee finds a scratch on the driver's door. The scratch is an observation; it does not by itself establish when it happened or who is responsible. The system keeps the inspection evidence and the rental's earlier condition record available for comparison. An AI component may summarize those records or propose an assessment, but it cannot turn an uncertain match into liability or an automatic charge. Conflicting or incomplete evidence goes to an authorized person.

This is where several ideas in the series meet. Specialists can examine policy, evidence quality, and vehicle condition in parallel, but their results must be joined explicitly. A reviewer may ask for missing evidence, and a bounded revision may produce a clearer proposal. Neither agreement nor another model response grants permission to charge Alex or return the car to service.

## Closing the Rental

Once the responsible person or policy owner approves the outcome, the rental service records the billing facts. The payment service carries out only the authorized capture, release, or refund. If the result is unknown, the system checks and reconciles it. Alex can see the basis for the amount and can raise a dispute without erasing the original inspection record.

The cellphone follows a different path from the scratch. Staff verify ownership and arrange its return under the applicable process. Finding the phone does not grant an agent authority to send messages, expose customer records, or make decisions outside that task's permissions.

Meanwhile, the fleet team decides whether the vehicle needs inspection or repair. A car with an unresolved safety concern stays unavailable. It becomes eligible for the next customer only after the authoritative readiness checks pass. That final handoff matters as much as the original reservation: the system has to close Alex's rental without losing track of the next renter's safety.

## What to Notice

As the chapters unfold, keep asking four questions: who owns each fact, who has authority to make each decision, what evidence supports it, and what happens if the result is uncertain? The AI can help people work through information. Deterministic code and authorized staff remain responsible for eligibility, money, liability, permissions, and safety.

The system also has to remember different things for different reasons. Business records describe the rental and its outcomes. Graph checkpoints describe workflow progress. Temporary model context helps with a current task. Audit evidence explains consequential decisions. A restart, cache eviction, lost telemetry export, or late response must not blur those roles or silently repeat an external effect.

That is the journey the series will build toward: Alex gets a car, returns it with an unresolved lost item and damage question, receives an accountable outcome, and leaves the fleet ready for the next customer. The technical chapters show how to make each promise explicit, testable, and recoverable.
