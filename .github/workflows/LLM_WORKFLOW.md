# Multi‑LLM GitHub Agent (labels + comments)

Ce kit fournit un workflow GitHub Actions qui route une Issue/PR vers un provider LLM (codex/claude/gemini)
en fonction des labels et d'une machine à états.

## Principe
- Vous ajoutez les labels:
  - `agent:on`
  - un provider: `codex` | `claude` | `gemini`
  - un state: `agent:UNDERSTANDING` (défaut), `agent:PLANNING`, `agent:READY_TO_CODE`
- Vous déclenchez l'agent via un commentaire contenant `/agent` ou `@agent`
- L'agent répond dans un commentaire unique mis à jour (thread marker)
- Il met à jour les labels selon le contrat JSON

## Secrets à configurer
Dans Settings → Secrets and variables → Actions:
- `OPENAI_API_KEY`
- `ANTHROPIC_API_KEY`
- `GEMINI_API_KEY`

## Fichiers
- `.github/workflows/ai-agent.yml` : workflow
- `.github/ai/agent.config.json` : config (models, tokens, labels, triggers)
- `.github/ai/prompts/*.md` : templates par état
- `.github/ai/schema/agent_contract.schema.json` : contrat JSON
- `scripts/ai/run_agent.mjs` : runner multi-provider

## Notes sécurité (public + forks)
- Aucun checkout du code PR head: le workflow ne lit que les templates de la branche par défaut.
- Le LLM ne reçoit jamais le `GITHUB_TOKEN`.
- Les labels appliqués sont filtrés via allowlist (`agent:` / `cap:` + provider exact).

## TODO (phase 2)
- Ajouter un workflow séparé "dev" qui checkout le code PR (sans secrets) et exécute:
  - `mvn test`
  - `pnpm lint`
  - `pnpm test`
Puis republie les résultats via un job `pull_request_target` qui ne checkout que la branche par défaut.
