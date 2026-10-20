package com.eia.assumedclaims.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read model returned by the expert and exposed to the web remote.
 */
public record AssumedClaimDto(
        UUID id,
        UUID tenantId,
        String businessBook,
        String assumedClaimNumber,
        UUID contractId,
        String cedentClaimNumber,
        AssumedClaimStatus status,
        LocalDate lossDate,
        LocalDate reportDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal reserveAmount,
        String reserveCurrency,
        String assignedAdjuster,
        String assignedTeam,
        Instant assignedAt,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}
