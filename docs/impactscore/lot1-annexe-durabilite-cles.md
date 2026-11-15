# Annexe Lot 1 : indice de durabilité / réparabilité, clés d'attribut brutes (TV et lave-linge)

Date de la mesure : 2026-11-10. Index ES : `products-moustik` (lecture seule, environnement dev).

## Conclusion courte

- Indice de réparabilité : présent, une seule clé brute dans `attributes.all` (source `rakuten.com`) : `REPAIRABILITY INDEX` (avec espace) et, pour une partie des fiches, `REPAIRABILITY_INDEX` (avec tiret bas). Les deux alimentent `attributes.indexed.REPAIRABILITY_INDEX`. Valeurs de 0 à 10, ex. `7.7`.
- Indice de durabilité (nouvel indice français, hors réparabilité) : AUCUN trouvé. Ni clé `DURABILITY_INDEX`, `DURABILITYINDEX`, `DURABILITY`, `DURABILITE` (sondées explicitement), ni dans `attributes.features` (absent de toutes les fiches lues), ni dans les 4 fiches lues en entier. Voir limites : la recherche de noms inconnus n'est pas exhaustive.
- Clés "voisines" (durée de vie / support), à ne pas confondre avec un indice : `MINAVAILABILITYSPAREPARTSYEARS`, `MINGUARANTEEDSUPPORTYEARS`, `MINAVAILABILITYSOFTWAREUPDATESYEARS`, `MINGUARANTEEDGUARANTEE` (TV, source `eprel`), `GUARANTEEDURATION` (lave-linge, source `eprel`), `WARRANTY` (sources revendeurs et `fnac.com`).
- Du texte libre mentionne la réparabilité (`DESCRIPTIF` : "Indice de réparabilité : 7,7 /10", "Disponibilité des pièces détachées : 10 ans" ; `ATTRIBUTS` : "Repairability Index=7.5"), mais ce n'est pas une clé dédiée.

## Méthode

1. `get-mapping` préfixe `attributes` : `attributes.all` et `attributes.features` sont des objets `disabled` (lisibles seulement en `_source`). `attributes.indexed.REPAIRABILITY_INDEX` existe (`value`, `numericValue`, `source`) ; aucun `DURABILITY_INDEX`. Champs de filtre : `vertical`, `excluded`, `excludedCauses`, `offersCount`, `gtinInfos.normalizedGtin14`.
2. Forme d'un document (size 2, TV) : `attributes.all` contient 60 à 150 clés par fiche (objets `{name, value, source[]}`), environ 7 000 jetons par fiche. Lire `attributes.all` en entier sur ~600 fiches était donc impossible dans le contexte de l'agent.
3. Contraintes de l'outil `search` : le filtre `source` refuse les jokers au milieu (`attributes.all.*REPAR*`) et les noms contenant une espace ou une apostrophe (`attributes.all.REPAIRABILITY INDEX`). Contournement : lecture de `attributes.indexed.REPAIRABILITY_INDEX.source` (petit objet qui recopie le nom brut d'origine dans `source[].name`) et lecture ciblée de `attributes.all.<clé sans espace>` pour les candidats `REPAIRABILITY_INDEX`, `DURABILITY_INDEX`, `DURABILITYINDEX`, `REPAIRABILITYINDEX`, `DURABILITY`, `DURABILITE`, `REPARABILITE`, plus `attributes.features` en entier.
4. Échantillons, tri `gtinInfos.normalizedGtin14` asc :
   - A : `excluded=false`, `offersCount>0`, 150 premières fiches (lues en une page de 150 au lieu de 3 pages de 50, même ordre).
   - B : `excluded=true`, `excludedCauses=missing_eprel`, `offersCount>0`, 75 fiches à partir du GTIN `01000000000000` + 75 à partir de `05500000000000`.
5. Agrégations (comptes) : présence de `attributes.indexed.REPAIRABILITY_INDEX.value` sur la population entière et sur les bornes GTIN des échantillons A (concordance exacte avec la lecture : 30 et 26).

Effectifs lus (fiches) : TV A 150, TV B 150, lave-linge A 150, lave-linge B 150, plus 2 (TV, lecture complète), 2 (test `features`), 8 (REP TV), 2 (lave-linge, lecture complète) = environ 614 fiches (budget 700).

## Présence de `REPAIRABILITY_INDEX` (indexé), agrégation

| Verticale | Population | Total (agrégation) | Avec REPAIRABILITY_INDEX | Part |
|---|---|---|---|---|
| tv | A (non exclu, offres>0) | 1 345 | 280 | 20,8 % |
| tv | B (missing_eprel, offres>0) | 287 | 17 | 5,9 % |
| washing-machine | A | 1 466 | 307 | 20,9 % |
| washing-machine | B | 1 313 | 19 | 1,4 % |

Dans les échantillons lus (n/150) : TV A 30 (agrégation sur la plage GTIN : 30), TV B 13, lave-linge A 26 (agrégation : 26), lave-linge B 2.

## Tableau des clés brutes liées à la durabilité / réparabilité

Seuil : nom contenant durab, repar, répar, repair, indice, index, score, lifetime, longévité, pièces, spare, support, guarantee, garantie. "n/150" = fiches de l'échantillon où la clé est présente (lecture directe sauf mention).

