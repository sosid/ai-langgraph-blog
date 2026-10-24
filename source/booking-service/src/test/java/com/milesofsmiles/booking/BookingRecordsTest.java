package com.milesofsmiles.booking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class BookingRecordsTest {
    static final BookingSubmission INPUT = new BookingSubmission(1, "COMPACT",
            "2026-11-01T10:00:00Z", "2026-11-03T10:00:00Z");

    @Test
    void hashUsesSemanticInstantsAndRejectsUnboundedOrUnsupportedInput() {
        assertEquals(INPUT.validate("key").hash(), new BookingSubmission(1, "COMPACT",
                "2026-11-01T11:00:00+01:00", "2026-11-03T10:00:00.000Z").validate("other").hash());
        assertEquals(64, INPUT.validate("key").hash().length());
        for (String key : new String[]{"", "a b", "x".repeat(129)}) {
            assertThrows(IllegalArgumentException.class, () -> INPUT.validate(key));
        }
        assertThrows(IllegalArgumentException.class, () -> INPUT.validate(null));
        assertThrows(IllegalArgumentException.class, () -> new BookingSubmission(2, "COMPACT",
                INPUT.startsAt(), INPUT.endsAt()).validate("key"));
        assertThrows(IllegalArgumentException.class, () -> new BookingSubmission(1, "COMPACT",
                INPUT.endsAt(), INPUT.startsAt()).validate("key"));
        assertThrows(IllegalArgumentException.class, () -> new BookingSubmission(1, "COMPACT",
                "2026-11-01T10:00:00.000000001Z", INPUT.endsAt()).validate("key"));
        assertThrows(IllegalArgumentException.class, () -> new BookingSubmission(1, "COMPACT",
                null, INPUT.endsAt()).validate("key"));
    }

    @Test
    void resourceDistinguishesCreationReplayConflictInvalidAndMissing() {
        var repository = mock(BookingRecordRepository.class);
        var resource = new BookingRecordResource(repository);
        var record = new BookingRecord(UUID.randomUUID(), 1, "COMPACT",
                Instant.parse(INPUT.startsAt()), Instant.parse(INPUT.endsAt()), Instant.EPOCH);
        when(repository.create(eq("key"), any())).thenReturn(
                new BookingRecordRepository.Creation(record, true, false),
                new BookingRecordRepository.Creation(record, false, false),
                new BookingRecordRepository.Creation(record, false, true));
        for (int status : new int[]{201, 200, 409}) {
            try (var response = resource.create("key", INPUT)) {
                assertEquals(status, response.getStatus());
            }
        }
        try (var response = resource.create("key", null);
                var invalid = resource.find("1-1-1-1-1")) {
            assertEquals(400, response.getStatus());
            assertEquals(400, invalid.getStatus());
        }
        when(repository.find(record.id())).thenReturn(Optional.empty(), Optional.of(record));
        try (var missing = resource.find(record.id().toString());
                var found = resource.find(record.id().toString())) {
            assertEquals(404, missing.getStatus());
            assertEquals(record, found.getEntity());
        }
    }

    @Test
    void failedCreateRollsBackAndPropagatesSafeStorageFailure() throws Exception {
        var source = mock(DataSource.class);
        var connection = mock(java.sql.Connection.class);
        var statement = mock(java.sql.PreparedStatement.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenThrow(new SQLException("private detail", "08006"));
        var repository = new BookingRecordRepository(source, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        assertThrows(BookingStorageException.class, () -> repository.create("key", INPUT.validate("key")));
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void schemaGuardRejectsIncompatibleVersion() throws Exception {
        var source = mock(DataSource.class);
        var connection = mock(java.sql.Connection.class);
        var statement = mock(java.sql.PreparedStatement.class);
        var result = mock(java.sql.ResultSet.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getInt(1)).thenReturn(2);
        assertThrows(IllegalStateException.class, () -> new SchemaGuard(source).check(null));
    }
}
