#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
context=kind-mos-step-1
if [[ "${CONFIRM_KIND_CONTEXT:-}" != "$context" ]]; then
    echo "Confirm cluster-wide controller installation with CONFIRM_KIND_CONTEXT=$context." >&2
    exit 1
fi
scripts/check-kind-prerequisites.sh
mkdir -p .local/step-1
manifest=.local/step-1/cert-manager-v1.21.2.yaml
curl -fsSL https://github.com/cert-manager/cert-manager/releases/download/v1.21.2/cert-manager.yaml -o "$manifest"
echo "e03b668ec8675214af6b0a671699d088f2601fa3878e0dbe1b41d3feafd1879f  $manifest" | sha256sum -c -
kubectl --context "$context" apply -f "$manifest"
kubectl --context "$context" -n cert-manager set image deployment/cert-manager \
    cert-manager-controller=quay.io/jetstack/cert-manager-controller:v1.21.2@sha256:70f532fd9cfde0b09d55687200942399d89838bc2d5d5b45152eb799a15912b8
kubectl --context "$context" -n cert-manager set image deployment/cert-manager-cainjector \
    cert-manager-cainjector=quay.io/jetstack/cert-manager-cainjector:v1.21.2@sha256:c85268c64f2e0e76684bf5fe8906caff34b82523561c6affe0fae3546bd87562
kubectl --context "$context" -n cert-manager set image deployment/cert-manager-webhook \
    cert-manager-webhook=quay.io/jetstack/cert-manager-webhook:v1.21.2@sha256:a60e2dac46dbb8a7f3df95c54ce941012f54c2fe022f0ee55aaa1ab40ed957ae
for component in cert-manager cert-manager-cainjector cert-manager-webhook; do
    kubectl --context "$context" -n cert-manager set resources "deployment/$component" \
        --requests=cpu=50m,memory=64Mi --limits=cpu=500m,memory=256Mi
    kubectl --context "$context" -n cert-manager rollout status "deployment/$component" --timeout=180s
done
