package com.milesofsmiles.booking;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class BookingStorageExceptionMapper implements ExceptionMapper<BookingStorageException> {
    private static final Logger LOG = Logger.getLogger(BookingStorageExceptionMapper.class);

    @Override
    public Response toResponse(BookingStorageException failure) {
        LOG.errorf("Booking storage unavailable (SQL state %s)", failure.sqlState());
        return Response.status(503)
                .entity(new BookingResource.ApiError("STORAGE_UNAVAILABLE", "Booking storage is unavailable"))
                .build();
    }
}
