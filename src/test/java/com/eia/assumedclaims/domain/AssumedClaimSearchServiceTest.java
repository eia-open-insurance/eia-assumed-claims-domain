package com.eia.assumedclaims.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssumedClaimSearchServiceTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID OTHER_TENANT = UUID.fromString("99999999-9999-7999-8999-999999999999");
    private static final UUID CONTRACT = UUID.fromString("22222222-2222-7222-8222-222222222222");
    private static final Instant BASE = Instant.parse("2026-03-18T09:00:00Z");

    @Mock
    private AssumedClaimClient client;

    @InjectMocks
    private AssumedClaimService service;

    @Test
    void matchesAssumedClaimNumberCaseInsensitively() {
        AssumedClaimDto claim = claim(1, "RNS-2026-0000042", CONTRACT, "CED-778",
                AssumedClaimStatus.NOTIFIED, "ASSUMED", TENANT, BASE);
        when(client.findAll(TENANT)).thenReturn(List.of(claim));

        AssumedClaimSearchResponse result = service.search(TENANT, "rns-2026-0000042", 10);

        assertEquals(1, result.items().size());
        assertEquals(claim.id(), result.items().get(0).id());
    }

    @Test
    void matchesCedentClaimNumber() {
        AssumedClaimDto claim = claim(1, "RNS-2026-0000042", CONTRACT, "CED-778",
                AssumedClaimStatus.NOTIFIED, "ASSUMED", TENANT, BASE);
        when(client.findAll(TENANT)).thenReturn(List.of(claim));

        AssumedClaimSearchResponse result = service.search(TENANT, "ced-77", 10);

        assertEquals(1, result.items().size());
        assertEquals("RNS-2026-0000042", result.items().get(0).label());
    }

    @Test
    void matchesContractId() {
        AssumedClaimDto claim = claim(1, "RNS-2026-0000042", CONTRACT, "CED-778",
                AssumedClaimStatus.NOTIFIED, "ASSUMED", TENANT, BASE);
        when(client.findAll(TENANT)).thenReturn(List.of(claim));

        AssumedClaimSearchResponse result = service.search(TENANT, "22222222-2222-7222", 10);

        assertEquals(1, result.items().size());
    }

    @Test
    void blankQueryReturnsTheLatestTenByUpdatedAt() {
        List<AssumedClaimDto> claims = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            claims.add(claim(i, "RNS-2026-" + String.format("%07d", i), CONTRACT, "CED-" + i,
                    AssumedClaimStatus.NOTIFIED, "ASSUMED", TENANT, BASE.plusSeconds(i)));
        }
        when(client.findAll(TENANT)).thenReturn(claims.reversed());

        AssumedClaimSearchResponse result = service.search(TENANT, "  ", 10);

        assertEquals("", result.query());
        assertEquals(AssumedClaimService.DEFAULT_SEARCH_LIMIT, result.limit());
        assertEquals(10, result.items().size());
        assertEquals(claims.get(11).id(), result.items().get(0).id());
        assertEquals(claims.get(2).id(), result.items().get(9).id());
    }

    @Test
    void honorsRequestedLimit() {
        when(client.findAll(TENANT)).thenReturn(claims(5));

        AssumedClaimSearchResponse result = service.search(TENANT, "", 2);

        assertEquals(2, result.limit());
        assertEquals(2, result.items().size());
    }

    @Test
    void capsLimitAtTwentyFive() {
        when(client.findAll(TENANT)).thenReturn(claims(30));

        AssumedClaimSearchResponse result = service.search(TENANT, "", 100);

        assertEquals(AssumedClaimService.MAX_SEARCH_LIMIT, result.limit());
        assertEquals(25, result.items().size());
    }

    @Test
    void defaultsNonPositiveLimitToTen() {
        when(client.findAll(TENANT)).thenReturn(claims(12));

        AssumedClaimSearchResponse result = service.search(TENANT, "", 0);

        assertEquals(AssumedClaimService.DEFAULT_SEARCH_LIMIT, result.limit());
        assertEquals(10, result.items().size());
    }

    @Test
    void excludesAnotherTenantsClaims() {
        AssumedClaimDto mine = claim(1, "RNS-2026-0000001", CONTRACT, "CED-1",
                AssumedClaimStatus.NOTIFIED, "ASSUMED", TENANT, BASE);
        AssumedClaimDto other = claim(2, "RNS-2026-0000002", CONTRACT, "CED-2",
                AssumedClaimStatus.NOTIFIED, "ASSUMED", OTHER_TENANT, BASE);
        when(client.findAll(TENANT)).thenReturn(List.of(mine, other));

        AssumedClaimSearchResponse result = service.search(TENANT, "", 10);

        assertEquals(1, result.items().size());
        assertEquals(mine.id(), result.items().get(0).id());
    }

    @Test
    void bookWallExcludesRowsOutsideTheAssumedBook() {
        AssumedClaimDto assumed = claim(1, "RNS-2026-0000001", CONTRACT, "CED-1",
                AssumedClaimStatus.NOTIFIED, "ASSUMED", TENANT, BASE);
        AssumedClaimDto direct = claim(2, "RNS-2026-0000002", CONTRACT, "CED-2",
                AssumedClaimStatus.NOTIFIED, "DIRECT", TENANT, BASE);
        when(client.findAll(TENANT)).thenReturn(List.of(assumed, direct));

        AssumedClaimSearchResponse result = service.search(TENANT, "RNS", 10);

        assertEquals(1, result.items().size());
        assertEquals(assumed.id(), result.items().get(0).id());
    }

    @Test
    void projectsTheCommonSearchItemShape() {
        AssumedClaimDto claim = claim(1, "RNS-2026-0000001", CONTRACT, "CED-123",
                AssumedClaimStatus.NOTIFIED, "ASSUMED", TENANT, BASE);
        when(client.findAll(TENANT)).thenReturn(List.of(claim));

        AssumedClaimSearchResponse.Item item = service.search(TENANT, "RNS", 10).items().get(0);

        assertEquals(claim.id(), item.id());
        assertEquals("ASSUMED_CLAIM", item.type());
        assertEquals("RNS-2026-0000001", item.label());
        assertEquals("CED-123 · NOTIFIED", item.secondary());
        assertEquals("NOTIFIED", item.status());
        assertEquals("/assumed-claims?record=" + claim.id(), item.href());
    }

    @Test
    void secondaryFallsBackToTheContractIdWhenCedentReferenceIsMissing() {
        AssumedClaimDto claim = claim(1, "RNS-2026-0000001", CONTRACT, null,
                AssumedClaimStatus.OPEN, "ASSUMED", TENANT, BASE);
        when(client.findAll(TENANT)).thenReturn(List.of(claim));

        AssumedClaimSearchResponse.Item item = service.search(TENANT, "RNS", 10).items().get(0);

        assertEquals(CONTRACT + " · OPEN", item.secondary());
    }

    @Test
    void noMatchReturnsAnEmptyEnvelope() {
        when(client.findAll(TENANT)).thenReturn(claims(3));

        AssumedClaimSearchResponse result = service.search(TENANT, "ZZZ-NO-SUCH-CLAIM", 10);

        assertTrue(result.items().isEmpty());
        assertEquals("ZZZ-NO-SUCH-CLAIM", result.query());
        assertEquals(10, result.limit());
    }

    private static List<AssumedClaimDto> claims(int count) {
        List<AssumedClaimDto> claims = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            claims.add(claim(i, "RNS-2026-" + String.format("%07d", i), CONTRACT, "CED-" + i,
                    AssumedClaimStatus.NOTIFIED, "ASSUMED", TENANT, BASE.plusSeconds(i)));
        }
        return claims;
    }

    private static AssumedClaimDto claim(int sequence, String number, UUID contract, String cedent,
                                         AssumedClaimStatus status, String book, UUID tenant,
                                         Instant updatedAt) {
        return new AssumedClaimDto(
                UUID.fromString(String.format("0192f3a4-5b1c-7e2d-9a3f-%012d", sequence)),
                tenant,
                book,
                number,
                contract,
                cedent,
                status,
                LocalDate.of(2026, 3, 14),
                LocalDate.of(2026, 3, 18),
                new BigDecimal("12500.0000"),
                "USD",
                0,
                updatedAt,
                updatedAt);
    }
}
