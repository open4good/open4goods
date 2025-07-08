# Nudger : Our world

## La Tribe Nudger

Nudger s’appuie sur une **équipe** de passionnés qui donnent de leur temps et compétences.
Différents niveaux d'engagement sont prévus, et ceux-ci sont exposés de façon transparente à toute l'équipe
Chaque membre a un ou plusieurs rôle défini (ex: CEO/CTO, communication/PMO, développement,, SEO, design), et se retrouve dans un college spécifique

**Core Team** : Fondateur, associés et co-fondateurs. En plus du statut “**engagés”**, ils ont un contrat moral envers Nudger et les membres du collectif.
**Engagés** : Engagement contre perspective ou participation financière, mode de travail professionnel
**Volontaires** : Coup de main volontaire, pour etre sympa, pour se faire de la pub, pour mettre un pied dans l'aventure de Nudger
**Learners** : Ceux qui profitent de Nudger pour apprendre au sein d'un collectif sympa et dynamique.
**spectateurs** peuvent aussi suivre le projet en tant qu’observateurs intéressés (par exemple des membres de la communauté ou mentors), sans participation active aux tâches.

Tous les membres commencent bénévoles "volontaire". Ces typologies d’engagement permettent à chacun de trouver une place selon le niveau de disponibilité et d’implication, tout en permettant un pilotage projet structuré et professionnel. Il permet egalement une fluiité dans les niveaux d'engagement, chaque membre du collectif pouvant passer d'un collège à l'autre suivant les differets moments de vie (personnels et du projet)

### Organisation en Squads

Afin d’organiser le travail, les membres du collectifs Nudger peuvent mener des missions en autonomie, ou être affectés à des squads. Les squads Nudger sont des équipes de quelques personnes, pluri disciplinaires, qui se mobilisent ensemble autour de l’accomplissement d’un objectif précis.

Squad Front

Squad 

### Formalisation des pactes d’engagement sur le dépôt GitHub

Afin d’acter l’engagement de chacun et de cadrer les attentes, Nudger propose de formaliser des **“pactes d’engagement”** directement dans le repository GitHub. L’idée est de rendre publics et explicites les engagements et les reciprocites pris par les membres, en accord avec l’esprit open source et transparent du projet. Cela pourrait prendre la forme d’un document Markdown (par exemple `PACTE_ENGAGEMENT.md` ou dans le wiki du dépôt) listant pour chaque membre engagé :

- Son **rôle** et périmètre (quelle responsabilité principale il assume dans le projet).
- Sa **disponibilité estimée** ou charge qu’il s’engage à consacrer (ex: “5 heures par semaine”, “présence à la réunion hebdo + prise en charge d’une feature par mois”, etc.).
- La **durée** de l’engagement (certains bénévoles préfèrent s’engager par période, ex: “jusqu’à la v1.0” ou “pour les 3 prochains mois, renouvelables”).
- Les **engagements qualitatifs** qu’il promet de respecter (ex: communiquer en cas de problème ou de retard, respecter les standards de code et processus, véhiculer les valeurs de Nudger, etc.).

Chaque nouveau membre qui rejoint en tant qu’engagé sera invité à ajouter son pacte dans ce document via une Pull Request, officialisant ainsi son arrivée et son engagement envers l’équipe. Si un membre décide de modifier son niveau d’implication (par exemple passer d’engagé à spectateur temporairement faute de temps), une mise à jour du pacte dans le dépôt permet de garder une trace officielle de ce changement.

Ce pacte d’engagement formalisé apporte plusieurs bénéfices : il clarifie les **attentes réciproques** (le projet sait sur qui compter et dans quelle mesure, et le bénévole sait ce que le projet attend de lui), et sert de **référence** en cas de flou. En outre, l’afficher dans le dépôt renforce la confiance de la communauté externe en montrant que l’équipe est structurée et professionnelle même en environnement bénévole, il permet aussi de factualiser l'expérience Nudger pour les membres souhaitant faire de leur engagement une expérience professionnellement valorisable

## Gestion de projet

Le cycle de gestion de projet chez Nudger s’articule autour de plusieurs étapes collaboratives, de la génération d’idée jusqu’à la validation, le développement, le déploiement, le feedback (approche lean) :

- **Génération et collecte des idées :** Les idées d’évolution peuvent provenir aussi bien de l’équipe interne que de la communauté d’utilisateurs. En interne, tout membre du collectif est encouragé à proposer des nouvelles fonctionnalités, des améliorations ou des corrections de bugs.

