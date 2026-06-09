# Product Data API - Capacités nudger & plan de facettes priorisé

> Annexe de [`b2B.md`](b2B.md). Document jumeau : [`b2b-conccurrence.md`](b2b-conccurrence.md).
> Investigation menée sur le modèle `Product.java`, les DTO existants de `front-api`, et
> **mesurée en live sur l'Elasticsearch de production** (`products-moustik`, profil devsec) le
> **2026-06-02**.

## 1. Méthodologie

1. **Modèle** : analyse de `model/.../product/Product.java` (1 325 lignes) et de ses blocs
   (`AggregatedPrices`, `Score`, `AiReview`, `EprelProduct`, `ProductAttributes`, `Resource`...).
2. **DTO existants** : inventaire des 40 DTO de `front-api/.../dto/product/` - la couche de mapping
   `Product (ES) → DTO sanitisé` **existe déjà** et doit être réutilisée (cf. `ProductMappingService`).
3. **Volumétrie réelle** : requêtes `_count` / `_search` (aggs) sur l'index de prod pour mesurer la
   **couverture effective** de chaque champ - la priorisation est *data-driven*, pas théorique.

> ⚠️ Limite ES : plusieurs blocs sont stockés mais **non indexés** (`reviews`, `ranking` → `enabled:false` ;
> `eprelDatas` → `dynamic:false` ; `coverImagePath`, `googleTaxonomyId` → non indexés). Leur couverture
> **ne peut pas être comptée par requête** ; elle est estimée à partir du sous-ensemble curé (voir §2.3).

---

## 2. Capacités nudger mesurées (2026-06-02)

Index `products-moustik` : **83 168 478 documents**.

### 2.1 Deux populations très différentes

nudger n'a pas *une* base mais **deux**, qu'il faut traiter distinctement dans le pricing et le discours :

| Population | Volume | Nature | Stratégie B2B |
|---|---|---|---|
| **Masse commodité** | ~34 M GTIN avec offres | Prix/offres scrapés multi-marchands, attributs Icecat | Facette `price` d'appel, **volume**, no-data-no-pay |
| **Curé premium** | ~10-50 K produits | ImpactScore, énergie, réparabilité, review IA, 7 verticales | Facettes **exclusives**, **marge**, faible volume |

### 2.2 Couverture - facette PRIX & enrichissement de masse

| Champ / capacité | Documents | % du total | Facette cible | Lecture |
|---|---:|---:|---|---|
| Total index | 83 168 478 | 100 % | - | - |
| `offersCount > 0` (≥ 1 offre) | **33 894 029** | 40,8 % | `product.price` | Socle prix exploitable |
| `offersCount ≥ 2` (multi-marchand) | **3 914 220** | 4,7 % | `product.price` | Vraie comparaison prix |
| `offersCount ≥ 5` | 153 818 | 0,18 % | `product.price` | Profondeur marché |
| `attributes.indexed` présent | 33 961 365 | 40,8 % | `product.attributes` | ≈ adossé aux produits à offres |
| `resources` (images/PDF) présent | 125 610 | 0,15 % | `product.images` / `product.documents` | **faible** - surtout sur le curé |
| `lastChange ≤ 30 j` parmi offres > 0 | 33 894 029 | 100 % des offres | freshness `price` | **Toutes les offres sont fraîches** ✅ |

> **Conséquence majeure** : la fraîcheur du prix est excellente (100 % des produits à offres ont été
> rafraîchis dans les 30 jours). Le " no-data-no-pay " sur la fraîcheur sera donc **rarement déclenché**
> sur la masse → bonne nouvelle pour la conversion, argument commercial honnête.

### 2.3 Couverture - facettes PROPRIÉTAIRES (différenciation)

| Champ (score) | Documents | Facette cible | Différenciation |
|---|---:|---|---|
| `scores.REPAIRABILITY_INDEX.value` | **49 640** | `product.impact` | Indice réparabilité FR - **exclusif** |
| `scores.CLASSE_ENERGY.value` | **47 521** | `product.energy` | Classe énergie UE (proxy EPREL) - **exclusif** |
| `scores.ECOSCORE.value` (ImpactScore©) | **45 123** | `product.impact` | Score écologique nudger - **exclusif** |
| `scores.DURABILITY.value` | 10 652 | `product.impact` | Durabilité |
| `scores.REPARABILITY.value` | 6 428 | `product.impact` | Réparabilité (legacy) |
| `scores.ESG.value` | 48 | - | Négligeable (ignorer en v1) |
| `vertical` présent (curé) | **87 609** | toutes premium | Catalogue curé |
| `vertical` + `offersCount > 0` | 26 643 | `impact` + `price` croisés | **Cœur monétisable premium** |
| `vertical` + `ECOSCORE` | 13 274 | `product.impact` vendable | Curé scoré ET affichable |
| `vertical` + `excluded=false` (affichés) | 10 286 | review IA, premium UX | Le " vrai " catalogue éditorial |

