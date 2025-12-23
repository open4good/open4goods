# Packs Evenementiels - Guide de Configuration

Ce document explique comment configurer et personnaliser les packs evenementiels (themes saisonniers) dans Nudger.

## Vue d'ensemble

Les packs evenementiels permettent de personnaliser l'apparence et les textes de Nudger selon des periodes specifiques (14 juillet, campagne ODD, etc.).

## Configuration de la periode

### Fichier de configuration

Les periodes d'activation des packs sont definies dans :

```
frontend/config/theme/event-packs.ts
```

### Structure de configuration

```typescript
export const eventPackSchedule: EventPackSchedule[] = [
  {
    id: 'sdg-campaign',          // Identifiant unique
    start: '04-15',              // Date de debut (MM-DD)
    end: '05-02',                // Date de fin (MM-DD)
    pack: 'sdg',                 // Nom du pack a activer
    description: 'Journee de la Terre et sensibilisation aux ODD',
  },
  {
    id: 'bastille-day',
    start: '07-10',
    end: '07-16',
    pack: 'bastille-day',
    description: 'Fete nationale du 14 juillet',
  },
]
```

### Format des dates

- Les dates sont au format `MM-DD` (mois-jour) en UTC
- Les fenetres peuvent chevaucher le changement d'annee (ex: `12-01` a `01-15`)

### Ajouter un nouveau pack

1. Ajouter le nom du pack dans `EVENT_PACK_NAMES` :

```typescript
export const EVENT_PACK_NAMES = [
  'default',
  'sdg',
  'bastille-day',
  'christmas',      // Nouveau pack
  'hold',
] as const
```

2. Ajouter la periode dans `eventPackSchedule` :

```typescript
{
  id: 'christmas',
  start: '12-15',
  end: '12-31',
  pack: 'christmas',
  description: 'Periode de Noel',
}
```

## Configuration des textes

### Structure i18n

Les textes des packs sont definis a la racine des fichiers de localisation :

```
frontend/i18n/locales/fr-FR.json
frontend/i18n/locales/en-US.json
```

### Organisation des cles

```json
{
  "packs": {
    "default": {
      "hero": {
        "eyebrow": "Comparateur responsable",
        "title": "Acheter mieux. Sans depenser plus.",
        "titleSubtitle": ["Acheter mieux. Sans depenser plus."],
        "subtitles": [
          "Gagne du temps. Choisis librement.",
          "Consomme mieux sans payer plus."
        ],
        "search": {
          "label": "Une marque ou un modele precis en tete ?",
          "placeholder": "Recherchez un produit",
          "helpers": [...]
        }
      }
    },
    "bastille-day": {
      "hero": {
        "eyebrow": "Special 14 juillet",
        "title": "Celebrez des choix responsables",
        "titleSubtitle": ["Un feu d'artifice de prix justes."],
        "subtitles": [
          "Achetez avec liberte, egalite, durabilite."
        ]
      }
    }
  }
}
```

### Surcharger une cle

Pour personnaliser les textes d'un pack, il suffit de definir la cle correspondante sous `packs.{nom-du-pack}`. Si une cle n'est pas definie pour un pack, le systeme utilise automatiquement la valeur de `packs.default`.

**Exemple de surcharge partielle :**

```json
{
  "packs": {
    "christmas": {
      "hero": {
        "eyebrow": "Special Noel",
        "title": "Des cadeaux responsables"
        // Les autres cles (subtitles, search, etc.)
        // utilisent les valeurs de packs.default
      }
    }
  }
}
```

### Cles disponibles

| Cle | Description |
|-----|-------------|
| `hero.eyebrow` | Texte au-dessus du titre |
| `hero.title` | Titre principal |
| `hero.titleSubtitle` | Sous-titre (tableau pour rotation) |
| `hero.subtitles` | Liste de sous-titres alternatifs |
| `hero.search.label` | Label du champ de recherche |
| `hero.search.placeholder` | Placeholder du champ |
| `hero.search.helpers` | Liste d'aide contextuelle |
| `parallax.*` | Chemins des images parallax |

## Configuration des images

### Emplacement des fichiers

