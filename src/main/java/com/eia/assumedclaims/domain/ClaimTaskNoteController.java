package com.eia.assumedclaims.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Public assumed-claim task and note API. The SPEC path is served under the
 * {@code /api/v1/assumed-claims/claims} alias (and, like the other second-slice routes, the short
 * form). {@link BookScopeFilter} keeps the ASSUMED book wall; the tenant is bound from
 * {@code X-EIA-Tenant} and forwarded to the expert by {@link AssumedClaimClient}.
 */
@RestController
@RequestMapping({"/api/v1/assumed-claims", "/api/v1/assumed-claims/claims"})
public class ClaimTaskNoteController {
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
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimTaskDto addTask(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID claimId,
            @Valid @RequestBody CreateTaskCommand command) {
        return service.addTask(tenantId, claimId, command);
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
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimNoteDto addNote(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID claimId,
            @Valid @RequestBody CreateNoteCommand command) {
        return service.addNote(tenantId, claimId, command);
    }
}
