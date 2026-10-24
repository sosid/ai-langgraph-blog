CREATE TABLE rental.schema_version (
    singleton BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (singleton),
    contract_version INTEGER NOT NULL
);
INSERT INTO rental.schema_version (contract_version) VALUES (1);

CREATE TABLE rental.draft_rental (
    id UUID PRIMARY KEY,
    contract_version INTEGER NOT NULL CHECK (contract_version = 1),
    request_key VARCHAR(128) NOT NULL UNIQUE,
    request_hash CHAR(64) NOT NULL,
    booking_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    state VARCHAR(5) NOT NULL CHECK (state = 'DRAFT'),
    created_at TIMESTAMPTZ NOT NULL
);

REVOKE ALL ON rental.schema_version, rental.draft_rental FROM PUBLIC;
GRANT USAGE ON SCHEMA rental TO rental_app;
GRANT SELECT ON rental.schema_version TO rental_app;
GRANT SELECT, INSERT, UPDATE ON rental.draft_rental TO rental_app;
