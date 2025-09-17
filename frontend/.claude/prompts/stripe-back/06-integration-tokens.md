# PROMPT : Intégration Stripe avec le système de tokens Upiik

## Contexte
Intégrer la logique métier Stripe avec le système de tokens existant d'Upiik : plan gratuit (1 échange/mois), plans payants (illimité), gestion des limites et renouvellements.

## Prérequis
✅ Configuration Stripe (`01-config-initiale.md`)
✅ Architecture domaine (`02-architecture-domaine.md`)
✅ Modèles de données (`03-models-donnees.md`)
✅ Routes API (`04-routes-api.md`)
✅ Webhooks (`05-webhooks.md`)

## Tâche : Intégrer Stripe avec la logique tokens Upiik

### 1. Service de gestion des tokens

**Fichier :** `src/domains/payments/services/tokenService.js`

**Logique principale :**
```javascript
class TokenService {
  // Vérifier si un utilisateur peut faire un échange
  async canUserMakeExchange(userId) {
    const user = await User.findById(userId).populate('subscription');

    // Plan payant actif = échanges illimités
    if (user.subscription?.isActive && user.subscription?.type !== 'free') {
      return {
        canExchange: true,
        reason: 'unlimited_plan',
        tokensRemaining: -1 // -1 = illimité
      };
    }

    // Plan gratuit = vérifier limite mensuelle
    const currentMonth = new Date().getMonth();
    const currentYear = new Date().getFullYear();

    const exchangesThisMonth = await this.countExchangesThisMonth(userId, currentYear, currentMonth);

    const freeLimit = 1; // 1 échange gratuit par mois
    const canExchange = exchangesThisMonth < freeLimit;

    return {
      canExchange,
      reason: canExchange ? 'within_free_limit' : 'free_limit_exceeded',
      tokensRemaining: Math.max(0, freeLimit - exchangesThisMonth),
      nextRenewal: this.getNextMonthStart()
    };
  }

  // Compter les échanges du mois pour un utilisateur
  async countExchangesThisMonth(userId, year, month) {
    const startOfMonth = new Date(year, month, 1);
    const endOfMonth = new Date(year, month + 1, 0, 23, 59, 59);

    // Adapter selon votre modèle d'échanges existant
    const exchangeCount = await Exchange.countDocuments({
      $or: [
        { requesterId: userId },
        { ownerId: userId }
      ],
      status: 'completed',
      completedAt: {
        $gte: startOfMonth,
        $lte: endOfMonth
      }
    });

    return exchangeCount;
  }

  // Enregistrer un nouvel échange (à appeler quand un échange est confirmé)
  async recordExchange(exchangeData) {
    const { requesterId, ownerId } = exchangeData;

    // Vérifier que les deux users peuvent encore échanger
    const requesterCheck = await this.canUserMakeExchange(requesterId);
    const ownerCheck = await this.canUserMakeExchange(ownerId);

    if (!requesterCheck.canExchange) {
      throw new Error(`Requester has exceeded exchange limit: ${requesterCheck.reason}`);
    }

    if (!ownerCheck.canExchange) {
      throw new Error(`Owner has exceeded exchange limit: ${ownerCheck.reason}`);
    }

    // Procéder avec l'échange...
    // (logique existante d'Upiik)
  }

  // Activer un abonnement premium (appelé par webhook)
  async activatePremiumSubscription(userId, planType, stripeSubscriptionId) {
    await User.findByIdAndUpdate(userId, {
      'subscription.isActive': true,
      'subscription.type': planType, // 'monthly' | 'yearly'
      'subscription.stripeSubscriptionId': stripeSubscriptionId,
      'subscription.tokensRemaining': -1, // Illimité
      'subscription.renewsAt': this.calculateRenewalDate(planType),
      'subscription.activatedAt': new Date()
    });

    // Déclencher événement pour notifications
    eventEmitter.emit('subscription.premium_activated', {
      userId,
      planType,
      previousLimit: await this.countExchangesThisMonth(userId)
    });
  }

  // Désactiver abonnement (retour plan gratuit)
  async deactivatePremiumSubscription(userId, endDate = null) {
    const user = await User.findById(userId);

    await User.findByIdAndUpdate(userId, {
      'subscription.isActive': false,
      'subscription.type': 'free',
      'subscription.stripeSubscriptionId': null,
      'subscription.tokensRemaining': this.calculateFreeTokensRemaining(userId),
      'subscription.deactivatedAt': new Date(),
      'subscription.gracePeriodEnd': endDate || new Date()
    });

    // Notification à l'utilisateur
    eventEmitter.emit('subscription.reverted_to_free', {
      userId,
      gracePeriodEnd: endDate
    });
  }

  // Calculer tokens gratuits restants pour le mois
  async calculateFreeTokensRemaining(userId) {
    const exchangesThisMonth = await this.countExchangesThisMonth(
      userId,
      new Date().getFullYear(),
      new Date().getMonth()
    );

    return Math.max(0, 1 - exchangesThisMonth); // 1 échange gratuit max
  }
}
```

