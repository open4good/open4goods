# PROMPT : Routes API Stripe pour Upiik

## Contexte

Créer les endpoints REST pour la gestion des paiements et abonnements Stripe, intégrés à l'architecture Express d'Upiik avec authentification JWT.

## Prérequis

✅ Configuration Stripe (`01-config-initiale.md`)
✅ Architecture domaine (`02-architecture-domaine.md`)
✅ Modèles de données (`03-models-donnees.md`)

## Tâche : Implémenter les routes API Stripe

### 1. Routes publiques - Paiements

**Fichier :** `src/domains/payments/routes/paymentsRoutes.js`

**Endpoints à créer :**

```javascript
// Informations utilisateur
GET    /api/payments/subscription-status    # Statut abonnement actuel
GET    /api/payments/payment-methods        # Méthodes de paiement sauvées
GET    /api/payments/invoices               # Historique factures

// Gestion abonnements
POST   /api/payments/subscribe              # Souscrire à un plan (€4 ou €30)
PUT    /api/payments/subscription/modify    # Changer de plan
DELETE /api/payments/subscription/cancel    # Annuler abonnement

// Méthodes de paiement
POST   /api/payments/setup-intent           # Créer SetupIntent pour sauver carte
POST   /api/payments/payment-method         # Ajouter méthode de paiement
DELETE /api/payments/payment-method/:pmId   # Supprimer méthode

// Informations système
GET    /api/payments/plans                  # Plans disponibles (€4/€30)
GET    /api/payments/health                 # Status de l'intégration Stripe
```

### 2. Routes webhooks - Événements Stripe

**Fichier :** `src/domains/payments/routes/webhooksRoutes.js`

**Endpoints webhooks :**

```javascript
POST   /api/webhooks/stripe                 # Réception tous événements Stripe
```

**Événements à gérer :**

- `customer.subscription.created` → Activer abonnement
- `customer.subscription.updated` → Mise à jour statut
- `customer.subscription.deleted` → Annulation
- `invoice.payment_succeeded` → Paiement réussi
- `invoice.payment_failed` → Échec paiement
- `payment_method.attached` → Nouvelle méthode
- `setup_intent.succeeded` → Carte validée

### 3. Middlewares de sécurité

**Créer les middlewares dans :** `src/domains/payments/middlewares/`

**stripeAuth.js :**

```javascript
// Validation signatures webhooks Stripe
const validateStripeSignature = (req, res, next) => {
  // Vérifier signature avec STRIPE_WEBHOOK_SECRET
  // Rejeter si signature invalide
  // Passer le payload vérifié à req.body
}
```

**paymentValidation.js :**

```javascript
// Validation données paiement avec Joi
const validateSubscriptionData = (req, res, next) => {
  // Valider planType ('monthly' | 'yearly')
  // Valider montants (400 centimes ou 3000 centimes)
  // Valider métadonnées utilisateur
}

const validatePaymentMethodData = (req, res, next) => {
  // Valider données carte
  // Vérifier format payment method
}
```

**subscriptionAuth.js :**

```javascript
// Auth spécifique abonnements
const requireActiveUser = (req, res, next) => {
  // Vérifier JWT (utiliser middleware existant)
  // Vérifier que user.isActive
  // Charger données Stripe du user
}

const requireSubscriptionOwner = (req, res, next) => {
  // Vérifier que user possède l'abonnement
  // Bloquer accès aux abonnements d'autres users
}
```

### 4. Controllers - Logique endpoints

**Fichier :** `src/domains/payments/controllers/paymentsController.js`

**Fonctions à implémenter :**

