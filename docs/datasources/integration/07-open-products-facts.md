---
title: "Plan d'intégration : Open Products Facts"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : Open Products Facts

> Socle commun : [README](README.md). Fiche source :
> section 4.9 de l'inventaire interne des sources candidates (non publié).
> Mesures du 2026-11-08 sur les exports nocturnes du 2026-11-08 (générés vers 04 h UTC),
> téléchargés en entier.

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | `open-products-facts` |
| Mode | **R** et non R+ : 48 924 lignes, 41 Mo compressés. Appariement `GTIN_ONLY` |
| Priorité | P3 (aligné au sommaire), à remonter si la mesure de recouvrement (section 3) dépasse 5 000 produits |
| Effort | M : provider S (1 à 2 jours, socle livré), attribution par image et retrait des ressources M (2 à 3 jours) |
| Verticales | marginal : `smartphones` (162 fiches hors imports), `tv` (4), `washing-machine` (4). Le gros du volume est hors verticale (entretien, hygiène, papeterie, bricolage) |
| Volume | 48 924 fiches, 44 056 GTIN exploitables, dont 35 113 hors imports déjà couverts par les plans 02 et 06 |
| Dépend de | P0-1 (bloquant, section 7), P0-4, P0-5 ; propositions de socle S1 à S3 (section 10) |

## 1. Apport

Open Products Facts (OPF) est la base collaborative non alimentaire de la famille Open Food
Facts. Apport mesuré :

- des noms : 26 231 GTIN exploitables ont un nom, hors imports réglementaires ; une marque est
  renseignée sur 22 480 fiches (toutes origines) ;
- une photo de face sélectionnée pour 29 635 GTIN exploitables, dont 10 486 marqués
  `en:france` dans `countries_tags` ;
- une taxonomie (`categories_tags`) sur 25 045 fiches, hétérogène : étiquettes `en:`, `fr:` et
  `de:` mélangées, par exemple `fr:hard-covering-products` ou `de:haushalt` ;
- une contenance (`quantity`, texte libre) sur 13 025 fiches, normalisée
  (`product_quantity`) sur 7 898.

Ce que la source n'apporte pas :

- **Une part notable est une copie de sources déjà planifiées.** 7 452 fiches viennent de
  l'import `producer-eu-ecolabel-test` (EU Ecolabel, plan 06 : revêtements durs 4 501, papier
  1 562, peintures 402). 1 498 viennent de `database-fr-datagouv-indice-rep` (indice de
  réparabilité, plan 02 : 1 025 portables, 308 lave-vaisselle, 83 lave-linge, 71 aspirateurs,
  6 smartphones, 5 divers). L'indice n'y figure que sous forme d'étiquette
  (`en:repairability-index-9-3-france`). Ces valeurs doivent venir de la source primaire,
  jamais d'OPF : sinon la même valeur voterait deux fois dans `SourcableAttribute`.
- Aucune caractéristique technique structurée, à la différence d'Icecat ou d'EPREL.
- Les champs nutritionnels (4 913 fiches) et les scores alimentaires (Nutri-Score, NOVA) sont
  des résidus de fiches mal classées : ils sont ignorés dès l'import.

L'intérêt réel tient donc aux images et aux noms des produits hors verticale. OPF sert aussi
de cas d'école du socle : une source communautaire dont les médias portent une licence par
image.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Export JSONL | `https://static.openproductsfacts.org/data/openproductsfacts-products.jsonl.gz`, 41 005 198 octets, 48 924 lignes, `Last-Modified` 2026-11-08 04:02 UTC | vérifié |
| Export CSV | `https://static.openproductsfacts.org/data/en.openproductsfacts.org.products.csv.gz`, 6 387 502 octets, tabulation, 187 colonnes (dont environ 130 nutritionnelles), 48 911 lignes | vérifié |
| Dump MongoDB | `https://static.openproductsfacts.org/data/openproductsfacts-mongodbdump.gz`, 46 869 918 octets | vérifié |
| Deltas | `https://static.openproductsfacts.org/data/delta/index.txt` : 13 fichiers quotidiens. La page précise : "the delta files cannot tell you about deleted products" | vérifié |
| RDF | `https://world.openproductsfacts.org/data/en.openproductsfacts.org.products.rdf.gz`, 2,1 Mo, **figé au 2026-10-21** | vérifié |
| Parquet | **aucun pour OPF**. La page de données pointe vers `openfoodfacts/product-database/food.parquet` (alimentaire, 8 Go). Le dépôt Hugging Face ne contient que `food.parquet`, `beauty.parquet` et `petfood.parquet` | vérifié |
| Fréquence | nocturne, vers 04 h UTC | vérifié (dates HTTP) |
| Authentification | aucune | vérifié |
| API produit | `https://world.openproductsfacts.org/api/v2/product/{code}.json` ; inutile ici, le dump complet suffit | vérifié |
| Page publique | `https://world.openproductsfacts.org/product/{code}` (200, redirection vers un slug) | vérifié |
| Licence | ODbL (base), DbCL (contenus), CC BY-SA (images) | champ d'attribution seulement |

