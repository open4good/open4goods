---
title: "Plan d'intégration : Safety Gate (alerte rapide UE, produits non alimentaires)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : Safety Gate (alerte rapide UE, produits non alimentaires)

> Socle commun : [README](README.md), dont le prérequis P0-6 (bloc `recalls`), partagé avec le
> plan [03 RappelConso](03-rappelconso.md). Fiche source :
> section 4.5 de l'inventaire interne des sources candidates (non publié).
> Mesures du 2026-11-08 : historique complet des rapports hebdomadaires XML (2005 à S44 2026),
> API JSON de détail sur un échantillon, export RappelConso V2 du jour pour le dédoublonnage.

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | `safety-gate` |
| Mode | R (référentiel compact) + bloc `recalls` (P0-6), appariement `GTIN_THEN_BRAND_MODEL` limité aux catégories des verticales |
| Priorité | P2 (inchangée) ; à traiter juste après le plan 03, qui livre P0-6 |
| Effort | M : 3 à 4 jours, P0-6 non compris (livré par 03) |
| Verticales | aucune en pratique (une poignée de téléviseurs et de réfrigérateurs en quatre ans) ; masse hors verticale : cosmétiques, jouets, électrique |
| Volume | 47 526 alertes depuis 2005, environ 75 par semaine ; 22 882 GTIN distincts valides ; 8 % des GTIN récents déjà présents dans notre catalogue (sondage) |
| Dépend de | P0-4 (attribution), P0-5 (socle), P0-6 (bloc `recalls`), plan 03 (dédoublonnage) |

## 1. Apport

Safety Gate est le système d'alerte rapide de l'UE pour les produits non alimentaires
dangereux : chaque autorité nationale de surveillance du marché y notifie les produits
dangereux trouvés et les mesures prises. Par alerte, la source donne :

- la catégorie, le type de produit, la marque, le nom, les références de modèle, les lots, les
  codes-barres ;
- le type de risque (chimique, blessure, choc électrique, étouffement...) et le niveau
  ("Serious risk" pour 94 % des alertes depuis 2023) ;
