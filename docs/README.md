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
- `operations/`: local tooling, MCP servers, deployment, and runbooks. See the
  [dependency maintenance and deployment runbook](operations/dependency-maintenance.md)
  for Renovate review and release policy.
- `en/` and `fr/`: localized product-facing documentation.
- `templates/`: reusable documentation templates.

## Active References

- [Documentation guidelines](conventions/documentation-guidelines.md)
- [MCP server setup](operations/mcp-servers.md)
- [Google SSO OAuth clients](operations/google-sso-oauth-clients.md)
- [Service secrets, beta and prod (render + restart)](operations/prod-service-secrets.md)
- [Infrastructure secrets, beta and prod (wave 3 runbook)](operations/infra-secrets-rotation.md)
- [Frontend asset hardening](operations/frontend-asset-hardening.md)
- [Production log triage](operations/production-log-triage.md)
- [Products index mapping at startup, api only (GOU-1835)](operations/product-index-mapping.md)
- [Recreated products and lost verticals, count and repair (GOU-1700)](operations/product-recreation-recovery.md)
- [Beta deployment workflows (auto deploy on merge, BETA_DEPLOY_FREEZE)](operations/ci-workflows-status.md)
- [ADR index](adr/README.md)
- [Icecat reference data](architecture/icecat-reference-data.md)
- [Amazon PA-API completion](architecture/amazon-paapi-completion.md)
- [ETIM integration and cross-referential design](architecture/etim_integration_design.md)
- [Outbound affiliation clicks](architecture/affiliation-clicks.md)
- [Ranking and listing disclosure, art. D111-6 C. conso (GOU-1995)](business/ranking-and-listing-disclosure.md)
- [External GTIN/ISBN datasources: integration plans](datasources/integration/README.md)
- [ImpactScore V2 design (versioned methodology, coverage, switch, simulation)](impactscore/v2-design.md)
- [ADR 0001: EPREL matching logic scoring](adr/0001-eprel-matching-logic-scoring.md)
- [ADR 0002: Product model identity confidence](adr/0002-product-model-identity-confidence.md)
- [ADR 0003: EPREL dry-run and logging redirection](adr/0003-eprel-dry-run-and-logging-redirection.md)
- [ADR 0004: Aggregation service design](adr/0004-aggregation-service-design.md)
- [ADR 0005: Product Data API B2B v1 (withdrawn)](adr/0005-product-data-api-b2b-v1.md)
