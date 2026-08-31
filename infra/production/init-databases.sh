#!/bin/sh
set -eu

required_variables="
USER_DATABASE_PASSWORD
WATCHLIST_DATABASE_PASSWORD
INGESTION_DATABASE_PASSWORD
NOTIFICATION_DATABASE_PASSWORD
"

for variable_name in $required_variables; do
    variable_value="$(printenv "$variable_name" || true)"

    if [ -z "$variable_value" ]; then
        echo "Required environment variable $variable_name is empty" >&2
        exit 1
    fi
done

psql \
    --variable=ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname postgres \
    --set=user_database_password="$USER_DATABASE_PASSWORD" \
    --set=watchlist_database_password="$WATCHLIST_DATABASE_PASSWORD" \
    --set=ingestion_database_password="$INGESTION_DATABASE_PASSWORD" \
    --set=notification_database_password="$NOTIFICATION_DATABASE_PASSWORD" <<'SQL'
CREATE USER user_service WITH PASSWORD :'user_database_password';
CREATE DATABASE user_service OWNER user_service;

CREATE USER watchlist_service WITH PASSWORD :'watchlist_database_password';
CREATE DATABASE watchlist_service OWNER watchlist_service;

CREATE USER ingestion_service WITH PASSWORD :'ingestion_database_password';
CREATE DATABASE ingestion_service OWNER ingestion_service;

CREATE USER notification_service WITH PASSWORD :'notification_database_password';
CREATE DATABASE notification_service OWNER notification_service;
SQL