### 2. Middleware de vérification des échanges

**Fichier :** `src/domains/payments/middlewares/exchangeLimit.js`

**Middleware pour les endpoints d'échange :**
```javascript
const checkExchangeLimit = async (req, res, next) => {
  try {
    const userId = req.user.id; // Depuis JWT auth
    const tokenService = new TokenService();

    const checkResult = await tokenService.canUserMakeExchange(userId);

    if (!checkResult.canExchange) {
      // Enrichir la réponse avec infos sur l'upgrade
      const upgradeInfo = {
        currentPlan: 'free',
        limitation: 'Exchange limit exceeded for free plan',
        availablePlans: [
          {
            type: 'monthly',
            price: 400, // centimes
            currency: 'eur',
            benefits: 'Unlimited exchanges + priority support'
          },
          {
            type: 'yearly',
            price: 3000, // centimes (économie de 2€)
            currency: 'eur',
            benefits: 'Unlimited exchanges + priority support + 17% discount'
          }
        ],
        upgradeUrl: '/api/payments/subscribe'
      };

      return res.status(402).json({ // 402 Payment Required
        error: 'EXCHANGE_LIMIT_EXCEEDED',
        message: checkResult.reason,
        tokensRemaining: checkResult.tokensRemaining,
        nextRenewal: checkResult.nextRenewal,
        upgrade: upgradeInfo
      });
    }

    // Ajouter infos tokens à la requête pour logging
    req.tokenInfo = checkResult;
    next();

  } catch (error) {
    logger.error('Exchange limit check failed', { error, userId: req.user?.id });
    res.status(500).json({ error: 'Token verification failed' });
  }
};

module.exports = { checkExchangeLimit };
```

### 3. Intégration avec les endpoints d'échange existants

**Modification des routes d'échange existantes :**
```javascript
// Dans les routes d'échange existantes (src/domains/products/routes ou équivalent)
const { checkExchangeLimit } = require('../../payments/middlewares/exchangeLimit');

// Appliquer le middleware aux endpoints critiques
router.post('/exchanges/request',
  authenticateToken,           // Auth JWT existante
  checkExchangeLimit,          // Nouveau : vérification tokens
  exchangeController.createRequest
);

router.post('/exchanges/:id/accept',
  authenticateToken,
  checkExchangeLimit,
  exchangeController.acceptRequest
);
```

### 4. Service de synchronisation Stripe ↔ Upiik

**Fichier :** `src/domains/payments/services/syncService.js`

**Tâches de synchronisation :**
```javascript
class SyncService {
  // Synchroniser tous les abonnements (batch quotidien)
  async syncAllSubscriptions() {
    const stripe = require('../../../config/stripe');

    // Récupérer tous les abonnements actifs en DB
    const localSubscriptions = await Subscription.find({ status: 'active' });

    for (const localSub of localSubscriptions) {
      try {
        // Vérifier le statut dans Stripe
        const stripeSub = await stripe.subscriptions.retrieve(localSub.stripeSubscriptionId);

        if (stripeSub.status !== localSub.status) {
          await this.handleStatusMismatch(localSub, stripeSub);
        }

      } catch (error) {
        logger.warn('Sync failed for subscription', {
          subscriptionId: localSub.stripeSubscriptionId,
          error: error.message
        });
      }
    }
  }

  // Gérer les désynchronisations
  async handleStatusMismatch(localSub, stripeSub) {
    logger.info('Status mismatch detected', {
      local: localSub.status,
      stripe: stripeSub.status,
      subscriptionId: stripeSub.id
    });

    // Mettre à jour selon le statut Stripe (source de vérité)
    await Subscription.findByIdAndUpdate(localSub._id, {
      status: stripeSub.status,
      currentPeriodEnd: new Date(stripeSub.current_period_end * 1000),
      syncedAt: new Date()
    });

    // Ajuster les tokens utilisateur
    if (stripeSub.status === 'canceled' || stripeSub.status === 'unpaid') {
      await tokenService.deactivatePremiumSubscription(localSub.userId);
    }
  }

  // Réconcilier un utilisateur spécifique
  async reconcileUser(userId) {
    const user = await User.findById(userId);
    const stripeCustomer = await StripeCustomer.findOne({ userId });

    if (!stripeCustomer) {
      logger.warn('No Stripe customer found', { userId });
      return;
    }

    // Récupérer abonnements Stripe
    const stripe = require('../../../config/stripe');
    const subscriptions = await stripe.subscriptions.list({
      customer: stripeCustomer.stripeCustomerId,
      status: 'active'
    });

    const hasActiveStripeSubscription = subscriptions.data.length > 0;
    const hasActiveLocalSubscription = user.subscription?.isActive;

    // Corriger les incohérences
    if (hasActiveStripeSubscription && !hasActiveLocalSubscription) {
      logger.info('Activating missing local subscription', { userId });
      const stripeSub = subscriptions.data[0];
      await tokenService.activatePremiumSubscription(
        userId,
        this.determinePlanType(stripeSub),
        stripeSub.id
      );
    } else if (!hasActiveStripeSubscription && hasActiveLocalSubscription) {
      logger.info('Deactivating orphaned local subscription', { userId });
      await tokenService.deactivatePremiumSubscription(userId);
    }
  }
}
```

