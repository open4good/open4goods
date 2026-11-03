# Sitemap generation guide

This project uses the `@nuxtjs/sitemap` module to expose domain-aware XML
sitemaps for every hostname defined in
[`shared/utils/domain-language.ts`](../shared/utils/domain-language.ts).
This document explains how the sitemap index is produced, how the additional
sitemap files are configured per domain language, and what to update when a new
locale or section needs to be published.

## How the sitemap is built

1. `nuxt.config.ts` enables the `@nuxtjs/sitemap` module and registers the main
   routes sitemap under the `main-pages` key. Raw app-source discovery is
   disabled for this sitemap so canonical output does not mix source paths such
   as `/team` with French production paths such as `/equipe`. The Nitro plugin in
   [`server/plugins/sitemap-main-pages.ts`](../server/plugins/sitemap-main-pages.ts)
   ensures the main marketing pages are always emitted for the requested
   domain language. It scans the static pages under `app/pages` (skipping
   auth/contrib flows and other dynamic routes) and merges them with the
   localized wiki routes defined in
   [`shared/utils/localized-routes.ts`](../shared/utils/localized-routes.ts).
   This keeps the list self-updating as new marketing sections are added
   without editing the plugin.
2. The Nitro plugin in
   [`server/plugins/sitemap-index.ts`](../server/plugins/sitemap-index.ts)
   listens to the `sitemap:index-resolved` hook. For each request it
   determines the domain language via
   [`getDomainLanguageFromHostname`](../shared/utils/domain-language.ts) and
   appends any configured extra sitemap file paths for that language to the
   index. The plugin reads a server-only runtime configuration map so the
   additional file list never leaks to the client bundle.
3. Server-side helpers in
   [`server/utils/sitemap-local-files.ts`](../server/utils/sitemap-local-files.ts)
   normalise the runtime configuration and return the deduplicated list of
   local sitemap files to append.

## Configuring domain languages and hosts

All host → language mappings live in
[`HOST_DOMAIN_LANGUAGE_MAP`](../shared/utils/domain-language.ts). Each entry in
that object must point to a valid `DomainLanguage` union member (`'en' | 'fr'` at
present). When you add a new domain:

1. Extend the `DomainLanguage` union with the new language code.
2. Add the hostname to `HOST_DOMAIN_LANGUAGE_MAP`.
3. Optionally extend the Nuxt locale map (`NuxtLocale`) if the language is new.
4. Update the sitemap configuration so the new language exposes the right
   additional sitemap URLs (see below).

The sitemap generation utilities automatically derive the set of supported
languages from this map, so adding the entry is enough to make the locale
eligible for sitemap generation.

## Additional sitemap files per language

Additional sitemap entries are declared in the server-only `runtimeConfig`
under the `sitemapLocalFiles` key defined in
[`nuxt.config.ts`](../nuxt.config.ts). Each domain language maps to an explicit
list of XML files on disk. For example, the defaults currently ship with:

```ts
runtimeConfig: {
  // ...
  sitemapLocalFiles: {
    fr: [
      '/opt/open4goods/sitemap/fr/category-pages.xml',
      '/opt/open4goods/sitemap/fr/product-pages.xml',
    ],
    en: [
      '/opt/open4goods/sitemap/default/category-pages.xml',
      '/opt/open4goods/sitemap/default/product-pages.xml',
    ],
  },
  // ...
}
```

`guides.xml` is not listed: the ui only writes it for non-vertical guides
under `verticals/src/main/resources/guides/default/`, which holds none, so the
entry pointed to a 404 (GOU-1366). Add it back with the first default guide.

To add or override entries for another language, extend this map with the new
domain language key and provide the list of file paths. Because the configuration
is server-only, the paths remain private while still being appended to the
rendered sitemap index. Requests for these entries are served through the Nitro
route at `/sitemap/<domainLanguage>/<fileName>.xml`, which streams the matching
local file without exposing the underlying filesystem path.

## Product and category files (ui generation)

