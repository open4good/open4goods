---
title: "Plan d'intégration : Open Prices (prix relevés en magasin)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : Open Prices (prix relevés en magasin)

> Socle commun : [README](README.md). Fiche source :
> section 4.9 de l'inventaire interne des sources candidates (non publié).
> Mesures du 2026-11-08 sur les exports JSONL du 2026-11-07 (générés vers 23 h UTC),
> téléchargés en entier.

## 0. Fiche

**Verdict : non recommandé en l'état**, pour trois raisons :

- **Volume non alimentaire faible.** Il représente 2,4 % du volume : 3 762 prix sur des produits
  Open Products Facts et 4 455 sur des produits Open Beauty Facts, sur 342 132 prix.
- **Relevés ponctuels et anciens.** Sur les six derniers mois, on compte 332 prix non
  alimentaires (OPF) relevés en France. Les 46 prix de smartphones datent tous du 2025-10-20
  (Fnac Paris - Ternes et un magasin Darty), sauf un.
- **Des observations, pas des offres.** Ce sont des prix vus en magasin, pas des offres
  achetables : sans URL marchande, sans affiliation, sans stock. Les faire passer par la chaîne de
  prix fausserait `minPrice`, `offersCount`, l'export ouvert et le lien sortant (section 4).

Le plan minimal ci-dessous importe la source en référentiel et laisse la projection désactivée.
Un bloc distinct de prix observés est décrit pour le cas où le propriétaire voudrait l'afficher
(Q1).

| Champ | Valeur |
|---|---|
| Identifiant | `open-prices` |
| Mode | **R** et non R+ : 342 132 lignes, 22,7 Mo. Bloc `observedPrices` (à créer), projection désactivée par défaut |
| Priorité | non recommandé en l'état (aligné au sommaire) |
| Effort | S pour l'import seul (provider multi-fichiers) ; M avec le bloc `observedPrices` et son affichage |
| Verticales | `smartphones` : 30 GTIN, 46 prix. Aucune TV, aucun gros électroménager |
| Volume | 342 132 prix, 148 627 GTIN valides distincts ; non alimentaire : 2 398 GTIN OPF et 2 718 GTIN OBF |
| Dépend de | P0-1 (bloquant : toutes les URL de la source contiennent "openfoodfacts"), P0-4, P0-5 ; propositions S5 (provider multi-fichiers) et S3 (liste blanche de projection), section 10 |

## 1. Apport

Open Prices recueille des prix relevés par des contributeurs. Chaque prix porte une date, un
lieu (objet OpenStreetMap ou site marchand) et une preuve photographique (étiquette en rayon
ou ticket de caisse) : 3 761 prix non alimentaires sur 3 762 en ont une.

Usages possibles pour nudger :

1. **Contrôle de cohérence** des prix marchands : signaler une offre en ligne très au-dessus ou
   très en dessous d'un relevé récent en magasin (usage interne).
2. **Mention "prix relevé en magasin"** sur une fiche sans offre en ligne, clairement distincte
   des offres (Q1).
3. **Analyse** : écart entre prix en ligne et prix en magasin, par enseigne.

Ce que la source n'apporte pas : ni URL d'achat, ni affiliation, ni disponibilité, ni frais de
port. Une observation de 13 mois n'est pas un prix actuel.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Prix | `https://prices.openfoodfacts.org/data/prices.jsonl.gz`, 22 744 751 octets, 342 132 lignes, `Last-Modified` 2026-11-07 23:08 UTC | vérifié |
| Preuves | `https://prices.openfoodfacts.org/data/proofs.jsonl.gz`, 9 735 052 octets, 133 180 lignes | vérifié |
| Lieux | `https://prices.openfoodfacts.org/data/locations.jsonl.gz`, 1 241 403 octets, 7 833 lignes | vérifié |
| Produits | `https://prices.openfoodfacts.org/data/products.jsonl.gz`, 20 301 145 octets, 149 080 lignes ("only products with at least one price") | vérifié |
| Parquet | Hugging Face `openfoodfacts/open-prices`, un seul fichier `prices.parquet` de 81 202 931 octets, modifié le 2026-11-07 09:01 UTC. Colonnes (prix seuls, ou dénormalisés avec lieux et produits) **non vérifiées** | vérifié (taille, date) |
| Fréquence | quotidienne (tâche `dump_db_task`, guide `docs/guides/data.md` du dépôt GitHub `openfoodfacts/open-prices`) | vérifié |
| API | `https://prices.openfoodfacts.org/api/v1/prices` en GET (HEAD répond 405). Total au 2026-11-08 : 342 576 prix, contre 342 132 dans le dump de la veille | vérifié |
| Filtre pays de l'API | **ignoré** : `location__osm_address_country_code=NO` renvoie le total complet (342 576) et un premier prix situé en France | vérifié |
| Pages publiques | `https://prices.openfoodfacts.org/prices/{id}`, `https://prices.openfoodfacts.org/products/{code}` ; image de preuve `https://prices.openfoodfacts.org/img/{file_path}` (toutes en 200) | vérifié |
| Authentification | aucune en lecture | vérifié |
| Licence | ODbL | champ d'attribution seulement |

