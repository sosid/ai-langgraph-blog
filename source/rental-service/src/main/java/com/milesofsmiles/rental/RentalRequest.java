package com.milesofsmiles.rental;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

public record RentalRequest(Integer contractVersion, String bookingId, String vehicleId) {
    private static final Pattern UUID_TEXT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern REQUEST_KEY = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    public static UUID identifier(String value) {
        if (value == null || !UUID_TEXT.matcher(value).matches()) {
            throw new IllegalArgumentException("Canonical UUID required");
        }
        return UUID.fromString(value);
    }

    public Validated validate(String requestKey) {
        if (requestKey == null || !REQUEST_KEY.matcher(requestKey).matches()
                || contractVersion == null || contractVersion != 1) {
            throw new IllegalArgumentException("Invalid request key or contract version");
        }
        return new Validated(contractVersion, identifier(bookingId), identifier(vehicleId));
    }

    public record Validated(int contractVersion, UUID bookingId, UUID vehicleId) {
        /**
         * UTF-8, ordered named fields, lowercase canonical UUIDs, LF after every field.
         * Transport metadata and the request key are deliberately excluded.
         */
        public String canonical() {
            return "contractVersion=" + contractVersion + "\nbookingId=" + bookingId
                    + "\nvehicleId=" + vehicleId + "\n";
        }

        public String hash() {
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(canonical().getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException("Required SHA-256 implementation unavailable", impossible);
            }
        }
    }
}
