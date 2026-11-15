---
title: "Plan d'intégration : offres reconditionnées et d'occasion des flux d'affiliation"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : offres reconditionnées et d'occasion des flux d'affiliation

> Socle commun : [README](README.md). Fiche source : section 4.19 de l'inventaire interne des
> sources candidates et étude interne de couverture occasion (non publiées).
> Les chiffres du flux Back Market datent du **2026-10-22** (étude interne de couverture) et
> n'ont pas été refaits : le téléchargement des flux Awin exige les identifiants du réseau, non
> utilisés ici. Lecture du code de `main` (c6ed39707).

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | les datasources d'affiliation existantes, par exemple `backmarket.fr` (nom observé dans l'index) ; pas d'identifiant nouveau |
| Mode | F (flux marchand) : chaîne des flux existante, avec l'état `productState` et un appariement par titre pour les lignes sans GTIN |
| Priorité | P2 |
| Effort | M pour l'appariement par titre sur les smartphones ; M de plus si l'on crée un état `REFURBISHED` distinct (section 4) |
| Verticales | `smartphones` (premier cas) ; même mécanisme possible ensuite pour d'autres verticales à variantes simples |
| Volume | Back Market (Awin) : 169 664 lignes, dont 23 364 avec GTIN (14 %) ; part en stock **à mesurer** (section 3.1) |
| Dépend de | correctif #3468 déployé ; P0-4 pour l'attribution (optionnel ici, les flux marchands ont déjà leur fiche source) |

## 1. Apport

- **Le reconditionné est l'occasion la plus utile pour les smartphones** : garantie, état
  contrôlé, prix affiché. C'est le coeur d'une alerte "devient disponible en occasion ou
  reconditionné".
- Couverture actuelle (mesure du 2026-10-03) : 7 476 produits des verticales ont une offre
  occasion, dont 5 546 smartphones, avec 1,6 marchand par produit en moyenne. Back Market pèse
  0,6 % des offres occasion de l'échantillon aléatoire (dominé par les livres), mais figure
  parmi les marchands des verticales avec Certideal et Underdog.
- **Ce qui est perdu aujourd'hui** : toute ligne sans GTIN numérique est écartée sans bruit par
  `DataFragmentStoreService.queueDataFragment` (`if (!StringUtils.isNumeric(data.gtin()))`, log
  en `debug`). Pour Back Market, 146 300 lignes sur 169 664 n'atteignent jamais l'agrégation.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Canal | flux Awin, programme Back Market FR | **non vérifié** ici |
| Chaîne | `AwinFeedService` (`services/feedservice`), datasource "volatile" (`AbstractFeedService.getVolatileDatasource`), `FeedIndexingWorker`, `DataFragmentStoreService` | vérifié (code) |
| Fréquence | `BatchService.fetchFeeds()`, cron `${feed.indexation.cron:19 13 23 * * ?}` ; offres valables 2 jours (`ProductRepository.VALID_UNTIL_DURATION`) | vérifié (code) |
| Colonnes reconnues par défaut | GTIN : `ean`, `product_GTIN`, `EAN`, `gtin`... ; modèle : `model_number`, `product_model` ; état : `condition`, `product condition`, `item condition` ; stock : `stock_status`... (`FeedIndexingWorker`) | vérifié (code) |
| Colonnes du flux Back Market | `search_price`, `stock_status`, état `used` (mesure du 2026-10-22) ; `brand_name`, `colour`, `model_number` (colonnes Awin standard) | partiel, **non vérifié** pour les trois dernières |
| Surcharge par flux | un fichier `open4goods-config/datasources/<nom>.yml` portant la `feedKey` est repris par `dataSourceConfigService.getDatasourcePropertiesForFeed(feedKey)` ; il peut fixer `defaultItemCondition` | vérifié (code) |

Mesures du flux Back Market, 2026-10-22 (étude interne de couverture) :

