package com.milesofsmiles.fleet;

import java.sql.SQLException;

public class IntervalConflictException extends RuntimeException {
    public IntervalConflictException(SQLException cause) {
        super("Blocking interval overlaps an existing interval", cause);
    }
}
