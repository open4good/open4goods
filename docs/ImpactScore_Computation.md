# Impact Score (Eco-Score) Computation Methodology

This document details the computation of the **Impact Score** (often referred to as `ECOSCORE` in the codebase), explaining the steps from raw attribute data to the final aggregated score.

> This page describes the current method (V1). The versioned V2 method
> (real coverage, no imputation, per-vertical switch) is designed in
> [impactscore/v2-design.md](impactscore/v2-design.md).

## Overview

The Impact Score is a **Weighted Sum** of **Relativized Sub-scores**.

- **Sub-scores**: Individual criteria (e.g., Repairability, Warranty) are first normalized to a **0-5 scale** (by default) using **Sigma Scoring**. When a distribution is too discrete (few distinct values), the system automatically switches to **Percentile Scoring** to preserve meaningful separation.
- **Impact Score**: These normalized sub-scores are summed according to configured weights to produce the final Impact Score.

## Step-by-Step Computation

### 1. Data Collection & Cardinality Accumulation

**Service**: `AbstractScoreAggregationService` (and subclasses)

As products are processed in a batch, the system tracks the statistical distribution of every scores.
For each sub-score (attribute), we compute:

- **Count**: Number of products.
- **Sum**: Sum of raw values.
- **Min / Max**: Range of raw values.
- **Mean ($\mu$)**: Average value.
- **Standard Deviation ($\sigma$)**: Measure of data dispersion (variance).

_Code Reference_: `incrementCardinality` in `AbstractScoreAggregationService.java`.

### 2. Sub-score Relativization (Sigma Scoring)

**Service**: `AbstractScoreAggregationService`
**Method**: `relativize(Double value, Cardinality abs)`

Raw attribute values are converted to a standard rating (default **0 to 5**).
This uses a **Sigma (Standard Deviation)** approach to be robust against outliers.

#### Formula

We define a "Normal Range" of **$\pm 2\sigma$** around the Mean ($\mu$).

1. **Calculate Bounds**:
   $$ LowerBound = \mu - (2 \times \sigma) $$
   $$ UpperBound = \mu + (2 \times \sigma) $$

2. **Normalize (0 to 1)**:
   $$ Normalized = \frac{Value - LowerBound}{UpperBound - LowerBound} $$

3. **Scale and Clamp (0 to MaxRating)**:
   $$ Score = Normalized \times DEFAULT*MAX_RATING $$
   *(Default Max Rating is 5.0)\_

   The score is clamped to be between 0 and MaxRating.

#### Implications

- A product with an **Average** value gets **2.5 / 5**.
- A product at **$\mu + 2\sigma$** (top ~2.5% of random normal dist) gets **5 / 5**.
- A product at **$\mu - 2\sigma$** (bottom ~2.5%) gets **0 / 5**.

#### Low-entropy fallback: Percentile Scoring

> **Inactive today.** This fallback only applies to attributes without
> `scoring.normalization.method`. Every scored attribute configures one
> (`SIGMA`, `MINMAX_FIXED` or `MINMAX_OBSERVED`), and no vertical sets
> `minDistinctValuesForSigma`.

When a criterion has too few distinct values (e.g., power standby reported as only 0 W / 0.5 W / 2 W), sigma scoring collapses values near the mean. In that case, the system switches to **percentile scoring** to restore ranking separation.

**Rule (configurable)**:

```
if distinctValues < impactScoreConfig.minDistinctValuesForSigma
  use percentile scoring
else
  use sigma scoring
```

**Percentile formula** (mid-rank):

$$ Percentile = \frac{CountBelow + 0.5 \times CountAt}{TotalCount} $$

The percentile is then scaled to the same 0-5 range and clamped as usual.

### 3. Impact Score Aggregation

**Service**: `EcoScoreAggregationService`
**Method**: `generateEcoScore`

The final Impact Score is calculated as the **Coefficiented Sum** of the relativized sub-scores.

#### Formula

$$ ImpactScore = \sum (SubScore_i \times Weight_i) $$

- **$SubScore_i$**: The relativized score (0-5) for criteria $i$.
- **$Weight_i$**: The weighting coefficient defined in the Vertical Configuration (`vConf`).

#### Scale of the Impact Score

The **Impact Score is NOT relativized** again. Weights are normalized so that
they sum to 4 (`w * 4 / sum(w)`), so the Impact Score is always on a **0-20**
scale, whatever the configured weights. `ECOSCORE.relativ.value` is the same
score on 0-5 (`absolute / 4`).

