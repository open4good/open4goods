# Décisions de méthode de l'ImpactScore

Registre des décisions de Goulven sur la méthode, les seuils et la publication. Une ligne par décision ; le détail chiffré est dans les mesures citées. Les valeurs vivent dans les blocs `v2` de `verticals/src/main/resources/verticals/impactscores/<verticale>.yml` : ce document en donne la source.

## Porte n° 2 : seuils V2 et levée du filtre EPREL (13/11/2026)

- **Décision** : approuvée en entier, sans réserve (approbation board `c81ef147`, 13/11/2026 ; GOU-1877).
- **Mesures** : [`lot1-mesures-2026-11.md`](lot1-mesures-2026-11.md) (GOU-1875, ES prod du 10/11), [`lot1b-distribution-2026-11.md`](lot1b-distribution-2026-11.md) (GOU-1876, ES prod du 13/11), la mesure smartphones de GOU-2048 (12/11) et deux comptages ES prod du 13/11 (lave-linge, climatiseurs).
- **Portée** : matrices V2 seulement. Rien ne change en prod avant la porte n° 3 (simulation GOU-1887, publication GOU-1888) : les verticales restent en `methodologyVersion: 1`.

### Filtre EPREL et seuil de couverture

C = part du poids V2 des critères réellement renseignés (`v2.minRealWeightCoverage`, seuil inclusif). "Exclus EPREL" = fiches avec offre exclues seulement faute d'EPREL.

| Verticale | Notés (13/11) | Exclus EPREL | `v2.eprelRequired` | `v2.minRealWeightCoverage` | Fiches ajoutées (estimation) |
|---|---:|---:|---|---:|---:|
| Réfrigérateurs | 2 354 | 128 | `false` (filtre levé) | 0,50 | +128 |
| Lave-vaisselle | 1 011 | 305 | `false` (filtre levé) | 0,60 | +51 |
| Smartphones | 952 | 8 661 | `true` | 0,50 | 0 |
| Fours | 1 446 | 6 819 | `true` | 0,50 | 0 |
| TV | 2 151 | 176 | `true` | 0,50 | 0 |
| Lave-linge | 1 480 | 779 | `true` | 0,50 | 0 |
| Climatiseurs | 81 | 211 | `true` | 0,50 | 0 |