| Mesure | Valeur |
|---|---|
| Lignes | 169 664 |
| Lignes avec GTIN | 23 364 (14 %), validité de la somme de contrôle **non mesurée** |
| Lignes `used` | 130 561 |
| Lignes `used` en rupture, à `search_price = 1` | 129 829 |
| Offres `backmarket.fr` dans l'index à 1 EUR, avant correctif | 14 649 sur 17 719 |
| Produits occasion des verticales avec un prix minimum de 1 EUR | 1 221 sur 8 273 |

Incident des offres à 1 EUR : `PriceAggregationService.onDataFragment` agrégeait les fragments
`OUTOFSTOCK`. Le correctif #3468 (commit 627a3ea35, "ignore out-of-stock fragments in price
aggregation") est dans `main` et dans les tags v1.0.77 à v1.0.79. Version déployée en prod
**non vérifiée** ; nettoyage de l'historique délégué à Ops (GOU-1607).

## 3. Identifiants et appariement

### 3.1 Le chiffre qui manque

Les chiffres du 2026-10-22 ne disent pas combien de lignes **en stock** n'ont pas de GTIN :

- parmi les 130 561 lignes `used`, 129 829 sont en rupture : il reste 732 lignes `used` en
  stock ;
- mais 39 103 lignes (169 664 - 130 561) ont un autre état, inconnu ;
- et l'index portait 3 070 offres `backmarket.fr` à un vrai prix (17 719 - 14 649), toutes issues
  de lignes avec GTIN. Une partie peut être ancienne, puisqu'une offre n'est purgée qu'à
  l'arrivée d'un nouveau fragment.

L'offre en stock se trouve donc surtout hors des lignes `used`, probablement dans ces 39 103
lignes. Le gain d'un appariement par titre est **inconnu**, ni petit ni grand. Il faut d'abord
croiser état, stock et GTIN :

| À mesurer sur le flux du jour | Pourquoi |
|---|---|
| lignes par `condition` x `stock_status` x GTIN valide (somme de contrôle) | taille réelle du gain |
| répartition des marques et des catégories marchandes en stock | part des smartphones |
| 50 titres en stock sans GTIN | forme réelle des titres (marque, modèle, stockage, couleur, état) |
| valeurs distinctes de `condition` et des colonnes de grade | correspondance avec `ProductCondition` |

Script hors dépôt, lancé par un humain qui a les identifiants Awin.

### 3.2 Pourquoi `ReferenceMatcher` ne suffit pas

`ReferenceMatcher` (README, section 3.2) cherche un GTIN exact, puis une marque et un
**identifiant de modèle** (logique d'`EprelCompletionService.selectUniqueResult`). Un titre de
reconditionné porte un **nom commercial et une variante** : forme supposée "iPhone 13 128 Go -
Minuit - Débloqué" ou "Galaxy S21 FE 5G 128 Go - Graphite - Dual-SIM" (**non vérifié** sur le
flux). Rien de cela n'est un identifiant de modèle, et un même nom commercial couvre plusieurs
GTIN (capacité, couleur, région). Le socle n'a pas de matcher de variantes : c'est une extension
à proposer (section 10).

### 3.3 Proposition : index de variantes construit sur nos produits

On n'a pas besoin d'un référentiel externe : nos produits de la verticale `smartphones` sont
déjà le référentiel des variantes neuves, avec leur GTIN.

- `SmartphoneVariantIndex` **(à créer)**, en mémoire dans `api`, reconstruit chaque jour à
  partir des produits de la verticale. `ProductRepository` offre `exportAll(String vertical)`
  (produits complets) et `exportAll(String[] includeFields)` (tous les produits) : il faut soit
  une méthode combinée (à créer), soit `exportAll(String vertical)`. Champs utiles : GTIN, marque
  (`attributes.referentielAttributes.BRAND`), modèle et `akaModels`, noms d'offres,
  `attributes.indexed.STORAGE_CAPACITY.numericValue`, `attributes.indexed.COLOR.value`,
  `offersCount`. Environ 49 000 entrées.
