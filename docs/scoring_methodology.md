# Metric Impact Scoring Methodology

## Overview

The Nudge project calculates environmental impact scores (Eco-score) by aggregating various product attributes (e.g., Warranty, Power Consumption). These raw attributes are normalized into a **0 to 5** score to allow comparison and aggregation.

## Methodology: Sigma Scoring (Statistical Normalization)

To ensure fair scoring across diverse product categories, we use a statistical approach based on **Standard Deviation ($\sigma$)**, also known as **Sigma Scoring**.

### The Problem with Min-Max Scoring

Previously, scores were calculated using a linear Min-Max scale:
$$ Score = \frac{Value - Min}{Max - Min} \times 5 $$

This method is highly sensitive to **outliers**.
_Example_: In a TV category, most products have a 2-year warranty. One exceptional product offers 12 years.

- **Result**: The "Standard" 2-year warranty receives a score of $\approx 0.5 / 5$ (Failing grade), merely because it is compared to the 12-year outlier.
- **Impact**: Users perceive standard products as "poor" quality, which is misleading.

### The Solution: Sigma Scoring

We calculate the **Mean ($\mu$)** and **Standard Deviation ($\sigma$)** of the distribution for each attribute within the batch.
We then define a "Normal Range" as:
$$ [\mu - 2\sigma, \mu + 2\sigma] $$

Scores are mapped linearly within this range:

- Values $\le \mu - 2\sigma$ receive **0 / 5**.
- Values $\ge \mu + 2\sigma$ receive **5 / 5**.
- Values at the Mean ($\mu$) receive **2.5 / 5**.

### Benefits

1.  **Robustness**: Outliers (e.g., the 12-year warranty) are capped at the maximum score (5/5) but do not stretch the scale for everyone else.
2.  **Representativity**: A product with "Average" performance receives an "Average" score (2.5), aligning with user expectations.
3.  **Automatic Tuning**: The scale adjusts automatically to the diversity (variance) of the products in the category.

## Technical Implementation

This logic is implemented in `AbstractScoreAggregationService.java`.
The variance is tracked incrementally in `Cardinality.java` to handle large batches efficiently.
