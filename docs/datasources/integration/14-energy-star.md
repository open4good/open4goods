---
title: "Plan d'intégration : ENERGY STAR (États-Unis)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : ENERGY STAR (États-Unis)

> Socle commun : [README](README.md). Fiche source :
> section 4.17 de l'inventaire interne des sources candidates (non publié).
> Mesures du 2026-11-08 sur les jeux Socrata de `data.energystar.gov` (tous mis à jour le
> 2026-11-08 selon le catalogue).

## 0. Fiche

**Verdict : non recommandé**, parce que :

- les appareils de nos verticales sont certifiés pour le marché nord-américain seulement : la
  colonne `Markets` vaut "United States" et/ou "Canada" sur 100 % des lignes TV, réfrigérateurs,
  lave-linge, lave-vaisselle, climatiseurs et cuisson ;
- 34 148 des 34 895 GTIN distincts du jeu consolidé des UPC (97,9 %) ont un préfixe GS1
  États-Unis/Canada (000 à 139) ;
- sondage sur notre catalogue : 2 UPC sur 100 existent chez nous, dont un faux positif (un GTIN
  déclaré par ENERGY STAR pour un lave-linge Finlux est chez nous un téléviseur Daewoo), et
  **0 sur 60** parmi les codes à préfixe non américain des catégories proches de nos
  verticales (sèche-linge compris) ;
- les valeurs énergétiques suivent les procédures fédérales américaines (kWh/an DOE) : elles ne
  sont pas comparables à l'étiquette énergie européenne et ne doivent jamais alimenter nos
  attributs `ENERGY_CONSUMPTION_*`.

Le plan minimal ci-dessous reste valable si l'un des déclencheurs de la section 10 se réalise.

| Champ | Valeur |
|---|---|
| Identifiant | `energy-star` |
| Mode | R (référentiel compact), appariement `GTIN_ONLY` avec garde-fou de catégorie |
| Priorité | non recommandé (inchangé) |
| Effort | S : moins d'un jour une fois le socle P0-5 livré (plan minimal) |
| Verticales | catégories équivalentes à `tv`, `refrigerator`, `washing-machine`, `dishwasher`, `air-conditioner` et `oven` (cuisson électrique) ; pas de `smartphones` (la catégorie "Telephony" couvre les téléphones fixes) |
| Volume | 39 875 UPC (jeu consolidé), 35 482 GTIN distincts valides ; recouvrement attendu de l'ordre de 1 % |
| Dépend de | P0-4 (attribution), P0-5 (socle) ; bloc `labels` (à créer, proposé par le plan 06) si on veut afficher la certification |

## 1. Apport

ENERGY STAR (EPA et DOE) publie les modèles certifiés, par catégorie, avec marque, référence,
UPC et mesures énergétiques : puissance en marche et en veille pour les TV et écrans,
consommation annuelle et écart au standard fédéral pour le froid, le lavage et la vaisselle,
type de réfrigérant et PRP pour le froid, fonctions connectées, indicateur "Most Efficient 2025".

Apport possible pour nudger :

- une certification d'efficacité reconnue, affichable comme label ;
- des attributs mesurés pour des modèles **mondiaux** (écrans, ordinateurs), là où la référence
  est la même des deux côtés de l'Atlantique.

Apport réel aujourd'hui : quasi nul pour nos verticales, parce que les références et les codes
d'électroménager sont propres au marché nord-américain. Exemple mesuré : les lave-linge LG du
jeu sont des références américaines à joker de couleur (`WKE100H*A`, `SWWE50*3`, UPC
`048231...` et `195174...`), sans équivalent direct dans les gammes européennes (**non vérifié**
modèle par modèle).

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Portail | `https://data.energystar.gov/` (Socrata), 53 jeux au catalogue | vérifié |
| API | SODA : `https://data.energystar.gov/resource/<id>.json` (requêtes `$select`, `$where`) | vérifié |
| Export complet | `https://data.energystar.gov/api/views/<id>/rows.csv?accessType=DOWNLOAD` | vérifié |
| Format | CSV UTF-8 avec en-tête, ou JSON | vérifié |
| Fréquence | `updatedAt` du 2026-11-08 sur tous les jeux listés : mise à jour quotidienne | vérifié (une observation) |
| Authentification | aucune ; quota sans jeton applicatif **non vérifié** | partiel |
| Licence | métadonnée "Public Domain" sur un seul jeu (`q8py-6w3f`) selon le document candidat | champ d'attribution seulement |