`ECOSCORE` is a reserved name: an attribute cannot declare it in
`participateInScores`. A composite with that name would replace the weighted
0-20 score with a 0-5 sub-score (this happened to air conditioners through
`REFRIGERANT_GWP`). `VerticalYamlValidationTest` rejects it in every vertical
of the repository; at runtime `ParticipatingScoreHelper` ignores it with an
error log, so that a configuration override cannot stop a vertical from being
scored.

### 4. Ranking

`Score.ranking` is the ascending position of a **real** score in the scored
batch: 0 is the lowest score and `absolute.count - 1` the highest. Virtual
(imputed) scores are not ranked, so `ranking` and `absolute.count` describe the
same population. The frontend shows:

- for the Impact Score, "better than X %" with `X = ranking / (count - 1)`;
- for a criterion, a position from the best product, `count - ranking`.

`EcoScoreRanking.globalPosition` already counts from the best product (1 = best).

### 5. Persistence after a batch

`BatchService.batch(vertical)` re-indexes every product of the vertical but only
scores the products that have offers and are not excluded. After scoring:

- excluded products and products unmatched from the vertical lose all their
  scores and their ranking;
- products without offers keep their last score (they stay in the sitemap,
  GOU-1744). If their Impact Score is a composite, which only the defect above
  produces, it is recomputed from the sub-scores they hold, with the ranking of
  `EcoScoreRanking`; it is removed only when it cannot be recomputed;
- if scoring throws, nothing is indexed for that vertical;
- if an automatic check below is blocking, nothing is indexed for that vertical either.

### 6. Automatic checks after each batch (GOU-1895)

`ImpactScoreBatchGuard` runs after scoring and before indexation, per vertical, and compares with the previous
accepted batch of the same vertical (kept in memory: after a restart the first batch only gets the absolute
checks; a blocked batch never becomes the baseline). Thresholds live under `impactScoreGuard` in
`application.yml` (`ApiProperties`), with `defaults` and per-vertical overrides in `verticals.<id>`.

| Rule | Severity | Condition | Property (default) |
|------|----------|-----------|--------------------|
| `BOUNDS` | blocking | ImpactScore outside [0, 20] | fixed |
| `RELATIV` | blocking | `relativ` != `value / 4` | fixed |
| `COMPOSITE` | blocking | `aggregates` not empty (cannot happen since lot 0) | fixed |
| `SPREAD` | blocking | p95 - p5 under the floor (compressed scale, defect B1), if at least `minProductsForDistribution` rated | `minSpread` (1.0 point) |
| `MEDIAN_SHIFT` | warning | median moves by more than X points | `maxMedianShift` (2.0) |
| `COVERAGE_SHIFT` | warning | mean real-weight coverage (`1 - imputed share`, V1 and V2) moves by more than Y | `maxCoverageShift` (0.10) |
| `RATED_DROP` / `SCORED_DROP` | warning | rated or scored count drops by more than Z | `maxCountDrop` (0.20) |
| `EPREL_DROP` | warning | EPREL-matched count drops by more than Z | `maxEprelDrop` (0.20) |

Starting values are conservative and to be tuned per vertical from the GOU-1876 distributions;
`impactScoreGuard.enabled=false` switches the checks off.

Alert: one structured `ERROR` log line per violation (`impactscore_guard vertical=… severity=… rule=… message=…
action=indexation_blocked|none`), Micrometer gauge `impactscore.guard.violations{vertical,severity}`, and the
`impactScoreGuard` health component (`/actuator/health/impactScoreGuard`, status `ALERT` when blocking, `WARN`
otherwise, never `DOWN`, so the aggregate health and the probes are unchanged). A failure of the guard itself is
logged and does not block the indexation.

## Source Code pointers

- **Base Logic**: `api/src/main/java/org/.../AbstractScoreAggregationService.java`
  - Handles `relativize` logic (Sigma calculation).
  - Manages `Cardinality` statistics.
- **Aggregation Logic**: `api/src/main/java/org/.../EcoScoreAggregationService.java`

  - Implements `generateEcoScore`.
  - Performs the weighted sum.
  - Skips re-relativization of the final EcoScore.

- **Configuration**:
  - `StandardiserService.DEFAULT_MAX_RATING` (Default: 5.0).
  - Vertical YAML files (define weights). See [Verticals Configuration](VerticalsConfiguration.md) for details on file structure (`verticals/*.yml` and `verticals/impactscores/*.yml`).
  - `impactScoreConfig.minDistinctValuesForSigma` (minimum distinct values required to keep sigma scoring).

## Potential Improvements / Methodology Notes

1. **Dependency Order**: This computation assumes sub-scores are available and can be relativized. The `EcoScoreAggregationService` runs these logic steps generally in the `onProduct` (summation) and `done` (relativization) phases.
