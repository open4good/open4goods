#!/bin/sh
# Runs ON THE DEPLOY HOST, sent over SSH by infra-secrets.sh, which prepends the new values as NAME='value' lines
# (GOU-1092, campaign GOU-1038 wave 3). Never run it by hand with values on a command line.
#
# Usage: sh <payload> <test|source|restart|rollback> <beta|prod> <target>
#
#   test      Read only: prerequisites, the credential currently in service (found by authenticating, never
#             printed), whether the new value is already in service, what apply would do. Runs without the new
#             values too (dry run before Access writes them).
#   source    Changes the password at its source, authenticated with the value currently in service on this host:
#             Elasticsearch users `elastic` / `kibana_system` (_security/user/<user>/_password), MySQL users `xwiki`
#             and `root` (ALTER USER) plus XWiki's persisted hibernate.cfg.xml, the Postgres users of Plausible
#             and b2b-api (ALTER USER); ClickHouse takes its new password when restart recreates it. Before any
#             change, the value in service is kept in /etc/open4goods/infra-previous.env (mode 600) for
#             `rollback`. Idempotent: a value already accepted by the source is not changed again.
#   restart   Restarts the consumers once infra-secrets.sh has rendered the new value, in the order that keeps the
#             outage shortest, then checks them. Refuses to restart onto a render that does not hold the new value.
#   rollback  Puts the previous value back at the source and in the rendered files, then restarts the consumers.
#
# Targets and their secrets (only the target's secrets are set by the caller):
#   elasticsearch  O4G_ELASTICSEARCH_PASSWORD (user elastic) -> api, ui, front-api, b2b-api (config tree)
#   kibana         INFRA_KIBANA_SYSTEM_PASSWORD (user kibana_system), INFRA_KIBANA_ENCRYPTION_KEY -> Kibana; the
#                  key in service becomes INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY (decryptionOnlyKeys), then saved
#                  objects are re-encrypted (POST /api/encrypted_saved_objects/_rotate_key)
#   mysql          INFRA_XWIKI_DB_PASSWORD (user xwiki), INFRA_MYSQL_ROOT_PASSWORD (user root) -> XWiki (prod)
#   plausible      INFRA_PLAUSIBLE_SECRET_KEY_BASE -> Plausible, as PLAUSIBLE_SECRET_KEY_BASE (beta)
#   plausible-postgres    INFRA_PLAUSIBLE_PG_PASSWORD (Postgres user postgres of plausible_db) -> Plausible, as
#                         PLAUSIBLE_PG_PASSWORD (beta)
#   plausible-clickhouse  INFRA_CLICKHOUSE_PASSWORD (ClickHouse user default of plausible_events_db) -> ClickHouse
#                         itself, the dictionaries of Plausible's database (their source holds the password) and
#                         Plausible, as CLICKHOUSE_PASSWORD (beta); restart and rollback then check that Plausible
#                         writes (no Code: 516 in its logs)
#   b2b-postgres          B2B_API_POSTGRES_PASSWORD (Postgres user of b2b_postgres) -> b2b-api (config tree
#                         spring.datasource.password), compose as B2B_POSTGRES_PASSWORD
#
# Lines marked `ggignore` copy a variable to a file: GitGuardian takes them for hard-coded passwords.
# No value is printed: values are compared and passed through files of a umask 077 temp directory, stdin, or the
# environment of `docker exec -e NAME` (never `NAME=value` on a command line).
# Docs: docs/operations/infra-secrets-rotation.md.
set -eu

fail() { echo "ERROR: $*" >&2; exit 1; }
ok() { echo "OK: $*"; }
info() { echo "INFO: $*"; }

[ "$#" -eq 3 ] || fail "Usage: sh <payload> <test|source|restart|rollback> <beta|prod> <target>"
action=$1
env_name=$2
target=$3
case "$action" in
  test | source | restart | rollback) ;;
  *) fail "Unknown action" ;;
esac
case "$env_name" in
  beta | prod) ;;
  *) fail "Unknown environment" ;;
esac
case "$target" in
  elasticsearch | kibana) ;;
  mysql) [ "$env_name" = prod ] || fail "mysql: only prod runs XWiki and MySQL" ;;
  plausible | plausible-postgres | plausible-clickhouse)
    [ "$env_name" = beta ] || fail "$target: only beta runs Plausible"
    ;;
  b2b-postgres) ;;
  *) fail "Unknown target" ;;
esac

# INFRA_SECRETS_ROOT, ES_URL, KIBANA_URL, XWIKI_URL and PLAUSIBLE_URL only exist for scripts/tests/infra-secrets.test.sh.
root=${INFRA_SECRETS_ROOT:-}
etc=$root/etc/open4goods
opt=$root/opt/open4goods
infra_env=$etc/infra.env
previous_env=$etc/infra-previous.env
es_url=${ES_URL:-http://127.0.0.1:9200}
kibana_url=${KIBANA_URL:-http://127.0.0.1:5601}
xwiki_url=${XWIKI_URL:-http://127.0.0.1:8080}
plausible_url=${PLAUSIBLE_URL:-http://127.0.0.1:8000}
# Spring consumers of the elastic password, in restart order: front-api first (it answers the site; publish-jars.sh
# only waits for the running api to answer), then api, ui and b2b-api.
es_services="front-api api ui b2b-api"

umask 077
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# --- values -------------------------------------------------------------------------------------------------------

# Last value of KEY in an env file (KEY=value, optional `export `), as written: infra-previous.env.
raw_value() {
  [ -f "$1" ] || return 0
  sed -n "s/^\(export \)\{0,1\}$2=//p" "$1" | tail -n 1
}

# Same, single quotes of a compose env file removed (infra.env, config/<env>/infra/.env).
env_value() {
  value=$(raw_value "$1" "$2")
  case "$value" in
    \'*\') value=${value#\'}; value=${value%\'} ;;
  esac
  printf '%s\n' "$value"
}

# KEY=value line of infra.env, a compose --env-file: unquoted in [A-Za-z0-9/._-], else single-quoted (taken
# literally by compose, `$` included). A quote or a newline cannot be carried.
set_infra_line() {
  case "$2" in
    *"'"* | *'
'*) fail "the value for $1 holds a quote or a newline: an env file cannot carry it (runbook)" ;;
    *[!A-Za-z0-9/._-]*) set_env_line "$infra_env" "$1" "'$2'" ;;
    *) set_env_line "$infra_env" "$1" "$2" ;;
  esac
}

# Replaces (or adds) KEY=value in an env file, atomically, mode 600, keeping the owner and the other lines.
set_env_line() {
  file=$1 key=$2 value=$3
  mkdir -p "$(dirname "$file")"
  tmp=$(mktemp "$file.XXXXXX")
  if [ -f "$file" ]; then
    grep -v -E "^(export )?$key=" "$file" > "$tmp" || true
    chown --reference="$file" "$tmp" 2>/dev/null || true
  fi
  printf '%s=%s\n' "$key" "$value" >> "$tmp"
  mv "$tmp" "$file"
}

# Writes a value into a file (config tree: no trailing newline), atomically, mode 600, keeping the owner.
write_exact() {
  file=$1 value=$2
  tmp=$(mktemp "$file.XXXXXX")
  chown --reference="$file" "$tmp" 2>/dev/null || true
  printf '%s' "$value" > "$tmp"
  mv "$tmp" "$file"
}

# `password:` of the block `<parent>:` in a Spring YAML file (application-active.yml), quotes removed.
yaml_block_password() {
  [ -f "$1" ] || return 0
  awk -v parent="$2" '
    function indent(s) { match(s, /^ */); return RLENGTH }
    $0 ~ "^ *" parent ": *$" { depth = indent($0); inside = 1; next }
    inside && /^ *[^ #]/ && indent($0) <= depth { inside = 0 }
    inside && /^ *password: */ {
      sub(/^ *password: */, ""); sub(/ +$/, "")
      if ($0 ~ /^".*"$/ || $0 ~ /^'\''.*'\''$/) $0 = substr($0, 2, length($0) - 2)
      print; exit
    }' "$1"
}

# Value of a flat or nested `key:` in kibana.yml, quotes removed; empty when it is a ${VAR} placeholder.
kibana_yml_value() {
  [ -f "$1" ] || return 0
  value=$(sed -n "s/^[[:space:]]*$2:[[:space:]]*//p" "$1" | head -n 1 | sed 's/[[:space:]]*$//')
  case "$value" in
    \"*\") value=${value#\"}; value=${value%\"} ;;
    \'*\') value=${value#\'}; value=${value%\'} ;;
  esac
  case "$value" in
    *'${'*) ;;
    *) printf '%s' "$value" ;;
  esac
}

# Literal `KEY=value` (list form) or `KEY: value` (map form) of a compose file; empty when it is a ${VAR} placeholder.
compose_value() {
  [ -f "$1" ] || return 0
  value=$(sed -n -e "s/^[[:space:]]*- *$2=//p" -e "s/^[[:space:]]*$2:[[:space:]]*//p" "$1" | head -n 1 \
    | sed 's/[[:space:]]*$//')
  case "$value" in
    \"*\") value=${value#\"}; value=${value%\"} ;;
  esac
  case "$value" in
    *'${'*) ;;
    *) printf '%s' "$value" ;;
  esac
}

