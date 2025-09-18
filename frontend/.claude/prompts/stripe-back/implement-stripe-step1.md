# ÉTAPE 1 : Planification et Architecture de l'intégration Stripe

## Objectif
Établir un plan détaillé pour l'intégration complète de Stripe dans le projet Upiik sous forme de todo list structuré.

## Todo List : Intégration Stripe Optimisée pour Upiik

> **🎯 Modules déjà disponibles dans package.json :**
> `stripe` (14.25.0), `helmet` (6.0.0), `joi` (17.9.1), `winston` (3.11.0), `express` (4.18.2), `mongoose` (7.6.3), `dotenv` (16.0.3), `cors` (2.8.5), `express-rate-limit` (6.7.0), `jsonwebtoken` (9.0.0)

### Phase 1 : Configuration et Architecture adaptée à Upiik ⚡
- [ ] **Configuration initiale** (modules existants)
  - [ ] ~~Installer les dépendances~~ → **DÉJÀ FAIT** ✅
  - [ ] Ajouter variables Stripe dans `env/local.env` et `env/production.env`
  - [ ] Créer `src/config/stripe.js` (utilise SDK Stripe v14.25.0 existant)
  - [ ] Étendre `src/config/environment.js` pour validation Stripe
  - [ ] Configurer Winston existant pour logs des transactions

- [ ] **Architecture DDD existante** (suit les patterns Upiik)
  - [ ] Créer `src/domains/payments/` (comme `user/`, `products/`, etc.)
  - [ ] Structure : `controllers/`, `services/`, `routes/`, `models/`, `middlewares/`
  - [ ] Utiliser les middlewares de sécurité existants (Helmet, CORS, Rate limiting)
  - [ ] Intégrer avec le système d'événements `src/events/`

- [ ] **Modèles de données** (utilise Mongoose v7.6.3)
  - [ ] Créer `src/domains/payments/models/StripeCustomer.js`
  - [ ] Créer `src/domains/payments/models/Payment.js`
  - [ ] Créer `src/domains/payments/models/Subscription.js`
  - [ ] Étendre modèles User existants avec références Stripe

### Phase 2 : Intégration avec l'écosystème Upiik existant 🔄
- [ ] **Multi-base de données** (architecture existante)
  - [ ] Configurer Stripe pour les DBs existantes (Clients, Products, Admin)
  - [ ] Utiliser les connexions MongoDB existantes
  - [ ] Gérer les payments par ville (city-specific DBs)
  - [ ] Intégrer avec Sessions DB (Redis existant)

- [ ] **Système d'événements** (utilise `src/events/` existant)
  - [ ] Event subscribers pour les paiements Stripe
  - [ ] Notifications email (Nodemailer existant)
  - [ ] Push notifications (`web-push` existant)
  - [ ] Intégrer avec les événements chats et users

- [ ] **Authentification et sécurité** (JWT existant)
  - [ ] Middlewares d'auth pour endpoints Stripe
  - [ ] Rate limiting spécifique (utilise `express-rate-limit` existant)
  - [ ] Validation Joi pour les données Stripe
  - [ ] Utiliser les patterns de sécurité existants

### Phase 3 : Paiements et abonnements Upiik 💰
- [ ] **Abonnements selon le modèle Upiik**
  - [ ] Plan Gratuit : 1 échange/mois (existant)
  - [ ] Plan Mensuel : €4/mois unlimited
  - [ ] Plan Annuel : €30/année unlimited
  - [ ] Synchronisation avec le système de tokens existant

- [ ] **PaymentIntents adaptés**
  - [ ] Endpoints dans `src/domains/payments/routes/`
  - [ ] Services utilisant les utilitaires `src/utils/_*.js`
  - [ ] Intégration avec les Users existants
  - [ ] Gestion multi-langue (FR/EN/ES/DE/IT/PT existant)

