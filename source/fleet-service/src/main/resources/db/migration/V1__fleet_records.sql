CREATE TABLE fleet.vehicle (
    id UUID PRIMARY KEY,
    reference VARCHAR(80) NOT NULL,
    contract_version INTEGER NOT NULL CHECK (contract_version = 1)
);

INSERT INTO fleet.vehicle (id, reference, contract_version)
VALUES ('00000000-0000-4000-8000-000000000002', 'SYNTHETIC-COMPACT-001', 1);

CREATE TABLE fleet.allocation_interval (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL REFERENCES fleet.vehicle(id),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(8) NOT NULL CHECK (status IN ('BLOCKING', 'RELEASED')),
    CHECK (starts_at < ends_at),
    CONSTRAINT allocation_interval_no_overlap EXCLUDE USING gist (
        vehicle_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    ) WHERE (status = 'BLOCKING')
);

CREATE TABLE fleet.schema_version (
    version INTEGER PRIMARY KEY CHECK (version = 1)
);
INSERT INTO fleet.schema_version VALUES (1);

GRANT USAGE ON SCHEMA fleet TO fleet_app;
GRANT SELECT, INSERT, UPDATE ON fleet.vehicle, fleet.allocation_interval TO fleet_app;
GRANT SELECT ON fleet.schema_version TO fleet_app;
