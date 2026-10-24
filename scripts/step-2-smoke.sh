#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
booking=${BOOKING_URL:-https://localhost:8543}
rental=${RENTAL_URL:-https://localhost:8544}
fleet=${FLEET_URL:-https://localhost:8545}
ca=${CA_FILE:-.local/step-2/ca.crt}
key=${SMOKE_KEY:-"smoke-$(date +%s)-$RANDOM"}
connections=()
for mapping in "${BOOKING_CONNECT_TO:-}" "${RENTAL_CONNECT_TO:-}" "${FLEET_CONNECT_TO:-}"; do
    [[ -z "$mapping" ]] || connections+=(--connect-to "$mapping")
done
work=$(mktemp -d)
trap 'rm -f "$work/body" "$work/headers" "$work/booking-id" "$work/rental-id"; rmdir "$work"' EXIT
request() {
    expected=$1
    shift
    status=$(curl --noproxy '*' --silent --show-error --max-time 15 --cacert "$ca" "${connections[@]}" \
        -D "$work/headers" -o "$work/body" -w '%{http_code}' "$@")
    if [[ "$status" != "$expected" ]]; then
        echo "Expected HTTP $expected, got $status: $(cat "$work/body")" >&2
        exit 1
    fi
}
for url in "$booking" "$rental" "$fleet"; do
    request 200 "$url/q/health/ready"
    request 200 "$url/q/health/live"
done
request 200 "$booking/api/bookings/00000000-0000-4000-8000-000000000001"
request 200 "$fleet/api/v1/vehicles/00000000-0000-4000-8000-000000000002"
request 400 "$fleet/api/v1/vehicles/1-1-1-1-1"
request 404 "$fleet/api/v1/vehicles/00000000-0000-4000-8000-000000000099"
payload='{"contractVersion":1,"vehicleClass":"COMPACT","startsAt":"2026-11-01T10:00:00Z","endsAt":"2026-11-03T10:00:00Z"}'
request 201 -H 'Content-Type: application/json' -H "Idempotency-Key: $key" \
    -d "$payload" "$booking/api/v1/booking-requests"
python3 -c 'import json,sys; x=json.load(open(sys.argv[1])); assert x["contractVersion"]==1; print(x["id"])' \
    "$work/body" > "$work/booking-id"
booking_id=$(cat "$work/booking-id")
request 200 -H 'Content-Type: application/json' -H "Idempotency-Key: $key" \
    -d "$payload" "$booking/api/v1/booking-requests"
python3 -c 'import json,sys; assert json.load(open(sys.argv[1]))["id"]==sys.argv[2]' "$work/body" "$booking_id"
grep -qi '^Idempotency-Replayed: true' "$work/headers"
request 409 -H 'Content-Type: application/json' -H "Idempotency-Key: $key" \
    -d "${payload/2026-11-03/2026-11-04}" "$booking/api/v1/booking-requests"
request 200 "$booking/api/v1/booking-requests/$booking_id"
rental_payload="{\"contractVersion\":1,\"bookingId\":\"$booking_id\",\"vehicleId\":\"00000000-0000-4000-8000-000000000002\"}"
request 201 -H 'Content-Type: application/json' -H "Idempotency-Key: $key" \
    -d "$rental_payload" "$rental/api/v1/rentals"
python3 -c 'import json,sys; x=json.load(open(sys.argv[1])); assert x["state"]=="DRAFT"; print(x["id"])' \
    "$work/body" > "$work/rental-id"
rental_id=$(cat "$work/rental-id")
request 200 -H 'Content-Type: application/json' -H "Idempotency-Key: $key" \
    -d "$rental_payload" "$rental/api/v1/rentals"
python3 -c 'import json,sys; assert json.load(open(sys.argv[1]))["id"]==sys.argv[2]' "$work/body" "$rental_id"
grep -qi '^Idempotency-Replayed: true' "$work/headers"
request 409 -H 'Content-Type: application/json' -H "Idempotency-Key: $key" \
    -d "${rental_payload/000000000002/000000000003}" "$rental/api/v1/rentals"
request 200 "$rental/api/v1/rentals/$rental_id"
printf 'PASS: trusted health, legacy fixture, versioned records, replay/conflict, vehicle, invalid/missing identifiers.\n'
printf 'booking=%s rental=%s\n' "$booking_id" "$rental_id"
