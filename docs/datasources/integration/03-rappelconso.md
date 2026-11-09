---
title: "Plan d'intégration : RappelConso V2 (rappels de produits)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : RappelConso V2 (rappels de produits)

> Socle commun : [README](README.md), prérequis P0-6 (bloc `recalls`). Fiche source :
> section 4.4 de l'inventaire interne des sources candidates (non publié). Plan voisin
> pour le même bloc : `04-safety-gate.md`.
> Mesures du 2026-11-08 sur l'export complet du jeu `rappelconso-v2-gtin-trie` (traité par la
> plateforme le 2026-11-08 à 20 h 05 UTC), téléchargé en entier.

## 0. Fiche

**Verdict : import complet recommandé, car il est peu coûteux. Le bloc `recalls`, le front et
l'export restent conditionnés au ticket 0 (mesure du recouvrement avec nos produits).** Sur les
verticales, le recouvrement attendu est quasi nul (section 1.2) ; la valeur, si elle existe, est
sur les fiches non classées.

| Champ | Valeur |
|---|---|
| Identifiant | `rappelconso` |
| Mode | R (référentiel compact) + bloc `recalls` (P0-6), appariement `GTIN_ONLY` |
| Priorité | P1 pour l'import et la mesure (ticket 0) ; P2 pour le reste, à confirmer par la mesure |
| Effort | M : 3 à 4 jours si le bloc `recalls` (P0-6) est construit ici ; S (1 jour) pour l'import seul |
| Verticales | aucune de façon significative : 8 fiches touchent `oven`, `air-conditioner` et un sèche-linge, toutes terminées |
| Hors verticale | jouets, puériculture, maison, sport, vêtements, petit électroménager |
| Volume | 18 862 lignes, 12 619 fiches, **13 652 GTIN exploitables** après filtrage (dont 3 670 hors alimentaire) |
| Dépend de | P0-4 (attribution), P0-5 (socle), P0-6 (bloc `recalls`, construit ici une fois pour 03 et 04) |

## 1. Apport

### 1.1 Contenu

Un rappel est un fait de sécurité daté, publié par l'administration (DGCCRF, DGAL, DGEC, DGPR) :

- motif du rappel, risques encourus, conduite à tenir, modalités de compensation ;
- lots concernés, par GTIN (`identification_produits`) ;
- dates de publication, de commercialisation et de fin de procédure ;
- nature juridique (volontaire, ou imposé par arrêté préfectoral : 2 284 lignes) ;
- lien vers la fiche publique, l'affichette PDF et les photos.

Aucun comparateur grand public ne l'affiche sur la fiche produit à notre connaissance
(**non vérifié**).

### 1.2 Ce qu'on gagne réellement

| Mesure (2026-11-08) | Valeur |
|---|---|
| Lignes alimentaires | 14 536 sur 18 862 (77,1 %) ; 10 510 fiches sur 12 619 (83,3 %) |
| GTIN alimentaires exploitables | 9 983 |
| GTIN hors alimentaire exploitables | 3 670, sur 1 824 fiches |
| Fiches touchant une verticale | 4 micro-ondes encastrables du groupe Brandt (De Dietrich, Vedette, Sauter, Brandt ; 17 GTIN, septembre 2025), 1 lot de fours Domedia (2023), 2 climatiseurs mobiles (Qilive, Essentiel B, 2023), 1 sèche-linge Hisense (2025). Toutes terminées |
| Téléviseurs, lave-linge, lave-vaisselle, réfrigérateurs, smartphones | 0 fiche (recherche par mots-clés sur `libelle` et `modeles_ou_references`) |
| Rappels actifs | par date de fin : 98 lignes, 53 fiches. Avec la règle de la section 4.2 : 530 fiches, dont 105 hors alimentaire |

Nudger n'a pas de produits alimentaires : `BatchService.clean()` a supprimé les produits de la
famille Open Food Facts. Les 9 983 GTIN alimentaires ne rattacheront presque rien. Restent les
3 670 GTIN non alimentaires : jouets (923 lignes), puériculture (388), divers (466), électroménager
et appareils électriques (419), cosmétiques (345), sport (337), vêtements (331), cuisine (262).
Ils ne peuvent rattacher que des produits non classés, venus des flux marchands. Ces produits ont
une page : `/{gtin}-{slug}`, couverte par `frontend/app/pages/product-uncategorized.spec.ts`. La
section vigilance et son bandeau ne dépendent pas de la verticale (`ProductPage.vue`). Combien de
ces GTIN existent chez nous, avec une offre ? C'est l'objet du ticket 0 (section 3.3).

