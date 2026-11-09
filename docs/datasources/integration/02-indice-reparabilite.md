---
title: "Plan d'intégration : indice de réparabilité"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : indice de réparabilité

> Socle commun : [README](README.md). Fiche source :
> section 4.3 de l'inventaire interne des sources candidates (non publié). Plan voisin,
> même famille de jeux : [01-indice-durabilite.md](01-indice-durabilite.md).
> Mesures du 2026-11-08 sur le fichier consolidé du 2026-11-07 (schéma v0.1.2), téléchargé en
> entier.

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | `fr-indice-reparabilite` |
| Mode | R (référentiel compact), appariement `GTIN_THEN_BRAND_MODEL` limité aux verticales cibles |
| Priorité | P1, à livrer juste après 01 (coût marginal faible). Le gain sur les verticales se concentre sur le lave-vaisselle |
| Effort | S : 1 à 1,5 jour une fois 01 livré, plus 0,5 jour pour la normalisation du score (section 4.3) |
| Verticales | `dishwasher` (580 GTIN), `washing-machine` (92), `smartphones` (6) |
| Hors verticale | ordinateurs portables (1 946 GTIN), aspirateurs (178), tondeuses (16), nettoyeurs haute pression (4) |
| Volume | 4 896 lignes, **2 822 GTIN distincts valides** |
| Dépend de | P0-5 (socle, livré par 01), P0-3 (priorité), P0-4 (attribution). P0-2 n'est pas bloquant (section 4.1) |

## 1. Apport

Pour chaque modèle, la source fournit :

- la note globale sur 10 (`note_ir`, une décimale, bornée à [0, 10] par le schéma) ;
- les cinq critères sur 20 : documentation (`note_c1`), démontabilité (`note_c2`), disponibilité
  des pièces détachées (`note_c3`), prix des pièces (`note_c4`), critère spécifique à la
  catégorie (`note_c5`), et leurs sous-critères sur 10 (`note_c2.1` à `note_c5.3`). Libellés lus
  dans `schema.json` du schéma `etalab/schema-indice-reparabilite` v0.1.2 ;
- la disponibilité et les délais de 15 pièces détachées pour quatre publics (réparateur,
  producteur, distributeur, consommateur) ;
- la date de calcul, le lien vers le tableau de notation et vers la documentation technique.

C'est le seul jeu officiel de cette famille qui note le **prix des pièces** (`note_c4`) : la fiche
candidate indique que l'indice de durabilité l'exclut (R. 541-219).

Situation actuelle de nudger :

- `REPAIRABILITY_INDEX` existe et figure dans `_default.yml` (`attributesConfig.configs`). Il est
  donc résolu pour tout produit, y compris sans verticale : `StandardAggregator` passe par
  `VerticalsConfigService.getConfigByIdOrDefault`.
- Il pèse dans cinq matrices ImpactScore V1 : `smartphones` 0,375, `tv` 0,14,
  `washing-machine` 0,12, `dishwasher` 0,12, `air-conditioner` 0,0825.
- La fiche candidate annonce 49 640 valeurs en base, de provenance non tracée (**non vérifié**
  ici, faute d'accès à l'ES de prod).
- En V1, un produit sans valeur reçoit la moyenne de sa cohorte
  (`AbstractScoreAggregationService.done()`, cf. `docs/impactscore/v2-design.md`). La V2 supprime
  cette imputation : une vraie valeur devient nécessaire pour qu'un produit soit noté.

