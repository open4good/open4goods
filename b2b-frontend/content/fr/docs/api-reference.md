---
title: "Référence API"
description: "Tous les endpoints de Product Data API, l'enveloppe de réponse partagée et les conventions communes."
tags:
  - api-reference
  - endpoints
scope: public
---

# Référence API

Product Data API expose une API REST versionnée à `https://api.product-data-api.com`. Tous les endpoints retournent du JSON.

## URL de base

```
https://api.product-data-api.com
```

## Authentification

Chaque requête nécessite une clé API dans le header `Authorization` :

```http
Authorization: Bearer pdapi_VOTRE_CLÉ_ICI
```

Consultez [Authentification](/docs/authentication) pour la création et la rotation des clés.

## Enveloppe de réponse

Tous les endpoints de données encapsulent leur payload dans une enveloppe commune :

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
  "data": { ... }
}
```

| Champ | Type | Description |
|---|---|---|
| `meta.requestId` | string | Identifiant unique de la requête pour le support |
| `meta.gtin` | string | GTIN normalisé utilisé pour la recherche |
| `meta.facet` | string | Identifiant de la facette (`product.price` en v1) |
| `meta.billable` | boolean | Si des crédits ont été consommés |
| `meta.creditsConsumed` | integer | Crédits débités pour cette requête |
| `meta.creditsRemaining` | integer | Solde après cette requête |
| `meta.reason` | string | Code de décision de facturation |
| `meta.responseTimeMs` | integer | Temps de traitement serveur |

## Codes de raison de facturation

| Code | Signification |
|---|---|
| `fresh-offer` | Données fraîches retournées - facturé |
| `invalid-gtin` | Validation du checksum échouée - non facturé |
| `product-not-found` | Aucun produit correspondant - non facturé |
| `no-data` | Produit trouvé mais aucune offre - non facturé |
| `stale-data` | Offres existantes mais toutes périmées - non facturé |
| `insufficient-credits` | Crédits insuffisants - 402 retourné |

## Endpoints

### Facette prix

```
GET /api/v1/products/{gtin}/price
```

Retourne les offres prix fraîches pour le GTIN donné. Voir [Référence facette prix](/docs/products/price) pour le schéma complet.

**Paramètres**

| Paramètre | Dans | Type | Description |
|---|---|---|---|
| `gtin` | path | string | GTIN-8, -12, -13 ou -14 |
| `language` | query | string | Langue de la réponse (`en`, `fr`). Défaut : `en` |

### Facturation client

| Méthode | Chemin | Description |
|---|---|---|
| `GET` | `/api/v1/customer/billing/catalog` | Catalogue de facturation public (sans auth) |
| `GET` | `/api/v1/customer/billing/balance` | Solde de crédits et buckets |
| `GET` | `/api/v1/customer/billing/transactions` | Historique des transactions du grand livre |
| `GET` | `/api/v1/customer/billing/invoices` | Liste des factures Stripe |
| `POST` | `/api/v1/customer/billing/checkout/pack` | Démarrer un achat de pack |
| `POST` | `/api/v1/customer/billing/checkout/subscription` | Démarrer un abonnement |
| `POST` | `/api/v1/customer/billing/portal` | Ouvrir le portail de facturation |

### Gestion des clés API

| Méthode | Chemin | Description |
|---|---|---|
| `GET` | `/api/v1/customer/api-keys` | Lister les clés API de l'organisation |
| `POST` | `/api/v1/customer/api-keys` | Créer une nouvelle clé API |
| `POST` | `/api/v1/customer/api-keys/{id}/rotate` | Faire tourner une clé |
| `POST` | `/api/v1/customer/api-keys/{id}/revoke` | Révoquer définitivement une clé |

## Versioning

La version actuelle de l'API est `v1`. Les changements incompatibles incrémentent le préfixe de version.