Choix : les quatre dumps JSONL, joints par le provider. Le Parquet n'apporte rien tant que son
schéma n'est pas vérifié.

Champs utiles :

- `prices` : `id`, `product_code`, `type` (`PRODUCT` ou `CATEGORY`), `price`,
  `price_is_discounted`, `price_without_discount`, `discount_type`, `price_per`, `currency`,
  `date`, `receipt_quantity`, `location_id`, `proof_id`, `source`, `created`, `updated`,
  `duplicate_of`. Le champ `owner` (pseudonyme du contributeur) est lu mais jamais projeté.
- `locations` : `type` (`OSM` 7 689, `ONLINE` 144), `osm_name`, `osm_brand`,
  `osm_address_city`, `osm_address_postcode`, `osm_address_country_code`, `website_url`.
- `proofs` : `type` (`PRICE_TAG` 120 830, `RECEIPT` 12 329, `GDPR_REQUEST` 15, `SHOP_IMPORT` 6),
  `date`, `file_path`.
- `products` : `code`, `source` (famille : `off`, `obf`, `opf`, `opff`, ou vide), `product_name`,
  `brands`, `categories_tags`.

## 3. Identifiants et appariement

Mesure sur les 342 132 prix, avec contrôle de la somme GTIN :

| Mesure | Valeur |
|---|---|
| Prix `PRODUCT` / `CATEGORY` | 331 513 / 10 619 (fruits et légumes au poids, sans code) |
| `duplicate_of` renseigné | 9 517 |
| Prix à GTIN valide | 330 419 (96,6 %) ; 148 627 GTIN valides distincts sur 149 253 codes |
| Devise | EUR 249 755 (73 %), USD 47 325, NOK 25 044, SEK 5 881 |
| Magasin situé en France | 219 535 (64 %) |
| Année de relevé | 2026 : 173 753 ; 2025 : 97 476 ; 2024 : 49 295 ; avant 2024 : 21 423 |

Répartition par famille de produit (`products.source`) :

| Famille | Prix | GTIN distincts |
|---|---|---|
| `off` (alimentaire) | 299 167 | 124 990 |
| `opf` (non alimentaire) | 3 762 | 2 398 |
| `obf` (cosmétiques) | 4 455 | 2 718 |
| `opff` (alimentation animale) | 1 020 | 464 |
| sans famille (produit inconnu de la famille OFF) | 23 109 | 18 683 |

Détail des prix `opf` :

