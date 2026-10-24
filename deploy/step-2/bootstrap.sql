\getenv booking_app_password booking_app
\getenv booking_migrator_password booking_migrator
\getenv rental_app_password rental_app
\getenv rental_migrator_password rental_migrator
\getenv fleet_app_password fleet_app
\getenv fleet_migrator_password fleet_migrator

BEGIN;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
SELECT format('REVOKE CREATE, TEMPORARY ON DATABASE %I FROM PUBLIC', current_database())
\gexec
CREATE EXTENSION btree_gist WITH SCHEMA public;

CREATE ROLE booking_migrator LOGIN PASSWORD :'booking_migrator_password';
CREATE ROLE booking_app LOGIN PASSWORD :'booking_app_password';
CREATE ROLE rental_migrator LOGIN PASSWORD :'rental_migrator_password';
CREATE ROLE rental_app LOGIN PASSWORD :'rental_app_password';
CREATE ROLE fleet_migrator LOGIN PASSWORD :'fleet_migrator_password';
CREATE ROLE fleet_app LOGIN PASSWORD :'fleet_app_password';
CREATE SCHEMA booking AUTHORIZATION booking_migrator;
CREATE SCHEMA rental AUTHORIZATION rental_migrator;
CREATE SCHEMA fleet AUTHORIZATION fleet_migrator;
GRANT USAGE ON SCHEMA public TO booking_migrator, fleet_migrator;

DO $$
BEGIN
    IF to_regclass('public.booking_request_fixture') IS NOT NULL THEN
        ALTER TABLE public.booking_request_fixture OWNER TO booking_migrator;
    END IF;
END $$;
COMMIT;
