---
title: "Plan d'intégration : EPREL smartphones et tablettes (Règl. 2023/1669)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : EPREL smartphones et tablettes (Règl. 2023/1669)

> Socle commun : [README](README.md). Fiche source :
> section 4.1 de l'inventaire interne des sources candidates (non publié).
> Mesures du 2026-11-08 sur l'API publique EPREL (sans clé), sur 4 fiches du groupe
> `smartphonestablets20231669` : 2339793 (Samsung, smartphone), 2284539 et 2472907 (Samsung,
> tablettes), 2251145 (Apple, smartphone). Lecture du code de `main` (c6ed39707).

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | `eprel` (existant), groupe `smartphonestablets20231669` |
| Mode | E : extension du pipeline EPREL (`EprelCatalogueService`, `EprelSearchService`, `EprelCompletionService`). Pas d'index `ref-<id>` : `eprel-products` en tient lieu |
| Priorité | P1 |
| Effort | S pour la configuration et le mapping. M si l'hypothèse `productGroup` de la section 3.3 se confirme, ou si l'on traite l'appariement Apple (section 3.4) |
| Verticales | `smartphones`. Le groupe contient aussi les tablettes (`deviceType = TABLET`), sans verticale nudger |
| Volume | 48 759 produits dans la verticale (README). Nombre de fiches du groupe **non vérifié** (export soumis à clé) |
| Dépend de | P0-4 (attribution). P0-2 et P0-3 ne bloquent pas : les synonymes EPREL sont sans accent, et `eprel` est déjà en tête de `SourcableAttribute.DEFAULT_TRUSTED_SOURCE_PRIORITY` |

## 1. Apport

Le règlement délégué (UE) 2023/1669 impose une étiquette énergie aux smartphones et tablettes
depuis le 20 juin 2025 (article 8, lu sur EUR-Lex le 2026-11-08). Les fiches EPREL du groupe
portent, en plus de la classe énergie :

- la classe de réparabilité A-E et l'indice de réparabilité européen (1,00 à 5,00) ;
- la classe de fiabilité aux chutes répétées A-E et le nombre de chutes sans défaut ;
- l'endurance de la batterie par cycle et en nombre de cycles ;
- l'indice de protection IP ;
- la durée minimale des mises à jour logicielles, la garantie, la batterie remplaçable par
  l'utilisateur, le système d'exploitation, les sous-notes de réparabilité.

Situation actuelle de nudger :

- `smartphones.yml` déclare `eprelGroupNames: [smartphonestablets20231669]`.
- Les critères propres au groupe ne sont mappés sur aucun attribut. Ils arrivent dans
  `EprelProduct.categorySpecificAttributes` (`@JsonAnySetter`), puis
  `EprelCompletionService.addCategoryAttributes` les émet sous leur nom brut. Sans synonyme, ils
  restent hors de `attributes.indexed`.
- Couverture relevée dans les commentaires des YAML (2026-05-21, **non remesurée**) :
  `CLASSE_ENERGY` 589 produits (1,3 %), `REPAIRABILITY_INDEX` 1 983 produits.
- **Enjeu sous-estimé** : `AttributeRealtimeAggregationService.updateExcludeStatus` ajoute
  `missing_eprel` et passe `excluded = true` pour tout produit sans `eprelDatas`, quelle que soit
  la verticale. En ImpactScore V1, un smartphone non rattaché à EPREL n'est donc ni noté ni
  classé. Le taux de rattachement EPREL fixe le nombre de smartphones notés.
- **Enjeu SEO** : `ProductRepository.exportVerticalForSitemap` exige `excluded = false` (vérifié
  dans le code). Un smartphone sans EPREL sort donc aussi du sitemap de la verticale. Le chantier
  ImpactScore V2 prévoit de lever ce filtre par verticale (`eprelRequired`, GOU-1881) : l'enjeu
  diminuerait alors, mais les données resteraient utiles.

