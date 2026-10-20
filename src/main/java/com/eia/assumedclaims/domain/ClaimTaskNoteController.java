package com.eia.assumedclaims.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public assumed-claim task and note API. The SPEC path is served under the
 * {@code /api/v1/assumed-claims/claims} alias (and, like the other second-slice routes, the short
 * form). {@link BookScopeFilter} keeps the ASSUMED book wall; the tenant is bound from
 * {@code X-EIA-Tenant} and forwarded to the expert by {@link AssumedClaimClient}.
 *
 * <p>The task and note POSTs accept an optional {@code Idempotency-Key} (1-64 characters). The
 * domain validates and forwards it; the expert owns the reservation and replay, and the domain
 * preserves the original 201 body plus the {@code Idempotent-Replay} marker.</p>
 */
@RestController
@RequestMapping({"/api/v1/assumed-claims", "/api/v1/assumed-claims/claims"})
public class ClaimTaskNoteController {
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String IDEMPOTENT_REPLAY = "Idempotent-Replay";

    private final ClaimTaskNoteService service;

    public ClaimTaskNoteController(ClaimTaskNoteService service) {
        this.service = service;
    }

    // ------------------------------------------------------------------ tasks
    @GetMapping("/{claimId}/tasks")
    public List<ClaimTaskDto> tasks(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID claimId) {
        return service.tasks(tenantId, claimId);
    }

    @PostMapping("/{claimId}/tasks")
    public ResponseEntity<ClaimTaskDto> addTask(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @PathVariable UUID claimId,
            @Valid @RequestBody CreateTaskCommand command) {
        CommandResult<ClaimTaskDto> result = service.addTask(tenantId, claimId, command,
                idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(IDEMPOTENT_REPLAY, Boolean.toString(result.replayed()))
                .body(result.body());
    }

    @PatchMapping("/{claimId}/tasks/{taskId}")
    public ClaimTaskDto changeTaskStatus(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID claimId,
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskStatusChangeCommand command) {
        return service.changeTaskStatus(tenantId, claimId, taskId, command);
    }

    // ------------------------------------------------------------------ notes
    @GetMapping("/{claimId}/notes")
    public List<ClaimNoteDto> notes(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID claimId) {
        return service.notes(tenantId, claimId);
    }

    @PostMapping("/{claimId}/notes")
    public ResponseEntity<ClaimNoteDto> addNote(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @RequestHeader(value = IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @PathVariable UUID claimId,
            @Valid @RequestBody CreateNoteCommand command) {
        CommandResult<ClaimNoteDto> result = service.addNote(tenantId, claimId, command,
                idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(IDEMPOTENT_REPLAY, Boolean.toString(result.replayed()))
                .body(result.body());
    }
}
