# Facet spec - `barcode.render`

> Status: RELEASED
> Authority: [`00-canonical-decisions.md`](../00-canonical-decisions.md) ·
> Lifecycle: [`README.md`](README.md) · Catalogue:
> [`facet-catalog.md`](../product/facet-catalog.md) · Common contract:
> [API contract](../../architecture/product-data-api-contract.md)

## 1. Overview and value proposition

### 1.1 Value proposition
The `barcode.render` utility facet provides high-quality, print-ready barcode image generation. It supports standard 1D (EAN-8, EAN-13, Code 128, GS1-128, ITF-14, UPC-A, UPC-E) and 2D (QR, Aztec, Data Matrix, PDF417) symbologies. 

It generates signed, temporary (30-day) asset URLs that can be embedded directly in third-party environments (e.g., PDF reports, HTML emails, shipping templates) without exposing API bearer tokens. Additionally, it supports a unique **metadata injection** feature to write custom copyright and origin metadata directly into the image files (PNG chunks / SVG metadata tags).

### 1.2 Competitive analysis
Detailing the competitive landscape for barcode APIs:
- **Competitors**:
  - **Orca Scan**: Free image API, no key required, supports basic formats but enforces standard rate limits and offers no custom metadata control or print-safe verification.
  - **TEC-IT**: High enterprise cost starting from EUR 199/year (up to 500 requests/month). Does not support dynamic metadata injection.
  - **Labelary Barcode API**: Focuses on ZPL/PDF rendering. Trial is watermarked; paid plans start at $44/month.
  - **QuickChart**: Flexible open-source renderer. Paid tier at $40/month. No built-in metadata modification or structural copyright injection.
- **Pricing**: Aligned with our utility pricing, we charge a flat **1 credit** per successfully generated barcode (approx. 0.002 EUR). ZIP exports containing multiple barcodes are billed at `itemCount * 1` credit.
- **Uncovered Areas & Gaps**:
  - No competitor allows embedding custom **metadata / copyright annotations** into the generated image file headers (PNG ancillary chunks or SVG metadata elements) to protect intellectual property or preserve asset origin info.
  - Most competitors charge high flat-rate monthly subscriptions; our pay-per-render metering offers a low-barrier, high-flexibility option.
  - Competitors return images directly but do not offer signed, public CDN-cacheable asset URLs with automated expiration.

## 2. Coverage and data quality (measured)

| Measure | Result | Date |
|---|---:|---|
| Logical coverage | 100% (on-demand generation) | 2026-06-16 |
| Supported symbologies | 13 (11 1D/GS1 + 2 2D) | 2026-06-16 |

Because `barcode.render` is a generation utility rather than a database index lookup, it has 100% logical coverage. 

Quality and print correctness are guaranteed via two layers:
1. **1D Symbologies (Barcode4J)**: Built-in validation of character sets, length requirements, and checksums (e.g., Modulo 10 for EAN/UPC).
2. **2D Symbologies (ZXing)**: Automated loopback checks during build/tests to guarantee that the generated QR/Aztec images decode back to the exact input payload.

Quality probes (spot-checking a running `b2b-api` endpoint with invalid data formats):
```bash
# Verify checksum validation fails with 400 Bad Request
curl -i -X POST -H "Authorization: Bearer $KEY" \
  -H "Content-Type: application/json" \
  -d '{"type":"ean13", "data":"400638133393"}' \
  "http://localhost:8087/api/v1/barcodes/render"
```

## 3. Endpoint and credits

- Endpoints:
  - `POST /api/v1/barcodes/render` (Single barcode)
  - `POST /api/v1/barcodes/render-zip` (Batch ZIP export)
  - `GET /api/v1/barcodes/assets/{token}` (Public signed asset download)
- Credits: **1** credit per successfully generated barcode (ZIP export bills `N` credits for `N` items)
- Billable when: `valid-render` (the barcode is successfully generated and written to the cache store)

Catalog entry:

```yaml
b2b:
  facets:
    barcode.render:
      path: /api/v1/barcodes/render
      credits: 1
      doc: barcodes/render
      billable-when: valid-render
```

### 3.1 DTO Structures

#### B2bBarcodeRenderRequest
```json
{
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
}
```

#### B2bBarcodeRenderResponse
```json
{
  "meta": {
    "requestId": "pdreq_01HF...",
    "billable": true,
    "creditsConsumed": 1
  },
  "assetUrl": "https://api.product-data-api.com/api/v1/barcodes/assets/eyJhbGci...",
  "expiresAt": "2026-07-16T12:00:00Z",
  "dimensions": {
    "width": 200,
    "height": 100,
    "dpi": 300
  },
  "contentType": "image/png",
  "warnings": [],
  "inputHash": "a1b2c3d4..."
}
```