C'est la plus grosse verticale, et la seule source réglementaire de réparabilité pour les
smartphones mis sur le marché depuis juin 2025.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Liste des groupes | `GET https://eprel.ec.europa.eu/api/product-groups`, sans clé : `{"code":"SMARTPHONE_TABLET_2023_1669","url_code":"smartphonestablets20231669","name":"Smartphones and slate tablets","regulation":"Regulation (EU) 2023/1669"}` | vérifié |
| Export complet du groupe | `GET /api/exportProducts/smartphonestablets20231669`, en-tête `x-api-key` (`RestEprelApiClient.downloadCatalogueZip`) | clé requise ; taille **non vérifiée** |
| Liste paginée | `GET /api/products/smartphonestablets20231669?_page=1&_limit=5` : HTTP 403 sans clé | vérifié |
| Fiche d'un produit | `GET /api/products/smartphonestablets20231669/{numéro}` : JSON complet sans clé | vérifié sur 4 fiches |
| Étiquette | `GET /api/products/smartphonestablets20231669/{numéro}/labels?format=SVG` : SVG sans clé, environ 830 000 caractères | vérifié (2339793) |
| Fiche d'information PDF | `https://eprel.ec.europa.eu/fiches/smartphonestablets20231669/Fiche_{numéro}_{LANGUE}.pdf` | vérifié (2339793, EN) |
| Lien QR | `https://eprel.ec.europa.eu/qr/{numéro}`, présent dans la fiche PDF | vérifié |
| Fréquence chez nous | catalogue : `EprelCatalogueScheduler`, cron `${eprel.refresh-cron:0 0 3 */2 * *}` ; rattachement : batch de 13 h, `api.eprelRefreshDays` = 1 jour | vérifié (code) |
| Filtre de groupes en prod | bloc `eprel:` de `open4goods-config/configs/prod/api/application-active.yml` sans `groups-to-index`, donc tous les groupes | vérifié (lecture) |
| Filtre de groupes en devsec | `api/src/main/resources/application-devsec.yml` : `televisions`, `electronicdisplays`, `ovens`, `washingmachines2019`, `washerdriers2019`. Fichier **non suivi par git** et exclu du jar | vérifié |

Clés JSON du groupe, relevées en entier sur la fiche 2339793 (Samsung `SM-S936B/DS`) et sur la
fiche 2284539 (aucune clé supplémentaire), puis retrouvées sur les deux autres pour les clés
interrogées :

| Clé JSON | Exemple | Type | Remarque |
|---|---|---|---|
| `deviceType` | `SMARTPHONE` / `TABLET` | texte | à filtrer : le groupe mélange les deux |
| `energyClass` | `B` | texte | déjà lu comme champ typé d'`EprelProduct` |
| `energyClassRange` | `A_G` | texte | |
| `energyClassImage`, `energyClassImageWithScale` | `B-Left-MediumGreen-WithAGScale.svg` | texte | nom de fichier, pas une URL |
| `repairabilityClass` | `C` | texte | A-E |
| `repairabilityIndex` | `3.23` | nombre | échelle européenne 1,00-5,00, **pas** l'indice français sur 10 |
| `repeatedFreeFallReliabilityClass` | `A` | texte | A-E |
| `fallsWithoutDefect`, `fallsWithoutDefectFullyExtended` | `270`, `null` | nombre | le second pour les pliables |
| `batteryEnduranceInCycles` | `20` | nombre | **en centaines de cycles** : la fiche PDF affiche "2 000" |
| `batteryEndurancePerCycle` | `2618` | nombre | **en minutes** (43 h 38) |
| `batteryEndurancePerCycleInHours` | `43.63` | nombre | heures décimales : à préférer |
| `ingressProtectionRating` | `IP68` | texte | |
| `ingressProtectionRatingSolid`, `ingressProtectionRatingWater` | `DUST_TIGHT`, `CONT_IMMERSION` | texte | |
| `ingressProtectionRatingSolidNumber`, `ingressProtectionRatingWaterNumber`, `immersionDepthWater` | `6`, `8`, `1.5` | nombre | |
| `batteryUserReplaceable` | `false` | booléen | |
| `ratedBatteryCapacity` | `4755` | nombre | mAh |
| `minYearsSoftwareUpdates` | `5` | nombre | années |
| `guaranteeDuration` | `24` | nombre | mois |
| `operatingSystem` | `ANDROID` | texte | |
| `isFoldable`, `shippedWithProtectiveCover`, `chargerReceptacleType`, `chargerRequiredOutputPower` | `false`, `false`, `USB_C`, `10` | divers | |
| `sparePartScore`, `softwareUpdatesScore`, `repairInfoScore` | `PRIORITY`, `MINIMUM_YEARS_5`, `NO_COST` | énumération | sous-critères de l'indice |
| `toolsScore`, `fastenersScore`, `disassemblyDepthScore` | `2.65`, `3.1`, `2.9` | nombre | sous-critères de l'indice |
| `webLinkRepairInstructions`, `webLinkInfoSparePartsAvailability`, `webLinkPretaxPrices` | URL fabricant | texte | |
| `modelIdentifier`, `supplierOrTrademark`, `productModelCoreId`, `versionNumber`, `onMarketStartDate`, `implementingAct` | `SM-S936B/DS`, `Samsung`, ..., `[2024,12,24]`, `EU_2023_1669` | divers | champs communs |

