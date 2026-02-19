# Benchmark UX - Présentation des scores environnementaux (Nudger)

> Date : 2026-02-19  
> Périmètre : composant score en liste produits + fiche produit  
> Objectif : proposer une cible UX/UI conforme à la logique Impact Score (Nudger) et aux exigences de transparence.

## 1) Benchmark externe (3 à 5 références)

## 1.1 ADEME / Etiquetage environnemental textile (FR)

**Ce qui fonctionne**
- **Lisibilité** : échelle simple (A à E), forte reconnaissance visuelle, code couleur immédiatement interprétable.
- **Contexte** : présence d'une explication méthodologique côté institution (critères, périmètre, limites).
- **Action** : aide à la comparaison rapide de produits entre eux.

**Limites observées**
- Lecture parfois "label d'autorité" sans accès immédiat au détail du calcul depuis le même écran produit.
- Granularité limitée quand l'utilisateur veut comprendre pourquoi deux produits de même classe diffèrent.

**Pattern réutilisable pour Nudger**
- Utiliser un **code couleur discret mais explicite**, combiné à un **grade lisible** en un coup d'œil.

## 1.2 Back Market (indice réparabilité / reconditionné)

**Ce qui fonctionne**
- **Lisibilité** : score saillant près du prix et des attributs décisifs.
- **Contexte** : focus sur un indicateur concret (réparabilité) avec wording pédagogique.
- **Action** : pousse naturellement vers des alternatives mieux notées.

**Limites observées**
- Forte mise en avant du score principal, mais hétérogénéité possible de compréhension sur la méthode exacte selon les catégories.

**Pattern réutilisable pour Nudger**
- Placement du score dans la **zone de décision** (titre/prix/CTA), pas uniquement dans un onglet secondaire.

## 1.3 Amazon Climate Pledge Friendly

**Ce qui fonctionne**
- **Lisibilité** : badge visible et reconnaissable.
- **Contexte** : rattachement du badge à des certifications identifiées.
- **Action** : filtrage et repérage rapide des produits éligibles.

**Limites observées**
- Badge binaire (présent/absent) : peu d'information sur la performance relative fine.
- Exige un clic supplémentaire pour comprendre les référentiels et leur couverture.

**Pattern réutilisable pour Nudger**
- Ajouter un **niveau de preuve/source** directement associé au score (pas seulement un label visuel).

## 1.4 Comparez / Idealo (comparateurs)

**Ce qui fonctionne**
- **Lisibilité** : informations normalisées en listing, forte comparabilité entre cartes produit.
- **Contexte** : séparation claire entre critères principaux et détails techniques.
- **Action** : tri, filtres et comparaison côte à côte.

**Limites observées**
- L'information environnementale reste souvent secondaire par rapport au prix/livraison.

**Pattern réutilisable pour Nudger**
- Prévoir un **tri par impact score** et un affichage de score compact dans toutes les cartes listées.

## 1.5 EcoScore (agroalimentaire, apps/retail)

**Ce qui fonctionne**
- **Lisibilité** : lettres + couleurs (A/E) robustes cognitivement.
- **Contexte** : mention d'une base méthodologique (LCA, labels, etc.).
- **Action** : compréhension instantanée + possibilité d'approfondir.

**Limites observées**
- Risque de confusion entre différentes méthodologies si les sources ne sont pas explicitées clairement.

**Pattern réutilisable pour Nudger**
- Combiner un **résumé très court** (score/grade/couleur) avec un **panneau d'explication détaillé**.

---

## 2) Patterns UX/UI réutilisables pour Nudger

## 2.1 Code couleur

- Conserver la logique actuelle "faible / moyen / élevé" déjà implémentée par paliers (`low`, `mid`, `high`) pour l'accent visuel.
- Associer systématiquement la couleur à une **valeur numérique** et une **étiquette texte** (ex. "Bon", "Moyen", "À améliorer") pour l'accessibilité.
- Ne jamais dépendre de la couleur seule (WCAG).

## 2.2 Échelle

- L'échelle cible doit rester **0..20** (cohérente avec la méthodologie Nudger).
- Pour la lecture rapide, afficher :
  - valeur principale (ex. 14.2/20),
  - niveau textuel,
  - position relative (barre de progression).
