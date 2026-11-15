# Workflows GitHub Actions de déploiement beta

Décision de Goulven sur GOU-945, mise en oeuvre par GOU-1036 : beta se
redéploie **automatiquement à chaque merge sur `main`**, composant par
composant. Cela remplace le déploiement par dispatch uniquement de GOU-387.
Un gel de recette (`BETA_DEPLOY_FREEZE`) suspend ces déploiements
automatiques sans bloquer un correctif livré à la main.

## Workflows de déploiement beta

| Workflow                           | Fichier                                    | Id        | Déploie sur beta                          |
| ---------------------------------- | ------------------------------------------ | --------- | ----------------------------------------- |
| `🔘 Beta - Build, Test, and Publish` | `.github/workflows/testAndPublishBeta.yml` | 81022703  | JARs `admin`, `api`, `ui` et `front-api`. |
| `Frontend CI & Deploy`               | `.github/workflows/frontend-ci.yml`        | 171671519 | Frontend Nuxt SSR (`frontend-ssr`).       |

Les deux workflows sont `active`. Chacun a deux jobs :

1. `build` : build et tests (Maven ; ou lint,
   generate, tests et build Nuxt avec les contrôles ABI et `.output`). Il ne
   lit aucun secret. Il publie le livrable en artefact (archive tar, gardée
   3 jours).
2. `deploy` (`needs: build`, `environment: beta`) : récupère l'artefact,
   rend les secrets de l'environnement `beta` sur l'hôte et publie.

### Chemins qui déclenchent un déploiement (`push` sur `main`)

Filtres `paths` de chaque workflow. Un push qui touche les deux périmètres
lance les deux workflows, indépendamment.

| Workflow                 | Chemins déclencheurs                                                                 |
| ------------------------ | ------------------------------------------------------------------------------------ |
| `testAndPublishBeta.yml` | Tout le dépôt sauf `frontend/**` et sauf les autres fichiers de `.github/workflows/` ; son propre fichier déclenche. Inclut donc les modules Maven, `docs/**` (les `.md` sont embarqués dans les JARs), `scripts/**`, `.github/actions/**`. |
| `frontend-ci.yml`        | `frontend/**`, `docs/**` et `verticals/src/main/resources/guides/**` (collection `docs` de `frontend/content.config.ts`), `scripts/deploy/**`, `.github/workflows/frontend-ci.yml`. |

Un changement purement frontend (`frontend/**`) ne redéploie pas le backend,
et un changement purement backend ne redéploie pas le frontend. Les `.md` de
`frontend/` sont aussi embarqués dans les JARs : ils y arrivent au prochain
déploiement backend.

### Gardes

- Les jobs `build` et `deploy` sont gardés par
  `if: github.ref == 'refs/heads/main'`, et les étapes de déploiement de
  `frontend-ci.yml` aussi. Aucun déploiement depuis une PR, un fork ou une
  autre branche : un dispatch sur une autre branche termine en `skipped`.
- Aucun des deux workflows n'a de déclencheur `pull_request`.

### Concurrence

- `deploy` : groupes `deploy-beta-backend` et `deploy-beta-frontend`,
  `cancel-in-progress: false`. Un seul déploiement beta à la fois par
  composant ; un redémarrage en cours n'est jamais interrompu. GitHub ne
  garde qu'un run en attente par groupe (le plus récent remplace l'ancien) :
  le dernier merge finit déployé.
- `build` : groupes `build-beta-backend-<événement>` et
  `build-beta-frontend-<événement>`, `cancel-in-progress: true`. Un push plus
  récent annule le build d'un push plus ancien, pour que beta ne reçoive
  jamais un commit plus ancien après un plus récent. Le groupe est séparé par
  événement : un push pendant un gel n'annule pas un dispatch qui livre un
  correctif.

## Gel de recette : `BETA_DEPLOY_FREEZE`

Variable de dépôt (Settings > Secrets and variables > Actions > Variables),
posée par Ops au besoin :