- Clé : (marque résolue, famille commerciale, stockage en Go, couleur normalisée) vers un ensemble
  de GTIN. La famille ("iPhone 13", "Galaxy S21 FE") est extraite des noms de nos produits ; la
  couleur passe par les `mappings` de `COLOR.yml`, qui ramènent les couleurs à des familles
  (`NOIR`, `BLEU`...). Les deux côtés sont normalisés de la même façon.
- Analyse du titre marchand : marque par `BrandService.resolve` sur `brand_name` ou le premier
  mot ; codes de modèle par `ProductModelCandidateHelper.extractModelsFromTitles` (utile pour
  Samsung `SM-...`) ; famille par plus long préfixe connu de l'index ; stockage par motif
  (`128 Go`, `128GB`, `1 To`) ; couleur par dictionnaire ; mots d'état ("Débloqué",
  "Reconditionné", grades) retirés.

Règles de décision :

1. marque, famille et stockage obligatoires ;
2. couleur présente et unique : rattachement ;
3. plusieurs GTIN pour la même clé (variantes régionales) : on prend le GTIN qui a le plus
   d'offres, de façon déterministe, et on compte ces cas dans le rapport ;
4. couleur absente ou ambiguë : rejet, comme `selectUniqueResult` rejette les ambiguïtés ;
5. le matcher ne renvoie que des GTIN **existants** : jamais de création de produit à partir
   d'un titre.

Mesure de recouvrement à faire lancer sur la prod (part des smartphones utilisables comme
référentiel de variantes) :

```json
GET products-moustik/_search
{ "size": 0,
  "query": { "term": { "vertical": "smartphones" } },
  "aggs": {
    "stockage": { "filter": { "exists": { "field": "attributes.indexed.STORAGE_CAPACITY.numericValue" } } },
    "couleur":  { "filter": { "exists": { "field": "attributes.indexed.COLOR.value" } } },
    "les_deux": { "filter": { "bool": { "filter": [
        { "exists": { "field": "attributes.indexed.STORAGE_CAPACITY.numericValue" } },
        { "exists": { "field": "attributes.indexed.COLOR.value" } } ] } } } } }
```

## 4. Modèle cible

Aucun attribut : la source apporte des offres.

État des offres :

- `ProductCondition` n'a que `NEW` et `OCCASION`. `ProductConditionParser` range "RECONDITIONNE",
  "REFURBISHED", "PRODUIT RECONDITIONNE" et "REFURBUSHED" dans `OCCASION`. L'information est
  perdue dès le parsing, et `AmazonCompletionService.isUsedCondition` fait de même pour
  "Refurbished".
- Le front sépare déjà neuf et occasion (`ProductPriceRows.vue`, filtre
  `CategoryOffersStateToggle.vue`) et possède la clé i18n `condition.refurbished`
  ("Reconditionné"), inutilisée faute de valeur.

Deux options :

| Option | Changements | Effort |
|---|---|---|
| A. Nouvel état `ProductCondition.REFURBISHED` (à créer) | parser, `PriceAggregationService.computePriceHistory` (troisième historique, `price.refurbishedPricehistory`, ajouté au mapping par `putMapping` au démarrage), front (lignes de prix, filtre, JSON-LD), `ProductTimelineEventType`, Amazon (`Refurbished`), eBay (plan 11, `conditionId` 2000 à 2500) | M |
| B. Grade sur l'offre : `AggregatedPrice.conditionGrade` (à créer), texte libre ("Reconditionné, très bon état") | parser (garde `OCCASION`), affichage seulement | S |

Recommandation : A. Le reconditionné et l'occasion entre particuliers n'ont ni le même prix ni
la même garantie, et une alerte occasion doit pouvoir les distinguer.

Par marchand : un fichier `open4goods-config/datasources/<marchand>.yml` **(à créer)** avec la
`feedKey` du flux et `defaultItemCondition: REFURBISHED` (option A) pour les marchands qui ne
vendent que du reconditionné (Back Market, Certideal, Recommerce, à confirmer).

