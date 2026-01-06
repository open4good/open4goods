# Packs Événementiels - Localisation & Assets (Frontend)

Ce document explique comment les packs événementiels pilotent à la fois les assets (parallaxes, visuels à thème) et **le contenu textuel localisé** de la page d'accueil. Les packs sont résolus par nom (ex: `default`, `hold`, `sdg`, `bastille-day`) via le paramètre URL `?event=` ou la date actuelle.

## Démarrage Rapide

### Tester un Pack manuellement

Forcer un pack spécifique via le paramètre de requête URL :

```
http://localhost:3000?event=bastille-day
http://localhost:3000?event=sdg
```

> **Note**: Seuls les noms de packs valides définis dans `EVENT_PACK_NAMES` sont acceptés.

### Ajouter un nouveau pack

1.  **Définir le pack** :
    Ajouter le nom du pack à `EVENT_PACK_NAMES` dans `frontend/config/theme/event-packs.ts`.

    ```typescript
    export const EVENT_PACK_NAMES = [
      'default',
      'sdg',
      'bastille-day',
      'christmas', // Nouveau pack
      'hold',
    ] as const
    ```

2.  **Le planifier (Optionnel)** :
    Ajouter la fenêtre d'activation dans `eventPackSchedule` dans le même fichier.

    ```typescript
    {
      id: 'christmas',
      start: '12-15', // MM-JJ
      end: '12-31',
      pack: 'christmas',
      description: 'Période de Noël',
    }
    ```

3.  **Ajouter contenu & assets** :
    Mettre à jour `frontend/i18n/locales/fr-FR.json` (et autres locales) avec la configuration du pack.

    ```json
    "packs": {
      "christmas": {
        "hero": {
          "title": "Joyeux Noël responsable",
          "subtitles": ["Offrez des cadeaux durables."]
        },
        "assets": {
          "heroBackground": "christmas/hero-background.svg",
          "problemImage": "christmas/problem.webp",
          "solutionImage": "christmas/solution.webp"
        },
        "parallax": {
          "essentials": "parallax/christmas-essentials.svg"
        }
      }
    }
    ```

## Architecture & Configuration

### Définitions des Packs

Les packs événementiels sont configurés dans :
`frontend/config/theme/event-packs.ts`

Ce fichier contient :

- `EVENT_PACK_NAMES`: Liste des noms de packs disponibles.
- `eventPackSchedule`: Fenêtres de dates pour l'activation automatique des packs.
- Fonctions de résolution (`resolveActiveEventPack`, `resolveEventPackName`).

### Structure I18n

Toutes les chaînes de caractères et chemins d'assets liés aux packs se trouvent sous `packs.<pack>.*` à la racine des fichiers de locale.

**Emplacement :** `frontend/i18n/locales/fr-FR.json`

**Structure :**

```jsonc
{
  "packs": {
    "default": {
      "hero": { ... },   // Contenu textuel par défaut
      "assets": { ... }, // Assets statiques par défaut
      "parallax": { ... } // Calques parallaxes par défaut
    },
    "bastille-day": {
      "hero": {
        "eyebrow": "Spécial 14 juillet",
        "title": "Célébrez des choix responsables"
      },
      "assets": {
        "heroBackground": "bastille-day/hero-background.svg"
      },
      "parallax": {
        "essentials": "parallax/parallax-background-bastille-essentials.svg"
      }
    }
  }
}
```

### Configuration des Assets

Les assets sont entièrement gérés via les clés i18n. Cela permet d'avoir des assets différents par locale si nécessaire, mais regroupe surtout toute la configuration du pack au même endroit.

#### Assets Statiques

Définis sous `packs.<pack>.assets`. Clés communes :

- `heroBackground`
- `illustration`
- `problemImage` (Image section Problèmes)
- `solutionImage` (Image section Solution)
- `logo`, `footerLogo`, `favicon` (surtout pour le pack `hold`)

#### Arrière-plans Parallaxes

Définis sous `packs.<pack>.parallax`. Les clés correspondent aux sections de la page d'accueil :

- `essentials`
- `features`
- `blog`
- `objections`
- `cta`

Vous pouvez fournir vos propres fichiers SVG pour chaque calque ou réutiliser les placeholders existants.

**Extension des Parallaxes** :
Le système de parallaxe supporte maintenant la fusion de calques multiples. Si vous souhaitez des effets plus complexes, vous pouvez définir des couches supplémentaires dans le code du composant, mais la configuration i18n reste simple : une clé par section principale.

