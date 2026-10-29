# Outbound affiliation clicks

How a click on an offer button (`/contrib/<token>`) is recorded and reported in
Metriks, without personal data (GOU-1188).

## Flow

1. The product page links to `/contrib/<token>`. The token is the compressed
   JSON of a `ContributionVote`: merchant (`datasourceName`), target URL and,
   since GOU-1188, the product `gtin` (tokens without `gtin` stay valid).
2. The Nuxt route (`frontend/server/routes/contrib/[...token].ts`) refuses
   robots, then calls front-api `GET /affiliations/redirect`, forwarding the
   optional `X-Affiliation-Origin` header (path of the originating page).
3. front-api `AffiliationService.trackRedirect` writes one `AffiliationClick`
   document and the legacy `ContributionVote` (without IP nor user agent), both
   fail-open: a persistence failure never blocks the redirect.
4. front-api answers `302` with `Cache-Control: no-store` (a `301` could be
   cached by the browser, and a cached click never reaches the server). The
   Nuxt route forwards that status to the browser; it also accepts `301` from
   an older front-api.
5. api `GET /api/metriks/affiliation` aggregates the `affiliation-clicks` index.

## `affiliation-clicks` index

Document `org.open4goods.services.contribution.model.AffiliationClick`, shared
by front-api (writer, `AffiliationClickRepository`, which creates the index)
and api (reader, `AffiliationClickStatsService`).

| Field        | Type      | Content                                                    |
|--------------|-----------|------------------------------------------------------------|
| `id`         | keyword   | Random UUID                                                |
| `ts`         | date      | Click time, epoch milliseconds                             |
| `network`    | keyword   | `awin`, `effiliation`, `kwanko`, `tradetracker`, `amazon` or `other` |
| `merchant`   | keyword   | Datasource name of the offer                               |
| `gtin`       | keyword   | Product GTIN, `null` for tokens without it                 |
| `originPath` | keyword   | Originating path, `null` when absent or invalid            |
| `targetHost` | keyword   | Host of the target URL (never the full URL)                |

No IP, no user agent, no full URL. `network` is deduced from the target host by
`AffiliationNetwork.fromUrl`: `awin1.com` and its sub-domains are `awin`;
`effiliation.com`, `effinity.fr` and their sub-domains are `effiliation`;
`metaffiliation.com`, `kwanko.com` and their sub-domains are `kwanko`;
`tradetracker.net`, `tradetracker.com` and their sub-domains are
`tradetracker`; `amazon.fr` and its sub-domains, `amzn.to` and `amzn.eu` are
`amazon` (not `amazon.com`); anything else (including an unreadable URL) is `other`.

TradeTracker links on a merchant's own domain (e.g. `partner.aosom.fr`) are
recognised by their signature, read on the full URL: query parameters `c`, `m`
and `a` all numeric and `u` present. Only the host is stored. A direct merchant
link without this signature stays `other`.

Clicks stored before this classification (since 2026-10-26) keep `network =
other`: they are not rewritten, so Kwanko and TradeTracker counts start at the
deployment date.

`X-Affiliation-Origin` is kept only when it is a bare path: it starts with a
single `/`, has no `?`, `#`, whitespace or control character, and is at most
200 characters long. Otherwise it is ignored (`null`).

The documents already stored in `contribution-votes` with IP and user agent
were not touched by this change. Their purge (board approval 2f4935d1) runs in
front-api at startup, see `ContributionVoteIpUaPurgeService` and the front-api
README, "Contribution votes ip/ua purge" (GOU-1230).

## Metriks events

Added after the commission events of `/api/metriks/affiliation`:

- `affiliation.<awin|effiliation|kwanko|tradetracker|amazon|other|total>.clicks.<7d|28d>`, unit `count`,
  windows `[D-7, D-1]` and `[D-28, D-1]` in Europe/Paris by click time, with
  the same window `params` as the commission events (no `currency`);
- `affiliation.clicks.top_merchants.28d`: `value` is the number of rows,
  `params.rows` lists `merchant`, `network` and `clicks`, top 20, most clicked
  first (ties ordered by merchant).

A single Elasticsearch request (date range query, `terms` and `multi_terms`
aggregations, no document fetched) feeds all of them. A network without click
reports `0`. When Elasticsearch cannot answer (unreachable, missing index), all
click events are in `error`, never `0`; the commission events are unaffected.

api and front-api must use the same Elasticsearch cluster
(`spring.elasticsearch.uris` in open4goods-config: `localhost:9200` on the beta
box for both, the same production node for both).

## Kwanko revenues (GOU-1379)

`KwankoReportClient` (api, `services/metriks/affiliation`) reads
`GET https://api.kwanko.com/publishers/conversions?completion_date_from=...&completion_date_to=...`
(bearer token, dates as UTC ISO with `+00:00`) and feeds
`affiliation.kwanko.commission.{validated,pending}.{7d,28d}` and
`affiliation.kwanko.sales.*`, included in the totals. `state` maps to validated
(`approved`, `validated`, `accepted`), pending (`pending`, `waiting`,
`ongoing`) or declined (`rejected`, `declined`, `refused`, `cancelled`); any
other value, a missing commission or a non-EUR currency is a network error.
Further pages are read (`page=N`) only when the response announces
`pagination.total_pages` or `last_page`.

The token is `metriks.affiliation.kwanko.api-token`, which defaults to
`${affiliation-config.kwanko-token:}` in `api/src/main/resources/application.yml`:
the feed secret `API_KWANKO_TOKEN`, no dedicated secret. Without it the Kwanko
events are in `error` (missing credential).

## TradeTracker revenues (GOU-1381)

`TradeTrackerReportClient` (api, `services/metriks/affiliation`) calls the SOAP
API `https://ws.tradetracker.com/soap/affiliate`: `authenticate(customerID,
passphrase)` (the session is a cookie kept between the calls of a run),
`getAffiliateSites`, then `getConversionTransactions` for every affiliate site
(`registrationDate` window, `limit`/`offset` pages). It feeds
`affiliation.tradetracker.commission.{validated,pending}.{7d,28d}` and
`affiliation.tradetracker.sales.*`, included in the totals. `transactionStatus`
maps to validated (`accepted`), pending (`pending`, `onhold`) or declined
(`rejected`, `declined`); any other value, a missing commission, a SOAP fault or
a non-EUR currency is a network error (the fault text is never quoted).

The credentials are `metriks.affiliation.tradetracker.customer-id` and
`metriks.affiliation.tradetracker.passphrase`, defaulting to
`${affiliation-config.tradetracker-customer-id:}` and
`${affiliation-config.tradetracker-api-key:}` in
`api/src/main/resources/application.yml`: the feed secret
`API_TRADETRACKER_API_KEY`, no dedicated secret. Without them the TradeTracker
events are in `error` (missing credential).

## Tests

- `AffiliationNetworkTest` (network deduction) and
  `AffiliationClickStatsServiceIT` (real Elasticsearch through Testcontainers:
  mapping, window bounds, counts, top merchants, missing index) in
  `services/contribution`. The IT is not run by CI (no failsafe execution);
  run it with
  `mvn -pl services/contribution test -Dtest=AffiliationClickStatsServiceIT`.
- `AffiliationServiceTest` and `AffiliationControllerTest` in front-api.
- `AffiliationClickMetriksTest` and `MetriksControllerTest` in api.
