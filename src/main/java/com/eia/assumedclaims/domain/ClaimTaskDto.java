package com.eia.assumedclaims.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Public projection of an assumed-claim diary task. It mirrors the Direct Claims task projection
 * exactly: the expert's {@code tenantId} is deliberately omitted because the tenant is implied by
 * the request context.
 */
public record ClaimTaskDto(
        UUID id,
        UUID claimId,
        String taskNumber,
        String title,
        String description,
        LocalDate dueDate,
        String priority,
        String status,
        String assignedTo,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {}