Champs `Product` : aucun nouveau. Mapping ES : `price.conditions` est déjà un `keyword` ; une
valeur de plus suffit.

## 5. Pipeline

1. **Mesure** du flux (section 3.1), ventilée par état, stock et GTIN valide. Si les lignes en
   stock sans GTIN sont rares, s'arrêter à l'étape 2 et à l'option A ; sinon, poursuivre.
2. **Correctif #3468** confirmé en prod, et historique nettoyé (GOU-1607).
3. **Appariement par titre** (à créer) dans `api`, ce qui respecte la frontière d'ADR 0007
   (`services/feedservice` ne dépend pas de `api`) :
   - `OfferVariantMatcher`, à côté de `ReferenceMatcher` dans `api/.../services/reference/`, et
     `SmartphoneVariantIndex` (section 3.3) ;
   - appel dans `DataFragmentStoreService.queueDataFragment`, juste avant le rejet des fragments
     sans GTIN numérique, et seulement pour les datasources listées
     (`offer-matching.datasources: [backmarket.fr, ...]`) ;
   - en cas de succès : GTIN ajouté en `referentielAttributes`, et fragment réduit à une offre
     (prix, état, URL affiliée, un nom pour `offerName`). Attributs, images, descriptions et
     catégories d'une annonce de reconditionné ne doivent pas enrichir la fiche du produit neuf :
     il faut un marqueur de fragment "offre seule" (à créer) respecté par les services
     d'agrégation autres que `PriceAggregationService` ;
   - en cas d'échec : rejet comme aujourd'hui, mais compté par motif (marque, famille, stockage,
     couleur, ambiguïté) dans un rapport journalier.
4. **État** : option A ou B (section 4), surcharges par marchand.
5. **Affichage séparé du neuf** : trois lignes dans `ProductPriceRows.vue` (neuf, reconditionné,
   occasion), chacune avec son meilleur prix et l'écart au neuf. Le prix d'en-tête de la fiche
   reste le prix neuf. Aujourd'hui, `price.minPrice` prend le minimum **tous états confondus**
   (`computeMinPrice`), et l'export ouvert publie ce minimum en `min_price`
   (`OpenDataService`, `Product.bestPrice()`). Proposition : calculer et exporter le minimum par
   état, au moins le neuf.
6. **Alertes occasion** : rien de spécifique ; un matching d'alertes branché sur l'agrégation
   standard verrait les nouvelles offres.

## 6. Planification et volumétrie

- Aucune tâche nouvelle : batch des flux (23 h 13).
- Index de variantes : un export d'environ 49 000 produits par jour, quelques dizaines de Mo en
  mémoire. À reconstruire après le batch de 13 h, qui met à jour les attributs.
- Coût de l'appariement : une analyse de titre et une recherche en mémoire par ligne sans GTIN
  des datasources listées. Pour Back Market, au plus 146 300 lignes par jour, beaucoup moins si
  les lignes en rupture sont filtrées avant l'appariement (à faire : tester le stock avant le
  matcher). Le volume restant dépend de la mesure 3.1.

## 7. Sourcing transparent

- Les flux marchands portent déjà leur nom et leur logo (`DataSourceProperties`). L'offre
  rattachée par titre affiche le marchand, l'état et le grade.
- Transparence propre à ce plan : une offre rattachée par titre est marquée comme telle (par
  exemple "Correspondance établie à partir du titre de l'annonce"), pour que l'utilisateur sache
  que le marchand n'a pas fourni le code-barres.
- `SourceAttribution` (P0-4) : non nécessaire pour les offres.

## 8. Tests et critères d'acceptation

- Tests unitaires de l'analyse de titre, sur 50 titres réels tirés de la mesure 3.1 (formes
  Apple, Samsung, Xiaomi ; stockage en Go et To ; couleurs françaises et anglaises).
