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

/**
 * Tenant and book scope are mandatory on every route. The book guard is enforced by
 * {@link BookScopeFilter}; the tenant header is bound here.
 */
@RestController
@RequestMapping({"/api/v1/assumed-claims", "/api/v1/assumed-claims/claims"})
public class AssumedClaimController {
    private final AssumedClaimService service;

    public AssumedClaimController(AssumedClaimService service) {
        this.service = service;
    }

    @GetMapping
    public List<AssumedClaimDto> list(@RequestHeader("X-EIA-Tenant") UUID tenantId) {
        return service.list(tenantId);
    }

    @GetMapping("/{id}")
    public AssumedClaimDto get(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.get(tenantId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssumedClaimDto create(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @Valid @RequestBody CreateAssumedClaimCommand command) {
        return service.create(tenantId, command);
    }

    @PostMapping("/{id}/status")
    public AssumedClaimDto changeStatus(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody StatusChangeCommand command) {
        return service.changeStatus(tenantId, id, command);
    }

    @PostMapping("/{id}/close")
    public AssumedClaimDto close(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.close(tenantId, id);
    }

    @GetMapping("/{id}/financials")
    public FinancialSummaryDto financials(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.financials(tenantId, id);
    }

    @GetMapping("/{id}/audit")
    public List<ClaimAuditEntryDto> audit(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.audit(tenantId, id);
    }
}
