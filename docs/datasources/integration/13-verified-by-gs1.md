---
title: "Plan d'intégration : Verified by GS1 (contrôle interne des GTIN)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : Verified by GS1 (contrôle interne des GTIN)

> Socle commun : [README](README.md). Fiche source :
> section 4.16 de l'inventaire interne des sources candidates (non publié).
> Recherches du 2026-11-08 : articles du support GS1 (support.gs1.org) et pages de GS1 UK via
> moteur de recherche. Les pages gs1.org "Getting started", les conditions d'utilisation (PDF)
> et la page GTIN Check de GS1 UK renvoient HTTP 403 à un client non navigateur : leur contenu
> n'a pas été relu aujourd'hui. Aucune mesure sur données réelles (pas de compte). Lecture du
> code de `main` (c6ed39707).

## 0. Fiche

**Verdict : non recommandé, parce que** l'accès par API passe par l'adhésion à une organisation
GS1 (GS1 France pour nous, conditions et coût **non vérifiés**), que l'accès public est limité à
30 requêtes par jour, et que les résultats sont réservés à un usage interne : rien n'est
projetable sur la fiche produit ni dans l'export ouvert. Une alternative gratuite, fondée sur nos
propres GTIN et sur les champs `GtinInfo` déjà calculés, couvre l'essentiel du besoin de
contrôle (section 5.1). Le plan minimal ci-dessous reste valable si l'adhésion existe ou est
décidée.

| Champ | Valeur |
|---|---|
| Identifiant | `gs1-verified` |
| Mode | C (contrôle) : appel par GTIN, résultat stocké hors de `Product`, aucune projection publique |
| Priorité | non recommandé ; l'alternative interne (section 5.1) est P3 |
| Effort | S pour l'alternative interne ; S pour le client API si l'accès existe |
| Verticales | toutes, en commençant par les 7 verticales |
| Volume | 30 requêtes/jour en accès public ; quota de l'API membre **non vérifié** |
| Dépend de | adhésion GS1 France (pour l'API) ; aucun prérequis P0 (pas de projection, pas d'attribution publique) |

## 1. Apport

Verified by GS1 répond à la question "Is this the product that I think it is?" (support GS1,
article modifié le 30 août 2024). Pour un GTIN, le service indique :

- si le GTIN est valide et bien structuré ;
- s'il est concédé (licencié) à une partie connue de GS1, et à qui ;
- s'il existe dans le GS1 Registry Platform ;
- lesquels des 7 attributs Verified by GS1 sont disponibles. La liste des 7 (marque,
  description, image, catégorie GPC, contenu net, marché cible, GTIN) est **non vérifiée** sur
  la page GS1.

Usage visé chez nudger, strictement interne :

- détecter les **GTIN erronés** : codes non concédés, inventés, ou réutilisés par un
  distributeur ;
- détecter les **marques mal résolues** : marque du produit (`BrandService.resolve`) différente
  du titulaire de la licence du GTIN ;
- auditer la qualité des fusions produit par GTIN, base de tout le modèle.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Recherche publique | sur gs1.org, essai gratuit "limited to 30 queries per day" | vérifié (support GS1) |
| API | "more advanced querying capabilities like an API connection", réservées aux membres, via leur organisation GS1 | vérifié (support GS1) |
| GS1 France | conditions, tarif et quota de l'API **non vérifiés** ; aucune page trouvée. Adhésion actuelle d'open4goods **non vérifiée** | à demander |
| GS1 UK, "GTIN Check API" | réservée aux membres de GS1 UK munis d'une clé ; les conditions mentionnent aussi les membres d'autres organisations GS1 et des non-membres titulaires d'une clé d'accès de 20 caractères ; tarif non publié | **non vérifié** (extraits de recherche, pages en 403) |
| GEPIR | remplacé par Verified by GS1 fin décembre 2023 | **non vérifié** (source secondaire) |
| Conditions d'utilisation | "All Query Results are for your internal use only and you may not sell, lease, license, publish, publicise, or otherwise distribute the Query Results." (PDF "Last update: June 2023", cité par le document candidat le 2026-11-08 ; 403 aujourd'hui) | vérifié par le document candidat |
| Licence | conditions d'utilisation Verified by GS1 (champ d'attribution seulement) | - |

L'automatisation de la recherche publique (30 requêtes par jour par un robot) n'est pas
proposée : les conditions n'ont pas pu être relues sur ce point.

## 3. Identifiants et appariement

