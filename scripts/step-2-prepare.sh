#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
umask 077
directory=.local/step-2
if [[ -e "$directory" ]]; then
    echo "Refusing to overwrite $directory; preserve its credentials and persistent data." >&2
    exit 1
fi
PKI_DIR="$directory" scripts/pki.sh
for owner in booking rental fleet; do
    for identity in app migrator; do
        openssl rand -hex 24 > "$directory/${owner}_${identity}-password"
        chmod 640 "$directory/${owner}_${identity}-password"
    done
done
echo "Step 2 private inputs prepared. Existing Step 1 inputs are unchanged."
