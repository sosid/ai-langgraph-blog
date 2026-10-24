package com.milesofsmiles.booking;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class BookingRecordRepository {
    private final DataSource source;
    private final Clock clock;

    @Inject
    public BookingRecordRepository(DataSource source, Clock clock) {
        this.source = source;
        this.clock = clock;
    }

    public Creation create(String key, BookingSubmission.Validated input) {
        try (var connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int inserted;
                try (var statement = connection.prepareStatement("""
                        INSERT INTO booking.booking_request
                            (id, contract_version, request_key, request_hash, vehicle_class,
                             starts_at, ends_at, created_at)
                        VALUES (?, 1, ?, ?, 'COMPACT', ?, ?, ?)
                        ON CONFLICT (request_key) DO NOTHING
                        """)) {
                    statement.setObject(1, UUID.randomUUID());
                    statement.setString(2, key);
                    statement.setString(3, input.hash());
                    statement.setObject(4, input.start().atOffset(ZoneOffset.UTC));
                    statement.setObject(5, input.end().atOffset(ZoneOffset.UTC));
                    statement.setObject(6, clock.instant().atOffset(ZoneOffset.UTC));
                    inserted = statement.executeUpdate();
                }
                Creation creation;
                try (var statement = connection.prepareStatement(
                        "SELECT * FROM booking.booking_request WHERE request_key = ?")) {
                    statement.setString(1, key);
                    try (var row = statement.executeQuery()) {
                        if (!row.next()) {
                            throw new SQLException("Request vanished during transaction", "40001");
                        }
                        creation = new Creation(map(row), inserted == 1,
                                !input.hash().equals(row.getString("request_hash")));
                    }
                }
                connection.commit();
                return creation;
            } catch (SQLException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    failure.addSuppressed(rollback);
                }
                throw failure;
            }
        } catch (SQLException failure) {
            throw new BookingStorageException(failure);
        }
    }

    public Optional<BookingRecord> find(UUID id) {
        try (var connection = source.getConnection();
                var statement = connection.prepareStatement(
                        "SELECT * FROM booking.booking_request WHERE id = ?")) {
            statement.setObject(1, id);
            try (var row = statement.executeQuery()) {
                return row.next() ? Optional.of(map(row)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw new BookingStorageException(failure);
        }
    }

    private static BookingRecord map(ResultSet row) throws SQLException {
        return new BookingRecord(row.getObject("id", UUID.class), row.getInt("contract_version"),
                row.getString("vehicle_class"), row.getTimestamp("starts_at").toInstant(),
                row.getTimestamp("ends_at").toInstant(), row.getTimestamp("created_at").toInstant());
    }

    public record Creation(BookingRecord record, boolean inserted, boolean conflict) {
    }
}
