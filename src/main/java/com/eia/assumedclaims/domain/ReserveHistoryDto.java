package com.eia.assumedclaims.domain;

import com.eia.assumedclaims.domain.AssumedClaimEnums.ReserveAuthority;
import com.eia.assumedclaims.domain.AssumedClaimEnums.ReserveComponent;
import com.eia.assumedclaims.domain.AssumedClaimEnums.ReserveReason;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReserveHistoryDto(
        UUID id,
        UUID tenantId,
        UUID assumedClaimId,
        ReserveComponent component,
        int sequence,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        String currency,
        LocalDate effectiveDate,
        ReserveReason reasonCode,
        String reasonText,
        ReserveAuthority authority,
        boolean authorityRequired,
        UUID approvedBy,
        Instant approvedAt,
        boolean isCurrent,
        int version,
        UUID createdBy,
        Instant createdAt
) {}
