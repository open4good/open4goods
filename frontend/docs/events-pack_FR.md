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

Définis sous `packs.<pack>.parallax`. Les clés correspondent aux sections de la page d'accueil avec effet parallaxe :

| Clé Parallax | Section | Description |
|--------------|---------|-------------|
| `essentials` | Section Essentiels | Arrière-plan pour la section des fonctionnalités essentielles |
| `features` | Section Fonctionnalités | Arrière-plan pour la section des caractéristiques principales |
| `blog` | Section Blog | Arrière-plan pour la section articles/actualités |
| `objections` | Section Objections | Arrière-plan pour la section FAQ/objections |
| `cta` | Section CTA | Arrière-plan pour la section appel à l'action finale |

**Format des fichiers :**
Les arrière-plans parallaxes doivent être au format SVG pour une performance optimale et un redimensionnement fluide.

**Emplacement des fichiers Assets :**
Placez vos fichiers SVG/image dans :
`frontend/app/assets/themes/`

Organisation recommandée :
- Fichiers communs : `frontend/app/assets/themes/common/parallax/`
- Fichiers par pack : `frontend/app/assets/themes/common/[pack-name]/`
- Fichiers par thème : `frontend/app/assets/themes/[light|dark]/parallax/`

Exemples de chemins :
- `frontend/app/assets/themes/common/parallax/parallax-background-bastille-essentials.svg`
- `frontend/app/assets/themes/common/bastille-day/hero-background.svg`

**Important :** La valeur i18n doit être le chemin relatif depuis `assets/themes/`.

#### Arrière-plans des Page Headers

Les composants `PageHeader` peuvent utiliser des arrière-plans thémés via la prop `backgroundImageAssetKey`.
Ces assets sont définis de la même manière dans `packs.<pack>.assets`.

##### Clés standard d'arrière-plans

Les clés suivantes sont disponibles pour personnaliser les en-têtes de page par pack événementiel :

| Clé Asset | Page/Contexte | Description | Placeholder par défaut |
|-----------|---------------|-------------|------------------------|
| `productBackground` | Pages produit | Arrière-plan pour les fiches produit individuelles | `placeholders/product-background.svg` |
| `contactBackground` | Page contact | Arrière-plan pour le formulaire de contact | `placeholders/contact-background.svg` |
| `blogBackground` | Liste articles blog | Arrière-plan pour la page index du blog | `placeholders/blog-background.svg` |
| `categoriesBackground` | Page catégories | Arrière-plan pour la navigation des catégories | `placeholders/category-background.svg` |
| `heroBackground` | Page d'accueil | Arrière-plan principal du hero de la page d'accueil | `placeholders/hero-background.svg` |

##### Exemples d'utilisation dans les composants

**Utilisation basique :**

```vue
<PageHeader
  background="image"
  background-image-asset-key="productBackground"
  title="Mon produit"
  variant="hero-standard"
/>
```

**Avec overlay personnalisé :**

```vue
<PageHeader
  background="image"
  background-image-asset-key="blogBackground"
  overlay-opacity="0.75"
  title="Articles du blog"
  subtitle="Découvrez nos derniers articles"
  variant="hero-standard"
/>
```

**Avec surface variant (sans image) :**

```vue
<PageHeader
  background="surface-variant"
  surface-variant="aurora"
  title="Contactez-nous"
  variant="section-header"
/>
```

##### Exemples de configurations par pack événementiel

**Pack par défaut :**

```json
"packs": {
  "default": {
    "assets": {
      "productBackground": "placeholders/product-background.svg",
      "contactBackground": "placeholders/contact-background.svg",
      "blogBackground": "placeholders/blog-background.svg",
      "categoriesBackground": "placeholders/category-background.svg",
      "heroBackground": "placeholders/hero-background.svg"
    }
  }
}
```

**Pack événementiel Bastille Day :**

```json
"packs": {
  "bastille-day": {
    "assets": {
      "productBackground": "bastille-day/product-tricolor.svg",
      "contactBackground": "bastille-day/contact-fireworks.svg",
      "blogBackground": "bastille-day/blog-celebration.svg",
      "categoriesBackground": "bastille-day/categories-flag.svg",
      "heroBackground": "bastille-day/hero-background.svg",
      "illustration": "bastille-day/illustration-fireworks.svg"
    },
    "hero": {
      "eyebrow": "Spécial 14 juillet",
      "title": "Célébrez des choix responsables"
    }
  }
}
```

**Pack événementiel Noël :**

```json
"packs": {
  "christmas": {
    "assets": {
      "productBackground": "christmas/product-snow.svg",
      "contactBackground": "christmas/contact-winter.svg",
      "blogBackground": "christmas/blog-gifts.svg",
      "categoriesBackground": "christmas/categories-tree.svg",
      "heroBackground": "christmas/hero-background.svg"
    },
    "hero": {
      "eyebrow": "Saison des fêtes",
      "title": "Offrez responsable cette année",
      "subtitles": ["Des cadeaux durables pour un Noël écoresponsable."]
    }
  }
}
```

##### Exemples visuels des placeholders par défaut

Les placeholders par défaut sont des SVG simples et élégants :

