# Documentation Guidelines

Documentation should be short, discoverable, and tied to decisions.

## Layout

- Put durable engineering decisions in `docs/adr/`.
- Put system designs and shared contracts in `docs/architecture/`.
- Put repeatable operational procedures in `docs/operations/`.
- Put datasource onboarding in `docs/datasources/`.
- Put market, governance, and positioning docs in `docs/business/`.
- Keep localized public content under `docs/en/` and `docs/fr/`.

## Writing Rules

- Prefer one canonical page and link to it from related docs.
- Update existing docs before adding a new document with overlapping scope.
- Record architectural choices in ADRs instead of burying them in chat, PR text,
  or implementation comments.
- Keep implementation-local details in the module README or nearest `AGENTS.md`.
- Use ASCII punctuation in Markdown and JSON. Run `./scripts/lint.sh --fix`
  before committing documentation changes.

## Agent Workflow

When an agent changes code or configuration:

1. Read the nearest `AGENTS.md` files.
2. Check `docs/README.md` for the canonical doc location.
3. Update or create an ADR for significant decisions.
4. Link new durable docs from `docs/README.md`.
5. Run `./scripts/lint.sh` or the smallest relevant subset before handing off.
