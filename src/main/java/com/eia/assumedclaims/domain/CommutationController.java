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

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/assumed-claims", "/api/v1/assumed-claims/claims"})
public class CommutationController {
    private final AssumedClaimsWorkbenchService service;

    public CommutationController(AssumedClaimsWorkbenchService service) {
        this.service = service;
    }

    @GetMapping("/{id}/commutation")
    public CommutationDto commutation(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.commutation(tenantId, id);
    }

    @PostMapping("/{id}/commute")
    @ResponseStatus(HttpStatus.CREATED)
    public CommutationDto commute(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody CommuteClaimCommand command) {
        return service.commute(tenantId, id, command);
    }
}
