# Changelog

All notable changes to this repository are documented here.

The format is based on Keep a Changelog and the project uses Semantic Versioning.

## [Unreleased]

### Added

- Repository baseline.
- Spring Boot 3.4.0 domain slice: `/api/v1/assumed-claims` API, tenant and ASSUMED book enforcement, date and reserve invariants, the SPEC status lifecycle, a RestClient expert adapter, structured errors, and JUnit controller and lifecycle tests.
- Second slice: public loss advice/bordereau, reserve history, cash call, recovery, commutation, dispute, financial, closure, and audit endpoints; expert problem propagation preserves status/code/category and field errors (including 422 and 428); client header and error-translation tests added.
- Common search at `GET /api/v1/assumed-claims/search?q={term}&limit=10`: tenant- and ASSUMED-book-scoped matching over `assumedClaimNumber`, `cedentClaimNumber`, and `contractId`, latest-by-`updatedAt` results for a blank query, and a limit capped at 25.
