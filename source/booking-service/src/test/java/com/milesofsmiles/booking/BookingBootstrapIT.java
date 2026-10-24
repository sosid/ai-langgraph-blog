package com.milesofsmiles.booking;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

class BookingBootstrapIT {
    @Test
    void productionBootstrapCreatesIsolatedRolesAndMigratesLegacyDatabase() throws Exception {
        Path root = Path.of(System.getProperty("baseline.repository.root")).toAbsolutePath().normalize();
        Path passwordFile = Files.createTempFile(Path.of("target"), "bootstrap-password-", ".txt");
        Path legacyFile = Files.createTempFile(Path.of("target"), "legacy-fixture-", ".sql");
        String password = UUID.randomUUID().toString();
        Files.writeString(passwordFile, password);
        Files.writeString(legacyFile, """
                CREATE TABLE public.booking_request_fixture (
                    id UUID PRIMARY KEY, reference TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL);
                INSERT INTO public.booking_request_fixture VALUES
                    ('00000000-0000-4000-8000-000000000001',
                     'SYNTHETIC-BOOTSTRAP-UPGRADE', '2025-03-04T05:06:07Z');
                """);
        try (var postgres = new PostgreSQLContainer<>(DockerImageName.parse(PostgresResource.IMAGE)
                .asCompatibleSubstituteFor("postgres")).withDatabaseName("records")
                .withUsername("bootstrap").withPassword(password)
                .withCopyFileToContainer(MountableFile.forHostPath(legacyFile, 0444),
                        "/docker-entrypoint-initdb.d/00-legacy.sql")
                .withCopyFileToContainer(MountableFile.forHostPath(root.resolve("deploy/step-2/bootstrap.sh"), 0555),
                        "/docker-entrypoint-initdb.d/01-bootstrap.sh")
                .withCopyFileToContainer(MountableFile.forHostPath(root.resolve("deploy/step-2/bootstrap.sql"), 0444),
                        "/bootstrap/bootstrap.sql")) {
            for (String owner : new String[]{"booking", "rental", "fleet"}) {
                for (String identity : new String[]{"app", "migrator"}) {
                    postgres.withCopyFileToContainer(MountableFile.forHostPath(passwordFile, 0444),
                            "/run/secrets/" + owner + "_" + identity + "-password");
                }
            }
            postgres.start();
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), "bootstrap", password);
                    var sql = connection.createStatement();
                    var row = sql.executeQuery(
                            "SELECT extversion FROM pg_extension WHERE extname = 'btree_gist'")) {
                assertTrue(row.next());
                assertEquals("1.7", row.getString(1));
            }
            Flyway.configure().dataSource(postgres.getJdbcUrl(), "booking_migrator", password)
                    .schemas("booking").defaultSchema("booking").createSchemas(false)
                    .locations("classpath:db/migration").cleanDisabled(true).load().migrate();
            for (String owner : new String[]{"booking", "rental", "fleet"}) {
                try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), owner + "_app", password);
                        var sql = connection.createStatement()) {
                    assertEquals("42501", assertThrows(SQLException.class,
                            () -> sql.execute("CREATE TEMP TABLE forbidden (id int)")).getSQLState());
                    assertEquals("42501", assertThrows(SQLException.class,
                            () -> sql.execute("SET ROLE " + owner + "_migrator")).getSQLState());
                }
            }
            try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), "booking_app", password);
                    var sql = connection.createStatement();
                    var row = sql.executeQuery("SELECT reference FROM booking.booking_request_fixture")) {
                assertTrue(row.next());
                assertEquals("SYNTHETIC-BOOTSTRAP-UPGRADE", row.getString(1));
            }
        } finally {
            Files.deleteIfExists(passwordFile);
            Files.deleteIfExists(legacyFile);
        }
    }
}
