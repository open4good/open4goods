---
title: "Plan d'intégration : indice de durabilité (téléviseurs, lave-linge)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : indice de durabilité (téléviseurs, lave-linge)

> Socle commun : [README](README.md). Fiche source :
> section 4.2 de l'inventaire interne des sources candidates (non publié).
> Mesures du 2026-11-08 sur les fichiers consolidés du 2026-11-07.

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | `fr-indice-durabilite` |
| Mode | R (référentiel compact), appariement `GTIN_THEN_BRAND_MODEL` |
| Priorité | P1, première implémentation du socle (P0-5) |
| Effort | M : 4 à 6 jours, socle compris ; 1 à 2 jours pour une source R suivante |
| Verticales | `tv`, `washing-machine` |
| Volume | TV : 1 233 lignes, 1 154 modèles. Lave-linge : 1 588 lignes, 1 551 modèles |
| Dépend de | P0-2 (synonymes), P0-3 (priorité), P0-4 (attribution), P0-5 (socle) |

## 1. Apport

L'indice de durabilité remplace l'indice de réparabilité depuis le 8 janvier 2025 pour les
téléviseurs et le 8 avril 2025 pour les lave-linge (décret n° 2024-316 ; dates **non
vérifiées** sur Légifrance). Pour chaque modèle, la source fournit :

- la note globale sur 10 (`note_id`) ;
- les sous-notes de réparabilité (`note_reparabilite`) et de fiabilité (`note_fiabilite`) ;
- les notes par critère, `note_A_*` (réparabilité) et `note_B_*` (fiabilité) ;
- la disponibilité et les délais des pièces détachées (jusqu'à 12 pièces par public :
  réparateur, producteur, consommateur) ;
- l'accessibilité du compteur d'usage ;
- le lien vers le tableau de notation du fabricant.

Situation actuelle de nudger :

- `washing-machine.yml` utilise encore `REPAIRABILITY_INDEX`, devenu obsolète pour cette
  catégorie.
- `DURABILITY` est un score composite (garantie, durée de support, pièces), pas l'indice légal.
- `tv.yml` contient seulement une suggestion commentée `"INDICE DE DURABILITE"`, issue de
  `rakuten.com`, avec 15 % de couverture.

