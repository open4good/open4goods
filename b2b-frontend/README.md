# b2b-frontend

Web frontend for the **Product Data API** (B2B), served on
`product-data-api.com`. Nuxt 4 / Vue 3 / Vuetify 4 / TypeScript, bootstrapped
from the Infera frontend stack.

It is a B2B developer platform (not a marketing microsite): public docs, pricing,
self-serve onboarding, API-key management, billing, a live playground, and an
admin control plane - English at `/`, French at `/fr/`.

## Quick start

```bash
pnpm --dir b2b-frontend install
pnpm --dir b2b-frontend dev        # / (en) and /fr/
```

Generate the API client from `b2b-api` first - see
[frontend build & codegen](../docs/b2b/frontend/build.md).

## Documentation

- [Canonical decisions](../docs/b2b/00-canonical-decisions.md)
- [UX/UI spec](../docs/b2b/frontend/ui-spec.md)
- [Frontend build & OpenAPI codegen](../docs/b2b/frontend/build.md)
- [Agent guide](AGENTS.md)
- [Local runbook](../docs/operations/product-data-api-local-runbook.md)
