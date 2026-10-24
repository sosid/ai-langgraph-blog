package com.milesofsmiles.fleet;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Path("/api/v1/vehicles")
@Produces(MediaType.APPLICATION_JSON)
public class VehicleResource {
    private final FleetRepository repository;

    @Inject
    public VehicleResource(FleetRepository repository) {
        this.repository = repository;
    }

    @GET
    @Path("/{id}")
    public Response find(@PathParam("id") String value) {
        UUID id;
        try {
            id = UUID.fromString(value);
            if (!id.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException("Noncanonical UUID");
            }
        } catch (IllegalArgumentException invalid) {
            return Response.status(400).entity(new ApiError("INVALID_ID", "Use a canonical UUID")).build();
        }
        return repository.find(id).map(vehicle -> Response.ok(vehicle).build())
                .orElseGet(() -> Response.status(404)
                        .entity(new ApiError("NOT_FOUND", "Vehicle not found")).build());
    }

    public record ApiError(String code, String message) {}
}
