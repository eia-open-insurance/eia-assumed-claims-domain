package com.eia.assumedclaims.domain;

import com.eia.assumedclaims.domain.AssumedClaimEnums.ApproverRole;
import com.eia.assumedclaims.domain.AssumedClaimEnums.CashCallReason;
import com.eia.assumedclaims.domain.AssumedClaimEnums.CashCallStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CashCallDto(
        UUID id,
        UUID tenantId,
        UUID assumedClaimId,
        String requestNumber,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal requestedAmount,
        String currency,
        LocalDate requestedAt,
        LocalDate dueDate,
        CashCallStatus status,
        CashCallReason reason,
        String reasonText,
        UUID approvedBy,
        ApproverRole approverRole,
        Instant approvedAt,
        String approvalStatus,
        UUID paymentId,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}
