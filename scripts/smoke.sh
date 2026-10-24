#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
port=${1:-8443}
id=00000000-0000-4000-8000-000000000001
for domain in nip.io sslip.io; do
    host="booking.127.0.0.1.$domain"
    args=(--noproxy '*' --connect-timeout 3 --max-time 10 --cacert .local/step-1/ca.crt
        --resolve "$host:$port:127.0.0.1")
    payload=$(curl "${args[@]}" -fsS "https://$host:$port/api/booking-requests/$id")
    python3 -c 'import json,sys
value=json.loads(sys.argv[1])
assert value == {"id":sys.argv[2], "reference":"SYNTHETIC-ALEX-WEEKEND", "createdAt":"2026-01-02T10:00:00Z"}, value
print(value)' "$payload" "$id"
    curl "${args[@]}" -fsS "https://$host:$port/q/health/live"
    curl "${args[@]}" -fsS "https://$host:$port/q/health/ready"
    for pair in "not-a-uuid:400" "00000000-0000-4000-8000-000000000099:404"; do
        identifier=${pair%:*}
        expected=${pair##*:}
        code=$(curl "${args[@]}" -sS -o /dev/null -w '%{http_code}' "https://$host:$port/api/booking-requests/$identifier")
        test "$code" = "$expected"
    done
done
echo "Both hostnames, verified TLS, health, fixture, invalid and absent identifiers: PASS"
