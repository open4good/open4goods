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
