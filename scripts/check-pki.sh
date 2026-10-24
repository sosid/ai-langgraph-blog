#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p target
directory=$(mktemp -d "$PWD/target/pki-check-XXXXXX")
PKI_DIR="$directory" scripts/pki.sh
test "$(stat -c '%a' "$directory/ca.key")" = 600
test "$(stat -c '%a' "$directory/app/server.key")" = 640
for host in booking.127.0.0.1.nip.io booking.127.0.0.1.sslip.io; do
    openssl verify -CAfile "$directory/ca.crt" -verify_hostname "$host" "$directory/app/server.crt"
done
if openssl verify -CAfile "$directory/ca.crt" -verify_hostname wrong.invalid "$directory/app/server.crt"; then
    echo "Wrong hostname unexpectedly accepted" >&2
    exit 1
fi
future=$(date -u -d '+31 days' +%s)
if openssl verify -attime "$future" -CAfile "$directory/ca.crt" "$directory/app/server.crt"; then
    echo "Expired certificate unexpectedly accepted" >&2
    exit 1
fi
before_ca=$(sha256sum "$directory/ca.crt")
before_serial=$(openssl x509 -in "$directory/app/server.crt" -noout -serial)
PKI_DIR="$directory" scripts/pki.sh renew
test "$before_ca" = "$(sha256sum "$directory/ca.crt")"
test "$before_serial" != "$(openssl x509 -in "$directory/app/server.crt" -noout -serial)"
openssl verify -CAfile "$directory/ca.crt" "$directory/app/server.crt"
echo "PKI permissions, hostnames, expiry rejection, CA-preserving renewal: PASS"