| Clé brute | Verticale | Échantillon | n/150 | Exemple | Source |
|---|---|---|---|---|---|
| `REPAIRABILITY INDEX` (espace) | tv | A | 23 | `6.3` | rakuten.com |
| `REPAIRABILITY_INDEX` (tiret bas, présence confirmée dans `attributes.all`) | tv | A | 6 | `6.6` | rakuten.com |
| Valeur indexée sans nom brut dans la source (clé absente des chemins sondés ; nom brut inconnu) | tv | A | 1 | `4.1` (GTIN 05025232917518) | rakuten.com |
| `REPAIRABILITY INDEX` | tv | B | 4 | `6.5` | rakuten.com |
| `REPAIRABILITY_INDEX` | tv | B | 6 | `6.8` | rakuten.com |
| Valeur indexée sans nom brut | tv | B | 3 | `6` | rakuten.com |
| `REPAIRABILITY INDEX` | washing-machine | A | 15 | `7.7` | rakuten.com |
| `REPAIRABILITY_INDEX` | washing-machine | A | 11 | `7.1` | rakuten.com (une fiche aussi `123comparer.fr`) |
| `REPAIRABILITY INDEX` | washing-machine | B | 2 | `6.6` | rakuten.com |
| `REPAIRABILITY_INDEX` | washing-machine | B | 0 | - | - |
| `MINAVAILABILITYSPAREPARTSYEARS` (via valeur indexée) | tv | A | environ 124 (comptage manuel) | `7` | eprel |
| `MINGUARANTEEDSUPPORTYEARS` (via valeur indexée) | tv | A | environ 124 | `7` | eprel |
| `WARRANTY` (via valeur indexée) | tv | A | environ 131 | `2` | revendeurs, fnac.com |
| `MINAVAILABILITYSOFTWAREUPDATESYEARS` | tv | A | non compté (2/2 fiches lues en entier) | `8` | eprel |
| `MINGUARANTEEDGUARANTEE` | tv | A | non compté (2/2 fiches lues en entier) | `2` | eprel |
| `GUARANTEEDURATION` | washing-machine | A | non compté (2/2 fiches lues en entier) | `2` | eprel |
| `WARRANTY` | washing-machine | A | non compté (2/2 fiches lues en entier) | `2` | UNDERDOGFR, fnac.com |
| `ENERGYEFFICIENCYINDEX`, `WASHINGEFFICIENCYINDEX`, `WASHINGEFFICIENCYINDEXV2` | washing-machine | A | non compté (2/2) | `60` | eprel (indices énergie/lavage, pas durabilité) |
| `DURABILITY_INDEX`, `DURABILITYINDEX`, `DURABILITY`, `DURABILITE`, `REPAIRABILITYINDEX`, `REPARABILITE` | tv et washing-machine | A et B | 0 | - | sondés dans `attributes.all` sur 600 fiches |
| `attributes.features` (tout contenu) | tv et washing-machine | A et B | 0 | - | absent de toutes les fiches lues |

Notes :
- Le détail "espace" vs "tiret bas" se lit via `source[].name` de la valeur indexée (`REPAIRABILITY INDEX`) ; quand `name` est absent de la source, la clé `REPAIRABILITY_INDEX` existe dans `attributes.all` (6 TV A, 6 TV B, 11 lave-linge A) sauf pour 1 TV A et 3 TV B où aucun nom n'a pu être établi.
- Les clés `MIN*`, `WARRANTY` ne sont comptées que sur la TV A, et via les valeurs indexées (noms bruts normalisés vers `MINAVAILABILITYSPAREPARTSYEARS`, etc.) ; pour les autres cellules elles n'ont pas été relevées.
- Clés vues dans les 4 fiches lues en entier et hors liste de mots-clés : non listées ici.

## Clés brutes distinctes et plus fréquentes

Non mesurable sur 600 fiches avec cet outil (voir limites). Contexte uniquement, sur 4 fiches lues en entier : TV ~60 à ~150 clés par fiche, lave-linge ~65 à ~70. Clés présentes sur les 4 fiches : `EPREL CERTIFICATION CODE`, `TITRE`, `TRANCHE DE POIDS`, `FABRICANT`, `CLASSE D'EFFICACITE ENERGETIQUE`, `ENERGYCLASS`, `ENERGYCLASSRANGE`, `ENERGYCLASSIMAGE`, `ENERGYCLASSIMAGEWITHSCALE`, `AVAILABILITYINEUEEACOUNTRIES`, `EXPORTDATETS`, `POWEROFFMODE` (les 15 plus fréquentes réelles ne sont pas établies).

## Limites

- Échantillon, pas exhaustif : 150 fiches par cellule, ordre GTIN croissant (biais vers préfixes GTIN bas, 00-05) ; l'échantillon B TV (287 fiches au total) couvre plus de la moitié de la population.
- Les noms de clés inconnus ne peuvent pas être découverts avec l'outil : pas de joker, pas d'espace dans les chemins `source`. Un indice de durabilité nommé avec espace (ex. "INDICE DE DURABILITE") dans `attributes.all` n'aurait pas été vu, sauf s'il alimentait un attribut indexé ; seul `REPAIRABILITY_INDEX` existe dans le mapping `indexed`, et l'absence d'un `DURABILITY_INDEX` indexé est certaine, pas celle d'une clé brute non mappée.
- Le total des clés brutes distinctes et le top 15 n'ont pas été calculés.
- Les comptes `MIN*` / `WARRANTY` sont des comptages manuels sur sortie JSON, approximatifs (±2).
- Aucune écriture ES ; lecture sur un seul environnement (dev).
- Pour trancher définitivement : un export ou un script côté serveur énumérant les clés de `attributes.all` par verticale (hors outil MCP) ou un test sur les premières fiches éligibles du nouvel indice une fois publié.
