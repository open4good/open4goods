---
title: "Méthodologie de l’Impact Score"
description: "Une vue structurée des critères, pondérations et principes statistiques qui fondent l’Impact Score."
tags: ["impact-score", "methodologie", "scoring"]
icon: "mdi-flask-outline"
weight: 20
updatedAt: "2026-02-12"
draft: false
---

# Méthodologie de l’Impact Score

Ce document explique **comment chaque critère est noté puis combiné**.
Nous utilisons un mélange de données vérifiées, de pondérations assistées
par IA et de normalisation statistique pour garantir la cohérence.

## Processus de scoring

1. Collecte de sources structurées (fiches techniques, bases de certification).
2. Application des règles de notation par critère.
3. Pondération selon la pertinence de la catégorie.
4. Application d’une pénalité de qualité si l’information manque.

## Garde-fous statistiques

Nous utilisons le **sigma scoring** pour éviter d’avantager les valeurs
extrêmes. Quand les données sont trop peu nombreuses, nous utilisons un
fallback percentile pour garder un classement juste.

### Ce que cela signifie

Vous pouvez comparer les produits d’une même catégorie avec une échelle
cohérente, même si certains attributs sont incomplets.
