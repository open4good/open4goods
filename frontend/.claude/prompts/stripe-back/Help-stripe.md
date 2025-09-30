# Prompt : Implémentation Stripe dans une API REST Node.js

## Contexte

```
Stack technique :
- Backend : Node.js + Express.js
- Base de données : MongoDB + Mongoose + MongoDB Atlas
- Langages : JavaScript/TypeScript
- Niveau : Développeur full-stack expérimenté
```

## Objectif

Implémenter une solution de paiement Stripe complète dans mon API REST en suivant les **meilleures pratiques 2025**.

## Étapes d'implémentation requises

### 1. Configuration et sécurité

```
- Installation des dépendances (stripe, dotenv, helmet, etc.)
- Configuration sécurisée des clés API Stripe (dev/prod)
- Variables d'environnement avec validation (Joi/Zod)
- Middlewares de sécurité pour webhooks
- Configuration CORS appropriée
```

### 2. Architecture du projet

```
Structure des dossiers :
├── controllers/payments/
├── services/stripe/
├── models/
│   ├── Order.js
│   ├── Payment.js
│   └── Customer.js
├── routes/payments/
├── middlewares/stripe/
└── utils/stripe/

Patterns à implémenter :
- Séparation controllers/services/models
- Gestion centralisée des erreurs
- Validation des schémas de données
```

### 3. Endpoints principaux

```
POST   /api/payments/create-intent     # Création PaymentIntent
POST   /api/payments/confirm           # Confirmation paiement
GET    /api/payments/:id/status        # Status d'un paiement
POST   /api/webhooks/stripe           # Webhooks Stripe
POST   /api/customers                 # Gestion customers
GET    /api/payments/history/:userId  # Historique paiements
POST   /api/payments/refund           # Remboursements
```

### 4. Fonctionnalités avancées

```
- Paiements récurrents/abonnements
- Support multi-devises (EUR, USD, etc.)
- Métadonnées personnalisées
- Gestion des coupons/promotions
- Paiements différés (capture manuelle)
- Split payments (marketplace)
```

### 5. Sécurité et validation

```
- Validation signatures webhooks Stripe
- Sanitisation des inputs utilisateur
- Gestion des idempotency keys
- Logs sécurisés (pas de données sensibles)
- Rate limiting sur les endpoints
- Validation stricte des montants/devises
```

### 6. Gestion d'erreurs et monitoring

```
- Gestion centralisée des erreurs Stripe
- Retry logic avec backoff exponentiel
- Alertes sur échecs de paiement
- Monitoring des performances
- Dead letter queue pour webhooks échoués
- Tests unitaires et d'intégration
```

### 7. Synchronisation et cohérence

```
- Synchronisation Stripe ↔ MongoDB
- Gestion des états incohérents
- Transactions atomiques MongoDB
- Mécanisme de réconciliation
- Audit trail des opérations
```

## Livrables attendus

### Code source

```
- Controllers avec gestion d'erreurs complète
- Services Stripe modulaires et testables
- Modèles Mongoose avec validation
- Middlewares de sécurité et validation
- Routes avec documentation OpenAPI/Swagger
```

### Configuration

```
- Variables d'environnement (.env.example)
- Configuration MongoDB avec indexes
- Configuration Stripe (webhooks endpoints)
- Scripts de déploiement et migration
```

### Documentation

```
- Documentation des endpoints (format OpenAPI)
- Exemples de requêtes cURL/Postman
- Guide de déploiement
- Procédures de test et debugging
```

### Tests

```
- Tests unitaires (Jest/Mocha)
- Tests d'intégration avec Stripe Test API
- Mocks pour les webhooks
- Tests de charge et performance
```

## Contraintes techniques

### Code quality

```
- ES6+ avec async/await exclusivement
- TypeScript avec types stricts si possible
- Validation avec Joi ou Zod
- Respect des principes SOLID
- Code coverage > 80%
```

### Performance

```
- Gestion optimisée des connexions DB
- Cache Redis pour les données fréquentes
- Pagination pour les listes
- Optimisation des requêtes MongoDB
```

### Versions et packages

```
- Node.js >= 18 LTS
- Stripe API version 2024-12-18
- Mongoose >= 8.x
- Express >= 4.x avec security middlewares
```

## Cas d'usage à couvrir

### E-commerce standard

```
1. Création d'une commande
2. Paiement par carte bancaire
3. Confirmation et traitement
4. Envoi de reçu par email
5. Gestion des remboursements
```

### Abonnements

```
1. Création d'un plan d'abonnement
2. Souscription utilisateur
3. Gestion des renouvellements
4. Annulation et prorata
```

### Marketplace

```
1. Paiements avec commission
2. Transferts vers vendeurs
3. Gestion des disputes
4. Reporting financier
```

## Questions spécifiques à résoudre

```
1. Comment gérer les webhooks dupliqués de Stripe ?
2. Quelle stratégie pour les paiements partiellement échoués ?
3. Comment implémenter un système de retry robuste ?
4. Gestion des timeouts et connexions perdues ?
5. Stratégie de test avec l'environnement Stripe ?
6. Comment gérer les migrations de données Stripe ?
7. Monitoring et alerting : quelles métriques suivre ?
```

## Format de réponse souhaité

```
Pour chaque composant :
- Code complet avec commentaires détaillés
- Explication des choix d'architecture
- Exemples d'utilisation concrets
- Gestion des cas d'erreur
- Tests associés
- Points d'attention sécurité

Structure : Code → Explication → Tests → Bonnes pratiques
```

---

**Générez-moi une implémentation production-ready complète avec tous ces éléments, en expliquant chaque décision technique prise.**
