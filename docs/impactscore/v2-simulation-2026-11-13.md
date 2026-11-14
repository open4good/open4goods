# Simulation ImpactScore V1/V2 du 13/11/2026

GOU-1887, sous le programme GOU-1864. Conception : [v2-design.md](v2-design.md), section 6. Valeurs simulées : celles
de la porte n° 2 ([decisions.md](decisions.md), GOU-1877, appliquées par GOU-2137), lues dans les blocs `v2` de
`main` au commit `0f0ef122d` (TV par surface d'écran, GOU-1884, comprise). Rien n'est écrit en prod : V1 et V2 sont
calculées hors ligne sur le même export.

## Données

- Export de prod d'Ops (GOU-1904) : `~paperclip/data/impactscore-sim/2026-11-13/`, index `products-moustik`, lu du
  13/11/2026 06:07 à 06:21 UTC, 7 verticales, tous les produits (exclus compris), un `_source` par ligne. Lignes =
  `count` ES pour les 7 verticales (`MANIFEST.md`, `SHA256SUMS` vérifié). **L'export n'est pas dans Git.**
- Dernier batch noté dans l'export : 12/11/2026 (TV 12 h UTC, autres verticales entre 13 h et 19 h UTC).
- Cohorte V1 (règle du batch) : `offersCount > 0` et `excluded = false` stocké. Ses effectifs sont ceux de la porte
  n° 2 (81, 1 011, 1 480, 2 354, 2 151, 1 446, 952).

## Méthode (reproductible)

Harnais : `ImpactScoreSimulationTest` (module `api`, ignoré en CI faute de
`-Dimpactscore.simulation.input`). Il charge les vraies configurations deux fois, forcées en version 1 puis 2 par
`impactscore.methodology-version-overrides`, et fait tourner la vraie chaîne de notation dans l'ordre de prod
(`AggregationFacadeService.getScoringAggregator()`).

1. Lecture de `<verticale>.jsonl.gz`. `SerialisationService.fromJson` ne relit pas un `Product` (Jackson 3 construit
   `SourcedAttribute` et `IndexedAttribute` par leur constructeur de copie, qui échoue) : le harnais utilise les mêmes
   réglages, sans détection des noms de paramètres.
2. **V1** : cohorte `offersCount > 0 && !excluded` (exclusion stockée), version 1. L'exclusion recalculée en V1
   est écrite à côté (`v1_excluded`, `v1_causes`) pour vérifier qu'un changement de règle laisse la V1 intacte.
3. **V2** : exclusion recalculée par `ExclusionRules.evaluate` avec la configuration V2 (`eprelRequired`), même règle
   de cohorte, version 2. Paramètres candidats : `-Dimpactscore.simulation.params=<fichier.yml>`, par verticale les
   champs qui remplacent ceux du bloc `v2` (exemple plus bas).
4. Notes de marque : un `BrandService` bouchon rend, par marque, les `BRAND_*` réels du produit de cette marque noté
   le plus récemment (une fiche notée par un batch plus ancien garde des notes de marque que le dernier batch ne
   donnait plus).
5. Chaque version note sa propre copie des produits. Une troisième passe sert au contrôle de fidélité (plus bas).

Commandes, depuis la racine du dépôt :

```bash
mvn --offline -q -pl api -am install -DskipTests
mvn --offline -q -pl api surefire:test -Dtest=ImpactScoreSimulationTest \
  -Dsurefire.failIfNoSpecifiedTests=false -DargLine=-Xmx10g \
  -Dimpactscore.simulation.input=$HOME/data/impactscore-sim/2026-11-13 \
  -Dimpactscore.simulation.output=$HOME/sim-scratch/out \
  -Dimpactscore.simulation.fidelity-waived=air-conditioner
python3 -I scripts/impactscore/simulation_report.py --input $HOME/sim-scratch/out \
  --manifest $HOME/data/impactscore-sim/2026-11-13/MANIFEST.md \
  --markdown $HOME/sim-scratch/report.md --csv-dir docs/impactscore/simulation
```

Durée : environ 2 minutes pour les 7 verticales (12 cœurs, 10 Go de tas). Options :
`-Dimpactscore.simulation.verticals=tv,oven` restreint le calcul ; un fichier de paramètres s'écrit ainsi :

```yaml
washing-machine:
  minRealWeightCoverage: 0.40
```

Le harnais écrit par verticale `<verticale>-products.csv` (une ligne par produit avec offre ou avec un ECOSCORE
stocké : états V1 et V2, valeurs relatives de chaque critère, valeurs brutes) et `<verticale>-weights.csv`, plus
`fidelity.csv`. Le script produit les tableaux ci-dessous et les CSV de [simulation/](simulation/).

## Contrôle de fidélité

V1 hors ligne contre l'ECOSCORE stocké, à 0,05 près. Critère de validité : au moins 99 % des produits notés par le
dernier batch.

- **Cohorte du dernier batch.** Les échelles `MINMAX_OBSERVED` dépendent de la cohorte. Or elle a bougé entre le batch
  du 12/11 et l'export : 30 smartphones notés le 12/11 n'ont plus d'offre, dont un à 3,3 d'indice de réparabilité, le
  minimum observé par le batch ; sans lui l'échelle de ce critère change pour tous. Le contrôle rejoue donc en V1 la
  cohorte exacte du batch : les produits dont `impactScoreMeta.scoredAt` appartient au dernier batch, offre perdue
  depuis ou non.
- **Résultat : 100 % sur 6 verticales, écart maximal 0,0000** (TV, fours, smartphones, lave-vaisselle, lave-linge,
  réfrigérateurs). La simulation est valide.
- **Climatiseurs : 21 sur 81 avec la configuration de `main`, 81 sur 81 (écart 0,0000) avec la configuration de
  climatiseur d'avant GOU-2137.** GOU-2137 a corrigé, en V1 aussi, l'échelle de `CLASSE_ENERGY` des climatiseurs
  (A+++ à D) ; ce changement n'est pas encore en prod. Les 60 climatiseurs dont l'ECOSCORE V1 change le feront à la
  prochaine release, avec ou sans V2. La V1 de comparaison est celle de `main`, correction comprise. Preuve : même
  commande avec `verticals/.../air-conditioner.yml` de `2ecf81dde^`, restauré ensuite.
- Sur la cohorte de l'export (règle de l'issue), la V1 hors ligne s'écarte de l'ECOSCORE stocké par le seul effet de
  cohorte : 100 % reproduits à 0,05 sauf lave-linge (98,0 %, écart max 0,07) et smartphones (0 %, écart max 4,4 : le
  minimum de réparabilité ci-dessus). La comparaison V1/V2 se fait sur cette cohorte, la même pour les deux versions.

## Constats

1. **Garde-fou dépassé pour les lave-linge** : 234 des 1 480 notés V1 (15,8 %) passent en Non noté au seuil 0,50
   (224 couverture faible, 10 aucun critère réel). Il leur manque d'abord les critères EPREL (kWh/100 cycles,
   eau, réparabilité ou durabilité : 233 sur 234). **Seuil qui l'évite : 0,40**, vérifié par une passe avec ce
   paramètre : 141 Non noté (9,5 %). Ce seuil revient à Goulven avant la porte n° 3. Les autres verticales restent
   sous 10 % (TV 5,3 %, fours 6,6 %, réfrigérateurs 2,0 %, lave-vaisselle 1,1 %, smartphones et climatiseurs 0).
2. **Classements très différents pour la TV, les fours et les smartphones** (Spearman V1/V2 0,22, 0,17 et 0,19 ;
   1, 0 et 3 produits communs dans le top 20). Ce n'est pas un défaut du harnais (décomposition exacte des notes par
   critère, voir les tableaux) mais l'effet voulu des changements de méthode :
   - fours : en V1 le classement suit surtout `DATA_QUALITY` (Spearman 0,83 avec la note) car l'énergie par cycle,
     écrasée par une valeur extrême, varie à peine (écart-type 0,16 sur 5). Les bornes robustes 2-98 % lui rendent
     son étendue (écart-type 0,89) : elle classe désormais (0,83) ;
   - smartphones : en V1, `DATA_QUALITY` (0,80) ; 828 des 952 notés ont un indice de réparabilité imputé. En V2, la
     classe de réparabilité UE, réelle pour les 952, et la garantie classent (0,68 et 0,54) ;
   - TV : en V1, les watts bruts (0,69), en V2 la densité W/dm² (0,81) et la classe SDR (0,77).
3. **TV : le biais de taille s'inverse.** Spearman note/diagonale -0,59 en V1, +0,36 en V2 ; diagonale médiane du
   top 5 % : 32 pouces en V1, 75 en V2 (cohorte : 55). Dans le top 20 V2, 15 écrans font 65 pouces ou plus. Les dix TV 27" E/G de l'audit passent du centile 89-99 au centile 30-72. La répartition
   du bloc énergie (GOU-1884, option D) est à confirmer par Goulven à la porte n° 3 avec ces chiffres.
