# PROMPT : Architecture du domaine Payments

## Contexte

Créer la structure du domaine `payments` en suivant l'architecture DDD d'Upiik existante. Le domaine doit s'intégrer naturellement avec `user/`, `products/`, `chats/`, etc.

## Prérequis

✅ Configuration Stripe terminée (`01-config-initiale.md`)

## Tâche : Créer l'architecture du domaine payments

### 1. Structure des dossiers à créer

```
src/domains/payments/
├── controllers/
│   ├── paymentsController.js        # Endpoints publics API
│   ├── subscriptionsController.js   # Gestion abonnements
│   └── webhooksController.js        # Réception webhooks Stripe
├── services/
│   ├── stripeService.js            # Interface avec Stripe SDK
│   ├── subscriptionService.js      # Logique abonnements
│   ├── customerService.js          # Gestion customers Stripe
│   └── paymentService.js           # Logique paiements
├── routes/
│   ├── paymentsRoutes.js           # Routes publiques
│   ├── webhooksRoutes.js           # Routes webhooks
│   └── index.js                    # Export toutes les routes
├── models/
│   ├── StripeCustomer.js           # Liaison User ↔ Stripe
│   ├── Payment.js                  # Historique transactions
│   ├── Subscription.js             # Abonnements actifs
│   └── index.js                    # Export tous les modèles
├── middlewares/
│   ├── stripeAuth.js               # Validation signatures webhooks
│   ├── paymentValidation.js        # Validation données paiement
│   └── subscriptionAuth.js         # Auth pour endpoints abonnements
├── events/
│   ├── paymentEvents.js            # Events internes paiements
│   └── subscriptionEvents.js       # Events abonnements
└── utils/
    ├── stripeHelpers.js            # Utilitaires Stripe
    ├── paymentHelpers.js           # Helpers paiements
    └── validators.js               # Validateurs Joi spécifiques
```

### 2. Intégration avec l'architecture existante

**Examiner ces fichiers pour comprendre les patterns :**

- `src/domains/user/` - Structure de référence
- `src/domains/products/` - Pattern controllers/services
- `src/events/` - Système d'événements global
- `src/routes/index.js` - Enregistrement des routes

### 3. Points d'intégration critiques

**Routes principales :**

```javascript
// À intégrer dans src/routes/index.js
app.use('/api/payments', paymentsRoutes)
app.use('/api/webhooks/stripe', webhooksRoutes)
```

**Événements à connecter :**

```javascript
// Intégrer avec src/events/
- payment.succeeded → Notification user
- subscription.created → Activation tokens
- subscription.cancelled → Retour plan gratuit
```

**Base de données :**

```javascript
// Utiliser les connexions existantes
- Clients DB → Customers et Subscriptions
- Sessions DB (Redis) → Cache des états Stripe
```

### 4. Middlewares de sécurité

**Réutiliser l'existant :**

- JWT auth pour endpoints protégés
- Rate limiting (express-rate-limit configuré)
- Validation Joi (patterns existants)
- Helmet + CORS (déjà actifs)

**Ajouter spécifique Stripe :**

- Validation signatures webhooks
- Validation montants/devises
- Auth spécifique abonnements

### 5. Système de logs

**Intégrer avec Winston existant :**

```javascript
// Loggers spécialisés
const stripeLogger = logger.child({ domain: 'payments', service: 'stripe' })
const paymentLogger = logger.child({ domain: 'payments', service: 'payments' })
```

## Contraintes techniques

- **Node.js 16.20.2** : Compatible
- **Architecture DDD** : Suit les patterns Upiik
- **Multi-DB** : Compatible avec l'architecture existante
- **Événements** : Intégré au système global

## Critères de réussite

✅ Structure `src/domains/payments/` créée
✅ Fichiers squelettes avec exports corrects
✅ Intégration routes dans `src/routes/index.js`
✅ Middlewares de base configurés
✅ Connexion au système d'événements
✅ Logs Winston configurés pour le domaine

## Instructions d'exécution

1. **Examiner** `src/domains/user/` pour comprendre la structure
2. **Créer** la structure complète du domaine payments
3. **Configurer** les exports et imports de base
4. **Intégrer** avec le système de routes existant
5. **Tester** que l'architecture se charge sans erreur

**Prochaine étape :** `03-models-donnees.md`
