#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ "${CONFIRM_KIND_CONTEXT:-}" != kind-mos-step-1 ]]; then
    echo "Confirm image loading with CONFIRM_KIND_CONTEXT=kind-mos-step-1." >&2
    exit 1
fi
# Import only the node platform without requiring unavailable multi-arch layers.
for image in \
    miles-of-smiles/booking:step-1 \
    postgres:17.6-alpine@sha256:ef257d85f76e48da1c64832459b59fcaba1a4dac97bf5d7450c77753542eee94 \
    haproxytech/kubernetes-ingress:3.2.18@sha256:edcf448e0439b7ac13a5964813ee1227cee04fc0a6bbb943d4408a243f7cc39a \
    quay.io/jetstack/cert-manager-controller:v1.21.2@sha256:70f532fd9cfde0b09d55687200942399d89838bc2d5d5b45152eb799a15912b8 \
    quay.io/jetstack/cert-manager-cainjector:v1.21.2@sha256:c85268c64f2e0e76684bf5fe8906caff34b82523561c6affe0fae3546bd87562 \
    quay.io/jetstack/cert-manager-webhook:v1.21.2@sha256:a60e2dac46dbb8a7f3df95c54ce941012f54c2fe022f0ee55aaa1ab40ed957ae; do
    if [[ "$image" != miles-of-smiles/booking:step-1 ]]; then
        docker pull "$image"
    fi
    docker image save "$image" | docker exec --privileged -i mos-step-1-control-plane \
        ctr -n k8s.io images import --platform linux/amd64 --digests - >/dev/null
done