Contradiction relevée : la page de données annonce pour le CSV "~ 0.9 Gb, uncompressed: ~ 9 Gb".
Ce texte est recopié d'Open Food Facts ; la taille mesurée est de 6,4 Mo.

Choix : import complet du JSONL chaque jour. Le CSV perd la structure des images : il ne
garde que `image_url`, dont le numéro de révision change. Il casse aussi le chemin des images
de 5 codes courts (`/images/products/invalid/...`). Les deltas ne signalent pas les
suppressions et n'ont pas d'intérêt pour un fichier complet de 41 Mo.

Champs utiles du JSONL : `code`, `product_name`, `product_name_fr`, `product_name_en`, `brands`,
`brands_tags`, `categories_tags`, `countries_tags`, `quantity`, `product_quantity`,
`product_quantity_unit`, `labels_tags`, `images.selected`, `images.uploaded`,
`data_sources_tags`, `created_t`, `last_modified_t`. Champs présents mais écartés : `nutriments`,
`ingredients_*`, `nova_*`, `nutriscore_*`, `environmental_score_*`, `pnns_*`, `editors_tags`,
`informers_tags`, `photographers_tags`, `creator`.

## 3. Identifiants et appariement

Mesure avec contrôle de la somme GTIN, sur les 48 924 lignes (codes uniques, aucun doublon) :

| Catégorie de `code` | Lignes |
|---|---|
| GTIN-13 valides, hors ISBN et circulation restreinte | 42 726 |
| GTIN-8 valides | 1 192 |
| GTIN-14 valides | 138 |
| **GTIN exploitables** | **44 056 (90,0 %)** |
| ISBN valides (préfixe 978 ou 979) | 3 050 |
| Circulation restreinte valides (préfixes 02, 04, 2) | 575 |
| Longueur GTIN mais somme fausse | 992 (506 à 8 chiffres, 379 à 13, 107 à 14) |
| Autre longueur (15 à 40 chiffres) | 250 |
| Vide | 1 |

Aucun code à 12 chiffres : OPF complète les UPC à 13 chiffres par un zéro de tête.

Comparaison avec le document candidat (API de recherche, même jour) : 48 998 produits annoncés,
48 924 dans le dump. Les 163 smartphones (`en:smartphones`), 4 TV et 5 lave-linge sont
confirmés. Mais l'étiquette `en:dishwashers` est polluée par des détergents, et les vrais
appareils de l'indice de réparabilité portent des étiquettes `fr:` que la recherche par
étiquette anglaise ne voit pas.

Répartition sur nos verticales (étiquettes `categories_tags`) :

| Verticale | Étiquette | Fiches | GTIN valides | Remarque |
|---|---|---|---|---|
| `smartphones` | `en:smartphones` (+ `en:mobile-phones`) | 168 (163 sous `en:smartphones`) | 155 | 1 seule vient de l'import indice-rep |
| `tv` | `en:televisions` | 4 | 4 | |
| `washing-machine` | `en:washing-machines` | 5 | 4 | 83 autres sous `fr:lave-linge-top` et `fr:lave-linge-hublot`, toutes issues de l'import indice-rep |
| `dishwasher` | `en:dishwashers` | 22 | 22 | **ce sont des détergents** (Fairy, Finish, liquides vaisselle). Les vrais lave-vaisselle sont les 308 fiches `fr:lave-vaisselle`, toutes issues de l'import indice-rep |
| `refrigerator` | `en:refrigerators` | 1 | 1 | |
| `air-conditioner` | `en:air-conditioners` | 4 | 4 | |
| `oven` | `en:ovens` | 0 | 0 | |

