# PROMPT : Déploiement et Monitoring Stripe en Production

## Contexte
Configurer le déploiement sécurisé de l'intégration Stripe en production, avec monitoring, alertes et procédures de rollback pour l'environnement Upiik.

## Prérequis
✅ Configuration Stripe (`01-config-initiale.md`)
✅ Architecture domaine (`02-architecture-domaine.md`)
✅ Modèles de données (`03-models-donnees.md`)
✅ Routes API (`04-routes-api.md`)
✅ Webhooks (`05-webhooks.md`)
✅ Intégration tokens (`06-integration-tokens.md`)
✅ Tests (`07-tests.md`)

## Tâche : Préparer et déployer Stripe en production

### 1. Configuration de production

**Variables d'environnement production :**

**Modifier `env/production.env` :**
```env
# Stripe Production Configuration
STRIPE_SECRET_KEY=sk_live_xxxxx                # Clé secrète LIVE
STRIPE_PUBLISHABLE_KEY=pk_live_xxxxx           # Clé publique LIVE
STRIPE_WEBHOOK_SECRET=whsec_xxxxx              # Secret webhook LIVE
STRIPE_API_VERSION=2023-10-16                  # Version fixe

# Stripe Product IDs (à créer dans dashboard Stripe)
STRIPE_PRICE_MONTHLY=price_xxxxx               # Prix €4/mois
STRIPE_PRICE_YEARLY=price_xxxxx                # Prix €30/an

# URLs de webhook production
STRIPE_WEBHOOK_URL=https://api.upiik.com/api/webhooks/stripe

# Sécurité renforcée
STRIPE_RATE_LIMIT_WINDOW=900000                # 15 minutes
STRIPE_RATE_LIMIT_MAX=5                        # 5 tentatives max

# Monitoring
STRIPE_ALERT_EMAIL=admin@upiik.com
STRIPE_MONITORING_ENABLED=true
```

### 2. Migration des clés test vers live

**Script de validation :** `scripts/validateStripeProduction.js`

```javascript
const stripe = require('stripe')(process.env.STRIPE_SECRET_KEY);

async function validateStripeConfiguration() {
  console.log('🔍 Validating Stripe production configuration...');

  try {
    // 1. Tester la connexion avec clé live
    const account = await stripe.accounts.retrieve();
    console.log('✅ Stripe account connected:', account.display_name);

    // 2. Vérifier que les produits existent
    const monthlyPrice = await stripe.prices.retrieve(process.env.STRIPE_PRICE_MONTHLY);
    const yearlyPrice = await stripe.prices.retrieve(process.env.STRIPE_PRICE_YEARLY);

    console.log('✅ Monthly price configured:', monthlyPrice.unit_amount, monthlyPrice.currency);
    console.log('✅ Yearly price configured:', yearlyPrice.unit_amount, yearlyPrice.currency);

    // 3. Tester webhook endpoint
    const webhookTest = await fetch(process.env.STRIPE_WEBHOOK_URL, {
      method: 'GET'
    });

    if (webhookTest.status === 405) { // Method not allowed = endpoint existe
      console.log('✅ Webhook endpoint accessible');
    } else {
      console.log('⚠️ Webhook endpoint might not be configured correctly');
    }

    // 4. Vérifier configuration webhook dans Stripe
    const webhooks = await stripe.webhookEndpoints.list();
    const productionWebhook = webhooks.data.find(
      wh => wh.url === process.env.STRIPE_WEBHOOK_URL
    );

    if (productionWebhook) {
      console.log('✅ Webhook configured in Stripe dashboard');
      console.log('   Events:', productionWebhook.enabled_events);
    } else {
      console.log('❌ Webhook NOT configured in Stripe dashboard');
      console.log('   Please create webhook endpoint:', process.env.STRIPE_WEBHOOK_URL);
    }

    console.log('\n🎉 Stripe production validation completed successfully!');
    return true;

  } catch (error) {
    console.error('❌ Stripe validation failed:', error.message);
    return false;
  }
}

// Exécuter si script appelé directement
if (require.main === module) {
  validateStripeConfiguration().then(success => {
    process.exit(success ? 0 : 1);
  });
}

module.exports = { validateStripeConfiguration };
```

### 3. Configuration des webhooks en production

**Endpoints webhooks à créer dans le dashboard Stripe :**

```
URL: https://api.upiik.com/api/webhooks/stripe
Events à écouter:
- customer.subscription.created
- customer.subscription.updated
- customer.subscription.deleted
- invoice.payment_succeeded
- invoice.payment_failed
- setup_intent.succeeded
- payment_intent.succeeded
- payment_intent.payment_failed
```

