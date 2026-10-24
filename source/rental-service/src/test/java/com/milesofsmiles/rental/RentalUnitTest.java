package com.milesofsmiles.rental;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class RentalUnitTest {
    static final String BOOKING = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
    static final String VEHICLE = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb";
    static final Instant NOW = Instant.parse("2026-01-02T10:00:00Z");
    static final RentalRequest REQUEST = new RentalRequest(1, BOOKING, VEHICLE);
    static final DraftRental DRAFT = new DraftRental(UUID.randomUUID(), 1, UUID.fromString(BOOKING),
            UUID.fromString(VEHICLE), "DRAFT", NOW);

    @Test
    void validatesVersionAndEveryRequiredField() {
        for (RentalRequest invalid : new RentalRequest[]{
                new RentalRequest(null, BOOKING, VEHICLE), new RentalRequest(0, BOOKING, VEHICLE),
                new RentalRequest(2, BOOKING, VEHICLE), new RentalRequest(1, null, VEHICLE),
                new RentalRequest(1, BOOKING, null), new RentalRequest(1, "1-1-1-1-1", VEHICLE),
                new RentalRequest(1, BOOKING, VEHICLE + " "), new RentalRequest(1, "", VEHICLE)}) {
            assertThrows(IllegalArgumentException.class, () -> invalid.validate("key"));
        }
    }

    @Test
    void validatesBoundedAsciiRequestKeys() {
        for (String key : new String[]{"", " ", "a/b", "é", "a\n", "x".repeat(129)}) {
            assertThrows(IllegalArgumentException.class, () -> REQUEST.validate(key));
        }
        assertThrows(IllegalArgumentException.class, () -> REQUEST.validate(null));
        assertNotNull(REQUEST.validate("x".repeat(128)));
        assertNotNull(REQUEST.validate("A_z.0:1-2"));
    }

    @Test
    void canonicalHashNormalizesCaseAndExcludesKey() throws Exception {
        String canonical = "contractVersion=1\nbookingId=" + BOOKING + "\nvehicleId=" + VEHICLE + "\n";
        var lower = REQUEST.validate("key1");
        var upper = new RentalRequest(1, BOOKING.toUpperCase(), VEHICLE.toUpperCase()).validate("key2");
        assertEquals(canonical, lower.canonical());
        assertEquals(lower.hash(), upper.hash());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(StandardCharsets.UTF_8))), lower.hash());
        assertEquals(64, lower.hash().length());
        assertNotEquals(lower.hash(), new RentalRequest.Validated(2, lower.bookingId(), lower.vehicleId()).hash());
        assertNotEquals(lower.hash(), new RentalRequest(1, VEHICLE, BOOKING).validate("key").hash());
    }

    @Test
    void jsonRejectsScalarCoercionAndTrailingTokens() {
        var mapper = new ObjectMapper();
        new StrictJsonCustomizer().customize(mapper);
        for (String payload : new String[]{
                "{\"contractVersion\":\"1\"}", "{\"contractVersion\":1.1}",
                "{\"bookingId\":123}", "{\"vehicleId\":true}", "{} {}",
                "{\"contractVersion\":1,\"contractVersion\":1}"}) {
            assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
                    () -> mapper.readValue(payload, RentalRequest.class));
        }
    }

    @Test
    void resourceUsesClockAndExplicitCreationAndReplayOutcomes() {
        var repository = mock(RentalRepository.class);
        when(repository.create(eq("key"), any(), eq(NOW)))
                .thenReturn(new RentalRepository.WriteResult(DRAFT, false),
                        new RentalRepository.WriteResult(DRAFT, true));
        var resource = new RentalResource(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        try (var first = resource.create("key", REQUEST); var replay = resource.create("key", REQUEST)) {
            assertEquals(201, first.getStatus());
            assertEquals(200, replay.getStatus());
            assertEquals(DRAFT, first.getEntity());
            assertEquals(first.getEntity(), replay.getEntity());
            assertEquals("false", first.getHeaderString("Idempotency-Replayed"));
            assertEquals("true", replay.getHeaderString("Idempotency-Replayed"));
            assertEquals("/api/v1/rentals/" + DRAFT.id(), first.getLocation().toString());
        }
        verify(repository, times(2)).create("key", REQUEST.validate("key"), NOW);
    }

    @Test
    void resourceRejectsInvalidInputBeforePersistence() {
        var repository = mock(RentalRepository.class);
        var resource = new RentalResource(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        try (var missing = resource.create("key", null);
                var key = resource.create(null, REQUEST);
                var invalid = resource.create("key", new RentalRequest(2, BOOKING, VEHICLE))) {
            assertEquals(400, missing.getStatus());
            assertEquals(400, key.getStatus());
            assertEquals(400, invalid.getStatus());
        }
        for (String invalid : new String[]{"1-1-1-1-1", "wrong", BOOKING + " ", "' OR 1=1 --"}) {
            try (var response = resource.find(invalid)) {
                assertEquals(400, response.getStatus());
            }
        }
        verifyNoInteractions(repository);
    }

    @Test
    void resourceDifferentiatesMissingFoundAndConflict() {
        var repository = mock(RentalRepository.class);
        when(repository.find(DRAFT.id())).thenReturn(Optional.of(DRAFT), Optional.empty());
        when(repository.create(anyString(), any(), any())).thenThrow(new RentalConflictException());
        var resource = new RentalResource(repository, Clock.fixed(NOW, ZoneOffset.UTC));
        try (var found = resource.find(DRAFT.id().toString()); var missing = resource.find(DRAFT.id().toString());
                var conflict = resource.create("key", REQUEST)) {
            assertEquals(200, found.getStatus());
            assertEquals(DRAFT, found.getEntity());
            assertEquals(404, missing.getStatus());
            assertEquals(409, conflict.getStatus());
        }
    }

    @Test
    void clockSupportsFixedAndSystemUtcAndRejectsBadConfiguration() {
        var producer = new ClockProducer();
        assertEquals(NOW, producer.clock(Optional.of(NOW.toString())).instant());
        assertEquals(ZoneOffset.UTC, producer.clock(Optional.empty()).getZone());
        assertThrows(java.time.format.DateTimeParseException.class, () -> producer.clock(Optional.of("bad")));
    }

    @Test
    void jsonMappersReturnStableErrorsWithoutProviderDetails() {
        try (var response = new RequestExceptionMapper().toResponse(
                new jakarta.ws.rs.WebApplicationException("private parser details", 400))) {
            assertEquals(400, response.getStatus());
            assertEquals("INVALID_REQUEST", ((RentalResource.ApiError) response.getEntity()).code());
            assertFalse(response.getEntity().toString().contains("private"));
        }
        try (var original = jakarta.ws.rs.core.Response.status(415).build()) {
            assertSame(original, new RequestExceptionMapper().toResponse(
                    new jakarta.ws.rs.WebApplicationException(original)));
        }
        try (var response = new InvalidJsonMapper().toResponse(null)) {
            assertEquals(400, response.getStatus());
            assertEquals("INVALID_REQUEST", ((RentalResource.ApiError) response.getEntity()).code());
        }
    }

    @Test
    void insertsWithBoundValuesAndCommits() throws Exception {
        var jdbc = new Jdbc();
        when(jdbc.result.next()).thenReturn(true);
        jdbc.row();
        var result = jdbc.repository.create("key", REQUEST.validate("key"), NOW);
        assertEquals(DRAFT, result.rental());
        assertFalse(result.replay());
        verify(jdbc.connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        verify(jdbc.connection).setAutoCommit(false);
        verify(jdbc.statement).setString(3, "key");
        verify(jdbc.statement).setString(4, REQUEST.validate("key").hash());
        verify(jdbc.statement).setObject(5, UUID.fromString(BOOKING));
        verify(jdbc.statement).setObject(6, UUID.fromString(VEHICLE));
        verify(jdbc.statement).setObject(7, NOW.atOffset(ZoneOffset.UTC));
        verify(jdbc.connection).commit();
        verify(jdbc.connection, never()).rollback();
    }

    @Test
    void uniqueKeyLoserReadsOriginalAndComparesHash() throws Exception {
        var jdbc = new Jdbc();
        when(jdbc.result.next()).thenReturn(false, true);
        when(jdbc.result.getString("request_hash")).thenReturn(REQUEST.validate("key").hash());
        jdbc.row();
        var result = jdbc.repository.create("key", REQUEST.validate("key"), NOW.plusSeconds(10));
        assertTrue(result.replay());
        assertEquals(DRAFT, result.rental());
        verify(jdbc.connection).commit();
    }

    @Test
    void changedPayloadRollsBackAndDoesNotCommit() throws Exception {
        var jdbc = new Jdbc();
        when(jdbc.result.next()).thenReturn(false, true);
        when(jdbc.result.getString("request_hash")).thenReturn("different");
        assertThrows(RentalConflictException.class,
                () -> jdbc.repository.create("key", REQUEST.validate("key"), NOW));
        verify(jdbc.connection).rollback();
        verify(jdbc.connection, never()).commit();
    }

    @Test
    void sqlFailureRollsBackAndPreservesRollbackFailure() throws Exception {
        var jdbc = new Jdbc();
        var sqlFailure = new SQLException("sensitive write detail", "08006");
        when(jdbc.statement.executeQuery()).thenThrow(sqlFailure);
        doThrow(new SQLException("sensitive rollback detail", "08006")).when(jdbc.connection).rollback();
        var failure = assertThrows(RentalStorageException.class,
                () -> jdbc.repository.create("key", REQUEST.validate("key"), NOW));
        assertSame(sqlFailure, failure.getCause());
        assertEquals(1, sqlFailure.getSuppressed().length);
        verify(jdbc.connection).rollback();
        verify(jdbc.connection, never()).commit();
    }

    @Test
    void commitFailureDoesNotReturnSuccess() throws Exception {
        var jdbc = new Jdbc();
        when(jdbc.result.next()).thenReturn(true);
        jdbc.row();
        doThrow(new SQLException("commit unavailable", "08006")).when(jdbc.connection).commit();
        assertThrows(RentalStorageException.class,
                () -> jdbc.repository.create("key", REQUEST.validate("key"), NOW));
        verify(jdbc.connection).rollback();
    }

    @Test
    void missingConflictRowIsStorageFailureNotReplay() throws Exception {
        var jdbc = new Jdbc();
        when(jdbc.result.next()).thenReturn(false);
        assertEquals("40001", assertThrows(RentalStorageException.class,
                () -> jdbc.repository.create("key", REQUEST.validate("key"), NOW)).sqlState());
        verify(jdbc.connection).rollback();
    }

    @Test
    void readBindsIdentifierAndDistinguishesMissing() throws Exception {
        var jdbc = new Jdbc();
        when(jdbc.result.next()).thenReturn(true, false);
        jdbc.row();
        assertEquals(DRAFT, jdbc.repository.find(DRAFT.id()).orElseThrow());
        assertTrue(jdbc.repository.find(DRAFT.id()).isEmpty());
        verify(jdbc.statement, times(2)).setObject(1, DRAFT.id());
    }

    @Test
    void failedConnectionReturnsSafe503AndOnlySqlStateIsLogged() throws Exception {
        var source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("SECRET provider detail", "08001"));
        var failure = assertThrows(RentalStorageException.class, () -> new RentalRepository(source).find(DRAFT.id()));
        var log = new StringBuilder();
        var logger = Logger.getLogger(RentalStorageExceptionMapper.class.getName());
        var handler = new Handler() {
            public void publish(LogRecord record) {
                log.append(record.getMessage());
                assertNull(record.getThrown());
            }
            public void flush() { }
            public void close() { }
        };
        logger.addHandler(handler);
        try (var response = new RentalStorageExceptionMapper().toResponse(failure)) {
            assertEquals(503, response.getStatus());
            assertEquals(new RentalResource.ApiError("STORAGE_UNAVAILABLE", "Rental storage is unavailable"),
                    response.getEntity());
            assertFalse(response.getEntity().toString().contains("SECRET"));
            assertTrue(log.toString().contains("08001"));
            assertFalse(log.toString().contains("SECRET"));
        } finally {
            logger.removeHandler(handler);
        }
    }

    @Test
    void startupGuardAcceptsExactlyOneVersionOneRow() throws Exception {
        var source = mock(DataSource.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        var result = mock(ResultSet.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT contract_version FROM rental.schema_version")).thenReturn(result);
        when(result.next()).thenReturn(true, false);
        when(result.getInt(1)).thenReturn(1);
        new RentalSchemaGuard(source).start(null);
        verify(statement, never()).execute(anyString());
        when(result.next()).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> new RentalSchemaGuard(source).check());
        when(result.next()).thenReturn(true, true);
        assertThrows(IllegalStateException.class, () -> new RentalSchemaGuard(source).check());
        when(result.getInt(1)).thenReturn(2);
        assertThrows(IllegalStateException.class, () -> new RentalSchemaGuard(source).check());
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("private schema detail", "42P01"));
        assertThrows(RentalStorageException.class, () -> new RentalSchemaGuard(source).check());
    }

    private static class Jdbc {
        final DataSource source = mock(DataSource.class);
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet result = mock(ResultSet.class);
        final RentalRepository repository = new RentalRepository(source);

        Jdbc() throws Exception {
            when(source.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(result);
        }

        void row() throws Exception {
            when(result.getObject("id", UUID.class)).thenReturn(DRAFT.id());
            when(result.getInt("contract_version")).thenReturn(1);
            when(result.getObject("booking_id", UUID.class)).thenReturn(DRAFT.bookingId());
            when(result.getObject("vehicle_id", UUID.class)).thenReturn(DRAFT.vehicleId());
            when(result.getString("state")).thenReturn("DRAFT");
            when(result.getTimestamp("created_at")).thenReturn(Timestamp.from(NOW));
        }
    }
}