Ce que la source change : une valeur déclarée par le producteur, datée et liée à son tableau de
notation, qui doit l'emporter sur les recopies marchandes.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Jeu | data.gouv `66e93c3609e8e1084058b1b4`, "Fichiers consolidés des données respectant le schéma "Indice de réparabilité"" | vérifié |
| Schéma | `etalab/schema-indice-reparabilite` v0.1.2, déclaré sur la ressource (`schema.name`, `schema.version`) | vérifié |
| Format | CSV UTF-8, séparateur virgule, 213 colonnes | vérifié |
| Taille | 4 236 144 octets | vérifié |
| Fréquence | `daily`, ressource mise à jour vers 11 h 57 UTC | vérifié |
| Authentification | aucune | vérifié |
| Ressource | `b0fddb1d-7032-4e30-a055-9d923e94ce59`, créée le 2024-09-17, **mise à jour en place** (même identifiant le 2026-11-07) | vérifié |
| URL stable | `https://www.data.gouv.fr/api/1/datasets/r/{resource_id}` répond 302 vers le fichier horodaté du jour (`.../20261107-115720/consolidation-etalab-schema-indice-reparabilite-v-0.1.2-20261107.csv`) | vérifié |
| Producteurs | 32 organisations, 371 jeux producteurs, 422 ressources | vérifié |
| Documentation | ressource `ref-table-etalab-schema-indice-reparabilite.csv` (271 Ko) : une ligne par ressource productrice (page du jeu, organisation, validité au schéma) | vérifié |
| Licence | `lov2` sur le jeu consolidé | vérifié (champ d'attribution seulement) |

Résolution de l'URL, identique à 01 : `GET https://www.data.gouv.fr/api/1/datasets/{id}/`, puis
la ressource dont `schema.name` vaut `etalab/schema-indice-reparabilite` et dont le titre
commence par "Consolidation de la dernière version à date du schéma". Repli : l'URL stable par
identifiant de ressource. La résolution par titre reste primaire, car elle survit à un changement
de version du schéma (le titre porte la version).

Les deux jeux de l'indice de durabilité ont le même comportement : ressources créées le
2025-01-07 (TV) et le 2025-04-09 (lave-linge), mises à jour en place le 2026-11-07. La stabilité
de l'identifiant, notée **non vérifiée** dans 01, est donc établie pour les trois jeux.

Colonnes utiles : `id_unique`, `id_modele`, `referentiel_id_modele`, `nom_modele`,
`categorie_produit`, `id_metteur_sur_le_marche`, `referentiel_id_metteur_sur_marche`,
`nom_metteur_sur_le_marche`, `date_calcul`, `url_tableau_detail_notation`, `note_ir`,
`note_c1` à `note_c5`, `lien_documentation`, `nb_annees_disponibilite_piece_detachee_*`,
`delai_jours_piece_detachee_*`, `last_modified`, `datagouv_dataset_id`, `datagouv_resource_id`,
`datagouv_organization_or_owner`.

## 3. Identifiants et appariement

### 3.1 Mesure

`referentiel_id_modele` déclare `GTIN_EAN` sur 4 880 lignes et `SH4` sur 16. Avec contrôle de la
somme GTIN, **4 695 lignes (95,9 %) portent un GTIN valide, pour 2 822 GTIN distincts**.

| Catégorie (`categorie_produit`) | Lignes | GTIN valides (lignes) | GTIN distincts | Organisations | Verticale |
|---|---|---|---|---|---|
| ordinateur portable | 3 657 | 3 624 | 1 946 | 9 | aucune |
| Lave-vaisselle | 608 | 588 | 580 | 11 | `dishwasher` |
| Aspirateur filaire, non filaire, robot | 417 | 361 | 178 | 8 | aucune |
| Lave-linge top et hublot | 99 | 96 | 92 | 5 | `washing-machine` |
| Nettoyeur haute pression | 76 | 4 | 4 | 2 | aucune |
| Tondeuse électrique batterie et robot | 26 | 16 | 16 | 3 | aucune |
| Smartphone | 13 | 6 | 6 | 2 | `smartphones` |

Le schéma prévoit aussi `Téléviseur` et `Tondeuse à gazon électrique filaire` : aucune ligne.

Défauts relevés :

- **Doublons entre organisations** : 1 678 `id_modele` apparaissent sur plusieurs lignes, dont
  1 663 entre organisations différentes. ASUS publie ses portables, puis TD SYNNEX, Boulanger,
  Fnac Darty, BTLEC et Ingram Micro republient les mêmes GTIN. 180 GTIN portent des notes
  divergentes, par exemple `4711387874196` : 9,9 chez ASUS (calcul du 2026-08-20), 9,4 chez
  TD SYNNEX (calcul du 2025-07-11).
- **Portables : un seul fabricant**. 1 930 des 1 946 GTIN portent le préfixe `471` et des
  références ASUS. Restent 16 HP (préfixe `199`) et 16 Dell sans GTIN.
- **GTIN détruits par un tableur** : `4711640000000` (Cdiscount, 16 lignes, notation
  scientifique).
- **Codes qui ne sont pas des GTIN** : 124 codes à 9 chiffres pour Nilfisk (organisation
  `gaetan-bouvret`), le code douanier SH4 `8471` pour Dell (16 lignes), des références
  commerciales (`COREM6` chez Crosscall, `LLV44W` chez Listo avec un code article dans
  `nom_modele`).
- **Metteur sur le marché = distributeur** : 1 754 lignes ont pour metteur un distributeur ou
  une marque de distributeur (TD SYNNEX, BOULANGER, FNAC DARTY, Cdiscount, BTLEC EST,
  INGRAM MICRO, ESSENTIELB). La marque ne s'en déduit pas.
- **Texte abîmé** : `ŕ` à la place de `à` dans deux libellés de catégorie (4 lignes) ; espaces
  ou tabulation autour de `nom_metteur_sur_le_marche` (18 lignes).
- `id_unique` vaut `id_modele-id_metteur-date_calcul` : il change à chaque recalcul et ne sert pas
  d'identifiant stable.

### 3.2 Règles

1. **GTIN** : `id_modele` passé à `BarcodeValidationService.toProductId`. On rejette un identifiant
   nul ou inférieur à 10^7 (moins de 8 chiffres significatifs). `gtins = {id_modele}`, et
   `nom_modele` est ajouté aux `models`.
2. **Dédoublonnage par GTIN**, à l'import : on garde la ligne au `date_calcul` le plus récent. À
   égalité, une ligne dont le metteur n'est pas dans la liste d'exclusion des distributeurs (règle
   3) passe avant celle d'un distributeur, puis on départage par `last_modified`. Les autres lignes
   restent dans `ref-fr-indice-reparabilite` (payload `otherPublishers`) pour l'audit.
3. **Sans GTIN** (201 lignes, dont 59 non numériques) : `models = {id_modele, nom_modele}`
   nettoyés (suffixe `code 80xxxxx`, préfixe `31020197 - `, suffixe `_869993370420`). La marque
   vient du metteur, sauf pour une liste d'exclusion de distributeurs propre au provider (repérés
   aussi par `id_metteur_sur_le_marche`, identifiant SYDEREP sur 4 855 lignes). Le repli
   marque/modèle ne vise que `dishwasher`, `washing-machine` et `smartphones` : 7 Crosscall,
   3 lave-linge Sharp et 19 lave-vaisselle (11 Hisense, 8 Essentielb et Listo, marques de
   distributeur). Les codes Nilfisk, Dell et Cdiscount sont écartés et comptés dans le rapport
   d'import.