### Phase 4 : Webhooks et architecture événementielle 🔗
- [ ] **Webhooks Stripe** (utilise patterns existants)
  - [ ] Endpoint dans la structure de routes existante
  - [ ] Validation avec Joi (module existant)
  - [ ] Gestion d'erreurs centralisée (`src/errors/`)
  - [ ] Logs Winston structurés (module existant)

- [ ] **Événements critiques pour Upiik**
  - [ ] `subscription.created` → Activer unlimited tokens
  - [ ] `subscription.deleted` → Retour plan gratuit (1/mois)
  - [ ] `payment.failed` → Notifications utilisateur
  - [ ] Synchronisation avec le système d'échanges

### Phase 5 : Interface et monitoring 📊
- [ ] **API publique** (suit patterns routes existants)
  - [ ] GET `/api/payments/subscription-status`
  - [ ] POST `/api/payments/subscribe`
  - [ ] DELETE `/api/payments/cancel-subscription`
  - [ ] Utiliser l'authentification JWT existante

- [ ] **Administration** (domaine `admin/` existant)
  - [ ] Étendre `src/domains/admin/` avec stats Stripe
  - [ ] Dashboard paiements dans l'interface admin
  - [ ] Utiliser les patterns Marvel API existants
  - [ ] Monitoring avec Winston existant

---

## **📁 Prompts spécialisés créés**

L'intégration Stripe a été décomposée en **8 prompts exécutables** :

### **Phase 1 : Fondations**
- **✅ 01-config-initiale.md** - Configuration Stripe + variables env
- **📁 02-architecture-domaine.md** - Structure `src/domains/payments/`
- **📁 03-models-donnees.md** - Schemas Mongoose + relations

### **Phase 2 : API Core**
- **📁 04-routes-api.md** - Endpoints REST + middlewares
- **📁 05-webhooks.md** - Gestion événements Stripe temps réel
- **📁 06-integration-tokens.md** - Logique métier Upiik + abonnements

### **Phase 3 : Qualité & Déploiement**
- **📁 07-tests.md** - Tests unitaires + intégration Jest
- **📁 08-deployment.md** - Config production + monitoring

---

## **🎯 Avantages de la décomposition**

✅ **Chaque prompt = 1 tâche exécutable**
✅ **Progression séquentielle claire**
✅ **Possibilité d'arrêt/reprise à tout moment**
✅ **Validation par étape**
✅ **Réutilisabilité des prompts**
✅ **Équipe peut travailler en parallèle**

---

## **▶️ Pour commencer l'implémentation**

**Utilise le prompt :** `01-config-initiale.md` (déjà créé)

Chaque prompt contient :
- Contexte spécifique à la tâche
- Fichiers à examiner avant implémentation
- Spécifications techniques précises
- Critères de validation clairs
- Instructions step-by-step

## Estimation optimisée avec infrastructure existante

### ⚡ **Gain de temps grâce aux modules existants**
- **Dépendances** : 0 jour (au lieu de 0.5 jour)
- **Configuration sécurité** : 0.5 jour (au lieu de 1.5 jours)
- **Architecture base** : 0.5 jour (au lieu de 1 jour)
- **Système de logs** : 0 jour (Winston configuré)

### **Priorité 1 (Critique - 1.5-2 jours)** ⭐
- Phase 1 : Configuration Stripe adaptée à Upiik
- Phase 2 : Intégration avec l'écosystème existant
- Modèles de données dans l'architecture DDD

### **Priorité 2 (Important - 2-3 jours)** 🎯
- Phase 3 : Abonnements €4/€30 avec tokens
- Phase 4 : Webhooks dans le système d'événements
- Tests avec Jest (déjà configuré)

### **Priorité 3 (Nice to have - 1-2 jours)** 📈
- Phase 5 : Interface admin étendue
- Monitoring avancé avec Winston
- Documentation technique

**Durée totale OPTIMISÉE : 4.5-7 jours** (gain de 2.5-3 jours) 🚀

