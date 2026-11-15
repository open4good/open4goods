## 🥳 Welcome to **v1.0.81**

This release covers **4 day(s)** of work, from **2026-11-06T10:29:19.000Z** to **2026-11-10T14:01:59.000Z**.

### 📦 Stats
- Diff: [compare](https://github.com/open4good/open4goods/compare/v1.0.80...v1.0.81)
- Changed files: **300**
- Commits: **64**
- Additions: **6154** / Deletions: **11918**

---



<details>
<summary>🙈 Other changes (63)</summary>

| PR | Title | Author |
|---:|---|---|
| [PR 3545](https://github.com/open4good/open4goods/pull/3545) | fix(api): le partial update remplace le prix au lieu de le fusionner (GOU-1784) | @GoulvenF |
| [PR 3547](https://github.com/open4good/open4goods/pull/3547) | feat(ops): ops_readonly lit aussi affiliation-clicks (GOU-1830) | @GoulvenF |
| [PR 3546](https://github.com/open4good/open4goods/pull/3546) | fix(api): purge de datasource observable, rapport persistant et INTERRUPTED (GOU-1815) | @GoulvenF |
| [PR 3549](https://github.com/open4good/open4goods/pull/3549) | feat(metriks): contentPath des clics d'affiliation et événement top_origins.28d (GOU-1828) | @GoulvenF |
| [PR 3550](https://github.com/open4good/open4goods/pull/3550) | fix(api): l'api seule applique le mapping produits, après démarrage (GOU-1835) | @GoulvenF |
| [PR 3551](https://github.com/open4good/open4goods/pull/3551) | docs(ops): procédure de renouvellement du token Awin (GOU-1839) | @GoulvenF |
| [PR 3552](https://github.com/open4good/open4goods/pull/3552) | feat(verticals): rattacher les cuisinières à four électrique à oven (GOU-1840) | @GoulvenF |
| [PR 3553](https://github.com/open4good/open4goods/pull/3553) | fix(api): les fiches ISBN ne sont plus recréées chaque nuit, recherche sous l'id indexé (GOU-1841) | @GoulvenF |
| [PR 3554](https://github.com/open4good/open4goods/pull/3554) | fix(scoring): ImpactScore des climatiseurs, classements inversés et scores périmés (audit ImpactScore lot 0) | @GoulvenF |
| [PR 3548](https://github.com/open4good/open4goods/pull/3548) | feat(front): article d'origine des clics /contrib/ (GOU-1829) | @GoulvenF |
| [PR 3422](https://github.com/open4good/open4goods/pull/3422) | chore(deps): update dependency highlight.js to v11.12.0 | @renovate[bot] |
| [PR 3423](https://github.com/open4good/open4goods/pull/3423) | chore(deps): update gradle to v9.8.0 | @renovate[bot] |
| [PR 3557](https://github.com/open4good/open4goods/pull/3557) | fix(scoring): rangs de l'ImpactScore et des critères faux pour les produits extrêmes (GOU-1894) | @GoulvenF |
| [PR 3556](https://github.com/open4good/open4goods/pull/3556) | docs(impactscore): conception de l'ImpactScore V2, méthode versionnée par verticale (GOU-1878) | @GoulvenF |
| [PR 3558](https://github.com/open4good/open4goods/pull/3558) | feat(ops): cible infraSecrets es-agents-readonly (GOU-1870) | @GoulvenF |
| [PR 3555](https://github.com/open4good/open4goods/pull/3555) | fix(impactscore): vérifications restantes de l'audit (GOU-1897) | @GoulvenF |
| [PR 3560](https://github.com/open4good/open4goods/pull/3560) | docs(operations): lancement des JVM par jvm-launch.sh, unité nudger-jvm@ et heap dumps (GOU-1912) | @GoulvenF |
| [PR 3562](https://github.com/open4good/open4goods/pull/3562) | fix(compare): /comparer ne plante plus sans revue IA (GOU-1942) | @GoulvenF |
| [PR 3563](https://github.com/open4good/open4goods/pull/3563) | fix(search): tri par ImpactScore en 500, départage sur le GTIN (GOU-1940) | @GoulvenF |
| [PR 3561](https://github.com/open4good/open4goods/pull/3561) | fix(product): liens méthodologie de la fiche vers /{verticale}/ecoscore (GOU-1941) | @GoulvenF |
| [PR 3566](https://github.com/open4good/open4goods/pull/3566) | fix(ui): borne le décodage des images .webp à 4096 x 4096 px et 2 conversions (GOU-1952) | @GoulvenF |
| [PR 3568](https://github.com/open4good/open4goods/pull/3568) | feat(secrets): clé de datafeed Awin rendue dans le config tree de l'api (GOU-1958) | @GoulvenF |
| [PR 3570](https://github.com/open4good/open4goods/pull/3570) | feat(blog): titres, metas et contenu des 4 articles TV, four, lave-vaisselle, frigo (GOU-1977) | @GoulvenF |
| [PR 3569](https://github.com/open4good/open4goods/pull/3569) | feat(metriks): fenêtres prev7d / prev28d de l'affiliation (GOU-1975) | @GoulvenF |
| [PR 3571](https://github.com/open4good/open4goods/pull/3571) | feat(frontend): plausible-guard ignore les navigateurs automatisés (GOU-1979) | @GoulvenF |
| [PR 3573](https://github.com/open4good/open4goods/pull/3573) | docs(datasources): plans d'intégration des sources externes à GTIN/ISBN | @GoulvenF |
| [PR 3572](https://github.com/open4good/open4goods/pull/3572) | feat(blog): réécriture des allégations environnementales (GOU-1985) | @GoulvenF |
| [PR 3575](https://github.com/open4good/open4goods/pull/3575) | feat(impactscore): socle V2, version de méthode, méta d'évaluation, règle de version (GOU-1882) | @GoulvenF |
| [PR 3574](https://github.com/open4good/open4goods/pull/3574) | fix(frontend,verticals): retire les allégations environnementales génériques et « Faible » sans note (GOU-1983) | @GoulvenF |
| [PR 3578](https://github.com/open4good/open4goods/pull/3578) | chore(b2b): supprime b2b-api, b2b-frontend et leur outillage | @GoulvenF |
| [PR 3576](https://github.com/open4good/open4goods/pull/3576) | feat(frontend): retire la page /compensation-ecologique, 301 vers /impact-score (GOU-1993) | @GoulvenF |
| [PR 3581](https://github.com/open4good/open4goods/pull/3581) | feat(impactscore): matrices V2 sans DATA_QUALITY, indicateur de confiance (GOU-1880) | @GoulvenF |
| [PR 3580](https://github.com/open4good/open4goods/pull/3580) | feat(impactscore): filtre missing_eprel configurable par verticale (GOU-1881) | @GoulvenF |
| [PR 3583](https://github.com/open4good/open4goods/pull/3583) | fix(frontend): retire « comparateur écologique » et allégations restantes (GOU-1994) | @GoulvenF |
| [PR 3582](https://github.com/open4good/open4goods/pull/3582) | feat(frontend): rubrique « Classement et référencement » (art. D111-6 C. conso) (GOU-1995) | @GoulvenF |
| [PR 3577](https://github.com/open4good/open4goods/pull/3577) | feat(impactscore): contrôles automatiques après chaque batch et alerte (GOU-1895) | @GoulvenF |
| [PR 3579](https://github.com/open4good/open4goods/pull/3579) | feat(frontend): affichage ImpactScore V2, note, position, critères renseignés, Non noté (GOU-1885) | @GoulvenF |
| [PR 3586](https://github.com/open4good/open4goods/pull/3586) | feat(impactscore): page méthode par verticale (GOU-1901) | @GoulvenF |
| [PR 3585](https://github.com/open4good/open4goods/pull/3585) | feat: historique des versions Impact Score, nom unique, JSON-LD, sitemap (GOU-1903) | @GoulvenF |
| [PR 3587](https://github.com/open4good/open4goods/pull/3587) | feat(frontend): bloc « Pourquoi cette note ? » sur la fiche produit (GOU-1902) | @GoulvenF |
| [PR 3590](https://github.com/open4good/open4goods/pull/3590) | feat(impactscore): page méthode, liens localisés, marques, exclus, mise à jour (GOU-2008) | @GoulvenF |
| [PR 3584](https://github.com/open4good/open4goods/pull/3584) | feat(impactscore): instantané mensuel par verticale, index ES et endpoint de lecture (GOU-1896) | @GoulvenF |
| [PR 3589](https://github.com/open4good/open4goods/pull/3589) | feat(frontend): refonte de /impact-score en 5 blocs (GOU-1900) | @GoulvenF |
| [PR 3588](https://github.com/open4good/open4goods/pull/3588) | feat(impactscore): calcul V2, couverture réelle, redistribution des poids et « Non noté » sous le seuil (GOU-1879) | @GoulvenF |
| [PR 3591](https://github.com/open4good/open4goods/pull/3591) | fix(impactscore): carte Méthodologie vers /{verticale}/ecoscore (GOU-2011) | @GoulvenF |
| [PR 3592](https://github.com/open4good/open4goods/pull/3592) | docs(adr): ADR-0006, amendement affiliation sans régénération (GOU-2018) | @GoulvenF |
| [PR 3593](https://github.com/open4good/open4goods/pull/3593) | docs(impactscore): mesures du lot 1a, exclusions, couverture sans EPREL, correspondances EPREL (GOU-1875) | @GoulvenF |
| [PR 3594](https://github.com/open4good/open4goods/pull/3594) | fix(search): résultats de /rechercher rendus côté serveur + banc d'essai Google CSS | @GoulvenF |
| [PR 3595](https://github.com/open4good/open4goods/pull/3595) | feat(impactscore): attribut DURABILITY_INDEX, lecture marchande TV et lave-linge (GOU-1892) | @GoulvenF |
| [PR 3596](https://github.com/open4good/open4goods/pull/3596) | feat(impactscore): mapper les champs EPREL des smartphones (GOU-1891) | @GoulvenF |
| [PR 3599](https://github.com/open4good/open4goods/pull/3599) | fix(verticals): téléviseurs récents sans verticale (GOU-2031) | @GoulvenF |
| [PR 3597](https://github.com/open4good/open4goods/pull/3597) | feat(impactscore): métriques de correspondance EPREL et retrait des fiches retirées (GOU-1893) | @GoulvenF |
| [PR 3598](https://github.com/open4good/open4goods/pull/3598) | feat(front-api): titre lisible pour les produits hors catégorie (GOU-2030) | @GoulvenF |
| [PR 3601](https://github.com/open4good/open4goods/pull/3601) | fix(frontend): bouton Comparer lisible et filtre « Pays du code-barres (GS1) » (GOU-2035) | @GoulvenF |
| [PR 3602](https://github.com/open4good/open4goods/pull/3602) | feat(css): accroche « comparez les prix » accueil, /rechercher, pied de page (GOU-2036) | @GoulvenF |
| [PR 3603](https://github.com/open4good/open4goods/pull/3603) | feat(search): nom du marchand dans les cartes de résultats (GOU-2038) | @GoulvenF |
| [PR 3607](https://github.com/open4good/open4goods/pull/3607) | fix(legal): éditeur immatriculé, contact @nudger.fr et nom « Nudger » (GOU-2021) | @GoulvenF |
| [PR 3605](https://github.com/open4good/open4goods/pull/3605) | feat(stats): domaines marchands distincts avec offre active (GOU-2037) | @GoulvenF |
| [PR 3606](https://github.com/open4good/open4goods/pull/3606) | feat(eprel): table Apple pour apparier les iPhone à leur fiche EPREL (GOU-2042) | @GoulvenF |
| [PR 3608](https://github.com/open4good/open4goods/pull/3608) | feat(impactscore): critère V2 « réparabilité ou durabilité » pour TV et lave-linge (GOU-2041) | @GoulvenF |
| [PR 3600](https://github.com/open4good/open4goods/pull/3600) | feat(frontend): frais et délai de livraison dans la liste des offres (GOU-2034) | @GoulvenF |
| [PR 3604](https://github.com/open4good/open4goods/pull/3604) | feat(search): recherche globale, accessoires à part, longue traîne filtrée, classement par offres (GOU-2029) | @GoulvenF |
| [PR 3609](https://github.com/open4good/open4goods/pull/3609) | feat(scripts): BETA_BASIC_AUTH pour le banc d'essai CSS et l'échantillonneur de sitemap (GOU-2050) | @GoulvenF |


</details>