4. **Climatiseurs : les fiches les moins renseignées sont les mieux notées.** Spearman note V2/couverture C : -0,70 ;
   C médiane 0,52, juste au-dessus du seuil. Les 7 premiers (18,8/20) n'ont que 52 % du poids en critères réels
   (classe A+++ et peu d'autres données) : la redistribution porte tout le poids sur des critères favorables. Pas de
   libellé (81 notés, moins de 100).
5. **Lave-vaisselle : 327 fiches entrent dans les listes, pas 51.** Le filtre EPREL levé fait entrer 327 fiches avec
   offre ; 65 sont notées (estimation de la porte n° 2 : 51), **262 restent "Non noté"** sous le seuil 0,60 mais
   entrent dans les pages catégories et, avec leur offre récente, dans le sitemap (323). Réfrigérateurs : 134
   entrants, 133 notés (estimation : 128). Liste pour QA : [dishwasher-entrants.csv](simulation/dishwasher-entrants.csv).
   Corrigé par GOU-2165 : seules les fiches notées entrent (voir [Relance GOU-2165](#relance-gou-2165)).
6. **Sitemap.** À la bascule, les fiches non exclues sans offre récente qui gardent un ECOSCORE V1 en sortent
   (design 4.2). Export du 13/11 06 h UTC, puis connexion "Elasticsearch lecture" (prod) le 13/11 vers 12 h 30 UTC
   (`count` : verticale, `excluded=false`, `exists scores.ECOSCORE.value`, sans `offersCount > 0` et
   `lastChange > now-2d`) :

   | Verticale | Export | ES prod |
   |---|---:|---:|
   | TV | 1 833 | 1 868 |
   | Fours | 667 | 676 |
   | Smartphones | 630 | 630 |
   | Lave-vaisselle | 736 | 736 |
   | Lave-linge | 1 576 | 1 576 |
   | Réfrigérateurs | 1 625 | 1 625 |
   | Climatiseurs | 154 | 154 |

   Aucune autre fiche ne sort du sitemap : les notés V1 qui passent en Non noté ont tous une offre récente.
7. **Distributions.** L'étendue p5-p95 s'élargit partout où la V1 était tassée par les moyennes imputées (TV 2,6 à
   5,1 points). Les libellés suivent les bornes 0,90 / 0,50 / 0,10 : environ 10 / 40 / 40 / 10 %.

## Relance GOU-2165

Même export, même commande, avec les deux règles d'exclusion V2 de GOU-2165 (design § 3) : sans EPREL, entrée
seulement si C atteint `v2.minRealWeightCoverage` (`missing_eprel_low_coverage`) ; TV, exclusion des catégories EPREL
`MONITOR`, `PROFESSIONAL_DISPLAY` et `SIGNAGE` (`eprel_category_<CATÉGORIE>`). Fiches avec offre ; "entrant" = exclu
dans l'export, gardé en V2 ; "sortant" = gardé dans l'export, exclu en V2.

| Verticale | Entrants | dont notés V2 | Exclus par `missing_eprel_low_coverage` | Sortants | Sortants par cause |
|---|---:|---:|---:|---:|---|
| Lave-vaisselle | 65 (327 avant) | 65 | 399 | 0 | |
| Réfrigérateurs | 133 (134 avant) | 133 | 2 132 | 0 | |
| TV | 0 | 0 | 0 (`eprelRequired: true`) | 75 | `eprel_category_MONITOR` 42, `eprel_category_SIGNAGE` 31, `eprel_category_PROFESSIONAL_DISPLAY` 2 |

- Lave-vaisselle : les 65 notés de la première passe entrent et restent notés ; les 262 non notés restent exclus.
  Réfrigérateurs : le seul entrant non noté (3527570079139) reste exclu. Les 399 lave-vaisselle et 2 132
  réfrigérateurs exclus par la nouvelle cause l'étaient déjà (`missing_eprel`, souvent avec d'autres causes).
- TV : les 4 moniteurs relevés par QA sortent (8806094355796, 8806097691679, 8806096527979 : `MONITOR` ;
  4719331862657 : `PROFESSIONAL_DISPLAY`). Les 75 sortants ont tous un ECOSCORE V1 stocké : liste
  [tv-sortants-categorie-eprel.csv](simulation/tv-sortants-categorie-eprel.csv). Notés V2 : 1 965 au lieu de 2 037 ;
  notés V1 passés en Non noté : 111 au lieu de 114.
- Lave-vaisselle, même nombre de notés V2 (1 065) : la médiane V2 passe de 9,92 à 9,85, les bornes robustes étant
  calculées sur une cohorte sans les 262 fiches non notées.
- **V1 inchangée** : l'exclusion recalculée avec la configuration V1 (colonnes `v1_excluded`, `v1_causes` du
  harnais) est identique à l'exclusion stockée sur les 36 828 fiches avec offre des 7 verticales (même état, mêmes
  causes à l'ordre près) ; contrôle de fidélité identique à la première passe.

## Relance porte n° 3 (GOU-2186)

Même export, mêmes commandes (`-Dimpactscore.simulation.verticals=tv,washing-machine`), règles GOU-2165 comprises, avec les
deux décisions de la porte n° 3 (14/11, [GOU-1888](decisions.md)) lues dans les blocs `v2` : lave-linge
`minRealWeightCoverage` 0,40 ; TV, bloc énergie option B (`POWER_DENSITY_SDR` et `POWER_CONSUMPTION_SDR`, 0,0963855
chacun, ce dernier en `MINMAX_QUANTILE` 0,02 / 0,98). `methodologyVersion` reste à 1. Contrôle de fidélité V1 inchangé
(TV 100 %, lave-linge 100 %). CSV régénérés : `tv-ecarts`, `tv-non-notes`, `washing-machine-ecarts`,
`washing-machine-non-notes`. Les tableaux des sections par verticale plus bas restent ceux de la relance GOU-2165.

| | Avant | Après |
|---|---:|---:|
| **TV** notés V2 | 1 965 | 1 965 |
| TV « Non noté » : couverture faible / aucun critère réel | 98 / 13 | 98 / 13 (111 notés V1, 5,2 %) |
| TV, Spearman V1/V2 | 0,218 (2 037 produits, avant GOU-2165) | **0,577** (1 965 produits) |
| TV, écrans de 65" ou plus dans les 20 premiers | 15/20 | **4/20** (65", 75", 85", 86") |
| TV, diagonale médiane du top 5 % | 75" | 40" |
| TV, Spearman note/diagonale en V2 | 0,36 | -0,14 |
| TV, 115" à 750 W en HDR (TCL-115C7K) | centile 97,7, « Parmi les mieux classés » | rang 373/1 965, centile 81, V2 10,31, libellé « good » |
| TV, p5 / p50 / p95 V2 | 6,69 / 9,10 / 11,76 | 6,98 / 9,07 / 11,39 |
| **Lave-linge** notés V2 | 1 246 | 1 339 |
| Lave-linge « Non noté » : couverture faible / aucun critère réel | 224 / 10 | 131 / 10 |
| Lave-linge, notés V1 passés en Non noté | 234 (15,8 %) | **141 (9,5 %)** |
| Lave-linge, Spearman V1/V2 | 0,874 | 0,892 |

Le garde-fou des 10 % n'est dépassé ni en TV (5,2 %) ni en lave-linge (9,5 %). Le top 20 TV de la relance se lit dans le
rapport régénéré ; les écrans de petite diagonale y reprennent la tête (24 à 43" pour 13 des 20).

## Relecture QA

- Les 20 plus gros écarts de rang de chaque verticale sont dans les tableaux ci-dessous (200 par verticale dans
  `simulation/<verticale>-ecarts.csv`). Colonne "Critères qui expliquent l'écart" : les trois critères dont l'effet
  sur la note (points sur 20, V2 moins V1) s'écarte le plus de l'effet moyen de la cohorte, avec sa cause :
  `DATA_QUALITY retiré`, `redistribution` (même valeur relative, poids renormalisé), `redistribution (moyenne imputée
  retirée)`, `bornes robustes`, `échelle V2`, `unité TV`, ou un critère remplacé. QA note "cohérent" ou "suspect"
  pour chacun, sur 2 verticales.
- Les 327 lave-vaisselle entrants (65 notés) : `simulation/dishwasher-entrants.csv`.
- Autres listes : `<verticale>-non-notes.csv` (notés V1 passés en Non noté, avec leur couverture) et
  `refrigerator-entrants.csv`.

## Limites

- Une seule date d'export ; la cohorte bouge d'un jour à l'autre (voir le contrôle de fidélité).
- "Offre récente" = `offersCount > 0` et `lastChange` de moins de 48 h avant le début de l'export, comme
  `ProductRepository.exportVerticalForSitemap`.
- Les rangs comparés sont des centiles (part de la cohorte notée que le produit dépasse), les cohortes V1 et V2
  n'ayant pas la même taille.

## Contrôle de fidélité

| Verticale | Notés par le dernier batch | Reproduits à 0,05 | Part | Écart max | Cohorte de l'export : comparés | Reproduits | Part |
|---|---:|---:|---:|---:|---:|---:|---:|
| tv | 2159 | 2159 | 100,0 % | 0,0000 | 2141 | 2141 | 100,0 % |
| oven | 1444 | 1444 | 100,0 % | 0,0000 | 1436 | 1436 | 100,0 % |
| smartphones | 977 | 977 | 100,0 % | 0,0000 | 947 | 0 | 0,0 % |
| dishwasher | 1044 | 1044 | 100,0 % | 0,0000 | 1007 | 1007 | 100,0 % |
| washing-machine | 1520 | 1520 | 100,0 % | 0,0000 | 1469 | 1440 | 98,0 % |
| refrigerator | 2403 | 2403 | 100,0 % | 0,0000 | 2341 | 2341 | 100,0 % |
| air-conditioner | 81 | 21 | 25,9 % | 3,5877 | 79 | 21 | 26,6 % |

## TV (`tv`)

Seuil V2 0,50, filtre EPREL gardé.

### Effectifs

| Notés V1 | Cohorte V2 | Notés V2 | Non noté : couverture faible | Non noté : aucun critère réel | Entrants (filtre EPREL levé) | dont notés | Notés V1 passés en Non noté |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 2151 | 2151 | 2037 | 101 | 13 | 0 | 0 | 114 (5,3 %) |

### Distribution

| Version | p5 | p50 | p95 | p95 - p5 |
|---|---:|---:|---:|---:|
| V1 | 11,43 | 13,01 | 14,07 | 2,64 |
| V2 | 6,69 | 9,10 | 11,76 | 5,07 |

Libellés V2 : excellent 204 (10,0 %), good 815 (40,0 %), fair 814 (40,0 %), poor 204 (10,0 %).

### Classements

Spearman V1/V2 sur les 2037 produits notés dans les deux versions : **0,218** (ECOSCORE stocké contre V2 : 0,217).

Top 20 : 1 produits communs. Place en V2 des produits du top 20 V1 :
178, 542, 41, 1565, 40, 383, 194, 1073, 1, 78, 293, 792, 339, 39, 181, 172, 563, 236, 660, 289.

| # | Top 20 V1 | V1 | Top 20 V2 | V2 | Place V1 |
|---:|---|---:|---|---:|---:|
| 1 | PHILIPS-40PFS6009/12 | 15,53 | HAIER-H85M92FUX | 14,71 | 24 |
| 2 | PHILIPS-24PHS6808/12 | 15,33 | HAIER-H75M92FUX | 14,71 | 9 |
| 3 | SCHNEIDER-GMS50F1 | 15,09 | HIKVISION-DS-D5C86RB/A | 14,29 | 576 |
| 4 | ASUS-MB14FCD | 15,05 | CONTINENTAL EDISON-CELED32HD26B7 | 14,16 | 26 |
| 5 | CONTINENTAL EDISON-CELED32SGHD24B6 | 15,02 | TCL-65RP630KX7 | 13,75 | 248 |
| 6 | PANASONIC-TB-24S40AEZ | 15,01 | HAIER-H100S90FUX | 13,67 | 337 |
| 7 | VIEWSONIC-VS19479(VG2709-2K-MHD-2 | 14,99 | PHILIPS-43HFL3014/12 | 13,58 | 136 |
| 8 | PHILIPS-32PFS6908/12 | 14,96 | SONY-K-85XR75M2 | 13,18 | 389 |
| 9 | HAIER-H75M92FUX | 14,95 | HISENSE-85U7Q PRO | 13,05 | 683 |
| 10 | NEVIR-NVR-8430-32WBT | 14,89 | HISENSE-85U8Q | 13,05 | 765 |
| 11 | PANASONIC-TB-32S40AEZ | 14,83 | SONY-K-85XR5 | 12,97 | 482 |
| 12 | LG-32LQ63006LA | 14,80 | LG-75QNED826RE | 12,91 | 111 |
| 13 | SCHNEIDER-GMS43F1 | 14,73 | TCL-85C61KX2 | 12,89 | 1370 |
| 14 | NEVIR-NVR-8840-40FHWBT | 14,71 | SHARP-DD-EA242W | 12,82 | 61 |
| 15 | HAIER-H55Q800UX | 14,70 | TCL-98C89KX1 | 12,79 | 1598 |
| 16 | PHILIPS-55PUS8009/12 | 14,65 | PANASONIC-TB-55W60CEZS | 12,79 | 390 |
| 17 | AOC-27G4ZRE | 14,65 | HISENSE-100UXQ | 12,78 | 1325 |
| 18 | PANASONIC-TB-40S45AEZ | 14,63 | TCL-85C69K PROX1 | 12,78 | 1214 |
| 19 | LG-27ART10AKPL | 14,61 | BENQ-GW2490TC | 12,77 | 42 |
| 20 | SYLVOX-KT16K0KOGA | 14,59 | TCL-75Q8C-UKX1 | 12,75 | 1016 |

Écart de rang (points de centile, |V2 - V1|) : médiane 25,0, p90 60,7, plus de 20 points pour 1195 produits.

Les 20 plus gros écarts de rang (centile : part de la cohorte notée que le produit dépasse ; effets en points sur 20, rapportés à l'effet moyen de la cohorte) :

| GTIN | Produit | V1 | Centile V1 | V2 | Centile V2 | C | Critères qui expliquent l'écart |
|---|---|---:|---:|---:|---:|---:|---|
| 5901292525699 | TCL-115C7K | 11,10 | 2,4 | 12,23 | 97,7 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,13; POWER_DENSITY_SDR [unité TV] +1,12; POWER_CONSUMPTION_HDR [unité TV] +1,11 |
| 8806096527979 | LG-27LX6TDGA | 14,29 | 97,6 | 6,85 | 6,3 | 0,83 | POWER_DENSITY_SDR [unité TV] -2,04; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,57; POWER_CONSUMPTION_SDR [unité TV] -0,46 |
| 8806097691679 | SAMSUNG-UE27LSM7FAX | 14,02 | 93,9 | 6,47 | 4,0 | 0,83 | POWER_DENSITY_SDR [unité TV] -2,06; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,57; POWER_CONSUMPTION_SDR [unité TV] -0,45 |
| 4719331862657 | GIGABYTE-MO27Q2 | 13,94 | 92,8 | 6,58 | 4,6 | 0,77 | POWER_DENSITY_SDR [unité TV] -2,53; CLASSE_ENERGY_SDR [redistribution] -0,59; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,57 |
| 6942351423159 | HISENSE-116UXQ | 11,81 | 10,1 | 11,78 | 95,3 | 0,83 | POWER_DENSITY_SDR [unité TV] +1,90; CLASSE_ENERGY_SDR [redistribution] +0,83; POWER_CONSUMPTION_HDR [unité TV] +0,64 |
| 5901292529840 | TCL-115RM9LX1 | 10,65 | 0,7 | 10,62 | 84,7 | 0,83 | POWER_DENSITY_SDR [unité TV] +1,73; POWER_CONSUMPTION_SDR [unité TV] +1,12; POWER_CONSUMPTION_HDR [unité TV] +1,01 |
| 8806094355796 | SAMSUNG-S27B800PXU | 13,86 | 91,3 | 7,00 | 7,7 | 0,77 | POWER_DENSITY_SDR [unité TV] -1,98; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,57; POWER_CONSUMPTION_SDR [unité TV] -0,46 |
| 8806094215946 | SAMSUNG-QE43LS03BAU | 13,91 | 92,2 | 7,09 | 8,7 | 1,00 | POWER_DENSITY_SDR [unité TV] -2,74; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,26; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,64 |
| 5901292531126 | TCL-98C79LX1 | 12,04 | 13,8 | 12,14 | 97,3 | 0,83 | POWER_DENSITY_SDR [unité TV] +1,90; CLASSE_ENERGY_SDR [redistribution] +0,83; POWER_CONSUMPTION_HDR [unité TV] +0,68 |
| 8806097067689 | SAMSUNG-TQ100QN80FU | 11,53 | 6,1 | 10,97 | 88,6 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,23; POWER_CONSUMPTION_SDR [unité TV] +0,90; POWER_DENSITY_SDR [unité TV] +0,61 |
| 8806092656864 | SAMSUNG-HG32EJ690FU | 13,72 | 86,0 | 6,45 | 3,8 | 0,83 | POWER_DENSITY_SDR [unité TV] -1,41; CLASSE_ENERGY_SDR [redistribution] -0,59; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,57 |
| 8806092150799 | SAMSUNG-QE43LS03AAU | 13,84 | 90,1 | 7,02 | 8,4 | 1,00 | POWER_DENSITY_SDR [unité TV] -2,74; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,19; CLASSE_ENERGY_SDR [redistribution] -0,59 |
| 8806094901221 | SAMSUNG-HG32T5300EU | 13,70 | 85,3 | 6,45 | 3,8 | 0,83 | POWER_DENSITY_SDR [unité TV] -1,41; CLASSE_ENERGY_SDR [redistribution] -0,59; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,57 |
| 6942351417042 | HISENSE-100E7Q PRO | 12,01 | 13,4 | 11,69 | 94,4 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,13; POWER_DENSITY_SDR [unité TV] +0,87; POWER_CONSUMPTION_SDR [unité TV] +0,74 |
| 5025232965687 | PANASONIC-TV-65W80AEZ | 11,61 | 7,3 | 10,85 | 87,6 | 1,00 | REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,76; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,59; DATA_QUALITY [DATA_QUALITY retiré] -0,40 |
| 5901292529918 | TCL-98C7L-UKX1 | 12,20 | 17,0 | 12,14 | 97,3 | 0,83 | POWER_DENSITY_SDR [unité TV] +1,90; CLASSE_ENERGY_SDR [redistribution] +0,83; POWER_CONSUMPTION_HDR [unité TV] +0,68 |
| 5901292529963 | TCL-98RM7L-UKX7 | 12,20 | 17,0 | 12,14 | 97,3 | 0,83 | POWER_DENSITY_SDR [unité TV] +1,90; CLASSE_ENERGY_SDR [redistribution] +0,83; POWER_CONSUMPTION_HDR [unité TV] +0,68 |
| 8806097959731 | SAMSUNG-TQ100QN80HU | 11,59 | 7,1 | 10,79 | 87,2 | 0,83 | POWER_DENSITY_SDR [unité TV] +1,59; POWER_CONSUMPTION_SDR [unité TV] +0,75; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,57 |
| 4548736177864 | SONY-K-100XR35M2 | 11,63 | 7,5 | 10,85 | 87,6 | 0,80 | POWER_DENSITY_SDR [unité TV] +1,80; POWER_CONSUMPTION_SDR [unité TV] +0,74; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,57 |
| 8806095540269 | SAMSUNG-TU98DU9005K | 11,43 | 4,9 | 10,64 | 84,9 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,06; POWER_CONSUMPTION_SDR [unité TV] +0,92; POWER_CONSUMPTION_HDR [unité TV] +0,64 |

### Listes et sitemap

| Pages catégories : fiches ajoutées | dont notées V2 | Notés V1 qui perdent leur note (restent listés) | Sitemap : fiches ajoutées | Sitemap : fiches qui sortent | dont sans offre récente, ECOSCORE hérité | dont non notées V2, offre ancienne |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 0 | 114 | 0 | 1833 | 1833 | 0 |

## Fours (`oven`)

Seuil V2 0,50, filtre EPREL gardé.

### Effectifs

| Notés V1 | Cohorte V2 | Notés V2 | Non noté : couverture faible | Non noté : aucun critère réel | Entrants (filtre EPREL levé) | dont notés | Notés V1 passés en Non noté |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 1446 | 1446 | 1350 | 96 | 0 | 0 | 0 | 96 (6,6 %) |

### Distribution

| Version | p5 | p50 | p95 | p95 - p5 |
|---|---:|---:|---:|---:|
| V1 | 13,79 | 15,15 | 16,08 | 2,28 |
| V2 | 9,50 | 13,98 | 16,78 | 7,29 |

Libellés V2 : excellent 130 (9,6 %), good 495 (36,7 %), fair 590 (43,7 %), poor 135 (10,0 %).

### Classements

Spearman V1/V2 sur les 1350 produits notés dans les deux versions : **0,172** (ECOSCORE stocké contre V2 : 0,171).

Top 20 : 0 produits communs. Place en V2 des produits du top 20 V1 :
426, 240, 578, 419, 601, 418, 178, 475, 263, 379, 339, 393, 557, 553, 553, 605, 781, 803, 803, 803.

| # | Top 20 V1 | V1 | Top 20 V2 | V2 | Place V1 |
|---:|---|---:|---|---:|---:|
| 1 | BEKO-BBIE12100XC 7727188412 | 17,37 | WHIRLPOOL-AMW 591/IXL | 19,16 | 937 |
| 2 | ESSENTIEL B-EFMP105I2 8010298 | 17,31 | HOTPOINT/ARISTON-HAO3 K78PSU1Z B | 18,99 | 837 |
| 3 | ELECTROLUX-FR53G 949716752 | 16,99 | HISENSE-BI64223EPBG | 18,90 | 840 |
| 4 | BEKO-BBCM13300XC 7780382909 | 16,99 | BEKO-CSS42014FS | 18,84 | 24 |
| 5 | BEKO-BBIE12100XD 7727188411 | 16,92 | CANDY-CA6 N5B3LYTB | 18,80 | 844 |
| 6 | BEKO-BBIE17302BMP 7739182960 | 16,92 | CANDY-CA6 N5B3EHTX | 18,72 | 865 |
| 7 | Proline-P602IMCIBK | 16,80 | CANDY-CA6 N3B1YTX | 18,69 | 870 |
| 8 | BEKO-BBVM13400XPSE 7768287625 | 16,77 | BOSCH-CBG7341B1 | 18,68 | 592 |
| 9 | Proline-PCC662WH | 16,75 | ELECTROLUX-KOBBS39WX 944032100 | 18,67 | 869 |
| 10 | BEKO-859993136200 FSE58301BC | 16,72 | CANDY-CA6 NP5B3HTX1 | 18,66 | 874 |
| 11 | AMICA-THV60X | 16,71 | HAIER-H6 ID46G5SYTB | 18,66 | 873 |
| 12 | BEKO-FSS53011DW 7786986811 | 16,68 | HAIER-H6 ID68C5YTB | 18,64 | 878 |
| 13 | LEISURE-PR90F560K | 16,64 | HAIER-H6 ID2P5B3YTX | 18,62 | 881 |
| 14 | BEKO-FSE68302MXC 7717187663 | 16,64 | OCEANIC-OCEACE4F5060W | 18,60 | 965 |
| 15 | BEKO-FBE62131XDT 7786187630 | 16,64 | OCEANIC-OCEACE4F5060B | 18,60 | 964 |
| 16 | BEKO-FBS63031WCS 7738987670 | 16,62 | HAIER-H6 ID46G3YTB | 18,53 | 894 |
| 17 | SMEG-C9IMMB2 | 16,60 | WHIRLPOOL-W7 MS450 | 18,51 | 196 |
| 18 | THOMSON-TMFC70IX2 | 16,59 | LG-WSED7613S | 18,10 | 889 |
| 19 | LEISURE-PR100F560K | 16,59 | DE'LONGHI-SEW 8540 N EDM | 17,90 | 981 |
| 20 | SAMSUNG-NV68A1170BS | 16,59 | LG-WSED7612S | 17,87 | 963 |

Écart de rang (points de centile, |V2 - V1|) : médiane 25,4, p90 61,4, plus de 20 points pour 864 produits.

Les 20 plus gros écarts de rang (centile : part de la cohorte notée que le produit dépasse ; effets en points sur 20, rapportés à l'effet moyen de la cohorte) :

| GTIN | Produit | V1 | Centile V1 | V2 | Centile V2 | C | Critères qui expliquent l'écart |
|---|---|---:|---:|---:|---:|---:|---|
| 8017709332020 | SMEG-CG90CIXT | 16,37 | 97,9 | 10,98 | 11,2 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -5,25; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 3615993774301 | Proline-PGC5050BK | 15,63 | 88,1 | 8,93 | 3,8 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -6,47; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 8690842671142 | BEKO-BBISA17302BMP 7757883898 | 15,98 | 94,0 | 10,82 | 10,1 | 0,76 | CLASSE_ENERGY [redistribution (moyenne imputée retirée)] -3,82; OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -1,49; WARRANTY [redistribution] +1,39 |
| 8054383965083 | AIRLUX-CC902GTBK3 | 15,62 | 87,9 | 9,46 | 4,9 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -6,01; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 8690842826764 | BEKO-BBIM18301XFPE-1C 7768288450 | 15,75 | 89,4 | 10,45 | 8,1 | 0,76 | CLASSE_ENERGY [redistribution (moyenne imputée retirée)] -3,82; OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -1,49; WARRANTY [redistribution] +1,39 |
| 8690842588853 | BEKO-BBIS17301BMP 7768287871 | 15,75 | 89,3 | 10,45 | 8,1 | 0,76 | CLASSE_ENERGY [redistribution (moyenne imputée retirée)] -3,82; OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -1,49; WARRANTY [redistribution] +1,39 |
| 8054383961801 | AIRLUX-AFGC310IX | 15,46 | 84,2 | 8,73 | 3,0 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -6,47; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 8690842594465 | BEKO-BBIM18301XFPE-1 7757883871 | 16,59 | 98,6 | 11,74 | 17,9 | 0,76 | CLASSE_ENERGY [redistribution (moyenne imputée retirée)] -3,82; OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -1,49; WARRANTY [redistribution] +1,39 |
| 8017709340643 | SMEG-TR90IMBL | 16,42 | 98,1 | 11,70 | 17,7 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -4,59; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 8017709332037 | SMEG-SCB95CIX | 16,43 | 98,1 | 11,70 | 17,8 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -4,59; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 8806094336672 | SAMSUNG-NV7B41307AB | 15,79 | 90,2 | 11,03 | 11,5 | 0,76 | CLASSE_ENERGY [redistribution (moyenne imputée retirée)] -3,82; OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -2,15; WARRANTY [redistribution] +1,39 |
| 8690842489334 | BEKO-FSE68304MAPS 7717187661 | 15,63 | 88,2 | 10,70 | 9,7 | 0,76 | CLASSE_ENERGY [redistribution (moyenne imputée retirée)] -3,82; WARRANTY [redistribution] +1,39; OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -1,04 |
| 8052745126561 | AIRLUX-AA65CMIX2 | 15,36 | 79,8 | 8,65 | 2,4 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -6,44; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 5028683124313 | FALCON-PROP90FXEISL/C-EU | 16,46 | 98,3 | 12,15 | 21,1 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -4,18; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 8017709195762 | SMEG-SF6381X | 15,91 | 92,5 | 11,58 | 16,8 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -4,09; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 8017709241537 | SMEG-SF6905N1 | 15,86 | 91,3 | 11,52 | 16,4 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -4,09; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 8017709241551 | SMEG-SF6905P1 | 15,86 | 91,3 | 11,53 | 16,5 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -4,09; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 8690842453397 | BEKO-BBIM13300CDXE 7768286734 | 16,18 | 96,5 | 12,28 | 22,2 | 0,76 | CLASSE_ENERGY [redistribution (moyenne imputée retirée)] -3,82; WARRANTY [redistribution] +1,39; WEIGHT [redistribution] +1,06 |
| 8050147715789 | INDESIT-I6GGKMWF | 15,31 | 76,3 | 8,53 | 2,1 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -6,49; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |
| 5028683128373 | FALCON-CLAS90FXDFBL/B-EU | 15,53 | 86,2 | 11,05 | 12,3 | 1,00 | OVEN_ENERGY_CONSUMPTION_CYCLE [bornes robustes] -4,18; DATA_QUALITY [DATA_QUALITY retiré] -0,89; CLASSE_ENERGY [redistribution] +0,88 |

### Listes et sitemap

| Pages catégories : fiches ajoutées | dont notées V2 | Notés V1 qui perdent leur note (restent listés) | Sitemap : fiches ajoutées | Sitemap : fiches qui sortent | dont sans offre récente, ECOSCORE hérité | dont non notées V2, offre ancienne |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 0 | 96 | 0 | 667 | 667 | 0 |

## Smartphones (`smartphones`)

Seuil V2 0,50, filtre EPREL gardé.

### Effectifs

| Notés V1 | Cohorte V2 | Notés V2 | Non noté : couverture faible | Non noté : aucun critère réel | Entrants (filtre EPREL levé) | dont notés | Notés V1 passés en Non noté |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 952 | 952 | 952 | 0 | 0 | 0 | 0 | 0 (0,0 %) |

### Distribution

| Version | p5 | p50 | p95 | p95 - p5 |
|---|---:|---:|---:|---:|
| V1 | 10,05 | 12,56 | 14,67 | 4,62 |
| V2 | 10,29 | 13,34 | 15,19 | 4,91 |

Libellés V2 : excellent 92 (9,7 %), good 286 (30,0 %), fair 467 (49,1 %), poor 107 (11,2 %).

### Classements

Spearman V1/V2 sur les 952 produits notés dans les deux versions : **0,186** (ECOSCORE stocké contre V2 : 0,161).

Top 20 : 3 produits communs. Place en V2 des produits du top 20 V1 :
49, 49, 49, 278, 281, 1, 2, 2, 104, 104, 104, 104, 104, 104, 526, 526, 526, 526, 526, 526.

| # | Top 20 V1 | V1 | Top 20 V2 | V2 | Place V1 |
|---:|---|---:|---|---:|---:|
| 1 | SAMSUNG-SM-S921B | 15,70 | REALME-RMX3939 | 16,85 | 6 |
| 2 | SAMSUNG-SM-S921B | 15,70 | REALME-RMX3853 | 16,84 | 8 |
| 3 | SAMSUNG-SM-S921B | 15,70 | REALME-RMX3853 | 16,84 | 7 |
| 4 | SAMSUNG-SM-G556B | 15,17 | REALME-RMX3851 | 16,81 | 27 |
| 5 | APPLE-A3290 | 15,16 | MOTOROLA-G57 POWER (XT2537-5 | 16,75 | 160 |
| 6 | REALME-RMX3939 | 15,09 | MOTOROLA-G57 POWER (XT2537-5 | 16,75 | 159 |
| 7 | REALME-RMX3853 | 15,09 | LOGICOM-ORION E2 | 16,67 | 531 |
| 8 | REALME-RMX3853 | 15,09 | BLACKVIEW-WAVE 2C | 16,67 | 524 |
| 9 | XIAOMI-25069PTEBG | 15,08 | REALME-RMX3840 | 16,67 | 523 |
| 10 | XIAOMI-25069PTEBG | 15,08 | REALME-RMX3851 | 16,67 | 522 |
| 11 | XIAOMI-25069PTEBG | 15,08 | OUKITEL-P2 PLUS | 16,67 | 514 |
| 12 | XIAOMI-25069PTEBG | 15,08 | OUKITEL-P2 PLUS | 16,67 | 513 |
| 13 | XIAOMI-25069PTEBG | 15,08 | OUKITEL-P2 PLUS | 16,67 | 512 |
| 14 | XIAOMI-25069PTEBG | 15,08 | OUKITEL-P2 PLUS | 16,67 | 511 |
| 15 | SAMSUNG-SM-S921B/DS | 15,07 | OUKITEL-P2 PLUS | 16,67 | 510 |
| 16 | SAMSUNG-SM-S921B/DS | 15,07 | OUKITEL-P2 PLUS | 16,67 | 509 |
| 17 | SAMSUNG-SM-S921B/DS | 15,07 | BLACKVIEW-BV8200 | 16,67 | 505 |
| 18 | SAMSUNG-SM-S921B/DS | 15,07 | BLACKVIEW-BV8200 | 16,67 | 501 |
| 19 | SAMSUNG-SM-S921B/DS | 15,07 | OUKITEL-C17 MINI | 16,67 | 480 |
| 20 | SAMSUNG-SM-S921B/DS | 15,07 | OUKITEL-C17 MINI | 16,67 | 479 |

Écart de rang (points de centile, |V2 - V1|) : médiane 27,9, p90 58,7, plus de 20 points pour 605 produits.

Les 20 plus gros écarts de rang (centile : part de la cohorte notée que le produit dépasse ; effets en points sur 20, rapportés à l'effet moyen de la cohorte) :

| GTIN | Produit | V1 | Centile V1 | V2 | Centile V2 | C | Critères qui expliquent l'écart |
|---|---|---:|---:|---:|---:|---:|---|
| 6931548308010 | BLACKVIEW-BL9000 | 14,47 | 91,0 | 10,29 | 0,3 | 0,92 | REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -3,17; WEIGHT [redistribution (moyenne imputée retirée)] -0,84; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -0,61 |
| 887614460891 | BLACKVIEW-BV9300 PRO | 13,87 | 88,4 | 10,29 | 0,3 | 0,92 | REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -2,57; WEIGHT [redistribution (moyenne imputée retirée)] -0,84; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -0,61 |
| 8806097038979 | SAMSUNG-SM-S938B/DS | 14,51 | 91,8 | 10,88 | 9,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097038924 | SAMSUNG-SM-S938B/DS | 14,51 | 91,7 | 10,88 | 9,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097038894 | SAMSUNG-SM-S938B/DS | 14,51 | 91,6 | 10,88 | 9,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097038856 | SAMSUNG-SM-S938B/DS | 14,51 | 91,5 | 10,88 | 9,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097038818 | SAMSUNG-SM-S938B/DS | 14,51 | 91,4 | 10,88 | 9,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097038771 | SAMSUNG-SM-S938B/DS | 14,51 | 91,3 | 10,88 | 9,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097072867 | SAMSUNG-SM-A266B/DS | 14,15 | 89,3 | 10,40 | 7,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097038689 | SAMSUNG-SM-S938B/DS | 14,51 | 91,2 | 10,88 | 9,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097072843 | SAMSUNG-SM-A266B/DS | 14,15 | 89,2 | 10,40 | 7,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097038603 | SAMSUNG-SM-S938B/DS | 14,51 | 91,1 | 10,88 | 9,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097072775 | SAMSUNG-SM-A266B/DS | 14,15 | 89,1 | 10,40 | 7,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806095984285 | SAMSUNG-SM-A366B/DS | 13,86 | 88,2 | 10,41 | 7,8 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; WEIGHT [redistribution] +0,69 |
| 6921815629511 | ONEPLUS-CPH2719 | 10,27 | 8,8 | 14,12 | 87,6 | 1,00 | REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] +4,03; DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] +1,44 |
| 8032325388671 | MOTOROLA-G06 POWER (XT2535-13 | 10,67 | 18,8 | 16,65 | 96,7 | 0,92 | EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] +4,85; DATA_QUALITY [DATA_QUALITY retiré] +1,54; WEIGHT [redistribution (moyenne imputée retirée)] -0,84 |
| 8806097044475 | SAMSUNG-SM-S936B/DS | 14,59 | 93,8 | 10,99 | 16,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097044444 | SAMSUNG-SM-S936B/DS | 14,59 | 93,7 | 10,99 | 16,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097044369 | SAMSUNG-SM-S936B/DS | 14,59 | 93,6 | 10,99 | 16,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |
| 8806097044321 | SAMSUNG-SM-S936B/DS | 14,59 | 93,5 | 10,99 | 16,4 | 1,00 | DATA_QUALITY [DATA_QUALITY retiré] -2,21; EU_REPAIRABILITY_CLASS [classe de réparabilité UE (porte n° 2)] -1,06; REPAIRABILITY_INDEX [classe de réparabilité UE (porte n° 2)] -0,77 |

### Listes et sitemap

| Pages catégories : fiches ajoutées | dont notées V2 | Notés V1 qui perdent leur note (restent listés) | Sitemap : fiches ajoutées | Sitemap : fiches qui sortent | dont sans offre récente, ECOSCORE hérité | dont non notées V2, offre ancienne |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 0 | 0 | 0 | 630 | 630 | 0 |

## Lave-vaisselle (`dishwasher`)

Seuil V2 0,60, filtre EPREL levé.

### Effectifs

| Notés V1 | Cohorte V2 | Notés V2 | Non noté : couverture faible | Non noté : aucun critère réel | Entrants (filtre EPREL levé) | dont notés | Notés V1 passés en Non noté |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 1011 | 1338 | 1065 | 85 | 188 | 327 | 65 | 11 (1,1 %) |

### Distribution

| Version | p5 | p50 | p95 | p95 - p5 |
|---|---:|---:|---:|---:|
| V1 | 7,54 | 11,27 | 15,04 | 7,50 |
| V2 | 4,58 | 9,92 | 15,75 | 11,17 |

Libellés V2 : excellent 87 (8,2 %), good 446 (41,9 %), fair 425 (39,9 %), poor 107 (10,0 %).

### Classements

Spearman V1/V2 sur les 1000 produits notés dans les deux versions : **0,992** (ECOSCORE stocké contre V2 : 0,992).

Top 20 : 10 produits communs. Place en V2 des produits du top 20 V1 :
73, 8, 65, 7, 6, 11, 16, 5, 22, 22, 28, 19, 9, 34, 24, 3, 3, 69, 68, 21.

| # | Top 20 V1 | V1 | Top 20 V2 | V2 | Place V1 |
|---:|---|---:|---|---:|---:|
| 1 | BEKO-BDIN38561C 7699501677 | 16,13 | SCHNEIDER-SCDW649-S | 17,23 | 24 |
| 2 | ELECTROLUX-E82LX220S 911473126 | 16,09 | BOMANN-TSG 7405 | 17,23 | 23 |
| 3 | SIEMENS-SN63EX22CE | 16,05 | MEDION-MD 37496 | 17,06 | 17 |
| 4 | Miele-G 5990 SCVI SL | 16,05 | MEDION-MD 37496 | 17,06 | 16 |
| 5 | SAMSUNG-DW60CG880B00ET | 15,90 | ELECTROLUX-E82LX520T 911474020 | 16,93 | 8 |
| 6 | MEDION-MD 37763 | 15,86 | SAMSUNG-DW60CG880B00ET | 16,93 | 5 |
| 7 | Miele-G 5611 SCI ACTIVE | 15,84 | Miele-G 5990 SCVI SL | 16,89 | 4 |
| 8 | ELECTROLUX-E82LX520T 911474020 | 15,80 | ELECTROLUX-E82LX220S 911473126 | 16,75 | 2 |
| 9 | SIEMENS-SR65EX09ME | 15,67 | MEDION-MD 37763 | 16,64 | 13 |
| 10 | BOSCH-SPS4EMI17E | 15,67 | BOMANN-TSG 5704 | 16,62 | 34 |
| 11 | BOSCH-SPS4EMW17E | 15,62 | MEDION-MD 37763 | 16,56 | 6 |
| 12 | Miele-G 7650 SCVI AUTODOS | 15,61 | DURONIC-DW10 | 16,39 | 26 |
| 13 | MEDION-MD 37763 | 15,61 | BOSCH-SMV8ECX05E | 16,32 | 67 |
| 14 | SIEMENS-SR25YI04ME | 15,60 | HOTPOINT/ARISTON-HA3ITA14B1N6W | 16,26 | 96 |
| 15 | Miele-G 7260 SCVI | 15,56 | HOTPOINT/ARISTON-H3FTD861W | 16,10 | 98 |
| 16 | MEDION-MD 37496 | 15,54 | Miele-G 5611 SCI ACTIVE | 16,08 | 7 |
| 17 | MEDION-MD 37496 | 15,54 | THOMSON-TDW10BWH | 16,03 | 73 |
| 18 | LG-DB597TXS | 15,47 | THOMSON-TDW10BSL | 16,03 | 72 |
| 19 | BOSCH-SMU4ECS28E | 15,45 | Miele-G 7650 SCVI AUTODOS | 15,97 | 12 |
| 20 | HISENSE-HV673A65 | 15,35 | Miele-G 5812 SCI ACTIVE PLUS | 15,96 | 27 |

Écart de rang (points de centile, |V2 - V1|) : médiane 2,6, p90 5,8, plus de 20 points pour 0 produits.

Les 20 plus gros écarts de rang (centile : part de la cohorte notée que le produit dépasse ; effets en points sur 20, rapportés à l'effet moyen de la cohorte) :

| GTIN | Produit | V1 | Centile V1 | V2 | Centile V2 | C | Critères qui expliquent l'écart |
|---|---|---:|---:|---:|---:|---:|---|
| 8690842472282 | BEKO-BDFS26020XQ 7637902335 | 11,65 | 57,6 | 9,68 | 46,1 | 0,86 | REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] -0,43; WEIGHT [redistribution] +0,21 |
| 8690842466847 | BEKO-BDIN38645D 7628502677 | 11,53 | 55,5 | 9,58 | 44,2 | 0,86 | REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] -0,43; ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] -0,26 |
| 8690842470608 | BEKO-BDIN38644D 7635302677 | 11,53 | 55,4 | 9,58 | 44,1 | 0,86 | REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] -0,43; ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] -0,26 |
| 3701693515402 | WHIRLPOOL-W0ITP61LSW | 14,13 | 83,2 | 15,71 | 94,4 | 0,72 | ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] +3,07; WARRANTY [redistribution (moyenne imputée retirée)] -1,23; REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50 |
| 8059019060682 | CANDY-CELDIMN 4S622PSE | 9,90 | 29,2 | 9,19 | 40,2 | 0,98 | REPAIRABILITY_INDEX [redistribution] +1,31; ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] -0,62; WATER_CONSUMPTION [redistribution] -0,18 |
| 8690842526794 | BEKO-BDFN36641XD 7662000777 | 11,47 | 53,9 | 9,49 | 43,0 | 0,86 | REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] -0,43; ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] -0,26 |
| 3701763602391 | SCHNEIDER-SCLVI1542GB | 11,27 | 50,0 | 10,87 | 60,7 | 0,82 | ENERGY_CONSUMPTION_100_CYCLES [redistribution] +0,86; REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] +0,37 |
| 8690842475368 | BEKO-BDIS38040A 7641103935 | 14,73 | 91,9 | 13,97 | 81,3 | 1,00 | REPAIRABILITY_INDEX [redistribution] +1,43; DATA_QUALITY [DATA_QUALITY retiré] -0,69; WATER_CONSUMPTION [bornes robustes] -0,30 |
| 3660767941304 | BRANDT-DFC6519S -02 | 13,28 | 75,4 | 14,74 | 85,8 | 0,72 | ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] +2,28; WATER_CONSUMPTION [redistribution] +1,60; WARRANTY [redistribution (moyenne imputée retirée)] -1,23 |
| 698147432947 | LINARIE-LFDW1641BFSB | 11,32 | 51,2 | 10,93 | 61,0 | 0,82 | ENERGY_CONSUMPTION_100_CYCLES [redistribution] +0,86; REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] +0,37 |
| 698147432985 | LINARIE-LFDI1641BBI | 11,32 | 51,3 | 10,93 | 61,0 | 0,82 | ENERGY_CONSUMPTION_100_CYCLES [redistribution] +0,86; REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] +0,37 |
| 7332543595891 | ELECTROLUX-ESL2500RO 911026022 | 13,36 | 76,1 | 14,71 | 85,7 | 0,74 | ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] +2,03; WATER_CONSUMPTION [redistribution] +1,45; WARRANTY [redistribution (moyenne imputée retirée)] -1,23 |
| 4974019182438 | SHARP-QW-I1GI47EX-FR | 9,43 | 24,6 | 8,43 | 33,7 | 0,99 | REPAIRABILITY_INDEX [redistribution] +1,34; ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] -0,66; WATER_CONSUMPTION [redistribution] -0,18 |
| 8690842242441 | BEKO-DEN4842421W 7678867377 | 8,67 | 17,5 | 5,28 | 8,6 | 0,83 | ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] -1,58; REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; WEIGHT [redistribution (moyenne imputée retirée)] -0,26 |
| 8690842683145 | BEKO-BDFN26446G 7622997377 | 11,54 | 55,9 | 9,71 | 47,0 | 0,86 | REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] -0,43; WEIGHT [redistribution] +0,19 |
| 8690842552038 | BEKO-BDFN38641XC 7671003977 | 12,92 | 72,2 | 11,37 | 63,3 | 0,86 | REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] -0,43; WATER_CONSUMPTION [bornes robustes] +0,43 |
| 8690842804830 | BEKO-BDIN36461 7657093977 | 14,80 | 92,4 | 14,41 | 83,5 | 1,00 | REPAIRABILITY_INDEX [redistribution] +1,36; DATA_QUALITY [DATA_QUALITY retiré] -0,69; ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] +0,34 |
| 3660767995796 | BRANDT-BDF3024DW -01 | 14,78 | 92,3 | 14,39 | 83,4 | 1,00 | REPAIRABILITY_INDEX [redistribution] +1,43; DATA_QUALITY [DATA_QUALITY retiré] -0,43; WATER_CONSUMPTION [bornes robustes] -0,32 |
| 8690842688447 | BEKO-BDFN26446WA 7623097377 | 11,54 | 55,7 | 9,71 | 46,9 | 0,86 | REPAIRABILITY_INDEX [redistribution (moyenne imputée retirée)] -0,50; DATA_QUALITY [DATA_QUALITY retiré] -0,43; WEIGHT [redistribution] +0,19 |
| 8059019093208 | HAIER-XF 4A4M4PB | 15,13 | 96,4 | 14,96 | 87,8 | 1,00 | REPAIRABILITY_INDEX [redistribution] +1,45; DATA_QUALITY [DATA_QUALITY retiré] -0,43; ENERGY_CONSUMPTION_100_CYCLES [bornes robustes] +0,34 |

