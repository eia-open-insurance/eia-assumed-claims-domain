package com.eia.assumedclaims.domain;

import com.eia.assumedclaims.domain.AssumedClaimEnums.DisputeStatus;
import com.eia.assumedclaims.domain.AssumedClaimEnums.DisputeType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DisputeDto(
        UUID id,
        UUID tenantId,
        UUID assumedClaimId,
        String disputeNumber,
        DisputeType disputeType,
        String reason,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal contestedAmount,
        String currency,
        DisputeStatus status,
        String resolution,
        LocalDate raisedAt,
        LocalDate resolvedAt,
        boolean arbitrationFlag,
        UUID raisedByPartyId,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}
