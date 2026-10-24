package com.milesofsmiles.booking;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.annotation.Priority;
import java.sql.SQLException;
import javax.sql.DataSource;

/** Runtime startup rejects an absent or incompatible schema without DDL privileges. */
@ApplicationScoped
public class SchemaGuard {
    private final DataSource source;

    @Inject
    public SchemaGuard(DataSource source) {
        this.source = source;
    }

    void check(@Observes @Priority(1) StartupEvent event) {
        try (var connection = source.getConnection();
                var statement = connection.prepareStatement("SELECT version FROM booking.schema_version");
                var rows = statement.executeQuery()) {
            if (!rows.next() || rows.getInt(1) != 1 || rows.next()) {
                throw new IllegalStateException("Incompatible booking schema; expected version 1");
            }
        } catch (SQLException failure) {
            throw new BookingStorageException(failure);
        }
    }
}
