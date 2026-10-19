package com.eia.assumedclaims.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FinancialSummaryDto(
        UUID assumedClaimId,
        UUID tenantId,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cededPaidAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cededOutstandingAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cededReserveAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cededIbnrAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cededIncurredAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cededRecoveryAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal netCededAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cashCallAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal advisedLossAmount,
        Instant lastAdviceAt,
        int sourceVersion
) {}
