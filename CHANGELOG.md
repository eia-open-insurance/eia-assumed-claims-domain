# Changelog

All notable changes to this repository are documented here.

The format is based on Keep a Changelog and the project uses Semantic Versioning.

## [Unreleased]

### Added

- Repository baseline.
- Spring Boot 3.4.0 domain slice: `/api/v1/assumed-claims` API, tenant and ASSUMED book enforcement, date and reserve invariants, the SPEC status lifecycle, a RestClient expert adapter, structured errors, and JUnit controller and lifecycle tests.
- Second slice: public loss advice/bordereau, reserve history, cash call, recovery, commutation, dispute, financial, closure, and audit endpoints; expert problem propagation preserves status/code/category and field errors (including 422 and 428); client header and error-translation tests added.
- Common search at `GET /api/v1/assumed-claims/search?q={term}&limit=10`: tenant- and ASSUMED-book-scoped matching over `assumedClaimNumber`, `cedentClaimNumber`, and `contractId`, latest-by-`updatedAt` results for a blank query, and a limit capped at 25.
- Claim task and note API: `GET|POST /api/v1/assumed-claims/claims/{claimId}/tasks`, `PATCH .../tasks/{taskId}` with `{status, version}`, and `GET|POST .../notes`; Direct-identical task/note projections, closed-set validation for statuses, priorities, and note types, ASSUMED book/tenant header forwarding, and expert problem propagation for 400/404/409.
- Idempotent task and note creation: both POSTs accept an optional `Idempotency-Key` (1-64 characters), validate its bounds before the expert call, forward it on the internal POST, and preserve the original 201 body with the expert's `Idempotent-Replay` response header. Reuse with a different body propagates the expert's 409 `EIA-RNS-4091` `IDEMPOTENCY_KEY_REUSED` problem unchanged.