# --- Elasticsearch ------------------------------------------------------------------------------------------------

# curl config holding the basic auth of $1:$2 (never on the command line); quotes and backslashes escaped.
curl_auth() {
  escaped=$(printf '%s:%s' "$1" "$2" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g')
  printf 'user = "%s"\n' "$escaped" > "$work/curl.cfg"
}

# HTTP code of an Elasticsearch call authenticated as $1:$2: <method> <path> [json body file].
es_call() {
  curl_auth "$1" "$2"
  set -- "$3" "$4" "${5:-}"
  if [ -n "$3" ]; then
    curl -s -o "$work/es.out" -w '%{http_code}' -K "$work/curl.cfg" -X "$1" -H 'Content-Type: application/json' \
      --data-binary "@$3" "$es_url$2" || echo 000
  else
    curl -s -o "$work/es.out" -w '%{http_code}' -K "$work/curl.cfg" -X "$1" "$es_url$2" || echo 000
  fi
}

# True when $2 is the password of the Elasticsearch user $1.
es_accepts() {
  [ -n "$2" ] || return 1
  [ "$(es_call "$1" "$2" GET /_security/_authenticate)" = 200 ]
}

# The first candidate that is the password of user $1, written to $work/current (never printed); $2...: files
# holding one candidate each, in order.
find_es_password() {
  user=$1
  shift
  for candidate in "$@"; do
    [ -s "$candidate" ] || continue
    if es_accepts "$user" "$(cat "$candidate")"; then
      cp "$candidate" "$work/current"
      echo "${candidate#"$work/"}"
      return 0
    fi
  done
  return 1
}

# Candidate files of the elastic password in service: rendered trees, then open4goods-config, then the saved one.
elastic_candidates() {
  for svc in $es_services; do
    tree=$etc/secrets/$svc/spring.elasticsearch.password
    [ -f "$tree" ] && cp "$tree" "$work/candidate-tree-$svc"
    yaml_block_password "$opt/config/$env_name/$svc/application-active.yml" elasticsearch > "$work/candidate-yml-$svc"
  done
  raw_value "$previous_env" O4G_ELASTICSEARCH_PASSWORD > "$work/candidate-previous"
  for svc in $es_services; do
    printf '%s ' "$work/candidate-tree-$svc"
  done
  for svc in $es_services; do
    printf '%s ' "$work/candidate-yml-$svc"
  done
  printf '%s\n' "$work/candidate-previous"
}

# Sets the password of user $3 to the content of file $4, authenticated as $1 with the content of file $2.
es_set_password() {
  value=$(cat "$4")
  printf '{"password":"%s"}' "$(printf '%s' "$value" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g')" > "$work/body.json"
  code=$(es_call "$1" "$(cat "$2")" POST "/_security/user/$3/_password" "$work/body.json")
  rm -f "$work/body.json"
  [ "$code" = 200 ] || fail "Elasticsearch refused to change the password of $3 (HTTP $code)"
  es_accepts "$3" "$value" || fail "the new password of $3 is not accepted after the change"
  ok "password of the Elasticsearch user $3 changed"
}

# The password of `elastic` in service, to $work/elastic (the new one if already applied).
working_elastic() {
  printf '%s' "${O4G_ELASTICSEARCH_PASSWORD:-}" > "$work/new-elastic"
  # shellcheck disable=SC2046 # file names without spaces, split on purpose.
  found=$(find_es_password elastic "$work/new-elastic" $(elastic_candidates)) \
    || fail "no known password of the Elasticsearch user elastic is accepted (trees, application-active.yml, previous)"
  mv "$work/current" "$work/elastic"
  echo "$found"
}

# --- docker -------------------------------------------------------------------------------------------------------

container_running() {
  [ "$(docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null || true)" = true ]
}

# kibana.yml as the running Kibana container sees it: publish-infra.sh replaces the file of the host (new inode),
# so once open4goods-config step d-infra is copied there, only the container still has the literal in service.
kibana_container_yml() {
  docker exec kibana cat /usr/share/kibana/config/kibana.yml > "$work/kibana-container.yml" 2>/dev/null \
    || : > "$work/kibana-container.yml"
}

# MySQL as user $1 with the password in file $2; SQL on stdin; output to stdout.
mysql_as() {
  MYSQL_PWD=$(cat "$2") docker exec -i -e MYSQL_PWD mysql mysql -u"$1" -N -B 2>/dev/null
}

mysql_accepts() {
  [ -s "$2" ] || return 1
  echo 'SELECT 1;' | mysql_as "$1" "$2" > /dev/null
}

find_mysql_password() {
  user=$1
  shift
  for candidate in "$@"; do
    if mysql_accepts "$user" "$candidate"; then
      cp "$candidate" "$work/current-$user"
      echo "${candidate#"$work/"}"
      return 0
    fi
  done
  return 1
}

# Waits until $1 answers one of the HTTP codes $2 (comma-separated), $3 attempts 5 s apart; $4: what to do on failure.
wait_http() {
  i=0
  while [ "$i" -lt "$3" ]; do
    code=$(curl -s -o /dev/null -w '%{http_code}' "$1" || echo 000)
    case ",$2," in
      *",$code,"*) ok "$1 answers HTTP $code"; return 0 ;;
    esac
    i=$((i + 1))
    sleep "${WAIT_SECONDS:-5}"
  done
  fail "$1 did not answer HTTP $2 in time (last: $code)${4:+: $4}"
}

# publish-infra.sh must read /etc/open4goods/infra.env and recreate a single service (open4goods-config step d-infra).
check_publish_infra() {
  script=$opt/bin/publish-infra.sh
  [ -f "$script" ] || fail "$script is missing"
  grep -q '/etc/open4goods/infra.env' "$script" \
    || fail "$script does not read /etc/open4goods/infra.env yet: deploy open4goods-config step d-infra first"
  grep -q 'INFRA_SERVICES' "$script" \
    || fail "$script cannot recreate a single service yet: deploy open4goods-config step d-infra first"
  ok "$script reads /etc/open4goods/infra.env and recreates single services"
}

# $1 must reference every ${VAR} given after it (open4goods-config step d-infra deployed to latest/<env>/).
check_placeholders() {
  file=$1
  shift
  [ -f "$file" ] || fail "$file is missing"
  for var in "$@"; do
    grep -q "\${$var" "$file" || fail "$file does not read \${$var} yet: deploy open4goods-config step d-infra first"
  done
  ok "$file reads $*"
}

writable_etc() {
  [ -d "$etc" ] && [ -w "$etc" ] || fail "$etc is not writable by $(id -un)"
  for file in "$infra_env" "$previous_env"; do
    [ ! -e "$file" ] || [ -w "$file" ] || fail "$file is not writable by $(id -un)"
  done
  ok "$etc is writable"
}

# Keeps the value in service of $1 (file $2) for rollback, unless already kept (a re-run after a partial apply).
keep_previous() {
  if [ -n "$(raw_value "$previous_env" "$1")" ]; then
    info "the previous value of $1 is already kept in $previous_env"
    return 0
  fi
  case "$(cat "$2")" in
    *'
'*) fail "the value in service of $1 spans several lines: change it by hand (runbook)" ;;
  esac
  set_env_line "$previous_env" "$1" "$(cat "$2")"
  ok "previous value of $1 kept in $previous_env (mode $(stat -c '%a' "$previous_env"))"
}

# True when the file $1 holds exactly the value $2.
holds() {
  [ -f "$1" ] && [ "$(cat "$1")" = "$2" ]
}

# --- targets ------------------------------------------------------------------------------------------------------

elasticsearch_test() {
  writable_etc
  for svc in $es_services; do
    runtime=$opt/runtime/$svc.jar
    latest=$opt/latest/$svc-latest.jar
    [ -f "$runtime" ] && [ -f "$latest" ] || fail "$runtime or $latest is missing"
    cmp -s "$runtime" "$latest" || fail "$runtime differs from $latest: a restart would deploy a JAR that is not running"
  done
  ok "the running JARs of $es_services are the latest ones"
  [ -f "$opt/bin/publish-jars.sh" ] || fail "$opt/bin/publish-jars.sh is missing"
  found=$(working_elastic)
  if [ "$found" = new-elastic ]; then
    ok "Elasticsearch already accepts the new password of elastic: source would skip the change"
  elif [ -z "${O4G_ELASTICSEARCH_PASSWORD:-}" ]; then
    ok "the password of elastic in service is found ($found) and accepted (no new value yet)"
  else
    ok "the password of elastic in service is found ($found) and accepted; source would change it"
  fi
}

