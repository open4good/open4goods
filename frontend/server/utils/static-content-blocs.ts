import type { DomainLanguage } from '~~/shared/utils/domain-language'

/**
 * Real editorial copy for XWiki bloc ids that used to be fetched live, extracted 2026-09-09 from
 * the owner-provided export archive (`/opt/open4goods/backup/xwiki-export.xar`) rather than the
 * live wiki -- anonymous XWiki REST access is down (see
 * incident_xwiki_blog_locked_down_sept2026), and `xwiki-editorial-content-to-nuxt-content`'s AC1
 * needs this content to be real, not fabricated placeholder text.
 *
 * Per bloc id, the plain-text content per language actually authored in the archive. English is
 * missing for several team bios and for two partner descriptions -- `getStaticContentBloc` falls
 * back to French rather than inventing a translation of someone's personal bio. One correction was
 * made against the source: `pages/ecosystem/frenchtech.xml`'s French content was a copy-paste of
 * the Wekey blurb (a pre-existing data bug in XWiki, not a translation choice here) -- its `fr`
 * value below is translated from the correct English content instead of repeating that error.
 */
const STATIC_CONTENT_BLOCS: Record<string, Partial<Record<DomainLanguage, string>>> = {
  // Team member bios and titles (front-api's team-config.{cores,contributors}[].bloc-id)
  'pages:team:goulven-furet:': {
    fr: "Goulven est le fondateur de cette initiative. CTO/CEO augmenté à l'IA, encore bien incapable de dire ce qui lui plait le plus...",
    en: 'Goulven is the founder of this initiative. Carrying the trajectory and technical coherence of the platform, he believes in it so much that he decided to dedicate himself fully to this project. Juggling everything at once, he switches between the role of CTO and that of CEO, still quite unable to say which one he enjoys most...',
  },
  'pages:team:goulven-furet-title:': { fr: 'CEO / CTO' },
  'pages:team:berangere-leven:': {
    fr: 'Bérangère assure la communication et la PMO de ce projet. Grâce à elle, nos jalons sont (souvent) respectés, et nos présentations sont (toujours) compréhensibles et agréables à regarder. Bérangère assure également la stratégie de communication sur les réseaux sociaux.',
  },
  'pages:team:berangere-leven-title:': {
    fr: 'Communication et pilotage',
    en: 'Communication and pilotage',
  },
  'pages:team:thomas-vandewalle:': {
    fr: "Thomas est notre expert SEO. Ancré depuis longtemps dans ce secteur, les autorités de noms de domaines et stratégies de backlinking n'ont pas de secret pour lui. Il aide également sur la rédaction des contenus et -pour ne rien gâcher- il est toujours de bonne humeur.",
  },
  'pages:team:thomas-vandewalle-title:': { fr: 'Stratégie SEO & contenus' },
  'pages:team:candide-cherel:': {
    fr: "Candide est une touche à tout engagée, curieuse et passionnée. Elle met son savoir faire en matière de conduite de projet et de développement business au profit de l'aventure Nudger. Si vous y voyez clair dans la roadmap de Nuger, c'est graĉe à Candide !",
  },
  'pages:team:candide-cherel-title:': { fr: 'Gestion et conduite de projet' },
  'pages:team:louis-marie-toudoire:': {
    fr: "Louis est notre Padawan, qui profite de son envie de bien commun pour prendre de l'XP en développant sur Nudger. Que ce soit sur les intégrations IA génératives ou sur les sujets open-data, il est fort probable que vous utilisiez son travail quand vous nudgez vos articles !",
  },
  'pages:team:louis-marie-toudoire-title:': { fr: 'Développeur Backend' },
  'pages:team:laurent-blondel:': {
    fr: 'Laurent est notre spécialiste front, qui a l\'art de traduire le "mot joli" en réalité technique. Entre les désirs des uns et le manque d\'intérêt des autres, il est la personne grâce à qui notre volonté de "communs" s\'inscrit dans une interface agréable et facile à utiliser.',
  },
  'pages:team:laurent-blondel-title:': { fr: 'Développeur & intégrateur frontend' },
  'pages:team:max-ziliani:': {
    fr: "Max est à l'origine de l'identité visuelle de Nudger. C'est grâce à lui que le site est beau, et que Laurent peut transformer les rêves de beauté des uns en réalité pour tous.",
  },
  'pages:team:max-ziliani-title:': { fr: 'Identité visuelle et artistique' },
  'pages:team:loic-gourmelon:': {
    fr: "Loïc donne un coup de main sur la plateforme de Nudger. Mise a niveau et modernisation de notre infrastucture, il nous démontre que les approches cloud modernes peuvent aussi s'inscrire dans une logique de souveraineté.",
  },
  'pages:team:loic-gourmelon-title:': { fr: 'Devops' },
  'pages:team:thierry-ledan:': {
    fr: "Thierry a contribué sur la partie technique, notamment sur l'intégration Xwiki, qui permet à toute l'équipe d'éditer les contenus de Nudger.",
  },
  'pages:team:thierry-ledan-title:': { fr: 'Développements backend' },
  'pages:team:nicolas-bonamy:': {
    fr: 'Nicolas est un expert backend, il a fait une passe sur différents aspects techniques du site, développé plusieurs optimisations pour le système et travaillé sur la prise en main technique de Nudger.',
  },
  'pages:team:nicolas-bonamy-title:': { fr: 'Dev backend & infra' },
  'pages:team:stephane-castrec:': {
    fr: 'Stéphane est architecte logiciel et développeur multi facettes. Il donne un peu de son temps pour explorer la mise en place d\'une extension de navigateur pour nudger.',
  },
  // The source title bloc's own content is literally the person's name, not a role -- kept as-is,
  // it is still real authored content and not this document's place to invent a job title.
  'pages:team:stephane-castrec-title:': { fr: 'Stephane Castrec' },
  'pages:team:yann-mingant:': {
    fr: "Yann est dev UX / UI. Définition des maquettes et mise en musique dans une approche technique moderne, les arcanes de l'expérience utilisateur et des interfaces graphiques vibrantes n'ont pas de secrets pour lui.",
  },
  'pages:team:yann-mingant-title:': { fr: 'Lead UX / UI' },
  'pages:team:albert-ljv:': {
    fr: "Albert file un coup de main sur Nudger, en contribuant aux développements de l'interface graphique et en mettant l'accent sur l'indispensable phase de recette du site.",
  },
  'pages:team:albert-ljv-title:': { fr: 'Devs UX / Recette' },

  // Team page hero sections
  'pages:team:hero-core-team:': {
    fr: "Nudger, c'est avant tout une équipe de passionnés qui donne son temps et ses compétences de façon désintéressée. Voici le noyau dur, l'équipe des réguliers qui ne conçoit pas une semaine sans travailler ou penser à ce beau projet.",
  },
  'pages:team:hero-contributors-team:': {
    fr: 'Un grand merci également à tous nos contributeurs, qui ont donné de leur temps et de leur expertise pour aider Nudger a aller plus loin.',
    en: 'Big up !',
  },
  'pages:team:partenaires:': {
    fr: 'Ce comparateur écologique de bien commun ne serait pas non plus possible sans nos nombreux partenaires. Merci à nos mentors et aux plateformes marchandes qui ont accepté de jouer le jeu de Nudger !',
  },

  // Mentor and ecosystem partners (front.partners.{mentors,ecosystem}[].bloc-id)
  'pages:partners:ecotree': {
    fr: 'EcoTree accompagne entreprises et particuliers dans l’investissement durable : posséder des arbres, restaurer des écosystèmes, séquestrer du carbone, protéger la biodiversité — des actions concrètes et mesurables.',
    en: 'EcoTree enables companies and individuals to invest sustainably: own trees, restore ecosystems, sequester carbon, protect biodiversity — concrete and measurable actions.',
  },
  'pages:partners:les-imparfaits': {
    fr: 'Les Imparfaits créent des déjeuners responsables pour les entreprises à Brest et Vannes, en cuisine d’insertion, avec des produits locaux et une livraison en vélo-cargo — pour des repas bons pour les gens et la planète.',
    en: 'Les Imparfaits make responsible lunches for companies in Brest and Vannes, via social kitchens, local ingredients and cargo-bike delivery — meals that are good for people and planet.',
  },
  'pages:partners:moovance': {
    fr: 'Moovance propose une application qui motive chacun à adopter une mobilité durable, en valorisant les gestes éco-responsables par des récompenses. Une démarche concrète pour réduire l’empreinte carbone collective.',
    en: 'Moovance offers an app that encourages everyone to adopt sustainable mobility by rewarding eco-friendly actions. A concrete approach to reduce collective carbon footprint.',
  },
  // No English translation was ever authored for these two -- fr is served for both languages.
  'pages:partners:polaria': {
    fr: 'Polaria accompagne entreprises, institutions et collectivités dans l’intégration concrète de l’intelligence artificielle. Elle propose de construire des stratégies "humain + IA" visant à transformer les organisations de façon éthique, durable et mesurable.',
  },
  'pages:partners:wekey': {
    fr: 'Wekey accompagne les entreprises dans leurs projets digitaux en mobilisant des consultants experts - de la stratégie à la réalisation - tout en aidant les freelances à trouver des missions alignées avec leurs compétences et leurs ambitions.',
  },
  'pages:ecosystem:frenchtech': {
    fr: 'French Tech Brest Bretagne Ouest fédère l’écosystème des startups de l’ouest de la Bretagne. Elle organise des événements, des programmes d’accélération et des temps de réseautage pour soutenir l’innovation locale.',
    en: 'French Tech Brest Bretagne Ouest unites the startup ecosystem of western Brittany. It organizes events, acceleration programs, and networking to support local innovation.',
  },
  // No English translation authored -- fr is served for both languages.
  'pages:ecosystem:icecat': {
    fr: 'Icecat est un fournisseur international de données produits. Il collecte, normalise et diffuse des fiches techniques enrichies pour les fabricants, distributeurs et e-commerçants afin d’optimiser la présentation des produits et améliorer l’expérience d’achat en ligne.',
  },

  // Opensource page (app/pages/opensource/index.vue)
  'webpages:opensource:hero-description': {
    fr: 'Découvrez les coulisses de Nudger pour comprendre comment est construit votre comparateur préféré.',
    en: 'Open4goods makes its code, data, and decision logs public so anyone can verify how the comparator works and suggest improvements.',
  },
  'webpages:opensource:pillars-intro': {
    fr: 'Nos piliers open source garantissent que chaque partie prenante puisse comprendre, réutiliser et améliorer la plateforme en toute confiance.',
    en: 'Our open-source pillars ensure every stakeholder can understand, reuse, and enhance the platform with confidence.',
  },
  'webpages:opensource:pillars-transparency': {
    fr: 'Consultez l’architecture modulaire, les pipelines CI et les standards de codage qui assurent la maintenabilité du projet.',
    en: 'Review the modular architecture, CI pipelines, and coding standards that keep the project maintainable.',
  },
  'webpages:opensource:pillars-methodology': {
    fr: 'Plongez dans notre documentation sur l’Impact Score pour voir comment les indicateurs sont sourcés, pondérés et régulièrement mis à jour.',
    en: 'Dive into our Impact Score documentation to see how indicators are sourced, weighted, and regularly updated.',
  },
  'webpages:opensource:pillars-community': {
    fr: 'Rencontrez le collectif de citoyens, chercheurs et partenaires qui nourrissent la feuille de route et la gouvernance.',
    en: 'Meet the collective of citizens, researchers, and partners who nurture the roadmap and governance.',
  },
  'webpages:opensource:contribute-intro': {
    fr: "Contribuer est un parcours collaboratif - suivez ces étapes pour configurer votre environnement et commencer à jouer avec la bête !",
    en: 'Contributing is a collaborative journey. Follow these steps to set up your environment and ship value quickly.',
  },
  'webpages:opensource:contribute-setup': {
    fr: 'Clonez le dépôt et explorez la documentation pour comprendre l’espace de travail.',
    en: 'Clone the repository, and explore the documentation to understand the workspace.',
  },
  'webpages:opensource:contribute-issues': {
    fr: 'Choisissez une issue étiquetée pour les nouveaux arrivants ou les champions de l’impact, puis discutez de votre approche avec les mainteneurs.',
    en: 'Choose an issue tagged for newcomers or impact champions, then discuss your approach with the maintainers.',
  },
  'webpages:opensource:contribute-share': {
    fr: 'Ouvrez une pull request, demandez des revues et célébrez les apprentissages avec la communauté une fois la fusion réalisée.',
    en: 'Open a pull request, request reviews, and celebrate the learnings with the community once it’s merged.',
  },
  'webpages:opensource:resources-intro': {
    fr: 'Trouvez l’essentiel pour contribuer sur le code, en design ou en idées sans perdre de temps à chercher le bon lien.',
    en: 'Find the essentials to contribute code, design, or insights without losing time searching for the right link.',
  },
  'webpages:opensource:resources-guide': {
    fr: 'Lisez le guide de contribution pour la stratégie de branches, les conventions de code et la cadence de publication.',
    en: 'Read the contribution guide for branching strategy, coding conventions, and release cadence.',
  },
  'webpages:opensource:resources-issues': {
    fr: 'Parcourez le suivi des issues sur GitHub pour vous porter volontaire sur des bugs, des fonctionnalités ou des tâches de recherche.',
    en: 'Browse the GitHub issue tracker to volunteer on bugs, features, or research tasks.',
  },
  'webpages:opensource:resources-updates': {
    fr: 'Restez informé grâce aux mises à jour du blog mettant en avant les nouvelles versions, partenariats et décisions produit.',
    en: 'Stay tuned with blog updates highlighting new releases, partnerships, and product decisions.',
  },
  'webpages:opensource:community-callout': {
    fr: 'Vous préférez une conversation directe ? Contactez l’équipe et nous co-concevrons le chemin de contribution qui vous convient.',
    en: 'Prefer a direct conversation? Reach out to the team and we’ll co-design the contribution path that suits you.',
  },

  // Opendata pages (app/pages/opendata/{gtin,isbn}.vue) -- no English translation authored yet.
  'webpages:opendata:gtin-hero-overview': {
    fr: 'Accédez à notre base de données GTIN. Ces données sont mises à jour une fois par semaine et proviennent de notre agrégation de données autour des produits disponibles sur Nudger.',
  },
  'webpages:opendata:isbn-hero-overview': {
    fr: 'Accédez à notre base de données ISBN, mise à jour une fois par semaine. Ces données proviennent des différentes informations que nous collectons sur les livres.',
  },
}

const escapeHtml = (value: string): string =>
  value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')

/**
 * Looks up real static content for a bloc id, HTML-escaped and paragraph-wrapped the way
 * `TextContent.vue` expects `htmlContent` to already be (it renders the value with `v-html`
 * unmodified). Falls back from the requested language to French, then returns `null` when the
 * bloc id has no static entry at all -- callers should fall through to the live XWiki fetch in
 * that case.
 */
export function getStaticContentBloc(
  blocId: string,
  domainLanguage: DomainLanguage
): string | null {
  const entry = STATIC_CONTENT_BLOCS[blocId]
  if (!entry) {
    return null
  }
  const text = entry[domainLanguage] ?? entry.fr
  if (!text) {
    return null
  }
  return `<p>${escapeHtml(text)}</p>`
}
