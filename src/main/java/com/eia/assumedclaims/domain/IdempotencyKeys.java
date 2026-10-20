package com.eia.assumedclaims.domain;

import java.util.List;

/**
 * Validates and normalizes the optional {@code Idempotency-Key} request header
 * for claim task and note creation.
 *
 * <p>The key is optional for compatibility; when supplied it must be between 1
 * and 64 characters and scopes a create to the tenant that sent it. The expert
 * owns the stored reservation and replay.</p>
 */
final class IdempotencyKeys {
    static final int MIN_LENGTH = 1;
    static final int MAX_LENGTH = 64;

    private IdempotencyKeys() {
    }

    /**
     * @return the trimmed key, or {@code null} when the header is absent
     * @throws ApiException 400 when a supplied key is empty or longer than 64 characters
     */
    static String normalize(String rawKey) {
        if (rawKey == null) {
            return null;
        }
        String key = rawKey.trim();
        if (key.length() < MIN_LENGTH || key.length() > MAX_LENGTH) {
            String message =
                    "Idempotency-Key must be between " + MIN_LENGTH + " and " + MAX_LENGTH
                            + " characters.";
            throw ApiException.validation(message, List.of(
                    new FieldIssue("Idempotency-Key", "INVALID_IDEMPOTENCY_KEY", message)));
        }
        return key;
    }
}
