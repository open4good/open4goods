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
The former standing order allowed beta and production writes without a fresh decision.
On 2026-09-30 the owner instead approved scoped autonomy through beta, with human
acceptance of a delivered milestone and separate production authorization (GOU-150).

## Decision

Paperclip's Nudger project owns active work. One issue contains the bounded
outcome, scope, acceptance criteria and evidence. Native issue blockers express
open task dependencies; issue documents hold detailed plans. ADRs and durable
technical contracts remain in Git. The former WorkOrder ledger remains
read-only history, and the dated migration index maps legacy IDs to issues.

DEVELOPMENT remains isolated as defined in ADR-0014. Phase readiness is established
from the relevant Paperclip issues. A delivery mandate delegates beta promotion and
corrections within one project and goal, after promotion tooling qualification.
The owner approves sensitive designs at framing; Lead Tech cites those decisions
for conforming PRs and escalates missing decisions or deviations. Every PR receives
Lead Tech approval on the reviewed SHA and green CI; its own PR gets independent review.
Production still needs a fresh owner decision for the exact candidate, dataset,
backups, rollback and phase evidence. The 2026-09-17 standing order remains retired.
Irreversible removal and secret rotation need their distinct approvals; retirement
also retains its seven healthy days and verified restoration gate.

The 2026-09-29 owner decision separates integration from deployment: ordinary PRs
require independent review and green CI, not an owner decision for each merge.
Push and PR workflows never deploy. The legacy production rebuild-and-deploy path
is frozen until the immutable-artifact promotion and rollback path is qualified.
Removing this freeze is part of that reviewed implementation, not a runtime switch.

An objective states the outcome; a delivery milestone issue owns the approved
mandate, scope and final human acceptance. Existing task parents and blockers are
preserved. The milestone has the beta-validation phase, so it does not block beta
entry but remains a production prerequisite. GOU-63 remains technical coordination.
Daily reporting runs belong to the fleet-management project, outside executable phases.

The backlog evolves until QA presents a working beta candidate. At that point Lead
Tech freezes the issue list, criteria and version in the milestone's `acceptance`
document. Blocking defects stay in scope and require renewed candidate evidence;
new features go to the next milestone. A scope reduction needs an explicit decision.
Every executable issue has exactly one phase label. Full inventory reconciliation
includes new, reopened and hidden work; cancelled or unclassified work never passes.
Future work is excluded only with explicit next-milestone membership after the
scope freeze, not merely a different label or an unfinished task omitted from a list.
Existing global phase gates remain conservative until GOU-94 qualifies this distinction.

QA prepares acceptance scenarios at framing. Ops supplies the deployed SHA, artifact
and dataset digests, exact CI/CD runs, beta URL, backup and recovery evidence; QA
attests real beta behavior, not just fixtures or green CI. The milestone enters
`in_review` with `reviewPolicy: human_only` and a confirmation bound to that version.
Only the owner's explicit acceptance permits `done` and objective `achieved`. Agents
must not bypass review through a direct status change. Acceptance never deploys production.

### Live beta mandate check

`scripts/verify/beta_mandate.py` checks the authenticated Paperclip records for an
explicitly configured company, project, milestone, goal, owner, decision comment
and qualification issue. The owner-authored comment is JSON with schema
`nudger-beta-mandate/v1`, those issue/project/goal references, `target: beta`,
`scopePolicy: evolving-until-review`, `sensitiveChanges: approved-design-only`,
`productionAuthorized: false` and `qualificationIssueId`. No local JSON file or
workflow dispatch substitutes for this live decision. An agent-authored or deleted
comment, inactive goal/milestone, hidden record or incomplete qualification fails closed.
Changing the goal away from active or blocking the milestone suspends the mandate.
The check reads twice to detect concurrent changes; it is not an atomic deployment lock.

Invocation: `python3 scripts/verify/beta_mandate.py --company-id <id> --project-id <id>
--milestone-id <id> --goal-id <id> --comment-id <id> --owner-id <id>
--qualification-id <id> --target beta`. Credentials are environment-only:
`PAPERCLIP_API_URL` and `PAPERCLIP_API_KEY`; identity inputs come from reviewed
deployment configuration, not arbitrary PR or dispatch payloads.

A successful check reports `mandateValid: true`, always `authorized: false`, and
whether human review has frozen scope. GOU-94/135/137 integrate it with complete
phase visibility and candidate-specific checks for SHA/digests, sensitive decisions,
CI, backup, rollback and frozen scope, rechecking at the write boundary. Until that
integration is independently reviewed and qualified, promotions remain frozen.
Production never consumes a beta mandate as authorization.

## Consequences

The project board shows assignments, blockers and progress in one place.
Agents handle intermediate integration; the owner receives a daily short report and
exceptions requiring their decision. Lead Tech changes approach after repeated technical
failures instead of automatically requesting a human merge. Track delivery lead time,
human interventions, review waiting time and regressions before changing fleet size.
Git retains technical contracts; prompts, mandates and live task state stay in Paperclip.
