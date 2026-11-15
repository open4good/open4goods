---
title: "Plan d'intégration : EU Ecolabel (catalogue ECAT)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : EU Ecolabel (catalogue ECAT)

> Socle commun : [README](README.md). Fiche source :
> section 4.6 de l'inventaire interne des sources candidates (non publié).
> Mesures du 2026-11-08 sur l'export `most-recent-export.csv` du jour (`extract_date` =
> 2026-11-08, `Last-Modified` 2026-11-08 03:27 GMT).

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | `eu-ecolabel` |
| Mode | R (référentiel compact), appariement `GTIN_ONLY` |
| Priorité | P3 (inchangée) : aucune verticale touchée, valeur de masse seulement |
| Effort | S : 1 à 2 jours une fois le socle P0-5 livré |
| Verticales | aucune des 7 verticales (0 ligne) ; masse hors verticale (peintures, papier, mobilier, détergents, textile) |
| Volume | 91 497 lignes, 3 409 licences, 20 586 GTIN distincts valides ; environ 6 % déjà présents dans notre catalogue (sondage) |
| Dépend de | P0-4 (attribution), P0-5 (socle). P0-3 inutile (aucune source concurrente sur cet attribut) |

## 1. Apport

L'EU Ecolabel est le label écologique officiel de l'Union (règlement (CE) n° 66/2010, **non
vérifié** sur EUR-Lex). Le catalogue ECAT recense les produits et services sous licence en cours
de validité. Pour chaque ligne, la source donne :

- le numéro de licence (`licence_number`, par exemple `PT/004/004`) ;
- le groupe de produits (`group_name`) et la décision de critères (`decision`) ;
- un code produit typé (`code_type`, `code_value`), EAN13 dans 30 % des lignes ;
- le nom commercial (`product_or_service_name`) ;
- la date d'expiration de la licence (`expiration_date`) ;
- le titulaire (`company_name`, `company_country`, `vat_number`).

Ce que nudger y gagne :

- un **label environnemental officiel et vérifiable** sur des produits du quotidien, pour lesquels
  nous n'avons aujourd'hui aucune donnée d'impact (les produits hors verticale ne sont ni complétés
  ni scorés) ;
