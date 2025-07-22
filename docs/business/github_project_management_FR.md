# Déploiement opérationnel sur GitHub Projects

## Choix méthodologique : Kanban

Pour la gestion opérationnelle, Nudger adopte une approche **Kanban agile** souple, en s’appuyant sur GitHub Projects pour le suivi des tâches. Ce choix méthodologique privilégie un flux continu d’intégration des évolutions, ce qui convient bien à une équipe héterogene où la disponibilité peut varier – on évite ainsi la rigidité de sprints fixes une fois le produit lancé. **L’intelligence artificielle** est mise à contribution pour assister ce Kanban : Nudger étant un projet techniquement innovant (intégrant de l’IA générative côté produit), l’équipe exploite également des outils basés sur l’IA pour optimiser sa gestion. Concrètement, cela signifie utiliser des assistants comme GitHub Copilot ou OpenAI Codex pour aider à la planification et au développement (rédaction de tâches, aide à l’estimation, voire automatisation de certaines opérations de routine).

## Organisation des board GitHub Projects 


### Board orientés squad.



Des boards spécifiques (vues projets des issues github) calquent le fonctionnement en squad du collectif Nudger. (Cf. governance.md). Cela permet à chaque squad de s'organiser comme elle le veut en terme de gestion de backlog (colonnes spécifiques, tags spécifiques, ...)

A ce titre, chaque Squad dispose de son propre board de pilotage. Le tableau de bord doit être nommé par **SQUAD - [NOM_SQUAD]**. Une règle d'automatisation github workflow (cf. label-to-project.yml) va automatiquement faire remonter les issues tagguées **squad:NOM_SQUAD** dans les board correspondant.


Une règle importante : afin de permettre le pilotage global au niveau du Board PMO, toutes les tâches qui méritent d'être suivies au niveau macro (Board PMO) doivent être tagguées par **EPIC** ou **USER STORY** 


Il sera classique d'utiliser des sub issues pour décrire et formaliser les tâches techniques associées à la feature parent.



### Board PMO

Le board "PMO - Global Board" permet le pilotage macro et la gestion des tâches non affectés aux squads. Pour cela TOUS les tickets remontent dans ce board. 

La PMO a donc accès à tous les tickets, mais portera une attention particulière, grâce aux systèmes de filtrages  : 
* Aux tickets labellisés EPIC et/ou USER STORY,

.
### Stratégie de labellisation pour la catégorisation des issues

Afin de faciliter le filtrage et le suivi des tickets, Nudger met en place une **stratégie de labels** (aka : tags github issues)  couvrant plusieurs axes de catégorisation cohérents. Les principales catégories de labels envisagées incluent :


Le tag **votable** sera attribuée aux issues pour lequelle l'avis des utilisateurs est demandé. Toute issue nouvellement créés **depuis les sites utilisateurs** se verront affecter cette etiquette **votable




priorité

squad

bug

feature





Chaque ticket se verra donc attribuer une **combinaison de labels** couvrant ces axes (ex: un bug critique sur le frontend demandé par un utilisateur aura `type:bug`, `component:frontend`, `priority:P0`, `source:community`). Une utilisation **cohérente des libellés** est importante : l’équipe définira une nomenclature claire et veillera à l’appliquer lors du triage. Cela permettra d’utiliser efficacement les filtres de GitHub (par exemple afficher toutes les features backend P1 en attente) et de générer des **vues personnalisées** dans GitHub Projects.

L
### Vues GitHub Projects : publique vs interne

GitHub Projects (Beta) offre la possibilité de créer des **vues personnalisées** du board, ce que Nudger exploitera pour adresser différemment le public externe et l’équipe interne. Deux vues principales seront configurées :

- **Vue publique (backlog communautaire)** – Cette vue, rendue publique et facilement accessible (depuis le README ou le site Nudger via un lien), présentera aux utilisateurs la liste des idées et évolutions envisagées, avec la possibilité pour la communauté de **voter** et commenter.

