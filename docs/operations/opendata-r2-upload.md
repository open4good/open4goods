# Envoi des fichiers open data vers R2

Les zips open data (`gtin.zip`, `isbn.zip`) sont servis depuis un bucket Cloudflare R2 (`nudger-opendata`, domaine
`data.nudger.fr`) pour permettre la reprise de téléchargement sans passer par `ui` (GOU-2237, lot 0a GOU-2245).

## Interrupteurs (vides par défaut)

| Propriété | Module | Effet quand renseignée |
|---|---|---|
| `open-data-config.public-base-url` (`OPEN_DATA_PUBLIC_BASE_URL`) | front-api | `downloadUrl` = `{base}/{nom}`, ex. `https://data.nudger.fr/gtin-open-data.zip` |
| `open-data-config.redirect-base-url` (`OPEN_DATA_REDIRECT_BASE_URL`) | ui | `GET`/`HEAD` sur `/opendata/{gtin,isbn}-open-data.zip` répondent 301 vers `{base}/{nom}` |

Vides : comportement inchangé. Un `HEAD` n'ouvre jamais le flux bridé. Activation en prod : lot 0c, une fois les
fichiers sur R2 et vérifiés.

## Workflow `📤 Open data → R2` (`opendataR2Upload.yml`)

Lancement manuel, environnement `prod`. Entrées : `dataset` (`gtin`, `isbn`, `all`), `mode`, `upload_limit`.

Prérequis dans l'environnement GitHub `prod` :
- variable `R2_ACCOUNT_ID` ; secrets `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` (jeton R2 limité au bucket) ;
- `rclone` en binaire local sur l'hôte de prod : `~/bin/rclone` (sans installation système).

1. **`mode: test`** : liste chaque zip, affiche taille et sha256 locaux et l'objet distant actuel. Rien n'est écrit.
   Le job échoue si le bucket ne peut pas être lu (identifiants, endpoint, réseau) : un objet absent n'est pas une
   erreur.
2. **`mode: apply`** : envoie `/opt/open4goods/opendata/{gtin,isbn}.zip` vers `s3://nudger-opendata/{gtin,isbn}-open-data.zip`
   avec `Content-Type: application/zip`, `Content-Disposition: attachment`, `Cache-Control: public, max-age=86400` et
   la métadonnée `sha256`. Le processus tourne en `nice -n 19` / `ionice -c3`, débit montant limité par `upload_limit`
   (défaut 2 Mio/s).
3. **Vérification** : l'objet distant est relu en entier et son sha256 est comparé au sha256 local ; un écart fait
   échouer le job. L'objet public est alors déjà remplacé : relance `apply` avant d'activer le lot 0c (ou tout de
   suite s'il est déjà actif).

Durée : environ 51 min pour 6 Gio à 2 Mio/s, plus le calcul des sommes et la relecture. Le job s'arrête à 180 min :
avec `dataset: all`, ne descends pas sous `2M`.

Les identifiants passent en variables d'environnement (configuration rclone `RCLONE_CONFIG_R2_*`), envoyés par l'entrée
standard SSH (`bash -s`, script `scripts/deploy/opendata-r2-upload.sh`) : ils n'apparaissent dans aucune ligne de
commande (ni runner ni hôte), ne sont pas écrits sur disque et sont masqués dans les logs par GitHub.

## Coût

R2 : 10 Go-mois de stockage et 1 M d'opérations de classe A / 10 M de classe B gratuits par mois, sortie réseau
gratuite. Les deux zips tiennent largement sous 10 Go : coût 0 €. Surveiller la taille cumulée dans le tableau de bord
Cloudflare si les fichiers grossissent.
