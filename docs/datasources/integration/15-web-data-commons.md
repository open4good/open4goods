---
title: "Plan d'intégration : Web Data Commons (schema.org Product)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : Web Data Commons (schema.org Product)

> Socle commun : [README](README.md). Fiche source :
> section 4.14 de l'inventaire interne des sources candidates (non publié).
> Mesures du 2026-11-08 sur la release 2024-12 (crawl d'octobre 2024) : fichier de
> statistiques par domaine complet (3 309 209 domaines), table de correspondance complète,
> et un fichier de quads complet (`part_1316.gz`, 135 Mo).

## 0. Fiche

**Verdict : non recommandé en l'état**, pour quatre raisons :

- **Données anciennes.** Aucune release n'a suivi le crawl d'octobre 2024, publié le
  2025-01-10 : les données ont deux ans.
- **Aucun raccourci possible.** Il faut lire les 243 Go. Les domaines européens qui portent des
  GTIN sont répartis sur 1 628 des 1 658 fichiers ; aucune sélection de fichiers ne réduit le
  coût.
- **Grands marchands français presque absents.** On trouve darty.com avec 1 entité,
  cdiscount.com avec 40, fnac.com avec 921, dont aucune n'a de GTIN.
- **Rien de fiable à projeter sur une fiche produit.** Les titres et les marques arrivent déjà
  par nos flux ; les prix et offres ont deux ans ; 12 % des GTIN déclarés sont faux. Ce qui
  reste utile (liste de marchands, jeu d'évaluation de l'appariement) sert en interne.

Le plan minimal a deux volets. Le volet A (S) exploite le fichier de statistiques par domaine
pour découvrir des marchands, sans traiter un seul quad. Le volet B (L), hors JVM, filtre les
quads contre nos GTIN et charge le résultat dans `ref-web-data-commons`, sans projection.

| Champ | Valeur |
|---|---|
| Identifiant | `web-data-commons` |
| Mode | R+ hors ligne : job externe à l'api, import d'un fichier préfiltré, projection désactivée, **`CREATE` interdit** |
| Priorité | non recommandé en l'état ; volet A (découverte de marchands) P3 |
| Effort | volet A : S (1 jour). Volet B : L (job 3 à 4 jours, exécution environ 1 jour machine, import 1 à 2 jours) |
| Verticales | toutes, en masse |
| Volume | 570 780 409 entités Product (non dédoublonnées), dont 49,6 à 51,9 M avec une propriété GTIN déclarée (8,7 à 9,1 %) |
| Dépend de | P0-5 (import `ref-`), propositions S3 (projection `NONE`) et S4 (import d'un fichier produit hors api), section 10 |

## 1. Apport

Web Data Commons (WDC, université de Mannheim) extrait le balisage Microdata et JSON-LD du
Common Crawl. Le sous-ensemble Product contient, pour chaque page, l'entité `schema.org/Product`
et ce qui l'accompagne (offre, prix, marque, identifiants).

Usages possibles :

1. **Découverte de marchands hors affiliation** : domaines qui publient des fiches produit à GTIN,
   à croiser avec nos flux pour la prospection (volet A).
2. **Enrichissement des noms et marques** : titres alternatifs pour des GTIN mal nommés,
   vote de marque sur plusieurs sites.
3. **Jeu d'entraînement ou d'évaluation pour l'appariement** : couples (GTIN, titre) venant de
   sites différents, utiles pour mesurer `ReferenceMatcher` ou un futur matcher marque et modèle.
4. **Contrôle qualité** : un même GTIN publié avec des marques différentes signale une erreur
   dans nos données ou dans un flux.

Ce que la source n'apporte pas : des prix actuels (relevés d'octobre 2024), des offres
achetables ou des images exploitables.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Dernière release | crawl d'octobre 2024, annoncée le 2025-01-10. Aucune release 2025 trouvée : `/structureddata/2025-12/stats/schema_org_subsets.html` renvoie la page d'accueil générique | vérifié |
| Sous-ensemble Product | 21 539 828 659 quads, 279 715 051 URL, 3 309 209 hôtes ; 242,98 Go en 1 658 fichiers `part_N.gz` de 124 à 146 Mo | vérifié |
| Emplacement | `https://data.dws.informatik.uni-mannheim.de/structureddata/2024-12/quads/classspecific/Product/` (fichiers datés du 2025-10-22) | vérifié |
| Format | N-Quads. Le 4e élément est l'URL de la page. Microdata et JSON-LD fusionnés, **non dédoublonnés** ; vocabulaires `http://schema.org/` et `https://schema.org/` mélangés | vérifié |
| Statistiques par domaine | `Product_domain_stats.csv`, 444 992 134 octets : domaine, quads, entités, densité de chaque propriété | vérifié |
| Correspondance domaine -> fichier | `Product_lookup.csv`, 109 410 938 octets (`pld,tld,file_lookup`) | vérifié |
| Échantillon | `Product_sample.txt`, 196 Ko | vérifié |
| Fréquence | annuelle de 2012 à 2024, rien depuis | vérifié (historique des annonces) |
| Corpus dérivés | Schema.org Table Corpus 2023 (crawl d'octobre 2023) : environ 2,67 M tables Product, environ 288 M lignes après nettoyage, en JSON, **une table par hôte** (taille non vérifiée). WDC Product Data Corpus v2 (2019) : 26 M offres de 79 000 sites, regroupées en 16 M clusters par identifiant. WDC Products (2022) : 11 715 offres pour 2 162 produits. billiger.de Products (2026-07-20) : 13 730 offres en allemand | vérifié (pages des projets) |
| Authentification | aucune | vérifié |
| Licence | code d'extraction sous Apache ; contenu sous la responsabilité des sites (CGU Common Crawl) | champ d'attribution seulement |

Sous-ensembles déjà filtrés sur les GTIN : il n'en existe pas pour la release 2024. Les seuls
corpus filtrés par identifiant (`gtin*`, `mpn`) sont le Product Data Corpus v2 (2019) et sa
version nettoyée PDC2020-C, issus de crawls anciens. Les corpus d'appariement (LSPM v2, WDC
Products, billiger.de) sont en anglais et en allemand ; ils servent à évaluer une méthode, pas à
enrichir nos fiches.

Le Table Corpus 2023 offre une voie moins coûteuse pour une liste fermée de sites : on
télécharge seulement les tables JSON des hôtes retenus, sans parser de N-Quads. Il a un an de
plus que la release 2024.

## 3. Identifiants et appariement

**Mesure 1 : statistiques par domaine**, sur les 3 309 209 domaines. Les densités sont pondérées
par le nombre d'entités ; un GTIN y est seulement déclaré, pas encore validé.

| Mesure | Valeur |
|---|---|
| Entités Product | 570 780 409 |
| `name` / `offers` / `url` / `brand` | 97,0 % / 86,4 % / 52,2 % / 44,8 % |
| Propriété GTIN (`gtin`, `gtin8`, `gtin12`, `gtin13`, `gtin14`, `ean`) | 49,6 à 51,9 M entités (8,7 à 9,1 %), sur 238 361 domaines |
| Dont `gtin13` / `gtin` / `gtin12` / `gtin8` | 26,2 M / 16,1 M / 4,8 M / 4,4 M |
| Domaines `.fr` | 66 258 domaines, 14,1 M entités, environ 2,08 M avec GTIN, 6 320 domaines à GTIN |
| Domaines à GTIN en `.fr`, `.be`, `.ch`, `.lu`, `.eu` | 14 287 domaines, environ 3,77 M entités, **répartis sur 1 628 fichiers sur 1 658** |

Couverture des marchands français (entités dans le crawl, densité GTIN) :

| Domaine | Entités | GTIN | Domaine | Entités | GTIN |
|---|---|---|---|---|---|
| auchan.fr | 136 146 | 0 % | ldlc.com | 2 274 | 79 % (`gtin13`) |
| carrefour.fr | 27 330 | 60 % | materiel.net | 2 239 | 0 % (MPN 99 %) |
| conforama.fr | 11 892 | 0 % | castorama.fr | 1 596 | 100 % |
| boulanger.com | 8 940 | 88 % | rueducommerce.fr | 1 391 | 97 % |
| pixmania.com | 5 429 | 89 % | electrodepot.fr | 1 133 | 0 % |
| cobra.fr | 5 150 | 100 % | but.fr | 1 121 | 100 % |
| backmarket.fr | 2 925 | 0 % | fnac.com | 921 | 0 % |
| cdiscount.com | 40 | 0 % | darty.com | 1 | 0 % |

Les premiers domaines `.fr` par nombre d'entités à GTIN sont surtout du bruit :
modalova.fr (73 208), optical-center.fr (48 757), puis des sites de serrurerie, de balnéo ou de
radars (`serruriersargenteuil.fr`, `forfait-balneo.fr`, `radar-toulouse.fr`). La propriété GTIN
y est remplie avec autre chose qu'un GTIN : le contrôle de somme est indispensable.

**Mesure 2 : un fichier complet**, `part_1316.gz` (135 Mo, 12 554 600 quads), choisi parce qu'il
contient boulanger.com :

| Mesure | Toutes les entités du fichier | boulanger.com |
|---|---|---|
| Entités Product | 347 954 | 8 940 |
| `name` | 99,6 % | 100 % |
| `brand` | 51,5 % | 100 % |
| `offers` | 85,8 % | 100 % |
| Prix (`price` ou `offers.price`) | 87,8 % | 100 % |
| Propriété `url` | 50,0 % | 100 % |
| GTIN déclaré | 10,3 % | 88,1 % |
| **GTIN valide** (somme de contrôle) | **9,1 %** (88 % des déclarés) | **88,0 %** |
| GTIN valide + `name` + `brand` | 6,7 % | 88,0 % |
| GTIN valides distincts | 27 718 (pour 31 686 entités) | 7 802 |

Règles :

1. Valeurs lues dans `gtin`, `gtin8`, `gtin12`, `gtin13`, `gtin14` et `ean`, puis réduites aux
   chiffres et validées par la somme de contrôle, avant normalisation à 13 chiffres. `productID`
   n'est retenu que s'il contient un GTIN valide (338 entités de plus seulement dans le fichier
   mesuré).
2. Appariement `GTIN_ONLY` contre notre ensemble de GTIN.
3. URL de page sans paramètres de requête : boulanger.com est souvent crawlé via des liens
   d'affiliation (`?at_medium=affiliate&...&eff_cpt=...`).
4. Domaine = hôte sans `www.`, rattaché au domaine enregistrable (le `pld` de WDC).

Recouvrement avec nos produits : **non mesurable** sans accès à l'Elasticsearch de prod. Le
volet B le mesure de fait : c'est le nombre de nos GTIN trouvés dans le corpus.

## 4. Modèle cible

- Aucune projection sur `Product`, aucun attribut, aucun champ nouveau.
- `ref-web-data-commons` : un document par GTIN, avec `gtin`, `observations` (au plus 20 domaines
  par GTIN : domaine, URL de page, nom, marque, prix, devise, release) et des agrégats (nombre de
  domaines distincts, marque la plus fréquente, candidats de nom).
- Volet A : un simple fichier CSV (domaine, entités, densité GTIN, densité marque, présence dans
  nos flux), sans index.

## 5. Pipeline

**Volet A, découverte de marchands (S).**

1. Script hors api (à créer, par exemple `scripts/analysis/wdc_merchants.py`) qui lit
   `Product_domain_stats.csv` (424 Mo).
2. Filtre : densité GTIN au moins 0,5 et au moins 500 entités. Pour le pays, le TLD `fr` ne
   suffit pas : boulanger.com et ldlc.com sont en `.com`, et WDC ne donne pas la langue. Il faut
   compléter par une liste de domaines `.com` français connus, ou par un échantillon de pages.
3. Croisement avec nos sources marchandes (noms de datasources des flux d'affiliation) et sortie
   d'une liste de prospects.

**Volet B, filtrage complet (L).**

1. **Ensemble de GTIN** : le fichier GTIN d'`OpenDataService` suffit, sans nouvel endpoint. Il
   parcourt `ProductRepository.exportAll(String[] includeFields)` et écrit tous les produits de
   type `GTIN_8`, `GTIN_12`, `GTIN_13` ou `GTIN_14`, avec ou sans offre (requête
   `Query.findAll()`, aucun filtre sur `offersCount` dans `DatasetWriter.write` ni
   `toGtinEntry`).
2. **Job** (à créer, Python, hors api et hors JVM), par exemple `scripts/wdc/filter_products.py` :
   - lit les 1 658 fichiers en flux, sans les stocker ;
   - regroupe les quads par page ;
   - reconstitue les entités Product avec leurs nœuds `brand` et `offers` ;
   - valide les GTIN et garde ceux de l'ensemble ;
   - écrit `wdc-2024-12-matches.jsonl.gz` (gtin, nom, marque, domaine, URL sans requête, prix,
     devise).

   Le prototype écrit pour les mesures de ce plan (une centaine de lignes de Python) fait déjà
   l'essentiel : 51 s par fichier, 25 Mo de mémoire.
3. **Import** : `WebDataCommonsProvider implements ReferenceDatasetProvider` (à créer) lit ce
   fichier ; `read(Path)` convient. Limite du socle : `resolveDownloadUrls()` suppose une URL
   publique. Ici, le fichier est produit hors ligne et déposé sur la machine (proposition S4 :
   provider "fichier local" ou `POST /reference-datasets/{id}/import` avec un chemin).
4. **Projection** : aucune (`NONE`, proposition S3). `CREATE` est interdit pour cette source : il
   créerait des millions de pages sans offre.
5. **Scores, export ouvert, front** : rien.

## 6. Planification et volumétrie

- Fréquence : une fois par release, soit au mieux une fois par an. Aucun cron dans l'api.
- Téléchargement : 243 Go, à lire en entier. À 50 Mo/s, environ 1 h 30 (débit du serveur WDC
  **non vérifié**).
- Calcul : 51 s par fichier, mesuré (Python mono-cœur, 12,5 M quads, 25 Mo de RAM). 1 658
  fichiers font environ 23,5 h de CPU, soit environ 3 h sur 8 processus. Spark est inutile.
- Stockage : lecture en flux, donc pas de 243 Go sur disque. Sortie estimée : 9 % de 570,8 M,
  soit environ 51 M entités à GTIN valide avant filtrage. Le volume après filtrage dépend de la
  taille de notre ensemble de GTIN, inconnue ici ; de l'ordre de quelques millions de lignes, 100
  à 500 Mo compressés (estimation **non vérifiée**).
- Machine : pas sur beta (disque saturé par le cache de ressources en juillet 2026, **non
  revérifié**). Une VM temporaire ou un poste de travail suffit, pour une journée.
- Coût de lancement du volet A : 424 Mo de téléchargement et quelques minutes de calcul. Le
  calcul complet sur ce fichier a été fait pour ce plan.

## 7. Sourcing transparent

- Usage interne seulement, par construction : faute de projection, rien n'atteint la fiche
  produit ni l'export.
- `open4goods-config/datasources/web-data-commons.yml` : `name: "Web Data Commons"`,
  `portalUrl: https://webdatacommons.org/structureddata/`, `license: "Apache 2.0 (extraction) ;
  contenu sous la responsabilité des sites"`, `attribution: "Web Data Commons, données schema.org
  extraites du Common Crawl d'octobre 2024"`.
- Chaque observation de `ref-web-data-commons` garde l'URL de sa page et la release, donc la date
  du crawl.
- Pas de `SourceAttribution` sur `Product`, faute de projection. Aucune URL de la source ne
  contient "openfoodfacts".

## 8. Tests et critères d'acceptation

- Test unitaire du job sur une fixture N-Quads tirée de `Product_sample.txt` et de
  `part_1316.gz` :
  - `gtin13` valide ;
  - GTIN avec espaces ou tirets ;
  - somme fausse ;
  - `productID` de la forme `ean:...` ;
  - vocabulaires `http` et `https` ;
  - `brand` en littéral et en nœud ;
  - prix dans `offers` ;
  - paramètres de suivi dans l'URL.
- Reproduction de la mesure : sur `part_1316.gz`, 31 686 entités à GTIN valide et 7 802 GTIN
  distincts pour boulanger.com.
- Test du provider : import du fichier de sortie, puis bascule d'alias.
- Critère du volet A : la liste contient les marchands déjà connus de nos flux (contrôle de
  rappel), et au moins 50 domaines `.fr` hors affiliation, vérifiés à la main sur un échantillon
  de 20.

## 9. Découpage

1. Volet A : script de découverte de marchands et liste de prospects (S).
2. Point de décision du propriétaire (Q1, Q2).
3. Volet B : job de filtrage et ses tests (M).
4. Exécution sur les 1 658 fichiers (S, environ une journée machine).
5. Proposition S4, import d'un fichier local, et `WebDataCommonsProvider` (S).
6. Usage aval, par exemple le jeu d'évaluation de `ReferenceMatcher` (M, hors périmètre de ce
   plan).

## 10. Risques, challenges, questions

- **Fraîcheur.** Deux ans sans release. Si WDC n'en publie plus, il faudrait lancer nous-mêmes
  l'extraction sur un Common Crawl récent (framework Apache) : plusieurs To à lire, hors de
  proportion.
- **Couverture biaisée.** Le Common Crawl ne prend qu'une fraction de chaque site ("the data is
  only the tip of the iceberg", page des sous-ensembles). Les grands marchands français sont
  quasi absents, et les sites les mieux représentés ne sont pas ceux qui nous intéressent.
- **Bruit.** 12 % des GTIN déclarés sont faux dans le fichier mesuré. Des domaines entiers
  remplissent `gtin` avec autre chose, et les entités sont dupliquées entre Microdata et JSON-LD.
- **Alternative.** Pour quelques marchands précis, le Table Corpus 2023 (une table JSON par hôte)
  ou un crawl ciblé de leurs fiches coûte moins cher que la lecture des 243 Go.

Propositions pour le socle (numérotation commune aux plans 07, 08 et 15) :

Les propositions de socle S1 à S5 des plans 07, 08 et 15 sont reprises dans le README, P0-10
(S1 : retrait des ressources ; S2 : attribution par image ; S3 : liste blanche de projection ;
S4 : import d'un fichier produit hors api ; S5 : provider multi-fichiers).

- S3 : liste blanche de projection par source, dont la valeur `NONE`, pour une source chargée en
  référentiel sans aucune projection.
- S4 : import d'un fichier produit hors api (chemin local ou dépôt manuel), pour les jobs hors
  ligne.

Questions au propriétaire :

1. La découverte de marchands (volet A) répond-elle à un besoin de prospection en affiliation ?
   Si oui, on fait le volet A seul, en S.
2. Un jeu d'évaluation de l'appariement est-il un besoin réel à court terme (matcher marque et
   modèle du socle) ? Sinon, on abandonne le volet B.
3. Des données d'octobre 2024 sont-elles acceptables pour enrichir des noms et des marques, ou
   faut-il attendre une nouvelle release ?
