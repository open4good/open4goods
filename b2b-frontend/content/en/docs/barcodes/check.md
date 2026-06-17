---
title: "Barcode validity check"
description: "Validate any GTIN, EAN-13, UPC-A, or ISBN barcode and get forensic metadata plus a live product teaser from the nudger.fr index."
tags:
  - barcode
  - gtin
  - validation
  - free
scope: public
---

# Barcode validity check

The `barcode.check` facet validates a barcode and returns forensic GS1 metadata along with a product teaser when the GTIN is found in the nudger.fr index. This facet is **free** (0 credits) and available both as a public no-auth endpoint (rate-limited per IP) and as an authenticated API-key endpoint.

## Public endpoint (no authentication)

Use this endpoint for anonymous checks - no API key required. Requests are rate-limited per IP address.

```http
GET /api/v1/barcodes/check?barcode={barcode}
```

### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `barcode` | string | Yes | Raw barcode value (GTIN-8, GTIN-12, GTIN-13, GTIN-14, ISBN-10, ISBN-13, ISSN) |

### Example request

```bash
curl "https://api.product-data-api.com/api/v1/barcodes/check?barcode=3017620422003"
```

## Authenticated endpoint

For higher rate limits and audit-trail tracking, pass your API key.

```http
GET /api/v1/barcodes/{barcode}/check
Authorization: Bearer pdapi_YOUR_KEY_HERE
```

### Example request

```bash
curl -H "Authorization: Bearer pdapi_YOUR_KEY_HERE" \
  "https://api.product-data-api.com/api/v1/barcodes/3017620422003/check"
```

## Response schema

Both endpoints return the same JSON structure.

```json
{
  "barcode": "3017620422003",
  "forensics": {
    "valid": true,
    "type": "GTIN_13",
    "gs1Prefix": "301",
    "issuingCountryCode": "FR",
    "issuingCountryName": "France",
    "flagUrl": "https://flagcdn.com/fr.svg",
    "gs1Class": "GTIN",
    "gs1ClassLabel": "Standard trade item",
    "packagingIndicator": null,
    "isbnRegistrationGroup": null,
    "normalizedGtin14": "03017620422003",
    "normalizedGtin13": "3017620422003",
    "checkDigit": 3
  },
  "product": {
    "gtin": "3017620422003",
    "title": "Nutella - Ferrero - 400g",
    "coverImageUrl": "https://www.nudger.fr/api/image/...",
    "offersCount": 14,
    "bestPrice": 3.49,
    "currency": "EUR",
    "productUrl": "https://www.nudger.fr/fr/product/3017620422003"
  }
}
```

### `forensics` fields

| Field | Type | Description |
|---|---|---|
| `valid` | boolean | Whether the barcode passes check-digit validation |
| `type` | string | Detected barcode type (`GTIN_8`, `GTIN_12`, `GTIN_13`, `GTIN_14`, `ISBN_10`, `ISBN_13`, `ISSN`) |
| `gs1Prefix` | string | First 3 digits of the GS1 prefix |
| `issuingCountryCode` | string | ISO 3166-1 alpha-2 country code of the GS1 issuing organization |
| `issuingCountryName` | string | Human-readable country name |
| `flagUrl` | string | URL to a country flag SVG (flagcdn.com) |
| `gs1Class` | string | GS1 barcode class (`GTIN`, `ISBN_BOOKLAND`, `ISMN_MUSIC`, `ISSN_PERIODICAL`, `RESTRICTED_INTERNAL`, `COUPON`, `UNKNOWN`) |
| `gs1ClassLabel` | string | Human-readable label for the GS1 class |
| `packagingIndicator` | integer | For GTIN-14 only: packaging level indicator (first digit, 1-8) |
| `isbnRegistrationGroup` | string | For ISBN-13 only: registration group element (4th digit after 978/979 prefix) |
| `normalizedGtin14` | string | Barcode left-padded to 14 digits |
| `normalizedGtin13` | string | Barcode normalized to 13 digits (EAN-13 canonical form, null for GTIN-14 with packaging indicator ≠ 0) |
| `checkDigit` | integer | The check digit (last digit) |

### `product` field

The `product` field is `null` when the GTIN is not found in the nudger.fr index. When present:

| Field | Type | Description |
|---|---|---|
| `gtin` | string | Canonical GTIN |
| `title` | string | Product display name |
| `coverImageUrl` | string | Cover image URL (may be null) |
| `offersCount` | integer | Number of live price offers |
| `bestPrice` | number | Best available price in `currency` |
| `currency` | string | ISO 4217 currency code |
| `productUrl` | string | Direct link to the product page on nudger.fr |

## Billing

- **Credits consumed:** 0 (always free)
- **Billable:** never
- **Rate limiting:** IP-based for the public endpoint; key-based for the authenticated endpoint

## GS1 class reference

| Class | Description | Example prefix range |
|---|---|---|
| `ISBN_BOOKLAND` | Books (978-979) | 978, 979 |
| `ISMN_MUSIC` | Sheet music (979-0) | 979 + 4th digit 0 |
| `ISSN_PERIODICAL` | Periodicals / magazines | 977 |
| `RESTRICTED_INTERNAL` | In-store / internal use only | 20-29, 40-49, 200-299 |
| `COUPON` | Coupons and vouchers | 980-999 |
| `GTIN` | Standard trade item | All other prefixes |

## Quickstarts

- [Java quickstart](/docs/barcodes/check/documentation/java)
- [Python quickstart](/docs/barcodes/check/documentation/python)
- [Live playground](/docs/barcodes/check/playground)
