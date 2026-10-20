package com.eia.assumedclaims.domain;

/**
 * Enumerations shared by the public Assumed Claims API. They mirror SPEC §11.5.4; the expert
 * repeats the closed sets so an internal call cannot widen them.
 */
public final class AssumedClaimEnums {
    private AssumedClaimEnums() {}

    public enum AdviceType {
        INITIAL_NOTICE, RESERVE_CHANGE, PAYMENT, RECOVERY, EXPENSE, SALVAGE, SUBROGATION,
        CLOSE, REOPEN, UPDATE, CASH_CALL
    }

    public enum AdviceStatus {
        RECEIVED, VALIDATED, POSTED, REJECTED, SUPERSEDED, DUPLICATE
    }

    public enum AdviceSource {
        MANUAL, BORDEREAU, API
    }

    public enum BordereauStatus {
        PROCESSED, PARTIAL, DUPLICATE, REJECTED
    }

    public enum BordereauLineStatus {
        ACCEPTED, DUPLICATE, REJECTED
    }

    public enum ReserveComponent {
        CASE, IBNR, IBNER, OUTSTANDING, ALAE, ULAE, CASH_LOSS, SALVAGE, SUBROGATION
    }

    public enum ReserveReason {
        INITIAL, ADVICE, DEVELOPMENT, PAYMENT_APPLIED, RECOVERY_APPLIED, REVIEW, COMMUTATION, CLOSE
    }

    public enum ReserveAuthority {
        SYSTEM, ADJUSTER, SENIOR_ADJUSTER, ACTUARIAL, UNDERWRITER
    }

    public enum ApproverRole {
        CLAIMS, UNDERWRITER, ACTUARIAL, SENIOR_ADJUSTER
    }

    public enum CashCallStatus {
        DRAFT, SUBMITTED, APPROVED, PAID, REJECTED, CANCELLED
    }

    public enum CashCallReason {
        LARGE_LOSS, CASH_LOSS, EXPENSE, SECURITY, OTHER
    }

    public enum RecoveryType {
        RETROCESSION, INURING_ASSUMED, FACULTATIVE, SALVAGE, SUBROGATION, OTHER
    }

    public enum RecoveryStatus {
        IDENTIFIED, PURSUING, AGREED, RECEIVED, DENIED, ABANDONED
    }

    public enum CommutationType {
        FULL, PARTIAL, BUYOUT
    }

    public enum CommutationBasis {
        NEGOTIATED, CONTRACTUAL, INDUCED
    }

    public enum CommutationStatus {
        PROPOSED, APPROVED, EXECUTED, REJECTED
    }

    public enum DisputeType {
        COVERAGE, QUANTUM, LATE_NOTICE, ALLOCATION, CESSION, BORDEREAU, SECURITY, OTHER
    }

    public enum DisputeStatus {
        OPEN, UNDER_REVIEW, NEGOTIATING, RESOLVED, ESCALATED, ARBITRATION, CLOSED
    }

    public enum AuditEventType {
        CLAIM_REGISTERED, CLAIM_STATUS_CHANGED, LOSS_ADVICE_RECORDED, LOSS_ADVICE_ACCEPTED,
        BORDEREAU_INGESTED, RESERVE_CHANGED, CASH_CALL_REQUESTED, RECOVERY_RECORDED,
        COMMUTATION_EXECUTED, DISPUTE_RAISED, DISPUTE_RESOLVED, CLAIM_CLOSED, CLAIM_ASSIGNED
    }
}
