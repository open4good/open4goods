---
title: "Plan d'intégration : eBay Browse API (offres neuves, occasion et reconditionnées)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : eBay Browse API (offres neuves, occasion et reconditionnées)

> Socle commun : [README](README.md). Fiche source :
> section 4.15 de l'inventaire interne des sources candidates (non publié).
> Documentation eBay consultée le 2026-11-08, par l'index Context7 de developer.ebay.com et par
> recherche : les pages de référence de developer.ebay.com renvoient HTTP 403 à un client non
> navigateur. Aucune mesure sur données réelles : l'API exige un compte développeur, que nous
> n'avons pas ici. Lecture du code de `main` (c6ed39707).

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | `ebay` |
| Mode | C (completion par appel API), avec une contrainte de fraîcheur de 6 h qui n'existe pour aucune autre source |
| Priorité | P3 pour nudger.fr ; P2 si l'occasion devient un axe prioritaire (alertes occasion) |
| Effort | M : client OAuth et service (S), offres "éphémères" dans `PriceAggregationService` et front-api (S à M), sélection des produits et planification (S) |
| Verticales | `smartphones` d'abord ; techniquement toute verticale dont les produits ont un GTIN |
| Volume | quota par défaut de 5 000 appels/jour : environ 1 250 produits tenus frais en continu à 6 h |
| Dépend de | P0-4 (attribution) ; compte développeur eBay et compte eBay Partner Network ; nouveau prérequis transverse : offres à durée de vie courte et hors historique (section 5, point 3) |

## 1. Apport

- **Offres d'occasion et reconditionnées** de particuliers et de professionnels, absentes des
  flux d'affiliation. Aujourd'hui, 5 546 smartphones ont une offre occasion (mesure interne du
  2026-10-03), avec en
  moyenne 1,6 marchand par produit (Darty, Fnac, Cdiscount, Rue du Commerce, Back Market,
  Certideal, Underdog, Electro Dépôt).
- **Prix neufs** de vendeurs professionnels eBay (`conditionId` 1000).
- **Revenu** : liens affiliés eBay Partner Network (EPN), cohérent avec le modèle
  100 % affiliation de nudger.