Gains possibles, par ordre de certitude :

1. l'historique des rappels sur les fiches non classées qui existent ("ce produit a fait l'objet
   d'un rappel en 2024") ;
2. le bandeau "rappel en cours" (rare : 105 fiches non alimentaires actives le 2026-11-08,
   avec la règle de la section 4.2) ;
3. une colonne de l'export ouvert ;
4. un signal par marque, indépendant du GTIN (section 10).

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Plateforme | data.economie.gouv.fr, Opendatasoft, API Explore v2.1 | vérifié |
| Jeu | `rappelconso-v2-gtin-trie`, "RappelConso V2 (produits triés par GTIN)", une ligne par GTIN et par fiche | vérifié |
| Métadonnées | `GET https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/rappelconso-v2-gtin-trie` : `records_count` 18 862, `data_processed` 2026-11-08T20:05:35 UTC, `publisher` DGCCRF | vérifié |
| Export | `.../exports/csv?delimiter=%3B` : 31 662 070 octets, 4,6 Mo compressés en gzip, 3,4 s. Aussi `parquet` (4,5 Mo), `jsonl`, `json`, `xlsx` | vérifié |
| Colonnes | 35 | vérifié |
| Fréquence | `update_frequency` vide. 66 lignes publiées depuis le 2026-11-01, 141 fiches dans les 30 derniers jours. Rythme de retraitement par la plateforme **non vérifié** : relever `data_processed` pendant une semaine | partiel |
| Authentification | aucune ; quotas anonymes Opendatasoft **non vérifiés** | partiel |
| Licence | "Licence Ouverte v2.0 (Etalab)" (métadonnée `license`) | vérifié (champ d'attribution seulement) |
| Jeu compagnon | `rappelconso-v2-gtin-espaces` : 19 207 fiches, une par fiche, sans colonne `gtin` (GTIN dans `identification_produits`), soit 6 588 fiches de plus, sans GTIN (les automobiles comptent 1 922 fiches dans ce jeu) | vérifié |

L'export est la seule voie complète : l'endpoint `records` pagine et plafonne l'offset (limite
de 10 000 de la documentation Opendatasoft, **non vérifiée** ici). On ne fige pas d'URL de
fichier : l'URL d'export est stable par construction.

Champs disponibles (taux de remplissage sur 18 862 lignes) :

| Groupe | Colonnes | Remplissage |
|---|---|---|
| Identité | `gtin`, `numero_fiche`, `numero_version`, `id`, `rappel_guid`, `libelle`, `marque_produit`, `modeles_ou_references` | 100 % |
| Classement | `categorie_produit` (10 valeurs), `sous_categorie_produit` | 100 % |
| Lots | `identification_produits` (`GTIN$lot$type de date$date1$date2`, séparés par `\|`) | 100 % |
| Motif et risques | `motif_rappel`, `risques_encourus` (cases séparées par `\|`) ; `description_complementaire_risque` 6 179 ; `preconisations_sanitaires` 7 215 | 100 % / partiel |
| Conduite à tenir | `conduites_a_tenir_par_le_consommateur`, `modalites_de_compensation` (cases séparées par `\|`) ; `numero_contact` 15 015 | 100 % / partiel |
| Dates | `date_publication` (horodatage) 100 % ; `date_de_fin_de_la_procedure_de_rappel` 15 766 ; `date_debut_commercialisation` 16 965 ; `date_date_fin_commercialisation` 16 518 | voir section 4.2 |
| Diffusion | `distributeurs`, `zone_geographique_de_vente` (France entière : 14 714) | 100 % |
| Liens | `lien_vers_la_fiche_rappel`, `lien_vers_affichette_pdf`, `liens_vers_les_images` (une image ou plus par ligne) 100 % ; listes de produits 4 311, de distributeurs 4 552 | 100 % / partiel |
| Alimentaire | `temperature_conservation`, `marque_salubrite`, `conditionnements` | partiel |

