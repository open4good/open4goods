# PROMPT : Tests pour l'intégration Stripe

## Contexte
Créer une suite de tests complète pour valider l'intégration Stripe avec Jest (déjà configuré dans Upiik). Tests unitaires, d'intégration et end-to-end pour tous les composants Stripe.

## Prérequis
✅ Configuration Stripe (`01-config-initiale.md`)
✅ Architecture domaine (`02-architecture-domaine.md`)
✅ Modèles de données (`03-models-donnees.md`)
✅ Routes API (`04-routes-api.md`)
✅ Webhooks (`05-webhooks.md`)
✅ Intégration tokens (`06-integration-tokens.md`)

## Tâche : Implémenter les tests Stripe

### 1. Configuration des tests Stripe

**Fichier :** `__tests__/setup/stripeTestSetup.js`

**Configuration Jest pour Stripe :**
```javascript
// Mock Stripe SDK pour les tests
jest.mock('stripe', () => {
  return jest.fn().mockImplementation(() => ({
    customers: {
      create: jest.fn(),
      retrieve: jest.fn(),
      update: jest.fn(),
      list: jest.fn()
    },
    subscriptions: {
      create: jest.fn(),
      retrieve: jest.fn(),
      update: jest.fn(),
      cancel: jest.fn(),
      list: jest.fn()
    },
    paymentIntents: {
      create: jest.fn(),
      confirm: jest.fn(),
      retrieve: jest.fn()
    },
    setupIntents: {
      create: jest.fn(),
      confirm: jest.fn()
    },
    paymentMethods: {
      attach: jest.fn(),
      detach: jest.fn(),
      list: jest.fn()
    },
    invoices: {
      retrieve: jest.fn(),
      list: jest.fn()
    },
    webhooks: {
      constructEvent: jest.fn()
    }
  }));
});

// Helper pour créer des données de test Stripe
class StripeTestHelpers {
  static createMockCustomer(overrides = {}) {
    return {
      id: 'cus_test123',
      email: 'test@upiik.com',
      created: Math.floor(Date.now() / 1000),
      metadata: {},
      ...overrides
    };
  }

  static createMockSubscription(overrides = {}) {
    const now = Math.floor(Date.now() / 1000);
    return {
      id: 'sub_test123',
      customer: 'cus_test123',
      status: 'active',
      current_period_start: now,
      current_period_end: now + (30 * 24 * 60 * 60), // +30 jours
      items: {
        data: [{
          price: {
            id: 'price_monthly_test',
            unit_amount: 400,
            currency: 'eur'
          }
        }]
      },
      ...overrides
    };
  }

  static createMockPaymentIntent(overrides = {}) {
    return {
      id: 'pi_test123',
      amount: 400,
      currency: 'eur',
      status: 'succeeded',
      customer: 'cus_test123',
      payment_method: 'pm_test123',
      ...overrides
    };
  }

  static createMockWebhookEvent(type, data) {
    return {
      id: 'evt_test123',
      type,
      data: { object: data },
      created: Math.floor(Date.now() / 1000),
      livemode: false,
      api_version: '2023-10-16'
    };
  }
}

module.exports = { StripeTestHelpers };
```

### 2. Tests unitaires des services

**Fichier :** `__tests__/unit/domains/payments/services/stripeService.test.js`