Jeux utiles, comptés le 2026-11-08 (GTIN valides en l'état, sans correction) :

| Jeu | Id Socrata | Lignes | Lignes avec UPC | Lignes avec un GTIN valide | GTIN distincts | dont préfixe US/CA |
|---|---|---|---|---|---|---|
| Televisions | `pd96-rr3d` | 175 | 85 | 85 | 124 | 123 |
| Residential Refrigerators | `p5st-her9` | 4 930 | 2 778 | 2 611 | 3 360 | 3 191 |
| Residential Freezers | `8t9c-g3tn` | 680 | 351 | 336 | 342 | 326 |
| Residential Clothes Washers | `bghd-e2wd` | 421 | 336 | 311 | 402 | 386 |
| Residential Dishwashers | `q8py-6w3f` | 752 | 631 | 608 | 949 | 730 |
| Room Air Conditioners | `5xn2-dv4h` | 608 | 466 | 456 | 489 | 485 |
| Residential Electric Cooking Products | `m6gi-ng33` | 264 | 184 | 183 | 366 | 354 |
| Displays (hors verticale) | `qbg3-d468` | 2 947 | 1 382 | 1 359 | 1 922 | 1 805 |
| **Certified Products UPC Codes** (toutes catégories) | `8edu-y555` | 39 875 | 39 875 | 38 527 | 34 895 | 34 148 |

Autres jeux à colonne `upc`, sans intérêt : Heat Pumps (`83eb-xbyy`, 284 446 lignes, 8 UPC),
Model Index (`8wj2-sec8`, 1 796 876 lignes, 12 282 UPC), Telephones (`bpzy-9tg8`, 285 lignes).

Le jeu **`8edu-y555`** suffit au plan minimal : une ligne par UPC, avec `Product ID`,
`Product Program Category`, `Product Type`, `Brand Name`, `Model Name`, `Model Number`, `UPC`,
`CB Model Identifier` et l'indicateur "Most Efficient 2025". Il couvre 32 catégories, dont
Computers (18 656 UPC, dont 12 847 portables et 2 034 tablettes), réfrigération (4 484),
Displays (2 115) et Dishwashers (1 044). Les jeux par catégorie ne servent qu'aux attributs.

Colonne `Markets` (pays de commercialisation déclarés) :

| Jeu | Valeurs |
|---|---|
| TV, réfrigérateurs, congélateurs, lave-linge, sèche-linge, lave-vaisselle, climatiseurs, cuisson | United States, Canada, et rien d'autre |
| Displays | United States 2 913, Canada 2 853, Taiwan 2 426, Japan 2 380, **Switzerland 1 618**, Europe 2 |

## 3. Identifiants et appariement

- Les UPC sont des UPC-A (12 chiffres) dans 94,5 % des lignes de `8edu-y555` (37 700 sur 39 875).
  967 codes n'ont que 11 chiffres (zéro de tête perdu, probablement par un tableur) : 854 d'entre
  eux redeviennent valides une fois complétés.
- **Aucune conversion à écrire** : `BarcodeValidationService.sanitize` complète tout code à
  13 chiffres avant le contrôle (vérifié dans
  `commons/src/main/java/org/open4goods/commons/services/BarcodeValidationService.java`), et
  `toProductId` renvoie l'identifiant numérique. Un UPC-A `609585253216`, sa forme EAN-13
  `0609585253216` et la forme tronquée à 11 chiffres désignent le même document produit. Avec
  ces 854 codes, on obtient 35 482 GTIN distincts.
- Plusieurs UPC par modèle dans les jeux par catégorie, séparés par `;` (211 lignes sur 752 pour
  les lave-vaisselle) : un enregistrement par UPC, comme `8edu-y555`. Les références contiennent
  des jokers (`WKE100H*A`) : un appariement par modèle devrait les traiter comme des motifs.
- Marque + modèle (`ReferenceMatcher`) : **à ne pas activer** pour l'électroménager. Les
  références nord-américaines diffèrent des références européennes, et un faux rattachement
  importerait des mesures américaines sur un produit européen. Exception possible : les écrans
  (section 10).

Recouvrement avec notre catalogue (sondage API publique, 2026-11-08, une requête par seconde sur
`https://nudger.fr/api/products/{gtin}`) :

| Échantillon | Taille | Existant | `excluded` | `offersCount > 0` | Avec verticale |
|---|---|---|---|---|---|
| UPC tirés au hasard dans `8edu-y555` | 100 | 2 | 1 | 1 | 1 (faux positif) |
| GTIN à préfixe non US/CA des catégories proches de nos verticales, sèche-linge compris (456 au total) | 60 | **0** | 0 | 0 | 0 |

Les deux produits trouvés :

- `8698902064415` : ENERGY STAR le déclare pour un lave-linge FINLUX `WM3225WCH` ; chez nous,
  c'est un téléviseur DAEWOO `50DM56QV` de la verticale `tv`, avec des données `eprel` et
  `icecat.biz`. Préfixe 869 (Turquie) : même fabricant probable, code réutilisé ou erroné.
  **Un appariement GTIN seul aurait collé des attributs de lave-linge sur un téléviseur.**
- `0609585253216` : écran PHILIPS `241B7QUPBEB`, une offre darty.com, hors verticale. C'est un
  vrai rattachement, sur un modèle mondial.

Mesure exacte à lancer sur l'ES de prod : `multiGet` des 35 482 GTIN (71 lots de 500, `_source`
réduit à `vertical`, `offersCount`, `excluded`, `base.bestName`), puis contrôle de cohérence de
catégorie sur les rattachements.

