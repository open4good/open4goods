# ImpactScore lot 1a : mesures en production (2026-11-10)

Issue GOU-1875, sous-issue de GOU-1864. Mesures **en lecture seule** sur Elasticsearch de production via la connexion
« Elasticsearch lecture (Dev) ». Aucune écriture.

- **Date des mesures :** 2026-11-10, entre 08:05 et 08:25 (heure serveur), hors du batch de 13:00 à 15:00.
- **Index :** `products-moustik` (124 729 378 documents au total, dont 142 374 pour les 7 verticales) et `eprel-products` (2 353 842).
- **Poids :** poids `v2.criteriasPonderation` de `verticals/src/main/resources/verticals/impactscores/<verticale>.yml`
  (sans `DATA_QUALITY`, somme = 1), tels que présents dans la branche `main` au moment des mesures.
- **Charge :** environ 1 150 documents lus (plafond : 20 000 par heure), le reste en agrégations et comptages.
  Une seule requête à la fois, pause d'au moins une seconde entre les pages.
- **Annexes :** `lot1-echantillon-<verticale>.csv` (7 fichiers de 30 produits), `lot1-annexe-eprel-smartphones.md`,
  `lot1-annexe-durabilite-cles.md`, `lot1-annexe-requetes.md` (générateur des requêtes de motifs de présence).

## 0. Résumé et recommandations

Définitions : « population A » = produits `excluded=true`, `excludedCauses` contenant `missing_eprel` et aucune autre cause
(`missing_brand`, `missing_model`, `missing_attr_*`), avec `offersCount > 0`. « Population B » = produits notés
(`excluded=false`, `offersCount > 0`). C = couverture réelle pondérée (poids présents / somme des poids).

