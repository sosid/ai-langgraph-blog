package com.milesofsmiles.booking;

import jakarta.enterprise.context.ApplicationScoped;
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
import java.util.UUID;

@ApplicationScoped
@Path("/api/v1/booking-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookingRecordResource {
    private final BookingRecordRepository repository;

    @Inject
    public BookingRecordResource(BookingRecordRepository repository) {
        this.repository = repository;
    }

    @POST
    public Response create(@HeaderParam("Idempotency-Key") String key, BookingSubmission input) {
        BookingSubmission.Validated validated;
        try {
            if (input == null) {
                throw new IllegalArgumentException("Request body required");
            }
            validated = input.validate(key);
        } catch (IllegalArgumentException invalid) {
            return error(400, "INVALID_REQUEST", invalid.getMessage());
        }
        var creation = repository.create(key, validated);
        if (creation.conflict()) {
            return error(409, "REQUEST_KEY_CONFLICT", "Idempotency-Key already used for a different payload");
        }
        return Response.status(creation.inserted() ? 201 : 200)
                .location(URI.create("/api/v1/booking-requests/" + creation.record().id()))
                .header("Idempotency-Replayed", !creation.inserted()).entity(creation.record()).build();
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
            return error(400, "INVALID_ID", "Use a canonical UUID");
        }
        return repository.find(id).map(row -> Response.ok(row).build())
                .orElseGet(() -> error(404, "NOT_FOUND", "Booking request not found"));
    }

    private static Response error(int status, String code, String message) {
        return Response.status(status).entity(new BookingResource.ApiError(code, message)).build();
    }
}
