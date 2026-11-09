---
title: "Plan d'intégration : Open Library (livres, ISBN)"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Plan d'intégration : Open Library (livres, ISBN)

> Socle commun : [README](README.md). Fiche source :
> section 4.13 de l'inventaire interne des sources candidates (non publié).
> Plan jumeau : [09-data-bnf.md](09-data-bnf.md), qui livre la chaîne ISBN commune et les
> attributs livres réutilisés ici.
> Mesures du 2026-11-08 : en-têtes HTTP des dumps du 2026-10-31, échantillon des 100 premiers
> Mo du dump `editions`, API `/api/books` sur deux échantillons de 1 000 ISBN tirés de l'export
> ISBN public de nudger (fichier généré le 2026-06-04, 8 703 397 lignes ; échantillons tirés des
> 6 858 598 premières),
> API des couvertures.
> Pas d'accès à l'Elasticsearch de prod : le recouvrement est mesuré sur cet export.

## 0. Fiche

| Champ | Valeur |
|---|---|
| Identifiant | `open-library` |
| Mode | R+ : dump `editions` lu en flux et filtré contre notre ensemble d'ISBN, puis jointure avec le dump `authors` ; API `/api/books` pour les nouveaux ISBN entre deux dumps |
| Priorité | P2 (décision du 2026-11-09 : données livres intégrées, Q2 du README), juste après le plan 09 ; c'est la source qui couvre le plus de nos ISBN (50 %) |
| Effort | Scénario minimal : M, 4 à 6 jours si le plan 09 a livré la chaîne ISBN commune et les attributs ; 6 à 8 jours sinon. Surcoût verticale livres : partagé avec le plan 09 (section 5.7 de ce plan, section 5.8 du plan 09) |
| Périmètre | produits `ISBN_13` hors verticale : 10 000 891 ISBN dans l'index le 2026-11-08, 8 703 397 dans l'export du 2026-06-04 |
| Volume source | dump `editions` de 12,6 Go compressés, environ 60 M éditions dont environ 38 M avec ISBN (extrapolation de l'échantillon, **non vérifié**) |
| Recouvrement mesuré | 50,3 % de nos ISBN (échantillon aléatoire), 51,4 % de ceux qui ont une offre ; 22,8 % seulement des ISBN français |
| Dépend de | P0-3, P0-4, P0-5 ; chaîne ISBN commune (plan 09, section 5.1) ; extensions du socle de la section 10 |

## 1. Apport

Situation actuelle des fiches livres : voir le plan 09, section 1 (éditeur 35,9 %, pages 22,6 %,
aucun auteur, `souscategorie` toujours vide).

Open Library apporte :

- une couverture **internationale** : 62,7 % de nos ISBN non français, que la BnF ne couvre qu'à
  5,5 % ;
- l'**auteur** (95 à 97 % des ISBN trouvés), l'**éditeur** (98 à 99 %), la **date** (98 %) et le
  **nombre de pages** (78 à 80 %) ;
- un identifiant d'oeuvre (`/works/OL...W`) qui regroupe les éditions d'un même texte : utile
  pour relier poche et grand format, et pour la colonne d'export `ol_work_id` ;
- des **couvertures** (63 à 66 % des ISBN trouvés), affichables en lien direct seulement ;
- des signaux annexes, gardés dans `ref-open-library` : sujets, `ocaid` (scan lisible ou
  empruntable sur archive.org), identifiants OCLC, LCCN, Goodreads.

Complémentarité mesurée sur les mêmes 1 000 ISBN tirés au hasard parmi les 6 858 598 premières
lignes de l'export (BnF par la moisson complète `isbn` et `ean` du plan 09, Open Library par
l'API) :

| Population | n | BnF | Open Library | Union |
|---|---|---|---|---|
| tous | 1 000 | 19,6 % | 50,3 % | **61,1 %** |
| français | 311 | 50,8 % | 22,8 % | 56,3 % |
| non français | 689 | 5,5 % | 62,7 % | 63,3 % |
| avec offre (second échantillon) | 1 000 | 25,5 % | 51,4 % | **64,4 %** |
| dont français avec offre | 407 | 53,3 % | 28,5 % | 59,7 % |

Dans l'échantillon "tous", 88 ISBN sont chez les deux, 108 chez la BnF seule, 415 chez Open
Library seule et 389 chez aucune. Comme 62,2 % des lignes de l'export ont une offre, le
scénario minimal enrichit surtout des fiches vivantes.

## 2. Accès

| Élément | Valeur | Statut |
|---|---|---|
| Page | `https://openlibrary.org/developers/dumps` : "generated every month" | vérifié |
| URL stable | `https://openlibrary.org/data/ol_dump_editions_latest.txt.gz`, redirection 302 vers `archive.org/download/ol_dump_2026-10-31/ol_dump_editions_2026-10-31.txt.gz` | vérifié |
| Taille `editions` | **12 630 804 778 octets** (12,6 Go), `Last-Modified` 2026-11-02 | vérifié |
| Taille `works` | 4 078 855 962 octets (4,1 Go) | vérifié |
| Taille `authors` | 784 183 875 octets (0,78 Go) | vérifié |
| Autres | `covers_metadata` 75,6 Mo, `redirects` 75,7 Mo, `ratings` et `reading-log` | vérifié |
| Tailles affichées sur la page | "~ 9.2G", "~ 2.9G", "~ 0.5G" : la page date du 2025-06-10 et sous-estime la taille d'environ 27 % | vérifié |
| Format | TSV gzip, 5 colonnes : type, clé, révision, `last_modified`, JSON complet | vérifié |
| Requêtes `Range` | acceptées (réponse 206 sur les 100 premiers Mo) | vérifié |
| Débit | 100 Mo en 490 s (214 Ko/s) depuis le poste de travail, avec d'autres téléchargements en parallèle. À ce débit, 13,4 Go prendraient environ 17 h. Débit depuis un serveur : **non vérifié** ; la page propose aussi des torrents | vérifié (poste) |
| Authentification | aucune | vérifié |
| API `/api/books` | `bibkeys=ISBN:...` par 50, `jscmd=data` ; 20 appels en 68 s avec une pause de 1,5 s | vérifié |
| Quotas de l'API `/api/books` | non mesurés au-delà de 50 appels | **non vérifié** |
| API des couvertures | accès par ISBN limité à "100 requests/IP" par 5 minutes ; accès par identifiant de couverture ou OLID non limité ; "not for bulk download" | vérifié (documentation) |

## 3. Identifiants et appariement

### 3.1 Où se trouve l'ISBN

Dans chaque enregistrement `/type/edition` : `isbn_13` et `isbn_10`, des listes de chaînes. Les
oeuvres (`/type/work`) et les auteurs (`/type/author`) n'en portent pas.

Exemple réel (`/books/OL10025323M`) : `isbn_13: ["9780113500956"]`, `covers: [2319961]`,
`works: [{"key": "/works/OL9158704W"}]`, `authors: [{"key": "/authors/OL3235041A"}]`,
`publishers: ["Stationery Office Books"]`, `publish_date: "July 1997"`,
`number_of_pages: 49`, `physical_format: "Paperback"`.

### 3.2 Qualité des ISBN et des champs (échantillon du dump)

Les 100 premiers Mo du dump `editions` du 2026-10-31 : 471 827 éditions. L'ordre du fichier est
interne et non aléatoire (dominante MARC anglophone) : ces taux décrivent le fichier, pas nos
produits.

| Mesure | Valeur |
|---|---|
| Éditions avec au moins un ISBN-13 valide (ISBN-10 converti compris) | **299 192 (63,4 %)** |
| Champ `isbn_13` présent | 52,0 % ; valeurs fausses : 383 (0,1 % des éditions) |
| Champ `isbn_10` présent | 34,6 % ; valeurs fausses : 1 072 (0,2 %) |
| ISBN à préfixe français | 3,2 % des éditions à ISBN |
| Langue anglaise | 57 % des éditions |

Champs, rapportés aux 299 192 éditions à ISBN :

| Champ | Présent |
|---|---|
| `title` | 100 % |
| `publishers` | 98,5 % |
| `publish_date` (texte libre : "July 1997") | 97,7 % |
| `works` | 97,3 % |
| `authors` (clé d'auteur sur l'édition) | 89,2 % |
| `languages` | 83,1 % |
| `number_of_pages` | 58,1 % |
| `covers` | 30,0 % |
| `physical_format` | 27,4 % |

Le nom de l'auteur n'est pas dans l'édition : seule la clé `/authors/OL...A` y figure. Il faut
joindre le dump `authors`. Pour les 10,8 % d'éditions sans clé d'auteur, il faut remonter à
l'oeuvre (dump `works`, champ `authors[].author.key`).

### 3.3 Recouvrement avec nos produits (API)

| Échantillon (export du 2026-06-04, 6 858 598 premières lignes) | Trouvés | auteurs | éditeur | pages | couverture | date |
|---|---|---|---|---|---|---|
| 1 000 ISBN tirés au hasard | **50,3 %** | 97 % | 99 % | 80 % | 66 % | 98 % |
| 1 000 ISBN avec offre | **51,4 %** | 95 % | 98 % | 78 % | 63 % | 98 % |
| 500 premières lignes de l'export (biaisé) | 59,0 % | 96 % | 99 % | 73 % | 61 % | 98 % |

Les pourcentages de champs sont rapportés aux ISBN trouvés. Environ un tiers de nos ISBN
recevraient donc une couverture (50,3 % x 66 %). Les taux de champs sont plus élevés que dans
l'échantillon du dump : soit l'API résout l'auteur depuis l'oeuvre quand l'édition n'en a pas
(**non vérifié**), soit l'échantillon du dump, dominé par d'anciennes notices MARC, est moins
riche que nos ISBN, plus récents.

Requête à lancer en prod : exporter les identifiants `ISBN_13` (chaîne commune du plan 09),
puis compter les éditions du dump qui passent le filtre lors d'une exécution à blanc
(`POST /reference-datasets/open-library/import?dryRun=true`, à ajouter au socle).

### 3.4 Règles

1. Normaliser chaque valeur de `isbn_13` et `isbn_10` avec `BarcodeValidationService.sanitize`.
2. `gtins` = ISBN-13 valides de l'édition. Une édition peut en porter plusieurs.
3. ISBN porté par plusieurs éditions (doublons fréquents dans un catalogue collaboratif) :
   garder l'édition qui a le plus de champs utiles, puis la `revision` la plus haute. Les clés
   des autres vont dans `payload.duplicateEditions`. Le taux de doublons est à mesurer lors de
   la première exécution complète.
4. Année : première occurrence de `\b(1[5-9]\d\d|20\d\d)\b` dans `publish_date`.
5. `match: GTIN_ONLY`, pas de repli titre + auteur.

## 4. Modèle cible

### 4.1 Attributs (mutualisés avec le plan 09)

On réutilise les quatre attributs créés par le plan 09 et déclarés dans `_default.yml` :
`AUTHORS`, `PUBLISHER`, `PAGE_COUNT`, `PUBLICATION_YEAR`. Synonymes ajoutés pour cette
source, avec les noms bruts émis par le provider :

```yaml
synonyms:
  open-library: ["AUTHORS"]          # dans AUTHORS.yml
# PUBLISHER.yml        open-library: ["PUBLISHERS"]
# PAGE_COUNT.yml       open-library: ["NUMBER_OF_PAGES"]
# PUBLICATION_YEAR.yml open-library: ["PUBLISH_YEAR"]
```

`physical_format` ("Paperback") n'est pas projeté : 27 % de présence et un vocabulaire
anglais qui ne correspond pas au `FORMAT` marchand ("Poche", "Broché").

### 4.2 Priorité entre sources (P0-3)

`preferredSources: [data-bnf, open-library]` sur les quatre attributs. Raisons :

- la BnF est une notice de dépôt légal, Open Library un catalogue collaboratif alimenté par des
  imports (MARC, Better World Books, Amazon) ;
- un ordre statique suffit : quand la BnF n'a pas de notice (cas des livres étrangers), Open
  Library gagne parce que la BnF est absente ; quand aucune des deux n'a de notice, la valeur
  marchande reste ;
- sans P0-3, une valeur Open Library et une valeur marchande divergentes font un vote à 1 contre
  1, départagé par l'ordre lexical.

Rappel du plan 09 : P0-3 n'agit que sur `attributes.indexed`. Sans déclaration des attributs
dans `_default.yml`, la priorité ne s'applique pas aux livres.

### 4.3 Identifiants, couvertures et champs `Product`

- `sourceAttributions["open-library"]` (P0-4) : `sourceRecordId` = clé d'édition
  (`OL10025323M`), `recordUrl` = `https://openlibrary.org/books/OL10025323M`, qui sert aussi de
  lien de courtoisie demandé par la documentation des couvertures.
- Clé d'oeuvre : elle doit être sur le produit pour l'export. Proposition :
  `ExternalIds.openLibraryWork` (à créer), transmis par `DataFragment.externalIds`. Attention :
  `AttributeRealtimeAggregationService.mergeExternalIds` ne fusionne aujourd'hui que `mpn` et
  `sku`, il faut l'étendre. C'est le seul champ `Product` nouveau en scénario minimal.
- **Couverture en lien direct, sans mise en cache** : une `Resource` ajoutée par le
  `DataFragment` synthétique (chemin standard de `MediaAggregationService`), avec :
  - `url` = `https://covers.openlibrary.org/b/id/{coverId}-M.jpg`, construite à partir du
    premier identifiant de `editions.covers`. Jamais l'URL par ISBN, limitée à 100 requêtes par
    IP et par 5 minutes, ni de paramètre `?default=false`, qui ferait échouer le contrôle
    d'extension de `Product.isSupportedImageResource` ;
  - `datasourceName = open-library`, tag `cover` ;
  - un nouveau `ResourceTag.HOTLINK_ONLY` (à créer) dans `hardTags`.
- Ce qui existe déjà et ce qu'il faut changer :
  - les fiches livres affichent déjà des images marchandes en lien direct (vérifié :
    `images.fr.shopping.rakuten.com/photo/1124039485_S.jpg` sur `nudger.fr/9782355991097`),
    via `Product.externalCover()`, exposé par `ProductResourcesDto` ;
  - `externalCover()` prend la première image d'un `HashSet` : ordre arbitraire. Il faut une
    règle (image marchande d'abord, couverture Open Library sinon, ou l'inverse : question 1) ;
  - `ResourceCompletionService.shouldProcess` téléchargerait toute ressource ni traitée ni
    évincée dès que les livres auront une verticale : il doit ignorer `HOTLINK_ONLY` ;
  - `ProductMappingService.mapImage` passe l'URL dans `toWebpUrl`. Or
    `.../b/id/2319961-M.webp` renvoie 404 (vérifié). Il ne faut pas renseigner `resourceType`
    sur ces ressources, ou ne pas convertir en WebP une ressource sans `cacheKey`.

### 4.4 Export ouvert

- `ISBN_HEADER` : ajouter `ol_work_id`, lu dans `externalIds.openLibraryWork` (ajouter
  `externalIds` à `ISBN_REQUIRED_FIELDS`). Les colonnes `auteur`, `date_publication`, `editeur` et
  `nb_page` du plan 09 profitent d'Open Library sans changement, puisqu'elles lisent les
  attributs canoniques.
- Pas d'URL de couverture dans l'export : l'usage est limité à l'affichage.

## 5. Pipeline

### 5.1 Chaîne commune

`ProductRepository.exportIdsByBarcodeType`, `IsbnKeySetService` et `ReferenceKeyFilter`
(plan 09, section 5.1), réutilisés tels quels.

### 5.2 Provider

`OpenLibraryProvider implements ReferenceDatasetProvider` (à créer), dans
`services/reference-datasets`.

1. **Passe 1, éditions** (12,6 Go) : lecture en flux depuis l'URL `..._latest.txt.gz`
   (`GZIPInputStream` sur la réponse HTTP, sans fichier temporaire de 12,6 Go). Préfiltre
   textuel sur `"isbn_` avant tout parsing JSON, normalisation, `ReferenceKeyFilter.accepts`.
   Les éditions retenues sont écrites dans un fichier de travail compact (JSONL) ; les clés
   d'auteur nécessaires sont gardées dans un `long[]` trié (la partie numérique de
   `OL3235041A`).