4. **Catégorie** : libellé réparé (`ŕ` vers `à`), puis table de correspondance vers la verticale
   (`Lave-vaisselle` vers `dishwasher`, `Lave-linge*` vers `washing-machine`, `Smartphone` vers
   `smartphones`, le reste vers aucune).
5. **Projection** : le GTIN fait foi. Si le produit trouvé appartient à une autre verticale que
   celle de la catégorie, la valeur est projetée et le cas est compté dans le rapport
   ("verticale incohérente").

## 4. Modèle cible

### 4.1 Attributs

| Clé | Colonne | Changement |
|---|---|---|
| `REPAIRABILITY_INDEX` (existant) | `note_ir` | `preferredSources: [fr-indice-reparabilite]` (P0-3) |
| `MINAVAILABILITYSPAREPARTSYEARS` (existant) | même règle que 01 sur `nb_annees_disponibilite_piece_detachee_*` (900 lignes renseignées) | aucun |

Le provider émet le nom `REPAIRABILITY_INDEX`, déjà présent sans accent dans `synonyms.all` de
`REPAIRABILITY_INDEX.yml`. L'appariement ne dépend donc pas de P0-2. P0-2 doit pourtant partir
avec P0-3 : une fois les synonymes accentués normalisés, les valeurs marchandes "Indice de
réparabilité" commenceront à être reconnues, et sans priorité de source elles gagneront aux voix.

