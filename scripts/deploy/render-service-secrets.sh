#!/bin/sh
# Render a service's secrets from the `beta` or `prod` GitHub Environment onto its host
# (ADR-0006, D140, GOU-933, GOU-957, GOU-1014, GOU-1041, GOU-1092).
#
# Usage: render-service-secrets.sh [--if-present] [--source] <check|apply> <env> <service>
#
#   check         Check that the service's secrets are well-formed, on the runner
#                 only: no SSH access, no value printed.
#   apply         Run check, then write the service's files on the host. The
#                 service is not restarted: the caller does it.
#   <env>         beta or prod: the GitHub Environment the caller's job declares.
#   <service>     api, ui, front-api, admin, b2b-api, frontend or infra.
#   --if-present  A missing required secret is a ::warning:: and is not rendered
#                 (release and beta deploy); without it, it fails. A secret that
#                 is set but malformed always fails, before anything is written.
#   --source      Render the `source` rows only: infraSecrets.yml passes it once the
#                 password has been changed at its source (Elasticsearch, MySQL...).
#                 Without it, those rows are skipped silently and their previous
#                 render stays: a deploy never renders a password that the source
#                 does not accept yet, and infraSecrets.yml never touches the others.
#
# This script is the single definition of what a service receives, shared by
# serviceSecrets.yml, testAndPublishBeta.yml, frontend-ci.yml and
# releaseDeployProd.yml (and infraSecrets.yml for the `source` rows and the infra
# service). Each secret is read from an environment variable of the same name,
# mapped in the calling step's `env:`. Every secret is optional unless marked
# `required`: a missing one is a ::warning::, is not rendered and does not stop the
# others; the previous render, or the open4goods-config value, is kept. A `source`
# secret is optional too, and only rendered with --source (which renders nothing else).
#
# Kinds of target:
#   tree     A file of the service's Spring Boot config tree,
#            /etc/open4goods/secrets/<host service>/<property> (mode 600, directory
#            700 when new), holding the exact value without a trailing newline.
#            Any value: JSON, quotes, spaces, newlines. Spring Boot
#            reads it through SPRING_CONFIG_IMPORT=optional:configtree:<that dir>/,
#            a line of /etc/open4goods/<host service>.env written by every apply
#            of a Spring service (an empty or missing tree is a no-op). The tree
#            beats application-active.yml and application.yml (one-off imports
#            take precedence over spring.config.location); the OS environment and
#            -D system properties still beat it.
#   tree-sa  Same, and the value must be a Google service account JSON key.
#   env      A KEY=<value> line of the service's env file. The value must match
#            [A-Za-z0-9/._-] (sourced by publish-jars.sh, env_file of docker compose).
#   digits   Same as env, digits only.
#   path-sa  A Google service account JSON key written to the file named in the
#            target (KEY=<file>), plus the line KEY=<file> in the service's env file.
#
# Host service and env file: api, ui, front-api and b2b-api are themselves;
# admin runs as `sbadmin` (/etc/open4goods/sbadmin.env); frontend reads
# /opt/open4goods/config/<env>/frontend/.env (env_file of docker-compose.frontend.yml,
# rewritten by open4goods-config's deploy: rendered before every publish); infra is
# the docker compose infrastructure (Kibana, XWiki, MySQL, Plausible), which reads
# /etc/open4goods/infra.env as a second --env-file of publish-infra.sh: no
# open4goods-config deploy writes /etc/open4goods/, and publish-jars.sh does not
# source that file (docs/operations/infra-secrets-rotation.md).
#
# | Service   | Secret                          | Kind    | Target                                                                         | Need     |
# | --------- | ------------------------------- | ------- | ------------------------------------------------------------------------------ | -------- |
# | api       | API_GOOGLE_API_JSON             | path-sa | GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH=/etc/open4goods/secrets/google-indexation.json | optional |
# | api       | API_METRIKS_AWIN_PUBLISHER_ID   | digits  | METRIKS_AFFILIATION_AWIN_PUBLISHER_ID                                          | optional |
# | api       | API_METRIKS_AWIN_API_TOKEN      | env     | METRIKS_AFFILIATION_AWIN_API_TOKEN                                             | optional |
# | api       | API_METRIKS_EFFILIATION_API_KEY | env     | METRIKS_AFFILIATION_EFFILIATION_API_KEY                                        | optional |
# | api       | API_EFFILIATION_API_KEY         | env     | AFFILIATIONCONFIG_EFFILIATIONAPIKEY (affiliation-config.effiliation-api-key)   | optional |
# | api       | API_VERTEX_API_JSON             | tree-sa | gen-ai-config.google-api-json                                                  | optional |
# | api       | API_VERTEX_BATCH_JSON           | tree-sa | vertex.batch.credentials-json                                                  | optional |
# | api       | API_GOOGLE_SEARCH_API_KEY       | tree    | googlesearch.api-key                                                           | optional |
# | api       | API_OPENAI_API_KEY              | tree    | spring.ai.openai.api-key                                                       | optional |
# | api       | API_AMAZON_ACCESS_KEY           | tree    | amazon-config.access-key                                                       | optional |
# | api       | API_AMAZON_SECRET_KEY           | tree    | amazon-config.secret-key                                                       | optional |
# | api       | API_AWIN_ACCESS_TOKEN           | tree    | affiliation-config.awin-access-token                                           | optional |
# | api       | API_KWANKO_TOKEN                | tree    | affiliation-config.kwanko-token                                                | optional |
# | api       | API_TRADETRACKER_API_KEY        | tree    | affiliation-config.tradetracker-api-key                                        | optional |
# | api       | API_EPREL_API_KEY               | tree    | eprel.api-key                                                                  | optional |
# | api       | O4G_ICECAT_PASSWORD             | tree    | icecat-feature-config.password                                                 | optional |
# | api       | O4G_SBA_PASSWORD                | tree    | spring.boot.admin.client.password                                              | optional |
# | api       | O4G_INTERNAL_API_KEY            | tree    | admin-key                                                                      | optional |
# | api       | O4G_ELASTICSEARCH_PASSWORD      | tree    | spring.elasticsearch.password                                                  | source   |
# | ui        | O4G_SBA_PASSWORD                | tree    | spring.boot.admin.client.password                                              | optional |
# | ui        | O4G_SBA_PASSWORD                | tree    | o4g.actuator.monitor.password                                                  | optional |
# | ui        | O4G_CAPTCHA_SECRET              | tree    | captcha-secret                                                                 | optional |
# | ui        | O4G_ELASTICSEARCH_PASSWORD      | tree    | spring.elasticsearch.password                                                  | source   |
# | front-api | GOOGLE_SSO_CLIENT_ID            | env     | FRONT_SECURITY_GOOGLE_SSO_CLIENT_ID (front.security.google-sso.client-id)      | required |
# | front-api | O4G_SBA_PASSWORD                | tree    | spring.boot.admin.client.password                                              | optional |
# | front-api | O4G_SBA_PASSWORD                | tree    | o4g.actuator.monitor.password                                                  | optional |
# | front-api | FRONT_API_JWT_SECRET            | tree    | front.security.jwt-secret                                                      | optional |
# | front-api | O4G_MACHINE_TOKEN               | tree    | front.security.shared-token                                                    | optional |
# | front-api | O4G_INTERNAL_API_KEY            | tree    | front.affiliation-partners.api-key                                             | optional |
# | front-api | O4G_ICECAT_PASSWORD             | tree    | icecat-feature-config.password                                                 | optional |
# | front-api | O4G_MAIL_PASSWORD               | tree    | spring.mail.password                                                           | optional |
# | front-api | O4G_CAPTCHA_SECRET              | tree    | captcha.secret-key                                                             | optional |
# | front-api | FRONT_API_GITHUB_TOKEN          | tree    | feedback.github.access-token                                                   | optional |
# | front-api | O4G_ELASTICSEARCH_PASSWORD      | tree    | spring.elasticsearch.password                                                  | source   |
# | admin     | O4G_SBA_PASSWORD                | tree    | spring.boot.admin.instance-auth.default-password                               | optional |
# | admin     | O4G_SBA_PASSWORD                | tree    | o4g.sba.registration.password                                                  | optional |
# | admin     | O4G_MAIL_PASSWORD               | tree    | spring.mail.password                                                           | optional |
# | b2b-api   | O4G_SBA_PASSWORD                | tree    | spring.boot.admin.client.password                                              | optional |
# | b2b-api   | O4G_ELASTICSEARCH_PASSWORD      | tree    | spring.elasticsearch.password                                                  | source   |
# | frontend  | GOOGLE_SSO_CLIENT_ID            | env     | NUXT_GOOGLE_OAUTH_CLIENT_ID                                                    | required |
# | frontend  | GOOGLE_SSO_CLIENT_SECRET        | env     | NUXT_GOOGLE_OAUTH_CLIENT_SECRET                                                | required |
# | frontend  | O4G_MACHINE_TOKEN               | env     | NUXT_MACHINE_TOKEN (runtimeConfig.machineToken)                                | optional |
# | frontend  | O4G_MACHINE_TOKEN               | env     | MACHINE_TOKEN (token check of publish-frontend.sh)                             | optional |
# | frontend  | FRONT_API_JWT_SECRET            | env     | FRONT_SECURITY_JWT_SECRET (server/utils/auth.ts, read at request time)          | optional |
# | infra     | INFRA_KIBANA_SYSTEM_PASSWORD    | env     | INFRA_KIBANA_SYSTEM_PASSWORD (kibana.yml elasticsearch.password)               | source   |
# | infra     | INFRA_KIBANA_ENCRYPTION_KEY     | env     | INFRA_KIBANA_ENCRYPTION_KEY (xpack.encryptedSavedObjects.encryptionKey)        | source   |
# | infra     | INFRA_XWIKI_DB_PASSWORD         | env     | INFRA_XWIKI_DB_PASSWORD (xwiki DB_PASSWORD, mysql MYSQL_PASSWORD; prod)        | source   |
# | infra     | INFRA_MYSQL_ROOT_PASSWORD       | env     | INFRA_MYSQL_ROOT_PASSWORD (mysql MYSQL_ROOT_PASSWORD; prod)                    | source   |
# | infra     | INFRA_PLAUSIBLE_SECRET_KEY_BASE | env     | PLAUSIBLE_SECRET_KEY_BASE (overrides config/<env>/infra/.env; beta)            | source   |
#
# Coupled secrets: every consumer must receive the same value in the same deployment
# (serviceSecrets.yml service=all, or a full release), otherwise the services stop
# talking to each other until the next restart:
#   O4G_SBA_PASSWORD      api, ui, front-api, b2b-api (SBA clients) <-> admin (registration
#                         and instance-auth); ui and front-api actuators <-> admin.
#   O4G_MACHINE_TOKEN     front-api (X-Shared-Token check) <-> frontend (sends it); it must
#                         match [A-Za-z0-9/._-] because the frontend reads an env line.
#   FRONT_API_JWT_SECRET  front-api (signs) <-> frontend (verifies admin tokens); same rule.
#   O4G_INTERNAL_API_KEY  front-api (front.affiliation-partners.api-key, sent to api) <->
#                         api (admin-key: the only key api accepts besides test-keys).
# Coupled with their source (infraSecrets.yml changes the source, then renders and restarts
# every consumer in the same run; never serviceSecrets.yml nor a deploy):
#   O4G_ELASTICSEARCH_PASSWORD    Elasticsearch user `elastic` <-> api, ui, front-api, b2b-api.
#   INFRA_KIBANA_SYSTEM_PASSWORD  Elasticsearch user `kibana_system` <-> Kibana.
#   INFRA_XWIKI_DB_PASSWORD       MySQL user `xwiki` <-> XWiki (prod).
#   INFRA_MYSQL_ROOT_PASSWORD     MySQL user `root` (prod).
#
# Required environment for apply: DEPLOY_SSH_HOST, DEPLOY_SSH_USER, DEPLOY_SSH_KEY.
set -eu

