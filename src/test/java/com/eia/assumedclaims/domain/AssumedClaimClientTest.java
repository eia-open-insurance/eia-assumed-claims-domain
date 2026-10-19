package com.eia.assumedclaims.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AssumedClaimClientTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72");

    private MockRestServiceServer server;
    private AssumedClaimClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AssumedClaimClient(builder, new ObjectMapper(), "http://expert.test");
    }

    @Test
    void sendsAssumedBookAndTenantHeadersOnEveryCall() {
        server.expect(requestTo("http://expert.test/internal/v1/assumed-claims/" + CLAIM + "/reserves"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(AssumedClaimClient.BOOK_HEADER, "ASSUMED"))
                .andExpect(header(AssumedClaimClient.TENANT_HEADER, TENANT.toString()))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.findReserves(TENANT, CLAIM);

        server.verify();
    }

    @Test
    void reconstructsExpertApprovalRequiredStatusAndCode() {
        server.expect(requestTo("http://expert.test/internal/v1/assumed-claims/" + CLAIM + "/reserves"))
                .andRespond(withStatus(HttpStatus.PRECONDITION_REQUIRED)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"type":"https://errors.eia.example/eia-rns-7203","title":"Reserve approval required",
                                 "status":428,"detail":"named approver required","instance":"/internal",
                                 "code":"EIA-RNS-7203","category":"APPROVAL_REQUIRED","retryable":false,
                                 "errors":[{"field":"approvedBy","code":"RESERVE_APPROVAL_REQUIRED","message":"required"}]}
                                """));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.changeReserve(TENANT, CLAIM, new ReserveChangeCommand(
                        AssumedClaimEnums.ReserveComponent.CASE, new BigDecimal("2000000.00"), "USD",
                        LocalDate.parse("2026-10-01"), AssumedClaimEnums.ReserveReason.REVIEW, null,
                        AssumedClaimEnums.ReserveAuthority.ADJUSTER, null)));

        assertEquals(428, exception.status().value());
        assertEquals("EIA-RNS-7203", exception.code());
        assertEquals("APPROVAL_REQUIRED", exception.category());
        assertEquals("approvedBy", exception.errors().getFirst().field());
    }

    @Test
    void mapsExpertServerErrorToRetryableGatewayError() {
        server.expect(requestTo("http://expert.test/internal/v1/assumed-claims/" + CLAIM + "/advices"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        ApiException exception = assertThrows(ApiException.class, () -> client.findAdvices(TENANT, CLAIM));

        assertEquals(502, exception.status().value());
        assertEquals("EIA-INT-0001", exception.code());
    }

    @Test
    void mapsExpert404OnClaimDetailToNotFound() {
        server.expect(requestTo("http://expert.test/internal/v1/assumed-claims/" + CLAIM + "?tenantId=" + TENANT))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ApiException exception = assertThrows(ApiException.class, () -> client.findById(TENANT, CLAIM));

        assertEquals(404, exception.status().value());
        assertEquals("EIA-RNS-7001", exception.code());
    }
}