Côté utilisateurs, Nudger a mis en place un système de feedback ouvert : sur le site, les visiteurs sont invités à faire part des bugs rencontrés ou à **soumettre et prioriser leurs idées d’évolutions**. Ce système de collecte renvoie directement vers les issues GitHub du projet, ce qui centralise toutes les suggestions directement dans la backlog projet. Ainsi, chaque idée (qu’elle vienne d’un membre ou d’un utilisateur) est formalisée soit sous forme d’issue GitHub (ticket) et suit un workflow projet fluide mais rigoureux,  assisté par IA, qui vise à permettre de maximiser l'efficacité sa prise en charge.

- **Tri et priorisation :** Chaque nouvelle idée ou demande remonte initialement dans une file de **Triage**. Lors des sessions de tri (généralement pendant la réunion hebdomadaire ou en continu pour les petites demandes), l’équipe évalue chaque proposition. Plusieurs critères guident la **priorisation** : impact écologique et utilisateur attendu, alignement avec la vision du produit, complexité technique, effort estimé et priorité temporelle (ex. pour la première release majeure).
La **voix des utilisateurs** est également prise en compte au travers du système de vote de Nudger et sert d’indicateur précieux de pertinence. Le Product Owner veille à ce que les **idées soumises par les utilisateurs soient considérées** et triées avec transparence.

Après discussion, chaque idée est soit acceptée (elle passe alors dans la backlog priorisé : TODO : A travailler), mise en attente (pour des versions ultérieures) ou écartée si non réalisable ou hors scope (en expliquant la raison aux contributeurs pour conserver une bonne communication).

L'issue est en tout cas systematiquement mise à jour (commentaires) lors de cette étape et correctement labelisée afin de permettre :

- Priorisation
- Identification des composants impactés
- Identifier de la squad et du responsable d'operation

Le backlog est ainsi alimenté et ordonnancé en continue, dans une optique kanban / lean (TODO : A challenger : est ce le cas ?)

- **Spécification et validation :** Avant qu’une évolution priorisée ne passe en développement, l’équipe s’assure de bien la **formaliser et la valider**. Concrètement, cela passe par la rédaction de critères d’acceptation clairs (souvent sous forme de cases à cocher ou de description du **“definition of done”** dans l’issue GitHub concernée).
Une demande pourra être redecoupée en plusieurs issues, représentant des sous taches, ou des actons différentes. On veillera dans ce cas a bien respecter etformaliser les lients de parenté pour conserver la cohérence et le suivi entre une demande initiale et son processus de mise en oeuvre.

Ce point de formalisation est majeur, il vise a rendre les travaux et le process projet de Nudger compréhensible par les parties prenantes de Nudger (communauté, utilisateurs, financeurs, institutionnels,..) ainsi qu'a maximiser l'assistance IA sur le workflow des demandes

Si nécessaire, une mini-étude est réalisée, cette étape de validation peut se faire de manière asynchrone sur l’issue (commentaires) ou lors d’une réunion dédiée pour les fonctionnalités majeures. L’idée est d’obtenir un **accord collégial** des engagés sur la solution à implémenter avant de coder. Une fois validée, la tâche est marquée comme prête à développer (*ready for dev*) et peut être prise en charge par un développeur (engagé, volontaire,  ou learner encadré). À ce stade, l’issue comporte un plan d’action suffisamment clair (maquettes, sous-tâches éventuelles, ressources de conception, etc.), ce qui évite les ambiguïtés une fois en développement.

- **Suivi du moral et motivation de l’équipe :** Comme Nudger repose sur un collectif, **maintenir un bon moral et une motivation élevée** est un aspect crucial du management de projet. Dans ses rituels projet, L’équipe met en place un **“mood board”** hebdomadaire pour prendre la température de chacun. Concrètement, lors de la réunion hebdo, chaque membre engagé partage son **humeur du moment** vis-à-vis du projet – cela peut se faire via une note de 1 à 5, une couleur, un emoji représentant son état d’esprit, ou simplement en exprimant comment il/elle se sent. Ce suivi régulier du moral aide à **prévenir l’épuisement** de la Tribe Nudger et à adapter le rythme si nécessaire, afin que contribuer reste un plaisir et non une contrainte. Les réussites (même petites) de la semaine sont soulignées pour remercier chacun de ses efforts, renforçant ainsi la motivation collective.

## Rituels


Le **rythme de base** s'articule autour d'une **réunion hebdomadaire** de l'équipe core (en visioconférence). Durant cette réunion, nous organisons un **tour de table** structuré avec tous les engagés et les learners. Les spectateurs et volontaires peuvent assister en tant qu'auditeurs libres.

