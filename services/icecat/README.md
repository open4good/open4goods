# Icecat Service

Loads Icecat XML data (features, categories, languages) and exposes helper
methods for product enrichment.

## Features

- Downloads and caches Icecat XML files through `RemoteFileCachingService`.
- Parses languages, brands, feature groups and categories.
- Provides utilities to resolve Icecat feature names.

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