Le texte est en minuscules dans l'export ("petite voiture jouet", "zeeman") : on le capitalise à
l'affichage, et la marque affichée reste celle du produit. `lien_vers_la_fiche_rappel` renvoie une
page HTML publique (200 vérifié sur `https://rappel.conso.gouv.fr/fiche-rappel/24035/interne`),
alors que la description du champ annonce un PDF.

## 3. Identifiants et appariement

### 3.1 Mesure

La colonne `gtin` est typée `int` dans Opendatasoft (description : "8 à 17 chiffres") : **les zéros
de tête sont perdus**.

| Longueur de `gtin` | 1 à 7 | 8 | 9 à 11 | 12 | 13 | 14 | 15 à 17 |
|---|---|---|---|---|---|---|---|
| Lignes | 259 | 462 | 277 | 1 383 | 16 245 | 197 | 39 |

- GTIN valides tels quels : 16 805 lignes (89,1 %).
- Avec la règle de `BarcodeValidationService.toProductId` (remise à 13 chiffres par la gauche,
  puis somme de contrôle) : 16 989 lignes acceptées. Mais 89 valent `0` (13 zéros passent la
  somme de contrôle et donnent l'identifiant 0), 20 autres ont moins de 8 chiffres significatifs,
  et 62 sont des suites factices à somme juste (`1111111111649`, `1234543212578`,
  `1313131313162`).
- Restent 16 818 lignes, 14 428 GTIN distincts. Parmi elles, 871 lignes portent un code de
  diffusion restreinte : 607 GTIN-13 commençant par 2 (dont 513 en alimentaire) et 264 UPC-A de
  système 2 ou 4. Ce sont des codes internes aux magasins, réattribués d'une enseigne à l'autre.
- **Après filtrage complet (règle 3.2.1) : 15 947 lignes (84,5 %), 13 652 GTIN distincts, sur
  10 823 fiches.** Dont 3 670 GTIN hors alimentaire.
- 1 826 GTIN figurent sur plusieurs lignes : un même produit peut avoir plusieurs fiches.

### 3.2 Règles

1. `toProductId`, puis rejet des identifiants inférieurs à 10^7, des suites factices et des
   préfixes GS1 de diffusion restreinte (020-029, 040-049 et 200-299, après remise à 13
   chiffres). Un code interne de magasin poserait un bandeau de rappel sur un produit sans
   rapport.
2. Les identifiants produit sont des `Long` (`multiGetForAggregation`) : la perte des zéros de
   tête par Opendatasoft est sans effet sur l'appariement, seule la validation exige la remise
   à 13 chiffres.
3. Un `ReferenceRecord` par fiche (`numero_fiche`), `gtins` = ensemble des GTIN filtrés de la
   fiche.
4. Lots : `identification_produits` est découpé par `|` puis par `$`. On garde, pour chaque GTIN,
   ses lots et ses dates (`lotsByGtin`). Un rappel vise souvent certains lots seulement.
5. Pas de repli marque/modèle : `marque_produit` et `modeles_ou_references` sont du texte libre
   ("sans marque" sur 45 fiches non alimentaires). Le matcher marque/modèle est du ressort du
   plan 04.

### 3.3 Recouvrement avec nos produits (ticket 0, non mesuré)

Pas d'accès à l'ES de prod depuis cette rédaction. Le ticket 0 n'attend pas le provider : trois
listes de GTIN s'extraient de l'export public avec les filtres de la section 3.2, par un script
jetable. Au 2026-11-08 : alimentaire (9 983), non alimentaire (3 670), rappels actifs selon la
section 4.2 (815). Requête à lancer avec l'identité en lecture seule `agents_readonly` (droits
`read` sur `products-moustik`, `docs/operations/infra-secrets-rotation.md`), liste par liste.

```bash
# lots de 1 000 identifiants ; ids-nonalim.txt : un GTIN normalisé par ligne
split -l 1000 ids-nonalim.txt lot-
for f in lot-*; do
  jq -R . "$f" | jq -s '{size: 0, query: {ids: {values: .}},
      aggs: {vertical: {terms: {field: "vertical", missing: "_none_", size: 20}},
             avec_offres: {filter: {range: {offersCount: {gt: 0}}}}}}' \
  | curl -s -u "agents_readonly:$ES_PASSWORD" -H 'Content-Type: application/json' \
         "$ES_URL/products-moustik/_search" -d @- \
  | jq -c '{trouves: .hits.total.value, avec_offres: .aggregations.avec_offres.doc_count,
            vertical: .aggregations.vertical.buckets}'
done
```

