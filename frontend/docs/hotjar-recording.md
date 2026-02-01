# Hotjar recording gate

This frontend uses an opt-in gate for Hotjar recordings that only activates when
users explicitly land on the homepage with `/?record`.

## How it works

1. When the homepage is requested with `/?record`, the server middleware:
   - sets a `hotjar_record=true` cookie (90 days),
   - redirects the request to the canonical `/` URL to avoid indexing the query
     string.
2. On the client, the Hotjar plugin runs only when:
   - the `hotjar_record` cookie is present (when mode is `query`),
   - Do Not Track is **not** enabled,
   - a valid `public.hotjar.siteId` is set.

If the `record` parameter is absent, no cookie is set and Hotjar is not
initialized.

## Configuration

Hotjar is configured via runtime config values exposed to the client:

- `HOTJAR_ENABLED` (defaults to `true` in production, otherwise `false`)
- `HOTJAR_MODE` (`query`, `always`, or `never`; defaults to `query`)
- `HOTJAR_SITE_ID` (Hotjar site ID, required to enable recordings)
- `HOTJAR_SNIPPET_VERSION` (defaults to `6`)

These are surfaced as `runtimeConfig.public.hotjar` in `nuxt.config.ts`.