- GTIN natif : le contrôle porte sur le GTIN du produit, sans appariement.
- Normalisation : `BarcodeValidationService` et `GtinInfo.normalizedGtin14`.
- À exclure du contrôle : les ISBN (`Gs1Class.ISBN_BOOKLAND` ; 77 % d'un échantillon aléatoire
  de produits occasion d'après une mesure interne), et les codes `RESTRICTED_INTERNAL` et
  `COUPON`, qui ne sont pas des GTIN mondiaux (section 5.1).
- Comparaison de marque : titulaire de la licence (raison sociale) contre marque nudger. Il faut
  passer par la correspondance marque vers société (`BrandService`, modèle `Company` de
  `services/brand`) : une raison sociale ("BSH Hausgeräte GmbH") couvre plusieurs marques
  (Bosch, Siemens, Neff).

## 4. Modèle cible

- Aucun attribut, aucun champ public sur `Product`, rien dans `OpenDataService` ni dans les DTO
  de front-api.
- Résultats de l'API (si accès) : index interne `gs1-checks` **(à créer)**, une entrée par GTIN :
  `gtin`, `checkedAt`, `valid`, `licensed`, `licenseeName`, `attributesAvailable`, `verdict`
  (`OK`, `NOT_LICENSED`, `BRAND_MISMATCH`, `UNKNOWN`). Le garder hors de `products-moustik`
  écarte tout risque de fuite par front-api ou par l'export.
- Résultats de l'alternative interne : un rapport, pas d'index (section 5.1).

## 5. Pipeline

### 5.1 Alternative gratuite, recommandée

Ce que nous avons déjà, calculé par `IdentityAggregationService` dans `GtinInfo` :

- `gs1Prefix` : les **3 premiers chiffres** du GTIN normalisé, soit le préfixe de l'organisation
  GS1 qui a attribué le code (pays d'adhésion, pas l'usine ni la société) ;
- `country` : pays de cette organisation (`Gs1PrefixService.detectCountry`) ;
- `gs1Class` : `GTIN`, `ISBN_BOOKLAND`, `ISMN_MUSIC`, `ISSN_PERIODICAL`, `RESTRICTED_INTERNAL`,
  `COUPON`, `UNKNOWN`, déduit par `BarcodeForensicsService` ;
- `packagingIndicator`, `upcType`.

Le préfixe à 3 chiffres ne dit rien de la société. Contrôles proposés, sans appel externe :

1. **Codes à circulation restreinte** : un produit dont le GTIN est `RESTRICTED_INTERNAL`
   (codes internes de magasin) ou `COUPON` peut fusionner des articles différents de plusieurs
   marchands. Compter, puis exclure ces codes de la création de produits ou les signaler.
2. **Préfixe société déduit de nos données** (à créer, `GtinConsistencyService` ou script) :
   pour chaque produit des verticales, prendre les 7 premiers chiffres du GTIN-13 comme
   approximation du préfixe société (sa longueur réelle varie de 6 à 12 chiffres et n'est pas
   connue sans GS1). Pour chaque préfixe vu sur au moins 20 produits, calculer la marque
   majoritaire, regroupée par société. Signaler les produits dont la marque diffère de la marque
   majoritaire d'un préfixe à plus de 90 % homogène.
3. **Rapport** hebdomadaire : suspects par verticale, par marque et par datasource d'origine. Il
   sert à corriger les alias de marque et à repérer les flux qui publient de faux GTIN.

Mesures à faire lancer sur la prod (pas d'accès ES ici) :

```json
GET products-moustik/_search
{ "size": 0,
  "aggs": {
    "classes": { "terms": { "field": "gtinInfos.gs1Class", "missing": "(absent)" } },
    "verticales": { "filter": { "exists": { "field": "vertical" } },
                    "aggs": { "classes": { "terms": { "field": "gtinInfos.gs1Class", "missing": "(absent)" } } } } } }
```

Le contrôle 2 a besoin des GTIN complets : export des produits des verticales (`id`, `vertical`,
`attributes.referentielAttributes.BRAND`, `gtinInfos`), puis calcul hors ligne.
`ProductRepository` offre `exportAll(String vertical)` (produits complets, par verticale) et
`exportAll(String[] includeFields)` (champs choisis, tous les produits) : soit une méthode
combinée (à créer), soit `exportAll(String vertical)` verticale par verticale.

### 5.2 Plan minimal avec l'API, si l'adhésion existe

1. **Client** (à créer) : `Gs1VerifiedClient` dans un petit module `services/gs1-verified`, avec
   les propriétés `gs1-verified.*` (`enabled`, `api-url`, `api-key`, `daily-budget`). Point
   d'accès et format de réponse **non vérifiés** (documentation réservée aux membres).
2. **Service de contrôle** (à créer) : `Gs1VerifiedControlService extends
   AbstractCompletionService`. `processProduct` appelle l'API, écrit dans `gs1-checks`, horodate
   `Product.datasourceCodes["gs1-verified"]` pour `shouldProcess`, et **n'appelle pas**
   `aggregator.onDatafragment` : rien n'entre dans le produit.
3. **Sélection** : d'abord les suspects du contrôle 5.1, puis les produits des verticales avec
   offres (`exportVerticalWithValidDate`), dans la limite du budget.
4. **Exploitation** : rapport interne ; correction manuelle des alias de marque ; éventuellement
   cause d'exclusion interne (voir section 10, question 2).

### 5.3 Audit manuel sans développement

Avec l'accès public (30 requêtes par jour), un humain peut vérifier chaque jour 30 suspects du
contrôle 5.1 sur gs1.org. En une semaine, 210 vérifications donnent la précision du détecteur
interne. C'est le moyen le moins cher de savoir si l'API vaudrait une adhésion.

## 6. Planification et volumétrie

| Voie | Débit | Couverture |
|---|---|---|
| Recherche publique, à la main | 30 GTIN/jour | audit par échantillon |
| API membre | quota **non vérifié** | inconnue |
| Alternative interne | tous les produits des verticales, chaque semaine | complète, mais sans vérité terrain |

Les volumes des verticales hors smartphones (48 759 produits) sont **non mesurés** ici.

## 7. Sourcing transparent

- Aucune mention publique : les résultats sont à usage interne.
- Dans le rapport interne : "Verified by GS1, interrogé le <date>" pour chaque vérification, et
  la méthode (préfixe à 7 chiffres, seuils) pour le détecteur interne.
- `open4goods-config/datasources/gs1-verified.yml` n'est utile que si la voie API est ouverte, et
  ne porte alors que `name` et `portalUrl`.

## 8. Tests et critères d'acceptation

- Tests unitaires du détecteur interne : préfixe homogène, préfixe partagé par deux marques d'une
  même société (pas d'alerte), préfixe de marque de distributeur (pas d'alerte au-dessous du
  seuil d'homogénéité), ISBN ignorés.
- Test du service de contrôle (voie API) : aucun champ de `Product` modifié en dehors de
  `datasourceCodes`, aucune entrée dans un `DataFragment`.
- Critères :
  - rapport hebdomadaire produit, suspects classés par verticale et par datasource ;
  - précision du détecteur mesurée par l'audit manuel (5.3) avant toute décision d'adhésion.

## 9. Découpage

1. Mesure `gs1Class` sur la prod (XS, Ops avec `agents_readonly`).
2. Détecteur de préfixe société et rapport, en script d'abord (S).
3. Audit manuel d'une semaine (XS par jour, humain).
4. Décision d'adhésion GS1 France selon la précision mesurée (propriétaire).
5. Si oui : client, service de contrôle, index `gs1-checks` (S).

## 10. Risques, challenges, questions

- **Le vrai besoin est la qualité des fusions par GTIN**, pas GS1. L'alternative interne y répond
  pour une part, sans coût ni contrainte d'usage.
- **Usage interne seulement** : même un signal interne (exclure un produit suspect de
  l'ImpactScore, par exemple) se voit indirectement sur le site. Si le contrôle sert à exclure
  des produits, c'est une décision à prendre consciemment (question 2).
- **Préfixe à 7 chiffres approximatif** : les préfixes société courts (grandes marques) ou longs
  (petites entreprises) produiront des faux positifs et des faux négatifs. Les seuils (20
  produits, 90 % d'homogénéité) sont à calibrer sur l'audit manuel.
- **Marques de distributeur et reconditionneurs** : un distributeur peut apposer son propre GTIN
  sur un produit d'une autre marque. Ce n'est pas une erreur au sens GS1, mais c'en est une pour
  la fusion de nos produits.
- **Le socle ne prévoit pas de mode "contrôle"** : `AbstractCompletionService` suppose un
  enrichissement du produit. La voie API l'utilise sans appeler l'agrégateur. Si d'autres
  contrôles internes apparaissent (Web Data Commons en usage interne, plan 15), un petit
  contrat dédié "contrôle sans projection" serait plus propre.
- Questions au propriétaire :
  1. L'association est-elle adhérente de GS1 France (ou prête à l'être) ? Sans cela, la voie API
     est fermée.
  2. Un GTIN jugé suspect peut-il exclure un produit (ImpactScore, sitemap), ou le contrôle
     reste-t-il un rapport pour corriger les données à la main ?
  3. Veux-tu lancer l'audit manuel d'une semaine (30 vérifications par jour) avant toute autre
     décision ?
