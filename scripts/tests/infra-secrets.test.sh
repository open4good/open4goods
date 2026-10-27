#!/usr/bin/env bash
# Tests scripts/deploy/infra-secrets.sh and infra-secrets-remote.sh (GOU-1092, GOU-1256) against a fake `ssh` that
# runs the remote script locally, a fake `curl` playing Elasticsearch and Kibana, and a fake `docker` playing MySQL,
# XWiki, Plausible, its Postgres and ClickHouse, and the b2b Postgres. /etc/open4goods/ and /opt/open4goods/ live in a temp dir. Every value is a sentinel that must never
# appear in an output, a summary or a command line.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY="${ROOT}/scripts/deploy"
WORK="$(mktemp -d)"
trap 'rm -rf "${WORK}"' EXIT

FAKE_ROOT="${WORK}/host"
FAKE_ES="${WORK}/es"
FAKE_MYSQL="${WORK}/mysql"
FAKE_XWIKI_DATA="${WORK}/xwiki-data"
FAKE_DB="${WORK}/db"
FAKE_ENV="${WORK}/container-env"
CALLS="${WORK}/calls.log"
mkdir -p "${WORK}/bin" "${FAKE_ROOT}" "${FAKE_ES}" "${FAKE_MYSQL}" "${FAKE_XWIKI_DATA}" "${FAKE_DB}" "${FAKE_ENV}"

cat > "${WORK}/bin/ssh" <<'SH'
#!/bin/sh
# Fake ssh: record the arguments, then run the last one (the remote script) locally, host paths mapped.
printf 'ssh %s\n' "$*" >> "$CALLS"
for arg; do last=$arg; done
script=$(printf '%s' "$last" | sed -e "s#/etc/open4goods/#$FAKE_ROOT/etc/open4goods/#g" \
  -e "s#/opt/open4goods/#$FAKE_ROOT/opt/open4goods/#g")
exec sh -c "$script"
SH

cat > "${WORK}/bin/curl" <<'SH'
#!/bin/sh
# Fake curl: Elasticsearch users are files of $FAKE_ES (name = user, content = password); Kibana always answers.
printf 'curl %s\n' "$*" >> "$CALLS"
out= method=GET cfg= data= url= write=
while [ "$#" -gt 0 ]; do
  case "$1" in
    -s) ;;
    -o) out=$2; shift ;;
    -w) write=$2; shift ;;
    -K) cfg=$2; shift ;;
    -X) method=$2; shift ;;
    -H) shift ;;
    --data-binary) data=${2#@}; shift ;;
    *) url=$1 ;;
  esac
  shift
done
user=
[ -n "$cfg" ] && user=$(sed -n 's/^user = "\(.*\)"$/\1/p' "$cfg")
u=${user%%:*}
p=${user#*:}
path=/${url#*://*/}
auth_ok() { [ -n "$u" ] && [ -f "$FAKE_ES/$u" ] && [ "$(cat "$FAKE_ES/$u")" = "$p" ]; }
code=401 body=
case "$method $path" in
  "GET /_security/_authenticate") auth_ok && code=200 ;;
  "POST /_security/user/"*"/_password")
    if auth_ok && [ "$u" = elastic ]; then
      target=${path#/_security/user/}
      target=${target%/_password}
      sed -n 's/^{"password":"\(.*\)"}$/\1/p' "$data" | tr -d '\n' > "$FAKE_ES/$target"
      code=200
    fi
    ;;
  "GET /api/status") auth_ok && code=200 && body='{"status":{"overall":{"level":"available"}}}' ;;
  # Kibana 9 answers 400 to an unknown query parameter (e.g. conflicts).
  "POST /api/encrypted_saved_objects/_rotate_key") auth_ok && code=200 && body='{"total":2,"successful":2,"failed":0}' ;;
  "POST /api/encrypted_saved_objects/_rotate_key?"*) code=400 ;;
  "GET /"*) code=200 ;;
esac
if [ -n "$out" ]; then printf '%s' "$body" > "$out"; else printf '%s' "$body"; fi
[ -z "$write" ] || printf '%s' "$code"
SH

cat > "${WORK}/bin/docker" <<'SH'
#!/bin/sh
# Fake docker: MySQL users are files of $FAKE_MYSQL; XWiki's permanent directory is $FAKE_XWIKI_DATA; the Postgres
# and ClickHouse passwords are files of $FAKE_DB (<container>.<user>; $FAKE_DB/trust: any password accepted); the
# environment a container was created with is $FAKE_ENV/<container>.<NAME>.
printf 'docker %s\n' "$*" >> "$CALLS"
case "$1" in
  inspect)
    case "$3" in
      *IPAddress*) echo '172.18.0.9 ' ;;
      *) echo true ;;
    esac
    exit 0
    ;;
  exec) shift ;;
  *) exit 1 ;;
esac
while :; do
  case "$1" in
    -i) shift ;;
    -e) shift 2 ;;
    *) break ;;
  esac
done
container=$1
shift
if [ "$1" = printenv ]; then
  [ -f "$FAKE_ENV/$container.$2" ] || exit 1
  cat "$FAKE_ENV/$container.$2"
  echo
  exit 0
