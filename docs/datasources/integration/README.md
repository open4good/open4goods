---
title: "Intégration des sources de données externes à GTIN/ISBN : socle commun"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Intégration des sources de données externes à GTIN/ISBN : socle commun

> Rédigé le 2026-11-08 à partir de
> l'inventaire interne des sources candidates (non publié) et d'une lecture du code
> de `main` (c6ed39707).
> Consigne de cadrage : **agrégation maximale, sourcing transparent, questions juridiques
> traitées dans un second temps**. Les licences ne figurent ici que comme un champ de
> l'attribution (section 4.4) ; aucun plan ne conditionne son intégration à une analyse
> juridique.
> Les classes marquées **(à créer)** n'existent pas encore. Toutes les autres ont été
> vérifiées dans le code.
>
> **Décisions du 2026-11-09** (section 6) : la projection crée les produits inconnus (`CREATE`
> par défaut), et les données livres sont intégrées (plans 09 et 10 en P2).

## 1. Sommaire des plans

| # | Source | Identifiant | Mode (section 3) | Verticales / périmètre | Priorité | Effort |
|---|---|---|---|---|---|---|
| 01 | [Indice de durabilité (TV, lave-linge)](01-indice-durabilite.md) | GTIN partiel (TV 43 %, LL 82 %) + marque/modèle | R | tv, washing-machine | P1 | M |
| 02 | [Indice de réparabilité](02-indice-reparabilite.md) | GTIN (95,9 % valides, 2 822 distincts) | R | dishwasher, washing-machine ; hors verticale : portables (ASUS à 99 %), aspirateurs ; smartphones négligeables (6) | P1 | S (réutilise 01) |
| 03 | [RappelConso V2](03-rappelconso.md) | GTIN (13 652 distincts dont 3 670 non alimentaires) | R + bloc `recalls` | quasi rien dans les verticales ; masse hors verticale | P1 import + mesure go/no-go, P2 bloc et front | M |
| 04 | [Safety Gate (UE)](04-safety-gate.md) | GTIN valide sur 44 % des alertes récentes (22 882 sur l'historique) + marque/modèle | R + bloc `recalls` | masse hors verticale | P2 | M (hors P0-6) |
| 05 | [EPREL smartphones et tablettes](05-eprel-smartphones.md) | pas de GTIN dans les fiches lues ; modèle réglementaire | E | smartphones | P1 (conditionne ImpactScore et sitemap) | S à M |
| 06 | [EU Ecolabel](06-eu-ecolabel.md) | GTIN valide sur 30,6 % des lignes, pas de marque ni de modèle | R + bloc `labels` | 0 ligne dans les verticales | P3 | S |
| 07 | [Open Products Facts](07-open-products-facts.md) | GTIN (44 056 exploitables sur 48 924) | R | masse ; 155 smartphones, 4 TV, 4 lave-linge | P3 | M (S sans les images) |
| 08 | [Open Prices](08-open-prices.md) | GTIN (87 % de prix alimentaires) | R (observations, jamais des offres) | 2 398 produits non alimentaires | non recommandé en l'état | S (import) à M |
| 09 | [data.bnf.fr](09-data-bnf.md) | ISBN (3,4 M distincts ; 19,6 % de nos ISBN, 50 % des ISBN français) | R+ par moisson SPARQL + complément SRU | livres | P2 (Q2 décidée) | M ; +L pour une verticale |
| 10 | [Open Library](10-open-library.md) | ISBN (50 % de nos ISBN ; 61 % en union avec la BnF) | R+ (dump mensuel de 12,6 Go filtré) | livres | P2, après 09 | M |
| 11 | [eBay Browse API](11-ebay-browse.md) | GTIN/ePID partiel | C + offres éphémères (P0-8) | smartphones (occasion) ; environ 1 250 produits frais au quota par défaut | P3 (P2 si alertes occasion) | M |
| 12 | [Reconditionné via affiliation](12-reconditionne-affiliation.md) | GTIN 14 % | F + état `REFURBISHED` (P0-8) | smartphones | P2, après mesure état x stock x GTIN | M |
| 13 | [Verified by GS1](13-verified-by-gs1.md) | GTIN | C (contrôle) | toutes ; 30 requêtes par jour en accès public | non recommandé | S |
| 14 | [ENERGY STAR (US)](14-energy-star.md) | UPC (98 % de préfixes US/Canada) | R + bloc `labels` | 6 verticales, recouvrement quasi nul | non recommandé | S |
| 15 | [Web Data Commons](15-web-data-commons.md) | GTIN schema.org (9 % des 570 M entités) | R+ hors ligne | masse ; crawl d'octobre 2024 | non recommandé en l'état | L (S pour la seule découverte de marchands) |

Hors périmètre de cette série (pas de GTIN ni d'ISBN dans la source) :

- endoflife.date : jointure par famille de modèles, à traiter avec le matcher marque/modèle de
  la section 3.2 une fois celui-ci généralisé.
- Boavizta : jointure fabricant + nom de modèle, 50 smartphones.
- iFixit : jointure par modèle, pas d'identifiant commercial.
- Open Repair Alliance : statistiques par catégorie, sans produit.
- Registre DPP (ESPR) : non accessible aux tiers avant les actes délégués.

Sources déjà branchées et non replanifiées ici : Open Icecat, Amazon PA-API, flux d'affiliation,
EPREL (groupes actuels), Wikidata.

## 2. Ce qui existe aujourd'hui (et ne convient pas tel quel)

### 2.1 Les trois voies d'entrée existantes

| Voie | Classes | Ce qu'elle fait | Limite pour les nouvelles sources |
|---|---|---|---|
| Flux CSV | `FeedIndexingService`, `FeedIndexingWorker` (`services/feedservice`), `DataFragmentStoreService.queueDataFragment` (`api/.../services/store/`) | télécharge, parse, crée un `DataFragment` par ligne, agrège en temps réel | `DataFragment.validate()` exige une `url` : une ligne sans URL est rejetée. Chaque GTIN inconnu **crée un produit**. CSV uniquement (XML, JSON, RDF, API : rien de générique). |
| Completion | `AbstractCompletionService` (`commons/.../services/`), `EprelCompletionService`, `IcecatCompletionService`, `WikidataCompletionService`, `AmazonCompletionService` (`api/.../services/completion/`) | parcourt les produits **d'une verticale ayant des offres**, appelle la source, construit un `DataFragment` synthétique passé à `StandardAggregator.onDatafragment(df, product)` | ne visite jamais les produits hors verticale : livres, alimentaire, produits non classés. Câblage manuel dans `ApiConfig.completionFacadeService`. |
| Catalogue de référence | `EprelCatalogueService`, `EprelSearchService` (`services/eprel-service`), index ES `eprel-products` | télécharge un catalogue complet dans un index dédié, puis la completion apparie par GTIN, puis par marque et modèle | spécifique à EPREL, upserts sans suppression des fiches disparues ni bascule d'alias. |

Point vérifié : la voie completion appelle `aggregator.onDatafragment(df, data)` directement
(`EprelCompletionService.java:131`, `WikidataCompletionService.java:106`). La validation d'URL
n'a lieu que dans `DataFragmentStoreService` (ligne 160). Un `DataFragment` synthétique sans URL
est donc accepté par cette voie. Le socle ci-dessous s'appuie sur ce fait.

### 2.2 Briques réutilisables

- Téléchargement : `RemoteFileCachingService` (`services/remotefilecaching`) :
  `getResource(url, refreshInDays)`, `downloadToTmpFile`, `decompressGzipAndDeleteSource`,
  `unzipFileAndDeleteSource`.
- Index versionné avec bascule d'alias : `IcecatIndexVersionManager` (`services/icecat`) :
  `reimport(documentClass, docs)` retourne un `IndexSwitchResult`, plus `rollback` et
  `currentIndexesForAlias`.
- Appariement GTIN puis marque/modèle : `EprelSearchService.search(gtin, models, groups)` et
  `EprelCompletionService.selectUniqueResult` (unicité, rejet des ambiguïtés).
- Résolution des marques : `BrandService.resolve(brandName)` (`services/brand`).
- Lecture par lots des produits : `ProductRepository.multiGetForAggregation(Collection<Long>)`.
  Réindexation : `ProductRepository.addToFullindexationQueue(Collection<Product>)`.
  Export projeté : `ProductRepository.exportAll(String[] includeFields)`.
- Agrégation : `AggregationFacadeService.getStandardAggregator(String name)`, qui enchaîne
  Identity, Taxonomy, Attribute, UsageCost, Names, Price et Media.
- Provenance des attributs : `SourcableAttribute` et `SourcedAttribute` (`model/.../attribute/`).
  `AttributeRealtimeAggregationService` enregistre le `datasourceName` du fragment comme source.
- Retrait d'une source : `Product.removeDatasourceData(name)` et `POST /batch/purge-datasource`.
- Export ouvert : `OpenDataService` (`services/opendata`), `GTIN_HEADER` et `ISBN_HEADER`.

## 3. Socle proposé : un mécanisme unique, plusieurs modes

L'idée centrale : **chaque source est d'abord agrégée en entier dans son propre index de
référence `ref-<id>`**. C'est là que se fait l'agrégation maximale, indépendamment de nos
produits. La projection sur `Product` est une seconde étape, configurable par source. Elle
peut seulement enrichir les produits existants, ou en créer : la création est le mode par
défaut depuis le 2026-11-09 (Q1, section 6).

### 3.1 Modes d'intégration

| Mode | Usage | Étapes |
|---|---|---|
| **R** (référentiel compact, moins de 1 M lignes) | data.gouv, RappelConso, Safety Gate, Ecolabel, ENERGY STAR, Open Products Facts, Open Prices | import complet dans `ref-<id>`, puis projection en **itérant le jeu de données**, par lots de GTIN |
| **R+** (référentiel volumineux) | Open Library, data.bnf.fr (moisson SPARQL), Web Data Commons | export de notre ensemble de GTIN, filtrage du dump en flux contre cet ensemble, import des seules lignes utiles (ou de tout, en mode `CREATE`), projection comme R |
| **C** (completion par appel API) | eBay, Verified by GS1 | `AbstractCompletionService` existant, quotas, sélection des produits prioritaires |
| **E** (extension d'un pipeline existant) | EPREL smartphones | configuration et mapping dans le pipeline EPREL |
| **F** (flux marchand) | reconditionné via affiliation | chaîne des flux existante, avec l'état `productState` |

Pourquoi itérer le jeu de données plutôt que les produits : `AbstractCompletionService`
parcourt `exportVerticalWithValidDate`, soit les produits classés ayant un prix récent. Les
rappels alimentaires, les produits Ecolabel ou les livres n'y passent jamais. Un jeu de
1 000 à 20 000 lignes se projette en quelques dizaines de `multiGet`, contre un parcours
complet de l'index produits.

### 3.2 Composants (à créer)

Nouveau module `services/reference-datasets`, package
`org.open4goods.services.referencedatasets`. Il est détecté automatiquement par l'api
(`scanBasePackages="org.open4goods.services"`). Il suit la frontière d'ADR 0007 : le module
possède le téléchargement, le parsing et les index ; il ne dépend jamais de `api`.

```text
services/reference-datasets
├─ ReferenceDatasetProvider     interface, une implémentation par source
│    String id()                         ex. "fr-indice-durabilite"
│    Stream<ReferenceRecord> read(Path)  parse le fichier téléchargé
│    List<URI> resolveDownloadUrls()     ex. via l'API data.gouv
├─ ReferenceRecord              record neutre (voir ci-dessous)
├─ ReferenceIndexService        import dans ref-<id>-<horodatage>, contrôle, bascule d'alias ref-<id>
│                               (même logique qu'IcecatIndexVersionManager), calcul du diff
├─ ReferenceImportReport        lignes lues, rejetées, GTIN valides, disparus depuis l'import précédent
└─ config.yml.ReferenceDatasetsProperties   préfixe reference-datasets.<id>.*

api
├─ services/reference/ReferenceProjectionService   itère ref-<id>, multiGet par lots de 500,
│                                                  DataFragment synthétique, StandardAggregator,
│                                                  attribution, réindexation
├─ services/reference/ReferenceMatcher             GTIN exact, puis marque (BrandService) + modèle,
│                                                  logique extraite d'EprelCompletionService.selectUniqueResult
├─ services/reference/ReferenceDatasetScheduler    une tâche par source déclarée, via ThreadPoolTaskScheduler
└─ controller/ReferenceDatasetController           POST /reference-datasets/{id}/import
                                                   POST /reference-datasets/{id}/project[?dryRun=true]
                                                   GET  /reference-datasets (statut, derniers rapports)
```

`ReferenceRecord` :

```java
public record ReferenceRecord(
        String sourceRecordId,        // id stable dans la source
        Set<String> gtins,            // GTIN normalisés (BarcodeValidationService), vide si absent
        String brand,                 // brut, résolu à la projection
        Set<String> models,           // candidats de modèle bruts
        Map<String, String> attributes, // nom brut de la source -> valeur brute
        Map<String, Object> payload,  // bloc métier non attributaire (rappel, prix observé...)
        String recordUrl,             // fiche publique de l'enregistrement, si elle existe
        Instant sourceUpdatedAt,      // date de mise à jour déclarée par la source
        RecordState state) {}         // ACTIVE, ENDED
```

Déroulé de la projection pour un produit trouvé :

1. Construire un `DataFragment` avec `datasourceName = <id>`, `datasourceConfigName = <id>`, le
   GTIN en `referentielAttributes`, les `attributes` de l'enregistrement et des dates. Gabarit :
   `EprelCompletionService.initDataFragment`.
2. Si `datasourceCodes[<id>]` vaut déjà le hash de contenu de l'enregistrement, ne rien faire.
3. Sinon, appeler `StandardAggregator.onDatafragment(df, product)`, écrire l'attribution
   (section 4.4) et le hash, puis réindexer par lots.
4. Pour un GTIN présent dans la version précédente de `ref-<id>` et absent de la nouvelle, ou
   passé à `ENDED` : `product.removeDatasourceData(<id>)`, retrait de l'attribution, réindexation.
   Exception : les rappels terminés restent en historique (P0-6).

Garde-fous de `ReferenceMatcher` :

- **Contrôle de catégorie** : même sur un GTIN exact, on refuse le rattachement si la catégorie
  de la source contredit celle du produit. Cas mesuré : `8698902064415` est un lave-linge
  Finlux chez ENERGY STAR et un téléviseur Daewoo chez nous.
- La recherche marque + modèle ne vaut que dans une verticale. Hors verticale (rappels, labels),
  on apparie au GTIN seul.

Contrat de `ReferenceDatasetProvider` : il doit accepter des milliers d'URL (Safety Gate :
1 123 rapports hebdomadaires pour l'historique) et des appels d'API pendant `read` (Safety Gate :
une fiche JSON par alerte). Le cache par URL de `RemoteFileCachingService` suffit.

Les deux consolidations data.gouv (01 et 02) partagent environ 160 colonnes. Une base commune
`AbstractDataGouvConsolidationProvider` **(à créer)** résout la ressource et lit le CSV.

En mode `CREATE`, un GTIN inconnu part par `DataFragmentStoreService.queueDataFragment`, avec
`url = recordUrl`, ce qui crée le produit par la voie standard. En mode `ENRICH_EXISTING`, il
reste dans `ref-<id>` et sera projeté dès qu'un flux marchand créera le produit : la projection
quotidienne le rattrape.

Les scores ne sont pas recalculés par la projection. Le batch quotidien
(`BatchService.batch()`, 13 h) les recalcule, ou `POST /score/{vertical}` à la demande.

### 3.3 Configuration

```yaml
# api, application.yml (valeurs par défaut) ; surcharges dans open4goods-config
reference-datasets:
  fr-indice-durabilite:
    enabled: true
    cron: "0 30 4 * * *"          # après la consolidation data.gouv du matin
    projection: CREATE            # défaut décidé le 2026-11-09 (Q1) ; ENRICH_EXISTING par source
    min-rows-ratio: 0.8           # refuse la bascule si la nouvelle version perd plus de 20 % des lignes
    match: GTIN_THEN_BRAND_MODEL  # ou GTIN_ONLY
```

Chaque source a aussi un fichier `open4goods-config/datasources/<id>.yml` sans bloc
`csvDatasource`, comme `amazon.fr.yml`. Il porte `name`, `favico`, `logo`, `portalUrl` et les
nouveaux champs d'attribution (section 4.4). Sans bloc `csvDatasource`, `FeedService` ne le
traite pas comme un flux orphelin.

Convention de nommage des identifiants : kebab-case court, stable, qui ne contient jamais la
chaîne `openfoodfacts` (prérequis P0-1). Liste : `fr-indice-durabilite`,
`fr-indice-reparabilite`, `rappelconso`, `safety-gate`, `eprel` (existant), `eu-ecolabel`,
`open-products-facts`, `open-prices`, `data-bnf`, `open-library`, `ebay`, `gs1-verified`,
`energy-star`, `web-data-commons`.

## 4. Prérequis transverses (à faire une fois, avant les plans)

### P0-1 Supprimer le nettoyage "openfoodfacts" de `BatchService.clean()`

`BatchService.clean()` (`api/.../services/BatchService.java:865`, marqué `TODO(p3,design) :
remove`) sérialise chaque produit et **supprime tout produit dont le JSON contient
"openfoodfacts"**. Il est déclenché par `POST /batch/tweak`. Mesuré le 2026-11-08 : les URL d'images Open Products
Facts (`images.openproductsfacts.org`) ne contiennent pas la chaîne, mais 45 % des fiches OPF la
contiennent dans des noms de contributeurs (`openfoodfacts-contributors`), et toutes les URL
Open Prices (`prices.openfoodfacts.org`) la contiennent. Ce bloc est à supprimer avant les
plans 07 et 08.

### P0-2 Corriger l'appariement des synonymes accentués

`DataFragment` normalise les noms d'attributs entrants avec `IdHelper.normalizeAttributeName`
(accents retirés, majuscules ; `DataFragment.java:618`). `AttributesConfig.synonyms()`
enregistre en revanche les synonymes bruts (`AttributesConfig.java:121`). Un synonyme accentué,
comme `"INDICE DE RÉPARABILITÉ"` dans `REPAIRABILITY_INDEX.yml`, ne peut donc jamais
correspondre (constat par lecture du code). Correctif : normaliser **les deux côtés** de la comparaison avec
la même fonction (les synonymes au chargement, et le nom de l'attribut au moment de la
résolution, car des fiches anciennes gardent des clés accentuées comme `CONTRIBUTEUR : ÉDITEUR`),
et écrire d'abord un test unitaire qui échoue.

### P0-3 Rendre la priorité des sources configurable

`SourcableAttribute.DEFAULT_TRUSTED_SOURCE_PRIORITY` vaut en dur `["eprel", "icecat.biz"]`.
Toute autre source ne gagne qu'au nombre de votes : un indice de durabilité réglementaire
perdrait contre trois marchands qui recopient une ancienne valeur. Proposition : un champ
`preferredSources` par `AttributeConfig` (YAML d'attribut), avec repli sur la liste actuelle.

### P0-4 Attribution de source (sourcing transparent)

Aujourd'hui, la provenance n'existe qu'au niveau des attributs (`SourcedAttribute.dataSourcename`)
et d'un hash ou horodatage dans `Product.datasourceCodes`. Les scores n'en portent aucune.

- **Produit** (à créer) : `Map<String, SourceAttribution> Product.sourceAttributions`, indexé par
  identifiant de source.

  ```java
  public record SourceAttribution(String sourceRecordId, String recordUrl,
          Instant sourceUpdatedAt, Instant retrievedAt, String contentHash) {}
  ```

  L'index produits est en **`"dynamic": "strict"`** à la racine de
  `model/src/main/resources/product-mappings.json` : un champ non mappé fait rejeter tout le
  document. `sourceAttributions` (et `recalls`, `labels`) doivent donc être déclarés dans le
  mapping, par exemple en objet `"dynamic": false` pour ne pas indexer leur contenu, avec les
  seuls sous-champs filtrables mappés explicitement (`recalls.active`, `recalls.source`).
  À confirmer sur l'index réel avec `GET products-moustik/_mapping` avant le premier
  déploiement.
- **Source** (à créer, dans `DataSourceProperties`) : `license`, `licenseUrl`, `attribution`
  (texte affiché, par exemple "Producteurs, via data.gouv.fr"), `dataPortalUrl`.
- **Scores** : `Attribute2ScoreAggregationService` recopie la source gagnante de l'attribut dans
  `Score.metadatas` (`source`, `sourceUpdatedAt`).
- **Front** : front-api expose déjà `ProductDatasourcesDto` et `ProductSourcedAttributeDto`
  (`ProductMappingService`). On y ajoute l'attribution. La fiche produit affiche un bloc
  "Sources" : nom, lien vers l'enregistrement, date de mise à jour, licence.
- **Export ouvert** : une colonne `sources` (identifiants séparés par `|`) dans `GTIN_HEADER`
  et `ISBN_HEADER`.

### P0-5 Module `services/reference-datasets` et projection (section 3.2)

Livré avec le plan 01 comme première implémentation, puis réutilisé par tous les modes R et R+.

### P0-6 Bloc `recalls` sur `Product` (pour 03 et 04)

Les rappels ne sont pas des attributs. Ils ont un cycle de vie (actif jusqu'à la date de fin)
et un contenu textuel (motif, risque, conduite à tenir).

Définition unique, qui fusionne les besoins des plans 03 et 04. Ces deux plans renvoient ici.

- À créer : `List<ProductRecall> Product.recalls`. C'est un vrai champ Java : la chaîne
  d'agrégation relit et réécrit le document entier, donc un champ inconnu serait perdu.
- Champs communs :
  - identité : `source`, `recallId`, `version`, `url`, `language` ;
  - dates : `publishedAt`, `endsAt` (nullable) ;
  - texte : `title`, `reason`, `productCategory`, `legalNature`, `compensation` (liste) ;
  - risque : `riskLevel` (nullable), `risks` (liste), `measures` (liste), `measureCategories`
    (liste, Safety Gate) ;
  - portée : `batchNumbers` (liste ; vide = toute la production du GTIN), `models` (liste) ;
  - origine : `notifyingCountry`, `reactingCountries` (liste) ;
  - liens : `relatedRecallIds` (liste), `posterUrl`, `imageUrls` (liens seuls, aucune copie) ;
  - état : `active` (calculé).
- Mapping ES : objet `recalls` en `"dynamic": false`, avec `recalls.active` (boolean) et
  `recalls.source` (keyword) mappés pour filtrer et compter. Ajouter
  `ProductPartialUpdateField.RECALLS`.
- Le bloc est écrit par `ReferenceProjectionService` à partir de `ReferenceRecord.payload`, et
  non par la chaîne d'agrégation d'attributs. `removeDatasourceData` ne le touche pas : le
  retrait d'une entrée est explicite.
- Cycle de vie : un rappel terminé **reste** sur le produit avec `active = false` (historique).
  Seule une fiche retirée par la source supprime l'entrée. L'exception à l'étape 4 de la
  section 3.2 est donc voulue.
- `active` dépend de la date du jour. La projection le recalcule à chaque passage, même quand
  le hash de contenu n'a pas changé.
- Règles propres à chaque source : 90 jours sans date de fin pour RappelConso (plan 03, à
  valider), à décider pour Safety Gate, qui n'a jamais de date de fin (plan 04).
- Export ouvert, pour toutes les sources : `recall_active` (au moins un rappel actif) et
  `recall_urls` (URL des fiches, actives d'abord, séparées par `|`).

### P0-7 Bloc `labels` sur `Product` (pour 06 et 14)

Un attribut booléen `EU_ECOLABEL` ne porte ni le numéro de licence ni la date d'expiration. On
crée `List<ProductLabel> Product.labels` **(à créer)** : `source`, `labelId`
(`eu-ecolabel`, `energy-star`), `licenseNumber`, `validUntil`, `url`. Même règle de mapping
que `recalls`. Un attribut dérivé peut rester utile pour les filtres.

### P0-8 Offres éphémères et état reconditionné (pour 11 et 12)

- `ProductCondition` ne connaît que `NEW` et `OCCASION` : "reconditionné" est perdu dès le
  parsing. Il faut ajouter `REFURBISHED` (enum, parser, historique de prix, front) ou un grade
  sur l'offre (plan 12, question 2).
- Le prix minimum (`bestPrice`, colonne `min_price` de l'export) mélange aujourd'hui tous les
  états. Il faut un minimum par état.
- Offres éphémères (eBay, fraîcheur de 6 h) : à créer `expiresAt` et `ephemeral` sur l'offre
  agrégée. Ces offres sont exclues du prix minimum, de l'historique et du nombre d'offres, et
  filtrées à la lecture (`ProductMappingService`). Le TTL actuel de 2 jours ne convient pas.

### P0-9 Filtre GTIN partagé

Constaté sur plusieurs sources : GTIN `0`, codes de moins de 8 chiffres, suites factices
(`1234567890128`), codes internes de magasin (préfixes 20-29), GTIN tronqués par Excel
(`4711640000000`), colonnes typées entières qui perdent les zéros de tête (RappelConso).

- Un filtre unique dans le socle, appliqué par tous les providers.
- Bug probable dans l'existant : `BarcodeValidationService.toProductId("0")` renvoie `0L`. Tout
  flux avec un GTIN "0" peut atterrir sur un produit d'identifiant 0. À vérifier avec
  `GET products-moustik/_doc/0`.

### P0-10 Liste blanche de projection par source

Ce que chaque source a le droit d'écrire sur `Product` se déclare dans
`reference-datasets.<id>.project` : `attributes`, `names`, `brand`, `resources`, `recalls`,
`labels`, ou `NONE`. Deux interdits par défaut :

- `price` pour toute source non marchande. Une observation projetée deviendrait une offre dans
  `PriceAggregationService` et fausserait `offersCount`, le prix minimum et l'export (plan 08).
- `category`. `excludingTokensFromCategoriesMatching` (CABLE, ACCESSOIRE...) s'applique aux
  catégories de toutes les sources : une catégorie externe peut sortir un produit de sa
  verticale, changer son URL et déclencher une redirection 301. 65 fiches Open Products Facts
  portent un tel jeton.

Limites connexes, à traiter avec les plans à médias (07, 10) :

- `Product.removeDatasourceData` ne retire ni les `resources` ni les `offerNames`.
- `MediaAggregationService` déduplique par URL seulement : une image remplacée par la source
  reste sur le produit.
- Aucune attribution par image n'existe (`Resource` n'a ni auteur ni licence), et
  `ReferenceRecord` n'a pas de champ média. Il faut ajouter `media` au record et des champs
  `author` et `license` à `Resource`.
- Le contrat de `ReferenceDatasetProvider` doit aussi couvrir :
  - un répertoire de travail plutôt qu'un fichier (Open Prices joint quatre fichiers) ;
  - un provider sans téléchargement, qui moissonne une API (SPARQL et SRU de la BnF) ;
  - une lecture en flux HTTP reprenable par `Range`, sans fichier temporaire (dump Open
    Library de 12,6 Go) ;
  - un filtre par ensemble de clés pendant la lecture (mode R+). Ce filtre s'appuie sur un
    export de nos identifiants par type de code-barres (`exportAll(String[])` n'a pas ce
    filtre aujourd'hui) ;
  - un `dryRun` sur l'import ;
  - l'import d'un fichier produit hors de l'api (job Web Data Commons, dépôt manuel).
- P0-3 n'agit que sur `attributes.indexed`. Un produit hors verticale prend la configuration
  par défaut : les attributs utiles hors verticale (livres, rappels) doivent être déclarés dans
  `_default.yml`.

### P0-11 Prérequis du mode `CREATE` (décision du 2026-11-09)

La création est le mode par défaut (Q1). Elle ne change pas la décision, mais rend obligatoires
avant la première source en `CREATE` :

- **P0-9 bloquant** : aucun produit créé sur un code rejeté par le filtre GTIN/ISBN. Sans ce
  filtre, `toProductId("0")` donne le produit 0, et chaque code factice ou tronqué crée une
  fiche.
- **Fiches hors verticale visibles** : elles sortent aujourd'hui du sitemap
  (`exportVerticalForSitemap` exige `excluded = false`), et front-api expose leurs attributs
  sous leur clé brute, sans libellé. Il faut un critère d'entrée au sitemap pour ces fiches, et
  des libellés pour les attributs déclarés dans `_default.yml` (note de P0-10).
- **Titre** : une fiche créée n'a pas d'offre, donc pas de nom marchand. Le provider doit
  projeter un nom exploitable (titre, ou marque et modèle), sinon la ligne n'est pas créée.
- **P0-10 renforcé** : les interdits `price` et `category` comptent encore plus, puisqu'une
  fiche créée n'a aucune offre pour les corriger.
- **Mesure** : nombre de produits créés par source et par import, dans le rapport de la
  projection, avec un plafond configurable par source (`max-created-per-run`) pour les
  sources volumineuses (mode R+, voir Q2 pour Open Library).

## 5. Gabarit d'un plan par source

Chaque plan suit ces sections, dans cet ordre. [01-indice-durabilite.md](01-indice-durabilite.md)
sert d'exemple.

0. **Fiche** : identifiant, mode, priorité, effort, verticales, volume attendu, dépendances P0.
1. **Apport** : ce que la source ajoute et pourquoi.
2. **Accès** : URL, format, taille, fréquence, authentification, quotas. Faits mesurés avec
   leur date ; tout fait non mesuré porte la mention **non vérifié**.
3. **Identifiants et appariement** : taux réel de GTIN valides (somme de contrôle), repli
   marque/modèle, normalisation.
4. **Modèle cible** : attributs canoniques à créer ou réutiliser (`verticals/.../attributes/`),
   champs `Product`, mapping ES.
5. **Pipeline** : provider, parsing, import, projection, scores, export ouvert, front.
6. **Planification et volumétrie**.
7. **Sourcing transparent** : texte d'attribution, URL d'enregistrement, date.
8. **Tests et critères d'acceptation** : fixtures, taux d'appariement attendu, métriques.
9. **Découpage** : tickets ordonnés, avec leur effort.
10. **Risques, challenges, questions**.

## 6. Questions qui changent le travail

**Q1. Créer des produits ou seulement enrichir ? Et à quoi servent les fiches hors
verticale ?** L'agrégation maximale plaide pour `CREATE`. Mais les mesures montrent que les
nouvelles sources tombent surtout **hors de nos 7 verticales** :

- RappelConso : 0 TV, lave-linge, lave-vaisselle, réfrigérateur ou smartphone.
- Ecolabel : 0 ligne dans les verticales.
- Safety Gate : 8 % des GTIN sondés existent chez nous, tous hors verticale et sans offre.
- Indice de réparabilité : portables (ASUS à 99 %) et aspirateurs.

Une fiche hors verticale est servie (`product-uncategorized.spec.ts`), mais elle sort du
sitemap (`exportVerticalForSitemap` exige `excluded = false`), et front-api y expose les
attributs sous leur clé brute, sans libellé. Créer en masse des pages sans prix ni contenu
propre, c'est un risque de "thin content" et un coût d'index.

**Décidé le 2026-11-09 : `CREATE`.** La projection crée les produits inconnus par défaut ;
`ENRICH_EXISTING` reste possible source par source. Les risques ci-dessus deviennent des
prérequis : voir P0-11 (filtre GTIN bloquant, sitemap et libellés des fiches hors verticale,
titre, plafond par import).

**Q2. Les livres : pour quoi faire ?** Aucune verticale livres, aucun scoring, et le batch
ne visite pas les ISBN. data.bnf.fr et Open Library ne servent aujourd'hui qu'à enrichir
l'export ISBN. Faut-il une verticale livres sur nudger.fr ? Ou seulement un export ISBN
enrichi ? Ou rien pour l'instant ? Mesuré, le scénario minimal (export ISBN et fiches
existantes enrichis) coûte déjà M par plan. Une verticale livres ajoute L. Notons que l'export
ISBN public est figé au 2026-06-04 alors qu'il devrait être hebdomadaire.

**Décidé le 2026-11-09 : les données livres sont intégrées** (plans 09 puis 10, en P2). Restent
ouverts : une verticale livres (surcoût L, plan 09 section 5.8), et le périmètre de la création
pour Open Library (38 M éditions à ISBN ; le plan 10 n'indexe aujourd'hui que les éditions
appariées).

**Q3. ADR 0010 et `data-reference`.** ADR 0010 (statut `accepted`) attribue l'ingestion à
`services/data-reference`, dont le code a été écarté lors du reset d'octobre 2026 : il ne reste
que des branches distantes non fusionnées. Le document candidat raisonne pourtant en
`surfaceGrants` ADR 0010. Ces plans ciblent la chaîne réelle de `main` (`DataFragment`,
agrégation, completion). Proposition : passer ADR 0010 en `superseded` et écrire un ADR court
"sources de référence : index `ref-<id>` et projection".

**Q4. Ordre de priorité.** Proposition :

1. P0-1, P0-2, P0-3, P0-9, puis P0-4 et P0-5, livrés avec le plan 01. Le plan 01 complète la
   carte Paperclip GOU-1892 (attribut `DURABILITY_INDEX` lu dans les flux marchands).
2. Plan 05 (EPREL smartphones). C'est le plus rentable : sans `eprelDatas`, un smartphone est
   exclu de l'ImpactScore V1 (`missing_eprel`) et du sitemap. Il faut d'abord vérifier
   l'hypothèse `productGroup` (plan 05, section 3.3). Ce plan sert de spécification à la carte
   GOU-1891 ; GOU-1875 et GOU-1893 mesurent l'appariement EPREL.
3. Plan 02.
4. Ticket de mesure du plan 03 (go/no-go), et mesure état x stock x GTIN du plan 12.
5. Plans 09 puis 10 (livres, Q2), puis le reste, avec P0-11 avant la première création.

Faut-il remonter un sujet, par exemple la sécurité produit avant tout ?

**Q5. Suivi.** Faut-il transformer chaque plan en ticket Paperclip (un ticket P0 par prérequis,
un par plan), ou garder ces documents comme seule source ?

## 7. Constats qui corrigent le document candidat

- **Indice de durabilité TV : 529 lignes sur 1 233 ont un GTIN valide** (mesure du 2026-11-08,
  fichier consolidé du 2026-11-07), et non 1 232. La colonne `referentiel_id_modele = GTIN_EAN`
  déclare un GTIN, mais `id_modele` contient souvent une référence commerciale (`TKW32HD26BH`,
  `40FHD036`). Lave-linge : 1 304 sur 1 588. L'appariement marque + modèle est indispensable
  (plan 01).
- Le document candidat raisonne en surfaces `NUDGER_WEB` / `B2B_API` / `ODBL_EXPORT`. La brique
  B2B étant supprimée, la surface `B2B_API` disparaît, et ADR 0017 (EPREL "zero-rated") devient
  sans objet.
- **Le document candidat compte des GTIN déclarés, pas des GTIN valides.** Mesures du
  2026-11-08, avec la somme de contrôle :
  - indice de réparabilité : 4 695 valides sur 4 896 lignes, soit 2 822 GTIN distincts (le
    candidat annonce 4 880). Beaucoup de doublons republiés par des distributeurs (TD SYNNEX,
    Boulanger, Fnac Darty) ;
  - Safety Gate : 135 alertes sur 308 avec un GTIN valide (le candidat annonce 146) ;
  - Ecolabel : 28 035 lignes sur 91 497, soit 30,6 %.
- **EPREL** : une fiche produit se lit sans clé API (`GET /api/products/{groupe}/{numéro}`) ;
  seuls l'export et la liste paginée en exigent une. Aucune des fiches smartphones lues ne
  porte de GTIN. Attention aux unités : `batteryEnduranceInCycles = 20` signifie 2 000 cycles.
- **Safety Gate** : il existe une API JSON publique par alerte (`barcodes[]`, `brands[]`,
  `modelTypes[]`, textes en français), plus fiable que le XML hebdomadaire, qui concatène les
  codes-barres sans séparateur.
- **Back Market** : les 14 % de lignes avec GTIN ne disent rien des offres en stock. Seules
  732 lignes `used` sont en stock, et 39 103 lignes ont un état inconnu.
- **Open Products Facts** recopie en partie d'autres sources : 7 452 fiches viennent de
  l'import Ecolabel et 1 498 de l'indice de réparabilité. Les 22 fiches `en:dishwashers` sont
  des détergents.

## 8. Anomalies relevées dans le code de `main`

Trouvées en préparant ces plans, et indépendantes des nouvelles sources :

- Synonymes accentués jamais appariés (P0-2).
- `BatchService.clean()` supprime tout produit contenant "openfoodfacts" (P0-1).
- `BarcodeValidationService.toProductId("0")` renvoie `0L` (P0-9).
- `ProductEprelDto.from` met à `null` `categorySpecificAttributes`, que lisent
  `EprelDetailsTable.vue` et `ProductPage.vue` : ces valeurs sont probablement toujours vides en
  front (plan 05).
- `EprelSearchService` filtre sur `productGroup`, que l'import ne renseigne pas lui-même et qui
  est déclaré `index = false` : hypothèse d'un appariement smartphone impossible (plan 05,
  section 3.3), à vérifier sur l'ES de prod.
- `AttributesConfig.byEprelFeatureGroup` est construit mais jamais lu.
- Les endpoints de dry-run EPREL décrits par ADR 0003 n'existent pas sur `main`.
- `ProductCondition` ne connaît pas l'état reconditionné, et `min_price` mélange neuf et
  occasion (P0-8).
- `REPAIRABILITY_INDEX` normalise en `MINMAX_OBSERVED` une note sur 10 (plan 02).
- L'export ISBN public (`isbn-open-data.zip`) date du 2026-06-04, alors qu'il devrait être
  régénéré chaque semaine, et sa colonne `souscategorie` est toujours vide (plan 09).
- `ProductMappingService.toWebpUrl` réécrit les URL d'images externes en `.webp`, ce qui donne
  une 404 (couvertures Open Library, plan 10).
