---
title: "Référence facette historique de prix"
description: "Référence complète de GET /api/v1/products/{gtin}/price/history - paramètres, schéma de réponse, pagination par curseur et facturation."
tags:
  - price-history
  - facet
  - products
scope: public
---

# Référence facette historique de prix

La facette historique de prix retourne des données de prix historiques sanitizées, multi-fournisseurs, pour un produit identifié par GTIN : agrégats quotidiens sur cinq ans maximum, ou événements de changement épars sur les 31 derniers jours.

## Endpoint

```http
GET /api/v1/products/{gtin}/price/history
Authorization: Bearer pdapi_VOTRE_CLÉ_ICI
```

## Paramètres

| Paramètre | Dans | Type | Défaut | Description |
|---|---|---|---|---|
| `gtin` | path | string | requis | GTIN-8, -12, -13 ou -14 |
| `language` | query | string | `en` | Langue de réponse (`en`, `fr`) |
| `from` | query | instant ISO-8601 | `to` moins 30 jours | Début inclusif de la plage, en UTC |
| `to` | query | instant ISO-8601 | maintenant | Fin exclusive de la plage, en UTC |
| `granularity` | query | `DAY` \| `CHANGE` | `DAY` | Voir ci-dessous |
| `provider` | query | string | aucun | Libellé public de fournisseur pour filtrer |
| `condition` | query | `NEW` \| `OCCASION` \| `UNKNOWN` | aucun | Filtre sur l'état du produit |
| `currency` | query | code ISO 4217 | aucun | Filtre sur la devise |
| `limit` | query | entier | 100 | Taille de page, bornée entre 1 et 500 |
| `cursor` | query | chaîne opaque | aucun | Continuation depuis le `nextCursor` d'une page précédente |

### Granularité

| Granularité | Fenêtre max | Forme du point |
|---|---|---|
| `DAY` | 5 ans | Agrégat quotidien : montants min/max/clôture et nombre d'offres |
| `CHANGE` | 31 jours | Événement épars : horodatage, montant et état de disponibilité |

Une fenêtre plus large que le maximum autorisé pour la granularité demandée, une plage inversée, ou une plage entièrement future, retourne un `400 Bad Request` avec la raison `invalid-date-range`.

### Pagination par curseur

`cursor` est entièrement opaque : renvoyez toujours exactement la valeur `nextCursor` reçue sur une page précédente. Ne construisez jamais de curseur à la main. Rejouer un curseur avec des filtres différents, ou envoyer une valeur falsifiée ou corrompue, échoue toujours avec un `400 Bad Request` (`cursor-mismatch`) - jamais une erreur serveur.

## Exemple de requête

```bash
curl -H "Authorization: Bearer pdapi_VOTRE_CLÉ_ICI" \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price/history?language=fr"
```

## Exemple de réponse (facturable, granularité DAY)

```json
{
  "meta": {
    "requestId": "req_01HXYZ",
    "gtin": "885909950805",
    "facet": "product.price-history",
    "billable": true,
    "creditsConsumed": 8,
    "creditsRemaining": 987,
    "reason": "has-history",
    "responseTimeMs": 44
  },
  "data": {
    "gtin": "0885909950805",
    "from": "2026-05-16T00:00:00Z",
    "to": "2026-06-15T00:00:00Z",
    "granularity": "DAY",
    "series": [
      {
        "provider": "Merchant Feed Partner",
        "condition": "NEW",
        "currency": "EUR",
        "dayPoints": [
          {
            "date": "2026-06-14",
            "minAmount": 749.00,
            "maxAmount": 819.99,
            "closeAmount": 799.99,
            "offerCount": 3
          }
        ],
        "changePoints": null
      }
    ],
    "nextCursor": null
  }
}
```

## Exemple de réponse (non facturable - pas d'historique)

```json
{
  "meta": {
    "requestId": "req_02HABC",
    "gtin": "885909950805",
    "facet": "product.price-history",
    "billable": false,
    "creditsConsumed": 0,
    "creditsRemaining": 987,
    "reason": "no-price-history",
    "responseTimeMs": 9
  },
  "data": null
}
```

## Schéma de réponse

### Objet `meta`

| Champ | Type | Description |
|---|---|---|
| `requestId` | string | Identifiant unique de requête pour le support |
| `gtin` | string | GTIN normalisé |
| `facet` | string | Toujours `product.price-history` |
| `billable` | boolean | `true` si des crédits ont été consommés |
| `creditsConsumed` | integer | Crédits débités (0 ou 8) |
| `creditsRemaining` | integer | Solde après cette requête |
| `reason` | string | Code de décision de facturation |
| `responseTimeMs` | integer | Temps de traitement serveur en ms |

### Objet `data`

| Champ | Type | Description |
|---|---|---|
| `gtin` | string | GTIN normalisé |
| `from` / `to` | string | Plage UTC effectivement servie (semi-ouverte) |
| `granularity` | string | Granularité effectivement servie |
| `series[]` | array | Une entrée par combinaison distincte fournisseur/état/devise |
| `nextCursor` | string \| null | Continuation opaque, absente sur la dernière page |

### Entrée `data.series[]`

| Champ | Type | Description |
|---|---|---|
| `provider` | string | Libellé public du marchand. Les identifiants internes ne sont jamais exposés |
| `condition` | string | `NEW`, `OCCASION` ou `UNKNOWN` |
| `currency` | string | Code devise ISO 4217 |
| `dayPoints[]` | array \| null | Renseigné quand `granularity` vaut `DAY` |
| `changePoints[]` | array \| null | Renseigné quand `granularity` vaut `CHANGE` |

Entrées `dayPoints[]` : `date`, `minAmount`, `maxAmount`, `closeAmount`, `offerCount`.

Entrées `changePoints[]` : `time`, `amount`, `state`.

## Facturation

La facette coûte **8 crédits**, débités uniquement si au moins un point autorisé par la politique est servi. GTIN invalide, produit absent, plage vide ou refusée, et résultat entièrement refusé par la politique consomment tous zéro crédit.

## Sourcing

Seules les sources revues et explicitement autorisées pour la redistribution de l'historique de prix sont servies. Un fournisseur sans libellé public revu n'apparaît jamais dans une réponse - refus par défaut, appliqué avant la décision de facturation.

## Démarrages rapides

- [Playground en direct](/docs/products/price-history/playground)
- Le code client suit le même schéma d'authentification et de gestion d'erreurs que les [démarrages rapides de la facette prix](/docs/products/price/documentation/java) - pointez la requête vers `/price/history` au lieu de `/price` et analysez la forme `series[]` ci-dessus.