- Tour de table par type d'intervenant
 Présentation envoyée le jeudi soir, à compléter par les engagés pour vendredi
 Mood board
 Gestion de la ponctualité
- Identification des funfacts pour la communication sur les réseaux sociaux et le blog
 Mood board
- Suivi projet squad par squad autour des  tableaux GitHub
 Présentation à envoyer le jeudi soir, à compléter par les engagés avant vendredi midi
Tour de table :
1. **Ce qui a été fait** depuis la dernière réunion (avancées, tâches terminées)
2. **Ce qu'il prévoit de faire** ensuite (tâches à entamer ou poursuivre)
3. **Les blocages ou besoins d’aide éventuels** rencontrés.

### KPI

Les Kpi seront suivis en seance

TODO : quels KPI ?

Géneration automatique des KPI

### 

Dans un souci de professionnalisation et de communication interne, Nudger va mettre en place un système de **génération automatique d’une slide de synthèse hebdomadaire** avec les indicateurs clés du projet. L’idée est qu’à chaque fin de semaine (le jeudi soir, pour diffusion immédiate ou le vendredi), un script récupère les données de la semaine écoulée et compile une courte présentation des **KPIs du sprint courant**. Cette slide au format image ou PDF pourra ensuite être partagée facilement sur le Slack/email interne, et éventuellement avec les partenaires ou la communauté pour montrer l’avancement.

Les **KPIs clés** suivis hebdomadairement incluront notamment :

- La **vélocité** de l’équipe : par exemple, le nombre de tickets fermés dans la semaine (ou le nombre de points si l’on utilise un chiffrage), comparé à la semaine précédente. On peut afficher cela avec un petit graphique d’évolution pour motiver l’équipe (tout en gardant en tête que la vélocité peut fluctuer selon la dispo des bénévoles).
- Le **moral de l’équipe** : issu du mood board hebdomadaire, on pourrait quantifier un score moyen de satisfaction/motivation (sur 5 ou sur 10) ou simplement indiquer les tendances (ex: “Moral moyen = 8/10, en léger progrès par rapport à 7/10 la semaine dernière”). C’est un indicateur rare dans les projets classiques, mais crucial ici pour surveiller la santé de l’équipe bénévole.
- La **participation et contributions** : nombre de **commits** fusionnés, nombre de **pull requests** créées et mergées dans la semaine, ainsi que le nombre de contributeurs actifs (ex: 4 membres core + 1 contributeur externe ont contribué cette semaine). On peut également mettre en avant les contributions des learners ou nouveaux venus (ex: “1ère PR de @nouveauLearner mergée – bienvenue à lui !”).
- D’autres KPIs pertinents pourraient être ajoutés : le nombre de nouvelles **issues** créées (idées ou bugs) et combien ont été résolues, le temps moyen de résolution d’un ticket, le **couverture de tests** si on la mesure, etc., pour peu qu’on puisse les extraire automatiquement.

Techniquement, cette génération automatique sera idéalement intégrée à la **CI/CD GitHub**. Par exemple, on peut imaginer une GitHub Action programmée (via un workflow déclenché *schedule* chaque jeudi 18h) qui exécute un script. Ce script utilisera l’**API GitHub** pour collecter les données (issues fermées depuis 7 jours, réactions, etc.), puis génèrera un slide. On peut s’appuyer sur des librairies Python (ou JS) pour créer une image (par exemple en remplissant un template graphique avec les chiffres et quelques icônes). Une option plus simple est de générer un document Markdown/résultats chiffrés que Bérangère ou un autre membre pourra copier dans un modèle PowerPoint/Canva manuellement – mais l’objectif est d’automatiser un maximum pour **gagner du temps** sur le reporting.

Une fois la slide générée automatiquement, elle pourrait être **envoyée** directement (par mail aux membres, ou postée sur un canal Slack Teams dédié) le jeudi soir. On envisagera même d’**intégrer cette slide dans le dépôt** (par ex, commiter le PNG/PDF dans un dossier `reports/`) afin de conserver l’historique semaine par semaine. Bien entendu, cette slide reste **modifiable manuellement ensuite** : l’équipe pourra y ajouter un commentaire, ajuster un chiffre si nécessaire ou corriger l’appréciation du moral qui peut être difficilement quantifiable automatiquement. L’automatisation apporte la trame et les chiffres bruts, et l’intervention humaine peut peaufiner l’histoire à raconter.

Au-delà de l’usage interne (pilotage et motivation de l’équipe), cette slide hebdo pourra servir de base à de la communication externe périodique (par exemple, publier sur LinkedIn un petit bilan mensuel du projet Nudger avec quelques chiffres de ce type, montrant la progression de la plateforme). C’est donc un **outil de visibilité** en plus d’être un outil de management interne.


