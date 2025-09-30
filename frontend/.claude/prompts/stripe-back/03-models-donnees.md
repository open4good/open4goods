# PROMPT : Modèles de données Stripe + Upiik

## Contexte

Créer les modèles Mongoose pour l'intégration Stripe, en liaison avec les modèles User existants. Architecture multi-DB d'Upiik à respecter.

## Prérequis

✅ Configuration Stripe (`01-config-initiale.md`)
✅ Architecture domaine (`02-architecture-domaine.md`)

## Tâche : Créer les modèles de données Stripe

### 1. Modèle StripeCustomer (Liaison User ↔ Stripe)

**Fichier :** `src/domains/payments/models/StripeCustomer.js`

**Spécifications :**

```javascript
// Liaison 1:1 avec User existant
{
  userId: ObjectId,           // Référence vers User._id
  stripeCustomerId: String,   // customer_xyz de Stripe
  email: String,              // Email synchronisé
  metadata: {                 // Métadonnées Upiik
    city: String,             // Ville de l'utilisateur
    registrationDate: Date,   // Date inscription Upiik
    totalExchanges: Number    // Historique échanges
  },
  defaultPaymentMethod: String, // pm_xyz par défaut
  billingAddress: {           // Adresse facturation
    line1: String,
    line2: String,
    city: String,
    postal_code: String,
    country: String
  },
  createdAt: Date,
  updatedAt: Date
}
```

### 2. Modèle Subscription (Abonnements)

**Fichier :** `src/domains/payments/models/Subscription.js`

**Spécifications :**

```javascript
// Abonnements Upiik (€4/mois, €30/an)
{
  userId: ObjectId,           // Référence User
  stripeCustomerId: String,   // customer_xyz
  stripeSubscriptionId: String, // sub_xyz
  planType: String,           // 'monthly' | 'yearly'
  status: String,             // 'active' | 'canceled' | 'past_due'
  currentPeriodStart: Date,   // Période actuelle
  currentPeriodEnd: Date,
  cancelAtPeriodEnd: Boolean, // Annulation programmée
  priceId: String,           // price_xyz de Stripe
  tokensGranted: {           // Tokens accordés par l'abonnement
    unlimited: Boolean,       // true pour plans payants
    monthlyLimit: Number,     // limite si gratuit
    currentCount: Number      // compteur actuel
  },
  metadata: {
    upiikPlan: String,       // 'free' | 'monthly' | 'yearly'
    promotionCode: String,   // Code promo éventuel
    sourceApp: String        // 'web' | 'mobile'
  },
  createdAt: Date,
  updatedAt: Date
}
```

### 3. Modèle Payment (Historique)

**Fichier :** `src/domains/payments/models/Payment.js`

**Spécifications :**

```javascript
// Historique de tous les paiements
{
  userId: ObjectId,           // Référence User
  stripeCustomerId: String,   // customer_xyz
  stripePaymentIntentId: String, // pi_xyz
  amount: Number,             // Montant en centimes
  currency: String,           // 'eur'
  status: String,             // 'succeeded' | 'failed' | 'pending'
  paymentMethod: {
    type: String,             // 'card' | 'sepa_debit'
    brand: String,            // 'visa' | 'mastercard'
    last4: String,            // 4 derniers chiffres
    expiryMonth: Number,
    expiryYear: Number
  },
  relatedSubscription: ObjectId, // Référence Subscription si applicable
  failureReason: String,      // Raison échec si applicable
  refunded: {
    isRefunded: Boolean,
    refundAmount: Number,
    refundReason: String,
    refundDate: Date
  },
  metadata: {
    planType: String,         // Type d'abonnement payé
    promoApplied: String,     // Promo utilisée
    invoiceUrl: String        // URL facture Stripe
  },
  createdAt: Date,
  updatedAt: Date
}
```

### 4. Extension modèle User existant

**Fichier à modifier :** Trouver le modèle User existant et ajouter :

```javascript
// Champs à ajouter au modèle User
{
  // ... champs existants
  stripeCustomerId: String,    // Référence rapide vers Stripe
  subscription: {
    isActive: Boolean,         // Abonnement actif
    type: String,             // 'free' | 'monthly' | 'yearly'
    tokensRemaining: Number,   // Tokens restants ce mois
    renewsAt: Date,           // Prochaine facturation
    lastPayment: Date         // Dernier paiement réussi
  },
  paymentPreferences: {
    defaultPaymentMethod: String, // pm_xyz préféré
    billingEmail: String,        // Email facturation si différent
    receiveInvoices: Boolean     // Recevoir factures par email
  }
}
```

### 5. Indexes et relations

**Pour chaque modèle, ajouter :**

```javascript
// Indexes pour performances
StripeCustomer: (userId(unique), stripeCustomerId(unique))
Subscription: (userId, stripeSubscriptionId(unique), status)
Payment: (userId, stripePaymentIntentId(unique), createdAt)
```

**Relations à configurer :**

```javascript
// Références croisées
User.stripeCustomerId → StripeCustomer.stripeCustomerId
StripeCustomer.userId → User._id
Subscription.userId → User._id
Payment.userId → User._id
Payment.relatedSubscription → Subscription._id
```

### 6. Base de données multi-DB

**Connexions à utiliser :**

```javascript
// Examiner src/config/database.js pour les connexions
- Clients DB → StripeCustomer, Subscription
- Sessions DB → Cache temporaire des payment intents
- Products DB → Potentielle liaison avec items premium
```

### 7. Validation Mongoose + Joi

**Chaque modèle doit avoir :**

- Validation Mongoose (types, required, enum)
- Schémas Joi pour validation API
- Méthodes d'instance utiles
- Hooks pre/post save si nécessaire

## Contraintes spéciales Upiik

- **Multi-villes** : Metadata city importante
- **Système tokens** : Intégration avec logique existante
- **RGPD** : Champs sensibles chiffrés si nécessaire
- **Performances** : Indexes optimisés pour requêtes fréquentes

## Critères de réussite

✅ Modèles StripeCustomer, Subscription, Payment créés
✅ Modèle User étendu avec champs Stripe
✅ Indexes configurés pour performances
✅ Relations entre modèles établies
✅ Validation Mongoose + Joi implémentée
✅ Compatible avec l'architecture multi-DB

## Instructions d'exécution

1. **Examiner** les modèles existants dans `src/domains/user/models/`
2. **Identifier** le modèle User actuel à étendre
3. **Créer** les 3 nouveaux modèles Stripe
4. **Configurer** les relations et indexes
5. **Tester** la création/lecture de base des modèles

**Prochaine étape :** `04-routes-api.md`
