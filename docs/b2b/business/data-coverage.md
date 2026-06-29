# Product Data API - Couverture de donnees nudger (etude mesuree)

> Décisions canoniques : [`00-canonical-decisions.md`](../00-canonical-decisions.md).
> Annexe de [`master-prompt.md`](../implementation/master-prompt.md). Documents jumeaux :
> [`competition.md`](competition.md) (étude concurrentielle) et
> [`facet-catalog.md`](../product/facet-catalog.md) (catalogue de facettes et barème crédits,
> issus de cette étude).
> Investigation menée sur le modèle `Product.java`, les DTO existants de `front-api`, et
> **mesurée en live sur l'Elasticsearch de production** (`products-moustik`, profil devsec) le
> **2026-06-02**.

## 1. Méthodologie

1. **Modèle** : analyse de `model/.../product/Product.java` et de ses blocs
   (`AggregatedPrices`, `Score`, `EprelProduct`, `ProductAttributes`, `Resource`...).
2. **DTO existants** : inventaire des 40 DTO de `front-api/.../dto/product/` - la couche de mapping
   `Product (ES) → DTO sanitisé` **existe déjà** et doit être réutilisée (cf. `ProductMappingService`).
3. **Volumétrie réelle** : requêtes `_count` / `_search` (aggs) sur l'index de prod pour mesurer la
   **couverture effective** de chaque champ - la priorisation est *data-driven*, pas théorique.
   Les requêtes utilisées sont consignées en §4 pour que la mesure soit **reproductible**.

> ⚠️ Limite ES : plusieurs blocs sont stockés mais **non indexés** (`ranking` → `enabled:false` ;
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
| **Curé premium** | ~10-50 K produits | ImpactScore, énergie, réparabilité, 7 verticales | Facettes **exclusives**, **marge**, faible volume |

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
| `vertical` + `excluded=false` (affichés) | 10 286 | premium UX | Le " vrai " catalogue éditorial |

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
- `eprelDatas` (`dynamic:false`) : présent surtout sur électroménager + TV (énergie ≈ 47,5 K via le score).
- `googleTaxonomyId` / référentiels multi-taxonomies : portés par la config verticale, pas par doc indexé.

> **À faire avant chiffrage premium** : exposer un compteur applicatif (côté pipeline) du nombre réel de
> produits avec `eprelDatas` non null - non récupérable par requête ES seule.

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
| `eprelDatas` (`EprelProduct`) | `ProductEprelDto` | `product.energy` | non (`dynamic:false`) |
| `resources` (`Resource`) | `ProductResourcesDto`, `ProductImageDto`, `ProductPdfDto`, `ProductVideoDto` | `product.images` / `product.documents` | partiel |
| `ranking` (`EcoScoreRanking`) | `ProductRankingDto` | `product.impact` (rang) | non |

**Règle de sanitisation B2B** (issue de [`master-prompt.md`](../implementation/master-prompt.md)) :
ne jamais exposer `compensation`, `affiliationToken`, identifiants datasource internes, clés
cache/crawler, scoring debug. `AggregatedPrice` porte `compensation` + `affiliationToken` →
**à retirer impérativement** dans le DTO B2B.

---

## 4. Requetes de mesure (reproduire la couverture)

Toutes les mesures de §2 sont des `_count` / `_search` (aggs) sur l'index `products-moustik`
(cluster devsec/prod, voir le [runbook](../../operations/product-data-api-local-runbook.md)).
Avant toute campagne de mesure : **verifier les noms de champs contre le mapping live**
(`GET products-moustik/_mapping`) - le modele evolue.

```bash
ES=http://<devsec-es-host>:9200
IDX=products-moustik

# Total index
curl -s "$ES/$IDX/_count"

# offersCount > 0 / >= 2 / >= 5  (socle de la facette price)
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"range":{"offersCount":{"gt":0}}}}'
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"range":{"offersCount":{"gte":2}}}}'
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"range":{"offersCount":{"gte":5}}}}'

# Fraicheur : produits a offres rafraichis dans les 30 jours
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"bool":{"filter":[
        {"range":{"offersCount":{"gt":0}}},
        {"range":{"lastChange":{"gte":"now-30d/d"}}}]}}}'

# Presence d un bloc (exemple : attributs indexes, scores proprietaires)
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"exists":{"field":"attributes.indexed"}}}'
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"exists":{"field":"scores.ECOSCORE.value"}}}'

# Croisements (curee + offres)
curl -s "$ES/$IDX/_count" -H 'Content-Type: application/json' \
  -d '{"query":{"bool":{"filter":[
        {"exists":{"field":"vertical"}},
        {"range":{"offersCount":{"gt":0}}}]}}}'

# Repartition par verticale
curl -s "$ES/$IDX/_search?size=0" -H 'Content-Type: application/json' \
  -d '{"aggs":{"verticals":{"terms":{"field":"vertical","size":20}}}}'
```

Chaque spec de facette ([`docs/b2b/facets/`](../facets/README.md)) consigne **ses** requetes de
couverture et de qualite ; ce paragraphe est la reference commune de methode. Les blocs non
indexes (`reviews`, `eprelDatas`, `ranking`) ne sont **pas** mesurables ainsi : prevoir un
compteur applicatif cote pipeline (voir §2.3).

---

## Suite de l etude

- Catalogue de facettes priorise, bareme credits, vagues d implementation et questions
  ouvertes produit : [`facet-catalog.md`](../product/facet-catalog.md).
- Positionnement concurrentiel : [`competition.md`](competition.md).
