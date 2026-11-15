# Open data : génération, manifests, diffusion

Jeux de données GTIN et ISBN publiés sous licence ODbL (GOU-2237). Ce document décrit l'architecture et le contrat
des manifests. Exploitation et relance : [opendata-generation.md](../operations/opendata-generation.md). Envoi vers
R2 : [opendata-r2-upload.md](../operations/opendata-r2-upload.md).

## Architecture

```mermaid
flowchart LR
  ES[(Elasticsearch partagé)] -- nuit, projection minimale --> API[api : passage hebdomadaire]
  API -- zips + manifests, manifest.json en dernier --> DISK[/opt/open4goods/opendata]
  DISK -- lot 0 / lot 1 --> R2[(R2 nudger-opendata, data.nudger.fr)]
  UI[ui : /opendata/*.zip] -- 301 si redirect-base-url --> R2
  FAPI[front-api /api/opendata/*] --> FE[pages /opendata]
```

- **Génération dans `api` uniquement** (GOU-2248) : `OpenDataGenerationScheduler` (cron
  `open-data-config.generation-cron`, par défaut dimanche 01:30 Europe/Paris) et `POST /opendata/generate`. `ui` et
  `front-api` ont `generation-enabled: false` et `OpenDataService` n'a plus de `@Scheduled` : seul `api` génère.
  Avant, la génération tournait dans `ui` 8 h après chaque démarrage et échouait depuis juin sur la limite de 100 Mo
  du buffer de réponse Elasticsearch (GOU-2249).
- **Lecture** : `ProductRepository.exportAll(fields, pageSize)` (scroll), `_source` limité à
  `OpenDataService.EXPORT_SOURCE_FIELDS` (environ 0,7 Ko par produit, 1,6 Ko au plus sur un échantillon de prod de
  200 produits, contre environ 21 Ko complet). Pages de `export-page-size` (2000, environ 1,5 Mo par réponse) et pause
  `page-pause` (50 ms) après chaque page. Aucune clé d'attribut autre que les 5 clés ISBN n'est lue (pas de clé
  Decitre, GOU-2262). Ne relève jamais le buffer du client.
- **Écriture** : un passage écrit dans `opendata/tmp/`, sur le même disque. Il échoue avant toute lecture si l'espace
  libre est inférieur à `min-free-space-ratio` (1,5) fois la taille des zips publiés. Le sha256, les lignes et le
  remplissage des colonnes sont calculés pendant l'écriture.
- **Garde-fou de lignes** : un jeu refusé si ses lignes sont inférieures à `min-rows-ratio` (0,9) fois celles du
  manifest publié, ou fois le compte de l'index pris au début du passage (export arrêté en route).
- **Publication locale** : renommages atomiques dans l'ordre zips, `gtin-manifest.json` et `isbn-manifest.json`,
  puis `manifest.json` en dernier (point de validation). `opendata/tmp/` est supprimé après chaque passage.