fi
# Postgres over TCP (psql -h <address> -U <user> -d <db> ...): SELECT 1 and ALTER USER.
if [ "$1" = psql ] && [ "$2" = -h ]; then
  user=$5
  [ -f "$FAKE_DB/trust" ] || { [ -f "$FAKE_DB/$container.$user" ] && [ "$(cat "$FAKE_DB/$container.$user")" = "$PGPASSWORD" ]; } \
    || exit 2
  while IFS= read -r sql; do
    case "$sql" in
      'SELECT 1;') echo 1 ;;
      'ALTER USER "'*)
        target=$(printf '%s' "$sql" | sed -n 's/^ALTER USER "\([A-Za-z0-9_]*\)".*/\1/p')
        printf '%s' "$sql" | sed -n "s/.* WITH PASSWORD '\(.*\)';$/\1/p" | sed "s/''/'/g" | tr -d '\n' \
          > "$FAKE_DB/$container.$target"
        ;;
    esac
  done
  exit 0
fi
case "$container" in
  mysql)
    [ "$1" = mysql ] || exit 1
    user=${2#-u}
    [ -f "$FAKE_MYSQL/$user" ] && [ "$(cat "$FAKE_MYSQL/$user")" = "$MYSQL_PWD" ] || exit 1
    while IFS= read -r sql; do
      case "$sql" in
        'SELECT 1;') echo 1 ;;
        "SELECT host FROM mysql.user WHERE user = '"*)
          echo '%'
          if [ "${sql#*\'}" = "root';" ]; then echo localhost; fi
          ;;
        "ALTER USER '"*)
          target=$(printf '%s' "$sql" | sed -n "s/^ALTER USER '\([a-z]*\)'.*/\1/p")
          printf '%s' "$sql" | sed -n "s/.* IDENTIFIED BY '\(.*\)';$/\1/p" | tr -d '\n' > "$FAKE_MYSQL/$target"
          ;;
      esac
    done
    ;;
  xwiki-mysql-tomcat-web)
    if [ "$1" = printenv ]; then
      exit 1
    elif [ "$1" = test ]; then
      [ -f "$FAKE_XWIKI_DATA/hibernate.cfg.xml" ]
    else
      script=$(printf '%s' "$3" | sed "s#/usr/local/xwiki/data#$FAKE_XWIKI_DATA#g")
      sh -c "$script"
    fi
    ;;
  plausible_db) echo 0 ;;
  plausible_events_db)
    # ClickHouse client config on stdin: <password><![CDATA[...]]></password>.
    given=$(sed -n 's/.*<password><!\[CDATA\[\(.*\)\]\]><\/password>.*/\1/p')
    [ -f "$FAKE_DB/trust" ] || [ "$(cat "$FAKE_DB/plausible_events_db.default" 2>/dev/null)" = "$given" ] || exit 1
    echo 1
    ;;
  kibana) [ "$1" = cat ] && [ -f "$FAKE_KIBANA_YML" ] && cat "$FAKE_KIBANA_YML" ;;
  *) exit 1 ;;
esac
SH
chmod +x "${WORK}/bin/"*

export PATH="${WORK}/bin:${PATH}"
FAKE_KIBANA_YML="${WORK}/kibana-container.yml"
export FAKE_ROOT FAKE_ES FAKE_MYSQL FAKE_XWIKI_DATA FAKE_KIBANA_YML FAKE_DB FAKE_ENV CALLS
export INFRA_SECRETS_ROOT="${FAKE_ROOT}" WAIT_SECONDS=0
export DEPLOY_SSH_HOST=beta.example DEPLOY_SSH_USER=deploy DEPLOY_SSH_KEY="fake-ssh-key"
export GITHUB_STEP_SUMMARY="${WORK}/summary.md"

ETC="${FAKE_ROOT}/etc/open4goods"
OPT="${FAKE_ROOT}/opt/open4goods"
OLD_ELASTIC="OLDELASTICSENTINEL0000000000000000"
NEW_ELASTIC="NEWELASTICSENTINEL1111111111111111"
OLD_KIBANA="OLDKIBANASENTINEL22222222222222222"
NEW_KIBANA="NEWKIBANASENTINEL33333333333333333"
# The key in service holds '$' and punctuation, as in open4goods-config: single-quoted in infra.env.
OLD_KEY='OLD$KEY%SENTINEL!44444444444444444'

NEW_KEY="NEWKEYSENTINEL55555555555555555555"
OLD_ROOT="OLDROOTSENTINEL6666666666666666666"
NEW_ROOT="NEWROOTSENTINEL7777777777777777777"
OLD_XWIKI="OLDXWIKISENTINEL888888888888888888"
NEW_XWIKI="NEWXWIKISENTINEL999999999999999999"
OLD_PLAUSIBLE="OLDPLAUSIBLESENTINELAAAAAAAAAAAAAA"
NEW_PLAUSIBLE="NEWPLAUSIBLESENTINELBBBBBBBBBBBBBB"
OLD_PG="OLDPGSENTINELCCCCCCCCCCCCCCCCCCCCC"
NEW_PG="NEWPGSENTINELDDDDDDDDDDDDDDDDDDDDD"
OLD_CH="OLDCHSENTINELEEEEEEEEEEEEEEEEEEEEE"
NEW_CH="NEWCHSENTINELFFFFFFFFFFFFFFFFFFFFF"
# The b2b default in service holds punctuation: single-quoted in infra.env on rollback.
OLD_B2B="OLDB2B%SENTINEL!GGGGGGGGGGGGGGGGGGG"
NEW_B2B="NEWB2BSENTINELHHHHHHHHHHHHHHHHHHHH"
SENTINELS="SENTINEL"

failures=0
pass() { echo "ok   - $1"; }
ko() { echo "FAIL - $1"; failures=$((failures + 1)); }

