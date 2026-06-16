---
title: "Référence facette prix"
description: "Référence complète de GET /api/v1/products/{gtin}/price - paramètres, schéma de réponse, règles de fraîcheur et facturation."
tags:
  - price
  - facet
  - products
scope: public
---

# Référence facette prix

La facette prix retourne des offres prix sanitizées, des métadonnées de fraîcheur et des libellés de provenance pour un produit identifié par GTIN.

## Endpoint

```http
GET /api/v1/products/{gtin}/price
Authorization: Bearer pdapi_VOTRE_CLÉ_ICI
```

## Paramètres

| Paramètre | Dans | Type | Requis | Description |
|---|---|---|---|---|
| `gtin` | path | string | Oui | GTIN-8, -12, -13 ou -14 |
| `language` | query | string | Non | Langue de réponse (`en`, `fr`). Défaut : `en` |

## Exemple de requête

```bash
curl -H "Authorization: Bearer pdapi_VOTRE_CLÉ_ICI" \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price?language=fr"
```

## Exemple de réponse (facturable)

```json
{
  "meta": {
    "requestId": "req_01HXYZ",
    "gtin": "885909950805",
    "facet": "product.price",
    "billable": true,
    "creditsConsumed": 5,
    "creditsRemaining": 995,
    "reason": "fresh-offer",
    "responseTimeMs": 31
  },
  "data": {
    "bestPrice": {
      "price": 699.0,
      "currency": "EUR",
      "merchant": "TechStore FR",
      "condition": "new"
    },
    "offers": [
      {
        "price": 699.0,
        "currency": "EUR",
        "merchant": "TechStore FR",
        "condition": "new",
        "lastSeenDays": 1
      },
      {
        "price": 739.0,
        "currency": "EUR",
        "merchant": "BigBox Online",
        "condition": "new",
        "lastSeenDays": 4
      }
    ],
    "offerCount": 2,
    "freshness": {
      "oldestOfferDays": 4,
      "newestOfferDays": 1,
      "windowDays": 30
    }
  }
}
```

## Exemple de réponse (non facturable - données périmées)

```json
{
  "meta": {
    "requestId": "req_02HABC",
    "gtin": "885909950805",
    "facet": "product.price",
    "billable": false,
    "creditsConsumed": 0,
    "creditsRemaining": 995,
    "reason": "stale-data",
    "responseTimeMs": 12
  },
  "data": null
}
```

## Fraîcheur et facturation

Une offre est considérée fraîche si elle a été observée dans les **30 derniers jours**. Si toutes les offres connues pour un produit ont plus de 30 jours, la réponse est non facturable (`reason: stale-data`).

## Normalisation GTIN

Les GTINs sont normalisés avant la recherche :
- Les zéros de tête sont préservés pour les GTIN-13 et GTIN-14.
- Les GTIN-8 et GTIN-12 (UPC-A) sont acceptés et validés par checksum.
- Les checksums invalides retournent `400 Bad Request` immédiatement, avant toute réservation de crédits.

## Quickstarts

- [Quickstart Java](/docs/products/price/documentation/java)
- [Quickstart Python](/docs/products/price/documentation/python)
- [Playground en direct](/docs/products/price/playground)