**Répartition des 7 verticales curées :**

| Vertical | Produits |
|---|---:|
| smartphones | 48 759 |
| refrigerator | 10 912 |
| tv | 9 103 |
| oven | 7 234 |
| washing-machine | 6 700 |
| dishwasher | 3 870 |
| air-conditioner | 1 031 |

**Non mesurable par ES** (stocké, non indexé) - couverture **estimée** :
- `reviews` (revue IA, `enabled:false`) : générée sur le sous-ensemble **affiché** (~10 K), partiellement.
- `eprelDatas` (`dynamic:false`) : présent surtout sur électroménager + TV (énergie ≈ 47,5 K via le score).
- `googleTaxonomyId` / référentiels multi-taxonomies : portés par la config verticale, pas par doc indexé.

> **À faire avant chiffrage premium** : exposer un compteur applicatif (côté pipeline) du nombre réel de
> produits avec `reviews[lang]` non vide et `eprelDatas` non null - non récupérable par requête ES seule.

---

## 3. Mapping modèle → DTO existants (réutilisation maximale)

La couche de mapping existe déjà dans `front-api`. **Ne pas réécrire** : copier/adapter en DTO B2B sanitisés.

| Bloc `Product` | DTO `front-api` réutilisable | Facette B2B | Indexé ES ? |
|---|---|---|---|
| `id`, `externalIds`, `names`, `gtinInfos`, `vertical` | `ProductBaseDto`, `ProductIdentityDto`, `ProductNamesDto`, `ProductGtinInfoDto`, `ProductExternalIdsDto` | `product.identity` | partiel |
| `price` (`AggregatedPrices`) | `ProductOffersDto`, `ProductAggregatedPriceDto` | `product.price` | `offersCount` only |
| `price.newPricehistory` / `trends` | `ProductPriceHistoryDto`, `ProductPriceHistoryEntryDto`, `ProductPriceTrendDto`, `PriceTrendState` | `product.price-history` | non |
| `attributes` (`ProductAttributes`, Icecat) | `ProductAttributesDto`, `ProductAttributeDto`, `ProductIndexedAttributeDto` | `product.attributes` | `attributes.indexed` |
| `scores` (`Map<String,Score>`) | `ProductScoresDto`, `ProductScoreDto`, `ProductCardinalityDto` | `product.impact` | `scores.*.value` ✅ |
| `reviews` (`AiReviewHolder`) | `ProductAiReviewDto`, `AiReviewDto`, `AiReviewAttributeDto`, `AiReviewSourceDto` | `product.review` | non (`enabled:false`) |
| `eprelDatas` (`EprelProduct`) | `ProductEprelDto` | `product.energy` | non (`dynamic:false`) |
| `resources` (`Resource`) | `ProductResourcesDto`, `ProductImageDto`, `ProductPdfDto`, `ProductVideoDto` | `product.images` / `product.documents` | partiel |
| `ranking` (`EcoScoreRanking`) | `ProductRankingDto` | `product.impact` (rang) | non |

**Règle de sanitisation B2B** (issue de `b2B.md`) : ne jamais exposer `compensation`, `affiliationToken`,
identifiants datasource internes, clés cache/crawler, scoring debug. `AggregatedPrice` porte
`compensation` + `affiliationToken` → **à retirer impérativement** dans le DTO B2B.

---

## 4. Catalogue de facettes nudger-native (priorisé)

> Crédits indicatifs, **100 % pilotés YAML** (`b2b-catalog.yml`). Unité : `1 crédit ≈ 0,002 €`.
> Règle transverse **" no-data-no-pay "** : une facette vide / périmée / sans donnée n'est **pas** débitée.

