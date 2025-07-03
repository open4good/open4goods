# Blog Service

This microservice fetches blog posts from XWiki and exposes them as an RSS feed.
Scheduled refresh keeps posts up to date and a Spring Boot health indicator
reports loading issues.

## Features

- Retrieves posts via `XwikiFacadeService`.
- Generates an RSS feed with Rome.
- Periodically refreshes posts using `@Scheduled`.
- Provides health status through Spring Boot Actuator.

## Configuration

Configuration properties (example `application.yml`):

```yaml
blog:
  feedType: "rss_2.0"
  blogUrl: "blog/"
  feedUrl: "blog/rss/"
  feedTitle: { en: "Open4goods", fr: "Open4goods" }
  feedDescription: { en: "Latest news", fr: "Dernières nouvelles" }
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for details.
This module is released under the [AGPL v3 license](../../LICENSE).
