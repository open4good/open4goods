# Impact Score - Méthodes de scoring disponibles

Ce document décrit les méthodes de normalisation disponibles dans le système et leurs usages recommandés.
Il complète :

- `IMPACT_SCORE_CONFIGURATION_YAML.md` pour la structure des champs YAML.
- `IMPACT_SCORE_STATISTIQUE.md` pour les formules et la théorie statistique.

## 1) Vue d'ensemble

Un attribut scorable (`asScore: true`) est normalisé sur une échelle `scale.min..scale.max` (par défaut 0..5).
Le sens d'évaluation impact est ensuite appliqué via `impactBetterIs` (inversion si `LOWER`).

La méthode est définie dans `scoring.normalization.method` :

- `SIGMA`
- `PERCENTILE`
- `MINMAX_FIXED`
- `MINMAX_OBSERVED`
- `MINMAX_QUANTILE`
- `FIXED_MAPPING`
- `BINARY`
- `CONSTANT`

## 2) Méthodes et paramètres

### 2.1 SIGMA (écart-type)

**Principe**
- Normalisation linéaire dans l'intervalle `[μ − kσ, μ + kσ]`.
- Les valeurs hors bornes sont clampées aux extrêmes de l'échelle.

**Paramètres**
- `sigmaK` (par défaut 2.0)

**À utiliser quand**
- La distribution est continue et suffisamment stable.
- On veut comparer des produits sur une base relative, centrée sur la moyenne.

**À éviter quand**
- Très peu de valeurs distinctes.
- Outliers forts qui écrasent la variance utile.

---

### 2.2 PERCENTILE (rang relatif)

**Principe**
- Score basé sur le rang (mid-rank) dans la distribution observée.

**Paramètres**
- Aucun paramètre obligatoire.

**À utiliser quand**
- Les valeurs sont discrètes ou très répétées.
- On veut une comparaison purement ordinale.

**À éviter quand**
- Peu d'observations disponibles.

---

### 2.3 MINMAX_FIXED (bornes fixes)

**Principe**
- Projection linéaire entre deux bornes connues `[fixedMin, fixedMax]`.

**Paramètres**
- `fixedMin`
- `fixedMax`

**À utiliser quand**
- L'attribut a une échelle normative stable (ex : note 0–10, valeur 0–100).
- On veut un score absolu, stable dans le temps.

**À éviter quand**
- Les bornes ne sont pas connues ou trop variables.

---

### 2.4 MINMAX_OBSERVED (bornes observées)

**Principe**
- Projection linéaire entre les bornes observées dans le batch `[min, max]`.
- Les valeurs hors bornes sont clampées aux extrêmes de l'échelle.

**Paramètres**
- Aucun paramètre obligatoire.

**À utiliser quand**
- On veut une relativisation « marché » stricte (meilleur produit = score max, pire = score min).
- Les bornes fixes sont inconnues, mais la couverture est suffisante.