run() {
  set +e
  out="$("$@" 2>&1)"
  status=$?
  set -e
}

# No value in the output, the summary or any command line (ssh, curl, docker arguments).
no_value_leaked() {
  ! printf '%s' "${out}" | grep -q "${SENTINELS}" && ! grep -q "${SENTINELS}" "${CALLS}" "${GITHUB_STEP_SUMMARY}"
}

check() {
  local label=$1
  shift
  if "$@"; then pass "${label}"; else ko "${label}: ${out}"; fi
}

reset_host() {
  rm -rf "${FAKE_ROOT:?}"/* "${FAKE_ES:?}"/* "${FAKE_MYSQL:?}"/* "${FAKE_XWIKI_DATA:?}"/* "${FAKE_KIBANA_YML}" \
    "${FAKE_DB:?}"/* "${FAKE_ENV:?}"/*
  : > "${CALLS}"
  : > "${GITHUB_STEP_SUMMARY}"
  mkdir -p "${ETC}" "${OPT}/bin" "${OPT}/runtime" "${OPT}/latest/beta" "${OPT}/latest/prod"
  for svc in front-api api ui b2b-api; do
    echo jar > "${OPT}/runtime/${svc}.jar"
    echo jar > "${OPT}/latest/${svc}-latest.jar"
    mkdir -p "${OPT}/config/beta/${svc}"
    printf 'spring:\n  elasticsearch:\n    uris: localhost:9200\n    username: elastic\n    password: "%s"\n  mail:\n    password: other\n' \
      "${OLD_ELASTIC}" > "${OPT}/config/beta/${svc}/application-active.yml"
  done
  printf '#!/bin/sh\necho "publish-jars $*" >> "%s"\n' "${WORK}/restarts.log" > "${OPT}/bin/publish-jars.sh"
  # publish-infra.sh of open4goods-config step d-infra: reads /etc/open4goods/infra.env, INFRA_SERVICES. Recreating
  # plausible_events_db sets the ClickHouse password from CLICKHOUSE_PASSWORD, as the image's entrypoint does.
  cat > "${OPT}/bin/publish-infra.sh" <<PUBLISH
#!/bin/sh
# /etc/open4goods/infra.env
echo "publish-infra \$* INFRA_SERVICES=\$INFRA_SERVICES" >> "${WORK}/restarts.log"
case " \$INFRA_SERVICES " in
  *" plausible_events_db "*)
    sed -n 's/^CLICKHOUSE_PASSWORD=//p' "${ETC}/infra.env" | sed "s/^'\\(.*\\)'\$/\\1/" | tr -d '\n' \
      > "${FAKE_DB}/plausible_events_db.default"
    ;;
esac
PUBLISH
  : > "${WORK}/restarts.log"
  printf '%s' "${OLD_ELASTIC}" > "${FAKE_ES}/elastic"
  printf '%s' "${OLD_KIBANA}" > "${FAKE_ES}/kibana_system"
}


########################################
# infra-secrets.sh: arguments and values (runner)
########################################
reset_host
run "${DEPLOY}/infra-secrets.sh" check beta mysql
check "mysql is prod only" [ "${status}" -ne 0 ]
run "${DEPLOY}/infra-secrets.sh" check prod plausible
check "plausible is beta only" [ "${status}" -ne 0 ]
run "${DEPLOY}/infra-secrets.sh" remote apply beta elasticsearch
check "unknown remote action" [ "${status}" -ne 0 ]
O4G_ELASTICSEARCH_PASSWORD="SHORTSENTINEL" run "${DEPLOY}/infra-secrets.sh" check beta elasticsearch
check "a value shorter than 32 characters fails without being printed" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"shorter than 32"* ]] && [[ "$1" != *SENTINEL* ]]' "${status}" "${out}"
O4G_ELASTICSEARCH_PASSWORD="${NEW_ELASTIC}+x" run "${DEPLOY}/infra-secrets.sh" check beta elasticsearch
check "a value outside [A-Za-z0-9._-] fails without being printed" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"outside [A-Za-z0-9._-]"* ]] && [[ "$1" != *SENTINEL* ]]' "${status}" "${out}"
run env -u O4G_ELASTICSEARCH_PASSWORD "${DEPLOY}/infra-secrets.sh" check beta elasticsearch
check "check without the new value is a dry run (warning)" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"::warning::O4G_ELASTICSEARCH_PASSWORD is not set"* ]]' "${status}" "${out}"
run env -u O4G_ELASTICSEARCH_PASSWORD "${DEPLOY}/infra-secrets.sh" remote source beta elasticsearch
check "source without the new value fails" [ "${status}" -ne 0 ]
O4G_ELASTICSEARCH_PASSWORD="${NEW_ELASTIC}" run "${DEPLOY}/infra-secrets.sh" check beta elasticsearch
check "check passes with a strong value, no SSH" \
  bash -c '[ "$0" -eq 0 ] && [ ! -s "$1" ]' "${status}" "${CALLS}"

########################################
# elasticsearch: test, source (idempotent), render, restart, rollback
########################################
reset_host
run env -u O4G_ELASTICSEARCH_PASSWORD "${DEPLOY}/infra-secrets.sh" remote test beta elasticsearch
check "test without the new value finds the password in service (application-active.yml)" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"found (candidate-yml-front-api) and accepted (no new value yet)"* ]]' \
  "${status}" "${out}"
check "test writes nothing" bash -c '[ ! -e "$0/infra-previous.env" ] && [ ! -e "$0/secrets" ]' "${ETC}"

export O4G_ELASTICSEARCH_PASSWORD="${NEW_ELASTIC}"
run "${DEPLOY}/infra-secrets.sh" remote test beta elasticsearch
check "test reports the change source would make" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"source would change it"* ]]' "${status}" "${out}"
check "test leaves Elasticsearch unchanged" [ "$(cat "${FAKE_ES}/elastic")" = "${OLD_ELASTIC}" ]

cp "${OPT}/latest/api-latest.jar" "${WORK}/api.jar" && echo other > "${OPT}/runtime/api.jar"
run "${DEPLOY}/infra-secrets.sh" remote test beta elasticsearch
check "test refuses a running JAR that is not the latest" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"differs from"* ]]' "${status}" "${out}"
cp "${WORK}/api.jar" "${OPT}/runtime/api.jar"

run "${DEPLOY}/infra-secrets.sh" remote restart beta elasticsearch
check "restart refuses before source" bash -c '[ "$0" -ne 0 ] && [ ! -s "$1" ]' "${status}" "${WORK}/restarts.log"

run "${DEPLOY}/infra-secrets.sh" remote source beta elasticsearch
check "source changes the password of elastic" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "$2" ]' "${status}" "${FAKE_ES}/elastic" "${NEW_ELASTIC}"
check "source keeps the previous value (600)" \
  bash -c 'grep -qx "O4G_ELASTICSEARCH_PASSWORD=$1" "$0" && [ "$(stat -c %a "$0")" = 600 ]' \
  "${ETC}/infra-previous.env" "${OLD_ELASTIC}"
check "source prints no value" no_value_leaked

run "${DEPLOY}/infra-secrets.sh" remote source beta elasticsearch
check "source is idempotent" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"already accepts the new password of elastic: nothing to change"* ]] \
    && grep -qx "O4G_ELASTICSEARCH_PASSWORD=$3" "$2"' "${status}" "${out}" "${ETC}/infra-previous.env" "${OLD_ELASTIC}"

run "${DEPLOY}/infra-secrets.sh" remote restart beta elasticsearch
check "restart refuses before the render" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"render it first"* ]] && [ ! -s "$2" ]' "${status}" "${out}" "${WORK}/restarts.log"

for svc in front-api api ui b2b-api; do
  run "${DEPLOY}/render-service-secrets.sh" --source apply beta "${svc}"
  [ "${status}" -eq 0 ] || ko "render --source ${svc}: ${out}"
done
check "render --source writes the config tree of the 4 consumers" \
  bash -c 'for s in front-api api ui b2b-api; do [ "$(cat "$0/secrets/$s/spring.elasticsearch.password")" = "$1" ] || exit 1; done' \
  "${ETC}" "${NEW_ELASTIC}"
check "render --source writes only the source rows (no other api secret, no warning about them)" \
  bash -c '[ "$(ls "$0/secrets/api")" = spring.elasticsearch.password ]' "${ETC}"

run "${DEPLOY}/infra-secrets.sh" remote restart beta elasticsearch
check "restart restarts front-api, api, ui, b2b-api in that order" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "$(printf "publish-jars beta restart %s\n" front-api api ui b2b-api)" ]' \
  "${status}" "${WORK}/restarts.log"
check "restart prints no value" no_value_leaked

: > "${WORK}/restarts.log"
run "${DEPLOY}/infra-secrets.sh" remote rollback beta elasticsearch
check "rollback puts the previous password back at the source and in the trees, then restarts" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1/elastic")" = "$3" ] && [ "$(cat "$2/secrets/api/spring.elasticsearch.password")" = "$3" ] \
    && [ "$(wc -l < "$4")" -eq 4 ]' "${status}" "${FAKE_ES}" "${ETC}" "${OLD_ELASTIC}" "${WORK}/restarts.log"
check "rollback prints no value" no_value_leaked
unset O4G_ELASTICSEARCH_PASSWORD

########################################
# kibana: prerequisites, source, render, restart (re-encryption)
########################################
reset_host
printf 'elasticsearch.username: kibana_system\nelasticsearch.password: %s\nxpack.encryptedSavedObjects:\n  encryptionKey: "%s"\n' \
  "${OLD_KIBANA}" "${OLD_KEY}" > "${OPT}/bin/kibana.yml"
cp "${OPT}/bin/kibana.yml" "${OPT}/latest/beta/kibana.yml"
export INFRA_KIBANA_SYSTEM_PASSWORD="${NEW_KIBANA}" INFRA_KIBANA_ENCRYPTION_KEY="${NEW_KEY}"
run "${DEPLOY}/infra-secrets.sh" remote test beta kibana
check "kibana test fails until open4goods-config step d-infra is deployed" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"deploy open4goods-config step d-infra first"* ]]' "${status}" "${out}"
# shellcheck disable=SC2016 # literal placeholders.
printf 'elasticsearch.password: ${INFRA_KIBANA_SYSTEM_PASSWORD}\nencryptionKey: ${INFRA_KIBANA_ENCRYPTION_KEY}\ndecryptionOnlyKeys: ["${INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY}"]\n' \
  > "${OPT}/latest/beta/kibana.yml"
# shellcheck disable=SC2016 # literal placeholders.
printf '      INFRA_KIBANA_SYSTEM_PASSWORD: ${INFRA_KIBANA_SYSTEM_PASSWORD:?}\n      INFRA_KIBANA_ENCRYPTION_KEY: ${INFRA_KIBANA_ENCRYPTION_KEY:?}\n      INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY: ${INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY:?}\n' \
  > "${OPT}/latest/beta/docker-compose.infra.yml"
run "${DEPLOY}/infra-secrets.sh" remote test beta kibana
check "kibana test finds kibana_system and the key in service (kibana.yml)" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"kibana_system in service is found (candidate-kibana-yml)"* ]] \
    && [[ "$1" == *"keep it as the decryption-only key"* ]]' "${status}" "${out}"
# Another target's publish-infra.sh already copied the d-infra kibana.yml to bin/: the running container still has
# the literal values in service.
mv "${OPT}/bin/kibana.yml" "${FAKE_KIBANA_YML}"
cp "${OPT}/latest/beta/kibana.yml" "${OPT}/bin/kibana.yml"
run "${DEPLOY}/infra-secrets.sh" remote test beta kibana
check "kibana test reads the values in service from the running container once bin/kibana.yml has placeholders" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"kibana_system in service is found (candidate-kibana-container)"* ]]' "${status}" "${out}"
run "${DEPLOY}/infra-secrets.sh" remote source beta kibana
check "kibana source changes kibana_system and makes the key in service decryption-only" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1/kibana_system")" = "$3" ] \
    && grep -Fqx "INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY='"'"'$4'"'"'" "$2/infra.env" \
    && grep -Fqx "INFRA_KIBANA_ENCRYPTION_KEY=$4" "$2/infra-previous.env" \
    && grep -qx "INFRA_KIBANA_SYSTEM_PASSWORD=$5" "$2/infra-previous.env"' \
  "${status}" "${FAKE_ES}" "${ETC}" "${NEW_KIBANA}" "${OLD_KEY}" "${OLD_KIBANA}"
run "${DEPLOY}/render-service-secrets.sh" --source apply beta infra
check "render --source infra writes the Kibana lines and keeps the decryption-only key" \
  bash -c '[ "$0" -eq 0 ] && grep -qx "INFRA_KIBANA_SYSTEM_PASSWORD=$2" "$1" && grep -qx "INFRA_KIBANA_ENCRYPTION_KEY=$3" "$1" \
    && grep -Fqx "INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY='"'"'$4'"'"'" "$1" && [ "$(stat -c %a "$1")" = 600 ]' \
  "${status}" "${ETC}/infra.env" "${NEW_KIBANA}" "${NEW_KEY}" "${OLD_KEY}"
run "${DEPLOY}/infra-secrets.sh" remote source beta kibana
check "kibana source is idempotent after the render (the new key is never made decryption-only)" \
  bash -c '[ "$0" -eq 0 ] && grep -Fqx "INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY='"'"'$2'"'"'" "$1"' "${status}" "${ETC}/infra.env" "${OLD_KEY}"
run "${DEPLOY}/infra-secrets.sh" remote restart beta kibana
check "kibana restart recreates Kibana only, then re-encrypts the saved objects" \
  bash -c '[ "$0" -eq 0 ] && grep -qx "publish-infra beta INFRA_SERVICES=kibana" "$1" && [[ "$2" == *"\"successful\":2"* ]]' \
  "${status}" "${WORK}/restarts.log" "${out}"
run "${DEPLOY}/infra-secrets.sh" remote rollback beta kibana
check "kibana rollback: previous kibana_system and key back, the new key decryption-only" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1/kibana_system")" = "$3" ] \
    && grep -Fqx "INFRA_KIBANA_ENCRYPTION_KEY='"'"'$4'"'"'" "$2/infra.env" \
    && grep -Fqx "INFRA_KIBANA_PREVIOUS_ENCRYPTION_KEY=$5" "$2/infra.env" \
    && grep -Fqx "INFRA_KIBANA_SYSTEM_PASSWORD=$3" "$2/infra.env"' \
  "${status}" "${FAKE_ES}" "${ETC}" "${OLD_KIBANA}" "${OLD_KEY}" "${NEW_KEY}"
check "kibana prints no value" no_value_leaked
unset INFRA_KIBANA_SYSTEM_PASSWORD INFRA_KIBANA_ENCRYPTION_KEY

########################################
# mysql (prod): ALTER USER, persisted hibernate.cfg.xml, restart of XWiki only
########################################
reset_host
printf '%s' "${OLD_ROOT}" > "${FAKE_MYSQL}/root"
printf '%s' "${OLD_XWIKI}" > "${FAKE_MYSQL}/xwiki"
printf '    environment:\n      - DB_USER=xwiki\n      - DB_PASSWORD=%s\n      - MYSQL_ROOT_PASSWORD=%s\n' \
  "${OLD_XWIKI}" "${OLD_ROOT}" > "${OPT}/bin/docker-compose.infra.yml"
# shellcheck disable=SC2016 # literal placeholders.
printf '      - DB_PASSWORD=${INFRA_XWIKI_DB_PASSWORD:?}\n      - MYSQL_ROOT_PASSWORD=${INFRA_MYSQL_ROOT_PASSWORD:?}\n' \
  > "${OPT}/latest/prod/docker-compose.infra.yml"
printf '<property name="hibernate.connection.url">jdbc:mysql://mysql/xwiki</property>\n<property name="hibernate.connection.password">%s</property>\n' \
  "${OLD_XWIKI}" > "${FAKE_XWIKI_DATA}/hibernate.cfg.xml"
export INFRA_XWIKI_DB_PASSWORD="${NEW_XWIKI}" INFRA_MYSQL_ROOT_PASSWORD="${NEW_ROOT}"
run "${DEPLOY}/infra-secrets.sh" remote test prod mysql
check "mysql test finds root and xwiki in service (compose)" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"root in service is found (candidate-root-compose)"* ]] \
    && [[ "$1" == *"xwiki in service is found (candidate-xwiki-compose)"* ]]' "${status}" "${out}"
run "${DEPLOY}/infra-secrets.sh" remote source prod mysql
check "mysql source changes xwiki and root, and the persisted hibernate.cfg.xml" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1/xwiki")" = "$3" ] && [ "$(cat "$1/root")" = "$4" ] \
    && grep -q "connection.password\">$3<" "$2" && grep -q "connection.url\">jdbc:mysql://mysql/xwiki<" "$2"' \
  "${status}" "${FAKE_MYSQL}" "${FAKE_XWIKI_DATA}/hibernate.cfg.xml" "${NEW_XWIKI}" "${NEW_ROOT}"
run "${DEPLOY}/infra-secrets.sh" remote source prod mysql
check "mysql source is idempotent" bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"already accepts the new password of root"* ]]' \
  "${status}" "${out}"
run "${DEPLOY}/render-service-secrets.sh" --source apply prod infra
run "${DEPLOY}/infra-secrets.sh" remote restart prod mysql
check "mysql restart recreates XWiki only" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "publish-infra prod INFRA_SERVICES=xwiki" ]' "${status}" "${WORK}/restarts.log"
: > "${WORK}/restarts.log"
run "${DEPLOY}/infra-secrets.sh" remote rollback prod mysql
check "mysql rollback restores both users, hibernate.cfg.xml and infra.env" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1/xwiki")" = "$4" ] && [ "$(cat "$1/root")" = "$5" ] \
    && grep -q "connection.password\">$4<" "$2" && grep -qx "INFRA_XWIKI_DB_PASSWORD=$4" "$3"' \
  "${status}" "${FAKE_MYSQL}" "${FAKE_XWIKI_DATA}/hibernate.cfg.xml" "${ETC}/infra.env" "${OLD_XWIKI}" "${OLD_ROOT}"
check "mysql prints no value" no_value_leaked
unset INFRA_XWIKI_DB_PASSWORD INFRA_MYSQL_ROOT_PASSWORD

########################################
# plausible (beta): key kept, render overrides config/<env>/infra/.env, restart
########################################
reset_host
mkdir -p "${OPT}/config/beta/infra"
printf 'PLAUSIBLE_BASE_URL=https://plausible.example\nPLAUSIBLE_SECRET_KEY_BASE=%s\n' "${OLD_PLAUSIBLE}" \
  > "${OPT}/config/beta/infra/.env"
export INFRA_PLAUSIBLE_SECRET_KEY_BASE="${NEW_PLAUSIBLE}"
run "${DEPLOY}/infra-secrets.sh" remote source beta plausible
check "plausible source keeps the key in service and reports TOTP users" \
  bash -c '[ "$0" -eq 0 ] && grep -qx "INFRA_PLAUSIBLE_SECRET_KEY_BASE=$3" "$1" && [[ "$2" == *"TOTP enabled"*": 0"* ]]' \
  "${status}" "${ETC}/infra-previous.env" "${out}" "${OLD_PLAUSIBLE}"
run "${DEPLOY}/render-service-secrets.sh" --source apply beta infra
run "${DEPLOY}/infra-secrets.sh" remote restart beta plausible
check "plausible restart after the render" \
  bash -c '[ "$0" -eq 0 ] && grep -qx "PLAUSIBLE_SECRET_KEY_BASE=$3" "$1" && grep -qx "publish-infra beta INFRA_SERVICES=plausible" "$2"' \
  "${status}" "${ETC}/infra.env" "${WORK}/restarts.log" "${NEW_PLAUSIBLE}"
check "plausible prints no value" no_value_leaked
unset INFRA_PLAUSIBLE_SECRET_KEY_BASE

########################################
# plausible-postgres (beta): ALTER USER over TCP, render, restart of Plausible only
########################################
reset_host
mkdir -p "${OPT}/config/beta/infra"
printf 'PLAUSIBLE_BASE_URL=https://plausible.example\n' > "${OPT}/config/beta/infra/.env"
# The literal default of open4goods-config is the value in service; the compose of d-infra still reads the variable.
# shellcheck disable=SC2016 # literal placeholders.
printf '      POSTGRES_PASSWORD: ${PLAUSIBLE_PG_PASSWORD:-%s}\n      DATABASE_URL: postgres://postgres:${PLAUSIBLE_PG_PASSWORD:-%s}@plausible_db:5432/plausible\n' \
  "${OLD_PG}" "${OLD_PG}" > "${OPT}/bin/docker-compose.infra.yml"
cp "${OPT}/bin/docker-compose.infra.yml" "${OPT}/latest/beta/docker-compose.infra.yml"
printf '%s' "${OLD_PG}" > "${FAKE_DB}/plausible_db.postgres"
export INFRA_PLAUSIBLE_PG_PASSWORD="${NEW_PG}"
run "${DEPLOY}/infra-secrets.sh" check prod plausible-postgres
check "plausible-postgres is beta only" [ "${status}" -ne 0 ]
touch "${FAKE_DB}/trust"
run "${DEPLOY}/infra-secrets.sh" remote test beta plausible-postgres
check "plausible-postgres test refuses a Postgres that accepts any password" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"accepts any password over TCP"* ]]' "${status}" "${out}"
rm -f "${FAKE_DB}/trust"
run "${DEPLOY}/infra-secrets.sh" remote test beta plausible-postgres
check "plausible-postgres test finds the password in service (compose default)" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"postgres in service is found (candidate-compose)"* ]] \
    && [ "$(cat "$2")" = "$3" ] && [ ! -e "$4" ]' \
  "${status}" "${out}" "${FAKE_DB}/plausible_db.postgres" "${OLD_PG}" "${ETC}/infra-previous.env"
run "${DEPLOY}/infra-secrets.sh" remote restart beta plausible-postgres
check "plausible-postgres restart refuses before source" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"run source first"* ]] && [ ! -s "$2" ]' "${status}" "${out}" "${WORK}/restarts.log"
run "${DEPLOY}/infra-secrets.sh" remote source beta plausible-postgres
check "plausible-postgres source changes postgres and keeps the previous value" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "$3" ] && grep -qx "INFRA_PLAUSIBLE_PG_PASSWORD=$4" "$2"' \
  "${status}" "${FAKE_DB}/plausible_db.postgres" "${ETC}/infra-previous.env" "${NEW_PG}" "${OLD_PG}"
run "${DEPLOY}/infra-secrets.sh" remote source beta plausible-postgres
check "plausible-postgres source is idempotent" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"already accepts the new password of postgres: nothing to change"* ]]' \
  "${status}" "${out}"
run "${DEPLOY}/infra-secrets.sh" remote restart beta plausible-postgres
check "plausible-postgres restart refuses before the render" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"render it first"* ]] && [ ! -s "$2" ]' "${status}" "${out}" "${WORK}/restarts.log"
run "${DEPLOY}/render-service-secrets.sh" --source apply beta infra
check "render --source infra writes PLAUSIBLE_PG_PASSWORD only" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "PLAUSIBLE_PG_PASSWORD=$2" ]' "${status}" "${ETC}/infra.env" "${NEW_PG}"
run "${DEPLOY}/infra-secrets.sh" remote restart beta plausible-postgres
check "plausible-postgres restart recreates Plausible only" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "publish-infra beta INFRA_SERVICES=plausible" ]' "${status}" "${WORK}/restarts.log"
: > "${WORK}/restarts.log"
run "${DEPLOY}/infra-secrets.sh" remote rollback beta plausible-postgres
check "plausible-postgres rollback restores postgres and infra.env, recreates Plausible" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "$3" ] && grep -qx "PLAUSIBLE_PG_PASSWORD=$3" "$2" \
    && grep -qx "publish-infra beta INFRA_SERVICES=plausible" "$4"' \
  "${status}" "${FAKE_DB}/plausible_db.postgres" "${ETC}/infra.env" "${OLD_PG}" "${WORK}/restarts.log"
check "plausible-postgres prints no value" no_value_leaked
unset INFRA_PLAUSIBLE_PG_PASSWORD

########################################
# plausible-clickhouse (beta): the password changes when ClickHouse is recreated, then Plausible
########################################
reset_host
mkdir -p "${OPT}/config/beta/infra"
: > "${OPT}/config/beta/infra/.env"
# shellcheck disable=SC2016 # literal placeholders.
printf '      CLICKHOUSE_PASSWORD: ${CLICKHOUSE_PASSWORD:-x}\n' > "${OPT}/bin/docker-compose.infra.yml"
cp "${OPT}/bin/docker-compose.infra.yml" "${OPT}/latest/beta/docker-compose.infra.yml"
# The running Plausible holds the value in service in its URL (the compose default above is not it).
printf 'http://default:%s@plausible_events_db:8123/plausible' "${OLD_CH}" > "${FAKE_ENV}/plausible.CLICKHOUSE_DATABASE_URL"
printf '%s' "${OLD_CH}" > "${FAKE_DB}/plausible_events_db.default"
export INFRA_CLICKHOUSE_PASSWORD="${NEW_CH}"
run "${DEPLOY}/infra-secrets.sh" remote test beta plausible-clickhouse
check "plausible-clickhouse test finds the password in service (Plausible's URL)" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"default in service is found (candidate-plausible-container)"* ]]' "${status}" "${out}"
run "${DEPLOY}/infra-secrets.sh" remote restart beta plausible-clickhouse
check "plausible-clickhouse restart refuses before the render" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"render it first"* ]] && [ ! -s "$2" ]' "${status}" "${out}" "${WORK}/restarts.log"
run "${DEPLOY}/render-service-secrets.sh" --source apply beta infra
run "${DEPLOY}/infra-secrets.sh" remote restart beta plausible-clickhouse
check "plausible-clickhouse restart refuses before source (no previous value kept)" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"run source first"* ]] && [ ! -s "$2" ]' "${status}" "${out}" "${WORK}/restarts.log"
run "${DEPLOY}/infra-secrets.sh" remote source beta plausible-clickhouse
check "plausible-clickhouse source keeps the value in service, changes nothing yet" \
  bash -c '[ "$0" -eq 0 ] && grep -qx "INFRA_CLICKHOUSE_PASSWORD=$3" "$1" && [ "$(cat "$2")" = "$3" ]' \
  "${status}" "${ETC}/infra-previous.env" "${FAKE_DB}/plausible_events_db.default" "${OLD_CH}"
run "${DEPLOY}/infra-secrets.sh" remote restart beta plausible-clickhouse
check "plausible-clickhouse restart recreates ClickHouse, which takes the new password, then Plausible" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "$3" ] \
    && [ "$(cat "$2")" = "$(printf "publish-infra beta INFRA_SERVICES=%s\n" plausible_events_db plausible)" ]' \
  "${status}" "${FAKE_DB}/plausible_events_db.default" "${WORK}/restarts.log" "${NEW_CH}"
: > "${WORK}/restarts.log"
run "${DEPLOY}/infra-secrets.sh" remote restart beta plausible-clickhouse
check "plausible-clickhouse restart is idempotent (ClickHouse not recreated again)" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "publish-infra beta INFRA_SERVICES=plausible" ]' "${status}" "${WORK}/restarts.log"
: > "${WORK}/restarts.log"
run "${DEPLOY}/infra-secrets.sh" remote rollback beta plausible-clickhouse
check "plausible-clickhouse rollback puts the previous password back in infra.env and ClickHouse" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "$3" ] && grep -qx "CLICKHOUSE_PASSWORD=$3" "$2"' \
  "${status}" "${FAKE_DB}/plausible_events_db.default" "${ETC}/infra.env" "${OLD_CH}"
check "plausible-clickhouse prints no value" no_value_leaked
unset INFRA_CLICKHOUSE_PASSWORD

########################################
# b2b-postgres (beta and prod): ALTER USER, render infra.env and the b2b-api config tree, restart b2b-api
########################################
reset_host
# shellcheck disable=SC2016 # literal placeholders.
printf '      POSTGRES_PASSWORD: ${B2B_POSTGRES_PASSWORD:-%s}\n' "${OLD_B2B}" > "${OPT}/bin/docker-compose.infra.yml"
mkdir -p "${OPT}/config/prod/b2b-api"
# shellcheck disable=SC2016 # literal placeholders.
printf 'spring:\n  datasource:\n    password: ${B2B_POSTGRES_PASSWORD:%s}\n' "${OLD_B2B}" \
  > "${OPT}/config/prod/b2b-api/application-active.yml"
printf 'product_data_api' > "${FAKE_ENV}/b2b_postgres.POSTGRES_USER"
printf 'product_data_api' > "${FAKE_ENV}/b2b_postgres.POSTGRES_DB"
printf '%s' "${OLD_B2B}" > "${FAKE_DB}/b2b_postgres.product_data_api"
export B2B_API_POSTGRES_PASSWORD="${NEW_B2B}"
printf 'SPRING_DATASOURCE_PASSWORD=x\n' > "${ETC}/b2b-api.env"
run "${DEPLOY}/infra-secrets.sh" remote test prod b2b-postgres
check "b2b-postgres test refuses an env line that beats the config tree" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"sets SPRING_DATASOURCE_PASSWORD"* ]]' "${status}" "${out}"
rm -f "${ETC}/b2b-api.env"
run "${DEPLOY}/infra-secrets.sh" remote test prod b2b-postgres
check "b2b-postgres test finds the password in service (application-active.yml default)" \
  bash -c '[ "$0" -eq 0 ] && [[ "$1" == *"product_data_api in service is found (candidate-yml)"* ]]' "${status}" "${out}"
run "${DEPLOY}/infra-secrets.sh" remote source prod b2b-postgres
check "b2b-postgres source changes the user and keeps the previous value" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "$3" ] && grep -Fqx "B2B_API_POSTGRES_PASSWORD=$4" "$2"' \
  "${status}" "${FAKE_DB}/b2b_postgres.product_data_api" "${ETC}/infra-previous.env" "${NEW_B2B}" "${OLD_B2B}"
run "${DEPLOY}/render-service-secrets.sh" --source apply prod infra
run "${DEPLOY}/infra-secrets.sh" remote restart prod b2b-postgres
check "b2b-postgres restart refuses before the b2b-api render" \
  bash -c '[ "$0" -ne 0 ] && [[ "$1" == *"config tree of b2b-api does not hold"* ]] && [ ! -s "$2" ]' \
  "${status}" "${out}" "${WORK}/restarts.log"
run "${DEPLOY}/render-service-secrets.sh" --source apply prod b2b-api
check "render --source b2b-api writes spring.datasource.password next to the Elasticsearch tree" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1/spring.datasource.password")" = "$2" ] && [ "$(ls "$1")" = spring.datasource.password ]' \
  "${status}" "${ETC}/secrets/b2b-api" "${NEW_B2B}"
run "${DEPLOY}/infra-secrets.sh" remote restart prod b2b-postgres
check "b2b-postgres restart restarts b2b-api only" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "publish-jars prod restart b2b-api" ] && grep -qx "B2B_POSTGRES_PASSWORD=$3" "$2"' \
  "${status}" "${WORK}/restarts.log" "${ETC}/infra.env" "${NEW_B2B}"
run "${DEPLOY}/infra-secrets.sh" remote rollback prod b2b-postgres
check "b2b-postgres rollback restores the user, the config tree and infra.env (single-quoted)" \
  bash -c '[ "$0" -eq 0 ] && [ "$(cat "$1")" = "$4" ] && [ "$(cat "$2/secrets/b2b-api/spring.datasource.password")" = "$4" ] \
    && grep -Fqx "B2B_POSTGRES_PASSWORD='"'"'$4'"'"'" "$2/infra.env" && [ "$(wc -l < "$3")" -eq 2 ]' \
  "${status}" "${FAKE_DB}/b2b_postgres.product_data_api" "${ETC}" "${WORK}/restarts.log" "${OLD_B2B}"
check "b2b-postgres prints no value" no_value_leaked
unset B2B_API_POSTGRES_PASSWORD

if [[ ${failures} -gt 0 ]]; then
  echo "${failures} test(s) failed"
  exit 1
fi
echo "infra-secrets test passed"