- Comble en partie la lacune `availability` relevée par le document candidat.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Endpoint | `GET https://api.ebay.com/buy/browse/v1/item_summary/search` | vérifié (doc) |
| Recherche par GTIN | paramètre `gtin` (UPC, EAN, ISBN), combinable avec `q`, `category_ids`, `epid` ("or a combination of these") | vérifié (doc) |
| Filtres | `filter=conditionIds:{1000\|3000}` ; `filter=buyingOptions:{FIXED_PRICE}` (valeurs `FIXED_PRICE`, `AUCTION`, `BEST_OFFER`, `CLASSIFIED_AD`, à utiliser avec une catégorie feuille) ; filtres de prix, de pays de l'objet ou de livraison | `conditionIds` et `buyingOptions` vérifiés ; autres **non vérifiés** |
| Marketplace | en-tête `X-EBAY-C-MARKETPLACE-ID: EBAY_FR` | **non vérifié** (support d'EBAY_FR par la Browse API) |
| Affiliation | en-tête `X-EBAY-C-ENDUSERCTX: affiliateCampaignId=<campid EPN, 10 chiffres>,affiliateReferenceId=<sub-id, 256 caractères max>`. Sans cet en-tête, la réponse ne contient pas `itemAffiliateWebUrl` | vérifié (doc) |
| Compte EPN | nécessaire pour obtenir `affiliateCampaignId` ; programme actif en France ; taux de commission et fenêtre de conversion **non vérifiés** (sources tierces divergentes) | partiel |
| Authentification | OAuth 2.0 "client credentials" (jeton d'application), portée `https://api.ebay.com/oauth/api_scope` | **non vérifié** |
| Quota | Browse API : 5 000 appels/jour (toutes méthodes hors `getItems`, et `getItems` : 5 000), au niveau de l'application. Relèvement par l'"Application Growth Check", gratuit, réponse en 5 à 7 jours ouvrés | vérifié (page "API call limits" et doc du growth check) |
| Champs de `itemSummaries[]` | `itemId`, `title`, `price {value, currency}`, `condition`, `conditionId`, `seller {username, feedbackPercentage, feedbackScore}`, `itemWebUrl`, `itemAffiliateWebUrl`, `epid`, `image`, `buyingOptions`, `itemLocation`, `shippingOptions` | `conditionId`, `itemAffiliateWebUrl` vérifiés ; le reste **non vérifié** (page de référence en 403) |
| GTIN en réponse | renvoyé par `getItem` et `getItems` (newsletter eBay du 2e trimestre 2023), pas par `item_summary/search` à notre connaissance | partiel |
| Disponibilité | la doc recommande de vérifier `itemEndDate` et `estimatedAvailabilityStatus` via `getItem` avant d'afficher une annonce | vérifié (doc) |
| Fraîcheur imposée | "Displayed item listing information may not be more than six (6) hours older than information displayed on the eBay Site" (API License Agreement, cité au 4.15 du document candidat) | vérifié (document candidat) |
| Licence | API License Agreement eBay (champ d'attribution seulement) | - |

Correspondance des états (identifiants eBay, puis `ProductCondition`) :

| `conditionId` | Libellé eBay | État nudger proposé |
|---|---|---|
| 1000 | New | `NEW` |
| 1500 | New other (libellé **non vérifié**) | `OCCASION` : neuf sans emballage, pas un prix neuf |
| 1750 | New with defects (**non vérifié**) | `OCCASION` |
| 2000 | Certified Refurbished (**non vérifié**) | reconditionné (plan 12), sinon `OCCASION` |
| 2010, 2020, 2030 | Excellent, Very Good, Good - Refurbished | reconditionné, sinon `OCCASION` |
| 2500 | Seller Refurbished | reconditionné, sinon `OCCASION` |
| 3000 | Used | `OCCASION` |
| 7000 | For parts or not working (**non vérifié**) | exclu |

Les identifiants 2010, 2020, 2030 sont réservés aux vendeurs qualifiés du programme reconditionné
d'eBay, qui couvre la France (doc `GetItem`, vérifié). C'est le meilleur signal de qualité de la
source.

## 3. Identifiants et appariement

- On interroge eBay avec le GTIN du produit nudger : l'appariement est natif, au sens où la
  réponse ne contient que des annonces associées à ce GTIN (déclaré par le vendeur ou issu du
  catalogue eBay). Le taux de réponse non vide par GTIN est **non mesuré**.
- Garde-fous, car une annonce peut porter le GTIN d'un téléphone sans être ce téléphone (coque,
  lot, pièce) :
  1. catégorie "Téléphones mobiles et smartphones" de l'arbre EBAY_FR, identifiant à résoudre par
     la Taxonomy API (**non vérifié**) ;
  2. `buyingOptions:{FIXED_PRICE}` : une enchère n'a pas de prix final ;
  3. marque du produit présente dans le titre ;
  4. plancher de prix : écarter un prix inférieur à 10 % du prix neuf médian du produit, (règle
     interne de filtrage des prix aberrants) ;
  5. vendeur : `feedbackPercentage` >= 98 et `feedbackScore` >= 50 (seuils à calibrer).
- `epid` : identifiant produit eBay. On pourrait le conserver pour les appels suivants, mais
  `ExternalIds` n'a pas de champ eBay et l'appel par GTIN suffit. Pas de champ nouveau en v1.
- Mesure à faire dès qu'un compte existe : 200 GTIN de smartphones (les plus fournis en offres),
  un appel chacun ; compter les réponses non vides, la répartition par `conditionId`, la part
  d'annonces rejetées par les garde-fous, et vérifier `EBAY_FR`.

## 4. Modèle cible

Aucun attribut : eBay n'apporte que des offres.

Une réponse produit au plus trois `DataFragment`, un par état (neuf, occasion, reconditionné
quand le plan 12 l'aura créé), chacun portant l'annonce la moins chère de cet état. C'est déjà ce
que fait `AmazonCompletionService.offerFragments` (une offre neuve, une offre d'occasion), et
`PriceAggregationService.pricMerchanteKey` ne garde de toute façon qu'une offre par couple
(source, état).

Contenu du fragment : `datasourceName = datasourceConfigName = "ebay"`, `price` (valeur, devise,
horodatage de l'appel), `productState` selon la table de la section 2, `url = itemWebUrl`,
`affiliatedUrl = itemAffiliateWebUrl`, `shippingCost` tiré de `shippingOptions`, `names = {title}`,
GTIN en `referentielAttributes`.

Champs à créer sur `AggregatedPrice` (`model/.../price/`) :

- `expiresAt` (Long) : fin de validité d'affichage, `horodatage + 6 h` pour eBay, `null` pour
  les autres sources ;
- `ephemeral` (boolean) : offre exclue du prix minimum, du nombre d'offres, des états
  disponibles, de l'historique et des tendances.

Mapping ES : aucun changement. `price.offers` est déjà non indexé (`enabled: false`).

## 5. Pipeline

1. **Client** (à créer), module `services/ebay`, package `org.open4goods.services.ebay`,
   détecté par l'api (`scanBasePackages`). Comme pour Icecat (ADR 0007), le module possède les
   appels et le parsing et ne dépend pas de `api` :
   - `EbayOAuthTokenProvider` : jeton d'application, mis en cache jusqu'à expiration ;
   - `EbayBrowseClient.searchByGtin(gtin)` : un appel, `sort=price`, `limit` de 50, tous les états
     utiles dans un seul filtre `conditionIds`, puis choix de l'annonce la moins chère par état
     côté client ;
   - `EbayProperties`, préfixe `ebay.*` : `enabled`, `client-id`, `client-secret`,
     `marketplace: EBAY_FR`, `campaign-id`, `daily-budget: 5000`, `offer-ttl: 6h`, `cron`.
2. **Completion** (à créer) : `EbayCompletionService extends AbstractCompletionService`, dans
   `api/.../services/completion/`, câblé dans `ApiConfig.completionFacadeService` comme
   `AmazonCompletionService`.
   - `getDatasourceName()` : `"ebay"` ; `shouldProcess` : dernier appel il y a plus de 6 h
     (horodatage dans `Product.datasourceCodes`).
   - `processProduct` : appel, garde-fous de la section 3, fragments, puis
     `aggregator.onDatafragment(df, product)` ; horodatage même en l'absence de résultat.
   - `completeAll` surchargé avec un budget global, sur le modèle
     d'`AmazonCompletionService.completeAll`.
3. **`PriceAggregationService`** (prérequis transverse) :
   - entrée inchangée : `onDataFragment` crée `new AggregatedPrice(fragment)`, dont `url` vaut
     `fragment.affiliatedUrlIfPossible()`, donc `itemAffiliateWebUrl`, et `productState` l'état
     mappé. Le front-api chiffre cette URL dans `affiliationToken`, et `AffiliationController`
     redirige : le lien affilié arrive au clic sans autre changement ;
   - `onProduct` retire, en plus du filtre `VALID_UNTIL_DURATION` (2 jours), toute offre dont
     `expiresAt` est passé ;
   - `computeMinPrice`, `setOffersCount`, `getConditions()`, `recordLastOfferSeen` et
     `computePriceHistory` ignorent les offres `ephemeral`. Sans cela, un prix eBay entrerait
     dans `occasionPricehistory` pour deux ans, deviendrait `price.minPrice` (indexé, utilisé
     pour le tri et l'export ouvert `min_price`), et survivrait bien au-delà de 6 h : une offre
     n'est purgée que lorsque le produit reçoit un nouveau fragment.
4. **front-api** : `ProductMappingService.mapOffers` filtre à la lecture les offres dont
   `expiresAt` est passé, et affiche l'âge de l'offre ("vu sur eBay il y a 2 h"). Les caches de
   front-api et du rendu Nuxt doivent rester sous 6 h pour la fiche produit (durées actuelles
   **non vérifiées**).
5. **Front** : les offres eBay apparaissent dans les lignes d'offres existantes
   (`ProductPriceRows.vue` sépare déjà neuf et occasion), avec le libellé "Annonce eBay" et
   l'état. Elles ne changent ni le prix affiché en tête de fiche ni la courbe d'historique.
6. **Export ouvert** : rien à faire, puisque les offres éphémères ne deviennent jamais
   `minPrice`.
7. **Alertes occasion** : si un matching d'alertes tourne dans `api` sur chaque mise à jour de
   prix, une offre eBay qui passe par `onDatafragment` peut déclencher une alerte "sous X EUR en
   occasion" : la notification affiche un prix frais. Elle ne doit alimenter ni les tendances ni
   le graphique d'historique, pour les raisons du point 3.

## 6. Planification et volumétrie

Un appel par produit et par rafraîchissement.

| Fraîcheur visée | Produits couverts avec 5 000 appels/jour | Part du temps où l'offre est affichable |
|---|---|---|
| 6 h | 1 250 | 100 % |
| 12 h | 2 500 | 50 % |
| 24 h | 5 000 | 25 % |
| 6 h sur toute la verticale (48 759 produits) | 195 036 appels/jour nécessaires | growth check obligatoire |

Le batch de 13 h ne convient pas (un passage par jour). Proposition : tâche dédiée
`ebay.cron: "0 0 */6 * * *"`, 1 250 produits par passage, budget journalier vérifié par un
compteur.

Sélection des produits prioritaires (à créer, `EbayPrioritySelector`), dans cet ordre :

1. GTIN portant une alerte occasion active, si un service d'alertes existe ;
2. smartphones les plus consultés (export Plausible ; l'API Plausible était indisponible le
   2026-11-08, donc volume de pages vues **non mesuré**) ;
3. à défaut, smartphones avec le plus d'offres (`ProductRepository.exportVerticalWithOffersCountGreater`).

## 7. Sourcing transparent

- `open4goods-config/datasources/ebay.yml` **(à créer)**, sans bloc `csvDatasource` :
  `name: "eBay"`, `portalUrl: https://www.ebay.fr`, `attribution: "Annonces eBay, prix relevé il y
  a moins de 6 h"`, licence "eBay API License Agreement" (champ d'attribution seulement).
- `SourceAttribution` par produit : `sourceRecordId = itemId`, `recordUrl = itemWebUrl` (non
  affiliée), `sourceUpdatedAt` = en-tête `Date` de la réponse (la doc eBay le recommande comme
  date "as of"), `retrievedAt` = date de l'appel.
- Affichage : âge de l'offre et vendeur.

## 8. Tests et critères d'acceptation

- Tests unitaires du client sur des réponses enregistrées (bac à sable eBay ou première réponse
  de prod) : correspondance des états, garde-fous (catégorie, enchère, marque, plancher,
  vendeur), absence de `itemAffiliateWebUrl` sans `campaign-id`.
- Tests de `PriceAggregationService` :
  - une offre éphémère n'est ni `minPrice`, ni comptée dans `offersCount`, ni dans
    `conditions`, ni dans l'historique ;
  - une offre dont `expiresAt` est passé disparaît à l'agrégation suivante ;
  - les autres sources ne changent pas (non-régression GOU-1699 sur `preferredOffer`).
- Test de `ProductMappingService.mapOffers` : une offre expirée n'est pas exposée, même si le
  produit n'a pas été réagrégé.
- Critères en beta :
  - taux de réponse non vide sur l'échantillon de 200 GTIN publié dans le rapport ;
  - âge maximal d'une offre eBay affichée inférieur à 6 h (métrique Micrometer) ;
  - consommation du quota inférieure à 90 % ;
  - zéro accessoire affiché sur un échantillon de 50 produits vérifiés à la main.

## 9. Découpage

1. Compte développeur eBay, compte EPN, `campaign-id` (XS, humain).
2. Mesure sur 200 GTIN avec un script hors dépôt (S).
3. Offres éphémères dans `AggregatedPrice`, `PriceAggregationService` et front-api, avec tests
   (S à M). Prérequis réutilisable par toute source à fraîcheur courte.
4. Module `services/ebay` : OAuth, client, propriétés (S).
5. `EbayCompletionService`, tâche planifiée, sélection des produits (S).
6. Affichage front (S).
7. Growth check si la mesure justifie plus de 1 250 produits (XS, délai de 5 à 7 jours ouvrés).

## 10. Risques, challenges, questions

- **Le pipeline n'est pas fait pour 6 h.** Les offres vivent dans `Product.price`, persisté et
  indexé, et ne sont purgées qu'à l'arrivée d'un nouveau fragment. Le point 3 de la section 5
  colmate, mais c'est une exception dans un agrégat pensé pour des flux quotidiens.
  **Alternative proposée** : ne jamais persister les offres eBay. front-api interroge eBay à
  l'affichage de la fiche (chargement asynchrone côté client), avec un cache Caffeine borné de
  6 h. Plus simple et conforme par construction, mais c'est un appel externe dans front-api, qui
  a déjà subi des OOM, et le matching des alertes (dans `api`) ne verrait plus ces offres.
- **Quota minuscule face à la verticale** : 1 250 produits frais sur 48 759. Sans growth check,
  eBay reste un complément sur les pages les plus vues, pas une couverture.
- **Interdiction des statistiques de prix** : le document candidat (4.15) relève l'interdiction de
  dériver un prix moyen de vente. C'est en tension directe avec l'historique et les tendances de
  prix, d'où l'exclusion stricte de l'historique.
- **Clause de concurrence** (document candidat 4.15) : un comparateur peut être jugé
  "competitive". eBay peut retirer la clé : la source doit rester optionnelle (`ebay.enabled`),
  sans dépendance d'affichage.
- **Qualité du GTIN des annonces** de particuliers : probablement faible, **non mesuré**. La
  mesure du point 2 décide de la suite.
- Questions au propriétaire :
  1. Avons-nous déjà un compte développeur eBay ou un compte EPN ?
  2. eBay sert-il nudger.fr, ou seulement des alertes occasion ? La réponse fixe la priorité (P3 ou
     P2) et la sélection des produits.
  3. Préfères-tu la voie `api` (compatible avec le matching des alertes, mais exceptions dans
     `PriceAggregationService`) ou la voie front-api sans persistance ?