```javascript
// Status et infos
exports.getSubscriptionStatus = async (req, res) => {
  // Retourner statut abonnement + tokens restants
  // Format : { isActive, plan, tokensRemaining, renewsAt }
}

exports.getPaymentMethods = async (req, res) => {
  // Lister méthodes sauvées de l'utilisateur
  // Masquer données sensibles (montrer que last4)
}

exports.getPlans = async (req, res) => {
  // Retourner plans disponibles avec prix
  // { monthly: { price: 400, currency: 'eur' }, yearly: {...} }
}

// Abonnements
exports.subscribe = async (req, res) => {
  // 1. Créer/récupérer Stripe Customer
  // 2. Créer Subscription Stripe
  // 3. Sauver en DB (modèle Subscription)
  // 4. Activer tokens illimités pour user
  // 5. Déclencher événement subscription.created
}

exports.cancelSubscription = async (req, res) => {
  // 1. Annuler dans Stripe
  // 2. Mettre à jour DB
  // 3. Programmer retour plan gratuit à la fin de période
  // 4. Déclencher événement subscription.cancelled
}

// Méthodes de paiement
exports.createSetupIntent = async (req, res) => {
  // Créer SetupIntent Stripe pour sauver carte
  // Retourner client_secret pour frontend
}

exports.attachPaymentMethod = async (req, res) => {
  // Attacher payment method au customer
  // Sauver en DB si défaut
}
```

**Fichier :** `src/domains/payments/controllers/webhooksController.js`

```javascript
exports.handleStripeWebhook = async (req, res) => {
  // 1. Signature déjà validée par middleware
  // 2. Switch sur event.type
  // 3. Appeler handler spécifique selon événement
  // 4. Toujours retourner 200 (même si erreur interne)
  // 5. Logger tous les événements
}

// Handlers spécifiques
exports.handleSubscriptionCreated = async subscription => {
  // Activer abonnement en DB
  // Mettre à jour User.subscription
  // Déclencher événement interne
}

exports.handlePaymentSucceeded = async invoice => {
  // Enregistrer Payment en DB
  // Envoyer email confirmation
  // Renouveler tokens si nécessaire
}
```

### 5. Intégration avec l'authentification JWT

**Utiliser les middlewares existants :**

```javascript
// Dans paymentsRoutes.js
const { authenticateToken } = require('../../user/middlewares/auth') // Adapter selon structure

// Appliquer à toutes les routes protégées
router.use('/api/payments', authenticateToken)
```

### 6. Gestion d'erreurs et logging

**Pattern Upiik existant :**

```javascript
// Utiliser le système d'erreurs centralisé
const { ApiError } = require('../../../errors') // Adapter chemin

// Dans chaque controller
try {
  // Logique métier
} catch (error) {
  // Logger avec Winston configuré
  stripeLogger.error('Subscription creation failed', {
    error,
    userId: req.user.id,
  })

  // Retourner erreur formatée
  throw new ApiError(400, 'SUBSCRIPTION_CREATION_FAILED', error.message)
}
```

### 7. Rate limiting spécifique

**Configuration pour endpoints Stripe :**

```javascript
// Utiliser express-rate-limit existant
const rateLimit = require('express-rate-limit')

const stripeRateLimit = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 10, // 10 tentatives max pour paiements
  message: 'Too many payment attempts, please try again later',
  standardHeaders: true,
  legacyHeaders: false,
})

// Appliquer aux routes sensibles
router.post('/subscribe', stripeRateLimit, paymentsController.subscribe)
```

## Intégration système Upiik

### Tokens et échanges

```javascript
// Dans subscribe controller
// Après création abonnement réussie :
await User.findByIdAndUpdate(userId, {
  'subscription.isActive': true,
  'subscription.type': planType,
  'subscription.tokensRemaining': -1, // -1 = illimité
  'subscription.renewsAt': subscription.current_period_end,
})
```

### Événements internes

```javascript
// Déclencher événements pour le système Upiik
const { eventEmitter } = require('../../../events') // Adapter

eventEmitter.emit('subscription.activated', {
  userId,
  planType,
  stripeSubscriptionId,
})
```

## Critères de réussite

✅ Routes publiques créées et fonctionnelles
✅ Routes webhooks avec validation signature
✅ Middlewares de sécurité implémentés
✅ Controllers avec gestion d'erreurs
✅ Intégration JWT auth existante
✅ Rate limiting configuré
✅ Logs Winston intégrés
✅ Événements internes connectés

## Instructions d'exécution

1. **Examiner** les routes existantes dans `src/domains/user/routes/`
2. **Créer** la structure complète des routes
3. **Implémenter** les controllers de base
4. **Configurer** les middlewares de sécurité
5. **Intégrer** dans le système de routes principal
6. **Tester** les endpoints avec des données factices

**Prochaine étape :** `05-webhooks.md`
