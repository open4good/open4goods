# Product Data API - Étude concurrentielle détaillée

> Décisions canoniques : [`00-canonical-decisions.md`](../00-canonical-decisions.md).
> Annexe de [`master-prompt.md`](../implementation/master-prompt.md). Document jumeau : [`data-coverage.md`](data-coverage.md).
> Pricing vérifié en live le **2026-06-02** pour les benchmarks structurants (PriceAPI,
> SerpApi, Winamaz, Bright Data). Les autres acteurs reprennent les données de l'annexe
> de cadrage, signalées comme non re-vérifiées.

## 1. Résumé exécutif

Le marché des API " données produit orientées prix " se range en **trois archétypes** :

1. **Scraping e-commerce normalisé, job-based** - *PriceAPI, SerpApi, Bright Data, Oxylabs,
   Decodo, Piloterr*. Ils monétisent la **collecte structurée** depuis des sources publiques.
   Le coût suit le volume, le type de cible, le nombre de pages ou la complexité de requête.
2. **Affiliation / comparaison marchande** - *Winamaz*. La donnée prix est adossée aux réseaux
   d'affiliation et aux marchands partenaires ; monétisation par le trafic et la conversion.
3. **Bases produit / code-barres enrichies** - *Barcode Lookup, Go-UPC*. Excellence sur
   l'**identification** et l'enrichissement, faiblesse sur la surveillance prix temps réel.

**Aucun de ces acteurs ne possède la donnée différenciante de nudger** (ImpactScore© écologique,
revues IA sourcées, étiquette énergie EPREL, indice de réparabilité, référentiels multi-taxonomies).
Tous vendent une **commodité** : du prix scrapé ou une fiche code-barres. Le positionnement de
Product Data API ne doit donc **pas** se jouer sur leur terrain (couverture brute, profondeur de
scraping) mais sur deux axes qu'eux ne peuvent pas tenir :

- **Une promesse contractuelle plus nette** sur la facette prix : *GTIN-first, réponse normalisée,
  provenance + fraîcheur explicites, et " no-data-no-pay " strict par facette*. C'est plus lisible
  que le scraping généraliste et plus robuste que l'affiliation.
- **Des facettes propriétaires** (impact, review IA, énergie, réparabilité) qu'**aucun concurrent
  ne propose**. Voir [`facet-catalog.md`](../product/facet-catalog.md).

**Signal de facturation clé** : les concurrents n'ont pas la même définition d'un appel " facturable ".

| Acteur | Que facture-t-il réellement ? |
|---|---|
| SerpApi | Recherches **réussies** uniquement - mais **un résultat vide réussi consomme 1 crédit** |
| Oxylabs | " Pay only for successful results " |
| Bright Data | " Pay only for success " (par record livré) |
| Winamaz | 1 crédit dès qu'un appel renvoie **un résultat contenant un EAN** |
| PriceAPI | Facture aussi les produits **" not found "** dès lors qu'ils ont pu être recherchés à la source |

→ Product Data API peut se différencier en ne facturant **que la donnée fraîche et exploitable de la
facette demandée** : ni GTIN invalide, ni produit absent, ni facette vide, ni prix périmé. Plus
agressif et plus simple à expliquer que **tous** les concurrents observés.

**Visibilité France** : peu d'acteurs réellement *France-first* avec docs + pricing publics + API
produit self-serve. L'offre francophone lisible est surtout de l'affiliation (Winamaz). Les
concurrents les plus structurés pour une architecture réutilisable à facettes sont européens
(PriceAPI/Allemagne) ou globaux. **PriceAPI reste le benchmark structurel le plus proche** d'un B2B
" price vertical first ", même si sa convention de coût et sa politique de facturation diffèrent de
la cible.

---

## 2. Périmètre et critères de concurrence directe

Un concurrent est retenu comme **direct** s'il remplit la majorité des critères suivants :

| Critère | Exigence |
|---|---|
| Nature | API B2B / endpoint développeur documenté publiquement |
| Donnée cœur | Prix, offres, disponibilité ou données produit marchandes exploitables |
| Accès | Intégration self-serve ou semi self-serve, docs et/ou pricing publics |
| Granularité | Réponse structurée à l'échelle produit / offre / seller / recherche |
| Usage cible | Repricing, monitoring concurrentiel, enrichissement catalogue, market intelligence |
| Facturation | Quotas, crédits, volume mensuel, per-request, ou complexité de requête |

Sont exclus de la comparaison centrale : les simples revendeurs de proxies sans endpoints
e-commerce dédiés, et les suites enterprise sans docs/pricing publics exploitables.

---

## 3. Vue comparative synthétique (pricing vérifié 2026-06-02)

