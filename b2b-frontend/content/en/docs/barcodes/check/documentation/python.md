---
title: "Python quickstart"
description: "Validate barcodes and retrieve product metadata using the Product Data API barcode.check facet from Python."
tags:
  - python
  - quickstart
  - barcode
  - validation
scope: public
---

# Python quickstart - Barcode Validity Check

This guide shows how to call the free barcode validity check endpoint from Python using `httpx` (recommended) or `requests`.

## Prerequisites

- Python 3.8 or higher
- No API key needed for the public endpoint (optional for higher rate limits)

## Public endpoint using httpx (no authentication)

Install:

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
    print(f"Valid: {forensics['valid']}")
    print(f"Type: {forensics['type']}")
    print(f"Country: {forensics.get('issuingCountryName')}")
    print(f"GS1 class: {forensics.get('gs1Class')}")

    product = result.get("product")
    if product:
        print(f"Product: {product.get('title')}")
        print(f"Best price: {product.get('bestPrice')} {product.get('currency')}")
        print(f"Offers: {product.get('offersCount')}")
    else:
        print("Product not found in nudger.fr index")
```

## Authenticated endpoint (higher rate limits)

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

## Using requests

Install:

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

## Next steps

- [Barcode check reference](/docs/barcodes/check) - full response schema and GS1 class reference
- [Java quickstart](/docs/barcodes/check/documentation/java)
- [Live playground](/docs/barcodes/check/playground)
