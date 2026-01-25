# Impact Score (EcoScore) - Méthodologie complète

> Version de travail - 2026-01-23

## 1. Objet

L’**Impact Score** (souvent nommé `ECOSCORE` côté backend) est un score composite visant à comparer des produits sur un axe « impact » (environnemental / durabilité, selon la définition produit).  
Il est calculé par **agrégation pondérée** de plusieurs **sous-scores** (critères) dérivés d’attributs produit (consommation, poids, réparabilité, garantie, etc.).

**Convention produit** : un score plus élevé indique un **meilleur impact** (donc un impact environnemental plus faible). Cette convention est obtenue en appliquant `impactBetterIs` lors de la normalisation.

## 2. Concepts

### 2.1 Sous-score (score critère)

Un sous-score est construit à partir d’une valeur brute (numérique ou mappée) associée à un attribut configuré `asScore: true`.

Chaque sous-score est normalisé sur une échelle commune (par défaut **0 à 5**) afin de pouvoir être combiné avec d’autres critères hétérogènes.

### 2.2 Deux “sens” à distinguer : userBetterIs vs impactBetterIs

Certains attributs sont « meilleurs » pour l’utilisateur dans un sens, mais « meilleurs » pour l’impact dans le sens inverse.

- **userBetterIs** : direction qui décrit ce qui est généralement préférable pour l’utilisateur (confort/performance).
  - ex : diagonale d’écran TV → **plus grand = mieux** pour l’utilisateur.
- **impactBetterIs** : direction qui décrit ce qui est préférable pour l’impact (selon la définition du score).
  - ex : diagonale d’écran TV → **plus petit = mieux** pour l’impact (à hypothèse égale).

Implication :
- L’inversion qui sert à calculer les sous-scores « impact » doit dépendre de **impactBetterIs**.
- L’UI (explication) peut s’appuyer sur **les deux** pour contextualiser (« confortable mais plus impactant »).

Compatibilité :
- les champs `userBetterIs` et `impactBetterIs` sont requis pour expliciter le sens des comparaisons.

## 3. Pipeline de calcul (batch)

### 3.1 Extraction des sous-scores (valeurs brutes)

Le service `Attribute2ScoreAggregationService` convertit des attributs produit en valeurs numériques :

- Si l’attribut est NUMERIC : parsing en `Double`
- Sinon : utilisation d’un `numericMapping` (YAML)

Ces valeurs brutes sont stockées dans `product.scores[attributeKey].value`.

### 3.2 Accumulation statistique (distribution)

Pendant le traitement batch, le système maintient pour chaque sous-score :
- N (count), somme, min/max, moyenne (µ), écart-type (σ)
- et éventuellement des fréquences (pour percentile) et/ou quantiles.

### 3.3 Normalisation (relativisation) en 0..5

Chaque sous-score est normalisé via une méthode **déclarée dans le YAML**, puis éventuellement inversé selon `impactBetterIs`.  
Voir la documentation statistique pour les formules.

### 3.4 Agrégation : calcul de l’Impact Score global

L’Impact Score est une **somme pondérée** :
\[
ImpactScore = \sum_i (SubScore_i \times Weight_i)
\]

**Échelle stable (cible 0–20)**  
Comme `SubScore_i` est sur 0..5, pour garantir une échelle 0..20, on veut :
\[
\sum_i Weight_i = 4
\]
On peut l’obtenir en normalisant automatiquement les poids :
\[
Weight'_i = Weight_i \times \frac{4}{\sum_j Weight_j}
\]
Puis :
\[
ImpactScore_{0..20} = \sum_i (SubScore_i \times Weight'_i)
\]

### 3.5 Valeurs manquantes

Une politique explicite doit exister :
- **NEUTRAL** (ex : 2.5/5)
- **WORST** (0/5)

Décision actuelle :
- Utiliser **NEUTRAL** partout pour ne pas sur- ou sous-pondérer un produit sur une donnée absente.
- Reporter la pénalisation globale via le critère `DATA_QUALITY`.

## 4. Points de cohérence à vérifier (backend)

1) Ordonnancement des services batch : l’EcoScore doit être calculé après disponibilité des sous-scores normalisés (ou disposer d’un fallback maîtrisé).
2) Application du sens (inversion) : l’inversion doit être fondée sur `impactBetterIs` (pas `userBetterIs`).
3) Échelle : l’Impact Score doit respecter la cible 0..20 pour toutes les verticales (poids normalisés).
4) Politique de manquants : le comportement “score manquant => 0 contribution” doit être aligné sur la stratégie retenue.
5) Métadonnées UI : l’UI ne doit plus supposer une méthode sigma universelle.

## 5. Questions ouvertes à verrouiller

1) Sens produit : « plus haut = meilleur (moins d’impact) » ou « plus haut = plus d’impact » ?
2) Population statistique : verticale, catégorie, autre ?
3) Chart : distribution affichée en valeurs absolues (unités) ou valeurs normalisées ?
4) Sustainalytics : quelle métrique exacte (Score vs Risk Rating) ?
