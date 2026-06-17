---
title: "Démarrage rapide Python"
description: "Générez des codes-barres prêts à l'impression avec l'API Product Data de open4goods en Python."
tags:
  - python
  - quickstart
  - barcode
scope: public
---

# Démarrage rapide Python - Rendu de code-barres

Ce guide montre comment appeler le point de terminaison de génération de code-barres en Python à l'aide de `httpx` ou de `requests`.

## Prérequis

- Python 3.8 ou version supérieure
- Une clé API active (`pdapi_...`) récupérée depuis le [Tableau de bord → Clés API](/dashboard/api-keys)

## Utilisation de httpx (recommandé)

Installez la dépendance :

```bash
pip install httpx
```

```python
import os
import httpx

API_BASE = "https://api.product-data-api.com"


def render_barcode(payload: dict, api_key: str) -> dict:
    """
    Génère un code-barres et renvoie les métadonnées de réponse.
    """
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    url = f"{API_BASE}/api/v1/barcodes/render"

    with httpx.Client() as client:
        r = client.post(url, json=payload, headers=headers)

    r.raise_for_status()
    return r.json()


if __name__ == "__main__":
    api_key = os.environ["PRODUCT_DATA_API_KEY"]

    request_payload = {
        "type": "ean13",
        "data": "4006381333931",
        "format": "png",
        "width": 200,
        "height": 100,
        "metadata": {
            "copyright": "Copyright 2026 open4goods"
        }
    }

    response = render_barcode(request_payload, api_key)
    print(f"URL de l'image : {response['assetUrl']}")
    print(f"Expire le : {response['expiresAt']}")
```

## Utilisation de requests

Installez la dépendance :

```bash
pip install requests
```

```python
import os
import requests

API_BASE = "https://api.product-data-api.com"


def render_barcode(payload: dict, api_key: str) -> dict:
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    url = f"{API_BASE}/api/v1/barcodes/render"

    r = requests.post(url, json=payload, headers=headers)
    r.raise_for_status()
    return r.json()
```

## Liens rapides

- [Spécification de rendu](/fr/docs/barcodes/render) - options de symbologies
- [Démarrage rapide Java](/fr/docs/barcodes/render/documentation/java)
- [Bac à sable (Playground) interactif](/fr/docs/barcodes/render/playground)
