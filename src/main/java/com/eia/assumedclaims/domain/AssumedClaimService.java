package com.eia.assumedclaims.domain;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Assumed Claims business rules: cross-field invariants and the status lifecycle. The expert owns
 * persistence; this service owns the decision.
 */
@Service
public class AssumedClaimService {
    public static final int DEFAULT_SEARCH_LIMIT = 10;
    public static final int MAX_SEARCH_LIMIT = 25;

    private static final String BOOK = "ASSUMED";
    private static final String SEARCH_TYPE = "ASSUMED_CLAIM";

    private final AssumedClaimClient client;

    public AssumedClaimService(AssumedClaimClient client) {
        this.client = client;
    }

    public List<AssumedClaimDto> list(UUID tenantId) {
        return client.findAll(tenantId);
    }

    /**
     * Common search over the assumed claims the expert exposes for a tenant. The domain owns no
     * tables, so the projection is built from the existing tenant-scoped list and still applies
     * the book wall defensively.
     */
    public AssumedClaimSearchResponse search(UUID tenantId, String query, int limit) {
        String term = query == null ? "" : query.trim();
        int effectiveLimit = normalizeLimit(limit);
        String needle = term.toLowerCase(Locale.ROOT);

        List<AssumedClaimSearchResponse.Item> items = Optional.ofNullable(client.findAll(tenantId))
                .orElseGet(List::of)
                .stream()
                .filter(claim -> tenantId.equals(claim.tenantId()))
                .filter(claim -> BOOK.equals(claim.businessBook()))
                .filter(claim -> needle.isEmpty() || matches(needle,
                        claim.assumedClaimNumber(), claim.cedentClaimNumber(), claim.contractId()))
                .sorted(Comparator.comparing(AssumedClaimDto::updatedAt,
                                Comparator.nullsLast(Comparator.<Instant>reverseOrder()))
                        .thenComparing(AssumedClaimDto::id))
                .limit(effectiveLimit)
                .map(AssumedClaimService::searchItem)
                .toList();

        return new AssumedClaimSearchResponse(term, effectiveLimit, items);
    }

    public AssumedClaimDto get(UUID tenantId, UUID id) {
        return client.findById(tenantId, id);
    }

    public AssumedClaimDto create(UUID tenantId, CreateAssumedClaimCommand command) {
        validateCreate(command);
        return client.create(tenantId, command);
    }

    public AssumedClaimDto changeStatus(UUID tenantId, UUID id, StatusChangeCommand command) {
        AssumedClaimDto current = client.findById(tenantId, id);

        if (current.version() != command.expectedVersion()) {
            throw ApiException.versionConflict(command.expectedVersion());
        }
        if (!current.status().canTransitionTo(command.targetStatus())) {
            throw ApiException.invalidTransition(current.status(), command.targetStatus());
        }
        return client.changeStatus(tenantId, id, command.targetStatus(), command.expectedVersion());
    }

    /**
     * Assignment is an expert-owned command: the expert checks the expected version, writes the
     * assignment audit, and fixes {@code assignedAt}. The domain forwards the actor and preserves
     * the expert's 400/404/409 problem instead of translating it.
     */
    public AssumedClaimDto assign(UUID tenantId, UUID claimId, AssignClaimCommand command, String actor) {
        return client.assign(tenantId, claimId, command, actor);
    }

    public AssumedClaimDto close(UUID tenantId, UUID id) {
        return client.close(tenantId, id);
    }

    public FinancialSummaryDto financials(UUID tenantId, UUID id) {
        return client.financials(tenantId, id);
    }

    public List<ClaimAuditEntryDto> audit(UUID tenantId, UUID id) {
        return client.audit(tenantId, id);
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }
        return Math.min(limit, MAX_SEARCH_LIMIT);
    }

    private static boolean matches(String needle, Object... values) {
        for (Object value : values) {
            if (value != null && value.toString().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static AssumedClaimSearchResponse.Item searchItem(AssumedClaimDto claim) {
        String status = claim.status() == null ? null : claim.status().name();
        return new AssumedClaimSearchResponse.Item(
                claim.id(),
                SEARCH_TYPE,
                claim.assumedClaimNumber(),
                secondary(secondaryReference(claim), status),
                status,
                "/assumed-claims?record=" + claim.id()
        );
    }

    /**
     * The cedent reference is the human-readable counterpart on an assumed claim; fall back to
     * the contract id when the cedent did not supply one.
     */
    private static String secondaryReference(AssumedClaimDto claim) {
        String cedent = claim.cedentClaimNumber();
        if (cedent != null && !cedent.isBlank()) {
            return cedent;
        }
        return claim.contractId() == null ? null : claim.contractId().toString();
    }

    private static String secondary(String first, String status) {
        return (first == null ? "" : first) + " · " + (status == null ? "" : status);
    }

    private static void validateCreate(CreateAssumedClaimCommand command) {
        List<FieldIssue> issues = new ArrayList<>();
        if (command.lossDate() != null && command.reportDate() != null
                && command.reportDate().isBefore(command.lossDate())) {
            issues.add(new FieldIssue(
                    "reportDate",
                    "REPORT_BEFORE_LOSS",
                    "reportDate must be on or after lossDate"
            ));
        }
        if (command.reserveAmount() != null
                && command.reserveAmount().compareTo(BigDecimal.ZERO) < 0) {
            issues.add(new FieldIssue(
                    "reserveAmount",
                    "RESERVE_NEGATIVE",
                    "reserveAmount must be greater than or equal to zero"
            ));
        }
        if (!issues.isEmpty()) {
            throw ApiException.validation("The assumed claim violates a business invariant.", issues);
        }
    }
}
