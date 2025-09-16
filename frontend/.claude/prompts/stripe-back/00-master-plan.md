# MASTER PLAN : Intégration Stripe dans Upiik

## Vue d'ensemble du projet
Intégration complète de Stripe dans l'API Upiik pour gérer les abonnements premium (€4/mois, €30/an) avec tokens illimités.

## Infrastructure existante ✅
- **Stripe SDK** : v14.25.0 installé
- **Sécurité** : Helmet, CORS, Joi, JWT configurés
- **Architecture** : DDD avec `src/domains/`
- **Node.js** : v16.20.2 (contrainte hébergeur)

## Séquence d'exécution des prompts

### **Phase 1 : Fondations** (1-2 jours)
1. **01-config-initiale.md** ✅ → Configuration Stripe + variables env
2. **02-architecture-domaine.md** → Structure `src/domains/payments/`
3. **03-models-donnees.md** → Schemas Mongoose + relations

### **Phase 2 : API Core** (2-3 jours)
4. **04-routes-api.md** → Endpoints REST + middlewares
5. **05-webhooks.md** → Gestion événements Stripe temps réel
6. **06-integration-tokens.md** → Logique métier Upiik + abonnements

### **Phase 3 : Qualité** (1-2 jours)
7. **07-tests.md** → Tests unitaires + intégration Jest
8. **08-deployment.md** → Config production + monitoring

## Modèle d'abonnements Upiik
- **Gratuit** : 1 échange/mois (existant)
- **Mensuel** : €4/mois → tokens illimités
- **Annuel** : €30/an → tokens illimités + économie

## Estimation totale optimisée
**4.5-7 jours** (gain de 2.5-3 jours grâce à l'infrastructure existante)

---

**▶️ Démarrer avec :** `01-config-initiale.md`