Seuils de décision proposés (question en section 10) :

- **go** pour le bloc, le front et l'export : au moins 100 produits non alimentaires avec offre
  rattachés, ou au moins 10 rattachés à un rappel actif ;
- **sinon** : import dans `ref-rappelconso` seul, sans projection. La donnée reste prête pour
  le jour où les flux créent ces produits, et pour le plan 04.

## 4. Modèle cible

### 4.1 Bloc `ProductRecall` (P0-6)

`List<ProductRecall> Product.recalls`, champ Java réel : la chaîne d'agrégation relit et réécrit
le document entier, un champ inconnu de `Product` serait perdu à la réécriture suivante.

| Champ | Type | Colonne source | Règle |
|---|---|---|---|
| `source` | String | constante | `rappelconso` |
| `recallId` | String | `numero_fiche` | clé stable entre versions ; stabilité de `id` et `rappel_guid` d'une version à l'autre **non vérifiée** |
| `url` | String | `lien_vers_la_fiche_rappel` | page HTML publique |
| `publishedAt` | Long (epoch ms, comme `creationDate`) | `date_publication` | date de la dernière version ou de la première : **non vérifié** |
| `endsAt` | Long, nullable | `date_de_fin_de_la_procedure_de_rappel` | vide si absente ou antérieure à la publication (194 lignes incohérentes) |
| `riskLabel` | String | `risques_encourus` | proposition : `List<String> risks`, découpé par `\|` |
| `measures` | String | `conduites_a_tenir_par_le_consommateur` | proposition : `List<String>`, découpé par `\|` |
| `active` | boolean | calculé | section 4.2 |

Ajouts proposés au P0-6, intégrés depuis dans la définition unique du README (P0-6), avec les
champs propres à Safety Gate du plan 04 (`lots` y devient `batchNumbers`, `category` devient
`productCategory`, `riskLabel` devient `risks`) :

| Champ | Type | Colonne source |
|---|---|---|
| `version` | int | `numero_version` (1 361 lignes en version 2 ou plus) |
| `title` | String | `libelle` |
| `reason` | String | `motif_rappel` |
| `compensation` | List<String> | `modalites_de_compensation` |
| `lots` | String | entrée de `lotsByGtin` pour le GTIN du produit |
| `legalNature` | String | `nature_juridique_rappel` |
| `category` | String | `categorie_produit` et `sous_categorie_produit` |
| `posterUrl` | String | `lien_vers_affichette_pdf` |
| `imageUrls` | List<String> | `liens_vers_les_images`, lien seul, aucune copie |

Restent dans `ref-rappelconso` sans aller sur le produit : distributeurs, zone de vente, numéro de
contact, préconisations sanitaires, compléments libres, dates de commercialisation, champs
alimentaires. La fiche publique les présente déjà.

### 4.2 Cycle de vie : actif, terminé, retiré

Mesure des dates de fin au 2026-11-08 : 98 lignes dans le futur, 15 668 dans le passé (dont 194
antérieures à la publication), 3 096 vides. Écart entre publication et fin quand les deux
existent : médiane 36 jours, 90e centile 97 jours.

| Situation | Détection | Effet sur le produit |
|---|---|---|
| Fin dans le futur | `endsAt >= aujourd'hui` | `active = true` |
| Fin passée | `endsAt < aujourd'hui` | `active = false`, l'entrée reste (historique) |
| Fin absente ou incohérente, publication récente | `endsAt` vide (ou antérieur à la publication) et publication depuis 90 jours au plus (956 lignes) | `active = true` |
| Fin absente ou incohérente, publication ancienne | même cas, publication depuis plus de 90 jours (2 334 lignes) | `active = false`, libellé "date de fin non communiquée" |
| Fiche mise à jour | `numero_version` augmente | entrée remplacée ; GTIN retirés de la fiche : entrée supprimée sur ces produits |
| Fiche retirée | `numero_fiche` présent dans la version précédente de `ref-rappelconso`, absent de la nouvelle | entrée supprimée |

Avec cette règle, 1 054 lignes et 530 fiches seraient actives aujourd'hui, dont 324 lignes et
105 fiches hors alimentaire. Le seuil de 90 jours colle au 90e centile mesuré ; c'est une
question au propriétaire.

Deux écarts avec le déroulé du socle (README section 3.2, étape 4) :

