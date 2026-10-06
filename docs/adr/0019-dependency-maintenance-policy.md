---
title: "ADR 0019: Dependency maintenance and release policy"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: []
---

# ADR 0019: Dependency maintenance and release policy

## Context

Renovate maintains dependency versions while retaining human review as the
safety boundary. Green CI is required evidence for a merge, not permission to
merge automatically. No ADR or product API change is required for ordinary
dependency maintenance.

## Decision

### Renovate policy

- Renovate extends `config:best-practices`, which pins Docker image and GitHub
  Action digests, enables configuration migration PRs, protects npm updates with
  a minimum release age, and maintains lock files weekly.
- Ordinary dependency branches and PRs are created only before 06:00 Europe/Paris
  time on Mondays. At most four ordinary branches and PRs may be open at once.
- Non-major updates are grouped into four review domains: frontend npm, backend
  Maven, Python workers, and build/infrastructure (Dockerfiles, Compose, and
  GitHub Actions).
- Frontend lock-file maintenance is a separate, labelled maintenance PR. Its
  frontend npm rule uses `npmInstallTwice` so generated npm lock files remain
  compatible with `npm ci`.
- Renovate creates PRs immediately so the PR-only CI workflow can run. Automerge
  is disabled for all updates.
- Docker and Compose changes remain review-gated until PR CI also builds the
  affected runtime images; they are not candidates for a future automerge policy
  before that prerequisite is met.
- Major updates stay ungrouped, create one PR per skipped major version, and
  require a Dependency Dashboard checkbox before their PR is created.
- Vulnerability fixes are urgent, ungrouped, and manually reviewed. They bypass
  the normal schedule and ordinary PR/branch limits.

### Review and release procedure

1. Review the Dependency Dashboard before the Monday window. Approve a major
   update there only when it is ready for a dedicated review.
2. Review each Renovate PR, its changelog, and all applicable CI results. Merge
   it manually only after the evidence is satisfactory.
3. Treat security PRs independently from the ordinary queue. Review and merge
   them manually as soon as their validation is acceptable.
4. Dependency changes reach `main` only through that manual review. The current
   private-repository settings do not provide enforceable branch protection or
   GitHub auto-merge, so the release workflow's successful-main-CI condition
   remains the deployment safeguard.

### Validation expectations

- Validate `renovate.json` with JSON parsing and Renovate's config validator.
- Confirm the Dependency Dashboard holds majors for approval, security fixes
  appear immediately, and no more than four ordinary grouped branches exist.
- For generated frontend updates, run `npm ci`, typecheck, unit tests, and E2E.
  Retain the existing backend, worker, Compose, corpus, and secret-scan CI gates.
- Confirm Renovate PR metadata reports automerge as disabled.
