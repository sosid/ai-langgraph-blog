package com.milesofsmiles.booking;

import java.sql.SQLException;

public class BookingStorageException extends RuntimeException {
    public BookingStorageException(SQLException cause) {
        super("Booking storage is unavailable", cause);
    }

    public String sqlState() {
        return ((SQLException) getCause()).getSQLState();
    }
}
