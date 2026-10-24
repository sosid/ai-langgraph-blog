package com.milesofsmiles.booking;

import java.sql.SQLException;

/** Distinguishes a failed database operation from an absent booking request. */
public class BookingStorageException extends RuntimeException {
    public BookingStorageException(SQLException cause) {
        super("Booking storage is unavailable", cause);
    }

    /**
     * Returns the provider's SQL state for logging without its exception message.
     *
     * @return SQL state, or {@code null} if the provider did not supply one
     */
    public String sqlState() {
        return ((SQLException) getCause()).getSQLState();
    }
}
