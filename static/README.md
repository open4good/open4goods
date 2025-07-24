# Static Service

The **static** module provides a lightweight Spring Boot application to expose
cached images and other resources on a dedicated domain. It reuses logic from the
legacy UI module but runs as an independent service.

## Features
- Serves brand logos, datasource icons and vertical images
- Exposes the latest UI jar via `/ui-latest.jar`
- Uses existing caching services for remote files

## Configuration
Configuration properties follow the prefix `static` and mainly define the root
folder used for cached resources.

```yaml
static:
  root-folder: /opt/open4goods/
```

## Build & Test
```bash
mvn clean install
mvn test
```

See the main [open4goods](../README.md) project for global information.
