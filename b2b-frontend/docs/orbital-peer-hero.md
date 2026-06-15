---
title: "Orbital peer hero"
description: "`components/landing/OrbitalPeerHero.vue` is the reusable landing animation for premium peer-to-peer visuals. It is intentionally data-driven: the visual topology is gener"
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
doc_url: /docs/apps/frontend/docs/orbital-peer-hero
doc_path: apps/frontend/docs/orbital-peer-hero.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
# Orbital peer hero

`components/landing/OrbitalPeerHero.vue` is the reusable landing animation for premium peer-to-peer visuals. It is intentionally data-driven: the visual topology is generated from a numeric seed derived from cluster metrics, so the same component can render stable marketing pages while reflecting live network activity when the backend allows it.

## Pages

- `/index-1`: aurora orbit split hero, closest to the darker CSS/SVG samples.
- `/index-2`: full-page canvas particle field for a higher-motion launch page.
- `/index-3`: light SaaS orbit variant for B2B pages that need a quieter control-plane feel.

## Configuration

The `config` prop controls the effect without changing component internals:

| Key | Effect |
|---|---|
| `density` | Base number of peer nodes before live metrics are applied. |
| `orbitCount` | Number of visible orbital paths. |
| `packetCount` | Base number of moving packets before token pressure is applied. |
| `motion` | Animation speed multiplier, automatically disabled for reduced motion. |
| `glow` | Light emission intensity for nodes and packets. |
| `visualScale` | Overall scale of the orbital field. |
| `linkOpacity` | Strength of peer-to-peer connection lines. |

## Metrics seed

Use `useLandingClusterMetrics()` for landing pages. It tries the real admin node stats endpoint and falls back to deterministic preview values for public visitors. The component derives:

- node count from `activeNodes`;
- packet count from `totalTokens`;
- stable placement seed from active nodes, total nodes, routed jobs, and total tokens;
- animation speed from the active-to-total node ratio.

## Theme integration

The component resolves the current Vuetify theme through `useThemePreference()`. The `light` variant pins itself to a light palette; other variants follow the active theme. Colors are defined as local CSS variables so future theme tokens can be mapped without rewriting animation logic.
