package com.eia.assumedclaims.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Command to assign an assumed claim to an adjuster and a team at the claim version the caller
 * observed. The expert owns the optimistic concurrency check and returns a 409 problem for a stale
 * version; the domain forwards the command and preserves that problem.
 */
public record AssignClaimCommand(
        @NotBlank(message = "assignedAdjuster is required")
        String assignedAdjuster,

        @NotBlank(message = "assignedTeam is required")
        String assignedTeam,

        @NotNull(message = "expectedVersion is required")
        @Min(value = 0, message = "expectedVersion must not be negative")
        Integer expectedVersion
) {}
