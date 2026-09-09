---
title: "open4goods - Agents Guide (Root)"
normative: true
audience: PROJECT_SCOPED
---

# open4goods - Agents Guide (Root)

This guide defines **project-wide conventions** for all human contributors **and AI coding agents** working on the open4goods repository.  
Adhering to these rules keeps every sub-project consistent, maintainable, and predictable for automated tools (LLMs included).

> **Where to look next**  
> Each sub-module (e.g. `/api`, `/services/*`) ships its own `agents.md` that may add or override rules.  
> The standards below are **mandatory everywhere** unless a module's guide explicitly says otherwise.

---

## 0  Reading order

Load what the task needs, knowing the price. Everything below is the project corpus;
`README.md` files are outside it (canonical decision 5).

| Read | Cost | For |
|---|---|---|
| [canonical decisions](docs/00-canonical-decisions.md) | ~0.8k tokens | the numbered rules everything else cites |
| [ADR index](docs/adr/README.md) | ~0.4k tokens | which ADR owns a boundary, before opening one |
| [roadmap](docs/reference/roadmap.md) | ~1.2k tokens | the open WorkOrders, their state and their blockers |
| this guide, sections 1-11 | ~2.5k tokens | conventions for writing code here |
| [docs/README.md](docs/README.md) | ~0.9k tokens | the rest of the corpus, by subject |

Pick a WorkOrder from the roadmap, then read its own YAML under `.o4g/work/` and stay
within its `pathScope`. `docs/reference/roadmap.md` and `docs/adr/README.md` are
generated projections: change the contracts, not the projection.

Before changing anything under `docs/` or `.o4g/`, run `./scripts/lint.sh`. It gates
the corpus budget, so growing the prose fails the build unless you raise the ceiling
in the same commit.

---

## 1  Technology stack

| Area            | Standard                           |
|-----------------|------------------------------------|
| **JDK**         | Java 21                            |
| **Framework**   | Spring Boot 4.x                    |
| **Build tool**  | Maven (reactor / multi-module)     |
| **Testing**     | JUnit 5, AssertJ, Testcontainers   |

---

## 2  Standard package layout

Every module follows a *type-based* (layered) structure under  
`org.open4goods.<module>`:

```
org.open4goods.<module>
├─ config        ← @Configuration
├─ config.yml    ← @ConfigurationProperties, or yaml related configuration properties
├─ controller    ← @RestController / @Controller
├─ service       ← @Service
├─ repository    ← @Repository (Spring Data or custom DAO)
├─ dto           ← Records / simple POJOs for transport
├─ model         ← Domain entities / JPA @Entity
├─ util          ← Generic helpers (use sparingly)
└─ Application   ← <ModuleName>Application (@SpringBootApplication)
```

**Layering rule**

```
controller → service → repository
```

No class in a lower layer may depend on a higher one.

---

## 3  Naming conventions

| Layer / Role        | Suffix example                     |
|---------------------|------------------------------------|
| Main application    | `ApiApplication`                   |
| Controller          | `ProductController`                |
| Service (interface) | `ProductService`                   |
| Service impl        | `ProductServiceImpl` (or clearer)  |
| Repository          | `ProductRepository`, `ElasticProductRepository` |
| DTO                 | `ProductDto`, `CreateOrderRequest` |
| Config              | `WebSecurityConfig`                |
| Exception           | `ProductNotFoundException`         |

---

## 4  Spring stereotype usage

| Package        | Required annotation                             |
|----------------|-------------------------------------------------|
| `controller`   | `@RestController` (REST) or `@Controller` (MVC) |
| `service`      | `@Service`                                      |
| `repository`   | `@Repository` (or Spring Data interface)        |
| `config`       | `@Configuration` (+ `@ConfigurationProperties`) |
| Misc beans     | `@Component` (or a specialised stereotype)      |

- **Injection**: Constructor-based, `final` fields. Never use field injection.  
- **DTOs**: Prefer Java 21 records for immutability and zero boiler-plate.

---

## 5  Coding standards

- **Language features**: Records (except for Spring Boot config binding), sealed classes, pattern matching where useful.  
- **Error handling**: Custom exceptions + RFC 9457 / Problem-Detail responses.  
- **Logging**: SLF4J parameterised messages and structured logging.  
- **Tests**: Unit + integration tests for every feature or bug-fix.  
- **Javadoc**: Keep class/method Javadoc up-to-date; generate it when writing a new class, or update it when modifying existing code.  
- **TODOs**: Resolve or raise an issue; none should remain in committed code.

---

## 6  Build & test

To compile **all modules**:

