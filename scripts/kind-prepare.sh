#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
context=kind-mos-step-1
if [[ "${CONFIRM_KIND_CONTEXT:-}" != "$context" ]]; then
    echo "Confirm the dedicated context with CONFIRM_KIND_CONTEXT=$context." >&2
    exit 1
fi
scripts/check-kind-prerequisites.sh
kubectl --context "$context" get node mos-step-1-control-plane >/dev/null
kubectl --context "$context" apply -f deploy/step-1/kind/namespace.yaml
docker exec mos-step-1-control-plane install -d -m 700 -o 70 -g 70 /var/local/mos-step-1/postgres
kubectl --context "$context" apply -f deploy/step-1/kind/storage.yaml
kubectl --context "$context" -n mos-step-1 create secret tls development-ca \
    --cert=.local/step-1/ca.crt --key=.local/step-1/ca.key \
    --dry-run=client -o yaml | kubectl --context "$context" apply -f -
kubectl --context "$context" -n mos-step-1 create secret generic database \
    --from-file=password=.local/step-1/db-password \
    --dry-run=client -o yaml | kubectl --context "$context" apply -f -
kubectl --context "$context" apply -f deploy/step-1/kind/certificates.yaml
kubectl --context "$context" -n mos-step-1 wait --for=condition=Ready certificate/app certificate/postgres --timeout=120s
kubectl --context "$context" apply -f deploy/step-1/kind/workloads.yaml
kubectl --context "$context" -n mos-step-1 rollout status deployment/postgres --timeout=120s
kubectl --context "$context" -n mos-step-1 rollout status deployment/app --timeout=120s
kubectl --context "$context" apply -f deploy/step-1/kind/haproxy.yaml
kubectl --context "$context" apply -f deploy/step-1/kind/ingress.yaml
kubectl --context "$context" -n mos-step-1 rollout status deployment/haproxy --timeout=120s