fail() { echo "::error::$*" >&2; exit 1; }

script_dir=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)

# <service> <secret> <kind> <target> <need>: see the table above.
SECRETS_TABLE='
api API_GOOGLE_API_JSON path-sa GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH=/etc/open4goods/secrets/google-indexation.json optional
api API_METRIKS_AWIN_PUBLISHER_ID digits METRIKS_AFFILIATION_AWIN_PUBLISHER_ID optional
api API_METRIKS_AWIN_API_TOKEN env METRIKS_AFFILIATION_AWIN_API_TOKEN optional
api API_METRIKS_EFFILIATION_API_KEY env METRIKS_AFFILIATION_EFFILIATION_API_KEY optional
api API_EFFILIATION_API_KEY env AFFILIATIONCONFIG_EFFILIATIONAPIKEY optional
api API_VERTEX_API_JSON tree-sa gen-ai-config.google-api-json optional
api API_VERTEX_BATCH_JSON tree-sa vertex.batch.credentials-json optional
api API_GOOGLE_SEARCH_API_KEY tree googlesearch.api-key optional
api API_OPENAI_API_KEY tree spring.ai.openai.api-key optional
api API_AMAZON_ACCESS_KEY tree amazon-config.access-key optional
api API_AMAZON_SECRET_KEY tree amazon-config.secret-key optional
api API_AWIN_ACCESS_TOKEN tree affiliation-config.awin-access-token optional
api API_KWANKO_TOKEN tree affiliation-config.kwanko-token optional
api API_TRADETRACKER_API_KEY tree affiliation-config.tradetracker-api-key optional
api API_EPREL_API_KEY tree eprel.api-key optional
api O4G_ICECAT_PASSWORD tree icecat-feature-config.password optional
api O4G_SBA_PASSWORD tree spring.boot.admin.client.password optional
api O4G_INTERNAL_API_KEY tree admin-key optional
api O4G_ELASTICSEARCH_PASSWORD tree spring.elasticsearch.password source
ui O4G_SBA_PASSWORD tree spring.boot.admin.client.password optional
ui O4G_SBA_PASSWORD tree o4g.actuator.monitor.password optional
ui O4G_CAPTCHA_SECRET tree captcha-secret optional
ui O4G_ELASTICSEARCH_PASSWORD tree spring.elasticsearch.password source
front-api GOOGLE_SSO_CLIENT_ID env FRONT_SECURITY_GOOGLE_SSO_CLIENT_ID required
front-api O4G_SBA_PASSWORD tree spring.boot.admin.client.password optional
front-api O4G_SBA_PASSWORD tree o4g.actuator.monitor.password optional
front-api FRONT_API_JWT_SECRET tree front.security.jwt-secret optional
front-api O4G_MACHINE_TOKEN tree front.security.shared-token optional
front-api O4G_INTERNAL_API_KEY tree front.affiliation-partners.api-key optional
front-api O4G_ICECAT_PASSWORD tree icecat-feature-config.password optional
front-api O4G_MAIL_PASSWORD tree spring.mail.password optional
front-api O4G_CAPTCHA_SECRET tree captcha.secret-key optional
front-api FRONT_API_GITHUB_TOKEN tree feedback.github.access-token optional
front-api O4G_ELASTICSEARCH_PASSWORD tree spring.elasticsearch.password source
admin O4G_SBA_PASSWORD tree spring.boot.admin.instance-auth.default-password optional
admin O4G_SBA_PASSWORD tree o4g.sba.registration.password optional
admin O4G_MAIL_PASSWORD tree spring.mail.password optional
b2b-api O4G_SBA_PASSWORD tree spring.boot.admin.client.password optional
b2b-api O4G_ELASTICSEARCH_PASSWORD tree spring.elasticsearch.password source
frontend GOOGLE_SSO_CLIENT_ID env NUXT_GOOGLE_OAUTH_CLIENT_ID required
frontend GOOGLE_SSO_CLIENT_SECRET env NUXT_GOOGLE_OAUTH_CLIENT_SECRET required
frontend O4G_MACHINE_TOKEN env NUXT_MACHINE_TOKEN optional
frontend O4G_MACHINE_TOKEN env MACHINE_TOKEN optional
frontend FRONT_API_JWT_SECRET env FRONT_SECURITY_JWT_SECRET optional
infra INFRA_KIBANA_SYSTEM_PASSWORD env INFRA_KIBANA_SYSTEM_PASSWORD source
infra INFRA_KIBANA_ENCRYPTION_KEY env INFRA_KIBANA_ENCRYPTION_KEY source
infra INFRA_XWIKI_DB_PASSWORD env INFRA_XWIKI_DB_PASSWORD source
infra INFRA_MYSQL_ROOT_PASSWORD env INFRA_MYSQL_ROOT_PASSWORD source
infra INFRA_PLAUSIBLE_SECRET_KEY_BASE env PLAUSIBLE_SECRET_KEY_BASE source
'