```bash
mvn --offline clean install
```

Run the canonical lint suite before handoff:

```bash
./scripts/lint.sh
./scripts/lint.sh --fix
```

The suite covers Markdown/JSON text normalization, YAML, shell scripts, GitHub
Actions, Docker Compose, Dockerfiles when `hadolint` is installed, and the Nuxt
frontend lint checks. It does not build or generate the frontend.

The repository uses a tracked pre-push hook in `.githooks/pre-push`. Enable it
with:

```bash
git config core.hooksPath .githooks
```

## 6.1  Documentation policy

- Use [docs/README.md](docs/README.md) as the documentation entry point.
- Put significant architecture decisions in `docs/adr/` and link them from
  [docs/adr/README.md](docs/adr/README.md).
- Put shared designs and contracts in `docs/architecture/`.
- Put repeatable operational procedures in `docs/operations/`.
- Update the nearest durable document when code changes behavior, contracts,
  deployment, developer workflow, or AI-agent workflow.
- Markdown and JSON must use ASCII punctuation normalized by
  `scripts/python/text_replacements.py`.

## 7  Pull-request checklist

1. Clear commit & PR description (**why** and **what**).  
2. `mvn --offline clean install` passes locally.  
3. Tests added/updated; coverage not reduced.  
4. `./scripts/lint.sh` passes locally.
5. Docs (agents.md, README, ADR, Javadoc, spring-configuration-metadata.json) updated.

---

## 8  Rationale

A predictable, enforced structure lowers cognitive load for humans and gives large-language models a deterministic environment in which to operate. This accelerates onboarding, reduces bugs, and keeps architecture sound as we migrate monolith parts to independent Spring Boot services (see `services/*`).

## 9  Automated dependency updates

Dependencies for Maven modules, Node projects (`frontend` and `ui`), and GitHub
Actions workflows are maintained by Renovate. Updates run **Mondays between
00:00 and 05:59** (`renovate.json` `schedule`), at most 4 pull requests at a time.
Major upgrades are **not** disabled: they are gated behind
`dependencyDashboardApproval`. `ui/**` is excluded from Renovate.

## 10  Module-Specific Guides

For detailed module-specific conventions, see:

- **Frontend**: [frontend/AGENTS.md](frontend/AGENTS.md) - Nuxt 4 / Vue 3 / Vuetify
- **B2B Frontend**: [b2b-frontend/AGENTS.md](b2b-frontend/AGENTS.md) - Nuxt 4 / Vue 3 / Vuetify 4 / TypeScript
- **Front API**: [front-api/AGENTS.md](front-api/AGENTS.md) - SpringDoc / OpenAPI
- **B2B API**: [b2b-api/AGENTS.md](b2b-api/AGENTS.md) - Spring Boot 4 / Java 21
- **Services**: [services/AGENTS.md](services/AGENTS.md) - 25 service modules
- **Core Modules**: [admin](admin/AGENTS.md), [api](api/AGENTS.md), [commons](commons/AGENTS.md), [model](model/AGENTS.md), [verticals](verticals/AGENTS.md)
- **UI**: [ui/AGENTS.md](ui/AGENTS.md) - Thymeleaf app behind `static.nudger.fr`.
  **Not deprecated, despite what this guide said until 2026-09-08.** It serves every
  product and brand image, is the only producer of the sitemaps `frontend` reads
  (`SitemapGenerationService`), and is the only handler for the open-data download.
  It has no Maven dependents and was dropped from the local `docker-compose.yml`,
  which is what made it look dead; it is still deployed by `releaseDeployProd.yml`.

---

## 11  MCP Tooling

Run `scripts/mcp/doctor.sh --core` before stack work. The generated project
configuration is shared by Claude, Codex, Gemini and VS Code; edit
`ops/mcp/servers.yml`, never a generated client file.

- Use JavaLens for symbols, references, implementations and call hierarchies before
  a narrow `rg` fallback.
- Use `maven-deps` or `maven-tools` for dependency and effective-model questions;
  use the commands in section 6 for compilation and tests.
- Consult Vuetify MCP before changing a Vue component or relying on a component
  prop, slot or version-specific behavior.
- Use Docker MCP only for read-only container state and logs. Repository launchers
  own stack start, stop and mutation.
- Use Playwright for browser recette and Nuxt MCP while the local frontend exposes it.

If a required server fails the doctor, record its sanitized failure as evidence.
Only then may Java navigation fall back to `rg` within the WorkOrder `pathScope`.

---

> **Questions?**
> Open a GitHub Discussion or justify any deviation in your PR description.
