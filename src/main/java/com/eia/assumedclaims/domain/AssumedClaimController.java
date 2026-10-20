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
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/search")
    public AssumedClaimSearchResponse search(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @RequestParam(value = "q", required = false, defaultValue = "") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return service.search(tenantId, query, limit);
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

    /**
     * Assigns the claim to an adjuster and a team at the observed claim version. The tenant is
     * forwarded with the ASSUMED book and the optional {@code X-EIA-Actor} actor; the expert's
     * 400/404/409 problem is preserved unchanged.
     */
    @PatchMapping("/{claimId}/assignment")
    public AssumedClaimDto assign(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @RequestHeader(value = "X-EIA-Actor", required = false) String actor,
            @PathVariable UUID claimId,
            @Valid @RequestBody AssignClaimCommand command) {
        return service.assign(tenantId, claimId, command, actor);
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