Absents : `gtinIdentifier` dans les quatre réponses publiques ; `productGroup` dans les deux
réponses dont la liste complète des clés a été relevée (2339793, 2284539). Les noms de clés de
l'export ZIP sont supposés identiques : le parseur (`JsonZipEprelCatalogueParser`) lit la même
classe `EprelProduct`, et les synonymes existants (`ENERGYCLASS`, `MINGUARANTEEDSUPPORTYEARS`)
reprennent la casse de l'API. Ce point reste **non vérifié** pour ce groupe.

## 3. Identifiants et appariement

### 3.1 GTIN

Aucune des 4 fiches ne porte de clé `gtinIdentifier` dans l'API publique (mesure du 2026-11-08).
On ignore si l'export ZIP la contient quand le fournisseur l'a déclarée. L'appariement passera
donc surtout par marque et modèle, avec la chaîne existante :
`EprelSearchService.search(gtin, models, groups)` (GTIN, modèle exact, modèle sans espaces,
préfixe, meilleur préfixe, "contient"), puis `EprelCompletionService.selectUniqueResult`
(marque, preuve de modèle par score, rejet des variantes ambiguës, ADR 0001).

### 3.2 Formes de `modelIdentifier`

| Marque | `modelIdentifier` EPREL | Forme probable côté flux marchands |
|---|---|---|
| Samsung (smartphone) | `SM-S936B/DS` | `SM-S936BZKDEUB` (code couleur et région), ou "Galaxy S25+" |
| Samsung (tablettes) | `SM-X920`, `SM-X230` | idem |
| Apple | `A3094` (iPhone 15 Plus) | "iPhone 15 Plus", ou MPN `MU0D3ZD/A` |

Le suffixe `/DS` (double SIM) casse la preuve par inclusion : la forme compacte `SMS936BDS` n'est
pas contenue dans `SMS936BZKDEUB`. Le numéro réglementaire Apple (`A3094`) n'apparaît presque
jamais dans les titres marchands, et il couvre toutes les capacités et couleurs d'un modèle.
Ces deux marques dominent probablement la verticale : sans traitement dédié, une grande part des
smartphones resterait sans EPREL, donc exclue de l'ImpactScore V1.

### 3.3 Hypothèse à vérifier en premier : `productGroup`

