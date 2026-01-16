---
title: "Impact Score methodology"
description: "A structured view of criteria, weightings, and the statistical principles that power the Impact Score."
tags: ["impact-score", "methodology", "scoring"]
icon: "mdi-flask-outline"
weight: 20
updatedAt: "2026-02-12"
draft: false
---

# Impact Score methodology

This document explains **how each criterion is scored and combined**.
We use a mix of verified data, AI-assisted weighting, and statistical
normalisation to keep the score consistent across product categories.

## Scoring process

1. Collect structured sources (spec sheets, certification databases).
2. Apply criterion-specific scoring rules.
3. Weight the criteria based on relevance for the category.
4. Apply a data quality penalty if information is missing.

## Statistical safeguards

We use **sigma scoring** to avoid over-valuing outliers. When the available data
is too sparse, we fallback to percentile scoring so the ranking stays fair.

### What this means for you

You can compare products inside the same category with a consistent scale, even
when some attributes are incomplete.