**Script de configuration automatique :**
```javascript
// scripts/setupProductionWebhooks.js
async function setupProductionWebhooks() {
  const stripe = require('stripe')(process.env.STRIPE_SECRET_KEY);

  const webhookData = {
    url: process.env.STRIPE_WEBHOOK_URL,
    enabled_events: [
      'customer.subscription.created',
      'customer.subscription.updated',
      'customer.subscription.deleted',
      'invoice.payment_succeeded',
      'invoice.payment_failed',
      'setup_intent.succeeded',
      'payment_intent.succeeded',
      'payment_intent.payment_failed'
    ],
    description: 'Upiik Production Webhook'
  };

  try {
    const webhook = await stripe.webhookEndpoints.create(webhookData);
    console.log('✅ Production webhook created:', webhook.id);
    console.log('🔑 Webhook secret:', webhook.secret);
    console.log('⚠️ UPDATE your STRIPE_WEBHOOK_SECRET environment variable!');

    return webhook;
  } catch (error) {
    console.error('❌ Failed to create webhook:', error.message);
    throw error;
  }
}
```

### 4. Monitoring et alertes

**Service de monitoring :** `src/domains/payments/services/monitoringService.js`

```javascript
class PaymentMonitoringService {
  constructor() {
    this.metrics = {
      totalPayments: 0,
      successfulPayments: 0,
      failedPayments: 0,
      webhookEvents: 0,
      failedWebhooks: 0,
      lastPaymentAt: null,
      lastWebhookAt: null
    };
  }

  // Enregistrer une métrique de paiement
  recordPayment(success, amount, error = null) {
    this.metrics.totalPayments++;
    this.metrics.lastPaymentAt = new Date();

    if (success) {
      this.metrics.successfulPayments++;
    } else {
      this.metrics.failedPayments++;
      this.alertPaymentFailure(amount, error);
    }

    // Persister les métriques
    this.persistMetrics();
  }

  // Enregistrer un webhook
  recordWebhook(success, eventType, error = null) {
    this.metrics.webhookEvents++;
    this.metrics.lastWebhookAt = new Date();

    if (!success) {
      this.metrics.failedWebhooks++;
      this.alertWebhookFailure(eventType, error);
    }

    this.persistMetrics();
  }

  // Alerte pour échec de paiement
  async alertPaymentFailure(amount, error) {
    const alertData = {
      type: 'payment_failure',
      severity: 'high',
      amount: amount / 100, // Convertir centimes en euros
      error: error.message,
      timestamp: new Date(),
      environment: process.env.NODE_ENV
    };

    // Email d'alerte
    await this.sendAlert(alertData);

    // Log structuré
    logger.error('Payment failure detected', alertData);
  }

  // Alerte pour échec webhook
  async alertWebhookFailure(eventType, error) {
    const alertData = {
      type: 'webhook_failure',
      severity: 'medium',
      eventType,
      error: error.message,
      timestamp: new Date()
    };

    await this.sendAlert(alertData);
    logger.warn('Webhook processing failed', alertData);
  }

  // Health check endpoint
  getHealthStatus() {
    const now = new Date();
    const last24h = new Date(now - 24 * 60 * 60 * 1000);

    // Vérifier si on a eu des événements récents
    const hasRecentActivity = this.metrics.lastWebhookAt > last24h;

    // Calculer taux d'échec
    const failureRate = this.metrics.totalPayments > 0
      ? (this.metrics.failedPayments / this.metrics.totalPayments) * 100
      : 0;

    const webhookFailureRate = this.metrics.webhookEvents > 0
      ? (this.metrics.failedWebhooks / this.metrics.webhookEvents) * 100
      : 0;

    // Déterminer status global
    let status = 'healthy';
    if (failureRate > 10 || webhookFailureRate > 5) {
      status = 'degraded';
    }
    if (failureRate > 25 || webhookFailureRate > 15) {
      status = 'critical';
    }

    return {
      status,
      metrics: this.metrics,
      rates: {
        paymentFailureRate: failureRate,
        webhookFailureRate: webhookFailureRate
      },
      lastActivity: {
        hasRecentActivity,
        lastPayment: this.metrics.lastPaymentAt,
        lastWebhook: this.metrics.lastWebhookAt
      }
    };
  }

  // Endpoint API pour health check
  async getHealthCheckEndpoint(req, res) {
    try {
      const health = this.getHealthStatus();

      const httpStatus = {
        'healthy': 200,
        'degraded': 200,
        'critical': 503
      }[health.status] || 500;

      res.status(httpStatus).json(health);
    } catch (error) {
      res.status(500).json({
        status: 'error',
        error: error.message
      });
    }
  }
}
```