Les verticales qui gardent le filtre sont en "V2 sans levée du filtre" : leurs exclus sont surtout hors sujet (fours : petit électroménager ; smartphones : 20 % de claviers ou jumelles ; TV : aucune fiche n'atteint C = 0,50). Pour les smartphones, la voie retenue est l'appariement EPREL (GOU-2042).

**Périmètre des fiches ajoutées (décision CTO, GOU-2163, 13/11/2026, exécution du mandat de la porte n° 2 ; GOU-2165)** : quand `v2.eprelRequired` vaut `false`, une fiche sans EPREL entre seulement si sa couverture C atteint `v2.minRealWeightCoverage` (cause sinon : `missing_eprel_low_coverage`) : "fiches ajoutées" = fiches notées. TV : `v2.excludedEprelCategories: [MONITOR, PROFESSIONAL_DISPLAY, SIGNAGE]` (cause `eprel_category_<CATÉGORIE>`). Simulation du 13/11 : 65 lave-vaisselle et 133 réfrigérateurs entrent, tous notés ; 75 TV sortent. Chiffres : [`v2-simulation-2026-11-13.md`](v2-simulation-2026-11-13.md#relance-gou-2165).

**Garde-fou** : si la simulation fait passer plus de 10 % des notés actuels d'une verticale en "Non noté", le seuil de cette verticale revient à Goulven avant la porte n° 3. QA relit dans la simulation la liste des 51 lave-vaisselle entrants (modèles professionnels).

### Libellés de position

`v2.positionLabels`, sur la part de la cohorte notée que le produit dépasse (ex æquo au même rang, design § 2.4) :

| Libellé | `minBetterThan` |
|---|---:|
| "Parmi les mieux classés" | 0,90 |
| "Au-dessus de la médiane" | 0,50 |
| "En dessous de la médiane" | 0,10 |
| "Parmi les moins bien classés" | 0,00 |

Remplace les bornes provisoires 0,90 / 0,67 / 0,33 : avec 0,67, un produit mieux classé que 60 % de sa catégorie aurait été affiché "En dessous de la médiane". **Aucun libellé sous 100 produits notés** dans la verticale (climatiseurs aujourd'hui : note et position seules).

### Bornes robustes (`v2.criteriaScoring`)

- `MINMAX_QUANTILE`, centiles **0,02 et 0,98**, sur les valeurs réelles : consommation SDR et HDR des TV (rapportée à la surface, GOU-1884), énergie par cycle des fours, eau des lave-linge et des lave-vaisselle, énergie pour 100 cycles. Exemple TV SDR : 16,6 à 172,7 W au lieu de 7,4 à 360 W.
- `MINMAX_FIXED` : consommation à l'arrêt et en veille **0 à 1 W** ; veille réseau des TV **0 à 2 W** (le centile 98 y vaut la médiane).
- Climatiseurs : bornes inchangées (13 et 2 valeurs réelles).

### Critères remplacés ou retirés (V2 ; la V1 reste gelée)

| Verticale | Changement | Raison mesurée |
|---|---|---|
| Smartphones | `REPAIRABILITY_INDEX` → `EU_REPAIRABILITY_CLASS`, même poids | classe UE sur 939/939 notés, indice français sur 123 (abrogé pour les téléphones) |
| Lave-linge | `POWER_CONSUMPTION_TYPICAL` → `ENERGY_CONSUMPTION_100_CYCLES`, même poids | 19/1 480 valeurs aux unités mélangées, contre 1 169/1 480 kWh/100 cycles d'EPREL |
| Climatiseurs | `REPAIRABILITY_INDEX` retiré, poids redistribué au prorata ; `CLASSE_ENERGY` lu sur l'échelle A+++ à D | réparabilité absente de tous les documents ; A+ à A+++ ignorés aujourd'hui (70/81 imputés) et "A" noté au maximum |
| TV | `DIAGONALE_POUCES` retirée, consommation rapportée à la surface | GOU-1884 |

## Porte n° 3 : publication de la V2 (14/11/2026)

- **Décision** : approuvée sans note, donc selon les six recommandations du dossier (approbation board `5f2236b6`, 14/11/2026 ; GOU-1888).
- **Sources** : simulation sur l'export prod du 13/11 ([`v2-simulation-2026-11-13.md`](v2-simulation-2026-11-13.md), GOU-1887) relue par QA (GOU-2162) ; analyse juridique GOU-1886 ; rendu bêta de la TV en V2 (GOU-2166, page méthode corrigée et recontrôlée dans GOU-2167).
- **Portée** : chaque vague passe `methodologyVersion: 2` par sa propre PR, livrée par le train de release (GOU-1889). Retour arrière sans release : surcharge `IMPACTSCORE_METHODOLOGY_OVERRIDES=<verticale>:1` de l'environnement de prod.

### Vagues

| Vague | Verticales | Préalable |
|---|---|---|
| 1 | Réfrigérateurs, lave-vaisselle | conseil juridique (décision 6, GOU-2187) |
| 2 | Lave-linge, fours, smartphones | une semaine après la vague 1, suivi SEO sans alerte ; seuil lave-linge appliqué (GOU-2186) |
| 3 | TV, climatiseurs | bloc énergie TV et nouvelle simulation TV (GOU-2186) |

### Décisions

| # | Objet | Décision |
|---|---|---|
| 1 | Lave-linge, seuil de couverture | `v2.minRealWeightCoverage` 0,50 → **0,40** : 9,5 % des notés passent en "Non noté" au lieu de 15,8 % (garde-fou de 10 %). |
| 2 | TV, bloc énergie | **Option B** : `POWER_DENSITY_SDR` 0,08 et `POWER_CONSUMPTION_SDR` (watts bruts) 0,08 au lieu de la densité seule à 0,16 ; classes SDR 0,14 et HDR 0,04 inchangées. Avec la densité seule, 15 des 20 premiers TV font 65" ou plus. Nouvelle simulation TV avant la vague 3 ; au-delà de 10 % en "Non noté", retour à Goulven. |
| 3 | Climatiseurs | V2 au seuil 0,50, sans libellé de position (moins de 100 notés) ; la note affiche "x critères sur y". |
| 4 | Lave-vaisselle, filtre EPREL | **Gardé** (`v2.eprelRequired: true`), contrairement à la porte n° 2. Sa levée reviendra à Goulven après le tri des fiches hors périmètre (GOU-2163, GOU-2165). |
| 5 | Niveaux affichés | Option A, formulés sur la position (section "Libellés de position" ci-dessus). |
| 6 | Conseil juridique | Consultation d'un conseil externe avant la vague 1 sur les questions 1 ("label de développement durable" ?) et 4 (méthode et hypothèses communes) du § 6 de l'analyse GOU-1886 (GOU-2187). |

**Sitemap** : 7 265 fiches sans offre récente sortent du sitemap sur les sept verticales ; elles reviennent dès qu'elles ont une offre (design § 4.2). Suivi Search Console avant chaque vague puis à J+2, J+7 et J+14 ; une baisse nette des clics imputable à la bascule déclenche le retour arrière.
