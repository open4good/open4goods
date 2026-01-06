# Packs Evenementiels - Guide Complet

Ce document explique comment configurer et personnaliser les packs evenementiels (themes saisonniers) dans Nudger.

---

## Vue d'ensemble

Les packs evenementiels permettent de personnaliser l'apparence et les textes de Nudger selon des periodes specifiques (14 juillet, Noel, campagne ODD, etc.). Le systeme supporte :

- Activation automatique par date
- Test via parametre URL (`?event=xxx` ou `?theme=xxx`)
- Surcharge des textes via i18n
- Surcharge des images (hero, parallax)

---

## 1. Configuration de la periode

### Fichier de configuration

```
frontend/config/theme/event-packs.ts
```

### Declarer un nouveau pack

**Etape 1 : Ajouter le nom du pack**

```typescript
export const EVENT_PACK_NAMES = [
  'default',
  'sdg',
  'bastille-day',
  'christmas', // <-- Nouveau pack
  'hold',
] as const
```

**Etape 2 : Definir la periode d'activation**

```typescript
export const eventPackSchedule: EventPackSchedule[] = [
  {
    id: 'christmas', // Identifiant unique
    start: '12-15', // Date de debut (MM-DD)
    end: '12-31', // Date de fin (MM-DD)
    pack: 'christmas', // Nom du pack a activer
    description: 'Periode de Noel',
  },
]
```

### Format des dates

- Format : `MM-DD` (mois-jour) en **UTC**
- Les fenetres peuvent chevaucher le changement d'annee :
  - Exemple : `start: '12-01'`, `end: '01-15'` couvre du 1er decembre au 15 janvier

---

## 2. Configuration des textes

**Tous les textes peuvent etre modifies en surchargeant les cles i18n.**

### Fichiers de localisation

```
frontend/i18n/locales/fr-FR.json
frontend/i18n/locales/en-US.json
```

### Structure des cles

Les textes sont a la **racine** du fichier JSON sous la cle `packs` :

```json
{
  "packs": {
    "default": {
      "hero": { ... }
    },
    "christmas": {
      "hero": { ... }
    }
  }
}
```

### Surcharger une cle

Definissez uniquement les cles a personnaliser. Les cles non definies heritent automatiquement de `packs.default`.

```json
{
  "packs": {
    "christmas": {
      "hero": {
        "eyebrow": "Special Noel",
        "title": "Des cadeaux responsables sous le sapin",
        "subtitles": [
          "Offrez mieux, depensez moins.",
          "Des idees cadeaux durables."
        ]
      }
    }
  }
}
```

### Cles disponibles

| Cle                        | Description                                       |
| -------------------------- | ------------------------------------------------- |
| `hero.eyebrow`             | Texte court au-dessus du titre                    |
| `hero.title`               | Titre principal                                   |
| `hero.titleSubtitle`       | Sous-titre (tableau, rotation aleatoire)          |
| `hero.subtitles`           | Messages d'accroche (tableau, rotation aleatoire) |
| `hero.iconAlt`             | Description icone (accessibilite)                 |
| `hero.imageAlt`            | Description image hero (accessibilite)            |
| `hero.search.label`        | Label du champ de recherche                       |
| `hero.search.placeholder`  | Placeholder du champ                              |
| `hero.search.helpersTitle` | Titre des points forts                            |
| `hero.search.helpers`      | Liste des points forts avec icones                |
| `parallax.*`               | Chemins des images parallax                       |
| `assets.*`                 | Chemins des images hero/illustration              |

### Template de reference

```
frontend/docs/templates/pack-example.i18n.json
```

---

## 3. Configuration des images

### Ressources minimales a creer

TODO : Montrer qu'on peut surcharger les images background des PageHeader (montrer page produit, page recherche)

Pour un nouveau pack, creez **au minimum** les fichiers suivants :

| Ressource               | Chemin relatif                                              | Description               |
| ----------------------- | ----------------------------------------------------------- | ------------------------- |
| **Hero background**     | `common/{pack}/hero-background.svg`                         | Fond de la section hero   |
| **Parallax essentials** | `common/parallax/parallax-background-{pack}-essentials.svg` | Section "problemes"       |
| **Parallax features**   | `common/parallax/parallax-background-{pack}-features.svg`   | Section "fonctionnalites" |
| **Parallax blog**       | `common/parallax/parallax-background-{pack}-blog.svg`       | Section "blog"            |
| **Parallax objections** | `common/parallax/parallax-background-{pack}-objections.svg` | Section "objections"      |
| **Parallax cta**        | `common/parallax/parallax-background-{pack}-cta.svg`        | Section "call-to-action"  |

