DO $$
BEGIN
    IF to_regclass('public.booking_request_fixture') IS NOT NULL THEN
        ALTER TABLE public.booking_request_fixture SET SCHEMA booking;
    ELSE
        CREATE TABLE booking.booking_request_fixture (
            id UUID PRIMARY KEY,
            reference TEXT NOT NULL,
            created_at TIMESTAMPTZ NOT NULL
        );
    END IF;
END $$;

CREATE TABLE booking.schema_version (
    singleton BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (singleton),
    version INTEGER NOT NULL CHECK (version > 0)
);
INSERT INTO booking.schema_version (version) VALUES (1);

CREATE TABLE booking.booking_request (
    id UUID PRIMARY KEY,
    contract_version INTEGER NOT NULL CHECK (contract_version = 1),
    request_key VARCHAR(128) NOT NULL UNIQUE,
    request_hash CHAR(64) NOT NULL,
    vehicle_class VARCHAR(32) NOT NULL CHECK (vehicle_class = 'COMPACT'),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (starts_at < ends_at)
);

GRANT USAGE ON SCHEMA booking TO booking_app;
GRANT SELECT, INSERT, UPDATE ON booking.booking_request_fixture, booking.booking_request TO booking_app;
GRANT SELECT ON booking.schema_version TO booking_app;