Cette backlog sera un filtrepour ne restituer que les issues tagguées **visibility:public**. Un usage classique peut être d'avoir les issue "parent" en visibility::public, et les issues enfants en visibility:internal, permetant ainsi de garder un lien clair entre issues presentés au public et les taches de travail pilotés et utilisées par l'équipe interne.

Concrètement, cette vue affichera surtout les issues de type *feature requests* et suggestions ouvertes, probablement celles dans la colonne Backlog (ou une colonne dédiée aux idées proposées ?). On peut filtrer pour ne montrer que les items marqués `source:community` ou ayant un label particulier signifiant “Suggestion d’amélioration”.  Ces **votes utilisateurs** serviront de référence lors de la priorisation : un tri par nombre de 👍 (ou un champ custom “Votes”) permettra d’identifier les demandes les plus plébiscitées. La vue publique sera présentée de manière simple et transparente, sans informations techniques superflues – par exemple, on n’y affichera pas nécessairement les champs internes comme la complexité ou l’assignee. Le but est que les contributeurs non techniques et utilisateurs puissent **suivre l’avancement** du projet et se sentir impliqués dans les choix d’évolution. De même,

- **Vue interne (pilotage technique)** – A destination de la Tribe (mais pour autant publiquement exposée pour les curieux) , la vue interne offrira un **tableau de bord complet** pour le suivi au jour le jour. Elle inclura l’ensemble des colonnes (Triage, Backlog, In Progress, Review, Done) et toutes les issues, y compris les tâches purement techniques ou de maintenance que la communauté n’a pas forcément besoin de voir en priorité. Dans cette vue, on pourra afficher des champs supplémentaires comme l’**assignation** (qui travaille sur quoi), la **priorité interne** (parfois l’équipe peut ajuster l’ordre indépendamment des votes publics, par exemple pour corriger un bug critique ayant peu de votes car peu visible du public), ou encore un champ **échéance** si certaines tâches ont une deadline. La vue interne servira au **pilotage technique** lors des réunions d’équipe : on peut l’utiliser en partage d’écran durant le stand-up hebdo pour passer en revue chaque colonne. Elle permet également aux membres de savoir sur quoi travaillent leurs collègues en un coup d’œil et de détecter les surcharges (trop de cartes sur une personne, par exemple). Cette vue pourra être privée (accessible uniquement aux membres du repo) si l’équipe souhaite y mettre des commentaires ou champs confidentiels, mais comme le projet Nudger est public, même les aspects techniques pourraient en grande partie être ouverts. Quoi qu’il en soit, la séparation des vues garantit une **lisibilité adaptée à chaque audience** : le public voit l’état du produit et peut contribuer aux idées, l’équipe voit le détail opérationnel pour conduire le projet efficacement.

En somme, ces deux perspectives sur le même projet assurent la **transparence vis-à-vis des utilisateurs** tout en préservant un outil de travail complet et "end to end" pour l’équipe. Cette configuration est évolutive : Nudger pourra ajouter d’autres vues filtrées (par exemple une vue “Bugs only” pour se concentrer sur la qualité, ou une vue par composant pour les squads frontend/backend).

Les taches autres que techniques (communication, redactionnel, gestion de projet) seront également suivies de la même manière, afin d'offrir une vue complete et cohérente de l'integralité des tâches du projt Nudger

## Release

Les releases sont des marqueurs importants et de célebration. En mode déploiement continue sur la cible, il convient pour autant d'animer et suivre les releases.

Pour cela, on utilisera du Semver, on visera a trouver une thematique de noms "Tribe, Green ? TODO : Action equipe", et on generera automatiquement les releases notes, comprenant les tâches non techs. On visera si possible a animer ces releases de façon ouverte sur le blog et les réseaux

TODO : Expliquer
TODO : Les releases integrent aussi les taches non tech
TODO: Animation autour des Releases

> On gere les taches "projet / communication" egalement comme cela
PArler du cycle de Release
> 
