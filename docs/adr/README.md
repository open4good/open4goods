# Architecture Decision Records

Store ADR files as `NNNN-short-title.md`, for example
`0001-documentation-layout.md`.

Each ADR should include:

- Status: proposed, accepted, superseded, or rejected.
- Context: the constraint or problem that made the decision necessary.
- Decision: the selected option.
- Consequences: expected tradeoffs, follow-up work, and operational impact.

Agents should enrich or link an ADR when a change alters architecture,
cross-module contracts, infrastructure, data layout, security posture, or
developer workflow.

## Records

- [ADR 0001: Enhanced EPREL matching logic with score resolution](0001-eprel-matching-logic-scoring.md)
- [ADR 0002: Product model identity confidence](0002-product-model-identity-confidence.md)
- [ADR 0003: EPREL dry-run endpoints and logging redirection](0003-eprel-dry-run-and-logging-redirection.md)
- [ADR 0004: Aggregation service design](0004-aggregation-service-design.md)
- [ADR 0005: Product Data API B2B v1](0005-product-data-api-b2b-v1.md)
- [ADR 0006: GitHub Environments and systemd runtime](0006-github-environments-and-systemd-runtime.md)
- [ADR 0007: Icecat module and data boundaries](0007-icecat-module-and-data-boundaries.md)
- [ADR 0008: Portable project MCP tooling](0008-portable-project-mcp-tooling.md)
- [ADR 0009: XWiki content and identity retirement](0009-xwiki-content-and-identity-retirement.md)
- [ADR 0010: Source-neutral product reference](0010-source-neutral-product-reference.md)
- [ADR 0011: Price observation time series](0011-price-observation-time-series.md)
- [ADR 0012: Consumer product search](0012-consumer-product-search.md)
- [ADR 0013: Beta-first development and production promotion](0013-beta-first-development-and-production-promotion.md) (superseded by ADR 0014)
- [ADR 0014: Local-first development and staged promotion](0014-local-first-development-and-staged-promotion.md)
- [ADR 0015: Paperclip issue governance](0015-paperclip-issue-governance.md)
- [ADR 0016: Secret-scan baseline as a reviewed exception ledger](0016-secret-scan-accepted-baseline.md)
- [ADR 0017: EPREL-sourced facets are zero-rated](0017-eprel-sourced-facets-are-zero-rated.md)
- [ADR 0018: Local promotion readiness gate](0018-promotion-readiness-gate.md)
- [ADR 0019: Dependency maintenance and release policy](0019-dependency-maintenance-policy.md)
- [ADR 0020: Legacy backup import contract](0020-legacy-backup-import-contract.md)
