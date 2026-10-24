package com.milesofsmiles.booking;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

/** Read-only HTTP boundary for the synthetic request fixture. */
@Path("/api/bookings")
@Produces(MediaType.APPLICATION_JSON)
public class BookingResource {
    private final BookingRepository repository;

    @Inject
    public BookingResource(BookingRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieves a request without making a reservation.
     *
     * @param value canonical UUID text, case-insensitive
     * @return 200 with the request, 400 for invalid text, or 404 for a missing row
     * @throws BookingStorageException mapped to HTTP 503 by {@link BookingStorageExceptionMapper}
     */
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

    /**
     * A stable client-facing error without database provider details.
     *
     * @param code machine-readable outcome
     * @param message safe client-facing explanation
     */
    public record ApiError(String code, String message) {
    }
}
