#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
case "${1:-}" in
    compose) execute=(docker compose -f deploy/step-2/compose.yaml exec -T postgres) ;;
    kind)
        [[ "${CONFIRM_KIND_CONTEXT:-}" == kind-mos-step-2 ]] || {
            echo "Confirm CONFIRM_KIND_CONTEXT=kind-mos-step-2." >&2; exit 1;
        }
        execute=(kubectl --context kind-mos-step-2 -n mos-step-2 exec -i deployment/postgres --)
        ;;
    *) echo "Usage: scripts/step-2-isolation.sh compose|kind" >&2; exit 2 ;;
esac
"${execute[@]}" sh -se <<'SH'
export PGSSLMODE=verify-full PGSSLROOTCERT=/tls/ca.crt
for owner in booking rental fleet; do
    PGPASSWORD=$(cat "/run/secrets/${owner}_app-password")
    export PGPASSWORD
    case "$owner" in
        booking) own_table=booking.booking_request ;;
        rental) own_table=rental.draft_rental ;;
        fleet) own_table=fleet.vehicle ;;
    esac
    psql -h localhost -U "${owner}_app" -d records -v ON_ERROR_STOP=1 \
        -c "SELECT count(*) FROM $own_table" >/dev/null
    for other in booking rental fleet; do
        [ "$owner" != "$other" ] || continue
        case "$other" in
            booking) table=booking.booking_request ;;
            rental) table=rental.draft_rental ;;
            fleet) table=fleet.vehicle ;;
        esac
        for sql in "SELECT * FROM $table" "DELETE FROM $table" "INSERT INTO $table DEFAULT VALUES"; do
            if result=$(psql -h localhost -U "${owner}_app" -d records -v ON_ERROR_STOP=1 \
                -v VERBOSITY=sqlstate -c "$sql" 2>&1); then
                echo "Unexpected cross-owner permission: $owner -> $other" >&2
                exit 1
            fi
            printf '%s' "$result" | grep -q '42501' || {
                echo "Failure was not a permission denial: $owner -> $other: $result" >&2
                exit 1
            }
        done
    done
    for sql in "CREATE TABLE $owner.forbidden (id int)" "CREATE TEMP TABLE forbidden (id int)" "SET ROLE ${owner}_migrator" \
        "SELECT * FROM $owner.flyway_schema_history"; do
        if result=$(psql -h localhost -U "${owner}_app" -d records -v ON_ERROR_STOP=1 \
            -v VERBOSITY=sqlstate -c "$sql" 2>&1); then
            echo "Unexpected runtime authority: $owner" >&2
            exit 1
        fi
        printf '%s' "$result" | grep -q '42501' || {
            echo "Failure was not a permission denial: $result" >&2
            exit 1
        }
    done
done
echo "PASS: all six cross-owner read/write pairs denied; runtime DDL, role assumption and migration-history reads denied."
SH