### Listes et sitemap

| Pages catégories : fiches ajoutées | dont notées V2 | Notés V1 qui perdent leur note (restent listés) | Sitemap : fiches ajoutées | Sitemap : fiches qui sortent | dont sans offre récente, ECOSCORE hérité | dont non notées V2, offre ancienne |
|---:|---:|---:|---:|---:|---:|---:|
| 327 | 65 | 11 | 323 | 736 | 736 | 0 |

## Lave-linge (`washing-machine`)

Seuil V2 0,50, filtre EPREL gardé.

### Effectifs

| Notés V1 | Cohorte V2 | Notés V2 | Non noté : couverture faible | Non noté : aucun critère réel | Entrants (filtre EPREL levé) | dont notés | Notés V1 passés en Non noté |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 1480 | 1480 | 1246 | 224 | 10 | 0 | 0 | 234 (15,8 %) |

**Garde-fou dépassé** : 15,8 % des notés V1 passent en Non noté (plus de 10 %). Seuil qui le ramènerait sous 10 % : 0,40 (au lieu de 0,50).

### Distribution

| Version | p5 | p50 | p95 | p95 - p5 |
|---|---:|---:|---:|---:|
| V1 | 9,03 | 11,52 | 12,78 | 3,75 |
| V2 | 8,51 | 13,64 | 16,15 | 7,63 |

