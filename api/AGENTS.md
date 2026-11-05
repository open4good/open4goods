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

`POST /batch/purge-datasource?datasource=<nom>&dryRun=true|false` (GOU-1592, `DatasourcePurgeService`) retire de
l'index produit toutes les données d'une datasource : offres `price.offers` dont `datasourceName` vaut `<nom>`,
puis `Product.removeDatasourceData` (clé de `datasourceCodes`, alias de marque, catégorie, description, sources
d'attributs ; une catégorie encore portée par une autre datasource reste). Meilleure offre (`price.minPrice`),
`offersCount` et `price.conditions` sont recalculés sur les offres restantes, sans filtre de fraîcheur : les offres
des autres datasources ne sont jamais retirées. Historique et tendances de prix ne sont pas touchés.

- `<nom>` est le `name` du YAML de la datasource (valeur exacte de `datasourceName` et des clés de
  `datasourceCodes`), pas le nom du fichier : `rakuten.com` pour `rakuten.com-csv.yml`. `Rakuten FR` est une autre
  datasource.
- `dryRun=true` par défaut : compte produits et offres touchés, donne jusqu'à 20 GTIN d'exemple, n'écrit rien.
- Parcours complet par scroll (pages de `FULL_PRODUCT_PAGE_SIZE`, produits complets, embeddings compris), écriture
  par bulks de 200, progression loguée tous les 50 000 produits. Un produit sans donnée de la datasource n'est
  jamais réécrit : un second passage ne touche rien. À lancer hors de la fenêtre du batch nocturne.
- Asynchrone (un seul run à la fois) : réponse `202` ; `GET /batch/purge-datasource` donne l'état (`IDLE`,
  `RUNNING`, `COMPLETED` avec le rapport `scannedProducts`, `touchedProducts`, `removedOffers`, `savedProducts`,
  `failedProducts`, `sampleGtins`, ou `FAILED`).

Appel Ops (clé `admin-key`, secret `O4G_INTERNAL_API_KEY`, brute dans l'en-tête `Authorization`), dry run, état, puis
run réel une fois le rapport du dry run vérifié :

```bash
curl -X POST -H "Authorization: $O4G_INTERNAL_API_KEY" "https://api.nudger.fr/batch/purge-datasource?datasource=rakuten.com"
curl -H "Authorization: $O4G_INTERNAL_API_KEY" https://api.nudger.fr/batch/purge-datasource
curl -X POST -H "Authorization: $O4G_INTERNAL_API_KEY" "https://api.nudger.fr/batch/purge-datasource?datasource=rakuten.com&dryRun=false"
```

## Agrégation temps réel des fragments

`DataFragmentStoreService.aggregateAndstore` (GOU-1700) lit les produits du lot par identifiant numérique
(`ProductRepository.multiGetForAggregation`, `Long.valueOf(gtin)`), jamais par la chaîne GTIN brute : `07333394145372`
et `7333394145372` désignent le même document. Un produit n'est créé que si Elasticsearch le déclare absent ; un
fragment dont la lecture a échoué est ignoré (log `error`), il ne recrée jamais le produit.

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