```javascript
const StripeService = require('../../../../../src/domains/payments/services/stripeService');
const { StripeTestHelpers } = require('../../../../setup/stripeTestSetup');

describe('StripeService', () => {
  let stripeService;
  let mockStripe;

  beforeEach(() => {
    stripeService = new StripeService();
    mockStripe = require('stripe')();
  });

  describe('createCustomer', () => {
    it('should create a Stripe customer with correct data', async () => {
      const userData = {
        email: 'test@upiik.com',
        firstName: 'John',
        lastName: 'Doe',
        city: 'Paris'
      };

      const mockCustomer = StripeTestHelpers.createMockCustomer({
        email: userData.email,
        name: `${userData.firstName} ${userData.lastName}`,
        metadata: { city: userData.city, source: 'upiik' }
      });

      mockStripe.customers.create.mockResolvedValue(mockCustomer);

      const result = await stripeService.createCustomer(userData);

      expect(mockStripe.customers.create).toHaveBeenCalledWith({
        email: userData.email,
        name: 'John Doe',
        metadata: {
          city: 'Paris',
          source: 'upiik',
          upiik_user_id: expect.any(String)
        }
      });

      expect(result).toEqual(mockCustomer);
    });

    it('should handle Stripe API errors', async () => {
      const userData = { email: 'invalid-email' };

      mockStripe.customers.create.mockRejectedValue(
        new Error('Invalid email format')
      );

      await expect(stripeService.createCustomer(userData))
        .rejects
        .toThrow('Invalid email format');
    });
  });

  describe('createSubscription', () => {
    it('should create monthly subscription correctly', async () => {
      const subscriptionData = {
        customerId: 'cus_test123',
        planType: 'monthly'
      };

      const mockSubscription = StripeTestHelpers.createMockSubscription();
      mockStripe.subscriptions.create.mockResolvedValue(mockSubscription);

      const result = await stripeService.createSubscription(subscriptionData);

      expect(mockStripe.subscriptions.create).toHaveBeenCalledWith({
        customer: 'cus_test123',
        items: [{ price: expect.stringContaining('price_monthly') }],
        payment_behavior: 'default_incomplete',
        payment_settings: { save_default_payment_method: 'on_subscription' },
        expand: ['latest_invoice.payment_intent']
      });

      expect(result).toEqual(mockSubscription);
    });

    it('should create yearly subscription with correct price', async () => {
      const subscriptionData = {
        customerId: 'cus_test123',
        planType: 'yearly'
      };

      mockStripe.subscriptions.create.mockResolvedValue(
        StripeTestHelpers.createMockSubscription({
          items: {
            data: [{
              price: {
                id: 'price_yearly_test',
                unit_amount: 3000, // 30€
                currency: 'eur'
              }
            }]
          }
        })
      );

      await stripeService.createSubscription(subscriptionData);

      expect(mockStripe.subscriptions.create).toHaveBeenCalledWith(
        expect.objectContaining({
          items: [{ price: expect.stringContaining('price_yearly') }]
        })
      );
    });
  });
});
```

### 3. Tests unitaires du service TokenService

**Fichier :** `__tests__/unit/domains/payments/services/tokenService.test.js`

```javascript
const TokenService = require('../../../../../src/domains/payments/services/tokenService');
const User = require('../../../../../src/domains/user/models/User'); // Adapter chemin

// Mock des modèles
jest.mock('../../../../../src/domains/user/models/User');

describe('TokenService', () => {
  let tokenService;

  beforeEach(() => {
    tokenService = new TokenService();
    jest.clearAllMocks();
  });

  describe('canUserMakeExchange', () => {
    it('should allow unlimited exchanges for premium users', async () => {
      const mockUser = {
        _id: 'user123',
        subscription: {
          isActive: true,
          type: 'monthly'
        }
      };

      User.findById.mockResolvedValue(mockUser);

      const result = await tokenService.canUserMakeExchange('user123');

      expect(result).toEqual({
        canExchange: true,
        reason: 'unlimited_plan',
        tokensRemaining: -1
      });
    });

    it('should enforce monthly limit for free users', async () => {
      const mockUser = {
        _id: 'user123',
        subscription: {
          isActive: false,
          type: 'free'
        }
      };

      User.findById.mockResolvedValue(mockUser);

      // Mock countExchangesThisMonth to return 1 (limit exceeded)
      jest.spyOn(tokenService, 'countExchangesThisMonth').mockResolvedValue(1);

      const result = await tokenService.canUserMakeExchange('user123');

      expect(result).toMatchObject({
        canExchange: false,
        reason: 'free_limit_exceeded',
        tokensRemaining: 0
      });
    });

    it('should allow exchange for free users within limit', async () => {
      const mockUser = {
        _id: 'user123',
        subscription: {
          isActive: false,
          type: 'free'
        }
      };

      User.findById.mockResolvedValue(mockUser);
      jest.spyOn(tokenService, 'countExchangesThisMonth').mockResolvedValue(0);

      const result = await tokenService.canUserMakeExchange('user123');

      expect(result).toMatchObject({
        canExchange: true,
        reason: 'within_free_limit',
        tokensRemaining: 1
      });
    });
  });

  describe('activatePremiumSubscription', () => {
    it('should activate premium subscription correctly', async () => {
      User.findByIdAndUpdate = jest.fn().mockResolvedValue(true);

      await tokenService.activatePremiumSubscription(
        'user123',
        'monthly',
        'sub_test123'
      );

      expect(User.findByIdAndUpdate).toHaveBeenCalledWith(
        'user123',
        expect.objectContaining({
          'subscription.isActive': true,
          'subscription.type': 'monthly',
          'subscription.tokensRemaining': -1
        })
      );
    });
  });
});
```

### 4. Tests d'intégration des webhooks

**Fichier :** `__tests__/integration/domains/payments/webhooks.test.js`