---

## **Avantages de l'approche Upiik-native**

✅ **Réutilise 90% de l'infrastructure**
✅ **Suit l'architecture DDD établie**
✅ **Intègre naturellement le multi-DB**
✅ **Utilise les patterns de sécurité existants**
✅ **Compatible avec Node.js 16.20.2** (contrainte hébergeur)
✅ **S'appuie sur Redis/sessions existants**
✅ **Exploite le système d'événements en place**

> **🎯 Ce que vous allez apprendre :**
> - Comment structurer un projet Node.js pour les paiements
> - Les meilleures pratiques de sécurité pour Stripe
> - Comment créer des modèles de données cohérents
> - L'architecture en couches (controllers/services/models)

## Livrables attendus

### 1. Configuration initiale
```
- Installation et configuration des dépendances
- Variables d'environnement sécurisées
- Configuration Stripe (dev/test/prod)
- Middlewares de sécurité de base
```

> **📚 Explication :** La configuration est le socle de votre application. Les variables d'environnement permettent de séparer les secrets (clés API) du code source. Les middlewares de sécurité protègent votre API contre les attaques courantes.

### 2. Architecture du projet
```
Structure des dossiers :
├── config/               # 🔧 Fichiers de configuration
│   ├── database.js       # Connexion MongoDB
│   ├── stripe.js         # Configuration Stripe
│   └── environment.js    # Validation des variables d'env
├── controllers/payments/ # 🎮 Logique de contrôle (routes handlers)
├── services/stripe/      # 🔧 Logique métier Stripe
├── models/               # 🗃️ Schémas de données MongoDB
│   ├── User.js          # Utilisateurs de votre app
│   ├── Order.js         # Commandes e-commerce
│   ├── Payment.js       # Historique des paiements
│   └── Customer.js      # Clients Stripe (sync avec Users)
├── routes/payments/      # 🛤️ Définition des routes API
├── middlewares/          # ⚡ Fonctions intermédiaires
│   ├── auth.js          # Authentification utilisateur
│   ├── validation.js    # Validation des données
│   └── stripe.js        # Middlewares spécifiques Stripe
├── utils/               # 🛠️ Fonctions utilitaires
│   ├── logger.js        # Système de logs
│   ├── errors.js        # Classes d'erreurs custom
│   └── helpers.js       # Fonctions d'aide
└── tests/               # 🧪 Tests automatisés
```

> **🏗️ Pourquoi cette structure ?**
> - **Séparation des responsabilités** : Chaque dossier a un rôle précis
> - **Scalabilité** : Facile d'ajouter de nouvelles fonctionnalités
> - **Maintenance** : Code organisé = bugs plus faciles à corriger
> - **Collaboration** : Équipe peut travailler sur différentes parties

### 3. Modèles de données MongoDB
```
Créer les schémas Mongoose pour :
- User (liaison avec Stripe Customer)    # Vos utilisateurs
- Order (commandes e-commerce)           # Les commandes
- Payment (transactions Stripe)          # Historique paiements
- Customer (données clients Stripe)      # Sync avec Stripe

Avec validation, indexes et relations appropriées
```

> **🗄️ Pourquoi ces modèles ?**
> - **User** : Vos utilisateurs de base (email, mot de passe, etc.)
> - **Customer** : Représentation Stripe de vos users (pour sync)
> - **Order** : Les commandes avec produits, prix, statut
> - **Payment** : Historique détaillé de tous les paiements
> 
> **Relation clé** : User ↔ Customer (un User a un Customer Stripe)

## Spécifications techniques

