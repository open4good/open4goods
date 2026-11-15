# Ranking and listing disclosure (art. D111-6 C. conso)

A comparison site must publish, in a section reachable from every page, the
default ranking criteria for products and offers, the conditions for listing
merchants and whether a payment influences the ranking (article D111-6 of the
French consumer code). On nudger.fr this is the "Classement et referencement"
page (GOU-1995):

- route `ranking-and-listing`: `/classement-et-referencement` (fr-FR),
  `/ranking-and-listing` (en-US), in `frontend/shared/utils/localized-routes.ts`;
- page `frontend/app/pages/ranking-and-listing/index.vue`, texts under
  `rankingPolicy` in `frontend/i18n/locales/*.json`;
- linked from the footer (`TheMainFooterContent.vue`), from the sort control
  of listings (`CategoryResultsToolbar.vue`) and from the "Liens d'affiliation"
  paragraph of the legal notice (`frontend/server/utils/static-full-pages.ts`);
- listed in the sitemap through `LOCALIZED_ROUTE_PATHS`.

## The text describes the code

The page is a public statement: it must stay true to the code. Update its
texts and `RANKING_POLICY_UPDATED_AT` in the same pull request as any change
to one of these behaviours:

| Behaviour described | Code |
|---|---|
| Default listing sort (ImpactScore, descending) | `frontend/app/utils/_category-initial-sort.ts`, `CategoryPage.vue` (`PRIMARY_SORT_STRATEGY`) |
| Unrated products last, GTIN tie-break | `ProductController.sanitizeSort`, `SearchService.withStableTieBreak` (front-api) |
| Other sorts | `ProductDto.ProductDtoSortableFields`, `SearchService.resolveVerticalFields` |
| Price used for sorting (lowest, new and used) | `PriceAggregationService.computeMinPrice` (api) |
| Search relevance and suggestions | `SearchService.globalSearch`, `OFFER_NAMES_DENSITY_SCRIPT` |
| Highlighted offer and offer list order | `OfferFreshnessService.bestPrice`, `AggregatedPrices.sortedOffers`, `ProductHeroPricing.vue`, `ProductPriceSection.vue` |
| Alternatives and choice assistant | `ProductAlternatives.vue`, `NudgeToolWizard.vue` |
| Merchant sources (joined affiliate programmes) | `services/feedservice`, `feed.*` in `api/src/main/resources/application.yml` |
| Amazon search link (no offer only) | `AmazonSearchLinkService` (front-api) |

Any change that would let a commission, an affiliate programme or a merchant
change an order (a "sponsored" slot, a boost, a sort on `compensation`) changes
the legal text: raise it to the CTO before merging.