```javascript
const request = require('supertest');
const app = require('../../../../src/app'); // Adapter selon structure
const { StripeTestHelpers } = require('../../../setup/stripeTestSetup');
const crypto = require('crypto');

describe('Stripe Webhooks Integration', () => {
  const webhookSecret = 'whsec_test123';

  // Helper pour générer signature webhook valide
  const generateWebhookSignature = (payload, secret) => {
    const timestamp = Math.floor(Date.now() / 1000);
    const signedPayload = `${timestamp}.${payload}`;
    const signature = crypto
      .createHmac('sha256', secret)
      .update(signedPayload, 'utf8')
      .digest('hex');

    return `t=${timestamp},v1=${signature}`;
  };

  describe('POST /api/webhooks/stripe', () => {
    it('should handle customer.subscription.created event', async () => {
      const subscription = StripeTestHelpers.createMockSubscription();
      const event = StripeTestHelpers.createMockWebhookEvent(
        'customer.subscription.created',
        subscription
      );

      const payload = JSON.stringify(event);
      const signature = generateWebhookSignature(payload, webhookSecret);

      const response = await request(app)
        .post('/api/webhooks/stripe')
        .set('stripe-signature', signature)
        .send(payload);

      expect(response.status).toBe(200);
      expect(response.body).toEqual({ received: true });

      // Vérifier que l'abonnement a été créé en DB
      // (nécessite setup DB de test)
    });

    it('should handle customer.subscription.deleted event', async () => {
      const subscription = StripeTestHelpers.createMockSubscription({
        status: 'canceled'
      });
      const event = StripeTestHelpers.createMockWebhookEvent(
        'customer.subscription.deleted',
        subscription
      );

      const payload = JSON.stringify(event);
      const signature = generateWebhookSignature(payload, webhookSecret);

      const response = await request(app)
        .post('/api/webhooks/stripe')
        .set('stripe-signature', signature)
        .send(payload);

      expect(response.status).toBe(200);
    });

    it('should reject webhooks with invalid signature', async () => {
      const event = StripeTestHelpers.createMockWebhookEvent(
        'customer.subscription.created',
        {}
      );

      const response = await request(app)
        .post('/api/webhooks/stripe')
        .set('stripe-signature', 'invalid_signature')
        .send(JSON.stringify(event));

      expect(response.status).toBe(400);
      expect(response.body.error).toContain('Invalid signature');
    });
  });
});
```

### 5. Tests des middlewares

**Fichier :** `__tests__/unit/domains/payments/middlewares/exchangeLimit.test.js`

```javascript
const { checkExchangeLimit } = require('../../../../../src/domains/payments/middlewares/exchangeLimit');
const TokenService = require('../../../../../src/domains/payments/services/tokenService');

jest.mock('../../../../../src/domains/payments/services/tokenService');

describe('checkExchangeLimit middleware', () => {
  let req, res, next;

  beforeEach(() => {
    req = {
      user: { id: 'user123' }
    };
    res = {
      status: jest.fn().mockReturnThis(),
      json: jest.fn()
    };
    next = jest.fn();
    jest.clearAllMocks();
  });

  it('should call next() for users with unlimited plan', async () => {
    TokenService.prototype.canUserMakeExchange = jest.fn().mockResolvedValue({
      canExchange: true,
      reason: 'unlimited_plan',
      tokensRemaining: -1
    });

    await checkExchangeLimit(req, res, next);

    expect(next).toHaveBeenCalled();
    expect(res.status).not.toHaveBeenCalled();
  });

  it('should return 402 for users exceeding free limit', async () => {
    TokenService.prototype.canUserMakeExchange = jest.fn().mockResolvedValue({
      canExchange: false,
      reason: 'free_limit_exceeded',
      tokensRemaining: 0
    });

    await checkExchangeLimit(req, res, next);

    expect(res.status).toHaveBeenCalledWith(402);
    expect(res.json).toHaveBeenCalledWith(
      expect.objectContaining({
        error: 'EXCHANGE_LIMIT_EXCEEDED',
        upgrade: expect.objectContaining({
          availablePlans: expect.any(Array)
        })
      })
    );
    expect(next).not.toHaveBeenCalled();
  });

  it('should handle service errors gracefully', async () => {
    TokenService.prototype.canUserMakeExchange = jest.fn().mockRejectedValue(
      new Error('Database error')
    );

    await checkExchangeLimit(req, res, next);

    expect(res.status).toHaveBeenCalledWith(500);
    expect(res.json).toHaveBeenCalledWith({
      error: 'Token verification failed'
    });
  });
});
```

### 6. Tests end-to-end des flux complets

**Fichier :** `__tests__/e2e/payments/subscriptionFlow.test.js`