Libellés V2 : excellent 125 (10,0 %), good 498 (40,0 %), fair 498 (40,0 %), poor 125 (10,0 %).

### Classements

Spearman V1/V2 sur les 1246 produits notés dans les deux versions : **0,874** (ECOSCORE stocké contre V2 : 0,870).

Top 20 : 5 produits communs. Place en V2 des produits du top 20 V1 :
92, 325, 10, 1026, 22, 39, 22, 22, 4, 50, 105, 149, 64, 19, 2, 423, 153, 25, 25, 11.

| # | Top 20 V1 | V1 | Top 20 V2 | V2 | Place V1 |
|---:|---|---:|---|---:|---:|
| 1 | LG-F4X7011TWB | 15,50 | ASKO-W7124XXLW | 17,70 | 47 |
| 2 | LG-F4X7511TWB | 15,14 | ASKO-W7096XG | 17,67 | 15 |
| 3 | HAIER-HW50-BP12307 | 14,54 | HISENSE-WF3S8043BB3 | 17,47 | 30 |
| 4 | LG-F94V41WH | 14,50 | ASPES-AL8202AIDV | 17,43 | 9 |
| 5 | CANDY-CW50-BP12307G-S | 13,76 | ASKO-W3096CW | 17,21 | 52 |
| 6 | HISENSE-WF3S9043BB3 | 13,68 | ASKO-W5096RG | 17,21 | 51 |
| 7 | CANDY-CW50-BP12307U1-S | 13,67 | CURTISS-SLF6010AW | 17,16 | 94 |
| 8 | CANDY-CW50-BP12307WU-S | 13,67 | OCEANIC-OCEALL610WA | 17,16 | 93 |
| 9 | ASPES-AL8202AIDV | 13,63 | ASKO-W1094W | 17,16 | 57 |
| 10 | LG-F14X71WHST | 13,40 | HAIER-HW50-BP12307 | 17,15 | 3 |
| 11 | GRUNDIG-GW7P79419W 7178543800 | 13,35 | CANDY-EY 26SB7-S | 17,15 | 20 |
| 12 | HISENSE-WF3S1043BW3 | 13,31 | HISENSE-WF5I8043BWF | 17,11 | 58 |
| 13 | LG-F14X76BMST | 13,26 | HAIER-HW90-B14367GU1 | 17,05 | 85 |
| 14 | CANDY-GD 06SS6-S | 13,25 | HAIER-HW90-BD14387U1 | 17,03 | 90 |
| 15 | ASKO-W7096XG | 13,22 | HAIER-HW90-B14367U1 | 17,03 | 89 |
| 16 | SAMSUNG-WW80CGC04DAH | 13,22 | HAIER-HW90-B14367U1 | 17,03 | 88 |
| 17 | CANDY-TCAS 274TM5-S | 13,21 | CANDY-BP 47SBL8-S | 17,00 | 38 |
| 18 | CANDY-BR 26SSB5-S | 13,21 | HOOVER-H5W 48SBL10GUH-S | 16,99 | 103 |
| 19 | CANDY-BR 26SSB6G-S | 13,21 | CANDY-GD 06SS6-S | 16,98 | 14 |
| 20 | CANDY-EY 26SB7-S | 13,20 | CANDY-GD 27SB7-S | 16,97 | 98 |

