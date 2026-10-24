package com.milesofsmiles.rental;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class RentalApiIT {
    private static final String BOOKING = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    private static final String VEHICLE = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";
    private static final String PAYLOAD = """
            {"contractVersion":1,"bookingId":"%s","vehicleId":"%s"}
            """.formatted(BOOKING, VEHICLE);
    private static final Instant NOW = Instant.parse("2026-01-02T10:00:00Z");
    @Inject
    RentalRepository repository;

    @Test
    void versionedApiCreatesReadsReplaysCanonicalPayloadAndRejectsConflict() {
        String key = UUID.randomUUID().toString();
        String id = given().contentType("application/json").header("Idempotency-Key", key).body(PAYLOAD)
                .post("/api/v1/rentals").then().statusCode(201)
                .header("Idempotency-Replayed", "false")
                .body("contractVersion", equalTo(1)).body("bookingId", equalTo(BOOKING))
                .body("vehicleId", equalTo(VEHICLE)).body("state", equalTo("DRAFT"))
                .body("createdAt", equalTo(NOW.toString())).extract().path("id");
        given().get("/api/v1/rentals/" + id).then().statusCode(200).body("id", equalTo(id));
        String canonicalEquivalent = """
                {"vehicleId":"%s","bookingId":"%s","contractVersion":1}
                """.formatted(VEHICLE.toUpperCase(), BOOKING.toUpperCase());
        given().contentType("application/json").header("Idempotency-Key", key).body(canonicalEquivalent)
                .post("/api/v1/rentals").then().statusCode(200).header("Idempotency-Replayed", "true")
                .body("id", equalTo(id)).body("createdAt", equalTo(NOW.toString()));
        given().contentType("application/json").header("Idempotency-Key", key)
                .body(PAYLOAD.replace(VEHICLE, BOOKING)).post("/api/v1/rentals")
                .then().statusCode(409).body("code", equalTo("REQUEST_KEY_CONFLICT"));
        given().get("/api/v1/rentals/" + id).then().statusCode(200).body("vehicleId", equalTo(VEHICLE));
    }

    @Test
    void rejectsMissingUnknownMalformedOversizeAndUnsupportedRequests() {
        for (String invalid : new String[]{
                "null", "{}", "{", "{\"bookingId\":}",
                PAYLOAD.replace("\"contractVersion\":1", "\"contractVersion\":1,\"contractVersion\":1"),
                PAYLOAD.replace("\"contractVersion\":1", "\"contractVersion\":2"),
                PAYLOAD.replace("\"contractVersion\":1", "\"contractVersion\":null"),
                PAYLOAD.replace("\"contractVersion\":1", "\"contractVersion\":\"1\""),
                PAYLOAD.replace("\"contractVersion\":1", "\"contractVersion\":1.5"),
                PAYLOAD.replace("\"contractVersion\":1", "\"contractVersion\":1,\"unknown\":true"),
                PAYLOAD.replace(BOOKING, "1-1-1-1-1"), PAYLOAD.replace(VEHICLE, "not-a-uuid"),
                PAYLOAD.replace("\"" + BOOKING + "\"", "null"),
                PAYLOAD.replace("\"" + VEHICLE + "\"", "123"), PAYLOAD + "{}"}) {
            given().contentType("application/json").header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(invalid).post("/api/v1/rentals").then().statusCode(400)
                    .body("code", equalTo("INVALID_REQUEST"));
        }
        given().contentType("application/json").body(PAYLOAD).post("/api/v1/rentals")
                .then().statusCode(400).body("code", equalTo("INVALID_REQUEST"));
        given().contentType("application/json").header("Idempotency-Key", UUID.randomUUID().toString())
                .post("/api/v1/rentals").then().statusCode(400).body("code", equalTo("INVALID_REQUEST"));
        for (String key : new String[]{"wrong/key", "x".repeat(129)}) {
            given().contentType("application/json").header("Idempotency-Key", key).body(PAYLOAD)
                    .post("/api/v1/rentals").then().statusCode(400);
        }
        given().contentType("application/json").header("Idempotency-Key", "oversize")
                .body(" ".repeat(16_385) + PAYLOAD).post("/api/v1/rentals").then().statusCode(413);
    }

    @Test
    void getValidatesStrictUuidAndMissingRecord() {
        for (String invalid : new String[]{"not-a-uuid", "1-1-1-1-1"}) {
            given().get("/api/v1/rentals/" + invalid).then().statusCode(400).body("code", equalTo("INVALID_ID"));
        }
        given().get("/api/v1/rentals/" + UUID.randomUUID()).then().statusCode(404).body("code", equalTo("NOT_FOUND"));
    }

    @Test
    void concurrentSameKeyPersistsExactlyOneRow() throws Exception {
        String key = UUID.randomUUID().toString();
        var ready = new CountDownLatch(8);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
            Callable<Integer> task = () -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return given().contentType("application/json").header("Idempotency-Key", key).body(PAYLOAD)
                        .post("/api/v1/rentals").statusCode();
            };
            for (int i = 0; i < 8; i++) {
                futures.add(executor.submit(task));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var statuses = new ArrayList<Integer>();
            for (var future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
            assertEquals(1, statuses.stream().filter(status -> status == 201).count());
            assertEquals(7, statuses.stream().filter(status -> status == 200).count());
        }
        try (var connection = PostgresResource.runtimeSource().getConnection();
                var statement = connection.prepareStatement("SELECT count(*) FROM rental.draft_rental WHERE request_key = ?")) {
            statement.setString(1, key);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }

    @Test
    void identitySurvivesNewApplicationRepositoryAndConnections() {
        String key = UUID.randomUUID().toString();
        var request = new RentalRequest(1, BOOKING, VEHICLE).validate(key);
        var original = repository.create(key, request, NOW);
        var reconnected = new RentalRepository(PostgresResource.runtimeSource());
        var replay = reconnected.create(key, request, NOW.plusSeconds(500));
        assertTrue(replay.replay());
        assertEquals(original.rental(), replay.rental());
        assertEquals(original.rental(), reconnected.find(original.rental().id()).orElseThrow());
    }

    @Test
    void responseUsesPersistedPostgresTimestampPrecision() {
        String key = UUID.randomUUID().toString();
        var request = new RentalRequest(1, BOOKING, VEHICLE).validate(key);
        Instant precise = Instant.parse("2026-01-02T10:00:00.123456Z");
        var first = repository.create(key, request, precise);
        assertEquals(precise, first.rental().createdAt());
        assertEquals(first.rental(), repository.find(first.rental().id()).orElseThrow());
        assertEquals(precise, repository.create(key, request, NOW).rental().createdAt());
    }

    @Test
    void runtimeHasOnlyRequiredPrivilegesAndCannotReadHistoryOrMigrate() throws Exception {
        try (var connection = PostgresResource.runtimeSource().getConnection();
                var statement = connection.createStatement()) {
            for (String denied : new String[]{
                    "CREATE TABLE rental.forbidden(id int)", "CREATE TABLE public.forbidden(id int)",
                    "SELECT * FROM rental.flyway_schema_history",
                    "UPDATE rental.schema_version SET contract_version = 2",
                    "DELETE FROM rental.draft_rental", "SET ROLE rental_migrator",
                    "CREATE ROLE forbidden", "DROP TABLE rental.draft_rental"}) {
                assertEquals("42501", assertThrows(SQLException.class, () -> statement.execute(denied)).getSQLState());
            }
            assertEquals(0, statement.executeUpdate("UPDATE rental.draft_rental SET state = 'DRAFT' WHERE false"));
            try (var result = statement.executeQuery("""
                    SELECT tableowner FROM pg_tables
                    WHERE schemaname = 'rental' AND tablename = 'draft_rental'
                    """)) {
                assertTrue(result.next());
                assertEquals("rental_migrator", result.getString(1));
            }
        }
        assertThrows(org.flywaydb.core.api.FlywayException.class, () ->
                org.flywaydb.core.Flyway.configure()
                        .dataSource(PostgresResource.url, "rental_app", PostgresResource.appPassword)
                        .schemas("rental").defaultSchema("rental").locations("classpath:db/migration")
                        .createSchemas(false).cleanDisabled(true).load().migrate());
    }

    @Test
    void migrationsAreIdempotentValidateChecksumAndGuardRejectsFutureVersion() throws Exception {
        String key = UUID.randomUUID().toString();
        var marker = repository.create(key, new RentalRequest(1, BOOKING, VEHICLE).validate(key), NOW);
        assertEquals(0, PostgresResource.flyway().migrate().migrationsExecuted);
        assertEquals(marker.rental(), repository.find(marker.rental().id()).orElseThrow());
        PostgresResource.flyway().validate();
        var source = PostgresResource.runtimeSource();
        new RentalSchemaGuard(source).check();
        try (var owner = DriverManager.getConnection(PostgresResource.url, "rental_migrator",
                PostgresResource.migrationPassword); var statement = owner.createStatement()) {
            int checksum;
            try (var result = statement.executeQuery(
                    "SELECT checksum FROM rental.flyway_schema_history WHERE version = '1'")) {
                assertTrue(result.next());
                checksum = result.getInt(1);
            }
            try {
                statement.executeUpdate("UPDATE rental.flyway_schema_history SET checksum = 0 WHERE version = '1'");
                assertThrows(org.flywaydb.core.api.exception.FlywayValidateException.class,
                        () -> PostgresResource.flyway().migrate());
            } finally {
                statement.executeUpdate("UPDATE rental.flyway_schema_history SET checksum = " + checksum
                        + " WHERE version = '1'");
            }
            try {
                statement.executeUpdate("UPDATE rental.schema_version SET contract_version = 2");
                assertThrows(IllegalStateException.class, () -> new RentalSchemaGuard(source).check());
            } finally {
                statement.executeUpdate("UPDATE rental.schema_version SET contract_version = 1");
            }
        }
        PostgresResource.flyway().validate();
        new RentalSchemaGuard(source).check();
    }

    @Test
    void databaseEnforcesDraftOnlyVersionOne() throws Exception {
        try (var connection = PostgresResource.runtimeSource().getConnection();
                var statement = connection.createStatement()) {
            for (String values : new String[]{"2, 'DRAFT'", "1, 'OTHER'"}) {
                var split = values.split(", ");
                assertEquals("23514", assertThrows(SQLException.class, () -> statement.execute("""
                        INSERT INTO rental.draft_rental
                        (id, contract_version, request_key, request_hash, booking_id, vehicle_id, state, created_at)
                        VALUES (gen_random_uuid(), %s, '%s', '%s', '%s', '%s', %s, now())
                        """.formatted(split[0], UUID.randomUUID(), "a".repeat(64), BOOKING, VEHICLE, split[1])))
                        .getSQLState());
            }
        }
    }

    @Test
    void jdbcVerifiesServerTlsAndRejectsWrongTrustRoot() throws Exception {
        try (var connection = PostgresResource.runtimeSource().getConnection();
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT ssl FROM pg_stat_ssl WHERE pid = pg_backend_pid()")) {
            assertTrue(result.next());
            assertTrue(result.getBoolean(1));
        }
        var wrong = PostgresResource.runtimeSource();
        wrong.setSslRootCert(PostgresResource.pki.resolve("app/server.crt").toString());
        assertThrows(SQLException.class, wrong::getConnection);
    }

    @Test
    void storageOutageKeepsLivenessButFailsReadinessAndApiSafely() {
        var docker = DockerClientFactory.instance().client();
        String container = PostgresResource.postgres.getContainerId();
        docker.pauseContainerCmd(container).exec();
        try {
            given().get("/q/health/live").then().statusCode(200);
            given().get("/q/health/ready").then().statusCode(503);
            given().get("/api/v1/rentals/" + UUID.randomUUID()).then().statusCode(503)
                    .body("code", equalTo("STORAGE_UNAVAILABLE")).body("message", equalTo("Rental storage is unavailable"));
            given().contentType("application/json").header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(PAYLOAD).post("/api/v1/rentals").then().statusCode(503)
                    .body("code", equalTo("STORAGE_UNAVAILABLE"));
        } finally {
            docker.unpauseContainerCmd(container).exec();
        }
        given().get("/q/health/ready").then().statusCode(200);
    }
}
