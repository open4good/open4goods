# Rapport d'Audit de Sécurité Frontend - Open4Goods

**Date:** 2025-12-23
**Scope:** Frontend Nuxt 3, Docker Compose, Infrastructure
**Auditeur:** Security Audit

---

## Synthèse Exécutive

Cet audit de sécurité a identifié **23 vulnérabilités** réparties sur plusieurs niveaux de criticité. Les problèmes les plus critiques concernent l'infrastructure Docker (Elasticsearch sans authentification), des faiblesses dans la gestion des cookies et des potentielles vulnérabilités XSS dans certains composants CMS.

### Score de Risque Global: **MOYEN-ÉLEVÉ**

| Criticité | Nombre | Pourcentage |
|-----------|--------|-------------|
| CRITIQUE  | 4      | 17%         |
| HAUTE     | 6      | 26%         |
| MOYENNE   | 8      | 35%         |
| BASSE     | 5      | 22%         |

---

## Vulnérabilités Critiques (P1)

### CRIT-01: Elasticsearch sans authentification
**Fichier:** `docker-compose.yml:16`
```yaml
- xpack.security.enabled=false
```
**Impact:** Un attaquant peut accéder, modifier ou supprimer toutes les données Elasticsearch sans authentification.
**CVSS Score:** 9.8 (Critical)
**Recommandation:** Activer `xpack.security.enabled=true` et configurer l'authentification avec des utilisateurs/rôles appropriés.

### CRIT-02: Token machine codé en dur par défaut
**Fichier:** `nuxt.config.ts:465`
```typescript
machineToken: process.env.MACHINE_TOKEN || 'CHANGE_ME_SHARED_TOKEN',
```
**Impact:** Si la variable d'environnement n'est pas définie, le token par défaut est utilisé, permettant un accès non autorisé aux APIs backend.
**CVSS Score:** 9.1 (Critical)
**Recommandation:** Ne jamais utiliser de valeur par défaut. Lever une exception si `MACHINE_TOKEN` n'est pas défini en production.

### CRIT-03: Mots de passe MySQL/XWiki hardcodés
**Fichier:** `docker-compose.xwiki.yml:48-51`
```yaml
- MYSQL_ROOT_PASSWORD=xwiki
- MYSQL_USER=xwiki
- MYSQL_PASSWORD=xwiki
```
**Impact:** Les mots de passe en clair dans le code source exposent la base de données à des accès non autorisés.
**CVSS Score:** 9.0 (Critical)
**Recommandation:** Utiliser Docker secrets ou des variables d'environnement externalisées. Ne jamais committer des mots de passe.

### CRIT-04: Kibana exposé sans authentification
**Fichier:** `docker-compose.yml:67`
```yaml
ports:
  - "5601:5601"
```
**Impact:** Kibana est accessible publiquement sans authentification, exposant toutes les données Elasticsearch.
**CVSS Score:** 8.6 (High)
**Recommandation:** Configurer X-Pack Security pour Kibana ou utiliser un reverse proxy avec authentification.

---

## Vulnérabilités Hautes (P2)

### HIGH-01: XSS potentielle dans les composants CMS
**Fichiers affectés:**
- `app/components/domains/content/TextContent.vue:87`
- `app/components/cms/XwikiFullPageRenderer.vue:99`

```vue
<div class="xwiki-sandbox" v-html="htmlContent" />
```
**Impact:** Le contenu HTML provenant de XWiki est affiché sans sanitization côté client. Si le backend XWiki est compromis ou mal configuré, des scripts malveillants peuvent être injectés.
**CVSS Score:** 7.5 (High)
**Recommandation:** Appliquer DOMPurify.sanitize() au contenu HTML avant affichage, comme c'est déjà fait dans `ProductAiReviewSection.vue` et `TheArticle.vue`.

### HIGH-02: Cookie SameSite=None en production
**Fichier:** `server/routes/auth/login.post.ts:27`
```typescript
const sameSite: 'lax' | 'none' = secure ? 'none' : 'lax'
```
**Impact:** `SameSite=None` rend les cookies vulnérables aux attaques CSRF cross-site. Cela ne devrait être utilisé que si nécessaire pour le cross-origin.
**CVSS Score:** 6.5 (Medium)
**Recommandation:** Utiliser `SameSite=Lax` ou `SameSite=Strict` sauf si le cross-origin est absolument nécessaire.

### HIGH-03: Absence de rate limiting sur les endpoints d'authentification
**Fichiers:**
- `server/routes/auth/login.post.ts`
- `server/routes/auth/refresh.post.ts`

**Impact:** Sans rate limiting, les endpoints sont vulnérables aux attaques par force brute.
**CVSS Score:** 7.3 (High)
**Recommandation:** Implémenter un rate limiter (ex: `h3-rate-limit`) sur les endpoints d'authentification.

