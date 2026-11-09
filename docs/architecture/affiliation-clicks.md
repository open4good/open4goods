# Outbound affiliation clicks

How a click on an offer button (`/contrib/<token>`) is recorded and reported in
Metriks, without personal data (GOU-1188).

## Flow

1. The product page links to `/contrib/<token>`. The token is the compressed
   JSON of a `ContributionVote`: merchant (`datasourceName`), target URL and,
   since GOU-1188, the product `gtin` (tokens without `gtin` stay valid).
2. The Nuxt route (`frontend/server/routes/contrib/[...token].ts`) refuses
   robots and requests without the `nudger_nav` navigation cookie (a site
   `Referer` alone is refused since GOU-1520), then calls front-api
   `GET /affiliations/redirect`, forwarding the optional
   `X-Affiliation-Origin` header (path of the originating page, read from the
   same-site `Referer`) and, since GOU-1828, the optional
   `X-Affiliation-Content` header (article or guide the offer was reached from,
   read from the `from` query parameter of the link, see below).
3. front-api `AffiliationService.trackRedirect` writes one `AffiliationClick`
   document and the legacy `ContributionVote` (without IP nor user agent), both
   fail-open: a persistence failure never blocks the redirect.
4. front-api answers `302` with `Cache-Control: no-store` (a `301` could be
   cached by the browser, and a cached click never reaches the server). The
   Nuxt route forwards that status to the browser; it also accepts `301` from
   an older front-api.
5. api `GET /api/metriks/affiliation` aggregates the `affiliation-clicks` index.

## Amazon search link (GOU-1396)

A product page **without any offer** shows a text link "Rechercher sur Amazon"
towards `https://www.amazon.fr/s?k={EAN}&tag=nudger-21`, through `/contrib/`
like any offer, so the click is an `AffiliationClick` (merchant `amazon.fr`,
target host `www.amazon.fr`). Compliance conditions come from the Amazon
Associates review in GOU-1389.

- front-api: `AmazonSearchLinkService` builds `offers.amazonSearchLink`
  (`/contrib/<token>`) only when the product has no **fresh** offer (the
  mapped `offersCount`, best price and `offersByCondition` all empty, see
  `OfferFreshnessService`: a product whose offers are all stale gets the link,
  exactly when the price block is empty) and its GTIN is digits only,
  has at least 8 significant digits and a valid GS1 check digit (normalised to
  GTIN-8, EAN-13 or GTIN-14). Configuration (not secret):
  `front.amazon-search-link.{enabled,tag,url-template,merchant}` in
  `front-api/src/main/resources/application.yml`. Amazon is never called: no
  price, no image. The link is not an offer: `offersCount`, ranking and the
  JSON-LD `offers` are unchanged.
- frontend: `ProductAmazonSearchLink.vue` renders the link (text only, no
  Amazon logo, `rel="sponsored nofollow noopener"`,
  `referrerpolicy="same-origin"`, new tab, plain anchor without prefetch) with
  the verbatim disclosure underneath: "En tant que Partenaire Amazon, je
  réalise un bénéfice sur les achats remplissant les conditions requises."
  It sits in the empty hero pricing panel, or in the price section when the
  hero panel is hidden (uncategorised product without offer), never both. The
  same disclosure is on the partners page (affiliation section) and in the
  legal notice.
- `/contrib/` (`frontend/server/routes/contrib/[...token].ts`): the redirect
  answers `Referrer-Policy: strict-origin-when-cross-origin` when the target
  is `https://amazon.fr` or one of its sub-domains
  (`server/utils/contrib-referrer-policy.ts`), so Amazon receives the origin
  `https://nudger.fr/` and never the page path. Reason: the Associates
  agreement §6(v) forbids hiding the site a click comes from, including
  through redirect links. Every other target keeps `no-referrer` (GOU-1184).
  The navigation-signal and robot guards of `/contrib/` are unchanged.

Prerequisite for production, outside the code: nudger.fr must be declared in
the Associates account with the `nudger-21` tag.

The `amazon` network in Metriks is handled by `AffiliationNetwork`, not here.

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
| `contentPath`| keyword   | Article or guide path (`/blog/...`, `/guides/...`), `null` when absent or invalid (GOU-1828) |
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

### Article or guide of the click (GOU-1828)

`/contrib/` links only exist on product pages, so a visit coming from an
article (`/blog/...`) or a guide (`/guides/...`) is recorded with the product page
as `originPath`. The frontend therefore adds the article of context to the
offer link, and only when there is one: `/contrib/<token>?from=<path>`. The
Nuxt route forwards it to front-api as `X-Affiliation-Content: <path>`
(`X-Affiliation-Origin` is unchanged).

front-api (`AffiliationService.sanitiseContentPath`) keeps the value only when
it follows the `X-Affiliation-Origin` rules above **and** starts with `/blog/`
or `/guides/`; anything else is ignored (`null`) and never blocks the
redirect, which stays a `302`.

The field was added to an existing index: `createIndex = true` only maps a new
index, and the dynamic mapping would turn the first value into `text` plus
`.keyword`. front-api therefore runs `AffiliationClickMappingService` at
startup (`AffiliationClicksMappingRunner`, background thread after
`ApplicationReadyEvent`): it writes the `AffiliationClick` mapping onto the
existing index (additive, idempotent; creates the index when it is missing)
and checks that `contentPath` is a `keyword`. It is fail-open: an error (for
example Elasticsearch unreachable, or `contentPath` already mapped with
another type) is one `ERROR` line starting with `affiliation-clicks mapping`
and the next startup retries; neither the startup nor the redirects are
affected. Toggle: `front.affiliation-clicks-mapping.enabled` (default `true`,
`FRONT_AFFILIATION_CLICKS_MAPPING_ENABLED`; `false` in the `local` profile).
Existing documents are not rewritten: clicks stored before have no
`contentPath`.

