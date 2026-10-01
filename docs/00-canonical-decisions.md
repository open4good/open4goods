---
title: "Canonical decisions"
status: accepted
last_updated: 2026-09-30
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
3. Each governed concept has one authoritative contract: project and corpus
   contracts under `.o4g/`, or a Paperclip issue for active tasks. Human
   explanations stay short and link to that contract. A long narrative
   document restating both is refused. Every governed Markdown document
   declares `title`, `normative` and `audience` in front matter.
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
   reviewable act recorded in the same commit as the growth it permits. CI enforces
   the direction against the default branch; a raise is satisfied only by the PR
   reviewer approving that commit.
8. Bounded change is tracked by an issue in the Nudger Paperclip project. The issue
   owns its purpose, scope, acceptance criteria, dependencies, blockers, assignee
   and evidence. Legacy closed WorkOrders remain read-only historical records under
   `.o4g/work/ledger/`. `docs/reference/roadmap.md` is a dated migration index;
   `docs/adr/README.md` remains a generated projection.
9. A claim about the system in a governed document is verified against the code
   before it is written, and corrected at the source when found false. A stale
   agent guide is a defect with the same standing as a stale test: it is what
   turned the load-bearing `ui` module into a deletion candidate.
10. Product reference data is represented by provider-neutral, replayable source
    records and resolved through a Git-versioned O4G concept registry. A source
    usage policy filters evidence by content type, effective period and publication
    surface before resolution; generic contracts contain no provider-specific field.
11. Offers and price observations are not product-reference assertions. Current
    offer state, append-only price change events and daily provider rollups have
    separate stores and retention policies; unchanged polling never creates a
    synthetic price event.
12. A normalized GTIN remains the unique product identity. Model and family groups
    are versioned, queryable relations on GTIN leaves; a model prefix is evidence
    only when a reviewed brand-and-class pattern rule gives it semantics.
13. Public consumer product search is lexical Elasticsearch search over the
    source-neutral GTIN projection. Reintroducing semantic or hybrid retrieval
    requires a dedicated Paperclip issue, a relevance benchmark and a capacity benchmark;
    no dormant query-time text embedding or vector-search path is retained.
14. Issues progress through DEVELOPMENT, BETA_VALIDATION, PRODUCTION and
    POST_PRODUCTION. DEVELOPMENT uses isolated stores and applications, including on
    a shared build host, with no dependency on beta services or Nudger domains. An explicit
    delivery mandate may delegate qualified beta promotions within its scope (ADR-0015).
    Production requires a fresh candidate-specific owner decision after phase checks;
    readiness alone never authorizes promotion. Physical retirement still waits for seven
    healthy days and verified restoration.
    Merges run CI without deployment. Final qualification uses a recent pinned product
    backup and explicit provider enrichment; startup never launches heavy ingestion jobs.
15. Legacy backup import exposes explicit INVENTORY, SAMPLE, APPLY, STATUS and CANCEL
    operations, never run on boot. Progress is a per-file checkpoint that resumes by
    decompress-and-skip, never by seeking a compressed offset, and advances only once a
    batch is fully reconciled. Record ids are deterministic, so the target store's own
    find-before-write is the dedup oracle: a conflicting duplicate is dead-lettered on
    both sides for review instead of a thread-order winner.
