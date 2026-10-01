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

### CSV column mapping — no label-guessing

`FeedIndexingWorker` reads each CSV cell only from the column names declared in the feed's
`csvDatasource` configuration (`CsvDataSourceProperties`: `url`, `price`, `name`, `image`,
`description`, `inStock`, `productState`, `shippingCost`, `shippingTime`, `quantityInStock`,
`mpn`, `sku`, `referentiel`, ...). Those files live outside this repository, under the
deployment's `datasourcesfolder` (see `DataSourceConfigService`, default
`<rootFolder>/config/datasources/`, `./datasources/` in dev/local) and are reviewed by a human
when a feed is activated. There is no fallback list of guessed column labels: a column that is
not declared is never silently matched by name.

For each feed URL, `FeedColumnResolver.resolve()` is called once against the CSV header row
(not once per data row) using a `FeedDefinition` built by `FeedDefinitionFactory` from that
feed's declared columns. Headers present in the file but declared nowhere in the configuration
are reported as `unknownColumns`: logged in the feed's dedicated log file and recorded on
`FeedIndexingJobStat#getUnknownColumns()`. They are never used to populate a field.

A feed whose `csvDatasource` declares neither an explicit `url` column nor at least one
explicit `price` column is not indexed at all: it is skipped with a single error log naming the
feed key, and counted in the `feed_missing_required_columns` health indicator
(`FeedIndexingService#health()`). This is a deliberate regression versus the previous
label-guessing behaviour: the operational follow-up (GOU-172) inventories `datasources` files and
backfills the missing declarations from the `unknownColumns` reports.

### Effiliation scheduler

Effiliation refresh is controlled by `feed.effiliation.*` properties:

- `cron`: refresh schedule
- `enabled`: enables/disables all Effiliation retrieval methods
- `cache-ttl-days`: remote cache TTL
- `max-jitter-seconds`: random delay applied before scheduled execution
