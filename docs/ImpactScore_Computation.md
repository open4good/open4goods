# Impact Score (Eco-Score) Computation Methodology

This document details the computation of the **Impact Score** (often referred to as `ECOSCORE` in the codebase), explaining the steps from raw attribute data to the final aggregated score.

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
   $$ Score = Normalized \times DEFAULT_MAX_RATING $$
   _(Default Max Rating is 5.0)_

   The score is clamped to be between 0 and MaxRating.

#### Implications

- A product with an **Average** value gets **2.5 / 5**.
- A product at **$\mu + 2\sigma$** (top ~2.5% of random normal dist) gets **5 / 5**.
- A product at **$\mu - 2\sigma$** (bottom ~2.5%) gets **0 / 5**.

#### Low-entropy fallback: Percentile Scoring

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

The percentile is then scaled to the same 0–5 range and clamped as usual.

### 3. Impact Score Aggregation

**Service**: `EcoScoreAggregationService`
**Method**: `generateEcoScore`

The final Impact Score is calculated as the **Coefficiented Sum** of the relativized sub-scores.

#### Formula

$$ ImpactScore = \sum (SubScore_i \times Weight_i) $$

- **$SubScore_i$**: The relativized score (0-5) for criteria $i$.
- **$Weight_i$**: The weighting coefficient defined in the Vertical Configuration (`vConf`).

#### Scale of the Impact Score

Important: The **Impact Score is NOT relativized** again. Its scale depends entirely on the sum of the weights.

- If $\sum Weights = 1$, the Impact Score is on a **0-5** scale.
- If $\sum Weights = 4$, the Impact Score is on a **0-20** scale.
- If $\sum Weights = 20$, the Impact Score is on a **0-100** scale.

_Example Observation_: Scores between **6.5 and 14** suggest a configuration where the weights sum to approximately **4**, targeting a **0-20 scale**.

- Average product: $2.5 \times 4 = 10$.
- Range $6.5 \dots 14$ represents products within roughly $\pm 1 \sigma$ of the mean.

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
  - Vertical YAML files (define weights).
  - `impactScoreConfig.minDistinctValuesForSigma` (minimum distinct values required to keep sigma scoring).

## Potential Improvements / Methodology Notes

1. **Implicit Scale**: The final score range varies if weights are changed. Normalizing weights to a fixed sum (e.g., 20) is recommended to ensure consistent scoring ranges.
2. **Dependency Order**: This computation assumes sub-scores are available and can be relativized. The `EcoScoreAggregationService` runs these logic steps generally in the `onProduct` (summation) and `done` (relativization) phases.
