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
