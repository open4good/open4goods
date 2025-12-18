# GitHub / MCP Hybrid Workflow

This document sets the rules for using both the GitHub CLI (`gh`) and MCP GitHub so we do not double‑edit PRs or leak credentials.

## Token policy

Use **environment variables** only. Do not commit tokens or write them into VS Code settings.

- `GH_TOKEN`: PAT for the GitHub CLI. Scopes: `repo`, `workflow`, and `pull_request:write`. Avoid `admin:org` and `delete_repo`.
- `MCP_GITHUB_TOKEN`: PAT for the MCP GitHub integration. If MCP can edit PR bodies, mirror `GH_TOKEN` scopes; otherwise restrict to `repo:read` + `pull_request:read` and use comments/labels only.

Rotation: generate per‑repository PATs, store them in your password manager, rotate quarterly, and delete old tokens immediately.

## Source of truth: PR bodies

- **Single writer rule**: only **gh CLI** is allowed to edit PR titles/bodies. MCP GitHub must post comments or labels only.
- PR body template lives in `.github/pull_request_template.md` (see below). gh populates it; MCP can append a validation report comment with links to artifacts.

## Checklist & templates

- `.github/pull_request_template.md` provides the canonical checklist (build, tests, lint, visual evidence, doctor output link, auth notes).
- Validation reports: add as PR **comments** (not body edits) with links to Playwright screenshots/traces and CI runs.

## Local configuration

- Export tokens in your shell profile (or a gitignored `.env.local`).
- VS Code tasks/launch configurations read tokens from the environment; they never embed secrets.

### Example

```bash
export GH_TOKEN=ghp_xxx
export MCP_GITHUB_TOKEN=ghp_yyy
```

## Collision avoidance

- Do not run gh commands that rewrite the PR body after MCP has posted a comment; use `gh pr view` before `gh pr edit` to confirm ownership.
- If MCP needs to edit metadata, restrict it to labels/assignees; bodies remain gh-only.

## CI considerations

- CI secrets should use the same scope split: one token for publishing PR bodies (if needed) and a separate, reduced-scope token for comment bots.
- Never reuse local tokens in CI.
