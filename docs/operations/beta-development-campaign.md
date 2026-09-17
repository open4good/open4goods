---
title: "Developpement strictement local et promotions phasees"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Developpement strictement local et promotions phasees

ADR-0014 et le contrat [Project](../../.o4g/project.yml) decrivent quatre phases.
DEVELOPMENT utilise uniquement la machine locale; beta sert ensuite a valider le
candidat deja qualifie, puis le meme SHA, les memes artefacts et le meme dataset
sont promus en production. L'ordre proprietaire permanent du 17 septembre 2026
autorise ces deux promotions lorsque les gates sont verts.

## Stack locale

La stack hybride conserve Elasticsearch, Redis et PostgreSQL dans Docker. Kibana
est dans le profil `tools`; XWiki et MySQL sont dans le profil transitoire `xwiki`.
Les sept applications Java et les deux frontends tournent nativement sur les ports
du contrat local.

```bash
scripts/local/open4goods.sh init
scripts/local/open4goods.sh doctor
scripts/local/open4goods.sh up
scripts/local/open4goods.sh status
scripts/local/open4goods.sh logs front-api
scripts/local/open4goods.sh restart front-api
scripts/local/open4goods.sh down
```

`init` conserve tout fichier existant et cree seulement les entrees manquantes.
Les donnees, PID et logs restent sous `.local/`. Les valeurs privees vivent dans
`.env.local` et `.local/config/<service>.yml`, tous deux ignores. Les templates
suivis sont `.env.local.example` et `ops/local/config/*.yml.example`.

## Sauvegarde et donnees

`PRODUCT_BACKUP_SOURCE_URI` designe la copie privee montee en lecture seule et
`XWIKI_XAR_PATH` le XAR transitoire. La verification attend le manifeste original,
controle son ensemble gzip et ecrit uniquement des metadonnees sous `.local/backup`.
Elle ne modifie aucun octet source.

```bash
scripts/local/open4goods.sh backup verify
scripts/local/open4goods.sh data sample
scripts/local/open4goods.sh data full
docker compose --profile xwiki up -d mysql xwiki
scripts/local/open4goods.sh xwiki import
```

L'echantillon est selectionne par empreinte de contenu et couvre les sept verticales,
les produits riches, les cas legacy, Amazon ambigu et les erreurs connues. Il sert
aux boucles rapides. La recette de fermeture utilise le backup entier, des index
versionnes vides et l'importeur dedie configure par `O4G_LOCAL_FULL_IMPORT_URL`;
l'ancien import direct vers Product ne fait pas partie de ce chemin.

## Jobs et gates

Le scheduling commun est desactive dans le profil local. Les connecteurs restent
accessibles uniquement par commande explicite et chaque lancement est journalise
sans secret dans `.local/jobs.log`.

```bash
scripts/local/open4goods.sh jobs run eprel
scripts/local/open4goods.sh jobs run icecat
scripts/local/open4goods.sh jobs run feeds
scripts/local/open4goods.sh jobs run batch
scripts/work/wo.py next --phase BETA_VALIDATION
```

`local-campaign-readiness` ne peut fermer tant qu'un autre ordre DEVELOPMENT reste
ouvert et exige la preuve exacte `local-full-recette:passed`. La beta reste alors
`AWAITING_DEVELOPMENT`; la production reste `AWAITING_BETA_VALIDATION`. Les retraits
physiques d'index historiques et de XWiki attendent sept jours complets sains et une
restauration verifiee en POST_PRODUCTION.
