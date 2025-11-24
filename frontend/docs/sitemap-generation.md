# Sitemap generation guide

This project uses the `@nuxtjs/sitemap` module to expose domain-aware XML
sitemaps for every hostname defined in
[`shared/utils/domain-language.ts`](../shared/utils/domain-language.ts).
This document explains how the sitemap index is produced, how the additional
sitemap files are configured per domain language, and what to update when a new
locale or section needs to be published.

## How the sitemap is built

1. `nuxt.config.ts` enables the `@nuxtjs/sitemap` module and registers the app
   routes sitemap under the `app-pages` key. Nuxt automatically crawls the
   file-based routes in `app/pages` for each configured locale and generates
   `app-pages.xml` under the `/sitemap` prefix. The Nitro plugin in
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
      '/opt/open4goods/sitemap/fr/blog-posts.xml',
      '/opt/open4goods/sitemap/fr/category-pages.xml',
      '/opt/open4goods/sitemap/fr/product-pages.xml',
      '/opt/open4goods/sitemap/fr/verticals-pages.xml',
      '/opt/open4goods/sitemap/fr/wiki-pages.xml',
    ],
    en: [
      '/opt/open4goods/sitemap/en/blog-posts.xml',
      '/opt/open4goods/sitemap/en/category-pages.xml',
      '/opt/open4goods/sitemap/en/product-pages.xml',
      '/opt/open4goods/sitemap/en/verticals-pages.xml',
      '/opt/open4goods/sitemap/en/wiki-pages.xml',
    ],
  },
  // ...
}
```

To add or override entries for another language, extend this map with the new
domain language key and provide the list of file paths. Because the configuration
is server-only, the paths remain private while still being appended to the
rendered sitemap index.

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
