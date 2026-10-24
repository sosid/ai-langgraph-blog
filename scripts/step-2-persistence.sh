#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mode=${1:-}
case "$mode" in
    compose) execute=(docker compose -f deploy/step-2/compose.yaml exec -T postgres) ;;
    kind)
        [[ "${CONFIRM_KIND_CONTEXT:-}" == kind-mos-step-2 ]] || {
            echo "Confirm CONFIRM_KIND_CONTEXT=kind-mos-step-2." >&2; exit 1;
        }
        k=(kubectl --context kind-mos-step-2 -n mos-step-2)
        execute=("${k[@]}" exec -i deployment/postgres --)
        ;;
    *) echo "Usage: scripts/step-2-persistence.sh compose|kind" >&2; exit 2 ;;
esac
query() {
    "${execute[@]}" sh -ec '
        PGPASSWORD=$(cat /run/secrets/db-password)
        export PGPASSWORD PGSSLMODE=verify-full PGSSLROOTCERT=/tls/ca.crt
        exec psql -h localhost -U bootstrap -d records -v ON_ERROR_STOP=1 -At
    '
}
marker="SYNTHETIC-RESTART-$(python3 -c 'import uuid; print(uuid.uuid4())')"
printf "COMMENT ON TABLE booking.booking_request IS '%s';\n" "$marker" | query >/dev/null
snapshot() {
    query <<'SQL'
SELECT obj_description('booking.booking_request'::regclass, 'pg_class');
SELECT count(*), md5(coalesce(string_agg(id::text || request_hash, ',' ORDER BY id), ''))
    FROM booking.booking_request;
SELECT count(*), md5(coalesce(string_agg(id::text || request_hash, ',' ORDER BY id), ''))
    FROM rental.draft_rental;
SELECT count(*), md5(coalesce(string_agg(id::text || reference, ',' ORDER BY id), ''))
    FROM fleet.vehicle;
SQL
}
before=$(snapshot)
[[ "$before" == "$marker"* ]] || { echo "Marker was not persisted." >&2; exit 1; }
for owner in booking rental fleet postgres; do
    if [[ "$mode" == compose ]]; then
        docker compose -f deploy/step-2/compose.yaml restart "$owner" >/dev/null
        docker compose -f deploy/step-2/compose.yaml up -d --wait --wait-timeout 120 >/dev/null
    else
        "${k[@]}" rollout restart "deployment/$owner"
        "${k[@]}" rollout status "deployment/$owner" --timeout=120s
    fi
    after=$(snapshot)
    [[ "$before" == "$after" ]] || {
        echo "Persistent marker/record identities changed after $owner restart." >&2
        exit 1
    }
    echo "PASS: marker and record identities/hashes survived $owner restart."
done
echo "Reopen any port-forwards terminated by pod replacement, then run the smoke script."
