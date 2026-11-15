# Génération open data (api)

Exploitation du passage hebdomadaire qui produit `gtin.zip`, `isbn.zip` et leurs manifests dans
`/opt/open4goods/opendata/` (GOU-2248). Architecture et contrat : [opendata.md](../architecture/opendata.md).

## Planification

- `api` seul, `open-data-config.generation-cron` (`OPEN_DATA_GENERATION_CRON`), par défaut `0 30 1 * * SUN`,
  fuseau `Europe/Paris` : en dehors de la fenêtre batch (à partir de 12:30, heure serveur) et de la fenêtre de release.
  `-` désactive le cron sans désactiver le déclencheur manuel.
- `open-data-config.generation-enabled` (`OPEN_DATA_GENERATION_ENABLED`, `true` par défaut dans `api`, `false` en
  `local` et `dev`) coupe toute génération.
- Un seul passage à la fois ; il tourne sur le thread `opendata-generation`, hors du planificateur.

## Relance manuelle

Clé `admin-key` (secret `O4G_INTERNAL_API_KEY`), brute dans l'en-tête `Authorization`, hors de la fenêtre batch :

```bash
curl -X POST -H "Authorization: $O4G_INTERNAL_API_KEY" https://api.nudger.fr/opendata/generate
curl -H "Authorization: $O4G_INTERNAL_API_KEY" https://api.nudger.fr/opendata/generation
```

`POST` répond 202 (passage lancé) ou 409 (passage en cours, ou génération désactivée), avec l'état courant. Le `GET`
renvoie `state` (`IDLE`, `RUNNING`, `SUCCEEDED`, `FAILED`), `startedAt`, `finishedAt`, `lastSuccessAt`, `error` et
`rows` par jeu.

L'ancien `GET /opendata/generate` de `ui` est retiré.

## Surveillance

- Log `ERROR` `Open data generation failed, the published files and manifests are unchanged`, avec la cause.
- Health `openData` de `api` : DOWN après un passage en échec (détails `state`, `error`, `lastSuccessAt`),
  UNKNOWN si un zip publié manque, UP sinon.
- Métriques Micrometer (noms Prometheus) :
  - `opendata_generation_runs_total{outcome="success|failure"}` ;
  - `opendata_generation_duration_seconds{outcome}` ;
  - `opendata_generation_failed` (1 si le dernier passage du processus a échoué) ;
  - `opendata_generation_running` ;
  - `opendata_generation_last_success_timestamp_seconds` (NaN sans succès depuis le démarrage) ;
  - `opendata_dataset_rows{dataset="gtin|isbn"}`.
- Âge des fichiers publiés (`opendata_dataset_age_seconds`, seuil de 8 jours) : front-api, lecture du manifest (lot 1,
  PR suivante).

## Échecs et reprise

| Cause | Message | Action |
|---|---|---|
| Disque | `Not enough free disk space ... required` | libérer de l'espace (1,5 fois les zips publiés), relancer |
| Lignes perdues | `Dataset gtin refused: ... the published manifest has ...` ou `... the index counted ...` | vérifier l'index (réindexation, purge), relancer ; ne baisser `min-rows-ratio` qu'en connaissance de cause |
| Elasticsearch | exception ES (`ContentTooLongException`, timeout, 429) | vérifier l'ES ; une réponse trop grosse se corrige par `export-page-size`, jamais par le buffer du client |

Un échec laisse en place les zips et les manifests publiés ; `opendata/tmp/` est supprimé. Un dossier `tmp/` laissé
par un arrêt brutal de `api` est supprimé au passage suivant.

## Mesures en bêta (critère d'acceptation)

Pour le compte rendu d'un passage complet :

- durée : `finishedAt - startedAt` du `GET /opendata/generation`, ou `opendata_generation_duration_seconds` ;
- lignes : `rows` du statut et des manifests ;
- taille maximale d'une réponse ES : `export-page-size` fois le poids du plus gros document projeté. Le mesurer sur un
  échantillon (`_source` limité à `EXPORT_SOURCE_FIELDS`, tri par taille d'`offerNames`) ;
- latence p95 de la recherche du site pendant le passage, comparée à une nuit sans passage (métriques de front-api).