C'est une donnée réglementaire, affichée en magasin et absente de la plupart des comparateurs.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Jeu TV | data.gouv `677ce460c58a7cc3d7ea9708`, "Fichiers consolidés des données respectant le schéma indice de durabilité téléviseur" | vérifié |
| Jeu lave-linge | data.gouv `67f62e8693c5c74265692d64` | vérifié |
| Schéma | `etalab/indice-durabilite-lave-linge` v0.1.1 (équivalent TV) | vérifié |
| Format | CSV UTF-8, séparateur virgule, 198 colonnes (TV) | vérifié |
| Taille | TV 2,6 Mo, lave-linge 4,6 Mo | vérifié |
| Fréquence | `daily`, consolidation vers 12 h UTC | vérifié |
| Authentification | aucune | vérifié |
| URL du fichier | nom horodaté qui change chaque jour (par exemple `.../20261107-115732/consolidation-etalab-indice-durabilite-televiseur-v-0.1.1-20261107.csv`) ; l'identifiant de ressource, lui, est stable (mise à jour en place) | vérifié |
| Licence | `lov2` sur le jeu consolidé | vérifié (champ d'attribution seulement) |

Résolution de l'URL : URL stable `https://www.data.gouv.fr/api/1/datasets/r/{resourceId}` de
la ressource "Consolidation de la dernière version à date du schéma". Repli si la ressource
disparaît : `GET https://www.data.gouv.fr/api/1/datasets/{id}/`, puis choix par `schema.name` et
par titre. On ne fige jamais l'URL horodatée dans la configuration.

Colonnes utiles : `id_modele`, `referentiel_id_modele`, `nom_modele`,
`nom_metteur_sur_le_marche`, `date_calcul`, `url_tableau_detail_notation`, `note_id`,
`note_reparabilite`, `note_fiabilite`, `note_A_*`, `note_B_*`, `accessibilite_compteur_usage`,
`nb_annees_disponibilite_piece_detachee_*`, `delai_jours_piece_detachee_*`, `last_modified`,
`datagouv_dataset_id`, `datagouv_resource_id`, `datagouv_organization_or_owner`.

## 3. Identifiants et appariement

Mesure avec contrôle de la somme GTIN :

| Fichier | Lignes | `id_modele` numérique (8-14 chiffres) | GTIN valides | Modèles sur plusieurs lignes |
|---|---|---|---|---|
| TV | 1 233 | 539 | **529 (43 %)** | 77 |
| Lave-linge | 1 588 | 1 447 | **1 304 (82 %)** | 35 |

`referentiel_id_modele` annonce `GTIN_EAN` sur presque toutes les lignes, mais `id_modele`
contient souvent une référence commerciale (`TKW32HD26BH`, `ES_NFA814TWA`). Quand il contient
un GTIN, `nom_modele` porte généralement la référence (`K-55A65/P`), parfois préfixée
(`10017726_ED50QT01FHD-RE`).

Règles :

1. Si `id_modele` est un GTIN valide : `gtins = {id_modele}`, et `nom_modele` est ajouté aux
   `models`.
2. Sinon : `gtins = {}`, `models = {id_modele, nom_modele}`, nettoyés : espaces multiples,
   préfixe numérique suivi de `_`, suffixe `_FR`.
3. Marque : `nom_metteur_sur_le_marche` contient souvent une raison sociale ("Sony France.
   succursale de Sony. Europe B.V.", "Panasonic Marketing Europe GmbH", "HAIER FRANCE"). On
   passe par `BrandService.resolve`, complété par une table d'alias propre à la source dans le
   provider. `ELECTRO DEPOT` est un distributeur à marques propres : la marque n'est pas
   exploitable, on apparie alors sur le GTIN ou le modèle seuls.
4. Dédoublonnage : pour un même `id_modele`, on garde la ligne la plus récente
   (`date_calcul`, puis `last_modified`).
5. Projection : `ReferenceMatcher` essaie le GTIN exact, puis marque + modèle, en reprenant
   les garde-fous d'`EprelCompletionService.selectUniqueResult` (résultat unique, rejet des
   ambiguïtés, modèle contenu dans le libellé entier). Recherche inverse pour les
   enregistrements sans GTIN : produits de la verticale cible dont `model()` ou les candidats
   de modèle correspondent.

## 4. Modèle cible

Nouveaux attributs dans `verticals/src/main/resources/attributes/` :

| Clé | Colonne | Type | Score |
|---|---|---|---|
| `DURABILITY_INDEX` | `note_id` | NUMERIC, valeur 0-10 | `asScore: true`, `participateInScores: [DURABILITY]` |
| `DURABILITY_INDEX_REPAIRABILITY` | `note_reparabilite` | NUMERIC 0-10 | `participateInScores: [REPARABILITY]` |
| `DURABILITY_INDEX_RELIABILITY` | `note_fiabilite` | NUMERIC 0-10 | non |
| `USAGE_COUNTER_ACCESSIBILITY` | `accessibilite_compteur_usage` | NUMERIC 0-2 | non |
| `MINAVAILABILITYSPAREPARTSYEARS` (existant) | **minimum** des `nb_annees_disponibilite_piece_detachee_*` renseignés (durée garantie pour toutes les pièces listées) | NUMERIC | existant |

Normalisation des scores : `scoring.scale` (`ScoreScaleConfig`, défaut 0-5) est l'échelle de
**sortie** du score, commune à tous les attributs. Pour `DURABILITY_INDEX` et ses sous-notes,
on garde `scale: {min: 0, max: 5}` et on déclare
`normalization: {method: MINMAX_FIXED, params: {fixedMin: 0.0, fixedMax: 10.0}}`, comme
`CLASSE_ENERGY.yml`. `MINMAX_OBSERVED` étirerait des notes tassées (TV de 2,3 à 9,1) sur toute
l'échelle et ferait varier le score d'un même produit selon le catalogue.

Les colonnes de critères (`note_A_*`, `note_B_*`) et le détail des pièces restent dans
`ref-fr-indice-durabilite`, consultables pour une page de détail future. On ne les projette pas
en attributs : cela ferait 150 attributs peu lisibles.

Synonymes : le provider émet directement les noms normalisés (majuscules, sans accent) :
`synonyms: { fr-indice-durabilite: ["NOTE_ID"] }`, et ainsi de suite. La normalisation
corrigée par P0-2 les rend robustes.

Priorité (P0-3) : `preferredSources: [fr-indice-durabilite]` sur les quatre attributs. Une
valeur marchande (`rakuten.com`) ne peut plus l'emporter.

Verticales :

- `tv.yml` et `washing-machine.yml` : ajouter `- key: DURABILITY_INDEX` (et les trois autres) à
  `attributesConfig.configs`, à `availableImpactScoreCriterias` et à `commonAttributes`.
- `washing-machine.yml` : garder `REPAIRABILITY_INDEX` pour les modèles antérieurs à
  avril 2025, mais pondérer `DURABILITY_INDEX` en priorité dans `impactscores/washing-machine.yml`
  (à arbitrer avec le chantier ImpactScore V2).
- `REPAIRABILITY_INDEX.yml` normalise en `MINMAX_OBSERVED` une valeur sur 10 : même défaut,
  traité dans le plan 02.

Champs `Product` : aucun nouveau, hors `sourceAttributions` (P0-4).

## 5. Pipeline

1. **Provider** (à créer) : `FrDurabilityIndexProvider implements ReferenceDatasetProvider`,
   dans `services/reference-datasets`. Il gère deux jeux (TV, lave-linge) sous un seul
   identifiant, avec la catégorie en champ.
   - `resolveDownloadUrls()` : API data.gouv, résolution par titre.
   - Téléchargement : `RemoteFileCachingService.downloadToTmpFile`.
   - `read(Path)` : Jackson `CsvMapper` (déjà utilisé par `FeedIndexingWorker`), puis
     dédoublonnage et règles de la section 3.
   - `sourceRecordId = datagouv_resource_id + ":" + id_modele`, `recordUrl` =
     `url_tableau_detail_notation` (vide sur 376 lignes lave-linge : repli sur la page du jeu
     data.gouv), `sourceUpdatedAt` = `last_modified`.
2. **Import** : `ReferenceIndexService` écrit `ref-fr-indice-durabilite-<horodatage>`, vérifie
   `min-rows-ratio` et bascule l'alias. Le rapport donne les lignes lues, les GTIN valides et les
   modèles disparus.
3. **Projection** : `ReferenceProjectionService`, par lots de 500 GTIN (`multiGetForAggregation`),
   puis passe marque/modèle sur la verticale cible. Elle construit le `DataFragment` synthétique,
   l'agrège, écrit l'attribution et réindexe.
4. **Scores** : le batch quotidien recalcule `DURABILITY`, `REPARABILITY` et l'ImpactScore.
   Vérifier dans le rapport de scoring que `DURABILITY_INDEX` sort de `MINMAX_OBSERVED` sans
   distribution dégénérée.
5. **Export ouvert** : colonne `fr_durability_index` dans `OpenDataService.GTIN_HEADER`, avec les
   quatre modifications décrites dans `OpenDataService` (en-tête, champs requis, `toGtinEntry`,
   libellés i18n `frontend/i18n/locales/{fr-FR,en-US}.json`).
6. **Front** : la note s'affiche par le chemin standard des attributs indexés et des scores. On
   ajoute un badge "Indice de durabilité x/10" dans le bloc impact, avec un lien vers
   `recordUrl`. Composants à identifier dans `frontend/app/components/product/` **(non vérifié)**.

## 6. Planification et volumétrie

- Cron `0 30 4 * * *`, après la consolidation de la veille. La donnée change peu : un import
  quotidien suffit, et un import inchangé est idempotent grâce au hash.
- Import : 2 800 lignes, en quelques secondes. Projection : environ 6 `multiGet` pour les GTIN,
  plus la passe marque/modèle (de l'ordre de 1 500 recherches ES sur deux verticales).

## 7. Sourcing transparent

- `open4goods-config/datasources/fr-indice-durabilite.yml` : `name: "Indice de durabilité
  (data.gouv.fr)"`, `portalUrl` = page du jeu, `license: "Licence Ouverte 2.0"`,
  `attribution: "Données des producteurs, consolidées par data.gouv.fr"`.
- `SourceAttribution` par produit : `recordUrl` (tableau de notation du fabricant),
  `sourceUpdatedAt` (`last_modified`), `retrievedAt`. Les deux dates sont affichées, comme
  l'exige la Licence Ouverte (source et date de dernière mise à jour).

## 8. Tests et critères d'acceptation

- Test unitaire du provider sur une fixture de 20 lignes, tirée du vrai fichier :
  GTIN valide, GTIN à somme fausse, référence commerciale, doublon de modèle, marque en raison
  sociale, `url_tableau_detail_notation` vide.
- Test unitaire de `ReferenceMatcher` : GTIN exact, marque + modèle unique, ambiguïté rejetée.
- Test d'intégration (Testcontainers ES) : import, bascule d'alias, projection sur trois
  produits, puis retrait d'un enregistrement disparu.
- Critères en beta :
  - au moins 70 % des 1 304 GTIN lave-linge rattachés à un produit existant (seuil à ajuster
    après une première mesure) ;
  - taux de rattachement marque/modèle TV mesuré et publié dans le rapport ;
  - zéro rattachement ambigu accepté.

## 9. Découpage

1. P0-2 synonymes, avec test (S).
2. P0-3 `preferredSources` (S).
3. P0-4 `SourceAttribution`, champs d'attribution dans `DataSourceProperties`, DTO front-api
   (M).
4. P0-5 module `services/reference-datasets` : `ReferenceDatasetProvider`, `ReferenceRecord`,
   `ReferenceIndexService` (M).
5. P0-5 côté api : `ReferenceProjectionService`, `ReferenceMatcher` extrait d'EPREL,
   scheduler, `ReferenceDatasetController` (M).
6. `FrDurabilityIndexProvider` et ses tests (S).
7. Attributs YAML et configuration des verticales `tv` et `washing-machine` (S).
8. Colonne d'export ouvert et badge front (S).

## 10. Risques, challenges, questions

- **Le GTIN n'est pas "natif"** pour les TV (43 %) : sans appariement marque/modèle, plus de la
  moitié de la source est perdue. L'extraction de `ReferenceMatcher` hors d'EPREL est le vrai
  coût, et il profite aussi aux plans 04 et 14.
- **Qualité producteur** : 20 organisations publient pour les TV, 19 pour les lave-linge. Des
  valeurs aberrantes sont possibles (`note_id` va de 2,3 à 9,1 pour les TV). Le rapport d'import
  journalise les notes hors de [0, 10].
- **Indice de réparabilité pour les modèles plus anciens** : faut-il afficher les deux indices
  côte à côte, ou masquer `REPAIRABILITY_INDEX` dès qu'un `DURABILITY_INDEX` existe ? Proposition :
  masquer, puisque c'est la même logique que l'étiquette en magasin.
- **ImpactScore V2** (design `dd6d4a893`) : la pondération de `DURABILITY_INDEX` doit être
  décidée dans ce chantier, pas ici.