### 5. Interface d'administration

**Extension du domaine admin existant :**

**Fichier :** `src/domains/admin/controllers/paymentsAdminController.js`

```javascript
// Statistiques pour le dashboard admin
exports.getPaymentStats = async (req, res) => {
  const stats = await Promise.all([
    // Revenus du mois
    Payment.aggregate([
      {
        $match: {
          status: 'succeeded',
          createdAt: { $gte: startOfMonth(), $lte: endOfMonth() }
        }
      },
      {
        $group: {
          _id: null,
          totalRevenue: { $sum: '$amount' },
          totalTransactions: { $sum: 1 }
        }
      }
    ]),

    // Répartition des plans
    Subscription.aggregate([
      { $match: { status: 'active' } },
      {
        $group: {
          _id: '$planType',
          count: { $sum: 1 }
        }
      }
    ]),

    // Taux de conversion
    User.aggregate([
      {
        $group: {
          _id: '$subscription.type',
          count: { $sum: 1 }
        }
      }
    ])
  ]);

  res.json({
    revenue: stats[0][0] || { totalRevenue: 0, totalTransactions: 0 },
    planDistribution: stats[1],
    conversionRate: this.calculateConversionRate(stats[2])
  });
};

// Interface de gestion des abonnements
exports.getSubscriptionDetails = async (req, res) => {
  const { userId } = req.params;

  const user = await User.findById(userId).select('email firstName subscription');
  const stripeCustomer = await StripeCustomer.findOne({ userId });
  const subscription = await Subscription.findOne({ userId });
  const payments = await Payment.find({ userId }).sort({ createdAt: -1 }).limit(10);

  res.json({
    user,
    stripeCustomer,
    subscription,
    recentPayments: payments
  });
};

// Actions admin sur les abonnements
exports.refundPayment = async (req, res) => {
  const { paymentId, reason } = req.body;
  const stripe = require('../../../config/stripe');

  const payment = await Payment.findById(paymentId);
  if (!payment) {
    return res.status(404).json({ error: 'Payment not found' });
  }

  // Effectuer le remboursement dans Stripe
  const refund = await stripe.refunds.create({
    payment_intent: payment.stripePaymentIntentId,
    reason: reason || 'requested_by_customer'
  });

  // Mettre à jour en DB
  await Payment.findByIdAndUpdate(paymentId, {
    'refunded.isRefunded': true,
    'refunded.refundAmount': refund.amount,
    'refunded.refundReason': reason,
    'refunded.refundDate': new Date()
  });

  res.json({ success: true, refund });
};
```

### 6. Tâches cron pour la maintenance

**Fichier :** `src/batchs/paymentMaintenance.js`

```javascript
// Tâche quotidienne de maintenance
const dailyPaymentMaintenance = async () => {
  logger.info('Starting daily payment maintenance');

  try {
    // 1. Synchroniser les abonnements
    const syncService = new SyncService();
    await syncService.syncAllSubscriptions();

    // 2. Nettoyer les tokens expirés (si applicable)
    await TokenService.cleanupExpiredTokens();

    // 3. Envoyer rappels de renouvellement
    await this.sendRenewalReminders();

    // 4. Générer rapport quotidien
    await this.generateDailyReport();

    logger.info('Daily payment maintenance completed');
  } catch (error) {
    logger.error('Daily payment maintenance failed', { error });
  }
};

// Programmer avec cron (si utilisé dans Upiik)
const cron = require('cron');
const maintenanceJob = new cron.CronJob(
  '0 2 * * *', // Tous les jours à 2h du matin
  dailyPaymentMaintenance,
  null,
  true,
  'Europe/Paris'
);
```

## Critères de réussite
✅ Service TokenService complet et fonctionnel
✅ Middleware de vérification des échanges
✅ Intégration avec les endpoints d'échange existants
✅ Service de synchronisation Stripe ↔ Upiik
✅ Interface d'administration étendue
✅ Tâches de maintenance automatisées
✅ Gestion des cas limites (désynchronisation, etc.)
✅ Logs et monitoring des opérations tokens

## Instructions d'exécution
1. **Identifier** les modèles d'échange existants dans Upiik
2. **Implémenter** le service TokenService
3. **Créer** le middleware de vérification des limites
4. **Intégrer** avec les routes d'échange existantes
5. **Tester** les scénarios : gratuit, premium, limites
6. **Configurer** les tâches de synchronisation

**Prochaine étape :** `07-tests.md`