> **Tranché le 2026-11-10 (GOU-2042) : hypothèse infirmée, aucun correctif.** Sur `eprel-products`
> (prod, rôle en lecture seule), l'agrégation `terms` sur `productGroup` renvoie
> `smartphonestablets20231669` pour 2 815 fiches (2 804 d'acte `EU_2023_1669`, dont 77 avec GTIN), et
> l'agrégation `missing` sur `productGroup` renvoie 0 fiche, tous groupes confondus. Le mapping réel pose
> `productGroup` en `keyword` cherchable ; un filtre `terms` `{"productGroup": ["smartphonestablets20231669"]}`
> renvoie bien les 2 815 fiches. Le faible taux d'appariement vient donc des identifiants (section 3.4),
> pas du filtre. Le texte ci-dessous est conservé pour mémoire.

`EprelSearchService.execute` filtre sur `productGroup` avec les valeurs de
`vertical.getEprelGroupNames()` (ici `smartphonestablets20231669`).
`EprelCatalogueService.processGroup` n'écrit jamais `group.urlCode()` dans les produits lus : il
s'en remet au champ `productGroup` du JSON exporté. Or ce champ est absent des deux réponses
publiques relevées en entier (2339793, 2284539). Les fixtures de test (`JsonZipEprelCatalogueParserTest`) et `tv.yml` (qui déclare à la
fois `televisions` et `Televisions`) laissent penser que l'export ZIP le contient pour les
anciens groupes, avec une casse variable.

Si l'export du groupe smartphones ne le contient pas, ou sous une autre forme
(`SMARTPHONE_TABLET_2023_1669` par exemple), **aucune recherche smartphone ne renvoie de résultat,
même avec tous les groupes indexés en prod**. Les 1,3 % de `CLASSE_ENERGY` relevés en mai sont
compatibles avec cette hypothèse.

Correctif si elle se confirme : dans `processGroup`, écrire `group.urlCode()` dans
`productGroup` quand il est vide, puis relancer `POST /eprel/index`. C'est une ligne, avec un
test sur une fixture sans `productGroup`.

Second point à vérifier au même moment : `EprelProduct.productGroup` est déclaré
`@Field(type = FieldType.Keyword, index = false)` (`model/.../eprel/EprelProduct.java`). Un
filtre `terms` sur un keyword non indexé ne peut s'appuyer que sur les doc values, selon la
version d'Elasticsearch et le mapping réellement posé. `GET eprel-products/_mapping` tranche :
si le champ n'est pas cherchable, le filtre échoue ou ne renvoie rien pour **tous** les groupes,
pas seulement les smartphones.

### 3.4 Règles proposées

1. Filtrer sur `deviceType = SMARTPHONE` pour la verticale `smartphones`, dans
   `selectUniqueResult` (la méthode `categoryPreference` existe déjà pour départager les groupes).
2. Samsung : comparer les racines de modèle. Côté EPREL, retirer `/DS`, `/DSN` ; côté produit,
   réduire `SM-XNNNNx` suivi de trois lettres de couleur et de région à `SM-XNNNNx`. À mesurer
   avant d'élargir à d'autres marques.
3. Apple : table de correspondance "nom commercial vers numéro A", puis
   `promoteModel(..., ModelCandidateSource.EPREL)` comme aujourd'hui. **Faite (GOU-2042)** :
   `verticals/src/main/resources/eprel-model-aliases/smartphones/apple.yml`, chargée dans
   `VerticalConfig.eprelModelAliases` (`EprelModelAliasTable`). Source : page Apple
   <https://support.apple.com/en-us/108044> (publiée le 2026-10-22) ; le numéro enregistré dans EPREL
   est celui des "autres pays et régions". Les 15 smartphones Apple d'EPREL (A3090 à A3720) y sont,
   ainsi que les iPhone 15 Pro et 15 Pro Max (sans fiche EPREL) pour que leur nom ne retombe pas sur
   l'iPhone 15. Chaque libellé du produit (titres d'offres, modèle, modèles alternatifs, MPN) vote :
   numéro A exact, base de référence Apple (`MU163` pour `MU163ZD/A`), ou nom commercial le plus long
   trouvé en mots entiers, sauf après "pour", "for", "compatible"... Il faut une majorité stricte des
   votes. `EprelCompletionService` ajoute le numéro trouvé en dernier candidat de modèle : un code
   fourni par un flux reste prioritaire. Maintenance annuelle décrite en tête du fichier.
4. Garder le rejet des ambiguïtés : un faux rattachement donnerait une classe de réparabilité
   fausse à un produit.