elasticsearch_source() {
  found=$(working_elastic)
  if [ "$found" = new-elastic ]; then
    ok "Elasticsearch already accepts the new password of elastic: nothing to change"
    return 0
  fi
  writable_etc
  keep_previous O4G_ELASTICSEARCH_PASSWORD "$work/elastic"
  es_set_password elastic "$work/elastic" elastic "$work/new-elastic"
}

elasticsearch_restart() {
  printf '%s' "$O4G_ELASTICSEARCH_PASSWORD" > "$work/new-elastic" # ggignore
  es_accepts elastic "$O4G_ELASTICSEARCH_PASSWORD" || fail "Elasticsearch does not accept the new password: run source first"
  for svc in $es_services; do
    holds "$etc/secrets/$svc/spring.elasticsearch.password" "$O4G_ELASTICSEARCH_PASSWORD" \
      || fail "the config tree of $svc does not hold the new password: render it first"
  done
  ok "the config trees of $es_services hold the new password"
  for svc in $es_services; do
    echo "----- Restarting $svc -----"
    sh "$opt/bin/publish-jars.sh" "$env_name" restart "$svc"
  done
}

elasticsearch_rollback() {
  previous=$(raw_value "$previous_env" O4G_ELASTICSEARCH_PASSWORD)
  [ -n "$previous" ] || fail "no previous password of elastic is kept in $previous_env"
  printf '%s' "$previous" > "$work/previous"
  if ! es_accepts elastic "$previous"; then
    working_elastic > /dev/null
    es_set_password elastic "$work/elastic" elastic "$work/previous"
  else
    ok "Elasticsearch already accepts the previous password of elastic"
  fi
  for svc in $es_services; do
    tree=$etc/secrets/$svc/spring.elasticsearch.password
    if [ -f "$tree" ]; then
      write_exact "$tree" "$previous"
      ok "$tree holds the previous password"
    fi
  done
  for svc in $es_services; do
    echo "----- Restarting $svc -----"
    sh "$opt/bin/publish-jars.sh" "$env_name" restart "$svc"
  done
}

kibana_prerequisites() {
  container_running kibana || fail "the kibana container is not running"
  check_publish_infra
  check_placeholders "$opt/latest/$env_name/kibana.yml" INFRA_KIBANA_SYSTEM_PASSWORD INFRA_KIBANA_ENCRYPTION_KEY \
    INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY
  check_placeholders "$opt/latest/$env_name/docker-compose.infra.yml" INFRA_KIBANA_SYSTEM_PASSWORD \
    INFRA_KIBANA_ENCRYPTION_KEY INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY
}

# The encryption key in service, to $work/key: infra.env once rendered, else the literal of kibana.yml (host file,
# then the running container's).
kibana_key_in_service() {
  key=$(env_value "$infra_env" INFRA_KIBANA_ENCRYPTION_KEY)
  [ -n "$key" ] || key=$(kibana_yml_value "$opt/bin/kibana.yml" encryptionKey)
  if [ -z "$key" ]; then
    kibana_container_yml
    key=$(kibana_yml_value "$work/kibana-container.yml" encryptionKey)
  fi
  [ -n "$key" ] || fail "no Kibana encryption key in service found (infra.env, kibana.yml of the host and of the container)"
  printf '%s' "$key" > "$work/key"
}

kibana_system_candidates() {
  env_value "$infra_env" INFRA_KIBANA_SYSTEM_PASSWORD > "$work/candidate-infra-env"
  kibana_yml_value "$opt/bin/kibana.yml" elasticsearch.password > "$work/candidate-kibana-yml" # ggignore
  kibana_container_yml
  kibana_yml_value "$work/kibana-container.yml" elasticsearch.password > "$work/candidate-kibana-container" # ggignore
  raw_value "$previous_env" INFRA_KIBANA_SYSTEM_PASSWORD > "$work/candidate-previous"
  echo "$work/candidate-infra-env $work/candidate-kibana-yml $work/candidate-kibana-container $work/candidate-previous"
}

# test reports every missing prerequisite (in a subshell), then fails at the end.
blocked=false
prerequisite() {
  if [ "$action" = test ]; then
    ( "$@" ) || blocked=true
  else
    "$@"
  fi
}

kibana_test() {
  writable_etc
  prerequisite kibana_prerequisites
  found=$(working_elastic)
  ok "the password of elastic in service is found ($found) and accepted (used to change kibana_system)"
  printf '%s' "${INFRA_KIBANA_SYSTEM_PASSWORD:-}" > "$work/new-kibana" # ggignore
  # shellcheck disable=SC2046 # file names without spaces, split on purpose.
  found=$(find_es_password kibana_system "$work/new-kibana" $(kibana_system_candidates)) \
    || fail "no known password of kibana_system is accepted (infra.env, kibana.yml of the host and of the container, previous)"
  if [ "$found" = new-kibana ]; then
    ok "Elasticsearch already accepts the new password of kibana_system"
  else
    ok "the password of kibana_system in service is found ($found); source would change it"
  fi
  kibana_key_in_service
  if [ "$(cat "$work/key")" = "${INFRA_KIBANA_ENCRYPTION_KEY:-}" ]; then
    ok "the new encryption key is already in service"
  else
    case "$(cat "$work/key")" in
      *"'"*) fail "the encryption key in service holds a quote: an env file cannot carry it (runbook)" ;;
    esac
    ok "the encryption key in service is found; source would keep it as the decryption-only key"
  fi
}

kibana_source() {
  writable_etc
  kibana_prerequisites
  working_elastic > /dev/null
  kibana_key_in_service
  if [ "$(cat "$work/key")" != "$INFRA_KIBANA_ENCRYPTION_KEY" ]; then
    keep_previous INFRA_KIBANA_ENCRYPTION_KEY "$work/key"
    set_infra_line INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY "$(cat "$work/key")"
    ok "the encryption key in service becomes INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY in $infra_env (decryption only)"
  else
    ok "the new encryption key is already in service"
  fi
  printf '%s' "$INFRA_KIBANA_SYSTEM_PASSWORD" > "$work/new-kibana" # ggignore
  # shellcheck disable=SC2046 # file names without spaces, split on purpose.
  found=$(find_es_password kibana_system "$work/new-kibana" $(kibana_system_candidates)) \
    || fail "no known password of kibana_system is accepted (infra.env, kibana.yml of the host and of the container, previous)"
  if [ "$found" = new-kibana ]; then
    ok "Elasticsearch already accepts the new password of kibana_system: nothing to change"
    return 0
  fi
  keep_previous INFRA_KIBANA_SYSTEM_PASSWORD "$work/current"
  es_set_password elastic "$work/elastic" kibana_system "$work/new-kibana"
}

kibana_restart() {
  es_accepts kibana_system "$INFRA_KIBANA_SYSTEM_PASSWORD" || fail "Elasticsearch does not accept the new kibana_system password: run source first"
  [ "$(env_value "$infra_env" INFRA_KIBANA_SYSTEM_PASSWORD)" = "$INFRA_KIBANA_SYSTEM_PASSWORD" ] \
    && [ "$(env_value "$infra_env" INFRA_KIBANA_ENCRYPTION_KEY)" = "$INFRA_KIBANA_ENCRYPTION_KEY" ] \
    || fail "$infra_env does not hold the new Kibana values: render it first"
  check_publish_infra
  INFRA_SERVICES=kibana sh "$opt/bin/publish-infra.sh" "$env_name"
  working_elastic > /dev/null
  curl_auth elastic "$(cat "$work/elastic")"
  i=0
  until curl -s -K "$work/curl.cfg" "$kibana_url/api/status" 2>/dev/null | grep -q '"level":"available"'; do
    i=$((i + 1))
    [ "$i" -lt "${KIBANA_ATTEMPTS:-60}" ] || fail "Kibana is not available after its restart"
    sleep "${WAIT_SECONDS:-5}"
  done
  ok "Kibana is available"
  # No query parameter: Kibana 9 rejects any unknown one with HTTP 400 (only batch_size and type exist).
  code=$(curl -s -o "$work/rotate.json" -w '%{http_code}' -K "$work/curl.cfg" -X POST -H 'kbn-xsrf: true' \
    "$kibana_url/api/encrypted_saved_objects/_rotate_key" || echo 000)
  [ "$code" = 200 ] || fail "re-encryption of the saved objects failed (HTTP $code)"
  # Counts only: {"total":n,"successful":n,"failed":n}.
  info "saved objects re-encryption: $(head -c 300 "$work/rotate.json")"
}

