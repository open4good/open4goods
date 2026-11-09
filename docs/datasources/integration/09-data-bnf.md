---
title: "Plan d'intégration : data.bnf.fr (livres, ISBN)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : data.bnf.fr (livres, ISBN)

> Socle commun : [README](README.md). Fiche source :
> section 4.12 de l'inventaire interne des sources candidates (non publié).
> Plan jumeau : [10-open-library.md](10-open-library.md) (même chaîne ISBN, section 5.1).
> Mesures du 2026-11-08 : point SPARQL data.bnf.fr (moisson complète des ISBN), SRU du
> catalogue général, dumps du 2026-05-28 (taille et début du fichier `editions`), et export
> ISBN public de nudger (`static.nudger.fr/opendata/isbn-open-data.zip`, 398 625 927 octets,
> fichier généré le 2026-06-04, lu en entier : 8 703 397 lignes). L'index en compte aujourd'hui
> 10 000 891 (`recordCount` de `/api/opendata/isbn`, compté en direct par
> `countItemsByBarcodeType`) : l'export a 5 mois de retard.
> Pas d'accès à l'Elasticsearch de prod : le recouvrement est mesuré sur cet export.

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | `data-bnf` |
| Mode | R+, mais acquisition par **moisson SPARQL paginée** plutôt que par dump (section 2.4), complétée par le SRU du catalogue général pour les ISBN récents |
| Priorité | en attente (Q2 du README, 2026-11-09). Au déblocage, à faire **avant** le plan 10, parce qu'il est moins cher et livre la chaîne ISBN commune |
| Effort | Scénario minimal : M, 5 à 7 jours, chaîne ISBN commune comprise (section 5.1), socle P0-3 à P0-5 supposé livré par le plan 01. Surcoût verticale livres : L, 10 à 15 jours partagés avec le plan 10 (section 5.8) |
| Périmètre | produits dont `gtinInfos.upcType = ISBN_13`, hors verticale : 10 000 891 ISBN dans l'index le 2026-11-08, 8 703 397 dans l'export du 2026-06-04 |
| Volume source | 4 703 410 valeurs `bnf-onto:isbn` sur 4 033 190 manifestations, plus 585 544 manifestations qui n'ont qu'un `bnf-onto:ean` ; 3 424 324 ISBN-13 distincts et valides |
| Recouvrement mesuré | 19,6 % des ISBN de l'export (50,1 % des ISBN à préfixe français, 51,9 % des français avec offre), soit environ 1,96 M produits sur l'index actuel par extrapolation |
| Dépend de | P0-3 (priorité), P0-4 (attribution), P0-5 (socle), extensions du socle proposées en section 10 (filtre par ensemble de clés, provider sans téléchargement) |

## 1. Apport

Aujourd'hui, une fiche livre de nudger ne contient que ce que disent les marchands (flux Rakuten
`BOOKS_*`, données Decitre dans la colonne `attributs`). Mesure sur l'export ISBN :

| Colonne de l'export | Remplie (toutes lignes) | Remplie (lignes avec offre) | Origine |
|---|---|---|---|
| `title` | 99,8 % | 99,9 % | `shortestOfferName()` |
| `editeur` | 35,9 % | 44,8 % | attribut marchand `EDITEUR`, souvent entre crochets (`[Maison de vie éditeur]`) |
| `format` | 35,7 % | 44,0 % | attribut marchand `FORMAT` (`Poche`, `Broché`) |
| `nb_page` | 22,6 % | 29,0 % | attribut marchand `NB DE PAGES` |
| `souscategorie` | **0,0 %** | 0,0 % | `SOUSCATEGORIE` figure dans les `exclusions` de `_default.yml` : la colonne est structurellement vide |
| `souscategorie2` | 0,7 % | 0,7 % | attribut marchand |
| auteur | absent | absent | aucune colonne ; aucun attribut auteur vu sur deux fiches livres inspectées |

data.bnf.fr apporte, pour les ISBN qu'il connaît (mesure sur 1 000 manifestations appariées,
section 3.4) :

- l'**auteur** (86 % des manifestations), absent aujourd'hui ;
- un **éditeur** propre (99,9 %), sans crochets ni raison sociale ;
- l'**année de publication** (99,8 %) et le **nombre de pages** (91,7 %, extrait de
  `dcterms:description`) ;