```bash
grep 'affiliation-clicks mapping' front-api.log
```

If `contentPath` was ever mapped dynamically (a click with the header written
before the update ran), the update keeps failing: the index must then be
reindexed with the right mapping, which is a separate, approved operation.

The documents already stored in `contribution-votes` with IP and user agent
were not touched by this change. Their purge (board approval 2f4935d1) runs in
front-api at startup, see `ContributionVoteIpUaPurgeService` and the front-api
README, "Contribution votes ip/ua purge" (GOU-1230).

## Metriks events

Added after the commission events of `/api/metriks/affiliation`:

- `affiliation.<awin|effiliation|kwanko|tradetracker|amazon|other|total>.clicks.<7d|28d|prev7d|prev28d>`, unit `count`,
  windows `[D-7, D-1]`, `[D-28, D-1]`, `[D-14, D-8]` (`prev7d`) and `[D-56, D-29]` (`prev28d`) in Europe/Paris by
  click time, with the same window `params` as the commission events (no `currency`) and names suffixed
  " (7 j préc.) " / " (28 j préc.) " for the previous windows (GOU-1975);
- `affiliation.clicks.top_merchants.28d`: `value` is the number of rows,
  `params.rows` lists `merchant`, `network` and `clicks`, top 20, most clicked
  first (ties ordered by merchant);
- `affiliation.clicks.top_origins.28d` (GOU-1828), window `[D-28, D-1]`:
  `value` is the number of rows, `params.rows` lists `origin` and `clicks`,
  top 50, most clicked first (ties ordered by `origin`).

Attribution rule of `top_origins`: the `origin` of a click is its
`contentPath` when it has one, otherwise its `originPath`, otherwise
`(none)`. It is read without script, by a `terms` aggregation on
`contentPath` (size 50) and a `filter` aggregation (`must_not exists
contentPath`) holding a `terms` aggregation on `originPath` (size 50,
`missing: "(none)"`), merged in Java. Both sets of clicks are disjoint, so the
top 50 of their union is exact; a path present in both (a `/blog/` page as the
`originPath` of a click without `contentPath`) is summed. On an index whose
mapping does not hold `contentPath` yet, every click counts by `originPath`.

One Elasticsearch request (date range query, `terms`, `multi_terms` and
`filter` aggregations, no document fetched) feeds the `7d` and `28d` events and the tops; one more request
(`AffiliationClickStatsService.count`, a `terms` aggregation) feeds each previous window. A network without click
reports `0`. When Elasticsearch cannot answer (unreachable, missing index), all
click events are in `error`, never `0`; the commission events are unaffected.

api and front-api must use the same Elasticsearch cluster
(`spring.elasticsearch.uris` in open4goods-config: `localhost:9200` on the beta
box for both, the same production node for both).

### Event contract of `/api/metriks/affiliation` (GOU-1975)

Windows (D = today, Europe/Paris): `7d` = `[D-7, D-1]`, `28d` = `[D-28, D-1]`,
`prev7d` = `[D-14, D-8]`, `prev28d` = `[D-56, D-29]`. The previous windows are
read fresh at each call, never from an old run: statuses (pending, validated,
declined) change with time.

| Events | Ids | Count |
|---|---|---|
| Commissions and sales | `affiliation.<awin\|effiliation\|kwanko\|tradetracker\|total>.<commission.validated\|commission.pending\|sales>.<7d\|28d\|prev7d\|prev28d>` | 5 x 3 x 4 = 60 |
| Clicks | `affiliation.<awin\|effiliation\|kwanko\|tradetracker\|amazon\|other\|total>.clicks.<window>` | 7 x 4 = 28 |
| Tops | `affiliation.clicks.top_merchants.28d`, `affiliation.clicks.top_origins.28d` | 2 |

Each commission network is read once, over the 56 days `[D-56, D-1]`, and the
four windows are derived from that single read. The Awin transactions API
accepts 31 days at most per request: `AwinReportClient` splits the range into
consecutive chunks of 31 days (each chunk ends one second before the next
starts: no gap, no duplicate at the bounds). Effiliation (90 days), Kwanko and
TradeTracker (paginated) read 56 days in one go. Error rules are unchanged: a
failing network (or a failing chunk) puts all its windows in `error`, a total
is in `error` as soon as one network is, a missing figure is never `0`.

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

- `AffiliationNetworkTest` (network deduction),
  `AffiliationClickStatsServiceIT` (real Elasticsearch through Testcontainers:
  mapping, window bounds, counts, top merchants, top origins, missing index)
  and `AffiliationClickMappingServiceIT` (`contentPath` added as `keyword` to
  an index created without it, idempotence, conflict) in
  `services/contribution`. The ITs are not run by CI (no failsafe execution);
  run them with
  `mvn -pl services/contribution test -Dtest='AffiliationClick*IT'`.
- `AffiliationServiceTest`, `AffiliationControllerTest`,
  `AffiliationClicksMappingRunnerTest` and `AmazonSearchLinkServiceTest` in
  front-api.
- `server/routes/contrib/[...token].spec.ts` and
  `server/utils/contrib-referrer-policy.spec.ts` (Referrer-Policy per target)
  in the frontend.
- `AffiliationClickMetriksTest` and `MetriksControllerTest` in api.