| # | Facette | Endpoint | Source modèle | Couverture | Crédits | Supply | Différenciation |
|---|---|---|---|---:|---:|---|---|
| 1 | `product.price` ⭐ | `/products/{gtin}/price` | `price` (offers, min/best/condition) | 33,9 M | **5** | interne ES | commodité, **volume** |
| 2 | `product.identity` | `/products/{gtin}/identity` | base/identity/names/gtinInfos | ~34 M | 1 | interne ES | fallback/matching |
| 3 | `product.attributes` | `/products/{gtin}/attributes` | `attributes` (Icecat) | 34 M | 4 | interne ES | specs mappées |
| 4 | `product.images` | `/products/{gtin}/images` | `resources` IMAGE | 125 K | 3 | interne ES | UX |
| 5 | `product.documents` | `/products/{gtin}/documents` | `resources` PDF | <125 K | 3 | interne ES | datasheets/manuels |
| 6 | `product.price-history` | `/products/{gtin}/price/history` | `price` history/trends | ⊂ 34 M | 8 | interne (persisté) | série temporelle |
| 7 | `product.impact` ⭐⭐ | `/products/{gtin}/impact` | `scores` (ECOSCORE, réparabilité, durabilité, ranking) | ~45-50 K | **15** | **interne EXCLUSIF** | **éco / réparabilité / CO₂** |
| 8 | `product.energy` ⭐⭐ | `/products/{gtin}/energy` | `eprelDatas` + `CLASSE_ENERGY` | ~47 K | 10 | **interne EXCLUSIF** | **étiquette énergie UE** |
| 9 | `product.review` ⭐⭐⭐ | `/products/{gtin}/review` | `reviews` (`AiReview` multi-niveaux) | ~10 K | **30** | **interne EXCLUSIF** | **revue IA sourcée** |
| 10 | `product.taxonomy` | `/products/{gtin}/taxonomy` | référentiels Google/ICECAT/ETIM/Wikidata | curé | 15 | **interne EXCLUSIF** | mapping multi-taxonomies |
| 11 | _exp._ `product.alternatives` | `/products/{gtin}/alternatives` | embedding KNN / ranking | curé | 20 | interne (KNN) | reco mieux notés |

⭐ socle v1 · ⭐⭐ différenciation premium · ⭐⭐⭐ premium fort

**Détail des facettes propriétaires (le vrai pitch) :**

- **`product.impact`** - `scores.ECOSCORE` (ImpactScore©), `REPAIRABILITY_INDEX` (indice FR),
  `DURABILITY`, `BRAND_SUSTAINABILITY`, `WARRANTY`, + `ranking` (rang dans la verticale). DTO :
  `ProductScoresDto`. Chaque score porte `value`, `absolute`/`relativ` (cardinalités), `letter()` (A-E).
  → **Aucun concurrent du panel n'a cette donnée.**
- **`product.energy`** - `eprelDatas` (EPREL UE) + score `CLASSE_ENERGY` (et HDR/SDR pour TV). DTO :
  `ProductEprelDto`. Vendable sur électroménager + TV.
- **`product.review`** - `AiReview` : `technicalReview`/`ecologicalReview`/`communityReview` en 3 niveaux
  (novice/intermédiaire/avancé), `pros`/`cons`, `sources` (sourcing vérifiable), `ratings`,
  `manufacturingCountry`, `obsolescenceWarning`, `dataQuality`. DTO : `ProductAiReviewDto`. **Facette la
  plus chère** (30 cr.) : coût de génération réel + responsabilité éditoriale + exclusivité totale.

---

## 5. Facettes " marché " non couvertes par nudger

L'annexe concurrentielle liste des facettes **scraping/marketplace** que nudger **ne possède pas** dans
`products-moustik`. Elles ne doivent **pas** entrer dans le catalogue v1/v2 sans décision de supply externe :

| Facette annexe | Statut nudger | Voie d'acquisition éventuelle |
|---|---|---|
| `product.availability` / `stockLevel` | ❌ pas de stock fiable par offre | crawl live ou connecteur affiliation |
| `product.shipping` / `returnPolicy` | ❌ non collecté | crawl PDP marchand |
| `product.buyBox` / `seller.reputation` | ❌ marketplace-specific | scraping Amazon/marketplace |
| `product.searchRanking` / `shareOfShelf` | ❌ non collecté | scraping SERP shopping |
| `category.topSellers` | ⚠️ partiel via `ranking` interne (éco), pas ventes | scraping best-sellers |
| `product.mapCompliance` | ❌ | price-history interne + tracking seller (faisable plus tard) |

