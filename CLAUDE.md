# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Full build (all modules)
mvn --offline clean install

# Build specific module with dependencies
mvn --offline -pl <module> -am clean install

# Run tests only
mvn --offline test

# Run tests for specific module
mvn --offline -pl <module> test
```

### Frontend (Nuxt 3)

```bash
cd frontend
pnpm install
pnpm dev          # Dev server on http://localhost:3000
pnpm build        # Production build
pnpm lint         # ESLint
pnpm lint:fix     # ESLint + Prettier fix
pnpm test         # Vitest tests
pnpm generate:api # Regenerate OpenAPI client from front-api
```

### Running Applications

```bash
# Start infrastructure (Elasticsearch, Redis)
docker compose up

# API (port 8081)
java -Dspring.profiles.active=dev -jar api/target/api-*.jar

# UI - deprecated (port 8082)
java -Dspring.profiles.active=dev -jar ui/target/ui-*.jar

# Crawler (port 8080) - usually embedded in API
java -Dspring.profiles.active=dev -jar crawler/target/crawler-*.jar
```

## Architecture Overview

Open4goods is a multi-module Maven **modulith** for product data aggregation, identified by GTIN/UPC codes.

### Core Modules

| Module | Purpose |
|--------|---------|
| `model` | Domain objects shared across modules |
| `commons` | Shared utilities and configurations |
| `crawler` | Scrapes external product sources |
| `api` | Central REST API, aggregation logic, data pipelines |
| `front-api` | Lightweight API consumed by Nuxt frontend |
| `frontend` | Nuxt 3 + Vue 3 + Vuetify application |
| `verticals` | YAML configs for product categories (TVs, washing machines, etc.) |
| `admin` | Spring Boot Admin console for monitoring |
| `ui` | **Deprecated** Thymeleaf interface |
| `services/*` | Independent Spring Boot microservices (20+ services) |

### Data Flow

```
External Sources → Crawler → API (aggregation) → Elasticsearch/Redis
                                    ↓
                             front-api → Frontend (Nuxt 3)
```

### Package Layout Convention

All Java modules follow a type-based structure under `org.open4goods.<module>`:

```
org.open4goods.<module>
├─ config        ← @Configuration
├─ config.yml    ← @ConfigurationProperties
├─ controller    ← @RestController / @Controller
├─ service       ← @Service
├─ repository    ← @Repository
├─ dto           ← Records / POJOs for transport
├─ model         ← Domain entities
└─ util          ← Generic helpers
```

**Layering rule**: `controller → service → repository` (no upward dependencies)

## Technology Stack

| Area | Standard |
|------|----------|
| JDK | Java 21 |
| Framework | Spring Boot 3.x |
| Build | Maven (multi-module reactor) |
| Testing | JUnit 5, AssertJ, Testcontainers |
| Frontend | Nuxt 3, Vue 3, Vuetify, TypeScript, pnpm |
| Data | Elasticsearch, Redis |

## Coding Standards

### Java

- **Formatting**: 4-space indent, braces on new line
- **Injection**: Constructor-based, `final` fields. Never use field injection
- **DTOs**: Prefer Java 21 records for immutability
- **Logging**: SLF4J parameterized messages
- **Javadoc**: Required for all classes and public methods
- **Error handling**: Custom exceptions + RFC 9457 Problem-Detail responses

### Frontend (Nuxt/Vue)

- Use `<script setup lang="ts">` in all components
- Prefer Vuetify components, design mobile-first
- All static text must be internationalized (i18n)
- Keep composables SSR-safe (guard against `window`/`document`)
- OpenAPI client in `shared/api-client/` - regenerate with `pnpm generate:api`

## Module-Specific Guides

Each module has its own `AGENTS.md` with specific conventions:

- [Root AGENTS.md](AGENTS.md) - Project-wide mandatory conventions
- [frontend/AGENTS.md](frontend/AGENTS.md) - Nuxt 4 / Vue 3 / Vuetify rules
- [front-api/AGENTS.md](front-api/AGENTS.md) - SpringDoc / OpenAPI rules
- [services/AGENTS.md](services/AGENTS.md) - Microservices (20+ services)

## Key Integration Points

### front-api ↔ frontend Contract

1. Controllers and DTOs in `front-api` are the **single source** of the OpenAPI contract
2. Every controller method requires `@RequestParam DomainLanguage domainLanguage`
3. All DTO fields must have `@Schema` annotations
4. After changes: `mvn --offline clean install` in front-api, then `pnpm generate:api` in frontend

### Verticals Configuration

Product verticals (TVs, washing machines, etc.) are defined in YAML files under `verticals/src/main/resources/verticals/`. These configs include eco-scoring rules.

## Environment Setup

Prerequisites: Java 21+, Maven, Node 20+, pnpm 10.12.1+

```bash
# Check environment
./scripts/dev-doctor.sh

# Required environment variables (frontend)
API_URL=http://localhost:8082
TOKEN_COOKIE_NAME=access_token
REFRESH_COOKIE_NAME=refresh_token
MACHINE_TOKEN=<server-to-server-token>
```

### Elasticsearch Setup

```bash
# Required kernel setting
sudo sysctl -w vm.max_map_count=262144
```

## PR Checklist

1. Clear commit & PR description (**why** and **what**)
2. `mvn --offline clean install` passes locally
3. Tests added/updated; coverage not reduced
4. Docs updated (AGENTS.md, README, Javadoc)
5. For frontend: `pnpm lint && pnpm test && pnpm generate` pass

## Spring profiles
Use profiles devsec as default profile