Écart de rang (points de centile, |V2 - V1|) : médiane 10,2, p90 24,6, plus de 20 points pour 221 produits.

Les 20 plus gros écarts de rang (centile : part de la cohorte notée que le produit dépasse ; effets en points sur 20, rapportés à l'effet moyen de la cohorte) :

| GTIN | Produit | V1 | Centile V1 | V2 | Centile V2 | C | Critères qui expliquent l'écart |
|---|---|---:|---:|---:|---:|---:|---|
| 8806091554833 | LG-F94V41WH | 14,50 | 99,8 | 11,45 | 17,7 | 0,86 | POWER_CONSUMPTION_TYPICAL [kWh/100 cycles (porte n° 2)] -3,13; ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] -0,65; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,62 |
| 8690842701047 | BEKO-B5WFT31145W | 12,36 | 84,5 | 12,70 | 33,7 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,78; WATER_CONSUMPTION [bornes robustes] -0,73; DATA_QUALITY [DATA_QUALITY retiré] -0,70 |
| 8690842600470 | BEKO-B3WFT51140W | 12,37 | 85,1 | 12,71 | 34,5 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,78; WATER_CONSUMPTION [bornes robustes] -0,73; DATA_QUALITY [DATA_QUALITY retiré] -0,70 |
| 8806084115362 | LG-F214WT8GRBE | 12,35 | 84,3 | 12,76 | 35,9 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,72; WATER_CONSUMPTION [bornes robustes] -0,89; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,79 |
| 8806091690708 | LG-F94V31WHSA | 12,17 | 77,1 | 12,50 | 29,8 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,00; ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] -1,03; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,79 |
| 8806091420596 | LG-F94V52IXA | 12,17 | 77,1 | 12,50 | 29,8 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,00; ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] -1,03; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,79 |
| 8806091340177 | LG-F94V51WHSA | 12,17 | 77,0 | 12,50 | 29,8 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,00; ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] -1,03; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,79 |
| 8806098740598 | LG-F14V50WHS | 11,89 | 66,1 | 11,78 | 20,5 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,00; ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] -1,31; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,79 |
| 8059019093697 | HAIER-THASN2856TM4-S | 12,12 | 75,1 | 12,54 | 30,4 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,45; ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] -0,83; CLASSE_ENERGY [redistribution] -0,54 |
| 8806096122426 | LG-F214WT8PB | 12,08 | 73,4 | 12,45 | 28,9 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,72; WATER_CONSUMPTION [bornes robustes] -0,89; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,79 |
| 8806092192409 | SAMSUNG-WW12T504DTW | 12,11 | 74,6 | 12,55 | 30,6 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,00; WATER_CONSUMPTION [bornes robustes] -0,89; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,79 |
| 8806094597844 | SAMSUNG-WW11BB744DGW | 12,33 | 83,6 | 13,04 | 39,8 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,97; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,74; WATER_CONSUMPTION [bornes robustes] -0,73 |
| 4242003923276 | SIEMENS-WI14W549FF | 11,94 | 68,6 | 12,28 | 26,6 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,83; ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] -1,41; CLASSE_ENERGY [redistribution] -0,62 |
| 7333394112206 | ELECTROLUX-EW6FI4112RA 914932082 | 12,30 | 82,8 | 13,16 | 41,3 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,70; WATER_CONSUMPTION [bornes robustes] -0,62; DATA_QUALITY [DATA_QUALITY retiré] -0,46 |
| 8690842580482 | BEKO-WUV8011XWW 7000940019 | 11,92 | 67,6 | 12,28 | 26,7 | 0,86 | ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] -1,09; WATER_CONSUMPTION [bornes robustes] +0,72; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,62 |
| 8806096053522 | LG-F94B15MBS | 12,95 | 96,7 | 13,90 | 56,9 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,91; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,79; WATER_CONSUMPTION [bornes robustes] -0,51 |
| 7333394123325 | ELECTROLUX-EW8FI5114OV 914475307 | 11,51 | 48,9 | 15,50 | 88,6 | 0,83 | ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] +2,60; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,62; CLASSE_ENERGY [redistribution] +0,38 |
| 8003437067655 | WHIRLPOOL-W0M011WADSFR SENSE WASH | 11,32 | 43,1 | 15,14 | 82,7 | 0,83 | ENERGY_CONSUMPTION_100_CYCLES [kWh/100 cycles (porte n° 2)] +2,60; REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,62; CLASSE_ENERGY [redistribution] +0,38 |
| 8690842666025 | BEKO-BM3WFU49411B 7004840035 | 12,92 | 96,3 | 13,91 | 56,9 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +1,78; DATA_QUALITY [DATA_QUALITY retiré] -0,70; WATER_CONSUMPTION [bornes robustes] -0,46 |
| 8806095539669 | SAMSUNG-WW11DG5B25TH | 12,38 | 85,7 | 13,55 | 48,7 | 1,00 | REPAIRABILITY_OR_DURABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] +2,00; REPAIRABILITY_INDEX [réparabilité ou durabilité (GOU-2041)] -0,79; WATER_CONSUMPTION [bornes robustes] -0,73 |