Les critères `note_c1` à `note_c5`, les sous-critères et le détail des pièces restent dans
`ref-fr-indice-reparabilite`, comme dans 01. `note_c4` (prix des pièces) est un candidat naturel
pour une page de détail future.

Aucun nouveau champ `Product` hors `sourceAttributions` (P0-4). Aucun changement de
`product-mappings.json` : `attributes.indexed.*.numericValue` est déjà mappé en `double` par un
gabarit dynamique.

### 4.2 Quelle valeur gagne

Trois valeurs légales coexistent selon la catégorie et la date. Ce sont trois grandeurs
différentes : on ne les fusionne jamais dans un même attribut.

| Verticale | Valeur légale en vigueur | Attribut porteur | Rôle de `fr-indice-reparabilite` |
|---|---|---|---|
| `tv` | indice de durabilité depuis le 2025-01-08 (**non vérifié**, cf. 01) | `DURABILITY_INDEX` (plan 01) | aucun : pas de téléviseur dans le fichier |
| `washing-machine` | indice de durabilité depuis le 2025-04-08 (**non vérifié**, cf. 01) | `DURABILITY_INDEX` | `REPAIRABILITY_INDEX` pour 92 GTIN, surtout des modèles antérieurs. 19 lignes sur 99 ont pourtant un `date_calcul` postérieur au 2025-04-08 |
| `smartphones` | classe de réparabilité UE A à E (Règl. 2023/1669, applicable le 2025-06-20) ; fin de l'indice français pour cette catégorie **non vérifiée** sur Légifrance | `EU_REPAIRABILITY_CLASS`, créé par le plan 05 | `REPAIRABILITY_INDEX` pour 6 GTIN Fairphone (calcul du 2025-06-25) |
| `dishwasher` | indice de réparabilité (toujours en vigueur, **non vérifié**) | `REPAIRABILITY_INDEX` | source principale, 580 GTIN |
| hors verticale | indice de réparabilité | `REPAIRABILITY_INDEX` via `_default.yml` | affichage sur la fiche, sans score |

Règles proposées :

1. **Dans `REPAIRABILITY_INDEX`** : `fr-indice-reparabilite` gagne sur toute autre source
   (`preferredSources`, P0-3), puis la liste actuelle (`eprel`, `icecat.biz`), puis les voix. Dans
   la source elle-même, la règle de dédoublonnage de la section 3.2 s'applique.
2. **Entre attributs, à l'affichage** : si `DURABILITY_INDEX` existe, `REPAIRABILITY_INDEX` est
   masqué (proposition de 01). Si la classe UE existe, elle prend le bloc impact, et l'indice
   français reste dans les caractéristiques.
3. **Entre attributs, dans l'ImpactScore** : une seule grandeur de réparabilité par produit, pour
   éviter le double comptage. La V1 ne sait pas exprimer un repli d'un critère vers un autre ;
   c'est une décision du chantier ImpactScore V2 (question en section 10).

### 4.3 Échelle et normalisation du score

`REPAIRABILITY_INDEX.yml` déclare `scale.max: 5.0`. **Ce n'est pas une erreur d'échelle** :
`scale` est la plage de sortie du score normalisé (`ScoreScaleConfig`, défaut 0 à 5 pour tous
les scores, appliquée par `AbstractNormalizationStrategy.scaleAndClamp`). La valeur brute reste
sur 10 dans `attributes.indexed`.

Le vrai sujet est la méthode `MINMAX_OBSERVED` : elle étire l'intervalle observé dans la
cohorte sur 0-5. Or les notes sont tassées en haut de l'échelle (mesure du fichier) :

| Catégorie | Min | Max | Moyenne |
|---|---|---|---|
| ordinateur portable | 8,7 | 10 | 9,39 |
| Lave-vaisselle | 6,0 | 9,9 | 9,04 |
| Lave-linge (top et hublot) | 7,9 | 9,7 | 9,22 |
| Smartphone | 9,2 | 9,6 | 9,30 |