**Optionnel :**

| Ressource          | Chemin                           | Description                |
| ------------------ | -------------------------------- | -------------------------- |
| Illustration       | `common/{pack}/illustration.svg` | Illustration personnalisee |
| Header backgrounds | `common/{pack}/header-*.svg`     | Fonds pages internes       |

### Emplacement des fichiers

```
frontend/app/assets/themes/
  ├── common/                    # Assets partages (recommande)
  │   ├── {votre-pack}/          # Dossier du pack
  │   │   ├── hero-background.svg
  │   │   └── illustration.svg
  │   └── parallax/              # Fonds parallax
  │       ├── parallax-background-{pack}-essentials.svg
  │       ├── parallax-background-{pack}-features.svg
  │       ├── parallax-background-{pack}-blog.svg
  │       ├── parallax-background-{pack}-objections.svg
  │       └── parallax-background-{pack}-cta.svg
  ├── light/                     # Assets theme clair
  └── dark/                      # Assets theme sombre
```

### Methode 1 : Configuration via i18n (recommandee)

Definissez les chemins dans vos fichiers i18n :

```json
{
  "packs": {
    "christmas": {
      "parallax": {
        "essentials": "common/parallax/parallax-background-christmas-essentials.svg",
        "features": "common/parallax/parallax-background-christmas-features.svg",
        "blog": "common/parallax/parallax-background-christmas-blog.svg",
        "objections": "common/parallax/parallax-background-christmas-objections.svg",
        "cta": "common/parallax/parallax-background-christmas-cta.svg"
      }
    }
  }
}
```

### Methode 2 : Configuration TypeScript

Modifiez `config/theme/assets.ts` :

```typescript
// Pour le hero et l'illustration
export const seasonalThemeAssets: SeasonalThemeAssets = {
  christmas: {
    common: {
      heroBackground: 'christmas/hero-background.svg',
      illustration: 'christmas/illustration.svg',
    },
  },
}

// Pour les parallax
export const eventParallaxPacks = {
  common: {
    christmas: {
      essentials: ['parallax/parallax-background-christmas-essentials.svg'],
      features: ['parallax/parallax-background-christmas-features.svg'],
      blog: ['parallax/parallax-background-christmas-blog.svg'],
      objections: ['parallax/parallax-background-christmas-objections.svg'],
      cta: ['parallax/parallax-background-christmas-cta.svg'],
    },
  },
}
```

### Priorite de resolution

1. Valeur definie dans i18n (`packs.{pack}.parallax.*`)
2. Valeur definie dans TypeScript (`eventParallaxPacks`)
3. Fallback vers `packs.default`

---

## 4. Generation des images parallax

### Prompt de generation

Un prompt optimise pour generer des fonds parallax avec un LLM generateur d'images est disponible :

```
frontend/prompts/prompt-background-parallax-light.prompt
```

### Specifications techniques

| Propriete             | Valeur                            |
| --------------------- | --------------------------------- |
| Format                | SVG vectoriel pur (pas de bitmap) |
| Dimensions            | `1600x800` (viewBox)              |
| preserveAspectRatio   | `xMidYMid slice`                  |
| Zone centrale libre   | ~1080x560 px (pour le contenu)    |
| Nombre d'elements max | < 140                             |
| Epaisseur traits min  | >= 0.75px                         |
| Flou max              | stdDev <= 18                      |

### Palette de couleurs Nudger

Le prompt definit la palette :

```json
{
  "bg_start": "#00DE9F",
  "bg_mid": "#00A1C2",
  "bg_end": "#0088D6",
  "accent_1": "#FF8479",
  "accent_2": "#5BDB3B",
  "neutral_text": "#004A63",
  "surface": "#F4F4F4"
}
```

### Structure du prompt

Le prompt definit :

- **Fond en gradient** doux (25-30 degres)
- **Zone securisee centrale** pour que le contenu reste lisible
- **3 couches parallax** avec profondeurs differentes :
  - `parallax-back` (profondeur 0.10)
  - `parallax-mid` (profondeur 0.18)
  - `parallax-front` (profondeur 0.28)