- Pour le détail, expliciter les sous-scores normalisés (0..5) + pondérations.

## 2.3 Tooltip / aide contextuelle

- En liste : tooltip court = "ce score est calculé à partir de X critères pondérés" + lien "Voir la méthode".
- En fiche : panneau explicatif enrichi (méthode de normalisation, sens `impactBetterIs`, poids du critère, qualité des données).
- Afficher les métadonnées minimales de confiance : date de calcul, complétude des données, méthode de normalisation.

## 2.4 Positionnement

- **Liste produits** : score visible sur chaque carte, au même niveau visuel que prix / marque / modèle.
- **Fiche produit** :
  - bloc score principal au-dessus de la ligne de flottaison,
  - section "Pourquoi ce score ?" juste en dessous,
  - détail des sous-scores en cartes, triables par poids ou marge d'amélioration.

---

## 3) Version cible du composant score (liste + fiche)

## 3.1 Variante liste (compacte)

**Contenu minimal**
- Valeur `x/20`.
- Pastille niveau (`Bon`, `Moyen`, `Faible`).
- Icône info avec tooltip 1 ligne.
- Lien discret vers méthodologie.

**Comportement**
- Zone cliquable vers la fiche produit.
- Tooltip accessible clavier + lecteur d'écran.
- Affichage stable sur mobile (pas de saut de layout).

## 3.2 Variante fiche produit (détaillée)

**Contenu minimal**
- Score global + barre de progression.
- Min/max de population de comparaison (si dispo).
- Date de calcul.
- Bouton "Comprendre le calcul".

**Contenu explicatif**
- Sous-scores (0..5), pondérations, sens d'interprétation (`impactBetterIs` vs `userBetterIs`).
- Méthode statistique affichée de manière conditionnelle (SIGMA, PERCENTILE, MINMAX, etc.).
- Bloc "Sources" : origine des données par critère (API, fournisseur, mapping, valeur manquante).

## 3.3 Critères d'acceptation

1. **Compréhension instantanée (listing)** : 80% des utilisateurs test confirment comprendre quel produit est "meilleur impact" en < 5 secondes.
2. **Transparence minimale** : chaque score affiche au moins méthode, date de calcul, et lien vers la documentation.
3. **Traçabilité** : chaque sous-score expose ses données de base (valeur brute ou statut manquant), sa normalisation et sa contribution.
4. **Accessibilité** : score compréhensible sans couleur seule, labels ARIA présents, focus clavier fonctionnel.
5. **Cohérence cross-device** : rendu stable mobile/desktop, sans masquer l'information clé.
6. **Conformité Nudger** : l'UI n'affiche jamais une règle contraire à la logique backend (échelle, sens, pondération).

---

## 4) Validation de conformité avec la logique Nudger

## 4.1 Conformité calcul

La cible proposée est compatible avec les invariants méthodologiques documentés :
- score global agrégé par somme pondérée,
- sous-scores normalisés sur 0..5,
- score final stabilisé sur 0..20,
- inversion pilotée par `impactBetterIs`.

La version cible impose explicitement que ces éléments soient visibles côté UI pour éviter les interprétations trompeuses.

## 4.2 Conformité transparence des sources

La cible impose :
- un niveau "résumé" en listing,
- un niveau "audit" en fiche (méthode + stats + contribution + source),
- un fallback clair quand une donnée est manquante (neutralisation, qualité des données).

Cela évite l'effet "boîte noire" et aligne l'expérience avec un standard de preuve compréhensible.

## 4.3 Écarts potentiels à surveiller

- Sur-promesse en listing (trop de simplification sans lien vers preuve).
- Confusion entre score environnemental composite et labels/certifications tiers.
- Régression d'accessibilité si la couleur reste dominante sans texte associé.

## 4.4 Recommandation de mise en œuvre

1. Étendre le composant mutualisé de score pour gérer explicitement : `gradeLabel`, `confidence`, `calculationDate`, `methodTag`.
2. Normaliser un contrat de données "transparence" dans l'API front.
3. Ajouter des tests UI dédiés : affichage méthode, cas données manquantes, cohérence `impactBetterIs`.
4. Mesurer l'impact UX (CTR vers alternatives mieux notées, usage du tri impact, ouverture des tooltips).