- **Échec visible** : toute exception termine le passage en `FAILED`, sans toucher aux fichiers publiés : log
  `ERROR`, health `openData` DOWN, métriques (voir l'exploitation). L'état n'est pas persisté : un redémarrage le remet
  à `IDLE`.

## Fichiers publiés

| Fichier local | Objet public | Contenu |
|---|---|---|
| `gtin.zip` | `gtin-open-data.zip` | `open4goods-gtin-dataset.csv` (UTF-8) |
| `isbn.zip` | `isbn-open-data.zip` | `open4goods-isbn-dataset.csv` (UTF-8) |
| `gtin-manifest.json`, `isbn-manifest.json` | idem | manifest d'un jeu (GOU-2239) |
| `manifest.json` | `manifest.json` | tous les jeux (contrat du plan GOU-2237) |
| `impact-{vertical}.zip` | idem | `impact-{vertical}.csv` : critères bruts d'une verticale (GOU-2250) |
| `impact-{vertical}-manifest.json` | idem | manifest d'un jeu impact |

## Contrat des manifests (`schemaVersion` 1)

Contrat public. Ajout de champs : compatible. Retrait ou changement de sens : `schemaVersion` + 1. Les dates sont en
ISO 8601 UTC, à la seconde.

`manifest.json` :

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-11-22T03:10:00Z",
  "license": "ODbL-1.0",
  "datasets": [ { "...": "entrée identique à gtin-manifest.json" } ]
}
```

`gtin-manifest.json` (et `isbn-manifest.json`, même forme) :

```json
{
  "schemaVersion": 1,
  "id": "gtin",
  "file": "gtin-open-data.zip",
  "url": "https://data.nudger.fr/gtin-open-data.zip",
  "format": "text/csv+zip",
  "license": "ODbL-1.0",
  "bytes": 6400000000,
  "sha256": "...",
  "rows": 115786654,
  "indexCount": 115790000,
  "generatedAt": "2026-11-22T02:55:00Z",
  "columns": [ { "name": "gtin", "fillRate": 1.0 } ]
}
```

- `bytes`, `sha256` : du zip, tel que publié.
- `rows` : lignes de données du CSV, en-tête exclu.
- `indexCount` : produits du type de code-barres dans l'index au début du passage.
- `fillRate` : part des lignes dont la cellule n'est pas vide, de 0 à 1, arrondie à 4 décimales.
- `url` : absent si `open-data-config.public-base-url` est vide.

## Format des CSV et journal du schéma

Colonnes et formats inchangés au lot 1, pour les réutilisateurs actuels. Les corrections (`last_updated` en ISO 8601,
URL canonique, colonne de catégorie Nudger, `souscategorie` vide) forment une version 2 du format, décidée au lot 3.

Journal des modifications du schéma (à compléter par la version 2) :

- **Lot 1 (GOU-2248)**, par rapport à l'ancien fichier encore diffusé par Kaggle (GTIN : 59,7 M lignes, ISBN :
  7,76 M) : les colonnes `min_price_compensation` (héritée, à ne pas utiliser) et `classification_decitre_1`,
  `classification_decitre_2`, `classification_decitre_3` ne figurent plus dans les fichiers. `nb_page` reste dans le
  jeu ISBN, sans changement de nom.

## Datasets impact (lot 2, GOU-2250)

Un CSV zippé par verticale activée de `verticals/`, `impact-{vertical}.zip`, produit dans le même passage que GTIN et
ISBN (une seule lecture de l'index, un produit va dans chaque jeu dont il relève) et listé dans `manifest.json` avec
l'identifiant `impact-{vertical}`. La liste des verticales et des critères vient de `VerticalsConfigService` et de
`availableImpactScoreCriterias` : une nouvelle verticale donne un nouveau jeu, sans liste en dur. `DATA_QUALITY` est
exclu ; une verticale sans autre critère n'a pas de jeu.

- **Critères bruts uniquement** (décision du 15/11) : valeur de `attributes.indexed.{CRITERE}` telle quelle, jamais de
  score agrégé, sous-score, percentile ni rang. Valeur absente : cellule vide, jamais de défaut.
- **Lignes** : tous les produits de la verticale (`vertical` dans l'index), exclus compris ; `indexCount` du manifest
  est `countItemsByVertical` au début du passage et le garde-fou de lignes (0,9) s'applique.
- **Projection** : les champs ci-dessus plus `vertical`, `names.url`, `externalIds.eprel` et un champ
  `attributes.indexed.{CRITERE}` par critère ; aucune clé Decitre (GOU-2262).
- **Manifest** : les colonnes d'un jeu impact portent en plus `unit`, `description` et `source` (ajout compatible).
- Colonnes et définitions : [opendata-impact-dictionary.md](opendata-impact-dictionary.md).
- Non fait : le ZIP global de la spécification GOU-2240 et la date de la source par critère (absente de l'index).