kibana_rollback() {
  check_publish_infra
  working_elastic > /dev/null
  previous=$(raw_value "$previous_env" INFRA_KIBANA_SYSTEM_PASSWORD)
  if [ -n "$previous" ]; then
    printf '%s' "$previous" > "$work/previous"
    if ! es_accepts kibana_system "$previous"; then
      es_set_password elastic "$work/elastic" kibana_system "$work/previous"
    fi
    set_infra_line INFRA_KIBANA_SYSTEM_PASSWORD "$previous"
    ok "kibana_system has its previous password, in $infra_env too"
  fi
  previous_key=$(raw_value "$previous_env" INFRA_KIBANA_ENCRYPTION_KEY)
  if [ -n "$previous_key" ]; then
    # The new key stays readable: objects re-encrypted with it are not lost.
    current_key=$(env_value "$infra_env" INFRA_KIBANA_ENCRYPTION_KEY)
    set_infra_line INFRA_KIBANA_ENCRYPTION_KEY "$previous_key"
    [ -z "$current_key" ] || [ "$current_key" = "$previous_key" ] \
      || set_infra_line INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY "$current_key"
    ok "the previous encryption key is back in $infra_env, the new one is decryption only"
  fi
  INFRA_SERVICES=kibana sh "$opt/bin/publish-infra.sh" "$env_name"
}

mysql_prerequisites() {
  container_running mysql || fail "the mysql container is not running"
  container_running xwiki-mysql-tomcat-web || fail "the xwiki-mysql-tomcat-web container is not running"
  check_publish_infra
  check_placeholders "$opt/latest/$env_name/docker-compose.infra.yml" INFRA_XWIKI_DB_PASSWORD INFRA_MYSQL_ROOT_PASSWORD
}

# Candidates: infra.env, the literal of the host's compose file, the environment the container was created with,
# the kept previous value.
mysql_candidates() {
  # $1: root or xwiki.
  if [ "$1" = root ]; then
    env_value "$infra_env" INFRA_MYSQL_ROOT_PASSWORD > "$work/candidate-$1-infra-env"
    compose_value "$opt/bin/docker-compose.infra.yml" MYSQL_ROOT_PASSWORD > "$work/candidate-$1-compose"
    docker exec mysql printenv MYSQL_ROOT_PASSWORD 2>/dev/null | tr -d '\n' > "$work/candidate-$1-container" || :
    raw_value "$previous_env" INFRA_MYSQL_ROOT_PASSWORD > "$work/candidate-$1-previous"
  else
    env_value "$infra_env" INFRA_XWIKI_DB_PASSWORD > "$work/candidate-$1-infra-env"
    compose_value "$opt/bin/docker-compose.infra.yml" DB_PASSWORD > "$work/candidate-$1-compose"
    docker exec xwiki-mysql-tomcat-web printenv DB_PASSWORD 2>/dev/null | tr -d '\n' > "$work/candidate-$1-container" || :
    raw_value "$previous_env" INFRA_XWIKI_DB_PASSWORD > "$work/candidate-$1-previous"
  fi
  echo "$work/candidate-$1-infra-env $work/candidate-$1-compose $work/candidate-$1-container $work/candidate-$1-previous"
}

# The root password in service to $work/current-root; prints where it was found.
working_root() {
  printf '%s' "${INFRA_MYSQL_ROOT_PASSWORD:-}" > "$work/new-root" # ggignore
  # shellcheck disable=SC2046 # file names without spaces, split on purpose.
  find_mysql_password root "$work/new-root" $(mysql_candidates root) \
    || fail "no known password of the MySQL user root is accepted (infra.env, compose, previous)"
}

# ALTER USER for every host of user $1, to the content of file $2, as root.
mysql_alter() {
  hosts=$(printf "SELECT host FROM mysql.user WHERE user = '%s';\n" "$1" | mysql_as root "$work/current-root") \
    || fail "cannot list the hosts of the MySQL user $1"
  [ -n "$hosts" ] || fail "the MySQL user $1 does not exist"
  # SQL string literals: quotes doubled, backslashes escaped (a previous value may hold them).
  value=$(sed -e 's/\\/\\\\/g' -e "s/'/''/g" "$2")
  for host in $hosts; do
    printf "ALTER USER '%s'@'%s' IDENTIFIED BY '%s';\n" "$1" "$host" "$value"
  done | mysql_as root "$work/current-root" > /dev/null || fail "ALTER USER $1 failed"
  mysql_accepts "$1" "$2" || fail "the new password of the MySQL user $1 is not accepted after the change"
  ok "password of the MySQL user $1 changed ($(echo "$hosts" | wc -w) host(s))"
}

# XWiki keeps hibernate.cfg.xml in its permanent directory and restores it at every start (the DB_PASSWORD of the
# compose file is only used when that file does not exist): its connection password is rewritten there too.
xwiki_hibernate() {
  file=/usr/local/xwiki/data/hibernate.cfg.xml
  if ! docker exec xwiki-mysql-tomcat-web test -f "$file"; then
    info "no persisted $file: XWiki configures itself from DB_PASSWORD at its next start"
    return 0
  fi
  if [ "$1" = test ]; then
    ok "persisted $file found: source rewrites its connection password"
    return 0
  fi
  # shellcheck disable=SC2016 # expanded in the container.
  XWIKI_DB_PASSWORD=$(cat "$2") docker exec -e XWIKI_DB_PASSWORD xwiki-mysql-tomcat-web sh -c '
    set -eu
    f=/usr/local/xwiki/data/hibernate.cfg.xml
    awk '\''{
      while (match($0, /name="hibernate\.connection\.password">[^<]*</)) {
        $0 = substr($0, 1, RSTART - 1) "name=\"hibernate.connection.password\">" ENVIRON["XWIKI_DB_PASSWORD"] "<" \
          substr($0, RSTART + RLENGTH)
        break
      }
      print
    }'\'' "$f" > "$f.new"
    cat "$f.new" > "$f"
    rm -f "$f.new"'
  ok "the persisted hibernate.cfg.xml of XWiki holds the new connection password"
}

mysql_test() {
  writable_etc
  prerequisite mysql_prerequisites
  found=$(working_root)
  ok "the password of root in service is found ($found) and accepted"
  printf '%s' "${INFRA_XWIKI_DB_PASSWORD:-}" > "$work/new-xwiki" # ggignore
  # shellcheck disable=SC2046 # file names without spaces, split on purpose.
  found=$(find_mysql_password xwiki "$work/new-xwiki" $(mysql_candidates xwiki)) \
    || fail "no known password of the MySQL user xwiki is accepted (infra.env, compose, previous)"
  ok "the password of xwiki in service is found ($found) and accepted"
  xwiki_hibernate test
}

mysql_source() {
  writable_etc
  mysql_prerequisites
  working_root > /dev/null
  printf '%s' "$INFRA_XWIKI_DB_PASSWORD" > "$work/new-xwiki" # ggignore
  # shellcheck disable=SC2046 # file names without spaces, split on purpose.
  found=$(find_mysql_password xwiki "$work/new-xwiki" $(mysql_candidates xwiki)) \
    || fail "no known password of the MySQL user xwiki is accepted (infra.env, compose, previous)"
  if [ "$found" = new-xwiki ]; then
    ok "MySQL already accepts the new password of xwiki"
  else
    keep_previous INFRA_XWIKI_DB_PASSWORD "$work/current-xwiki"
    mysql_alter xwiki "$work/new-xwiki"
  fi
  xwiki_hibernate apply "$work/new-xwiki"
  if mysql_accepts root "$work/new-root"; then
    ok "MySQL already accepts the new password of root"
  else
    keep_previous INFRA_MYSQL_ROOT_PASSWORD "$work/current-root"
    mysql_alter root "$work/new-root"
  fi
}

mysql_restart() {
  printf '%s' "$INFRA_XWIKI_DB_PASSWORD" > "$work/new-xwiki" # ggignore
  mysql_accepts xwiki "$work/new-xwiki" || fail "MySQL does not accept the new xwiki password: run source first"
  [ "$(env_value "$infra_env" INFRA_XWIKI_DB_PASSWORD)" = "$INFRA_XWIKI_DB_PASSWORD" ] \
    && [ "$(env_value "$infra_env" INFRA_MYSQL_ROOT_PASSWORD)" = "$INFRA_MYSQL_ROOT_PASSWORD" ] \
    || fail "$infra_env does not hold the new MySQL values: render it first"
  check_publish_infra
  # XWiki only: the mysql container keeps running (MYSQL_* are only read when its data directory is created).
  INFRA_SERVICES=xwiki sh "$opt/bin/publish-infra.sh" "$env_name"
  wait_http "$xwiki_url/" 200,301,302,401 "${XWIKI_ATTEMPTS:-120}"
}

mysql_rollback() {
  check_publish_infra
  printf '%s' "${INFRA_MYSQL_ROOT_PASSWORD:-}" > "$work/new-root" # ggignore
  working_root > /dev/null
  previous=$(raw_value "$previous_env" INFRA_XWIKI_DB_PASSWORD)
  if [ -n "$previous" ]; then
    printf '%s' "$previous" > "$work/previous-xwiki"
    mysql_accepts xwiki "$work/previous-xwiki" || mysql_alter xwiki "$work/previous-xwiki"
    xwiki_hibernate apply "$work/previous-xwiki"
    set_infra_line INFRA_XWIKI_DB_PASSWORD "$previous"
  fi
  previous=$(raw_value "$previous_env" INFRA_MYSQL_ROOT_PASSWORD)
  if [ -n "$previous" ]; then
    printf '%s' "$previous" > "$work/previous-root"
    mysql_accepts root "$work/previous-root" || mysql_alter root "$work/previous-root"
    set_infra_line INFRA_MYSQL_ROOT_PASSWORD "$previous"
  fi
  INFRA_SERVICES=xwiki sh "$opt/bin/publish-infra.sh" "$env_name"
  wait_http "$xwiki_url/" 200,301,302,401 "${XWIKI_ATTEMPTS:-120}"
}

