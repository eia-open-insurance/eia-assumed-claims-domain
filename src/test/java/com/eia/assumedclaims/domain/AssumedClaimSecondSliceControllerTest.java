package com.eia.assumedclaims.domain;

import com.eia.assumedclaims.domain.AssumedClaimEnums.AdviceSource;
import com.eia.assumedclaims.domain.AssumedClaimEnums.AdviceStatus;
import com.eia.assumedclaims.domain.AssumedClaimEnums.AdviceType;
import com.eia.assumedclaims.domain.AssumedClaimEnums.ApproverRole;
import com.eia.assumedclaims.domain.AssumedClaimEnums.BordereauStatus;
import com.eia.assumedclaims.domain.AssumedClaimEnums.CashCallReason;
import com.eia.assumedclaims.domain.AssumedClaimEnums.CashCallStatus;
import com.eia.assumedclaims.domain.AssumedClaimEnums.CommutationBasis;
import com.eia.assumedclaims.domain.AssumedClaimEnums.CommutationStatus;
import com.eia.assumedclaims.domain.AssumedClaimEnums.CommutationType;
import com.eia.assumedclaims.domain.AssumedClaimEnums.DisputeStatus;
import com.eia.assumedclaims.domain.AssumedClaimEnums.DisputeType;
import com.eia.assumedclaims.domain.AssumedClaimEnums.RecoveryStatus;
import com.eia.assumedclaims.domain.AssumedClaimEnums.RecoveryType;
import com.eia.assumedclaims.domain.AssumedClaimEnums.ReserveAuthority;
import com.eia.assumedclaims.domain.AssumedClaimEnums.ReserveComponent;
import com.eia.assumedclaims.domain.AssumedClaimEnums.ReserveReason;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AssumedClaimController.class,
        LossAdviceController.class,
        ReserveController.class,
        CashCallController.class,
        RecoveryController.class,
        CommutationController.class,
        DisputeController.class
})
@Import({AssumedClaimService.class, AssumedClaimsWorkbenchService.class})
class AssumedClaimSecondSliceControllerTest {
    private static final UUID TENANT = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID CLAIM = UUID.fromString("0192f3a4-5b1c-7e2d-9a3f-6c8d1e0b4a72");
    private static final UUID ADVICE = UUID.fromString("0192f3a4-aaaa-7aaa-8aaa-000000000001");
    private static final UUID APPROVER = UUID.fromString("0192f3a4-bbbb-7bbb-8bbb-000000000002");
    private static final UUID DOCUMENT = UUID.fromString("0192f3a4-cccc-7ccc-8ccc-000000000003");
    private static final String ASSUMED = "ASSUMED";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssumedClaimClient client;

    @Test
    void newPathsObeyTheAssumedBookWall() throws Exception {
        mvc.perform(get("/api/v1/assumed-claims/{id}/advices", CLAIM)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("EIA-RNS-7005"));
        verifyNoInteractions(client);
    }

