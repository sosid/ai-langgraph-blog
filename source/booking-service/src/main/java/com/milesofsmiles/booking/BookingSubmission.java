package com.milesofsmiles.booking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;

/** Contract version and UTC microsecond precision are part of the hash identity. */
public record BookingSubmission(int contractVersion, String vehicleClass, String startsAt, String endsAt) {
    public Validated validate(String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,128}")
                || contractVersion != 1 || !"COMPACT".equals(vehicleClass)) {
            throw new IllegalArgumentException("Use version 1, COMPACT and a valid Idempotency-Key");
        }
        try {
            Instant start = Instant.parse(startsAt);
            Instant end = Instant.parse(endsAt);
            if (!start.isBefore(end) || start.getNano() % 1_000 != 0 || end.getNano() % 1_000 != 0
                    || start.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
                    || end.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z"))) {
                throw new IllegalArgumentException("Use a positive UTC window at microsecond precision");
            }
            return new Validated(start, end, hash("contractVersion=1\nvehicleClass=COMPACT\nstartsAt="
                    + start + "\nendsAt=" + end + "\n"));
        } catch (DateTimeParseException | NullPointerException invalid) {
            throw new IllegalArgumentException("Use ISO-8601 instants", invalid);
        }
    }

    private static String hash(String canonical) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 unavailable", unavailable);
        }
    }

    public record Validated(Instant start, Instant end, String hash) {
    }
}