### HIGH-04: Absence de validation CSRF
**Observation:** Aucun token CSRF n'est implémenté pour les formulaires POST.
**Impact:** Les requêtes POST peuvent être forgées depuis des sites malveillants.
**CVSS Score:** 6.8 (Medium)
**Recommandation:** Implémenter des tokens CSRF pour tous les formulaires POST critiques.

### HIGH-05: Headers de sécurité non appliqués au runtime
**Fichier:** `app/public/_headers`
```
Content-Security-Policy: default-src 'self'...
```
**Impact:** Les headers dans `_headers` sont pour les déploiements statiques (Netlify/Cloudflare Pages). En mode SSR/Node, ils ne sont pas appliqués automatiquement.
**CVSS Score:** 5.3 (Medium)
**Recommandation:** Configurer les headers de sécurité dans le serveur Nitro via `nitro.routeRules` ou un middleware.

### HIGH-06: Endpoint admin sans vérification d'autorisation
**Fichier:** `server/api/admin/cache/reset.post.ts`
**Observation:** L'endpoint `/api/admin/cache/reset` ne vérifie pas les rôles/permissions de l'utilisateur.
**Impact:** N'importe quel utilisateur authentifié pourrait réinitialiser le cache.
**CVSS Score:** 6.5 (Medium)
**Recommandation:** Ajouter une vérification de rôle admin avant d'exécuter l'action.

---

## Vulnérabilités Moyennes (P3)

### MED-01: Logging d'informations sensibles
**Fichiers:**
- `server/routes/auth/login.post.ts:71`
- `server/routes/auth/logout.post.ts:56`

```typescript
console.error('Login fetch error', fetchErrorDetails)
```
**Impact:** Les détails d'erreur incluent potentiellement des informations sensibles qui sont loggées.
**Recommandation:** Masquer les données sensibles dans les logs. Utiliser un logger structuré.

### MED-02: Absence de Content-Security-Policy strict
**Fichier:** `app/public/_headers:3`
```
Content-Security-Policy: default-src 'self'; ... style-src 'self' 'unsafe-inline'
```
**Impact:** `unsafe-inline` pour les styles permet l'injection de styles malveillants.
**Recommandation:** Utiliser des nonces ou des hashes pour les styles inline.

### MED-03: JWT décodé côté client sans vérification de signature
**Fichier:** `shared/api-client/services/auth.services.ts:31`
```typescript
const decoded = jwtDecode<JwtPayload>(tokenValue)
```
**Impact:** `jwt-decode` ne vérifie pas la signature. Si le token est manipulé, le client affichera des données incorrectes.
**Recommandation:** La vérification de signature doit toujours être faite côté serveur. Documenter cette limitation.

### MED-04: Exposition des ports Elasticsearch
**Fichier:** `docker-compose.yml:33`
```yaml
ports:
  - "9200:9200"
```
**Impact:** Elasticsearch est exposé sur le réseau, potentiellement accessible depuis l'extérieur.
**Recommandation:** Ne pas exposer le port 9200 directement. Utiliser un réseau Docker interne uniquement.

### MED-05: Absence de validation des URL de redirection
**Fichier:** `app/pages/contrib/[token].vue:125`
```typescript
const redirectUrl = computed(() => data.value?.location ?? null)
```
**Impact:** Si l'URL de redirection n'est pas validée côté serveur, cela pourrait permettre des redirections ouvertes.
**Recommandation:** Valider que l'URL de redirection est dans une whitelist de domaines autorisés.

### MED-06: v-html multiple sans documentation claire
**20 fichiers** utilisent `v-html` selon l'audit.
**Impact:** Chaque utilisation de v-html est un vecteur potentiel de XSS si le contenu n'est pas sanitizé.
**Recommandation:** Documenter et auditer chaque utilisation. Créer un composant wrapper qui applique automatiquement DOMPurify.

### MED-07: DevTools activé en développement
**Fichier:** `nuxt.config.ts:246`
```typescript
enabled: process.env.NODE_ENV !== 'production',
```
**Impact:** Correct, mais vérifier que NODE_ENV est bien défini en production.
**Recommandation:** Ajouter une vérification explicite au démarrage.

### MED-08: Absence de politique de mots de passe forte
**Observation:** Le login accepte n'importe quel format de mot de passe.
**Impact:** Des mots de passe faibles peuvent être utilisés.
**Recommandation:** Implémenter une validation côté client et serveur.

---

## Vulnérabilités Basses (P4)

### LOW-01: Cookies de préférence sans flag Secure
**Fichier:** `app/plugins/vuetify-theme.ts:55`
```typescript
sameSite: 'lax',
// Pas de `secure: true`
```
**Recommandation:** Ajouter `secure: true` pour les cookies en production.

