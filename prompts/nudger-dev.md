# Autonomous development of Nudger Paperclip issues

You implement one bounded issue from the [Nudger board](https://yamaka.me/GOU/projects/nudger/issues) at a time. Read the repository `AGENTS.md`, the closest module guide, the assigned issue, its linked document, native blockers and named ADRs. The issue owns task state, scope, acceptance criteria and evidence.

## Selection and safety

- Select an assigned, unblocked DEVELOPMENT issue. Resume an issue already in progress before starting another. Do not infer readiness from priority alone.
- Use a separate Git worktree. Record branch, HEAD and `git status --short` before each lot; preserve another session's changes. Never reset, clean, stash or broadly stage them.
- Use Paperclip checkout to claim the issue atomically. Keep work inside its stated scope, or update the issue with a justified scope change.
- DEVELOPMENT is strictly local. No Nudger or beta runtime is a development dependency. Use the local stack and pinned backup contract in ADR-0014.
- A task is not permission to mutate beta or production. Beta and production promotion each require an explicit owner decision recorded in Paperclip after their phase checks pass, per ADR-0015. Physical retirement also requires the healthy-window and verified-restore gate.
- Never expose secrets, credentials, private topology or authenticated URLs in code, logs, comments or issue evidence.

## Implementation loop

1. Verify the issue premise against code and current state. Record a failing test, inspection or measurable baseline for every acceptance criterion.
2. Implement the smallest cohesive change, including tests, Javadoc, configuration metadata and durable documentation when behavior changes.
3. Run focused checks. Record concise, sanitized observations and artifact references in the issue. A skipped external check remains an explicit blocker.
4. Inspect the diff for unrelated files, TODOs, generated noise and secrets.
5. Before handoff, run `./scripts/lint.sh` and `mvn --offline clean install`; for frontend work run its applicable lint, tests and build. Report the exact checks actually run.

Use the repository's MCP routing: JavaLens for Java navigation, Maven tools for dependency questions, Vuetify MCP before component API changes, read-only Docker MCP for stack state, and Playwright for local browser behavior. Run `scripts/mcp/doctor.sh --core` when a required server is absent. A narrow `rg` fallback follows only after the doctor failure is recorded.

## Delivery

Push an issue-specific branch and open a PR. Link the PR, candidate commit and test evidence in the Paperclip issue. Move it to review; close only when the criteria are evidenced and the review is complete. Do not push implementation directly to `main` or promote a release from an issue's status alone. If blocked, set the issue to blocked with a concrete native blocker, owner action or approval path, then continue independent work.

Do not silently change a decision. Put enduring architecture and operations contracts in Git and record the related issue/ADR link.
