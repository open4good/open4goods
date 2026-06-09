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
