# Déploiement opérationnel avec GitHub Projects

## Choix méthodologique : Kanban Agile

Pour la gestion opérationnelle, Nudger adopte une approche **Kanban agile** souple, s’appuyant sur GitHub Projects pour le suivi des tâches. Ce choix favorise un flux continu d’intégration des évolutions, adapté à une équipe hétérogène dont les disponibilités peuvent varier, évitant ainsi la rigidité de sprints fixes après le lancement du produit. L’**intelligence artificielle générative** est intégrée pour assister ce processus Kanban.

---

## Organisation des boards GitHub Projects

### Boards par squads

Chaque squad possède un board GitHub Projects dédié pour suivre ses tâches opérationnelles. Les boards sont nommés selon la règle suivante :  
`SQUAD - [NOM_SQUAD]`.

Une règle d'automatisation via GitHub Actions (`label-to-project.yml`) affecte automatiquement les issues portant le label `squad:[NOM_SQUAD]` au board correspondant.

**Gestion des tâches au niveau macro (PMO) :**  
Toutes les tâches nécessitant un suivi macro (au niveau PMO) doivent impérativement être labellisées `EPIC` ou `USER STORY`.  
Les tâches techniques détaillées associées sont généralement créées sous forme de sous-issues liées à une issue parente.

---

### Board PMO – Global Board

Le board `PMO - Global Board` permet le suivi macro et la gestion des tâches transverses ou non affectées à une squad spécifique. Il agrège l’ensemble des issues et s'appuie notamment sur les labels `EPIC` et/ou `USER STORY` pour assurer une visibilité d'ensemble cohérente.

La PMO porte ainsi une attention particulière aux tickets identifiés comme stratégiques ou impactant le produit à un niveau global.

---

### Stratégie de labellisation des issues

Nudger applique une stratégie cohérente de labels pour faciliter le filtrage et le suivi des tickets GitHub. Chaque issue doit comporter une combinaison adaptée parmi ces labels :

| Catégorie       | Labels exemples          | Usage typique                                |
|-----------------|--------------------------|----------------------------------------------|
| `type`          | `type:bug`, `type:feature`, `type:tech` | Type de tâche (fonctionnalité, bug, technique...)|
| `component`     | `component:frontend`, `component:backend` | Composant technique concerné                 |
| `priority`      | `priority:P0`, `priority:P1`, `priority:P2` | Niveau de criticité/urgence                  |
| `source`        | `source:community`, `source:internal` | Origine de la demande (communauté/interne)   |
| `visibility`    | `visibility:public`, `visibility:internal` | Public visé (communauté ou interne)          |
| `votable`       | `votable`                | Issue ouverte au vote de la communauté       |

Exemple concret de combinaison :  
`type:bug`, `component:frontend`, `priority:P0`, `source:community`

**Règle spécifique :** Toute issue créée directement depuis le site communautaire reçoit automatiquement le label `votable`.

---

### Vues GitHub Projects : publique vs interne

Deux vues principales sont configurées pour répondre aux besoins spécifiques des utilisateurs externes et de l’équipe interne.

**Vue publique – backlog communautaire :**  
- Accessible publiquement (via README et site Nudger).
- Filtrage sur `visibility:public`.
- Affichage principalement des issues de type feature requests, suggestions d'amélioration.
- Permet à la communauté de voter 👍 et commenter.
- Utilisée pour identifier les demandes prioritaires via le nombre de votes.
- N'affiche pas les informations techniques internes (assignation, complexité technique, etc.).

**Vue interne – pilotage technique :**  
- Destinée à l’équipe Nudger (mais accessible publiquement pour transparence).
- Affichage de toutes les issues (
