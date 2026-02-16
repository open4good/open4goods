# Plan complet — Dataviz statistiques verticales (front-api + frontend)

## 1. Objectif

Construire une plateforme de restitution statistique orientée **verticales** (issues de `VerticalsConfigService` et des YAML de configuration) permettant :

- des pages statistiques complètes par verticale ;
- des composants de dataviz fortement paramétrables réutilisables ailleurs (ex. article) ;
- des analyses marché, "le saviez-vous", et explorations avancées (drill-down) ;
- un socle d'agrégations Elasticsearch multi-niveaux exploitable par eCharts + D3 ciblé.

## 2. Réponses validées et contraintes

- **Scope principal** : verticales (via `VerticalsConfigService` + YAML).
- **Nombre de charts** : cible 20 à 30.
- **Cache HTTP** : 1h (`CacheControlConstants.ONE_HOUR_PUBLIC_CACHE`).
- **Filtre par défaut** : `lastChange >= now-2d` + `offersCount > 0`.
- **Override des filtres** : possible (par YAML + props composants).
- **Libs dataviz** : eCharts prioritaire + D3 ciblé (Chord, Beeswarm).
- **Export V1** : CSV uniquement.
- **I18n** : FR + EN systématique.
- **Contrôle d'accès UI** : via props `hasRole` (mapper à partir des rôles utilisateur).

## 3. Architecture cible

### 3.1 Front-API (moteur analytique)

Ajouter un sous-domaine "stats dataviz" avec :

- endpoint de **query générique** (filtres + agrégations + méta de rendu) ;
- endpoint de **presets** (issus des YAML de verticales) ;
- endpoint de **capabilities** (champs filtrables/agrégables/chartables) ;
- endpoint de **hero stats** (contenu localisé, piloté YAML).

Tous les endpoints supportent :

- `domainLanguage` ;
- cache 1h ;
- validation stricte des mappings de champ ;
- garde-fous (profondeur d'agrégation, max buckets, cardinalité).

### 3.2 Frontend (Nuxt + Vuetify)

Créer une page verticale dédiée (ex. `/[categorySlug]/stats`) et des composants réutilisables :

- `StatsHeroSection` ;
- `StatsFiltersBar` ;
- `StatsGallery` ;
- `StatsChartCard` ;
- `StatsKpiCard` ;
- `StatsEmbedWidget` (usage blog/landing ultérieur).

### 3.3 Config YAML verticale

Dans chaque verticale, ajouter une section `stats`.

Exemple de structure :

```yaml
stats:
  enabled: true
  hero:
    titleKey: stats.hero.title
    subtitleKey: stats.hero.subtitle
    eyebrowKey: stats.hero.eyebrow
  defaults:
    filters:
      - field: lastChange
        operator: range
        minRelative: now-2d
      - field: offersCount
        operator: range
        min: 1
  capabilities:
    allowFilterOverride: true
    maxAggregationDepth: 3
    maxBuckets: 100
  charts:
    - id: products-by-brand
      type: bar
      titleKey: stats.charts.productsByBrand.title
      descriptionKey: stats.charts.productsByBrand.description
      hasRole: ROLE_FRONTEND
      queryPreset: productsByBrand
      export:
        csv: true
```

## 4. Catalogue V1+ (24 charts proposés)

### A. KPI (4)

1. Nombre de produits actifs
2. Nombre total d'offres actives
3. Prix minimum médian
4. Répartition neuf / occasion

### B. Structure de marché (6)

5. Produits par marque (bar)
6. Parts de marque (treemap)
7. Produits par plateforme (bar)
8. Offres par plateforme (stacked bar)
9. Neuf/occasion par plateforme (100% stacked)
10. Produits par pays GTIN (bar)

### C. Temporel (5)

11. Nouveaux produits (date histogram)
12. Produits mis à jour (date histogram)
13. Évolution des offres actives
14. Évolution du prix médian
15. Volatilité des prix (écart-type par période)

### D. Prix & distribution (4)

16. Histogramme des prix min
17. Boxplot des prix par marque (Top N)
18. Distribution des prix par plateforme
19. Déciles de prix

### E. Qualité catalogue (3)

20. Produits exclus vs non exclus
21. Top causes d'exclusion (pareto)
22. Densité `offersCount` (histogram)

### F. Corrélation et analytique avancée (2)

23. Scatter prix vs score (ex. ECOSCORE)
24. Heatmap marque × plateforme

### G. D3 ciblé (V2)

- Chord diagram (relations marques ↔ plateformes)
- Beeswarm (dispersion fine des prix ou scores)

## 5. Drill-down interactif

Oui, c'est faisable de façon robuste.

Mécanique proposée :

- clic sur bucket -> ajout d'un filtre au store global ;
- relance synchronisée des charts dépendants ;
- breadcrumb des filtres actifs ;
- mode "verrouiller un chart" ;
- bouton reset global.

Garde-fous :

- limitation profondeur et cardinalité ;
- debounce des interactions ;
- annulation des requêtes obsolètes.

## 6. Endpoints front-api proposés

- `POST /stats/verticals/{verticalId}/charts/query`
- `GET /stats/verticals/{verticalId}/charts/presets`
- `GET /stats/verticals/{verticalId}/charts/capabilities`
- `GET /stats/verticals/{verticalId}/charts/hero`

Tous les endpoints :

- exigent `domainLanguage` ;
- injectent les filtres par défaut sauf override explicite ;
- renvoient des métadonnées de localisation ;
- exposent `Cache-Control: public, max-age=3600`.

## 7. Contrat d'embeddabilité (prévu)

Prévoir des props communes sur les composants :

- `verticalId`
- `presetId`
- `filters`
- `timeRange`
- `hasRole`
- `allowOverride`
- `exportCsv`

Ce contrat permet un montage en page stats, en bloc éditorial, ou en composant isolé.

## 8. I18n et contenu éditorial

- Créer les clés FR/EN pour hero + titres + descriptions + labels de filtres.
- Conserver un fallback propre : clé i18n -> valeur par défaut en anglais.
- Prévoir un sous-namespace i18n dédié (`stats.*`) pour éviter la dérive.

## 9. Plan de livraison

### Lot 1 (socle)

- Endpoints query/presets/capabilities/hero
- Filtres par défaut + override
- 8 charts prioritaires
- Export CSV

### Lot 2 (galerie complète)

- Passage à 20-24 charts
- Drill-down complet
- i18n FR/EN finalisée

### Lot 3 (analytique avancée)

- D3 Chord + Beeswarm
- Optimisations perf et UX
- instrumentation usage des charts

## 10. Questions restantes minimales

1. Pour `hasRole`, voulez-vous une logique **any-role** (au moins un rôle) ou **all-roles** (tous requis) ?
2. L'override des filtres par props doit-il pouvoir **désactiver entièrement** les defaults, ou seulement les compléter ?
3. Pour les charts D3 (Chord, Beeswarm), souhaitez-vous les activer uniquement sur desktop dans un premier temps ?
