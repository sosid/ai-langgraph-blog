package com.milesofsmiles.booking;

import java.time.Instant;
import java.util.UUID;

/** A request record, not a confirmed booking. */
public record BookingRecord(UUID id, int contractVersion, String vehicleClass,
        Instant startsAt, Instant endsAt, Instant createdAt) {
}
