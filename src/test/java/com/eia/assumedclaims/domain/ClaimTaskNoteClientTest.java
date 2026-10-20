package com.eia.assumedclaims.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClaimTaskNoteClientTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72");
    private static final UUID TASK = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a73");
    private static final UUID NOTE = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a74");
    private static final String EXPERT = "http://expert.test";
    private static final String TASK_URL = EXPERT + "/internal/v1/assumed-claims/" + CLAIM + "/tasks";
    private static final String NOTE_URL = EXPERT + "/internal/v1/assumed-claims/" + CLAIM + "/notes";

    private static final String TASK_JSON = """
            {
              "id": "0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a73",
              "tenantId": "11111111-1111-7111-8111-111111111111",
              "claimId": "0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72",
              "taskNumber": "TSK-2026-0000001",
              "title": "Obtain updated medical records",
              "description": "Provider records requested at FNOL",
              "dueDate": "2026-04-01",
              "priority": "HIGH",
              "status": "OPEN",
              "assignedTo": "A. Adjuster",
              "version": 3,
              "createdAt": "2026-03-18T09:00:00Z",
              "updatedAt": "2026-03-18T09:00:00Z",
              "completedAt": null
            }
            """;

    private static final String NOTE_JSON = """
            {
              "id": "0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a74",
              "tenantId": "11111111-1111-7111-8111-111111111111",
              "claimId": "0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72",
              "noteNumber": "NTE-2026-0000001",
              "noteType": "DIARY",
              "body": "Called the claimant to confirm the medical authorization",
              "author": "Dana Handler",
              "createdAt": "2026-03-18T09:00:00Z"
            }
            """;

    private MockRestServiceServer server;
    private AssumedClaimClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(converters -> {
                    ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .build();
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(0, new MappingJackson2HttpMessageConverter(mapper));
                });
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AssumedClaimClient(builder, new ObjectMapper(), EXPERT);
    }

    @Test
    void findTasksSendsAssumedBookAndTenantHeadersAndParsesTheList() {
        server.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AssumedClaimClient.BOOK_HEADER, "ASSUMED"))
                .andExpect(header(AssumedClaimClient.TENANT_HEADER, TENANT.toString()))
                .andRespond(withSuccess("[" + TASK_JSON + "]", MediaType.APPLICATION_JSON));

        List<ClaimTaskDto> tasks = client.findTasks(TENANT, CLAIM);

        assertEquals(1, tasks.size());
        ClaimTaskDto task = tasks.getFirst();
        assertEquals(TASK, task.id());
        assertEquals(CLAIM, task.claimId());
        assertEquals("TSK-2026-0000001", task.taskNumber());
        assertEquals("Obtain updated medical records", task.title());
        assertEquals("Provider records requested at FNOL", task.description());
        assertEquals(LocalDate.of(2026, 4, 1), task.dueDate());
        assertEquals("HIGH", task.priority());
        assertEquals("OPEN", task.status());
        assertEquals("A. Adjuster", task.assignedTo());
        assertEquals(3L, task.version());
        assertNotNull(task.createdAt());
        assertNull(task.completedAt());
        server.verify();
    }

    @Test
    void addTaskPostsThePublicPayloadWithHeaders() {
        server.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AssumedClaimClient.BOOK_HEADER, "ASSUMED"))
                .andExpect(header(AssumedClaimClient.TENANT_HEADER, TENANT.toString()))
                .andExpect(content().json("""
                        {
                          "title": "Obtain updated medical records",
                          "description": "Provider records requested at FNOL",
                          "dueDate": "2026-04-01",
                          "priority": "HIGH",
                          "assignedTo": "A. Adjuster"
                        }
                        """, JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(TASK_JSON));

        CommandResult<ClaimTaskDto> result = client.addTask(TENANT, CLAIM, new CreateTaskCommand(
                "Obtain updated medical records", "Provider records requested at FNOL",
                LocalDate.of(2026, 4, 1), "HIGH", "A. Adjuster"), null);

        assertEquals(TASK, result.body().id());
        assertEquals("OPEN", result.body().status());
        assertEquals(3L, result.body().version());
        assertFalse(result.replayed());
        server.verify();
    }

    @Test
    void addTaskForwardsTheIdempotencyKeyAndReadsTheReplayMarker() {
        server.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AssumedClaimClient.IDEMPOTENCY_KEY_HEADER, "task-key-1"))
                .andExpect(content().json("""
                        {
                          "title": "Obtain updated medical records",
                          "description": "Provider records requested at FNOL",
                          "dueDate": "2026-04-01",
                          "priority": "HIGH",
                          "assignedTo": "A. Adjuster"
                        }
                        """, JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AssumedClaimClient.IDEMPOTENT_REPLAY_HEADER, "true")
                        .body(TASK_JSON));

        CommandResult<ClaimTaskDto> result = client.addTask(TENANT, CLAIM, new CreateTaskCommand(
                "Obtain updated medical records", "Provider records requested at FNOL",
                LocalDate.of(2026, 4, 1), "HIGH", "A. Adjuster"), "task-key-1");

        assertEquals(TASK, result.body().id());
        assertTrue(result.replayed());
        server.verify();
    }

    @Test
    void changeTaskStatusPatchesStatusAndVersionWithHeaders() {
        server.expect(requestTo(TASK_URL + "/" + TASK))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(AssumedClaimClient.BOOK_HEADER, "ASSUMED"))
                .andExpect(header(AssumedClaimClient.TENANT_HEADER, TENANT.toString()))
                .andExpect(content().json("""
                        {"status": "COMPLETE", "version": 3}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(TASK_JSON
                        .replace("\"status\": \"OPEN\"", "\"status\": \"COMPLETE\"")
                        .replace("\"completedAt\": null",
                                "\"completedAt\": \"2026-03-18T10:00:00Z\""),
                        MediaType.APPLICATION_JSON));

        ClaimTaskDto task = client.changeTaskStatus(TENANT, CLAIM, TASK,
                new TaskStatusChangeCommand("COMPLETE", 3L));

        assertEquals("COMPLETE", task.status());
        assertNotNull(task.completedAt());
        server.verify();
    }

    @Test
    void findNotesSendsHeadersAndParsesTheList() {
        server.expect(requestTo(NOTE_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AssumedClaimClient.BOOK_HEADER, "ASSUMED"))
                .andExpect(header(AssumedClaimClient.TENANT_HEADER, TENANT.toString()))
                .andRespond(withSuccess("[" + NOTE_JSON + "]", MediaType.APPLICATION_JSON));

        List<ClaimNoteDto> notes = client.findNotes(TENANT, CLAIM);

        assertEquals(1, notes.size());
        ClaimNoteDto note = notes.getFirst();
        assertEquals(NOTE, note.id());
        assertEquals(CLAIM, note.claimId());
        assertEquals("NTE-2026-0000001", note.noteNumber());
        assertEquals("DIARY", note.noteType());
        assertEquals("Dana Handler", note.author());
        assertNotNull(note.createdAt());
        server.verify();
    }

    @Test
    void addNotePostsThePublicPayloadWithHeaders() {
        server.expect(requestTo(NOTE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AssumedClaimClient.BOOK_HEADER, "ASSUMED"))
                .andExpect(header(AssumedClaimClient.TENANT_HEADER, TENANT.toString()))
                .andExpect(content().json("""
                        {
                          "noteType": "DIARY",
                          "body": "Called the claimant to confirm the medical authorization",
                          "author": "Dana Handler"
                        }
                        """, JsonCompareMode.STRICT))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(NOTE_JSON));

        CommandResult<ClaimNoteDto> result = client.addNote(TENANT, CLAIM, new CreateNoteCommand(
                "DIARY", "Called the claimant to confirm the medical authorization",
                "Dana Handler"), null);

        assertEquals(NOTE, result.body().id());
        assertEquals("DIARY", result.body().noteType());
        assertFalse(result.replayed());
        server.verify();
    }

    @Test
    void addNoteForwardsTheIdempotencyKeyAndReadsTheReplayMarker() {
        server.expect(requestTo(NOTE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AssumedClaimClient.IDEMPOTENCY_KEY_HEADER, "note-key-1"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(AssumedClaimClient.IDEMPOTENT_REPLAY_HEADER, "true")
                        .body(NOTE_JSON));

        CommandResult<ClaimNoteDto> result = client.addNote(TENANT, CLAIM, new CreateNoteCommand(
                "DIARY", "Called the claimant to confirm the medical authorization",
                "Dana Handler"), "note-key-1");

        assertEquals(NOTE, result.body().id());
        assertTrue(result.replayed());
        server.verify();
    }

    @Test
    void propagatesAnExpertIdempotencyKeyConflict() {
        server.expect(requestTo(TASK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(AssumedClaimClient.IDEMPOTENCY_KEY_HEADER, "task-key-1"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "https://errors.eia.example/eia-rns-4091",
                                  "title": "Idempotency key reused",
                                  "status": 409,
                                  "detail": "Idempotency-Key was already used with a different request.",
                                  "code": "EIA-RNS-4091",
                                  "category": "CONFLICT",
                                  "errors": [
                                    {
                                      "field": "Idempotency-Key",
                                      "code": "IDEMPOTENCY_KEY_REUSED",
                                      "message": "Idempotency-Key was already used with a different request."
                                    }
                                  ]
                                }
                                """));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.addTask(TENANT, CLAIM, new CreateTaskCommand(
                        "A different task", null, LocalDate.of(2026, 4, 1), "HIGH", null),
                        "task-key-1"));

        assertEquals(409, exception.status().value());
        assertEquals("EIA-RNS-4091", exception.code());
        assertEquals("CONFLICT", exception.category());
        assertEquals("IDEMPOTENCY_KEY_REUSED", exception.errors().getFirst().code());
        server.verify();
    }

    @Test
    void propagatesAnExpertConflictProblemWithStatusAndDetail() {
        server.expect(requestTo(TASK_URL + "/" + TASK))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "https://errors.eia.example/eia-rns-7006",
                                  "title": "Conflict",
                                  "status": 409,
                                  "detail": "Claim task version 2 does not match current version 3",
                                  "code": "EIA-RNS-7006",
                                  "category": "CONFLICT"
                                }
                                """));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.changeTaskStatus(TENANT, CLAIM, TASK,
                        new TaskStatusChangeCommand("COMPLETE", 2L)));

        assertEquals(409, exception.status().value());
        assertEquals("EIA-RNS-7006", exception.code());
        assertEquals("CONFLICT", exception.category());
        assertEquals("Claim task version 2 does not match current version 3", exception.getMessage());
        server.verify();
    }

    @Test
    void propagatesAnExpertBadRequestProblemForNotes() {
        server.expect(requestTo(NOTE_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"title": "Validation failed", "status": 400,
                                 "detail": "noteType is required", "code": "EIA-RNS-7004",
                                 "category": "VALIDATION"}
                                """));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.addNote(TENANT, CLAIM,
                        new CreateNoteCommand("GENERAL", "body", "author"), null));

        assertEquals(400, exception.status().value());
        assertEquals("EIA-RNS-7004", exception.code());
        assertEquals("noteType is required", exception.getMessage());
        server.verify();
    }

    @Test
    void preservesAnExpertNotFoundProblemForTasks() {
        server.expect(requestTo(TASK_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"title": "Assumed claim not found", "status": 404,
                                 "detail": "Assumed claim 0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72 was not found.",
                                 "code": "EIA-RNS-7001", "category": "NOT_FOUND"}
                                """));

        ApiException exception = assertThrows(ApiException.class, () -> client.findTasks(TENANT, CLAIM));

        assertEquals(404, exception.status().value());
        assertEquals("EIA-RNS-7001", exception.code());
        assertTrue(exception.getMessage().contains("was not found"));
        server.verify();
    }

    @Test
    void mapsABareExpert404ToTheDomainNotFoundProblem() {
        server.expect(requestTo(TASK_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ApiException exception = assertThrows(ApiException.class, () -> client.findTasks(TENANT, CLAIM));

        assertEquals(404, exception.status().value());
        assertEquals("EIA-RNS-7001", exception.code());
        server.verify();
    }

    @Test
    void mapsAnExpertServerErrorToRetryableGatewayError() {
        server.expect(requestTo(NOTE_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        ApiException exception = assertThrows(ApiException.class, () -> client.findNotes(TENANT, CLAIM));

        assertEquals(502, exception.status().value());
        assertEquals("EIA-INT-0001", exception.code());
        server.verify();
    }
}
