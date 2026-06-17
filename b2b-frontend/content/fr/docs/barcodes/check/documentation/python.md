---
title: "Démarrage rapide Python"
description: "Validez des codes-barres et récupérez des métadonnées produit via le facet barcode.check de l'API Product Data depuis Python."
tags:
  - python
  - quickstart
  - barcode
  - validation
scope: public
---

# Démarrage rapide Python - Vérification de code-barres

Ce guide montre comment appeler le point de terminaison de vérification de code-barres depuis Python en utilisant `httpx` (recommandé) ou `requests`.

## Prérequis

- Python 3.8 ou supérieur
- Aucune clé API requise pour le point de terminaison public (optionnel pour des limites de débit plus élevées)

## Point de terminaison public avec httpx (sans authentification)

Installation :

```bash
pip install httpx
```

```python
import httpx

API_BASE = "https://api.product-data-api.com"


def check_barcode(barcode: str) -> dict:
    url = f"{API_BASE}/api/v1/barcodes/check"
    with httpx.Client() as client:
        r = client.get(url, params={"barcode": barcode})
    r.raise_for_status()
    return r.json()


if __name__ == "__main__":
    result = check_barcode("3017620422003")

    forensics = result["forensics"]
    print(f"Valide : {forensics['valid']}")
    print(f"Type : {forensics['type']}")
    print(f"Pays : {forensics.get('issuingCountryName')}")
    print(f"Classe GS1 : {forensics.get('gs1Class')}")

    product = result.get("product")
    if product:
        print(f"Produit : {product.get('title')}")
        print(f"Meilleur prix : {product.get('bestPrice')} {product.get('currency')}")
        print(f"Offres : {product.get('offersCount')}")
    else:
        print("Produit non trouvé dans l'index nudger.fr")
```

## Point de terminaison authentifié (limites de débit plus élevées)

```python
import os
import httpx

API_BASE = "https://api.product-data-api.com"


def check_barcode_auth(gtin: str, api_key: str) -> dict:
    url = f"{API_BASE}/api/v1/barcodes/{gtin}/check"
    headers = {"Authorization": f"Bearer {api_key}"}
    with httpx.Client() as client:
        r = client.get(url, headers=headers)
    r.raise_for_status()
    return r.json()


if __name__ == "__main__":
    api_key = os.environ["PRODUCT_DATA_API_KEY"]
    result = check_barcode_auth("3017620422003", api_key)
    print(result)
```

## Avec requests

Installation :

```bash
pip install requests
```

```python
import requests

API_BASE = "https://api.product-data-api.com"


def check_barcode(barcode: str) -> dict:
    url = f"{API_BASE}/api/v1/barcodes/check"
    r = requests.get(url, params={"barcode": barcode})
    r.raise_for_status()
    return r.json()
```

## Prochaines étapes

- [Référence barcode.check](/docs/barcodes/check) - schéma complet et référence des classes GS1
- [Démarrage rapide Java](/docs/barcodes/check/documentation/java)
- [Bac à sable interactif](/docs/barcodes/check/playground)
