# Product Data API - Catalogue de facettes priorise

> Décisions canoniques : [`00-canonical-decisions.md`](../00-canonical-decisions.md).
> Issu de l'étude de couverture mesurée [`data-coverage.md`](../business/data-coverage.md)
> (2026-06-02) et de l'étude concurrentielle [`competition.md`](../business/competition.md).
> Chaque facette livrée possède sa **spec dédiée** sous [`docs/b2b/facets/`](../facets/README.md) ;
> ce document est la vue d'ensemble produit (priorisation, barème, vagues).

## 1. Catalogue de facettes nudger-native (priorisé)

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
| 9 | `product.taxonomy` | `/products/{gtin}/taxonomy` | référentiels Google/ICECAT/ETIM/Wikidata | curé | 15 | **interne EXCLUSIF** | mapping multi-taxonomies |
| 10 | _exp._ `product.alternatives` | `/products/{gtin}/alternatives` | embedding KNN / ranking | curé | 20 | interne (KNN) | reco mieux notés |

⭐ socle v1 · ⭐⭐ différenciation premium · ⭐⭐⭐ premium fort

**Détail des facettes propriétaires (le vrai pitch) :**

- **`product.impact`** - `scores.ECOSCORE` (ImpactScore©), `REPAIRABILITY_INDEX` (indice FR),
  `DURABILITY`, `BRAND_SUSTAINABILITY`, `WARRANTY`, + `ranking` (rang dans la verticale). DTO :
  `ProductScoresDto`. Chaque score porte `value`, `absolute`/`relativ` (cardinalités), `letter()` (A-E).
  → **Aucun concurrent du panel n'a cette donnée.**
- **`product.energy`** - `eprelDatas` (EPREL UE) + score `CLASSE_ENERGY` (et HDR/SDR pour TV). DTO :
  `ProductEprelDto`. Vendable sur électroménager + TV.
---

## 2. Facettes " marché " non couvertes par nudger

L'étude concurrentielle liste des facettes **scraping/marketplace** que nudger **ne possède pas** dans
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

## 3. Barème crédits recommandé (adapté à la couverture)

| Niveau | Facettes | Crédits | € / appel | Justification |
|---|---|---:|---:|---|
| XS | `identity` | 1 | 0,002 € | cache long, coût marginal nul |
| M | `attributes`, `images`, `documents` | 3-4 | 0,006-0,008 € | enrichissement |
| **Baseline** | **`price`** | **5** | **0,010 €** | aligné PriceAPI Starter, volume |
| L | `price-history` | 8 | 0,016 € | persistance série temporelle |
| **Premium** | **`energy`**, **`impact`**, **`taxonomy`** | 10-15 | 0,020-0,030 € | **exclusif**, faible volume, forte valeur |

Don de bienvenue freemium : **2 500 crédits** (≈ 5 €) - cohérent avec
[`master-prompt.md`](../implementation/master-prompt.md).

**Lecture stratégique** : la facette `price` reste un **produit d'appel loyal** (no-data-no-pay,
fraîcheur réelle excellente). La marge vient des facettes propriétaires `impact`/`energy`,
**impossibles à répliquer** par la concurrence (cf. [`competition.md`](../business/competition.md) §6).

---

## 4. Vagues d'implémentation

Aligné sur [`master-prompt.md`](../implementation/master-prompt.md) (v1 = facette prix uniquement).
**L'ordre suit la valeur commerciale × la réutilisation d'infra**, pas l'abondance de données.
Chaque nouvelle facette suit le **cycle de vie** décrit dans [`facets/README.md`](../facets/README.md)
(spec dédiée générée par IA, mesure de couverture, SEO, checklist de lancement).

### Vague 0 - v1 (périmètre du master prompt, à livrer en premier)
- **`product.price`** seule - spec : [`facets/product-price.md`](../facets/product-price.md).
  GET synchrone GTIN-first, validation GTIN avant débit, no-data-no-pay,
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
- `product.taxonomy` (15 cr.) - mapping multi-référentiels.

### Vague 3 - expérimental / roadmap
- `product.alternatives` (KNN embedding, 20 cr.).
- Facettes marché (availability/shipping/ranking) **uniquement si** décision de supply externe (§2).

**Effort de mapping par facette** : faible pour vague 1-2 (DTO front-api existants), à condition
d'extraire la logique de `ProductMappingService` dans un service B2B partagé.

---

## 5. Questions ouvertes

> **Statut (voir [`00-canonical-decisions.md`](../00-canonical-decisions.md)) :**
> Q2 **tranchée** -> GTIN-first strict en v1. Q6 **tranchée** -> `meta.coverage`
> par facette dans l'enveloppe. Q1/Q3/Q4 restent ouvertes (radar roadmap).

1. **Couverture EPREL non mesurable par ES** : exposer un compteur applicatif côté pipeline pour
   chiffrer le volume premium réel avant de communiquer sur cette facette ? (recommandé)
2. **GTIN-first strict** ou accepter rapidement `asin`/`mpn`/`merchant_sku`/`keyword` en entrée ?
3. **Facettes marché (scraping)** : assumer durablement le " non ", ou prévoir une supply hybride (crawl /
   affiliation) pour `availability`/`shipping`/`ranking` ?
4. **Séparation live vs derived/stored** : isoler dès maintenant les facettes persistées (history, ranking)
   pour éviter l'explosion des coûts de collecte ?
5. **Verticales** : commercialiser les facettes premium uniquement sur les 7 verticales existantes, avec un
   `meta.coverage` explicite par facette dans l'enveloppe de réponse ?
