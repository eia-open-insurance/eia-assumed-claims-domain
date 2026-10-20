# Implementation plan

1. Add the Gradle build, Spring Boot application class, and runtime configuration.
2. Add domain or expert dependencies according to the specification.
3. Add unit, component, contract, and integration tests.
4. Add database migrations in the expert repository only.
5. Document the local Gradle verification step.
6. Add Docker image build and Helm values for the target environment.
7. Publish OpenAPI/AsyncAPI contracts to `eia-contracts`.
8. Add the service to the release manifest.

## Implemented

- Public claim API, lifecycle pre-checks, ASSUMED wall, structured errors.
- Second-slice public orchestration for advices/bordereaux, reserves, cash calls, recoveries,
  commutation, disputes, financials, closure, and audit; expert problem propagation preserves
  status/code/category/field errors.
- Claim tasks and notes: list/create tasks, versioned task status changes, and append-only notes,
  with Direct-identical projections, closed-set validation, and 400/404/409 problem propagation.
- Idempotent create header forwarding: both task and note POSTs accept an optional
  `Idempotency-Key` (1-64 characters), validate its bounds, forward it to the expert, and preserve
  the expert's 201 body plus `Idempotent-Replay`; a 409 `EIA-RNS-4091`
  `IDEMPOTENCY_KEY_REUSED` problem is propagated unchanged.
