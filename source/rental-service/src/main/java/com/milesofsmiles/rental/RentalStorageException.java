package com.milesofsmiles.rental;

import java.sql.SQLException;

public class RentalStorageException extends RuntimeException {
    public RentalStorageException(SQLException cause) {
        super("Rental storage is unavailable", cause);
    }

    public String sqlState() {
        return ((SQLException) getCause()).getSQLState();
    }
}