- une donnée datée (date d'expiration de la licence), donc retirable proprement ;
- un premier cas d'usage du socle pour une source **sans aucune verticale**, ce qui teste la
  projection hors `exportVerticalWithValidDate`.

Ce que la source n'apporte pas : ni marque, ni modèle, ni critère chiffré (la licence atteste le
respect de critères, sans valeur mesurée), ni image.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Jeu | data.europa.eu `eu-ecolabel-products`, "EU Ecolabel Products", éditeur DG ENV | vérifié |
| URL | `https://publicstorage.data.env.service.ec.europa.eu/ecolabel/exports/most-recent-export.csv` (stable, nom fixe) | vérifié |
| Format | CSV UTF-8 **avec BOM**, séparateur point-virgule, 12 colonnes, une ligne d'en-tête | vérifié |
| Taille | 17 600 175 octets (`Content-Length`) ; 91 818 lignes physiques, en-tête compris, pour 91 497 enregistrements (champs multilignes) | vérifié |
| Fréquence | métadonnée data.europa.eu : `monthly` ; fichier observé régénéré le jour même (03:27 GMT, `extract_date` du jour) | une seule observation : rythme quotidien **non vérifié** |
| Authentification | aucune | vérifié |
| Cache | `Cache-Control: public, max-age=300`, `ETag` et `Content-MD5` fournis | vérifié |
| Historique | aucun export daté accessible (listage du conteneur Azure refusé, `ResourceNotFound`) | vérifié |
| Fiche publique par licence | aucune URL exploitable : la page ECAT `http://ec.europa.eu/ecat/` redirige vers une page 403 | vérifié |
| Licence | "Creative Commons Attribution 4.0 International" (métadonnée de la distribution) | vérifié (champ d'attribution seulement) |

Colonnes : `product_or_service`, `licence_number`, `group_name`, `code_type`, `code_value`,
`product_or_service_name`, `decision`, `expiration_date`, `company_name`, `company_country`,
`vat_number`, `extract_date`.

Répartition utile :

| Mesure | Valeur |
|---|---|
| `product_or_service` | `PRODUCT` 90 464, `SERVICE` 1 033 (hébergement touristique) |
| Groupes de produits | 25 |
| Titulaires distincts | 2 459 |
| Pays des titulaires | Italie 18 599, France 14 946, Portugal 8 853, Pologne 8 778, Danemark 7 165 |
| `expiration_date` | de 2026-12-31 à 2032-12-31 ; aucune licence expirée dans l'export |

Complément possible, hors de ce plan : ADEME publie sur data.ademe.fr le jeu "Ecolabel européen -
Produits français certifiés par un organisme national" (4 282 lignes, Licence Ouverte 2.0), avec
`marque_produit` et `reference_produit` mais **sans GTIN** (schéma lu le 2026-11-08).

## 3. Identifiants et appariement

Mesure avec contrôle de la somme GTIN, sur tous les `code_type` :

| `code_type` | Lignes | `code_value` numérique (8 à 14 chiffres) | GTIN valides |
|---|---|---|---|
| (vide) | 54 085 | 0 | 0 |
| `EAN13` | 27 596 | 27 595 | **27 595** |
| `Internal Producer ID` | 5 338 | 148 | 17 |
| `Other` | 4 204 | 371 | 158 |
| `GTIN14` | 257 | 257 | 257 |
| `ITF` | 13 | 6 | 4 |
| `GTIN8` | 3 | 3 | 3 |
| `GTIN12` | 1 | 1 | 1 |
| **Total** | **91 497** | 28 381 | **28 035 (30,6 %)** |

- Le chiffre du document candidat (27 596 EAN13) est confirmé à une ligne près : la seule valeur
  `EAN13` invalide est un numéro de licence saisi dans le mauvais champ (`ES-MD/052/00003`).
- **20 586 GTIN distincts** après normalisation sur 13 chiffres. Les 28 035 lignes valides se
  replient fortement : 860 GTIN apparaissent sur plusieurs lignes, jusqu'à **960 lignes pour un
  même GTIN** (`5908441104576`, chaise de bureau Profim déclinée en variantes qui partagent un
  code). 79 GTIN relèvent de plusieurs licences.
- Préfixes GS1 des GTIN distincts : Italie (800-839) 3 413, Espagne (84x) 1 758, France (300-379)
  1 427, Pologne (590) 1 062, Allemagne (400-440) 1 037, États-Unis/Canada (000-139) 327.
- Les 63 462 lignes sans GTIN valide (dont 1 033 services) **ne peuvent pas** être rapprochées
  par marque et modèle : `company_name` est une raison sociale de titulaire, souvent un
  façonnier ou un distributeur, et il n'y a pas de colonne modèle. `ReferenceMatcher` n'a rien
  à exploiter ; ces lignes restent consultables dans `ref-eu-ecolabel`.

Règles du provider :

1. Ne garder que `product_or_service = PRODUCT`.
2. `code_value` débarrassé des espaces et tirets ; GTIN retenu si la somme est valide, **quel que
   soit** `code_type` (on récupère ainsi 175 GTIN déclarés `Other` ou `Internal Producer ID`).
   Normalisation par `BarcodeValidationService.toProductId`, qui complète à 13 chiffres.
3. **Un enregistrement par GTIN** : les lignes d'un même GTIN sont fusionnées. On garde la liste
   des licences, des groupes et des noms (bornée à 20 noms), et la date d'expiration la plus
   lointaine.
4. Les lignes sans GTIN sont importées dans `ref-eu-ecolabel` avec `gtins = {}` (agrégation
   maximale, recherche future par nom), mais ne sont jamais projetées.
5. Projection : `match: GTIN_ONLY`. Pas de passe marque/modèle.

Recouvrement avec notre catalogue (sondage API publique, 2026-11-08) : 100 GTIN tirés au hasard
parmi les 20 586, interrogés sur `https://nudger.fr/api/products/{gtin}` à une requête par
seconde.

| Résultat | Nombre sur 100 |
|---|---|
| Produit existant (HTTP 200) | **6** |
| dont `excluded: true` | 6 |
| dont `offersCount > 0` | 4 |
| dont rattaché à une verticale | 0 |
| Absent (HTTP 404) | 94 |

Extrapolation prudente : 6 % de 20 586, soit environ 1 200 produits (intervalle de Wilson à 95 % :
3 % à 12 %, soit 600 à 2 600). Sources marchandes vues sur ces produits : darty.com, fnac.com,
cdiscount.com, manomano.fr. Mesure exacte à lancer sur l'ES de prod : `multiGet` des 20 586 GTIN
(ids numériques, `_source` réduit à `vertical`, `offersCount`, `excluded`), en 42 lots de 500.

## 4. Modèle cible

Attribut nouveau dans `verticals/src/main/resources/attributes/` :

| Clé | Source | Type | Score |
|---|---|---|---|
| `EU_ECOLABEL` | présence d'une licence valide | BOOLEAN (comme `FAST_FREEZE.yml`), valeur `TRUE` | `asScore: false` |

- Déclaration dans `verticals/src/main/resources/verticals/_default.yml`, sous
  `attributesConfig.configs`. Vérifié dans le code : un produit sans verticale est agrégé avec la
  configuration par défaut (`StandardAggregator` appelle
  `VerticalsConfigService.getConfigByIdOrDefault`), et `mergeDefaults` fusionne les `configs` du
  défaut dans chaque verticale (`mergeByKey`). L'attribut est donc indexé partout, verticales
  comprises, sans toucher aux sept YAML.
- Synonymes : le provider émet directement `EU_ECOLABEL` ; `synonyms: { eu-ecolabel:
  ["EU_ECOLABEL"] }`.
- Les autres champs (numéros de licence, groupe, décision, expiration, titulaire) ne deviennent
  pas des attributs : ils vont dans `ReferenceRecord.payload` et restent dans `ref-eu-ecolabel`.
  Pour les afficher sur la fiche, voir la proposition de bloc `labels` en section 10.

ImpactScore : **pas de participation**. Aucune des sept verticales n'a de produit labellisé
(0 ligne mesurée), donc un critère `EU_ECOLABEL` dans `impactscores/<vertical>.yml` serait vide.
À rouvrir seulement si une verticale "produits ménagers" ou "peintures" apparaît.

Champs `Product` : aucun nouveau hors `sourceAttributions` (P0-4). Mapping ES : rien à ajouter si
l'attribut passe par `attributes.indexed`. Un filtre "produits labellisés" hors verticale
demanderait le mapping du bloc `labels` (section 10).

## 5. Pipeline

1. **Provider** (à créer) : `EuEcolabelProvider implements ReferenceDatasetProvider`, dans
   `services/reference-datasets`.
   - `resolveDownloadUrls()` : URL fixe, en configuration.
   - Téléchargement : `RemoteFileCachingService.downloadToTmpFile`. Requête conditionnelle sur
     l'`ETag` si le socle la permet ; sinon le hash de contenu suffit.
   - `read(Path)` : Jackson `CsvMapper`, séparateur `;`, BOM retiré, champs multilignes acceptés.
     Fusion par GTIN (section 3).
   - `sourceRecordId` : la source n'a pas d'identifiant de ligne. Proposition : `gtin` pour un
     enregistrement fusionné ; `licence_number + ":" + sha1(product_or_service_name + code_value)`
     pour une ligne sans GTIN.
   - `recordUrl` : aucune fiche par licence ; repli sur la page du jeu
     `https://data.europa.eu/data/datasets/eu-ecolabel-products`.
   - `sourceUpdatedAt` : `extract_date`. `state` : `ENDED` si `expiration_date` est passée,
     sinon `ACTIVE`.
2. **Import** : `ReferenceIndexService` écrit `ref-eu-ecolabel-<horodatage>`, contrôle
   `min-rows-ratio` (0,8) et bascule l'alias. Le rapport donne les lignes lues, les GTIN valides,
   les GTIN disparus et les licences disparues.
3. **Projection** : `ReferenceProjectionService`, 42 `multiGetForAggregation` de 500 GTIN. Pour
   chaque produit trouvé : `DataFragment` synthétique avec `EU_ECOLABEL = TRUE`, agrégation,
   attribution, hash. Pour un GTIN disparu de l'export ou passé à `ENDED` :
   `product.removeDatasourceData("eu-ecolabel")`, ce qui retire l'attribut et l'attribution.
4. **Scores** : aucun.
5. **Export ouvert** : colonne `eu_ecolabel` dans `OpenDataService.GTIN_HEADER`, valeur = numéros
   de licence séparés par `|` (vide sinon), avec les quatre modifications habituelles (en-tête,
   champs requis, `toGtinEntry`, libellés i18n `frontend/i18n/locales/{fr-FR,en-US}.json`).
6. **Front** : pastille "EU Ecolabel" sur la fiche, avec le numéro de licence, la date de
   validité et le lien vers la page du jeu. Composant à identifier dans
   `frontend/app/components/product/` **(non vérifié)**. Les produits concernés étant `excluded`,
   vérifier d'abord que leurs fiches sont servies et indexables : sinon la pastille ne sera vue
   par personne (question 1, section 10).

## 6. Planification et volumétrie

- Cron `0 15 5 * * *` (après la régénération observée vers 03:30 GMT). Si l'`ETag` n'a pas
  changé, l'import s'arrête là.
- Import : 91 497 lignes, 17,6 Mo, quelques secondes. Index `ref-eu-ecolabel` : environ 83 000
  documents (20 586 fusionnés par GTIN + 62 429 produits sans GTIN).
- Projection : 42 `multiGet`, puis environ 1 200 produits à réindexer au premier passage
  (estimation par sondage), presque rien ensuite.

## 7. Sourcing transparent

- `open4goods-config/datasources/eu-ecolabel.yml` : `name: "EU Ecolabel (Commission
  européenne)"`, `portalUrl` = page data.europa.eu du jeu, `license: "CC BY 4.0"`,
  `licenseUrl: "https://creativecommons.org/licenses/by/4.0/"`, `attribution: "Catalogue EU
  Ecolabel, Commission européenne (DG ENV), via data.europa.eu"`.
- `SourceAttribution` par produit : `sourceRecordId` (GTIN), `recordUrl` (page du jeu, faute de
  mieux), `sourceUpdatedAt` (`extract_date`), `retrievedAt`. Le numéro de licence est affiché à
  côté : c'est lui qui permet à l'utilisateur de vérifier le label.

## 8. Tests et critères d'acceptation

- Test unitaire du provider sur une fixture de 20 lignes tirées du vrai fichier : BOM, EAN13
  valide, `ES-MD/052/00003` en `code_value`, GTIN valide déclaré `Other`, `GTIN14`, `ITF`, ligne
  `SERVICE` à ignorer, GTIN répété sur plusieurs lignes et deux licences, nom contenant des
  guillemets typographiques et un point-virgule entre guillemets.
- Test de fusion : 960 lignes d'un même GTIN donnent un seul enregistrement et au plus 20 noms.
- Test d'intégration (Testcontainers ES, réutilisé du plan 01) : import, projection sur deux
  produits sans verticale, l'attribut apparaît dans `attributes.indexed` ; retrait après
  disparition du GTIN.
- Critères en beta :
  - 20 586 GTIN distincts importés (plus ou moins 2 % selon le jour) ;
  - taux de rattachement publié dans le rapport, attendu entre 3 % et 12 % ;
  - nombre de produits créés publié dans le rapport (`CREATE` par défaut depuis le 2026-11-09),
    et aucun produit créé sur un code rejeté par P0-9.

## 9. Découpage

1. `EuEcolabelProvider`, fusion par GTIN et tests (S).
2. Attribut `EU_ECOLABEL` dans `_default.yml`, fichier `datasources/eu-ecolabel.yml` (S).
3. Colonne d'export ouvert et pastille front (S).
4. Mesure du recouvrement réel sur l'ES de prod (`multiGet`) et décision Q1 (S).

## 10. Risques, challenges, questions

- **Challenge de la valeur** : environ 1 200 produits touchés, tous hors verticale, `excluded`,
  sans score. Le label ne change ni un classement ni un ImpactScore. La source ne vaut que si les
  fiches hors verticale sont réellement servies et visibles. Sinon, l'intégration produit une
  donnée que personne ne voit.
- **Q1 (CREATE)**, décidée le 2026-11-09 en faveur de la création ; ce qui suit reste la liste
  des risques, traités par P0-11 du README. 94 % des GTIN du catalogue sont inconnus chez nous. En mode `CREATE`, on
  créerait environ 19 000 fiches dont la seule information est un nom commercial souvent
  technique (`PRODUITS : Vinimat SF wall and ceiling paint BASE : Base W...`). Pas de marque, pas
  d'image, pas de prix : c'est le cas d'école du "thin content". **Recommandation :
  `ENRICH_EXISTING`, sans exception pour cette source.** Si le propriétaire veut quand même
  créer, limiter aux préfixes GS1 français (1 427 GTIN), qui ont le plus de chances d'avoir une
  offre plus tard.
- **Proposition de bloc `labels` sur `Product`** (manque du socle) : un attribut BOOLEAN ne porte ni
  le numéro de licence ni la date d'expiration. `ProductRecall` (P0-6) couvre les rappels. Il
  faudrait un équivalent pour les labels et certifications : `List<ProductLabel> Product.labels` (à
  créer), avec `ProductLabel(source, labelId, licenceNumber, url, validUntil, active)` (à créer), et
  un mapping ES `labels.source` (keyword) pour filtrer. Il servirait aussi à ENERGY STAR (plan 14),
  à un éventuel TCO Certified ou EPEAT, et aux labels marchands. Sans ce bloc, le numéro de licence
  n'est disponible que dans `ref-eu-ecolabel`.
- **GTIN partagés entre variantes** : un GTIN porté par 960 lignes signifie que le titulaire a mis
  le même code sur toute une gamme. Le label reste vrai pour le produit, mais le nom affiché ne
  doit pas venir de l'Ecolabel. Le provider n'émet donc aucun nom vers l'agrégation des noms.
- **Fréquence** : métadonnée "monthly", fichier régénéré le jour de la mesure. Un import
  quotidien conditionnel ne coûte rien ; à confirmer par deux observations à J+1 et J+7.
- **Questions au propriétaire** :
  1. Les fiches `excluded` (hors verticale) sont-elles servies et indexées par le front ? Sinon,
     faut-il reporter ce plan derrière une décision sur les pages hors verticale ?
  2. Valides-tu le bloc `labels` sur `Product` comme prérequis transverse (P0-7), plutôt qu'un
     attribut BOOLEAN seul ?
  3. Tranchée le 2026-11-09 : création (README, Q1).
