package com.milesofsmiles.booking;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class BookingRecordsIT {
    static final String JSON = """
            {"contractVersion":1,"vehicleClass":"COMPACT",
             "startsAt":"2026-11-01T10:00:00Z","endsAt":"2026-11-03T10:00:00Z"}
            """;
    @Inject
    BookingRecordRepository repository;
    @Inject
    SchemaGuard guard;

    @Test
    void versionedWriteReplaysSemanticPayloadAndRejectsChangedPayload() {
        String key = UUID.randomUUID().toString();
        String id = given().contentType("application/json").header("Idempotency-Key", key).body(JSON)
                .post("/api/v1/booking-requests").then().statusCode(201)
                .body("contractVersion", equalTo(1)).extract().path("id");
        given().contentType("application/json").header("Idempotency-Key", key)
                .body(JSON.replace("10:00:00Z", "10:00:00.000Z"))
                .post("/api/v1/booking-requests").then().statusCode(200).body("id", equalTo(id));
        given().contentType("application/json").header("Idempotency-Key", key)
                .body(JSON.replace("2026-11-03", "2026-11-04"))
                .post("/api/v1/booking-requests").then().statusCode(409)
                .body("code", equalTo("REQUEST_KEY_CONFLICT"));
        given().get("/api/v1/booking-requests/" + id).then().statusCode(200).body("id", equalTo(id));
        given().get("/api/v1/booking-requests/" + UUID.randomUUID()).then().statusCode(404);
        given().get("/api/v1/booking-requests/1-1-1-1-1").then().statusCode(400);
    }

    @Test
    void invalidOrUnversionedAndOversizedPayloadsDoNotCreateRecords() {
        for (String body : new String[]{JSON.replace(":1", ":2"), JSON.replace("COMPACT", "UNKNOWN"),
                JSON.replace("\"vehicleClass\"", "\"unrecognized\""), "null", "{}",
                JSON.replace(":1", ":1.5"), JSON.replace(":1", ":\"1\""),
                JSON.replace("\"COMPACT\"", "123"), JSON + "{}",
                JSON.replace("\"contractVersion\":1", "\"contractVersion\":1,\"contractVersion\":1")}) {
            given().contentType("application/json").header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(body).post("/api/v1/booking-requests").then().statusCode(400);
        }
        given().contentType("application/json").body(JSON)
                .post("/api/v1/booking-requests").then().statusCode(400);
        given().contentType("application/json").header("Idempotency-Key", "oversize")
                .body("{\"large\":\"" + "x".repeat(17000) + "\"}")
                .post("/api/v1/booking-requests").then().statusCode(413);
    }

    @Test
    void simultaneousSameKeyCreatesOnePersistentRecord() throws Exception {
        String key = UUID.randomUUID().toString();
        var input = BookingRecordsTest.INPUT.validate(key);
        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = java.util.stream.IntStream.range(0, 8)
                    .<Callable<BookingRecordRepository.Creation>>mapToObj(i -> () -> repository.create(key, input))
                    .toList();
            var results = pool.invokeAll(tasks).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            }).toList();
            assertEquals(1, results.stream().filter(BookingRecordRepository.Creation::inserted).count());
            assertEquals(1, results.stream().map(result -> result.record().id()).distinct().count());
            assertTrue(results.stream().noneMatch(BookingRecordRepository.Creation::conflict));
        }
    }

    @Test
    void allOrderedOwnerPairsDenyAccessAndRuntimeCannotMigrate() throws Exception {
        try (var admin = DriverManager.getConnection(PostgresResource.url(),
                PostgresResource.postgres.getUsername(), PostgresResource.postgres.getPassword());
                var statement = admin.createStatement()) {
            statement.execute("SET ROLE rental_migrator");
            statement.execute("CREATE TABLE rental.isolation_probe (id integer)");
            statement.execute("RESET ROLE");
            statement.execute("SET ROLE fleet_migrator");
            statement.execute("CREATE TABLE fleet.isolation_probe (id integer)");
            statement.execute("RESET ROLE");
        }
        for (String owner : new String[]{"booking", "rental", "fleet"}) {
            try (var connection = DriverManager.getConnection(PostgresResource.url(), owner + "_app",
                    PostgresResource.ROLE_PASSWORD); var statement = connection.createStatement()) {
                for (String other : new String[]{"booking", "rental", "fleet"}) {
                    if (!owner.equals(other)) {
                        String table = other + (other.equals("booking")
                                ? ".booking_request" : ".isolation_probe");
                        for (String sql : new String[]{"SELECT * FROM " + table,
                                "DELETE FROM " + table, "INSERT INTO " + table + " DEFAULT VALUES"}) {
                            SQLException denied = assertThrows(SQLException.class, () -> statement.execute(sql));
                            assertEquals("42501", denied.getSQLState());
                        }
                    }
                }
                assertEquals("42501", assertThrows(SQLException.class,
                        () -> statement.execute("CREATE TABLE " + owner + ".forbidden (id int)")).getSQLState());
                assertEquals("42501", assertThrows(SQLException.class,
                        () -> statement.execute("SET ROLE " + owner + "_migrator")).getSQLState());
                assertEquals("42501", assertThrows(SQLException.class,
                        () -> statement.execute("CREATE TEMP TABLE forbidden (id int)")).getSQLState());
            }
        }
    }

    @Test
    void repeatMigrationAndSchemaMismatchAreExplicit() throws Exception {
        assertEquals(0, PostgresResource.flyway().migrate().migrationsExecuted);
        try (var owner = DriverManager.getConnection(PostgresResource.url(), "booking_migrator",
                PostgresResource.ROLE_PASSWORD); var statement = owner.createStatement()) {
            try {
                statement.execute("UPDATE booking.schema_version SET version = 2");
                assertThrows(IllegalStateException.class, () -> guard.check(null));
            } finally {
                statement.execute("UPDATE booking.schema_version SET version = 1");
            }
        }
        guard.check(null);
    }
}
