package com.eia.assumedclaims.domain;

import java.util.List;
import java.util.UUID;

/**
 * Envelope for the common search endpoint. The response is always scoped to the
 * caller's tenant and the ASSUMED book, so every item uses the
 * {@code ASSUMED_CLAIM} type and links back to the assumed-claims workbench.
 */
public record AssumedClaimSearchResponse(String query, int limit, List<Item> items) {
    public record Item(
            UUID id,
            String type,
            String label,
            String secondary,
            String status,
            String href
    ) {
    }
}
