# PROMPT : Gestion des Webhooks Stripe

## Contexte

Implémenter la gestion robuste des webhooks Stripe pour synchroniser en temps réel les événements de paiement avec la logique métier Upiik (tokens, abonnements, notifications).

## Prérequis

✅ Configuration Stripe (`01-config-initiale.md`)
✅ Architecture domaine (`02-architecture-domaine.md`)
✅ Modèles de données (`03-models-donnees.md`)
✅ Routes API (`04-routes-api.md`)

## Tâche : Implémenter les webhooks Stripe

### 1. Service de gestion des webhooks

**Fichier :** `src/domains/payments/services/webhookService.js`

**Fonctionnalités principales :**

```javascript
class WebhookService {
  // Validation et parsing des événements
  async validateAndParseEvent(rawBody, signature) {
    // Valider signature avec STRIPE_WEBHOOK_SECRET
    // Parser l'événement Stripe
    // Vérifier l'idempotence (éviter double traitement)
    // Retourner événement validé
  }

  // Dispatcher principal
  async processEvent(event) {
    // Switch sur event.type
    // Router vers le bon handler
    // Gestion d'erreurs et retry
    // Logging détaillé
  }

  // Handlers spécifiques pour chaque type d'événement
  async handleCustomerSubscriptionCreated(subscription) {
    // Activer l'abonnement en DB
    // Mettre à jour tokens utilisateur
    // Déclencher notifications
  }

  async handleCustomerSubscriptionUpdated(subscription) {
    // Synchroniser changements (plan, statut)
    // Ajuster tokens si changement de plan
  }

  async handleCustomerSubscriptionDeleted(subscription) {
    // Désactiver abonnement
    // Retour au plan gratuit (1 échange/mois)
    // Notification utilisateur
  }

  async handleInvoicePaymentSucceeded(invoice) {
    // Enregistrer paiement en DB
    // Envoyer email de confirmation
    // Renouveler période d'abonnement
  }

  async handleInvoicePaymentFailed(invoice) {
    // Enregistrer échec en DB
    // Notification utilisateur
    // Éventuellement suspendre abonnement
  }

  async handleSetupIntentSucceeded(setupIntent) {
    // Confirmer ajout méthode de paiement
    // Mettre à jour customer
  }
}
```

### 2. Gestion de l'idempotence

**Système anti-doublons :**

```javascript
// Table/Collection pour tracking événements traités
const WebhookEvent = {
  stripeEventId: String, // evt_xyz unique
  eventType: String, // type d'événement
  processedAt: Date, // Date de traitement
  status: String, // 'processed' | 'failed' | 'retrying'
  attempts: Number, // Nombre de tentatives
  lastError: String, // Dernière erreur si échec
  metadata: Object, // Données additionnelles
}

// Dans validateAndParseEvent :
const existingEvent = await WebhookEvent.findOne({ stripeEventId: event.id })
if (existingEvent && existingEvent.status === 'processed') {
  // Événement déjà traité, ignorer
  return { alreadyProcessed: true }
}
```

### 3. Intégration avec le système de tokens Upiik

**Logique d'activation des abonnements :**

```javascript
async handleSubscriptionActivation(stripeSubscription) {
  // 1. Identifier l'utilisateur
  const customer = await StripeCustomer.findOne({
    stripeCustomerId: stripeSubscription.customer
  });

  if (!customer) {
    throw new Error('Customer not found in Upiik DB');
  }

  // 2. Déterminer le type de plan
  const planType = this.determinePlanType(stripeSubscription);

  // 3. Activer tokens illimités
  await User.findByIdAndUpdate(customer.userId, {
    'subscription.isActive': true,
    'subscription.type': planType,
    'subscription.tokensRemaining': -1, // -1 = illimité
    'subscription.renewsAt': new Date(stripeSubscription.current_period_end * 1000)
  });

  // 4. Créer/Mettre à jour l'abonnement en DB
  await Subscription.findOneAndUpdate(
    { stripeSubscriptionId: stripeSubscription.id },
    {
      userId: customer.userId,
      stripeCustomerId: customer.stripeCustomerId,
      planType,
      status: stripeSubscription.status,
      currentPeriodStart: new Date(stripeSubscription.current_period_start * 1000),
      currentPeriodEnd: new Date(stripeSubscription.current_period_end * 1000),
      tokensGranted: { unlimited: true }
    },
    { upsert: true }
  );

  // 5. Déclencher événements internes Upiik
  eventEmitter.emit('subscription.activated', {
    userId: customer.userId,
    planType,
    subscriptionId: stripeSubscription.id
  });
}
```

### 4. Intégration avec le système d'événements Upiik

**Fichier :** `src/domains/payments/events/paymentEvents.js`

**Événements à déclencher :**

```javascript
// Événements pour le système Upiik
const paymentEvents = {
  SUBSCRIPTION_ACTIVATED: 'subscription.activated',
  SUBSCRIPTION_CANCELLED: 'subscription.cancelled',
  PAYMENT_SUCCEEDED: 'payment.succeeded',
  PAYMENT_FAILED: 'payment.failed',
  TOKENS_RENEWED: 'tokens.renewed',
}

// Handlers dans src/events/subscribers/
class SubscriptionSubscriber {
  async onSubscriptionActivated({ userId, planType }) {
    // Envoyer email de bienvenue
    // Notifier via push notification
    // Logger l'activation
  }

  async onSubscriptionCancelled({ userId, endDate }) {
    // Email de confirmation annulation
    // Programmer retour plan gratuit
    // Proposer feedback
  }

  async onPaymentFailed({ userId, reason, amount }) {
    // Email d'alerte paiement échoué
    // Notification push urgente
    // Proposer mise à jour méthode paiement
  }
}
```

