---
title: "Authentification"
description: "Comment s'authentifier avec Product Data API via les clés API - création, rotation, révocation et bonnes pratiques de sécurité."
tags:
  - authentication
  - api-keys
  - security
scope: public
---

# Authentification

Product Data API utilise des clés API bearer pour authentifier les requêtes de données produit. Les clés commencent par le préfixe `pdapi_` et se passent dans le header `Authorization`.

## Effectuer une requête authentifiée

```http
GET /api/v1/products/0885909950805/price HTTP/1.1
Host: api.product-data-api.com
Authorization: Bearer pdapi_abc123...
```

Ou avec curl :

```bash
curl -H "Authorization: Bearer pdapi_VOTRE_CLÉ_ICI" \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price"
```

## Créer une clé API

1. Connectez-vous sur [product-data-api.com/auth/login](/auth/login).
2. Rendez-vous dans [Tableau de bord → Clés API](/dashboard/api-keys).
3. Cliquez sur **Créer une clé** et entrez un nom.
4. **Copiez le secret en clair immédiatement** - il n'est affiché qu'une seule fois après la création.
5. Stockez le secret dans votre gestionnaire de secrets, variable d'environnement ou secret CI.

## États d'une clé

| État | Description |
|---|---|
| `ACTIVE` | La clé accepte les requêtes |
| `ROTATED` | Remplacée par une rotation ; la nouvelle clé est active |
| `REVOKED` | Désactivée définitivement ; toute requête retourne 401 |

## Faire tourner une clé

La rotation crée une nouvelle clé et invalide immédiatement le secret précédent. Utilisez la rotation pour renouveler les identifiants régulièrement ou après une exposition suspectée.

1. Rendez-vous dans [Tableau de bord → Clés API](/dashboard/api-keys).
2. Cliquez sur l'icône **Rotation** en regard de la clé.
3. Copiez le nouveau secret en clair - affiché une seule fois.
4. Mettez à jour vos déploiements avec le nouveau secret avant de les redémarrer.

## Révoquer une clé

La révocation est permanente. Utilisez-la si une clé est compromise ou n'est plus nécessaire.

Toute requête utilisant une clé révoquée retourne `401 Unauthorized` :

```json
{
  "type": "https://product-data-api.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "La clé API est révoquée ou invalide."
}
```

## Bonnes pratiques de sécurité

- **N'exposez jamais les clés dans le code côté navigateur ou les dépôts publics.** Utilisez des variables d'environnement ou des gestionnaires de secrets.
- **Faites tourner les clés régulièrement** - au moins tous les 90 jours pour les clés de production.
- **Créez des clés séparées par environnement** - une pour le développement, une pour la staging, une pour la production.
- **Révoquez immédiatement en cas d'exposition suspectée.**
