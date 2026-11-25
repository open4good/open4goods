# UI Module - Legacy, resource server only

> **Status**: the server-rendered UI is gone; the site is the [frontend](../frontend) (Nuxt).
> **Action**: do not add pages or features here. For UI work, see
> [frontend/AGENTS.md](../frontend/AGENTS.md).

This module is part of the open4goods multi-module Maven project. It is still
built and deployed (`ui-latest.jar`, port 8082) because it does three jobs:

1. **static.nudger.fr resources.** `ResourceController` serves the cached
   product images, PDFs and videos from `/opt/open4goods/.cached`, plus the
   legacy `/icon`, `/logo`, `/images/marques` and `/images/{gtin}-gtin.png`
   shapes. `ImageResizeInterceptor` produces the resized `.webp` variants.
2. **Sitemaps.** `SitemapGenerationService` writes
   `/opt/open4goods/sitemap/{lang}/*.xml` daily; the Nuxt containers mount and
   serve that folder.
3. **Open data redirect.** `OpenDataController` answers
   `/opendata/{gtin,isbn}-open-data.zip` (301 to
   `open-data-config.redirect-base-url` when set).

There are no templates, views or i18n bundles any more: a controller returns
data, never a view.

## Technology

- Java 21
- Spring Boot 4

## Directory structure

- `src/main/java` - resource controllers, sitemap job, configuration
- `src/main/resources` - configuration and the few static files
  (`favicon.ico`, `robots.txt`, `icons/no-image.png`)
- `src/test/java` - unit tests

## Build and test this module only

From this directory:

```bash
mvn --offline clean install
```

Run only the tests with:

```bash
mvn --offline test
```

From the repository root you can also execute:

```bash
mvn --offline -pl ui -am clean install
```
