# Impact Score - Spécification YAML (proposition)

> Version de travail - 2026-01-23

## 1) Objectif

Déclarer *dans le YAML de chaque attribut scorable* :
- comment transformer une valeur brute en sous-score 0..5
- comment gérer les cas manquants / dégénérés
- le sens utilisateur vs sens impact

## 2) Champs à ajouter (suggestion)

```yaml
asScore: true

# sens "utilisateur"
userBetterIs: GREATER | LOWER

# sens "impact"
impactBetterIs: GREATER | LOWER

scoring:
  scale:
    min: 0.0
    max: 5.0

  normalization:
    method: SIGMA | PERCENTILE | MINMAX_FIXED | MINMAX_OBSERVED | MINMAX_QUANTILE | FIXED_MAPPING | BINARY | CONSTANT
    params:
      # SIGMA
      sigmaK: 2.0

      # MINMAX_FIXED
      fixedMin: 0
      fixedMax: 10

      # MINMAX_QUANTILE
      quantileLow: 0.05
      quantileHigh: 0.95

      # FIXED_MAPPING
      mapping:
        A: 5
        B: 4
        C: 3
        D: 2
        E: 1
        F: 0.5
        G: 0

      # CONSTANT
      constantValue: 2.5

      # BINARY
      threshold: 1.0
      greaterIsPass: true

  transform: NONE | LOG | SQRT

  missingValuePolicy: NEUTRAL | WORST

  degenerateDistributionPolicy: NEUTRAL | ERROR | FALLBACK

```

## 3) Compatibilité

- Si `scoring.normalization.method` absent :
  - utiliser legacy behavior (sigma + fallback percentile) le temps de migrer, avec un log de warning.

## 4) Exemples

### 4.1 DIAGONALE_POUCES (exemple “sens opposés”)

```yaml
key: DIAGONALE_POUCES
asScore: true
userBetterIs: GREATER
impactBetterIs: LOWER
scoring:
  normalization:
    method: SIGMA
    params:
      sigmaK: 2.0
  missingValuePolicy: WORST
```

### 4.2 Classe énergétique (bornes fixes via mapping numérique)

```yaml
key: CLASSE_ENERGETIQUE
asScore: true
userBetterIs: GREATER
impactBetterIs: GREATER
scoring:
  normalization:
    method: MINMAX_FIXED
    params:
      fixedMin: 0
      fixedMax: 18
```

### 4.3 Réparabilité (0..10 borné)

```yaml
key: INDICE_REPARABILITE
asScore: true
userBetterIs: GREATER
impactBetterIs: GREATER
scoring:
  normalization:
    method: MINMAX_FIXED
    params:
      fixedMin: 0
      fixedMax: 10
```

### 4.4 Garantie (bornes observées)

```yaml
key: WARRANTY
asScore: true
userBetterIs: GREATER
impactBetterIs: GREATER
scoring:
  normalization:
    method: MINMAX_OBSERVED
  missingValuePolicy: WORST
```

## 5) Validation à implémenter

- method doit être connue
- params requis selon method
- mapping non vide si FIXED_MAPPING
- fixedMax > fixedMin
- quantileHigh > quantileLow
- policy manquants connue
