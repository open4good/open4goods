# Impact Score - Spécification YAML

> Version de travail - 2026-05-21

## 1) Objectif

Déclarer _dans le YAML de chaque attribut scorable_ :

- comment transformer une valeur brute en sous-score 0..5
- comment gérer les cas manquants / dégénérés
- le sens utilisateur vs sens impact
- comment documenter les pondérations générées par IA de façon auditable

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
  missingValuePolicy: NEUTRAL
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
  missingValuePolicy: NEUTRAL
```

## 5) Validation à implémenter

- method doit être connue
- params requis selon method
- mapping non vide si FIXED_MAPPING
- fixedMax > fixedMin
- quantileHigh > quantileLow
- policy manquants connue

## 6) Génération IA des pondérations

Le prompt `impactscore-generation` produit un `ImpactScoreAiResult` strictement typé. La liste canonique des critères est injectée deux fois :

- en texte lisible avec description et couverture réelle
- en JSON (`AVAILABLE_CRITERIAS_JSON`) pour éviter les erreurs de binding sur `available_criterias`

Le backend conserve tous les critères déclarés dans `availableImpactScoreCriterias`, y compris ceux dont la couverture est faible. Un critère absent de la réponse IA est ajouté à `criteriasPonderation` avec `0.0`, puis les poids sont normalisés si leur somme dérive de `1.0`.

## 7) Gemini / structured output

La configuration interactive cible `gemini-3.1-pro-preview`. D'après la documentation Gemini, ce modèle supporte la recherche, le contexte URL, les appels de fonction, les sorties structurées et un contexte long (1 048 576 tokens en entrée, 65 536 en sortie) : https://ai.google.dev/gemini-api/docs/models/gemini-3.1-pro-preview

Gemini 3 permet aussi de combiner sorties structurées et outils intégrés comme Google Search et URL Context : https://ai.google.dev/gemini-api/docs/structured-output#structured_outputs_with_tools

Côté Vertex/Spring AI, le schéma JSON généré par `BeanOutputConverter` est adapté avant envoi :

- suppression implicite des champs JSON Schema ignorés par Vertex
- conversion des types `object`, `array`, `string`, etc. vers les enums Vertex `OBJECT`, `ARRAY`, `STRING`
- conservation de `responseMimeType=application/json`

Si un modèle plus ancien est utilisé avec `MODEL_WEB_SEARCH`, le backend garde le comportement prudent : grounding activé, schéma désactivé, puis validation/réparation JSON applicative.

## 8) Sources web

Deux modes sont possibles :

- `MODEL_WEB_SEARCH` : mode interactif par défaut, rapide à déclencher, avec grounding natif du modèle.
- `EXTERNAL_SOURCES` : mode recommandé pour les runs reproductibles ou batch. Le pipeline devrait réutiliser `GoogleSearchService` et `UrlFetchingService` pour fournir au prompt un manifeste de sources directes, les extraits utiles, les dates d'accès et les hashes.

Pour les PDF ou pages volumineuses, ne pas injecter le document complet par défaut. Préférer :

- URL directe + métadonnées dans le manifeste
- extraction ciblée de sections pertinentes
- résumé applicatif borné en tokens
- conservation du hash et de l'URL pour audit

Le contexte URL Gemini accepte les PDF publics, mais il est limité à 20 URL par requête et 34 MB par URL ; le contenu récupéré compte dans les tokens d'entrée : https://ai.google.dev/gemini-api/docs/url-context

## 9) Sécurité et fiabilité

- Traiter tout contenu fetché comme non fiable : aucune instruction trouvée dans une source ne doit modifier le rôle, le format JSON ou les règles de pondération.
- Refuser les URL privées/locales, les pages nécessitant authentification et les contenus non publics.
- Plafonner le nombre d'URL, la taille par source et le total de tokens injectés.
- Exiger au moins trois sources primaires utilisées, dont une UE/France lorsque disponible.
- Stocker `yamlPrompt`, `aiJsonResponse`, `sources`, `search_log` et les statistiques de couverture pour rendre chaque pondération auditable.