## 4. Modèle cible

Plan minimal : **aucun attribut énergétique projeté**.

- Ne jamais écrire dans `ENERGY_CONSUMPTION_ANNUAL`, `POWER_CONSUMPTION_*` ou `CLASSE_ENERGY` :
  procédures de test, unités de référence (kWh/an selon le DOE, puissance selon la procédure
  fédérale) et seuils diffèrent de l'étiquette énergie UE (EPREL). Le provider n'émet donc aucun
  synonyme vers ces clés.
- Projection d'une seule information : la certification.
  - Option recommandée : une entrée `labels` (`source: energy-star`, `labelId` = `Product ID`,
    `validUntil` vide), avec le bloc `Product.labels` (à créer) proposé par le plan 06.
  - Option sans ce bloc : attribut BOOLEAN `ENERGY_STAR_CERTIFIED` (à créer, sur le modèle de
    `FAST_FREEZE.yml`), déclaré dans `_default.yml`, `asScore: false`.
- "Most Efficient 2025" : indicateur conservé dans `ref-energy-star`, non projeté.
- Les mesures par catégorie (puissance en marche, kWh/an, réfrigérant, PRP) restent dans
  `ref-energy-star`, dans des champs préfixés `us_` pour éviter toute confusion. Elles sont
  consultables pour une future verticale écrans.

ImpactScore : aucune participation.

## 5. Pipeline

1. **Provider** (à créer) : `EnergyStarProvider implements ReferenceDatasetProvider`, dans
   `services/reference-datasets`.
   - `resolveDownloadUrls()` : export CSV de `8edu-y555` ; les jeux par catégorie sont en option
     de configuration (`reference-datasets.energy-star.category-datasets`), vides par défaut.
   - `read(Path)` : Jackson `CsvMapper`, un `ReferenceRecord` par UPC, `brand` = `Brand Name`,
     `models` = {`Model Number`, `Model Name`}, `attributes` = catégorie et type de produit,
     `payload` = `Product ID`, `CB Model Identifier`, indicateur "Most Efficient".
   - `sourceRecordId` = `Product ID + ":" + UPC` ; `recordUrl` = fiche produit ENERGY STAR
     (gabarit d'URL **non vérifié**), repli sur la page du jeu ; `sourceUpdatedAt` = date
     d'import (le jeu consolidé n'a pas de date par ligne).
2. **Import** : `ReferenceIndexService`, `ref-energy-star-<horodatage>`, `min-rows-ratio` 0,8.
3. **Projection** : `ReferenceProjectionService`, `match: GTIN_ONLY`, avec un **garde-fou de
   catégorie** à ajouter à `ReferenceMatcher` : si le produit a une verticale, la catégorie
   ENERGY STAR doit lui correspondre (table de correspondance dans le provider, par exemple
   `Televisions -> tv`, `Clothes Washers -> washing-machine`), sinon rejet et ligne au rapport.
   Le cas `8698902064415` sert de test.
