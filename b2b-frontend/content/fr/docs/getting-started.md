---
title: "Bien démarrer"
description: "Créez votre compte, obtenez votre première clé API et effectuez votre première requête prix en quelques minutes."
tags:
  - getting-started
  - quickstart
scope: public
---

# Bien démarrer

Product Data API est une API REST métrée qui retourne des données de prix fraîches par GTIN. Vous êtes facturé uniquement quand la facette retourne des données fraîches exploitables - GTIN invalides, produits introuvables et offres périmées coûtent zéro crédit.

## 1. Créer un compte

Créez un compte gratuit sur [product-data-api.com/auth/login](/auth/login). Connectez-vous avec Google, Microsoft, GitHub ou Apple. Votre compte reçoit automatiquement **2 500 crédits gratuits** après la première connexion.

## 2. Créer votre première clé API

Rendez-vous dans [Tableau de bord → Clés API](/dashboard/api-keys) et cliquez sur **Créer une clé**.

- Choisissez un nom descriptif (ex. : `dev-test`).
- Le secret en clair est affiché **une seule fois**. Copiez-le immédiatement et conservez-le en sécurité.
- Votre clé a le préfixe `pdapi_` et s'utilise dans le header `Authorization`.

## 3. Interroger la facette prix

```bash
curl -H "Authorization: Bearer pdapi_VOTRE_CLÉ_ICI" \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price"
```

Remplacez `0885909950805` par un GTIN-13 ou GTIN-14 réel.

## 4. Comprendre la réponse

Une réponse réussie ressemble à :

```json
{
  "meta": {
    "requestId": "req_01HXYZ",
    "gtin": "885909950805",
    "facet": "product.price",
    "billable": true,
    "creditsConsumed": 5,
    "creditsRemaining": 2495,
    "reason": "fresh-offer",
    "responseTimeMs": 34
  },
  "data": {
    "bestPrice": {
      "price": 699.0,
      "currency": "EUR",
      "merchant": "TechStore FR"
    },
    "offerCount": 3,
    "freshness": {
      "oldestOfferDays": 4,
      "newestOfferDays": 1
    }
  }
}
```

Si aucune donnée fraîche n'est disponible, `billable` est `false` et `creditsConsumed` vaut `0`.

## Cas non facturés

| Situation | Statut HTTP | Crédits |
|---|---|---|
| Checksum GTIN invalide | 400 | 0 |
| Produit introuvable | 404 | 0 |
| Toutes les offres périmées (> 30 jours) | 200 | 0 |
| Offre fraîche retournée | 200 | 5 |

## Étapes suivantes

- [Référence API](/docs/api-reference) - contrat complet des endpoints
- [Authentification](/docs/authentication) - sécurité et rotation des clés
- [Facturation et crédits](/docs/billing-and-credits) - règles détaillées
- [Référence facette prix](/docs/products/price) - schéma complet de la réponse
- [Playground](/docs/products/price/playground) - testez des appels en direct dans le navigateur