Les bornes observées sont celles de la cohorte ES, pas du fichier. Si la cohorte lave-vaisselle
avait les bornes du fichier (6,0 et 9,9), un modèle noté 8,5/10 en magasin sortirait à 3,2/5, et
un 9,0 à 3,8/5. Une seule valeur marchande aberrante (par exemple "95" pour 9,5) écraserait tous
les autres produits vers 0. Proposition : `MINMAX_FIXED` avec `fixedMin: 0.0` et
`fixedMax: 10.0`, sur le modèle de `CLASSE_ENERGY.yml`. La note légale devient lisible telle
quelle (8,5/10 donne 4,25/5) et les valeurs hors de [0, 10] sont bornées. Le changement déplace
l'ImpactScore V1 de cinq verticales : il passe par une simulation, ou par une surcharge
`criteriaScoring` du bloc `v2` (V2 uniquement, cf. `docs/impactscore/v2-design.md`).

Le plan 01 propose `max: 10.0` pour `DURABILITY_INDEX` : avec la même lecture, ce score
sortirait sur 0-10 alors que tous les autres sont sur 0-5. Il faut plutôt `scale.max: 5.0` et
`fixedMax: 10.0`.

## 5. Pipeline

1. **Base commune (à créer)** : `AbstractDataGouvConsolidationProvider implements
   ReferenceDatasetProvider`, dans `services/reference-datasets`. Mesure : 160 des 213 colonnes de
   l'indice de réparabilité existent à l'identique dans le fichier TV de l'indice de durabilité
   (198 colonnes). La classe porte tout ce qui est commun aux trois jeux :
   - résolution de l'URL (section 2) et téléchargement par
     `RemoteFileCachingService.downloadToTmpFile` ;
   - lecture CSV par Jackson `CsvMapper` ;
   - les 16 colonnes communes hors pièces (identité, metteur, dates, URL, provenance data.gouv) et
     les colonnes de pièces détachées ;
   - dédoublonnage, nettoyage (espaces, tabulation, `ŕ`), validation GTIN, table des
     distributeurs.

   Les sous-classes déclarent seulement les jeux et le schéma, les colonnes de notes vers les
   attributs et la table catégorie vers verticale. Ce n'est pas un nouveau mécanisme générique :
   c'est une factorisation interne au module, sous l'interface du socle. `FrDurabilityIndexProvider`
   (plan 01) gagne à être écrit directement dessus, pour le même coût.
2. **Provider (à créer)** : `FrRepairabilityIndexProvider extends
   AbstractDataGouvConsolidationProvider`.
   - `sourceRecordId` : `fr-indice-reparabilite:` suivi du GTIN, ou de l'`id_modele` nettoyé sans
     GTIN. `id_unique`, l'organisation et le `datagouv_resource_id` vont dans le payload.
   - `recordUrl` : `url_tableau_detail_notation` (876 lignes), sinon `lien_documentation`
     (650 lignes), sinon la page du jeu producteur `https://www.data.gouv.fr/datasets/{datagouv_dataset_id}`,
     toujours disponible. Les deux premières sont vides sur 4 020 lignes (82 %).
   - `sourceUpdatedAt` : `last_modified`.
3. **Import** : `ReferenceIndexService` écrit `ref-fr-indice-reparabilite-<horodatage>`, vérifie
   `min-rows-ratio` et bascule l'alias. Le rapport compte les lignes lues, les GTIN valides, les
   doublons arbitrés, les codes écartés par motif et les GTIN disparus.
