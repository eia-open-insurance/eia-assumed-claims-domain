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
public class CashCallController {
    private final AssumedClaimsWorkbenchService service;

    public CashCallController(AssumedClaimsWorkbenchService service) {
        this.service = service;
    }

    @GetMapping("/{id}/cash-calls")
    public List<CashCallDto> cashCalls(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.cashCalls(tenantId, id);
    }

    @PostMapping("/{id}/cash-calls")
    @ResponseStatus(HttpStatus.CREATED)
    public CashCallDto requestCashCall(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody RequestCashCallCommand command) {
        return service.requestCashCall(tenantId, id, command);
    }
}
