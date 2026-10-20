package com.eia.assumedclaims.domain;

/**
 * Result of a create command proxied to the expert: the public body plus the
 * expert's idempotent replay marker. A replay keeps the original 201 status and
 * body; only the {@code Idempotent-Replay} response header changes.
 */
public record CommandResult<T>(T body, boolean replayed) {
}
