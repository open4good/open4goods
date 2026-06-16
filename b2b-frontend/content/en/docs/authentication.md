---
title: "Authentication"
description: "How to authenticate with Product Data API using API keys - creation, rotation, revocation, and security best practices."
tags:
  - authentication
  - api-keys
  - security
scope: public
---

# Authentication

Product Data API uses bearer API keys for authenticating product data requests. Keys start with the prefix `pdapi_` and are passed in the `Authorization` header.

## Making an authenticated request

```http
GET /api/v1/products/0885909950805/price HTTP/1.1
Host: api.product-data-api.com
Authorization: Bearer pdapi_abc123...
```

Or with curl:

```bash
curl -H "Authorization: Bearer pdapi_YOUR_KEY_HERE" \
  "https://api.product-data-api.com/api/v1/products/0885909950805/price"
```

## Creating an API key

1. Sign in at [product-data-api.com/auth/login](/auth/login).
2. Navigate to [Dashboard → API Keys](/dashboard/api-keys).
3. Click **Create key** and enter a name.
4. **Copy the clear secret immediately** - it is shown only once after creation.
5. Store the secret in your secrets manager, environment variable, or CI secret store.

The API response includes:

```json
{
  "key": {
    "id": "key_01HXYZ",
    "name": "production",
    "keyPrefix": "pdapi_abc1",
    "status": "ACTIVE",
    "createdAt": "2026-06-16T12:00:00Z",
    "lastUsedAt": null
  },
  "clearKey": "pdapi_abc1234567890abcdef..."
}
```

The `clearKey` field is only present in the creation response and in rotation responses.

## Key states

| State | Description |
|---|---|
| `ACTIVE` | Key accepts requests |
| `ROTATED` | Superseded by a rotation; the new key is active |
| `REVOKED` | Permanently disabled; any request returns 401 |

## Rotating a key

Rotation creates a new key and immediately invalidates the previous secret. Use rotation to refresh credentials on a schedule or after a suspected exposure.

1. Go to [Dashboard → API Keys](/dashboard/api-keys).
2. Click the **Rotate** icon next to the key.
3. Copy the new clear secret - it appears only once.
4. Update your deployments with the new secret before restarting them.

```bash
# Rotate via API (session auth required)
curl -X POST \
  -H "Cookie: session=..." \
  "https://api.product-data-api.com/api/v1/customer/api-keys/key_01HXYZ/rotate"
```

## Revoking a key

Revocation is permanent. Use it if a key is compromised or no longer needed.

```bash
curl -X POST \
  -H "Cookie: session=..." \
  "https://api.product-data-api.com/api/v1/customer/api-keys/key_01HXYZ/revoke"
```

Any request using a revoked key returns `401 Unauthorized`:

```json
{
  "type": "https://product-data-api.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "API key is revoked or invalid."
}
```

## Security best practices

- **Never expose keys in browser-side code or public repositories.** Use environment variables or secrets managers.
- **Rotate keys periodically** - at least every 90 days for production keys.
- **Create separate keys per environment** - one for development, one for staging, one for production.
- **Revoke immediately on suspected exposure.**
- **Use key names that reflect the environment** - helps identify and rotate the right key quickly.

## Key metadata

Each key tracks:

| Field | Description |
|---|---|
| `keyPrefix` | First 8 characters of the key, safe to log |
| `createdAt` | ISO 8601 creation timestamp |
| `lastUsedAt` | Last successful request timestamp; null if never used |
| `createdBy` | User who created the key |
