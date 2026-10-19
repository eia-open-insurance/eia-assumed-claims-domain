package com.eia.assumedclaims.domain;

import com.eia.assumedclaims.domain.AssumedClaimEnums.AuditEventType;

import java.time.Instant;
import java.util.UUID;

public record ClaimAuditEntryDto(
        UUID id,
        UUID tenantId,
        UUID assumedClaimId,
        AuditEventType eventType,
        String actor,
        String detail,
        Instant occurredAt,
        long sequence
) {}