Règles :

1. `gtins = {code}` si le code est un GTIN exploitable, normalisé par
   `BarcodeValidationService`.
2. Les ISBN restent dans `ref-open-products-facts`, marqués comme tels, projetés comme les
   autres depuis la décision du 2026-11-09 (Q2 du README : données livres intégrées). Les codes de circulation restreinte et les codes
   invalides sont rejetés et comptés dans le rapport d'import.
3. `GTIN_ONLY` : OPF n'a pas de champ modèle fiable. Le nom commercial ("Ordinateur portable -
   STRIX-G16-G614PM-RV066W") contient parfois la référence, mais seulement sur les fiches
   importées, déjà couvertes par le plan 02.
4. Pas de dédoublonnage nécessaire : `code` est unique dans le dump.

Recouvrement avec nos produits : **non mesurable** sans accès à l'Elasticsearch de prod.
Mesure proposée, à lancer avant tout développement de projection :
`POST /reference-datasets/open-products-facts/project?dryRun=true` une fois le socle livré. À
défaut, un script qui envoie les 44 056 GTIN par lots de 500 à `_mget` sur l'index produits et
compte : produits trouvés, trouvés avec une verticale, trouvés avec `offersCount > 0`, trouvés
sans aucune ressource image.

## 4. Modèle cible

| Donnée OPF | Projetée | Cible | Raison |
|---|---|---|---|
| `product_name`, `product_name_fr` | oui | `DataFragment.addName`, donc `Product.offerNames` via `NamesAggregationService` | noms alternatifs, utiles hors verticale |
| `brands` (première valeur) | oui | `referentielAttributes` BRAND | résolution standard de la marque |
| `images.selected.front` | oui, une image par produit | `DataFragment.addResource(Resource)`, puis `Product.resources` via `MediaAggregationService` | section 5 |
| `categories_tags` | **non** en phase 1 | reste dans `ref-open-products-facts` | risque de verticale (section 10) |
| `quantity`, `product_quantity` | **non**, et jamais en `WEIGHT` | reste dans `ref-open-products-facts` ; option `NET_CONTENT` (à créer), sans score | contenance, pas poids |
| `labels_tags` réparabilité et `en:eu-ecolabel` | non | `ref-open-products-facts` | copies des plans 02 et 06 |
| `countries_tags`, `stores_tags`, `origins`, `manufacturing_places` | non | `ref-open-products-facts` | renseignés sur moins de 12 % des fiches, qualité inégale |
| nutrition, ingrédients, scores alimentaires | non, ni dans `ref-` | ignorés à la lecture | hors sujet |

Pourquoi pas de poids : la mission demandait d'étudier le poids. `quantity` est une contenance
nette ("820 ml", "1 l", "385g"), pas le poids de l'objet. `WEIGHT.yml` est `asScore: true`,
`participateInACV: [EXTRACTION, TRANSPORTATION]`, avec `synonyms.all: ["POIDS", "WEIGHT"]` et le
`WeightParser`. Un litre de liquide vaisselle deviendrait un poids et fausserait le score et
l'ACV. Le provider n'émet donc aucun attribut nommé `POIDS`, `WEIGHT` ou `QUANTITY`. Si la
contenance doit être affichée un jour, ce sera dans un attribut `NET_CONTENT` (à créer), sans
`asScore` ni `participateInACV`.

Nouveaux attributs YAML : aucun en phase 1. Le provider n'émet pas d'attribut, seulement noms,
marque et image.

Champs `Product` : `sourceAttributions` (P0-4). Attribution par image : nouveau champ sur
`Resource`, par exemple `MediaAttribution(author, licenseUrl, sourcePageUrl, sourceName)`
(à créer), proposition S2. Mapping ES : à déclarer, car l'index est en `"dynamic": "strict"` (README, P0-4) ; un objet
`"dynamic": false` suffit.

