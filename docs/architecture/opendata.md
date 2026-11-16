# Open data : génération, manifests, diffusion

Jeux de données GTIN et ISBN publiés sous licence ODbL (GOU-2237). Ce document décrit l'architecture et le contrat
des manifests. Exploitation et relance : [opendata-generation.md](../operations/opendata-generation.md). Envoi vers
R2 : [opendata-r2-upload.md](../operations/opendata-r2-upload.md).

## Architecture

```mermaid
flowchart LR
  ES[(Elasticsearch partagé)] -- nuit, projection minimale --> API[api : passage hebdomadaire]
  API -- zips + manifests, manifest.json en dernier --> DISK[/opt/open4goods/opendata]
  API -- après le passage, S3 multipart, si publication.enabled --> R2[(R2 nudger-opendata, data.nudger.fr)]
  DISK -. lot 0, envoi manuel opendataR2Upload.yml .-> R2
  UI[ui : /opendata/*.zip] -- 301 si redirect-base-url, sinon 404 --> R2
  R2 -- manifest.json, cache 5 min --> FAPI[front-api /api/opendata/*]
  FAPI --> FE[pages /opendata]
```

- **Génération dans `api` uniquement** (GOU-2248) : `OpenDataGenerationScheduler` (cron
  `open-data-config.generation-cron`, par défaut dimanche 01:30 Europe/Paris) et `POST /opendata/generate`.
  `front-api` a `generation-enabled: false`, `ui` n'a plus la dépendance `opendata` (GOU-2290) et `OpenDataService`
  n'a plus de `@Scheduled` : seul `api` génère.
  Avant, la génération tournait dans `ui` 8 h après chaque démarrage et échouait depuis juin sur la limite de 100 Mo
  du buffer de réponse Elasticsearch (GOU-2249).
- **Lecture** : `ProductRepository.exportAll(fields, pageSize)` (scroll), `_source` limité à
  `OpenDataService.EXPORT_SOURCE_FIELDS` (environ 0,7 Ko par produit, 1,6 Ko au plus sur un échantillon de prod de
  200 produits, contre environ 21 Ko complet). Pages de `export-page-size` (2000, environ 1,5 Mo par réponse) et pause
  `page-pause` (50 ms) après chaque page. Les seules clés d'attribut lues sont les 5 clés ISBN et les critères des jeux
  impact (`attributes.indexed.{CRITERE}`, GOU-2250) ; pas de clé Decitre (GOU-2262). Ne relève jamais le buffer du client.
- **Écriture** : un passage écrit dans `opendata/tmp/`, sur le même disque. Il échoue avant toute lecture si l'espace
  libre est inférieur à `min-free-space-ratio` (1,5) fois la taille des zips publiés. Le sha256, les lignes et le
  remplissage des colonnes sont calculés pendant l'écriture.
- **Garde-fou de lignes** : un jeu refusé si ses lignes sont inférieures à `min-rows-ratio` (0,9) fois celles du
  manifest publié, ou fois le compte de l'index pris au début du passage (export arrêté en route). Un jeu *impact*
  refusé est isolé (GOU-2308) : il n'est pas publié, GTIN, ISBN et les autres jeux le sont ; voir
  `operations/opendata-generation.md`.
- **Publication locale** : renommages atomiques dans l'ordre zips, `gtin-manifest.json` et `isbn-manifest.json`,
  puis `manifest.json` en dernier (point de validation). `opendata/tmp/` est supprimé après chaque passage.
