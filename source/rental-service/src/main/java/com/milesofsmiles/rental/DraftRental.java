package com.milesofsmiles.rental;

import java.time.Instant;
import java.util.UUID;

/** References are stored values, not evidence of remote existence or authorization. */
public record DraftRental(UUID id, int contractVersion, UUID bookingId, UUID vehicleId,
        String state, Instant createdAt) {
}
