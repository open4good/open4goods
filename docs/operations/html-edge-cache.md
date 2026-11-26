# Cache du HTML public à la périphérie (Cloudflare)

Le frontend SSR (Nuxt) publie depuis GOU-2700 un `Cache-Control` sur chaque page rendue. Ce document décrit
ce contrat d'origine et la règle Cloudflare attendue pour l'activer (lot H, GOU-2701). Tant que la règle
n'existe pas, Cloudflare ne met pas le HTML en cache (`cf-cache-status: DYNAMIC`) et ces en-têtes sont sans
effet.

## Contrat de l'origine

Le hook Nitro `render:response` (`frontend/server/plugins/html-cache-control.ts`, logique dans
`frontend/server/utils/html-cache-control.ts`) choisit l'en-tête de chaque page HTML :

| Cas | `Cache-Control` |
| --- | --- |
| GET ou HEAD anonyme, statut 200, aucun appel interne en échec | `public, max-age=0, s-maxage=600, stale-while-revalidate=3600` |
| Cookie `access_token` ou `refresh_token` présent (connecté) | `private, no-store` |
| Cookie `open4goods-preferred-theme=dark` ou `hotjar_record` présent | `private, no-store` |
| La réponse pose un cookie (`Set-Cookie`) | `private, no-store` |
| Page déclarée par visiteur : `definePageMeta({ htmlCache: false })` (connexion, retour de partage, Metriks) | `private, no-store` |
| Statut différent de 200, ou appel interne en 5xx ou en erreur réseau pendant le rendu (page peut-être dégradée) | `no-store` |

Pour une page publique, deux en-têtes s'ajoutent :

- `Cloudflare-CDN-Cache-Control: max-age=600, stale-while-revalidate=3600`. Cloudflare ignore
  `stale-while-revalidate` quand `s-maxage` est présent : cet en-tête, qui lui est réservé, porte le même TTL
  sans `s-maxage`.
- `Vary: Host, X-Forwarded-Host, CF-Device-Type, User-Agent, Cookie`, pour les autres caches : Cloudflare
  ignore `Vary`, la règle ci-dessous porte les mêmes partitions.

`max-age=0` fait revalider le navigateur à chaque visite : après une connexion, il ne réutilise jamais la
page anonyme.

Les TTL se règlent sans build : `NUXT_HTML_CACHE_S_MAXAGE` et `NUXT_HTML_CACHE_STALE_WHILE_REVALIDATE`
(secondes) dans l'environnement du service `frontend-ssr`.

Les pages d'erreur Nuxt (404, 500) gardent leur `cache-control: no-cache`.

### Jeton CSRF à la demande

Le serveur ne pose plus le cookie `csrf_token` (GOU-2700) :

- pendant le SSR, les POST internes (recherche de produits) sont signés avec un jeton propre au rendu, qui
  reste dans `event.context` ;
- dans le navigateur, le client `$fetch` (`frontend/app/plugins/csrf-fetch.ts`) crée le cookie juste avant
  le premier appel non sûr (contact, feedback, recherche) ;
- la vérification ne change pas : en-tête `x-csrf-token` égal au cookie, et même origine.

## Ce qui varie d'une requête à l'autre dans le SSR

Relevé fait pour GOU-2700 sur les pages publiques. Chaque variation est soit rendue côté client, soit
couverte par la clé de cache ou par un contournement du cache :