# Users with TOTP enabled lose their second factor: Plausible derives TOTP_VAULT_KEY from SECRET_KEY_BASE when it
# is not set (config/runtime.exs of Plausible CE).
plausible_totp_users() {
  count=$(echo 'SELECT count(*) FROM users WHERE totp_enabled;' \
    | docker exec -i plausible_db psql -U postgres -d plausible -tA 2>/dev/null || echo unknown)
  info "Plausible users with TOTP enabled (their 2FA must be reset after the rotation): $count"
}

# Plausible CE hashes every API key with SECRET_KEY_BASE (Plausible.Auth.ApiKey.do_hash/1: sha256(secret_key_base <> key)):
# after the rotation all Stats API keys answer 401. Only the count and the names are read, never key_hash/key_prefix.
plausible_api_keys() {
  if names=$(echo 'SELECT name FROM api_keys ORDER BY name;' \
    | docker exec -i plausible_db psql -U postgres -d plausible -tA 2>/dev/null); then
    count=$(printf '%s' "$names" | grep -c . || :)
    names=$(printf '%s' "$names" | grep . | paste -sd, - | sed 's/,/, /g')
    info "Plausible API keys: $count${names:+ ($names)}. All of them will be invalid after the rotation (hashed with SECRET_KEY_BASE): recreate them and set them again at their consumers"
  else
    info "Plausible API keys: unknown (all of them will be invalid after the rotation: recreate them and set them again at their consumers)"
  fi
}

plausible_key_in_service() {
  key=$(env_value "$infra_env" PLAUSIBLE_SECRET_KEY_BASE)
  [ -n "$key" ] || key=$(env_value "$opt/config/$env_name/infra/.env" PLAUSIBLE_SECRET_KEY_BASE)
  printf '%s' "$key" > "$work/key"
}

plausible_test() {
  writable_etc
  container_running plausible || fail "the plausible container is not running"
  prerequisite check_publish_infra
  plausible_key_in_service
  [ -s "$work/key" ] || fail "no SECRET_KEY_BASE in service found (infra.env, config/$env_name/infra/.env)"
  if [ "$(cat "$work/key")" = "${INFRA_PLAUSIBLE_SECRET_KEY_BASE:-}" ]; then
    ok "the new SECRET_KEY_BASE is already in service"
  else
    ok "the SECRET_KEY_BASE in service is found; source would keep it for rollback"
  fi
  plausible_totp_users
  plausible_api_keys
}

plausible_source() {
  writable_etc
  plausible_key_in_service
  if [ "$(cat "$work/key")" = "$INFRA_PLAUSIBLE_SECRET_KEY_BASE" ]; then
    ok "the new SECRET_KEY_BASE is already in service"
    return 0
  fi
  [ -s "$work/key" ] && keep_previous INFRA_PLAUSIBLE_SECRET_KEY_BASE "$work/key"
  plausible_totp_users
  plausible_api_keys
}

plausible_restart() {
  [ "$(env_value "$infra_env" PLAUSIBLE_SECRET_KEY_BASE)" = "$INFRA_PLAUSIBLE_SECRET_KEY_BASE" ] \
    || fail "$infra_env does not hold the new SECRET_KEY_BASE: render it first"
  check_publish_infra
  INFRA_SERVICES=plausible sh "$opt/bin/publish-infra.sh" "$env_name"
  wait_http "$plausible_url/api/health" 200 "${PLAUSIBLE_ATTEMPTS:-60}"
}

plausible_rollback() {
  check_publish_infra
  previous=$(raw_value "$previous_env" INFRA_PLAUSIBLE_SECRET_KEY_BASE)
  [ -n "$previous" ] || fail "no previous SECRET_KEY_BASE is kept in $previous_env"
  set_infra_line PLAUSIBLE_SECRET_KEY_BASE "$previous"
  INFRA_SERVICES=plausible sh "$opt/bin/publish-infra.sh" "$env_name"
  wait_http "$plausible_url/api/health" 200 "${PLAUSIBLE_ATTEMPTS:-60}"
}

# --- Postgres and ClickHouse (Plausible, b2b-api) -----------------------------------------------------------------

# Default of the first `${KEY:-default}` (compose) or `${KEY:default}` (Spring) placeholder of a file; empty if none.
placeholder_default() {
  [ -f "$1" ] || return 0
  value=$(sed -n "s/.*\${$2:-\{0,1\}\([^}]*\)}.*/\1/p" "$1" | head -n 1)
  case "$value" in
    '?'*) ;;
    *) printf '%s' "$value" ;;
  esac
}

# Password of a scheme://user:password@host URL on stdin.
url_password() {
  sed -n 's#^[a-z]*://[^:/@]*:\([^@]*\)@.*#\1#p' | tr -d '\n'
}

# Environment variable $2 the container $1 was created with (empty when it is not set).
container_env() {
  docker exec "$1" printenv "$2" 2>/dev/null | tr -d '\n' || :
}

# A value no source can accept: authentication must refuse it, else it proves nothing.
random_value() {
  printf 'wrong-%s' "$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')"
}

# The first candidate file accepted by `$1 <file>`, copied to $work/current-$2 (never printed); prints its name.
find_accepted() {
  accepts=$1 label=$2
  shift 2
  for candidate in "$@"; do
    if "$accepts" "$candidate"; then
      cp "$candidate" "$work/current-$label"
      echo "${candidate#"$work/"}"
      return 0
    fi
  done
  return 1
}

# The Postgres of the official image trusts its socket and loopback connections: only a TCP connection to the
# container's network address (pg_hba `host all all all scram-sha-256`) checks the password. pg_target sets the
# container, user and database, and refuses a server that accepts any password.
pg_target() {
  pg_container=$1 pg_user=$2 pg_db=$3
  container_running "$pg_container" || fail "the $pg_container container is not running"
  case "$pg_user$pg_db" in
    '' | *[!A-Za-z0-9_]*) fail "unexpected Postgres user or database name in $pg_container" ;;
  esac
  pg_host=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}' "$pg_container" 2>/dev/null \
    | awk '{ print $1 }')
  [ -n "$pg_host" ] || fail "no network address found for $pg_container"
  random_value > "$work/wrong"
  if pg_accepts "$work/wrong"; then
    fail "$pg_container accepts any password over TCP (trust): the value in service cannot be found (runbook)"
  fi
}

# psql as $pg_user on $pg_db over TCP, password in file $1 (environment of docker exec, never a command line); SQL
# on stdin.
pg_as() {
  PGPASSWORD=$(cat "$1") docker exec -i -e PGPASSWORD "$pg_container" \
    psql -h "$pg_host" -U "$pg_user" -d "$pg_db" -w -X -q -t -A -v ON_ERROR_STOP=1 2>/dev/null
}

pg_accepts() {
  [ -s "$1" ] || return 1
  [ "$(echo 'SELECT 1;' | pg_as "$1" || true)" = 1 ]
}

# Sets the password of $pg_user to the content of file $2, authenticated with the content of file $1.
pg_set_password() {
  # SQL string literal: quotes doubled (standard_conforming_strings: backslashes are literal).
  value=$(sed -e "s/'/''/g" "$2")
  printf 'ALTER USER "%s" WITH PASSWORD '\''%s'\'';\n' "$pg_user" "$value" | pg_as "$1" > /dev/null \
    || fail "Postgres refused to change the password of $pg_user ($pg_container)"
  pg_accepts "$2" || fail "the new password of $pg_user is not accepted after the change ($pg_container)"
  ok "password of the Postgres user $pg_user changed ($pg_container)"
}

# clickhouse-client in plausible_events_db as user `default`, password in file $1, one SQL statement on stdin; its
# output to stdout. A client config file holding the password is streamed first, as one line (mode 600 in the
# container, removed), never a command line; the CLICKHOUSE_* variables of the container are unset so that the client
# only uses that file. stderr goes to $work/ch.err, never printed: an error may quote the statement (ch_error).
ch_query() {
  [ -s "$1" ] || return 1
  {
    printf '<config><user>default</user><password><![CDATA['
    sed 's/]]>/]]]]><![CDATA[>/g' "$1" | tr -d '\n'
    printf ']]></password></config>\n'
    cat
  } | docker exec -i plausible_events_db sh -c '
    set -eu
    umask 077
    f=$(mktemp)
    trap "rm -f \"$f\"" EXIT
    IFS= read -r line
    printf "%s\n" "$line" > "$f"
    env -u CLICKHOUSE_USER -u CLICKHOUSE_PASSWORD clickhouse-client --config-file "$f"' 2> "$work/ch.err"
}

