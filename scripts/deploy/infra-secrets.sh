#!/bin/sh
# Rotation of the infrastructure secrets from the `beta` / `prod` GitHub Environment (GOU-1092, campaign GOU-1038
# wave 3), runner side of infraSecrets.yml. Docs: docs/operations/infra-secrets-rotation.md.
#
# Usage: infra-secrets.sh check <env> <target>
#        infra-secrets.sh remote <test|source|restart|rollback> <env> <target>
#
#   check   On the runner only, no SSH: the target's secrets are set and strong ([A-Za-z0-9._-], 32 characters or
#           more: Access generates them with kind=hex32), and render-service-secrets.sh --source check passes for
#           their consumers. The Google OAuth client of plausible-google is issued by Google, not generated: its
#           shape is checked instead, then Google's token endpoint must accept the pair (an invalid authorization
#           code answers invalid_grant once the client is authenticated, invalid_client otherwise). A missing
#           secret is only a ::warning:: for check and remote test (a dry run before Access writes it); every
#           other action needs all of them.
#   remote  Runs scripts/deploy/infra-secrets-remote.sh on the host with that action: the target's values are
#           prepended to the script as NAME='value' lines and the whole is streamed over SSH stdin, from a umask
#           077 temp file: never on a command line, in an artifact or in a log. On the host it lives in a mode
#           600 file (/dev/shm when available) for the duration of the run.
#   <env>     beta or prod.
#   <target>  elasticsearch (both), kibana (both), mysql (prod), plausible (beta), plausible-postgres (beta),
#             plausible-clickhouse (beta), plausible-google (beta), b2b-postgres (both).
#
# Each secret is read from the environment variable of the same name (the calling step's `env:`).
# Required environment for remote: DEPLOY_SSH_HOST, DEPLOY_SSH_USER, DEPLOY_SSH_KEY.
set -eu

fail() { echo "::error::$*" >&2; exit 1; }

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)

[ "$#" -ge 3 ] || fail "Usage: $0 check <env> <target> | $0 remote <action> <env> <target>"
command=$1
shift
case "$command" in
  check)
    [ "$#" -eq 2 ] || fail "Usage: $0 check <env> <target>"
    action=check
    ;;
  remote)
    [ "$#" -eq 3 ] || fail "Usage: $0 remote <test|source|restart|rollback> <env> <target>"
    action=$1
    shift
    case "$action" in
      test | source | restart | rollback) ;;
      *) fail "Unknown remote action: $action (test, source, restart or rollback)" ;;
    esac
    ;;
  *) fail "Unknown command: $command (check or remote)" ;;
esac
env_name=$1
target=$2
case "$env_name" in
  beta | prod) ;;
  *) fail "Unknown environment: $env_name (beta or prod)" ;;
esac

# <target>: its secrets, then the services whose render carries them.
case "$target" in
  elasticsearch)
    secrets=O4G_ELASTICSEARCH_PASSWORD
    services="front-api api ui b2b-api"
    ;;
  kibana)
    secrets="INFRA_KIBANA_SYSTEM_PASSWORD INFRA_KIBANA_ENCRYPTION_KEY"
    services=infra
    ;;
  mysql)
    [ "$env_name" = prod ] || fail "mysql: only prod runs XWiki and MySQL"
    secrets="INFRA_XWIKI_DB_PASSWORD INFRA_MYSQL_ROOT_PASSWORD"
    services=infra
    ;;
  plausible)
    [ "$env_name" = beta ] || fail "plausible: only beta runs Plausible"
    secrets=INFRA_PLAUSIBLE_SECRET_KEY_BASE
    services=infra
    ;;
  plausible-postgres)
    [ "$env_name" = beta ] || fail "plausible-postgres: only beta runs Plausible"
    secrets=INFRA_PLAUSIBLE_PG_PASSWORD
    services=infra
    ;;
  plausible-clickhouse)
    [ "$env_name" = beta ] || fail "plausible-clickhouse: only beta runs Plausible"
    secrets=INFRA_CLICKHOUSE_PASSWORD
    services=infra
    ;;
  plausible-google)
    [ "$env_name" = beta ] || fail "plausible-google: only beta runs Plausible"
    secrets="INFRA_GOOGLE_CLIENT_ID INFRA_GOOGLE_CLIENT_SECRET"
    services=infra
    ;;
  b2b-postgres)
    secrets=B2B_API_POSTGRES_PASSWORD
    services="infra b2b-api"
    ;;
  *) fail "Unknown target: $target (elasticsearch, kibana, mysql, plausible, plausible-postgres, plausible-clickhouse, plausible-google or b2b-postgres)" ;;
esac

