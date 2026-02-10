# Cody Orchestrator and Multi‑LLM Agent

## Overview

The repository ships two complementary AI automation layers:

- **Multi‑LLM runner** (`scripts/ai/run_agent.mjs`) for state-driven prompts and JSON contract output.
- **Cody orchestrator settings** (`.github/ai/cody.json`) for high-level workflow controls used by GitHub workflows/UI integrations.

Cody remains **stateless**: labels and issue comments are the source of truth.

## Cody defaults (validated decisions)

- `cody:evaluate` is not part of the default workflow.
- Default path is `UNDERSTANDING -> PLANNING -> READY_TO_CODE`.
- Manual resume is mandatory via `cody:retry` (or `cody:evaluate` if kept as compatibility trigger).
- `cody:ask` is a free-form assistant answer (simple comment mode).
- Single continuously-updated assistant thread comment (`comment_thread.marker`).
- Coding phase should open a PR linked to the source issue.

## `cody.json` reference

Main file: `.github/ai/cody.json`.

### Labels

- `labels.enabled`: main switch (`cody:enabled`).
- `labels.triggers.ask`: direct Q/A mode.
- `labels.triggers.clarify`: starts/continues clarification path.
- `labels.triggers.code`: starts coding/PR creation phase.
- `labels.triggers.retry`: explicit manual resume trigger.
- `labels.triggers.evaluate`: optional compatibility trigger.

### Workflow behavior

- `workflow.default_sequence`: ordered states for default orchestration.
- `workflow.auto_transition`: if `true`, transitions proceed automatically until a human gate.
- `workflow.manual_retry_labels`: labels accepted for manual resume.
- `workflow.single_thread_comment`: keep one edited thread comment.

### Provider mapping

- `providers.default`: fallback provider when no explicit provider is set.
- `providers.by_step`: flexible provider mapping by logical step (`ASK`, `UNDERSTANDING`, ...).
- `providers.fallback_order`: provider fallback order.

### PR linking

- `pull_request.link_mode`: `close` or `reference` style behavior.
- `pull_request.closing_keyword`: keyword used for auto-close (`Closes`, `Fixes`, ...).
- `pull_request.link_template`: issue link format used in PR body.
- Other fields (`branch_prefix`, templates...) control branch/commit/title conventions.

### Check mode

- `check_mode.validate_required_secrets`: enables secrets verification logic.
- `check_mode.required_secrets_by_provider`: required secrets mapping.

## Runner check mode (`run_agent.mjs --check`)

Use this mode in CI or locally to validate runtime prerequisites before calling providers.

### What is validated

- AI config can be loaded.
- Optional referenced files exist (`--template`, `--schema`, `--context`).
- Required API secrets exist for selected providers.

### Command examples

Check a single provider:

```bash
node scripts/ai/run_agent.mjs \
  --check \
  --config .github/ai/agent.config.json \
  --provider codex \
  --template .github/ai/prompts/UNDERSTANDING.md \
  --schema .github/ai/schema/agent_contract.schema.json \
  --context ai_context.json \
  --out ai_check.json
```

Check all configured providers:

```bash
node scripts/ai/run_agent.mjs \
  --check \
  --config .github/ai/agent.config.json \
  --out ai_check.json
```

### Exit codes

- `0`: checks passed.
- `1`: missing required secrets.
- `2`: invalid arguments/config/provider.

### Output format

`--out` writes a JSON report with:

- `ok`: global status.
- `checked_at`: UTC timestamp.
- `providers[]`: provider metadata + secret presence.
- `missing_secrets[]`: unique missing secret names.

## Bot identity recommendation

For production usage, prefer a **GitHub App named Cody** over a personal bot account:

- Clear audit trail (`Cody[bot]`).
- Principle of least privilege.
- Easier key rotation and permission scoping.



## Enable the workflow (`ai-agent.yml`)

The executable workflow is `.github/workflows/ai-agent.yml` (not `.old`).

Quick activation checklist:

1. Ensure repository secrets are present: `OPENAI_API_KEY`, `ANTHROPIC_API_KEY`, `GEMINI_API_KEY`.
2. Add labels on the target issue/PR:
   - `agent:on`
   - one provider label (`codex` / `claude` / `gemini`)
   - one state label (`agent:UNDERSTANDING` by default)
3. Trigger with an issue/PR comment containing `/agent` or `@agent`.
4. The workflow now runs a preflight step (`run_agent.mjs --check`) before calling provider APIs.

If preflight fails, fix missing secrets/files first.
