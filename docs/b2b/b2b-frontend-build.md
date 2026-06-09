# Product Data API - frontend build, layout & OpenAPI codegen

> Canonical authority: [`00-canonical-decisions.md`](00-canonical-decisions.md).
> Companion to the UX spec [`b2b-ui.md`](b2b-ui.md). Resolves two
> implementation ambiguities the UX spec left open: the **project layout** and the
> **OpenAPI client codegen pipeline**. `b2b-frontend` is bootstrapped from
> `/home/goulven/git/infera/apps/frontend`.

## 1. Project layout (reconciled)

`b2b-ui.md` 4 sketched an `app/`-nested layout (`app/components/`, `app/pages/`,
...). The actual Infera bootstrap source uses a **flat** Nuxt layout
(`pages/`, `composables/`, `domains/`, `components/`, `server/`, `content/`,
`i18n/` at the project root - verified in `infera/apps/frontend`). To keep the
bootstrap mechanical, **adopt the flat Infera layout** and treat the `app/`
drawing in `b2b-ui.md` 4 as illustrative grouping, not literal paths.

```text
b2b-frontend/
  components/   {admin, billing, dashboard, docs, keys, landing, playground, shared}
  composables/  repository-style composables (useApiClient, useBackendClient, ...)
  domains/      DTO -> ViewModel mappers (one folder per domain, e.g. products/)
  pages/        route files (public, dashboard, admin)
  layouts/
  stores/
  server/       api/ (session-backed proxy routes), utils/
  content/      en/ , fr/  (@nuxt/content docs)
  i18n/locales/ en.json , fr.json
  generated/    OpenAPI client output (git-ignored or committed - decide once)
  scripts/      generate-openapi.ts
  nuxt.config.ts, eslint.config.mjs, tsconfig.json, content.config.ts, AGENTS.md
```

Stack invariants (from `b2B.md` / `b2b-ui.md`): Nuxt 4, Vue 3, Vuetify 4, TS,
`@nuxtjs/i18n` (`prefix_except_default`, default `en`, French `/fr/`),
`@nuxtjs/seo`, `@nuxt/content`. All UI copy localized; every route has localized
SEO metadata.

## 2. OpenAPI client codegen

Reuse the Infera pattern (`infera/apps/frontend/scripts/generate-openapi.ts` +
`openapitools.json`):

1. **Source of truth**: the `b2b-api` SpringDoc spec. Produce it either by hitting
   a running backend (`http://localhost:8087/v3/api-docs`) or by exporting a static
   `openapi.json` during the Maven build. Prefer a committed static spec so the
   frontend builds without a live backend in CI.
2. **Generate**: `pnpm --dir b2b-frontend generate:openapi` runs the script ->
   typed client into `generated/`. **Never hand-edit generated output.**
3. **Map**: components call the generated client through `composables/` (repository
   style) and `domains/` mappers (DTO -> ViewModel), never the raw client inline.
4. **CI**: regenerate (or verify up-to-date) on backend contract changes; fail the
   build if the committed spec/client drifts from the backend.

## 3. Session-backed backend calls

Browser components never hold a stored clear API secret, session cookie, or
machine token. Session-authenticated calls go through Nuxt **server routes**
(`server/api/...`) that attach the session and proxy to `b2b-api`.

Playground live mode uses the proxy endpoint
`POST /api/v1/customer/playground/products/price` with `{ apiKeyId, gtin,
language }`; the backend executes the real external call with the selected key and
returns the executed request (key masked), response body+headers, and metering
(`billable`, `creditsConsumed`, `creditsRemaining`, `reason`). See
[`b2b-ui.md`](b2b-ui.md) 7.5.

## 4. Validation

```bash
pnpm --dir b2b-frontend lint
pnpm --dir b2b-frontend typecheck
pnpm --dir b2b-frontend test
pnpm --dir b2b-frontend build
```

Required focused tests are listed in [`b2b-ui.md`](b2b-ui.md) 12 (i18n routing,
localized SEO, docs rendering, pricing catalog from backend, login/session,
dashboard states, key create/rotate/revoke, playground success/401/402/no-data-no-pay,
admin role gating, admin manual grant).
