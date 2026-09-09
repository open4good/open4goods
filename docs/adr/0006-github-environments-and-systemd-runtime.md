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

Beta proves configuration parity, health and rollback before production. Production deploys one
service at a time. Certificates and keystores are generated on their host.

The owner chose to archive `open4goods-config` privately without rewriting it. Therefore every
credential it ever held is invalidated before archive, and none of its history is copied here.

## Consequences

Deployment becomes environment-scoped and auditable. Hot configuration remains adjustable without
a release, while public defaults stay reviewable. Historical personal documents and invalid
credentials remain in a restricted archive and are retained sensitive material.
