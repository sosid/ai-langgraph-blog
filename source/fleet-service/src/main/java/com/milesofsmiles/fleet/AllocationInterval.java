package com.milesofsmiles.fleet;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Storage windows only; this record does not implement a hold lifecycle. */
public record AllocationInterval(UUID id, UUID vehicleId, Instant startsAt, Instant endsAt, Status status) {
    public enum Status { BLOCKING, RELEASED }

    public AllocationInterval {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(vehicleId, "vehicleId");
        Objects.requireNonNull(startsAt, "startsAt");
        Objects.requireNonNull(endsAt, "endsAt");
        Objects.requireNonNull(status, "status");
        // Bound timestamps to four-digit UTC years and PostgreSQL's microsecond precision.
        Instant lower = Instant.parse("0001-01-01T00:00:00Z");
        Instant upper = Instant.parse("9999-12-31T23:59:59.999999Z");
        if (startsAt.isBefore(lower) || endsAt.isAfter(upper) || !startsAt.isBefore(endsAt)
                || startsAt.getNano() % 1_000 != 0 || endsAt.getNano() % 1_000 != 0) {
            throw new IllegalArgumentException("Use a bounded microsecond UTC interval with start before end");
        }
    }
}