**Emplacement des fichiers Assets :**
Placez vos fichiers SVG/image dans :
`frontend/app/assets/themes/`

Vous pouvez les organiser par thème ou pack, ex : `frontend/app/assets/themes/bastille-day/hero-background.svg`.
La valeur i18n doit être le chemin relatif depuis `assets/themes/`.

#### Arrière-plans des Page Headers

Les composants `PageHeader` peuvent utiliser des arrière-plans thémés via la prop `backgroundImageAssetKey`.
Ces assets sont définis de la même manière dans `packs.<pack>.assets`.

Exemples de clés standard :

- `productBackground` : Arrière-plan pour les pages produit
- `contactBackground` : Arrière-plan pour la page contact
- `blogBackground` : Arrière-plan pour la page liste des articles de blog
- `categoriesBackground` : Arrière-plan pour la page catégories

Usage dans le code :

<PageHeader background-image-asset-key="productBackground" ... />

````

#### Exemples de Page Headers

Voici les assets par défaut utilisés pour les headers de pages:

**Produit (`productBackground`)**
![Product Background](/app/assets/themes/placeholders/product-background.svg)

**Contact (`contactBackground`)**
![Contact Background](/app/assets/themes/placeholders/contact-background.svg)

**Blog (`blogBackground`)**
![Blog Background](/app/assets/themes/placeholders/blog-background.svg)

**Catégories (`categoriesBackground`)**
![Categories Background](/app/assets/themes/placeholders/category-background.svg)

## Guide Développeur

### Consommer les chaînes du Pack

Utiliser `useEventPackI18n(packName)` :

```typescript
const activePack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activePack)

// Résout packs.<activePack>.hero.title
// Se replie sur fallbackKeys fournis si non trouvé dans le pack
const heroTitle = computed(() =>
  packI18n.resolveString('hero.title', { fallbackKeys: ['home.hero.title'] })
)
````

### Randomisation au rendu

Les listes (ex: `hero.subtitles`) sont randomisées **par rendu** en utilisant une graine cohérente pour éviter les erreurs d'hydratation.

```typescript
const heroSubtitle = computed(() =>
  packI18n.resolveStringVariant('hero.subtitles', {
    stateKey: 'home-hero-subtitles',
    fallbackKeys: ['home.hero.subtitles'],
  })
)
```

### Résolution des Assets

Utiliser `useThemedAsset` ou `useThemedParallaxBackgrounds`. Ces composables recherchent automatiquement le bon chemin dans le fichier i18n pour le pack actif.

```typescript
const heroBackground = useHeroBackgroundAsset()
// Ou générique :
const myImage = useThemeAsset('problemImage')
```

## Exemples Exhaustifs de PageHeader Background Images

Le composant `PageHeader` supporte plusieurs types de backgrounds configurables via les props. Voici tous les cas d'usage possibles :

### 1. Background: Gradient (CSS Gradients)

Utilise des dégradés CSS définis par les variables de thème.

**Configuration i18n :** Aucune (géré par CSS)

**Utilisation :**

```vue
<PageHeader
  variant="hero-standard"
  background="gradient"
  title="Page avec dégradé"
  subtitle="Gradient géré par les variables CSS du thème"
/>
```

**Effet :** Dégradé automatique basé sur `--v-theme-hero-gradient-start` et `--v-theme-hero-gradient-end`.

---

### 2. Background: Image (Static Background Image)

Affiche une image statique en arrière-plan avec overlay.

**Configuration i18n :**

```json
{
  "packs": {
    "christmas": {
      "assets": {
        "heroBackground": "christmas/hero-background.svg"
      }
    }
  }
}
```

**Utilisation (via Asset Key) :**

```vue
<PageHeader
  variant="hero-standard"
  background="image"
  background-image-asset-key="heroBackground"
  overlay-opacity="0.65"
  title="Joyeux Noël responsable"
/>
```

**Utilisation (URL directe) :**

```vue
<PageHeader
  variant="hero-standard"
  background="image"
  background-image="/assets/themes/common/bastille-day/hero-background.svg"
  overlay-opacity="0.7"
  title="Célébrez le 14 juillet"
/>
```

**Utilisation (Light/Dark Mode) :**

```vue
<PageHeader
  variant="hero-standard"
  background="image"
  :background-image="{
    light: '/assets/themes/light/hero.svg',
    dark: '/assets/themes/dark/hero.svg',
  }"
  title="Image adaptée au thème"
