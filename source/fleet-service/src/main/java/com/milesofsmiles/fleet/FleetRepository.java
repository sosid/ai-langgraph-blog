package com.milesofsmiles.fleet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

@ApplicationScoped
public class FleetRepository {
    private final DataSource dataSource;

    @Inject
    public FleetRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Vehicle> find(UUID id) {
        Objects.requireNonNull(id, "id");
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(
                        "SELECT id, reference, contract_version FROM fleet.vehicle WHERE id = ?")) {
            statement.setObject(1, id);
            try (var result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(new Vehicle(result.getObject("id", UUID.class),
                                result.getString("reference"), result.getInt("contract_version")))
                        : Optional.empty();
            }
        } catch (SQLException failure) {
            throw new FleetStorageException(failure);
        }
    }

    public void insertInterval(AllocationInterval interval) {
        Objects.requireNonNull(interval, "interval");
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.prepareStatement("""
                        INSERT INTO fleet.allocation_interval (id, vehicle_id, starts_at, ends_at, status)
                        VALUES (?, ?, ?, ?, ?)
                        """)) {
                    statement.setObject(1, interval.id());
                    statement.setObject(2, interval.vehicleId());
                    statement.setObject(3, interval.startsAt().atOffset(ZoneOffset.UTC));
                    statement.setObject(4, interval.endsAt().atOffset(ZoneOffset.UTC));
                    statement.setString(5, interval.status().name());
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
            if ("23P01".equals(failure.getSQLState())) {
                throw new IntervalConflictException(failure);
            }
            throw new FleetStorageException(failure);
        }
    }

    public void verifySchema() {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("SELECT version FROM fleet.schema_version");
                var result = statement.executeQuery()) {
            if (!result.next() || result.getInt(1) != 1 || result.next()) {
                throw new IllegalStateException("Incompatible fleet schema version; expected 1");
            }
        } catch (SQLException failure) {
            throw new FleetStorageException(failure);
        }
    }
}
