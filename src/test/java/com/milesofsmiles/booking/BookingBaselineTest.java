package com.milesofsmiles.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class BookingBaselineTest {
    private static final Instant NOW = Instant.parse("2026-01-02T10:00:00Z");

    @Test
    void initializerUsesInjectedClock() {
        var repository = mock(BookingRepository.class);
        new FixtureInitializer(repository, Clock.fixed(NOW, ZoneOffset.UTC)).start(null);
        verify(repository).initialize(NOW);
    }

    @Test
    void clockSupportsDeterministicAndSystemUtcModes() {
        var producer = new ClockProducer();
        assertEquals(NOW, producer.clock(Optional.of(NOW.toString())).instant());
        assertEquals(ZoneOffset.UTC, producer.clock(Optional.empty()).getZone());
        assertThrows(java.time.format.DateTimeParseException.class,
                () -> producer.clock(Optional.of("not-an-instant")));
    }

    @Test
    void resourceReturnsStoredFixtureAndExplicitMissingOutcome() {
        var repository = mock(BookingRepository.class);
        var fixture = new BookingRequest(BookingRequest.FIXTURE_ID, BookingRequest.FIXTURE_REFERENCE, NOW);
        when(repository.find(BookingRequest.FIXTURE_ID)).thenReturn(Optional.of(fixture), Optional.empty());
        var resource = new BookingResource(repository);
        try (var found = resource.find(BookingRequest.FIXTURE_ID.toString());
                var missing = resource.find(BookingRequest.FIXTURE_ID.toString())) {
            assertEquals(200, found.getStatus());
            assertEquals(fixture, found.getEntity());
            assertEquals(404, missing.getStatus());
        }
    }

    @Test
    void invalidIdentifierNeverReachesStorage() {
        var repository = mock(BookingRepository.class);
        var resource = new BookingResource(repository);
        for (String invalid : new String[]{"wrong", "1-1-1-1-1", "' OR 1=1 --"}) {
            try (var response = resource.find(invalid)) {
                assertEquals(400, response.getStatus());
            }
        }
        verifyNoInteractions(repository);
    }

    @Test
    void databaseFailureIsNotReportedAsMissingOrSuccess() throws Exception {
        var source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("sensitive provider detail", "08001"));
        var failure = assertThrows(BookingStorageException.class,
                () -> new BookingRepository(source).find(BookingRequest.FIXTURE_ID));
        try (var response = new BookingStorageExceptionMapper().toResponse(failure)) {
            assertEquals(503, response.getStatus());
            assertFalse(response.getEntity().toString().contains("sensitive provider detail"));
        }
    }

    @Test
    void repositoryMapsDatabaseTimestampAndBindsIdentifier() throws Exception {
        var source = mock(DataSource.class);
        var connection = mock(Connection.class);
        var statement = mock(PreparedStatement.class);
        var result = mock(ResultSet.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true, false);
        when(result.getObject("id", java.util.UUID.class)).thenReturn(BookingRequest.FIXTURE_ID);
        when(result.getString("reference")).thenReturn(BookingRequest.FIXTURE_REFERENCE);
        when(result.getTimestamp("created_at")).thenReturn(Timestamp.from(NOW));
        var repository = new BookingRepository(source);
        assertEquals(NOW, repository.find(BookingRequest.FIXTURE_ID).orElseThrow().createdAt());
        assertTrue(repository.find(BookingRequest.FIXTURE_ID).isEmpty());
        verify(statement, times(2)).setObject(1, BookingRequest.FIXTURE_ID);
    }

    @Test
    void initializationCommitsAndFailureRollsBack() throws Exception {
        var source = mock(DataSource.class);
        var connection = mock(Connection.class);
        var ddl = mock(Statement.class);
        var insert = mock(PreparedStatement.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(ddl);
        when(connection.prepareStatement(anyString())).thenReturn(insert);
        var repository = new BookingRepository(source);
        repository.initialize(NOW);
        verify(insert).setObject(3, NOW.atOffset(ZoneOffset.UTC));
        verify(connection).commit();
        when(insert.executeUpdate()).thenThrow(new SQLException("write failed", "08006"));
        assertThrows(BookingStorageException.class, () -> repository.initialize(NOW));
        verify(connection).rollback();
    }
}