### 5. Dashboard d'administration

**Extension du domaine admin existant :**

**Fichier :** `src/domains/admin/controllers/paymentsDashboardController.js`

```javascript
exports.getPaymentsDashboard = async (req, res) => {
  try {
    const timeframe = req.query.timeframe || '30d'; // 7d, 30d, 90d
    const endDate = new Date();
    const startDate = new Date();

    switch (timeframe) {
      case '7d':
        startDate.setDate(endDate.getDate() - 7);
        break;
      case '30d':
        startDate.setDate(endDate.getDate() - 30);
        break;
      case '90d':
        startDate.setDate(endDate.getDate() - 90);
        break;
    }

    // Métriques de revenus
    const revenueStats = await Payment.aggregate([
      {
        $match: {
          status: 'succeeded',
          createdAt: { $gte: startDate, $lte: endDate }
        }
      },
      {
        $group: {
          _id: {
            $dateToString: { format: '%Y-%m-%d', date: '$createdAt' }
          },
          totalRevenue: { $sum: '$amount' },
          transactionCount: { $sum: 1 }
        }
      },
      { $sort: { '_id': 1 } }
    ]);

    // Répartition des abonnements
    const subscriptionStats = await Subscription.aggregate([
      {
        $match: { status: 'active' }
      },
      {
        $group: {
          _id: '$planType',
          count: { $sum: 1 },
          revenue: { $sum: '$amount' }
        }
      }
    ]);

    // Taux de conversion
    const totalUsers = await User.countDocuments();
    const premiumUsers = await User.countDocuments({
      'subscription.isActive': true
    });

    // Top 10 des villes par revenus
    const topCities = await Payment.aggregate([
      {
        $match: {
          status: 'succeeded',
          createdAt: { $gte: startDate }
        }
      },
      {
        $lookup: {
          from: 'users',
          localField: 'userId',
          foreignField: '_id',
          as: 'user'
        }
      },
      {
        $group: {
          _id: '$user.city',
          revenue: { $sum: '$amount' },
          transactions: { $sum: 1 }
        }
      },
      { $sort: { revenue: -1 } },
      { $limit: 10 }
    ]);

    res.json({
      period: { start: startDate, end: endDate, timeframe },
      revenue: {
        total: revenueStats.reduce((sum, day) => sum + day.totalRevenue, 0),
        daily: revenueStats
      },
      subscriptions: {
        active: subscriptionStats,
        conversionRate: (premiumUsers / totalUsers) * 100
      },
      geographic: topCities,
      summary: {
        totalUsers,
        premiumUsers,
        freeUsers: totalUsers - premiumUsers
      }
    });

  } catch (error) {
    logger.error('Dashboard data fetch failed', { error });
    res.status(500).json({ error: 'Failed to fetch dashboard data' });
  }
};

// Endpoint pour les actions admin
exports.adminActions = {
  // Forcer synchronisation d'un utilisateur
  async forceSyncUser(req, res) {
    const { userId } = req.params;
    const syncService = new SyncService();

    try {
      await syncService.reconcileUser(userId);
      res.json({ success: true, message: 'User synchronized' });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  },

  // Statistiques détaillées d'un utilisateur
  async getUserPaymentDetails(req, res) {
    const { userId } = req.params;

    try {
      const user = await User.findById(userId);
      const stripeCustomer = await StripeCustomer.findOne({ userId });
      const subscription = await Subscription.findOne({ userId });
      const payments = await Payment.find({ userId }).sort({ createdAt: -1 });

      // Données Stripe live si disponibles
      let stripeData = null;
      if (stripeCustomer?.stripeCustomerId) {
        const stripe = require('../../../config/stripe');
        stripeData = await stripe.customers.retrieve(stripeCustomer.stripeCustomerId);
      }

      res.json({
        user: user.toObject(),
        stripeCustomer,
        subscription,
        payments,
        stripeData
      });
    } catch (error) {
      res.status(500).json({ error: error.message });
    }
  }
};
```

### 6. Procédures de rollback

**Script de rollback :** `scripts/rollbackStripe.js`

