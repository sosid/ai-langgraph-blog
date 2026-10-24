#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
context=${CONFIRM_KIND_CONTEXT:?Confirm the existing kind context}
mode=${STEP2_NETWORK:-pod}
if [[ "$mode" != pod && "$mode" != host ]]; then
    echo "STEP2_NETWORK must be pod or the explicitly approved host fallback." >&2
    exit 2
fi
if [[ "$context" != kind-mos-step-2 ]]; then
    echo "This lab is scoped to kind-mos-step-2, namespace mos-step-2." >&2
    exit 1
fi
export KUBECONFIG=${KUBECONFIG:-"$PWD/.local/step-2/kubeconfig"}
k=(kubectl --context "$context" -n mos-step-2)
kubectl --context "$context" get node mos-step-2-control-plane >/dev/null
kubectl --context "$context" apply -f <(printf '%s\n' \
    'apiVersion: v1' 'kind: Namespace' 'metadata:' '  name: mos-step-2')
"${k[@]}" create configmap bootstrap \
    --from-file=bootstrap.sh=deploy/step-2/bootstrap.sh \
    --from-file=bootstrap.sql=deploy/step-2/bootstrap.sql \
    --from-file=pg_hba.conf=deploy/step-1/pg_hba.conf \
    --dry-run=client -o yaml | "${k[@]}" apply -f -
"${k[@]}" create secret generic bootstrap-credentials \
    --from-file=db-password=.local/step-2/db-password \
    --from-file=booking_app-password=.local/step-2/booking_app-password \
    --from-file=booking_migrator-password=.local/step-2/booking_migrator-password \
    --from-file=rental_app-password=.local/step-2/rental_app-password \
    --from-file=rental_migrator-password=.local/step-2/rental_migrator-password \
    --from-file=fleet_app-password=.local/step-2/fleet_app-password \
    --from-file=fleet_migrator-password=.local/step-2/fleet_migrator-password \
    --dry-run=client -o yaml | "${k[@]}" apply -f -
for service in app postgres; do
    "${k[@]}" create secret generic "$service-tls" \
        --from-file="server.crt=.local/step-2/$service/server.crt" \
        --from-file="server.key=.local/step-2/$service/server.key" \
        --from-file=ca.crt=.local/step-2/ca.crt \
        --dry-run=client -o yaml | "${k[@]}" apply -f -
done
for owner in booking rental fleet; do
    for pair in runtime:app migrator:migrator; do
        "${k[@]}" create secret generic "$owner-${pair%:*}" \
            --from-file="password=.local/step-2/${owner}_${pair#*:}-password" \
            --dry-run=client -o yaml | "${k[@]}" apply -f -
    done
done
for image in miles-of-smiles/booking:step-2 miles-of-smiles/rental:step-2 miles-of-smiles/fleet:step-2 \
    postgres:17.6-alpine@sha256:ef257d85f76e48da1c64832459b59fcaba1a4dac97bf5d7450c77753542eee94; do
    options=()
    if [[ "$image" == postgres:* ]]; then
        options=(--base-name docker.io/library/postgres)
    fi
    docker image save "$image" | docker exec -i mos-step-2-control-plane \
        ctr -n k8s.io images import --platform linux/amd64 --digests "${options[@]}" - >/dev/null
done
app_revision=$(docker image inspect miles-of-smiles/booking:step-2 miles-of-smiles/rental:step-2 \
    miles-of-smiles/fleet:step-2 --format '{{.Id}}' | LC_ALL=C sort | sha256sum | cut -c1-32)
database_revision=$(docker image inspect \
    postgres:17.6-alpine@sha256:ef257d85f76e48da1c64832459b59fcaba1a4dac97bf5d7450c77753542eee94 \
    --format '{{.Id}}' | cut -c8-39)
docker exec mos-step-2-control-plane sh -ec '
    path=/var/local/mos-step-2/postgres
    if [ ! -e "$path" ]; then
        install -d -o 70 -g 70 -m 0700 "$path"
    fi
    test "$(stat -c %u "$path")" = 70
'
"${k[@]}" create --dry-run=client -f deploy/step-2/kind/database.yaml -o json |
    python3 scripts/step-2-kind-render.py "$mode" "" "$database_revision" "$database_revision" |
    "${k[@]}" apply -f -
"${k[@]}" rollout status deployment/postgres --timeout=180s
database_ip=$("${k[@]}" get pods -l app=postgres -o jsonpath='{.items[0].status.podIP}')
if [[ ! "$database_ip" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "No usable IPv4 PostgreSQL pod address; refusing application deployment." >&2
    exit 1
fi
migration_revision=$(printf '%s:%s:%s' "$app_revision" "$database_revision" "$database_ip" |
    sha256sum | cut -c1-32)
for file in migrations services; do
    "${k[@]}" create --dry-run=client -f "deploy/step-2/kind/$file.yaml" -o json |
        python3 scripts/step-2-kind-render.py "$mode" "$database_ip" "$app_revision" "$migration_revision" |
        "${k[@]}" apply -f -
    if [[ "$file" == migrations ]]; then
        suffix=
        [[ "$mode" != host ]] || suffix=-host
        "${k[@]}" wait --for=condition=complete "job/booking-migrate$suffix-$migration_revision" \
            "job/rental-migrate$suffix-$migration_revision" \
            "job/fleet-migrate$suffix-$migration_revision" --timeout=180s
    fi
done
for owner in booking rental fleet; do
    "${k[@]}" rollout status "deployment/$owner" --timeout=180s
done
echo "Configured $mode networking. No ClusterIP, ingress, or NetworkPolicy guarantee is implied."
if [[ "$mode" == pod ]]; then
    echo "Pod mode recorded the current PostgreSQL pod address; reapply after a database pod replacement."
else
    echo "Single-node hostNetwork fallback: loopback DB and HTTPS on ports 8443, 8444, 8445."
fi
