# Datasources Maintenance Batch 000

Date: 2026-05-24

Verticals: `air-conditioner`, `dishwasher`, `oven`

## TL;DR

- Maven validation: passed
- Verticals artifact install: passed
- API compile: passed
- Focused API test: `VerticalsGenerationControllerTest` passed
- Local loop: completed once on port `9069`
- Local boot caveat: Spring AI auto-configurations were excluded for validation because the local dependency graph has an `okio` binary mismatch unrelated to category aggregation.

## Product Count Delta

| vertical | before | after | delta |
|---|---:|---:|---:|
| air-conditioner | 1403 | 1523 | +120 |
| dishwasher | 3782 | 3775 | -7 |
| oven | 7263 | 6935 | -328 |

The negative deltas for `dishwasher` and `oven` came from the local full re-aggregation against the current YAML, not from approved removals in those files. Review before assuming the added mappings are the cause.

## Per-Vertical Changes

### air-conditioner

- Removed `Cdiscount FR` mapping: `GROS ELECTROMENAGER / CLIMATISEUR / CLIMATISEUR MOBILE | GROS ELECTROMENAGER>CLIMATISEUR>CLIMATISEUR MOBILE | CLIMATISEUR | CLIMATISEUR MOBILE`
- Rationale: leakage warning, split between `refrigerator` and `air-conditioner` at roughly 50% / 50%, above the 20% ambiguity threshold.

### dishwasher

- Added `Boulanger - R&eacute;f&eacute;rentiel produits`: `GROS ELECTROMENAGER | LAVE-VAISSELLE | LAVE-VAISSELLE ENCASTRABLE | LAVE-VAISSELLE ENCASTRABLE`
- Rationale: exact dishwasher category, volume 79 in-vertical, significant_terms score 1066.48, 33 unattached docs estimated.
- Added `Darty FR`: `GROS ELECTROMENAGER / LAVE-VAISSELLE / LAVE-VAISSELLE ENCASTRABLE | GROS ELECTROMENAGER / LAVE-VAISSELLE / LAVE-VAISSELLE ENCASTRABLE`
- Rationale: exact dishwasher category, volume 73 in-vertical, significant_terms score 3756.41, 8 unattached docs estimated.

### oven

- Added `Darty FR`: `GROS ELECTROMENAGER | GROS ELECTROMENAGER>FOUR>FOUR ENCASTRABLE | FOUR | GROS ELECTROMENAGER`
- Rationale: exact oven category, volume 110 in-vertical, significant_terms score 2037.68, 9 unattached docs estimated.
- Added `manomano.fr`: `ELECTROMENAGER | ELECTROMENAGER > GROS ELECTROMENAGER > FOUR ENCASTRABLE > FOUR CHALEUR TOURNANTE | FOUR CHALEUR TOURNANTE`
- Rationale: exact oven category, volume 158 in-vertical, significant_terms score 518.28, 73 unattached docs estimated.
- Added `manomano.fr`: `ELECTROMENAGER | ELECTROMENAGER > GROS ELECTROMENAGER > MINI FOUR > MINI FOUR GRILL | MINI FOUR GRILL`
- Rationale: exact mini-oven category, volume 68 in-vertical, significant_terms score 250.29, 28 unattached docs estimated.

## Fnac Parked

Fnac-derived strings stayed parked by user decision and by the `_default.yml` Fnac exclusion intent:

- `air-conditioner`: `PETIT MENAGER & CUISINE - FROID`, volume 76, significant_terms score 62.58.
- `dishwasher`: `PETIT MENAGER & CUISINE - LAVAGE`, volume 1277, significant_terms score 12915.47, leakage with `washing-machine`.
- `dishwasher`: `PETIT MENAGER & CUISINE LAVAGE`, volume 445, significant_terms score 7204.46.
- `oven`: broad Fnac cooking GEM/PEM strings stayed parked despite high volume because they are coarse merchant departments.

## Phase Z Status

- Added local stats endpoints:
  - `GET /{vertical}/datasources/stats/coverage`
  - `GET /{vertical}/datasources/stats/unmapped`
  - `GET /{vertical}/datasources/stats/leakage`
  - `GET /{vertical}/datasources/stats/significant`
- Added DTO records:
  - `DatasourceCoverageDto`
  - `UnmappedCategoryDto`
  - `LeakageWarningDto`
  - `SignificantCategoryDto`
- Added focused controller tests in `VerticalsGenerationControllerTest`.

## Endpoint Smoke Test

| vertical | coverage | unmapped | leakage | significant |
|---|---:|---:|---:|---:|
| air-conditioner | 5 | 2 | 9 | 7 |
| dishwasher | 12 | 2 | 21 | 15 |
| oven | 14 | 10 | 36 | 33 |

## Open Questions

- Investigate why full local re-aggregation lowered `dishwasher` and `oven` counts despite additive YAML changes.
- Fix the local dependency graph conflict between legacy Amazon PA-API `okio` 1.x and Spring AI/OpenAI/Google clients requiring newer OkHttp/Okio.
