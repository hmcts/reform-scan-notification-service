#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER notification_service;
    CREATE DATABASE notification_service
        WITH OWNER = notification_service
        ENCODING ='UTF-8'
        CONNECTION LIMIT = -1;
EOSQL

psql -v ON_ERROR_STOP=1 --dbname=notification_service --username "$POSTGRES_USER" <<-EOSQL
    CREATE SCHEMA notification_service AUTHORIZATION notification_service;
EOSQL

psql -v ON_ERROR_STOP=1 --dbname=notification_service --username "$POSTGRES_USER" <<-EOSQL
    CREATE EXTENSION lo;
EOSQL
