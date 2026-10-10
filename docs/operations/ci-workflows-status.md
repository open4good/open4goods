# État des workflows GitHub Actions désactivés

Contexte : GOU-249 a désactivé manuellement deux workflows pour éviter qu'un
push forcé sur `main` ne déclenche un déploiement vers beta.nudger.fr. GOU-330
documente cet état et le garde-fou de remplacement mis en place côté PR.

## Workflows désactivés (`disabled_manually`)

| Workflow                           | Fichier                                | Id         | Couvrait            |
| ----------------------------------- | --------------------------------------- | ---------- | -------------------- |
| `Frontend CI & Deploy`               | `.github/workflows/frontend-ci.yml`     | 171671519  | Lint/generate/test/build `frontend/**` sur push et PR, puis déploiement SSR vers beta. |
| `🔘 Beta - Build, Test, and Publish` | `.github/workflows/testAndPublishBeta.yml` | 81022703 | Build/test backend puis publication vers beta. |

Les deux fichiers restent dans le dépôt et déclarent toujours leurs
déclencheurs `push`/`pull_request` ; seul l'état `disabled_manually` côté
GitHub empêche leur exécution. Leur réactivation est du périmètre de
[GOU-251](/GOU/issues/GOU-251) (refonte du déploiement beta) : ne pas les
réactiver ni en changer les déclencheurs sans ce mandat.

## Garde-fou de remplacement (GOU-330)

- `.github/workflows/frontend-pr.yml` (nouveau, `active`) : `pull_request`
  uniquement, `paths: frontend/**` et
  `verticals/src/main/resources/guides/**`. Exécute
  `pnpm install --frozen-lockfile`, `pnpm lint`, `pnpm generate`,
  `pnpm test --run` et `pnpm build` dans `frontend/`, avec les mêmes versions
  pnpm/Node que `frontend-ci.yml`. **Aucune étape de déploiement, aucun secret
  de déploiement.**
- Ce workflow ne couvre pas les push sur `main` (puisque `frontend-ci.yml`,
  désactivé, les couvrait côté build) : un échec de test uniquement visible
  après merge sur `main` n'est pas détecté avant GOU-251. Les PR restent donc
  la seule ligne de défense automatique pour `frontend/**` en attendant.
- `.github/workflows/b2b-frontend-ci.yml` (`B2B Frontend CI`) est `active` et
  exécute déjà `pnpm test` sur les PR touchant `b2b-frontend/**` : pas de trou
  équivalent côté B2B, aucune action nécessaire.
- `.github/workflows/ci-pr.yml` ignore `frontend/**` (`paths-ignore`) ; il
  continue de couvrir le build Maven sur toute autre modification de PR.

## Pour réactiver le déploiement beta (GOU-251)

Réactiver `frontend-ci.yml` et `testAndPublishBeta.yml` (ou leur
remplacement) reste conditionné à la résolution du déclenchement non voulu
sur force-push identifié par GOU-249. Ne pas réactiver ces workflows sans
l'accord explicite porté par GOU-251.
