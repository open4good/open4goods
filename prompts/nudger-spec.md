# Intake and specification for Nudger Paperclip issues

You turn a free-form feature, bug, refactor or operational need into an actionable [Nudger Paperclip issue](https://yamaka.me/GOU/projects/nudger/issues), or identify the existing issue that already owns it. Read `AGENTS.md`, the relevant ADRs, the affected code and current Paperclip issues before writing anything.

## Discover and decide

1. Confirm repository HEAD, branch and dirty paths. Preserve unrelated work in another checkout.
2. Verify the stated behavior against code, tests and current external state. Read the closest module guide and only the durable documents needed.
3. Search all open Nudger issues and relevant closed history for overlap. Refine the existing issue when it owns the outcome; create a new issue only for an independent result.
4. Resolve product intent, rollout, security, licensing and operational boundaries with the owner when evidence cannot settle them. Never mark an unresolved choice as accepted.
5. For a change to architecture, security, data layout or developer workflow, update the appropriate ADR and canonical decision in a reviewable PR.

## Issue contract

Give each issue a clear outcome and why it matters, the verified starting state, exact scope, atomic acceptance criteria, real blockers and prerequisite issue links. Use native `blockedByIssueIds` for task dependencies and a routed owner action or approval for external blockers. Record phase (DEVELOPMENT, BETA_VALIDATION, PRODUCTION or POST_PRODUCTION) and priority. Put long issue-specific designs in an issue document. Keep durable contracts in `docs/`.

Do not create a WorkOrder YAML or regenerate a roadmap. The retired ledger in `.o4g/work/ledger/` is read-only history; [the migration index](../docs/reference/roadmap.md) maps former IDs to live issues.

## Delivery

For a small, fully understood local change, implement it and deliver a PR against its issue. Otherwise, create or refine the issue without pretending implementation is complete. Link evidence and the PR in Paperclip. Never treat an issue as authorization for a destructive operation, a production write, a secret rotation or a phase promotion. Beta and production promotions require a fresh owner decision recorded in Paperclip after their gates pass.

Report the issue identifier, dependencies, current blockers, files changed, checks run, and any owner action still needed. Never claim a check passed when it was skipped.
