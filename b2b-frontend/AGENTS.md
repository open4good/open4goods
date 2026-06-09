# b2b-frontend - agent guide

> Extends the root [`AGENTS.md`](../AGENTS.md). Public + authenticated web
> experience for the Product Data API, served on `product-data-api.com`.

## What this module is

Nuxt 4 / Vue 3 / Vuetify 4 / TypeScript frontend, bootstrapped from
`/home/goulven/git/infera/apps/frontend`. Public docs/pricing/playground +
authenticated customer dashboard + admin control plane in one app.

## Read first

- [docs/b2b/00-canonical-decisions.md](../docs/b2b/00-canonical-decisions.md)
- [docs/b2b/b2b-ui.md](../docs/b2b/b2b-ui.md) - UX/UI spec, route map, components
- [docs/b2b/b2b-frontend-build.md](../docs/b2b/b2b-frontend-build.md) - layout & OpenAPI codegen
- [docs/b2b/b2B.md](../docs/b2b/b2B.md) - master prompt (frontend section)

## Conventions (module-specific)

- **Flat Infera layout** (`pages/`, `composables/`, `domains/`, `components/`,
  `server/`, `content/`, `i18n/` at root) - not an `app/`-nested layout.
- i18n `prefix_except_default`, default `en` at `/`, French at `/fr/`. All UI copy
  in JSON locale files; every route has localized SEO metadata (`@nuxtjs/seo`).
- Public docs via `@nuxt/content` (`content/en`, `content/fr`); French pages are
  real translations, not placeholders.
- API access only through the **generated OpenAPI client** + `composables/`
  (repository style) + `domains/` mappers. Never hand-edit generated client code.
- Session/secret-bearing calls go through Nuxt **server routes** (`server/api`),
  never directly from browser components. Never expose API secrets in the bundle;
  clear API keys are shown once at create/rotate only.
- Pricing renders the backend YAML catalog (`/api/v1/customer/billing/catalog`),
  never hardcoded prices.
- Reduced-motion disables non-essential orbital animation.

## Validate

```bash
pnpm --dir b2b-frontend lint
pnpm --dir b2b-frontend typecheck
pnpm --dir b2b-frontend test
pnpm --dir b2b-frontend build
```
