#!/bin/sh
set -eu
for name in booking_app booking_migrator rental_app rental_migrator fleet_app fleet_migrator; do
    value=$(cat "/run/secrets/$name-password")
    export "$name=$value"
done
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -f /bootstrap/bootstrap.sql
