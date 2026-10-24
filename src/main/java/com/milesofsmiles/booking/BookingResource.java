package com.milesofsmiles.booking;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Path("/api/bookings")
@Produces(MediaType.APPLICATION_JSON)
public class BookingResource {
    private final BookingRepository repository;

    @Inject
    public BookingResource(BookingRepository repository) {
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
        return repository.find(id)
                .map(request -> Response.ok(request).build())
                .orElseGet(() -> Response.status(404)
                        .entity(new ApiError("NOT_FOUND", "Booking request not found")).build());
    }

    public record ApiError(String code, String message) {
    }
}
