package com.milesofsmiles.fleet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.quarkus.runtime.StartupEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FleetUnitTest {
    DataSource source;
    Connection connection;
    PreparedStatement statement;
    ResultSet result;
    FleetRepository repository;

    @BeforeEach
    void setup() throws Exception {
        source = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        result = mock(ResultSet.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        repository = new FleetRepository(source);
    }

    @Test
    void invalidIdsNeverReachStorage() {
        var resource = new VehicleResource(repository);
        for (String id : new String[] {"bad", "1-1-1-1-1", " 00000000-0000-4000-8000-000000000002"}) {
            assertEquals(400, resource.find(id).getStatus());
        }
        verifyNoInteractions(source);
    }

    @Test
    void resourceMapsPresentAndMissing() {
        var mocked = mock(FleetRepository.class);
        var vehicle = new Vehicle(Vehicle.FIXTURE_ID, "SYNTHETIC-COMPACT-001", 1);
        when(mocked.find(Vehicle.FIXTURE_ID)).thenReturn(Optional.of(vehicle), Optional.empty());
        var resource = new VehicleResource(mocked);
        assertEquals(vehicle, resource.find(Vehicle.FIXTURE_ID.toString()).getEntity());
        assertEquals(404, resource.find(Vehicle.FIXTURE_ID.toString()).getStatus());
    }

    @Test
    void readsUseBoundIdAndMapVersion() throws Exception {
        when(result.next()).thenReturn(true);
        when(result.getObject("id", UUID.class)).thenReturn(Vehicle.FIXTURE_ID);
        when(result.getString("reference")).thenReturn("SYNTHETIC-COMPACT-001");
        when(result.getInt("contract_version")).thenReturn(1);
        assertEquals(new Vehicle(Vehicle.FIXTURE_ID, "SYNTHETIC-COMPACT-001", 1),
                repository.find(Vehicle.FIXTURE_ID).orElseThrow());
        verify(statement).setObject(1, Vehicle.FIXTURE_ID);
    }

    @Test
    void missingIsNotStorageFailure() throws Exception {
        assertTrue(repository.find(Vehicle.FIXTURE_ID).isEmpty());
        when(statement.executeQuery()).thenThrow(new SQLException("private provider detail", "08006"));
        var failure = assertThrows(FleetStorageException.class, () -> repository.find(Vehicle.FIXTURE_ID));
        var response = new FleetStorageExceptionMapper().toResponse(failure);
        assertEquals(503, response.getStatus());
        assertFalse(response.getEntity().toString().contains("private"));
        assertEquals("08006", failure.sqlState());
    }

    @Test
    void validatesIntervalsBeforeStorage() {
        Instant start = Instant.parse("2026-06-01T10:00:00Z");
        Instant end = start.plusSeconds(3_600);
        assertDoesNotThrow(() -> interval(start, end));
        assertThrows(IllegalArgumentException.class, () -> interval(start, start));
        assertThrows(IllegalArgumentException.class, () -> interval(end, start));
        assertThrows(IllegalArgumentException.class, () -> interval(start.plusNanos(1), end));
        assertThrows(IllegalArgumentException.class,
                () -> interval(Instant.parse("0000-01-01T00:00:00Z"), end));
        assertThrows(IllegalArgumentException.class,
                () -> interval(start, Instant.parse("+10000-01-01T00:00:00Z")));
        assertThrows(NullPointerException.class,
                () -> new AllocationInterval(UUID.randomUUID(), Vehicle.FIXTURE_ID, start, end, null));
        verifyNoInteractions(source);
    }

    @Test
    void successfulIntervalCommitsTypedUtcValues() throws Exception {
        var interval = interval(Instant.parse("2026-06-01T10:00:00Z"), Instant.parse("2026-06-01T11:00:00Z"));
        repository.insertInterval(interval);
        verify(connection).setAutoCommit(false);
        verify(statement).setObject(3, interval.startsAt().atOffset(java.time.ZoneOffset.UTC));
        verify(statement).setString(5, "BLOCKING");
        verify(connection).commit();
        verify(connection, never()).rollback();
    }

    @Test
    void onlyExclusionSqlStateIsIntervalConflictAndAlwaysRollsBack() throws Exception {
        for (String state : new String[] {"23P01", "23505", "23503", "08006"}) {
            reset(connection, statement);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeUpdate()).thenThrow(new SQLException("private", state));
            Class<? extends RuntimeException> expected = state.equals("23P01")
                    ? IntervalConflictException.class : FleetStorageException.class;
            assertThrows(expected, () -> repository.insertInterval(
                    interval(Instant.parse("2026-06-01T10:00:00Z"), Instant.parse("2026-06-01T11:00:00Z"))));
            verify(connection).rollback();
            verify(connection, never()).commit();
        }
    }

    @Test
    void commitFailureRollsBackAndRollbackFailureIsPreserved() throws Exception {
        var failure = new SQLException("commit", "08006");
        doThrow(failure).when(connection).commit();
        doThrow(new SQLException("rollback", "08006")).when(connection).rollback();
        var thrown = assertThrows(FleetStorageException.class, () -> repository.insertInterval(
                interval(Instant.parse("2026-06-01T10:00:00Z"), Instant.parse("2026-06-01T11:00:00Z"))));
        assertSame(failure, thrown.getCause());
        assertEquals(1, failure.getSuppressed().length);
    }

    @Test
    void schemaGuardAcceptsOnlyExactlyOneVersionOneRow() throws Exception {
        when(result.next()).thenReturn(true, false);
        when(result.getInt(1)).thenReturn(1);
        new SchemaGuard(repository).onStart(new StartupEvent());
        for (int version : new int[] {0, 2}) {
            when(result.next()).thenReturn(true, false);
            when(result.getInt(1)).thenReturn(version);
            assertThrows(IllegalStateException.class, repository::verifySchema);
        }
        when(result.next()).thenReturn(false);
        assertThrows(IllegalStateException.class, repository::verifySchema);
        when(result.next()).thenReturn(true);
        when(result.getInt(1)).thenReturn(1);
        assertThrows(IllegalStateException.class, repository::verifySchema);
    }

    @Test
    void schemaStorageFailureDoesNotPassGuard() throws Exception {
        when(statement.executeQuery()).thenThrow(new SQLException("private", "42P01"));
        assertThrows(FleetStorageException.class, repository::verifySchema);
    }

    private static AllocationInterval interval(Instant start, Instant end) {
        return new AllocationInterval(UUID.randomUUID(), Vehicle.FIXTURE_ID, start, end,
                AllocationInterval.Status.BLOCKING);
    }
}
