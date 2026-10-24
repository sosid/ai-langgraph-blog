# Step 2 HTTP Contracts

These unauthenticated synthetic lab APIs use JSON over HTTPS. They do not
establish authenticated tenant scope. Version 1 is in the path and the payload;
unknown fields, unsupported versions, coercible-but-wrong JSON types, duplicate
fields, trailing JSON documents, and invalid values are rejected.

Both write endpoints require `Content-Type: application/json` and
`Idempotency-Key` matching `[A-Za-z0-9._:-]{1,128}`. Each service has its own key
namespace. The maximum request body is 16 KiB. No payment or external operation
is invoked.

## Booking

`POST /api/v1/booking-requests`:

```json
{"contractVersion":1,"vehicleClass":"COMPACT","startsAt":"2026-11-01T10:00:00Z","endsAt":"2026-11-03T10:00:00Z"}
```

All fields are required. The interval must be positive; instants normalize to
UTC, use at most microsecond precision, and fall within years 0001 through 9999.
Only `COMPACT` is supported. JSON property order and equivalent instant
representations do not change identity.

The canonical UTF-8 input to SHA-256 is the following ordered text, including
the final LF:

```text
contractVersion=1
vehicleClass=COMPACT
startsAt=2026-11-01T10:00:00Z
endsAt=2026-11-03T10:00:00Z
```

`GET /api/v1/booking-requests/{id}` returns `id`, `contractVersion`,
`vehicleClass`, `startsAt`, `endsAt`, and `createdAt`. The ID is generated once;
the creation clock defaults to the deterministic Step 1 instant. Neither the
request key nor its hash is returned.

The original `GET /api/bookings/{id}` remains a separate read-only fixture
contract with exactly `id`, `reference`, and `createdAt`.

## Rental

`POST /api/v1/rentals`:

```json
{"contractVersion":1,"bookingId":"00000000-0000-4000-8000-000000000001","vehicleId":"00000000-0000-4000-8000-000000000002"}
```

All fields are required. IDs must use the canonical UUID shape; uppercase is
accepted and normalized to lowercase. The references are not remotely verified.
This endpoint cannot activate a rental.

The canonical UTF-8 SHA-256 input, including the final LF, is:

```text
contractVersion=1
bookingId=00000000-0000-4000-8000-000000000001
vehicleId=00000000-0000-4000-8000-000000000002
```

`GET /api/v1/rentals/{id}` returns `id`, `contractVersion`, `bookingId`,
`vehicleId`, `state` (always `DRAFT`), and `createdAt`. Keys/hashes are not
returned. The deterministic default clock matches booking.

## Write and read outcomes

| Outcome | HTTP / contract |
| --- | --- |
| First write | 201; stored record; `Location` identifying the stored resource (resolved to an absolute URL by the HTTP runtime); `Idempotency-Replayed: false` |
| Same key and same semantic payload | 200; original ID/time/body; `Idempotency-Replayed: true` |
| Same key and different semantic payload | 409; `REQUEST_KEY_CONFLICT`; original record unchanged |
| Invalid version, key, fields, window or body | 400; `INVALID_REQUEST` for mapped validation/type errors |
| Body above 16 KiB | 413 from the HTTP boundary |
| Read with malformed/noncanonical UUID | 400; `INVALID_ID` |
| Read with absent canonical UUID | 404; `NOT_FOUND` |
| Database operation failure | 503; `STORAGE_UNAVAILABLE`, never a missing-row or successful fallback |

Application error bodies have `code` and a safe `message`. HTTP infrastructure
errors such as body-size rejection need not share that JSON shape. No response
contains provider exception details. Malformed JSON syntax is an HTTP 400;
its infrastructure response is not a versioned business error.

Changing canonicalization requires a new compatible contract/migration design,
not silently recomputing hashes for accepted requests. Hashes detect payload
reuse; they do not authenticate a caller or make records tamper-evident.

## Fleet

`GET /api/v1/vehicles/00000000-0000-4000-8000-000000000002` returns:

```json
{"id":"00000000-0000-4000-8000-000000000002","reference":"SYNTHETIC-COMPACT-001","contractVersion":1}
```

Other read outcomes follow the UUID/404/storage rules above. There is no public
allocation-write endpoint. The owner repository stores UTC `[start, end)`
windows with `BLOCKING` or `RELEASED` status. Its exclusion constraint rejects
overlapping blocking windows for the same vehicle, allows adjacency/different
vehicles, and does not make released rows block capacity. This is a storage
contract, not a hold acquisition/expiry lifecycle.