# Every secret of the target, set and strong; the value is never printed.
for secret in $secrets; do
  value=$(printenv "$secret" || true)
  if [ -z "$value" ]; then
    case "$action" in
      check | test)
        echo "::warning::$secret is not set in the $env_name GitHub Environment: dry run without the new value."
        continue
        ;;
    esac
    fail "$secret is empty or missing from the $env_name GitHub Environment (target $target)"
  fi
  case "$secret" in
    INFRA_GOOGLE_CLIENT_ID)
      case "$value" in
        *[!a-z0-9.-]*) ;;
        [0-9]*-?*.apps.googleusercontent.com) continue ;;
      esac
      fail "$secret is not a Google OAuth client ID (<digits>-<id>.apps.googleusercontent.com): copy it from the Google Cloud console"
      ;;
    INFRA_GOOGLE_CLIENT_SECRET)
      case "$value" in
        *[!A-Za-z0-9_-]*) fail "$secret contains characters outside [A-Za-z0-9_-]: copy it from the Google Cloud console" ;;
      esac
      [ "${#value}" -ge 24 ] || fail "$secret is shorter than 24 characters: copy it from the Google Cloud console"
      continue
      ;;
  esac
  case "$value" in
    *[!A-Za-z0-9._-]*) fail "$secret contains characters outside [A-Za-z0-9._-]: generate it with kind=hex32" ;;
  esac
  [ "${#value}" -ge 32 ] || fail "$secret is shorter than 32 characters: generate it with kind=hex32"
done

# Google's token endpoint authenticates the client before it reads the authorization code: a random code answers
# invalid_grant (or redirect_uri_mismatch) for a valid pair, invalid_client or deleted_client otherwise. The request
# body goes through a umask 077 file, never a command line; only the error code of the answer is printed.
# GOOGLE_TOKEN_URL only exists for scripts/tests/infra-secrets.test.sh.
google_client_check() {
  (
    umask 077
    dir=$(mktemp -d)
    trap 'rm -rf "$dir"' EXIT
    code=wrong-$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')
    printf 'client_id=%s&client_secret=%s&grant_type=authorization_code&code=%s&redirect_uri=%s' \
      "$INFRA_GOOGLE_CLIENT_ID" "$INFRA_GOOGLE_CLIENT_SECRET" "$code" \
      "https%3A%2F%2Fplausible.nudger.fr%2Fauth%2Fgoogle%2Fcallback" > "$dir/body"
    : > "$dir/answer"
    http=$(curl -s -o "$dir/answer" -w '%{http_code}' --data-binary "@$dir/body" \
      "${GOOGLE_TOKEN_URL:-https://oauth2.googleapis.com/token}" || echo 000)
    error=$(tr -d '\n' < "$dir/answer" | sed -n 's/.*"error"[[:space:]]*:[[:space:]]*"\([a-z_]*\)".*/\1/p')
    case "$error" in
      invalid_grant | redirect_uri_mismatch)
        echo "Google's token endpoint accepts the OAuth client of $env_name (answer to a random code: $error)." ;;
      invalid_client | deleted_client | unauthorized_client)
        fail "Google refuses the OAuth client of the $env_name GitHub Environment ($error, HTTP $http): stale or deleted, copy the client in use from the Google Cloud console" ;;
      *) fail "cannot check the OAuth client with Google's token endpoint (HTTP $http, error '${error:-none}'): run again" ;;
    esac
  )
}

if [ "$action" = check ]; then
  if [ "$target" = plausible-google ] && [ -n "${INFRA_GOOGLE_CLIENT_ID:-}" ] && [ -n "${INFRA_GOOGLE_CLIENT_SECRET:-}" ]; then
    google_client_check
  fi
  for svc in $services; do
    "$script_dir/render-service-secrets.sh" --source check "$env_name" "$svc"
  done
  echo "The secrets of $target ($secrets) are set and strong in $env_name."
  exit 0
fi

: "${DEPLOY_SSH_HOST:?DEPLOY_SSH_HOST is required}"
: "${DEPLOY_SSH_USER:?DEPLOY_SSH_USER is required}"
: "${DEPLOY_SSH_KEY:?DEPLOY_SSH_KEY is required}"

umask 077
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# Values were checked above: no quote, so single quotes keep them literal.
for secret in $secrets; do
  printf "%s='%s'\n" "$secret" "$(printenv "$secret" || true)"
done > "$work/payload.sh"
cat "$script_dir/infra-secrets-remote.sh" >> "$work/payload.sh"
printf '%s\n' "$DEPLOY_SSH_KEY" > "$work/id"

# On the host, the payload is first copied to a mode 600 file (in memory under /dev/shm when possible) and removed on
# exit: run from stdin, a command of the script that reads stdin would swallow the rest of it.
remote_script="set -eu
umask 077
dir=/dev/shm
[ -d \"\$dir\" ] && [ -w \"\$dir\" ] || dir=\${TMPDIR:-/tmp}
payload=\$(mktemp \"\$dir/infra-secrets.XXXXXX\")
trap 'rm -f \"\$payload\"' EXIT
cat > \"\$payload\"
sh \"\$payload\" $action $env_name $target < /dev/null"

ssh -i "$work/id" \
  -o BatchMode=yes \
  -o StrictHostKeyChecking=accept-new \
  -o UserKnownHostsFile="$work/known_hosts" \
  "$DEPLOY_SSH_USER@$DEPLOY_SSH_HOST" "$remote_script" < "$work/payload.sh"