> **Recommandation** : assumer que la **force de nudger n'est pas le scraping marketplace** mais la
> donnée propriétaire. Ces facettes restent en radar (roadmap supply hybride), pas en v1.

---

## 6. Barème crédits recommandé (adapté à la couverture)

| Niveau | Facettes | Crédits | € / appel | Justification |
|---|---|---:|---:|---|
| XS | `identity` | 1 | 0,002 € | cache long, coût marginal nul |
| M | `attributes`, `images`, `documents` | 3-4 | 0,006-0,008 € | enrichissement |
| **Baseline** | **`price`** | **5** | **0,010 €** | aligné PriceAPI Starter, volume |
| L | `price-history` | 8 | 0,016 € | persistance série temporelle |
| **Premium** | **`energy`**, **`impact`**, **`taxonomy`** | 10-15 | 0,020-0,030 € | **exclusif**, faible volume, forte valeur |
| Premium++ | **`review`** | 30 | 0,060 € | coût génération + exclusivité + responsabilité |

Don de bienvenue freemium : **2 500 crédits** (≈ 5 €) - cohérent avec `b2B.md`.

**Lecture stratégique** : la facette `price` reste un **produit d'appel loyal** (no-data-no-pay,
fraîcheur réelle excellente). La marge vient des facettes propriétaires `impact`/`energy`/`review`,
**impossibles à répliquer** par la concurrence (cf. [`b2b-conccurrence.md`](b2b-conccurrence.md) §6).

---

## 7. Plan d'implémentation priorisé

Aligné sur `b2B.md` (v1 = facette prix uniquement) et le plan
`~/.claude/plans/prompt-b2b-on-peaceful-peacock.md`. **L'ordre suit la valeur commerciale × la
réutilisation d'infra**, pas l'abondance de données.

### Vague 0 - v1 (périmètre `b2B.md`, à livrer en premier)
- **`product.price`** seule. GET synchrone GTIN-first, validation GTIN avant débit, no-data-no-pay,
  metering Redis + ledger Postgres, sanitisation (`compensation`/`affiliationToken` retirés).
- Patron de référence (auth + crédits + doc + playground) dupliqué ensuite.

### Vague 1 - enrichissement de masse (réutilise l'infra v1, même supply ES)
- `product.identity` (1 cr.) - friction nulle, améliore playground/matching.
- `product.attributes` (4 cr.) - ouvre le marché catalogue (34 M couverts).
- `product.images` + `product.documents` (3 cr.) - UX, couverture faible → no-data-no-pay protège.
- `product.price-history` (8 cr.) - persistance interne, forte rétention.

### Vague 2 - différenciation propriétaire (le vrai pitch, marge)
- **`product.impact`** (15 cr.) - DTO `ProductScoresDto` déjà existant, données indexées ES ✅.
- **`product.energy`** (10 cr.) - `ProductEprelDto`, vendable électroménager + TV.
- **`product.review`** (30 cr.) - `ProductAiReviewDto`, facette signature.
- `product.taxonomy` (15 cr.) - mapping multi-référentiels.

### Vague 3 - expérimental / roadmap
- `product.alternatives` (KNN embedding, 20 cr.).
- Facettes marché (availability/shipping/ranking) **uniquement si** décision de supply externe (§5).

**Effort de mapping par facette** : faible pour vague 1-2 (DTO front-api existants), à condition
d'extraire la logique de `ProductMappingService` dans un service B2B partagé.

---

## 8. Questions ouvertes

1. **Couverture review/EPREL non mesurable par ES** : exposer un compteur applicatif côté pipeline pour
   chiffrer le volume premium réel avant de communiquer sur ces facettes ? (recommandé)
2. **GTIN-first strict** ou accepter rapidement `asin`/`mpn`/`merchant_sku`/`keyword` en entrée ?
3. **Facettes marché (scraping)** : assumer durablement le " non ", ou prévoir une supply hybride (crawl /
   affiliation) pour `availability`/`shipping`/`ranking` ?
4. **Séparation live vs derived/stored** : isoler dès maintenant les facettes persistées (history, ranking)
   pour éviter l'explosion des coûts de collecte ?
5. **`product.review` UGC/IA** : exposer la revue complète multi-niveaux, ou un résumé + `sources` seulement,
   pour limiter la responsabilité éditoriale en B2B ?
6. **Verticales** : commercialiser les facettes premium uniquement sur les 7 verticales existantes, avec un
   `meta.coverage` explicite par facette dans l'enveloppe de réponse ?
