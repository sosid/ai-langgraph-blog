package com.milesofsmiles.booking;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class BookingUpgradeIT {
    @Test
    void exactStepOneTableSurvivesOwnerTransferMigrationAndChecksumChecks() throws Exception {
        String password = UUID.randomUUID().toString();
        try (var postgres = new PostgreSQLContainer<>(DockerImageName.parse(PostgresResource.IMAGE)
                .asCompatibleSubstituteFor("postgres")).withUsername("bootstrap").withPassword(password)) {
            postgres.start();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), "bootstrap", password);
                    var sql = connection.createStatement()) {
                sql.execute("CREATE ROLE booking_migrator LOGIN PASSWORD '" + password + "'");
                sql.execute("CREATE ROLE booking_app LOGIN PASSWORD '" + password + "'");
                sql.execute("REVOKE ALL ON SCHEMA public FROM PUBLIC");
                sql.execute("GRANT USAGE ON SCHEMA public TO booking_migrator");
                sql.execute("CREATE SCHEMA booking AUTHORIZATION booking_migrator");
                sql.execute("""
                        CREATE TABLE public.booking_request_fixture (
                            id UUID PRIMARY KEY, reference TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL)
                        """);
                sql.execute("""
                        INSERT INTO public.booking_request_fixture VALUES
                        ('00000000-0000-4000-8000-000000000001',
                         'SYNTHETIC-UPGRADE-MARKER', '2025-03-04T05:06:07Z')
                        """);
                sql.execute("ALTER TABLE public.booking_request_fixture OWNER TO booking_migrator");
            }
            var flyway = Flyway.configure().dataSource(postgres.getJdbcUrl(), "booking_migrator", password)
                    .schemas("booking").defaultSchema("booking").createSchemas(false)
                    .locations("classpath:db/migration").cleanDisabled(true).load();
            assertEquals(1, flyway.migrate().migrationsExecuted);
            assertEquals(0, flyway.migrate().migrationsExecuted);
            var source = new org.postgresql.ds.PGSimpleDataSource();
            source.setURL(postgres.getJdbcUrl());
            source.setUser("booking_app");
            source.setPassword(password);
            var repository = new BookingRepository(source);
            repository.initialize(java.time.Instant.parse("2030-01-01T00:00:00Z"));
            var fixture = repository.find(BookingRequest.FIXTURE_ID).orElseThrow();
            assertEquals("SYNTHETIC-UPGRADE-MARKER", fixture.reference());
            assertEquals(java.time.Instant.parse("2025-03-04T05:06:07Z"), fixture.createdAt());
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), "booking_migrator", password);
                    var sql = connection.createStatement()) {
                sql.execute("UPDATE booking.flyway_schema_history SET checksum = 0 WHERE version = '1'");
            }
            assertThrows(FlywayValidateException.class, flyway::migrate);
        }
    }
}
