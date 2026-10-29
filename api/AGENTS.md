# Api Agents Guide

> **Parent Guide**: [Root AGENTS.md](../AGENTS.md)  
> This guide **extends** the root conventions with API-specific rules.

This module is part of the open4goods multi-module Maven project.

## Technology

- Java 21
- Spring Boot 4

## Directory structure

- `src/main/java` - REST API and batch code
- `src/main/resources` - configuration
- `src/test/java` - unit tests

## Purpose

The `api` module exposes the REST endpoints and orchestrates data ingestion pipelines.

## Build and test this module only

From this directory:

```bash
mvn --offline clean install
```

Run only the tests with:

```bash
mvn --offline test
```

From the repository root you can also execute:

```bash
mvn --offline -pl api -am clean install
```

## Sécurité et accès

`config/WebSecurityConfig` (GOU-1393) : api.nudger.fr est "pur SSO Google" pour les personnes.

- Machines : clé API brute dans l'en-tête ou le paramètre `Authorization` (`admin-key` → `ROLE_USER` +
  `XWIKIADMINGROUP`, `test-keys` → `ROLE_USER` + `ROLE_TESTER`). Inchangé.
- Personnes : `oauth2Login` Google, câblé seulement si la registration `google` existe
  (`spring.security.oauth2.client.registration.google.*`, rendue depuis `GOOGLE_SSO_CLIENT_ID/SECRET`). E-mail
  vérifié et présent dans `api.security.google-sso.role-allowlist` (`ApiOidcUserService`), sinon 403. Pas de
  formulaire, pas de page `/login`, pas d'utilisateur `user` généré.
- HTTP Basic : uniquement sur `/actuator/**` (chaîne dédiée), pour l'identité moniteur Spring Boot Admin
  `o4g.actuator.monitor.*` (`ActuatorMonitorAuthenticationProvider`, même classe que ui et front-api).
- Anonyme : navigation navigateur (`Accept: text/html`) → redirection vers `/oauth2/authorization/google` ; tout
  autre appel → 401 sans `WWW-Authenticate` (pas de popup, pas de session).

Preuve : `ApiGoogleSsoAccessTest` (contexte complet servi par Tomcat, y compris `redirect_uri` derrière
`X-Forwarded-Proto`/`X-Forwarded-Host` avec `forward-headers-strategy: NATIVE`). Doc :
[Google SSO OAuth clients](../docs/operations/google-sso-oauth-clients.md#role-allowlist-api).

## Sauvegarde produits

`BackupService.backupProducts()` écrit `products-backup-{N}.gz` dans `api.backup.data-backup-folder`, puis
`products-backup-manifest.json` (champ optionnel `sha256` : nom → empreinte) et enfin `products-backup.sha256` au
format `sha256sum` (`<hex>  <fichier>`, écrit atomiquement, en dernier, en `rw-r--r--` comme les `.gz` et le manifest pour rester lisible par le compte de tirage `backup-pull` : sa présence signale une génération complète).
Les empreintes sont calculées pendant l'écriture (`DigestOutputStream`), jamais en relisant les fichiers. Vérification :
`cd <dossier> && sha256sum -c products-backup.sha256`. Le health check signale `product_backup_checksum_missing` si le
manifest déclare des empreintes mais que le `.sha256` manque ; il ne recalcule rien.

Déclenchement : `checkProductsBackup()` (`@Scheduled`, `api.backup.products-backup-check-initial-delay` défaut `PT15M`,
`api.backup.products-backup-check-interval` défaut `PT1H`) lance `backupProducts()` si aucun export ne tourne et si
`products-backup-manifest.json` est absent, illisible ou plus ancien que `api.backup.products-backup-interval-hours`
(défaut 168). Un redémarrage de l'api ne repousse donc plus la sauvegarde. La décision est loguée en `info`.
`POST /backup/products/export` force toujours la sauvegarde. Le profil local ne lance rien (stub `LocalDevConfig`).
À la première vérification, si aucun export ne tourne, `.tmp/` est vidé (volume libéré logué) ; les fichiers publiés,
le manifest et le `.sha256` ne sont jamais touchés. Pas de reprise d'une sauvegarde interrompue.

## Nettoyage des ressources orphelines

`POST /batch/clean-resources?mode=DRY_RUN|MOVE|DELETE` répond `202` immédiatement et exécute le balayage en tâche de
fond (un seul à la fois ; un second appel pendant un run renvoie l'état courant). `GET /batch/clean-resources` donne
l'état (`IDLE`, `RUNNING`, `COMPLETED` avec le rapport, `FAILED`). Les clés actives sont stockées par empreinte 64 bits
dans un `long[]` trié (`ActiveKeySet`, ~8 o/clé) : une collision ne peut que conserver un fichier, jamais classer un
fichier référencé comme orphelin. Si le tas ne suffit pas, le run s'abandonne (`aborted`, aucun fichier touché).
