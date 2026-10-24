package com.milesofsmiles.rental;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class RentalStorageExceptionMapper implements ExceptionMapper<RentalStorageException> {
    private static final Logger LOG = Logger.getLogger(RentalStorageExceptionMapper.class);

    @Override
    public Response toResponse(RentalStorageException failure) {
        LOG.errorf("Rental storage unavailable (SQL state %s)", failure.sqlState());
        return RentalResource.error(503, "STORAGE_UNAVAILABLE", "Rental storage is unavailable");
    }
}
