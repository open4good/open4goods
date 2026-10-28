# Contribution Service

> **Service Guide**: See [/services/AGENTS.md](../AGENTS.md#contribution) for detailed guidance.

**Purpose**: Handles contribution votes statistics and persistence.

**Affiliation clicks**: `AffiliationClick` (index `affiliation-clicks`, no
personal data), `AffiliationNetwork`, `AffiliationClickRepository` (writer,
front-api) and `AffiliationClickStatsService` (aggregated reader, api) form the
contract shared by front-api and api. See
[docs/architecture/affiliation-clicks.md](../../docs/architecture/affiliation-clicks.md).

**ip/ua purge**: `ContributionVoteIpUaPurgeService` removes the historical
`ip`/`ua` keys from `contribution-votes` (one-shot, idempotent, run by
front-api at startup, GOU-1230); covered by `ContributionVoteIpUaPurgeServiceIT`
(Testcontainers, not run by the CI reactor).
