#!/bin/sh
set -eu
if [ -n "${DB_PASSWORD_FILE:-}" ]; then
    DB_PASSWORD=$(cat "$DB_PASSWORD_FILE")
    export DB_PASSWORD
fi
exec java -jar /app/quarkus-app/quarkus-run.jar
