# Feedservice

Provides utilities to load product feed definitions from affiliation providers such as Awin and Effiliation. It relies on remote file caching and serialisation services.

## Configuration

Feed parameters are supplied via `FeedConfiguration` instances in your application properties.

## Build & Test

```bash
mvn clean install
mvn test
```

See the [main project](../../README.md) for details.

### Effiliation scheduler

Effiliation refresh is controlled by `feed.effiliation.*` properties:

- `cron`: refresh schedule
- `enabled`: enables/disables all Effiliation retrieval methods
- `cache-ttl-days`: remote cache TTL
- `max-jitter-seconds`: random delay applied before scheduled execution
