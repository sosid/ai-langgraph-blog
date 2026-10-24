package com.milesofsmiles.booking;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class BookingRepository {
    private final DataSource dataSource;

    @Inject
    public BookingRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize(Instant createdAt) {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.createStatement()) {
                    statement.execute("""
                            CREATE TABLE IF NOT EXISTS booking_request_fixture (
                                id UUID PRIMARY KEY,
                                reference TEXT NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL
                            )
                            """);
                }
                try (var statement = connection.prepareStatement("""
                        INSERT INTO booking_request_fixture (id, reference, created_at)
                        VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING
                        """)) {
                    statement.setObject(1, BookingRequest.FIXTURE_ID);
                    statement.setString(2, BookingRequest.FIXTURE_REFERENCE);
                    statement.setObject(3, createdAt.atOffset(java.time.ZoneOffset.UTC));
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        } catch (SQLException failure) {
            throw new BookingStorageException(failure);
        }
    }

    public Optional<BookingRequest> find(UUID id) {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("""
                        SELECT id, reference, created_at FROM booking_request_fixture WHERE id = ?
                        """)) {
            statement.setObject(1, id);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new BookingRequest(result.getObject("id", UUID.class),
                        result.getString("reference"), result.getTimestamp("created_at").toInstant()));
            }
        } catch (SQLException failure) {
            throw new BookingStorageException(failure);
        }
    }
}