if_present=false
with_source=false
while [ "$#" -gt 0 ]; do
  case "$1" in
    --if-present) if_present=true ;;
    --source) with_source=true ;;
    *) break ;;
  esac
  shift
done
[ "$#" -eq 3 ] || fail "Usage: $0 [--if-present] [--source] <check|apply> <beta|prod> <service>"
action=$1
env_name=$2
service=$3
case "$action" in
  check | apply) ;;
  *) fail "Unknown action: $action (check or apply)" ;;
esac
case "$env_name" in
  beta | prod) ;;
  *) fail "Unknown environment: $env_name (beta or prod)" ;;
esac
case "$service" in
  api | ui | front-api | b2b-api) host_service=$service ;;
  admin) host_service=sbadmin ;;
  frontend | infra) host_service=$service ;;
  *) fail "Unknown service: $service (api, ui, front-api, admin, b2b-api, frontend or infra)" ;;
esac

if [ "$service" = frontend ]; then
  spring=false
  env_file=/opt/open4goods/config/$env_name/frontend/.env
elif [ "$service" = infra ]; then
  spring=false
  env_file=/etc/open4goods/infra.env
else
  spring=true
  env_file=/etc/open4goods/$host_service.env
  tree_dir=/etc/open4goods/secrets/$host_service
