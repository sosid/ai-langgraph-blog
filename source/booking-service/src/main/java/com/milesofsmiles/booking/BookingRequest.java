package com.milesofsmiles.booking;

import java.time.Instant;
import java.util.UUID;

/**
 * A synthetic stored request, without eligibility, inventory, or payment authority.
 *
 * @param id stable request identifier
 * @param reference synthetic customer-scenario reference
 * @param createdAt stored UTC creation instant
 */
public record BookingRequest(UUID id, String reference, Instant createdAt) {
    public static final UUID FIXTURE_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    public static final String FIXTURE_REFERENCE = "SYNTHETIC-ALEX-WEEKEND";
}
