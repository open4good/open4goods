# Frontend - Alignement explications & graphiques (Impact Subscores)

> Version de travail - 2026-01-23

## 1) Constat (code actuel)

### 1.1 ProductImpactSubscoreExplanation.vue

Le composant suppose explicitement un scoring sigma : la moyenne est présentée comme un pivot fixe à 10/20.

Extrait :

```ts
const averageOn20Value = computed(() => {
  // Sigma scoring definition: Average is always the pivot at 10/20 (2.5/5)
  return formatNumber(10, { maximumFractionDigits: 0 })
})
```

### 1.2 ProductImpactSubscoreChart.vue

La « moyenne » et la valeur du produit sont rendues comme des barres de surlignage dans le bucket.

## 2) Changements demandés

### 2.1 Adapter les explications selon normalizationMethod

Objectif : varier les textes + les valeurs mises en avant.

- SIGMA
  - expliquer µ/σ et les bornes ±kσ
  - le pivot µ correspond à 2.5/5 => 10/20 (si l’échelle du sous-score est 0..5 et l’Impact Score final 0..20)
- PERCENTILE
  - expliquer rang : « votre produit est dans les X% meilleurs / p = … »
  - ne pas annoncer un pivot fixe
- FIXED_MAPPING / MINMAX_FIXED
  - expliquer barème fixe (normatif)
  - expliciter la conversion (ex : A→5/5)

### 2.2 Modifier la représentation de la “moyenne” dans le graphe

Recommandation ECharts :

- utiliser `markLine` pour afficher la moyenne (ligne verticale) plutôt qu’une barre pleine.
- conserver un surlignage discret du bucket produit (ou passer en `scatter`).

Optionnel (si métadonnées dispo) :

- SIGMA : afficher bande ±kσ (markArea) ou deux lignes (µ−kσ, µ+kσ)
- PERCENTILE : afficher médiane (p50) et quartiles (p25/p75)

### 2.3 Ajouter un toggle “valeurs absolues” / “scores normalisés”

Objectif : permettre au même graphe d’explorer :

- la distribution en **valeurs absolues** (unités, données brutes)
- la distribution en **scores normalisés** (0..5) pour comparer les critères

Le toggle doit être visible dès que la normalisation est connue (méthode + stats suffisantes).

## 3) Données nécessaires côté API

Pour permettre ces variations, le backend doit exposer (par sous-score) :

- normalizationMethod
- params (k, mapping, bornes, quantiles)
- stats : avg, stdDev, éventuellement median/quantiles
- userBetterIs + impactBetterIs (pour contextualiser)

## 4) Checklist UI

- Tooltips corrects selon method
- Texte "moyenne = 10/20" uniquement en SIGMA (ou legacy sigma)
- Si stats manququantes : fallback texte simple (« comparaison relative »)
