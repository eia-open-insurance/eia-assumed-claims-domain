# eia-assumed-claims-domain

- GitHub organization: `eia-open-insurance`
- Repository URL: https://github.com/eia-open-insurance/eia-assumed-claims-domain
- Type: Spring Boot service
- Deployable: Yes
- Owner: Assumed Claims
- Runtime: Java 22 and Spring Boot
- Data store: eia_assumed (tx schema)
- Depends on: eia-assumed-claims-expert, eia-contracts, eia-platform/libs

## Purpose

Assumed Claims business rules, transaction catalog, orchestration, approvals, and APIs. Assumed claims: loss advices, reserves, cash calls, recoveries, and commutations.

## Responsibilities

- Keep this repository focused on one deployable component or one platform concern.
- Follow the repository standard in `../CONVENTIONS.md` and the specification in `../../SPEC.md`.
- Publish and consume contracts through `eia-contracts`.
- Maintain README, CODEOWNERS, SECURITY, CHANGELOG, tests, and local verification.
- Contain no business logic in shared libraries or front-end remotes.

## Implemented slice

- Spring Boot 3.4.0 on Java 22, listening on port `8090`.
- Public claim API: `GET /api/v1/assumed-claims`, `GET /api/v1/assumed-claims/search`,
  `GET /api/v1/assumed-claims/{id}`, `POST /api/v1/assumed-claims`,
  `POST /api/v1/assumed-claims/{id}/status`,
  `POST /api/v1/assumed-claims/{id}/close`, `GET /api/v1/assumed-claims/{id}/financials`, and
  `GET /api/v1/assumed-claims/{id}/audit`. The SPEC path `/api/v1/assumed-claims/claims` is served
  as an alias.
- Claim assignment at `PATCH /api/v1/assumed-claims/claims/{claimId}/assignment` with
  `{assignedAdjuster, assignedTeam, expectedVersion}`. The domain forwards the tenant, the ASSUMED
  book, and the optional `X-EIA-Actor` actor to the expert and preserves the expert's `400`, `404`,
  and `409` problems. Claim projections include `assignedAdjuster`, `assignedTeam`, and
  `assignedAt`.
- Public second-slice API (both the short and the `/claims` alias form):
  - `GET|POST /{id}/advices`, `POST /{id}/advices/{adviceId}/accept`, `GET|POST /{id}/bordereaux`;
  - `GET|POST /{id}/reserves`;
  - `GET|POST /{id}/cash-calls`;
  - `GET|POST /{id}/recoveries`;
  - `GET /{id}/commutation`, `POST /{id}/commute`;
  - `GET|POST /{id}/disputes`, `POST /{id}/disputes/{disputeId}/resolve`.
- Public task and note API (also under the `/claims` alias):
  - `GET|POST /{id}/tasks`, `PATCH /{id}/tasks/{taskId}` with `{status, version}`;
  - `GET|POST /{id}/notes`.
  The task and note projections mirror Direct Claims exactly (the expert's `tenantId` is not
  exposed). The domain validates task statuses (`OPEN`, `COMPLETE`, `CANCELLED`), priorities
  (`LOW`, `NORMAL`, `HIGH`, `URGENT`), and note types (`GENERAL`, `DIARY`, `COVERAGE`, `RESERVE`,
  `LITIGATION`, `SETTLEMENT`) before forwarding the command to the expert. The task and note POSTs
  accept an optional `Idempotency-Key` (1-64 characters), forward it to the expert, and preserve
  the expert's original 201 body plus the `Idempotent-Replay` marker; reuse with a different body
  keeps the expert's 409 `EIA-RNS-4091` `IDEMPOTENCY_KEY_REUSED` problem.
- Every request requires `X-EIA-Tenant` (UUID) and `X-EIA-Book: ASSUMED`. Missing tenant context
  returns a structured `400`; a missing or different book returns a structured `404`
  (`EIA-RNS-7005`), never `403`.
- The service enforces `report_date >= loss_date`, non-negative reserves, and the
  `AssumedClaimStatus` lifecycle from SPEC §11.5.6, including optimistic version checks. The
  second-slice business invariants live in the expert, which owns the transactional record; the
  domain propagates the expert's status, stable code, category, and field errors unchanged
  (including `409`, `422`, and `428`).
- `GET /api/v1/assumed-claims/search` matches `assumedClaimNumber`, `cedentClaimNumber`, and
  `contractId` case-insensitively for the caller's tenant and the ASSUMED book. A blank `q`
  returns the latest records by `updatedAt`; `limit` defaults to 10 and is capped at 25. The
  domain owns no tables, so the projection is built from the existing tenant-scoped expert list.
- The expert is called through a `RestClient` component at `eia.assumed-claims.expert-url`
  (default `http://localhost:8089`), forwarding the ASSUMED book and tenant headers.
- Errors are structured `Problem`-shaped JSON for `400`, `404`, `409`, `422`, `428`, and `502` cases.

## Local run

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-22.0.2.9-hotspot'
.\gradlew.bat build
java -jar build\libs\eia-assumed-claims-domain-0.1.0-SNAPSHOT.jar
```

Start the expert first; the domain does not persist claims itself.

## Verification

- Unit and controller tests: `.\gradlew.bat test`.
- End-to-end local check: run the expert on 8089 and the domain on 8090, then exercise
  `advice -> accept -> open -> reserve -> cash call -> recovery -> financials` through
  `/api/v1/assumed-claims`.
- Lint and format
- Security scan and SBOM
- Build and publish

## Notes

Owns the business logic for this vertical.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