### Listes et sitemap

| Pages catégories : fiches ajoutées | dont notées V2 | Notés V1 qui perdent leur note (restent listés) | Sitemap : fiches ajoutées | Sitemap : fiches qui sortent | dont sans offre récente, ECOSCORE hérité | dont non notées V2, offre ancienne |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 0 | 234 | 0 | 1576 | 1576 | 0 |

## Réfrigérateurs (`refrigerator`)

Seuil V2 0,50, filtre EPREL levé.

### Effectifs

| Notés V1 | Cohorte V2 | Notés V2 | Non noté : couverture faible | Non noté : aucun critère réel | Entrants (filtre EPREL levé) | dont notés | Notés V1 passés en Non noté |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 2354 | 2488 | 2441 | 46 | 1 | 134 | 133 | 46 (2,0 %) |

### Distribution

| Version | p5 | p50 | p95 | p95 - p5 |
|---|---:|---:|---:|---:|
| V1 | 7,41 | 9,34 | 13,02 | 5,62 |
| V2 | 5,90 | 8,65 | 14,90 | 9,00 |

Libellés V2 : excellent 245 (10,0 %), good 967 (39,6 %), fair 985 (40,4 %), poor 244 (10,0 %).

### Classements

Spearman V1/V2 sur les 2308 produits notés dans les deux versions : **0,982** (ECOSCORE stocké contre V2 : 0,981).

Top 20 : 3 produits communs. Place en V2 des produits du top 20 V1 :
30, 13, 13, 15, 21, 22, 23, 24, 24, 26, 28, 29, 31, 35, 32, 33, 34, 36, 37, 38.