**À éviter quand**
- Trop peu de valeurs ou distribution instable (risque d'effet “yoyo” dans le temps).
- Présence d’outliers forts (préférer `MINMAX_QUANTILE`).

---

### 2.5 MINMAX_QUANTILE (bornes par quantiles)

**Principe**
- Projection linéaire entre deux quantiles (ex : p5/p95) pour limiter l'effet des outliers.

**Paramètres**
- `quantileLow`
- `quantileHigh`

**À utiliser quand**
- La distribution est étalée ou contient des outliers.
- On veut stabiliser le scoring sans figer des bornes absolues.

---

### 2.6 FIXED_MAPPING (barème fixe)

**Principe**
- Mapping direct d'une valeur discrète vers un score.

**Paramètres**
- `mapping` (table valeur → score)

**À utiliser quand**
- L'attribut est catégoriel (ex : classes énergétiques, labels).

---

### 2.7 BINARY (seuil)

**Principe**
- Score min ou max selon un seuil.

**Paramètres**
- `threshold`
- `greaterIsPass` (par défaut `false` si absent)

**À utiliser quand**
- L'attribut est une conformité oui/non.

---

### 2.8 CONSTANT (score constant)

**Principe**
- Score constant (ou neutre par défaut).

**Paramètres**
- `constantValue` (optionnel)

**À utiliser quand**
- On veut neutraliser un attribut ou figer temporairement un score.

## 3) Application du sens d'impact

Après normalisation, l'inversion est appliquée si `impactBetterIs: LOWER` :

```
score_final = scale.max + scale.min - score_normalisé
```

Cela permet d'utiliser la même normalisation (ex: MINMAX_FIXED) tout en indiquant que « plus bas = meilleur ».

## 4) Recommandations par attribut scorable (audit rapide)

> Objectif : vérifier la pertinence de la méthode actuelle et proposer des alternatives.

### Durabilité / support / disponibilité

- `MINAVAILABILITYSOFTWAREUPDATESYEARS` (SIGMA) : proposer `MINMAX_FIXED` avec bornes marché (ex: 0–10 ans) si ces bornes sont connues, ou `MINMAX_OBSERVED` si l’on veut un score relatif au marché actuel.
- `MINAVAILABILITYSPAREPARTSYEARS` (SIGMA) : même recommandation.
- `MINGUARANTEEDSUPPORTYEARS` (SIGMA) : même recommandation.
- `WARRANTY` (SIGMA) : même recommandation.

**Pourquoi** : les durées sont souvent discrètes et bornées. Une échelle fixe donne un score stable et lisible.

### Consommations électriques (W)

- `POWER_CONSUMPTION_TYPICAL` (SIGMA) : considérer `MINMAX_QUANTILE` (p5/p95) ou `MINMAX_FIXED` si bornes stables.
- `POWER_CONSUMPTION_SDR` (SIGMA) : idem.
- `POWER_CONSUMPTION_HDR` (SIGMA) : idem.
- `POWER_CONSUMPTION_STANDBY` (SIGMA) : idem.
- `POWER_CONSUMPTION_STANDBY_NETWORKD` (SIGMA) : idem.
- `POWER_CONSUMPTION_OFF` (SIGMA) : idem.

**Pourquoi** : distributions souvent très asymétriques / coverage faible → SIGMA peu robuste.

### Classe énergétique (catégoriel)

- `CLASSE_ENERGY` (MINMAX_FIXED + mapping numérique) : préférable en `FIXED_MAPPING` pour éviter une distance linéaire implicite entre classes.
- `CLASSE_ENERGY_SDR` (MINMAX_FIXED + mapping numérique) : idem.
- `CLASSE_ENERGY_HDR` (MINMAX_FIXED + mapping numérique) : idem.

**Pourquoi** : les classes énergétiques sont des paliers, pas une mesure continue.

### Indice de réparabilité (0–10)

- `REPAIRABILITY_INDEX` (MINMAX_FIXED 0–10) : méthode pertinente si la note officielle est 0–10. Alternative : `PERCENTILE` si on veut une relativisation purement marché.

### Taille / poids

- `DIAGONALE_POUCES` (SIGMA) : alternative `MINMAX_QUANTILE` pour limiter l'effet des extrêmes.
- `WEIGHT` (SIGMA) : alternative `MINMAX_QUANTILE` ou `MINMAX_FIXED` si bornes connues par segment.

### ESG / Sustainalytics

- `ESG` (SIGMA) : recommandé `MINMAX_FIXED` avec `fixedMin: 0`, `fixedMax: 100`, et `impactBetterIs: LOWER`.
- `BRAND_SUSTAINALYTICS_SCORING` (SIGMA) : recommandé `MINMAX_FIXED` avec `fixedMin: 0`, `fixedMax: 100`, et `impactBetterIs: LOWER`.

**Pourquoi** : scores ESG et Sustainalytics sont sur 0–100 avec sens inversé (plus bas = meilleur). Un MINMAX_FIXED donne un score stable, tout en permettant l’inversion via `impactBetterIs`.

### Qualité de la donnée

- `DATA_QUALITY` (SIGMA) : recommandé `MINMAX_FIXED` sur une borne marché (ex: 0–100), avec `impactBetterIs: GREATER` si plus de complétude = meilleur.

**Pourquoi** : la qualité de donnée est souvent un taux ou une note bornée, donc une échelle fixe est plus claire.

## 5) Note sur la relativisation (Score.relativ)

Le champ `Score.relativ` est calculé dans le batch à partir des méthodes ci-dessus. Il utilise :

- la méthode explicite si `scoring.normalization.method` est définie,
- sinon un comportement legacy SIGMA (ou PERCENTILE si la distribution est trop discrète).

Autrement dit : **la relativisation est déjà “calculée” via ces méthodes**.
Pour obtenir un score « relatif au marché », privilégier `MINMAX_OBSERVED` (bornes observées) ou `MINMAX_QUANTILE` si la distribution contient des outliers.
