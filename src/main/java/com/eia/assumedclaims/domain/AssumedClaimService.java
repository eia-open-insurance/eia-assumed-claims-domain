package com.eia.assumedclaims.domain;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Assumed Claims business rules: cross-field invariants and the status lifecycle. The expert owns
 * persistence; this service owns the decision.
 */
@Service
public class AssumedClaimService {
    private final AssumedClaimClient client;

    public AssumedClaimService(AssumedClaimClient client) {
        this.client = client;
    }

    public List<AssumedClaimDto> list(UUID tenantId) {
        return client.findAll(tenantId);
    }

    public AssumedClaimDto get(UUID tenantId, UUID id) {
        return client.findById(tenantId, id);
    }

    public AssumedClaimDto create(UUID tenantId, CreateAssumedClaimCommand command) {
        validateCreate(command);
        return client.create(tenantId, command);
    }

    public AssumedClaimDto changeStatus(UUID tenantId, UUID id, StatusChangeCommand command) {
        AssumedClaimDto current = client.findById(tenantId, id);

        if (current.version() != command.expectedVersion()) {
            throw ApiException.versionConflict(command.expectedVersion());
        }
        if (!current.status().canTransitionTo(command.targetStatus())) {
            throw ApiException.invalidTransition(current.status(), command.targetStatus());
        }
        return client.changeStatus(tenantId, id, command.targetStatus(), command.expectedVersion());
    }

    private static void validateCreate(CreateAssumedClaimCommand command) {
        List<FieldIssue> issues = new ArrayList<>();
        if (command.lossDate() != null && command.reportDate() != null
                && command.reportDate().isBefore(command.lossDate())) {
            issues.add(new FieldIssue(
                    "reportDate",
                    "REPORT_BEFORE_LOSS",
                    "reportDate must be on or after lossDate"
            ));
        }
        if (command.reserveAmount() != null
                && command.reserveAmount().compareTo(BigDecimal.ZERO) < 0) {
            issues.add(new FieldIssue(
                    "reserveAmount",
                    "RESERVE_NEGATIVE",
                    "reserveAmount must be greater than or equal to zero"
            ));
        }
        if (!issues.isEmpty()) {
            throw ApiException.validation("The assumed claim violates a business invariant.", issues);
        }
    }
}
