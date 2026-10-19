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
- Public API: `GET /api/v1/assumed-claims`, `GET /api/v1/assumed-claims/{id}`,
  `POST /api/v1/assumed-claims`, and `POST /api/v1/assumed-claims/{id}/status`. The SPEC path
  `/api/v1/assumed-claims/claims` is served as an alias.
- Every request requires `X-EIA-Tenant` (UUID) and `X-EIA-Book: ASSUMED`. Missing tenant context
  returns a structured `400`; a missing or different book returns a structured `404`
  (`EIA-RNS-7005`), never `403`.
- The service enforces `report_date >= loss_date`, non-negative reserves, and the
  `AssumedClaimStatus` lifecycle from SPEC §11.5.6, including optimistic version checks.
- The expert is called through a `RestClient` component at `eia.assumed-claims.expert-url`
  (default `http://localhost:8089`), forwarding the ASSUMED book and tenant headers.
- Errors are structured `Problem`-shaped JSON for `400`, `404`, `409`, and `502` cases.

## Local run

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-22.0.2.9-hotspot'
.\gradlew.bat build
java -jar build\libs\eia-assumed-claims-domain-0.1.0-SNAPSHOT.jar
```

Start the expert first; the domain does not persist claims itself.

## Verification

- Lint and format
- Unit tests with 100% business-logic coverage where applicable
- Contract tests
- Integration or Playwright tests where applicable
- Security scan and SBOM
- Build and publish

## Notes

Owns the business logic for this vertical.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
