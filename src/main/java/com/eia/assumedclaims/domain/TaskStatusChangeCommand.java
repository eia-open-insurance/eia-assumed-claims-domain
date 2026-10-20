package com.eia.assumedclaims.domain;

/**
 * Command to move a task to {@code OPEN}, {@code COMPLETE}, or {@code CANCELLED} at the observed
 * task version. A stale version is preserved from the expert as a 409 problem.
 */
public record TaskStatusChangeCommand(
        String status,
        Long version
) {}
