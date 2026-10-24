package com.milesofsmiles.rental;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class RentalRepository {
    private static final String COLUMNS = "id, contract_version, booking_id, vehicle_id, state, created_at";
    private final DataSource dataSource;

    @Inject
    public RentalRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record WriteResult(DraftRental rental, boolean replay) {
    }

    public WriteResult create(String key, RentalRequest.Validated request, Instant createdAt) {
        try (var connection = dataSource.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                var outcome = insertOrReplay(connection, key, request, createdAt);
                connection.commit();
                return outcome;
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        } catch (SQLException failure) {
            throw new RentalStorageException(failure);
        }
    }

    private WriteResult insertOrReplay(Connection connection, String key, RentalRequest.Validated request,
            Instant createdAt) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO rental.draft_rental
                    (id, contract_version, request_key, request_hash, booking_id, vehicle_id, state, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?)
                ON CONFLICT (request_key) DO NOTHING RETURNING
                """ + COLUMNS)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setInt(2, request.contractVersion());
            statement.setString(3, key);
            statement.setString(4, request.hash());
            statement.setObject(5, request.bookingId());
            statement.setObject(6, request.vehicleId());
            statement.setObject(7, createdAt.atOffset(ZoneOffset.UTC));
            try (var result = statement.executeQuery()) {
                if (result.next()) {
                    return new WriteResult(read(result), false);
                }
            }
        }
        // The insert waits for a competing unique-key writer; READ COMMITTED gives this
        // subsequent statement a fresh snapshot containing that writer's committed row.
        try (var statement = connection.prepareStatement(
                "SELECT " + COLUMNS + ", request_hash FROM rental.draft_rental WHERE request_key = ?")) {
            statement.setString(1, key);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("Conflicting row not visible", "40001");
                }
                if (!request.hash().equals(result.getString("request_hash"))) {
                    throw new RentalConflictException();
                }
                return new WriteResult(read(result), true);
            }
        }
    }

    public Optional<DraftRental> find(UUID id) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "SELECT " + COLUMNS + " FROM rental.draft_rental WHERE id = ?")) {
            statement.setObject(1, id);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw new RentalStorageException(failure);
        }
    }

    private static DraftRental read(ResultSet result) throws SQLException {
        return new DraftRental(result.getObject("id", UUID.class), result.getInt("contract_version"),
                result.getObject("booking_id", UUID.class), result.getObject("vehicle_id", UUID.class),
                result.getString("state"), result.getTimestamp("created_at").toInstant());
    }
}
