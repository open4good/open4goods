# Notifier webhook signé SBA → event-gateway

## Contexte

En complément du `MailNotifier` standard de Spring Boot Admin (laissé inchangé), le module `admin`
expose un second `Notifier` : `EventWebhookNotifier`. Il envoie un `POST` HTTPS JSON signé vers un
service `event-gateway` (hors périmètre de ce module) à chaque changement de statut d'instance
surveillée, afin de réveiller Ops sur incident (`DOWN`).

Voir le plan [GOU-482](https://github.com/open4good/open4goods) pour la conception complète et les
critères d'acceptation.

## Configuration

```yaml
o4g:
  sba:
    event-webhook:
      url: ""                     # URL du event-gateway (vide = désactivé)
      secret: ""                  # secret HMAC partagé (vide = désactivé)
      environment: ""             # "prod" ou "beta" (requis seulement si url/secret renseignés)
      down-confirmation-delay: 60s  # délai anti-bruit avant de confirmer un DOWN
```

- Le notifier est **désactivé par défaut**. Il ne devient actif que si `url` et `secret` sont tous
  les deux non vides. Dans ce cas, `environment` doit être `prod` ou `beta`, sinon le contexte
  Spring échoue au démarrage (erreur de configuration explicite plutôt qu'un comportement silencieux
  incorrect).
- Les valeurs réelles (`O4G_SBA_EVENT_WEBHOOK_URL`, `O4G_SBA_EVENT_WEBHOOK_SECRET`,
  `O4G_SBA_EVENT_WEBHOOK_ENVIRONMENT`) viennent d'open4goods-config ou de variables d'environnement.
  **Aucun secret n'est commité dans ce dépôt.**

## Comportement

- **Corps JSON fermé**, exactement 8 champs : `source` (toujours `"sba"`), `environment`,
  `application` (nom d'enregistrement SBA), `instanceId`, `from`, `to`, `at` (ISO-8601 UTC,
  horodatage SBA de l'événement), `eventId`. Aucun détail, texte d'exception ni métadonnée
  d'instance n'est transmis.
- **`eventId`** : UUID v3 (`UUID.nameUUIDFromBytes`) de `instanceId:version`, stable entre les
  tentatives d'une même transition (déduplication côté gateway).
- **Signature** : en-têtes `X-Event-Timestamp` (secondes Unix) et
  `X-Event-Signature: sha256=<hex HMAC-SHA256(secret, "<timestamp>." + corps brut)>`. Recalculés à
  chaque tentative.
- **Confirmation DOWN** : une transition vers `DOWN` est retenue pendant
  `down-confirmation-delay` (60 s par défaut) puis relit l'instance en base. Si elle est toujours
  `DOWN`, l'événement est envoyé ; sinon rien n'est envoyé (anti-bruit sur un flap résolu seul).
  Toutes les autres transitions, y compris le retour `UP`, partent immédiatement.
- **Résilience** : timeout de 5 s par tentative, 3 tentatives au plus avec backoff, puis un `WARN`
  (sans le secret) si toutes échouent. L'échec n'est jamais propagé à SBA.

## Démonstration (critère 7)

Un récepteur HTTP local (`com.sun.net.httpserver.HttpServer`, `127.0.0.1:4104`) a rejoué le cycle
`UP → DOWN (confirmé après 3 s) → UP` avec le vrai `EventWebhookNotifier` et a vérifié la signature
HMAC-SHA256 reçue :

```
Received event:
  body      = {"source":"sba","environment":"beta","application":"front-api","instanceId":"demo-777de8a5e899","from":"UNKNOWN","to":"DOWN","at":"2026-10-13T11:36:39.261684355Z","eventId":"f076f5ea-ec4f-384f-9f7d-515f9adf8e98"}
  timestamp = 1791891402
  signature = sha256=763c4b843aa3a77ddef651f7de3537335d532ff690564fd477725da2f2fce5e3
  expected  = sha256=763c4b843aa3a77ddef651f7de3537335d532ff690564fd477725da2f2fce5e3
  valid     = true
Received event:
  body      = {"source":"sba","environment":"beta","application":"front-api","instanceId":"demo-777de8a5e899","from":"DOWN","to":"UP","at":"2026-10-13T11:36:42.852415218Z","eventId":"22053ddc-44a1-3d25-90bb-afda4ffd42bd"}
  timestamp = 1791891402
  signature = sha256=79daeb9e9ecc7cb5df23a65c2f48a0005f9f70334aa1a6307dc1dba691162839
  expected  = sha256=79daeb9e9ecc7cb5df23a65c2f48a0005f9f70334aa1a6307dc1dba691162839
  valid     = true
```

## Tests

`EventWebhookNotifierTest` (`admin/src/test/java/org/open4goods/sbadmin/notify`) couvre les
critères 1 à 6 du plan : signature HMAC, corps fermé à 8 clés, désactivation sans `url`/`secret`,
cycle `DOWN` confirmé → `UP`, anti-bruit sur un `DOWN` qui revient avant confirmation, et bornage
des tentatives sans jamais propager d'erreur à SBA.
