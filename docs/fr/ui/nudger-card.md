---
title: "NudgerCard : cartes à coins modulaires"
description: "Guide d’utilisation du composant NudgerCard pour mutualiser les styles de cartes à coins arrondis."
tags: ["frontend", "ui", "cartes"]
icon: "mdi-card-bulleted-outline"
weight: 20
updatedAt: "2026-02-10"
draft: false
---

# NudgerCard : guide d’usage

Le composant `NudgerCard` centralise les styles de cartes “nudger” afin de garder
un **rendu homogène** et de **limiter les classes ad-hoc** dans les sections
frontend.

## Pourquoi l’utiliser ?

- **Cohérence visuelle** : un seul point de vérité pour les rayons et bordures.
- **Maintenance simplifiée** : les ajustements se font dans un seul composant.
- **Variantes explicites** : les coins modifiés sont déclarés par props.
- **Design system** : facilite la revue UX et les ajustements globaux.

## Quand l’utiliser ?

Utilisez `NudgerCard` pour les cartes de contenu qui reprennent le style “nudger”
dans les sections marketing (home, landing pages, etc.).  
Évitez de le dupliquer avec des classes globales : préférez les props dédiées.

## API principale

| Prop | Type | Rôle |
| --- | --- | --- |
| `border` | `boolean` | Ajoute la bordure primaire. |
| `accentCorners` | `Array<'top-left'|'top-right'|'bottom-right'|'bottom-left'>` | Applique un rayon “accent” à un ou plusieurs coins. |
| `flatCorners` | `Array<'top-left'|'top-right'|'bottom-right'|'bottom-left'>` | Force un coin à `0` pour obtenir un angle droit. |
| `baseRadius` | `string` | Rayon par défaut pour tous les coins. |
| `accentRadius` | `string` | Rayon appliqué aux coins “accent”. |
| `padding` | `string` | Padding interne de la carte. |
| `background` | `string` | Couleur ou gradient de fond. |
| `elevation` | `number` | Élévation Vuetify optionnelle. |

## Exemples

### Coin haut gauche “carré” + coin bas gauche accentué

```vue
<NudgerCard
  border
  :flat-corners="['top-left']"
  :accent-corners="['bottom-left']"
>
  <p>Contenu</p>
</NudgerCard>
```

### Carte “plein écran” personnalisée

```vue
<NudgerCard
  background="rgba(var(--v-theme-surface-default), 0.96)"
  padding="clamp(2rem, 5vw, 3rem)"
  base-radius="clamp(1.75rem, 4vw, 2.5rem)"
>
  <p>CTA</p>
</NudgerCard>
```

## Bonnes pratiques

- **Préférez les props** plutôt que d’ajouter de nouvelles classes globales.
- **Ciblez uniquement les coins nécessaires** (ex. un seul coin accentué).
- **Centralisez la logique** : si un nouveau style devient récurrent, ajoutez une
  prop ou une variante dans `NudgerCard` plutôt que de dupliquer.
