# Packs événementiels – localisation & assets (frontend)

Ce document décrit comment les packs événementiels pilotent à la fois les assets (parallaxes, visuels) et les textes localisés de la page d’accueil. Les noms de packs (`default`, `christmas`, `sdg`, …) sont partagés avec les ressources graphiques et héritent toujours de `default` quand une clé est absente.

## Structure i18n

Toutes les chaînes dépendantes d’un pack vivent sous `home.events.<pack>.*`. La branche `default` contient les valeurs de base. Exemple (extrait) :

```jsonc
{
  "home": {
    "events": {
      "default": {
        "hero": {
          "eyebrow": "Responsable",
          "title": "Acheter mieux. Sans dépenser plus.",
          "titleSubtitle": ["Acheter mieux. Sans dépenser plus."],
          "subtitles": [
            "Gagne du temps. Choisis librement.",
            "Consomme mieux sans payer plus.",
          ],
          "search": {
            "label": "Tu sais déjà ce que tu cherches ?",
            "placeholder": "Recherchez un produit ou une catégorie",
            "helpersTitle": "Offre avec intention. Compare avec impact.",
            "helpers": [
              {
                "icon": "🌿",
                "segments": [
                  {
                    "text": "Une évaluation écologique",
                    "to": "/impact-score",
                  },
                ],
              },
            ],
            "partnerLinkLabel": "{formattedCount} partenaire | {formattedCount} partenaires",
            "partnerLinkFallback": "nos partenaires",
          },
          "context": {
            "ariaLabel": "Carte contexte du héros présentant la promesse Nudger",
          },
          "iconAlt": "Icône du lanceur de l'application Nudger",
          "imageAlt": "Illustration du comparateur Nudger...",
        },
      },
      "christmas": {
        "hero": {
          "titleSubtitle": ["Des idées cadeaux qui respectent tes valeurs."],
          "subtitles": [
            "Offre avec intention : compare prix et impact avant d'emballer.",
          ],
        },
      },
    },
  },
}
```

Champs surchargés par pack (liste ouverte) :

- `hero.title`, `hero.eyebrow`, `hero.titleSubtitle`, `hero.subtitles`
- `hero.search.*` (label, placeholder, aria, CTA, `helpersTitle`, `helpers`, chaînes de lien partenaire)
- `hero.context.*`
- `hero.iconAlt`, `hero.imageAlt`

## Règles de fallback

1. Chercher `home.events.<packActif>.<chemin>`.
2. Revenir à `home.events.default.<chemin>`.
3. Facultatif : clés de secours passées en option (les anciennes `home.hero.*` sont conservées en dernier recours).

## Tirage aléatoire au rendu

Les listes (`hero.subtitles`, `hero.titleSubtitle`, etc.) sont tirées **à chaque rendu**. Les seeds sont stockées dans `useState('event-pack-variant-seeds')` avec une clé déterministe pour rester cohérent entre SSR et client pour une même vue.

- `resolveStringVariant(path, { stateKey })` choisit une entrée dans une liste.
- `resolveList(path)` récupère les tableaux (ex. helpers).

## Consommation côté code

Utilisez `useEventPackI18n(packName)` :

```ts
const activePack = useSeasonalEventPack()
const packI18n = useEventPackI18n(activePack)

const heroTitle = computed(() =>
  packI18n.resolveString('hero.title', { fallbackKeys: ['home.hero.title'] })
)

const heroSubtitle = computed(() =>
  packI18n.resolveStringVariant('hero.subtitles', {
    stateKey: 'home-hero-subtitles',
    fallbackKeys: ['home.hero.subtitles'],
  })
)

const helpers = computed(() =>
  packI18n.resolveList('hero.search.helpers', {
    fallbackKeys: ['home.hero.search.helpers'],
  })
)
```

API disponible :

- `resolveString(path, { fallbackKeys? })` → string | undefined
- `resolveStringVariant(path, { stateKey?, randomize?, fallbackKeys? })` → string | undefined (gère les listes)
- `resolveList<T>(path, { fallbackKeys? })` → `T[]`

## Alignement avec les assets

Les noms de pack sont identiques à ceux des assets dans `config/theme/assets.ts` (`eventParallaxPacks`). Sélectionnez le pack actif via `useSeasonalEventPack`, puis réutilisez-le pour les assets (`useThemedParallaxBackgrounds`) et les textes (`useEventPackI18n`) afin de garder visuels et contenus synchronisés.
