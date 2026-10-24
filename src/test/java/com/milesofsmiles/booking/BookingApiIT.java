package com.milesofsmiles.booking;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
class BookingApiIT {
    @Inject
    BookingRepository repository;

    @Test
    void apiReturnsPersistedFixtureAndValidatesIdentifiers() {
        given().get("/api/booking-requests/" + BookingRequest.FIXTURE_ID)
                .then().statusCode(200)
                .body("id", equalTo(BookingRequest.FIXTURE_ID.toString()))
                .body("reference", equalTo(BookingRequest.FIXTURE_REFERENCE))
                .body("createdAt", equalTo("2026-01-02T10:00:00Z"));
        given().get("/api/booking-requests/not-a-uuid").then().statusCode(400)
                .body("code", equalTo("INVALID_ID"));
        given().get("/api/booking-requests/1-1-1-1-1").then().statusCode(400);
        given().get("/api/booking-requests/00000000-0000-4000-8000-000000000099")
                .then().statusCode(404).body("code", equalTo("NOT_FOUND"));
    }

    @Test
    void repeatedInitializationNeverReplacesStoredFixture() {
        var before = repository.find(BookingRequest.FIXTURE_ID).orElseThrow();
        repository.initialize(Instant.parse("2030-01-01T00:00:00Z"));
        assertEquals(before, repository.find(BookingRequest.FIXTURE_ID).orElseThrow());
    }

    @Test
    void jdbcUsesVerifiedTlsAndRejectsUntrustedCertificate() throws Exception {
        var properties = new Properties();
        properties.setProperty("user", PostgresResource.postgres.getUsername());
        properties.setProperty("password", PostgresResource.postgres.getPassword());
        properties.setProperty("sslmode", "verify-full");
        properties.setProperty("sslrootcert", PostgresResource.pki.resolve("ca.crt").toString());
        try (var connection = DriverManager.getConnection(PostgresResource.postgres.getJdbcUrl(), properties);
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT ssl FROM pg_stat_ssl WHERE pid = pg_backend_pid()")) {
            assertTrue(result.next());
            assertTrue(result.getBoolean(1));
        }
        properties.setProperty("sslrootcert", PostgresResource.pki.resolve("app/server.crt").toString());
        assertThrows(java.sql.SQLException.class,
                () -> DriverManager.getConnection(PostgresResource.postgres.getJdbcUrl(), properties));
    }

    @Test
    void databaseOutageLeavesLivenessUpButReadinessAndApiFail() {
        var docker = DockerClientFactory.instance().client();
        String id = PostgresResource.postgres.getContainerId();
        docker.pauseContainerCmd(id).exec();
        try {
            given().get("/q/health/live").then().statusCode(200);
            given().get("/q/health/ready").then().statusCode(503);
            given().get("/api/booking-requests/" + BookingRequest.FIXTURE_ID)
                    .then().statusCode(503).body("code", equalTo("STORAGE_UNAVAILABLE"));
        } finally {
            docker.unpauseContainerCmd(id).exec();
        }
        given().get("/q/health/ready").then().statusCode(200);
        given().get("/api/booking-requests/" + BookingRequest.FIXTURE_ID).then().statusCode(200);
    }

    @Test
    void httpsHealthProbeVerifiesCaAndHostname() throws Exception {
        String endpoint = "https://localhost:8444/q/health/live";
        HealthProbe.main(new String[]{endpoint, PostgresResource.pki.resolve("ca.crt").toString()});
        assertThrows(javax.net.ssl.SSLHandshakeException.class, () -> HealthProbe.main(
                new String[]{endpoint, PostgresResource.pki.resolve("postgres/server.crt").toString()}));
    }
}
