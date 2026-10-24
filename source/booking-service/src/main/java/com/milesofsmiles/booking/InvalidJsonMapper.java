package com.milesofsmiles.booking;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InvalidJsonMapper implements ExceptionMapper<MismatchedInputException> {
    @Override
    public Response toResponse(MismatchedInputException failure) {
        return Response.status(400).entity(new BookingResource.ApiError(
                "INVALID_REQUEST", "A valid version 1 JSON request is required")).build();
    }
}
