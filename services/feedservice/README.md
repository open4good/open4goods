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

### Effiliation API

`EffiliationFeedService` reads the Effinity (ex-Effiliation) publisher API v3
(`https://api.effinity.fr/apiv3/{apikey}/...`, OpenAPI description at
<https://api.effinity.fr/apiv3/docs>). The API v2 is shut down on 11/01/2027
(dd/MM/yyyy). The publisher key is the same as for the API v2.

| Data | API v3 resource | Former API v2 resource |
|------|-----------------|------------------------|
| Programs | `publisher/programs?status=active&status=inactive` | `programs.json?filter=mines` |
| Product feeds | `publisher/links/productfeeds` | `productfeeds.json?filter=mines` |
| Promotions | `publisher/links?linkType=voucherOrDeal` | `commercialtrades.json` |
| Transactions | `publisher/conversions` (90-day windows) | `transaction.json` |

- Every resource is paginated (`page`, `perPage=1000`) and read until
  `pagination.lastPage`, with a 50-page guard.
- The program identifier exposed to callers is the `merchantId` (advertiser),
  as the v2 `id_affilieur` was. A feed is named after its `linkName` and points
  to its `trackedLink`; feeds of `frozen` or `closed` programs are skipped.
- Values the API v3 no longer provides (program click, sale, lead and display
  commissions, EPC, conversion rate, signup URL) stay `null`.
- The key is a path segment, so request URLs are never logged: logs and the
  remote file cache use the `{apikey}` placeholder.