- la description du danger et la non-conformité réglementaire (REACH, directive jouets...) ;
- les mesures prises (retrait du marché, rappel auprès des consommateurs, interdiction,
  retrait de l'annonce par la place de marché, refus à la frontière...) ;
- le pays notifiant, les pays ayant réagi, le pays d'origine, des photos, parfois le vendeur en
  ligne.

Ce que nudger y gagne :

- une information de sécurité sur des produits **que nous référençons déjà**. Exemples vérifiés
  le 2026-11-08 sur notre API publique : un casque audio pour enfants Hello Kitty (alerte suédoise
  SR/04119/25, risque environnemental), présent chez nous avec neuf sources marchandes dont
  cdiscount.com, darty.com, fnac.com et rakuten.com ; une rallonge électrique KOPP (alerte
  allemande, choc électrique) ; une tondeuse à cheveux Esperanza (alerte lettone) ; un vernis
  gel OPI ; une nappe PVC Soleil d'Ocre au DEHP (alerte luxembourgeoise de la semaine 44,
  marque écrite "Solei d'Ocre" dans l'alerte), présente chez nous avec six sources marchandes
  dont fnac.com, manomano.fr et darty.com ;
- un complément européen à RappelConso : 91 % des GTIN de Safety Gate depuis 2021 ne figurent
  pas dans RappelConso (section 3) ;
- un argument éditorial fort ("produit signalé dangereux par une autorité européenne"),
  absent de la plupart des comparateurs.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Liste des rapports | `https://ec.europa.eu/safety-gate-alerts/api/download/weeklyReport/list/xml/en` (392 Ko) | vérifié |
| Rapport d'une semaine | `.../api/download/weeklyReport/detail/xml/{reportId}?language=en`, URL donnée par la liste | vérifié |
| Profondeur d'historique | **1 123 rapports, de Report-2005-1 à Report-2026-44**, tous téléchargés et lus sans erreur (99 Mo) | vérifié |
| Fréquence | un rapport par semaine, publié le vendredi (`publicationDate` 06/11/2026 pour S44) | vérifié |
| Version française | `.../weeklyReport/list/xml/fr` et `?language=fr` sur le rapport : catégorie, produit, danger, mesures et niveau traduits ("Risque grave", "Retrait du produit du marché") | vérifié |
| API JSON de détail | `https://ec.europa.eu/safety-gate-alerts/public/api/notification/{id}?language=en` (ou `fr`), `id` = suffixe de l'URL `alertDetail/{id}` du rapport ; répond aussi pour les alertes anciennes (id négatifs, par exemple `-15181` de 2009) | vérifié (API publique non documentée) |
| Recherche JSON | `POST public/api/search` utilisé par le portail : refusé hors navigateur (HTTP 405) | **non vérifié** |
| Export de recherche | `api/download/search/xml/` et `api/download/search/excel/`, plafonnés à 5 000 alertes selon le code du portail | **non vérifié** |
| Export complet | aucun fichier unique : l'historique complet s'obtient par les 1 123 rapports | vérifié |
| Authentification | aucune | vérifié |
| Licence | catalogue data.europa.eu, distribution XML hebdomadaire : "Creative Commons CC0 1.0 Universal" | champ d'attribution seulement |

Structure XML d'une alerte (élément `notifications`), identique de 2005 à 2026 :
`order`, `caseNumber` (par exemple `SR/03088/26`, `A12/00505/23`, `0585/09`),
`publicationDate` (`JJ-MM-AAAA`), `reference` (URL `alertDetail`), `category`, `product`,
`brand`, `name`, `type_numberOfModel`, `batchNumber`, `barcode`, `riskType`, `danger`,
`measures` (texte concaténé), `URLrecall`, `description`, `notifyingCountry`,
`countryOfOrigin`, `type` (Consumer, Professional), `level`, `pictures/picture`,
`onlineTrader`, `companyRecallCode`, `productionDates`. Les champs vides sont présents
(`CDATA` vide), sauf dans une variante de type XSI (`webReportXmlWeeklyReportDetailContentDto`)
qui n'a ni `companyRecallCode` ni `productionDates`.

Ce que l'API JSON ajoute au XML :

- des **listes** au lieu de textes concaténés : `product.barcodes[]`, `product.brands[]`,
  `product.modelTypes[]`, `product.batchNumbers[]`, `product.photos[]` ;
- des mesures structurées : `measureCategory` (par exemple
  `PRODUCT_WITHDRAWAL_FROM_MARKET`), `measureType` (`COMPULSORY`, volontaire), opérateur,
  date d'entrée en vigueur ;
- `reactingCountries[]` (pays qui ont trouvé le produit chez eux : 8 alertes sur 20 de
  l'échantillon), `product.isCounterfeit`, `creationDate`, `modificationDate`, `corrigendum`.

## 3. Identifiants et appariement

### 3.1 Taux de GTIN valides

Mesure avec contrôle de la somme GTIN, sur l'historique complet :

| Période | Alertes | Alertes avec GTIN valide dans `barcode` | Alertes avec GTIN valide, lecture étendue (voir ci-dessous) | GTIN distincts, lecture étendue |
|---|---|---|---|---|
| 2005-2009 | 6 149 | 503 (8,2 %) | 1 556 (25,3 %) | 1 909 |
| 2010-2014 | 9 707 | 316 (3,3 %) | 3 366 (34,7 %) | 3 667 |
| 2015-2019 | 9 948 | 12 (0,1 %) | 4 205 (42,3 %) | 4 627 |
| 2020-2022 | 6 416 | 2 598 (40,5 %) | 2 602 (40,6 %) | 2 977 |
| 2023-2026 (S44) | 15 306 | 8 765 (57,3 %) | 8 765 (57,3 %) | 9 809 |
| **Total** | **47 526** | **12 194 (25,7 %)** | **20 494 (43,1 %)** | **22 882** |

- De 2010 à 2019, le champ `barcode` est presque toujours vide : 384 alertes sur 19 655 l'ont
  renseigné, 328 avec un GTIN valide. L'API JSON confirme `barcodeKnown: false` sur les 12 alertes
  de 2012 et 2017 testées. Le code n'a pourtant pas disparu : il est rangé dans `batchNumber`
  (surtout 2010-2019 : `5998083591643`, `3276004594853Batch: S2009075312`) ou dans
  `type_numberOfModel` (2005-2014 : `EAN 3283111807264`), dans le XML comme dans le JSON
  (`batchNumbers[]`, vérifié sur 4 alertes de 2014 à 2019). Lire aussi ces deux champs (codes de 12
  à 14 chiffres à somme valide, les codes à 8 chiffres y étant trop souvent des numéros de lot) fait
  passer l'historique de **13 690 à 22 882 GTIN distincts**. C'est la "lecture étendue" du tableau.
- **Quatre derniers rapports (S41 à S44 2026)** : 308 alertes, 147 avec un code de 8 à 14
  chiffres (le document candidat en comptait 146), mais **135 avec un GTIN à somme valide
  (43,8 %)**.
- 52 derniers rapports : 3 872 alertes, 1 871 (48,3 %) avec GTIN valide.

### 3.2 Pièges du champ `barcode`

- **Le XML concatène les codes sans séparateur** quand une alerte en porte plusieurs :
  `060894055943706089405603890608940572252` pour trois UPC, et jusqu'à dix EAN-13 collés. L'API
  JSON renvoie la liste propre (`["0608940559437", "0608940560389", "0608940572252"]`). Sur 20
  alertes multi-codes de 2024 à 2026, un découpage heuristique du XML (morceaux de 12 ou 13
  chiffres validés par la somme) retrouve la liste JSON 19 fois sur 20. L'échec : deux EAN-8 collés
  (`8085984080936251`).
- Codes préfixés par une référence (`C749G 5038024322933`), suivis d'un point-virgule, entourés
  d'espaces.
- Chaînes GS1 à identifiants d'application : `010360844990314121` est `(01)03608449903141(21)...`,
  dont le GTIN-14 `03608449903141` est valide. 30 alertes portent une chaîne de ce type
  sans autre GTIN valide.

Règles :

1. Le XML sert à la découverte (liste des alertes de la semaine, identifiants). **L'API JSON
   sert de source de parsing** pour les identifiants : `product.barcodes[]`.
2. Lecture étendue : codes de 12 à 14 chiffres à somme valide dans `batchNumbers[]` et
   `modelTypes[]` quand `barcodes[]` ne donne rien, **pour les alertes antérieures à 2020
   seulement**, ou après 2020 si le champ contient `EAN`, `barcode`, `bar code`, `GTIN` ou `UPC`.
   Depuis 2023, `batchNumber` contient de vrais numéros de lot, dont un sur dix passerait la
   somme de contrôle par hasard : sans cette borne, un faux GTIN mettrait un bandeau de sécurité
   sur le mauvais produit. La borne ne coûte presque rien (la lecture sans borne n'ajoutait que
   61 alertes après 2020). Le tableau de la section 3.1 applique cette règle.
3. Chaînes GS1 : extraire le GTIN-14 qui suit l'identifiant `(01)`.
4. Repli sur le découpage heuristique du XML si l'appel JSON échoue, signalé dans le rapport
   d'import.

### 3.3 Marque et modèle

Depuis 2023 (15 306 alertes) : marque vide ou "Unknown" dans 3 350 (22 %) alertes, modèle vide
dans 5 294 (35 %). 3 337 alertes sans GTIN ont une marque et un modèle exploitables.

`ReferenceMatcher` (marque via `BrandService.resolve`, puis modèle) ne s'applique qu'aux
catégories proches des verticales : "Electrical appliances and equipment" et "Communication and
media equipment" (1 889 alertes depuis 2023). La recherche inverse du matcher est bornée à une
verticale ; pour la masse hors verticale (cosmétiques, jouets), on reste au GTIN. Les recherches
par mots-clés dans les noms donnent, depuis 2023, trois téléviseurs (Technika, NordMende, Finlux)
et un réfrigérateur-congélateur (Good Home), aucun lave-vaisselle, aucun smartphone.

### 3.4 Recouvrement avec notre catalogue

Sondage API publique, 2026-11-08 : 100 GTIN tirés au hasard parmi les GTIN valides des alertes
2023-2026, interrogés sur `https://nudger.fr/api/products/{gtin}` à une requête par seconde.

| Résultat | Nombre sur 100 |
|---|---|
| Produit existant (HTTP 200) | **8** |
| dont `excluded: true` | 8 |
| dont `offersCount > 0` aujourd'hui | 0 |
| dont rattaché à une verticale | 0 |
| Absent (HTTP 404) | 92 |

Sept des huit produits ont eu des offres (sources cdiscount.com, rueducommerce.fr, darty.com,
fnac.com, Carrefour, Joom) ; le huitième (déodorant Borotalco, `0000080859840`) n'a aucune
source marchande. Aucun n'a d'offre active : retrait du marché ou simple fin de
référencement, la donnée ne permet pas de trancher. Extrapolation sur la population tirée
(9 809 GTIN des alertes 2023-2026) : environ 790 produits (intervalle de Wilson à 95 % : 4 % à
15 %, soit 400 à 1 470). Le taux est probablement plus faible sur l'historique ancien, dont les
produits ont quitté le marché. Mesure exacte à lancer sur l'ES de prod : `multiGet` de tous les
GTIN de `ref-safety-gate`, `_source` réduit à `vertical`, `offersCount`, `excluded`,
`datasourceCodes`.

### 3.5 Dédoublonnage avec RappelConso

Mesure sur l'export `rappelconso-v2-gtin-trie` du 2026-11-08 (18 862 lignes, 14 362 GTIN
distincts valides, publications depuis le 2021-03-31), comparé aux alertes Safety Gate publiées
depuis la semaine 13 de 2021 :

| Mesure | Valeur |
|---|---|
| GTIN Safety Gate depuis 2021-S13 | 11 623 |
| GTIN présents dans les deux sources | **1 059** (9,1 % des GTIN Safety Gate, 7,4 % des GTIN RappelConso) |
| Alertes notifiées par la France, avec GTIN | 567, dont 390 retrouvées dans RappelConso |
| Alertes notifiées par un autre pays, avec GTIN | 9 764, dont 88 retrouvées dans RappelConso |

Lecture : quand la France notifie, le rappel figure en général aussi dans RappelConso (environ
deux fois sur trois). Les alertes des autres pays sont presque toujours absentes de RappelConso.
C'est là que Safety Gate apporte une information nouvelle.

Règles de dédoublonnage (à appliquer à la projection, en coordination avec le plan 03) :

1. On ne fusionne jamais deux enregistrements de sources différentes : un produit peut porter
   une entrée `recalls` `rappelconso` et une entrée `safety-gate`. Chacune garde son lien, son
   texte et sa date.
2. On les relie quand le GTIN est commun et que les dates de publication sont à moins de
   180 jours l'une de l'autre, quel que soit le pays notifiant. Le front affiche alors un seul
   bandeau "Rappel" avec deux sources, sinon deux bandeaux. Champ proposé en section 10. Fenêtre
   mesurée sur les 1 059 GTIN communs (paires alerte-GTIN, écart minimal entre `publicationDate`
   et `date_publication`) :

   | Pays notifiant | Paires | Écart médian | 90e centile | Sous 90 jours | Sous 180 jours | Sous 365 jours |
   |---|---|---|---|---|---|---|
   | France | 955 | 76 jours | 193 jours | 68 % | 86 % | 97 % |
   | Autres pays | 121 | 34 jours | 117 jours | 83 % | 97 % | 98 % |

   Quand la France notifie, l'alerte européenne suit presque toujours le rappel français (943
   paires sur 955, médiane +76 jours). Une fenêtre de 90 jours raterait un tiers des paires
   françaises ; 180 jours en couvre 86 % (365 jours : 97 %, au prix de liens erronés possibles
   entre deux rappels distincts du même GTIN). La condition "notifiée par la France" est
   abandonnée : les recoupements avec les autres pays sont aussi proches dans le temps. Fenêtre
   configurable (`reference-datasets.safety-gate.recall-link-window-days`).