- un rappel terminé ne se retire pas : il passe à `active = false` et reste en historique ;
- `Product.removeDatasourceData` ne touche pas un bloc `recalls`. Le retrait d'une fiche est donc
  une suppression explicite de l'entrée `source = rappelconso, recallId = X`.

L'état `active` dépend de la date du jour : la projection le recalcule à chaque passage, même si
la source n'a pas changé. Le front le recalcule aussi (section 5), pour qu'un import en échec ne
laisse pas un bandeau périmé.

La suppression réelle d'une fiche sur RappelConso (par exemple une erreur de publication), et sa
disparition de l'export, sont **non vérifiées**. Le garde-fou `min-rows-ratio` protège contre
un export tronqué.

### 4.3 Mapping ES

`product-mappings.json` déclare `"dynamic": "strict"` à la racine. Le README a été corrigé
en ce sens (P0-4) : `ProductIndexMappingRunner` pose ce JSON tel quel
(`@Mapping(mappingPath = "/product-mappings.json")` sur `Product`), et tous les champs persistés
de `Product` y figurent. Constat par lecture du code, à confirmer par
`GET products-moustik/_mapping` (`agents_readonly` a `view_index_metadata`). Sous `strict`,
indexer un document qui porte un champ non déclaré échoue : `recalls` doit être déclaré avant la
première écriture.

```json
"recalls": {
  "type": "object",
  "dynamic": "false",
  "properties": {
    "source":      { "type": "keyword" },
    "recallId":    { "type": "keyword" },
    "active":      { "type": "boolean" },
    "publishedAt": { "type": "date" },
    "endsAt":      { "type": "date" },
    "category":    { "type": "keyword" }
  }
}
```

`dynamic: false` au niveau du bloc, comme `eprelDatas` : les autres champs restent dans
`_source` sans être indexés. Un tableau d'objets aplatit ses éléments : un filtre combiné
(`active = true` et `source = safety-gate`) pourrait apparier deux entrées différentes. Les
besoins actuels (produits ayant un rappel actif, comptes par source) n'utilisent qu'un champ à
la fois ; passer en `nested` le jour où un filtre combiné devient nécessaire.
`ProductIndexMappingRunner` ajoute le champ au démarrage de l'api (un ajout de champ est accepté
sur l'index existant).

### 4.4 Écriture partielle

`ProductPartialUpdateField` ne connaît que `LAST_CHANGE`, `PRICE` et `OFFERS_COUNT`. Proposition :
ajouter `RECALLS("recalls")`, pour que la projection écrive le bloc par
`addToPartialIndexationQueue` sans réindexer le document entier (embedding compris). Une
réindexation complète concurrente, partie d'une lecture antérieure, peut écraser le bloc. La
projection suivante le réécrit, car elle compare l'état du produit à `ref-rappelconso` et non à un
hash mémorisé sur le produit.

## 5. Pipeline

