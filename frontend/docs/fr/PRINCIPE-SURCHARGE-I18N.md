# Principe de Surcharge i18n

Ce document explique le mecanisme de surcharge des ressources i18n pour les packs evenementiels.

---

## Vue d'ensemble

Le systeme de packs evenementiels permet de surcharger **n'importe quelle cle i18n** sans avoir a redefinir l'ensemble du fichier de localisation. Le mecanisme utilise une **chaine de fallback automatique** qui remonte vers la racine.

---

## Chaine de resolution

Quand une cle est demandee (ex: `hero.title`), le systeme cherche dans cet ordre :

```
1. packs.{packActif}.{path}     →  packs.bastille-day.hero.title
2. packs.default.{path}         →  packs.default.hero.title
3. {path}                       →  hero.title (racine)
4. fallbackKeys (optionnel)     →  cles de compatibilite
```

**Des qu'une cle est trouvee, la recherche s'arrete.**

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
  }
}
```

Demande : `hero.title` avec pack `bastille-day`
- ✓ Trouve `packs.bastille-day.hero.title`
- Resultat : `"Celebrez le 14 juillet !"`

---

### Exemple 2 : Cle non definie dans le pack, definie dans default

```json
{
  "packs": {
    "default": {
      "hero": {
        "title": "Acheter mieux. Sans depenser plus."
      }
    },
    "bastille-day": {
      "hero": {
        "eyebrow": "Special 14 juillet"
      }
    }
  }
}
```

Demande : `hero.title` avec pack `bastille-day`
- ✗ `packs.bastille-day.hero.title` n'existe pas
- ✓ Trouve `packs.default.hero.title`
- Resultat : `"Acheter mieux. Sans depenser plus."`

---

### Exemple 3 : Cle non definie dans les packs, definie a la racine

```json
{
  "packs": {
    "default": {
      "hero": {
        "eyebrow": "Comparateur responsable"
      }
    }
  },
  "home": {
    "hero": {
      "title": "Titre depuis la racine"
    }
  }
}
```

Demande : `home.hero.title` avec pack `bastille-day`
- ✗ `packs.bastille-day.home.hero.title` n'existe pas
- ✗ `packs.default.home.hero.title` n'existe pas
- ✓ Trouve `home.hero.title` (racine)
- Resultat : `"Titre depuis la racine"`

---

## Implications pratiques

### Ce que vous devez definir

**Dans `packs.default`** : Les valeurs par defaut pour les cles specifiques aux packs (hero, search, etc.)

**Dans `packs.{votre-pack}`** : Uniquement les cles que vous voulez surcharger

**A la racine** : Toutes les autres cles i18n (navigation, footer, pages, etc.)

### Ce que vous n'avez PAS besoin de faire

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
│    ├── bastille-day:     ◄── 1. Cherche ici d'abord        │
│    │     └── hero.title: "14 juillet"                       │
│    │                                                        │
│    └── default:          ◄── 2. Puis ici                   │
│          └── hero.title: "Acheter mieux"                    │
│                                                             │
│  home:                   ◄── 3. Puis a la racine           │
│    └── hero:                                                │
│          └── title: "Titre racine"                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Utilisation dans le code

### Composable `useEventPackI18n`

```typescript
const activeEventPack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activeEventPack)

// Resolution automatique avec fallback
const title = packI18n.resolveString('hero.title')

// Avec fallback explicite supplementaire (rare)
const subtitle = packI18n.resolveString('hero.subtitle', {
  fallbackKeys: ['home.hero.subtitle']
})

// Desactiver le fallback vers la racine (rare)
const strictValue = packI18n.resolveString('hero.custom', {
  noRootFallback: true
})
```

### Options disponibles

| Option | Type | Description |
|--------|------|-------------|
| `fallbackKeys` | `string[]` | Cles additionnelles a essayer apres la racine |
| `noRootFallback` | `boolean` | Desactiver le fallback vers la racine |

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

Les autres cles (`search`, `helpers`, etc.) viendront de `packs.default` ou de la racine.

### 2. Garder `packs.default` complet pour les cles hero

```json
{
  "packs": {
    "default": {
      "hero": {
        "eyebrow": "...",
        "title": "...",
        "titleSubtitle": ["..."],
        "subtitles": ["...", "..."],
        "search": { ... }
      }
    }
  }
}
```

### 3. Ne pas dupliquer les cles globales

Les cles comme `navigation.*`, `footer.*`, `product.*` restent a la racine et ne sont pas concernees par les packs.

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
| `packs.default.*` | Valeur par defaut | Cles communes aux packs |
| Racine (`*`) | Fallback automatique | Toutes les autres cles |

**Principe cle** : Definissez uniquement ce qui change. Le reste est herite automatiquement.
