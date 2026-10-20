package com.eia.assumedclaims.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AssumedClaimAssignmentClientTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72");
    private static final String EXPERT = "http://expert.test";
    private static final String ASSIGNMENT_URL =
            EXPERT + "/internal/v1/assumed-claims/" + CLAIM + "/assignment";
    private static final String ACTOR = "adjuster@example.com";

    private static final String CLAIM_JSON = """
            {
              "id": "0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72",
              "tenantId": "11111111-1111-7111-8111-111111111111",
              "businessBook": "ASSUMED",
              "assumedClaimNumber": "RNS-2026-0007731",
              "contractId": "22222222-2222-7222-8222-222222222222",
              "cedentClaimNumber": "CED-778",
              "status": "OPEN",
              "lossDate": "2026-09-01",
              "reportDate": "2026-09-02",
              "reserveAmount": "12500.0000",
              "reserveCurrency": "USD",
              "assignedAdjuster": "A. Adjuster",
              "assignedTeam": "CASUALTY-TEAM-2",
              "assignedAt": "2026-10-20T14:00:00Z",
              "version": 5,
              "createdAt": "2026-10-19T12:00:00Z",
              "updatedAt": "2026-10-20T14:00:00Z"
            }
            """;

    private MockRestServiceServer server;
    private AssumedClaimClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .messageConverters(converters -> {
                    ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .build();
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(0, new MappingJackson2HttpMessageConverter(mapper));
                });
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AssumedClaimClient(builder, new ObjectMapper(), EXPERT);
    }

    @Test
    void assignPatchesTenantBookActorAndThePublicPayload() {
        server.expect(requestTo(ASSIGNMENT_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(AssumedClaimClient.BOOK_HEADER, "ASSUMED"))
                .andExpect(header(AssumedClaimClient.TENANT_HEADER, TENANT.toString()))
                .andExpect(header(AssumedClaimClient.ACTOR_HEADER, ACTOR))
                .andExpect(content().json("""
                        {
                          "assignedAdjuster": "A. Adjuster",
                          "assignedTeam": "CASUALTY-TEAM-2",
                          "expectedVersion": 4
                        }
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(CLAIM_JSON, MediaType.APPLICATION_JSON));

        AssumedClaimDto claim = client.assign(TENANT, CLAIM,
                new AssignClaimCommand("A. Adjuster", "CASUALTY-TEAM-2", 4), ACTOR);

        assertEquals("A. Adjuster", claim.assignedAdjuster());
        assertEquals("CASUALTY-TEAM-2", claim.assignedTeam());
        assertNotNull(claim.assignedAt());
        assertEquals(5, claim.version());
        server.verify();
    }

    @Test
    void assignOmitsTheActorHeaderWhenTheActorIsAbsent() {
        server.expect(requestTo(ASSIGNMENT_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(AssumedClaimClient.BOOK_HEADER, "ASSUMED"))
                .andExpect(header(AssumedClaimClient.TENANT_HEADER, TENANT.toString()))
                .andExpect(content().json("""
                        {
                          "assignedAdjuster": "A. Adjuster",
                          "assignedTeam": "CASUALTY-TEAM-2",
                          "expectedVersion": 4
                        }
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(CLAIM_JSON, MediaType.APPLICATION_JSON));

        client.assign(TENANT, CLAIM,
                new AssignClaimCommand("A. Adjuster", "CASUALTY-TEAM-2", 4), null);

        server.verify();
    }

    @Test
    void preservesAnExpertAssignmentBadRequest() {
        server.expect(requestTo(ASSIGNMENT_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "https://errors.eia.example/eia-rns-7004",
                                  "title": "Request validation failed",
                                  "status": 400,
                                  "detail": "assignedTeam is required",
                                  "code": "EIA-RNS-7004",
                                  "category": "VALIDATION",
                                  "errors": [
                                    {
                                      "field": "assignedTeam",
                                      "code": "REQUIRED",
                                      "message": "assignedTeam is required"
                                    }
                                  ]
                                }
                                """));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.assign(TENANT, CLAIM,
                        new AssignClaimCommand("A. Adjuster", "CASUALTY-TEAM-2", 4), ACTOR));

        assertEquals(400, exception.status().value());
        assertEquals("EIA-RNS-7004", exception.code());
        assertEquals("VALIDATION", exception.category());
        assertEquals("assignedTeam is required", exception.getMessage());
        assertEquals("assignedTeam", exception.errors().getFirst().field());
        server.verify();
    }

    @Test
    void preservesAnExpertAssignmentNotFound() {
        server.expect(requestTo(ASSIGNMENT_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "https://errors.eia.example/eia-rns-7001",
                                  "title": "Assumed claim not found",
                                  "status": 404,
                                  "detail": "Assumed claim 0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72 was not found.",
                                  "code": "EIA-RNS-7001",
                                  "category": "NOT_FOUND"
                                }
                                """));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.assign(TENANT, CLAIM,
                        new AssignClaimCommand("A. Adjuster", "CASUALTY-TEAM-2", 4), ACTOR));

        assertEquals(404, exception.status().value());
        assertEquals("EIA-RNS-7001", exception.code());
        assertEquals("NOT_FOUND", exception.category());
        server.verify();
    }

    @Test
    void preservesAnExpertAssignmentVersionConflict() {
        server.expect(requestTo(ASSIGNMENT_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "type": "https://errors.eia.example/eia-rns-7006",
                                  "title": "Version conflict",
                                  "status": 409,
                                  "detail": "Assumed claim version 4 does not match current version 5",
                                  "code": "EIA-RNS-7006",
                                  "category": "CONFLICT",
                                  "retryable": true
                                }
                                """));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.assign(TENANT, CLAIM,
                        new AssignClaimCommand("A. Adjuster", "CASUALTY-TEAM-2", 4), ACTOR));

        assertEquals(409, exception.status().value());
        assertEquals("EIA-RNS-7006", exception.code());
        assertEquals("CONFLICT", exception.category());
        assertEquals("Assumed claim version 4 does not match current version 5",
                exception.getMessage());
        server.verify();
    }

    @Test
    void mapsABareAssignmentConflictToTheDomainVersionConflict() {
        server.expect(requestTo(ASSIGNMENT_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.assign(TENANT, CLAIM,
                        new AssignClaimCommand("A. Adjuster", "CASUALTY-TEAM-2", 4), ACTOR));

        assertEquals(409, exception.status().value());
        assertEquals("EIA-RNS-7006", exception.code());
        assertEquals("Assumed claim assignment was modified concurrently; reload and retry.",
                exception.getMessage());
        server.verify();
    }

    @Test
    void mapsABareAssignment404ToTheDomainNotFoundProblem() {
        server.expect(requestTo(ASSIGNMENT_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.assign(TENANT, CLAIM,
                        new AssignClaimCommand("A. Adjuster", "CASUALTY-TEAM-2", 4), ACTOR));

        assertEquals(404, exception.status().value());
        assertEquals("EIA-RNS-7001", exception.code());
        server.verify();
    }

    @Test
    void mapsAnExpertServerErrorToARetryableGatewayError() {
        server.expect(requestTo(ASSIGNMENT_URL))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        ApiException exception = assertThrows(ApiException.class,
                () -> client.assign(TENANT, CLAIM,
                        new AssignClaimCommand("A. Adjuster", "CASUALTY-TEAM-2", 4), ACTOR));

        assertEquals(502, exception.status().value());
        assertEquals("EIA-INT-0001", exception.code());
        server.verify();
    }
}