## 4. Sanitization (allow-list)

No internal database records are exposed by this utility. However, user input fields (`data` and the `metadata` blocks) are subjected to strict sanitization rules:
- **Maximum string length**: `metadata.copyright`, `metadata.author`, and `metadata.description` are restricted to a maximum of 256 characters each.
- **Character set sanitization**: Standard HTML/XML escaping is applied to all input parameters to prevent injection attacks when generating SVG documents or embedding metadata elements inside XML tags.
- **Control characters**: Non-printable ASCII control characters are stripped to prevent header/chunk corruption in PNG files.

## 5. No-data-no-pay matrix

Unlike product facets, `barcode.render` handles generation actions. Billing is strictly based on successful file cache generation.

| Condition | HTTP | `billable` | no-pay reason |
|---|---:|---|---|
| Invalid parameters (type, data length, bad EAN checksum) | 400 | - | `invalid-input` |
| Insufficient credit balance | 402 | - | `insufficient-credits` |
| Internal generator error or storage write failure | 500 | - | `render-failure` |
| Successful rendering & storage | 200 | true | - (debit 1 credit) |

## 6. Docs pages

| Page | en | fr |
|---|---|---|
| Facet reference | `/docs/barcodes/render` | `/fr/docs/barcodes/render` |
| Playground | `/docs/barcodes/render/playground` | mirror |
| Java quickstart | `/docs/barcodes/render/documentation/java` | mirror |
| Python quickstart | `/docs/barcodes/render/documentation/python` | mirror |

## 7. SEO plan

Target queries (intent: developers looking for barcode generator APIs):

| Query (en) | Query (fr) | Capturing page |
|---|---|---|
| barcode generator API, EAN-13 generator API | API generateur code-barres, API code-barres EAN-13 | `/docs/barcodes/render` |
| SVG barcode API | API code-barres SVG | `/docs/barcodes/render` |
| protect barcode image metadata | protection metadonnees code-barres | `/docs/barcodes/render` |

- Slugs as in section 6; localized metadata, sitemaps, and structured data (`TechArticle` for docs, `FAQPage` for playground/help).
- Wording guidelines: emphasize the payload-based pricing model, the signed asset URLs, and the unique metadata copyright injection option. Do not claim database search capability on this route (redirect to product price/identity facets instead).

## 8. Examples

### Single Render Request
```bash
curl -s -X POST -H "Authorization: Bearer pdapi_..." \
  -H "Content-Type: application/json" \
  -d '{"type":"code128","data":"NUDGER-12345","format":"png","metadata":{"copyright":"Copyright 2026"}}' \
  "https://api.product-data-api.com/api/v1/barcodes/render"
```

### Signed Asset Download
```bash
# Publicly accessible without Bearer token
curl -i "https://api.product-data-api.com/api/v1/barcodes/assets/eyJhbGci..."
```

## 9. Playground behavior

- **Configuration Controls**:
  - Symbology select list (EAN, Code128, QR, etc.).
  - Text input for barcode payload.
  - Expose a "Metadata Settings" collapsible panel with fields for Copyright, Author, and Description.
  - Image size inputs (width, height, DPI).
  - Format toggle (SVG vs. PNG).
- **Preview Panel**:
  - Shows the generated barcode image dynamically using the returned signed URL.
  - Displays expiration badge (e.g., "Expires in 30 days").
  - Copy action for the signed URL.
  - Action button to download the image file directly (with metadata intact).

## 10. Launch checklist

- [x] Barcode4J and ZXing dependencies declared and isolated
- [x] Backend implementation of controller endpoints and storage engine complete
- [x] PNG metadata text chunk writing logic tested and verified
- [x] SVG XML metadata parser/injector tests passing
- [x] `b2b-catalog.yml` updated with `barcode.render` catalog entry
- [x] No-data-no-pay behavior verified via integration tests (400, 402, 500 cases consumption = 0)
- [x] Docs pages written in English and French
- [x] Playground component supports the layout settings and metadata inputs
- [x] SEO sitemaps and localized pages configured

## 11. Open questions

- **Metadata sanitization**: Metadata strings are limited to 256 characters and stripped of control characters. Standard XML entity escaping is used for SVG metadata block rendering to avoid injection.
