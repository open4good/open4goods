# Plan d'amélioration des emails Spring Boot Admin

## Contexte

Le module `admin` utilise Spring Boot Admin 3.x et un template Thymeleaf pour les notifications email (`status-changed.html`).
L'objectif est d'améliorer la lisibilité des alertes en cas d'incident, de réduire le bruit, et d'ajouter des liens directs vers l'interface SBA (`https://sb-admin.nudger.fr`).

## Décisions déjà validées

- Notifier les statuts critiques avec un focus sur `DOWN`.
- Notifier également `OUT_OF_SERVICE`.
- Ne pas envoyer d'email hors production (environnement beta).
- Destinataires fixes.
- Pas de logique CC/BCC spécifique par criticité.
- Pas d'exclusion de services.
- Objet validé au format métier explicite.
- Afficher une durée depuis le changement d'état.
- Liens directs vers la page instance SBA.
- Format HTML + fallback texte.
- Style visuel minimal charte Nudger.
- Contenu en anglais.
- Anti-bruit avec confirmation `DOWN` après 60 secondes.
- Regroupement des événements simultanés.
- Journalisation des notifications.
- Ajout de tests.

## 1) Autres options possibles

### Option A - Personnalisation uniquement par templates SBA

- Conserver `MailNotifier` standard SBA.
- Personnaliser le rendu via templates Thymeleaf (`status-changed.html` et template texte associé).
- Configurer les propriétés `spring.boot.admin.notify.mail.*`.

**Avantages**

- Mise en œuvre rapide.
- Peu de code Java.

**Limites**

- Faible contrôle sur l'anti-flapping (60 s) et le regroupement multi-instances.
- Logique métier de filtrage/déduplication limitée.

### Option B - Étendre `MailNotifier` (recommandé)

- Créer un notifier custom qui hérite de la mécanique email SBA.
- Gérer explicitement:
  - filtrage prod uniquement,
  - anti-bruit 60 s,
  - regroupement d'événements,
  - génération du modèle de données pour templates HTML/texte,
  - logs structurés.

**Avantages**

- Bon compromis entre maîtrise et effort.
- Compatible avec l'écosystème SBA 3.x.

**Limites**

- Plus de code et de tests.

### Option C - Implémenter un `Notifier` entièrement custom

- Construire un `Notifier` from scratch avec pipeline de notification interne.
- Utiliser un moteur de template libre (Thymeleaf/Mustache/Freemarker).

**Avantages**

- Contrôle maximal.

**Limites**

- Coût de maintenance plus élevé.
- Risque de diverger des comportements SBA standards.

## 2) Option retenue

Option B: **étendre `MailNotifier` avec templates custom HTML + texte**.

Cette option couvre les exigences de lisibilité, anti-bruit, regroupement, et audit, sans reconstruire tout le sous-système de notifications.

## 3) Plan d'implémentation

### Étape 1 - Configuration & feature flags

- Ajouter des propriétés de notification dédiées:
  - activation en production uniquement,
  - base URL SBA (`https://sb-admin.nudger.fr`),
  - délai anti-bruit (`60s`),
  - fenêtre de regroupement.
- Laisser `spring.boot.admin.notify.mail.*` pour la connectivité SMTP (from/to/reply-to).

### Étape 2 - Notifier custom

- Implémenter un notifier custom basé sur SBA 3.x.
- Règles de diffusion:
  - envoyer `DOWN` et `OUT_OF_SERVICE`,
  - envoyer le retour `UP` pour résolution,
  - ignorer beta/hors-prod.
- Ajouter un buffer temporel pour confirmer `DOWN` à +60 s avant envoi.

### Étape 3 - Regroupement des alertes

- Regrouper les événements arrivant dans une même fenêtre temporelle.
- Construire un email unique contenant la liste des instances touchées.
- Ajouter un résumé en tête: nombre de services impactés, premier timestamp, statut dominant.

### Étape 4 - Rendu email

- Adapter le template HTML existant vers un rendu lisible:
  - objet clair,
  - section "Current status" concise,
  - composants health en anomalie uniquement (`DOWN`, `OUT_OF_SERVICE`),
  - liens directs vers page instance SBA,
  - charte minimale Nudger.
- Ajouter un template texte fallback avec les mêmes informations essentielles.

### Étape 5 - Durée et contexte

- Inclure le temps écoulé depuis le dernier état sain (`UP`) ou depuis la dégradation.
- Ajouter host/instance, service name, environnement, timestamp UTC + local.

### Étape 6 - Logging & observabilité

- Journaliser chaque notification envoyée:
  - type d'événement,
  - instances concernées,
  - destinataires,
  - mode grouped/unitaire,
  - délai appliqué.
- Ajouter logs de suppression (événement ignoré par règles) pour audit.

### Étape 7 - Tests

- Tests unitaires:
  - filtrage prod/beta,
  - anti-bruit 60 s,
  - inclusion `DOWN` + `OUT_OF_SERVICE`,
  - génération des liens SBA,
  - calcul de durée.
- Tests de rendu:
  - snapshot HTML,
  - snapshot texte fallback.
- Test d'intégration:
  - scénario multi-instances avec regroupement.

### Étape 8 - Déploiement progressif

- Activer d'abord sur un périmètre réduit en prod.
- Vérifier bruit, lisibilité, et précision des liens.
- Généraliser après validation de l'astreinte.

## 4) Format cible des emails

### Subject

`[Nudger] open4goods-api is DOWN - 777de8a5e899`

### Body (HTML et texte)

- Service: `open4goods-api`
- Instance: `777de8a5e899`
- Status change: `UP -> DOWN`
- Duration: `4m 12s`
- Failed indicators:
  - `reviewGenerationService: DOWN (timeout)`
  - `catalogSync: OUT_OF_SERVICE (dependency unavailable)`
- Links:
  - `Instance: https://sb-admin.nudger.fr/#/instances/<id>/details`
  - `Dashboard: https://sb-admin.nudger.fr`

## 5) Risques et parades

- **Flapping persistant**: augmenter la fenêtre anti-bruit ou introduire un seuil de répétition.
- **Volume en incident majeur**: renforcer le regroupement par lot temporel.
- **Régression template**: snapshots HTML/texte + tests d'intégration SBA.