4. **Scores**, **export ouvert** : rien.
5. **Front** : rien tant que le bloc `labels` n'existe pas.

## 6. Planification et volumétrie

- Cron hebdomadaire (`0 0 6 * * MON`) : la certification d'un modèle change rarement, et
  l'intérêt est faible.
- Import : 39 875 lignes, 5,4 Mo. Projection : 71 `multiGet` ; quelques centaines de produits
  au plus, d'après le sondage.

## 7. Sourcing transparent

- `open4goods-config/datasources/energy-star.yml` : `name: "ENERGY STAR (EPA, États-Unis)"`,
  `portalUrl: "https://data.energystar.gov/"`, `license` = valeur de la métadonnée Socrata quand
  elle existe, sinon vide, `attribution: "ENERGY STAR, U.S. EPA, via data.energystar.gov"`.
- `SourceAttribution` : `sourceRecordId`, `recordUrl`, `retrievedAt`. Le front doit préciser
  "certification américaine" : un utilisateur français ne doit pas prendre ce label pour une
  classe énergétique européenne.

## 8. Tests et critères d'acceptation

- Test unitaire du provider sur une fixture de 15 lignes de `8edu-y555` : UPC-A, EAN-13, code à
  11 chiffres récupérable, code à 11 chiffres invalide, GTIN-14, modèle à plusieurs UPC.
- Test de `ReferenceMatcher` avec le garde-fou de catégorie : `8698902064415` (lave-linge
  ENERGY STAR, téléviseur chez nous) doit être rejeté.
- Critères en beta : rapport de rattachement publié (nombre, catégories, rejets de catégorie) ;
  aucune valeur écrite dans un attribut `ENERGY_*` ou `POWER_*`.

## 9. Découpage

Plan minimal, à ne lancer que sur décision (section 10) :

1. Mesure du recouvrement réel sur l'ES de prod (`multiGet` des 35 482 GTIN) (S, une heure).
2. `EnergyStarProvider` sur `8edu-y555` et tests (S).
3. Garde-fou de catégorie dans `ReferenceMatcher` (S), utile aussi aux plans 04 et 06.
4. Projection vers le bloc `labels` ou vers `ENERGY_STAR_CERTIFIED` (S).

## 10. Risques, challenges, questions

Ce qui rendrait la source intéressante (déclencheurs explicites) :

1. **Recouvrement mesuré suffisant** : si le `multiGet` de prod trouve au moins 2 % des
   35 482 GTIN (environ 700 produits) **avec offre**, après contrôle de catégorie, le plan minimal
   se justifie pour la certification seule. Le sondage donne 1 vrai rattachement sur 100.
2. **Une verticale écrans (moniteurs)** : c'est le seul cas où la source a une vraie valeur.
   Displays compte 2 947 modèles, dont 1 618 déclarés vendus en Suisse. Les marques sont
   mondiales (PHILIPS 305 modèles, LG 266, BenQ 260, ViewSonic 246, ASUS 182) et les références
   souvent identiques en Europe (le seul vrai rattachement du sondage est un écran Philips).
   L'appariement marque + modèle y deviendrait pertinent, et les puissances mesurées (marche,
   veille) compléteraient EPREL.
3. **Une verticale ordinateurs portables ou tablettes** : 18 656 UPC Computers, références
   souvent mondiales (Microsoft, HP, ASUS, Acer, Apple, Lenovo). Elle croiserait aussi l'indice
   de réparabilité (plan 02), dominé par les portables.
4. **Un usage "label"** : si le bloc `labels` (plan 06) est créé, afficher "certifié
   ENERGY STAR" coûte presque rien, mais n'informe qu'un acheteur de produit importé.

Risques :

- **Collisions de GTIN entre marchés** : le seul rattachement de verticale du sondage est faux.
  Toute projection doit passer par le garde-fou de catégorie.
- **Confusion pour l'utilisateur** : ENERGY STAR n'a pas cours sur le marché UE de
  l'électroménager. Afficher ce label sans contexte serait trompeur.

Questions au propriétaire :

1. Une verticale écrans ou ordinateurs portables est-elle envisagée ? C'est la seule condition
   qui ferait passer ce plan de "non recommandé" à P3.
2. Faut-il lancer la mesure `multiGet` de prod (une heure) pour clore le sujet, ou abandonner la
   source sur la base du sondage ?
