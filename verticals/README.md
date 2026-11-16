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

Proposal merged disabled, to be validated by the CTO at review. It is switched on by its own PR after the
before/after measure. Each datasource votes once on each candidate vertical V, a vertical matched by at
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

Choices open at review:

- **Another vertical votes against.** The card only counts marked categories against V. Counting the votes
  for another vertical makes the winner unique. Alternative: they abstain, and a tie between verticals is
  broken by the number of votes for, then by id.
- **One vote per datasource name.** The legacy and current names of one merchant (`darty.com` and
  `Darty FR`) are two voters. Alternative: one vote per distinct category.
- **A tie loses.** One row against one row is no evidence; the historical rule already drops the vertical
  when that row carries an excluding token. Alternative: a tie falls back to the historical rule.
- **EPREL group of an unmatched vertical.** Step 2 keeps every EPREL-confirmed product in its vertical.
  The historical rule moves such a product to the vertical of its first matching category. Alternative:
  drop step 2 and accept that move.
- **The marker wins over the match.** The two `air-conditioner` categories of `ManoMano FR` that contain
  `QUINCAILLERIE` vote against their own vertical. They never assign it with the historical rule either.
- **Known limit.** PC component categories without marker (`BOITIER PC ...`,
  `INFORMATIQUE ET BUREAU > COMPOSANT PC ...`) abstain. A PC case with two phone rows and one marked
  component row stays a smartphone.

Outside four populations, the vote gives the historical result:

- (a) products with a vertical and a category holding a nature marker that is not an excluding token of
  their vertical (`PIECE DETACHEE`, `HOUSSE`, `PRODUIT(S) D'ENTRETIEN`, `PRODUIT ENTRETIEN`): they may lose it;
- (b) products without vertical and without EPREL confirmation, with a matched category and a category
  holding an excluding token: they gain the vertical when its votes for outnumber the votes against;
- (c) products whose categories match several verticals;
- (d) products whose EPREL group belongs to a vertical that no category matches, while a category matches
  another vertical: they get none (their current vertical stays when EPREL confirms it).

Tests: `CategoryVoteTest` (this module) and `TaxonomyRealTimeAggregationServiceCategoryVoteTest` (`api`).

## Contributing new verticals

Contributions are welcome! Add new YAML files under `src/main/resources/verticals` and open a pull request. For project-wide details see the [root README](../README.md).