# Code of the ClickHouse error of the last ch_query (516 for `Code: 516`), never its message.
ch_error() {
  ch_code=$(sed -n 's/.*Code: \([0-9]*\).*/\1/p' "$work/ch.err" 2>/dev/null | head -n 1)
  printf '%s' "${ch_code:-unknown}"
}

ch_accepts() {
  [ "$(echo 'SELECT 1' | ch_query "$1" || true)" = 1 ]
}

# Waits until ClickHouse accepts the password in file $1 (after its recreation).
wait_ch_accepts() {
  i=0
  until ch_accepts "$1"; do
    i=$((i + 1))
    [ "$i" -lt "${CLICKHOUSE_ATTEMPTS:-60}" ] || fail "ClickHouse does not accept the expected password after its restart"
    sleep "${WAIT_SECONDS:-5}"
  done
  ok "ClickHouse accepts the expected password"
}

# The new value of the target's secret ($1) to $work/new (empty when not set: test without the new value).
new_value() {
  eval "printf '%s' \"\${$1:-}\"" > "$work/new"
}

plausible_postgres_prerequisites() {
  container_running plausible || fail "the plausible container is not running"
  check_publish_infra
  check_placeholders "$opt/latest/$env_name/docker-compose.infra.yml" PLAUSIBLE_PG_PASSWORD
}

plausible_pg_candidates() {
  env_value "$infra_env" PLAUSIBLE_PG_PASSWORD > "$work/candidate-infra-env"
  env_value "$opt/config/$env_name/infra/.env" PLAUSIBLE_PG_PASSWORD > "$work/candidate-config-env"
  placeholder_default "$opt/bin/docker-compose.infra.yml" PLAUSIBLE_PG_PASSWORD > "$work/candidate-compose"
  container_env plausible DATABASE_URL | url_password > "$work/candidate-plausible-container"
  container_env plausible_db POSTGRES_PASSWORD > "$work/candidate-db-container"
  raw_value "$previous_env" INFRA_PLAUSIBLE_PG_PASSWORD > "$work/candidate-previous"
  echo "$work/candidate-infra-env $work/candidate-config-env $work/candidate-compose" \
    "$work/candidate-plausible-container $work/candidate-db-container $work/candidate-previous"
}

# The password of postgres in service to $work/current-pg (the new one first); prints where it was found.
working_plausible_pg() {
  # shellcheck disable=SC2046 # file names without spaces, split on purpose.
  find_accepted pg_accepts pg "$work/new" $(plausible_pg_candidates) \
    || fail "no known password of the Postgres user postgres of plausible_db is accepted (infra.env, config/$env_name/infra/.env, compose, containers, previous)"
}

plausible_postgres_test() {
  writable_etc
  prerequisite plausible_postgres_prerequisites
  new_value INFRA_PLAUSIBLE_PG_PASSWORD
  pg_target plausible_db postgres plausible
  found=$(working_plausible_pg)
  if [ "$found" = new ]; then
    ok "plausible_db already accepts the new password of postgres: source would skip the change"
  else
    ok "the password of postgres in service is found ($found) and accepted; source would change it"
  fi
}

plausible_postgres_source() {
  writable_etc
  plausible_postgres_prerequisites
  new_value INFRA_PLAUSIBLE_PG_PASSWORD
  pg_target plausible_db postgres plausible
  found=$(working_plausible_pg)
  if [ "$found" = new ]; then
    ok "plausible_db already accepts the new password of postgres: nothing to change"
    return 0
  fi
  keep_previous INFRA_PLAUSIBLE_PG_PASSWORD "$work/current-pg"
  pg_set_password "$work/current-pg" "$work/new"
}

plausible_postgres_restart() {
  new_value INFRA_PLAUSIBLE_PG_PASSWORD
  pg_target plausible_db postgres plausible
  pg_accepts "$work/new" || fail "plausible_db does not accept the new password of postgres: run source first"
  [ "$(env_value "$infra_env" PLAUSIBLE_PG_PASSWORD)" = "$(cat "$work/new")" ] \
    || fail "$infra_env does not hold the new PLAUSIBLE_PG_PASSWORD: render it first"
  check_publish_infra
  # Plausible only: plausible_db keeps running (POSTGRES_PASSWORD is only read when its data directory is created).
  INFRA_SERVICES=plausible sh "$opt/bin/publish-infra.sh" "$env_name"
  wait_http "$plausible_url/api/health" 200 "${PLAUSIBLE_ATTEMPTS:-60}"
}

plausible_postgres_rollback() {
  check_publish_infra
  previous=$(raw_value "$previous_env" INFRA_PLAUSIBLE_PG_PASSWORD)
  [ -n "$previous" ] || fail "no previous password of postgres (plausible_db) is kept in $previous_env"
  printf '%s' "$previous" > "$work/previous"
  new_value INFRA_PLAUSIBLE_PG_PASSWORD
  pg_target plausible_db postgres plausible
  if pg_accepts "$work/previous"; then
    ok "plausible_db already accepts the previous password of postgres"
  else
    working_plausible_pg > /dev/null
    pg_set_password "$work/current-pg" "$work/previous"
  fi
  set_infra_line PLAUSIBLE_PG_PASSWORD "$previous"
  INFRA_SERVICES=plausible sh "$opt/bin/publish-infra.sh" "$env_name"
  wait_http "$plausible_url/api/health" 200 "${PLAUSIBLE_ATTEMPTS:-60}"
}

plausible_clickhouse_prerequisites() {
  container_running plausible_events_db || fail "the plausible_events_db container is not running"
  container_running plausible || fail "the plausible container is not running"
  check_publish_infra
  check_placeholders "$opt/latest/$env_name/docker-compose.infra.yml" CLICKHOUSE_PASSWORD
}

# The ClickHouse password is not stored in ClickHouse: the entrypoint of the image writes it from CLICKHOUSE_PASSWORD
# to users.d/default-user.xml at every start. The value in service is the one the running container was created with.
plausible_ch_candidates() {
  env_value "$infra_env" CLICKHOUSE_PASSWORD > "$work/candidate-infra-env"
  env_value "$opt/config/$env_name/infra/.env" CLICKHOUSE_PASSWORD > "$work/candidate-config-env"
  placeholder_default "$opt/bin/docker-compose.infra.yml" CLICKHOUSE_PASSWORD > "$work/candidate-compose"
  container_env plausible CLICKHOUSE_DATABASE_URL | url_password > "$work/candidate-plausible-container"
  container_env plausible_events_db CLICKHOUSE_PASSWORD > "$work/candidate-db-container"
  raw_value "$previous_env" INFRA_CLICKHOUSE_PASSWORD > "$work/candidate-previous"
  echo "$work/candidate-infra-env $work/candidate-config-env $work/candidate-compose" \
    "$work/candidate-plausible-container $work/candidate-db-container $work/candidate-previous"
}

working_plausible_ch() {
  random_value > "$work/wrong"
  if ch_accepts "$work/wrong"; then
    fail "plausible_events_db accepts any password: the value in service cannot be found (runbook)"
  fi
  # shellcheck disable=SC2046 # file names without spaces, split on purpose.
  find_accepted ch_accepts ch "$work/new" $(plausible_ch_candidates) \
    || fail "no known password of the ClickHouse user default is accepted (infra.env, config/$env_name/infra/.env, compose, containers, previous)"
}

# Plausible's migrations create dictionaries whose source is this ClickHouse, with the user and password of the
# moment in their DDL (SOURCE(CLICKHOUSE(TABLE ... DB ... USER 'default' PASSWORD '...'))): insertions into events_v2
# read them, so they fail with Code: 516 once the password changes, until the dictionaries are recreated (GOU-1271).
ch_rollback_hint="run infraSecrets.yml mode=rollback for plausible-clickhouse (runbook)"
ch_rollback_failed_hint="the rollback did not restore Plausible's writes: see the runbook (plausible-clickhouse) before any other run"

# Database of Plausible in ClickHouse: the path of the CLICKHOUSE_DATABASE_URL the plausible container was created
# with (docker inspect: also when it is stopped), to $ch_db. The URL holds the password: never printed.
plausible_ch_database() {
  ch_db=$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' plausible 2>/dev/null \
    | sed -n 's#^CLICKHOUSE_DATABASE_URL=##p' | head -n 1 \
    | sed -n -e 's#^[a-z]*://##' -e 's#.*@##' -e 's#^[^/]*/\([^/?]*\).*#\1#p')
  case "$ch_db" in
    '' | *[!A-Za-z0-9_]*) fail "no database name found in the CLICKHOUSE_DATABASE_URL of the plausible container" ;;
  esac
}

