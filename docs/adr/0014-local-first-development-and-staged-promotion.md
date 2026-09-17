---
title: "ADR 0014: Local-first development and staged promotion"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [8, 14]
---

# ADR 0014: Local-first development and staged promotion

## Context

Direct beta validation coupled ordinary development to a remote host, scarce storage,
remote credentials and a shared operational state. It also made beta evidence a blocker
for code that can and must be qualified locally from the pinned product backup.

## Decision

WorkOrders use four ordered phases: DEVELOPMENT, BETA_VALIDATION, PRODUCTION and
POST_PRODUCTION. DEVELOPMENT is strictly local. Its applications and infrastructure use
loopback endpoints, ignored local configuration and a read-only backup mount; no runtime,
code generation or test in this phase depends on `nudger.fr`, `beta.nudger.fr` or the beta
host. Public brand and deployment contracts may retain Nudger production domains.

Local infrastructure runs persistently in Docker while Java and Nuxt applications run
natively. The `local` Spring profile is the normal path and never implicitly loads
`devsec`. Scheduling is disabled locally. Live connectors and their external writes remain
available only through explicit, logged operator commands; heavy ingestion never starts
with the stack. Fast loops use a deterministic representative sample, but closing local
readiness requires an empty-index import and full recette against the complete, immutable,
checksum-verified backup. XWiki is transitional and local-only for migration validation;
the final local recette runs without it.

Phase gates are global. BETA_VALIDATION waits until every DEVELOPMENT order, including the
dynamic local campaign readiness order, is complete. PRODUCTION waits until every beta
validation order is complete. The owner's 2026-09-17 order is recorded permanently as
`owner-order-2026-09-17-local-first-promotion`; it authorizes automatic beta and production
promotion when their gates pass without a new confirmation. Candidate SHA, artifacts and
dataset digests do not change between phases. POST_PRODUCTION remains dependency-gated and
cannot physically remove legacy indexes or XWiki until seven complete healthy days and a
verified restoration.

The original backup manifest and bytes are never modified. Pin SHA-256, gzip integrity,
line counts, manifest consistency and XAR integrity only after the owner-supplied copy is
complete. Provider publication rights, mappings and schemas remain real owner blockers;
local infrastructure or beta access does not replace those decisions.

## Consequences

Developers can reproduce and qualify the whole system without beta availability or Nudger
network access. Beta becomes a promotion environment rather than a development dependency.
Local disk and runtime capacity must accommodate the full qualification dataset, and every
external mutation is deliberate and auditable. Historical closed WorkOrders remain unchanged
evidence of the former beta-first process.
