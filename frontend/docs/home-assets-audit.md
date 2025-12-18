# Audit des fonds et assets d’accueil (frontend)

Ce document recense l’état actuel des visuels de la page d’accueil et propose un plan de remise à plat avant régénération des SVG (light/dark et variantes saisonnières).

## Cartographie rapide
- **Hero** : résolu via `useHeroBackgroundAsset` (`config/theme/assets.ts`), fichiers `app/assets/themes/light/hero-background.webp` (light) et fallback `app/assets/themes/common/hero-background.svg` pour le dark. Les références manuelles inexistantes ont été retirées de `app/pages/index.vue`.
- **Parallax packs** : définis dans `config/theme/assets.ts` et résolus par `useThemedParallaxBackgrounds`.
  - Light : `parallax/parallax-background-{1..3}.svg` (pack default) et `parallax-background-bubbles-*.svg` (pack christmas). Variantes transparentes pour les bubbles présentes **uniquement** en light.
  - Dark : uniquement `parallax-background-{1..3}.svg` et `parallax-background-bubbles-*.svg` (pas de versions « transparent »).
  - Common : `hero-background.svg`, `illustration-generic.svg`, `backgrounds/texture-grid.svg`.
- **Aplats** : `/app/public/images/home/parallax-aplats.svg` utilisé via `ParallaxSection` (`enableAplats`).
- **Composants** : `HomeSolutionSection.vue` et `HomeFeaturesSection.vue` sont désormais agnostiques du background (parallax géré par `app/pages/index.vue`).

## Lacunes constatées
1) Packs saisonniers incomplets : pack `sdg` vide, pack `christmas` dépend de fichiers bubbles non symétriques (transparent seulement en light).
2) Tailles hétérogènes : mélange de SVG 1440×800 et 1600×800 avec `height="800"` explicite, provoquant des bandes/blancs sur grands écrans malgré `preserveAspectRatio="xMidYMid slice"`.

## Recommandations de taille (SVG)
- **Hero & parallax plein écran** : viewBox `1600x900` (ou `1920x1080`), `width="100%" height="100%"`, `preserveAspectRatio="xMidYMid slice"`, contenu utile centré avec 10–15% de marge de sécurité pour le parallaxe.
- **Illustrations/placeholder** : viewBox ~`1200x900` max, toujours en SVG si possible.
- Garder le poids <200 Ko par fond après optimisation (SVGO), éviter les filtres SVG lourds ; privilégier dégradés et patterns.
- Harmoniser l’opacité pour matcher les overlays du composant `ParallaxSection` (`overlayOpacity` ~0.35–0.6 selon le thème).

## Plan d’action proposé
1) **Aligner le hero** : rester sur `useHeroBackgroundAsset` + `config/theme/assets.ts`; ajouter une variante dark dédiée si besoin pour dépasser le fallback commun.
2) **Compléter les packs** : remplir le pack `sdg` avec une déclinaison par section (light/dark) et ajouter les versions « transparent » côté dark pour le pack `christmas` afin de garder un rendu cohérent.
3) **Régénérer les SVG** : appliquer le gabarit 1600×900 (ou 1920×1080) à tous les fichiers `app/assets/themes/{light,dark}/parallax/*.svg`, en gardant la palette décrite dans les commentaires existants (tokens `hero-gradient-*`, `accent-primary-highlight`, `surface-*`).
4) **Placeholders de validation** : produire 2–3 SVG exemples (light, dark, Noël) montrant l’effet parallaxe et la répartition des aplats, à stocker dans `app/assets/themes/common/` ou `light/` le temps de la régénération complète.
5) **Tests visuels** : valider le rendu avec `ParallaxSection.vue` aux breakpoints mobiles/tablettes/desktop (largeur <960 px désactive le parallaxe) pour vérifier l’absence d’artefacts de bord.

## Points d’attention
- Les overlays (`overlayOpacity`) varient par section dans `app/pages/index.vue` ; conserver des couleurs de fond suffisamment contrastées pour éviter le « wash out » en light comme en dark.
- Le fallback d’assets suit l’ordre `theme -> common -> THEME_ASSETS_FALLBACK` ; garder des noms symétriques entre thèmes pour faciliter la régénération automatisée.
- Les packs saisonniers sont sélectionnés par `config/theme/seasons.ts` (fenêtres de dates UTC) — toute nouvelle variante doit y être référencée.
