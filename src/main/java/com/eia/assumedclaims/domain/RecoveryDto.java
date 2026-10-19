package com.eia.assumedclaims.domain;

import com.eia.assumedclaims.domain.AssumedClaimEnums.RecoveryStatus;
import com.eia.assumedclaims.domain.AssumedClaimEnums.RecoveryType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecoveryDto(
        UUID id,
        UUID tenantId,
        UUID assumedClaimId,
        RecoveryType recoveryType,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        String currency,
        LocalDate receivedDate,
        RecoveryStatus status,
        UUID sourcePartyId,
        UUID sourceContractId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal expectedAmount,
        UUID paymentId,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}
