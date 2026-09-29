# Icecat Service

Loads Icecat XML data (features, categories, languages) and exposes helper
methods for product enrichment.

## Features

- Downloads and caches Icecat XML files through `RemoteFileCachingService`.
- Parses languages, brands, feature groups and categories.
- Provides utilities to resolve Icecat feature names.
- Streams the Open Icecat reference-catalogue export (CategoriesList, FeaturesList,
  FeatureGroupsList, LanguageList, and the Full-account CategoryFeaturesList) with a StAX
  cursor reader (`IcecatReferenceCatalogueReader`), never buffering a whole file — the manual
  documents CategoryFeaturesList.xml as larger than 10 GB. `IcecatCatalogueInventoryService`
  resolves each configured `*-file-uri` from a local path (no network, no credential) or a
  remote URL (delegated to `IcecatFileDownloadService`, which requires an Open Icecat account)
  and builds an `IcecatCatalogueInventory` of category, feature, feature-group and language
  counts. `IcecatMappingCoverageService.coverage(LocalDate, IcecatCatalogueInventory)` and
  `unmappedCategories(LocalDate, int, IcecatCatalogueInventory)` report coverage against that
  inventory instead of the Elasticsearch index, so coverage can be exercised offline against the
  hand-built fixtures under `src/test/resources/icecat/inventory/`. Feature-group counts are
  presentation metadata only; nothing here promotes them to a canonical attribute.

## Configuration

```yaml
icecat:
  featuresListFileUri: "https://.../features_list.xml.gz"
  categoryFeatureListFileUri: "https://.../category_features.xml.gz"
  languageListFileUri: "https://.../language_list.xml.gz"
  brandsListFileUri: "https://.../brands_list.xml.gz"
  categoriesListFileUri: "https://.../categories_list.xml.gz"
  featureGroupsFileUri: "https://.../feature_groups.xml.gz"
  user: "myuser"
  password: "secret"
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for details.
This module is licensed under the [AGPL v3](../../LICENSE).