```javascript
async function rollbackStripe(options = {}) {
  console.log('🚨 Starting Stripe rollback procedure...');

  const {
    disableWebhooks = true,
    pauseNewSubscriptions = true,
    notifyUsers = false
  } = options;

  try {
    // 1. Désactiver les webhooks temporairement
    if (disableWebhooks) {
      await disableStripeWebhooks();
      console.log('✅ Webhooks disabled');
    }

    // 2. Passer les routes en mode maintenance
    if (pauseNewSubscriptions) {
      await enableMaintenanceMode();
      console.log('✅ New subscriptions paused');
    }

    // 3. Sauvegarder l'état actuel
    await backupCurrentState();
    console.log('✅ Current state backed up');

    // 4. Notifier les utilisateurs si nécessaire
    if (notifyUsers) {
      await notifyUsersOfMaintenance();
      console.log('✅ Users notified');
    }

    console.log('🎉 Rollback completed successfully');
    console.log('⚠️ Remember to re-enable webhooks when ready');

  } catch (error) {
    console.error('❌ Rollback failed:', error.message);
    throw error;
  }
}

async function disableStripeWebhooks() {
  const stripe = require('stripe')(process.env.STRIPE_SECRET_KEY);
  const webhooks = await stripe.webhookEndpoints.list();

  for (const webhook of webhooks.data) {
    if (webhook.url.includes('upiik.com')) {
      await stripe.webhookEndpoints.update(webhook.id, {
        disabled: true
      });
    }
  }
}

async function enableMaintenanceMode() {
  // Créer flag de maintenance
  await redis.set('stripe:maintenance', true, 'EX', 3600); // 1 heure
}
```

### 7. Sécurité production

**Fichier :** `src/domains/payments/middlewares/productionSecurity.js`

```javascript
// Validation stricte en production
const productionValidation = (req, res, next) => {
  // Vérifier que les clés sont bien en mode live
  if (process.env.NODE_ENV === 'production') {
    if (!process.env.STRIPE_SECRET_KEY?.startsWith('sk_live_')) {
      throw new Error('Production requires live Stripe keys');
    }
  }

  // Rate limiting renforcé pour la production
  const productionRateLimit = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: 5, // Limite très stricte
    message: 'Too many payment requests',
    standardHeaders: true
  });

  return productionRateLimit(req, res, next);
};

// Logging sécurisé (masquer données sensibles)
const secureLogger = (req, res, next) => {
  const originalJson = res.json;
  res.json = function(body) {
    // Masquer les données sensibles dans les logs
    const sanitized = JSON.parse(JSON.stringify(body));
    if (sanitized.client_secret) {
      sanitized.client_secret = '***HIDDEN***';
    }
    if (sanitized.payment_method) {
      sanitized.payment_method = '***HIDDEN***';
    }

    logger.info('API Response', {
      method: req.method,
      url: req.url,
      status: res.statusCode,
      response: sanitized
    });

    return originalJson.call(this, body);
  };

  next();
};
```

### 8. Checklist de déploiement

**Documentation :** `docs/stripe-deployment-checklist.md`

```markdown
# Checklist de Déploiement Stripe

## Pré-déploiement
- [ ] Tests passent à 100% (unit + integration + e2e)
- [ ] Validation des clés Stripe live
- [ ] Configuration webhook production
- [ ] Variables d'environnement mises à jour
- [ ] Script de validation exécuté avec succès

## Déploiement
- [ ] Sauvegarde de l'état actuel
- [ ] Déploiement code en production
- [ ] Validation endpoints accessibles
- [ ] Test webhook endpoint
- [ ] Validation dashboard admin

## Post-déploiement
- [ ] Monitoring actif et alertes configurées
- [ ] Test transaction test en production
- [ ] Validation complète du flux utilisateur
- [ ] Documentation mise à jour
- [ ] Équipe formée aux nouveaux outils

## Rollback (si nécessaire)
- [ ] Désactivation webhooks
- [ ] Mode maintenance activé
- [ ] Notification utilisateurs
- [ ] Restauration version précédente
- [ ] Validation fonctionnement normal
```

## Critères de réussite
✅ Configuration production sécurisée
✅ Scripts de validation et déploiement
✅ Webhooks production configurés
✅ Monitoring et alertes actifs
✅ Dashboard admin fonctionnel
✅ Procédures de rollback documentées
✅ Sécurité production renforcée
✅ Checklist de déploiement suivie

## Instructions d'exécution
1. **Configurer** les variables de production
2. **Créer** les webhooks dans le dashboard Stripe
3. **Implémenter** le monitoring et les alertes
4. **Étendre** le dashboard admin existant
5. **Tester** les procédures de rollback
6. **Documenter** le processus de déploiement
7. **Former** l'équipe aux nouveaux outils

**Intégration Stripe complète ! 🎉**