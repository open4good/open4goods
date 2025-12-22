# Codex Agent (labels + comments)

## Fichiers
- `.github/workflows/codex-agent.yml`
- `.github/codex/agent.config.json`
- `.github/codex/prompts/*.md`

## Prérequis
- Secret GitHub: `OPENAI_API_KEY`
- Labels recommandés:
  - `agent.created`
  - `agent.interacting` (optionnel: `agent.iteracting`)
  - `agent.blocked`
  - `agent.working`
  - `agent.done`

## Utilisation
1) Mets le label `agent.created` sur une Issue/PR → l’agent propose plan + questions.
2) Mets le label `agent.interacting` → l’agent passe en Q/R.
3) Tant que `agent.interacting` (ou `agent.blocked`) est présent, chaque nouveau commentaire déclenche une réponse.
4) Quand l’agent renvoie `next_state=done`, il retire `agent.interacting` automatiquement et met `agent.done`.

## Labels “métier”
Le workflow autorise l’agent à ajouter/retirer des labels non-`agent.*` (configurable dans `label_policy`).
Tu peux bloquer certains labels via:
- `protected_exact`
- `protected_prefixes`

## Model / Effort
- Laisser vide (defaults Codex) ou override via `workflow_dispatch`:
  - `model`
  - `effort` (low|medium|high)
