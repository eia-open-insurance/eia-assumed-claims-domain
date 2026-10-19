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
public class LossAdviceController {
    private final AssumedClaimsWorkbenchService service;

    public LossAdviceController(AssumedClaimsWorkbenchService service) {
        this.service = service;
    }

    @GetMapping("/{id}/advices")
    public List<LossAdviceDto> advices(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.advices(tenantId, id);
    }

    @PostMapping("/{id}/advices")
    @ResponseStatus(HttpStatus.CREATED)
    public LossAdviceDto recordAdvice(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody RecordAdviceCommand command) {
        return service.recordAdvice(tenantId, id, command);
    }

    @PostMapping("/{id}/advices/{adviceId}/accept")
    public LossAdviceDto acceptAdvice(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id,
            @PathVariable UUID adviceId,
            @RequestBody(required = false) AcceptAdviceCommand command) {
        return service.acceptAdvice(tenantId, id, adviceId, command);
    }

    @GetMapping("/{id}/bordereaux")
    public List<BordereauBatchDto> bordereaux(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id) {
        return service.bordereaux(tenantId, id);
    }

    @PostMapping("/{id}/bordereaux")
    public BordereauIngestResultDto ingestBordereau(
            @RequestHeader("X-EIA-Tenant") UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody IngestBordereauCommand command) {
        return service.ingestBordereau(tenantId, id, command);
    }
}
