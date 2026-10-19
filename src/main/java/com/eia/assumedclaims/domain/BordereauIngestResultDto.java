package com.eia.assumedclaims.domain;

import com.eia.assumedclaims.domain.AssumedClaimEnums.BordereauLineStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BordereauIngestResultDto(
        BordereauBatchDto batch,
        List<LineResult> lines,
        boolean replayed
) {
    public record LineResult(
            int lineNumber,
            String adviceNumber,
            BordereauLineStatus status,
            String reason,
            UUID adviceId,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal reportedAmount
    ) {}
}
