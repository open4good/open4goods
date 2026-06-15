---
title: "Commercial Offers"
description: "Infera pricing starts from model referential values loaded from LocalAI galleries:"
tags:
  - documentation
  - vue-content
  - structural
  - frontend
owner: platform
audience: all
language: en
component: frontend
maturity: draft
security_classification: public
doc_url: /docs/apps/frontend/content/en/docs/commercial-offers
doc_path: apps/frontend/content/en/docs/commercial-offers.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
## Pricing model by tier

Infera pricing starts from model referential values loaded from LocalAI galleries:

- `pricePaid`: internal cost baseline
- `priceSell`: resale baseline

Business rules:

1. **Public** tier uses baseline (`×1`)
2. **Trusted** tier applies `pricePaid × trustedRatio`
3. **HDS** tier applies `pricePaid × hdsRatio`

`trustedRatio` and `hdsRatio` are intentionally configurable from backend policy and must be visible in backend API for pricing governance.

## Designed for tier scaling

Current tiers (`PUBLIC`, `TRUSTED`, `HDS`) are not final. Product and contracts should remain forward-compatible with future variants such as:

- `HDS_FAST`
- `PUBLIC_SLOW`
- Tier variants by geography, latency profile, or compliance profile

## Commercial options roadmap

### 1) Safe inference fallback

Optional mode where requests fall back to Infera-operated servers if decentralized capacity cannot deliver.

**Business value:** better delivery guarantees for enterprise SLAs.

**Commercial caveat:** fallback usage must be metered and contractually transparent.

### 2) On-promise self-hosted nodes (frontend-mocked)

Enterprise proposition where customers run nodes on their own infrastructure.

- monetize idle GPU/CPU capacity,
- prioritize internal calls,
- burst to the federated grid if internal capacity fails.

### 3) Federated enterprise network

Allow large organizations to share compute resources between:

- local entities,
- territorial operations,
- partnered agents.

## Business challenge checkpoints

- Keep multiplier logic transparent to avoid “opaque pricing premium” perception.
- Separate core grid pricing from guarantee options to simplify upsell and procurement.
- Clarify SLA responsibility across fallback and federated paths.
