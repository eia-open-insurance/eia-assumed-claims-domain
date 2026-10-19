package com.eia.assumedclaims.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StatusChangeCommand(
        @NotNull(message = "targetStatus is required")
        AssumedClaimStatus targetStatus,

        @NotNull(message = "expectedVersion is required")
        @Min(value = 0, message = "expectedVersion must not be negative")
        Integer expectedVersion
) {}
