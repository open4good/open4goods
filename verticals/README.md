# open4goods-verticals

This module defines **product verticals** and their eco-score rules through YAML files. Vertical definitions describe how products are grouped and scored.

## Directory layout

```
verticals/
├─ src/main/resources/verticals  # YAML vertical definitions
├─ src/test/java                 # tests for vertical parsing
```

## Build and test

From the repository root build only this module:

```bash
mvn -pl verticals -am clean install
```

Run tests:

```bash
mvn test
```

## Vertical of a product from its merchant categories

`VerticalsConfigService.getVerticalForCategories(categoriesByDatasources, eprelGroup)` gives the vertical
of a product from its `categoriesByDatasources`: one category per datasource (the last one received),
upper-cased and without accents (`IdHelper.getCategoryName`). `TaxonomyRealTimeAggregationService` calls it
on every aggregation, then sets the `nature` of a product left without vertical (GOU-2233). A category
matches a vertical when it is listed in its `matchingCategories`, under its datasource first, then under
`all`. The flag `categoryVoteEnabled` of `_default.yml` chooses between two rules.

### Historical rule (`categoryVoteEnabled: false`, the default)

1. The first category of the map that matches a vertical gives it.
2. One category of any datasource containing one of the vertical's `excludingTokensFromCategoriesMatching`
   removes it, unless the product EPREL group is one of the vertical's `eprelGroupNames` (GOU-1700).

One misfiled merchant row decides in both directions: a PC case filed as a phone by one marketplace is a
smartphone, a real TV with one "PIECES DETACHEES" row loses its vertical. When categories match two
verticals, the result depends on the iteration order of the map.

### Vote between datasources (`categoryVoteEnabled: true`, GOU-2256)