## 5. Pipeline

1. **Provider** (à créer) : `OpenProductsFactsProvider implements ReferenceDatasetProvider`,
   dans `services/reference-datasets`.
   - `resolveDownloadUrls()` : URL fixe du JSONL, dont le nom ne change pas.
   - Téléchargement : `RemoteFileCachingService.downloadToTmpFile`, puis
     `decompressGzipAndDeleteSource`, ou lecture gzip en flux.
   - `read(Path)` : lecture ligne à ligne avec Jackson, sans charger les champs écartés.
   - `sourceRecordId = code`, `recordUrl = https://world.openproductsfacts.org/product/{code}`,
     `sourceUpdatedAt = last_modified_t`, `state = ACTIVE`. Une fiche absente du dump suivant est
     retirée par la logique de diff du socle.
   - `payload` : noms par langue, marque, `categories_tags`, `quantity`, `product_quantity`,
     `labels_tags`, `countries_tags`, `data_sources_tags`, et un descripteur de l'image de face
     (`imgid`, langue, `uploader`, `uploaded_t`, URL).
   - **URL d'image stable** : on construit l'URL de l'image téléversée, pas celle de l'image
     sélectionnée. Format : `https://images.openproductsfacts.org/images/products/{p1}/{p2}/{p3}/{reste}/{imgid}.jpg`,
     où le code est complété à 13 chiffres puis découpé en 3/3/3/4. Vérifié pour un code à 13
     chiffres (`0885909784387`) et un code à 8 chiffres (`54027060` devient
     `000/005/402/7060`). L'URL sélectionnée `front_fr.{rev}.400.jpg` change à chaque
     révision : `front_fr.3.400.jpg` répond 404 et `front_fr.6.400.jpg` répond 200 pour le
     même produit. Taille mesurée de l'original : 150 Ko à 1,6 Mo ; variante `{imgid}.400.jpg`
     disponible (17 Ko).
2. **Import** : `ReferenceIndexService` écrit `ref-open-products-facts-<horodatage>`, applique
   `min-rows-ratio: 0.9` (base stable et croissante), puis bascule l'alias. Le rapport donne les
   lignes lues, les GTIN exploitables, les ISBN, les codes restreints, les invalides et les
   fiches disparues.
3. **Projection** : `ReferenceProjectionService`, `CREATE` (défaut décidé le 2026-11-09), `GTIN_ONLY`, par lots de 500
   GTIN (`multiGetForAggregation`), soit environ 89 `multiGet`. Le `DataFragment` synthétique
   porte `datasourceName = open-products-facts`, les noms, la marque et une `Resource` avec son
   attribution (S2). Il ne porte jamais `category` ni `price` (liste blanche S3). L'image OPF ne
   reçoit pas le tag `PRIMARY` : les images marchandes restent prioritaires.
4. **Retrait et remplacement** : `Product.removeDatasourceData` ne retire ni les `resources` ni
   les `offerNames` (lecture du code, `Product.java:381`). `MediaAggregationService` déduplique
   par URL seulement : quand OPF change d'image de face (nouvel `imgid`), l'ancienne image reste.
   Proposition S1 : retirer les ressources dont `datasourceName` vaut l'identifiant de la
   source, et remplacer, à la projection, les ressources de la source au lieu de les ajouter.
   `offerNames` est un `Set<String>` sans source : un nom OPF disparu ne peut pas être retiré
   (accepté, ou proposition de noms par source, hors périmètre).
5. **Scores** : aucun effet, faute d'attribut projeté.
6. **Export ouvert** : aucune nouvelle colonne. L'identifiant apparaît dans la colonne `sources`
   (P0-4). Les images ne sont pas exportées.
7. **Front** : les images passent par le chemin standard des ressources, avec un crédit par
   image ("Photo : {uploader}, Open Products Facts, CC BY-SA 3.0"). La galerie concernée est
   `ProductHeroGallery.vue` ; la façon dont elle lit les ressources est **non vérifiée**. Bloc
   "Sources" de P0-4.

## 6. Planification et volumétrie