1. **Provider (à créer)** : `RappelConsoProvider implements ReferenceDatasetProvider`, dans
   `services/reference-datasets`.
   - `resolveDownloadUrls()` : URL d'export CSV (`delimiter=%3B`), avec `Accept-Encoding: gzip`.
   - Téléchargement : `RemoteFileCachingService.downloadToTmpFile`.
   - `read(Path)` : Jackson `CsvMapper` (séparateur `;`, BOM en tête), regroupement par
     `numero_fiche`, règles des sections 3.2 et 4.2.
   - `sourceRecordId` = `numero_fiche`, `recordUrl` = `lien_vers_la_fiche_rappel`,
     `sourceUpdatedAt` = `date_publication` (le jeu n'a pas de date de modification),
     `state` = `ACTIVE` ou `ENDED`, `payload` = champs de la section 4.1 et `lotsByGtin`.
     `attributes` reste vide.
2. **Import** : `ReferenceIndexService` écrit `ref-rappelconso-<horodatage>` (12 619
   enregistrements), `min-rows-ratio: 0.95` (le jeu ne fait que croître), bascule l'alias. Le
   rapport donne les lignes lues, les GTIN rejetés par motif (zéro, court, factice, diffusion
   restreinte), les fiches nouvelles, modifiées et disparues.
3. **Projection** : `ReferenceProjectionService` itère `ref-rappelconso` : 28 `multiGetForAggregation`
   de 500 GTIN, alimentaire compris (le coût est négligeable et l'agrégation reste maximale). Pour
   chaque produit trouvé, fusion des entrées `ProductRecall` par `(source, recallId)`, écriture
   partielle du bloc et de `sourceAttributions[rappelconso]` (P0-4). Pas de `DataFragment` ni de
   `StandardAggregator` (P0-6).
4. **Retraits** : diff avec la version précédente de l'index, puis suppression explicite des
   entrées (section 4.2).
5. **Scores** : aucun. Un rappel ne modifie ni les scores ni l'ImpactScore.
6. **front-api** : `ProductRecallDto` (à créer), nouveau composant `ProductDtoComponent.recalls`,
   champ `recalls` de `ProductDto`, rempli par `ProductMappingService` (méthode `mapRecalls`, à
   créer). Le produit est lu par `getByIdWithoutEmbedding` : le bloc est inclus sans changement du
   filtre de source. Client régénéré dans `frontend/shared/api-client`.
7. **Front, bandeau "rappel en cours"** :
   - `useProductVigilance` : nouvelle clé `'recall'` dans `VigilanceAlert`, couleur `error`,
     placée en tête de liste. `ProductVigilanceTeaser` l'affiche alors sans autre changement.
   - `ProductVigilanceSection.vue` : une carte par rappel actif (titre, date, risques, conduite à
     tenir, lots, lien "Voir la fiche RappelConso"), puis une ligne d'historique pour les rappels
     terminés.
   - `ProductPage.vue` : `showVigilanceSection` recopie les conditions du composable. Ajouter le
     rappel, ou mieux, dériver la condition de `hasAlerts`.
   - Retrait : un rappel s'affiche comme actif si `active` est vrai et si `endsAt` est vide ou
     dans le futur, calculé à l'affichage. Le bandeau disparaît donc le jour de la fin, même si
     l'import a échoué. Les rappels terminés restent en historique, sans bandeau.
   - Libellé : "Rappel en cours : certains lots" quand `lots` ne vaut pas "tous les lots", avec
     les lots détaillés dans la carte.
   - i18n `product.vigilance.recall.*` dans `frontend/i18n/locales/{fr-FR,en-US}.json`.
8. **Export ouvert** : colonnes `recall_active` (`true` si au moins un rappel actif, toutes
   sources) et `recall_urls` (URL des fiches, actives d'abord, séparées par `|`) dans
   `OpenDataService.GTIN_HEADER`. Il faut aussi `recalls` dans `GTIN_REQUIRED_FIELDS`, le
   remplissage dans `toGtinEntry` et les libellés `opendata.datasets.gtin.columns.*`. La fiche
   candidate proposait `fr_recall_active` et `fr_recall_url`. Des noms sans préfixe servent aussi
   à Safety Gate (04) : une seule famille de colonnes pour le bloc partagé.

## 6. Planification et volumétrie

- Cron quotidien après 21 h UTC, par exemple `0 30 22 * * *`, à recaler une fois le rythme de
  `data_processed` relevé.
- Import : 31,7 Mo de CSV (4,6 Mo transférés), 18 862 lignes, quelques secondes. Le format
  `parquet` est plus compact mais demanderait une bibliothèque de lecture : CSV d'abord.
- Projection : 28 `multiGet`, puis des écritures partielles pour les seuls produits rattachés.
- Volume de fond : de 1 700 à 2 900 fiches à GTIN publiées par an (2021 à 2026).

## 7. Sourcing transparent

- `open4goods-config/datasources/rappelconso.yml` : `name: "RappelConso"`, `portalUrl:
  "https://rappel.conso.gouv.fr/"`, `dataPortalUrl` = page du jeu sur data.economie.gouv.fr,
  `license: "Licence Ouverte 2.0"`, `attribution: "RappelConso (DGCCRF, DGAL, DGEC, DGPR), via
  data.economie.gouv.fr"`.
- Sur la fiche : "Source : RappelConso, fiche n° 2026-11-0048, publiée le ...", lien vers la
  fiche, date de récupération (`SourceAttribution.retrievedAt`). Photos : lien vers
  `rappel.conso.gouv.fr`, jamais copiées.

## 8. Tests et critères d'acceptation

- Test unitaire du provider sur une fixture d'environ 30 lignes tirées du vrai fichier : ligne
  alimentaire et non alimentaire, `gtin` à `0`, entier court, UPC à 11 chiffres (zéro perdu),
  suite factice `1111111111649`, code commençant par 2, fiche à plusieurs GTIN et lots, fin vide
  récente et ancienne, fin antérieure à la publication, fiche en version 2.
- Test de la règle `active` avec une horloge fixe.
- Test d'intégration (Testcontainers ES avec le vrai `product-mappings.json`, comme
  `ProductRepositoryPartialUpdateIT`) : un document portant `recalls` est accepté sous
  `strict` ; import, projection sur trois produits, date de fin dépassée (`active = false`),
  fiche disparue (entrée supprimée).
- Front : nouveau cas dans `useProductVigilance.spec.ts` (rappel actif, rappel terminé, `endsAt`
  dépassé alors que `active` vaut encore vrai).
- Critères en beta : rapport de rattachement publié (trouvés, avec offre, par verticale, actifs),
  zéro rattachement sur un code de diffusion restreinte, aucun bandeau sur un rappel dont la fin
  est passée.

## 9. Découpage

0. Mesure du recouvrement, section 3.3 : décision go ou non (XS).
1. `RappelConsoProvider` et import dans `ref-rappelconso`, utiles même sans go (S).
2. P0-6 : `ProductRecall`, `Product.recalls`, mapping, `ProductPartialUpdateField.RECALLS`, test
   sous `strict` (S).
3. Projection des rappels et retraits (S).
4. front-api : `ProductRecallDto` et composant `recalls` (S).
5. Front : vigilance, carte, bandeau, i18n (S).
6. Colonnes d'export ouvert (XS).

## 10. Risques, challenges, questions

- **77 % d'alimentaire, et zéro produit alimentaire chez nous.** Les chiffres de la fiche
  candidate sont justes en lignes (18 862 lignes, 14 536 alimentaires, 457 appareils électriques
  dont 419 électroménager). Mais "419 lignes électroménager" ne veut pas dire 419 produits de nos
  verticales : ce sont surtout des aspirateurs balais, sèche-serviettes, bouilloires, luminaires.
  Le classement P1 du README ne tient que si le ticket 0 trouve des produits non classés en
  nombre.
- **Faux positifs** : codes de diffusion restreinte et suites factices (section 3.2). Un bandeau
  de rappel sur le mauvais produit est pire que pas de bandeau.
- **Portée par lot** : beaucoup de rappels ne visent que certains lots ou dates. Le bandeau doit
  le dire et renvoyer à la fiche. "Ce produit est dangereux" serait faux.
- **Signal par marque** (proposition) : le jeu compagnon `rappelconso-v2-gtin-espaces` couvre les
  19 207 fiches, y compris sans GTIN. Un compte de rappels par marque sur trois ans, résolu par
  `BrandService.resolve`, toucherait nos verticales sans dépendre du GTIN. Exemples hors
  alimentaire : Philips (38 lignes), Rowenta (30), Brandt et ses marques (micro-ondes 2025),
  Hisense, Electrolux. C'est une donnée de page marque, à cadrer avec le chantier marques ; pas
  un score produit.
- **Manques du socle** :
  - un rappel terminé n'est pas une donnée à retirer (section 4.2) ;
  - `product-mappings.json` est en `strict` : tout nouveau champ de `Product` doit être mappé,
    `recalls` comme `sourceAttributions` (P0-4) ;
  - `ProductRecall` gagne neuf champs (section 4.1) et des listes à la place de `riskLabel` et
    `measures` ;
  - `ProductPartialUpdateField` doit connaître `recalls` ;
  - `ReferenceRecord.sourceUpdatedAt` n'a pas d'équivalent ici : on utilise `date_publication`
    et `numero_version`.
- **Construire P0-6 une seule fois** : si 04 (Safety Gate) passe avant 03, le bloc et le front
  s'y construisent ; 03 n'apporte alors que son provider.
- **Questions au propriétaire** :
  1. Seuils du ticket 0 (100 produits non alimentaires avec offre, ou 10 rappels actifs) : bons
     ordres de grandeur, ou faut-il livrer le bandeau quel que soit le volume, par principe de
     sécurité ?
  2. Rappel sans date de fin : actif 90 jours après publication (90e centile mesuré), puis
     "date de fin non communiquée". D'accord ?
  3. Afficher l'historique des rappels terminés sur la fiche, et un compte de rappels par
     marque ?
