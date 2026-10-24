package com.milesofsmiles.rental;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.Clock;
import java.util.UUID;

@Path("/api/v1/rentals")
@Produces(MediaType.APPLICATION_JSON)
public class RentalResource {
    private final RentalRepository repository;
    private final Clock clock;

    @Inject
    public RentalResource(RentalRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public record ApiError(String code, String message) {
    }

    public static Response error(int status, String code, String message) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(new ApiError(code, message)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@HeaderParam("Idempotency-Key") String key, RentalRequest request) {
        RentalRequest.Validated validated;
        try {
            if (request == null) {
                throw new IllegalArgumentException();
            }
            validated = request.validate(key);
        } catch (IllegalArgumentException invalid) {
            return error(400, "INVALID_REQUEST", "A valid version 1 request and Idempotency-Key are required");
        }
        try {
            var result = repository.create(key, validated, clock.instant());
            return Response.status(result.replay() ? 200 : 201)
                    .location(URI.create("/api/v1/rentals/" + result.rental().id()))
                    .header("Idempotency-Replayed", Boolean.toString(result.replay()))
                    .entity(result.rental()).build();
        } catch (RentalConflictException conflict) {
            return error(409, "REQUEST_KEY_CONFLICT", "Request key already belongs to a different payload");
        }
    }

    @GET
    @Path("/{id}")
    public Response find(@PathParam("id") String value) {
        UUID id;
        try {
            id = RentalRequest.identifier(value);
        } catch (IllegalArgumentException invalid) {
            return error(400, "INVALID_ID", "A canonical UUID is required");
        }
        return repository.find(id).map(rental -> Response.ok(rental).build())
                .orElseGet(() -> error(404, "NOT_FOUND", "Rental was not found"));
    }
}