- la **langue** (99,8 %), les **sujets RAMEAU** (63,7 %, sous forme d'ARK) et un **résumé**
  (19,5 %) ;
- un identifiant pérenne, l'**ARK** BnF, qui sert de lien d'attribution et de pivot vers
  Wikidata et VIAF (alignements présents sur les entités auteur).

Ce que dit la mesure sur l'intérêt du scénario minimal :

- **62,2 % des lignes de l'export ont `offers_count > 0`** : enrichir les fiches existantes ne
  touche donc pas que des pages mortes. Ces fiches sont servies (vérifié : `nudger.fr/9782355991097`
  renvoie 200, avec URL canonique `/9782355991097-l-epreuve-du-feu`).
- La BnF couvre le dépôt légal français : 50,1 % de nos ISBN français (préfixes `978-2` et
  `979-10`, 32,7 % de l'export), mais 5 à 6 % seulement des autres. Elle **ne peut pas remplacer**
  les colonnes marchandes, contrairement à ce que suggère le document candidat (section 5,
  point 4) : elle les complète, et Open Library (plan 10) couvre le reste.

## 2. Accès

### 2.1 Point SPARQL (voie recommandée)

| Élément | Valeur | Statut |
|---|---|---|
| URL | `https://data.bnf.fr/sparql` (Virtuoso), GET ou POST, réponses JSON ou CSV | vérifié |
| Authentification | aucune | vérifié |
| Page de 10 000 lignes | acceptée ; 0,5 à 1,9 s par requête, y compris à l'offset 4 600 000 | vérifié |
| Moisson complète `SELECT ?m ?isbn` | 471 requêtes, 333 s, aucune requête de plus de 5 s ; 4 703 410 lignes, soit exactement le `COUNT` : pagination stable ce jour-là | vérifié |
| Moisson complète `SELECT ?m ?ean` | 218 requêtes, 149 s ; 2 178 473 lignes, soit le `COUNT` | vérifié |
| Détails par `VALUES ?m { ... }` | 200 manifestations par requête, 1,07 s en moyenne, 1,89 s au plus (5 requêtes) | vérifié |
| Quotas | aucun documenté. api.bnf.fr écrit : "Les services (hors sparql) et les données de data.bnf.fr sont accessibles 24h/24h 7j/7j" : le SPARQL n'a donc pas d'engagement de disponibilité | **non vérifié** |
| Fraîcheur | manifestations avec EAN : 76 851 pour `firstYear` 2024, 55 245 pour 2025, **900 pour 2026** ; `dcterms:modified` maximal des notices 2026 : 2026-02-14. Le graphe a environ 9 mois de retard | vérifié |

### 2.2 SRU du catalogue général (complément pour les ISBN récents)

| Élément | Valeur | Statut |
|---|---|---|
| URL | `https://catalogue.bnf.fr/api/SRU?version=1.2&operation=searchRetrieve&query=bib.isbn any "i1 i2 ..."` | vérifié |
| Lot | 25 ISBN par requête (`bib.isbn any`, ISBN-13 sans tiret accepté) ; `maximumRecords` jusqu'à 100 (exemple de la documentation) | vérifié |
| Temps de réponse | 0,13 à 0,15 s par requête | vérifié |
| Formats | `dublincore`, `unimarcxchange`, `intermarcxchange` ; le Dublin Core donne `dc:creator` ("Von Schramm, J-Paul. Auteur du texte"), `dc:publisher`, `dc:date`, `dc:format` ("1 vol. (248 p.) ; 21 cm"), dates de création et de modification de la notice | vérifié |
| Fraîcheur | catalogue vivant : 52 731 notices "texte imprimé" datées 2026, 101 887 datées 2025 | vérifié |
| Quotas | aucun indiqué sur `api.bnf.fr/fr/api-sru-catalogue-general` | **non vérifié** |

### 2.3 Dumps

| Élément | Valeur | Statut |
|---|---|---|
| Page | `https://api.bnf.fr/fr/dumps-de-databnffr`, lien "Accès aux dumps" vers un partage GoAnywhere `pef.bnf.fr/link/539d6d59-...` | vérifié |
| Téléchargement | formulaire JSF (`PublicGoDrive.xhtml`, POST avec `ViewState`) : **pas d'URL stable**. Scriptable (fait le 2026-11-08), fragile | vérifié |
| Date | tous les fichiers datés du 2026-05-28 ; fréquence non publiée ("Date de dernière mise à jour : 2026") | **non vérifié** pour la fréquence |
| Fichiers utiles | `databnf_editions_nt.tar.gz` 3 800 402 756 octets (3,54 Go), `databnf_person_authors_nt.tar.gz` 767 Mo, `databnf_org_authors_nt.tar.gz` 112 Mo, `databnf_rameau_nt.tar.gz` 126 Mo ; variantes N3 et RDF/XML (editions : 2,32 Go et 2,87 Go) | vérifié |
| Contenu `editions` | archive tar de fichiers `databnf_editions__manif_NNNNNN.nt` et `__expr_NNNNNN.nt` entrelacés. Les 20 premiers Mo contiennent 163 000 manifestations des ARK les plus anciens (`cb30000001q`, 1702), sans aucun ISBN | vérifié |
| Présence de `bnf-onto:isbn` dans le dump | non observée dans l'échantillon (notices anciennes) ; fraîcheur du dump par rapport au SPARQL inconnue | **non vérifié** |

### 2.4 Choix de la voie d'acquisition

| Critère | Dump filtré (`editions` + `person_authors`) | Moisson SPARQL + détails `VALUES` | SRU par lot |
|---|---|---|---|
| Volume transféré | 4,3 Go par import, plus le stockage temporaire | environ 450 Mo pour l'index ISBN (estimation), puis environ 9 800 requêtes de détails pour 1,96 M ARK | 1 requête pour 25 ISBN |
| Durée | téléchargement + deux passes de parsing (manifestations, puis personnes) | 8 minutes pour l'index (`isbn` puis `ean`) ; environ 3 h de requêtes pour les détails (mesuré : 1,07 s pour 200 ARK), 4 h 15 avec une pause de 0,5 s | 1,25 M ISBN à tester : environ 50 000 requêtes, 14 h à 1 requête/s |
| URL stable | non (JSF) | oui | oui |
| Jointure auteur | à faire soi-même entre deux fichiers | dans la requête (`expressionManifested` puis `relators/aut` puis `foaf:name`) | déjà faite (`dc:creator`) |
| Fraîcheur | 2026-05-28, contenu ISBN non vérifié | graphe à environ 9 mois | vivant |
| Engagement de service | fichiers statiques | aucun (SPARQL hors engagement) | non documenté |

**Recommandation** : moisson SPARQL de l'index ISBN vers ARK, filtrée contre notre ensemble
d'ISBN, puis détails par `VALUES` sur les seules manifestations appariées. Le SRU complète,
par lots de 25, les ISBN français avec offre que le SPARQL ne connaît pas (section 3.3). Le
dump ne sert que de plan de secours si le SPARQL devient indisponible.

## 3. Identifiants et appariement

### 3.1 Où se trouve l'ISBN

Modèle data.bnf.fr (vérifié sur deux manifestations de 2023 et sur la documentation du modèle) :

- La **manifestation** est l'URI `http://data.bnf.fr/ark:/12148/<ark>#about`, de type
  `frbr-rda:Manifestation`. Elle porte `bnf-onto:isbn` (avec tirets, ISBN-10 ou ISBN-13),
  `bibo:isbn13` (avec tirets), `bnf-onto:ean` (13 chiffres, 2 178 473 triplets seulement),
  `dcterms:title`, `rdvocab:publishersName`, `dcterms:publisher` (lieu, éditeur et date
  concaténés), `dcterms:description` ("1 vol. (248 p.)"), `dcterms:date`,
  `bnf-onto:firstYear`, `dcterms:abstract`, `dcterms:subject` (ARK RAMEAU),
  `rdfs:seeAlso` (notice du catalogue général).
- La **notice** est l'URI sans suffixe : `dcterms:created`, `dcterms:modified`, `foaf:focus`
  vers la manifestation.
- L'**expression** est `<ark>#Expression` : `dcterms:language`, `dcterms:contributor`, et le
  rôle par une propriété dédiée (`http://id.loc.gov/vocabulary/relators/aut` et
  `http://data.bnf.fr/vocabulary/roles/r70`, "auteur du texte").
- Le nom de l'auteur est sur l'entité personne `<ark-auteur>#about` (`foaf:name`,
  `foaf:familyName`, `foaf:givenName`, `owl:sameAs` vers VIAF). Une jointure est donc
  nécessaire.

Aucune propriété ne suffit seule (comptages SPARQL du 2026-11-08) :

- `bnf-onto:ean` seul sous-estime : test sur 500 ISBN, 26 trouvés par `ean`, 34 par le SRU ;
- mais **585 544 manifestations portent `bnf-onto:ean` sans `bnf-onto:isbn`**. Un échantillon
  de 200 d'entre elles est fait de livres récents (166 des années 2010 et 2020, auteur 85 %,
  pages 83 %) ;
- `bibo:isbn13` (2 274 329 triplets) n'apporte rien : vérifié, aucune manifestation ne le porte
  sans `bnf-onto:isbn`.

Il faut donc moissonner `bnf-onto:isbn` et `bnf-onto:ean`, et normaliser localement.

### 3.2 Qualité des ISBN

| Mesure | Valeur |
|---|---|
| Valeurs `bnf-onto:isbn` moissonnées | 4 703 410 |
| Normalisables en ISBN-13 valide | **4 681 200 (99,5 %)** |
| Valeurs `bnf-onto:ean` moissonnées | 2 178 473, dont 1 677 239 ISBN-13 valides (le reste : EAN de disques, vidéos, partitions) |
| Échantillon de 60 000 valeurs (6 offsets) | 62,0 % ISBN-13 valides, 36,6 % ISBN-10 valides (convertis), 1,2 % qualificatifs à la place du numéro (`Br.`, `Rel.`), 0,2 % somme de contrôle fausse |
| ISBN-13 distincts | 3 118 309 par `isbn` (une manifestation porte souvent l'ISBN-10 et l'ISBN-13 du même numéro), plus 306 015 par `ean` seul : **3 424 324** au total |
| ISBN-13 à préfixe français | 2 154 299 (62,9 %) |
| ISBN portés par plusieurs manifestations | 101 893 |

Règles :

1. Normaliser chaque `bnf-onto:isbn` avec `BarcodeValidationService.sanitize` : ISBN-10 converti
   en ISBN-13, somme de contrôle vérifiée. C'est la même fonction qui calcule l'identifiant des
   produits, donc les clés concordent. Les valeurs non numériques (`Br.`) sont ignorées et
   comptées dans le rapport.
2. `gtins` du `ReferenceRecord` = tous les ISBN-13 valides de la manifestation, issus de
   `bnf-onto:isbn` et de `bnf-onto:ean`.
3. ISBN porté par plusieurs manifestations : garder la notice au `dcterms:modified` le plus
   récent, puis la plus complète (auteur présent) ; les autres ARK vont dans `payload`.
4. Pas de repli titre + auteur : il produirait des faux positifs entre éditions (poche et
   grand format ont des ISBN distincts). `match: GTIN_ONLY`.

### 3.3 Recouvrement avec nos produits

Mesure par intersection des deux moissons (`isbn` et `ean`) et des 8 703 395 ISBN valides de
l'export :

| Population | Part de l'export | Couverte par le SPARQL |
|---|---|---|
| tous les ISBN | 100 % | **19,6 %** (1 709 591) |
| préfixe français | 32,7 % | 50,1 % |
| avec offre (`offers_count > 0`) | 62,2 % | 25,1 % |
| français avec offre | 26,0 % | **51,9 %** |

La moisson `ean` compte : avec `isbn` seul, le taux tombe à 17,0 % (42,8 % des français), mesure
sur les 6 858 598 premières lignes de l'export.

Complément SRU : sur 439 ISBN français avec offre absents des deux moissons, le SRU en trouve
**45 (10,3 %)**. Le taux des français avec offre passe donc d'environ 51,9 % à environ 57 %.

Comparaison avec Open Library sur un échantillon aléatoire de 1 000 ISBN (tiré des 6 858 598
premières lignes) : BnF 19,6 %, Open Library 50,3 %, union 61,1 %. Sur les ISBN français : BnF
50,8 %, Open Library 22,8 %, union 56,3 %. Les deux sources sont complémentaires (détail dans
le plan 10, section 1).

Requête à lancer en prod pour confirmer, sans passer par l'export (qui date du 2026-06-04) :
exporter les identifiants `ISBN_13` (méthode de la section 5.1), puis intersecter avec
le fichier ISBN vers ARK produit par les deux moissons. Pour un contrôle ponctuel, un `multiGet` sur
`products-moustik` de 1 000 ISBN tirés de la moisson suffit.

### 3.4 Champs exploitables

Mesure sur 1 000 manifestations tirées au hasard parmi les appariées par `isbn` (un second
échantillon de 200 manifestations à `ean` seul donne : auteur 85 %, pages 83 %, résumé 34,5 %,
sujets 46,5 %) :

| Champ | Propriété | Présent | Traitement |
|---|---|---|---|
| titre | `dcterms:title` | 100 % | `ref-data-bnf` seulement (le titre marchand reste le nom du produit, voir section 10) |
| éditeur | `rdvocab:publishersName` | 99,9 % | attribut `PUBLISHER` |
| description matérielle | `dcterms:description` | 99,7 % | nombre de pages extrait par `(\d+)\s*p\.` : 91,7 % ; `IX-406 p.` donne 406 |
| année | `bnf-onto:firstYear`, sinon `dcterms:date` | 99,8 % | attribut `PUBLICATION_YEAR` |
| langue | expression, `dcterms:language` (ISO 639-2) | 99,8 % | `ref-data-bnf` |
| auteur (ARK) | expression, `relators/aut` | 86,2 % | `payload.authorArks` |
| auteur (nom) | `foaf:name` de la personne | 84,3 % | attribut `AUTHORS` ; 11,9 % des manifestations ont plusieurs auteurs |
| sujets RAMEAU | `dcterms:subject` (ARK) | 63,7 % | `ref-data-bnf`, libellés par jointure à faire (dump `rameau` ou SPARQL) |
| résumé | `dcterms:abstract` | 19,5 % | `ref-data-bnf` ; affichage à arbitrer (section 10) |
| date de notice | `dcterms:modified` de la notice | 100 % | `sourceUpdatedAt` |

## 4. Modèle cible

### 4.1 Attributs

Les livres n'ont pas de verticale. `StandardAggregator` les agrège avec
`VerticalsConfigService.getConfigByIdOrDefault(null)`, donc avec `_default.yml`, dont le bloc
`attributesConfig.configs` s'applique, selon son commentaire, à tous les produits qui portent
ces attributs. C'est le seul endroit où déclarer des attributs canoniques pour les livres. C'est aussi indispensable pour
la priorité : `preferredSources` (P0-3) vit sur `AttributeConfig`, et n'agit donc que sur
`attributes.indexed`, jamais sur les attributs bruts de `attributes.all`.

Nouveaux attributs dans `verticals/src/main/resources/attributes/`, ajoutés à
`_default.yml` sous `attributesConfig.configs`. Les noms sont génériques, parce que
`EDITEUR` existe aussi pour les jeux vidéo et la musique hors verticale :

| Clé | Type | Source BnF (nom brut émis par le provider) | Synonymes marchands | `referentials.wikidata` |
|---|---|---|---|---|
| `AUTHORS` | TEXT, valeurs jointes par ` ; ` | `AUTEURS` | aucun aujourd'hui | P50 |
| `PUBLISHER` | TEXT | `EDITEUR` | `rakuten.com: ["EDITEUR", "CONTRIBUTEUR : EDITEUR", "CONTRIBUTEUR : ÉDITEUR"]` | P123 |
| `PAGE_COUNT` | NUMERIC | `NB_PAGES` | `rakuten.com: ["NB DE PAGES"]` | P1104 |
| `PUBLICATION_YEAR` | NUMERIC | `ANNEE_PUBLICATION` | aucun | P577 |

`YEAR` existe déjà, mais son libellé est "Année de mise en service" : on ne le réutilise pas.
Le format (`Poche`, `Broché`) n'existe pas dans data.bnf.fr ; il reste un attribut marchand.

Synonymes par source : `synonyms: { data-bnf: ["EDITEUR"], rakuten.com: [...] }`. Les noms
bruts du provider sont déjà normalisés (majuscules, sans accent).

Attention aux noms bruts accentués : `IdHelper.normalizeAttributeName` retire les accents
depuis le 2024-09-04 (commit `5ca15c2b9`), mais les fiches livres gardent leurs anciens noms.
Vérifié sur deux fiches, dont une mise à jour en janvier 2026 (`nudger.fr/9782020837934`) :
`CONTRIBUTEUR : ÉDITEUR`, `QUANTITÉ FOURNIE`, `PROPRIÉTAIRE`. Les livres ne sont jamais
réagrégés en entier, donc ces clés persistent. Il faut lister les deux graphies, comme le fait
déjà `YEAR.yml`, et le correctif P0-2 doit normaliser les deux côtés de la comparaison (le
synonyme au chargement et le nom de l'attribut à la recherche), pas seulement le synonyme.

Priorité (P0-3) : `preferredSources: [data-bnf, open-library]` sur les quatre attributs. Un
ordre statique suffit : quand la BnF n'a pas de notice, Open Library gagne parce que la BnF
est absente ; sans les deux, la valeur marchande reste. Sans cette priorité, l'éditeur BnF
("Maison de vie") et l'éditeur marchand ("[Maison de vie éditeur]") font un vote à 1 contre 1,
départagé par l'ordre lexical.

### 4.2 Identifiants et champs `Product`

- **Scénario minimal** : aucun champ nouveau hors `sourceAttributions` (P0-4). L'ARK est le
  `sourceRecordId` de `sourceAttributions["data-bnf"]` ; `recordUrl` est l'URL du catalogue
  général (`https://catalogue.bnf.fr/ark:/12148/<ark>`, 200 en HTML ; l'URL data.bnf.fr de la
  manifestation renvoie du RDF/XML). Pas de nouveau champ dans `ExternalIds`.
- **Auteur : attribut ou champ dédié ?** Attribut `AUTHORS` en scénario minimal : il hérite
  gratuitement de la provenance, de la priorité et de l'export. Un champ dédié ne se justifie
  qu'en verticale livres, pour des pages auteur : bloc `Product.book` (à créer) avec
  `List<BookContributor(name, role, arkBnf, viaf, wikidata, olAuthorKey)>`, sujets et public
  (section 5.8).
- Mapping ES : aucun en scénario minimal (`attributes.indexed` est déjà le chemin standard).

### 4.3 Export ouvert

`OpenDataService` (`services/opendata`) :

- `ISBN_HEADER` : ajouter `auteur`, `date_publication`, `ark_bnf` (et `sources`, P0-4).
- `ISBN_ATTRIBUTE_KEYS` : lire les clés canoniques (`PUBLISHER`, `PAGE_COUNT`, `AUTHORS`,
  `PUBLICATION_YEAR`) dans `attributes.indexed`, avec repli sur les clés brutes actuelles
  (`EDITEUR`, `NB DE PAGES`) tant que tous les produits n'ont pas été réagrégés.
- `ISBN_REQUIRED_FIELDS` : ajouter `sourceAttributions`. `ark_bnf` =
  `sourceAttributions["data-bnf"].sourceRecordId`.
- `toIsbnEntry` et libellés i18n `frontend/i18n/locales/{fr-FR,en-US}.json`.
- `souscategorie` : la supprimer, ou la remplir depuis la taxonomie marchande. En l'état, elle
  est toujours vide.

## 5. Pipeline

### 5.1 Chaîne ISBN commune (livrée avec ce plan, réutilisée par le plan 10)

1. `ProductRepository.exportIdsByBarcodeType(BarcodeType...)` (à créer) : même requête que
   `countItemsByBarcodeType` (terme sur `gtinInfos.upcType`, mappé en `keyword`), filtre de
   source sur `id` seul, flux de `long`. `exportAll(String[])` ne convient pas : il fait un
   `Query.findAll()` sans filtre et parcourt tout l'index (114 M de GTIN et 10 M d'ISBN selon
   les compteurs de `/api/opendata`).
2. `IsbnKeySetService` (à créer, `api/.../services/reference/`) : trie les identifiants dans un
   `long[]` (10 M x 8 octets, environ 80 Mo), recherche dichotomique, régénéré avant chaque
   import. Pas de `HashSet<Long>`, qui coûterait plusieurs centaines de Mo.
3. `ReferenceKeyFilter` (à créer, dans `services/reference-datasets`) : interface
   `boolean accepts(long productId)` passée par l'api au provider. Le module reste
   indépendant de l'api (ADR 0007). C'est l'extension du socle demandée en section 10.

### 5.2 Provider

`DataBnfProvider implements ReferenceDatasetProvider` (à créer), dans
`services/reference-datasets`. Il ne télécharge pas de fichier : il interroge le SPARQL
(extension "provider sans téléchargement", section 10).

1. **Index** : moisson `SELECT ?m ?isbn WHERE { ?m bnf-onto:isbn ?isbn } LIMIT 10000 OFFSET n`
   (471 pages), puis la même sur `bnf-onto:ean` (218 pages). Normalisation (section 3.2),
   filtrage par `ReferenceKeyFilter`. Contrôle : chaque total moissonné doit égaler un `COUNT`
   lancé avant ; sinon import refusé (pagination sans `ORDER BY` instable).
2. **Détails** : `VALUES ?m { ... }` par lots de 200 manifestations appariées : titre, éditeur,
   description, année, langue, auteurs (ARK et nom), sujets, résumé, `dcterms:modified`.
   Requête de référence dans les fixtures de test.
3. **Complément SRU** : pour les ISBN français du `ReferenceKeyFilter` absents de l'index,
   requêtes `bib.isbn any` par 25, au plus N par exécution (`reference-datasets.data-bnf.sru-max-requests`),
   en commençant par les produits avec offre. Les notices SRU sont converties dans le même
   `ReferenceRecord` (Dublin Core : `dc:creator` sans le suffixe de rôle, pages extraites de
   `dc:format`).
4. `ReferenceRecord` :
   - `sourceRecordId` = ARK court (`cb47168030q`) ;
   - `gtins` = ISBN-13 valides ; `brand` = null ; `models` = vide ;
   - `attributes` = `AUTEURS`, `EDITEUR`, `NB_PAGES`, `ANNEE_PUBLICATION` ;
   - `payload` = `authorArks`, `subjectArks`, `language`, `abstract`, `otherArks`, `via`
     (`sparql` ou `sru`) ;
   - `recordUrl` = `https://catalogue.bnf.fr/ark:/12148/<ark>` ;
   - `sourceUpdatedAt` = `dcterms:modified` de la notice (SRU : `LastModificationDate`).

### 5.3 Import

`ReferenceIndexService` écrit `ref-data-bnf-<horodatage>` puis bascule l'alias. Agrégation
maximale sans coût : l'index garde **les 3,4 M ISBN** avec leur ARK (documents légers), et les
détails seulement pour les ISBN appariés. En mode `CREATE` ou en verticale livres, les détails
sont moissonnés pour tous (environ 17 000 requêtes `VALUES`, 5 à 8 h). `min-rows-ratio: 0.9` sur
l'index.

### 5.4 Projection

`ReferenceProjectionService`, `match: GTIN_ONLY`, lots de 500 (`multiGetForAggregation`),
`DataFragment` synthétique avec `datasourceName = data-bnf`, puis
`StandardAggregator.onDatafragment` : les quatre attributs sont indexés via `_default.yml`.
Hash de contenu dans `datasourceCodes["data-bnf"]` : une projection inchangée ne réindexe rien.

Les ISBN créés par un flux après l'import sont rattrapés au cycle suivant : la chaîne ISBN
commune est régénérée, l'index les connaît déjà, seuls leurs détails sont demandés.

### 5.5 Scores

Aucun : les livres ne sont pas scorés. Le batch quotidien `BatchService.batch()` ne les visite
pas (pas de verticale).

### 5.6 Export ouvert

Section 4.3.

### 5.7 Front

Les fiches livres existent et affichent déjà les attributs bruts (vérifié dans le HTML de
`nudger.fr/9782355991097` : `EDITEUR`, `NB DE PAGES`, `CONTRIBUTEUR : ÉDITEUR`). Les attributs
canoniques suivent le même chemin. À ajouter : l'auteur sous le titre et le bloc "Sources"
de P0-4 avec lien ARK. Composants à identifier dans `frontend/app/components/product/`
(**non vérifié**).

### 5.8 Surcoût d'une verticale livres

Ce qui change si Q2 répond "verticale livres" (partagé avec le plan 10) :

- `verticals/.../verticals/books.yml` (à créer), `matchingCategories` sur les catégories Rakuten
  `BOOKS_*` ; environ 5,4 M produits avec offre (export de juin) entreraient dans le batch quotidien
  (classification, completion, scoring). Le volume actuel des 7 verticales n'est pas mesuré ici,
  mais l'ordre de grandeur impose de mesurer la durée du batch avant tout.
- `ResourceCompletionService` téléchargerait alors les images de millions de livres, alors que
  le cache de ressources de la beta n'a pas d'éviction (494 Go en juillet 2026, **non vérifié**
  ici). Il faut l'exclure pour les livres, ou s'appuyer sur les couvertures en lien direct du
  plan 10.
- Côté BnF : bloc `Product.book` (section 4.2), libellés RAMEAU (dump `rameau` de 126 Mo ou
  jointure SPARQL) comme facettes, pages auteur à partir des entités personne (nom, dates,
  `owl:sameAs` VIAF et Wikidata), résumé comme texte de fiche si Q3 le permet.
- Scoring : aucun critère d'impact naturel pour un livre. À cadrer avant tout développement
  (neuf contre occasion, disponibilité en bibliothèque ?).
- Effort : L, 10 à 15 jours, hors contenu éditorial.

## 6. Planification et volumétrie

- Cron mensuel (`0 0 3 1 * *`) pour l'index et les détails : le graphe SPARQL bouge peu (retard
  d'environ 9 mois). Cron quotidien léger (`0 30 5 * * *`) pour le complément SRU des nouveaux
  ISBN, plafonné en nombre de requêtes.
- Index : 689 requêtes, 8 minutes, environ 450 Mo transférés (estimation).
- Détails : environ 1,96 M ARK appariés (19,6 % de 10 000 891, extrapolation), soit 9 800
  requêtes de 200, environ 4 h 15 avec une pause de 0,5 s.
- SRU, rattrapage initial : 1 086 270 ISBN français avec offre absents du SPARQL dans l'export
  de juin, environ 1,25 M sur l'index actuel (extrapolation), soit 50 000 requêtes et 14 h à
  1 requête/s, à étaler sur plusieurs nuits. Ensuite, quelques centaines de requêtes par jour.
- `ref-data-bnf` : 3,4 M documents légers et 1,96 M documents détaillés.
- Projection : 1,96 M produits, 3 900 `multiGet` de 500, puis réindexation par lots. Une fois le
  hash en place, seules les notices modifiées sont réécrites.

## 7. Sourcing transparent

- `open4goods-config/datasources/data-bnf.yml` : `name: "data.bnf.fr (Bibliothèque nationale
  de France)"`, `portalUrl: https://data.bnf.fr`, `license: "Licence Ouverte (Etalab)"`,
  `licenseUrl` vers la licence, `attribution: "Données de la Bibliothèque nationale de France
  (data.bnf.fr, catalogue général)"`.
- `SourceAttribution` : `sourceRecordId` = ARK, `recordUrl` = notice du catalogue général,
  `sourceUpdatedAt` = date de modification de la notice, `retrievedAt` = date de moisson.
- Fiche produit : "Auteur, éditeur, année et pages : BnF, notice ark:/12148/cb47168030q,
  mise à jour le 2024-02-07".

## 8. Tests et critères d'acceptation

- Tests unitaires du provider sur des réponses SPARQL enregistrées : ISBN-10 avec tirets,
  ISBN-13, valeur `Br.`, manifestation à deux ISBN, ISBN porté par deux ARK, trois auteurs,
  auteur sans `foaf:name`, description sans pagination.
- Test unitaire de l'extraction des pages : `1 vol. (248 p.)`, `93 p.`, `1 vol. (IX-406 p.)`,
  `1 vol. (XXI-404 p.)`, `2 vol.` (rien), `non paginé` (rien).
- Test unitaire de la conversion SRU Dublin Core : "Von Schramm, J-Paul. Auteur du texte"
  donne "J-Paul Von Schramm".
- Test d'intégration (Testcontainers ES, SPARQL simulé par WireMock) : moisson de trois pages,
  contrôle du total, filtre par ensemble de clés, import, projection sur trois produits, priorité
  BnF sur un éditeur marchand entre crochets.
- Critères en beta :
  - au moins 45 % des ISBN français avec offre rattachés par le SPARQL seul (mesuré : 51,9 %) ;
  - `AUTHORS` renseigné sur au moins 80 % des produits rattachés (mesuré : 84 %) ;
  - colonnes `auteur`, `date_publication`, `ark_bnf` remplies dans l'export pour au moins
    15 % des lignes ;
  - zéro ISBN rattaché dont la somme de contrôle est fausse.

## 9. Découpage

1. Chaîne ISBN commune : `exportIdsByBarcodeType`, `IsbnKeySetService`, `ReferenceKeyFilter`
   (S).
2. Extension du socle "provider sans téléchargement" (S).
3. Attributs `AUTHORS`, `PUBLISHER`, `PAGE_COUNT`, `PUBLICATION_YEAR`, déclarés dans
   `_default.yml`, avec synonymes et `preferredSources` (S).
4. `DataBnfProvider`, voie SPARQL : index, détails, tests (M).
5. Complément SRU plafonné et ses tests (S).
6. Export ouvert : colonnes, clés canoniques, suppression ou correction de `souscategorie` (S).
7. Front : auteur sous le titre, lien ARK dans le bloc Sources (S).

## 10. Risques, challenges, questions

- **Retard du graphe SPARQL** : 900 manifestations 2026 contre 52 731 notices 2026 dans le
  catalogue. Or les nouveautés sont les livres qui ont le plus d'offres. Sans le complément
  SRU, la source profite surtout au fonds.
- **Pas d'engagement sur le SPARQL ni de quotas publiés** : la moisson est polie (une requête à
  la fois, pause, plafond) et journalise les temps. Proposition : écrire à
  `data@bnf.fr` pour annoncer l'usage et demander les limites.
