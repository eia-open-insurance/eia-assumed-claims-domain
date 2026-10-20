package com.eia.assumedclaims.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssumedClaimController.class)
@Import(AssumedClaimService.class)
class AssumedClaimSearchControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CONTRACT = UUID.fromString("22222222-2222-7222-8222-222222222222");
    private static final UUID CLAIM_ID = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72");
    private static final String URL = "/api/v1/assumed-claims/search";
    private static final Instant NOW = Instant.parse("2026-03-18T09:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AssumedClaimClient client;

    @Test
    void returnsTheSearchEnvelopeForTheAssumedBook() throws Exception {
        when(client.findAll(TENANT)).thenReturn(List.of(claim("RNS-2026-0000001", "CED-123",
                AssumedClaimStatus.NOTIFIED)));

        mvc.perform(get(URL)
                        .queryParam("q", "RNS")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("RNS"))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.items[0].id").value(CLAIM_ID.toString()))
                .andExpect(jsonPath("$.items[0].type").value("ASSUMED_CLAIM"))
                .andExpect(jsonPath("$.items[0].label").value("RNS-2026-0000001"))
                .andExpect(jsonPath("$.items[0].secondary").value("CED-123 · NOTIFIED"))
                .andExpect(jsonPath("$.items[0].status").value("NOTIFIED"))
                .andExpect(jsonPath("$.items[0].href").value("/assumed-claims?record=" + CLAIM_ID));

        verify(client).findAll(TENANT);
    }

    @Test
    void capsTheRequestedLimitAtTwentyFive() throws Exception {
        when(client.findAll(TENANT)).thenReturn(claims(30));

        mvc.perform(get(URL)
                        .queryParam("q", "RNS")
                        .queryParam("limit", "100")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(25))
                .andExpect(jsonPath("$.items.length()").value(25));
    }

    @Test
    void specClaimsAliasIsAlsoServed() throws Exception {
        when(client.findAll(TENANT)).thenReturn(List.of(claim("RNS-2026-0000001", "CED-123",
                AssumedClaimStatus.OPEN)));

        mvc.perform(get("/api/v1/assumed-claims/claims/search")
                        .queryParam("q", "RNS")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "ASSUMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("OPEN"));
    }

    @Test
    void missingBookReturns404AndNeverReachesTheExpert() throws Exception {
        mvc.perform(get(URL)
                        .queryParam("q", "RNS")
                        .header("X-EIA-Tenant", TENANT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"));

        verifyNoInteractions(client);
    }

    @Test
    void directBookReturns404AndNeverReachesTheExpert() throws Exception {
        mvc.perform(get(URL)
                        .queryParam("q", "RNS")
                        .header("X-EIA-Tenant", TENANT)
                        .header("X-EIA-Book", "DIRECT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"));

        verifyNoInteractions(client);
    }

    @Test
    void missingTenantReturnsStructured400() throws Exception {
        mvc.perform(get(URL)
                        .queryParam("q", "RNS")
                        .header("X-EIA-Book", "ASSUMED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-PLT-0201"));

        verifyNoInteractions(client);
    }

    private static List<AssumedClaimDto> claims(int count) {
        List<AssumedClaimDto> claims = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            claims.add(new AssumedClaimDto(
                    UUID.fromString(String.format("0192f3a4-5b1c-7e2d-9a3f-%012d", i)),
                    TENANT,
                    "ASSUMED",
                    "RNS-2026-" + String.format("%07d", i),
                    CONTRACT,
                    "CED-" + i,
                    AssumedClaimStatus.NOTIFIED,
                    LocalDate.of(2026, 3, 14),
                    LocalDate.of(2026, 3, 18),
                    new BigDecimal("12500.0000"),
                    "USD",
                    null,
                    null,
                    null,
                    0,
                    NOW.plusSeconds(i),
                    NOW.plusSeconds(i)));
        }
        return claims;
    }

    private static AssumedClaimDto claim(String number, String cedent, AssumedClaimStatus status) {
        return new AssumedClaimDto(
                CLAIM_ID,
                TENANT,
                "ASSUMED",
                number,
                CONTRACT,
                cedent,
                status,
                LocalDate.of(2026, 3, 14),
                LocalDate.of(2026, 3, 18),
                new BigDecimal("12500.0000"),
                "USD",
                null,
                null,
                null,
                0,
                NOW,
                NOW);
    }
}
