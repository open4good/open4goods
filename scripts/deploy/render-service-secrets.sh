#!/bin/sh
# Render the `prod` GitHub Environment secrets of a service onto its host (ADR-0006, D140, GOU-933, GOU-957).
#
# Usage: render-service-secrets.sh [--if-present] <check|apply> <service>
#
#   check         Check that the service's secrets are set and well-formed, on the
#                 runner only: no SSH access, no value printed.
#   apply         Run check, then write the service's files on the host. The
#                 service is not restarted: the caller does it.
#   --if-present  A missing required secret is a ::warning:: and is neither
#                 checked nor rendered (release, until the secret exists in
#                 `prod`); the service's other secrets are still rendered. A
#                 secret that is set but malformed still fails.
#
# This script is the single definition of what a service receives, shared by
# prodSecrets.yml and releaseDeployProd.yml. Each secret is mapped to an
# environment variable of the same name in the calling step's `env:`.
#
# | Service | Secret                            | File on the host                               | Line in /etc/open4goods/<service>.env              |
# | ------- | --------------------------------- | ---------------------------------------------- | -------------------------------------------------- |
# | api     | API_GOOGLE_API_JSON               | /etc/open4goods/secrets/google-indexation.json | GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH=<that file> |
# | api     | API_METRIKS_AWIN_PUBLISHER_ID     | -                                              | METRIKS_AFFILIATION_AWIN_PUBLISHER_ID=<value>      |
# | api     | API_METRIKS_AWIN_API_TOKEN        | -                                              | METRIKS_AFFILIATION_AWIN_API_TOKEN=<value>         |
# | api     | API_METRIKS_EFFILIATION_API_KEY   | -                                              | METRIKS_AFFILIATION_EFFILIATION_API_KEY=<value>    |
#
# The API_METRIKS_* secrets (Metriks affiliation report, GOU-957) are optional, with
# or without --if-present: a missing one is a ::warning::, is not rendered and does
# not stop the others (the network's Metriks events then report the missing
# credential). A set one must match [A-Za-z0-9/._-] (digits only for the Awin publisher id).
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
    echo "::warning::$1 is not set in the prod GitHub Environment: not rendered for $service (a previous render is kept)."
    return 1
  fi
  fail "$1 is empty or missing from the prod GitHub Environment"
}

# Optional secret: returns 1 when it is missing, after a warning, whatever --if-present.
optional_secret() {
  if [ -n "$(printenv "$1" || true)" ]; then
    return 0
  fi
  echo "::warning::$1 is not set in the prod GitHub Environment: not rendered for $service (a previous render is kept)."
  return 1
}

summary() {
  echo "$*"
  if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    echo "$*" >> "$GITHUB_STEP_SUMMARY"
  fi
}

########################################
# api: Google service account (Metriks, GoogleSearchConsoleService)
#      and Metriks affiliation report credentials (AffiliationMetriksService, GOU-957)
########################################
API_GOOGLE_JSON_FILE=/etc/open4goods/secrets/google-indexation.json
API_ENV_FILE=/etc/open4goods/api.env
# <prod secret>:<line in api.env>
API_METRIKS_SECRETS="API_METRIKS_AWIN_PUBLISHER_ID:METRIKS_AFFILIATION_AWIN_PUBLISHER_ID
API_METRIKS_AWIN_API_TOKEN:METRIKS_AFFILIATION_AWIN_API_TOKEN
API_METRIKS_EFFILIATION_API_KEY:METRIKS_AFFILIATION_EFFILIATION_API_KEY"

# Sets api_google (true when API_GOOGLE_API_JSON is set), api_metriks (the pairs of
# API_METRIKS_SECRETS whose secret is set) and api_metriks_missing (the other secrets).
# Fails on a missing API_GOOGLE_API_JSON unless --if-present; returns 1 when nothing is set.
present_api() {
  api_google=false
  if require_secret API_GOOGLE_API_JSON; then
    api_google=true
  fi
  api_metriks=
  api_metriks_missing=
  for pair in $API_METRIKS_SECRETS; do
    if optional_secret "${pair%%:*}"; then
      api_metriks="$api_metriks $pair"
    else
      api_metriks_missing="$api_metriks_missing ${pair%%:*}"
    fi
  done
  [ "$api_google" = true ] || [ -n "$api_metriks" ]
}

check_api() {
  if [ "$api_google" = true ]; then
    "$script_dir/push-remote-file.sh" --service-account-json --validate-only \
      "$API_GOOGLE_JSON_FILE" API_GOOGLE_API_JSON
  fi
  for pair in $api_metriks; do
    secret=${pair%%:*}
    # Same character set as push-remote-env.sh, checked before anything is written; the value is never printed.
    case "$(printenv "$secret")" in
      *[!A-Za-z0-9/._-]*) fail "$secret contains characters outside [A-Za-z0-9/._-]" ;;
    esac
    case "$secret" in
      API_METRIKS_AWIN_PUBLISHER_ID)
        case "$(printenv "$secret")" in
          *[!0-9]*) fail "$secret must contain digits only" ;;
        esac
        ;;
    esac
  done
}

apply_api() {
  summary "### Secrets rendered for api"
  summary ""
  if [ "$api_google" = true ]; then
    file_result=$("$script_dir/push-remote-file.sh" --service-account-json \
      "$API_GOOGLE_JSON_FILE" API_GOOGLE_API_JSON)
    echo "$file_result"
    env_result=$(GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH="$API_GOOGLE_JSON_FILE" \
      "$script_dir/push-remote-env.sh" "$API_ENV_FILE" GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH)
    echo "$env_result"

    # Identifiers only, read from the validated JSON through a pipe: never the key itself.
    client_email=$(printenv API_GOOGLE_API_JSON | jq -r '.client_email')
    key_id=$(printenv API_GOOGLE_API_JSON | jq -r '.private_key_id // "unknown"' | cut -c1-8)
    summary "- $file_result"
    summary "- $env_result"
    summary "- Service account: $client_email (private_key_id ${key_id}...)"
  fi
  if [ -n "$api_metriks" ]; then
    keys=
    for pair in $api_metriks; do
      keys="$keys ${pair#*:}"
    done
    # Each api.env key is mapped from its secret in a subshell (export is a builtin: no value on a command line).
    # shellcheck disable=SC2086 # $keys holds key names only, split on purpose.
    metriks_result=$(
      for pair in $api_metriks; do
        export "${pair#*:}=$(printenv "${pair%%:*}")"
      done
      "$script_dir/push-remote-env.sh" "$API_ENV_FILE" $keys
    )
    echo "$metriks_result"
    summary "- $metriks_result"
  fi
  if [ -n "$api_metriks_missing" ]; then
    summary "- Not set in prod, not rendered:$api_metriks_missing"
  fi
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