fi

summary() {
  echo "$*"
  if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    echo "$*" >> "$GITHUB_STEP_SUMMARY"
  fi
}

is_set() {
  [ -n "$(printenv "$1" || true)" ]
}

# Words of $2 contain $1.
contains() {
  case " $2 " in
    *" $1 "*) return 0 ;;
    *) return 1 ;;
  esac
}

# Rows of this service whose secret is set (one per line), and the secrets not set.
rows=
missing=
while read -r row_service secret kind target need; do
  [ "$row_service" = "$service" ] || continue
  # A `source` row is the business of infraSecrets.yml only, and only that (see --source).
  if [ "$need" = source ]; then
    [ "$with_source" = true ] || continue
  else
    [ "$with_source" = false ] || continue
  fi
  if is_set "$secret"; then
    rows="$rows$secret $kind $target
"
    continue
  fi
  contains "$secret" "$missing" && continue
  missing="${missing:+$missing }$secret"
  if [ "$need" = required ] && [ "$if_present" = false ]; then
    fail "$secret is empty or missing from the $env_name GitHub Environment (required for $service)"
  fi
  echo "::warning::$secret is not set in the $env_name GitHub Environment: not rendered for $service (a previous render is kept)."
done <<EOF
$SECRETS_TABLE
EOF