| Mesure | Valeur |
|---|---|
| Prix à GTIN valide | 3 698 sur 3 762. Exemples de fautes de frappe : `9806095982984` pour un Samsung A56, `8206097661192` pour un A17 |
| GTIN distincts | 2 398, dont 1 669 avec au moins un prix relevé en France |
| Prix en France | 2 798 ; en EUR : 3 292 |
| Six derniers mois (date au 2026-05-08 ou après) | 706 prix, 572 GTIN ; en France, 332 prix sur 280 GTIN |
| Avec preuve | 3 761 : étiquette 2 090, ticket 853, preuve de type `GDPR_REQUEST` (demande RGPD auprès d'une enseigne) 630, import enseigne 188 |
| Lieux en ligne | 71 prix (carrefour.fr, monoprix.fr, amazon.fr...) |
| Verticales | `smartphones` : 46 prix sur 30 GTIN. Tous du 2025-10-20 (Fnac Paris - Ternes, Darty) sauf un, du 2026-01-24 (Carrefour) |

Prix relevés dans les enseignes non alimentaires spécialisées, toutes familles et tous pays
confondus (`osm_brand` ou `osm_name`) : Fnac 135 (dernier le 2026-09-15), Leroy Merlin 72,
Darty 29, But 22, Castorama 19, Boulanger 10 (dont 9 sans code). Moins de 300 prix au total.

Règles :

1. Ne garder que `type = PRODUCT`, un `product_code` GTIN valide, `duplicate_of` vide et
   `price > 0`.
2. Appariement `GTIN_ONLY` sur `product_code`, normalisé par `BarcodeValidationService`.
3. Un `ReferenceRecord` par GTIN (et non par prix) : la projection vise un produit, et l'index
   reste borné (148 627 enregistrements, ou environ 23 800 si l'on exclut la famille `off`).

Recouvrement avec nos produits : **non mesurable** sans accès à l'Elasticsearch de prod. Mesure
proposée : `_mget` par lots de 500 sur les 23 800 GTIN des familles `opf`, `obf` et sans
famille, en comptant les produits trouvés, ceux qui ont `offersCount > 0` et ceux qui n'ont plus
d'offre. Les 124 990 GTIN alimentaires peuvent être mesurés de la même façon, pour savoir si les
flux marchands ont déjà créé des produits alimentaires.

## 4. Modèle cible

**Pourquoi pas `PriceAggregationService`.** Lecture du code de `main` :

- `PriceAggregationService.onDataFragment` ajoute `new AggregatedPrice(fragment)` aux offres
  pour tout fragment dont le prix est non nul et qui n'est pas en rupture.
- Le constructeur `AggregatedPrice(DataFragment)` prend `df.affiliatedUrlIfPossible()` comme
  URL. L'offre deviendrait un lien sortant vers `prices.openfoodfacts.org`, qui n'est pas un
  marchand. Cette URL contient "openfoodfacts" : `BatchService.clean()` supprimerait le produit
  (P0-1).
- Son horodatage vient de `df.getLastIndexationDate()`. Avec la date du relevé, l'offre serait
  écartée par `onProduct` dès qu'elle dépasse `ProductRepository.VALID_UNTIL_DURATION` (2 jours),
  c'est-à-dire presque toujours. Avec la date de projection, un relevé de 13 mois deviendrait une
  offre "actuelle".
- Ensuite, `computeMinPrice` le prendrait pour prix minimum, et `offersCount = prices.size()`
  passerait au-dessus de 0. Le produit entrerait alors dans
  `ProductRepository.exportVerticalWithValidDate` (completion, batch de verticale) et dans les
  filtres `offersCount > 0` (`ElasticProductRepository`, `ProductRepository`). Il entrerait aussi
  dans les colonnes `min_price` et `offers_count` de l'export ouvert, dans l'historique et la
  tendance de prix (`computePriceHistory`), et dans la `compensation`, une contribution
  d'affiliation estimée sur un prix sans affiliation.

Conclusion : pour cette source, la projection ne remplit **jamais** `DataFragment.price`
(liste blanche S3). Elle n'a d'ailleurs aucune raison d'appeler `StandardAggregator`, puisqu'il
n'y a ni attribut, ni nom, ni image à agréger.

Deux options :

- **A (recommandée en l'état) : charge utile dans `ref-open-prices` seulement.** Aucun champ
  `Product`. Sert au contrôle de cohérence et aux analyses (usages 1 et 3), par requête directe
  sur l'index de référence.
- **B (si Q1 = oui) : bloc distinct `Product.observedPrices` (à créer)**, écrit par
  `ReferenceProjectionService` à partir de `ReferenceRecord.payload`, comme le bloc `recalls`
  de P0-6, hors chaîne d'agrégation :

  ```java
  public record ObservedPrices(int count, Instant lastObservedAt, List<ObservedPrice> recent) {}

  public record ObservedPrice(double price, String currency, LocalDate observedOn,
          String storeName, String storeCity, String countryCode,
          boolean discounted, String proofType, String priceUrl) {}
  ```

  `recent` garde les 5 relevés les plus récents en France et en EUR, plus jeunes que
  `max-age-days` (365 par défaut). Mapping ES : à déclarer en objet `"dynamic": false`, car
  l'index est en `"dynamic": "strict"` (README, P0-4) ; sous-champ mappé seulement si un filtre
  "a un prix relevé" devient nécessaire.

Jamais projetés, même dans l'option B : `owner` (pseudonyme du contributeur), les images de
preuve (tickets de caisse ; 630 prix `opf` reposent sur l'une des 15 preuves `GDPR_REQUEST`,
issues d'une demande RGPD auprès d'une enseigne), les coordonnées précises du lieu.

## 5. Pipeline

1. **Provider** (à créer) : `OpenPricesProvider implements ReferenceDatasetProvider`, dans
   `services/reference-datasets`.
   - `resolveDownloadUrls()` : les quatre URL fixes.
   - **Limite du socle** : `read(Path)` reçoit un seul fichier, alors qu'il faut joindre quatre
     dumps. Proposition S5 : `read(Map<String, Path>)`, ou un provider qui reçoit le dossier de
     téléchargement.
   - Lecture : `locations` (7 833) et `products` (149 080, réduits à code, famille, nom et
     marque) en mémoire, `proofs` réduits à `id -> type`, puis `prices` en flux, regroupés par
     GTIN.
   - `sourceRecordId = GTIN`, `recordUrl = https://prices.openfoodfacts.org/products/{code}`,
     `sourceUpdatedAt` = maximum des `updated`, `state = ACTIVE`.
   - `payload` : la liste des relevés filtrés (prix, devise, date, remise, nom, ville et pays du
     lieu, type de preuve, `id` du prix) et des agrégats (nombre de relevés, date du dernier,
     minimum et médiane par devise).
   - Option `families: [opf, obf, none]` pour exclure l'alimentaire, que nudger ne couvre pas.
2. **Import** : `ReferenceIndexService` écrit `ref-open-prices-<horodatage>`, applique
   `min-rows-ratio: 0.9` et bascule l'alias.
3. **Projection** : désactivée par défaut. Le socle ne connaît que `ENRICH_EXISTING` et
   `CREATE` ; il faut une valeur `NONE` (S3). Avec l'option B, la projection écrit seulement
   `observedPrices` et `sourceAttributions[open-prices]`, puis réindexe, par lots de 500 GTIN
   (une cinquantaine de `multiGet` sans l'alimentaire).
4. **Scores** : aucun effet.
5. **Export ouvert** : aucune colonne. Une colonne de prix relevé à côté de `min_price` prêterait
   à confusion, et la donnée est déjà ouverte à la source.
6. **Front** (option B seulement) : DTO (à créer) mappé dans `ProductMappingService` (front-api).
   Affichage dans `ProductPriceEmptyCard.vue` (fiche sans offre), ou en ligne secondaire sous
   `ProductPriceRows.vue` : "Prix relevé en magasin : 352 EUR, Fnac Paris - Ternes, le
   20/10/2025 (Open Prices)". Ni bouton d'achat, ni lien affilié ; un lien discret vers la page
   du relevé. Exclu de `ProductPriceVerdict.vue`, `ProductHeroPricing.vue`,
   `ProductStickyPriceBanner.vue` et `ProductMicroPrice.vue`.

## 6. Planification et volumétrie

- Cron `0 0 2 * * *`, après les dumps de 23 h 15 UTC (fuseau du serveur **non vérifié**).
- Téléchargement : 54 Mo pour les quatre fichiers.
- Import : 148 627 enregistrements, ou environ 23 800 sans l'alimentaire.
- Projection (option B) : une cinquantaine de `multiGet` sans l'alimentaire.

## 7. Sourcing transparent

- `open4goods-config/datasources/open-prices.yml` : `name: "Open Prices"`,
  `portalUrl: https://prices.openfoodfacts.org`, `license: "ODbL 1.0"`,
  `attribution: "Prix relevés par les contributeurs d'Open Prices (Open Food Facts)"`.
- `SourceAttribution` (option B) : `recordUrl` (page des prix du produit), `sourceUpdatedAt`,
  `retrievedAt`. Chaque relevé affiché porte sa date, son magasin et un lien vers sa page.
- **P0-1 est strictement bloquant.** `portalUrl`, `recordUrl`, la page de chaque prix et les
  images de preuve sont tous sous `prices.openfoodfacts.org`. S'y ajoutent `products.image_url`
  (`images.openfoodfacts.org` pour la famille `off`) et `osm_brand_logo_url`
  (`raw.githubusercontent.com/openfoodfacts/...`). Une seule de ces chaînes dans un produit suffit
  à le faire supprimer par `BatchService.clean()`.

## 8. Tests et critères d'acceptation

- Test unitaire du provider sur une fixture tirée des vrais dumps : prix `CATEGORY` sans code,
  `duplicate_of` renseigné, GTIN à somme fausse, devise NOK, lieu `ONLINE`, preuve
  `GDPR_REQUEST`, smartphone `opf` relevé chez Fnac, produit absent de `products`.
- Test du regroupement par GTIN et des agrégats (minimum, médiane, dernier relevé).
- Test de projection (option B) : aucune `AggregatedPrice` créée, `offersCount`, `price` et
  `minPrice` inchangés, `owner` absent du produit.
- Critères en beta :
  - rapport d'import : 330 419 prix retenus avant filtre de famille, à 2 % près ;
  - zéro variation d'`offersCount` ou de `min_price` dans l'export sur les produits projetés ;
  - zéro produit supprimé par `BatchService.clean()`.

## 9. Découpage

1. P0-1 (S, prérequis).
2. S5, provider multi-fichiers dans le socle (S).
3. `OpenPricesProvider`, import dans `ref-open-prices` et tests (S).
4. Analyse ponctuelle : nos `minPrice` comparés aux relevés récents sur les GTIN communs (S).
   Elle décide de Q1.
5. Si Q1 = oui : bloc `observedPrices`, projection "charge utile seule" (S3) et tests (M).
6. Si Q1 = oui : DTO front-api et affichage distinct (S).

## 10. Risques, challenges, questions

- **Volume et fraîcheur.** Le non alimentaire vient d'événements ponctuels de contributeurs :
  45 des 46 prix de smartphones ont été relevés le même jour dans deux magasins. Sans campagne
  de relevés ciblée sur l'électroménager, la couverture restera anecdotique.
- **Qualité.** Fautes de frappe dans les GTIN (64 prix `opf` invalides), lots vendus en pack
  ("Pack Smartphone Honor 400 Lite ... + écouteurs"), prix remisés (192 prix `opf`).
- **Données personnelles.** `owner`, tickets de caisse et preuves `GDPR_REQUEST` : rien de tout
  cela ne sort de `ref-open-prices`.
- **Confusion avec les offres.** C'est le risque produit principal : un prix relevé mal présenté
  serait lu comme un prix d'achat. D'où l'exclusion de tous les composants de verdict et de prix
  principal.
- **Proposition : contribuer plutôt que consommer.** Le relevé de prix en magasin par les
  utilisateurs de nudger, poussé vers l'API Open Prices, donnerait à la source un volume non
  alimentaire qu'elle n'a pas. C'est une piste communautaire, pas une intégration.

Propositions pour le socle (numérotation commune aux plans 07, 08 et 15) :

Les propositions de socle S1 à S5 des plans 07, 08 et 15 sont reprises dans le README, P0-10
(S1 : retrait des ressources ; S2 : attribution par image ; S3 : liste blanche de projection ;
S4 : import d'un fichier produit hors api ; S5 : provider multi-fichiers).

- S5 : `ReferenceDatasetProvider.read` multi-fichiers (aussi utile pour les dumps RDF de
  data.bnf.fr ou d'Open Library, **non vérifié**).
- S3 : liste blanche de projection par source, avec une valeur `NONE` et l'interdiction de
  `price` pour toute source non marchande.

Questions au propriétaire :

1. Faut-il afficher un prix relevé en magasin sur la fiche (option B, bloc distinct), ou garder
   Open Prices pour un usage interne seulement (option A) ?
2. L'usage "contrôle de cohérence des prix marchands" a-t-il un client (Ops, qualité des flux) ?
   C'est le seul usage qui exploite le volume alimentaire.
3. Faut-il exclure dès l'import la famille alimentaire (`off`, 87 % des prix), puisque nudger n'a
   pas de verticale alimentaire ?
