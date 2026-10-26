# Contribution Service

> **Service Guide**: See [/services/AGENTS.md](../AGENTS.md#contribution) for detailed guidance.

**Purpose**: Handles contribution votes statistics and persistence.

**Affiliation clicks**: `AffiliationClick` (index `affiliation-clicks`, no
personal data), `AffiliationNetwork`, `AffiliationClickRepository` (writer,
front-api) and `AffiliationClickStatsService` (aggregated reader, api) form the
contract shared by front-api and api. See
[docs/architecture/affiliation-clicks.md](../../docs/architecture/affiliation-clicks.md).
