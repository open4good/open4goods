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

### Historical rule (`categoryVoteEnabled: false`)

1. The first category of the map that matches a vertical gives it.
2. One category of any datasource containing one of the vertical's `excludingTokensFromCategoriesMatching`
   removes it, unless the product EPREL group is one of the vertical's `eprelGroupNames` (GOU-1700).

One misfiled merchant row decides in both directions: a PC case filed as a phone by one marketplace is a
smartphone, a real TV with one "PIECES DETACHEES" row loses its vertical. When categories match two
verticals, the result depends on the iteration order of the map.

### Vote between datasources (`categoryVoteEnabled: true`, the default since GOU-2317)

Merged disabled (GOU-2256), switched on by GOU-2317 after the before/after measure
([replay](#beforeafter-replay-gou-2317)). CTO decisions on GOU-2317: one vote per datasource name, another
vertical votes against, EPREL step 2 kept, a tie loses, moved to "tie = historical rule" if the measure showed
more real products lost than false positives removed. It did, through ties between verticals (population (c)):
step 4 below. Each datasource votes once on each candidate vertical V, a vertical matched by at least one
category of the product:

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
3. Otherwise V wins when it has strictly more votes for than against. At most one vertical can win: a
   datasource voting for W votes against V, so both winning would need
   `for(V) > against(V) >= for(W) > against(W) >= for(V)`.
4. When no vertical wins, a tie goes to the historical rule (GOU-2317): V wins when its votes for equal its
   votes against, none of these votes against comes from a marker or an excluding token, and the historical
   rule gives V. A tie with one marked vote against V loses.

Only step 4 depends on the order of the map, through the historical rule. The aggregation keeps its EPREL
fallback: a product whose EPREL group confirms its current vertical keeps it when the vote gives nothing.

The vote reuses the markers of the nature (GOU-2233) without a second list, but not its
`natureNeutralCategoryFragments`: for the nature, a PC component is not a spare part, whereas for the vote
`INFORMATIQUE > COMPOSANT - PIECE DETACHEE > ...` is what says the product is not a phone or a fridge.

PC component markers (GOU-2317): `BOITIER PC`, `COMPOSANT PC`, `COMPOSANTS PC` and `INFORMATIQUE | COMPOSANTS |`
are in `sparePartCategoryMarkers` for the vote, and in `natureNeutralCategoryFragments`, written identically, so
that the nature removes them before it searches the markers: the nature does not change (`PcComponentMarkersTest`
compares it with and without them on every real category, every `matchingCategories` entry and each marker next to
every other marker and fragment). There is no generic `COMPOSANT` token. The historical rule does not read these
lists.

Choices made at review (GOU-2317):

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
- **A tie with a marker against loses, a tie between verticals follows the historical rule** (GOU-2317). One
  marked row against one row for V is enough doubt: the historical rule already drops the vertical when that
  row carries an excluding token, and the false positives of the card (Lian-Li, ASRock) lose theirs this way.
  A tie between two verticals is a conflict of merchant mappings: Fnac files dishwashers under
  `PETIT MENAGER & CUISINE - LAVAGE`, a washing machine category, and air conditioners under
  `PETIT MENAGER & CUISINE - FROID`, a refrigerator one, while the long `Darty FR` categories are only mapped
  under `darty.com`. With "a tie loses", a real dishwasher with one Darty row and one Fnac row lost its
  vertical: 26 of 1,412 air conditioners and about 27 of 1,497 dishwashers of the measure. With step 4, 2 real
  products are lost on 10,412 (two air coolers with a misfiled PC component row, population (a)).
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
`products-moustik` on 16/11/2026, the excluding tokens of the measure on real products read on 23/11/2026, and
one case per rule), `PcComponentMarkersTest` (nature and historical rule
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

**Measure of the activation (23/11/2026, `products-moustik`).** Samples sorted on `_doc` (the first indexed
products, not a random draw): 1,500 products per vertical (all 1,412 air conditioners), and 500 products without
vertical per vertical with a `datasourceCategories` value of its `matchingCategories` (435 for dishwashers). On
both: 0 difference outside the populations, 0 EPREL-confirmed product lost.

- With vertical (10,412): 26 differences. 20 are right: 11 dishwashers filed as washing machines move to
  dishwasher, 1 dishwasher to washing-machine, 1 oven/dishwasher combo to dishwasher (EPREL `dishwashers2019`),
  1 refrigerator to air-conditioner, 3 PC components, 2 cleaning products and ASRock leave their vertical.
  2 real air coolers are lost (a misfiled PC component row), 1 dryer moves from refrigerator to washing-machine.
  The 3 others are signage displays or an interactive whiteboard (BenQ RE7503A, Samsung QM32C, LG 32SM5J) that
  the tv excluding tokens below remove from the historical rule, while the vote keeps them in `tv` (population
  (b)).
- Without vertical (3,407), population (b): 115 products gain a vertical, about 100 rightly (real phones that
  one `AUTRES ACCESSOIRES SMARTPHONE` row excluded, dishwashers, washing machines, ovens, 7 real TVs).

**Monitors and accessories of population (b)** (GOU-2317 review). Before the tokens below, 18 PC monitors,
signage displays or interactive whiteboards gained `tv` in the sample without vertical: Cdiscount
`TELEVISEUR LCD` / `TELEVISEUR LED` and Rakuten `HIFI | TELEVISION` outvoted Fnac `ACCESSOIRES POUR TELEVISEURS`.
`tv.yml` now excludes the merchant forms that name a monitor: Darty `MONITEUR SEMI PRO` and
`INFORMATIQUE / ECRAN / ECRAN PC`, GrosBill `ECRAN PC >`, `AFFICHAGE DYNAMIQUE >` and `AFFICHAGE COLLABORATIF >`,
Carrefour `> ECRAN PC`, Inmac `IT | MONITEURS | MONITEUR `, Joom and Back Market `MONITORS`. Measured on the same
two samples:

- 14 of the 18 no longer gain `tv`. 4 signage displays keep it (Philips 65BDL3117P and 43BDL3117P, LG 55EF5K-L,
  Sony FW-55BZ40H): their `tv` rows come twice, from `cdiscount.com` and `Cdiscount FR` or from `rueducommerce.fr`
  and `RueDuCommerce FR`, and no remaining row of theirs names a monitor without naming real TVs too.
- With vertical: 0 real TV lost, under the vote and under the historical rule. 6 monitors, signage displays or
  whiteboards leave `tv` under both rules, and the 3 above under the historical rule only. A Philips signage
  display of the sample without vertical leaves `tv` too. No other vertical changes.
- Rejected forms, each costing a real TV under the historical rule, where an excluding token removes the
  vertical: bare `MONITEUR` (alternate `TV & MONITEUR > TV` is a tv category, Inmac `IT | MONITEURS | TV HOTEL`),
  bare `ECRAN PC` (RueDuCommerce files Fox Spirit TVs there), Cdiscount `ECRAN ORDINATEUR` (LG 43LT340C hotel
  TV), icecat `ECRANS PLATS DE PC` (LG 28TK410V monitor TV), `AFFICHAGES DE MESSAGES` and
  `ECRAN D'AFFICHAGE DYNAMIQUE` (Samsung and Sony business TVs), Rakuten `INFORMATIQUE | ECRAN` (a Silver TV).

`smartphones` gained 51 products, 36 real phones. `smartphones.yml` excludes `BATTERIE EXTERNE` (a power bank
filed under Darty `TELEPHONE MOBILE`). Accepted residue, no safe token: a car charger, a phone bag and two cases,
and 10 GTINs shared by an old Nokia, BlackBerry or Sony phone and an iPhone spare part (`3663448...`). Rejected
forms: `CHARGEUR` (11 real phones in `products-moustik` carry a charger row: Redmi Note 15 sold "chargeur non
inclus") and `COQUE ET ETUI` (a real Xiaomi Mi 10).

## Contributing new verticals

Contributions are welcome! Add new YAML files under `src/main/resources/verticals` and open a pull request. For project-wide details see the [root README](../README.md).
