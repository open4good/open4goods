# Api Agents Guide

> **Parent Guide**: [Root AGENTS.md](../AGENTS.md)  
> This guide **extends** the root conventions with API-specific rules.

This module is part of the open4goods multi-module Maven project.

## Technology

- Java 21
- Spring Boot 4

## Directory structure

- `src/main/java` - REST API and batch code
- `src/main/resources` - configuration
- `src/test/java` - unit tests

## Purpose

The `api` module exposes the REST endpoints and orchestrates data ingestion pipelines.

## Build and test this module only

From this directory:

```bash
mvn --offline clean install
```

Run only the tests with:

```bash
mvn --offline test
```

From the repository root you can also execute:

```bash
mvn --offline -pl api -am clean install
```

## Sauvegarde produits

`BackupService.backupProducts()` écrit `products-backup-{N}.gz` dans `api.backup.data-backup-folder`, puis
`products-backup-manifest.json` (champ optionnel `sha256` : nom → empreinte) et enfin `products-backup.sha256` au
format `sha256sum` (`<hex>  <fichier>`, écrit atomiquement, en dernier : sa présence signale une génération complète).
Les empreintes sont calculées pendant l'écriture (`DigestOutputStream`), jamais en relisant les fichiers. Vérification :
`cd <dossier> && sha256sum -c products-backup.sha256`. Le health check signale `product_backup_checksum_missing` si le
manifest déclare des empreintes mais que le `.sha256` manque ; il ne recalcule rien.
