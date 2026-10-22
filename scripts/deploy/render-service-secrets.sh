#!/bin/sh
# Render the `prod` GitHub Environment secrets of a service onto its host (ADR-0006, D140, GOU-933).
#
# Usage: render-service-secrets.sh [--if-present] <check|apply> <service>
#
#   check         Check that the service's secrets are set and well-formed, on the
#                 runner only: no SSH access, no value printed.
#   apply         Run check, then write the service's files on the host. The
#                 service is not restarted: the caller does it.
#   --if-present  A missing secret is a ::warning:: and nothing is checked or
#                 rendered (release, until the secret exists in `prod`). A secret
#                 that is set but malformed still fails.
#
# This script is the single definition of what a service receives, shared by
# prodSecrets.yml and releaseDeployProd.yml. Each secret is mapped to an
# environment variable of the same name in the calling step's `env:`.
#
# | Service | Secret              | File on the host                               | Line in /etc/open4goods/<service>.env                 |
# | ------- | ------------------- | ---------------------------------------------- | ----------------------------------------------------- |
# | api     | API_GOOGLE_API_JSON | /etc/open4goods/secrets/google-indexation.json | GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH=<that file>    |
#
# publish-jars.sh (open4goods-config) sources /etc/open4goods/<service>.env,
# which sits outside the tree open4goods-config's deploy rsyncs with --delete;
# the OS environment variable beats application-active.yml.
#
# Required environment for apply: DEPLOY_SSH_HOST, DEPLOY_SSH_USER, DEPLOY_SSH_KEY.
set -eu

fail() { echo "::error::$*" >&2; exit 1; }

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)

if_present=false
if [ "${1:-}" = "--if-present" ]; then
  if_present=true
  shift
fi
[ "$#" -eq 2 ] || fail "Usage: $0 [--if-present] <check|apply> <service>"
action=$1
service=$2
case "$action" in
  check | apply) ;;
  *) fail "Unknown action: $action (check or apply)" ;;
esac

# Returns 1 when the secret is missing and --if-present is set, after a warning.
require_secret() {
  if [ -n "$(printenv "$1" || true)" ]; then
    return 0
  fi
  if [ "$if_present" = true ]; then
    echo "::warning::$1 is not set in the prod GitHub Environment: nothing rendered for $service (files from a previous render are kept)."
    return 1
  fi
  fail "$1 is empty or missing from the prod GitHub Environment"
}

summary() {
  echo "$*"
  if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    echo "$*" >> "$GITHUB_STEP_SUMMARY"
  fi
}

########################################
# api: Google service account (Metriks, GoogleSearchConsoleService)
########################################
API_GOOGLE_JSON_FILE=/etc/open4goods/secrets/google-indexation.json
API_ENV_FILE=/etc/open4goods/api.env

present_api() {
  require_secret API_GOOGLE_API_JSON
}

check_api() {
  "$script_dir/push-remote-file.sh" --service-account-json --validate-only \
    "$API_GOOGLE_JSON_FILE" API_GOOGLE_API_JSON
}

apply_api() {
  file_result=$("$script_dir/push-remote-file.sh" --service-account-json \
    "$API_GOOGLE_JSON_FILE" API_GOOGLE_API_JSON)
  echo "$file_result"
  env_result=$(GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH="$API_GOOGLE_JSON_FILE" \
    "$script_dir/push-remote-env.sh" "$API_ENV_FILE" GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH)
  echo "$env_result"

  # Identifiers only, read from the validated JSON through a pipe: never the key itself.
  client_email=$(printenv API_GOOGLE_API_JSON | jq -r '.client_email')
  key_id=$(printenv API_GOOGLE_API_JSON | jq -r '.private_key_id // "unknown"' | cut -c1-8)
  summary "### Secrets rendered for api"
  summary ""
  summary "- $file_result"
  summary "- $env_result"
  summary "- Service account: $client_email (private_key_id ${key_id}...)"
}

case "$service" in
  api) ;;
  *) fail "Unknown service: $service (known: api)" ;;
esac

# Kept out of a conditional context so that `set -e` applies inside the check and apply functions.
if ! "present_$service"; then
  exit 0
fi
"check_$service"
echo "Secrets of $service are set and well-formed."
if [ "$action" = apply ]; then
  "apply_$service"
fi
