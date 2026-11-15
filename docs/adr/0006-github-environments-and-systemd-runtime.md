---
title: "ADR 0006: GitHub Environments and systemd runtime"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [1, 8, 9]
---

# ADR 0006: GitHub Environments and systemd runtime

## Context

The private configuration repository mixes deployable structure, topology, credentials and host
scripts. Its process-name launcher cannot distinguish identical services across environments and
has already stopped the wrong process. The public repository cannot inherit its history.

## Decision

Non-secret defaults and deployment structure live in this repository. Environment-specific
topology is a GitHub Environment variable; credentials and private account lists are GitHub
Environment secrets. Workflows render explicit per-service environment files atomically without
publishing them as artifacts or logs.

Each host runs the five Java applications through `open4goods@.service` and
`open4goods.target`. Units use a dedicated account, `/etc/open4goods/<service>.env`, an immutable
release directory and an atomic `current` symlink. A deploy restarts only its named unit and rolls
back the symlink when readiness fails.

Beta proves configuration parity, health and rollback before production. ADR-0013 reserves
production inputs and execution for a separate owner order; beta independence does not wait for
private repository archival. Production deploys one service at a time with compatible retained
artifacts before the coordinated reader/data switch. Certificates and keystores are generated on-host.

The owner chose to archive `open4goods-config` privately without rewriting it. Therefore every
credential it ever held is invalidated before archive, and none of its history is copied here.

### Amendment (2026-11-10): affiliation keys moved without rotation

The owner accepted on 2026-11-10 a narrow exception to the invalidation rule above for four
affiliation credentials: the Awin datafeed key, the Kwanko API token, the TradeTracker
web-services passphrase and the Effiliation API key. They move from `open4goods-config` to the
GitHub Environment secrets `API_AWIN_DATAFEED_KEY`, `API_KWANKO_TOKEN`,
`API_TRADETRACKER_API_KEY`, `API_EFFILIATION_API_KEY` and `API_METRIKS_EFFILIATION_API_KEY`
with their current values, without regeneration.

These keys only read product feeds, statistics and commissions; none can move money.
Regenerating them would interrupt the four feeds until every consumer is redeployed. The
accepted risk is that the archived history keeps valid values: the Awin datafeed key has been
there since 2021, and external collaborators with read or triage access, including one private
fork, can still read them.

The exception ends, and the affected keys are regenerated through a credential campaign, when
abuse of one of these keys is observed, a value is found published, or a network extends what
its key can do (payments or account settings).

## Consequences

Deployment becomes environment-scoped and auditable. Hot configuration remains adjustable without
a release, while public defaults stay reviewable. Historical personal documents and invalid
credentials remain in a restricted archive and are retained sensitive material.
The four affiliation keys covered by the 2026-11-10 amendment stay valid in that archive until one of its return conditions occurs.
