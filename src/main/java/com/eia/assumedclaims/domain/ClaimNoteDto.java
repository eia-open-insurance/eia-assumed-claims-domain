package com.eia.assumedclaims.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Public projection of an append-only assumed-claim note. It mirrors the Direct Claims note
 * projection exactly: no {@code tenantId}, no version, and no update or delete route.
 */
public record ClaimNoteDto(
        UUID id,
        UUID claimId,
        String noteNumber,
        String noteType,
        String body,
        String author,
        Instant createdAt
) {}
