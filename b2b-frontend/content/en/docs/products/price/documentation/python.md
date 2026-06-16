---
title: "Python quickstart"
description: "Query the Product Data API price facet from Python using the requests library or httpx."
tags:
  - python
  - quickstart
  - price
scope: public
---

# Python quickstart

This guide shows how to call the price facet from Python using `httpx` (recommended) or the standard `urllib` module.

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


def get_price(gtin: str, api_key: str) -> dict | None:
    """
    Returns the price data dict or None if no fresh data is available.
    Raises on HTTP errors (400, 401, 402, 5xx).
    """
    headers = {"Authorization": f"Bearer {api_key}"}
    url = f"{API_BASE}/api/v1/products/{gtin}/price"

    with httpx.Client() as client:
        r = client.get(url, headers=headers)

    if r.status_code == 404:
        return None  # Product not found - no credits consumed

    r.raise_for_status()
    payload = r.json()

    if not payload["meta"]["billable"]:
        # Non-billable: stale or no data - no credits consumed
        return None

    return payload["data"]


if __name__ == "__main__":
    api_key = os.environ["PRODUCT_DATA_API_KEY"]
    data = get_price("0885909950805", api_key)

    if data:
        best = data["bestPrice"]
        print(f"Best price: {best['price']} {best['currency']} from {best['merchant']}")
    else:
        print("No fresh price data available")
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


def get_price(gtin: str, api_key: str) -> dict | None:
    headers = {"Authorization": f"Bearer {api_key}"}
    r = requests.get(
        f"{API_BASE}/api/v1/products/{gtin}/price",
        headers=headers
    )

    if r.status_code == 404:
        return None

    r.raise_for_status()
    payload = r.json()

    if not payload["meta"]["billable"]:
        return None

    return payload["data"]
```

## Reading metering metadata

Every response includes metering metadata regardless of billability:

```python
payload = r.json()
meta = payload["meta"]

print(f"Request ID:        {meta['requestId']}")
print(f"Billable:          {meta['billable']}")
print(f"Credits consumed:  {meta['creditsConsumed']}")
print(f"Credits remaining: {meta['creditsRemaining']}")
print(f"Reason:            {meta['reason']}")
```

## Handling errors

```python
try:
    data = get_price(gtin, api_key)
except httpx.HTTPStatusError as e:
    if e.response.status_code == 400:
        print("Invalid GTIN")
    elif e.response.status_code == 401:
        print("Invalid or missing API key")
    elif e.response.status_code == 402:
        print("Insufficient credits - top up at the dashboard")
    elif e.response.status_code == 429:
        retry_after = e.response.headers.get("Retry-After", "1")
        print(f"Rate limited. Retry after {retry_after}s")
    else:
        raise
```

## Setting the API key

Never hardcode the API key. Use an environment variable:

```bash
export PRODUCT_DATA_API_KEY="pdapi_YOUR_KEY_HERE"
```

Or a `.env` file with `python-dotenv`:

```bash
pip install python-dotenv
```

```python
from dotenv import load_dotenv
load_dotenv()
api_key = os.environ["PRODUCT_DATA_API_KEY"]
```

## Async usage (httpx)

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

# Run multiple GTINs concurrently
gtins = ["0885909950805", "0194253408994", "0190199353688"]
tasks = [get_price_async(g, api_key) for g in gtins]
results = asyncio.run(asyncio.gather(*tasks))
```

## Next steps

- [Price facet reference](/docs/products/price) - full response schema
- [Error handling](/docs/errors) - how to handle all error codes
- [Java quickstart](/docs/products/price/documentation/java)
