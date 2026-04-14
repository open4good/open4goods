---
title: "Intégrer un produit dans le markdown"
description: "Intègre des liens produits internes dans le markdown via GTIN ou marque/modèle."
type: "guide"
tags: ["language:fr", "frontend", "markdown", "product"]
weight: 40
updatedAt: "2026-04-12"
draft: false
published: true
navigation: true
---

# Intégrer un produit dans le markdown

Utilisez `ProductEmbed` directement dans le contenu markdown des docs.

## Modes d’identification

Identifiants supportés :

- `gtin`
- `brand` + `model`

Quand `brand` + `model` produisent plusieurs correspondances, le composant applique une politique conservative et **n’affiche pas** de lien produit.

## Comportement par défaut

- `style` : `text`
- `size` : `m`
- libellé visible : `BRAND - Model` si disponible
- titre au survol : meilleure chaîne de repli basée sur le nom long

## Exemples

```vue
<ProductEmbed gtin="8806092074061" />
```

```vue
<ProductEmbed brand="Samsung" model="QE55QN90A" size="s" />
```

```vue
<ProductEmbed gtin="8806092074061" size="l" />
```
