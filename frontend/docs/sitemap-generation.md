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
   appends any configured extra sitemap URLs for that language to the index.
3. The helper in
   [`shared/utils/sitemap-config.ts`](../shared/utils/sitemap-config.ts)
   resolves the correct origin (production host, custom preview domain, or a
   local hostname) and merges the configured paths with the entries generated
   by Nuxt.

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

The default additional sitemap entries live in
`DEFAULT_ADDITIONAL_SITEMAP_PATHS` within
[`shared/utils/sitemap-config.ts`](../shared/utils/sitemap-config.ts). These are
merged into the index for every host unless you override them per language.

To customise the list for a given domain language, edit the
`CONFIGURED_DOMAIN_LANGUAGE_SITEMAPS` object in the same file. Each language can
expose its own `additionalPaths` array. For example, both English and French
currently publish the same set of XML files:

```ts
const CONFIGURED_DOMAIN_LANGUAGE_SITEMAPS = {
  en: {
    additionalPaths: DEFAULT_ADDITIONAL_SITEMAP_PATHS,
  },
  fr: {
    additionalPaths: DEFAULT_ADDITIONAL_SITEMAP_PATHS,
  },
}
```

To add a language-specific sitemap (for example `/sitemap/nl-blog.xml` for
Dutch), create a new entry pointing to that file. Paths are automatically
resolved relative to the incoming request origin, or you can provide absolute
URLs if the file is hosted elsewhere.

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