3. Le badge "rappel en cours" en France reste piloté par RappelConso (date de fin de procédure
   explicite) ; Safety Gate apporte le badge "alerte UE".

## 4. Modèle cible

Pas d'attribut. Le bloc `recalls` est défini une seule fois dans le README (P0-6), qui
intègre les champs propres à Safety Gate proposés en section 10 (`riskLevel`,
`measureCategories`, `notifyingCountry`, `reactingCountries`, `batchNumbers`, `models`,
`relatedRecallIds`, `language`). Il est livré par le plan 03.

Remplissage pour Safety Gate :

| Champ | Valeur Safety Gate |
|---|---|
| `source` | `safety-gate` |
| `recallId` | `caseNumber` (`SR/03088/26`), stable et affiché sur le portail |
| `url` | URL `alertDetail/{id}` du champ `reference` |
| `publishedAt` | `publicationDate` (JSON, horodatée) |
| `endsAt` | `null` : une alerte Safety Gate n'a pas de date de fin |
| `riskLabel` | types de risque et niveau, en français (`Risque chimique, Risque grave`) |
| `measures` | catégories de mesures, en français, séparées par `;` |
| `active` | règle proposée ci-dessous |

Règle `active` proposée (Safety Gate ne publie pas de fin d'alerte) : `true` pendant 24 mois
après `publishedAt`, puis `false` ; l'entrée reste dans la liste comme historique. Exception :
une alerte dont la seule mesure est "Import rejected at border" passe directement à `false`, car
le lot n'a pas atteint le marché (section 10). À valider par le propriétaire.

Champs supplémentaires nécessaires : voir section 10 (à remonter au plan 03 et au README).

## 5. Pipeline

1. **Provider** (à créer) : `SafetyGateProvider implements ReferenceDatasetProvider`, dans
   `services/reference-datasets`.
   - `resolveDownloadUrls()` : lit la liste XML (anglais), renvoie les URL des rapports, en
     anglais et en français.
   - Téléchargement : `RemoteFileCachingService.getResource(url, refreshInDays)`. Un rapport
     publié depuis plus de 8 semaines est figé (TTL long, 3 650 jours) ; les 8 derniers sont
     relus à chaque import (TTL 1 jour) pour attraper les rectificatifs (`report_addendum`,
     `corrigendum`). Les appels JSON par alerte passent par le même cache.
   - `read(Path)` : StAX ou Jackson XML sur chaque rapport, puis un appel JSON par alerte
     (anglais pour les identifiants, français pour les textes ; les rapports XML français
     suffisent pour les textes si l'on veut un seul appel JSON).
   - `ReferenceRecord` : `sourceRecordId` = `caseNumber` ; `gtins` = GTIN valides de
     `product.barcodes[]`, chaînes GS1 `(01)` comprises, puis lecture étendue de `batchNumbers[]` et
     `modelTypes[]` avant 2020 ou avec mot-clé (section 3.2) ; `brand` = premier élément de
     `brands[]` ; `models` = `modelTypes[]` + `nameSpecific` ; `attributes` = vide ; `payload` =
     champs du bloc `recalls` et extras de la section 10 ; `recordUrl` = URL `alertDetail` ;
     `sourceUpdatedAt` = `modificationDate` ; `state` selon la règle `active`.
2. **Import** : `ReferenceIndexService` reconstruit `ref-safety-gate-<horodatage>` à partir du
   cache à chaque exécution, vérifie `min-rows-ratio` (0,98 : l'historique ne doit pas rétrécir)
   et bascule l'alias.
3. **Projection** : `ReferenceProjectionService`, `multiGetForAggregation` par lots de 500 GTIN,
   puis `ReferenceMatcher` marque + modèle pour les deux catégories électriques et les verticales
   qui leur correspondent. Écriture de l'entrée `recalls` à partir du `payload`, pas de
   `DataFragment` d'attributs. Attribution, hash, réindexation. Une alerte retirée du portail
   (rare) retire l'entrée.
4. **Dédoublonnage** : règles de la section 3.5, appliquées après la projection des deux
   sources (`rappelconso` puis `safety-gate`).
5. **Scores** : aucun. Une alerte ne modifie pas l'ImpactScore (question en section 10).
6. **Export ouvert** : pas de colonne propre. Les colonnes communes `recall_active` et
   `recall_urls` (README, P0-6) couvrent toutes les sources de rappel. L'URL d'une alerte
   Safety Gate y figure comme les fiches RappelConso.
7. **Front** : bandeau "Alerte de sécurité européenne" (texte français : risque, mesures, pays
   notifiant, date, lien vers l'alerte), commun avec le bandeau RappelConso du plan 03.
   Composant à identifier dans `frontend/app/components/product/` **(non vérifié)**. Les fiches
   concernées étant `excluded`, même question que pour l'Ecolabel (plan 06) : sont-elles
   servies ?

## 6. Planification et volumétrie

- Cron `0 0 7 * * SAT` (rapport publié le vendredi), plus
  `POST /reference-datasets/safety-gate/import` à la demande.
- Import initial : 1 123 rapports en anglais (99 Mo, environ 30 minutes avec une pause de 0,3 s
  entre requêtes, mesuré), autant en français, puis un appel JSON par alerte. Pour 47 526 alertes à
  2 requêtes par seconde, compter environ 6 à 7 heures, une seule fois. Variante plus légère :
  JSON seulement pour les alertes depuis 2020 qui ont un `barcode` non vide (environ 12 350),
  le reste en XML (la lecture étendue fonctionne aussi sur `batchNumber` et
  `type_numberOfModel` du XML).
- Import hebdomadaire : le nouveau rapport en anglais et en français, les 8 semaines précédentes
  relues, environ 75 appels JSON : quelques minutes.
- Projection : environ 46 `multiGet` de 500 GTIN, puis la passe marque/modèle sur les
  catégories électriques (quelques centaines de recherches).
- Cache : environ 200 Mo de XML (anglais et français) plus le JSON. Ces fichiers sont voulus
  permanents : les ranger dans un sous-dossier dédié du cache, exclu de tout nettoyage par âge.

## 7. Sourcing transparent

- `open4goods-config/datasources/safety-gate.yml` : `name: "Safety Gate (Commission
  européenne)"`, `portalUrl: "https://ec.europa.eu/safety-gate-alerts/"`, `license: "CC0 1.0"`,
  `licenseUrl: "https://creativecommons.org/publicdomain/zero/1.0/"`, `attribution: "Safety
  Gate, système d'alerte rapide de l'UE pour les produits non alimentaires dangereux, Commission
  européenne"`.
- `SourceAttribution` par produit : `sourceRecordId` (`caseNumber`), `recordUrl` (page de
  l'alerte), `sourceUpdatedAt` (`modificationDate`), `retrievedAt`.
- Le bandeau affiche toujours le pays notifiant et la date : une alerte vient d'une autorité
  nationale, pas de nudger.

## 8. Tests et critères d'acceptation

- Fixtures tirées des vrais fichiers : un rapport 2026 (alerte à codes concaténés, marque vide,
  `URLrecall` renseigné), un rapport 2009 (format ancien, id négatif), la version française du
  même rapport, trois réponses JSON (liste de codes, chaîne GS1 `(01)`, deux EAN-8).
- Test du provider : identifiants égaux à la liste JSON sur les 20 alertes multi-codes de la
  mesure ; repli heuristique testé sur le cas des deux EAN-8.
- Test de projection : écriture et retrait d'une entrée `recalls` ; règle `active` (24 mois, refus à
  la frontière) ; double entrée RappelConso + Safety Gate reliée par la fenêtre de 180 jours.
- Critères en beta :
  - 1 123 rapports importés, zéro erreur de lecture ;
  - au moins 22 882 GTIN dans `ref-safety-gate` ;
  - taux de rattachement publié, attendu entre 4 % et 15 % des GTIN ;
  - nombre de produits créés publié dans le rapport (`CREATE` par défaut depuis le 2026-11-09),
    et aucun produit créé sur un code rejeté par P0-9.

## 9. Découpage

1. (plan 03) P0-6 : `ProductRecall`, `Product.recalls`, mapping ES, écriture par
   `ReferenceProjectionService` (M, compté dans 03).
2. Champs supplémentaires de `ProductRecall` (section 10), décidés avec le plan 03 (S).
3. `SafetyGateProvider` : liste, rapports anglais et français, cache, JSON, extraction des GTIN,
   tests (M).
4. Règle `active` et dédoublonnage RappelConso (S).
5. Passe marque/modèle limitée aux catégories électriques (S, réutilise `ReferenceMatcher`).
6. Colonne d'export ouvert et bandeau front commun avec 03 (S).

## 10. Risques, challenges, questions

- **Champs propres à Safety Gate**, intégrés depuis dans la définition unique de `ProductRecall`
  (README, P0-6) :
  - `riskLevel` : "Serious risk" ou autre niveau ; sans lui, une alerte mineure s'affiche comme
    une alerte grave ;
  - `riskTypes` (liste) : pour filtrer ("chimique", "choc électrique") au lieu de parser
    `riskLabel` ;
  - `measureCategories` (liste d'énumérations JSON, par exemple
    `PRODUCT_WITHDRAWAL_FROM_MARKET`) : la règle `active` en dépend ;
  - `notifyingCountry` et `reactingCountries` : indispensables pour l'affichage et pour la règle
    de dédoublonnage ;
  - `batchNumbers` et `models` : portée réelle du rappel (voir le risque suivant) ;
  - `productCategory` (catégorie Safety Gate) ;
  - `relatedRecallIds` : lien entre une entrée RappelConso et une entrée Safety Gate du même
    produit ;
  - `language` des textes (`fr` traduit par la Commission ou repli `en`).
- **Rappels limités à un lot** : 5 065 alertes depuis 2023 ont un numéro de lot, dont 4 022 avec
  un GTIN valide. Un GTIN désigne toute une production : badger toutes les unités pour un lot
  défectueux est excessif. Proposition : afficher "certains lots" avec les numéros quand
  `batchNumbers` est renseigné.
- **Refus à la frontière** : 1 092 alertes depuis 2023 mentionnent "Import rejected at border"
  (360 avec un GTIN valide), dont 907 avec cette seule mesure. Le lot n'est pas entré dans l'UE ;
  le même GTIN peut pourtant être vendu en ligne. Proposition : entrée conservée,
  `active = false`, mention "lot refusé à la frontière".
- **Pas de fin d'alerte** : la règle des 24 mois est arbitraire. Alternative : `active` tant que
  le produit a des offres. Question 2.
- **Qualité des GTIN** : codes réutilisés ou faux (contrefaçons, champ `isCounterfeit`), et
  produits sous licence (Hello Kitty) dont plusieurs fabricants peuvent partager l'habillage.
  Le plan 14 a montré un GTIN qui désigne un lave-linge dans une source et un téléviseur chez
  nous : un contrôle de cohérence
  (catégorie Safety Gate contre verticale ou catégories marchandes du produit) doit filtrer les
  rattachements GTIN aberrants avant d'afficher un bandeau de sécurité.
- **Manques du socle** :
  - le socle suppose un fichier complet par import ; Safety Gate est une série de 1 123 fichiers
    figés plus une API par alerte. Le cache par URL de `RemoteFileCachingService` suffit si
    `resolveDownloadUrls()` peut renvoyer plus d'un millier d'URL et si le provider peut appeler
    une API pendant `read`. À écrire noir sur blanc dans le contrat de `ReferenceDatasetProvider` ;
  - `ReferenceMatcher` ne fait de recherche inverse que dans une verticale : pas d'appariement
    marque + modèle possible pour la masse.
- **Photos** : URL publiques (`public/api/notification/image/{id}`), en moyenne 2,9 par alerte
  depuis 2022. Les URL sont gardées dans `ref-safety-gate` (agrégation maximale) mais pas
  affichées : une photo de produit dangereux sur la fiche demande une décision éditoriale.
- **Questions au propriétaire** :
  1. Une alerte de sécurité doit-elle pénaliser l'ImpactScore ou le classement d'un produit, ou
     rester une information affichée ? Proposition : information seulement.
  2. Règle `active` : 24 mois après publication, ou tant que le produit a des offres ?
  3. Faut-il afficher les alertes sur des produits sans offre (les 8 produits trouvés n'en ont
     aucune) ? C'est pourtant là que l'information protège un acheteur passé.