Merged disabled (GOU-2256). It is switched on by its own PR after the before/after measure
([replay](#beforeafter-replay-gou-2317)). CTO decisions on GOU-2317: one vote per datasource name, another
vertical votes against, EPREL step 2 kept, a tie loses. The tie rule moves to "tie = historical rule" if the
measure of population (a) shows more real products lost than false positives removed; population (b) is measured
before the activation. Each datasource votes once on each candidate vertical V, a vertical matched by at
least one category of the product:

| Category of the datasource | Vote on V |
|---|---|
| contains one of V's `excludingTokensFromCategoriesMatching`, or a `sparePartCategoryMarkers` or `accessoryCategoryMarkers` marker of `_default.yml` (substrings) | against |
| matches V (and carries none of these markers) | for |
| matches another vertical | against |
| matches no vertical, or is blank | abstains |

Decision:

1. A candidate whose `eprelGroupNames` contain the product EPREL group wins, whatever the votes (GOU-1700).
   EPREL still never assigns a vertical that no category matches.
2. When the EPREL group belongs to a vertical that no category matches, no vertical: the categories
   contradict EPREL, and the aggregation keeps an EPREL-confirmed current vertical instead of moving the
   product to the vertical of the categories.
3. Otherwise V wins when it has strictly more votes for than against. On a tie V loses.

At most one vertical can win: a datasource voting for W votes against V, so both winning would need
`for(V) > against(V) >= for(W) > against(W) >= for(V)`. The result therefore does not depend on the order
of the map. The aggregation keeps its EPREL fallback: a product whose EPREL group confirms its current
vertical keeps it when the vote gives nothing.

The vote reuses the markers of the nature (GOU-2233) without a second list, but not its
`natureNeutralCategoryFragments`: for the nature, a PC component is not a spare part, whereas for the vote
`INFORMATIQUE > COMPOSANT - PIECE DETACHEE > ...` is what says the product is not a phone or a fridge.

PC component markers (GOU-2317): `BOITIER PC`, `COMPOSANT PC`, `COMPOSANTS PC` and `INFORMATIQUE | COMPOSANTS |`
are in `sparePartCategoryMarkers` for the vote, and in `natureNeutralCategoryFragments`, written identically, so
that the nature removes them before it searches the markers: the nature does not change (`PcComponentMarkersTest`
compares it with and without them on every real category, every `matchingCategories` entry and each marker next to
every other marker and fragment). There is no generic `COMPOSANT` token. The historical rule does not read these
lists.

Choices open at review:

- **Another vertical votes against.** The card only counts marked categories against V. Counting the votes
  for another vertical makes the winner unique. Alternative: they abstain, and a tie between verticals is
  broken by the number of votes for, then by id.
- **One vote per datasource name.** The legacy and current names of one merchant (`darty.com` and
  `Darty FR`) are two voters. Alternative: one vote per distinct category.
- **The vote moves the severity in both directions.** Compared with the historical rule:
  - *looser on excluding tokens*: one row with an excluding token (`ACCESSOIRE`, `PIECES DETACHEE`, `CABLE`,
    `MEUBLE`, `QUINCAILLERIE`...) was a veto, it is now one vote against. A phone case filed as a phone by two
    marketplaces and as `ACCESSOIRES TELEPHONE > COQUE` by a third goes from no vertical to `smartphones`; the
    same for a USB cable;
  - *stricter on nature markers that are not excluding tokens* (`PIECE DETACHEE` singular, `HOUSSE`,
    `PRODUIT(S) D'ENTRETIEN`, `PRODUIT ENTRETIEN`, the PC component markers): one such row did nothing, it is
    now one vote against, so a product with a single row for its vertical loses it on the tie. A real
    dishwasher with one dishwasher row and one `PIECE DETACHEE LAVE-VAISSELLE > PANIER` row goes from
    `dishwasher` to no vertical, which undoes the 4e.4 decision (GOU-2236) to keep the singular out of the
    excluding tokens; the same for a real TV or phone with one row for and one
    `INFORMATIQUE > COMPOSANT - PIECE DETACHEE > ...` or `BOITIER PC` row, or a fridge with one `HOUSSE` row.
    EPREL-confirmed products are not affected (step 1).

  Unchanged: the air conditioners under `ENTRETIEN DE LA MAISON > ...` (no marker, the row abstains) and the
  EPREL-confirmed fridges under `... > PORTE-SAVON`. The activation PR must rewrite
  `SparePartsCategoriesTest#singularSparePartCategoryKeepsTheVertical`, which asserts the historical result.
- **A tie loses.** One row against one row is no evidence. The historical rule already drops the vertical
  when that row carries an excluding token, but kept it when the row only carried a nature marker (see the
  stricter case above). Alternative: a tie falls back to the historical rule.
- **EPREL group of an unmatched vertical.** Step 2 keeps every EPREL-confirmed product in its vertical.
  The historical rule moves such a product to the vertical of its first matching category. Alternative:
  drop step 2 and accept that move.
- **The marker wins over the match.** The two `air-conditioner` categories of `ManoMano FR` that contain
  `QUINCAILLERIE` vote against their own vertical. They never assign it with the historical rule either.
- **PC component rows without `PIECE DETACHEE`** (`BOITIER PC ...`, `INFORMATIQUE ET BUREAU > COMPOSANT PC ...`,
  `INFORMATIQUE | COMPOSANTS | ...`) abstained with GOU-2256: the Zalman P10 PC case kept `refrigerator` from one
  row against two such rows. They vote against since GOU-2317 (PC component markers above).

Outside four populations, the vote gives the historical result:

- (a) products with a vertical and a category holding a nature marker that is not an excluding token of
  their vertical (`PIECE DETACHEE`, `HOUSSE`, `PRODUIT(S) D'ENTRETIEN`, `PRODUIT ENTRETIEN`, the PC component
  markers): they may lose it;
- (b) products without vertical and without EPREL confirmation, with a matched category and a category
  holding an excluding token: they gain the vertical when its votes for outnumber the votes against;
- (c) products whose categories match several verticals;
- (d) products whose EPREL group belongs to a vertical that no category matches, while a category matches
  another vertical: they get none (their current vertical stays when EPREL confirms it).

Tests: `CategoryVoteTest` (the eleven products of the card on their real categories, read in
`products-moustik` on 16/11/2026, and one case per rule), `PcComponentMarkersTest` (nature and historical rule
unchanged by the PC component markers), `CategoryVoteReplayTest` (this module) and
`TaxonomyRealTimeAggregationServiceCategoryVoteTest` (`api`).

### Before/after replay (GOU-2317)

`CategoryVoteReplayTest` replays the historical rule and the vote on an export of indexed products, without
Elasticsearch or network. For each vertical it gives the products of the historical rule, those of the vote, the
products lost and gained, and the differences classified in the populations (a) to (d) above (one product may
belong to several). It also counts the differences outside the four populations, which must be 0, and the products
whose EPREL group confirms their historical vertical and that lose it, which must be 0. In CI it runs on the eleven
products of the card (`src/test/resources/category-vote/real-categories-2026-11-16.json`) and writes nothing.

The replay measures the two rules on the categories, not the stored verticals: a stored vertical may be stale
(G.Skill RAM is stored in `dishwasher` but the historical rule already gives none), and the aggregation keeps an
EPREL-confirmed current vertical when the rule gives none.

**Export format.** One JSON object, product id to its `categoriesByDatasources` and its EPREL group
(`eprelDatas.productGroup`, missing or null without EPREL). Other fields are ignored. Keep the order of
`categoriesByDatasources` as indexed: the historical rule takes the first matching category.

```json
{
  "8800263650385": {
    "categoriesByDatasources": {
      "RueDuCommerce FR": "BOITIER PC",
      "rakuten.com-csv.yml": "INFORMATIQUE | COMPOSANTS | BOITIER"
    },
    "eprelGroup": null
  }
}
```

**Producing it with the Elasticsearch read connector** (agents CTO, Dev, QA, Growth; read only):

1. One `search` per sample, on `products-moustik`, with
   `"source": ["categoriesByDatasources", "eprelDatas.productGroup"]` and `size` up to 500 (20,000 documents per
   agent per hour). `categoriesByDatasources` and `eprelDatas` are not searchable (an `enabled: false` object, an
   unmapped one): select the products on `vertical` (`term`, or `must_not` + `exists` for the products without
   vertical), on exact `datasourceCategories` values (`terms`, a keyword: no substring search) or on `ids`.
2. Paginate only with a `sort` on a field that `get_mapping` flags `agg`, then `search_after`. `id` is not one
   (`index: false`, `doc_values: false`).
3. Save the text of each answer (`{"total": ..., "hits": [{"_id": ..., "_source": {...}}], ...}`) as a JSON file,
   then merge the hits, keyed by `_id`:

   ```bash
   jq -s 'map(.hits[]) | map({key: ._id, value: {categoriesByDatasources: ._source.categoriesByDatasources,
     eprelGroup: ._source.eprelDatas.productGroup}}) | from_entries' answers/*.json > export.json
   ```

**Running the replay** (absolute paths: the test runs in `verticals/`). It writes the Markdown report to
`categoryvote.replay.output` (default `verticals/target/category-vote-replay.md`; its directory must exist) and
nothing else:

```bash
mvn --offline -pl verticals -am test -Dtest=CategoryVoteReplayTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Dcategoryvote.replay.input=/abs/path/export.json -Dcategoryvote.replay.output=/abs/path/report.md
```

The report has a table by vertical, a table by change (historical vertical, voted vertical) and the list of the
differences with their populations, ready for the activation PR.

## Contributing new verticals

Contributions are welcome! Add new YAML files under `src/main/resources/verticals` and open a pull request. For project-wide details see the [root README](../README.md).