Les images des packs sont stockees dans :

```
frontend/app/assets/themes/common/{nom-du-pack}/
```

**Exemple pour bastille-day :**

```
frontend/app/assets/themes/common/bastille-day/
  hero-background.svg
  illustration-fireworks.svg
```

### Configuration des assets

Les surcharges d'images sont definies dans :

```
frontend/config/theme/assets.ts
```

```typescript
export const seasonalThemeAssets: SeasonalThemeAssets = {
  'bastille-day': {
    light: {
      heroBackground: 'bastille-day/hero-background.svg',
      illustration: 'bastille-day/illustration-fireworks.svg',
    },
    dark: {},
    common: {
      heroBackground: 'bastille-day/hero-background.svg',
    },
  },
}
```

### Images parallax

Les fonds parallax peuvent etre definis de deux manieres :

1. **Via la configuration TypeScript** (`assets.ts`) :

```typescript
export const eventParallaxPacks = {
  light: {
    'bastille-day': {
      essentials: ['parallax/parallax-background-bastille-essentials.svg'],
      features: ['parallax/parallax-background-bastille-features.svg'],
    },
  },
}
```

2. **Via les ressources i18n** (prioritaire) :

```json
{
  "packs": {
    "bastille-day": {
      "parallax": {
        "essentials": "parallax/parallax-background-bastille-essentials.svg",
        "features": "parallax/parallax-background-bastille-features.svg"
      }
    }
  }
}
```

## Test d'un pack

### Parametre URL

Pour tester un pack sans attendre sa periode d'activation, utilisez le parametre URL `event` :

```
https://nudger.fr?event=bastille-day
https://nudger.fr?event=sdg
https://nudger.fr?event=christmas
```

**Parametre legacy (retrocompatibilite) :**

```
https://nudger.fr?theme=bastille-day
```

### Validation

Seuls les noms de packs declares dans `EVENT_PACK_NAMES` sont acceptes. Les valeurs invalides sont ignorees et le pack par defaut est utilise.

### En developpement

```bash
# Demarrer le serveur de dev
pnpm dev

# Tester le pack bastille-day
# Ouvrir http://localhost:3000?event=bastille-day
```

## Chaine de resolution

Le systeme utilise une chaine de resolution pour determiner les valeurs a afficher :

1. **Texte i18n** : `packs.{packActif}.{chemin}`
2. **Fallback default** : `packs.default.{chemin}`
3. **Fallback legacy** : `home.hero.{chemin}` (retrocompatibilite)

## Fichiers cles

| Fichier | Role |
|---------|------|
| `config/theme/event-packs.ts` | Configuration des packs et periodes |
| `config/theme/assets.ts` | Configuration des images |
| `i18n/locales/fr-FR.json` | Textes francais (cle `packs`) |
| `i18n/locales/en-US.json` | Textes anglais (cle `packs`) |
| `composables/useSeasonalEventPack.ts` | Resolution du pack actif |
| `composables/useEventPackI18n.ts` | Resolution des textes i18n |

## Exemples complets

### Creer un pack "Black Friday"

1. **Declarer le pack** (`config/theme/event-packs.ts`) :

```typescript
export const EVENT_PACK_NAMES = [
  'default', 'sdg', 'bastille-day', 'hold',
  'black-friday',  // Ajouter
] as const

// Ajouter la periode
{
  id: 'black-friday',
  start: '11-20',
  end: '11-30',
  pack: 'black-friday',
  description: 'Black Friday et Cyber Monday',
}
```

2. **Ajouter les textes** (`i18n/locales/fr-FR.json`) :

```json
{
  "packs": {
    "black-friday": {
      "hero": {
        "eyebrow": "Black Friday responsable",
        "title": "Les bonnes affaires, sans mauvaise conscience",
        "subtitles": [
          "Profitez des promos sur des produits durables.",
          "Le bon prix pour le bon produit."
        ]
      }
    }
  }
}
```

3. **Ajouter les images** (optionnel) :

```
frontend/app/assets/themes/common/black-friday/
  hero-background.svg
```

4. **Tester** :

```
http://localhost:3000?event=black-friday
```
