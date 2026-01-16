---
title: "Impact Score data sources"
description: "Inventory of the main data sources and validation rules used in Impact Score calculations."
tags: ["impact-score", "data", "sources"]
icon: "mdi-database-search-outline"
weight: 30
updatedAt: "2026-02-12"
draft: false
---

# Impact Score data sources

Impact Score relies on a **stack of structured sources**. Each source is tagged
with provenance and freshness metadata to keep the score auditable.

## Typical sources

- Manufacturer technical sheets.
- Public eco-databases (repairability, recyclability).
- Certification bodies and regulatory registers.

## Validation rules

- Data must be **traceable** (link or reference ID).
- If conflicting values exist, we keep the most recent **verified** source.
- Missing attributes trigger a **data quality penalty**.