    @Test
    void newPathsRequireTenantHeader() throws Exception {
        mvc.perform(get("/api/v1/assumed-claims/{id}/advices", CLAIM)
                        .header("X-EIA-Book", ASSUMED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-PLT-0201"));
    }

    @Test
    void listAdvicesUsesClaimPathAndSerializesMoneyAsString() throws Exception {
        given(client.findAdvices(TENANT, CLAIM)).willReturn(List.of(advice(AdviceStatus.RECEIVED)));

        mvc.perform(get("/api/v1/assumed-claims/{id}/advices", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adviceNumber").value("ADV-001"))
                .andExpect(jsonPath("$[0].reportedAmount").value("250000.0000"))
                .andExpect(jsonPath("$[0].reportedAmount").isString());
    }

    @Test
    void listAdvicesIsAlsoServedUnderTheSpecClaimsAlias() throws Exception {
        given(client.findAdvices(TENANT, CLAIM)).willReturn(List.of(advice(AdviceStatus.RECEIVED)));

        mvc.perform(get("/api/v1/assumed-claims/claims/{id}/advices", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RECEIVED"));
    }

    @Test
    void recordAdviceReturns201() throws Exception {
        given(client.recordAdvice(eq(TENANT), eq(CLAIM), any())).willReturn(advice(AdviceStatus.RECEIVED));

        mvc.perform(post("/api/v1/assumed-claims/{id}/advices", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "adviceNumber", "ADV-001",
                                "adviceType", "INITIAL_NOTICE",
                                "adviceDate", "2026-09-03",
                                "reportedAmount", "250000.00",
                                "currency", "USD"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void recordAdviceRejectsMissingNumberBeforeExpertCall() throws Exception {
        mvc.perform(post("/api/v1/assumed-claims/{id}/advices", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "adviceType", "INITIAL_NOTICE",
                                "adviceDate", "2026-09-03",
                                "reportedAmount", "250000.00",
                                "currency", "USD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"));
        verifyNoInteractions(client);
    }

    @Test
    void acceptAdviceReturnsPostedAdvice() throws Exception {
        given(client.acceptAdvice(eq(TENANT), eq(CLAIM), eq(ADVICE), any()))
                .willReturn(advice(AdviceStatus.POSTED));

        mvc.perform(post("/api/v1/assumed-claims/{id}/advices/{adviceId}/accept", CLAIM, ADVICE)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));
    }

    @Test
    void expertDuplicateAdviceCodeIsPropagated() throws Exception {
        given(client.recordAdvice(eq(TENANT), eq(CLAIM), any()))
                .willThrow(ApiException.fromExpert(409, "EIA-RNS-7101", "CONFLICT",
                        "Duplicate loss advice", "already recorded", List.of()));

        mvc.perform(post("/api/v1/assumed-claims/{id}/advices", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "adviceNumber", "ADV-001",
                                "adviceType", "INITIAL_NOTICE",
                                "adviceDate", "2026-09-03",
                                "reportedAmount", "250000.00",
                                "currency", "USD"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7101"));
    }

    @Test
    void ingestBordereauReturnsLineOutcomes() throws Exception {
        given(client.ingestBordereau(eq(TENANT), eq(CLAIM), any())).willReturn(new BordereauIngestResultDto(
                batch(), List.of(new BordereauIngestResultDto.LineResult(1, "B-1",
                        AssumedClaimEnums.BordereauLineStatus.DUPLICATE, "duplicate", null,
                        new BigDecimal("100.0000"))), false));

        mvc.perform(post("/api/v1/assumed-claims/{id}/bordereaux", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bordereauReference", "BDX-1",
                                "adviceDate", "2026-09-03",
                                "currency", "USD",
                                "lines", List.of(Map.of("adviceNumber", "B-1", "reportedAmount", "100.00"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.batch.status").value("PARTIAL"));
    }

    @Test
    void listBordereauxCashCallsRecoveriesAndDisputesAreExposed() throws Exception {
        given(client.findBordereaux(TENANT, CLAIM)).willReturn(List.of());
        given(client.findCashCalls(TENANT, CLAIM)).willReturn(List.of());
        given(client.findRecoveries(TENANT, CLAIM)).willReturn(List.of());
        given(client.findDisputes(TENANT, CLAIM)).willReturn(List.of());

        mvc.perform(get("/api/v1/assumed-claims/{id}/bordereaux", CLAIM)
                        .header("X-EIA-Book", ASSUMED).header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/v1/assumed-claims/{id}/cash-calls", CLAIM)
                        .header("X-EIA-Book", ASSUMED).header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/v1/assumed-claims/{id}/recoveries", CLAIM)
                        .header("X-EIA-Book", ASSUMED).header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/v1/assumed-claims/{id}/disputes", CLAIM)
                        .header("X-EIA-Book", ASSUMED).header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listReservesReturnsHistory() throws Exception {
        given(client.findReserves(TENANT, CLAIM)).willReturn(List.of(reserve()));

        mvc.perform(get("/api/v1/assumed-claims/{id}/reserves", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].component").value("CASE"))
                .andExpect(jsonPath("$[0].amount").value("100000.0000"))
                .andExpect(jsonPath("$[0].isCurrent").value(true));
    }

    @Test
    void reserveApprovalRequiredPropagatesAs428() throws Exception {
        given(client.changeReserve(eq(TENANT), eq(CLAIM), any()))
                .willThrow(ApiException.fromExpert(428, "EIA-RNS-7203", "APPROVAL_REQUIRED",
                        "Reserve approval required", "named approver required", List.of()));

        mvc.perform(post("/api/v1/assumed-claims/{id}/reserves", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "component", "CASE",
                                "amount", "2000000.00",
                                "currency", "USD",
                                "effectiveDate", "2026-10-01",
                                "reasonCode", "REVIEW",
                                "authority", "ADJUSTER"))))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7203"))
                .andExpect(jsonPath("$.category").value("APPROVAL_REQUIRED"));
    }

    @Test
    void reserveRequiresReasonBeforeExpertCall() throws Exception {
        mvc.perform(post("/api/v1/assumed-claims/{id}/reserves", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "component", "CASE",
                                "amount", "100.00",
                                "currency", "USD",
                                "effectiveDate", "2026-10-01",
                                "authority", "ADJUSTER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7004"));
        verifyNoInteractions(client);
    }

    @Test
    void staleReserveConflictPropagatesAs409() throws Exception {
        given(client.changeReserve(eq(TENANT), eq(CLAIM), any()))
                .willThrow(ApiException.fromExpert(409, "EIA-RNS-7202", "CONFLICT",
                        "Stale reserve effective date", "must be later", List.of()));

        mvc.perform(post("/api/v1/assumed-claims/{id}/reserves", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "component", "CASE",
                                "amount", "100.00",
                                "currency", "USD",
                                "effectiveDate", "2026-10-01",
                                "reasonCode", "REVIEW",
                                "authority", "ADJUSTER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7202"));
    }

    @Test
    void cashCallOverReservePropagatesAs422() throws Exception {
        given(client.requestCashCall(eq(TENANT), eq(CLAIM), any()))
                .willThrow(ApiException.fromExpert(422, "EIA-RNS-7301", "BUSINESS_RULE",
                        "Cash call exceeds reserve", "exceeds available reserve", List.of()));

        mvc.perform(post("/api/v1/assumed-claims/{id}/cash-calls", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "requestedAmount", "500000.00",
                                "currency", "USD",
                                "reason", "LARGE_LOSS"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7301"));
    }

    @Test
    void recoveryOverAdvisedLossPropagatesAs422() throws Exception {
        given(client.recordRecovery(eq(TENANT), eq(CLAIM), any()))
                .willThrow(ApiException.fromExpert(422, "EIA-RNS-7401", "BUSINESS_RULE",
                        "Recovery exceeds advised loss", "exceeds cap", List.of()));

        mvc.perform(post("/api/v1/assumed-claims/{id}/recoveries", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recoveryType", "SALVAGE",
                                "amount", "500000.00",
                                "currency", "USD"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7401"));
    }

    @Test
    void commuteReturnsExecutedCommutation() throws Exception {
        given(client.commute(eq(TENANT), eq(CLAIM), any())).willReturn(commutation());

        mvc.perform(post("/api/v1/assumed-claims/{id}/commute", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "offerAmount", "800000.00",
                                "acceptedAmount", "750000.00",
                                "currency", "USD",
                                "effectiveDate", "2026-10-01",
                                "approvedBy", APPROVER.toString(),
                                "approverRole", "UNDERWRITER",
                                "releaseDocumentId", DOCUMENT.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"));
    }

    @Test
    void missingCommutationPropagates404() throws Exception {
        given(client.findCommutation(TENANT, CLAIM))
                .willThrow(ApiException.fromExpert(404, "EIA-RNS-7506", "NOT_FOUND",
                        "Commutation not found", "none recorded", List.of()));

        mvc.perform(get("/api/v1/assumed-claims/{id}/commutation", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EIA-RNS-7506"));
    }

    @Test
    void raiseAndResolveDispute() throws Exception {
        given(client.raiseDispute(eq(TENANT), eq(CLAIM), any())).willReturn(dispute(DisputeStatus.OPEN));
        given(client.resolveDispute(eq(TENANT), eq(CLAIM), any(), any()))
                .willReturn(dispute(DisputeStatus.RESOLVED));

        mvc.perform(post("/api/v1/assumed-claims/{id}/disputes", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "disputeType", "QUANTUM",
                                "reason", "Quantum disputed",
                                "contestedAmount", "100000.00",
                                "currency", "USD"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        mvc.perform(post("/api/v1/assumed-claims/{id}/disputes/{disputeId}/resolve", CLAIM,
                        UUID.fromString("0192f3a4-dddd-7ddd-8ddd-000000000004"))
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "resolution", "Settled by agreement",
                                "status", "RESOLVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void closeFinancialsAndAuditAreExposed() throws Exception {
        AssumedClaimDto claim = new AssumedClaimDto(CLAIM, TENANT, "ASSUMED", "RNS-2026-0007731",
                UUID.fromString("22222222-2222-7222-8222-222222222222"), "CED-778",
                AssumedClaimStatus.CLOSED, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-02"),
                new BigDecimal("0.0000"), "USD", 8, Instant.now(), Instant.now());
        given(client.close(TENANT, CLAIM)).willReturn(claim);
        given(client.financials(TENANT, CLAIM)).willReturn(new FinancialSummaryDto(
                CLAIM, TENANT, "USD", new BigDecimal("100.0000"), new BigDecimal("0.0000"),
                new BigDecimal("0.0000"), new BigDecimal("0.0000"), new BigDecimal("100.0000"),
                new BigDecimal("25.0000"), new BigDecimal("75.0000"), new BigDecimal("0.0000"),
                new BigDecimal("500.0000"), Instant.now(), 8));
        given(client.audit(TENANT, CLAIM)).willReturn(List.of(new ClaimAuditEntryDto(
                UUID.randomUUID(), TENANT, CLAIM, AssumedClaimEnums.AuditEventType.CLAIM_CLOSED,
                "adjuster", "closed", Instant.now(), 4)));

        mvc.perform(post("/api/v1/assumed-claims/{id}/close", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mvc.perform(get("/api/v1/assumed-claims/{id}/financials", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cededIncurredAmount").value("100.0000"))
                .andExpect(jsonPath("$.cededIncurredAmount").isString());

        mvc.perform(get("/api/v1/assumed-claims/{id}/audit", CLAIM)
                        .header("X-EIA-Book", ASSUMED)
                        .header("X-EIA-Tenant", TENANT.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("CLAIM_CLOSED"));
    }

    private static LossAdviceDto advice(AdviceStatus status) {
        Instant now = Instant.parse("2026-10-19T12:00:00Z");
        return new LossAdviceDto(ADVICE, TENANT, CLAIM, "ADV-001", "CED-778", AdviceType.INITIAL_NOTICE,
                LocalDate.parse("2026-09-03"), LocalDate.parse("2026-09-03"), null, null,
                new BigDecimal("250000.0000"), null, null, null, null, null, "USD",
                new BigDecimal("0.500000"), null, false, status, null, null, null, null,
                AdviceSource.MANUAL, 1, status == AdviceStatus.POSTED ? now : null, null, now, now);
    }

    private static ReserveHistoryDto reserve() {
        return new ReserveHistoryDto(UUID.randomUUID(), TENANT, CLAIM, ReserveComponent.CASE, 1,
                new BigDecimal("100000.0000"), "USD", LocalDate.parse("2026-10-01"), ReserveReason.REVIEW,
                "Review", ReserveAuthority.ADJUSTER, false, null, null, true, 1,
                UUID.fromString("00000000-0000-0000-0000-000000000000"), Instant.now());
    }

    private static BordereauBatchDto batch() {
        Instant now = Instant.parse("2026-10-19T12:00:00Z");
        return new BordereauBatchDto(UUID.randomUUID(), TENANT, CLAIM, "BDX-1", "CED-778",
                LocalDate.parse("2026-09-03"), new BigDecimal("100.0000"), "USD", BordereauStatus.PARTIAL,
                AdviceSource.BORDEREAU, 2, 1, 0, 1, 0, "a".repeat(64), now, now, now);
    }

    private static CommutationDto commutation() {
        Instant now = Instant.parse("2026-10-19T12:00:00Z");
        return new CommutationDto(UUID.randomUUID(), TENANT, CLAIM, "COM-2026-000001", CommutationType.FULL,
                new BigDecimal("800000.0000"), new BigDecimal("750000.0000"), "USD",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-01"), CommutationBasis.NEGOTIATED,
                DOCUMENT, CommutationStatus.EXECUTED, APPROVER, ApproverRole.UNDERWRITER, now, now,
                new BigDecimal("50000.0000"), 1, now, now);
    }

    private static DisputeDto dispute(DisputeStatus status) {
        Instant now = Instant.parse("2026-10-19T12:00:00Z");
        return new DisputeDto(UUID.randomUUID(), TENANT, CLAIM, "DSP-2026-000001", DisputeType.QUANTUM,
                "Quantum disputed", new BigDecimal("100000.0000"), "USD", status,
                status == DisputeStatus.RESOLVED ? "Settled" : null, LocalDate.parse("2026-10-01"),
                status == DisputeStatus.RESOLVED ? LocalDate.parse("2026-10-10") : null,
                false, null, 1, now, now);
    }
}
