---
title: "Barcode render facet reference"
description: "Full reference for POST /api/v1/barcodes/render - parameters, response schema, metadata protection, and billing rules."
tags:
  - barcode
  - render
  - utility
scope: public
---

# Barcode render facet reference

The `barcode.render` utility facet provides high-quality, print-ready barcode image generation. It supports standard 1D and 2D symbologies, temporary signed assets, and custom metadata injection.

## Endpoint

```http
POST /api/v1/barcodes/render
Authorization: Bearer pdapi_YOUR_KEY_HERE
Content-Type: application/json
```

## Symbology List

The service supports the following barcode symbologies:

### 1D Symbologies
- `ean8` (EAN-8)
- `ean13` (EAN-13)
- `upca` (UPC-A)
- `upce` (UPC-E)
- `code128` (Code 128)
- `gs128` (GS1-128)
- `itf14` (ITF-14)

### 2D Symbologies
- `qr` (QR Code)
- `aztec` (Aztec)
- `datamatrix` (Data Matrix)
- `pdf417` (PDF-417)

## Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `type` | string | Yes | Symbology name (e.g., `ean13`, `qr`) |
| `data` | string | Yes | The payload to encode |
| `format` | string | Yes | Output file format (`png` or `svg`) |
| `width` | integer | Yes | Image width in pixels |
| `height` | integer | Yes | Image height in pixels |
| `foreground` | string | No | Foreground color (hex format, e.g., `#000000`) |
| `background` | string | No | Background color (hex format, e.g., `#ffffff`) |
| `rotation` | integer | No | Rotation angle (`0`, `90`, `180`, `270`) |
| `showText` | boolean | No | Whether to show human-readable text (1D only) |
| `quietZone` | boolean | No | Whether to include quiet zones / margins |
| `options` | object | No | Advanced formatting options (see below) |
| `metadata` | object | No | Custom copyright / description injection (see below) |

### `options` object

| Field | Type | Required | Description |
|---|---|---|---|
| `dpi` | integer | No | Resolution for PNG output (default: 300) |
| `moduleWidthMm` | number | No | Module width in millimeters (1D only) |
| `barHeightMm` | number | No | Bar height in millimeters (1D only) |
| `fontSize` | number | No | Font size for human-readable text (1D only) |
| `preset` | string | No | Predefined output preset (e.g. `print-safe`) |

### `metadata` object

Used to inject intellectual property headers directly into the generated assets.

| Field | Type | Required | Description |
|---|---|---|---|
| `copyright` | string | No | Copyright notice (max 256 chars) |
| `author` | string | No | Author metadata (max 256 chars) |
| `description` | string | No | Brief details about the asset (max 256 chars) |

## Example request

```bash
curl -X POST -H "Authorization: Bearer pdapi_YOUR_KEY_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ean13",
    "data": "4006381333931",
    "format": "png",
    "width": 200,
    "height": 100,
    "foreground": "#000000",
    "background": "#ffffff",
    "rotation": 0,
    "showText": true,
    "quietZone": true,
    "options": {
      "dpi": 300,
      "moduleWidthMm": 0.33,
      "barHeightMm": 15.0,
      "fontSize": 8.0,
      "preset": "print-safe"
    },
    "metadata": {
      "copyright": "Copyright 2026 open4goods",
      "author": "Open4Goods B2B API",
      "description": "Product GTIN Barcode"
    }
  }' \
  "https://api.product-data-api.com/api/v1/barcodes/render"
```

## Example response

```json
{
  "meta": {
    "requestId": "pdreq_01HXYZ",
    "billable": true,
    "creditsConsumed": 1
  },
  "assetUrl": "https://api.product-data-api.com/api/v1/barcodes/assets/eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-07-16T12:00:00Z",
  "dimensions": {
    "width": 200,
    "height": 100,
    "dpi": 300
  },
  "contentType": "image/png",
  "warnings": [],
  "inputHash": "sha256_a1b2c3d4..."
}
```

## Batch ZIP Export

To render multiple barcodes and download them packaged together inside a single ZIP archive, call the `/render-zip` endpoint.

```http
POST /api/v1/barcodes/render-zip
Authorization: Bearer pdapi_YOUR_KEY_HERE
Content-Type: application/json
Accept: application/zip
```

Pass a JSON array containing rendering request configurations:
```json
[
  {
    "type": "ean13",
    "data": "4006381333931",
    "format": "png",
    "width": 200,
    "height": 100
  },
  {
    "type": "qr",
    "data": "https://open4goods.org",
    "format": "svg",
    "width": 150,
    "height": 150
  }
]
```

## Metadata protection

To prevent unauthorized usage or copying of generated barcodes, the API automatically injects custom metadata blocks directly into the file headers:
- **PNG format**: metadata is written into standard `tEXt` chunks (Keyword: `Copyright`, `Author`, and `Description`).
- **SVG format**: metadata is written into a custom `<metadata>` block at the beginning of the XML document.

## Billing rules

- Generates on-demand: **1** credit per successfully rendered barcode.
- Batch exports: billed at `N` credits for `N` generated barcodes.
- Non-billable cases: if validation fails or server error occurs, no credits are consumed.

## Quickstarts

- [Java quickstart](/docs/barcodes/render/documentation/java)
- [Python quickstart](/docs/barcodes/render/documentation/python)
- [Live playground](/docs/barcodes/render/playground)
