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

## Sécurité et accès

`config/WebSecurityConfig` (GOU-1393) : api.nudger.fr est "pur SSO Google" pour les personnes.

- Machines : clé API brute dans l'en-tête ou le paramètre `Authorization`, comparée en temps constant
  (`admin-key` → `ROLE_USER` + `XWIKIADMINGROUP`, `test-keys` → `ROLE_USER` + `ROLE_TESTER`). La clé Metriks
  (`metriks-key`, secret `API_METRIKS_KEY`, GOU-1412) → `ROLE_METRIKS` seul, et seulement sur un chemin
  `/api/metriks/<segment>` (hors context path, segments littéraux) : ailleurs elle n'authentifie rien (401).
  `MetriksController` accepte `XWIKIADMINGROUP` ou `ROLE_METRIKS`. Sans `metriks-key`, aucune clé Metriks.
- Personnes : `oauth2Login` Google, câblé seulement si la registration `google` existe
  (`spring.security.oauth2.client.registration.google.*`, rendue depuis `GOOGLE_SSO_CLIENT_ID/SECRET`). E-mail
  vérifié et présent dans `api.security.google-sso.role-allowlist` (`ApiOidcUserService`), sinon 403. Pas de
  formulaire, pas de page `/login`, pas d'utilisateur `user` généré.
- HTTP Basic : uniquement sur `/actuator/**` (chaîne dédiée), pour l'identité moniteur Spring Boot Admin
  `o4g.actuator.monitor.*` (`ActuatorMonitorAuthenticationProvider`, même classe que ui et front-api).
- Anonyme : navigation navigateur (`Accept: text/html`) → redirection vers `/oauth2/authorization/google` ; tout
  autre appel → 401 sans `WWW-Authenticate` (pas de popup, pas de session).

