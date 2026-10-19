package com.eia.assumedclaims.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssumedClaimController.class)
@Import(AssumedClaimService.class)
class AssumedClaimControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CONTRACT = UUID.fromString("22222222-2222-7222-8222-222222222222");
    private static final UUID CLAIM = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72");
    private static final String ASSUMED = "ASSUMED";
    private static final String DIRECT = "DIRECT";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssumedClaimClient client;

    @Test
    void listRejectsMissingBookWith404() throws Exception {
        mvc.perform(get("/api/v1/assumed-claims")
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"))
                .andExpect(jsonPath("$.category").value("NOT_FOUND"));

        verifyNoInteractions(client);
    }

    @Test
    void listRejectsDirectBookWith404Not403() throws Exception {
        mvc.perform(get("/api/v1/assumed-claims")
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("X-EIA-Book", DIRECT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"));

        verifyNoInteractions(client);
    }

    @Test
    void listRequiresTenantHeader() throws Exception {
        mvc.perform(get("/api/v1/assumed-claims")
                        .header("X-EIA-Book", ASSUMED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-PLT-0201"));

        verifyNoInteractions(client);
    }

    @Test
    void listRejectsMalformedTenant() throws Exception {
        mvc.perform(get("/api/v1/assumed-claims")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-PLT-0201"));
    }

    @Test
    void listReturnsClaimsForAssumedBook() throws Exception {
        given(client.findAll(TENANT)).willReturn(List.of(claim(AssumedClaimStatus.NOTIFIED, 0)));

        mvc.perform(get("/api/v1/assumed-claims")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assumedClaimNumber").value("RNS-2026-0007731"))
                .andExpect(jsonPath("$[0].cedentClaimNumber").value("CED-778"))
                .andExpect(jsonPath("$[0].status").value("NOTIFIED"))
                .andExpect(jsonPath("$[0].reserveAmount").value(12500.0));
    }

    @Test
    void specClaimsPathIsAlsoServed() throws Exception {
        given(client.findAll(TENANT)).willReturn(List.of(claim(AssumedClaimStatus.OPEN, 1)));

        mvc.perform(get("/api/v1/assumed-claims/claims")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void getMapsExpert404ToStructured404() throws Exception {
        given(client.findById(TENANT, CLAIM)).willThrow(ApiException.assumedClaimNotFound(CLAIM));

        mvc.perform(get("/api/v1/assumed-claims/{id}", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7001"))
                .andExpect(jsonPath("$.detail").value("Assumed claim " + CLAIM + " was not found."));
    }

    @Test
    void createReturns201ForValidCommand() throws Exception {
        given(client.create(eq(TENANT), any())).willReturn(claim(AssumedClaimStatus.NOTIFIED, 0));

        mvc.perform(post("/api/v1/assumed-claims")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NOTIFIED"))
                .andExpect(jsonPath("$.assumedClaimNumber").value("RNS-2026-0007731"));

        verify(client).create(eq(TENANT), any());
    }

    @Test
    void createRejectsReportDateBeforeLossDate() throws Exception {
        mvc.perform(post("/api/v1/assumed-claims")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT.toString(),
                                "lossDate", "2026-09-10",
                                "reportDate", "2026-09-01",
                                "reserveAmount", 100.00,
                                "reserveCurrency", "USD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.errors[0].field").value("reportDate"))
                .andExpect(jsonPath("$.errors[0].code").value("REPORT_BEFORE_LOSS"));

        verifyNoInteractions(client);
    }

    @Test
    void createRejectsNegativeReserve() throws Exception {
        mvc.perform(post("/api/v1/assumed-claims")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contractId", CONTRACT.toString(),
                                "reserveAmount", -0.01,
                                "reserveCurrency", "USD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.errors[0].field").value("reserveAmount"));

        verifyNoInteractions(client);
    }

    @Test
    void createRejectsMissingContract() throws Exception {
        mvc.perform(post("/api/v1/assumed-claims")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reserveAmount", 100.00,
                                "reserveCurrency", "USD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.errors[0].field").value("contractId"));
    }

    @Test
    void validTransitionReturnsUpdatedClaim() throws Exception {
        given(client.findById(TENANT, CLAIM)).willReturn(claim(AssumedClaimStatus.OPEN, 2));
        given(client.changeStatus(TENANT, CLAIM, AssumedClaimStatus.RESERVED, 2))
                .willReturn(claim(AssumedClaimStatus.RESERVED, 3));

        mvc.perform(post("/api/v1/assumed-claims/{id}/status", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetStatus", "RESERVED",
                                "expectedVersion", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.version").value(3));

        verify(client).changeStatus(TENANT, CLAIM, AssumedClaimStatus.RESERVED, 2);
    }

    @Test
    void illegalTransitionReturns409AndNeverCallsExpert() throws Exception {
        given(client.findById(TENANT, CLAIM)).willReturn(claim(AssumedClaimStatus.OPEN, 2));

        mvc.perform(post("/api/v1/assumed-claims/{id}/status", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetStatus", "PAID",
                                "expectedVersion", 2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7003"))
                .andExpect(jsonPath("$.category").value("CONFLICT"));

        verify(client, never()).changeStatus(any(), any(), any(), anyInt());
    }

    @Test
    void staleVersionReturns409() throws Exception {
        given(client.findById(TENANT, CLAIM)).willReturn(claim(AssumedClaimStatus.OPEN, 2));

        mvc.perform(post("/api/v1/assumed-claims/{id}/status", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetStatus", "RESERVED",
                                "expectedVersion", 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7006"));

        verify(client, never()).changeStatus(any(), any(), any(), anyInt());
    }

    @Test
    void expertOutageReturns502() throws Exception {
        given(client.findAll(TENANT)).willThrow(ApiException.expertUnavailable());

        mvc.perform(get("/api/v1/assumed-claims")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EIA-INT-0001"))
                .andExpect(jsonPath("$.retryable").value(true));
    }

    private String validCreateBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "contractId", CONTRACT.toString(),
                "cedentClaimNumber", "CED-778",
                "lossDate", "2026-09-01",
                "reportDate", "2026-09-02",
                "reserveAmount", 12500.00,
                "reserveCurrency", "USD"));
    }

    private static AssumedClaimDto claim(AssumedClaimStatus status, int version) {
        Instant now = Instant.parse("2026-10-19T12:00:00Z");
        return new AssumedClaimDto(
                CLAIM,
                TENANT,
                "ASSUMED",
                "RNS-2026-0007731",
                CONTRACT,
                "CED-778",
                status,
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-02"),
                new BigDecimal("12500.0000"),
                "USD",
                version,
                now,
                now
        );
    }
}