### Dépendances à installer
```json
{
  "dependencies": {
    "stripe": "^14.x",        // SDK officiel Stripe
    "express": "^4.x",        // Framework web Node.js
    "mongoose": "^8.x",       // ODM pour MongoDB
    "dotenv": "^16.x",        // Gestion variables d'environnement
    "helmet": "^7.x",         // Sécurité HTTP headers
    "cors": "^2.x",           // Cross-Origin Resource Sharing
    "joi": "^17.x",           // Validation des données
    "winston": "^3.x"         // Système de logs avancé
  },
  "devDependencies": {
    "jest": "^29.x",          // Framework de tests
    "supertest": "^6.x",      // Tests d'API HTTP
    "nodemon": "^3.x"         // Rechargement auto en dev
  }
}
```

> **📦 Pourquoi ces dépendances ?**
> - **stripe** : SDK officiel, toujours à jour avec l'API
> - **helmet** : Protège contre 11 vulnérabilités web courantes
> - **joi** : Validation robuste, meilleure que la validation manuelle
> - **winston** : Logs structurés, essentiels pour déboguer en production

### Variables d'environnement
```env
# Stripe Configuration
STRIPE_SECRET_KEY=sk_test_...      # Clé secrète TEST (jamais en production dans le code!)
STRIPE_PUBLISHABLE_KEY=pk_test_... # Clé publique (côté frontend)
STRIPE_WEBHOOK_SECRET=whsec_...    # Secret pour valider les webhooks

# Database
MONGODB_URI=mongodb+srv://...      # URI de connexion MongoDB Atlas
DB_NAME=ecommerce_db              # Nom de votre base de données

# Application
NODE_ENV=development              # Environnement (development/production)
PORT=3000                        # Port du serveur
JWT_SECRET=...                   # Secret pour les tokens JWT

# Logging
LOG_LEVEL=debug                  # Niveau de log (debug/info/warn/error)
```

> **🔐 Sécurité des variables d'environnement :**
> - **Jamais** dans le code source ou Git
> - **Différentes** par environnement (dev/test/prod)
> - **Rotation** régulière des secrets
> - **Validation** au démarrage de l'app

### Configuration Stripe
```javascript
// config/stripe.js - À implémenter
// Cette configuration permettra de basculer facilement entre test et prod
```

> **⚙️ Configuration Stripe expliquée :**
> - **Mode test** : Utilisez sk_test_ et pk_test_ pour le développement
> - **Mode live** : sk_live_ et pk_live_ uniquement en production
> - **Webhooks** : URL différente selon l'environnement
> - **API Version** : Fixez une version pour éviter les breaking changes

### Validation et sécurité
```
- Validation des variables d'environnement au démarrage
- Configuration Helmet pour la sécurité
- CORS approprié pour les webhooks
- Middleware de logging sécurisé
- Gestion centralisée des erreurs
```

> **🛡️ Couches de sécurité :**
> 1. **Validation env** : App crash si config manquante
> 2. **Helmet** : Headers HTTP sécurisés automatiquement
> 3. **CORS** : Contrôle d'accès cross-origin
> 4. **Logs sécurisés** : Pas de données sensibles dans les logs
> 5. **Gestion erreurs** : Pas de leak d'infos internes

## Code à fournir

### 1. Configuration complète
```
- config/database.js (connexion MongoDB sécurisée)
- config/stripe.js (initialisation Stripe)
- config/environment.js (validation des env vars)
```

> **🔧 Fichiers de configuration :**
> Ces fichiers centralisent la configuration et permettent un démarrage propre de l'application avec validation des prérequis.

### 2. Modèles Mongoose
```
- models/User.js (avec référence Stripe Customer)
- models/Order.js (structure commande e-commerce)
- models/Payment.js (historique des transactions)
- models/Customer.js (données clients Stripe)
```

> **🗃️ Modèles de données :**
> Chaque modèle représente une entité métier avec ses règles de validation, ses relations et ses indexes pour les performances.

### 3. Middlewares de base
```
- middlewares/auth.js (authentification JWT)
- middlewares/validation.js (validation Joi)
- middlewares/stripe.js (vérification signatures)
- middlewares/error.js (gestion centralisée erreurs)
```

