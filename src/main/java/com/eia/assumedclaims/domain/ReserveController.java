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
public class ReserveController {
    private final AssumedClaimsWorkbenchService service;

    public ReserveController(AssumedClaimsWorkbenchService service) {
        this.service = service;
    }

    @GetMapping("/{id}/reserves")
    public List<ReserveHistoryDto> reserves(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.reserves(tenantId, id);
    }

    @PostMapping("/{id}/reserves")
    @ResponseStatus(HttpStatus.CREATED)
    public ReserveHistoryDto changeReserve(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody ReserveChangeCommand command) {
        return service.changeReserve(tenantId, id, command);
    }
}
