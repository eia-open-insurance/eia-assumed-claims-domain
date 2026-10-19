package com.eia.assumedclaims.domain;

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
        BigDecimal reserveAmount,
        String reserveCurrency,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}
