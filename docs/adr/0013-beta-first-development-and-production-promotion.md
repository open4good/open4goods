---
title: "ADR 0013: Beta-first development and production promotion"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [8, 14]
---

# ADR 0013: Beta-first development and production promotion

## Context

Production acceptance in development dependencies prevents progress before the
owner's intended single promotion. The product backup is a legacy JSONL export,
not a provider assertion archive or a complete backup of every application domain.

## Decision

The execution contract is `.o4g/project.yml` and each open WorkOrder declares
`executionPhase` and numeric `priority` (lower first). DEVELOPMENT covers local
implementation and direct beta validation. PRODUCTION and POST_PRODUCTION require
a separate explicit owner order; readiness alone is not authorization. The default
selector excludes both. Dependencies express technical prerequisites within these
phases. A completed beta order is not evidence of production execution.

Agents may deploy, restart, configure and migrate beta, administer its GitHub
Environment and create or rotate beta-only secrets. They may remove exact obsolete
beta indexes after verifying a restorable backup. The supplied product archive,
shared resources and production data are not disposable beta indexes. A credential
rotation affecting production or a new infrastructure expense remains owner-held.
Read-only production inspection is permitted with available access.

Use environment input `PRODUCT_BACKUP_SOURCE_URI` for the owner-supplied location;
host, account and path stay outside Git. Pin one coherent immutable archive set
and checksums before importing. Convert only evidenced source data; unattributed
identity and legacy minima follow ADR-0010/0011. Ambiguous Amazon content stays out
of published projections; fresh independently identified merchant input can replace it.
The owner arbitrates ambiguous concept mappings and source publication rights.

The owner accepts a dated snapshot followed by resumed collections: no promise of
complete intervening changes, historical price events or deletions. Record the
selected snapshot time, enrichment watermark and known gap in readiness evidence.
Beta rehearsal produces a pinned release, dataset and restore recipe. At the later
owner order, recheck capacity, policies, configuration and readiness against those
versions; a changed candidate is rehearsed again. Product migration does not replace
production accounts, billing ledgers, API keys or other transactional stores with beta data.

Legacy code can be removed after beta proves independence, while immutable old
binaries, configuration and data preserve production rollback. Production indexes
and infrastructure remain recoverable for at least seven complete healthy days.
An Elasticsearch alias change does not atomically deploy applications: the runbook
uses maintenance gating and explicit version checks across readers and writers.

## Consequences

The campaign can finish development before production approval. Explicit follow-up
orders retain production rollout, credential work and physical retirement. Limited
beta capacity blocks full-volume acceptance, not implementation on bounded fixtures.
