# open4goods Documentation

Use this directory as the canonical documentation entry point. Keep docs small,
owned, and linked from this index when they become durable project knowledge.

## Sections

- `api/`: public API and endpoint documentation.
- `architecture/`: cross-module architecture notes and long-lived technical designs.
- `adr/`: architecture decision records.
- `business/`: mission, governance, product, and project-management material.
- `conventions/`: documentation and repository conventions.
- `datasources/`: datasource onboarding guides.
- `operations/`: local tooling, MCP servers, deployment, and runbooks.
- `en/` and `fr/`: localized product-facing documentation.
- `templates/`: reusable documentation templates.

## Active References

- [Documentation guidelines](conventions/documentation-guidelines.md)
- [MCP server setup](operations/mcp-servers.md)
- [Frontend asset hardening](operations/frontend-asset-hardening.md)
- [ADR index](adr/README.md)
- [Icecat reference data](architecture/icecat-reference-data.md)
- [Amazon PA-API completion](architecture/amazon-paapi-completion.md)
- [Review generation service](architecture/review-generation-service.md)
- [ETIM integration and cross-referential design](architecture/etim_integration_design.md)
- [ADR 0001: EPREL matching logic scoring](adr/0001-eprel-matching-logic-scoring.md)
- [ADR 0002: Product model identity confidence](adr/0002-product-model-identity-confidence.md)
- [ADR 0003: EPREL dry-run and logging redirection](adr/0003-eprel-dry-run-and-logging-redirection.md)
- [ADR 0004: Aggregation service design](adr/0004-aggregation-service-design.md)
- [ADR 0005: Product Data API B2B v1](adr/0005-product-data-api-b2b-v1.md)

## Product Data API (B2B)

- [Canonical decisions](b2b/00-canonical-decisions.md) - source of truth
- [Master implementation prompt](b2b/b2B.md)
- [Facet catalogue & data coverage](b2b/b2b-facets.md)
- [Competitive study](b2b/b2b-conccurrence.md)
- [Frontend UX specification](b2b/b2b-ui.md)
- [Frontend build & OpenAPI codegen](b2b/b2b-frontend-build.md)
- Architecture: [data model](architecture/product-data-api-data-model.md),
  [redis contract](architecture/product-data-api-redis-contract.md),
  [billing ledger](architecture/product-data-api-billing-ledger.md),
  [API contract](architecture/product-data-api-contract.md),
  [errors](architecture/product-data-api-errors.md),
  [auth](architecture/product-data-api-auth.md),
  [stripe](architecture/product-data-api-stripe-contract.md),
  [ops](architecture/product-data-api-ops.md)
- [Local runbook](operations/product-data-api-local-runbook.md)
