package com.eia.assumedclaims.domain;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssumedClaimController.class)
@Import(AssumedClaimService.class)
class AssumedClaimAssignmentControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CONTRACT = UUID.fromString("22222222-2222-7222-8222-222222222222");
    private static final UUID CLAIM = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72");
    private static final String ASSUMED = "ASSUMED";
    private static final String DIRECT = "DIRECT";
    private static final String ACTOR = "adjuster@example.com";
    private static final String CLAIMS = "/api/v1/assumed-claims/claims/" + CLAIM;
    private static final String SHORT = "/api/v1/assumed-claims/" + CLAIM;
    private static final String BODY = """
            {
              "assignedAdjuster": "A. Adjuster",
              "assignedTeam": "CASUALTY-TEAM-2",
              "expectedVersion": 4
            }
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AssumedClaimClient client;

    @Test
    void assignsTheClaimThroughTheSpecPathAndForwardsTheActor() throws Exception {
        given(client.assign(eq(TENANT), eq(CLAIM), any(AssignClaimCommand.class), eq(ACTOR)))
                .willReturn(assignedClaim());

        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("X-EIA-Actor", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAdjuster").value("A. Adjuster"))
                .andExpect(jsonPath("$.assignedTeam").value("CASUALTY-TEAM-2"))
                .andExpect(jsonPath("$.assignedAt").value("2026-10-20T14:00:00Z"))
                .andExpect(jsonPath("$.version").value(5));

        ArgumentCaptor<AssignClaimCommand> command = ArgumentCaptor.forClass(AssignClaimCommand.class);
        verify(client).assign(eq(TENANT), eq(CLAIM), command.capture(), eq(ACTOR));
        assertEquals(new AssignClaimCommand("A. Adjuster", "CASUALTY-TEAM-2", 4),
                command.getValue());
    }

    @Test
    void assignmentIsAlsoServedUnderTheShortAlias() throws Exception {
        given(client.assign(eq(TENANT), eq(CLAIM), any(AssignClaimCommand.class), isNull()))
                .willReturn(assignedClaim());

        mvc.perform(patch(SHORT + "/assignment")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAdjuster").value("A. Adjuster"));
    }

    @Test
    void assignWithoutAnActorForwardsNoActor() throws Exception {
        given(client.assign(eq(TENANT), eq(CLAIM), any(AssignClaimCommand.class), isNull()))
                .willReturn(assignedClaim());

        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk());

        verify(client).assign(eq(TENANT), eq(CLAIM), any(AssignClaimCommand.class), isNull());
    }

    @Test
    void assignmentRequiresTheTenantHeader() throws Exception {
        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Book", ASSUMED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-PLT-0201"));

        verifyNoInteractions(client);
    }

    @Test
    void assignmentKeepsTheAssumedBookWall() throws Exception {
        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"))
                .andExpect(jsonPath("$.category").value("NOT_FOUND"));

        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Book", DIRECT)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"));

        verifyNoInteractions(client);
    }

    @Test
    void assignmentRejectsMissingFieldsBeforeTheExpertCall() throws Exception {
        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedAdjuster": " ", "assignedTeam": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.category").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[*].field",
                        hasItems("assignedAdjuster", "expectedVersion")));

        verifyNoInteractions(client);
    }

    @Test
    void assignmentRejectsANegativeExpectedVersion() throws Exception {
        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignedAdjuster": "A. Adjuster",
                                 "assignedTeam": "CASUALTY-TEAM-2", "expectedVersion": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.errors[0].field").value("expectedVersion"))
                .andExpect(jsonPath("$.errors[0].code").value("MIN"));

        verifyNoInteractions(client);
    }

    @Test
    void preservesAnExpertAssignmentBadRequest() throws Exception {
        given(client.assign(eq(TENANT), eq(CLAIM), any(AssignClaimCommand.class), eq(ACTOR)))
                .willThrow(ApiException.fromExpert(400, "EIA-RNS-7004", "VALIDATION",
                        "Request validation failed", "assignedTeam is required",
                        List.of(new FieldIssue("assignedTeam", "REQUIRED",
                                "assignedTeam is required"))));

        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("X-EIA-Actor", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"))
                .andExpect(jsonPath("$.detail").value("assignedTeam is required"))
                .andExpect(jsonPath("$.errors[0].field").value("assignedTeam"));
    }

    @Test
    void preservesAnExpertAssignmentNotFound() throws Exception {
        given(client.assign(eq(TENANT), eq(CLAIM), any(AssignClaimCommand.class), eq(ACTOR)))
                .willThrow(ApiException.fromExpert(404, "EIA-RNS-7001", "NOT_FOUND",
                        "Assumed claim not found",
                        "Assumed claim " + CLAIM + " was not found.", List.of()));

        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("X-EIA-Actor", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7001"))
                .andExpect(jsonPath("$.detail").value(
                        "Assumed claim " + CLAIM + " was not found."));
    }

    @Test
    void preservesAnExpertAssignmentVersionConflict() throws Exception {
        given(client.assign(eq(TENANT), eq(CLAIM), any(AssignClaimCommand.class), eq(ACTOR)))
                .willThrow(ApiException.fromExpert(409, "EIA-RNS-7006", "CONFLICT",
                        "Version conflict",
                        "Assumed claim version 4 does not match current version 5", List.of()));

        mvc.perform(patch(CLAIMS + "/assignment")
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .header("X-EIA-Actor", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7006"))
                .andExpect(jsonPath("$.category").value("CONFLICT"))
                .andExpect(jsonPath("$.detail").value(
                        "Assumed claim version 4 does not match current version 5"));
    }

    private static AssumedClaimDto assignedClaim() {
        Instant assignedAt = Instant.parse("2026-10-20T14:00:00Z");
        return new AssumedClaimDto(
                CLAIM,
                TENANT,
                "ASSUMED",
                "RNS-2026-0007731",
                CONTRACT,
                "CED-778",
                AssumedClaimStatus.OPEN,
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-02"),
                new BigDecimal("12500.0000"),
                "USD",
                "A. Adjuster",
                "CASUALTY-TEAM-2",
                assignedAt,
                5,
                Instant.parse("2026-10-19T12:00:00Z"),
                assignedAt
        );
    }
}