### LOW-02: Dépendances potentiellement vulnérables
**Observation:** Les dépendances ne sont pas auditées régulièrement.
**Recommandation:** Exécuter `pnpm audit` régulièrement et configurer Dependabot/Renovate.

### LOW-03: Absence de Subresource Integrity (SRI)
**Observation:** Les ressources externes (CDN) ne sont pas vérifiées avec SRI.
**Recommandation:** Ajouter des hashes d'intégrité pour les ressources CDN.

### LOW-04: Images avec attribut src dynamique
**Fichier:** `app/components/product/ProductAiReviewSection.vue:278`
```vue
:src="source.favicon"
```
**Impact:** Les URLs d'images externes pourraient être utilisées pour le tracking ou le phishing.
**Recommandation:** Valider les URLs d'images et les proxifier si possible.

### LOW-05: Version Docker explicite sans mise à jour automatique
**Fichier:** `docker-compose.yml:7`
```yaml
image: docker.elastic.co/elasticsearch/elasticsearch:9.2.3
```
**Recommandation:** Mettre en place un processus de mise à jour des images Docker.

---

## Bonnes Pratiques Observées

1. **Cookies HttpOnly** : Les tokens JWT sont stockés dans des cookies `httpOnly: true` (login.post.ts:29)
2. **Sanitization DOMPurify** : Utilisé correctement dans `ProductAiReviewSection.vue` et `TheArticle.vue`
3. **hCaptcha** : Implémenté sur les formulaires de contact et feedback
4. **Validation d'entrée** : Les endpoints valident les entrées (longueur min/max, regex email)
5. **Encodage des paramètres URL** : `encodeURIComponent()` utilisé pour les IDs de page
6. **Error handling** : Les erreurs backend sont correctement gérées et loggées

---

## Plan d'Action Recommandé

### Phase 1 - Immédiat (1-2 jours)
1. [ ] Activer l'authentification Elasticsearch
2. [ ] Externaliser les mots de passe de la base de données
3. [ ] Supprimer le token machine par défaut
4. [ ] Restreindre l'accès à Kibana

### Phase 2 - Court terme (1 semaine)
1. [ ] Ajouter rate limiting aux endpoints d'authentification
2. [ ] Configurer les headers de sécurité dans Nitro
3. [ ] Auditer et sanitizer tous les v-html
4. [ ] Ajouter la vérification de rôle sur les endpoints admin

### Phase 3 - Moyen terme (2-4 semaines)
1. [ ] Implémenter CSRF tokens
2. [ ] Configurer SameSite=Lax pour tous les cookies
3. [ ] Mettre en place l'audit automatique des dépendances
4. [ ] Créer un composant SafeHtml wrapper

### Phase 4 - Long terme (1-3 mois)
1. [ ] Implémenter CSP strict avec nonces
2. [ ] Configurer SRI pour les ressources externes
3. [ ] Mettre en place un WAF (Web Application Firewall)
4. [ ] Effectuer un test de pénétration externe

---

## Méthodologie d'Audit

Cet audit a été réalisé en analysant :
- Configuration Nuxt (`nuxt.config.ts`)
- Routes API serveur (`server/api/**/*`, `server/routes/**/*`)
- Composants Vue avec rendu HTML (`v-html`)
- Gestion de l'authentification
- Configuration Docker Compose
- Middleware et plugins
- Gestion des cookies et sessions

### Outils utilisés
- Analyse statique du code source
- Recherche de patterns de vulnérabilités (grep/ripgrep)
- Revue de configuration

---

## Annexe A: Fichiers Analysés

| Catégorie | Fichiers |
|-----------|----------|
| Configuration | `nuxt.config.ts`, `docker-compose*.yml`, `package.json` |
| Authentification | `server/routes/auth/*.ts`, `app/plugins/auth-*.ts`, `stores/useAuthStore.ts` |
| API Server | `server/api/**/*.ts` (35+ fichiers) |
| Composants v-html | 20 fichiers identifiés |
| Middleware | `app/middleware/enforce-locale-path.global.ts` |
| Services | `shared/api-client/services/*.ts` |

---

## Annexe B: Références OWASP

- [A01:2021 - Broken Access Control](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)
- [A02:2021 - Cryptographic Failures](https://owasp.org/Top10/A02_2021-Cryptographic_Failures/)
- [A03:2021 - Injection](https://owasp.org/Top10/A03_2021-Injection/)
- [A05:2021 - Security Misconfiguration](https://owasp.org/Top10/A05_2021-Security_Misconfiguration/)
- [A07:2021 - Cross-Site Scripting (XSS)](https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/)

---

*Rapport généré le 2025-12-23 par l'audit de sécurité automatisé.*
