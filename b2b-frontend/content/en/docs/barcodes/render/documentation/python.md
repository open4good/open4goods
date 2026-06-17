---
title: "Python quickstart"
description: "Generate print-ready barcodes using the Product Data API barcode.render facet from Python."
tags:
  - python
  - quickstart
  - barcode
scope: public
---

# Python quickstart - Barcode Rendering

This guide shows how to call the barcode rendering facet from Python using `httpx` (recommended) or `requests`.

## Prerequisites

- Python 3.8 or higher
- A valid API key (`pdapi_...`) from [Dashboard → API Keys](/dashboard/api-keys)

## Using httpx (recommended)

Install:

```bash
pip install httpx
```

```python
import os
import httpx

API_BASE = "https://api.product-data-api.com"


def render_barcode(payload: dict, api_key: str) -> dict:
    """
    Renders a barcode and returns the metadata response envelope.
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
    print(f"Asset URL: {response['assetUrl']}")
    print(f"Expires at: {response['expiresAt']}")
```

## Using requests

Install:

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

## Next steps

- [Barcode render reference](/docs/barcodes/render) - full options and symbologies
- [Java quickstart](/docs/barcodes/render/documentation/java)
- [Live playground](/docs/barcodes/render/playground)
