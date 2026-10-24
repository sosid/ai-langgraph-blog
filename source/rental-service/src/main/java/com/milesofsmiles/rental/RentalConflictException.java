package com.milesofsmiles.rental;

public class RentalConflictException extends RuntimeException {
    public RentalConflictException() {
        super("Request key already belongs to a different payload");
    }
}