### 5. Gestion des notifications utilisateur

**Intégration avec Nodemailer existant :**

```javascript
// Utiliser le système email d'Upiik
const { sendEmail } = require('../../../utils/emailService'); // Adapter chemin

async sendSubscriptionConfirmation(userId, planType) {
  const user = await User.findById(userId);

  await sendEmail({
    to: user.email,
    template: 'subscription-activated', // Template à créer
    data: {
      firstName: user.firstName,
      planType,
      planPrice: planType === 'monthly' ? '4€' : '30€',
      tokensInfo: 'Échanges illimités'
    },
    language: user.preferredLanguage || 'fr'
  });
}

// Intégration push notifications existantes
const { sendPushNotification } = require('../../../services/pushService');

async sendPaymentFailedNotification(userId) {
  await sendPushNotification(userId, {
    title: 'Problème de paiement',
    body: 'Votre paiement a échoué. Vérifiez votre méthode de paiement.',
    data: { type: 'payment_failed', action: 'open_payment_settings' }
  });
}
```

### 6. Système de retry et gestion d'erreurs

**Retry automatique pour échecs temporaires :**

```javascript
class WebhookRetryService {
  async processWithRetry(event, maxRetries = 3) {
    let lastError

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        await this.processEvent(event)

        // Succès : marquer comme traité
        await WebhookEvent.updateOne(
          { stripeEventId: event.id },
          {
            status: 'processed',
            processedAt: new Date(),
            attempts: attempt,
          }
        )

        return { success: true }
      } catch (error) {
        lastError = error

        // Logger la tentative échouée
        webhookLogger.warn(`Webhook retry ${attempt}/${maxRetries}`, {
          eventId: event.id,
          eventType: event.type,
          error: error.message,
        })

        // Attendre avant retry (backoff exponentiel)
        if (attempt < maxRetries) {
          await this.delay(Math.pow(2, attempt) * 1000) // 2s, 4s, 8s
        }
      }
    }

    // Échec définitif après tous les retries
    await WebhookEvent.updateOne(
      { stripeEventId: event.id },
      {
        status: 'failed',
        attempts: maxRetries,
        lastError: lastError.message,
      }
    )

    // Alerter les développeurs
    webhookLogger.error('Webhook processing failed permanently', {
      eventId: event.id,
      eventType: event.type,
      error: lastError.message,
    })
  }
}
```

### 7. Monitoring et alerting

**Métriques importantes à tracker :**

```javascript
// Dashboard metrics
const webhookMetrics = {
  totalEventsReceived: Number,
  totalEventsProcessed: Number,
  totalEventsFailed: Number,
  averageProcessingTime: Number,
  lastEventReceivedAt: Date,
  failureRate: Number,
}

// Health check endpoint
exports.getWebhookHealth = async (req, res) => {
  const last24h = new Date(Date.now() - 24 * 60 * 60 * 1000)

  const stats = await WebhookEvent.aggregate([
    { $match: { processedAt: { $gte: last24h } } },
    {
      $group: {
        _id: '$status',
        count: { $sum: 1 },
      },
    },
  ])

  res.json({
    status: 'healthy', // ou 'degraded' si taux échec > 5%
    last24h: stats,
    lastEventAt: await WebhookEvent.findOne().sort({ processedAt: -1 })
      ?.processedAt,
  })
}
```

## Sécurité et bonnes pratiques

### Validation stricte

```javascript
// Validation signature webhook Stripe
const validateWebhookSignature = (rawBody, signature, secret) => {
  const expectedSig = crypto
    .createHmac('sha256', secret)
    .update(rawBody, 'utf8')
    .digest('hex')

  return crypto.timingSafeEqual(
    Buffer.from(signature, 'hex'),
    Buffer.from(expectedSig, 'hex')
  )
}
```

### Protection contre replay attacks

```javascript
// Vérifier timestamp de l'événement (max 5 minutes)
const eventTimestamp = event.created
const currentTimestamp = Math.floor(Date.now() / 1000)

if (currentTimestamp - eventTimestamp > 300) {
  // 5 minutes
  throw new Error('Event too old, possible replay attack')
}
```

## Critères de réussite

✅ Service webhook complet avec tous les handlers
✅ Système d'idempotence fonctionnel
✅ Intégration avec tokens Upiik
✅ Événements internes connectés
✅ Notifications email/push configurées
✅ Système de retry implémenté
✅ Monitoring et health checks
✅ Sécurité webhook validée

## Instructions d'exécution

1. **Examiner** le système d'événements existant `src/events/`
2. **Implémenter** le service webhook complet
3. **Configurer** les handlers pour chaque type d'événement
4. **Intégrer** avec les notifications existantes
5. **Tester** avec des webhooks Stripe de test
6. **Valider** l'idempotence et la gestion d'erreurs

**Prochaine étape :** `06-integration-tokens.md`