| Verticale | Pop. A | C ≥ 50 % (pop. A) | Fiches qui entreraient à C ≥ 50 % | « Vrais produits » (échantillon de 30) | Recommandation |
|---|---:|---:|---:|---:|---|
| smartphones | 8 661 | 11,6 % | **1 005** (pop. B : 928) | 80 % (24/30) | **Conditionnel** : seuil 0,50, après relecture ciblée des fiches C ≥ 0,5 (non couvertes par l'échantillon) |
| oven | 6 819 | 1,2 % | 82 | **0 %** (0/30 ; 13 % avec « limite ») | **Non** : le catalogue exclu est du petit électroménager (raclettes, planchas, woks) |
| refrigerator | 128 | 100 % | **128** (pop. B : 2 350) | 93 % (28/30) | **Oui**, seuil 0,50 (C ≥ 0,76 sur toute la population) |
| dishwasher | 305 | 16,7 % | **51** (pop. B : 1 025) | 63 % (87 % avec « limite ») | **Oui, avec réserve**, seuil 0,60 ; écarter les modèles professionnels |
| washing-machine | 779 | 6,4 à 9,9 % | 50 à 77 (pop. B : 1 466) | 47 % (80 % avec « limite ») | **Non** pour l'instant : peu de fiches, pollution forte (sèche-linge, fers à repasser) |
| tv | 176 | 0 % (C max 0,437) | 0 | 70 % (90 % avec « limite ») | **Non** : aucune fiche n'atteint 0,5 |
| air-conditioner | 211 | 6,2 % | 13 (pop. B : 76) | 77 % (87 % avec « limite ») | **Non** : 13 fiches, aucun gain |

Lecture :

1. **Le filtre `missing_eprel` ne peut pas être levé globalement.** Sans EPREL, la couverture réelle est quasi nulle
   dans 5 verticales sur 7 (médiane de C = 0 pour smartphones, fours, climatiseurs ; moyenne 0,05 à 0,19). Les critères
   lourds de chaque verticale (consommation, eau, durée de support) viennent d'EPREL.
2. **Le filtre sert bien de filtre qualité.** Les fours exclus sont à 87 % hors sujet, les smartphones à 20 %
   (claviers, jumelles, luminaires), les lave-linge à 17 % (fers à repasser, lave-vaisselle) avec en plus 33 % de cas
   limites (sèche-linge, séchants, professionnels).
3. **Gain réel seulement sur 3 verticales** : réfrigérateurs (128 fiches, qualité bonne), lave-vaisselle (51) et, sous
   condition, smartphones (environ 1 005 fiches, soit 2,1 fois la liste actuelle de 928).
4. **Les réfrigérateurs exclus sont surtout exclus pour `missing_attr_CLASSE_ENERGY`** (2 143 des 2 297 exclus avec offre),
   pas pour EPREL : lever le filtre EPREL ne les fait pas entrer. Même constat pour les TV (`missing_attr_DIAGONALE_POUCES`).
5. **Le seuil provisoire 0,50 de `minRealWeightCoverage`** convient aux 3 verticales où l'on lève le filtre. Pour les
   notés actuels (pop. B) il est déjà satisfait presque partout (voir §3.3).

Limites des mesures (à lire avant d'approuver) : échantillons de 30 (intervalle de confiance d'environ ±15 points) ;
échantillons non aléatoires (voir §4) ; distribution de C **exacte** pour smartphones, fours, réfrigérateurs et
climatiseurs, **encadrée** pour lave-linge, lave-vaisselle et TV ; pas de décile pour ces trois dernières ; distribution
de C des notés (pop. B) mesurée seulement pour smartphones, fours et réfrigérateurs.

## 1. Effectifs par verticale

Requêtes (outil `aggregate`, index `products-moustik`), avec `V = ["air-conditioner","dishwasher","oven","refrigerator","smartphones","tv","washing-machine"]` :

```json
// total + excluded
query {"terms":{"vertical":V}}
aggs  {"v":{"terms":{"field":"vertical","size":20},"aggs":{"excl":{"terms":{"field":"excluded","size":2}}}}}
// avec offre : même agrégation, query {"bool":{"filter":[{"terms":{"vertical":V}},{"range":{"offersCount":{"gt":0}}}]}}
```

| Verticale | Total | Avec offre | Notés (`excluded=false`, offre) | Exclus (total) | Exclus avec offre | Exclus sans offre |
|---|---:|---:|---:|---:|---:|---:|
| smartphones | 71 991 | 14 767 | 928 | 70 430 | 13 839 | 56 591 |
| oven | 30 856 | 9 214 | 856 | 29 282 | 8 358 | 20 924 |
| refrigerator | 14 352 | 4 647 | 2 350 | 10 331 | 2 297 | 8 034 |
| tv | 10 184 | 1 632 | 1 345 | 6 973 | 287 | 6 686 |
| washing-machine | 9 400 | 2 779 | 1 466 | 6 297 | 1 313 | 4 984 |
| dishwasher | 4 171 | 1 470 | 1 025 | 2 405 | 445 | 1 960 |
| air-conditioner | 1 420 | 322 | 76 | 1 183 | 246 | 937 |

Exclus sans offre = exclus total − exclus avec offre (mesuré séparément avec `must_not offersCount > 0` : mêmes totaux).
Exclus avec offre = exclus parmi « avec offre » (colonne 3 − colonne 4).

## 2. Causes d'exclusion

Requête : `query {"bool":{"filter":[{"terms":{"vertical":V}},{"term":{"excluded":true}},<offre>]}}`,
`aggs {"v":{"terms":{"field":"vertical"},"aggs":{"c":{"terms":{"field":"excludedCauses","size":100}}}}}`, avec
`<offre>` = `{"range":{"offersCount":{"gt":0}}}` ou `must_not` de ce filtre. Un produit peut cumuler plusieurs causes.

| Verticale | Offre | `missing_eprel` | `missing_model` | `missing_brand` | `missing_attr_*` |
|---|---|---:|---:|---:|---|
| smartphones | avec | 13 839 | 5 159 | 55 | |
| | sans | 56 591 | 32 380 | 10 454 | |
| oven | avec | 8 358 | 1 428 | 125 | |
| | sans | 20 924 | 6 425 | 1 324 | |
| refrigerator | avec | 2 292 | 819 | 33 | `CLASSE_ENERGY` 2 143 |
| | sans | 8 020 | 3 042 | 390 | `CLASSE_ENERGY` 6 988 |
| tv | avec | 287 | 22 | 1 | `DIAGONALE_POUCES` 108 |
| | sans | 6 686 | 1 700 | 3 271 | `DIAGONALE_POUCES` 4 585 |
| washing-machine | avec | 1 313 | 532 | 8 | |
| | sans | 4 984 | 1 096 | 124 | |
| dishwasher | avec | 445 | 138 | 5 | |
| | sans | 1 960 | 309 | 39 | |
| air-conditioner | avec | 246 | 35 | 0 | |
| | sans | 937 | 147 | 8 | |

**Exclus sans cause :** requête `excluded=true` + `must_not exists excludedCauses` sur les 7 verticales : **0**. Le cas
trouvé par GOU-1700 n'existe pas aujourd'hui sur ces verticales (non mesuré sur les autres verticales).

Particularité : 5 réfrigérateurs exclus avec offre (2 297 − 2 292) et 14 sans offre n'ont pas `missing_eprel`.

**Exclus seulement faute d'EPREL** (`excludedCauses = {missing_eprel}` exactement) :

```json
{"bool":{"filter":[{"terms":{"vertical":V}},{"term":{"excluded":true}},{"term":{"excludedCauses":"missing_eprel"}}],
 "must_not":[{"term":{"excludedCauses":"missing_brand"}},{"term":{"excludedCauses":"missing_model"}},{"prefix":{"excludedCauses":"missing_attr_"}}]}}
```

avec agrégation `range` sur `offersCount` (`< 1` / `≥ 1`) :

| Verticale | Avec offre (pop. A) | Sans offre |
|---|---:|---:|
| smartphones | 8 661 | 20 134 |
| oven | 6 819 | 13 992 |
| washing-machine | 779 | 3 819 |
| tv | 176 | 2 003 |
| dishwasher | 305 | 1 644 |
| refrigerator | 128 | 1 022 |
| air-conditioner | 211 | 786 |
| **Total** | **17 079** | 43 400 |

## 3. Couverture possible sans EPREL

### 3.1 Chemins et critères

`get_mapping` (préfixe `attributes.indexed`) : l'index est en `dynamic: strict`. Chaque attribut a les sous-champs
`value` (keyword) et, pour les numériques, `numericValue`. La présence se teste par
`{"exists":{"field":"attributes.indexed.<CLE>.value"}}`. Clés mappées utiles : `CLASSE_ENERGY`, `CLASSE_ENERGY_SDR`,
`CLASSE_ENERGY_HDR`, `POWER_CONSUMPTION_{TYPICAL,OFF,SDR,HDR,STANDBY,STANDBY_NETWORKD}`, `REFRIGERANT_GWP`,
`REPAIRABILITY_INDEX`, `WARRANTY`, `WEIGHT`, `WATER_CONSUMPTION`, `ENERGY_CONSUMPTION_100_CYCLES`, `NOISE_LEVEL`,
`OVEN_ENERGY_CONSUMPTION_CYCLE`, `MINAVAILABILITYSPAREPARTSYEARS`, `MINAVAILABILITYSOFTWAREUPDATESYEARS`,
`MINGUARANTEEDSUPPORTYEARS`, `DIAGONALE_POUCES`. Aucun `DATA_QUALITY` ni indice de durabilité dans `attributes.indexed`.

**Critères calculés (traités à part) :** `DATA_QUALITY` est un score (`scores.DATA_QUALITY`), pas un attribut : il ne
peut pas être « présent » par `attributes.indexed`. Le batch ne le calcule que pour `offersCount > 0 && !excluded`.
Parmi la pop. A, il n'existe que sur un résidu de valeurs anciennes (smartphones 8/8 661, oven 95/6 819, washing-machine
74/779, dishwasher 20/305, tv 34/176, refrigerator 21/128, air-conditioner 0/211). Les notes de marque (`BRAND_*`) ne
figurent dans aucun `criteriasPonderation` de ces verticales.