# A dictionary DDL (SHOW CREATE DICTIONARY) on stdin.
#   classify        prints `concerned` when its source is this ClickHouse (no HOST, or a local one) as user default
#                   (or no USER) with one PASSWORD literal; else why not: source=<TYPE>, no-password, passwords=<n>,
#                   user=<user>, remote-host.
#   rewrite <file>  prints it as CREATE OR REPLACE DICTIONARY, the PASSWORD literal (ClickHouse shows '[HIDDEN]')
#                   replaced by the content of <file>, SQL-escaped; nothing else changes. Fails on an unexpected DDL.
dict_ddl() {
  awk -v mode="$1" -v file="${2:-}" -v q="'" '
    BEGIN {
      lit = q "([^" q "\\\\]|\\\\.)*" q
      if (mode == "rewrite") {
        getline value < file
        escaped = ""
        for (k = 1; k <= length(value); k++) {
          c = substr(value, k, 1)
          escaped = escaped ((c == "\\" || c == q) ? "\\" : "") c
        }
      }
    }
    # Position of the string literal that follows the keyword key in the upper-cased line u: RSTART, RLENGTH.
    function literal(u, key,   start, len, s) {
      if (!match(u, "(^|[^A-Z0-9_])" key "[ \t]+" lit)) return 0
      start = RSTART; len = RLENGTH; s = substr(u, start, len)
      RSTART = start + index(s, q) - 1
      RLENGTH = len - index(s, q) + 1
      return 1
    }
    {
      u = toupper($0)
      if (NR == 1) first = u
      if (source == "" && match(u, /SOURCE[ \t]*\([ \t]*[A-Z_]+/)) {
        source = substr(u, RSTART, RLENGTH); sub(/^SOURCE[ \t]*\([ \t]*/, "", source)
      }
      if (literal(u, "USER")) user = substr($0, RSTART + 1, RLENGTH - 2)
      if (literal(u, "HOST")) host = substr($0, RSTART + 1, RLENGTH - 2)
      line = $0; out = ""
      while (literal(toupper(line), "PASSWORD")) {
        passwords++
        out = out substr(line, 1, RSTART - 1) q escaped q
        line = substr(line, RSTART + RLENGTH)
      }
      out = out line
      if (NR == 1 && mode == "rewrite") {
        if (u !~ /^CREATE DICTIONARY /) exit 2
        out = "CREATE OR REPLACE DICTIONARY " substr(out, 19)
      }
      if (mode == "rewrite") print out
    }
    END {
      if (mode == "rewrite") exit (first ~ /^CREATE DICTIONARY / && source == "CLICKHOUSE" && passwords == 1) ? 0 : 2
      if (source != "CLICKHOUSE") print "source=" (source == "" ? "unknown" : source)
      else if (passwords == 0) print "no-password"
      else if (passwords > 1) print "passwords=" passwords
      else if (user != "" && user != "default") print "user=" user
      else if (host != "" && host != "localhost" && host != "127.0.0.1" && host != "::1") print "remote-host"
      else print "concerned"
    }'
}

ch_list_dictionaries() {
  printf "SELECT name, status FROM system.dictionaries WHERE database = '%s' ORDER BY name FORMAT TSV\n" "$ch_db" \
    | ch_query "$1" > "$2" || fail "cannot list the dictionaries of $ch_db (ClickHouse error $(ch_error))${3:+: $3}"
}

# The dictionaries of $ch_db, read with the password in file $1: those this target recreates in
# $work/dicts-concerned, the others in $work/dicts-other (`<name> <status> [<why>]` lines), the DDL of each in
# $work/ddl-<name> (password hidden by ClickHouse). Never printed.
ch_dictionaries() {
  ch_list_dictionaries "$1" "$work/dicts" "${2:-}"
  : > "$work/dicts-concerned"
  : > "$work/dicts-other"
  while read -r dict_name dict_status; do
    case "$dict_name" in
      '' | *[!A-Za-z0-9_]*) fail "unexpected dictionary name in $ch_db" ;;
    esac
    printf 'SHOW CREATE DICTIONARY %s.%s FORMAT TSVRaw\n' "$ch_db" "$dict_name" | ch_query "$1" > "$work/ddl-$dict_name" \
      || fail "cannot read the definition of the dictionary $dict_name (ClickHouse error $(ch_error))${2:+: $2}"
    why=$(dict_ddl classify < "$work/ddl-$dict_name")
    if [ "$why" = concerned ]; then
      echo "$dict_name $dict_status" >> "$work/dicts-concerned"
    else
      echo "$dict_name $dict_status $why" >> "$work/dicts-other"
    fi
  done < "$work/dicts"
}

# test: the dictionaries restart would recreate, by name and status (no value, no DDL).
ch_report_dictionaries() {
  ok "$(wc -l < "$work/dicts-concerned" | tr -d ' ') dictionary(ies) of $ch_db read this ClickHouse as default with a password"
  while read -r dict_name dict_status; do
    case "$dict_status" in
      FAILED*) info "dictionary $dict_name: $dict_status, already failing; restart recreates it with the new password and reloads it" ;;
      *) ok "dictionary $dict_name: $dict_status; restart would recreate it with the new password, reload it and check it is LOADED" ;;
    esac
  done < "$work/dicts-concerned"
  while read -r dict_name dict_status why; do
    info "dictionary $dict_name: $dict_status, not changed by this target ($why)"
  done < "$work/dicts-other"
}

# Recreates each concerned dictionary of Plausible's database with the password in file $1 (CREATE OR REPLACE from its
# SHOW CREATE, the DDL streamed on stdin), reloads it, then checks they are all LOADED; $2: what to do on failure.
# Idempotent: a dictionary already holding that password is recreated identical.
ch_refresh_dictionaries() {
  plausible_ch_database
  ch_dictionaries "$1" "$2"
  [ -s "$work/dicts-concerned" ] || info "no dictionary of $ch_db reads this ClickHouse with a password: none to recreate"
  while read -r dict_name _; do
    dict_ddl rewrite "$1" < "$work/ddl-$dict_name" > "$work/create.sql" \
      || fail "the definition of the dictionary $dict_name has an unexpected form: $2"
    ch_query "$1" < "$work/create.sql" > /dev/null \
      || fail "recreating the dictionary $dict_name failed (ClickHouse error $(ch_error)): $2"
    rm -f "$work/create.sql"
    # A failed reload shows in its status, checked below.
    printf 'SYSTEM RELOAD DICTIONARY %s.%s\n' "$ch_db" "$dict_name" | ch_query "$1" > /dev/null || :
  done < "$work/dicts-concerned"
  ch_list_dictionaries "$1" "$work/dicts-after" "$2"
  bad=false
  while read -r dict_name _; do
    after=$(awk -v name="$dict_name" '$1 == name { print $2 }' "$work/dicts-after")
    case "$after" in
      LOADED | LOADED_AND_RELOADING) ok "dictionary $dict_name recreated with the expected password and reloaded: $after" ;;
      *) echo "ERROR: dictionary $dict_name is ${after:-missing} after its reload" >&2; bad=true ;;
    esac
  done < "$work/dicts-concerned"
  [ "$bad" = false ] || fail "dictionaries of $ch_db not LOADED (Plausible's insertions read them): $2"
}

# Proof that Plausible writes, once /api/health answers: no `Code: 516` (ClickHouse authentication failed: its write
# buffers and the dictionaries their insertions read) in `docker logs plausible --since $1`, checked
# PLAUSIBLE_WRITE_ATTEMPTS times WAIT_SECONDS apart (45 s by default); then the count of events written since $1, for
# information (beta may have no traffic). $2: password file, $3: what to do on failure.
plausible_writes() {
  i=0
  while :; do
    docker logs --since "$1" plausible > "$work/plausible.log" 2>&1 || fail "cannot read the logs of plausible: $3"
    auth_failures=$(grep -c 'Code: 516' "$work/plausible.log" || :)
    rm -f "$work/plausible.log"
    [ "$auth_failures" -eq 0 ] \
      || fail "plausible logged $auth_failures ClickHouse authentication failure(s) (Code: 516) since $1, it does not write: $3"
    i=$((i + 1))
    [ "$i" -lt "${PLAUSIBLE_WRITE_ATTEMPTS:-9}" ] || break
    sleep "${WAIT_SECONDS:-5}"
  done
  ok "no ClickHouse authentication failure (Code: 516) in the logs of plausible since $1 ($i checks)"
  events=$(printf "SELECT count() FROM %s.events_v2 WHERE timestamp >= parseDateTimeBestEffort('%s') FORMAT TSV\n" \
    "$ch_db" "$1" | ch_query "$2" || echo unknown)
  info "events written to $ch_db.events_v2 since $1: ${events:-unknown} (0 without traffic)"
}

plausible_clickhouse_test() {
  writable_etc
  prerequisite plausible_clickhouse_prerequisites
  new_value INFRA_CLICKHOUSE_PASSWORD
  found=$(working_plausible_ch)
  if [ "$found" = new ]; then
    ok "ClickHouse already accepts the new password of default"
  else
    ok "the password of default in service is found ($found) and accepted; source would keep it for rollback," \
      "restart would recreate ClickHouse with the new one, its dictionaries, then Plausible"
  fi
  plausible_ch_database
  ch_dictionaries "$work/current-ch"
  ch_report_dictionaries
}

