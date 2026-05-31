---
title: "Exemple de guide d'achat : téléviseur"
description: "Page exemple démontrant les composants embarqués des guides d'achat nudger (carte produit, parts de marché, top écolo)."
type: "guide"
tags: ["language:fr", "guide-achat", "tv"]
icon: "mdi-television"
weight: 60
updatedAt: "2026-05-31"
draft: false
published: true
navigation: true
---

# Exemple de guide d'achat : choisir son téléviseur

En ce mois de mai, Arthur de la Team Nudger doit remplacer son vieux téléviseur -
et il refuse de sacrifier la durabilité pour quelques pouces de plus. Voici sa
méthode, et les modèles qui passent notre filtre impact.

> **Page de démonstration.** Elle illustre le câblage des composants embarqués
> documentés dans `frontend/docs/markdown-mapping.md`. Les données affichées sont
> live (front-api), pas du contenu figé.

## Verdict express

Pour un usage polyvalent, sans se ruiner ni rogner sur l'impact :

<ProductCardEmbed gtin="8806092074061" size="medium" />

## Le marché en un coup d'œil

Quelques marques concentrent l'essentiel de l'offre référencée :

<BrandShareChart vertical="tv" type="pie" top="8" />

## Comment choisir : les critères qui comptent

Au-delà de la dalle et de la définition, Arthur regarde d'abord **la durabilité
et la réparabilité** - c'est ce que mesure le score d'impact nudger. Un téléviseur
qu'on garde dix ans bat un modèle " mieux noté " remplacé au bout de trois.

## Les modèles les plus durables

Notre top trié par score d'impact :

<GuideProductGrid vertical="tv" top="3" sort="ecoscore" />

## Notre sélection incarnée

Le choix d'Arthur, en lien interne : <ProductEmbed gtin="8806092074061" size="m" />.

## Sources & méthodologie

- Le score d'impact nudger agrège durabilité, réparabilité et données
  environnementales - méthodologie : `/docs/fr/impact-score/methodology`.
- _(Dans un vrai guide, lister ici chaque source web citée : titre, URL, date.)_