The XML files listed in `sitemapLocalFiles` are written by
`SitemapGenerationService` in the `ui` module, under
`<rootFolder>/sitemap/<language>/` (one folder per `namings.base-urls` key;
prod only configures `fr`).

- **Canonical rule.** Each product URL is the language base URL followed by
  `ProductCanonicalUrl.fullSlug(product, vertical, domainLanguage)` from the
  `model` module: `/<verticalHomeUrl>/<names.url>`, or `/<names.url>` for a
  product without vertical. The front-api `ProductDto.fullSlug` calls the same
  method, and `ProductPage.vue` 301-redirects every other path to it and uses
  it as `rel=canonical`, so a listed URL answers 200 without redirect. The
  rule never depends on the request locale.
- **Selection.** Products of enabled verticals only (pages of a disabled
  vertical are `noindex`, products without vertical are not listed), not
  `excluded`, with a recent price and an impact score.
- **`lastmod`** is the product `lastChange` (last real data change), omitted
  when unknown. Category pages carry no `lastmod`.
- **Frequency.** Five minutes after the ui starts, then every 24 hours. An
  admin can also trigger it with `GET /sitemap` on the ui.
- **Atomic write.** Each file is written to `.<name>.tmp` in the same folder,
  then moved over the previous file, so Nitro never streams a partial file. A
  file whose generation fails keeps its previous version.
- **Monitoring.** A failure is logged at ERROR (ui log and
  `logs/stats-sitemap.log`) and counted in the `sitemap.generation` metric with
  `result=failure`. `sitemap.generation.last.success` gives the epoch second of
  the last fully successful run and `sitemap.product.urls{language}` the number
  of product URLs written. Read them on the ui actuator, for example
  `/actuator/metrics/sitemap.generation.last.success`.

## Checking a published sitemap

`scripts/sample_sitemap_urls.py` samples URLs from a sitemap. With `--check`
it requests each sampled URL without following redirects and fails unless it
answers 200 with a `rel=canonical` equal to the URL; `--max-lastmod-age-hours`
also requires a recent `lastmod`. Requests are sequential (`--delay`, 1 s by
default) and a 403 or 429 stops the run.

```bash
python3 scripts/sample_sitemap_urls.py \
  --sitemap https://beta.nudger.fr/sitemap/fr/product-pages.xml \
  --n 200 --check --max-lastmod-age-hours 48
```

Beta is behind HTTP basic auth: export
`SITEMAP_SAMPLER_BASIC_AUTH=user:password` before running the script (the
value is sent on every request and never printed). The URLs of a sitemap come
from the ui `namings.base-urls`, so on beta they must start with
`https://beta.nudger.fr/` for the check to test beta.

The exit code is 0 when every URL passes, 1 when one fails (each failure is
printed with its reason), 2 when the site refused the requests. Without
`--check`, `--out <file>` only writes the sample, as before.

## Testing changes

After updating the configuration, run the usual quality gates to confirm that
static generation still succeeds and the sitemap plugin compiles:

```bash
pnpm lint
pnpm test
pnpm generate
```

You can also preview the generated XML locally by running `pnpm dev` and
navigating to `http://localhost:3000/sitemap/index.xml` with different `Host`
headers that correspond to the domains in `HOST_DOMAIN_LANGUAGE_MAP`.

## Troubleshooting

- **Missing domains in the index:** confirm the hostname is declared in
  `HOST_DOMAIN_LANGUAGE_MAP` and that DNS or your local `/etc/hosts` routes it
  to the development server.
- **Unexpected origin in generated URLs:** pass the desired origin through the
  `NUXT_PUBLIC_SITE_URL` runtime config or test with the `Host` header that
  mirrors production. The helper normalises the protocol automatically for
  local (`http`) vs. public (`https`) hosts.
- **Duplicate entries:** the plugin deduplicates sitemap URLs before pushing
  them to the index. If duplicates persist, ensure the strings match exactly
  (including trailing slashes).
