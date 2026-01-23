# Impact Score - Normalisation statistique (référence)

> Version de travail - 2026-01-23

## 1) Notation

- \(x\) : valeur brute (mesure ou valeur mappée)
- \(\mu\) : moyenne des valeurs dans la population de référence
- \(\sigma\) : écart-type
- \(N\) : nombre d’observations
- \(S_{min}\) : score minimum (par défaut 0)
- \(S_{max}\) : score maximum (par défaut 5)
- `impactBetterIs` : sens « meilleur » pour le score impact

On suppose que la normalisation produit un score \(s\in[S_{min},S_{max}]\).

## 2) Sigma scoring (standard deviation)

### 2.1 Formule

On définit un intervalle « normal » :
\[
[\mu - k\sigma, \mu + k\sigma]
\]
avec \(k\) configurable (souvent 2).

Projection linéaire et clamp :
\[
n = \frac{x - (\mu - k\sigma)}{(\mu + k\sigma) - (\mu - k\sigma)}
\]
\[
s = clamp(n \times S_{max}, 0, S_{max})
\]

### 2.2 Propriétés

- \(x=\mu\Rightarrow s=S_{max}/2\) (ex : 2.5/5)
- valeurs extrêmes au-delà de \(\pm k\sigma\) saturent à 0 ou \(S_{max}\)

### 2.3 Cas dégénérés

- Si \(\sigma = 0\) :
  - stratégie recommandée : renvoyer \(S_{max}/2\) (neutre) OU appliquer une policy YAML (NEUTRAL/ERROR/FALLBACK).
- Si bornes quasi égales (sécurité flottante) : renvoyer score neutre.

## 3) Percentile scoring (mid-rank)

### 3.1 Formule (mid-rank)

Soit :
- \(c_{<}(x)\) : nombre de valeurs strictement inférieures à \(x\)
- \(c_{=}(x)\) : nombre de valeurs égales à \(x\)

\[
p = \frac{c_{<}(x) + 0.5\,c_{=}(x)}{N}
\]
\[
s = clamp(p \times S_{max}, 0, S_{max})
\]

### 3.2 Usage

- recommandé si la distribution est très discrète (peu de valeurs distinctes) **ou** si l’on veut un score strictement ordinal.
- attention : « moyenne » n’est pas un pivot fixe comme en sigma.

### 3.3 Cas limites

- N=0 : renvoyer neutre (policy)
- fréquences absentes : renvoyer neutre (policy)

## 4) Min–max borné (MINMAX_FIXED)

Si l’attribut a des bornes stables \([a,b]\) :
\[
n = \frac{x-a}{b-a}
\quad ;\quad
s = clamp(n\times S_{max}, 0, S_{max})
\]

Usage : notes normatives (0–10), unités bornées “standard”.

## 5) Min–max par quantiles (MINMAX_QUANTILE)

Pour réduire l’influence des outliers tout en restant relatif :
- remplacer \(a\) et \(b\) par des quantiles \(q_{low}\) et \(q_{high}\) (ex : p5/p95)
- même projection linéaire + clamp

Nécessite une estimation de quantiles (stockage valeurs ou estimateur streaming).

## 6) Barème fixe (FIXED_MAPPING)

Ex : classe énergétique {A,B,C,D,E,F,G}
- mapping direct vers un score 0..5 selon table.
- policy si valeur non mappée : ERROR ou NEUTRAL ou WORST.

## 7) Inversion (impactBetterIs)

Après calcul du score \(s\) :
- si `impactBetterIs = LOWER` :
\[
s' = S_{max} + S_{min} - s
\]
- sinon : \(s'=s\)

## 8) Échelle finale stable 0–20 (poids)

Si sous-scores sont sur 0..5 :
- normaliser les poids pour \(\sum w_i = 4\).
- Impact Score 0..20 :
\[
IS = \sum_i (s_i \times w'_i)
\]
avec \(w'_i = w_i \times 4/\sum w\).

## 9) Métadonnées UI recommandées

Pour rendre l’explication et la dataviz correctes :
- `normalizationMethod`
- `normalizationParams` (k, quantiles, bornes, mapping)
- `stats` (avg, stdDev, median, quantiles si dispo)
- `impactBetterIs`, `userBetterIs`
