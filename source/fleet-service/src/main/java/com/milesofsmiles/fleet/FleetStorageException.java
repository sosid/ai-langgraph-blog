package com.milesofsmiles.fleet;

import java.sql.SQLException;

public class FleetStorageException extends RuntimeException {
    private final String sqlState;

    public FleetStorageException(SQLException cause) {
        super("Fleet storage is unavailable", cause);
        sqlState = cause.getSQLState();
    }

    public String sqlState() {
        return sqlState;
    }
}
