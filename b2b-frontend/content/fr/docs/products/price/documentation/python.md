---
title: "Quickstart Python"
description: "Interrogez la facette prix de Product Data API depuis Python avec httpx ou requests."
tags:
  - python
  - quickstart
  - price
scope: public
---

# Quickstart Python

Ce guide montre comment appeler la facette prix depuis Python avec `httpx` (recommandé) ou `requests`.

## Prérequis

- Python 3.8 ou supérieur
- Une clé API valide (`pdapi_...`) depuis [Tableau de bord → Clés API](/dashboard/api-keys)

## Avec httpx (recommandé)

Installation :

```bash
pip install httpx
```

```python
import os
import httpx

API_BASE = "https://api.product-data-api.com"


def get_price(gtin: str, api_key: str) -> dict | None:
    """
    Retourne les données prix ou None si aucune donnée fraîche n'est disponible.
    Lève une exception sur les erreurs HTTP (400, 401, 402, 5xx).
    """
    headers = {"Authorization": f"Bearer {api_key}"}
    url = f"{API_BASE}/api/v1/products/{gtin}/price"

    with httpx.Client() as client:
        r = client.get(url, headers=headers)

    if r.status_code == 404:
        return None  # Produit introuvable - aucun crédit consommé

    r.raise_for_status()
    payload = r.json()

    if not payload["meta"]["billable"]:
        # Non facturable : données périmées ou absentes - aucun crédit consommé
        return None

    return payload["data"]


if __name__ == "__main__":
    api_key = os.environ["PRODUCT_DATA_API_KEY"]
    data = get_price("0885909950805", api_key)

    if data:
        best = data["bestPrice"]
        print(f"Meilleur prix : {best['price']} {best['currency']} chez {best['merchant']}")
    else:
        print("Aucune donnée prix fraîche disponible")
```

## Lire les métadonnées de facturation

Chaque réponse inclut des métadonnées de facturation quel que soit le caractère facturable :

```python
payload = r.json()
meta = payload["meta"]

print(f"ID requête :       {meta['requestId']}")
print(f"Facturable :       {meta['billable']}")
print(f"Crédits consommés: {meta['creditsConsumed']}")
print(f"Crédits restants : {meta['creditsRemaining']}")
print(f"Raison :           {meta['reason']}")
```

## Gestion des erreurs

```python
try:
    data = get_price(gtin, api_key)
except httpx.HTTPStatusError as e:
    if e.response.status_code == 400:
        print("GTIN invalide")
    elif e.response.status_code == 401:
        print("Clé API invalide ou manquante")
    elif e.response.status_code == 402:
        print("Crédits insuffisants - rechargez depuis le tableau de bord")
    elif e.response.status_code == 429:
        retry_after = e.response.headers.get("Retry-After", "1")
        print(f"Limite de débit. Réessayez dans {retry_after}s")
    else:
        raise
```

## Lire la clé API

Ne codez jamais la clé en dur. Utilisez une variable d'environnement :

```bash
export PRODUCT_DATA_API_KEY="pdapi_VOTRE_CLÉ_ICI"
```

## Utilisation asynchrone (httpx)

```python
import asyncio
import httpx

async def get_price_async(gtin: str, api_key: str) -> dict | None:
    headers = {"Authorization": f"Bearer {api_key}"}
    async with httpx.AsyncClient() as client:
        r = await client.get(
            f"https://api.product-data-api.com/api/v1/products/{gtin}/price",
            headers=headers
        )
    if r.status_code == 404:
        return None
    r.raise_for_status()
    payload = r.json()
    return payload["data"] if payload["meta"]["billable"] else None

# Interroger plusieurs GTINs en parallèle
gtins = ["0885909950805", "0194253408994", "0190199353688"]
tasks = [get_price_async(g, api_key) for g in gtins]
results = asyncio.run(asyncio.gather(*tasks))
```

## Étapes suivantes

- [Référence facette prix](/docs/products/price) - schéma complet de la réponse
- [Gestion des erreurs](/docs/errors) - tous les codes d'erreur
- [Quickstart Java](/docs/products/price/documentation/java)
