package com.eia.assumedclaims.domain;

import com.eia.assumedclaims.domain.AssumedClaimEnums.AdviceSource;
import com.eia.assumedclaims.domain.AssumedClaimEnums.BordereauStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BordereauBatchDto(
        UUID id,
        UUID tenantId,
        UUID assumedClaimId,
        String bordereauReference,
        String cedentClaimNumber,
        LocalDate adviceDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal reportedAmountTotal,
        String currency,
        BordereauStatus status,
        AdviceSource source,
        int version,
        int lineCount,
        int acceptedCount,
        int duplicateCount,
        int rejectedCount,
        String payloadHash,
        Instant receivedAt,
        Instant createdAt,
        Instant updatedAt
) {}