```javascript
const request = require('supertest');
const app = require('../../../src/app');
const { generateAuthToken } = require('../../helpers/authHelpers'); // À créer

describe('Subscription Flow E2E', () => {
  let authToken;
  let testUser;

  beforeAll(async () => {
    // Setup user de test
    testUser = await createTestUser({
      email: 'test@upiik.com',
      firstName: 'John',
      subscription: { type: 'free', isActive: false }
    });

    authToken = generateAuthToken(testUser);
  });

  afterAll(async () => {
    // Cleanup
    await cleanupTestUser(testUser._id);
  });

  describe('Complete subscription flow', () => {
    it('should allow user to subscribe to monthly plan', async () => {
      // 1. Vérifier statut initial
      const statusResponse = await request(app)
        .get('/api/payments/subscription-status')
        .set('Authorization', `Bearer ${authToken}`);

      expect(statusResponse.body.plan).toBe('free');

      // 2. Créer abonnement
      const subscribeResponse = await request(app)
        .post('/api/payments/subscribe')
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          planType: 'monthly',
          paymentMethod: 'pm_card_visa' // Test payment method
        });

      expect(subscribeResponse.status).toBe(200);
      expect(subscribeResponse.body.subscription).toBeDefined();

      // 3. Simuler webhook de confirmation
      const webhookPayload = {
        type: 'customer.subscription.created',
        data: {
          object: {
            id: subscribeResponse.body.subscription.id,
            customer: subscribeResponse.body.customer.id,
            status: 'active'
          }
        }
      };

      await request(app)
        .post('/api/webhooks/stripe')
        .set('stripe-signature', generateValidSignature(webhookPayload))
        .send(webhookPayload);

      // 4. Vérifier activation
      const newStatusResponse = await request(app)
        .get('/api/payments/subscription-status')
        .set('Authorization', `Bearer ${authToken}`);

      expect(newStatusResponse.body.plan).toBe('monthly');
      expect(newStatusResponse.body.tokensRemaining).toBe(-1); // Illimité
    });

    it('should enforce exchange limits correctly', async () => {
      // Tester avec user gratuit ayant dépassé sa limite
      const freeUser = await createTestUser({
        subscription: { type: 'free', isActive: false }
      });
      const freeToken = generateAuthToken(freeUser);

      // Simuler que l'user a déjà fait son échange du mois
      await recordTestExchange(freeUser._id);

      // Essayer un nouvel échange
      const exchangeResponse = await request(app)
        .post('/api/exchanges/request')
        .set('Authorization', `Bearer ${freeToken}`)
        .send({
          productId: 'product123',
          message: 'Test exchange'
        });

      expect(exchangeResponse.status).toBe(402);
      expect(exchangeResponse.body.error).toBe('EXCHANGE_LIMIT_EXCEEDED');
      expect(exchangeResponse.body.upgrade).toBeDefined();
    });
  });
});
```

### 7. Tests de performance et charge

**Fichier :** `__tests__/performance/webhooks.test.js`

```javascript
describe('Webhook Performance Tests', () => {
  it('should handle webhook bursts efficiently', async () => {
    const webhookPromises = [];
    const eventCount = 100;

    // Simuler 100 webhooks simultanés
    for (let i = 0; i < eventCount; i++) {
      const event = StripeTestHelpers.createMockWebhookEvent(
        'customer.subscription.updated',
        { id: `sub_test${i}` }
      );

      const promise = request(app)
        .post('/api/webhooks/stripe')
        .set('stripe-signature', generateValidSignature(event))
        .send(event);

      webhookPromises.push(promise);
    }

    const startTime = Date.now();
    const responses = await Promise.all(webhookPromises);
    const endTime = Date.now();

    // Vérifier que tous ont réussi
    responses.forEach(response => {
      expect(response.status).toBe(200);
    });

    // Vérifier performance (moins de 5 secondes pour 100 webhooks)
    expect(endTime - startTime).toBeLessThan(5000);
  });
});
```

### 8. Configuration des scripts de test

**Ajouter dans `package.json` :**
```json
{
  "scripts": {
    "test:payments": "jest __tests__/**/payments/**/*.test.js",
    "test:payments:unit": "jest __tests__/unit/domains/payments/",
    "test:payments:integration": "jest __tests__/integration/domains/payments/",
    "test:payments:e2e": "jest __tests__/e2e/payments/",
    "test:payments:watch": "jest __tests__/**/payments/**/*.test.js --watch",
    "test:payments:coverage": "jest __tests__/**/payments/**/*.test.js --coverage"
  }
}
```

## Critères de réussite
✅ Configuration Jest pour Stripe complète
✅ Tests unitaires tous les services (>90% couverture)
✅ Tests d'intégration des webhooks
✅ Tests des middlewares de sécurité
✅ Tests end-to-end des flux complets
✅ Tests de performance pour charge
✅ Mocks Stripe SDK configurés
✅ Scripts de test dans package.json

## Instructions d'exécution
1. **Examiner** la configuration Jest existante d'Upiik
2. **Créer** la configuration de test Stripe
3. **Implémenter** les tests unitaires par service
4. **Développer** les tests d'intégration
5. **Configurer** les tests E2E avec base de test
6. **Valider** la couverture de code >85%

**Prochaine étape :** `08-deployment.md`