# Checks every set secret before anything is written; the value is never printed.
while read -r secret kind target; do
  [ -n "$secret" ] || continue
  case "$kind" in
    tree)
      # Any value; the property name becomes a file name under /etc/open4goods/ (push-remote-file.sh).
      ;;
    tree-sa | path-sa)
      if [ "$kind" = tree-sa ]; then
        file=$tree_dir/$target
      else
        file=${target#*=}
      fi
      "$script_dir/push-remote-file.sh" --service-account-json --validate-only "$file" "$secret" >/dev/null
      ;;
    env | digits)
      case "$(printenv "$secret")" in
        *[!A-Za-z0-9/._-]*) fail "$secret contains characters outside [A-Za-z0-9/._-] (rendered as an env line for $service)" ;;
      esac
      if [ "$kind" = digits ]; then
        case "$(printenv "$secret")" in
          *[!0-9]*) fail "$secret must contain digits only" ;;
        esac
      fi
      ;;
    *) fail "Unknown kind $kind for $secret" ;;
  esac
done <<EOF
$rows
EOF

if [ -z "$rows" ]; then
  echo "No secret of $service is set in the $env_name GitHub Environment."
else
  echo "Secrets of $service set in $env_name are well-formed:"
  while read -r secret kind target; do
    [ -n "$secret" ] && echo "- $secret -> ${target%%=*} ($kind)"
  done <<EOF
