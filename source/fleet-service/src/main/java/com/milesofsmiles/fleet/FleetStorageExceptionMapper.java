package com.milesofsmiles.fleet;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class FleetStorageExceptionMapper implements ExceptionMapper<FleetStorageException> {
    private static final Logger LOG = Logger.getLogger(FleetStorageExceptionMapper.class);

    @Override
    public Response toResponse(FleetStorageException failure) {
        LOG.errorf("Fleet storage unavailable (SQL state %s)", failure.sqlState());
        return Response.status(503)
                .entity(new VehicleResource.ApiError("STORAGE_UNAVAILABLE", "Fleet storage is unavailable"))
                .build();
    }
}