| # | Top 20 V1 | V1 | Top 20 V2 | V2 | Place V1 |
|---:|---|---:|---|---:|---:|
| 1 | BEKO-B7RCNE408HXBR 7282343101 | 17,60 | CASSELIN-CAP546LT | 20,00 | 73 |
| 2 | THOMSON-CTH378NFABIX | 17,02 | BARTSCHER KUHLSCHRANK 700L GN210-700804 | 20,00 | 72 |
| 3 | SIEMENS-KG39NAIAT | 17,02 | POLAR-CD080 | 20,00 | 71 |
| 4 | BOSCH-KGN492IAF | 16,87 | POLAR-UA105 | 20,00 | 70 |
| 5 | BOSCH-KGN392LAG | 16,82 | TEFCOLD-UF200V-P | 20,00 | 69 |
| 6 | SMEG-MC21XDNMA | 16,81 | ISOTECH-TPF2GLIR290 | 20,00 | 75 |
| 7 | SIEMENS-KG39NAIAU | 16,81 | TEFCOLD-UF200VS-P | 20,00 | 74 |
| 8 | LG-GBBS726AMB | 16,81 | ATOSA-PBF40RSGR | 20,00 | 68 |
| 9 | LG-GBBS726AEV | 16,81 | ATOSA-PBF80RSGR | 20,00 | 67 |
| 10 | BOSCH-KGN39AIAU | 16,80 | POLAR-FA442 | 20,00 | 46 |
| 11 | SAMSUNG-RB38C7AGWS9 | 16,78 | POLAR-GL015 | 20,00 | 45 |
| 12 | LG-GBBW726AEV | 16,77 | Frigelux-RE2T136A | 18,48 | 42 |
| 13 | LIEBHERR-RBA30 425I_091624051 | 16,74 | THOMSON-CTH378NFABIX | 17,67 | 3 |
| 14 | WHIRLPOOL-WHK 25404 XP8E | 16,66 | SIEMENS-KG39NAIAT | 17,67 | 2 |
| 15 | LIEBHERR-KGNSF 57VA03_091706651 | 16,65 | BOSCH-KGN492IAF | 17,50 | 4 |
| 16 | LIEBHERR-CBNBDA 572I_994909351 | 16,65 | LIEBHERR-CBNBSA20 575I_091904051 | 17,44 | 39 |
| 17 | LIEBHERR-CBNSDA 572I_994909451 | 16,65 | SIEMENS-KG49NVIAG | 17,44 | 38 |
| 18 | LG-GBV7280AMB | 16,65 | BOSCH-KGN392I1F | 17,44 | 37 |
| 19 | LIEBHERR-CNSDA 5723_994896251 | 16,64 | BOSCH-KGN49OCAF | 17,44 | 36 |
| 20 | SAMSUNG-RB38C607AS9 | 16,63 | LG-GBBS322APY | 17,44 | 35 |

Écart de rang (points de centile, |V2 - V1|) : médiane 1,8, p90 4,6, plus de 20 points pour 71 produits.