### 3.5 Mesures à faire lancer sur la prod (pas d'accès ES ici)

> **Mesure avant la table Apple (2026-11-10, GOU-2042)**, `products-moustik`, verticale `smartphones`,
> `offersCount > 0` : 14 767 produits, dont 928 appariés à EPREL (6,3 %, sans `missing_eprel`), tous notés.
> Réparabilité réelle (`REPAIRABILITY_INDEX` ou `EU_REPAIRABILITY_CLASS` dans `attributes.indexed`) : 126
> des 928 notés (13,6 %), dont 0 avec la classe européenne. Apple : 11 appariés sur 1 730, tous par un flux
> qui donnait déjà le numéro A comme modèle ; Samsung : 391 sur 3 750. Environ 420 smartphones Apple avec
> offres nomment dans leurs titres un modèle présent dans EPREL (`match_phrase` sur `offerNames`). La mesure
> après le premier batch bêta suivant le déploiement est publiée sur GOU-2042 et GOU-1891.

À lancer avec le rôle en lecture seule `agents_readonly` (GOU-1870) ou par un humain.

```json
GET eprel-products/_search
{ "size": 0,
  "aggs": {
    "groupes": { "terms": { "field": "productGroup", "size": 60, "missing": "(absent)" } },
    "actes":   { "terms": { "field": "implementingAct", "size": 60, "missing": "(absent)" },
                 "aggs": { "avec_gtin": { "filter": { "exists": { "field": "numericGtin" } } } } } } }
```

```json
GET products-moustik/_search
{ "size": 0,
  "query": { "bool": { "filter": [ { "term": { "vertical": "smartphones" } },
                                   { "range": { "offersCount": { "gt": 0 } } } ] } },
  "aggs": { "exclusions": { "terms": { "field": "excludedCauses", "size": 20 } } } }
```

Lecture attendue : nombre de fiches `EU_2023_1669`, valeur réelle de `productGroup` pour ces
fiches, part avec GTIN, et nombre de smartphones avec offres exclus pour `missing_eprel`. En
complément, `GET /eprel/search?gtin=0&model=A3094` (`EprelController`) montre la fiche stockée,
`productGroup` compris.

## 4. Modèle cible

Le mapping se fait par synonymes. `AttributesConfig.resolveFromProductAttribute` consulte les
identifiants Icecat, puis `synonyms[<datasourceName>]`, puis `synonyms.all`. Les blocs
`referentials.eprel.featureName` alimentent `byEprelFeatureGroup`, qui n'est lu nulle part dans la
résolution (vérifié par grep) : ils documentent sans mapper. Les noms entrants passent par
`IdHelper.normalizeAttributeName` (majuscules, sans accent, séparateurs conservés). Les
synonymes vont sous `synonyms.eprel`, pas sous `all`, pour éviter les collisions avec les
marchands.

Nouveaux attributs dans `verticals/src/main/resources/attributes/` :

| Clé (à créer) | Synonyme `eprel` | Type | Score |
|---|---|---|---|
| `EU_REPAIRABILITY_CLASS` | `REPAIRABILITYCLASS` | TEXT A-E, `numericMapping` (A 100, B 80, C 60, D 40, E 20) | `asScore: true`, `participateInScores: [REPARABILITY]` |
| `EU_REPAIRABILITY_INDEX` | `REPAIRABILITYINDEX` | NUMERIC 1,00-5,00 | non (la classe porte le score) |
| `FREE_FALL_RELIABILITY_CLASS` | `REPEATEDFREEFALLRELIABILITYCLASS` | TEXT A-E, `numericMapping` | `asScore: true`, `participateInScores: [DURABILITY]` |
| `BATTERY_ENDURANCE_CYCLES` | `BATTERYENDURANCEINCYCLES` (valeur x 100, section 5) | NUMERIC, cycles | `asScore: true`, `participateInScores: [DURABILITY]` |
| `BATTERY_ENDURANCE_PER_CYCLE` | `BATTERYENDURANCEPERCYCLEINHOURS` | NUMERIC, heures | non |
| `IP_RATING` | `INGRESSPROTECTIONRATING` | TEXT (`IP68`) | non |
| `BATTERY_USER_REPLACEABLE` | `BATTERYUSERREPLACEABLE` | TEXT oui/non | non |

