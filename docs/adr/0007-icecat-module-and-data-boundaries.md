---
title: "ADR 0007: Icecat module and data boundaries"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [8, 9]
---

# ADR 0007: Icecat module and data boundaries

## Context

Icecat reference documents and a shared download service already exist, but boot still rebuilds
large maps and copies them into Elasticsearch. Moving the whole completion service and controller
into `services/icecat` would introduce an icecat-to-api dependency through repositories,
aggregation and HTTP concerns.

## Decision

`services/icecat` owns bulk XML generation and parsing, versioned reference indexes, the live HTTP
client, tolerant provider DTOs and mapping to a neutral result. `api` owns product repository
access, aggregation orchestration and admin HTTP adapters. The icecat module never depends on api.

Bulk XML types are generated from the owned XSD. The provider live JSON contract remains a
separate Jackson model that ignores unknown fields. Elasticsearch read aliases are the runtime
source; imports build versioned indexes and switch aliases only after validation.

Vertical mappings are durable data with an authenticated typed API. Index rebuild is an explicit
asynchronous command, never a mutating GET or an application-start listener.

## Consequences

Provider transport can evolve without pulling API infrastructure into the service module. Imports
become observable and recoverable, and application startup no longer scales with the complete
Icecat reference corpus.