- Tests de `OfferVariantMatcher` : clé unique, variantes régionales (règle 3), couleur absente
  (rejet), marque inconnue (rejet), GTIN inexistant jamais renvoyé.
- Test d'intégration : un fragment sans GTIN d'une datasource listée arrive sur le bon produit,
  sans modifier ses noms, attributs ou images.
- Test du parser d'état : "Reconditionné" donne `REFURBISHED` (option A), "Occasion" donne
  `OCCASION`.
- Critères en beta :
  - taux de rattachement des lignes en stock sans GTIN publié par marque ;
  - précision sur 100 rattachements vérifiés à la main : au moins 98 % (bon modèle, bon
    stockage) ; une erreur de couleur est tolérée si la clé de couleur l'est aussi ;
  - zéro produit créé à partir d'un titre ;
  - zéro offre à 1 EUR dans les verticales deux jours après le déploiement de #3468.

## 9. Découpage

1. Vérifier le déploiement de #3468 et le nettoyage GOU-1607 (XS, Ops).
2. Mesure du flux Back Market, stock par GTIN (S, humain avec les identifiants Awin).
3. État `REFURBISHED` : enum, parser, historique, front, surcharges par marchand (M), ou option B
   (S).
4. `SmartphoneVariantIndex` et analyse de titre, avec tests (M).
5. `OfferVariantMatcher` branché dans `DataFragmentStoreService`, marqueur "offre seule",
   rapport des rejets (S).
6. Prix minimum par état dans l'agrégat et l'export ouvert (S).
7. Nouveaux flux reconditionnés, un par un (S chacun).

## 10. Risques, challenges, questions

- **Le gain réel est inconnu** : seules 732 lignes `used` sont en stock, mais 39 103 lignes ont
  un état non mesuré, et l'index portait 3 070 offres Back Market à un vrai prix, toutes avec
  GTIN. Les "86 % sans GTIN" ne disent rien de l'offre en stock. La mesure 3.1 (état x stock x
  GTIN valide) passe avant tout développement.
- **Extension du socle** : `ReferenceMatcher` apparie des identifiants de modèle, pas des
  variantes commerciales. Proposition : lui ajouter une méthode de variante (marque, famille,
  stockage, couleur), alimentée par un index de nos propres produits plutôt que par un
  `ref-<id>`. Le même outil servirait au plan 11 (titres eBay sans GTIN) et aux titres des
  datasources sans GTIN en général.
- **Pollution de la fiche** : sans le marqueur "offre seule", des titres "reconditionné, très bon
  état" et des photos de produits d'occasion entreraient dans la fiche du produit neuf.
- **Granularité** : nos produits sont des GTIN (une couleur, une capacité, une région). Un
  reconditionné n'a pas de GTIN propre ; le rattacher au GTIN neuf le plus fourni est une
  convention, à afficher comme telle (section 7).
- **Couleurs** : les couleurs marketing ("Minuit", "Lumière stellaire") ne sont pas dans
  `COLOR.yml`, qui ramène les couleurs à des familles. Il faut un dictionnaire de couleurs par
  marque, ou accepter la famille de couleur comme clé.
- **Autres flux, tous non vérifiés** : Rakuten reconditionné, Fnac et Darty "seconde vie",
  Certideal, Recommerce. Certideal et Underdog apparaissent déjà comme marchands occasion des
  verticales (mesure interne du 2026-10-03), donc un flux existe déjà pour eux (réseau
  **non vérifié**). Amazon Renewed passe déjà par `AmazonCompletionService` (état "Refurbished"
  rangé en occasion).
- Questions au propriétaire :
  1. Qui lance la mesure du flux Back Market (stock par GTIN) ? Elle décide si l'appariement par
     titre vaut M jours de travail.
  2. Option A (nouvel état `REFURBISHED`, M) ou option B (grade sur l'offre, S) ?
  3. Accepte-t-on de rattacher une offre reconditionnée au GTIN neuf le plus fourni quand
     plusieurs variantes régionales existent, en l'indiquant à l'utilisateur ?