/>
```

---

### 3. Background: Parallax (Multi-Layer Parallax Effect)

Utilise le composant `ParallaxWidget` avec plusieurs calques qui se déplacent à des vitesses différentes.

**Configuration i18n :**

```json
{
  "packs": {
    "bastille-day": {
      "parallax": {
        "essentials": "parallax/parallax-background-bastille-essentials.svg",
        "features": "parallax/parallax-background-bastille-features.svg",
        "blog": "parallax/parallax-background-bastille-blog.svg",
        "objections": "parallax/parallax-background-bastille-objections.svg",
        "cta": "parallax/parallax-background-bastille-cta.svg"
      }
    }
  }
}
```

**Utilisation :**

```vue
<PageHeader
  variant="hero-fullscreen"
  background="parallax"
  :is-parallax="true"
  :parallax-layers="[
    { src: '/parallax/layer-back.svg', speed: 0.3, blendMode: 'multiply' },
    { src: '/parallax/layer-mid.svg', speed: 0.6 },
    { src: '/parallax/layer-front.svg', speed: 1.0 },
  ]"
  :parallax-amount="0.18"
  :overlay-opacity="0.5"
  :enable-aplats="true"
  aplat-svg="/images/home/parallax-aplats.svg"
  title="Hero avec parallaxe"
/>
```

**Configuration Parallax Layers :**

- `src` : Chemin de l'image SVG du calque
- `speed` : Multiplicateur de vitesse (0.0 = statique, 1.0 = vitesse normale)
- `blendMode` : Mode de fusion CSS (`multiply`, `screen`, `overlay`, etc.)

---

### 4. Background: Surface Variant (Animated Gradient Surfaces)

Utilise le composant `HeroSurface` qui génère des effets visuels animés (aurora, halo, prism, etc.).

**Configuration i18n :** Aucune (géré par props)

**Variantes disponibles :**

- `aurora` : Aurore boréale animée
- `halo` : Halos lumineux concentriques
- `prism` : Effet prisme multicolore
- `pulse` : Pulsations lumineuses
- `mesh` : Grille animée en dégradé
- `orbit` : Cercles orbitaux

**Utilisation :**

```vue
<PageHeader
  variant="hero-standard"
  background="surface-variant"
  surface-variant="aurora"
  title="Hero avec effet Aurora"
/>
```

**Exemple avec différentes variantes :**

```vue
<!-- Aurora -->
<PageHeader background="surface-variant" surface-variant="aurora" />

<!-- Halo -->
<PageHeader background="surface-variant" surface-variant="halo" />

<!-- Prism -->
<PageHeader background="surface-variant" surface-variant="prism" />

<!-- Pulse -->
<PageHeader background="surface-variant" surface-variant="pulse" />

<!-- Mesh -->
<PageHeader background="surface-variant" surface-variant="mesh" />

<!-- Orbit (utilisé sur /partners) -->
<PageHeader background="surface-variant" surface-variant="orbit" />
```

---

### 5. Background: Solid (Couleur Unie)

Applique une couleur de fond unie personnalisée.

**Configuration i18n :** Aucune (géré par props)

**Utilisation :**

```vue
<PageHeader
  variant="section-header"
  background="solid"
  background-color="#1a237e"
  title="Section avec fond bleu"
/>
```

**Avec variable CSS :**

```vue
<PageHeader
  variant="section-header"
  background="solid"
  background-color="rgb(var(--v-theme-surface-default))"
  title="Fond basé sur le thème"
/>
```

---

## Tableau Récapitulatif des Types de Background

| Type                | Assets i18n               | Props                                                                         | Cas d'usage                                |
| ------------------- | ------------------------- | ----------------------------------------------------------------------------- | ------------------------------------------ |
| **gradient**        | ❌ Non                    | `background="gradient"`                                                       | Hero simple, design minimaliste            |
| **image**           | ✅ Oui (`heroBackground`) | `background="image"`<br/>`background-image-asset-key` ou `background-image`   | Pack événementiel avec visuel statique     |
| **parallax**        | ✅ Oui (`parallax.*`)     | `background="parallax"`<br/>`is-parallax`<br/>`parallax-layers`               | Page d'accueil immersive, storytelling     |
| **surface-variant** | ❌ Non                    | `background="surface-variant"`<br/>`surface-variant="aurora\|halo\|prism..."` | Pages statiques élégantes (partners, team) |
| **solid**           | ❌ Non                    | `background="solid"`<br/>`background-color`                                   | Sections internes, headers secondaires     |

---

## Exemples Complets par Pack Événementiel

### Pack "default" (Placeholder)

```vue
<PageHeader
  variant="hero-standard"
  background="image"
  background-image-asset-key="heroBackground"
  title="Open4Goods"
  subtitle="Des choix éclairés pour un monde durable"
