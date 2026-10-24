package com.milesofsmiles.booking;

import java.time.Instant;
import java.util.UUID;

public record BookingRequest(UUID id, String reference, Instant createdAt) {
    public static final UUID FIXTURE_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    public static final String FIXTURE_REFERENCE = "SYNTHETIC-ALEX-WEEKEND";
}