| Acteur | Siège | Produit | Modèle de pricing | Échantillon de prix publics | Statut vérif. |
|---|---|---|---|---|---|
| **PriceAPI** / metoda GmbH | Allemagne | Price API | Abonnement mensuel + overage par crédit | Go **99 €**/5 000 cr. · Basic **259 €**/20 000 · Starter **499 €**/50 000 · Advanced **999 €**/200 000 · Pro **1 499 €**/500 000 ; essai **1 000 crédits** sans CB | ✅ confirmé |
| **SerpApi**, LLC | États-Unis | Google Shopping / Amazon Product API | Abonnement par nb de recherches | Free **250** · Starter **25 $**/1 000 · Developer **75 $**/5 000 · Production **150 $**/15 000 · Big Data **275 $**/30 000 | ✅ confirmé |
| **Winamaz** / BFIRST OÜ | Estonie | Winamaz API | Abonnement à crédits/résultats | Novice **30 €** HT/20 000 cr. · Explorer **59 €**/60 000 · Pro **118 €**/180 000 · Advanced **236 €**/540 000 · Expert **472 €**/1 620 000 | ✅ confirmé |
| **Bright Data** Ltd. | Israël | Scraper APIs / Datasets / Retail Insights | Per-record / datasets / abonnement | Web Scraper PAYG **1,5 $**/1 000 records · Scale **499 $/mois** (384 000 records inclus + 1,3 $/1 000) · essai 1 000 records/1 sem. | ✅ confirmé (**↑** vs annexe : 0,75 → 1,5 $) |
| **Oxylabs** | - | Web Scraper API | Free trial + résultats réussis | Essai jusqu'à 2 000 résultats sans CB ; paiement sur résultats réussis | annexe |
| **Decodo** | Lituanie | Web/eCommerce Scraping API | Complexité / crédits / req / trafic | Web Scraping dès **0,09 $**/1 000 req · Site Unblocker dès 0,95 $/1 000 · résidentiel dès 2 $/GB | annexe |
| **Piloterr** | - | Marketplace / scraping endpoints | Crédits + coût par endpoint | 50 crédits gratuits ; certains endpoints à 1 crédit | annexe |
| **Barcode Lookup** | - | Barcode Lookup API | Abonnement mensuel + quotas | Abonnement récurrent ; plafond 100 appels/min | annexe |
| **Go-UPC** | - | GTIN enrichment | Lookups mensuels | Plans en lookups/mois ; 2 req/s | annexe (adjacent) |
| **Rainforest** / Traject Data | États-Unis (Colorado) | Amazon Product/Search API | Per-request | Preuve tarifaire publique non récupérée | annexe (adjacent) |

**Deux constats immédiats :**
- **PriceAPI** est l'abstraction la plus proche de la cible : GTIN/identifiant/terme en entrée, données
  normalisées multi-sources, crédits, gestion de fraîcheur, extensions `deals`/`best-sellers`/`reviews`.
- La **France stricte** est peu présente en self-serve documenté ; l'offre francophone la plus lisible
  (Winamaz) est adossée à l'affiliation, pas à une architecture API produit " clean-room ".

---

## 4. Fiches détaillées par acteur

### 4.1 PriceAPI (metoda GmbH) - *benchmark structurel n°1*

**Features.** Topics `product`, `offers`, `product_and_offers`, `search_results`, plus `best sellers`,
`deals/promotions`, `ratings/reviews` (v2). Sources : Amazon, eBay, Google Shopping, Idealo... sur ~35 pays.
Workflow **job-based** (`job_id` → `status` → `download`).

**Pricing par palier** (mensuel, overage dégressif) :

| Plan | Prix | Crédits inclus | € / crédit inclus | Overage / crédit |
|---|---|---|---|---|
| Go | 99 € | 5 000 | 0,0198 € | 0,02 € |
| Basic | 259 € | 20 000 | 0,0130 € | 0,015 € |
| Starter | 499 € | 50 000 | 0,0100 € | 0,01 € |
| Advanced | 999 € | 200 000 | 0,0050 € | 0,005 € |
| Pro | 1 499 € | 500 000 | 0,0030 € | 0,003 € |

> 1 crédit = 1 requête pour 1 produit depuis 1 source, une fois. Essai 1 000 crédits sans CB.