plausible_clickhouse_source() {
  writable_etc
  plausible_clickhouse_prerequisites
  new_value INFRA_CLICKHOUSE_PASSWORD
  found=$(working_plausible_ch)
  if [ "$found" = new ]; then
    ok "ClickHouse already accepts the new password of default: nothing to keep"
    return 0
  fi
  keep_previous INFRA_CLICKHOUSE_PASSWORD "$work/current-ch"
  info "ClickHouse takes the new password when restart recreates it (written from CLICKHOUSE_PASSWORD at its start)"
}

plausible_clickhouse_restart() {
  new_value INFRA_CLICKHOUSE_PASSWORD
  [ "$(env_value "$infra_env" CLICKHOUSE_PASSWORD)" = "$(cat "$work/new")" ] \
    || fail "$infra_env does not hold the new CLICKHOUSE_PASSWORD: render it first"
  check_publish_infra
  if ch_accepts "$work/new"; then
    ok "ClickHouse already accepts the new password of default"
  else
    [ -n "$(raw_value "$previous_env" INFRA_CLICKHOUSE_PASSWORD)" ] \
      || fail "the password of default in service is not kept in $previous_env: run source first"
    # ClickHouse first (its new password), then Plausible: events sent in between are lost.
    INFRA_SERVICES=plausible_events_db sh "$opt/bin/publish-infra.sh" "$env_name"
    wait_ch_accepts "$work/new"
  fi
  # Before Plausible: its insertions read the dictionaries.
  ch_refresh_dictionaries "$work/new" "$ch_rollback_hint"
  since=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  INFRA_SERVICES=plausible sh "$opt/bin/publish-infra.sh" "$env_name"
  wait_http "$plausible_url/api/health" 200 "${PLAUSIBLE_ATTEMPTS:-60}" "$ch_rollback_hint"
  plausible_writes "$since" "$work/new" "$ch_rollback_hint"
}

plausible_clickhouse_rollback() {
  check_publish_infra
  previous=$(raw_value "$previous_env" INFRA_CLICKHOUSE_PASSWORD)
  [ -n "$previous" ] || fail "no previous password of default (ClickHouse) is kept in $previous_env"
  printf '%s' "$previous" > "$work/previous"
  set_infra_line CLICKHOUSE_PASSWORD "$previous"
  INFRA_SERVICES=plausible_events_db sh "$opt/bin/publish-infra.sh" "$env_name"
  wait_ch_accepts "$work/previous"
  ch_refresh_dictionaries "$work/previous" "$ch_rollback_failed_hint"
  since=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  INFRA_SERVICES=plausible sh "$opt/bin/publish-infra.sh" "$env_name"
  wait_http "$plausible_url/api/health" 200 "${PLAUSIBLE_ATTEMPTS:-60}" "$ch_rollback_failed_hint"
  plausible_writes "$since" "$work/previous" "$ch_rollback_failed_hint"
}

# Files publish-jars.sh sources for b2b-api, in that order (load_service_env).
b2b_env_files() {
  echo "$opt/config/$env_name/infra/.env $etc/b2b-api.env $opt/config/$env_name/b2b-api/.env"
}

b2b_postgres_prerequisites() {
  runtime=$opt/runtime/b2b-api.jar
  latest=$opt/latest/b2b-api-latest.jar
  [ -f "$runtime" ] && [ -f "$latest" ] || fail "$runtime or $latest is missing"
  cmp -s "$runtime" "$latest" || fail "$runtime differs from $latest: a restart would deploy a JAR that is not running"
  [ -f "$opt/bin/publish-jars.sh" ] || fail "$opt/bin/publish-jars.sh is missing"
  # The OS environment beats the config tree: an env line for the property would hide the rendered password.
  for file in $(b2b_env_files); do
    [ -z "$(raw_value "$file" SPRING_DATASOURCE_PASSWORD)" ] \
      || fail "$file sets SPRING_DATASOURCE_PASSWORD, which beats the config tree: remove it first (runbook)"
  done
  ok "b2b-api runs its latest JAR and nothing overrides spring.datasource.password"
}

# User and database of b2b_postgres: those the container was created with (not secret).
b2b_pg_target() {
  pg_target b2b_postgres "$(container_env b2b_postgres POSTGRES_USER)" "$(container_env b2b_postgres POSTGRES_DB)"
}

b2b_pg_candidates() {
  tree=$etc/secrets/b2b-api/spring.datasource.password
  : > "$work/candidate-tree"
  [ -f "$tree" ] && cp "$tree" "$work/candidate-tree"
  env_value "$infra_env" B2B_POSTGRES_PASSWORD > "$work/candidate-infra-env"
  i=0
  for file in $(b2b_env_files); do
    i=$((i + 1))
    env_value "$file" B2B_POSTGRES_PASSWORD > "$work/candidate-env-file-$i"
  done
  placeholder_default "$opt/config/$env_name/b2b-api/application-active.yml" B2B_POSTGRES_PASSWORD > "$work/candidate-yml"
  placeholder_default "$opt/bin/docker-compose.infra.yml" B2B_POSTGRES_PASSWORD > "$work/candidate-compose"
  container_env b2b_postgres POSTGRES_PASSWORD > "$work/candidate-db-container"
  raw_value "$previous_env" B2B_API_POSTGRES_PASSWORD > "$work/candidate-previous"
  echo "$work/candidate-tree $work/candidate-infra-env $work/candidate-env-file-1 $work/candidate-env-file-2" \
    "$work/candidate-env-file-3 $work/candidate-yml $work/candidate-compose $work/candidate-db-container" \
    "$work/candidate-previous"
}

working_b2b_pg() {
  # shellcheck disable=SC2046 # file names without spaces, split on purpose.
  find_accepted pg_accepts pg "$work/new" $(b2b_pg_candidates) \
    || fail "no known password of the Postgres user of b2b_postgres is accepted (config tree, env files, application-active.yml, compose, container, previous)"
}

b2b_postgres_test() {
  writable_etc
  prerequisite b2b_postgres_prerequisites
  new_value B2B_API_POSTGRES_PASSWORD
  b2b_pg_target
  found=$(working_b2b_pg)
  if [ "$found" = new ]; then
    ok "b2b_postgres already accepts the new password: source would skip the change"
  else
    ok "the password of $pg_user in service is found ($found) and accepted; source would change it"
  fi
}

b2b_postgres_source() {
  writable_etc
  b2b_postgres_prerequisites
  new_value B2B_API_POSTGRES_PASSWORD
  b2b_pg_target
  found=$(working_b2b_pg)
  if [ "$found" = new ]; then
    ok "b2b_postgres already accepts the new password of $pg_user: nothing to change"
    return 0
  fi
  keep_previous B2B_API_POSTGRES_PASSWORD "$work/current-pg"
  pg_set_password "$work/current-pg" "$work/new"
}

b2b_postgres_restart() {
  new_value B2B_API_POSTGRES_PASSWORD
  b2b_pg_target
  pg_accepts "$work/new" || fail "b2b_postgres does not accept the new password: run source first"
  holds "$etc/secrets/b2b-api/spring.datasource.password" "$(cat "$work/new")" \
    || fail "the config tree of b2b-api does not hold the new password: render it first"
  [ "$(env_value "$infra_env" B2B_POSTGRES_PASSWORD)" = "$(cat "$work/new")" ] \
    || fail "$infra_env does not hold the new B2B_POSTGRES_PASSWORD: render it first"
  # b2b-api only: b2b_postgres keeps running (POSTGRES_PASSWORD is only read when its data directory is created).
  echo "----- Restarting b2b-api -----"
  sh "$opt/bin/publish-jars.sh" "$env_name" restart b2b-api
}

b2b_postgres_rollback() {
  previous=$(raw_value "$previous_env" B2B_API_POSTGRES_PASSWORD)
  [ -n "$previous" ] || fail "no previous password of b2b_postgres is kept in $previous_env"
  printf '%s' "$previous" > "$work/previous"
  new_value B2B_API_POSTGRES_PASSWORD
  b2b_pg_target
  if pg_accepts "$work/previous"; then
    ok "b2b_postgres already accepts the previous password"
  else
    working_b2b_pg > /dev/null
    pg_set_password "$work/current-pg" "$work/previous"
  fi
  tree=$etc/secrets/b2b-api/spring.datasource.password
  if [ -f "$tree" ]; then
    write_exact "$tree" "$previous"
    ok "$tree holds the previous password"
  fi
  set_infra_line B2B_POSTGRES_PASSWORD "$previous"
  echo "----- Restarting b2b-api -----"
  sh "$opt/bin/publish-jars.sh" "$env_name" restart b2b-api
}

echo "----- $target: $action ($env_name) -----"
# Function names cannot hold a dash: plausible-postgres -> plausible_postgres_<action>.
"$(echo "$target" | tr - _)_${action}"
[ "$blocked" = false ] || fail "$target: prerequisites missing (see the errors above); nothing was written"
echo "----- $target: $action done -----"