**C avec et sans `DATA_QUALITY` au dénominateur :** les poids v2 sont les poids v1 sans `DATA_QUALITY`, renormalisés
(vérifié dans les `.yml`). Avec `DATA_QUALITY` au dénominateur et absent, `C_avec = C_sans × (1 − w_DQ)`, avec w_DQ
(v1) = smartphones 0,25 ; oven 0,15 ; refrigerator 0,15 ; tv 0,13 ; washing-machine 0,12 ; dishwasher 0,12 ;
air-conditioner 0,0619. Exemple : smartphones, seuil 0,5 avec `DATA_QUALITY` revient à C_sans ≥ 0,667.

### 3.2 Pop. A : présence de chaque critère (part de produits)

Requête : filtre pop. A + `terms vertical` + un `filter exists` par critère (21 critères en 2 appels). Effectifs :

| Critère | smartph. (8 661) | oven (6 819) | wash. (779) | dish. (305) | AC (211) | TV (176) | fridge (128) |
|---|---:|---:|---:|---:|---:|---:|---:|
| `CLASSE_ENERGY` | 749 (8,6 %) | 306 (4,5 %) | 210 (27 %) | 74 (24 %) | 14 (6,6 %) | n/a | 128 (100 %) |
| `WARRANTY` | 2 744 (32 %) | 1 014 (15 %) | 172 (22 %) | 76 (25 %) | 7 (3,3 %) | 62 (35 %) | 70 (55 %) |
| `WEIGHT` | 2 612 (30 %) | 2 212 (32 %) | 254 (33 %) | 76 (25 %) | 22 (10 %) | n/a | 108 (84 %) |
| `REPAIRABILITY_INDEX` | 841 (9,7 %) | 8 (0,1 %) | 19 (2,4 %) | 25 (8,2 %) | 0 | 16 (9,1 %) | n/a |
| `POWER_CONSUMPTION_TYPICAL` | 5 | 23 | 8 | 0 | 18 (8,5 %) | 83 | 0 |
| `POWER_CONSUMPTION_OFF` | 1 | 50 | 104 (13 %) | 44 (14 %) | 0 | 99 (56 %) | 1 |
| `WATER_CONSUMPTION` | 0 | 0 | 156 (20 %) | 67 (22 %) | 0 | 0 | 0 |
| `ENERGY_CONSUMPTION_100_CYCLES` | 0 | 0 | 140 | 51 (17 %) | 0 | 0 | 0 |
| `NOISE_LEVEL` | 0 | 0 | 143 (18 %) | 76 (25 %) | 8 | 0 | 125 (98 %) |
| `OVEN_ENERGY_CONSUMPTION_CYCLE` | | **0** | | | | | |
| `REFRIGERANT_GWP` | 0 | 0 | 0 | 0 | **0** | 0 | 0 |
| `CLASSE_ENERGY_SDR` / `_HDR` | | | | | | 89 / 74 | |
| `DIAGONALE_POUCES` | 2 712 (hors v2) | | | | | 176 (100 %) | |
| `POWER_CONSUMPTION_SDR/HDR/STANDBY_NETWORKD`, `MINAVAILABILITY*`, `MINGUARANTEEDSUPPORTYEARS` | | | | | | **0** | |

