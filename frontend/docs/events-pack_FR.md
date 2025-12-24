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
      TODO : ajouter docs / exemples images parallax headers
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

TODO : PAreil, j m'attendais à voir des extensions
Définis sous `packs.<pack>.parallax`. Les clés correspondent aux sections de la page d'accueil :

- `essentials`
- `features`
- `blog`
- `objections`
- `cta`

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

```vue
<PageHeader background-image-asset-key="productBackground" ... />
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