### Intégration de la participation communautaire

Étant un projet ouvert et tourné vers le bien commun, Nudger cherche à **impliquer sa communauté** le plus possible dans le développement. Plusieurs mécanismes garantissent que les contributions externes soient accueillies et intégrées efficacement :

- **Ouverture des issues aux utilisateurs :** Grâce au système de feedback mis en place, n’importe quel utilisateur peut soumettre une idée ou remonter un problème, qui sera converti en ticket GitHub. L’équipe s’engage à lire et traiter ces issues communautaires, en taguant par exemple celles-ci avec `source:community` et en répondant poliment à chaque proposition. Cette **ouverture au dialogue** fait sentir aux contributeurs externes qu’ils font partie de l’aventure Nudger.
- **Votes et commentaires :** Comme évoqué, les utilisateurs peuvent **voter pour les fonctionnalités** qu’ils jugent importantes directement via les sites grâce au système spécifique mis en place. L’équipe prend ces votes en considération lors de la priorisation des features, offrant ainsi un **droit de regard aux usagers** sur la feuille de route. De plus, les commentaires supplémentaires sont les bienvenus pour préciser un besoin ou suggérer une implémentation – tout cela enrichit la compréhension des demandes côté équipe.
- **Contributions de code externes (PR) :** La communauté de développeurs peut également contribuer du code. Par exemple, si un développeur volontaire (qu’il soit simplement intéressé par le projet ou qu’il fasse partie des membres learners) souhaite résoudre un bug ou développer une petite fonctionnalité, il peut soumettre une **Pull Request**. Nudger facilite cela en marquant clairement les tickets où de l’aide est souhaitée (`help wanted`) et ceux qui sont adaptés aux nouveaux contributeurs (`good first issue`). Lorsqu’une PR externe est ouverte, l’équipe s’engage à la traiter dans des délais raisonnables : un membre core sera désigné pour **faire la revue** (vérification du respect des standards de code, tests, etc.) et pour accompagner le contributeur si des modifications sont nécessaires. Cette réactivité et bienveillance lors des reviews incitera les contributeurs externes à revenir.
- **Intégration des learners** : Les profils learners (parfois des étudiants ou de jeunes pros) qui souhaitent se former en contribuant se voient proposer un cadre accueillant. On peut par exemple leur attribuer un **mentor** parmi les engagés, qui va répondre à leurs questions, leur donner de petits défis progressifs et relire attentivement leurs PR en expliquant les corrections. L’idée est de faire de Nudger une **école de contribution open source** tout en profitant de leurs contributions. Les learners peuvent commencer par des tâches simples (documentation, petits bugs) et, au fil des réussites, monter en puissance vers des fonctionnalités plus complexes. S’ils gagnent en autonomie et démontrent leur engagement sur la durée, ils pourront graduellement prendre un rôle d’engagé dans l’équipe. Ce parcours progressif (spectateur curieux → contributeur learner sur quelques issues → membre engagé régulier) est encouragé et valorisé au sein du projet.
- **Communication et reconnaissance :** La participation communautaire est régulièrement mise en avant. Par exemple, lors de la réunion hebdo, l’équipe cite les contributions externes notables de la semaine (”Untel a soumis une super idée”, “Unetelle a résolu ce bug, merci à elle”). De même, dans la slide hebdomadaire des KPIs, on intègre le nombre de contributeurs et on peut mentionner les nouveaux venus. Cette **reconnaissance** est essentielle pour remercier les bénévoles externes et les fidéliser. Par ailleurs, Nudger pourrait maintenir un fichier `CONTRIBUTORS.md` ou un Hall of Fame listant toutes les personnes ayant contribué (core-team et externes) en guise de remerciement public. De même, la page "équipe" sur le site de Nudger restituera directement ce [Contributing.MD](http://contributing.md/)

En résumé, Nudger intègre sa communauté à toutes les étapes du projet : de l’idéation (votes, suggestions) jusqu’à l’implémentation (PR, tests). Cette approche ouverte et participative rejoint la philosophie du projet, qui se veut **collectif et communautaire sur le long terme**. En retour, cela apporte au projet plus d’idées, plus de force de développement, et tisse autour de Nudger une **communauté d’ambassadeurs** prêts à promouvoir et améliorer l’outil.

[Points hebdo](https://www.notion.so/Points-hebdo-218bd60ebb9380689c77c4f5da6b8e13?pvs=21)

[Suivi des actions](https://www.notion.so/218bd60ebb93806bb8b7c2250a3adf7e?pvs=21)