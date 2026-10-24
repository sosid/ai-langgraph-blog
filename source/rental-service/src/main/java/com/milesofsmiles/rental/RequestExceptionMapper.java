package com.milesofsmiles.rental;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RequestExceptionMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException failure) {
        // Quarkus wraps JSON stream/parser failures in a plain WebApplicationException.
        return failure.getResponse().getStatus() == 400
                ? RentalResource.error(400, "INVALID_REQUEST", "A valid version 1 JSON request is required")
                : failure.getResponse();
    }
}
