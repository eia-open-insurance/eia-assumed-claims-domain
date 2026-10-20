package com.eia.assumedclaims.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimTaskNoteServiceTest {
    private static final UUID TENANT =
            UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM =
            UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72");
    private static final UUID TASK =
            UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a73");

    @Mock
    private AssumedClaimClient client;

    @InjectMocks
    private ClaimTaskNoteService service;

    // ------------------------------------------------------------------ tasks
    @Test
    void listsTasksForTheClaim() {
        ClaimTaskDto task = task("OPEN");
        when(client.findTasks(TENANT, CLAIM)).thenReturn(List.of(task));

        assertEquals(List.of(task), service.tasks(TENANT, CLAIM));
        verify(client).findTasks(TENANT, CLAIM);
    }

    @Test
    void taskRequiresTitleDueDateAndPriority() {
        assertThrows(ApiException.class, () -> service.addTask(TENANT, CLAIM,
                new CreateTaskCommand(" ", null, LocalDate.of(2026, 4, 1), "HIGH", null), null));
        assertThrows(ApiException.class, () -> service.addTask(TENANT, CLAIM,
                new CreateTaskCommand("Title", null, null, "HIGH", null), null));
        assertThrows(ApiException.class, () -> service.addTask(TENANT, CLAIM,
                new CreateTaskCommand("Title", null, LocalDate.of(2026, 4, 1), " ", null), null));
        verifyNoInteractions(client);
    }

    @Test
    void taskRejectsAnUnknownPriorityBeforeTheExpertCall() {
        ApiException exception = assertThrows(ApiException.class, () -> service.addTask(TENANT, CLAIM,
                new CreateTaskCommand("Title", null, LocalDate.of(2026, 4, 1), "CATASTROPHE", null),
                null));

        assertEquals(400, exception.status().value());
        assertEquals("EIA-RNS-7004", exception.code());
        assertTrue(exception.getMessage().contains("priority"));
        assertEquals("priority", exception.errors().getFirst().field());
        verifyNoInteractions(client);
    }

    @Test
    void taskDelegatesAValidCreate() {
        CreateTaskCommand command = new CreateTaskCommand("Obtain records", "Provider records",
                LocalDate.of(2026, 4, 1), "high", "A. Adjuster");
        ClaimTaskDto created = task("OPEN");
        when(client.addTask(TENANT, CLAIM, command, null))
                .thenReturn(new CommandResult<>(created, false));

        assertEquals(created, service.addTask(TENANT, CLAIM, command, null).body());
        verify(client).addTask(TENANT, CLAIM, command, null);
    }

    @Test
    void taskForwardsTheIdempotencyKeyAndReplayMarker() {
        CreateTaskCommand command = new CreateTaskCommand("Obtain records", "Provider records",
                LocalDate.of(2026, 4, 1), "HIGH", "A. Adjuster");
        ClaimTaskDto created = task("OPEN");
        when(client.addTask(TENANT, CLAIM, command, "task-key-1"))
                .thenReturn(new CommandResult<>(created, true));

        CommandResult<ClaimTaskDto> result = service.addTask(TENANT, CLAIM, command, "task-key-1");

        assertTrue(result.replayed());
        assertEquals(created, result.body());
        verify(client).addTask(TENANT, CLAIM, command, "task-key-1");
    }

    @Test
    void taskRejectsAKeyOutsideTheLengthBoundsBeforeTheExpertCall() {
        CreateTaskCommand command = new CreateTaskCommand("Obtain records", "Provider records",
                LocalDate.of(2026, 4, 1), "HIGH", "A. Adjuster");

        ApiException tooLong = assertThrows(ApiException.class,
                () -> service.addTask(TENANT, CLAIM, command, "x".repeat(65)));
        assertEquals(400, tooLong.status().value());
        assertEquals("EIA-RNS-7004", tooLong.code());
        assertEquals("INVALID_IDEMPOTENCY_KEY", tooLong.errors().getFirst().code());

        assertThrows(ApiException.class, () -> service.addTask(TENANT, CLAIM, command, "   "));
        verifyNoInteractions(client);
    }

    @Test
    void taskStatusRequiresStatusAndVersion() {
        assertThrows(ApiException.class, () -> service.changeTaskStatus(TENANT, CLAIM, TASK,
                new TaskStatusChangeCommand(null, 1L)));
        assertThrows(ApiException.class, () -> service.changeTaskStatus(TENANT, CLAIM, TASK,
                new TaskStatusChangeCommand("COMPLETE", null)));
        verifyNoInteractions(client);
    }

    @Test
    void taskStatusRejectsAnUnknownStatusBeforeTheExpertCall() {
        ApiException exception = assertThrows(ApiException.class, () -> service.changeTaskStatus(
                TENANT, CLAIM, TASK, new TaskStatusChangeCommand("DONE", 1L)));

        assertEquals(400, exception.status().value());
        assertEquals("EIA-RNS-7004", exception.code());
        assertTrue(exception.getMessage().contains("status"));
        assertEquals("status", exception.errors().getFirst().field());
        verifyNoInteractions(client);
    }

    @Test
    void taskStatusDelegatesWithTheObservedVersion() {
        TaskStatusChangeCommand command = new TaskStatusChangeCommand("COMPLETE", 3L);
        ClaimTaskDto completed = task("COMPLETE");
        when(client.changeTaskStatus(TENANT, CLAIM, TASK, command)).thenReturn(completed);

        assertEquals(completed, service.changeTaskStatus(TENANT, CLAIM, TASK, command));
        verify(client).changeTaskStatus(TENANT, CLAIM, TASK, command);
    }

    // ------------------------------------------------------------------ notes
    @Test
    void listsNotesForTheClaim() {
        ClaimNoteDto note = note();
        when(client.findNotes(TENANT, CLAIM)).thenReturn(List.of(note));

        assertEquals(List.of(note), service.notes(TENANT, CLAIM));
        verify(client).findNotes(TENANT, CLAIM);
    }

    @Test
    void noteRequiresTypeBodyAndAuthor() {
        assertThrows(ApiException.class, () -> service.addNote(TENANT, CLAIM,
                new CreateNoteCommand(null, "Body", "Author"), null));
        assertThrows(ApiException.class, () -> service.addNote(TENANT, CLAIM,
                new CreateNoteCommand("DIARY", " ", "Author"), null));
        assertThrows(ApiException.class, () -> service.addNote(TENANT, CLAIM,
                new CreateNoteCommand("DIARY", "Body", " "), null));
        verifyNoInteractions(client);
    }

    @Test
    void noteRejectsAnUnknownNoteTypeBeforeTheExpertCall() {
        ApiException exception = assertThrows(ApiException.class, () -> service.addNote(TENANT, CLAIM,
                new CreateNoteCommand("RANDOM", "Body", "Author"), null));

        assertEquals(400, exception.status().value());
        assertEquals("EIA-RNS-7004", exception.code());
        assertTrue(exception.getMessage().contains("noteType"));
        assertEquals("noteType", exception.errors().getFirst().field());
        verifyNoInteractions(client);
    }

    @Test
    void noteDelegatesAValidAppend() {
        CreateNoteCommand command = new CreateNoteCommand("DIARY", "Called the claimant", "Dana");
        ClaimNoteDto created = note();
        when(client.addNote(TENANT, CLAIM, command, null))
                .thenReturn(new CommandResult<>(created, false));

        assertEquals(created, service.addNote(TENANT, CLAIM, command, null).body());
        verify(client).addNote(TENANT, CLAIM, command, null);
    }

    @Test
    void noteForwardsTheIdempotencyKeyAndReplayMarker() {
        CreateNoteCommand command = new CreateNoteCommand("DIARY", "Called the claimant", "Dana");
        ClaimNoteDto created = note();
        when(client.addNote(TENANT, CLAIM, command, "note-key-1"))
                .thenReturn(new CommandResult<>(created, true));

        CommandResult<ClaimNoteDto> result = service.addNote(TENANT, CLAIM, command,
                "note-key-1");

        assertTrue(result.replayed());
        assertEquals(created, result.body());
        verify(client).addNote(TENANT, CLAIM, command, "note-key-1");
    }

    // ------------------------------------------------------------------ fixtures
    private static ClaimTaskDto task(String status) {
        Instant now = Instant.parse("2026-03-18T09:00:00Z");
        return new ClaimTaskDto(TASK, CLAIM, "TSK-2026-0000001",
                "Obtain updated medical records", "Provider records requested at FNOL",
                LocalDate.of(2026, 4, 1), "HIGH", status, "A. Adjuster", 1, now, now,
                "COMPLETE".equals(status) ? now : null);
    }

    private static ClaimNoteDto note() {
        return new ClaimNoteDto(
                UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a74"),
                CLAIM, "NTE-2026-0000001", "DIARY",
                "Called the claimant to confirm the medical authorization", "Dana Handler",
                Instant.parse("2026-03-18T09:00:00Z"));
    }
}