/>
```

**Résolution asset :** `packs.default.assets.heroBackground` → `placeholders/hero-background.svg`

---

### Pack "hold" (Logo Nudger)

```vue
<PageHeader
  variant="hero-fullscreen"
  background="image"
  background-image-asset-key="heroBackground"
  overlay-opacity="0.7"
  title="Nudger"
  subtitle="Votre assistant d'achat responsable"
/>
```

**Résolution asset :** `packs.hold.assets.heroBackground` → `hero-background.webp`

---

### Pack "bastille-day" (14 juillet)

```vue
<PageHeader
  variant="hero-standard"
  background="image"
  background-image-asset-key="heroBackground"
  overlay-opacity="0.6"
  eyebrow="Spécial 14 juillet"
  title="Célébrez des choix responsables"
  subtitle="Un feu d'artifice de prix justes et d'impact transparent"
/>
```

**Résolution asset :** `packs.bastille-day.assets.heroBackground` → `bastille-day/hero-background.svg`

---

### Pack "sdg" (Objectifs de Développement Durable)

```vue
<PageHeader
  variant="hero-standard"
  background="parallax"
  :is-parallax="true"
  :parallax-layers="sdgParallaxLayers"
  :enable-aplats="true"
  title="Objectifs de Développement Durable"
  subtitle="Consommer en alignement avec les ODD"
/>
```

---

## Bonnes Pratiques

### Optimisation des Images

- **SVG** : Préféré pour les backgrounds (léger, vectoriel, responsive)
- **WebP** : Pour les photos/images complexes
- **Lazy Loading** : Activé automatiquement pour `background-image` (fetchpriority="high" désactivé)

### Accessibilité

- Toujours utiliser `overlay-opacity` pour garantir un contraste suffisant avec le texte
- Les images de background ont automatiquement `aria-hidden="true"`
- Fournir `alt=""` pour les images décoratives

### Performance

- **Gradient** : Plus performant (CSS pur, pas de requête réseau)
- **Surface Variant** : Animation GPU-accelerated, léger
- **Image** : 1 requête réseau, précharger si hero principal
- **Parallax** : Multiple requêtes, réserver aux pages d'accueil

### Responsive

Tous les types de background sont automatiquement responsive :

- Les images s'adaptent via `object-fit: cover`
- Les parallaxes ajustent leur amplitude sur mobile
- Les gradients et surfaces s'adaptent aux dimensions

---

## Résolution des Assets par Pack

Le système résout automatiquement les chemins d'assets selon le pack actif :

```typescript
// Pack actif = "bastille-day"
const bg = useThemeAsset('heroBackground')
// → Résout vers: /assets/themes/common/bastille-day/hero-background.svg

// Pack actif = "hold"
const bg = useThemeAsset('heroBackground')
// → Résout vers: /assets/themes/common/hold/hero-background.webp

// Pack actif = "default"
const bg = useThemeAsset('heroBackground')
// → Résout vers: /assets/themes/light/placeholders/hero-background.svg
```

**Hiérarchie de résolution :**

1. `packs.<activePack>.assets.<assetKey>`
2. `packs.default.assets.<assetKey>` (fallback)
3. Placeholder générique si non trouvé

## Placeholders générés

Un système de placeholders générés automatiquement est disponible pour garantir que chaque route dispose d'un visuel par défaut cohérent avec le thème.

Ces assets sont situés dans `assets/themes/common/placeholders/generated/` et portent le nom de la route ou du concept (ex: `contact.svg`, `opensource.svg`).

Ils sont configurés dans `fr-FR.json` pour être utilisés via les clés d'assets habituelles (`contactBackground`, etc.), permettant de les surcharger facilement par des packs événementiels si nécessaire.

## Parallax

Pour les pages `opensource` et `opendata`, un effet parallax à 3 couches est configuré. Les couches sont chargées via `useThemedAsset` pour permettre leur personnalisation par thème, mais utilisent par défaut des placeholders générés (`_layer1.svg`, `_layer2.svg`, `_layer3.svg`).

La configuration se fait dans le composant de page via la prop `parallaxLayers` du `PageHeader`.
