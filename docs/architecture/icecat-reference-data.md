---
title: "Icecat Reference Data"
normative: false
audience: PROJECT_SCOPED
---

# Icecat Reference Data

The Icecat integration stores reference metadata in Elasticsearch indexes managed by
`services/icecat`.

## Bulk Files

`CategoryFeaturesList.xml.gz` is the old large Icecat reference export, often over
1 GB once downloaded. It is not the product catalog itself. It maps each Icecat
category to:

- category-feature groups;
- available feature IDs;
- category-specific feature metadata such as mandatory, searchable, display order,
  category-feature group ID, and default display unit.

The API uses this metadata only after it has been loaded into Elasticsearch. No
live Icecat API call is made by the category attribute endpoints.

## API Endpoints

The `api` module exposes these admin endpoints for category tooling:

- `GET /icecat/verticals/{verticalId}/candidate-categories`
  returns candidate Icecat category IDs for a vertical. The configured category is
  returned first when present, then search candidates from the category index.
- `GET /icecat/categories/{id}/attributes`
  returns category feature groups and the available attributes for an Icecat
  category, combining category-scoped metadata with global Icecat feature metadata
  already stored in Elasticsearch.

These endpoints are stable JSON contracts and are included in the generated
OpenAPI contract from controller annotations.

## Runtime Read Path

Application startup builds no in-memory reference map and makes no Icecat bulk-export
download. Every feature/category/feature-group/supplier read (admin search, feature-name
resolution, attribute rendering) goes through `IcecatIndexService`'s Elasticsearch
repositories, bounded by small local caches (`IcecatIndexService.featureCache`,
`IcecatFeatureResolver`'s normalized-name and by-id caches) with size-inspection methods for
health checks.

## Import and Versioning

`IcecatIndexService.syncFromLoaders()` is the only entry point that downloads and parses the
bulk exports; it is triggered explicitly (`GET /icecat/index/sync`), never automatically at
startup. Each reference type is written into a new, uniquely-versioned physical index and
`IcecatIndexVersionManager` atomically switches that type's alias to it only after the
written document count is validated. The previous index is retained (not deleted) so a bad
switch can be rolled back; older versions beyond the retained count are pruned. A failed
import leaves the alias untouched and can simply be retried.
