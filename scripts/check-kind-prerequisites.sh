#!/usr/bin/env bash
set -euo pipefail
context=kind-mos-step-1
if [[ "${CONFIRM_KIND_CONTEXT:-}" != "$context" ]]; then
    echo "Confirm the dedicated context with CONFIRM_KIND_CONTEXT=$context." >&2
    exit 1
fi
kubectl --context "$context" get node mos-step-1-control-plane >/dev/null
if ! docker exec mos-step-1-control-plane sh -eu -c '
    chain="MOS-PROBE-$$"
    iptables -t filter -N "$chain"
    trap '"'"'iptables -t filter -F "$chain"; iptables -t filter -X "$chain"'"'"' EXIT
    iptables -t filter -A "$chain" -m comment --comment mos-step-1-prerequisite -j RETURN
'; then
    echo "Blocked: this kind node cannot use the iptables comment match required by kube-proxy." >&2
    echo "Use a Docker host/kernel supporting kind networking; do not disable routing or security checks." >&2
    exit 1
fi
echo "kind iptables comment-match prerequisite: PASS (not an end-to-end network check)"
