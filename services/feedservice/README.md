# Feedservice

Provides utilities to load product feed definitions from affiliation providers such as Awin and Effiliation. It relies on remote file caching and serialisation services.

## Configuration

Feed parameters are supplied via `FeedConfiguration` instances in your application properties.

## Secrets in feed URLs

The Awin catalog key is a secret, not configuration:
`feed.providers.awin.catalog-url` ends with
`apikey/${affiliation-config.awin-datafeed-key:}` and the key is rendered in the
api config tree from `API_AWIN_DATAFEED_KEY` (see
`docs/operations/prod-service-secrets.md`).

Catalog and feed URLs can carry API keys: Awin as a path segment of the catalog
and feed URLs (`.../apikey/{key}/...`), Effinity as a path segment of every API
v3 URL (`apiv3/{key}/...`), and some endpoints as query values (`accessToken=`,
`key=`). Logs, exception messages, job stats and health details therefore never
contain a full feed or catalog URL:

- the catalog is logged as its scheme and host
  (`RemoteFileCachingService.loggableUrl`), with the provider name;
- `FeedIndexingWorker` labels each feed URL with `loggableFeedUrl(url)`: scheme,
  host and port, then the first 12 hexadecimal characters of the SHA-256 of the
  URL (`https://productdata.awin.com/<redacted> #4c0cf312e02d`), also used in
  the `invalid_csv_files` health detail and `FeedIndexingJobStat.url`;
- the `feed_url` attribute added to each `DataFragment` (debug) holds this same
  label, never the URL, so no API key is persisted with the fragments;
- Awin catalog lines are logged by advertiser ID and column names, never by
  their values;
- API endpoints are logged with `HttpUtils.safeEndpointForLogs`, which masks
  the `key=` and, for Awin, `accessToken=` values;
- download failures come without the JDK message, which quotes the URL (see the
  [remotefilecaching README](../remotefilecaching/README.md)).

New logs and exceptions follow the same rule; tests capture the logs with a URL
carrying `secret-key` and check it never appears (`FeedIndexingWorkerTest`,
`AwinFeedServiceTest`, `EffiliationFeedServiceTest`).

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
  as the v2 `id_affilieur` was. A feed is named after its `feedName` and points
  to its `feedUrl` (array `feeds`); feeds of `frozen` or `closed` programs are skipped.
- Values the API v3 no longer provides (program click, sale, lead and display
  commissions, EPC, conversion rate, signup URL) stay `null`.
- The key is a path segment, so request URLs are never logged: logs and the
  remote file cache use the `{apikey}` placeholder.

### Tracked offer URLs

A CSV datasource can require a tracked url for every offer
(`CsvDataSourceProperties`):

- `trackedUrlPattern`: regular expression searched in the final offer url
  (after `enforceAffiliatedUrl`). An url that does not match is untracked;
- `trackingLinkTemplate`: click template whose `{XXX}` placeholder is replaced
  by the URL-encoded untracked url.

An untracked url is rewritten with the template, or the line is rejected when
no template contains `{XXX}`: such an offer is never published with a direct
merchant link. The `Done:` line of the feed log counts both cases
(`tracking_rewritten`, `untracked_rejected`; rejected lines are also counted in
`not_validable`).

`KwankoFeedService` sets both on every Kwanko product feed, unless the
datasource YAML already does:

- the pattern accepts Kwanko tracking hosts (`metaffiliation.com`,
  `netaffiliation.com`, `kwanko.com` and their sub-domains) and Kwanko click
  tokens (`P...S1U...V4`), also used by merchant first-party tracking domains
  (`qby.mda-electromenager.com/?P...V4`);
- the template is the `tracked_material_per_websites[].urls.click` of the
  campaign `deeplink` ad, read once per refresh
  (`GET /publishers/ads?ad_types=deeplink`). A feed whose campaign has none is
  logged at WARN, and its untracked rows are rejected.

Kwanko feeds (`product page URL` column, no tracking column) carry a tracked
link on most rows but a direct merchant link on some: in November 2026,
"Carrefour - marketplace" offers pointed to `www.carrefour.fr` without
commission (GOU-2320).
