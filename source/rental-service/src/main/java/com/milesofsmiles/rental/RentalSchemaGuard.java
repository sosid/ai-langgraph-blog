package com.milesofsmiles.rental;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.sql.SQLException;
import javax.sql.DataSource;

@ApplicationScoped
public class RentalSchemaGuard {
    private final DataSource dataSource;

    @Inject
    public RentalSchemaGuard(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    void start(@Observes StartupEvent event) {
        check();
    }

    public void check() {
        try (var connection = dataSource.getConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT contract_version FROM rental.schema_version")) {
            if (!result.next() || result.getInt(1) != 1 || result.next()) {
                throw new IllegalStateException("Rental schema version must be exactly 1");
            }
        } catch (SQLException failure) {
            throw new RentalStorageException(failure);
        }
    }
}
