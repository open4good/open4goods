## Summary

- [ ] Linked issue / context: 
- [ ] Scope owned by `gh` (PR body) vs MCP (comments/labels) confirmed

## Validation

- [ ] `pnpm lint` (frontend)
- [ ] `pnpm test --run` (frontend)
- [ ] `pnpm generate` (frontend)
- [ ] `pnpm test:visual` or visual evidence attached (screenshots/traces) — optional; do not block CI
- [ ] `mvn --offline -pl front-api -am clean install` (if backend touched)
- [ ] `scripts/dev-doctor.sh` reviewed (toolchain and env vars)

## Visual evidence

- Link to Playwright report / screenshots:

## Auth / tokens

- [ ] `GH_TOKEN` configured for gh CLI (repo + workflow + pull_request:write)
- [ ] `MCP_GITHUB_TOKEN` configured for MCP GitHub (comment/labels only)
