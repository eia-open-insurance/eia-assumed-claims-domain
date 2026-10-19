package com.eia.assumedclaims.domain;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Public orchestration for the Assumed Claims second slice. The domain keeps request-shape checks
 * (Bean Validation) and delegates every money, authority, and closure invariant to the expert,
 * which owns the transactional record and repeats the assumptions wall.
 */
@Service
public class AssumedClaimsWorkbenchService {
    private final AssumedClaimClient client;

    public AssumedClaimsWorkbenchService(AssumedClaimClient client) {
        this.client = client;
    }

    public List<LossAdviceDto> advices(UUID tenantId, UUID claimId) {
        return client.findAdvices(tenantId, claimId);
    }

    public LossAdviceDto recordAdvice(UUID tenantId, UUID claimId, RecordAdviceCommand command) {
        return client.recordAdvice(tenantId, claimId, command);
    }

    public LossAdviceDto acceptAdvice(UUID tenantId, UUID claimId, UUID adviceId, AcceptAdviceCommand command) {
        return client.acceptAdvice(tenantId, claimId, adviceId, command);
    }

    public List<BordereauBatchDto> bordereaux(UUID tenantId, UUID claimId) {
        return client.findBordereaux(tenantId, claimId);
    }

    public BordereauIngestResultDto ingestBordereau(UUID tenantId, UUID claimId, IngestBordereauCommand command) {
        return client.ingestBordereau(tenantId, claimId, command);
    }

    public List<ReserveHistoryDto> reserves(UUID tenantId, UUID claimId) {
        return client.findReserves(tenantId, claimId);
    }

    public ReserveHistoryDto changeReserve(UUID tenantId, UUID claimId, ReserveChangeCommand command) {
        return client.changeReserve(tenantId, claimId, command);
    }

    public List<CashCallDto> cashCalls(UUID tenantId, UUID claimId) {
        return client.findCashCalls(tenantId, claimId);
    }

    public CashCallDto requestCashCall(UUID tenantId, UUID claimId, RequestCashCallCommand command) {
        return client.requestCashCall(tenantId, claimId, command);
    }

    public List<RecoveryDto> recoveries(UUID tenantId, UUID claimId) {
        return client.findRecoveries(tenantId, claimId);
    }

    public RecoveryDto recordRecovery(UUID tenantId, UUID claimId, RecordRecoveryCommand command) {
        return client.recordRecovery(tenantId, claimId, command);
    }

    public CommutationDto commutation(UUID tenantId, UUID claimId) {
        return client.findCommutation(tenantId, claimId);
    }

    public CommutationDto commute(UUID tenantId, UUID claimId, CommuteClaimCommand command) {
        return client.commute(tenantId, claimId, command);
    }

    public List<DisputeDto> disputes(UUID tenantId, UUID claimId) {
        return client.findDisputes(tenantId, claimId);
    }

    public DisputeDto raiseDispute(UUID tenantId, UUID claimId, RaiseDisputeCommand command) {
        return client.raiseDispute(tenantId, claimId, command);
    }

    public DisputeDto resolveDispute(UUID tenantId, UUID claimId, UUID disputeId, ResolveDisputeCommand command) {
        return client.resolveDispute(tenantId, claimId, disputeId, command);
    }
}