- **Échec visible** : toute exception termine le passage en `FAILED`, sans toucher aux fichiers publiés : log
  `ERROR`, health `openData` DOWN, métriques (voir l'exploitation). L'état n'est pas persisté : un redémarrage le remet
  à `IDLE`.

## Publication sur R2 (GOU-2289)

`OpenDataPublisher`, appelé par `api` après un passage réussi, publie les fichiers locaux sur le bucket R2 par son API
S3 (`S3OpenDataStorage`, AWS SDK for Java v2, client synchrone Apache). Désactivée par défaut et en bêta
(`open-data-config.publication.enabled: false`) ; activée en prod par `application-prod.yml` (GOU-2300). Un passage en échec
ne publie rien.

Les clés publiques sont les mêmes à chaque passage. Un zip n'est donc jamais écrit sur sa clé publique : il passe
par une clé temporaire, comme dans l'envoi manuel de GOU-2285. Ordre :

1. **Contrôles locaux** : chaque zip a la taille et un sha256 valide de son manifest.
2. **Garde-fou budgétaire** : la taille du bucket après la publication (objets conservés, hors clés remplacées et
   temporaires, plus les nouveaux zips et manifests) ne doit pas dépasser `max-bucket-size` (9 Go, unités binaires de
   Spring). Sinon refus, rien n'est envoyé. Pendant l'envoi, la copie temporaire s'ajoute à l'ancienne version
   pendant quelques heures : l'effet sur les Go-mois facturés est négligeable.
3. **Envoi multipart** de chaque zip vers `staging-prefix` + clé (`.staging/gtin-open-data.zip`), en parts de
   `part-size` (64 Mo), débit limité à `upload-bandwidth` (2 Mo/s). En-têtes : `Content-Type: application/zip`,
   `Content-Disposition: attachment`, `Cache-Control` de `zip-cache-control` (`public, max-age=300`, comme les manifests : avec des clés fixes, un cache long ferait servir l'ancien zip alors que `manifest.json` annonce déjà le nouveau sha256), métadonnée `sha256`. Un envoi en échec est
   abandonné (`AbortMultipartUpload`).
4. **Relecture** : chaque objet temporaire est relu en entier ; son sha256 et sa taille sont comparés au manifest.
   L'ETag d'un envoi multipart n'est pas un condensé du fichier : seule la relecture prouve le contenu.
5. **Bascule** : copie serveur de chaque zip vers sa clé publique (en-têtes reposés, copie multipart au-delà de
   `copy-part-size`, car une copie simple est limitée à 5 Go), contrôle de la taille et du `sha256` de l'objet public,
   puis `gtin-manifest.json` et `isbn-manifest.json`, puis `manifest.json` en dernier, point de validation
   (`Cache-Control` de `manifest-cache-control`, 5 min).
6. **Nettoyage** : les objets temporaires, de ce passage et d'un passage interrompu, ne sont supprimés qu'après
   l'écriture de `manifest.json`.

Un échec aux étapes 1 à 4 laisse tous les objets publics intacts ; les objets temporaires du passage sont supprimés.
Un échec à l'étape 5 (rare : copies serveur et petits `PUT`) laisse `manifest.json` à l'ancienne version, alors qu'un
zip peut déjà être le nouveau, jusqu'au passage suivant. Tout échec écrit un log `ERROR`, passe la health `openData`
en DOWN et se compte dans `opendata_publication_*`.

Identifiants : `R2_ACCOUNT_ID` (variable), `R2_ACCESS_KEY_ID` et `R2_SECRET_ACCESS_KEY` (secrets) de l'environnement
GitHub `prod`, rendus pour `api` par `scripts/deploy/render-service-secrets.sh` dans son config tree
(`open-data-config.publication.*`). Ils ne sont ni dans le code ni dans `open4goods-config`. L'environnement `beta` ne
les a pas.

## Lecture du manifest par front-api (GOU-2290)

`/api/opendata/*` lit `{open-data-config.public-base-url}/manifest.json` (`OpenDataManifestService`), jamais les
comptes live de l'index : l'index et le fichier divergent (ISBN au 15/11 : 10,1 M dans l'index, 8,7 M dans le fichier).
Seul `manifest.json` est lu (écrit en dernier, ses entrées sont cohérentes entre elles) ; chaque jeu y est cherché par
son `id`. Les champs inconnus sont ignorés (ajout compatible du contrat).

| Champ de l'API | Source dans le manifest |
|---|---|
| `recordCount`, `recordCountValue` | `rows` de l'entrée du jeu |
| `lastUpdated` | `generatedAt` de l'entrée, formaté dans la langue demandée, fuseau Europe/Paris |
| `fileSize` | `bytes` |
| `sha256` | `sha256` |
| `downloadUrl` | `{public-base-url}/{gtin,isbn}-open-data.zip` (configuration, pas le manifest) |
| overview `totalProductCount`, `datasetCount`, `totalDatasetSize` | sommes des entrées `gtin` et `isbn` seulement : les jeux impact décrivent les mêmes produits |

- **Base vide** (bêta, local) : aucun appel HTTP. Les champs issus du manifest valent `null`, `downloadUrl` aussi (`ui`
  ne sert plus les fichiers) ; `headers` reste rempli. Les endpoints répondent 200.
- **Manifest absent (404), illisible ou réseau en échec** : jamais de 500. Les champs valent `null`, ou les valeurs du
  dernier manifest lu avec succès par le processus. Un WARN est écrit quand la cause change, pas à chaque lecture ;
  un INFO au retour.
- **Cache** : une lecture, réussie ou non, sert `front.opendata.manifest-cache-ttl` (5 min, comme le `max-age=300` des
  manifests sur R2). Un seul appelant relit ; les autres reçoivent la lecture précédente pendant ce temps. Délais :
  `connect-timeout` 2 s, `read-timeout` 3 s.
- **Santé** `openDataManifest` : `UNKNOWN` si la base est vide ; `DEGRADED` si le manifest est illisible, si `gtin` ou
  `isbn` manque, ou si un jeu quelconque du manifest (`impact-*` compris) n'a pas de date valide ou a plus de
  `front.opendata.max-age` (8 jours) ; `UP` sinon. Un jeu impact refusé par le contrôle de lignes garde son ancien
  `generatedAt` (isolation GOU-2308) : il passe la santé en `DEGRADED` au bout de 8 jours même si `gtin` et `isbn`
  sont frais. Le détail `problems` dit quel jeu est en cause.
  `DEGRADED` est un statut personnalisé hors de l'ordre d'agrégation : comme `ALERT` et `WARN` de `impactScoreGuard`
  (api), il ne passe jamais la santé globale de front-api en DOWN, ce qui bloquerait ses redémarrages pour un
  fichier en retard. Écart voulu avec le "DOWN" de GOU-2239 point 6.
- **Métrique** `opendata.dataset.age{dataset}` (Prometheus `opendata_dataset_age_seconds`) : maintenant moins le
  `generatedAt` de chaque jeu du manifest (`gtin`, `isbn`, `impact-*`), calculé à la lecture, donc croissant entre
  deux lectures ; NaN si inconnu. Ops branche l'alerte au-delà de 691 200 s (8 jours), sur
  `/actuator/metrics/opendata.dataset.age?tag=dataset:gtin` ou sur le `status` JSON de la santé (le code HTTP reste
  200 en `DEGRADED`).

`ui` ne génère ni ne sert aucun fichier (plus de dépendance `opendata`) : `GET`/`HEAD` sur
`/opendata/{gtin,isbn}-open-data.zip` répondent 301 vers `{open-data-config.redirect-base-url}/{nom}`, ou 404 si la
base est vide (`OpenDataRedirectProperties`). Le flux bridé (`ThrottlingInputStream`, `limitedRateStream`,
`download-speed-kb`, `concurrent-downloads`) et `downloadLimits` de l'overview sont retirés.

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
- **ZIP global** : `impact-all.zip` contient les CSV de tous les jeux impact du passage, avec son manifest
  (`impact-all`, lignes = somme des jeux).
- **Date de la source** : non disponible dans l'index par critère, voir `last_updated` (colonne du jeu).
- **Publication R2** : l'envoi depuis `api` (GOU-2289) publie tous les jeux du passage, `impact-all` compris ; le workflow `opendataR2Upload.yml` (jeu `impact`) envoie `impact-*.zip` et
  `impact-*-manifest.json`.
