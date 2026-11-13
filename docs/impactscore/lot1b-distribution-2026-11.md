# ImpactScore lot 1b : distribution des notes après le lot 0 (2026-11-13)

Issue GOU-1876, sous-issue de GOU-1864 (§4 du plan). Mesures **en lecture seule** sur Elasticsearch de production via la connexion « Elasticsearch lecture (Dev) ». Aucune écriture. Complète `lot1-mesures-2026-11.md` (lot 1a).

- **Date des mesures :** 2026-11-13, de 07:05 à 07:40 (heure serveur), hors du batch de 13:00 à 15:00. Prérequis GOU-1868 (lot 0 en prod, contrôlé après un batch de 13:00) et GOU-1874 levés au réveil de l'issue ; la date du dernier batch n'a pas été relue par cet auteur.
- **Index :** `products-moustik`. **Population :** `vertical` ∈ 7 verticales, `excluded=false`, `offersCount > 0`, soit **9 475 produits** (TV 2 151, réfrigérateurs 2 354, lave-linge 1 480, fours 1 446, lave-vaisselle 1 011, smartphones 952, climatiseurs 81). Tous ont un `scores.ECOSCORE.value` (agrégat `missing` = 0).
- **Charge :** 140 documents lus (extrêmes, annexe CSV), le reste en agrégations. Plafond de 20 000 par heure respecté. Une requête à la fois.
- **Méthode V1.** La production calcule encore en `methodologyVersion` 1 : les critères et poids sont ceux du bloc racine `criteriasPonderation` de `verticals/src/main/resources/verticals/impactscores/<verticale>.yml` (avec `DATA_QUALITY`). Les `scores.<CRITERE>.value` sont les **valeurs brutes** (W, kWh, années…), la sous-note 0-5 est dans `scores.<CRITERE>.relativ.value` (`_source` seulement).
- **Nettoyage du lot 0 :** produits `excluded=true` portant encore `scores.ECOSCORE.value`, 7 verticales : **1** (requête `count`, `excluded=true` + `exists scores.ECOSCORE.value`). Le 10/11 : 1 942 pour les seules TV.

## 0. Résumé

1. **La compression d'échelle persiste.** Écart p5–p95 (points sur 20) : TV 2,7, Smartphones 5,8, Fours 2,3, Réfrigérateurs 5,7, Lave-linge 3,8, Lave-vaisselle 7,5, Climatiseurs 1,8. TV : 10,8 à 14,6 pour 98 % des produits ; min 9,17, max 15,53.
2. **Les libellés actuels (seuils 17/14/11/8 de `getImpactLevel`) ne séparent rien** : « Excellent » (≥ 17) concerne 0 TV, 0 lave-linge, 0 lave-vaisselle, 3 réfrigérateurs, 2 fours, 4 smartphones, 1 climatiseur ; 92 % des TV sont « Correct » ; 91 % des fours sont « Bon ».
3. **L'imputation par la moyenne est massive sur quelques critères** : réparabilité (TV 87 %, lave-linge 79 %, lave-vaisselle 74 %, smartphones 87 %), consommation typique du lave-linge (99 %), poids (28 % à 54 %, 90 % pour les climatiseurs). Part de poids imputée moyenne : voir §2.
4. **`DATA_QUALITY` et note** : corrélation de rang ≈ 0,90 (smartphones) et ≈ 0,88 (fours), ≈ 0,54 (lave-linge), mais ≈ 0,06 (réfrigérateurs) et ≈ −0,04 (lave-vaisselle). Elle est en partie mécanique (§3).
5. **Bornes min-max sensibles aux valeurs aberrantes** : TV SDR max 360 W pour p99 = 194 W ; lave-linge eau max 180 l pour p99 = 89 l ; four 9,1 kWh pour p99 = 1,6 kWh. Les percentiles 2-98 resserrent la plage de 2 à 10 fois (§4,2).
6. **Propositions V2 chiffrées :** §6.

## 1. Distribution de `scores.ECOSCORE.value`

Requêtes (toutes avec le filtre de population) : `aggregate` `terms vertical` > `stats` + `percentiles [1,5,10,25,50,75,90,95,99]` ; `range` sur `scores.ECOSCORE.value` aux seuils 8/11/14/17 ; `histogram` pas 0,5. Mesures 2026-11-13, source ES de production.

### 1,1 Statistiques et percentiles