Les 20 plus gros écarts de rang (centile : part de la cohorte notée que le produit dépasse ; effets en points sur 20, rapportés à l'effet moyen de la cohorte) :

| GTIN | Produit | V1 | Centile V1 | V2 | Centile V2 | C | Critères qui expliquent l'écart |
|---|---|---:|---:|---:|---:|---:|---|
| 8690842752209 | BEKO-B3RCNE415HXB | 9,77 | 59,4 | 8,28 | 26,8 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] -0,22 |
| 8690842796357 | BEKO-B5BCNA325HS 7525520002 | 9,74 | 58,6 | 8,24 | 26,1 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] -0,22 |
| 3541362506177 | Frigelux-R4A218BD | 9,38 | 53,8 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362506139 | Frigelux-RC262XD | 9,38 | 53,7 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362506078 | Frigelux-RDP96BD | 9,38 | 53,7 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362505217 | Frigelux-R4A218ND | 9,38 | 53,6 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504272 | Frigelux-RANF445XD | 9,38 | 53,6 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504265 | Frigelux-RANF445BD | 9,38 | 53,5 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504258 | Frigelux-CBNF330RRD | 9,38 | 53,5 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504241 | Frigelux-CBNF330RND | 9,38 | 53,5 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504234 | Frigelux-CBNF330RCD | 9,38 | 53,4 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504227 | Frigelux-CB255RCD | 9,38 | 53,4 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504210 | Frigelux-CB255RRD | 9,38 | 53,3 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 7333394086385 | ELECTROLUX-ENT6MD18S4 925555035 | 9,42 | 55,0 | 8,36 | 29,4 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504203 | Frigelux-CB255RND | 9,38 | 53,3 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504180 | Frigelux-RFDP246RRD | 9,38 | 53,3 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504173 | Frigelux-RFDP246RND | 9,38 | 53,2 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504166 | Frigelux-RFDP246RCD | 9,38 | 53,2 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 6954903820736 | COMFEE-RCB310IX3EU(E | 9,42 | 55,0 | 8,37 | 29,6 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |
| 3541362504159 | Frigelux-RF218RRD | 9,38 | 53,1 | 8,33 | 27,7 | 0,91 | WEIGHT [redistribution (moyenne imputée retirée)] -0,97; CLASSE_ENERGY [redistribution] +0,34; DATA_QUALITY [DATA_QUALITY retiré] +0,21 |

### Listes et sitemap

| Pages catégories : fiches ajoutées | dont notées V2 | Notés V1 qui perdent leur note (restent listés) | Sitemap : fiches ajoutées | Sitemap : fiches qui sortent | dont sans offre récente, ECOSCORE hérité | dont non notées V2, offre ancienne |
|---:|---:|---:|---:|---:|---:|---:|
| 134 | 133 | 46 | 134 | 1625 | 1625 | 0 |

## Climatiseurs (`air-conditioner`)

Seuil V2 0,50, filtre EPREL gardé.

### Effectifs

| Notés V1 | Cohorte V2 | Notés V2 | Non noté : couverture faible | Non noté : aucun critère réel | Entrants (filtre EPREL levé) | dont notés | Notés V1 passés en Non noté |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 81 | 81 | 81 | 0 | 0 | 0 | 0 | 0 (0,0 %) |

### Distribution

| Version | p5 | p50 | p95 | p95 - p5 |
|---|---:|---:|---:|---:|
| V1 | 11,39 | 12,53 | 13,78 | 2,39 |
| V2 | 11,63 | 15,94 | 18,78 | 7,15 |

Libellés V2 : aucun 81 (100,0 %).

### Classements

Spearman V1/V2 sur les 81 produits notés dans les deux versions : **0,872** (ECOSCORE stocké contre V2 : 0,020).

Top 20 : 20 produits communs. Place en V2 des produits du top 20 V1 :
1, 1, 1, 1, 1, 1, 1, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8.

| # | Top 20 V1 | V1 | Top 20 V2 | V2 | Place V1 |
|---:|---|---:|---|---:|---:|
| 1 | MITSUBISHI ELECTRIC-MSZ-EF25VGKB / MUZ-EF25VGH | 13,78 | MITSUBISHI ELECTRIC-MSZ-EF25VGKB / MUZ-EF25VGH | 18,78 | 7 |
| 2 | MITSUBISHI ELECTRIC-MSZ-EF35VGKB / MUZ-EF35VGH | 13,78 | MITSUBISHI ELECTRIC-MSZ-EF35VGKB / MUZ-EF35VGH | 18,78 | 6 |
| 3 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | 13,78 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | 18,78 | 5 |
| 4 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | 13,78 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | 18,78 | 4 |
| 5 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | 13,78 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | 18,78 | 3 |
| 6 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | 13,78 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | 18,78 | 2 |
| 7 | MITSUBISHI ELECTRIC-MXZ-2F53VF-E1 / MSZ-LN18VG + MSZ-LN35VG | 13,78 | MITSUBISHI ELECTRIC-MXZ-2F53VF-E1 / MSZ-LN18VG + MSZ-LN35VG | 18,78 | 1 |
| 8 | SAUNIER DUVAL-SDH20-025NW | 13,66 | SAUNIER DUVAL-SDH20-025NW | 18,50 | 21 |
| 9 | SAUNIER DUVAL-SDH20-035NW | 13,66 | SAUNIER DUVAL-SDH20-035NW | 18,50 | 20 |
| 10 | SAUNIER DUVAL-SDH20-025NW | 13,66 | SAUNIER DUVAL-SDH20-025NW | 18,50 | 19 |
| 11 | SAUNIER DUVAL-SDH20-035NW | 13,66 | SAUNIER DUVAL-SDH20-035NW | 18,50 | 18 |
| 12 | TOSHIBA CARRIER CORPORATION-RAS-10J2AVSG-E1+RAS-B10G3KVSG-E | 13,66 | TOSHIBA CARRIER CORPORATION-RAS-10J2AVSG-E1+RAS-B10G3KVSG-E | 18,50 | 17 |
| 13 | HAIER-AS35PBPHRA-PRE / 1U35MEPFRA-PRE | 13,66 | HAIER-AS35PBPHRA-PRE / 1U35MEPFRA-PRE | 18,50 | 22 |
| 14 | HAIER-AS25S2SF1FA-BH / 1U25S2SM1FA | 13,66 | HAIER-AS25S2SF1FA-BH / 1U25S2SM1FA | 18,50 | 16 |
| 15 | DAIKIN-FTXZ35NV1B / RXZ35NV1B | 13,66 | DAIKIN-FTXZ35NV1B / RXZ35NV1B | 18,50 | 15 |
| 16 | DAIKIN-FTXZ25NV1B / RXZ25NV1B | 13,66 | DAIKIN-FTXZ25NV1B / RXZ25NV1B | 18,50 | 14 |
| 17 | PANASONIC-CS-Z35XKEW / CU-Z35XKE | 13,66 | PANASONIC-CS-Z35XKEW / CU-Z35XKE | 18,50 | 13 |
| 18 | TOSHIBA CARRIER CORPORATION-RAS-10J2AVSG-E1+RAS-B10N4KVRG-E | 13,66 | TOSHIBA CARRIER CORPORATION-RAS-10J2AVSG-E1+RAS-B10N4KVRG-E | 18,50 | 12 |
| 19 | DAIKIN-FTXZ50NV1B / RXZ50NV1B | 13,66 | DAIKIN-FTXZ50NV1B / RXZ50NV1B | 18,50 | 11 |
| 20 | TOSHIBA CARRIER CORPORATION-RAS-13J2AVSG-E1+RAS-B13N4KVRG-E | 13,66 | TOSHIBA CARRIER CORPORATION-RAS-13J2AVSG-E1+RAS-B13N4KVRG-E | 18,50 | 10 |

Écart de rang (points de centile, |V2 - V1|) : médiane 6,2, p90 33,8, plus de 20 points pour 19 produits.

Les 20 plus gros écarts de rang (centile : part de la cohorte notée que le produit dépasse ; effets en points sur 20, rapportés à l'effet moyen de la cohorte) :

| GTIN | Produit | V1 | Centile V1 | V2 | Centile V2 | C | Critères qui expliquent l'écart |
|---|---|---:|---:|---:|---:|---:|---|
| 3700845454484 | TRISTAR-AC-5474 | 13,55 | 72,5 | 14,78 | 15,0 | 0,87 | CLASSE_ENERGY [redistribution] -5,34; POWER_CONSUMPTION_TYPICAL [redistribution] +4,15; REFRIGERANT_GWP [redistribution] -0,87 |
| 3701410402015 | OPTIMEA-OAC-270-SDIN2 | 13,21 | 71,2 | 14,92 | 17,5 | 0,82 | CLASSE_ENERGY [redistribution] -4,59; POWER_CONSUMPTION_TYPICAL [redistribution] +4,28; REFRIGERANT_GWP [redistribution] -0,99 |
| 3760401210701 | CHAUFFAGE FRANCAIS GROUPE SANNOVER-FRESHO12HP | 13,19 | 70,0 | 14,90 | 16,2 | 0,82 | CLASSE_ENERGY [redistribution] -4,59; POWER_CONSUMPTION_TYPICAL [redistribution] +4,28; REFRIGERANT_GWP [redistribution] -1,00 |
| 8432884626601 | DAITSU-APD07FXA | 12,73 | 66,2 | 13,68 | 12,5 | 0,87 | CLASSE_ENERGY [redistribution] -5,34; POWER_CONSUMPTION_TYPICAL [redistribution] +3,70; REFRIGERANT_GWP [redistribution] -0,87 |
| 8431312260509 | MIDEA-MMCS-12HRN8-QRD0 | 12,84 | 67,5 | 15,11 | 18,8 | 0,60 | WARRANTY [redistribution] +1,35; CLASSE_ENERGY [redistribution] -1,19; POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67 |
| 8851492248102 | MITSUBISHI ELECTRIC-MXZ-3HA50VF | 12,53 | 57,5 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8851492247105 | MITSUBISHI ELECTRIC-MXZ-2HA40VF2 / MSZ-HR25VF + MSZ-HR25VF | 12,53 | 56,2 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8806096366851 | LG-S18EC NSKS / S18EC UL2S | 12,53 | 55,0 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8445946012113 | ASPES-AAAS1800W | 12,53 | 53,8 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8059657018533 | MITSUBISHI ELECTRIC-MXZ-2F33VF4 / MSZ-AY15VGK + MSZ-LN18VG2 | 12,53 | 52,5 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8059657010575 | MITSUBISHI ELECTRIC-MXZ-2HA50VF2 / MSZ-HR25VF + MSZ-HR25VF | 12,53 | 51,2 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8059657010537 | MITSUBISHI ELECTRIC-MXZ-2HA50VF2 / MSZ-HR25VF + MSZ-HR25VF | 12,53 | 50,0 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8059657010520 | MITSUBISHI ELECTRIC-MXZ-2HA40VF2 / MSZ-HR25VF + MSZ-HR25VF | 12,53 | 48,8 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8059657010506 | MITSUBISHI ELECTRIC-MXZ-2HA40VF2 / MSZ-HR25VF + MSZ-HR25VF | 12,53 | 47,5 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8059657008992 | DAIKIN-ATXF25A5V1B / ATXF35A5V1B / 2AMXF40A2V1B | 12,53 | 46,2 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 8059657005700 | TOSHIBA CARRIER CORPORATION-RAS-16J2AVSG-E1+RAS-B16N4KVRG-E | 12,53 | 45,0 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 6946087393434 | HISENSE-CA35YR03G / CA35YR03W | 12,53 | 43,8 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 6926597712253 | HISENSE-ACT52UR4RCC8 / AUW52U4RS7 | 12,53 | 42,5 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 6926597710174 | HISENSE-2AMW42U4RGC | 12,53 | 41,2 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |
| 6924362762748 | HAIER-AS25PBAHRA/1U25YEGFRA | 12,53 | 40,0 | 15,94 | 20,0 | 0,52 | POWER_CONSUMPTION_TYPICAL [redistribution (moyenne imputée retirée)] -0,67; CLASSE_ENERGY [redistribution] +0,60; REFRIGERANT_GWP [redistribution] +0,16 |

### Listes et sitemap

| Pages catégories : fiches ajoutées | dont notées V2 | Notés V1 qui perdent leur note (restent listés) | Sitemap : fiches ajoutées | Sitemap : fiches qui sortent | dont sans offre récente, ECOSCORE hérité | dont non notées V2, offre ancienne |
|---:|---:|---:|---:|---:|---:|---:|
| 0 | 0 | 0 | 0 | 154 | 154 | 0 |

## Cas de l'audit

### TV 27 pouces, classes E (SDR) et G (HDR)

| GTIN | Produit | ECOSCORE stocké | V1 | Centile V1 | V2 | Centile V2 | W/dm² | Statut V2 |
|---|---|---:|---:|---:|---:|---:|---:|---|
| 8806097238249 | SAMSUNG-TU27F6005FK | 14,45 | 14,45 | 98,5 | 9,25 | 53,4 | 1,06 | RATED |
| 8435693109112 | MARS GAMING-MV-27C | 13,81 | 13,82 | 89,4 | 8,83 | 45,0 | 1,04 | RATED |
| 3701693516270 | LG-27ART10AKPL | 14,03 | 14,04 | 94,3 | 8,57 | 37,3 | 1,07 | RATED |
| 4038986183239 | AOC-27G4ZRE | 14,64 | 14,65 | 99,3 | 10,05 | 72,4 | 1,04 | RATED |
| 4719331854317 | GIGABYTE-GS27FC | 14,41 | 14,41 | 98,4 | 9,94 | 69,4 | 1,04 | RATED |
| 8435693109129 | MARS GAMING-MV-27CW | 13,81 | 13,82 | 89,5 | 8,83 | 45,0 | 1,04 | RATED |
| 8806099054687 | SAMSUNG-TU27F6005FK | 14,12 | 14,12 | 96,1 | 8,32 | 30,4 | 1,06 | RATED |
| 8806097241447 | SAMSUNG-UE27F6002FK | 14,12 | 14,12 | 96,1 | 8,32 | 30,4 | 1,06 | RATED |
| 4719331852795 | GIGABYTE-GS27F | 14,41 | 14,41 | 98,4 | 9,94 | 69,4 | 1,04 | RATED |
| 8806084632395 | LG-27ART10AKPL | 14,61 | 14,61 | 99,2 | 9,87 | 67,6 | 1,07 | RATED |

Effet taille sur les 2037 TV notées dans les deux versions : Spearman note/diagonale -0,59 en V1, 0,36 en V2 ; diagonale médiane du top 5 % 32 pouces en V1, 75 en V2 (cohorte : 55).

### Smartphones à réparabilité imputée

828 des 952 notés V1 ont un indice de réparabilité imputé (moyenne, poids 0,50), 124 un indice réel. En V2, 828 des 828 ont une classe de réparabilité UE réelle ; 828 sont notés.

- Indice imputé : V1 médiane 12,56, V2 médiane 13,34 ; centile médian V1 44,6, V2 49,5.
- Indice réel : V1 médiane 14,51, V2 médiane 11,10 ; centile médian V1 91,6, V2 23,3.

### Climatiseurs

81 notés V1, 81 notés V2, 0 passent en Non noté. 60 ECOSCORE V1 changent déjà avec l'échelle A+++ à D (GOU-2137, V1 aussi). Aucun libellé de position (moins de 100 notés).

Spearman note V2/couverture C : -0,70 (C médiane 0,52).

| GTIN | Produit | Classe | ECOSCORE stocké | V1 (main) | V2 | Place V2 | C | Statut V2 |
|---|---|---|---:|---:|---:|---:|---:|---|
| 8059657002297 | MITSUBISHI ELECTRIC-MSZ-EF25VGKB / MUZ-EF25VGH | A+++ | 13,78 | 13,78 | 18,78 | 1 | 0,52 | RATED |
| 8059657003126 | MITSUBISHI ELECTRIC-MSZ-EF35VGKB / MUZ-EF35VGH | A+++ | 13,78 | 13,78 | 18,78 | 1 | 0,52 | RATED |
| 8851492359129 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | A+++ | 13,78 | 13,78 | 18,78 | 1 | 0,52 | RATED |
| 8059657018915 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | A+++ | 13,78 | 13,78 | 18,78 | 1 | 0,52 | RATED |
| 8851492932124 | MITSUBISHI ELECTRIC-MXZ-2F53VF-E1 / MSZ-LN18VG + MSZ-LN35VG | A+++ | 13,70 | 13,78 | 18,78 | 1 | 0,52 | RATED |
| 8059657018366 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | A+++ | 13,78 | 13,78 | 18,78 | 1 | 0,52 | RATED |
| 8059657018939 | MITSUBISHI ELECTRIC-MXZ-2F42VF-E1 / MSZ-LN18VG + MSZ-LN25VG | A+++ | 13,78 | 13,78 | 18,78 | 1 | 0,52 | RATED |
| 8059657000804 | DAIKIN-FTXZ35NV1B / RXZ35NV1B | A+++ | 13,66 | 13,66 | 18,50 | 8 | 0,52 | RATED |
| 8059657003157 | PANASONIC-CS-Z35XKEW / CU-Z35XKE | A+++ | 13,66 | 13,66 | 18,50 | 8 | 0,52 | RATED |
| 8059657002952 | DAIKIN-FTXZ25NV1B / RXZ25NV1B | A+++ | 13,66 | 13,66 | 18,50 | 8 | 0,52 | RATED |
| 8059657009715 | TOSHIBA CARRIER CORPORATION-RAS-10J2AVSG-E1+RAS-B10N4KVRG-E | A+++ | 13,66 | 13,66 | 18,50 | 8 | 0,52 | RATED |
| 3770022905146 | TOSHIBA CARRIER CORPORATION-RAS-10J2AVSG-E1+RAS-B10G3KVSG-E | A+++ | 13,66 | 13,66 | 18,50 | 8 | 0,52 | RATED |
| 3532041734394 | SAUNIER DUVAL-SDH20-025NW | A+++ | 13,66 | 13,66 | 18,50 | 8 | 0,52 | RATED |
| 7427255001829 | HAIER-AS25S2SF1FA-BH / 1U25S2SM1FA | A+++ | 13,66 | 13,66 | 18,50 | 8 | 0,52 | RATED |
| 8059657005434 | TOSHIBA CARRIER CORPORATION-RAS-10J2AVSG-E1+RAS-B10N4KVRG-E | A+++ | 13,66 | 13,66 | 18,50 | 8 | 0,52 | RATED |

## Synthèse

| Verticale | Notés V1 | Notés V2 | Notés V1 en Non noté | Entrants notés | Spearman | Top 20 communs | p50 V1 | p50 V2 | Sortent du sitemap (ECOSCORE hérité) |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| TV | 2151 | 2037 | 114 (5,3 %) | 0 | 0,218 | 1 | 13,01 | 9,10 | 1833 |
| Fours | 1446 | 1350 | 96 (6,6 %) | 0 | 0,172 | 0 | 15,15 | 13,98 | 667 |
| Smartphones | 952 | 952 | 0 (0,0 %) | 0 | 0,186 | 3 | 12,56 | 13,34 | 630 |
| Lave-vaisselle | 1011 | 1065 | 11 (1,1 %) | 65 | 0,992 | 10 | 11,27 | 9,92 | 736 |
| Lave-linge | 1480 | 1246 | 234 (15,8 %) | 0 | 0,874 | 5 | 11,52 | 13,64 | 1576 |
| Réfrigérateurs | 2354 | 2441 | 46 (2,0 %) | 133 | 0,982 | 3 | 9,34 | 8,65 | 1625 |
| Climatiseurs | 81 | 81 | 0 (0,0 %) | 0 | 0,872 | 20 | 12,53 | 15,94 | 154 |
