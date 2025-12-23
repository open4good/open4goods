# Principe de Surcharge i18n

Ce document explique le mecanisme de surcharge des ressources i18n pour les packs evenementiels.

---

## Vue d'ensemble

Le systeme de packs evenementiels permet de surcharger **n'importe quelle cle i18n** sans avoir a redefinir l'ensemble du fichier de localisation. Le mecanisme utilise une **chaine de fallback simplifiee a 2 niveaux**.

---

## Chaine de resolution

Quand une cle est demandee (ex: `hero.title`), le systeme cherche dans cet ordre :

```
1. packs.{packActif}.{path}     →  packs.bastille-day.hero.title
2. {path}                       →  home.hero.title (RACINE)
```

**Des qu'une cle est trouvee, la recherche s'arrete.**

> **Note** : Il n'y a PAS de niveau `packs.default`. Les valeurs par defaut sont definies directement a la racine du fichier i18n (ex: `home.hero.*`).

---

## Exemples concrets

### Exemple 1 : Cle definie dans le pack

```json
{
  "packs": {
    "bastille-day": {
      "hero": {
        "title": "Celebrez le 14 juillet !"
      }
    }
  },
  "home": {
    "hero": {
      "title": "Acheter mieux. Sans depenser plus."
    }
  }
}
```

Demande : `hero.title` avec pack `bastille-day`
- ✓ Trouve `packs.bastille-day.hero.title`
- Resultat : `"Celebrez le 14 juillet !"`

---

### Exemple 2 : Cle non definie dans le pack

```json
{
  "packs": {
    "bastille-day": {
      "hero": {
        "eyebrow": "Special 14 juillet"
      }
    }
  },
  "home": {
    "hero": {
      "title": "Acheter mieux. Sans depenser plus."
    }
  }
}
```

Demande : `hero.title` avec pack `bastille-day`
- ✗ `packs.bastille-day.hero.title` n'existe pas
- ✓ Trouve `home.hero.title` (via fallbackKeys vers la racine)
- Resultat : `"Acheter mieux. Sans depenser plus."`

---

## Implications pratiques

### Ce que vous devez definir

**Dans `packs.{votre-pack}`** : Uniquement les cles que vous voulez surcharger

**A la racine** : Toutes les valeurs par defaut (navigation, footer, pages, hero, etc.)

### Ce que vous n'avez PAS besoin de faire

- Creer un `packs.default`
- Copier toutes les cles dans chaque pack
- Redefinir les cles communes a chaque evenement
- Gerer manuellement les fallbacks

---

## Schema visuel

```
┌─────────────────────────────────────────────────────────────┐
│                     Fichier i18n                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  packs:                                                     │
│    └── bastille-day:     ◄── 1. Cherche ici d'abord        │
│          └── hero.title: "14 juillet"                       │
│                                                             │
│  home:                   ◄── 2. Puis a la racine           │
│    └── hero:                                                │
│          └── title: "Acheter mieux"                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Utilisation dans le code

### Composable `useEventPackI18n`

```typescript
const activeEventPack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activeEventPack)

// Resolution automatique avec fallback vers la racine
const title = packI18n.resolveString('hero.title', {
  fallbackKeys: ['home.hero.title']
})

// Pour les tableaux avec selection aleatoire
const subtitle = packI18n.resolveStringVariant('hero.subtitles', {
  stateKey: 'home-hero-subtitles',
  fallbackKeys: ['home.hero.subtitles']
})
```

### Options disponibles

| Option | Type | Description |
|--------|------|-------------|
| `fallbackKeys` | `string[]` | Cles additionnelles a essayer apres le pack (racine) |

---

## Bonnes pratiques

### 1. Definir le minimum dans les packs

```json
{
  "packs": {
    "christmas": {
      "hero": {
        "eyebrow": "Special Noel",
        "title": "Des cadeaux responsables"
      }
    }
  }
}
```

Les autres cles (`search`, `helpers`, etc.) viendront automatiquement de la racine.

### 2. Garder la racine complete

```json
{
  "home": {
    "hero": {
      "eyebrow": "Comparateur responsable",
      "title": "Acheter mieux. Sans depenser plus.",
      "titleSubtitle": ["..."],
      "subtitles": ["...", "..."],
      "search": { ... }
    }
  }
}
```

---

## Debug

Pour verifier quelle cle est resolue :

```typescript
// Dans la console Vue DevTools
const packI18n = useEventPackI18n(useSeasonalEventPack())

// Voir la valeur brute
console.log(packI18n.resolveRaw('hero.title'))

// Voir le pack actif
console.log(packI18n.packKey.value)
```

---

## Resume

| Niveau | Cle | Quand utiliser |
|--------|-----|----------------|
| `packs.{pack}.*` | Surcharge specifique | Personnalisation evenementielle |
| Racine (`home.hero.*`, etc.) | Valeur par defaut | Toutes les autres cles |

**Principe cle** : Definissez uniquement ce qui change dans le pack. Le reste est herite de la racine.