- **Pagination sans `ORDER BY`** : stable le 2026-11-08 (total exact), mais sans garantie. Le
  contrôle par `COUNT` est obligatoire ; en cas d'échec, partitionner par `bnf-onto:firstYear`.
- **Dump inutilisable en l'état** : pas d'URL stable, 4,3 Go, présence de l'ISBN non vérifiée.
  À ne pas choisir comme voie principale.
- **Titre** : on ne remplace pas le nom du produit par le titre BnF dans ce plan, parce que
  l'URL des fiches est construite à partir du nom (vérifié : `/9782355991097` redirige vers
  `/9782355991097-l-epreuve-du-feu`) ; changer le nom changerait l'URL. Le titre reste dans
  `ref-data-bnf`.
- **`souscategorie` toujours vide** et export ISBN figé au 2026-06-04 alors qu'il est censé se
  régénérer chaque semaine (`fixedDelay` 7 jours) : deux défauts existants, hors de ce plan,
  signalés au propriétaire.
- **Manques du socle** (propositions) :
  - `ReferenceDatasetProvider.read(Path)` n'a pas de filtre par ensemble de clés : le mode R+
    en a besoin. Proposition : `read(Path, ReferenceKeyFilter)`.
  - Le socle suppose un fichier à télécharger (`resolveDownloadUrls()` puis `read(Path)`).
    SPARQL et SRU sont des sources "à interroger". Proposition : une méthode par défaut
    `Stream<ReferenceRecord> fetch(ReferenceKeyFilter)` que le provider redéfinit quand il
    n'a rien à télécharger.
  - Pas de mode "index léger + détails à la demande" : il est utile ici et pour Open Library.

Questions au propriétaire :

1. **Q2** : en attente depuis le 2026-11-09 (verticale livres, périmètre de la création). Seul
   le surcoût de la section 5.8 dépend de la verticale.
2. **Complément SRU** : accepte-t-on environ 50 000 requêtes de rattrapage (14 h étalées) puis
   un flux quotidien, avec une prise de contact préalable avec la BnF ? Il fait passer la
   couverture des livres français avec offre d'environ 52 % à environ 57 %, et c'est la seule
   voie pour les nouveautés.
3. **Résumé BnF** (19,5 % des notices, souvent le texte de l'éditeur) : l'affiche-t-on sur la
   fiche, ou le garde-t-on dans `ref-data-bnf` ?