| Variation | Où | Prise en compte |
| --- | --- | --- |
| Langue et textes | `app/plugins/i18n-hostname.ts` : locale choisie par `Host` / `X-Forwarded-Host`. Ni `Accept-Language`, ni cookie i18n | Clé de cache (l'hôte en fait partie) |
| Mise en page mobile ou bureau de la page catégorie | `CategoryPage.vue`, `shared/utils/ssr-device.ts` : `CF-Device-Type` d'abord, puis le User-Agent | Clé de cache par type d'appareil |
| État connecté : menu du compte, section admin et audit de la fiche, filtres admin, catégories désactivées visibles, assistant du blog, verticales désactivées du NudgeTool | cookie `access_token` lu par `app/plugins/auth-init.server.ts` | Contournement (cookie) et `private, no-store` |
| Thème sombre | cookie `open4goods-preferred-theme` lu au SSR (`app/plugins/vuetify-theme.ts`). Sans cookie ou en `light`, le rendu est le même (clair) | Contournement si `=dark` |
| Pastille "enregistrement" du pied de page | cookie `hotjar_record` | Contournement |
| Requête de recherche | paramètre `q` de `/search` | Clé de cache (chaîne de requête) |
| Distance de fabrication (GeoIP) | `useManufacturingDistance`, appel dans `onMounted` | Côté client |
| Quotas IP | `useIpQuota`, garde `import.meta.client` | Côté client |
| IP affichée dans la carte de confidentialité | `AccountPrivacyCard.vue`, dans un `v-menu` chargé à l'ouverture | Côté client |
| Tiroir mobile, scanner de code-barres | `useDevice()` dans `<ClientOnly>` | Côté client |
| Accueil mobile ou bureau | les deux mises en page sont rendues et basculées en CSS (GOU-2458) | Sans objet |
| Variante d'aplat de l'accueil, icône animée | `Math.random` au SSR | Acceptée : figée pendant le TTL, cosmétique |
| Année du pied de page, dates relatives | `new Date()` au SSR | Acceptée : dépend du temps, pas du visiteur |

Les appels SSR vers front-api n'envoient que le jeton machine et la langue de l'hôte : ils répondent de la
même façon pour tous les visiteurs (sauf `manufacturing-distance`, appelé côté client).

## Règle Cloudflare attendue (lot H)

Une **Cache Rule** sur les deux zones (domaines français et anglais), qui rend le HTML public
"Eligible for cache".

Expression (à adapter aux noms d'hôte) :

```text
(http.request.method in {"GET" "HEAD"})
and not starts_with(http.request.uri.path, "/api/")
and not starts_with(http.request.uri.path, "/auth/")
and not starts_with(http.request.uri.path, "/_nuxt/")
and not starts_with(http.request.uri.path, "/metriks")
and not http.cookie contains "access_token="
and not http.cookie contains "refresh_token="
and not http.cookie contains "open4goods-preferred-theme=dark"
and not http.cookie contains "hotjar_record="
```

Réglages :

- **Cache eligibility** : Eligible for cache.
- **Edge TTL** : "Use cache-control header if present, bypass cache if not" : l'origine décide, et
  une réponse `private`, `no-store` ou avec `Set-Cookie` n'est jamais stockée.
- **Browser TTL** : respecter l'origine.
- **Cache key** : activer "Cache by device type" (tous les plans). Cloudflare envoie alors `CF-Device-Type`
  (`mobile`, `tablet`, `desktop`) à l'origine, qui rend la page catégorie d'après cette valeur : la page
  stockée correspond toujours à sa clé. Garder la chaîne de requête dans la clé.
- **Serve stale content while revalidating** : laisser activé.

Le contournement par cookie se fait dans l'expression : sur les plans hors Enterprise, la clé de cache ne
peut pas inclure de cookie. Une requête de connecté ne correspond donc pas à la règle, et Cloudflare ne lui
sert jamais une page stockée.

Nginx transmet `Cache-Control`, `Cloudflare-CDN-Cache-Control` et `Vary` tels quels (comportement par
défaut de `proxy_pass`). Il ne doit pas ajouter de `Set-Cookie` sur les pages.

### Déploiements

Une page stockée peut vivre jusqu'à 70 minutes (`600 s` de fraîcheur et `3 600 s` de `stale-while-revalidate`).
Elle référence les chunks `/_nuxt/` de son build : le déploiement en garde deux générations
(`frontend-asset-hardening.md`). Si plusieurs déploiements tombent dans cet intervalle, purger le cache HTML
de la zone après le déploiement.

### Vérification après activation

```bash
# Anonyme : MISS puis HIT, sans Set-Cookie
curl -sI https://nudger.fr/<fiche> | grep -iE 'cf-cache-status|cache-control|set-cookie'
curl -sI https://nudger.fr/<fiche> | grep -iE 'cf-cache-status'

# Connecté : jamais servi depuis le cache
curl -sI -H 'Cookie: access_token=x' https://nudger.fr/<fiche> | grep -iE 'cf-cache-status|cache-control'

# Mobile et bureau : deux entrées distinctes
curl -sI -A 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)' https://nudger.fr/<catégorie> | grep -i cf-cache-status
```

Attendu : `HIT` au second appel anonyme ; `BYPASS` ou `DYNAMIC` avec un cookie de connexion ; `UPDATING`
puis `REVALIDATED` après expiration du TTL. Si `UPDATING` n'apparaît jamais, Cloudflare ne lit pas
`Cloudflare-CDN-Cache-Control` : signaler le cas à Dev avant d'allonger le TTL.
