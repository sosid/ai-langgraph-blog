#!/bin/sh
set -eu
DB_PASSWORD=$(cat "${DB_PASSWORD_FILE:?Set DB_PASSWORD_FILE}")
export DB_PASSWORD
if [ -n "${MIGRATION_MAIN:-}" ]; then
    exec java -cp '/app/quarkus-app/app/*:/app/quarkus-app/lib/main/*' "$MIGRATION_MAIN"
fi
exec java -jar /app/quarkus-app/quarkus-run.jar