Seuils de la classe de réparabilité (annexe II, tableau 4) : A pour R >= 4,00, B de 3,35 à 4,00,
C de 2,55 à 3,35, D de 1,75 à 2,55, E de 1,00 à 1,75.

Attributs existants :

- `CLASSE_ENERGY` : déjà alimenté par `addCoreEprelAttributes` (`energyClass`) et le synonyme
  `ENERGYCLASS`. Ajouter une entrée `referentials.eprel` pour ce groupe (documentation).
- `WARRANTY` : `guaranteeDuration` est déjà couvert par le synonyme `GUARANTEEDURATION`.
  `WarrantyParser` lit 24 hors de [0, 10] ans et le convertit en 2 ans (lecture du code ; à
  confirmer par un test).
- `MINAVAILABILITYSOFTWAREUPDATESYEARS` : ajouter `MINYEARSSOFTWAREUPDATES` (la clé smartphone
  diffère du `featureName` actuel `minAvailabilitySoftwareUpdatesYears`).
- `OPERATING_SYSTEM` : ajouter `OPERATINGSYSTEM` sous `synonyms.eprel` (le synonyme actuel porte
  un tiret bas).
- `REPAIRABILITY_INDEX` (indice français sur 10) : **ne pas** y mapper `REPAIRABILITYINDEX`.
  Mélanger deux échelles casserait `MINMAX_OBSERVED`.

Relation avec l'indice de réparabilité français : un arrêté du 26 juin 2025 abroge l'arrêté du
29 décembre 2020 fixant les critères de l'indice des téléphones mobiles, en s'appuyant sur le
règlement 2023/1669 (source AIDA/INERIS, version Légifrance **non vérifiée**). Pour les
smartphones, `EU_REPAIRABILITY_CLASS` remplace donc `REPAIRABILITY_INDEX`. EPREL couvre aussi
des modèles plus anciens encore vendus : la fiche Apple 2251145 indique une mise sur le marché
le 19 septembre 2023. La classe européenne couvrira donc une partie des modèles qui n'ont que
l'indice français.

Verticale `smartphones.yml` : ajouter les sept clés à `attributesConfig.configs`, à
`commonAttributes`, et `EU_REPAIRABILITY_CLASS`, `FREE_FALL_RELIABILITY_CLASS` à `ecoFilters`.
ImpactScore : `impactscores/smartphones.yml` pondère `REPAIRABILITY_INDEX` à 0,375 (indice
obsolète pour les modèles récents). D'après [`v2-design.md`](../../impactscore/v2-design.md)
section 9, les nouveaux critères n'entrent que dans les matrices V2, jamais dans la V1 gelée.
L'ajout à `availableImpactScoreCriterias` et la pondération relèvent de GOU-1880.

Champs `Product` : aucun nouveau, hors `sourceAttributions` (P0-4). `eprelDatas` et
`externalIds.eprel` existent déjà.

## 5. Pipeline