- `true` : un `push` sur `main` lance toujours `build` (build et tests),
  mais le job `deploy` est `skipped`. Une annotation et le résumé du run
  indiquent "Beta deploy freeze".
- absente ou toute autre valeur : déploiement normal au merge.
- pendant le gel, un `workflow_dispatch` sur `main` déploie quand même, pour
  livrer un correctif bloquant de recette (le résumé le signale).

Poser et lever le gel :

```bash
gh variable set BETA_DEPLOY_FREEZE --body true --repo open4good/open4goods
gh variable delete BETA_DEPLOY_FREEZE --repo open4good/open4goods
```

Lever le gel ne redéploie rien : le prochain merge, ou un dispatch, déploie
l'état de `main`.

## Batch en cours : redémarrage de l'api différé (GOU-2106)

Un déploiement bêta ne coupe plus un batch complet de l'api (`BatchService.batch()`,
plus de 12 h) :

- l'api écrit `/opt/open4goods/run/api-batch.marker` (`pid=`, `started=`) au
  début du batch et le supprime à la fin, échec compris (propriété
  `batch-running-marker`, posée dans la configuration bêta d'open4goods-config) ;
- le job `deploy` appelle `publish-jars.sh beta` avec
  `DEFER_API_RESTART_DURING_BATCH=1`. Si le marqueur désigne une api vivante,
  les JARs sont publiés et `sbadmin`, `front-api` et `ui` redémarrent, mais pas
  l'api : le log du step `Publish Applications` affiche
  `DEFERRED: api restart deferred, batch in progress ...` et une annotation
  "Beta api restart deferred" ;
- l'unité transitoire `nudger-api-deferred-restart.service` attend la
  disparition du marqueur sur le serveur, puis redémarre l'api sur le dernier
  JAR (`journalctl -u nudger-api-deferred-restart.service`). Un merge suivant la
  trouve en attente et n'en lance pas une seconde ;
- garde-fous : processus mort, marqueur de plus de 20 h ou 20 h d'attente →
  redémarrage quand même, avec une ligne `WARNING` ;
- un dispatch avec `restart_api_now` coché redémarre l'api tout de suite
  (le batch est perdu). La prod (`releaseDeployProd.yml`) et
  `serviceSecrets.yml` ne sont pas concernés.

Le détail est dans `doc/DEPLOYMENT.md` d'open4goods-config.

## Déployer `main` à la main (dispatch)

1. `gh workflow run testAndPublishBeta.yml --ref main` (backend) et/ou
   `gh workflow run frontend-ci.yml --ref main` (frontend).
2. Suivre le run jusqu'à son résultat (`gh run watch`) et reporter un échec
   sur l'issue.

## Contrôles de PR (GOU-330)

- `.github/workflows/frontend-pr.yml` : `pull_request` uniquement,
  `paths: frontend/**`, `verticals/src/main/resources/guides/**` et les deux
  workflows frontend. Exécute `pnpm install --frozen-lockfile`, `pnpm lint`,
  `pnpm generate`, `pnpm test --run` et `pnpm build` dans `frontend/`, avec
  les mêmes versions pnpm/Node que `frontend-ci.yml`. **Aucune étape de
  déploiement, aucun secret de déploiement.**
- Node 24 (GOU-394) : `frontend-ci.yml` et `frontend-pr.yml` fixent
  `actions/setup-node` en `node-version: "24"`, la version du conteneur
  `frontend-ssr` (`node:24`, ABI 137). `pnpm install` compile `better-sqlite3`
  (Nuxt Content) pour ce Node ; une étape après `pnpm build` fait échouer le
  run si `.output/server/node_modules/better-sqlite3/build/Release/better_sqlite3.node`
  n'exporte pas `node_register_module_v137`, avant tout déploiement.
- `.github/workflows/ci-pr.yml` ignore `frontend/**` (`paths-ignore`) ; il
  couvre le build Maven sur toute autre modification de PR.
