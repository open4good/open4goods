---
title: "Sources de données Impact Score"
description: "Inventaire des sources principales et des règles de validation utilisées dans les calculs Impact Score."
tags: ["impact-score", "data", "sources"]
icon: "mdi-database-search-outline"
weight: 30
updatedAt: "2026-02-12"
draft: false
---

# Sources de données Impact Score

L’Impact Score s’appuie sur **un ensemble de sources structurées**. Chaque
source est associée à des métadonnées de provenance et de fraîcheur pour garder
le score auditable.

## Sources typiques

- Fiches techniques fabricant.
- Bases publiques (réparabilité, recyclabilité).
- Organismes de certification et registres réglementaires.

## Règles de validation

- Les données doivent être **traçables** (lien ou identifiant).
- En cas de conflit, nous gardons la source **vérifiée** la plus récente.
- Les attributs manquants entraînent une **pénalité de qualité**.