Les critères à poids fort dont la valeur est absente chez tous les exclus faute d'EPREL : `OVEN_ENERGY_CONSUMPTION_CYCLE`
(0,41 des fours), `POWER_CONSUMPTION_SDR/HDR` et `MINAVAILABILITY*` (0,42 cumulés des TV), `REFRIGERANT_GWP` (0,11 des
climatiseurs). Les réfrigérateurs de la pop. A ont tous `CLASSE_ENERGY` par construction (`missing_attr_CLASSE_ENERGY`
exclu de la pop. A), d'où C ≥ 0,647.

### 3.3 Distribution de C

Calcul : comptage des motifs de présence par requêtes `filter` (un `bool exists/must_not exists` par combinaison de
critères ; générateur dans `lot1-annexe-requetes.md`), puis somme des poids hors requête. Cette méthode est **exacte sur
la population entière** (pas d'échantillon) et n'utilise aucun document, au prix de 2^k motifs ; elle remplace le
parcours `_source` par pages de 500, qui aurait lu 17 079 + 8 046 documents pour une seule heure de quota.

Motifs de présence mesurés (clé = présence dans l'ordre des critères indiqués ; 1 = présent) :

- smartphones (`REPAIRABILITY_INDEX, WARRANTY, CLASSE_ENERGY, WEIGHT`) : 0000=4 451, 0001=714, 0010=9, 0011=445,
  0100=1 510, 0101=530, 0110=0, 0111=161, 1000=33, 1001=207, 1010=13, 1011=45, 1100=30, 1101=437, 1110=3, 1111=73.
- oven (`WARRANTY, WEIGHT, CLASSE_ENERGY`) : 000=4 154, 001=72, 010=1 451, 011=128, 100=359, 101=22, 110=549, 111=84.
- refrigerator (`WARRANTY, NOISE_LEVEL, WEIGHT`, avec `CLASSE_ENERGY` toujours présent) : 000=0, 001=1, 010=19, 011=38,
  100=0, 101=2, 110=1, 111=67.
- air-conditioner (`CLASSE_ENERGY, POWER_CONSUMPTION_TYPICAL, WARRANTY, WEIGHT`) : 0000=185, 0001=3, 0010=2, 0011=2,
  0100=2, 0101=2, 0110=0, 0111=1, 1000=0, 1001=0, 1010=0, 1011=1, 1100=0, 1101=12, 1110=0, 1111=1.
- washing-machine (`WATER_CONSUMPTION, CLASSE_ENERGY, WARRANTY`) : 000=454, 001=109, 010=23, 011=37, 100=5, 101=1,
  110=125, 111=25 ; parmi les produits avec `NOISE_LEVEL`, `POWER_CONSUMPTION_OFF` et `WEIGHT` tous présents (47) :
  111=3, 110=25, 101=0, 011=9.
- dishwasher : les 51 produits avec `ENERGY_CONSUMPTION_100_CYCLES` ont tous `WATER_CONSUMPTION` et `CLASSE_ENERGY`
  (C ≥ 0,636) ; 28 d'entre eux ont `WARRANTY` (C ≥ 0,75).

**Pop. A, C sans `DATA_QUALITY` (poids v2), part de produits au-dessus de chaque seuil :**

| Verticale | Moyenne de C | ≥ 30 % | ≥ 40 % | ≥ 50 % | ≥ 60 % | ≥ 70 % | Mesure |
|---|---:|---:|---:|---:|---:|---:|---|
| smartphones | 0,167 | 17,7 % | 11,6 % | 11,6 % | 6,9 % | 6,8 % | exacte |
| oven | 0,084 | 11,5 % | 1,6 % | 1,2 % | 0 % | 0 % | exacte |
| refrigerator | 0,919 | 100 % | 100 % | 100 % | 100 % | 100 % | exacte |
| air-conditioner | 0,055 | 8,1 % | 6,6 % | 6,2 % | 6,2 % | 0,5 % | exacte |
| washing-machine | 0,147 | 24,1 à 27,6 % | 19,3 à 23,9 % | 6,4 à 9,9 % | 0,4 à 3,9 % | 0 à 3,5 % | encadrée (a) |
| dishwasher | 0,185 | ≥ 16,7 % | ≥ 16,7 % | 16,7 % | 16,7 % | 9,2 % | encadrée (b) |
| tv | 0,163 | 1,7 à 4,0 % | 0 à 1,7 % | 0 % | 0 % | 0 % | encadrée (c) |

(a) Motifs mesurés sur 3 critères (eau, classe énergie, garantie) + 3 critères mineurs ; borne haute = ajout théorique
de `POWER_CONSUMPTION_TYPICAL` (8 produits) et `REPAIRABILITY_INDEX` (19 produits) à tous. (b) 254 produits sans
`ENERGY_CONSUMPTION_100_CYCLES` : C ≥ 0,3 possible pour une partie d'entre eux (borne haute non mesurée) ; ≥ 0,5 reste à
16,7 % hors cas marginaux avec `REPAIRABILITY_INDEX` (25 produits). (c) `REPAIRABILITY_INDEX` présent sur 16 produits
seulement ; sans lui C ≤ 0,276 ; le maximum théorique est 0,437.
Les moyennes sont calculées exactement à partir des présences par critère (somme poids × part de présence).

Déciles (P10 à P90) :

- smartphones : 0, 0, 0, 0, 0, 0,25, 0,25, 0,25, 0,50 (51 % des fiches ont C = 0).
- oven : 0, 0, 0, 0, 0, 0, 0,118, 0,118, 0,353 (61 % à C = 0).
- refrigerator : 0,765, 0,859, 0,859, 0,859, 1, 1, 1, 1, 1.
- air-conditioner : 0 pour P10 à P80, 0,077 pour P90 (88 % à C = 0).
- washing-machine, dishwasher, tv : déciles non mesurés (voir ci-dessus).

**Avec `DATA_QUALITY` au dénominateur** (absent pour la pop. A) : multiplier C par `(1 − w_DQ)` (§3.1). Sur les
seuils, cela décale « ≥ 0,5 » vers C_sans ≥ 0,667 (smartphones : 6,9 %, soit 601 fiches ; oven : 1,2 %, soit 84 fiches, le seuil
évalué à 0,588 étant le maximum possible ; refrigerator : 100 % ; washing-machine : < 4 %). Les chiffres des seuils ≥ 0,6 et ≥ 0,7 du tableau donnent donc l'ordre de grandeur.

**Pop. B (notés) : même calcul.**

| Verticale | N | Moyenne de C | ≥ 50 % | ≥ 70 % | Mesure |
|---|---:|---:|---:|---:|---|
| smartphones | 928 | 0,532 | 57,0 % (529) | 13,6 % (126) | exacte (CE et garantie toujours présents ; poids `REPAIRABILITY_INDEX` 126, `WEIGHT` 526) |
| oven | 856 | 0,826 | 100 % | 78,9 % (675) | exacte (`OVEN_ENERGY_CONSUMPTION_CYCLE` et CE toujours présents) |
| refrigerator | 2 350 | 0,946 | 100 % | ≥ 94 % | `CLASSE_ENERGY` toujours présent ; borne basse de Bonferroni |
| tv | 1 345 | 0,794 | non mesuré | non mesuré | moyenne seule |
| washing-machine | 1 466 | 0,599 | non mesuré | non mesuré | moyenne seule |
| dishwasher | 1 025 | 0,869 | non mesuré | non mesuré | moyenne seule |
| air-conditioner | 76 | 0,529 | non mesuré | non mesuré | moyenne seule |

Pour les smartphones notés, C ≥ 0,4 est atteint par 100 % des fiches (base 0,417 : garantie et classe énergie présentes),
alors que 85 % des exclus sont à C = 0 : les notés actuels sont donc les fiches avec EPREL, pas des fiches « moyennes ».

**Lecture du seuil `minRealWeightCoverage` :**

- 0,50 laisse entrer 1 005 smartphones, 128 réfrigérateurs, 51 lave-vaisselle, 13 climatiseurs, 50 à 77 lave-linge et 0 TV.
- 0,30 ajouterait surtout des fiches très pauvres : 784 fours (11,5 %), 1 533 smartphones, des lave-linge sans
  consommation. À proscrire sans relecture.
- Les seuils sont des choix de score ; la sensibilité au seuil est forte pour smartphones (17,7 % à 0,3, 11,6 % à 0,5,
  6,9 % à 0,6) et nulle pour réfrigérateurs.

## 4. Échantillons à relire

**Écart à la consigne :** le connecteur interdit `function_score`/`random_score`. L'échantillon est un tirage
**déterministe par ancres de GTIN** : pour chaque verticale, 5 ancres fixes (`gtinInfos.normalizedGtin14 ≥`
`01000000000000`, `03300000000000`, `04000000000000`, `05500000000000`, `08000000000000`), tri croissant, 6 produits
par ancre, soit 30. Il est reproductible mais regroupe des GTIN voisins (donc des marques voisines) : les taux ci-dessous
sont indicatifs. Population : pop. A. Chaque produit a été jugé sur pièce (nom, marque, modèle, catégories des
sources) ; les définitions de « vrai produit » et de « limite » figurent ci-dessous. Fichiers : `lot1-echantillon-<verticale>.csv`
(colonnes : GTIN, marque, modèle, nom affiché, catégories des sources, `offersCount`, C, jugement, motif ; les catégories
sont limitées à 3 sources par produit).

| Verticale | Vrai produit | Limite | Accessoire | Pièce | Mal classé | Vrai % | Vrai + limite % | C moyen (échant.) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| smartphones | 24 | 0 | 0 | 0 | 6 | **80 %** | 80 % | 0,039 |
| oven | 0 | 4 | 0 | 0 | 26 | **0 %** | 13 % | 0,067 |
| refrigerator | 28 | 2 | 0 | 0 | 0 | **93 %** | 100 % | 0,921 |
| dishwasher | 19 | 7 | 2 | 1 | 1 | **63 %** | 87 % | 0,203 |
| washing-machine | 14 | 10 | 0 | 1 | 5 | **47 %** | 80 % | 0,198 |
| tv | 21 | 6 | 2 | 0 | 1 | **70 %** | 90 % | 0,154 |
| air-conditioner | 23 | 3 | 1 | 1 | 2 | **77 %** | 87 % | 0,068 |

Définitions de « limite » : four = mini-four ou four à pizza de table ; smartphone = tablette, téléphone fixe, lot ;
lave-linge = séchant, sèche-linge, mini ou professionnel ; lave-vaisselle = professionnel, mini ; TV = moniteur, écran
de projection, TV de camping ; réfrigérateur = cave à vin, mini-bar ; climatiseur = rafraîchisseur, déshumidificateur.

Observations par verticale :

- **oven :** 26 produits sur 30 sont des appareils de table sans rapport avec un four (raclettes 6, planchas 4, cuiseurs,
  woks, cafetières 4, machines à pâtes 3, théières). C'est le cas d'école du « four contre bouilloire » signalé en octobre.
- **smartphones :** les 6 mal classés sont des claviers (3 Lenovo), des jumelles, un équipement Honeywell et un luminaire probable, tous classés
  « smartphone » par une seule source. 25 produits sur 30 ont C = 0 : mêmes fiches vraies mais sans donnée. La marque
  « CDISCOUNT FR » apparaît.
- **washing-machine :** mal classés = lave-vaisselle et fers à repasser ; limites = 4 sèche-linge, 3 séchants, 2 mini,
  1 professionnel. Aucune fiche de l'échantillon n'a `POWER_CONSUMPTION_TYPICAL`.
- **dishwasher :** 6 des 7 « limite » sont des modèles professionnels CASSELIN ; un désodorisant et un nettoyeur
  haute pression sont mal classés.
- **tv :** moniteurs PC, écrans de projection, TV 12 V de camping ; peu de couverture (C moyen 0,154, maximum 0,437).
- **refrigerator :** 100 % de produits plausibles ; C moyen 0,921.
- **air-conditioner :** 14 produits n'ont qu'une catégorie ManoMano et un nom réduit au GTIN, classés « vrai » par défaut :
  le taux est probablement optimiste.

Pour les smartphones, la relecture ne couvre pas les fiches à C ≥ 0,5 (aucune dans l'échantillon, qui comptait 5 produits
avec une donnée) : un échantillon ciblé est nécessaire avant de lever le filtre.

## 5. Correspondances EPREL

### 5.1 `eprel-products` (2 353 842 documents)

Requête : `aggregate`, `terms` sur `productGroup` (size 100).

| Groupe `productGroup` | Fiches | Verticale concernée |
|---|---:|---|
| lightsources | 781 524 | |
| refrigeratingappliancesdirectsalesfunction | 481 969 | |
| tyres | 335 696 | |
| spaceheaterpackages | 113 303 | |
| refrigeratingappliances2019 | 74 327 | refrigerator |
| spaceheaters | 68 250 | |
| lamps | 67 365 | |
| electronicdisplays | 54 204 | tv |
| ovens | 39 987 | oven |
| airconditioners | 38 987 | air-conditioner |
| professionalrefrigeratedstoragecabinets | 36 048 | |
| localspaceheaters | 33 334 | |
| rangehoods | 32 563 | |
| washingmachines2019 | 30 179 | washing-machine |
| refrigeratingappliances | 29 626 | refrigerator |
| dishwashers2019 | 20 570 | dishwasher |
| waterheaters | 15 999 | |
| washingmachines | 14 642 | washing-machine |
| televisions | 14 360 | tv |
| dishwashers | 12 052 | dishwasher |
| residentialventilationunits | 10 781 | |
| tumbledriers | 9 997 | |
| hotwaterstoragetanks | 9 361 | |
| solidfuelboilers | 9 095 | |
| tumbledryers20232534 | 7 014 | |
| washerdriers2019 | 4 146 | |
| **smartphonestablets20231669** | **2 815** | smartphones (192 smartphones et 108 tablettes sur les 300 premières fiches) |
| washerdriers | 2 287 | |
| solidfuelboilerpackages | 1 089 | |
| spaceheatertemperaturecontrol | 1 087 | |
| waterheatersolardevices | 848 | |
| spaceheatersolardevice | 236 | |
| waterheaterpackages | 101 | |

Le nombre de fiches EPREL d'un groupe ne dit rien de la correspondance : seules comptent les fiches reliées à un GTIN
de produit. Voir §5.2.

### 5.2 Taux de correspondance par verticale

Parmi les produits avec offre et sans `missing_brand` ni `missing_model` : part sans `missing_eprel`.
Requête : `query {"bool":{"filter":[{"terms":{"vertical":V}},{"range":{"offersCount":{"gt":0}}}],"must_not":[{"term":{"excludedCauses":"missing_brand"}},{"term":{"excludedCauses":"missing_model"}}]}}`,
`aggs terms vertical > filter {"bool":{"must_not":[{"term":{"excludedCauses":"missing_eprel"}}]}}`.

| Verticale | Produits avec marque, modèle, offre | Sans `missing_eprel` | Taux |
|---|---:|---:|---:|
| tv | 1 609 | 1 345 | **83,6 %** |
| dishwasher | 1 330 | 1 025 | 77,1 % |
| washing-machine | 2 245 | 1 466 | 65,3 % |
| refrigerator | 3 807 | 2 350 | 61,7 % |
| air-conditioner | 287 | 76 | 26,5 % |
| oven | 7 675 | 856 | **11,2 %** |
| smartphones | 9 589 | 928 | **9,7 %** |

Les taux bas (fours, smartphones) coïncident avec la pollution mesurée au §4 : une grande partie des fiches
« sans EPREL » ne sont pas des produits couverts par EPREL.

### 5.3 Smartphones : champs d'un document EPREL

Liste exhaustive dans `lot1-annexe-eprel-smartphones.md` (45 clés de `categorySpecificAttributes`, fréquence sur 300
fiches, deux exemples chacune). Repères pour le lot 3 (noms exacts) :

- type d'appareil : `deviceType` (`SMARTPHONE`, `TABLET`) ;
- classe de réparabilité : `repairabilityClass` (A à E) ; indice : `repairabilityIndex` (1 à 5 dans l'échantillon) avec
  sous-scores `disassemblyDepthScore`, `fastenersScore`, `toolsScore`, `sparePartScore`, `repairInfoScore` ;
- batterie : `batteryEnduranceInCycles` (valeurs 4 à 20, unité à confirmer), `ratedBatteryCapacity` ;
- chutes : `repeatedFreeFallReliabilityClass` (A à E), `fallsWithoutDefect`, `fallsWithoutDefectFullyExtended` ;
- IP : `ingressProtectionRating`, `ingressProtectionRatingSolid(Number)`, `ingressProtectionRatingWater(Number)`,
  `immersionDepthWater` ;
- mises à jour : `minYearsSoftwareUpdates`, `softwareUpdatesScore`, `operatingSystem` ;
- classe énergie : `energyClass` (premier niveau du document, A à G) ;
- pièces détachées : **aucune clé chiffrée** (années ou délais) ; seulement `sparePartScore` et des liens
  (`webLinkInfoSparePartsAvailability`, `webLinkRepairInstructions`, `webLinkPretaxPrices`).

Réserves : l'échantillon de 300 fiches est le début de l'index trié (pas aléatoire) ; une valeur de
`minYearsSoftwareUpdates` à 48 est aberrante.

## 6. Indice de durabilité

Détail dans `lot1-annexe-durabilite-cles.md` (échantillons de 150 fiches par cellule : TV et lave-linge, notés et
exclus faute d'EPREL, tri par GTIN croissant, bornes `01…` et `055…`).

- **Aucun indice de durabilité** (nouvel indice français) n'a été trouvé dans les attributs de ces deux verticales :
  `DURABILITY_INDEX`, `DURABILITYINDEX`, `DURABILITY`, `DURABILITE`, `REPARABILITE` absents des 600 fiches lues, et aucun
  `DURABILITY_INDEX` dans le mapping indexé.
- **Réparabilité :** deux clés brutes, de source `rakuten.com`, toutes deux normalisées vers
  `attributes.indexed.REPAIRABILITY_INDEX` : `REPAIRABILITY INDEX` (avec espace) et `REPAIRABILITY_INDEX`
  (valeurs 0 à 10, exemple 7,7).
- Présence de `REPAIRABILITY_INDEX` (agrégation sur la population entière) : TV notées 280/1 345 (21 %), TV exclues faute
  d'EPREL 17/287 (6 %), lave-linge notés 307/1 466 (21 %), lave-linge exclus faute d'EPREL 19/1 313 (1,4 %).
- Clés voisines qui ne sont pas des indices : `MINAVAILABILITYSPAREPARTSYEARS`, `MINGUARANTEEDSUPPORTYEARS`,
  `MINAVAILABILITYSOFTWAREUPDATESYEARS`, `MINGUARANTEEDGUARANTEE` (TV), `GUARANTEEDURATION` (lave-linge), toutes de
  source `eprel` ; `WARRANTY` vient des revendeurs. La réparabilité apparaît aussi en texte libre (`DESCRIPTIF`,
  `ATTRIBUTS`).
- **Limite :** le connecteur refuse les jokers et les clés avec espace dans `_source`. Les clés brutes inconnues n'ont pas
  pu être énumérées ; un indice de durabilité dont le nom contient une espace n'aurait pas été vu. Le total des clés
  distinctes et le classement des plus fréquentes sont **non mesurés**. Une énumération complète demande un relèvement du
  plafond ou un export côté application.

## 7. Non mesuré, écarts et suites

| Point | Statut |
|---|---|
| Déciles de C : lave-linge, lave-vaisselle, TV (pop. A) | non mesuré (encadrements au §3.3) |
| Distribution de C des notés : TV, lave-linge, lave-vaisselle, climatiseurs | non mesuré (moyenne seule) |
| Échantillon aléatoire à graine fixe (`random_score`) | impossible avec ce connecteur ; remplacé par 5 ancres de GTIN |
| Parcours `_source` complet par pages de 500 | remplacé par comptage de motifs (exact) ; il aurait consommé 25 000 documents |
| Taux de « vrai produit » des fiches smartphones à C ≥ 0,5 | non mesuré (échantillon ciblé à faire) |
| Exclus sans cause hors des 7 verticales | non mesuré |
| Liste exhaustive des clés brutes d'attributs | non mesurée (§6) |
| Plan GOU-1864 §4 (limites du mapping) | non relu par cet auteur ; à rapprocher par le CTO |

Décisions à prendre (CTO, puis Goulven) : lever le filtre pour réfrigérateurs et lave-vaisselle ; faire relire un
échantillon ciblé de smartphones à C ≥ 0,5 avant d'envisager de le lever ; garder le filtre pour fours, TV, lave-linge
et climatiseurs. Aucun code ni configuration n'est modifié par ce lot.