**Points forts** : modèle " facette/topic " mûr ; couverture multi-marchands ; fraîcheur paramétrable
(jusqu'à ~4×/h) ; dégressivité claire ; provenance métier (`source`, `country`, `topic`).
**Points faibles** : **facture les " not found "** recherchés (vs no-data-no-pay) ; entrée à 99 €/mois
relativement chère pour tester ; workflow job-based moins immédiat qu'un GET synchrone ; pas de donnée
écologique/énergie/réparabilité.

### 4.2 SerpApi - *benchmark mondial price-monitoring " search-led "*

**Features.** Google Shopping API, Amazon Product/Search API, autres sellers ; HTML + JSON ; cache 1 h
(Amazon), `no_cache` possible ; SLA 99,95 % ; playground développeur très soigné.

**Pricing** : Free 250 · Starter 25 $/1 000 · Developer 75 $/5 000 (15 $/1K) · Production 150 $/15 000
(10 $/1K) · Big Data 275 $/30 000 (9,17 $/1K). Facturation **par recherche réussie**, mais
**une réponse vide compte 1 recherche** ; le volume de résultats n'affecte pas le coût.

**Points forts** : DX excellente, playground, SLA affiché, US Legal Shield, async. **Points faibles** :
orienté **SERP/marketplace** (pas GTIN-first) ; un résultat vide est facturé ; donnée agrégée, moins
profonde sur stock/seller ; aucune donnée propriétaire.

### 4.3 Winamaz (BFIRST OÜ) - *offre affiliation francophone*

**Features.** Comparaison de prix multi-marchands, multilingue, stocks, coupons, actualisation
automatique, best sellers, promotions. Dépendance explicite à Amazon, Kelkoo et plateformes
d'affiliation validées. Auth Basic/Bearer, JSON, HTTPS.

**Pricing** : Novice 30 €/20 000 cr. · Explorer 59 €/60 000 · Pro 118 €/180 000 · Advanced 236 €/540 000 ·
Expert 472 €/1 620 000. **1 crédit consommé quand l'appel renvoie un résultat contenant un EAN.**

**Points forts** : entrée la moins chère du panel (30 €) ; coût par crédit très bas (~0,0015 €) ;
no-result ≈ non facturé (puisque conditionné à la présence d'un EAN) ; pertinent pour prix/promos/stock
multi-région. **Points faibles** : couverture limitée au réseau d'affiliation ; contrat développeur
peu ouvert (méthodes transmises après commande) ; pas de SLA ; logique affiliation ≠ API produit neutre.

### 4.4 Bright Data - *profondeur de supply maximale*

**Features.** Scraper APIs, datasets, data firehose, Retail Insights ; 250+ sites ; 5 Md+ records
rafraîchis ; webhooks/snapshots ; beaucoup de scrapers via `POST /datasets/v3/scrape`.

**Pricing (Web Scraper API, 2026-06-02)** : PAYG **1,5 $/1 000 records** (↑ vs 0,75 $ annexe) ;
Scale **499 $/mois** = 384 000 records inclus + 1,3 $/1 000 supplémentaires ; essai 1 000 records/1 sem. ;
**" pay only for success "**.

**Points forts** : échelle, profondeur, datasets historiques, no-data-no-pay de fait. **Points faibles** :
contractuellement complexe ; coût fixe élevé sur les gros plans ; orienté infrastructure/record, pas
API produit GTIN-first lisible ; aucune donnée propriétaire.

### 4.5 Oxylabs / Decodo / Piloterr - *boîtes à outils scraping e-commerce*

- **Oxylabs** : cibles préconfigurées Amazon Pricing/Product/Search (23/129/88 datapoints) ; JSON parsé
  ou HTML ; " pay only for successful results ". Bon repère pour **scinder proprement** price / product /
  search.
- **Decodo** : Web Scraping API unifiée dès 0,09 $/1 000 req, AI parser, templates ; concurrent surtout
  sur le **coût marginal de collecte**, pas sur la normalisation métier.
- **Piloterr** : endpoints marketplace riches (ex. Cdiscount : seller name/type/url, prix, remise, stock,
  livraison, images, rating, reviews, Q&A) ; 50 crédits gratuits. Bon **benchmark marketplace-first**
  pour shipping/stock/seller/rating.

**Points faibles communs** : pas de SLA métier produit ; provenance surtout **technique** (cible/moteur),
pas métier ; aucune donnée écologique/énergie.

### 4.6 Barcode Lookup & Go-UPC - *bases d'enrichissement GTIN (adjacents)*

- **Barcode Lookup** : lookup par barcode/MPN/ASIN/title/brand/category ; `stores` avec `price`,
  `sale_price`, `availability`, `condition`, `shipping`, `last_update`, `reviews` ; taxonomie Google
  Shopping ; `GET /v3/products?...&key=` ; 100 appels/min. Excellent **patron de DTO enrichi**
  (product + stores + shipping + reviews).
- **Go-UPC** : `GET /api/v1/code/:code`, 2 req/s, plans en lookups/mois. Repère pour les futures facettes
  **identité / catégorie / specs**, pas un concurrent prix direct.

---

## 5. Pricing par feature - synthèse transversale

Le marché **ne facture pas par attribut** mais par **profondeur de collecte / complexité d'accès** :

| Acteur | Unité de facturation | Coût d'une " facette " prix simple | No-data-no-pay ? |
|---|---|---|---|
| PriceAPI | crédit (1 produit × 1 source × 1 fois) | 0,003-0,02 €/crédit selon palier | ❌ (not found facturé) |
| SerpApi | recherche réussie | 0,009-0,025 $/recherche | ⚠️ partiel (vide = facturé) |
| Winamaz | résultat contenant un EAN | ~0,0015 €/crédit | ✅ de fait |
| Bright Data | record livré | 0,0013-0,0015 $/record | ✅ |
| Decodo | req / complexité | dès 0,00009 $/req (brut) | selon succès |
| Oxylabs | résultat réussi | variable | ✅ |

**Implication pour Product Data API** : conserver un **prix de base par facette** (pas par champ),
éventuellement modulé par options/fraîcheur, et faire du **" no-data-no-pay " par facette** la règle
transverse. C'est le seul positionnement à la fois plus simple **et** plus généreux que l'ensemble du
panel. La baseline `product.price` à **5 crédits @ 0,002 €** (= 0,01 €/appel) place nudger **au niveau
du tarif inclus PriceAPI Starter** tout en étant plus loyal sur la facturation.

---

## 6. Positionnement nudger vs concurrence

```
            Donnée commodité (prix scrapé, fiche)        Donnée propriétaire (impact, review IA, énergie)
            ───────────────────────────────────────      ─────────────────────────────────────────────────
PriceAPI    ████████████████  (cœur de métier)            ·  (aucune)
SerpApi     ████████████████  (cœur de métier)            ·
Winamaz     ███████████       (via affiliation)           ·
BrightData  ████████████████  (cœur de métier)            ·
Barcode L.  ████████          (enrichissement)            ·
nudger      ██████████        (33,9 M GTIN avec offres)   ████████████████  EXCLUSIF (45-50 K produits)
```

- **Sur la commodité prix**, nudger n'a pas à gagner la guerre du volume : 33,9 M GTIN avec offres et
  3,9 M multi-marchands suffisent pour une offre crédible (voir [`data-coverage.md`](data-coverage.md) §2).
  L'avantage se joue sur le **contrat** (no-data-no-pay strict, provenance + fraîcheur explicites) et la
  **DX** (GET synchrone GTIN-first vs jobs).
- **Sur la différenciation**, nudger est **seul** sur ImpactScore, réparabilité, étiquette énergie EPREL,
  revue IA sourcée multi-niveaux et référentiels multi-taxonomies. Ce sont des facettes **premium à forte
  marge** qu'aucun acteur du panel ne peut répliquer sans reconstruire le pipeline d'agrégation nudger.
- **Risque** : ces facettes n'existent que sur un **sous-ensemble curé (~10-50 K produits)**. Le discours
  commercial doit donc être *" prix partout, intelligence là où elle compte "*, pas *" tout sur tout "*.

**Recommandations de positionnement :**
1. Vendre `product.price` comme **produit d'appel loyal** (no-data-no-pay), aligné prix marché (~0,01 €/appel).
2. Faire des facettes propriétaires (`impact`, `review`, `energy`) le **vrai pitch** et la marge.
3. Communiquer la **provenance + fraîcheur** comme argument de confiance que les scrapers masquent.
4. Rester **GTIN-first synchrone** : différenciateur DX face au job-based de PriceAPI/Bright Data.

---

## 7. Limites et questions ouvertes

- Pricing Oxylabs/Decodo/Piloterr/Barcode Lookup/Go-UPC **non re-vérifié** le 2026-06-02 (repris de
  l'annexe de cadrage) - à confirmer avant tout chiffrage commercial définitif.
- Rainforest/Traject Data : preuve tarifaire publique non récupérée.
- Bright Data a **doublé** son tarif Web Scraper (0,75 → 1,5 $/1 000) depuis l'annexe : signe d'une
  tension à la hausse sur le coût de collecte → renforce l'intérêt d'une donnée propriétaire déjà acquise.
- À trancher : faut-il exposer un comparateur public " notre €/appel vs PriceAPI/SerpApi " sur la landing
  pricing ? (fort argument no-data-no-pay, mais expose à une guerre des prix sur la commodité).

---

## Sources

- [PriceAPI - Plans](https://www.priceapi.com/en/price/plans/) · [Costs & subscription](https://readme.priceapi.com/docs/costs-subscription-plans-and-contracts)
- [SerpApi - Pricing](https://serpapi.com/pricing)
- [Winamaz - Prices](https://winamaz.com/en/prices/) · [Winamaz API](https://winamaz.com/en/winamaz-api/)
- [Bright Data - Web Scraper pricing](https://brightdata.com/pricing/web-scraper)
- Données complémentaires (Oxylabs, Decodo, Piloterr, Barcode Lookup, Go-UPC, Rainforest) : annexe de cadrage fournie, non re-vérifiée le 2026-06-02.