| Verticale | n | min | p1 | p5 | p10 | p25 | p50 | p75 | p90 | p95 | p99 | max | moyenne | p95−p5 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TV | 2151 | 9,16 | 10,80 | 11,41 | 11,78 | 12,44 | 12,99 | 13,42 | 13,83 | 14,07 | 14,58 | 15,53 | 12,90 | **2,65** |
| Smartphones | 952 | 8,51 | 10,59 | 10,59 | 11,01 | 12,69 | 13,91 | 14,37 | 15,91 | 16,36 | 16,47 | 17,44 | 13,47 | **5,77** |
| Fours | 1446 | 8,65 | 13,50 | 13,79 | 14,24 | 14,46 | 15,15 | 15,29 | 15,78 | 16,08 | 16,61 | 17,37 | 14,95 | **2,28** |
| Réfrigérateurs | 2354 | 3,76 | 5,23 | 7,43 | 8,21 | 8,94 | 9,36 | 11,07 | 12,85 | 13,17 | 16,61 | 17,61 | 10,05 | **5,75** |
| Lave-linge | 1480 | 6,82 | 7,63 | 9,01 | 9,50 | 10,30 | 11,51 | 12,10 | 12,50 | 12,76 | 13,18 | 15,51 | 11,19 | **3,76** |
| Lave-vaisselle | 1011 | 6,31 | 6,84 | 7,54 | 8,14 | 9,49 | 11,25 | 13,16 | 14,47 | 15,02 | 15,61 | 16,13 | 11,30 | **7,47** |
| Climatiseurs | 81 | 11,81 | 12,13 | 13,58 | 13,66 | 13,66 | 13,66 | 13,78 | 14,38 | 15,39 | 16,42 | 17,08 | 13,85 | **1,82** |

Données en `float` ES, arrondies à 2 décimales ; les percentiles sont des estimations t-digest d'Elasticsearch (valeurs exactes pour de petits effectifs). Plateaux : smartphones p1 = p5 = 10,59 et climatiseurs p25 = p50 = 13,66 indiquent de nombreux produits à note identique (ex-æquo).

### 1,2 Part des produits par libellé actuel

Seuils de `frontend/app/utils/_product-verdict.ts` (`getImpactLevel`) : Excellent ≥ 17, Bon ≥ 14, Correct ≥ 11, Moyen ≥ 8, Faible < 8 (libellés `product.verdict.levels` : « Excellent », « Bon », « Correct », « Moyen », « Faible »).

