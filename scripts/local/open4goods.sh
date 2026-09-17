#!/usr/bin/env bash
# Hybrid local runtime: persistent Docker infrastructure, native Java/Nuxt applications.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOCAL_ROOT="$ROOT/.local"
ENV_FILE="$ROOT/.env.local"
TEMPLATE_ROOT="$ROOT/ops/local/config"
SERVICES=(api ui admin front-api b2b-api exposed-docs geocode frontend b2b-frontend)

usage() {
  cat >&2 <<'EOF'
usage: scripts/local/open4goods.sh <command>
  init | doctor | up | restart <service> | status | logs [service] | down
  backup verify | xwiki import | data sample | data full | jobs run <job>

services: api ui admin front-api b2b-api exposed-docs geocode frontend b2b-frontend
jobs: eprel icecat feeds batch
EOF
  exit 2
}

load_env() {
  if [ ! -f "$ENV_FILE" ]; then
    echo "missing .env.local; run '$0 init'" >&2
    exit 1
  fi
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
  : "${O4G_LOCAL_DATA_ROOT:=$LOCAL_ROOT/data}"
  : "${O4G_LOCAL_CONFIG_DIR:=$LOCAL_ROOT/config}"
  : "${PRODUCT_BACKUP_SOURCE_URI:=$LOCAL_ROOT/backup}"
  : "${XWIKI_XAR_PATH:=$PRODUCT_BACKUP_SOURCE_URI/xwiki-backup.xar}"
  case "$O4G_LOCAL_DATA_ROOT" in /*) ;; *) O4G_LOCAL_DATA_ROOT="$ROOT/${O4G_LOCAL_DATA_ROOT#./}" ;; esac
  case "$O4G_LOCAL_CONFIG_DIR" in /*) ;; *) O4G_LOCAL_CONFIG_DIR="$ROOT/${O4G_LOCAL_CONFIG_DIR#./}" ;; esac
  case "$PRODUCT_BACKUP_SOURCE_URI" in
    file://*) PRODUCT_BACKUP_SOURCE_URI="${PRODUCT_BACKUP_SOURCE_URI#file://}" ;;
    /*) ;;
    *) PRODUCT_BACKUP_SOURCE_URI="$ROOT/${PRODUCT_BACKUP_SOURCE_URI#./}" ;;
  esac
  case "$XWIKI_XAR_PATH" in /*) ;; *) XWIKI_XAR_PATH="$ROOT/${XWIKI_XAR_PATH#./}" ;; esac
  export O4G_LOCAL_DATA_ROOT O4G_LOCAL_CONFIG_DIR PRODUCT_BACKUP_SOURCE_URI XWIKI_XAR_PATH
}

init_local() {
  if [ ! -f "$ENV_FILE" ]; then
    cp "$ROOT/.env.local.example" "$ENV_FILE"
    chmod 600 "$ENV_FILE"
    echo "created .env.local from the tracked template"
  else
    echo "kept existing .env.local"
  fi
  load_env
  mkdir -p "$O4G_LOCAL_CONFIG_DIR" "$LOCAL_ROOT/logs" "$LOCAL_ROOT/pids" \
    "$PRODUCT_BACKUP_SOURCE_URI" "$O4G_LOCAL_DATA_ROOT"/{elasticsearch,redis,postgres,mysql,xwiki} \
    "$O4G_LOCAL_DATA_ROOT"/{sitemap,open-data,cache}
  for template in "$TEMPLATE_ROOT"/*.yml.example; do
    service="$(basename "$template" .yml.example)"
    target="$O4G_LOCAL_CONFIG_DIR/$service.yml"
    if [ ! -f "$target" ]; then
      cp "$template" "$target"
      chmod 600 "$target"
      echo "created .local/config/$service.yml"
    fi
  done
}

is_service() {
  local candidate="$1" service
  for service in "${SERVICES[@]}"; do
    [ "$candidate" = "$service" ] && return 0
  done
  return 1
}

pid_file() { printf '%s/pids/%s.pid\n' "$LOCAL_ROOT" "$1"; }
log_file() { printf '%s/logs/%s.log\n' "$LOCAL_ROOT" "$1"; }

alive() {
  local file pid
  file="$(pid_file "$1")"
  [ -f "$file" ] || return 1
  read -r pid < "$file"
  [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null
}

start_process() {
  local service="$1" directory="$2"
  shift 2
  if alive "$service"; then
    echo "$service already running"
    return 0
  fi
  : > "$(log_file "$service")"
  (
    cd "$directory"
    exec setsid "$@" >>"$(log_file "$service")" 2>&1
  ) &
  echo "$!" > "$(pid_file "$service")"
  echo "started $service pid=$!"
}

start_java() {
  local service="$1" module="$2" port="$3" config="$O4G_LOCAL_CONFIG_DIR/$1.yml"
  start_process "$service" "$ROOT/$module" env \
    SPRING_PROFILES_ACTIVE=local \
    SPRING_CONFIG_ADDITIONAL_LOCATION="optional:file:$config" \
    SERVER_PORT="$port" \
    mvn --offline spring-boot:run \
      -Dspring-boot.run.jvmArguments=--add-opens=java.base/java.math=ALL-UNNAMED
}

start_native() {
  local service="$1"
  case "$service" in
    api) start_java api api 8081 ;;
    ui) start_java ui ui 8082 ;;
    admin) start_java admin admin 8085 ;;
    front-api) start_java front-api front-api 8086 ;;
    b2b-api)
      start_process b2b-api "$ROOT/b2b-api" env SPRING_PROFILES_ACTIVE=local B2B_API_PORT=8087 \
        SPRING_CONFIG_ADDITIONAL_LOCATION="optional:file:$O4G_LOCAL_CONFIG_DIR/b2b-api.yml" \
        mvn --offline spring-boot:run \
          -Dspring-boot.run.jvmArguments=--add-opens=java.base/java.math=ALL-UNNAMED
      ;;
    exposed-docs) start_java exposed-docs services/exposed-docs 8088 ;;
    geocode) start_java geocode services/geocode 8089 ;;
    frontend)
      start_process frontend "$ROOT/frontend" env \
        API_URL=http://localhost:8086 PUBLIC_API_URL=http://localhost:8086 STATIC_SERVER=http://localhost:8082 \
        SITE_URL=http://localhost:3000 SITEMAP_BASE_PATH="$O4G_LOCAL_DATA_ROOT/sitemap" \
        NUXT_MACHINE_TOKEN="${FRONT_SECURITY_SHARED_TOKEN:-CHANGE_ME_SHARED_TOKEN}" \
        pnpm dev --host 127.0.0.1 --port 3000
      ;;
    b2b-frontend)
      start_process b2b-frontend "$ROOT/b2b-frontend" env \
        NUXT_PUBLIC_BACKEND_BASE_URL=http://localhost:8087 \
        NUXT_PUBLIC_ROUTER_BASE_URL=http://localhost:8087 \
        NUXT_PUBLIC_SITE_URL=http://localhost:3001 \
        BACKEND_OPENAPI_URL=http://localhost:8087/v3/api-docs \
        NUXT_BACKEND_OPEN_API_URL=http://localhost:8087/v3/api-docs \
        NUXT_OIDC_GOOGLE_REDIRECT_URI=http://localhost:3001/auth/callback/google \
        pnpm dev --host 127.0.0.1 --port 3001
      ;;
    *) usage ;;
  esac
}

stop_native() {
  local service="$1" file pid sid
  file="$(pid_file "$service")"
  [ -f "$file" ] || return 0
  read -r pid < "$file"
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
    sid="$(ps -o sid= -p "$pid" | tr -d ' ')"
    if [ "$sid" = "$pid" ]; then kill -- "-$pid"; else kill "$pid"; fi
    for _ in $(seq 1 40); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 0.25
    done
  fi
  rm -f -- "$file"
  echo "stopped $service"
}

http_ready() {
  local port="$1" path="$2" status
  status="$(curl --silent --output /dev/null --max-time 2 --write-out '%{http_code}' "http://127.0.0.1:$port$path" || true)"
  [ "$status" != 000 ] && [ "$status" -lt 500 ]
}

doctor() {
  load_env
  local missing=0 command_name config variable value
  for command_name in docker java mvn node pnpm curl gzip sha256sum unzip rg; do
    if ! command -v "$command_name" >/dev/null; then echo "missing command: $command_name" >&2; missing=1; fi
  done
  docker compose version >/dev/null 2>&1 || { echo "docker compose is unavailable" >&2; missing=1; }
  for config in api ui admin front-api b2b-api exposed-docs geocode; do
    [ -f "$O4G_LOCAL_CONFIG_DIR/$config.yml" ] || { echo "missing local config: $config" >&2; missing=1; }
  done
  for variable in API_URL PUBLIC_API_URL STATIC_SERVER SITE_URL FRONT_EXPOSED_DOCS_BASE_URL FRONT_GEOCODE_BASE_URL; do
    value="${!variable:-}"
    if [[ "$value" == *nudger.fr* || "$value" == *beta* ]]; then
      echo "remote dependency rejected in local variable: $variable" >&2
      missing=1
    fi
  done
  if rg -n --no-heading 'https?://[^ ]*(beta\.)?nudger\.fr' "$O4G_LOCAL_CONFIG_DIR" >/dev/null 2>&1; then
    echo "remote Nudger dependency rejected in .local/config" >&2
    missing=1
  fi
  [ "$missing" -eq 0 ] || return 1
  docker compose --env-file "$ENV_FILE" config --quiet
  echo "local doctor passed without exposing configuration values"
}

status() {
  local service port path
  docker compose ps
  for service in "${SERVICES[@]}"; do
    case "$service" in
      frontend) port=3000; path=/ ;;
      b2b-frontend) port=3001; path=/ ;;
      api) port=8081; path=/actuator/health ;;
      ui) port=8082; path=/actuator/health ;;
      admin) port=8085; path=/actuator/health ;;
      front-api) port=8086; path=/actuator/health ;;
      b2b-api) port=8087; path=/actuator/health ;;
      exposed-docs) port=8088; path=/actuator/health ;;
      geocode) port=8089; path=/actuator/health ;;
    esac
    if alive "$service"; then
      if http_ready "$port" "$path"; then
        echo "$service READY pid=$(<"$(pid_file "$service")") port=$port"
      else
        echo "$service STARTING pid=$(<"$(pid_file "$service")") port=$port"
      fi
    else
      echo "$service STOPPED port=$port"
    fi
  done
}

run_admin_job() {
  local job="$1" method=POST endpoint
  case "$job" in
    eprel) endpoint=/eprel/index ;;
    icecat) method=GET; endpoint=/icecat/index/sync ;;
    feeds) endpoint=/feeds ;;
    batch) endpoint=/batch ;;
    *) echo "unknown explicit job: $job" >&2; usage ;;
  esac
  : "${O4G_LOCAL_ADMIN_KEY:?set O4G_LOCAL_ADMIN_KEY in .env.local}"
  printf '%s job=%s target=local-api\n' "$(date -u +%FT%TZ)" "$job" >> "$LOCAL_ROOT/jobs.log"
  curl --fail --show-error --silent --request "$method" \
    --header "Authorization: $O4G_LOCAL_ADMIN_KEY" "http://127.0.0.1:8081$endpoint"
  echo "explicit local job submitted: $job"
}

command_name="${1:-}"
case "$command_name" in
  init) init_local ;;
  doctor) doctor ;;
  up)
    init_local
    doctor
    docker compose --env-file "$ENV_FILE" up --detach --wait elasticsearch redis postgres
    for service in "${SERVICES[@]}"; do start_native "$service"; done
    status
    ;;
  restart)
    load_env
    service="${2:-}"
    is_service "$service" || usage
    stop_native "$service"
    start_native "$service"
    ;;
  status) load_env; status ;;
  logs)
    service="${2:-}"
    if [ -n "$service" ]; then
      is_service "$service" || usage
      tail -n 200 -f "$(log_file "$service")"
    else
      tail -n 100 "$LOCAL_ROOT"/logs/*.log
    fi
    ;;
  down)
    load_env
    for service in "${SERVICES[@]}"; do stop_native "$service"; done
    docker compose --env-file "$ENV_FILE" down
    ;;
  backup)
    [ "${2:-}" = verify ] || usage
    load_env
    [ -f "$PRODUCT_BACKUP_SOURCE_URI/products-backup-manifest.json" ] || {
      echo "backup manifest absent; wait for the owner copy to finish" >&2; exit 1;
    }
    python3 "$ROOT/scripts/migration/pin_product_backup.py" \
      --source-uri "$PRODUCT_BACKUP_SOURCE_URI" --output "$LOCAL_ROOT/backup/products-backup-pin.json"
    if [ -f "$XWIKI_XAR_PATH" ]; then unzip -tq "$XWIKI_XAR_PATH" >/dev/null; fi
    echo "backup and optional XAR verification passed"
    ;;
  xwiki)
    [ "${2:-}" = import ] || usage
    load_env
    : "${XWIKI_USERNAME:?set XWIKI_USERNAME in .env.local}"
    : "${XWIKI_PASSWORD:?set XWIKI_PASSWORD in .env.local}"
    [ -f "$XWIKI_XAR_PATH" ] || { echo "configured XAR is absent" >&2; exit 1; }
    unzip -tq "$XWIKI_XAR_PATH" >/dev/null
    curl --fail --show-error --silent --user "$XWIKI_USERNAME:$XWIKI_PASSWORD" \
      --form "action=import" --form "file=@$XWIKI_XAR_PATH" \
      http://127.0.0.1:8080/xwiki/bin/import/XWiki/XWikiPreferences >/dev/null
    echo "local XWiki import request completed"
    ;;
  data)
    load_env
    case "${2:-}" in
      sample)
        python3 "$ROOT/scripts/local/product_backup_sample.py" --source "$PRODUCT_BACKUP_SOURCE_URI" \
          --output "$LOCAL_ROOT/backup/sample"
        ;;
      full)
        : "${O4G_LOCAL_FULL_IMPORT_URL:?set the dedicated versioned local importer URL}"
        [[ "$O4G_LOCAL_FULL_IMPORT_URL" == http://localhost:* || "$O4G_LOCAL_FULL_IMPORT_URL" == http://127.0.0.1:* ]] || {
          echo "full import URL must be loopback" >&2; exit 1;
        }
        curl --fail --show-error --silent --request POST "$O4G_LOCAL_FULL_IMPORT_URL"
        ;;
      *) usage ;;
    esac
    ;;
  jobs)
    [ "${2:-}" = run ] && [ -n "${3:-}" ] || usage
    load_env
    run_admin_job "$3"
    ;;
  *) usage ;;
esac