- **Contraintes SVG** : pas d'images, pas de texte, pas de bitmap

### Utilisation

1. Copiez le contenu du prompt
2. Adaptez les couleurs pour votre evenement si necessaire
3. Generez une image **par section** :
   - `essentials` : elements evoquant des defis/problemes
   - `features` : elements dynamiques/solutions
   - `blog` : elements editoriaux/lecture
   - `objections` : elements de reassurance
   - `cta` : elements d'action/conclusion
4. Placez les SVG dans `app/assets/themes/common/parallax/`

### Exemple de generation

```bash
# Generer 5 variantes pour un pack "soldes"
# 1. Utiliser le prompt avec un generateur d'images (Midjourney, DALL-E, etc.)
# 2. Demander des variations pour chaque section
# 3. Exporter en SVG
# 4. Placer dans common/parallax/parallax-background-soldes-*.svg
```

---

## 5. Test d'un pack

### Parametre URL

Pour tester un pack sans attendre sa periode d'activation :

```
https://nudger.fr?event=christmas
https://nudger.fr?event=bastille-day
```

**Parametre legacy (retrocompatible) :**

```
https://nudger.fr?theme=christmas
```

### Validation

Seuls les noms declares dans `EVENT_PACK_NAMES` sont acceptes. Les valeurs invalides sont ignorees.

### En developpement

```bash
# Demarrer le serveur
pnpm dev

# Ouvrir avec le pack de test
open "http://localhost:3000?event=christmas"
```

### Debuggage

Dans Vue DevTools, inspectez :

- `useSeasonalEventPack` : nom du pack actif
- `useEventPackI18n` : cles i18n resolues
- `useThemedParallaxBackgrounds` : assets parallax charges

---

## 6. Checklist - Creer un nouveau pack

### Etapes

- [ ] **1. Declarer le pack** (`config/theme/event-packs.ts`)
  - Ajouter le nom dans `EVENT_PACK_NAMES`
  - Ajouter la periode dans `eventPackSchedule`

- [ ] **2. Creer les textes i18n**
  - `i18n/locales/fr-FR.json` : ajouter `packs.{pack}`
  - `i18n/locales/en-US.json` : ajouter `packs.{pack}`

- [ ] **3. Creer les images**
  - Hero : `app/assets/themes/common/{pack}/hero-background.svg`
  - 5 parallax : `app/assets/themes/common/parallax/parallax-background-{pack}-*.svg`

- [ ] **4. Configurer les chemins** (via i18n ou TypeScript)

- [ ] **5. Tester** avec `?event={pack}`

### Exemple complet : Pack "Black Friday"

```bash
# 1. Modifier config/theme/event-packs.ts
# Ajouter 'black-friday' dans EVENT_PACK_NAMES et eventPackSchedule

# 2. Creer les dossiers
mkdir -p frontend/app/assets/themes/common/black-friday

# 3. Generer/placer les images
# - common/black-friday/hero-background.svg
# - common/parallax/parallax-background-black-friday-*.svg

# 4. Ajouter les textes dans fr-FR.json et en-US.json
# Sous packs.black-friday.hero.*

# 5. Tester
pnpm dev
# Ouvrir http://localhost:3000?event=black-friday
```

---

## Fichiers cles

| Fichier                                           | Role                    |
| ------------------------------------------------- | ----------------------- |
| `config/theme/event-packs.ts`                     | Packs et periodes       |
| `config/theme/assets.ts`                          | Images (TypeScript)     |
| `i18n/locales/*.json`                             | Textes et images (i18n) |
| `composables/useSeasonalEventPack.ts`             | Resolution pack actif   |
| `composables/useEventPackI18n.ts`                 | Resolution textes       |
| `composables/useThemedParallaxBackgrounds.ts`     | Resolution parallax     |
| `composables/useThemedAsset.ts`                   | Resolution assets       |
| `prompts/prompt-background-parallax-light.prompt` | Prompt generation       |
| `docs/templates/pack-example.i18n.json`           | Template pack           |

---

## Support

- Documentation anglaise : `docs/event-packs.md`
- Structure assets : `app/assets/themes/README.md`
- Template : `docs/templates/pack-example.i18n.json`
