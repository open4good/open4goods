# Recreated products and lost verticals (GOU-1700)

This runbook counts and repairs products damaged by the realtime aggregation
before the GOU-1700 fix. Read-only steps can run at any time. Write steps run
only after the fix is deployed, otherwise the next feed run damages the
products again. They are run by Ops on CTO request, never by the developer.

## Symptoms

| Damage | Visible on `/api/products/{gtin}` | Cause |
| --- | --- | --- |
| Recreated product | Recent `creationDate`, no `eprel` nor `icecat.biz` in `datasourceCodes`, empty `scores`, `excluded: true`, shorter slug (301) | An offer fragment did not find the existing document: the lookup used the raw feed GTIN string (`07333394145372`) instead of the numeric id, or the multi-get item failed. `DataFragmentStoreService` then built a `new Product()` from that single fragment and indexed it over the existing one. |
| Lost vertical | `vertical: null` while the `eprel` source is present, root URL `/{gtin}-{slug}` (301), out of the sitemap | `VerticalsConfigService.getVerticalForCategories` drops the vertical when any category of any datasource contains an excluding token (`ACCESSOIRE`, `MEUBLE`, `CABLE`, `QUINCAILLERIE`), for example a marketplace row with a wrong GTIN categorised as a shoe-care accessory, or when no category matches any more. |

Since the fix, a vertical whose EPREL group (`eprelGroupNames`) matches the
product EPREL `productGroup` is neither dropped by an excluding token nor by a
missing category match. A failed lookup skips the fragment instead of
creating a product.

## Count the damaged products (read only)

`$ES` is the production Elasticsearch endpoint, the index is
`products-moustik`. `eprelDatas`, `datasourceCodes` and
`categoriesByDatasources` are not indexed, so the queries read `_source`
through runtime fields: run them once, not in a loop.

### Products without vertical but with an EPREL group

```bash
curl -s "$ES/products-moustik/_search" -H 'Content-Type: application/json' -d '{
  "size": 0,
  "track_total_hits": true,
  "runtime_mappings": {
    "eprel_group": {
      "type": "keyword",
      "script": "def e = params._source.eprelDatas; if (e != null && e.productGroup != null) { emit(e.productGroup); }"
    }
  },
  "query": { "bool": {
    "must_not": [ { "exists": { "field": "vertical" } } ],
    "filter": [ { "exists": { "field": "eprel_group" } }, { "range": { "offersCount": { "gt": 0 } } } ]
  } },
  "aggs": { "by_group": { "terms": { "field": "eprel_group", "size": 50 } } }
}'
```

Only the groups listed in a vertical `eprelGroupNames` (for example
`washingmachines2019`, `ovens`, `dishwashers2019`) can be repaired by the
sanitisation below.

### Products created since 2026-11-04 that were already known

The overwritten document keeps no trace of its past, so "already known" comes
from the weekly product backup (`/diskb/open4goods/backup/products`,
`products-backup-*.gz`, one JSON product per line). Each backup replaces the
previous one: first read the date in `products-backup-manifest.json`. If the
backup is later than 2026-11-04, the recreated documents in it carry their new
`creationDate` and are filtered out: use an older off-host copy, or fall back to
the GTINs of Search Console and of a `sitemap/fr/product-pages.xml` taken before
2026-11-04.

```bash
# 1. Ids created since 2026-11-04 (creationDate is epoch millis, mapped as long), with offers
curl -s "$ES/products-moustik/_search?scroll=2m" -H 'Content-Type: application/json' -d '{
  "size": 5000, "_source": false,
  "query": { "bool": { "filter": [
    { "range": { "creationDate": { "gte": 1793750400000 } } },
    { "range": { "offersCount": { "gt": 0 } } }
  ] } }
}' > created-page-1.json
# Follow the scroll_id until no hit is returned, then:
jq -r '.hits.hits[]._id' created-page-*.json | sort -u > created-ids.txt

# 2. Ids of the backup taken before 2026-11-04
zcat /diskb/open4goods/backup/products/products-backup-*.gz | jq -r 'select(.creationDate < 1793750400000) | .id' | sort -u > known-ids.txt

# 3. Recreated products
comm -12 created-ids.txt known-ids.txt | tee recreated-ids.txt | wc -l
```

Expected among them: `7333394145372` (Electrolux E62LB200S) and
`8690842629433` (Beko BDFN26441S).

## Repair (after the fix is deployed)

`$API` is the admin API (`https://api.nudger.fr`), each call carries
`-H "Authorization: $O4G_INTERNAL_API_KEY"` as described in
[api/AGENTS.md](../../api/AGENTS.md).

1. Lost verticals: run the sanitisation of each vertical returned by the
   `by_group` aggregation, one at a time, outside the 13:00 batch:
   `curl -X POST "$API/aggregate/verticals/washing-machine"`, then `oven`,
   and so on. It reloads the products whose categories match the vertical,
   restores the vertical when the EPREL group confirms it and regenerates the
   slug. Check `8059019130255` (Hoover) and `8690842382758` (Leisure).
2. A product still without vertical after step 1 has no category matching its
   vertical any more. Read its categories with
   `curl "$ES/products-moustik/_doc/{gtin}?_source_includes=categoriesByDatasources"`
   and ask the CTO to add the category to the vertical `matchingCategories`
   (no manual write of `vertical`).
3. Recreated products, for each id of `recreated-ids.txt`:
   `POST $API/completion/eprel/gtin/{gtin}`, then
   `POST $API/completion/icecat/gtin/{gtin}`, then
   `POST $API/completion/resources/gtin/{gtin}`, then
   `POST $API/aggregate/gtin/{gtin}` (recomputes `excluded` and the slug).
   Run them sequentially, with a pause between products.
4. Scores are rebuilt by the 13:00 batch of the vertical, or on request with
   `POST $API/batch/{vertical}`. Do not run it while the nightly batch runs.
5. Check on `/api/products/{gtin}`: `vertical` set, `eprel` and `scores`
   present, `excluded: false`, then the presence of the URL in
   `sitemap/fr/product-pages.xml` after the next sitemap generation.

Old URLs keep their 301 by GTIN; the slug may differ from the one Google
indexed, see the slug stability rule proposed in GOU-1700.