2. **Passe 2, auteurs** (0,78 Go) : lecture en flux, conservation du seul nom des clés
   nécessaires (`name`, sinon `personal_name`).
3. **Passe 3, oeuvres** (4,1 Go, optionnelle) : uniquement pour les éditions sans clé d'auteur
   (10,8 % dans l'échantillon) et pour les sujets. Désactivée en scénario minimal
   (`reference-datasets.open-library.read-works: false`) : `ol_work_id` vient directement de
   `editions.works`.
4. `ReferenceRecord` :
   - `sourceRecordId` = clé d'édition ;
   - `gtins` = ISBN-13 valides ; `brand` = null ; `models` = vide ;
   - `attributes` = `AUTHORS`, `PUBLISHERS`, `NUMBER_OF_PAGES`, `PUBLISH_YEAR` ;
   - `payload` = `workKey`, `coverId`, `authorKeys`, `languages`, `physicalFormat`, `ocaid`,
     `oclc`, `lccn`, `duplicateEditions` ;
   - `recordUrl` = `https://openlibrary.org/books/<clé>` ;
   - `sourceUpdatedAt` = `last_modified` de l'édition.

### 5.3 Import

`ReferenceIndexService` écrit `ref-open-library-<horodatage>`, contrôle `min-rows-ratio: 0.9`
et bascule l'alias. Seules les éditions appariées sont indexées (environ 5 M, extrapolation
de 50,3 % sur 10 M). Indexer les 38 M éditions à ISBN n'a de sens qu'en mode `CREATE`, ce
qui créerait plus de 28 M de fiches sans offre. `CREATE` est le défaut depuis le 2026-11-09
(Q1 du README), mais son périmètre pour Open Library reste à fixer (README, Q2 et P0-11).