> **⚡ Middlewares expliqués :**
> - **auth.js** : Vérifie que l'utilisateur est connecté
> - **validation.js** : Valide les données avant traitement
> - **stripe.js** : Sécurise les webhooks Stripe
> - **error.js** : Formate les erreurs de manière cohérente

### 4. Utilitaires
```
- utils/logger.js (Winston configuration)
- utils/errors.js (classes d'erreurs personnalisées)
- utils/helpers.js (fonctions utilitaires)
```

> **🛠️ Utilitaires :**
> Ces fichiers contiennent du code réutilisable dans toute l'application, évitant la duplication et centralisant la logique commune.

### 5. Structure Express de base
```
- app.js (configuration Express principale)
- server.js (démarrage serveur)
- routes/index.js (routes principales)
```

> **🚀 Structure Express :**
> - **server.js** : Point d'entrée, démarrage du serveur
> - **app.js** : Configuration Express (middlewares, routes)
> - **routes/index.js** : Organisation des routes par domaine

## Contraintes techniques

```
- Node.js >= 18 LTS                     # Version stable et supportée
- TypeScript fortement typé si possible # Meilleure maintenabilité
- Validation stricte avec Joi           # Sécurité des données
- Logs structurés avec Winston          # Debugging efficace
- Gestion d'erreurs avec codes HTTP     # API REST standard
- Tests unitaires pour chaque composant # Qualité et non-régression
- Documentation JSDoc pour les fonctions # Code auto-documenté
```

> **📋 Pourquoi ces contraintes ?**
> - **Node.js 18+** : Fonctionnalités récentes et sécurité
> - **TypeScript** : Détection d'erreurs à la compilation
> - **Tests unitaires** : Confiance dans les modifications
> - **Documentation** : Code compréhensible par l'équipe

## Tests à inclure

```
- Test de connexion MongoDB             # La DB est accessible
- Test de configuration Stripe         # Les clés API sont valides
- Test de validation des variables      # L'app démarre correctement
- Test des modèles Mongoose            # Les schémas fonctionnent
- Test des middlewares de base         # La sécurité est active
```

> **🧪 Stratégie de tests :**
> Ces tests valident que l'infrastructure fonctionne avant d'implémenter la logique métier. Ils évitent les bugs de configuration en production.

## Questions spécifiques

```
1. Comment structurer les modèles pour optimiser les performances ?
   → Indexes, relations, requêtes efficaces

2. Quelle stratégie de validation des données adopter ?
   → Joi au niveau API + Mongoose au niveau DB

3. Comment gérer les environnements multiples (dev/test/prod) ?
   → Variables d'env + fichiers de config séparés

4. Architecture des logs pour le debugging et monitoring ?
   → Winston avec niveaux + formats structurés (JSON)
```

> **❓ Pourquoi ces questions ?**
> Ces questions anticipent les défis courants et vous préparent aux décisions techniques importantes de votre implémentation.

## Critères de validation

Cette étape est réussie quand :
- ✅ Serveur Express démarre sans erreur
- ✅ Connexion MongoDB fonctionnelle  
- ✅ Configuration Stripe validée
- ✅ Tous les tests passent
- ✅ Structure de projet claire et documentée

> **🎯 Validation de l'étape :**
> Ces critères garantissent que votre foundation est solide avant de passer aux fonctionnalités Stripe. Une base défaillante = problèmes en cascade !

---

## 📈 Prochaines étapes

Une fois cette étape terminée, vous aurez :
- ✅ **Une architecture propre** pour accueillir les fonctionnalités Stripe
- ✅ **Une sécurité de base** configurée
- ✅ **Des modèles de données** prêts pour les paiements
- ✅ **Une structure de tests** pour valider vos développements

**➡️ Étape 2** : Implémentation des endpoints de paiement de base (PaymentIntents, confirmations, statuts)

---

**Générez-moi une implémentation complète de cette première étape avec code production-ready, tests et documentation détaillée.**