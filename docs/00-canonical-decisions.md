---
title: "Canonical decisions"
status: accepted
last_updated: 2026-09-08
normative: true
audience: PROJECT_SCOPED
---

# Canonical decisions

Numbered rules that every other document cites by number, never by heading. An
ADR that changes a rule updates the entry here in the same commit.

1. This repository is **public**. No secret, no credential, and no internal
   infrastructure topology is committed. Configuration that reveals hosts, internal
   URLs or ports is delivered as a GitHub Environment variable or secret, never as
   a tracked file.
2. Three corpora share this repository and are never mixed. The **project corpus**
   (`AGENTS.md`, `docs/**` minus the exemptions) is read by humans and agents. The
   **published content** (`docs/en/**`, `docs/fr/**`) is website data, authored
   against the Nuxt Content schema in `frontend/content.config.ts`, and is out of
   the project corpus's budget and lint. The **build payload** is whatever a build
   step packages into an artifact; a build step never sweeps the repository
   wholesale to find it.
3. Every concept exists exactly twice: once as a machine contract under `.o4g/`,
   once as a short human explanation. The third form -- a long narrative document
   restating both -- is refused. Every governed Markdown document declares `title`,
   `normative` and `audience` in front matter.
4. Only `AGENTS.md`, this document and `docs/adr/**` may state rules
   (`normative: true`). Everywhere else, rule-shaped language is counted and
   budgeted: a rule found there belongs in an ADR or in this list.
5. The governed set is `AGENTS.md` plus `docs/**`. `README.md` files are outside
   it: this is a public open-source repository whose front page GitHub renders as a
   visible table, and front matter there is noise for the audience that reads it.
6. Normative and machine-read text is English. Explanatory text may be French and
   declares `lang: fr`; a normative document declaring `fr` is refused, because a
   rule an English-reading agent cannot parse is a rule that does not apply.
7. The corpus budget in `.o4g/corpus-budget.json` is a ratchet. Lowering a ceiling
   is an ordinary commit and needs no ceremony. Raising one is a deliberate,
   reviewable act recorded in the same commit as the growth it permits.
8. Bounded change is carried by a WorkOrder under `.o4g/work/`, which names its
   purpose, its `pathScope`, and its acceptance criteria. A closed WorkOrder moves
   to `.o4g/work/ledger/` rather than being deleted. `docs/reference/roadmap.md`
   and `docs/adr/README.md` are generated projections and are never hand-edited.
9. A claim about the system in a governed document is verified against the code
   before it is written, and corrected at the source when found false. A stale
   agent guide is a defect with the same standing as a stale test: it is what
   turned the load-bearing `ui` module into a deletion candidate.
