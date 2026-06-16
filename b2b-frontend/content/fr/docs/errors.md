---
title: "Gestion des erreurs"
description: "Format Problem Detail, tous les types d'erreurs avec exemples et comment les gérer dans votre intégration."
tags:
  - errors
  - error-handling
  - http
scope: public
---

# Gestion des erreurs

Toutes les erreurs de Product Data API suivent le format [RFC 9457 Problem Detail](https://www.rfc-editor.org/rfc/rfc9457). Chaque réponse d'erreur a un corps JSON cohérent et un code HTTP.

## Format de la réponse d'erreur

```json
{
  "type": "https://product-data-api.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Clé API manquante ou invalide.",
  "instance": "/api/v1/products/0885909950805/price"
}
```

| Champ | Description |
|---|---|
| `type` | URI identifiant le type d'erreur |
| `title` | Description courte lisible par un humain |
| `status` | Code HTTP |
| `detail` | Détail spécifique à cette occurrence |
| `instance` | Chemin de la requête ayant déclenché l'erreur |

## Codes HTTP

### 400 Bad Request - GTIN invalide

```json
{
  "type": "https://product-data-api.com/errors/invalid-gtin",
  "title": "Invalid GTIN",
  "status": 400,
  "detail": "Le GTIN '12345' a échoué la validation du checksum."
}
```

**Que faire :** Validez les GTINs côté client avant l'envoi. Un 400 ne consomme pas de crédits.

### 401 Unauthorized - Clé manquante ou invalide

```json
{
  "type": "https://product-data-api.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Clé API manquante ou invalide."
}
```

**Que faire :** Vérifiez que le header `Authorization: Bearer pdapi_...` est présent et que la clé est active. Les clés révoquées retournent 401.

### 402 Payment Required - Crédits insuffisants

```json
{
  "type": "https://product-data-api.com/errors/payment-required",
  "title": "Payment Required",
  "status": 402,
  "detail": "Crédits insuffisants pour cette requête."
}
```

**Que faire :** Rechargez des crédits dans [Tableau de bord → Facturation](/dashboard/billing). Surveillez `meta.creditsRemaining` pour anticiper l'épuisement.

### 404 Not Found - Produit introuvable

```json
{
  "type": "https://product-data-api.com/errors/product-not-found",
  "title": "Product Not Found",
  "status": 404,
  "detail": "Aucun produit trouvé pour le GTIN '0000000000000'."
}
```

**Que faire :** Le produit n'est pas dans notre index. Un 404 ne consomme pas de crédits.

### 429 Rate Limited

```json
{
  "type": "https://product-data-api.com/errors/rate-limited",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Limite de débit dépassée. Réessayez après 1 seconde."
}
```

**Que faire :** Respectez le header `Retry-After`. Implémentez un backoff exponentiel.

## Réponses 200 non facturables

Une réponse `200 OK` n'est pas toujours facturable. Vérifiez `meta.billable` pour savoir si des crédits ont été consommés. Un 200 non facturable signifie que le produit a été trouvé mais qu'aucune donnée fraîche n'était disponible.
