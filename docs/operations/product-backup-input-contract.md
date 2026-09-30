---
title: "Contrat d'entree de sauvegarde produit"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Contrat d'entree de sauvegarde produit

`PRODUCT_BACKUP_SOURCE_URI` reste une variable beta privee. Le script
`scripts/migration/pin_product_backup.py` lit le jeu fige depuis ce volume et produit
un manifeste d'import prive, sans copier des enregistrements vers Git.

Le manifeste d'import v1 contient l'empreinte SHA-256 du manifeste legacy, son heure de
fin, la liste ordonnee des seuls gzip autorises, et pour chacun l'empreinte, la taille et
le nombre de lignes decodees. Il refuse un manifeste absent ou changeant, un nom non
autorise, un fichier manquant, un gzip tronque, ou un total de lignes different du total
legacy. La source beta est un emplacement fige distinct de la sortie du publisher; sa
stabilite est revalidee pendant le pinning.

L'inventaire borne publie uniquement noms et formes de champs ainsi que des compteurs de
GTIN, doublons, attribution, vertical/non-classifie, media et prix legacy. Le mapping v1
renvoie les 94 dispositions d'attributs au manifeste de registre et interdit d'inventer
identifiants fournisseur, observations, langues, identite d'offre ou droits. Une source
inconnue est limitee a l'identite necessaire `NUDGER_WEB`; Amazon incertain est mis en
quarantaine; scores et disponibilite sont recalcules.

Les comptes de production, la facturation, les cles, les corrections hors Product et les
octets media sont des domaines de preservation distincts. Le comptage exhaustif des GTIN
distincts attend l'etape d'import controlee en capacite.

## Echantillon local deterministe (contrat pour l'import versionne)

`scripts/local/product_backup_sample.py` lit l'archive figee (memes manifeste et gzip que
ci-dessus, sans copie de bytes source hors du volume prive) et ecrit deux fichiers ignores
par Git : `products-sample.jsonl.gz` et son descripteur `products-sample-manifest.json`
(schema `open4goods-local-sample/v1`).

Le descripteur porte l'empreinte SHA-256 du manifeste legacy source, le nombre de lignes
lues, le nom, l'empreinte SHA-256 et le nombre d'enregistrements de l'echantillon, la
taille par compartiment demandee, et la liste des empreintes SHA-256 retenues par
compartiment. La generation echoue si l'un des compartiments suivants reste vide :
les sept verticales (`air-conditioner`, `dishwasher`, `oven`, `refrigerator`,
`smartphones`, `tv`, `washing-machine`) plus `source-rich`, `legacy-only`,
`amazon-ambiguous` et `known-error`. La selection retient les
empreintes de contenu les plus petites par compartiment et trie la sortie par empreinte,
donc une generation repetee depuis la meme entree produit un fichier et un descripteur
strictement identiques (test `test_repeated_generation_is_byte_identical_and_covers_every_bucket`).

Ce couple (manifeste d'import pin + echantillon deterministe) forme l'entree produit figee
que consomme l'import versionne (cibles, checkpoints, annulation, rejeu, quarantaine et
mesures de ressources restent portes par ce travail d'import, pas par ce contrat).