- **`product-background.svg`** : Fond gris clair (#F3F4F6) avec cercles décoratifs subtils
- **`contact-background.svg`** : Fond vert menthe (#ECFDF5) avec vague géométrique
- **`blog-background.svg`** : Fond bleu ciel (#EFF6FF) avec rectangles arrondis en rotation
- **`category-background.svg`** : Fond orange pastel (#FFF7ED) avec cercles dispersés

Tous les placeholders ont une dimension standard de 1440x600px et utilisent une palette de couleurs cohérente avec le design system.

##### Tous les types de background disponibles pour PageHeader

Le composant `PageHeader` supporte 5 types d'arrière-plans différents via la prop `background` :

| Type | Valeur | Description | Exemple d'utilisation |
|------|--------|-------------|----------------------|
| **Gradient** | `gradient` | Dégradé CSS utilisant les variables de thème | Parfait pour les hero sections modernes |
| **Image** | `image` | Image statique avec overlay | Idéal pour les pages avec visuel fort |
| **Parallax** | `parallax` | Effet parallaxe avec plusieurs couches | Pour les sections immersives |
| **Solid** | `solid` | Couleur unie personnalisable | Pour un style minimaliste |
| **Surface Variant** | `surface-variant` | Surface animée (aurora, halo, prism, etc.) | Pour des effets visuels dynamiques |

**Exemples complets d'utilisation :**

```vue
<!-- 1. Background Gradient -->
<PageHeader
  variant="hero-fullscreen"
  background="gradient"
  title="Bienvenue"
  subtitle="Découvrez notre plateforme"
/>

<!-- 2. Background Image avec asset key -->
<PageHeader
  variant="hero-standard"
  background="image"
  background-image-asset-key="productBackground"
  overlay-opacity="0.7"
  title="Nos produits"
/>

<!-- 3. Background Image avec URL directe -->
<PageHeader
  variant="hero-standard"
  background="image"
  background-image="/images/custom-header.jpg"
  overlay-opacity="0.6"
  title="Page personnalisée"
/>

<!-- 4. Background Parallax -->
<PageHeader
  variant="hero-fullscreen"
  background="parallax"
  is-parallax
  :parallax-layers="[
    { src: 'parallax/layer-back.svg', speed: 0.5 },
    { src: 'parallax/layer-mid.svg', speed: 0.8 },
    { src: 'parallax/layer-front.svg', speed: 1.2 }
  ]"
  parallax-amount="0.18"
  title="Expérience immersive"
/>

<!-- 5. Background Solid -->
<PageHeader
  variant="section-header"
  background="solid"
  background-color="#1E3A8A"
  title="Section simple"
/>

<!-- 6. Background Surface Variant - Aurora -->
<PageHeader
  variant="hero-standard"
  background="surface-variant"
  surface-variant="aurora"
  title="Effet Aurora"
/>

<!-- 7. Background Surface Variant - Halo -->
<PageHeader
  variant="hero-standard"
  background="surface-variant"
  surface-variant="halo"
  title="Effet Halo"
/>

<!-- 8. Background Surface Variant - Prism -->
<PageHeader
  variant="hero-standard"
  background="surface-variant"
  surface-variant="prism"
  title="Effet Prism"
/>
```

##### Variantes de PageHeader disponibles

Le composant `PageHeader` propose 3 variantes principales via la prop `variant` :

| Variante | Hauteur | Usage typique |
|----------|---------|---------------|
| `hero-fullscreen` | 100dvh (plein écran) | Page d'accueil, landing pages importantes |
| `hero-standard` | Variable (clamp 3-5.5rem padding) | Pages principales, sections hero |
| `section-header` | Compact (clamp 2-3.5rem padding) | En-têtes de section, pages intérieures |

##### Surface Variants disponibles

Lorsque `background="surface-variant"`, vous pouvez choisir parmi 6 effets visuels :

| Variant | Effet visuel | Meilleur usage |
|---------|--------------|----------------|
| `aurora` | Dégradé animé type aurore boréale | Pages d'accueil, sections hero |
| `halo` | Effet de halo lumineux | Pages produit, mises en avant |
| `prism` | Effet prismatique multicolore | Pages créatives, événements |
| `pulse` | Pulsation lumineuse | Pages dynamiques, call-to-action |
| `mesh` | Grille de dégradé | Pages techniques, documentation |
| `orbit` | Effet orbital rotatif | Pages innovation, futuriste |

##### Récapitulatif des props importantes

```typescript
interface PageHeaderProps {
  // Variant (requis)
  variant: 'hero-fullscreen' | 'hero-standard' | 'section-header'

  // Background type
  background?: 'gradient' | 'image' | 'parallax' | 'solid' | 'surface-variant'

  // Pour background="image"
  backgroundImage?: string | { light: string; dark: string }
  backgroundImageAssetKey?: string  // Clé i18n (ex: 'productBackground')
  overlayOpacity?: number           // 0-1, défaut: 0.65

  // Pour background="surface-variant"
  surfaceVariant?: 'aurora' | 'halo' | 'prism' | 'pulse' | 'mesh' | 'orbit'

  // Pour background="parallax"
  isParallax?: boolean
  parallaxLayers?: Array<{ src: string; speed?: number; blendMode?: string }>
  parallaxAmount?: number           // Défaut: 0.18

  // Pour background="solid"
  backgroundColor?: string          // Couleur CSS

  // Contenu
  title: string                     // Requis
  subtitle?: string
  eyebrow?: string

  // Layout
  layout?: 'single-column' | '2-columns' | '3-columns'
  contentAlign?: 'start' | 'center'
}
```

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
```

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