### 5.4 Projection

`ReferenceProjectionService`, `match: GTIN_ONLY`, lots de 500. Le `DataFragment` synthétique
porte les attributs et, si `coverId` existe, la `Resource` de couverture décrite en 4.3. La
projection de la couverture demande un point d'extension du socle (section 10).

### 5.5 Nouveaux ISBN entre deux dumps

Les produits créés après le dump attendent le mois suivant. Option : un complément quotidien
par `/api/books`, 50 ISBN par appel, plafonné
(`reference-datasets.open-library.api-max-requests`), sur les nouveaux ISBN avec offre du
`IsbnKeySetService` que ni `ref-open-library` ni `ref-data-bnf` ne connaissent. À 1,9 s par
appel, 1 000 appels traitent 50 000 ISBN en 32 minutes.

### 5.6 Scores, export, front

- Scores : aucun (pas de verticale).
- Export : section 4.4.
- Front : attributs par le chemin standard, couverture par `externalCover()`, bloc "Sources"
  de P0-4 avec le lien vers la page Open Library de l'édition.

### 5.7 Surcoût d'une verticale livres

En plus de la section 5.8 du plan 09 (fichier de verticale, batch, images, scoring) :

- la couverture Open Library en lien direct devient l'image principale des fiches sans image
  marchande, et évite de télécharger des millions d'images dans le cache de ressources ;
