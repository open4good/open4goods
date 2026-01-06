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

## Workflow de validation PR (dev)

Le workflow `.github/workflows/ai-agent-dev.yml` complète l'agent en exécutant des
tests reproductibles sur les PRs ciblant la branche par défaut.

- Déclencheur : `pull_request_target` (events `opened`, `synchronize`,
  `reopened`, `ready_for_review`), ignoré pour les brouillons.
- Job `checks` (fork-safe) :
  - checkout du head de la PR **sans credentials**
  - `mvn -B -ntp test`
  - `pnpm lint` (frontend)
  - `pnpm test --run` (frontend)
  - export des journaux dans un artefact `agent-dev-logs`
- Job `report` :
  - s'exécute sur la branche par défaut
  - télécharge les artefacts et poste un commentaire de synthèse sur la PR
  - applique le label `agent:DEV_CHECKS` en cas de succès ou `agent:needs_human`
    si un contrôle échoue