1. **Mesures** (section 3.5). Elles décident du point 2.
2. **Correctif `productGroup`** si nécessaire (section 3.3), puis réindexation du catalogue.
3. **Normalisation EPREL** dans `EprelCompletionService.getEprelAttributesFragments`, pour ce
   groupe uniquement :
   - `batteryEnduranceInCycles` multiplié par 100 avant émission (aucun multiplicateur
     n'existe dans `AttributeParserConfig`) ;
   - `batteryEndurancePerCycle` (minutes) ignoré au profit de `batteryEndurancePerCycleInHours` ;
   - sous-critères (`toolsScore`, `fastenersScore`...) et liens laissés dans `eprelDatas`, non
     projetés en attributs indexés.
4. **Appariement** : règles 3.4 dans `selectUniqueResult` et la table Apple.
5. **Attributs YAML** et verticale (section 4).
6. **Scores** : batch de 13 h. Aucun effet sur la V1 hors `CLASSE_ENERGY`, déjà pondéré, et hors
   la levée de `missing_eprel` pour les produits nouvellement rattachés. Ce second effet change
   le nombre de smartphones notés : le prévoir dans le suivi.
7. **Étiquette (média)** :
   - le front construit aujourd'hui `/images/eprel/{energyClassImage}`
     (`ProductImpactSubscoreGenericCard.vue`) ; aucun dossier `images/eprel` n'a été trouvé dans
     `frontend/` (servi d'ailleurs ? **non vérifié**) ;
   - proposition : garder le petit pictogramme de classe, et ajouter un lien vers l'étiquette
     complète (`.../labels?format=SVG` ou la fiche PDF) plutôt que d'embarquer un SVG de
     830 000 caractères. Le format PNG de l'endpoint est **non vérifié**.
8. **Front** : `ProductEprelDto.from` met `categorySpecificAttributes` à `null`, alors que
   `EprelDetailsTable.vue` (`getAttribute`) et `ProductPage.vue`
   (`resolveCategorySpecificAttribute`) le lisent. Par lecture du code, ces valeurs sont toujours
   vides côté front (à confirmer dans le navigateur). Les nouveaux critères passent donc par les
   attributs indexés et les scores, pas par ce DTO.
9. **Export ouvert** : colonnes `eu_energy_class` et `eu_repairability_class` dans
   `OpenDataService.GTIN_HEADER`, avec les quatre modifications habituelles (en-tête, champs
   requis, `toGtinEntry`, libellés i18n).
10. **devsec** : le fichier n'est pas suivi par git, donc l'alignement est une consigne locale,
    pas une PR. Liste à mettre (union des `eprelGroupNames` des verticales) : `televisions`,
    `electronicdisplays`, `ovens`, `washingmachines2019`, `washingmachines`, `washerdriers2019`,
    `dishwashers2019`, `dishwashers`, `refrigeratingappliances`, `refrigeratingappliances2019`,
    `professionalrefrigeratedstoragecabinets`, `airconditioners`, `smartphonestablets20231669`.
    Ou laisser la liste vide, comme en prod.

## 6. Planification et volumétrie

- Aucune nouvelle tâche planifiée : catalogue tous les deux jours à 3 h, rattachement au batch
  de 13 h (`api.eprelRefreshDays` = 1).
- Rattachement : environ 49 000 produits, jusqu'à six stratégies de recherche ES par produit
  (`EprelSearchService.search`), comme pour les autres verticales. Seuls les produits avec offres
  sont visités (`exportVerticalWithValidDate`).
- Taille de l'export ZIP du groupe : **non vérifiée**.

## 7. Sourcing transparent

- `open4goods-config/datasources/eprel.yml` **(à créer)**, sans bloc `csvDatasource` :
  `name: "EPREL (Commission européenne)"`, `portalUrl: https://eprel.ec.europa.eu`,
  `attribution: "Base européenne des produits pour l'étiquetage énergétique (EPREL)"`, licence
  "EPREL API Terms and Conditions" (champ d'attribution seulement).
- `SourceAttribution` par produit : `sourceRecordId = eprelRegistrationNumber`, `recordUrl =
  https://eprel.ec.europa.eu/qr/{numéro}` (redirige vers la fiche publique),
  `sourceUpdatedAt = publishedOnDateTS`, `retrievedAt` = date du rattachement.
- Fiche produit : la classe de réparabilité affiche "Source : EPREL" avec le lien QR et la date.

## 8. Tests et critères d'acceptation

- Fixtures : les 4 JSON publics (2339793, 2284539, 2472907, 2251145), plus une variante sans
  `productGroup`.
- Tests unitaires :
  - désérialisation `EprelProduct` : les clés du groupe arrivent dans
    `categorySpecificAttributes` ;
  - normalisation : 20 donne 2 000 cycles, la valeur en minutes est ignorée ;
  - résolution des synonymes : `REPAIRABILITYCLASS` donne `EU_REPAIRABILITY_CLASS`, et
    `REPAIRABILITYINDEX` ne donne jamais `REPAIRABILITY_INDEX` ;
  - `WarrantyParser` : 24 donne 2 ans ;
  - appariement : Samsung `SM-S936BZKDEUB` contre `SM-S936B/DS`, Apple "iPhone 15 Plus" contre
    `A3094`, tablette rejetée pour la verticale `smartphones` ;
  - import : un produit sans `productGroup` reçoit le code du groupe.
- Critères en beta :
  - part des smartphones avec offres portant `eprelDatas` mesurée avant et après, par marque ;
    seuil fixé après la première mesure ;
  - zéro fiche `TABLET` rattachée à un smartphone ;
  - échantillon de 50 rattachements vérifiés à la main (modèle, capacité sans effet) ;
  - distribution de `EU_REPAIRABILITY_CLASS` non dégénérée dans le rapport de scoring.
- Mesure de l'appariement : les endpoints de dry-run décrits par ADR 0003 sont absents de `main`
  (grep). `POST /completion/eprel/gtin/{gtin}` existe mais écrit le produit : le lancer sur beta
  sur un échantillon de 200 GTIN.

## 9. Découpage

1. Mesures prod de la section 3.5 (XS, humain ou Ops).
2. Correctif `productGroup` et test, si la mesure le justifie (S).
3. Consigne devsec et documentation de la liste des groupes (XS).
4. Attributs YAML, synonymes `eprel`, verticale `smartphones` (S).
5. Normalisation EPREL (cycles, `deviceType`) et tests (S).
6. Appariement Samsung et table Apple, avec mesure par marque (M).
7. Attribution P0-4 et lien vers l'étiquette (S, après P0-4).
8. Critères dans les matrices V2 (dans GOU-1880, pas ici).
9. Colonnes d'export ouvert (S).

## 10. Risques, challenges, questions

- **Le vrai sujet n'est pas le mapping mais le rattachement.** Sans GTIN dans les fiches
  publiques, et avec des numéros réglementaires (Apple) ou suffixés (Samsung), le taux de
  rattachement sera faible sans les règles 3.4. Comme `missing_eprel` exclut de l'ImpactScore V1
  et du sitemap de la verticale, chaque point de rattachement gagné ajoute des smartphones notés
  et des pages proposées aux moteurs. À arbitrer avec GOU-1881 : si le filtre EPREL est levé pour
  les smartphones en V2, ce plan redevient un enrichissement, sans effet sur la visibilité.
- **Hypothèse `productGroup`** : si elle se confirme, le "gain rapide" vient d'abord d'un
  correctif d'une ligne, et le même défaut peut toucher d'autres groupes récents
  (`tumbledryers20232534`).
- **Pièges d'unités** : centaines de cycles et minutes. Une erreur ici fausserait un score de
  durabilité sans alerte visible.
- **Données auto-déclarées** : les classes sont déclarées par le fabricant, sans contrôle
  préalable par un tiers. Afficher la source et la date (section 7) plutôt que présenter la
  classe comme un résultat de test.
- **Deux indices de réparabilité** : proposition, pour les smartphones, masquer
  `REPAIRABILITY_INDEX` dès qu'une `EU_REPAIRABILITY_CLASS` existe, comme pour l'indice de
  durabilité (plan 01).
- **Front muet** : les valeurs lues dans `categorySpecificAttributes` sont probablement vides
  depuis que le DTO les retire. À corriger avec ce plan ou séparément.
- Questions au propriétaire :
  1. Qui lance les deux requêtes de la section 3.5 (Ops avec `agents_readonly`, ou toi) ? Elles
     conditionnent l'effort (S ou M).
  2. Accepte-t-on une table Apple "nom commercial vers numéro A" maintenue à la main dans
     `verticals` (une centaine de lignes, une mise à jour par an) ?
  3. Pour les smartphones, la classe européenne remplace-t-elle l'indice français à l'affichage
     et dans la matrice V2, ou les montre-t-on côte à côte ?