- passe `works` activée : sujets comme facettes (en anglais, à traduire ou à mapper sur RAMEAU
  et Dewey), regroupement des éditions par oeuvre ("autres éditions de ce livre") ;
- dumps `ratings` (5 Mo) et `reading-log` (65 Mo) comme signal de popularité pour trier les
  pages de catégorie ;
- lien "lire ou emprunter sur Open Library" quand `ocaid` existe (11,3 % des éditions de
  l'échantillon).

## 6. Planification et volumétrie

- Cron mensuel, après la publication du dump : par exemple `0 0 2 5 * *` (dump daté du dernier
  jour du mois, mis en ligne le 2 novembre pour celui du 31 octobre).
- Transfert : 13,4 Go par mois (éditions et auteurs), 17,5 Go avec les oeuvres. Lu en flux, sans
  stockage. Durée dominée par le débit d'archive.org : 17 h au débit mesuré depuis le poste,
  quelques dizaines de minutes à 10 Mo/s (**non vérifié**).
- Parsing : environ 60 M lignes ; le préfiltre `"isbn_` évite de parser le JSON d'environ 36 %
  des lignes (éditions sans ISBN dans l'échantillon).
- Mémoire : ensemble ISBN (80 Mo, partagé avec le plan 09) et clés d'auteur nécessaires (un
  `long[]` de quelques M d'entrées) ; les noms d'auteurs retenus (quelques centaines de Mo au
  plus) sont à mesurer à la première exécution.
- Projection : environ 5 M produits, 10 000 `multiGet` de 500 ; ensuite, seules les éditions
  dont le hash change sont réécrites.

## 7. Sourcing transparent

- `open4goods-config/datasources/open-library.yml` : `name: "Open Library (Internet Archive)"`,
  `portalUrl: https://openlibrary.org`, `license` : aucun droit revendiqué par l'Internet
  Archive (champ d'attribution seulement), `licenseUrl: https://openlibrary.org/developers/licensing`,
  `attribution: "Données et couvertures : Open Library"`.
- `SourceAttribution` : clé d'édition, URL de l'édition, `last_modified`, date de lecture du
  dump.
- Couvertures : lien vers la page de l'édition sur chaque fiche qui en affiche une, comme le
  demande la documentation ("A courtesy link back to Open Library is appreciated").

## 8. Tests et critères d'acceptation

- Tests unitaires du provider sur une fixture de 30 lignes réelles du dump : ISBN-10 seul,
  ISBN-13 seul, les deux, valeur fausse, édition sans auteur, deux éditions pour un même ISBN,
  `publish_date` en texte libre ("July 1997", "c1997", "1997?"), édition sans couverture.
- Test unitaire de la jointure auteurs sur une fixture du dump `authors`.
- Test unitaire de la `Resource` de couverture : URL par identifiant, tag `HOTLINK_ONLY`,
  ignorée par `ResourceCompletionService.shouldProcess`, pas de conversion WebP dans
  `ProductMappingService`.
- Test d'intégration (Testcontainers ES, dumps réduits servis par WireMock) : lecture en flux,
  filtre par ensemble de clés, import, projection, priorité BnF sur Open Library pour
  `PUBLISHER`.
- Critères en beta :
  - au moins 45 % des ISBN avec offre rattachés (mesuré : 51,4 %) ;
  - avec le plan 09, au moins 50 % des ISBN avec offre ont un auteur (mesuré : 62,9 % sur
    l'échantillon, borne haute, car 14 % des notices BnF n'ont pas d'auteur) ;
  - couverture affichée sur au moins 25 % des fiches livres (mesuré : environ 33 %) ;
  - aucune requête vers `covers.openlibrary.org/b/isbn/` dans les pages servies.

## 9. Découpage

1. (Si le plan 09 n'est pas fait) chaîne ISBN commune et attributs livres (S + S).
2. Extension du socle : lecture en flux HTTP sans fichier temporaire (S).
3. `OpenLibraryProvider`, passes éditions et auteurs, tests (M).
4. Couvertures : `ResourceTag.HOTLINK_ONLY`, exclusion dans `ResourceCompletionService`, règle
   de `externalCover()`, pas de WebP pour les ressources sans `cacheKey` (S).
5. Point d'extension de projection pour les ressources (S, section 10).
6. `ExternalIds.openLibraryWork` et extension de `mergeExternalIds`, colonne `ol_work_id`,
   synonymes `open-library` dans les quatre attributs (S).
7. Complément quotidien par `/api/books` (S, optionnel).

## 10. Risques, challenges, questions

- **Volume et débit** : 13,4 Go par mois pour en garder une fraction. Le vrai risque est le débit
  d'archive.org depuis nos serveurs, non mesuré. À tester avant d'écrire le provider : un
  `curl -r 0-1073741823` depuis la machine cible.
- **Qualité collaborative** : doublons d'éditions, `publish_date` en texte libre, éditeurs non
  normalisés ("Stationery Office Books" et "Stationery Office Books (TSO)"). La priorité BnF
  limite l'impact sur les livres français ; pour les autres, Open Library reste meilleure que la
  valeur marchande absente.
- **Couvertures** : une règle d'affichage mal posée peut charger
  `covers.openlibrary.org/b/isbn/...` depuis le navigateur des visiteurs et tomber sous la limite
  de 100 requêtes par IP. N'utiliser que les URL par identifiant.
- **Couverture française faible** (22,8 %, contre 50,8 % pour la BnF) : Open Library ne dispense pas du plan 09 pour le coeur
  de cible de nudger.fr.
- **Manques du socle** (propositions, en plus de celles du plan 09) :
  - Jointure multi-fichiers (éditions puis auteurs, puis oeuvres) : rien dans le socle. Le
    provider gère ses fichiers de travail sous `reference-datasets.workdir` (à créer) ; le
    socle doit au moins fournir ce répertoire et son nettoyage.
  - `RemoteFileCachingService.downloadToTmpFile` écrit le fichier entier sur disque (12,6 Go
    ici). Proposition : une méthode d'ouverture en flux (à créer), avec reprise par `Range`
    en cas de coupure.
  - `ReferenceRecord` n'a pas de place pour des médias. Proposition : un champ
    `List<ReferenceMedia(url, tags, hotlinkOnly)>` (à créer), que `ReferenceProjectionService`
    transforme en `Resource` du `DataFragment`. Sinon, chaque source doit coder sa propre
    projection.
  - Exécution à blanc de l'import (`dryRun`) pour mesurer le taux de filtrage avant d'indexer.

Questions au propriétaire :

1. **Couvertures** : les affiche-t-on en lien direct (sans cache, avec lien de courtoisie) ? Et
   en premier, ou seulement quand la fiche n'a pas d'image marchande ?
2. **Infrastructure** : accepte-t-on un job mensuel de plusieurs heures qui lit 13,4 Go en flux,
   sur le serveur de l'api, ou faut-il un conteneur de batch séparé ?
3. **Fraîcheur** : les nouveaux ISBN attendent-ils le dump suivant (jusqu'à un mois), ou
   active-t-on le complément quotidien par l'API (section 5.5) ?