4. **Projection** : `ReferenceProjectionService`, 6 `multiGetForAggregation` de 500 GTIN, puis la
   passe marque/modèle de `ReferenceMatcher` sur les trois verticales cibles (une soixantaine
   d'enregistrements). `DataFragment` synthétique avec `REPAIRABILITY_INDEX` et
   `MINAVAILABILITYSPAREPARTSYEARS`, agrégation, attribution, réindexation.
   Les produits sans verticale reçoivent l'attribut à l'agrégation (`_default.yml`), sans score.
   Côté front-api, `resolveVerticalConfig` renvoie `null` sans verticale : `mapIndexedAttribute`
   expose l'attribut sous sa clé brute (`REPAIRABILITY_INDEX`, sans libellé localisé) et
   `mapClassifiedAttributes` ne renvoie rien. `ProductPage.vue` affiche la section
   caractéristiques sans condition de verticale (`showAttributesSection`), mais le rendu de la
   note sur une fiche non classée est **à vérifier en beta**. Correctif probable : repli sur la
   configuration par défaut pour le libellé (S, à ajouter au ticket 4).
5. **Scores** : batch quotidien. Vérifier dans le rapport de scoring la distribution de
   `REPAIRABILITY_INDEX` pour `dishwasher` avant et après.
6. **Export ouvert** : colonne `fr_repairability_index` dans `OpenDataService.GTIN_HEADER`.
   `toGtinEntry` ne la remplit que si `fr-indice-reparabilite` figure parmi les sources de
   l'attribut indexé (présentes dans `_source`), pour ne pas publier une recopie marchande sous
   ce nom. `GTIN_REQUIRED_FIELDS` contient déjà `attributes`. Libellés i18n
   `opendata.datasets.gtin.columns.fr_repairability_index` dans
   `frontend/i18n/locales/{fr-FR,en-US}.json`.
7. **Front** : même badge que 01 ("Indice de réparabilité x/10", lien vers `recordUrl`, date de
   calcul), masqué si `DURABILITY_INDEX` existe.

## 6. Planification et volumétrie

- Cron `0 45 4 * * *`, après celui de 01 : la consolidation de la veille est publiée vers
  11 h 57 UTC.
- Import : 4 896 lignes, 4,2 Mo, quelques secondes. Projection : 6 `multiGet` et une soixantaine
  de recherches marque/modèle.
- La donnée bouge peu : 88 lignes modifiées en novembre, 160 en octobre (`last_modified`). La
  projection est idempotente grâce au hash.

## 7. Sourcing transparent

- `open4goods-config/datasources/fr-indice-reparabilite.yml` : `name: "Indice de réparabilité
  (data.gouv.fr)"`, `portalUrl` = page du jeu consolidé, `license: "Licence Ouverte 2.0"`,
  `attribution: "Données des producteurs, consolidées par data.gouv.fr"`.
- `SourceAttribution` par produit : `recordUrl`, `sourceUpdatedAt` (`last_modified`),
  `retrievedAt`. Le payload garde l'organisation publiante (`datagouv_organization_or_owner`) et
  `date_calcul`, pour afficher "publié par ASUS, calculé le 2026-09-15". Afficher le publiant est
  utile ici, car un distributeur peut publier pour une marque.

## 8. Tests et critères d'acceptation

- Test unitaire du provider sur une fixture d'environ 25 lignes tirées du vrai fichier :
  doublon ASUS et TD SYNNEX à notes divergentes, `4711640000000`, code Nilfisk à 9 chiffres,
  Dell `8471`, Crosscall sans GTIN, Listo avec `code 80...`, catégorie avec `ŕ`, metteur
  avec tabulation, URL vides.
- Test de la base commune avec une fixture TV de 01 : mêmes règles communes, colonnes de notes
  différentes.
- Test de la règle de dédoublonnage (date, puis marque, puis `last_modified`).
- Si 4.3 est adopté : test de normalisation (9,0 donne 4,5 ; 12 est borné à 5).
- Critères en beta :
  - au moins 60 % des 580 GTIN lave-vaisselle rattachés à un produit existant (seuil à ajuster
    après le ticket 0) ;
  - zéro valeur élue hors de [0, 10] ;
  - nombre de produits dont la valeur gagnante change (marchand vers source) et distribution de
    l'écart, publiés dans le rapport.

## 9. Découpage

0. Mesure du recouvrement sur l'ES de prod, requêtes de la section 10, sur la liste des 2 822 GTIN
   extraite du fichier par un script jetable (XS).
1. Base commune `AbstractDataGouvConsolidationProvider`, livrée avec 01 (S, comptée dans 01).
2. `FrRepairabilityIndexProvider` et ses tests (S).
3. `preferredSources` sur `REPAIRABILITY_INDEX`, après P0-3 (XS).
4. Colonne d'export ouvert et badge front, partagés avec 01 (S).
5. Normalisation `MINMAX_FIXED` 0-10 avec simulation, à porter dans le chantier ImpactScore V2
   (S).

## 10. Risques, challenges, questions

- **Le volume annoncé est surestimé.** "4 880 avec GTIN" est le référentiel déclaré. On mesure
  2 822 GTIN distincts valides, dont 1 946 portables. Les "environ 100 lave-linge" sont 92 GTIN.
  La fiche candidate oublie les aspirateurs (178 GTIN), les tondeuses et les nettoyeurs.
- **Portables et Q1** (Q1 décidée le 2026-11-09 : création par défaut ; l'analyse qui suit
  reste la liste des risques, traités par P0-11) : le README cite les portables de cette source comme premier candidat au
  mode `CREATE`. C'est à revoir. 99 % des GTIN sont ASUS : créer ces pages reviendrait à publier un
  catalogue ASUS de 1 946 fiches sans offre, avec une seule note par fiche. Recommandation :
  `ENRICH_EXISTING`. La note atteint les fiches non classées qui existent (section 5, rendu à
  vérifier).
  Une verticale portables relève d'une autre décision, alimentée par les flux et Icecat, pas par
  cette source. Même raisonnement pour les aspirateurs.
- **Smartphones** : 6 GTIN, tous Fairphone. La source n'apporte presque rien à la verticale.
  L'ImpactScore smartphones pèse `REPAIRABILITY_INDEX` à 0,375 sur "1 983 hits" (commentaire de
  `impactscores/smartphones.yml`), qui viennent donc d'ailleurs. Le successeur légal est la
  classe UE d'EPREL (plan 05).
