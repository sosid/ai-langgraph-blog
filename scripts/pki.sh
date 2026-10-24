#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$(dirname "$0")/.."
directory=${PKI_DIR:-.local/step-1}
mkdir -p "$directory"
mode=${1:-init}
if [[ "$mode" != init && "$mode" != renew ]]; then
    echo "Usage: scripts/pki.sh [init|renew]" >&2
    exit 2
fi
if [[ "$mode" == init && -e "$directory/ca.key" ]]; then
    echo "PKI already exists; use renew to replace server certificates, preserving the CA." >&2
    exit 1
fi
if [[ "$mode" == renew && ! -f "$directory/ca.key" ]]; then
    echo "No local CA; run init first." >&2
    exit 1
fi
if [[ "$mode" == init ]]; then
    openssl req -x509 -newkey rsa:3072 -nodes -sha256 -days 365 \
        -subj "/CN=Miles of Smiles Step 1 Development CA" \
        -addext "basicConstraints=critical,CA:TRUE,pathlen:0" \
        -addext "keyUsage=critical,keyCertSign,cRLSign" \
        -keyout "$directory/ca.key" -out "$directory/ca.crt" 2>/dev/null
    openssl rand -hex 24 > "$directory/db-password"
fi
for service in app postgres; do
    destination="$directory/$service"
    mkdir -p "$destination"
    openssl req -new -newkey rsa:2048 -nodes -subj "/CN=$service" \
        -keyout "$destination/server.key" -out "$destination/server.csr" 2>/dev/null
    printf '%s\n' \
        "basicConstraints=critical,CA:FALSE" \
        "keyUsage=critical,digitalSignature,keyEncipherment" \
        "extendedKeyUsage=serverAuth" \
        "subjectAltName=DNS:localhost,DNS:$service,DNS:$service.mos-step-1.svc,DNS:booking.127.0.0.1.nip.io,DNS:booking.127.0.0.1.sslip.io,IP:127.0.0.1" \
        > "$destination/extensions"
    openssl x509 -req -sha256 -days 30 -in "$destination/server.csr" \
        -CA "$directory/ca.crt" -CAkey "$directory/ca.key" -CAcreateserial \
        -extfile "$destination/extensions" -out "$destination/server.crt" 2>/dev/null
    cp "$directory/ca.crt" "$destination/ca.crt"
    chmod 640 "$destination/server.key"
    chmod 644 "$destination/server.crt" "$destination/ca.crt"
    chmod 750 "$destination"
    rm "$destination/server.csr" "$destination/extensions"
done
chmod 644 "$directory/ca.crt"
chmod 640 "$directory/db-password"
echo "Development PKI ready in $directory; CA is not installed in system trust."