$rows
EOF
fi

[ "$action" = apply ] || exit 0

summary "### Secrets rendered for $service ($env_name)"
summary ""

# Identifiers only, read from the validated JSON through a pipe: never the key itself.
service_account_summary() {
  client_email=$(printenv "$1" | jq -r '.client_email')
  key_id=$(printenv "$1" | jq -r '.private_key_id // "unknown"' | cut -c1-8)
  summary "- $1: service account $client_email (private_key_id ${key_id}...)"
}

env_pairs=
while read -r secret kind target; do
  [ -n "$secret" ] || continue
  case "$kind" in
    tree | tree-sa)
      # The file content is the property value: no trailing newline (Spring Boot keeps it on multi-line values).
      option=--exact
      [ "$kind" = tree-sa ] && option="--exact --service-account-json"
      # shellcheck disable=SC2086 # $option holds flags only, split on purpose.
      result=$("$script_dir/push-remote-file.sh" $option "$tree_dir/$target" "$secret")
      summary "- $result ($secret)"
      [ "$kind" = tree-sa ] && service_account_summary "$secret"
      ;;
    path-sa)
      result=$("$script_dir/push-remote-file.sh" --service-account-json "${target#*=}" "$secret")
      summary "- $result ($secret)"
      service_account_summary "$secret"
      # The path line, as a pair whose "secret" is the literal path (exported below).
      env_pairs="$env_pairs ${target%%=*}=@${target#*=}"
      ;;
    env | digits)
      env_pairs="$env_pairs $target=$secret"
      ;;
  esac
done <<EOF
$rows
EOF

# Spring services always get their config tree import, even with no tree file yet: publish-jars.sh
# sources every service's env file in the same shell, so a service without the line would inherit the
# import of the service started before it.
if [ "$spring" = true ]; then
  env_pairs="$env_pairs SPRING_CONFIG_IMPORT=@optional:configtree:$tree_dir/"
fi

if [ -n "$env_pairs" ]; then
  keys=
  for pair in $env_pairs; do
    keys="$keys ${pair%%=*}"
  done
  # Each env key is mapped from its secret (or set to a literal after '@') in a subshell: export is a
  # builtin, so no value reaches a command line.
  # shellcheck disable=SC2086 # $keys holds key names only, split on purpose.
  env_result=$(
    for pair in $env_pairs; do
      source=${pair#*=}
      case "$source" in
        @*) export "${pair%%=*}=${source#@}" ;;
        *) export "${pair%%=*}=$(printenv "$source")" ;;
      esac
    done
    "$script_dir/push-remote-env.sh" "$env_file" $keys
  )
  summary "- $env_result"
fi

if [ -n "$missing" ]; then
  summary "- Not set in $env_name, not rendered: $missing"
fi
