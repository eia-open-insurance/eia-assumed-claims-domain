package com.eia.assumedclaims.domain;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClaimTaskNoteController.class)
@Import(ClaimTaskNoteService.class)
class ClaimTaskNoteControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72");
    private static final UUID TASK = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a73");
    private static final UUID NOTE = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a74");
    private static final String ASSUMED = "ASSUMED";
    private static final String DIRECT = "DIRECT";
    private static final String CLAIMS = "/api/v1/assumed-claims/claims/" + CLAIM;
    private static final String SHORT = "/api/v1/assumed-claims/" + CLAIM;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AssumedClaimClient client;

    // ------------------------------------------------------------------ tasks
    @Test
    void listsTasksWithTheExactDirectProjection() throws Exception {
        given(client.findTasks(TENANT, CLAIM)).willReturn(List.of(task("OPEN")));

        mvc.perform(get(CLAIMS + "/tasks")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TASK.toString()))
                .andExpect(jsonPath("$[0].claimId").value(CLAIM.toString()))
                .andExpect(jsonPath("$[0].taskNumber").value("TSK-2026-0000001"))
                .andExpect(jsonPath("$[0].title").value("Obtain updated medical records"))
                .andExpect(jsonPath("$[0].description").value("Provider records requested at FNOL"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-04-01"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].assignedTo").value("A. Adjuster"))
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[0].createdAt").value("2026-03-18T09:00:00Z"))
                .andExpect(jsonPath("$[0].updatedAt").value("2026-03-18T09:00:00Z"))
                .andExpect(jsonPath("$[0].completedAt").isEmpty())
                .andExpect(jsonPath("$[0].tenantId").doesNotExist());
    }

    @Test
    void taskListIsAlsoServedUnderTheShortAlias() throws Exception {
        given(client.findTasks(TENANT, CLAIM)).willReturn(List.of(task("OPEN")));

        mvc.perform(get(SHORT + "/tasks")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void opensATaskWithThePublicPayload() throws Exception {
        given(client.addTask(eq(TENANT), eq(CLAIM), any(CreateTaskCommand.class), isNull()))
                .willReturn(new CommandResult<>(task("OPEN"), false));

        mvc.perform(post(CLAIMS + "/tasks")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Obtain updated medical records",
                                  "description": "Provider records requested at FNOL",
                                  "dueDate": "2026-04-01",
                                  "priority": "HIGH",
                                  "assignedTo": "A. Adjuster"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replay", "false"))
                .andExpect(jsonPath("$.id").value(TASK.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));

        ArgumentCaptor<CreateTaskCommand> command = ArgumentCaptor.forClass(CreateTaskCommand.class);
        verify(client).addTask(eq(TENANT), eq(CLAIM), command.capture(), isNull());
        assertEquals(new CreateTaskCommand("Obtain updated medical records",
                "Provider records requested at FNOL", LocalDate.of(2026, 4, 1), "HIGH",
                "A. Adjuster"), command.getValue());
    }

    @Test
    void forwardsAnIdempotencyKeyOnTaskCreate() throws Exception {
        given(client.addTask(eq(TENANT), eq(CLAIM), any(CreateTaskCommand.class),
                eq("task-key-1"))).willReturn(new CommandResult<>(task("OPEN"), false));

        mvc.perform(post(CLAIMS + "/tasks")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("Idempotency-Key", "task-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Obtain records", "dueDate": "2026-04-01",
                                 "priority": "HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replay", "false"))
                .andExpect(jsonPath("$.id").value(TASK.toString()));

        verify(client).addTask(eq(TENANT), eq(CLAIM), any(CreateTaskCommand.class),
                eq("task-key-1"));
    }

    @Test
    void replaysATaskCreateWithTheOriginalBodyAndMarker() throws Exception {
        given(client.addTask(eq(TENANT), eq(CLAIM), any(CreateTaskCommand.class),
                eq("task-key-1"))).willReturn(new CommandResult<>(task("OPEN"), true));

        mvc.perform(post(CLAIMS + "/tasks")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("Idempotency-Key", "task-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Obtain records", "dueDate": "2026-04-01",
                                 "priority": "HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.id").value(TASK.toString()))
                .andExpect(jsonPath("$.taskNumber").value("TSK-2026-0000001"));
    }

    @Test
    void rejectsAnIdempotencyKeyOutsideTheLengthBounds() throws Exception {
        mvc.perform(post(CLAIMS + "/tasks")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("Idempotency-Key", "x".repeat(65))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Obtain records", "dueDate": "2026-04-01",
                                 "priority": "HIGH"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_IDEMPOTENCY_KEY"));

        verifyNoInteractions(client);
    }

    @Test
    void changesATaskStatusWithTheObservedVersion() throws Exception {
        given(client.changeTaskStatus(eq(TENANT), eq(CLAIM), eq(TASK),
                any(TaskStatusChangeCommand.class))).willReturn(task("COMPLETE"));

        mvc.perform(patch(CLAIMS + "/tasks/" + TASK)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETE\", \"version\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETE"))
                .andExpect(jsonPath("$.completedAt").value("2026-03-18T09:00:00Z"));

        ArgumentCaptor<TaskStatusChangeCommand> command =
                ArgumentCaptor.forClass(TaskStatusChangeCommand.class);
        verify(client).changeTaskStatus(eq(TENANT), eq(CLAIM), eq(TASK), command.capture());
        assertEquals(new TaskStatusChangeCommand("COMPLETE", 1L), command.getValue());
    }

    @Test
    void taskRoutesKeepTheAssumedBookWall() throws Exception {
        mvc.perform(get(CLAIMS + "/tasks")
                        .header("X-EIA-Book", DIRECT)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"))
                .andExpect(jsonPath("$.category").value("NOT_FOUND"));

        mvc.perform(post(CLAIMS + "/tasks")
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"));

        verifyNoInteractions(client);
    }

    @Test
    void taskRoutesRequireTheTenantHeader() throws Exception {
        mvc.perform(get(CLAIMS + "/tasks").header("X-EIA-Book", ASSUMED))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-PLT-0201"));

        verifyNoInteractions(client);
    }

    // ------------------------------------------------------------------ notes
    @Test
    void listsNotesWithTheExactDirectProjection() throws Exception {
        given(client.findNotes(TENANT, CLAIM)).willReturn(List.of(note()));

        mvc.perform(get(CLAIMS + "/notes")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(NOTE.toString()))
                .andExpect(jsonPath("$[0].claimId").value(CLAIM.toString()))
                .andExpect(jsonPath("$[0].noteNumber").value("NTE-2026-0000001"))
                .andExpect(jsonPath("$[0].noteType").value("DIARY"))
                .andExpect(jsonPath("$[0].body").value(
                        "Called the claimant to confirm the medical authorization"))
                .andExpect(jsonPath("$[0].author").value("Dana Handler"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-03-18T09:00:00Z"))
                .andExpect(jsonPath("$[0].tenantId").doesNotExist());
    }

    @Test
    void addsANoteWithThePublicPayload() throws Exception {
        given(client.addNote(eq(TENANT), eq(CLAIM), any(CreateNoteCommand.class), isNull()))
                .willReturn(new CommandResult<>(note(), false));

        mvc.perform(post(CLAIMS + "/notes")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "noteType": "DIARY",
                                  "body": "Called the claimant to confirm the medical authorization",
                                  "author": "Dana Handler"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replay", "false"))
                .andExpect(jsonPath("$.noteNumber").value("NTE-2026-0000001"));

        ArgumentCaptor<CreateNoteCommand> command = ArgumentCaptor.forClass(CreateNoteCommand.class);
        verify(client).addNote(eq(TENANT), eq(CLAIM), command.capture(), isNull());
        assertEquals(new CreateNoteCommand("DIARY",
                "Called the claimant to confirm the medical authorization", "Dana Handler"),
                command.getValue());
    }

    @Test
    void replaysANoteAppendWithTheOriginalMarker() throws Exception {
        given(client.addNote(eq(TENANT), eq(CLAIM), any(CreateNoteCommand.class),
                eq("note-key-1"))).willReturn(new CommandResult<>(note(), true));

        mvc.perform(post(CLAIMS + "/notes")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("Idempotency-Key", "note-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"noteType": "DIARY", "body": "Called the claimant",
                                 "author": "Dana Handler"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotent-Replay", "true"))
                .andExpect(jsonPath("$.id").value(NOTE.toString()))
                .andExpect(jsonPath("$.noteNumber").value("NTE-2026-0000001"));
    }

    @Test
    void preservesTheExpertIdempotencyConflict() throws Exception {
        given(client.addTask(eq(TENANT), eq(CLAIM), any(CreateTaskCommand.class),
                eq("task-key-1"))).willThrow(ApiException.fromExpert(409, "EIA-RNS-4091", "CONFLICT",
                "Idempotency key reused",
                "Idempotency-Key was already used with a different request.",
                List.of(new FieldIssue("Idempotency-Key", "IDEMPOTENCY_KEY_REUSED",
                        "Idempotency-Key was already used with a different request."))));

        mvc.perform(post(CLAIMS + "/tasks")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("Idempotency-Key", "task-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "A different task", "dueDate": "2026-04-01",
                                 "priority": "HIGH"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-4091"))
                .andExpect(jsonPath("$.category").value("CONFLICT"))
                .andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"))
                .andExpect(jsonPath("$.errors[0].code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    void noteRoutesKeepTheAssumedBookWall() throws Exception {
        mvc.perform(post(CLAIMS + "/notes")
                        .header("X-EIA-Book", DIRECT)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"));

        verifyNoInteractions(client);
    }

    // ------------------------------------------------------------------ domain validation
    @Test
    void invalidTaskPriorityIsAProblem400BeforeTheExpertCall() throws Exception {
        mvc.perform(post(CLAIMS + "/tasks")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Title", "dueDate": "2026-04-01", "priority": "CATASTROPHE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.category").value("VALIDATION"))
                .andExpect(jsonPath("$.detail").value("Unknown priority: CATASTROPHE"))
                .andExpect(jsonPath("$.errors[0].field").value("priority"));

        verifyNoInteractions(client);
    }

    @Test
    void invalidTaskStatusIsAProblem400BeforeTheExpertCall() throws Exception {
        mvc.perform(patch(CLAIMS + "/tasks/" + TASK)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DONE\", \"version\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.detail").value("Unknown task status: DONE"))
                .andExpect(jsonPath("$.errors[0].field").value("status"));

        verifyNoInteractions(client);
    }

    @Test
    void missingTaskVersionIsAProblem400BeforeTheExpertCall() throws Exception {
        mvc.perform(patch(CLAIMS + "/tasks/" + TASK)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.detail").value("version is required"));

        verifyNoInteractions(client);
    }

    @Test
    void invalidNoteTypeIsAProblem400BeforeTheExpertCall() throws Exception {
        mvc.perform(post(CLAIMS + "/notes")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"noteType": "RANDOM", "body": "Body", "author": "Author"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.detail").value("Unknown noteType: RANDOM"))
                .andExpect(jsonPath("$.errors[0].field").value("noteType"));

        verifyNoInteractions(client);
    }

    // ------------------------------------------------------------------ expert problems
    @Test
    void expertConflictOnATaskIsPreservedAs409() throws Exception {
        given(client.changeTaskStatus(eq(TENANT), eq(CLAIM), eq(TASK),
                any(TaskStatusChangeCommand.class)))
                .willThrow(ApiException.fromExpert(409, "EIA-RNS-7006", "CONFLICT",
                        "Version conflict", "Claim task version 1 does not match current version 2",
                        List.of()));

        mvc.perform(patch(CLAIMS + "/tasks/" + TASK)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETE\", \"version\": 1}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7006"))
                .andExpect(jsonPath("$.detail").value(
                        "Claim task version 1 does not match current version 2"));
    }

    @Test
    void expertNotFoundOnTasksIsPreservedAs404() throws Exception {
        given(client.findTasks(TENANT, CLAIM))
                .willThrow(ApiException.fromExpert(404, "EIA-RNS-7001", "NOT_FOUND",
                        "Assumed claim not found", "Assumed claim " + CLAIM + " was not found.",
                        List.of()));

        mvc.perform(get(CLAIMS + "/tasks")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7001"))
                .andExpect(jsonPath("$.detail").value("Assumed claim " + CLAIM + " was not found."));
    }

    @Test
    void expertBadRequestOnANoteIsPreservedAs400() throws Exception {
        given(client.addNote(eq(TENANT), eq(CLAIM), any(CreateNoteCommand.class), isNull()))
                .willThrow(ApiException.fromExpert(400, "EIA-RNS-7004", "VALIDATION",
                        "Request validation failed", "noteType is required", List.of()));

        mvc.perform(post(CLAIMS + "/notes")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"noteType\": \"GENERAL\", \"body\": \"Body\", \"author\": \"Author\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.detail").value("noteType is required"));
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
        return new ClaimNoteDto(NOTE, CLAIM, "NTE-2026-0000001", "DIARY",
                "Called the claimant to confirm the medical authorization", "Dana Handler",
                Instant.parse("2026-03-18T09:00:00Z"));
    }
}
