package com.eia.assumedclaims.domain;

import com.eia.assumedclaims.domain.AssumedClaimEnums.AdviceSource;
import com.eia.assumedclaims.domain.AssumedClaimEnums.AdviceStatus;
import com.eia.assumedclaims.domain.AssumedClaimEnums.AdviceType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LossAdviceDto(
        UUID id,
        UUID tenantId,
        UUID assumedClaimId,
        String adviceNumber,
        String cedentClaimNumber,
        AdviceType adviceType,
        LocalDate adviceDate,
        LocalDate receivedDate,
        LocalDate periodFrom,
        LocalDate periodTo,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal reportedAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal paidAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal outstandingAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal expenseAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal reserveAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal recoveryAmount,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cessionPct,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cededAmount,
        boolean layerAttachmentMet,
        AdviceStatus status,
        String rejectionReason,
        UUID sourceDocumentId,
        UUID bordereauId,
        Integer bordereauLineNumber,
        AdviceSource source,
        int version,
        Instant postedAt,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
