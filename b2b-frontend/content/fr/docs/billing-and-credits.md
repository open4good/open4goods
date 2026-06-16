---
title: "Facturation et crédits"
description: "Fonctionnement du système de crédits, ce qui est facturé, la garantie no-data-no-pay et comment acheter des crédits."
tags:
  - billing
  - credits
  - no-data-no-pay
scope: public
---

# Facturation et crédits

Product Data API utilise un système de crédits où vous n'êtes facturé que pour les requêtes qui retournent des données fraîches exploitables. C'est la garantie **no-data-no-pay**.

## Fonctionnement des crédits

Les crédits sont consommés par requête de facette. Le coût dépend de la facette :

| Facette | Crédits par requête (facturée) |
|---|---|
| `product.price` | 5 crédits |

Un crédit n'est débité que quand l'API retourne une réponse facturable - c'est-à-dire quand la facette a retourné des données fraîches et exploitables pour le produit demandé.

## No-data-no-pay

Les situations suivantes coûtent **zéro crédit** :

| Situation | Statut HTTP | Raison |
|---|---|---|
| Checksum GTIN invalide | 400 | `invalid-gtin` |
| Produit introuvable dans notre index | 404 | `product-not-found` |
| Produit trouvé mais aucune offre prix | 200 | `no-data` |
| Toutes les offres dépassent la fenêtre de fraîcheur | 200 | `stale-data` |

La fenêtre de fraîcheur pour `product.price` est de **30 jours**. Si toutes les offres connues pour un produit ont plus de 30 jours, la réponse est non facturable.

## Cas facturables

Une requête est facturable quand :

1. Le GTIN est valide.
2. Le produit est dans notre index.
3. Au moins une offre prix est dans la fenêtre de fraîcheur.

L'objet `meta` dans chaque réponse vous indique exactement ce qui s'est passé :

```json
{
  "meta": {
    "billable": true,
    "creditsConsumed": 5,
    "creditsRemaining": 995,
    "reason": "fresh-offer"
  }
}
```

## Crédits gratuits

Chaque nouveau compte reçoit **2 500 crédits gratuits** après la première connexion. Valables 12 mois à compter de la création.

## Acheter des crédits

### Packs de crédits

Achats ponctuels de crédits. Achetez autant que nécessaire, utilisez-les à votre rythme.

### Abonnements

Crédits mensuels avec renouvellement automatique. Les crédits non utilisés sont reportés jusqu'à un plafond configurable. L'annulation d'un abonnement programme l'expiration des crédits restants 30 jours après la fin de la période en cours.

Gérez packs et abonnements dans [Tableau de bord → Facturation](/dashboard/billing).

## Ordre de consommation des crédits

Lors d'une facturation, les crédits sont consommés en priorité depuis le bucket dont la date d'expiration est la plus proche. Cela garantit que vos crédits les plus anciens sont utilisés avant les plus récents.

## 402 Crédits insuffisants

Si votre solde est nul et qu'une requête facturable arrive, l'API retourne :

```json
{
  "type": "https://product-data-api.com/errors/payment-required",
  "title": "Payment Required",
  "status": 402,
  "detail": "Crédits insuffisants pour cette requête."
}
```

Aucune donnée n'est retournée et aucun crédit n'est consommé sur un 402.