- **Distributeurs republieurs** : TD SYNNEX republie des notes ASUS plus anciennes. La règle de
  la section 3.2 privilégie le calcul le plus récent ; si la marque publie elle-même, elle gagne
  à date égale.
- **Provenance des 49 640 valeurs actuelles** : non mesurable ici. `attributes.indexed.*.source`
  est en `enabled: false` dans `product-mappings.json` : une agrégation ES ne peut pas compter les
  sources, il faut lire `_source`. Requêtes à lancer avec l'identité ES en lecture seule
  `agents_readonly` (`docs/operations/infra-secrets-rotation.md`) :

  ```json
  POST products-moustik/_search
  { "size": 0,
    "query": { "exists": { "field": "attributes.indexed.REPAIRABILITY_INDEX.numericValue" } },
    "aggs": {
      "vertical": { "terms": { "field": "vertical", "missing": "_none_", "size": 20 } },
      "stats":    { "stats": { "field": "attributes.indexed.REPAIRABILITY_INDEX.numericValue" } },
      "hors_0_10": { "filter": { "bool": { "should": [
        { "range": { "attributes.indexed.REPAIRABILITY_INDEX.numericValue": { "gt": 10 } } },
        { "range": { "attributes.indexed.REPAIRABILITY_INDEX.numericValue": { "lt": 0 } } } ] } } } } }
  ```

  Puis la même requête avec `"size": 1000`, `"_source": ["attributes.indexed.REPAIRABILITY_INDEX.source"]`
  et `search_after`, en comptant `dataSourcename` côté client. Pour le recouvrement avec la
  source : une requête `ids` sur les 2 822 GTIN (par lots de 1 000), avec les mêmes agrégations
  `vertical`, un filtre `offersCount > 0` et un filtre `exists` sur l'attribut.
- **Disponibilité des pièces** : 01 projette le maximum des `nb_annees_disponibilite_piece_detachee_*`
  dans `MINAVAILABILITYSPAREPARTSYEARS`. Le nom de l'attribut et la lecture prudente plaident pour
  le minimum sur les pièces listées. À trancher une fois pour les deux plans.
- **Questions au propriétaire** :
  1. Normalisation du score (4.3) : changer `REPAIRABILITY_INDEX.yml` maintenant, ce qui déplace
     l'ImpactScore V1 de cinq verticales, ou seulement dans le bloc `v2` ?
  2. Portables et aspirateurs : création décidée le 2026-11-09 (README, Q1). Une verticale
     portables reste une décision distincte.
  3. Smartphones : dans l'ImpactScore V2, la classe UE remplace-t-elle l'indice français, qui ne
     resterait alors qu'une information de fiche ?
