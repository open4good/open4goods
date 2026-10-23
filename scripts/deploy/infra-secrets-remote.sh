#!/bin/sh
# Runs ON THE DEPLOY HOST, sent over SSH by infra-secrets.sh, which prepends the new values as NAME='value' lines
# (GOU-1092, campaign GOU-1038 wave 3). Never run it by hand with values on a command line.
#
# Usage: sh <payload> <test|source|restart|rollback> <beta|prod> <elasticsearch|kibana|mysql|plausible>
#
#   test      Read only: prerequisites, the credential currently in service (found by authenticating, never
#             printed), whether the new value is already in service, what apply would do. Runs without the new
#             values too (dry run before Access writes them).
#   source    Changes the password at its source, authenticated with the value currently in service on this host:
#             Elasticsearch users `elastic` / `kibana_system` (_security/user/<user>/_password), MySQL users `xwiki`
#             and `root` (ALTER USER) plus XWiki's persisted hibernate.cfg.xml. Before any change, the value in
#             service is kept in /etc/open4goods/infra-previous.env (mode 600) for `rollback`. Idempotent: a value
#             already accepted by the source is not changed again.
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
  plausible) [ "$env_name" = beta ] || fail "plausible: only beta runs Plausible" ;;
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

# Waits until $1 answers one of the HTTP codes $2 (comma-separated), $3 attempts 5 s apart.
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
  fail "$1 did not answer HTTP $2 in time (last: $code)"
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
  code=$(curl -s -o "$work/rotate.json" -w '%{http_code}' -K "$work/curl.cfg" -X POST -H 'kbn-xsrf: true' \
    "$kibana_url/api/encrypted_saved_objects/_rotate_key?conflicts=abort" || echo 000)
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

echo "----- $target: $action ($env_name) -----"
"${target}_${action}"
[ "$blocked" = false ] || fail "$target: prerequisites missing (see the errors above); nothing was written"
echo "----- $target: $action done -----"