| Verticale | Excellent (≥17) | Bon [14;17[ | Correct [11;14[ | Moyen [8;11[ | Faible (<8) |
|---|---|---|---|---|---|
| TV | 0 (0,0 %) | 135 (6,3 %) | 1974 (91,8 %) | 42 (2,0 %) | 0 (0,0 %) |
| Smartphones | 4 (0,4 %) | 403 (42,3 %) | 465 (48,8 %) | 80 (8,4 %) | 0 (0,0 %) |
| Fours | 2 (0,1 %) | 1322 (91,4 %) | 121 (8,4 %) | 1 (0,1 %) | 0 (0,0 %) |
| Réfrigérateurs | 3 (0,1 %) | 91 (3,9 %) | 567 (24,1 %) | 1492 (63,4 %) | 201 (8,5 %) |
| Lave-linge | 0 (0,0 %) | 4 (0,3 %) | 954 (64,5 %) | 496 (33,5 %) | 26 (1,8 %) |
| Lave-vaisselle | 0 (0,0 %) | 185 (18,3 %) | 349 (34,5 %) | 395 (39,1 %) | 82 (8,1 %) |
| Climatiseurs | 1 (1,2 %) | 10 (12,3 %) | 70 (86,4 %) | 0 (0,0 %) | 0 (0,0 %) |

Source : agrégat `range` (cinq tranches), 2026-11-13.

### 1,3 Histogramme par pas de 0,5 (0-20)

Effectifs par tranche `[x ; x+0,5[`, tranches vides omises (source : agrégat `histogram`, `min_doc_count` 1).

- **TV :** 9,0 : 1, 9,5 : 6, 10,0 : 5, 10,5 : 30, 11,0 : 84, 11,5 : 161, 12,0 : 295, 12,5 : 484, 13,0 : 627, 13,5 : 323, 14,0 : 105, 14,5 : 24, 15,0 : 5, 15,5 : 1
- **Smartphones :** 8,5 : 2, 10,0 : 5, 10,5 : 73, 11,0 : 98, 11,5 : 16, 12,0 : 22, 12,5 : 116, 13,0 : 100, 13,5 : 113, 14,0 : 242, 14,5 : 7, 15,0 : 39, 15,5 : 46, 16,0 : 68, 16,5 : 1, 17,0 : 4
- **Fours :** 8,5 : 1, 12,5 : 3, 13,0 : 10, 13,5 : 108, 14,0 : 295, 14,5 : 222, 15,0 : 599, 15,5 : 124, 16,0 : 62, 16,5 : 20, 17,0 : 2
- **Réfrigérateurs :** 3,5 : 2, 4,0 : 4, 4,5 : 9, 5,0 : 13, 5,5 : 21, 6,0 : 14, 6,5 : 25, 7,0 : 45, 7,5 : 68, 8,0 : 78, 8,5 : 347, 9,0 : 697, 9,5 : 117, 10,0 : 17, 10,5 : 236, 11,0 : 233, 11,5 : 17, 12,0 : 19, 12,5 : 257, 13,0 : 39, 13,5 : 2, 14,0 : 14, 14,5 : 32, 15,0 : 5, 15,5 : 1, 16,0 : 8, 16,5 : 31, 17,0 : 2, 17,5 : 1
- **Lave-linge :** 6,5 : 1, 7,0 : 6, 7,5 : 19, 8,0 : 14, 8,5 : 32, 9,0 : 77, 9,5 : 163, 10,0 : 109, 10,5 : 101, 11,0 : 212, 11,5 : 325, 12,0 : 272, 12,5 : 112, 13,0 : 28, 13,5 : 5, 14,0 : 1, 14,5 : 1, 15,0 : 1, 15,5 : 1
- **Lave-vaisselle :** 6,0 : 4, 6,5 : 12, 7,0 : 30, 7,5 : 36, 8,0 : 69, 8,5 : 55, 9,0 : 47, 9,5 : 60, 10,0 : 84, 10,5 : 80, 11,0 : 79, 11,5 : 55, 12,0 : 61, 12,5 : 68, 13,0 : 45, 13,5 : 41, 14,0 : 88, 14,5 : 37, 15,0 : 43, 15,5 : 13, 16,0 : 4
- **Climatiseurs :** 11,5 : 1, 12,0 : 1, 12,5 : 1, 13,0 : 1, 13,5 : 66, 14,0 : 3, 15,0 : 5, 15,5 : 1, 16,0 : 1, 17,0 : 1

## 2. Imputation par critère

**Chemin et indexation.** `get_mapping` (préfixes `scores.ECOSCORE`, `scores.POWER_CONSUMPTION_SDR`, 2026-11-13) : `scores.<CRITERE>.virtual` est un `boolean` agrégeable (`agg`), comme `value` (float) et `ranking` (long). Requête : un `terms` sur `scores.<CRITERE>.virtual` par critère. `true` = imputé par la moyenne.

### 2,1 Produits imputés par critère

**TV** (n = 2151)

| Critère | Poids V1 | Imputé (`true`) | Renseigné (`false`) | Part imputée |
|---|---|---|---|---|
| POWER_CONSUMPTION_SDR | 0,1600 | 83 | 2068 | 3,9 % |
| REPAIRABILITY_INDEX | 0,1400 | 1874 | 277 | 87,1 % |
| DATA_QUALITY | 0,1300 | 0 | 2151 | 0,0 % |
| MINAVAILABILITYSPAREPARTSYEARS | 0,0800 | 171 | 1980 | 7,9 % |
| POWER_CONSUMPTION_HDR | 0,0800 | 310 | 1841 | 14,4 % |
| MINAVAILABILITYSOFTWAREUPDATESYEARS | 0,0700 | 172 | 1979 | 8,0 % |
| WARRANTY | 0,0700 | 97 | 2054 | 4,5 % |
| CLASSE_ENERGY_SDR | 0,0600 | 77 | 2074 | 3,6 % |
| MINGUARANTEEDSUPPORTYEARS | 0,0500 | 170 | 1981 | 7,9 % |
| POWER_CONSUMPTION_STANDBY_NETWORKD | 0,0500 | 285 | 1866 | 13,2 % |
| CLASSE_ENERGY_HDR | 0,0400 | 308 | 1843 | 14,3 % |
| DIAGONALE_POUCES | 0,0400 | 0 | 2151 | 0,0 % |
| POWER_CONSUMPTION_OFF | 0,0300 | 477 | 1674 | 22,2 % |

Part de poids imputée moyenne (poids V1 hors `DATA_QUALITY`, renormalisés) : **20,7 %**.

**Smartphones** (n = 952)

| Critère | Poids V1 | Imputé (`true`) | Renseigné (`false`) | Part imputée |
|---|---|---|---|---|
| REPAIRABILITY_INDEX | 0,3750 | 828 | 124 | 87,0 % |
| DATA_QUALITY | 0,2500 | 0 | 952 | 0,0 % |
| WARRANTY | 0,1875 | 0 | 952 | 0,0 % |
| CLASSE_ENERGY | 0,1250 | 0 | 952 | 0,0 % |
| WEIGHT | 0,0625 | 426 | 526 | 44,7 % |

Part de poids imputée moyenne (poids V1 hors `DATA_QUALITY`, renormalisés) : **47,2 %**.

**Fours** (n = 1446)

| Critère | Poids V1 | Imputé (`true`) | Renseigné (`false`) | Part imputée |
|---|---|---|---|---|
| OVEN_ENERGY_CONSUMPTION_CYCLE | 0,3500 | 0 | 1446 | 0,0 % |
| WARRANTY | 0,2000 | 620 | 826 | 42,9 % |
| CLASSE_ENERGY | 0,2000 | 553 | 893 | 38,2 % |
| DATA_QUALITY | 0,1500 | 0 | 1446 | 0,0 % |
| WEIGHT | 0,1000 | 786 | 660 | 54,4 % |

Part de poids imputée moyenne (poids V1 hors `DATA_QUALITY`, renormalisés) : **25,5 %**.

**Réfrigérateurs** (n = 2354)

| Critère | Poids V1 | Imputé (`true`) | Renseigné (`false`) | Part imputée |
|---|---|---|---|---|
| CLASSE_ENERGY | 0,5500 | 44 | 2310 | 1,9 % |
| DATA_QUALITY | 0,1500 | 0 | 2354 | 0,0 % |
| WARRANTY | 0,1200 | 133 | 2221 | 5,6 % |
| NOISE_LEVEL | 0,1000 | 116 | 2238 | 4,9 % |
| WEIGHT | 0,0800 | 1008 | 1346 | 42,8 % |

Part de poids imputée moyenne (poids V1 hors `DATA_QUALITY`, renormalisés) : **6,6 %**.

**Lave-linge** (n = 1480)

| Critère | Poids V1 | Imputé (`true`) | Renseigné (`false`) | Part imputée |
|---|---|---|---|---|
| POWER_CONSUMPTION_TYPICAL | 0,2200 | 1461 | 19 | 98,7 % |
| WATER_CONSUMPTION | 0,1800 | 231 | 1249 | 15,6 % |
| CLASSE_ENERGY | 0,1800 | 87 | 1393 | 5,9 % |
| DATA_QUALITY | 0,1200 | 0 | 1480 | 0,0 % |
| REPAIRABILITY_INDEX | 0,1200 | 1168 | 312 | 78,9 % |
| WARRANTY | 0,1000 | 24 | 1456 | 1,6 % |
| NOISE_LEVEL | 0,0300 | 82 | 1398 | 5,5 % |
| WEIGHT | 0,0300 | 415 | 1065 | 28,0 % |
| POWER_CONSUMPTION_OFF | 0,0200 | 92 | 1388 | 6,2 % |

Part de poids imputée moyenne (poids V1 hors `DATA_QUALITY`, renormalisés) : **41,3 %**.

**Lave-vaisselle** (n = 1011)

| Critère | Poids V1 | Imputé (`true`) | Renseigné (`false`) | Part imputée |
|---|---|---|---|---|
| ENERGY_CONSUMPTION_100_CYCLES | 0,3400 | 11 | 1000 | 1,1 % |
| WATER_CONSUMPTION | 0,1600 | 2 | 1009 | 0,2 % |
| DATA_QUALITY | 0,1200 | 0 | 1011 | 0,0 % |
| REPAIRABILITY_INDEX | 0,1200 | 744 | 267 | 73,6 % |
| WARRANTY | 0,1000 | 62 | 949 | 6,1 % |
| CLASSE_ENERGY | 0,0600 | 27 | 984 | 2,7 % |
| NOISE_LEVEL | 0,0500 | 0 | 1011 | 0,0 % |
| WEIGHT | 0,0300 | 415 | 596 | 41,0 % |
| POWER_CONSUMPTION_STANDBY | 0,0100 | 127 | 884 | 12,6 % |
| POWER_CONSUMPTION_OFF | 0,0100 | 392 | 619 | 38,8 % |

Part de poids imputée moyenne (poids V1 hors `DATA_QUALITY`, renormalisés) : **13,4 %**.

**Climatiseurs** (n = 81)

| Critère | Poids V1 | Imputé (`true`) | Renseigné (`false`) | Part imputée |
|---|---|---|---|---|
| CLASSE_ENERGY | 0,3402 | 70 | 11 | 86,4 % |
| POWER_CONSUMPTION_TYPICAL | 0,2577 | 68 | 13 | 84,0 % |
| REFRIGERANT_GWP | 0,1031 | 0 | 81 | 0,0 % |
| REPAIRABILITY_INDEX | 0,0825 | critère absent de tous les documents | | non mesurable |
| WARRANTY | 0,0722 | 77 | 4 | 95,1 % |
| DATA_QUALITY | 0,0619 | 0 | 81 | 0,0 % |
| POWER_CONSUMPTION_OFF | 0,0412 | 79 | 2 | 97,5 % |
| WEIGHT | 0,0412 | 73 | 8 | 90,1 % |

Part de poids imputée moyenne (poids V1 hors `DATA_QUALITY`, renormalisés) : **76,7 %**.

`DATA_QUALITY` est calculé pour tous les produits (jamais imputé). `REPAIRABILITY_INDEX` n'existe pas dans les documents de climatiseurs (aucun `virtual` renvoyé). Smartphones : `WARRANTY` et `CLASSE_ENERGY` ne sont jamais imputés (EPREL).

### 2,2 Part de poids imputée par produit : déciles

Calcul exact par motifs de présence (agrégats `filter` sur `scores.<C>.virtual`, 2^k combinaisons des critères jamais constants).

| Verticale | D1 | D2 | D3 | D4 | D5 | D6 | D7 | D8 | D9 |
|---|---|---|---|---|---|---|---|---|---|
| Smartphones | 0 % | 50 % | 50 % | 50 % | 50 % | 58 % | 58 % | 58 % | 58 % |
| Fours | 0 % | 12 % | 12 % | 24 % | 24 % | 35 % | 35 % | 35 % | 47 % |

Smartphones : 44 % des produits ont réparabilité **et** poids imputés (58 % du poids), 43 % seulement la réparabilité (50 %), 13 % aucune imputation. Fours : motifs sur `WARRANTY`, `WEIGHT`, `CLASSE_ENERGY` ; 41 % ont au moins deux critères imputés sur trois.
**Non mesuré :** déciles pour TV, réfrigérateurs, lave-linge, lave-vaisselle et climatiseurs (10 à 13 critères variables : 2^k motifs, trop de requêtes ; un parcours `_source` aurait lu 8 000 documents dont la sortie n'a pas été exploitée). Seule la moyenne exacte est donnée (§2,1). Pour les réfrigérateurs, `WEIGHT` (9 % du poids hors `DATA_QUALITY`) porte l'essentiel : 43 % des produits ont au moins 9 % de poids imputé.

## 3. Effet de `DATA_QUALITY`

Corrélation de rang de Spearman entre `scores.DATA_QUALITY.value` et `scores.ECOSCORE.value`. **Approximation :** calculée sur le tableau croisé exact `histogram` DATA_QUALITY (pas 1) × `histogram` ECOSCORE (pas 0,5) renvoyé par Elasticsearch (rangs moyens par classe, ex-æquo à l'intérieur d'une classe de 0,5 point) et non sur un parcours `_source` produit par produit, qui n'a pas été fait. Les effectifs du tableau recoupent les n du §1 (somme = n pour les 7 verticales).

| Verticale | n | ρ de Spearman (≈) | `DATA_QUALITY` min / moyenne / max | Poids V1 de `DATA_QUALITY` |
|---|---|---|---|---|
| TV | 2151 | **0,25** | 1 / 13,1 / 17 | 0,13 |
| Smartphones | 952 | **0,90** | 2 / 7,2 / 9 | 0,25 |
| Fours | 1446 | **0,88** | 2 / 3,7 / 6 | 0,15 |
| Réfrigérateurs | 2354 | **0,06** | 1 / 5,4 / 8 | 0,15 |
| Lave-linge | 1480 | **0,54** | 0 / 6,5 / 10 | 0,12 |
| Lave-vaisselle | 1011 | **-0,04** | 1 / 7,4 / 10 | 0,12 |
| Climatiseurs | 81 | **0,37** | 2 / 2,5 / 7 | 0,0619 |

Lecture : « plus de données » rend « plus vert » nettement pour smartphones et fours, modérément pour les lave-linge, faiblement à modérément pour les TV (≈ 0,25) et climatiseurs (≈ 0,37, 81 produits), pas du tout pour réfrigérateurs et lave-vaisselle. **La corrélation est en partie mécanique** : `DATA_QUALITY` est lui-même un critère de la note V1 (poids de 0,25 pour les smartphones, 0,15 pour les fours) et un produit sans donnée reçoit la moyenne. Elle ne prouve donc pas qu'un produit mieux documenté soit meilleur ; elle justifie de sortir `DATA_QUALITY` de la note (V2) et de l'afficher comme indicateur de fiabilité.

## 4. Extrêmes

### 4,1 Dix meilleurs et dix moins bons

Annexe : `lot1b-annexe-extremes-2026-11.csv` (1 100 lignes, 140 produits : vertical, groupe top/bottom, position, GTIN, nom, note, critère, sous-note 0-5, valeur brute, imputée). Requête : `search`, `sort scores.ECOSCORE.value desc|asc`, `size` 10, filtre de population, 2026-11-13. Les ex-æquo sont ordonnés par Elasticsearch sans départage.

| Verticale | 10 meilleurs : note | critères imputés (hors `DATA_QUALITY`) | 10 moins bons : note | critères imputés |
|---|---|---|---|---|
| TV | 14,89 à 15,53 | 11 / 120 | 9,17 à 10,25 | 24 / 120 |
| Smartphones | 16,47 à 17,44 | 1 / 40 | 8,51 à 10,59 | 20 / 40 |
| Fours | 16,71 à 17,37 | 1 / 40 | 8,65 à 13,48 | 18 / 40 |
| Réfrigérateurs | 16,80 à 17,61 | 0 / 40 | 3,76 à 4,86 | 6 / 40 |
| Lave-linge | 13,37 à 15,51 | 14 / 80 | 6,82 à 7,59 | 19 / 80 |
| Lave-vaisselle | 15,65 à 16,13 | 11 / 90 | 6,31 à 6,81 | 16 / 90 |
| Climatiseurs | 14,32 à 17,08 | 28 / 70 | 11,82 à 13,66 | 38 / 70 |

Le dénominateur est 10 produits × nombre de critères hors `DATA_QUALITY`. Constats : les meilleurs smartphones, fours et réfrigérateurs n'ont presque aucun critère imputé, les moins bons smartphones en ont la moitié ; pour les TV, 9 % des critères des meilleurs et 20 % de ceux des moins bons sont imputés. Pour les climatiseurs (81 produits), la moitié du top comme du bas est imputée.

### 4,2 Consommation par critère d'énergie : bornes observées et alternatives

Requêtes : `extended_stats` et `percentiles [1,2,5,50,95,98,99]` sur `attributes.indexed.<CLE>.numericValue`, filtre de population, 2026-11-13. `n` = produits où la valeur brute existe (les autres sont imputés). Le critère `CLASSE_ENERGY*` est une classe (non numérique) : exclu.

| Verticale | Critère | n | min | p2 | p50 | p98 | max | moyenne | σ |
|---|---|---|---|---|---|---|---|---|---|
| TV | POWER_CONSUMPTION_SDR | 2068 | 7,4 | 16,6 | 69,8 | 173 | 360 | 75,8 | 41 |
| TV | POWER_CONSUMPTION_HDR | 1841 | 16 | 27 | 122 | 423 | 879 | 152 | 102 |
| TV | POWER_CONSUMPTION_OFF | 1674 | 0 | 0 | 0,5 | 0,5 | 110 | 0,527 | 2,77 |
| TV | POWER_CONSUMPTION_STANDBY_NETWORKD | 1866 | 0 | 0,5 | 2 | 2 | 5,7 | 1,92 | 0,336 |
| Fours | OVEN_ENERGY_CONSUMPTION_CYCLE | 1446 | 0,01 | 0,54 | 0,88 | 1,45 | 9,1 | 0,89 | 0,284 |
| Lave-linge | POWER_CONSUMPTION_TYPICAL | 19 | 2 | 2 | 1,85e+03 | 2,26e+03 | 2,3e+03 | 1,64e+03 | 724 |
| Lave-linge | WATER_CONSUMPTION | 1249 | 1 | 36 | 48 | 64 | 180 | 47,8 | 9,66 |
| Lave-linge | POWER_CONSUMPTION_OFF | 1388 | 0 | 0,11 | 0,5 | 0,5 | 72 | 0,524 | 1,93 |
| Lave-vaisselle | ENERGY_CONSUMPTION_100_CYCLES | 1000 | 28 | 49 | 75 | 95 | 97 | 73,4 | 14 |
| Lave-vaisselle | WATER_CONSUMPTION | 1009 | 4,5 | 6,5 | 9,5 | 12 | 12,9 | 9,56 | 1,12 |
| Lave-vaisselle | POWER_CONSUMPTION_OFF | 619 | 0 | 0 | 0,5 | 0,5 | 0,5 | 0,46 | 0,113 |
| Lave-vaisselle | POWER_CONSUMPTION_STANDBY | 884 | 0 | 0 | 0,5 | 1 | 2,5 | 0,585 | 0,256 |
| Climatiseurs | POWER_CONSUMPTION_TYPICAL | 13 | 0,6 | 0,624 | 0,9 | 1,4 | 1,4 | 0,931 | 0,237 |
| Climatiseurs | POWER_CONSUMPTION_OFF | 2 | 0,5 | 0,51 | 0,75 | 0,99 | 1 | 0,75 | 0,25 |

Comparaison des stratégies sur la note 0-20 d'un produit (plus c'est bas, mieux c'est ; note = 20 × (haut − v)/(haut − bas), bornée à [0 ; 20]). `MINMAX_OBSERVED` : bornes = min et max de la cohorte (stratégie V1) ; `SIGMA` : moyenne ± 2σ (défaut `sigmaK` = 2) ; `QUANTILE 2-98` : p2 et p98 (`MinMaxQuantileNormalizationStrategy`, déjà dans `normalization/`) ; `MINMAX_FIXED` : bornes réglementaires ou physiques fixes (à choisir par critère, voir §5).

| Critère | Plage min-max | Plage σ (±2) | Plage p2-p98 | Note à la médiane : min-max / σ / p2-p98 | Note d'un produit au p95 : min-max / σ / p2-p98 |
|---|---|---|---|---|---|
| TV POWER_CONSUMPTION_SDR | 7,4 à 360 | 7,4 à 158 | 16,6 à 173 | 16,5 / 11,7 / 13,2 | 11,7 / 0,6 / 2,5 |
| TV POWER_CONSUMPTION_HDR | 16 à 879 | 16 à 356 | 27 à 423 | 17,5 / 13,8 / 15,2 | 12,3 / 0,3 / 3,7 |
| TV POWER_CONSUMPTION_OFF | 0 à 110 | 0 à 6,06 | 0 à 0,5 | 19,9 / 18,3 / 0,0 | 19,9 / 18,3 / 0,0 |
| TV POWER_CONSUMPTION_STANDBY_NETWORKD | 0 à 5,7 | 1,25 à 2,59 | 0,5 à 2 | 13,0 / 8,8 / 0,0 | 13,0 / 8,8 / 0,0 |
| Fours OVEN_ENERGY_CONSUMPTION_CYCLE | 0,01 à 9,1 | 0,322 à 1,46 | 0,54 à 1,45 | 18,1 / 10,2 / 12,5 | 17,5 / 5,4 / 6,6 |
| Lave-linge WATER_CONSUMPTION | 1 à 180 | 28,5 à 67,1 | 36 à 64 | 14,7 / 9,9 / 11,4 | 13,9 / 5,8 / 5,7 |
| Lave-linge POWER_CONSUMPTION_OFF | 0 à 72 | 0 à 4,38 | 0,11 à 0,5 | 19,9 / 17,7 / 0,0 | 19,9 / 17,7 / 0,0 |
| Lave-vaisselle ENERGY_CONSUMPTION_100_CYCLES | 28 à 97 | 45,3 à 101 | 49 à 95 | 6,4 / 9,4 / 8,7 | 0,6 / 2,3 / 0,0 |
| Lave-vaisselle WATER_CONSUMPTION | 4,5 à 12,9 | 7,32 à 11,8 | 6,5 à 12 | 8,1 / 10,3 / 9,1 | 3,3 / 1,4 / 1,8 |
| Lave-vaisselle POWER_CONSUMPTION_OFF | 0 à 0,5 | 0,233 à 0,686 | 0 à 0,5 | 0,0 / 8,2 / 0,0 | 0,0 / 8,2 / 0,0 |
| Lave-vaisselle POWER_CONSUMPTION_STANDBY | 0 à 2,5 | 0,0717 à 1,1 | 0 à 1 | 16,0 / 11,6 / 10,0 | 12,0 / 1,9 / 0,0 |

Exemple cité par l'audit : TV `POWER_CONSUMPTION_SDR`, 180 W donne (360 − 180)/(360 − 7,4) × 20 = **10,2/20** avec min-max, 0 avec σ (borne haute 157,8 W) et 0,0 avec p2-p98 (borne haute 172,7 W). Plages quasi dégénérées (p2 = p98 ou σ presque nul) : `POWER_CONSUMPTION_OFF` et `POWER_CONSUMPTION_STANDBY_NETWORKD` des TV, `POWER_CONSUMPTION_OFF` et `POWER_CONSUMPTION_STANDBY` des lave-vaisselle (le p50 vaut le p98 : 0,5 W) : un percentile ne convient pas, une borne fixe est requise.
**Anomalie.** Lave-linge `POWER_CONSUMPTION_TYPICAL` : seulement 19 valeurs brutes sur 1 480 (min 2, max 2 300, moyenne 1 638), c'est-à-dire 99 % d'imputation et un mélange d'unités probable ; le critère n'est pas exploitable en l'état (à instruire par GOU-1883). Climatiseurs : 13 valeurs `POWER_CONSUMPTION_TYPICAL` et 2 `POWER_CONSUMPTION_OFF` seulement : aucune borne n'a de sens statistique.

## 5. TV : note selon la diagonale

Requête : `histogram` de `attributes.indexed.DIAGONALE_POUCES.numericValue` (pas 10) > `stats`, `percentiles [50]` et `avg` de `scores.ECOSCORE.value`, de `attributes.indexed.POWER_CONSUMPTION_SDR.numericValue` (W) et de `scores.DATA_QUALITY.value`, 2026-11-13. Tranche `[x ; x+10[` pouces ; toutes les TV de la population ont une diagonale (agrégat `missing` = 0).

| Diagonale (pouces) | n | note moyenne | note médiane | puissance SDR moyenne (W) | `DATA_QUALITY` moyen |
|---|---|---|---|---|---|
| 10 à 20 | 15 | 13,02 | 12,69 | 11 | 9,1 |
| 20 à 30 | 122 | 13,40 | 13,49 | 19 | 11,3 |
| 30 à 40 | 216 | 13,46 | 13,69 | 28 | 12,2 |
| 40 à 50 | 398 | 13,20 | 13,29 | 50 | 12,9 |
| 50 à 60 | 610 | 13,03 | 13,06 | 72 | 13,3 |
| 60 à 70 | 342 | 12,64 | 12,71 | 96 | 13,1 |
| 70 à 80 | 248 | 12,45 | 12,40 | 118 | 13,7 |
| 80 à 90 | 144 | 12,19 | 12,19 | 133 | 13,8 |
| 90 à 100 | 29 | 11,76 | 12,03 | 160 | 14,1 |
| 100 à 110 | 19 | 11,79 | 11,59 | 161 | 14,4 |
| 110 à 120 | 8 | 10,35 | 10,23 | 222 | 14,2 |

La note moyenne baisse de 13,4-13,5 (20 à 40 pouces) à 10,3 (110 pouces et plus) alors que la puissance passe de 19 W à 223 W : la note V1 pénalise la taille par la consommation absolue (corrélation visible tranche à tranche, hors tranche 10-20 pouces à 15 TV). Une comparaison à la surface (W/dm² ou kWh/1 000 h par pouce carré) est l'unité fonctionnelle à tester en V2 ; la note ne se compare pas honnêtement entre 32 et 75 pouces.

## 6. Propositions chiffrées pour la V2

### 6,1 Libellés par position

Le bloc provisoire `v2.positionLabels` des `.yml` (excellent ≥ 0,90, good ≥ 0,67, fair ≥ 0,33, poor sinon : « mieux que » la part indiquée de la cohorte) donne, avec les percentiles mesurés du §1 (note à partir de laquelle un produit est dans le libellé) :

| Verticale | Excellent (top 10 % : note ≥ p90) | Bon (≥ p66,7) | Correct (≥ p33,3) | Poor (en dessous) | Seuils actuels pour mémoire |
|---|---|---|---|---|---|
| TV | ≥ 13,83 | ≥ 13,27 | ≥ 12,66 | < 12,66 | 17 / 14 / 11 / 8 |
| Smartphones | ≥ 15,91 | ≥ 14,30 | ≥ 12,69 | < 12,69 | 17 / 14 / 11 / 8 |
| Fours | ≥ 15,78 | ≥ 15,25 | ≥ 14,58 | < 14,58 | 17 / 14 / 11 / 8 |
| Réfrigérateurs | ≥ 12,85 | ≥ 10,61 | ≥ 9,15 | < 9,15 | 17 / 14 / 11 / 8 |
| Lave-linge | ≥ 12,50 | ≥ 11,87 | ≥ 10,88 | < 10,88 | 17 / 14 / 11 / 8 |
| Lave-vaisselle | ≥ 14,47 | ≥ 12,55 | ≥ 10,12 | < 10,12 | 17 / 14 / 11 / 8 |
| Climatiseurs | ≥ 14,38 | ≥ 13,66 | ≥ 13,66 | < 13,66 | 17 / 14 / 11 / 8 |

Lecture : le libellé par position remplace des seuils fixes qui, aujourd'hui, mettent 92 % des TV en « Correct » et 91 % des fours en « Bon ». Réserves mesurées : (1) **ex-æquo** : climatiseurs p33 = p50 = p67 = 13,66 (plus de 80 % des produits à 13,5-14) : une règle de position stricte (« mieux que x % ») classerait tous ces produits « poor » ou tous « fair » ; il faut un départage ou une cohorte minimale ; (2) **cohorte de 81** pour les climatiseurs : un top 10 % = 8 produits ; (3) la position est relative à la cohorte du jour : elle bouge avec le catalogue, donc le libellé devrait s'accompagner de la date et de la taille de cohorte. **Recommandation :** conserver 0,90 / 0,67 / 0,33 pour les verticales de plus de 300 produits, avec départage des ex-æquo par la note brute puis le GTIN, et ne pas afficher de libellé en dessous de 100 produits notés (climatiseurs : « non classé »). À valider par le CTO.

### 6,2 Bornes robustes par critère d'énergie

| Stratégie | Avantage mesuré | Limite mesurée |
|---|---|---|
| `MINMAX_OBSERVED` (V1) | aucune configuration | une valeur aberrante fixe la borne : TV SDR 360 W, lave-linge eau 180 l, four 9,1 kWh |
| `SIGMA` (k = 2) | borne haute proche du bulk (TV SDR 158 W) | suppose une loi proche de la normale ; la borne basse mean − 2σ est négative pour TV SDR/HDR (−6 W, −51 W) donc inutile ; σ sensible aux mêmes aberrations (HDR σ = 102 W) |
| `QUANTILE 2-98` (`MinMaxQuantileNormalizationStrategy`) | indépendant de la loi ; 2 % de produits saturés de chaque côté ; plage divisée par 2 à 10 | plage nulle si le critère est quasi constant (veilles à 0,5 W) ; coûteux (valeurs étendues à chaque appel, la classe le note) |
| `MINMAX_FIXED` | stable dans le temps, identique entre cohortes et versions (utile à l'historique V2) | demande une borne par critère et par verticale, à documenter (à sourcer, par exemple EPREL) |

**Proposition :** (1) `QUANTILE 2-98` pour les consommations continues à large dispersion : `POWER_CONSUMPTION_SDR`, `POWER_CONSUMPTION_HDR`, `OVEN_ENERGY_CONSUMPTION_CYCLE`, `WATER_CONSUMPTION` (lave-linge, lave-vaisselle), `ENERGY_CONSUMPTION_100_CYCLES` ; (2) `MINMAX_FIXED` pour les critères quasi constants ou à valeurs plafonnées : `POWER_CONSUMPTION_OFF`, `POWER_CONSUMPTION_STANDBY(_NETWORKD)` (par exemple 0 à 2 W) ; (3) pas de borne statistique sur `POWER_CONSUMPTION_TYPICAL` du lave-linge et des climatiseurs tant que les valeurs brutes ne sont pas fiabilisées (§4,2). Plage retenue pour TV SDR : 16,6 W à 172,7 W au lieu de 7,4 à 360 W. Simulation sur la cohorte à faire avant arbitrage (GOU-1883).

### 6,3 Redistribution des poids imputés

Part de poids imputée moyenne (§2,1) : TV 21 %, Smartphones 47 %, Fours 25 %, Réfrigérateurs 7 %, Lave-linge 41 %, Lave-vaisselle 13 %, Climatiseurs 77 %. Un produit sans donnée recevant la moyenne du critère, la note V1 se tasse vers la moyenne, ce qui explique en partie la compression du §1 (TV : 87 % imputés sur la réparabilité, 14 % du poids). Argument chiffré pour une redistribution des poids sur les critères renseignés (V2, `minRealWeightCoverage` provisoire 0,5) : voir déciles §2,2 pour smartphones et fours (58 % de poids imputé pour 44 % des smartphones).

## 7. Non mesuré, écarts et suites

| Point | Statut |
|---|---|
| Déciles de la part de poids imputée : TV, réfrigérateurs, lave-linge, lave-vaisselle, climatiseurs | non mesuré (moyenne exacte seulement) |
| Spearman produit par produit (`_source`) | non mesuré ; approximation par classes de 0,5 sur tableau croisé exact (§3) |
| Date du dernier batch et délai depuis le déploiement du lot 0 | non mesuré |
| Simulation des stratégies de bornes sur la cohorte (notes recalculées) | non faite ; comparaison analytique sur trois points seulement (§4,2) |
| Valeurs brutes de lave-linge `POWER_CONSUMPTION_TYPICAL` (unités) | à instruire |
| Climatiseurs : `REPAIRABILITY_INDEX` absent des documents | constaté, cause non recherchée |
| Critères non numériques (`CLASSE_ENERGY*`) : bornes | sans objet (classe) |

Aucun code ni configuration n'est modifié par ce lot.
