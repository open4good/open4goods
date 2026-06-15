---
title: "Offres Commerciales"
description: "Le pricing Infera part des valeurs du référentiel modèles chargé depuis les galeries LocalAI :"
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
doc_url: /docs/apps/frontend/content/fr/docs/offres-commerciales
doc_path: apps/frontend/content/fr/docs/offres-commerciales.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
## Modèle de pricing par tier

Le pricing Infera part des valeurs du référentiel modèles chargé depuis les galeries LocalAI :

- `pricePaid` : baseline de coût interne
- `priceSell` : baseline de revente

Règles business :

1. tier **Public** en baseline (`×1`)
2. tier **Trusted** avec `pricePaid × trustedRatio`
3. tier **HDS** avec `pricePaid × hdsRatio`

`trustedRatio` et `hdsRatio` sont volontairement configurables côté backend et doivent être visibles via l'API backend pour la gouvernance pricing.

## Conçu pour l'extension des tiers

Les tiers actuels (`PUBLIC`, `TRUSTED`, `HDS`) ne sont pas finaux. Le produit et les contrats doivent rester compatibles avec des variantes futures :

- `HDS_FAST`
- `PUBLIC_SLOW`
- variantes par zone géographique, profil de latence ou profil de conformité

## Roadmap des options commerciales

### 1) Safe inference fallback

Mode optionnel où les requêtes basculent vers des serveurs opérés par Infera si la capacité décentralisée ne peut pas délivrer.

**Valeur business :** amélioration des garanties de delivery pour les SLA entreprise.

**Point de vigilance commercial :** l'usage fallback doit être mesuré et transparent dans le contrat.

### 2) Nœuds self-hosted on promise (mock frontend)

Proposition entreprise où les clients opèrent des nœuds sur leur propre infrastructure.

- monétiser les surplus GPU/CPU,
- prioriser les appels internes,
- burst vers la grille fédérée en cas d'échec de capacité interne.

### 3) Réseau fédéré entreprise

Permettre aux grandes organisations de mutualiser les ressources de calcul entre :

- entités locales,
- opérations territoriales,
- agents partenaires.

## Checkpoints de challenge business

- Garder la logique de multiplicateurs transparente pour éviter une perception de “sur-prix opaque”.
- Séparer le pricing grid de base des options de garantie pour simplifier l'upsell et les achats.
- Clarifier la responsabilité SLA entre les chemins fallback et fédérés.
