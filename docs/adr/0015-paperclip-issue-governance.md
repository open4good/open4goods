---
title: "ADR 0015: Paperclip issue governance"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [3, 8, 14]
---

# ADR 0015: Paperclip issue governance

## Context

The repository kept active tasks in WorkOrder YAML, with a generated roadmap and
local lifecycle tooling. Nudger now has a Paperclip project and a development
team. Keeping tasks in both systems would give status and blockers two owners.
The standing promotion order also allowed beta and production writes without a
fresh decision at the time of execution.

## Decision

Paperclip's Nudger project owns active work. One issue contains the bounded
outcome, scope, acceptance criteria and evidence. Native issue blockers express
open task dependencies; issue documents hold detailed plans. ADRs and durable
technical contracts remain in Git. The former WorkOrder ledger remains
read-only history, and the dated migration index maps legacy IDs to issues.

DEVELOPMENT remains local. Phase readiness is established from the relevant
Paperclip issues. Before each beta or production promotion, the owner records
an explicit decision in Paperclip after reviewing the candidate SHA, data
digests, backups, rollback and phase evidence. The 2026-09-17 standing order
no longer authorizes promotion. POST_PRODUCTION physical retirement retains
its seven healthy days and verified restoration gate.

## Consequences

The project board shows assignments, blockers and progress in one place.
Agents update the issue they are working on and link PRs and test evidence
there. Git no longer carries live task state or a generated task roadmap.