Preuve : `ApiGoogleSsoAccessTest` (contexte complet servi par Tomcat, y compris `redirect_uri` derrière
`X-Forwarded-Proto`/`X-Forwarded-Host` avec `forward-headers-strategy: NATIVE`). Doc :
[Google SSO OAuth clients](../docs/operations/google-sso-oauth-clients.md#role-allowlist-api).

## Mapping de l'index produits

`ProductIndexMappingRunner` (GOU-1835, `ApplicationRunner`) est la seule écriture du mapping de `products-moustik` :
index absent → création avec settings et mapping, présent → `putMapping` de `product-mappings.json`. 3 tentatives
(`api.product-index-mapping.retry-delay`, défaut `2s`, doublé), un WARN à pile complète par échec, puis un ERROR et
l'api continue de démarrer. `Product` garde `createIndex = false` et `alwaysWriteMapping = false` : ne les remets pas à
`true`, le constructeur du repository ferait de nouveau tomber `ui` et `api` sur une panne ES passagère. Doc :
[products index mapping](../docs/operations/product-index-mapping.md).

## Sauvegarde produits

`BackupService.backupProducts()` écrit `products-backup-{N}.gz` dans `api.backup.data-backup-folder`, puis
`products-backup-manifest.json` (champ optionnel `sha256` : nom → empreinte) et enfin `products-backup.sha256` au
format `sha256sum` (`<hex>  <fichier>`, écrit atomiquement, en dernier, en `rw-r--r--` comme les `.gz` et le manifest pour rester lisible par le compte de tirage `backup-pull` : sa présence signale une génération complète).
Les empreintes sont calculées pendant l'écriture (`DigestOutputStream`), jamais en relisant les fichiers. Vérification :
`cd <dossier> && sha256sum -c products-backup.sha256`. Le health check signale `product_backup_checksum_missing` si le
manifest déclare des empreintes mais que le `.sha256` manque ; il ne recalcule rien.

Déclenchement : `checkProductsBackup()` (`@Scheduled`, `api.backup.products-backup-check-initial-delay` défaut `PT15M`,
`api.backup.products-backup-check-interval` défaut `PT1H`) lance `backupProducts()` si aucun export ne tourne et si
`products-backup-manifest.json` est absent, illisible ou plus ancien que `api.backup.products-backup-interval-hours`
(défaut 168). Un redémarrage de l'api ne repousse donc plus la sauvegarde. La décision est loguée en `info`.
`POST /backup/products/export` force toujours la sauvegarde. Le profil local ne lance rien (stub `LocalDevConfig`).
À la première vérification, si aucun export ne tourne, `.tmp/` est vidé (volume libéré logué) ; les fichiers publiés,
le manifest et le `.sha256` ne sont jamais touchés. Pas de reprise d'une sauvegarde interrompue.

## Agrégation des prix

`PriceAggregationService` : un fragment `InStock.OUTOFSTOCK` n'ajoute pas d'offre (#3468) ; une offre expire
`ProductRepository.VALID_UNTIL_DURATION` (2 jours) après l'indexation de son fragment. Par couple (datasource, état),
on garde le prix le plus bas, et à prix égal l'offre la plus récente : une offre renvoyée au même prix rafraîchit sa
date (GOU-1699). Avant de retirer les offres expirées, on enregistre par état la date de la dernière offre vue
(`price.lastOfferSeen`, qui ne recule jamais). front-api s'en sert pour dater le dernier prix d'une fiche sans offre.

Diagnostic en lecture seule des fiches qui ont perdu leurs offres : `scripts/diagnostics/stale_offers_report.py`
(`product <gtin>`, `lost --days N`).

## Nettoyage des ressources orphelines

`POST /batch/clean-resources?mode=DRY_RUN|MOVE|DELETE` répond `202` immédiatement et exécute le balayage en tâche de
fond (un seul à la fois ; un second appel pendant un run renvoie l'état courant). `GET /batch/clean-resources` donne
l'état (`IDLE`, `RUNNING`, `COMPLETED` avec le rapport, `FAILED`). Les clés actives sont stockées par empreinte 64 bits
dans un `long[]` trié (`ActiveKeySet`, ~8 o/clé) : une collision ne peut que conserver un fichier, jamais classer un
fichier référencé comme orphelin. Si le tas ne suffit pas, le run s'abandonne (`aborted`, aucun fichier touché).

## Purge d'une datasource retirée

`POST /batch/purge-datasource?datasource=<nom>&scope=OFFERS|ALL&dryRun=true|false` (GOU-1592, GOU-2101,
`DatasourcePurgeService`) retire de l'index produit les données d'une datasource, selon le `scope` :

- `OFFERS` (défaut, le moins destructif, GOU-2101) : seulement les offres `price.offers` dont `datasourceName` vaut
  `<nom>`. Les données de fiche de la datasource (sources d'attributs, description, catégories, alias de marque,
  clé de `datasourceCodes`) restent : elles sont vraies, et les retirer ferait perdre attributs et verticales. Lecture
  limitée à `price`, `offersCount` et au GTIN référentiel ; écriture par mise à jour partielle
  (`ProductRepository#bulkUpdateDocument`, champs `price` et `offersCount`) : ni les embeddings ni les autres champs
  ne sont relus ou réécrits.
- `ALL` (comportement GOU-1592) : les offres, puis `Product.removeDatasourceData` (clé de `datasourceCodes`, alias de
  marque, catégorie, description, sources d'attributs ; une catégorie encore portée par une autre datasource reste).
  Lecture et écriture (`store`) du produit complet, embeddings compris.

Dans les deux cas, meilleure offre (`price.minPrice`), `offersCount` et `price.conditions` sont recalculés sur les
offres restantes, sans filtre de fraîcheur, y compris quand seul `minPrice` pointait encore vers la datasource : les
offres des autres datasources ne sont jamais retirées. Historique et tendances de prix ne sont pas touchés.

- `<nom>` est le `name` du YAML de la datasource (valeur exacte de `datasourceName` et des clés de
  `datasourceCodes`), pas le nom du fichier : `rakuten.com` pour `rakuten.com-csv.yml`. `Rakuten FR` est une autre
  datasource.
- `dryRun=true` par défaut : compte produits et offres touchés, donne jusqu'à 20 GTIN d'exemple, n'écrit rien.
- Parcours complet par scroll (pages de `FULL_PRODUCT_PAGE_SIZE`), écriture par bulks de 200. Un produit sans donnée
  de la datasource dans le `scope` n'est jamais réécrit : un second passage ne touche rien. Un bulk en échec est
  compté dans `failedProducts` (documents en échec seulement, par exemple un produit supprimé depuis sa lecture) et
  n'est pas rejoué. À lancer hors de la fenêtre du batch nocturne : un prix mis à jour pendant le run peut être
  recouvert par la version lue par le scroll.
- Asynchrone (un seul run à la fois) : réponse `202` ; `GET /batch/purge-datasource` donne l'état (`IDLE`,
  `RUNNING` avec les compteurs du dernier palier, `COMPLETED` avec le rapport `scope`, `scannedProducts`,
  `touchedProducts`, `removedOffers`, `savedProducts`, `failedProducts`, `sampleGtins`, `FAILED` avec `error`, ou
  `INTERRUPTED`). Statut et rapport portent le `scope` ; un fichier d'état écrit avant GOU-2101 est relu en `ALL`.
- Progression (GOU-1815) : un palier tous les `datasource-purge-progress-interval` produits lus (défaut 50 000) et au
  moins toutes les 5 minutes. Chaque palier écrit une ligne `Purge of datasource <nom> (scope=..., dryRun=...): N
  products scanned, ... touched, ... offers removed, ... saved, ... failed, sample GTINs [...]` dans `nohup-api.log`
  (le `application.yml` du jar garde `DatasourcePurgeService` en `INFO` alors que la config bêta/prod met
  `org.open4goods` en `warn`).
- Rapport persistant : l'état est écrit en JSON au lancement, à chaque palier puis à la fin, dans
  `datasource-purge-status-file` (défaut `<root-folder>/batch-reports/datasource-purge-status.json`, soit
  `/opt/open4goods/batch-reports/datasource-purge-status.json` en bêta et en prod), par fichier temporaire
  `.tmp` renommé. Il se lit sans l'api : `cat` du fichier.
- `INTERRUPTED` : au démarrage, l'api relit ce fichier ; un run resté `RUNNING` (api arrêtée ou redéployée pendant
  le run) devient `INTERRUPTED`, avec les compteurs et `sampleGtins` de son dernier palier (`lastProgressAt`) et sans
  `finishedAt`. Le run n'est pas repris : un nouveau `POST` relance un run complet depuis le début. `COMPLETED` et
  `FAILED` restent lisibles après redémarrage.

Appel Ops (clé `admin-key`, secret `O4G_INTERNAL_API_KEY`, brute dans l'en-tête `Authorization`), dry run (scope
`OFFERS` par défaut), état, puis run réel une fois le rapport du dry run vérifié :

```bash
curl -X POST -H "Authorization: $O4G_INTERNAL_API_KEY" "https://api.nudger.fr/batch/purge-datasource?datasource=rakuten.com"
curl -H "Authorization: $O4G_INTERNAL_API_KEY" https://api.nudger.fr/batch/purge-datasource
curl -X POST -H "Authorization: $O4G_INTERNAL_API_KEY" "https://api.nudger.fr/batch/purge-datasource?datasource=rakuten.com&scope=OFFERS&dryRun=false"
```

## Date de la dernière offre par datasource

GOU-2037. `price.offers` n'est pas indexé : pour savoir quels marchands ont encore une offre active sans parcourir
l'index produits, `DataFragmentStoreService` appelle `DatasourceOfferActivityService.record` pour chaque fragment
agrégé qui devient une offre (`PriceAggregationService.isOffer` : prix non nul, pas en rupture), dans les deux
chemins (mise à jour partielle et agrégation complète ; rien si l'agrégation est sautée).

- État : date de la dernière offre (timestamp du fragment) par nom de datasource, en mémoire, écrit toutes les
  5 minutes s'il a bougé et à l'arrêt dans `datasource-offer-activity-file` (défaut
  `<root-folder>/batch-reports/datasource-offer-activity.json`), par fichier `.tmp` renommé, relu au démarrage.
- `trackedSince` : début de l'enregistrement qui a écrit le fichier (écrit à la première offre agrégée). Une datasource dont la dernière offre est antérieure n'y figure pas.
- `GET /merchants/offer-activity` (rôle admin, comme `/partners`) renvoie `{ trackedSince, datasources: [{
  datasourceName, lastOfferSeen }] }`. front-api le lit toutes les heures pour compter les domaines marchands actifs
  (`MerchantDomainsService`, voir `front-api/README.md`). Ne pas renommer ce chemin sans mettre à jour ce client.

## Agrégation temps réel des fragments

`DataFragmentStoreService.aggregateAndstore` (GOU-1700) lit les produits du lot par identifiant numérique
(`ProductRepository.multiGetForAggregation`), jamais par la chaîne GTIN brute. Cet identifiant est
`BarcodeValidationService.toProductId(gtin)`, la valeur du GTIN assaini, celle sous laquelle
`IdentityAggregationService` indexe le produit (GOU-1841) : `07333394145372` et `7333394145372`, un UPC-A avec ou sans
zéros de tête, un ISBN-10 (`2726862217`) et son ISBN-13 (`9782726862216`) désignent le même document. Un fragment au
code-barres invalide est ignoré. Un produit n'est créé que si Elasticsearch le déclare absent ; un fragment dont la
lecture a échoué est ignoré (log `error`), il ne recrée jamais le produit. Le GTIN référentiel du produit reste le
GTIN assaini : `AttributeRealtimeAggregationService` ne l'écrase plus avec la forme brute d'un fragment (ISBN-10), qui
reste seulement dans `gtinInfos`.

`TaxonomyRealTimeAggregationService` garde la verticale courante quand le groupe EPREL du produit
(`eprelDatas.productGroup`) figure dans ses `eprelGroupNames` : ni un jeton `excludingTokensFromCategoriesMatching`
porté par une autre datasource, ni une catégorie qui ne correspond plus ne la retirent. EPREL n'attribue jamais seul
une verticale. Dénombrement et reprise des produits touchés :
[runbook](../docs/operations/product-recreation-recovery.md).

### Slug produit figé

`NamesAggregationService` (GOU-1708) ne génère le slug (`names.url`, base du `fullSlug` de front-api) que s'il
manque pour une langue, ou si la verticale du produit diffère de `names.urlVertical`, la verticale mémorisée à sa
génération (`_none` pour un slug généré sans verticale). Un changement de nom ou d'attributs, ou
`forceNameGeneration`, ne change plus l'URL publique : ce flag ne régénère que titres et méta. `urlVertical` absent
(slug antérieur à GOU-1708) = inconnu : le slug existant est figé et la verticale courante est mémorisée au passage
suivant, sans migration. Un produit qui perd sa verticale garde son slug ; la lui rendre ne change pas l'URL. Les
anciennes URL continuent de rediriger en 301 par GTIN vers le slug canonique.

## Batch des verticales et indexation temps réel

`BatchService.batch(vertical)` (13 h) charge les produits de la verticale, les complète, les agrège et les note, puis
les met en file d'indexation complète. L'indexation temps réel continue pendant ce temps (GOU-1709) :

- La file complète n'est jamais vidée : une mise à jour temps réel en file avant les produits du batch est indexée
  avant eux (les workers gardent l'ordre de la file dans un bulk).
- Par pages de `INDEXATION_PAGE_SIZE` (500), juste avant la mise en file, `ProductRepository.applyNewerOfferStates`
  reprend `price`, `offersCount` et `lastChange` de l'état temps réel le plus récent (index, file complète, file
  partielle) quand son `lastChange` est plus récent que celui du produit chargé. Le batch ne touche jamais
  `lastChange`. Un état plus ancien ou égal ne remplace rien.
- Les empreintes `datasourceCodes` restent celles du chargement : un fragment dont le contenu a changé pendant le batch
  sera réagrégé en entier à sa prochaine ingestion.
- Fenêtre restante : une mise à jour temps réel écrite entre la mise en file d'une page et son indexation (le temps de
  vider la file, 5 000 produits au plus) peut encore être recouverte, et une mise à jour complète lue avant
  l'indexation de la page peut recouvrir les champs calculés par le batch.

Durées (GOU-2099) : le logger `org.open4goods.api.services.BatchService.timings`, à INFO dans `application.yml` malgré
le `warn` de la bêta et de la prod, journalise le début et la fin de chaque verticale avec la durée de chaque phase
(chargement et classification, complétion, agrégation, notation, mise en file), et pour la complétion, tous les
1 000 produits puis à la fin, le nombre de produits traités et sautés et le temps passé par service de complétion.
Fichier dédié (GOU-2174) : ces lignes (`Start of batch`, `End of batch in N s`) sont aussi écrites en ajout dans
`batch-timings.log` (`BatchTimingsLogFile`, appender Logback ajouté au logger, la sortie standard et la configuration
de logging existante restent inchangées). Chemin : propriété `api.batch.timings-log-file`, sinon `logging.file.path`
ou le dossier de `logging.file.name`, sinon `/opt/open4goods/logs` s'il existe (bêta/prod) ; absent en dev et tests.
Rétention : 10 Mo par fichier, 10 archives `.gz` au plus, 100 Mo au total. `nohup-api.log` ne garde qu'un
redémarrage, ce fichier survit aux redémarrages.
Une complétion qui ne trouve rien a sa propre fenêtre : `api.eprelMissRefreshDays` (7 jours, ou recherche immédiate
si le GTIN ou les modèles changent) pour EPREL, `wikidata.miss-refresh-in-days` (7 jours) pour Wikidata.
