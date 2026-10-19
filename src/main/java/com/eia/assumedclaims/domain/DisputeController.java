package com.eia.assumedclaims.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/assumed-claims", "/api/v1/assumed-claims/claims"})
public class DisputeController {
    private final AssumedClaimsWorkbenchService service;

    public DisputeController(AssumedClaimsWorkbenchService service) {
        this.service = service;
    }

    @GetMapping("/{id}/disputes")
    public List<DisputeDto> disputes(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.disputes(tenantId, id);
    }

    @PostMapping("/{id}/disputes")
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeDto raiseDispute(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody RaiseDisputeCommand command) {
        return service.raiseDispute(tenantId, id, command);
    }

    @PostMapping("/{id}/disputes/{disputeId}/resolve")
    public DisputeDto resolveDispute(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id,
            @PathVariable UUID disputeId,
            @Valid @RequestBody ResolveDisputeCommand command) {
        return service.resolveDispute(tenantId, id, disputeId, command);
    }
}
