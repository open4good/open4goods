# État des workflows GitHub Actions de déploiement beta

Contexte : GOU-249 a désactivé manuellement deux workflows pour éviter qu'un
push forcé sur `main` ne déclenche un déploiement vers beta.nudger.fr. GOU-330
documente cet état et le garde-fou de remplacement mis en place côté PR.

## Workflows désactivés (`disabled_manually`)

| Workflow                           | Fichier                                | Id         | Rôle                |
| ----------------------------------- | --------------------------------------- | ---------- | -------------------- |
| `Frontend CI & Deploy`               | `.github/workflows/frontend-ci.yml`     | 171671519  | Lint/generate/test/build `frontend/**`, puis déploiement SSR vers beta. |
| `🔘 Beta - Build, Test, and Publish` | `.github/workflows/testAndPublishBeta.yml` | 81022703 | Build/test backend puis publication vers beta. |

Depuis GOU-387, les deux workflows ne déclarent plus que `workflow_dispatch` :
plus de déclencheur `push` sur `main` ni `pull_request`. Un merge ou un
force-push sur `main` ne redéploie donc plus beta, même une fois les
workflows réactivés. Le job de chacun est gardé par
`if: github.ref == 'refs/heads/main'` : un dispatch sur une autre branche
termine en `skipped` sans build ni déploiement. Dans `frontend-ci.yml`, les
étapes de déploiement exigent en plus `github.event_name == 'workflow_dispatch'`.

Ils restent `disabled_manually` côté GitHub tant que l'issue de déploiement
qui suit GOU-387 ne les réactive pas.

### Déployer `main` sur beta (dispatch manuel)

1. Réactiver le workflow (une seule fois, par l'issue de déploiement
   mandatée) : `gh workflow enable testAndPublishBeta.yml` et/ou
   `gh workflow enable frontend-ci.yml`.
2. Lancer le déploiement sur `main` uniquement :
   `gh workflow run testAndPublishBeta.yml --ref main` (backend) et
   `gh workflow run frontend-ci.yml --ref main` (frontend). Les deux sont
   indépendants : `testAndPublishBeta.yml` ne filtre plus `frontend/**` et
   rebuild tout le backend à chaque dispatch.
3. Suivre le run jusqu'à son résultat (`gh run watch`) et reporter un échec
   sur l'issue.

## Garde-fou de remplacement (GOU-330)

- `.github/workflows/frontend-pr.yml` (nouveau, `active`) : `pull_request`
  uniquement, `paths: frontend/**` et
  `verticals/src/main/resources/guides/**`. Exécute
  `pnpm install --frozen-lockfile`, `pnpm lint`, `pnpm generate`,
  `pnpm test --run` et `pnpm build` dans `frontend/`, avec les mêmes versions
  pnpm/Node que `frontend-ci.yml`. **Aucune étape de déploiement, aucun secret
  de déploiement.**
- Node 24 (GOU-394) : `frontend-ci.yml` et `frontend-pr.yml` fixent
  `actions/setup-node` en `node-version: "24"`, la version du conteneur
  `frontend-ssr` (`node:24`, ABI 137). `pnpm install` compile `better-sqlite3`
  (Nuxt Content) pour ce Node ; une étape après `pnpm build` fait échouer le
  run si `.output/server/node_modules/better-sqlite3/build/Release/better_sqlite3.node`
  n'exporte pas `node_register_module_v137`, avant tout déploiement. Les PR qui
  modifient ces deux workflows déclenchent aussi `frontend-pr.yml`.
- Ce workflow ne couvre pas les push sur `main` : `frontend-ci.yml` ne
  s'exécute plus que sur dispatch. Un échec de test uniquement visible après
  merge sur `main` n'apparaît qu'au prochain dispatch de déploiement. Les PR
  restent la seule ligne de défense automatique pour `frontend/**`.
- `.github/workflows/b2b-frontend-ci.yml` (`B2B Frontend CI`) est `active` et
  exécute déjà `pnpm test` sur les PR touchant `b2b-frontend/**` : pas de trou
  équivalent côté B2B, aucune action nécessaire.
- `.github/workflows/ci-pr.yml` ignore `frontend/**` (`paths-ignore`) ; il
  continue de couvrir le build Maven sur toute autre modification de PR.

## Réactivation

La réactivation et le premier dispatch relèvent de l'issue de déploiement qui
suit GOU-387 (demande de [GOU-253](/GOU/issues/GOU-253) et
[GOU-254](/GOU/issues/GOU-254)), pas de la PR qui change les déclencheurs.
Le risque de déclenchement sur force-push identifié par GOU-249 est levé par
la suppression du déclencheur `push`.