- Cron `0 30 6 * * *`, après l'export de 04 h UTC (fuseau du serveur **non vérifié**).
- Import : 41 Mo compressés, 48 924 lignes, de l'ordre de la minute (estimation).
- Projection : environ 89 `multiGet` ; seuls les produits dont le hash a changé sont réindexés.
- Images : au plus 29 635 ressources. `ResourceCompletionService` ne télécharge que pour les
  produits d'une verticale ayant des offres. Les images des produits hors verticale resteraient
  donc des URL jamais mises en cache ; leur affichage est **non vérifié**. Au pire (toutes les
  images téléchargées, 300 Ko en moyenne, estimation), cela ajoute environ 9 Go au cache de
  ressources, déjà saturé sur beta (constat d'exploitation de juillet 2026, **non revérifié**).

## 7. Sourcing transparent

- `open4goods-config/datasources/open-products-facts.yml` : `name: "Open Products Facts"`,
  `portalUrl: https://world.openproductsfacts.org`, `license: "ODbL 1.0 (base), DbCL (contenus),
  CC BY-SA 3.0 (images)"`, `attribution: "Contient des données d'Open Products Facts, disponibles
  sous licence ODbL"`. C'est la formule proposée par la source : "Contains data from Open
  Products Facts, available under the Open Database License".
- `SourceAttribution` par produit : `recordUrl` (page publique), `sourceUpdatedAt`
  (`last_modified_t`), `retrievedAt`.
- Par image (S2) : auteur = `images.uploaded[imgid].uploader`, licence CC BY-SA 3.0, page source
  = `recordUrl`.
- **P0-1 est bloquant, mesures à l'appui.** Les URL ne contiennent pas la chaîne :
  `images.openproductsfacts.org` et `world.openproductsfacts.org`, 0 occurrence sur 33 872
  `image_url` du CSV. En revanche, 22 256 lignes du JSONL (45 %) contiennent "openfoodfacts" :
  `data_sources_tags` (13 143, par exemple `app-smoothie-openfoodfacts`), `editors_tags`
  (9 739), `images` (4 753), `creator` (3 470). Surtout, l'auteur de 4 306 photos de face est
  `openfoodfacts-contributors`. Écrire cet auteur dans `Product.resources`, comme l'exige
  l'attribution par image, suffit pour que `BatchService.clean()` supprime le produit.

## 8. Tests et critères d'acceptation

- Test unitaire du provider sur une fixture de 20 lignes tirées du vrai JSONL : GTIN-13 valide,
  GTIN-8, UPC complété à 13, ISBN, code restreint (préfixe 2), somme fausse, code de 20
  chiffres, fiche issue de l'import indice-rep, fiche issue de l'import Ecolabel, image de face
  `fr`, image de face seulement `en`, auteur `openfoodfacts-contributors`, fiche sans image.
- Test de construction d'URL d'image : code à 13 chiffres, code à 8 chiffres complété.
- Test de projection : ni `category`, ni `price`, ni attribut `WEIGHT` ; ressource avec
  attribution ; une nouvelle projection avec un autre `imgid` remplace l'ancienne ressource ; le
  retrait de la fiche retire la ressource (S1).
- Test de non-régression P0-1 : un produit dont une ressource a pour auteur
  `openfoodfacts-contributors` survit à `POST /batch/tweak`.
- Critères en beta :
  - rapport d'import : 44 056 GTIN exploitables, à 2 % près ;
  - recouvrement publié par le dry-run avant activation de la projection ;
  - zéro changement de verticale sur les produits projetés (comparaison avant/après) ;
  - zéro produit supprimé par `BatchService.clean()`.

## 9. Découpage

1. P0-1, suppression du nettoyage "openfoodfacts" (S, prérequis).
2. Mesure de recouvrement, par dry-run ou script `_mget` (S). Elle décide de la suite.
3. S2, attribution par image sur `Resource`, DTO front-api et crédit dans la galerie (M).
4. S1, retrait et remplacement des ressources par source dans `Product.removeDatasourceData` et
   `MediaAggregationService` (S).
5. `OpenProductsFactsProvider` et ses tests (S).
6. Configuration `reference-datasets.open-products-facts` et
   `open4goods-config/datasources/open-products-facts.yml` (S).
7. Phase 2, optionnelle : catégories via la liste blanche S3 et des `matchingCategories`
   déclarées sous la clé `open-products-facts` dans les YAML de verticale (M).

## 10. Risques, challenges, questions

- **Apport faible pour les verticales.** 155 smartphones à GTIN valide, 4 TV, 4 lave-linge. Le
  document candidat en faisait un cas marginal ; la mesure le confirme et montre en plus que les
  appareils les mieux couverts (lave-vaisselle, portables) sont des copies de l'indice de
  réparabilité.
- **Provenance circulaire.** OPF réimporte des jeux publics (indice de réparabilité, EU
  Ecolabel, GDSN, Agena3000). Si nudger importe à la fois la source primaire et OPF, une même
  valeur compte deux fois dans le vote de `SourcableAttribute.bestValue()`. Règle : ne jamais
  projeter depuis OPF une donnée qu'une source primaire planifiée fournit. Le provider peut
  s'appuyer sur `data_sources_tags` pour exclure ces fiches.
- **Catégories et verticales.** `TaxonomyRealTimeAggregationService` range la catégorie du
  fragment dans `categoriesByDatasources[datasourceConfigName]`.
  `VerticalsConfigService.getVerticalForCategories` applique ensuite
  `excludingTokensFromCategoriesMatching` aux catégories **de toutes les sources** : `ACCESSOIRE`,
  `QUINCAILLERIE`, `CABLE`, `MEUBLE` dans `_default.yml`, comparés après passage en majuscules
  par `IdHelper.getCategoryName`. Mesure : 65 fiches OPF portent une étiquette qui contient l'un
  de ces jetons (52 `CABLE`, par exemple `en:cables` ou `en:USB-A to Lightning Cables` ; 12
  `ACCESSOIRE`, par exemple `fr:accessoire-xbox` ; 1 `QUINCAILLERIE`). Une seule suffirait à
  retirer un produit de sa verticale. Son URL changerait alors, avec une redirection 301 (voir
  l'incident SEO de juin 2026). D'où la proposition S3 : une liste blanche de ce que chaque
  source peut projeter (`category`, `price`, `resources`, `names`, `attributes`).
- **Images instables et volumineuses.** L'URL fondée sur `imgid` est stable, mais l'image de
  face choisie par la communauté peut changer : S1 est nécessaire pour ne pas empiler les
  anciennes. Le cache de ressources de beta n'a pas d'éviction (494 Go en juillet 2026,
  **non revérifié**).
- **Contribution en retour.** La source demande de renvoyer les contributions ("send those
  contributions back"). Nudger ne produit pas de photos ni de fiches propres ; rien à renvoyer
  aujourd'hui.

Propositions pour le socle (numérotation commune aux plans 07, 08 et 15) :

Les propositions de socle S1 à S5 des plans 07, 08 et 15 sont reprises dans le README, P0-10
(S1 : retrait des ressources ; S2 : attribution par image ; S3 : liste blanche de projection ;
S4 : import d'un fichier produit hors api ; S5 : provider multi-fichiers).

- S1 : retrait et remplacement des `resources` par source (`Product.removeDatasourceData`,
  `MediaAggregationService`).
- S2 : attribution par média (`Resource`), `SourceAttribution` étant par produit et par source.
- S3 : liste blanche de projection par source, `reference-datasets.<id>.project: [NAMES, BRAND,
  RESOURCES]`, avec une valeur `NONE` et l'interdiction de `price` pour toute source non
  marchande (plans 08 et 15).
- `ReferenceRecord` n'a pas de champ pour les médias : en attendant, ils passent dans `payload`.

Questions au propriétaire :

1. Les images OPF justifient-elles le chantier d'attribution par image (S2, M) ? Sans S2, on ne
   projette que noms et marques, en S.
2. Tranchée le 2026-11-09 : les fiches hors verticale sont créées ; leur visibilité dépend de
   P0-11 (sitemap, libellés).
3. Valide-t-on la priorité P3 (au lieu de P2), révisable après la mesure de recouvrement ?
