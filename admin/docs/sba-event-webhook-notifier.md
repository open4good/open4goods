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
  `down-confirmation-delay` (60 s par défaut) puis relit l'instance en base. Elle n'est confirmée,
  et donc envoyée, que si l'instance est toujours `DOWN` **pour cette même transition**
  (l'horodatage de statut relu doit correspondre à celui de l'événement) ; sinon rien n'est envoyé.
  Cela évite qu'un flap résolu seul déclenche l'alerte, et qu'un flap `DOWN → UP → DOWN` dans le
  délai ne déclenche deux envois (seul le dernier `DOWN`, avec son propre minuteur, est confirmé).
  Toutes les autres transitions, y compris le retour `UP`, partent immédiatement.
- **Dernier statut signalé** : contrairement au suivi interne par défaut de
  `AbstractStatusChangeNotifier` (qui avancerait dès la fin de `notify()`, donc avant même la
  confirmation d'un `DOWN` retenu), le notifier tient son propre état du dernier statut **réellement
  signalé** par instance. Il n'avance que lorsqu'un envoi est décidé (immédiat, ou `DOWN` confirmé) ;
  un `DOWN` non confirmé ne le modifie jamais. C'est ce qui garantit qu'une panne réelle survenant
  après un flap n'est pas masquée par un état `lastStatus` périmé.
- **Résilience** : timeout de 5 s par tentative, 3 tentatives au plus avec backoff, puis un `WARN`
  (sans le secret) si toutes échouent. L'échec n'est jamais propagé à SBA.

## Démonstration (critère 7)

Un récepteur HTTP local (`com.sun.net.httpserver.HttpServer`, `127.0.0.1:4104`) a rejoué le cycle
`UP (enregistrement) → DOWN (avec details, confirmé après 1 s) → UP` avec le vrai `EventWebhookNotifier`
et a recalculé la signature HMAC-SHA256 à partir du secret et des octets reçus. Le `UP` initial n'est
pas envoyé (aucun incident à ouvrir ni à clore) mais sert de `from` à la panne suivante ; les `details`
de santé ne sortent jamais :

```
SBA -> UP
SBA -> DOWN
RECEIVER body={"source":"sba","environment":"beta","application":"front-api","instanceId":"demo-lead","from":"UP","to":"DOWN","at":"2026-10-13T14:30:57.229892433Z","eventId":"c65d9fef-33c8-3924-ac49-efb71d64a5c7"}
RECEIVER ts=1791901858 signatureValid=true
SBA -> UP
RECEIVER body={"source":"sba","environment":"beta","application":"front-api","instanceId":"demo-lead","from":"DOWN","to":"UP","at":"2026-10-13T14:30:57.229929930Z","eventId":"7e7e3031-49b6-3f7d-a945-b673d9c66d70"}
RECEIVER ts=1791901858 signatureValid=true
```

## Tests

`EventWebhookNotifierTest` (`admin/src/test/java/org/open4goods/sbadmin/notify`) couvre les
critères 1 à 6 du plan : signature HMAC, corps fermé à 8 clés, désactivation sans `url`/`secret`,
cycle `DOWN` confirmé → `UP`, premier `UP` non envoyé mais retenu comme `from` de la panne suivante, anti-bruit sur un `DOWN` qui revient avant confirmation, et bornage
des tentatives sans jamais propager d'erreur à SBA. Elle couvre aussi le suivi du dernier statut
signalé : un flap `DOWN → UP` avant confirmation ne doit ni se faire passer pour un `DOWN` déjà
signalé, ni empêcher le `DOWN` réellement maintenu qui suit d'être signalé (`from=UP`) ; et un flap
`DOWN → UP → DOWN` dans le délai ne doit produire qu'un seul envoi `DOWN`, le minuteur du premier
`DOWN` se reconnaissant périmé à son échéance.
