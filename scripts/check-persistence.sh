#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mode=${1:-compose}
sql="COMMENT ON TABLE booking_request_fixture IS 'STEP-1-PERSISTENCE-EVIDENCE'"
query="SELECT obj_description('booking_request_fixture'::regclass)"
if [[ "$mode" == compose ]]; then
    compose=(docker compose -f deploy/step-1/compose.yaml)
    "${compose[@]}" exec -T postgres psql -U booking -d booking -v ON_ERROR_STOP=1 -c "$sql"
    "${compose[@]}" restart app
    "${compose[@]}" up -d --wait --wait-timeout 120
    "${compose[@]}" restart postgres
    "${compose[@]}" up -d --wait --wait-timeout 120
    result=$("${compose[@]}" exec -T postgres psql -U booking -d booking -Atc "$query")
    port=8443
elif [[ "$mode" == kind ]]; then
    context=kind-mos-step-1
    if [[ "${CONFIRM_KIND_CONTEXT:-}" != "$context" ]]; then
        echo "Confirm restart operations with CONFIRM_KIND_CONTEXT=$context." >&2
        exit 1
    fi
    kubectl=(kubectl --context "$context" -n mos-step-1)
    "${kubectl[@]}" exec deployment/postgres -- psql -U booking -d booking -v ON_ERROR_STOP=1 -c "$sql"
    "${kubectl[@]}" rollout restart deployment/app
    "${kubectl[@]}" rollout status deployment/app --timeout=120s
    "${kubectl[@]}" rollout restart deployment/postgres
    "${kubectl[@]}" rollout status deployment/postgres --timeout=120s
    "${kubectl[@]}" rollout status deployment/app --timeout=120s
    "${kubectl[@]}" wait --for=condition=Ready pod -l app=booking --timeout=120s
    result=$("${kubectl[@]}" exec deployment/postgres -- psql -U booking -d booking -Atc "$query")
    port=8444
else
    echo "Usage: scripts/check-persistence.sh [compose|kind]" >&2
    exit 2
fi
test "$result" = STEP-1-PERSISTENCE-EVIDENCE
scripts/smoke.sh "$port"
echo "Application/database restarts preserved a database-only marker and the API fixture: PASS"
