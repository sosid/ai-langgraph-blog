package com.milesofsmiles.fleet;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class FleetApiIT {
    private static final Instant START = Instant.parse("2026-06-01T10:00:00Z");
    @Inject FleetRepository repository;

    @BeforeEach
    void clearIntervals() throws Exception {
        try (var connection = PostgresResource.owner(); var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM fleet.allocation_interval");
        }
    }

    @Test
    void vehicleHttpContracts() {
        given().get("/api/v1/vehicles/" + Vehicle.FIXTURE_ID).then().statusCode(200)
                .body("id", equalTo(Vehicle.FIXTURE_ID.toString()))
                .body("reference", equalTo("SYNTHETIC-COMPACT-001")).body("contractVersion", equalTo(1));
        given().get("/api/v1/vehicles/not-a-uuid").then().statusCode(400).body("code", equalTo("INVALID_ID"));
        given().get("/api/v1/vehicles/1-1-1-1-1").then().statusCode(400);
        given().get("/api/v1/vehicles/00000000-0000-4000-8000-000000000099").then().statusCode(404);
        given().contentType("application/json").body("{}").post("/api/v1/vehicles").then().statusCode(404);
        given().post("/api/v1/vehicles/" + Vehicle.FIXTURE_ID).then().statusCode(405);
        given().get("/q/health/live").then().statusCode(200);
        given().get("/q/health/ready").then().statusCode(200);
    }

    @Test
    void httpStorageFailureIsExplicitAndSafe() throws Exception {
        try (var connection = PostgresResource.owner(); var statement = connection.createStatement()) {
            statement.execute("REVOKE SELECT ON fleet.vehicle FROM fleet_app");
            try {
                given().get("/api/v1/vehicles/" + Vehicle.FIXTURE_ID).then().statusCode(503)
                        .body("code", equalTo("STORAGE_UNAVAILABLE"))
                        .body("message", equalTo("Fleet storage is unavailable"));
            } finally {
                statement.execute("GRANT SELECT ON fleet.vehicle TO fleet_app");
            }
        }
        given().get("/api/v1/vehicles/" + Vehicle.FIXTURE_ID).then().statusCode(200);
    }

    @Test
    void adjacentAllowedOverlapBlockedAndNoPartialInsert() throws Exception {
        repository.insertInterval(interval(Vehicle.FIXTURE_ID, 0, 60, AllocationInterval.Status.BLOCKING));
        repository.insertInterval(interval(Vehicle.FIXTURE_ID, 60, 120, AllocationInterval.Status.BLOCKING));
        var overlap = interval(Vehicle.FIXTURE_ID, 30, 90, AllocationInterval.Status.BLOCKING);
        assertThrows(IntervalConflictException.class, () -> repository.insertInterval(overlap));
        assertEquals(2, countIntervals());
        // The failed transaction must not poison a subsequent independent insert.
        repository.insertInterval(interval(Vehicle.FIXTURE_ID, 120, 180, AllocationInterval.Status.BLOCKING));
        assertEquals(3, countIntervals());
    }

    @Test
    void differentVehiclesDoNotConflict() throws Exception {
        UUID second = UUID.fromString("00000000-0000-4000-8000-000000000003");
        try (var connection = PostgresResource.owner(); var statement = connection.prepareStatement(
                "INSERT INTO fleet.vehicle VALUES (?, 'SYNTHETIC-COMPACT-002', 1) ON CONFLICT DO NOTHING")) {
            statement.setObject(1, second);
            statement.executeUpdate();
        }
        repository.insertInterval(interval(Vehicle.FIXTURE_ID, 0, 60, AllocationInterval.Status.BLOCKING));
        repository.insertInterval(interval(second, 0, 60, AllocationInterval.Status.BLOCKING));
        assertEquals(2, countIntervals());
    }

    @Test
    void releasedRecordsDoNotBlockButReactivationCannotOverlap() throws Exception {
        repository.insertInterval(interval(Vehicle.FIXTURE_ID, 0, 60, AllocationInterval.Status.BLOCKING));
        var released = interval(Vehicle.FIXTURE_ID, 0, 60, AllocationInterval.Status.RELEASED);
        repository.insertInterval(released);
        try (var connection = PostgresResource.runtimeSource().getConnection();
                var statement = connection.prepareStatement(
                        "UPDATE fleet.allocation_interval SET status = 'BLOCKING' WHERE id = ?")) {
            statement.setObject(1, released.id());
            assertEquals("23P01", assertThrows(SQLException.class, statement::executeUpdate).getSQLState());
        }
        try (var connection = PostgresResource.runtimeSource().getConnection();
                var statement = connection.prepareStatement(
                        "UPDATE fleet.allocation_interval SET status = 'RELEASED' WHERE status = 'BLOCKING'")) {
            assertEquals(1, statement.executeUpdate());
        }
        repository.insertInterval(interval(Vehicle.FIXTURE_ID, 0, 60, AllocationInterval.Status.BLOCKING));
        assertEquals(3, countIntervals());
    }

    @Test
    void invalidWindowsRejectedByJavaAndDatabase() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> interval(Vehicle.FIXTURE_ID, 0, 0, AllocationInterval.Status.BLOCKING));
        assertThrows(IllegalArgumentException.class,
                () -> interval(Vehicle.FIXTURE_ID, 60, 0, AllocationInterval.Status.BLOCKING));
        try (var connection = PostgresResource.runtimeSource().getConnection();
                var statement = connection.prepareStatement("""
                        INSERT INTO fleet.allocation_interval VALUES (?, ?, ?, ?, 'BLOCKING')
                        """)) {
            for (int end : new int[] {0, -1}) {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, Vehicle.FIXTURE_ID);
                statement.setObject(3, START.atOffset(java.time.ZoneOffset.UTC));
                statement.setObject(4, START.plusSeconds(end).atOffset(java.time.ZoneOffset.UTC));
                assertEquals("23514", assertThrows(SQLException.class, statement::executeUpdate).getSQLState());
            }
        }
        assertEquals(0, countIntervals());
    }

    @Test
    void concurrentContendersHaveExactlyOneWinner() throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> contender = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Contender timeout");
                }
                try {
                    new FleetRepository(PostgresResource.runtimeSource()).insertInterval(
                            interval(Vehicle.FIXTURE_ID, 0, 60, AllocationInterval.Status.BLOCKING));
                    return true;
                } catch (IntervalConflictException conflict) {
                    return false;
                }
            };
            var first = executor.submit(contender);
            var second = executor.submit(contender);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        }
        assertEquals(1, countIntervals());
    }

    @Test
    void foreignKeyErrorsAreNotIntervalConflicts() throws Exception {
        var failure = assertThrows(FleetStorageException.class, () -> repository.insertInterval(
                interval(UUID.randomUUID(), 0, 60, AllocationInterval.Status.BLOCKING)));
        assertEquals("23503", failure.sqlState());
        assertEquals(0, countIntervals());
    }

    @Test
    void runtimeCannotDdlMigrateOrAssumeOwner() throws Exception {
        try (var connection = PostgresResource.runtimeSource().getConnection();
                var statement = connection.createStatement()) {
            for (String sql : new String[] {
                    "CREATE TABLE fleet.forbidden (id INTEGER)",
                    "CREATE TABLE public.forbidden (id INTEGER)",
                    "ALTER TABLE fleet.vehicle ADD COLUMN forbidden INTEGER",
                    "DROP TABLE fleet.vehicle",
                    "SET ROLE fleet_migrator",
                    "SELECT * FROM fleet.flyway_schema_history",
                    "INSERT INTO fleet.flyway_schema_history (installed_rank) VALUES (999)",
                    "UPDATE fleet.schema_version SET version = 1",
                    "DELETE FROM fleet.vehicle",
                    "CREATE SCHEMA forbidden"}) {
                assertEquals("42501", assertThrows(SQLException.class, () -> statement.execute(sql), sql).getSQLState());
            }
            try (var result = statement.executeQuery(
                    "SELECT has_schema_privilege(current_user, 'public', 'USAGE')")) {
                assertTrue(result.next());
                assertFalse(result.getBoolean(1));
            }
            try (var result = statement.executeQuery(
                    "SELECT current_user, ssl FROM pg_stat_ssl WHERE pid = pg_backend_pid()")) {
                assertTrue(result.next());
                assertEquals("fleet_app", result.getString(1));
                assertTrue(result.getBoolean(2));
            }
        }
        try (var connection = PostgresResource.owner(); var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT tablename, tableowner FROM pg_tables WHERE schemaname = 'fleet'
                        """)) {
            int count = 0;
            while (result.next()) {
                assertEquals("fleet_migrator", result.getString("tableowner"));
                count++;
            }
            assertEquals(4, count);
        }
        try (var connection = PostgresResource.owner(); var statement = connection.createStatement();
                var result = statement.executeQuery("""
                        SELECT pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname = 'fleet'
                        """)) {
            assertTrue(result.next());
            assertEquals("fleet_migrator", result.getString(1));
        }
    }

    @Test
    void repeatedMigrationStableAndChecksumDriftRejected() throws Exception {
        int checksum;
        try (var connection = PostgresResource.owner(); var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT checksum FROM fleet.flyway_schema_history WHERE version = '1'")) {
            assertTrue(result.next());
            checksum = result.getInt(1);
        }
        FleetMigrationMain.migrate(PostgresResource.url, "fleet_migrator", PostgresResource.OWNER_PASSWORD);
        assertEquals("SYNTHETIC-COMPACT-001", repository.find(Vehicle.FIXTURE_ID).orElseThrow().reference());
        Path directory = Files.createTempDirectory(Path.of("target"), "fleet-drift-");
        try {
            try (var stream = getClass().getResourceAsStream("/db/migration/V1__fleet_records.sql")) {
                String migration = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                Files.writeString(directory.resolve("V1__fleet_records.sql"),
                        migration.replace("SYNTHETIC-COMPACT-001", "SYNTHETIC-DRIFT-001"));
            }
            var drift = Flyway.configure().dataSource(PostgresResource.url, "fleet_migrator",
                    PostgresResource.OWNER_PASSWORD).schemas("fleet").defaultSchema("fleet")
                    .locations("filesystem:" + directory.toAbsolutePath()).cleanDisabled(true).load();
            assertThrows(FlywayValidateException.class, drift::migrate);
        } finally {
            Files.deleteIfExists(directory.resolve("V1__fleet_records.sql"));
            Files.deleteIfExists(directory);
        }
        try (var connection = PostgresResource.owner(); var statement = connection.createStatement();
                var result = statement.executeQuery(
                        "SELECT checksum FROM fleet.flyway_schema_history WHERE version = '1'")) {
            assertTrue(result.next());
            assertEquals(checksum, result.getInt(1));
        }
    }

    @Test
    void incompatibleSchemaIsRejectedUsingRuntimeCredentials() throws Exception {
        try (var connection = PostgresResource.owner(); var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE fleet.schema_version DROP CONSTRAINT schema_version_version_check");
            try {
                statement.executeUpdate("UPDATE fleet.schema_version SET version = 2");
                assertThrows(IllegalStateException.class, repository::verifySchema);
            } finally {
                statement.executeUpdate("UPDATE fleet.schema_version SET version = 1");
                statement.execute("ALTER TABLE fleet.schema_version ADD CONSTRAINT schema_version_version_check CHECK (version = 1)");
            }
        }
        repository.verifySchema();
    }

    private static AllocationInterval interval(UUID vehicle, int from, int to, AllocationInterval.Status status) {
        return new AllocationInterval(UUID.randomUUID(), vehicle, START.plusSeconds(from),
                START.plusSeconds(to), status);
    }

    private static int countIntervals() throws Exception {
        try (var connection = PostgresResource.runtimeSource().getConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT count(*) FROM fleet.allocation_interval")) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }
}
