#!/usr/bin/env bash
# Hybrid local runtime: persistent Docker infrastructure, native Java/Nuxt applications.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOCAL_ROOT="$ROOT/.local"
ENV_FILE="$ROOT/.env.local"
TEMPLATE_ROOT="$ROOT/ops/local/config"
SERVICES=(api ui admin front-api b2b-api exposed-docs geocode frontend b2b-frontend)
INFRA_PORT_VARS=(O4G_PORT_ELASTICSEARCH O4G_PORT_REDIS O4G_PORT_POSTGRES O4G_PORT_KIBANA O4G_PORT_XWIKI)

# A worktree checkout is per-agent and disposable; the shared-host lock has to
# survive independently of which worktree a given invocation runs from, so it
# lives under the invoking user's home, not under $ROOT/.local.
SHARED_LOCK_ROOT="${O4G_SHARED_HOST_LOCK_DIR:-${XDG_STATE_HOME:-$HOME/.local/state}/open4goods-shared-host}"

usage() {
  cat >&2 <<'EOF'
usage: scripts/local/open4goods.sh <command>
  init | doctor | preflight | up | restart <service> | status | logs [service] | down
  backup verify | xwiki import | data sample | data full | jobs run <job>

set O4G_SHARED_HOST=1 to gate 'up' behind the shared-build-host preflight
(rootless-only Docker, assigned port block, disk/RAM/CPU/pids budget); see
docs/operations/buildhost-runtime.md and .env.buildhost.example.

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
  local service="$1" module="$2" port="$3" config="$O4G_LOCAL_CONFIG_DIR/$1.yml" java21
  java21="$(java_home_21)" || exit 1
  start_process "$service" "$ROOT/$module" env \
    JAVA_HOME="$java21" \
    SPRING_PROFILES_ACTIVE=local \
    SPRING_CONFIG_ADDITIONAL_LOCATION="optional:file:$config" \
    SERVER_PORT="$port" \
    mvn --offline spring-boot:run \
      -Dspring-boot.run.jvmArguments=--add-opens=java.base/java.math=ALL-UNNAMED
}

java_home_21() {
  # pom.xml pins java.version=21; mvn forks spring-boot:run under whatever
  # JAVA_HOME points at, which can be a newer JDK on a shared build host.
  # Never fall back to an unverified JAVA_HOME: a JDK 25 default (as measured
  # on this host) would silently run the reactor under the wrong JDK.
  local candidate
  if [ -n "${JAVA_HOME:-}" ] && "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"21\.'; then
    printf '%s\n' "$JAVA_HOME"
    return 0
  fi
  for candidate in /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/java-21-openjdk; do
    if [ -x "$candidate/bin/java" ] && "$candidate/bin/java" -version 2>&1 | grep -q '"21\.'; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  echo "no verified JDK 21 found: JAVA_HOME=${JAVA_HOME:-unset} is not a JDK 21 and neither" \
    "/usr/lib/jvm/java-21-openjdk-amd64 nor /usr/lib/jvm/java-21-openjdk resolves to one." \
    "Install a JDK 21 and either export JAVA_HOME to it or place it at one of those paths." >&2
  return 1
}

port_for() {
  # Every port has a literal fallback so the strict-local contract test can
  # still find the historical numbers; a buildhost profile overrides them
  # via .env.buildhost (see docs/operations/buildhost-runtime.md).
  case "$1" in
    api) echo "${O4G_PORT_API:-8081}" ;;
    ui) echo "${O4G_PORT_UI:-8082}" ;;
    admin) echo "${O4G_PORT_ADMIN:-8085}" ;;
    front-api) echo "${O4G_PORT_FRONT_API:-8086}" ;;
    b2b-api) echo "${O4G_PORT_B2B_API:-8087}" ;;
    exposed-docs) echo "${O4G_PORT_EXPOSED_DOCS:-8088}" ;;
    geocode) echo "${O4G_PORT_GEOCODE:-8089}" ;;
    frontend) echo "${O4G_PORT_FRONTEND:-3000}" ;;
    b2b-frontend) echo "${O4G_PORT_B2B_FRONTEND:-3001}" ;;
  esac
}

start_native() {
  local service="$1" api_port ui_port front_api_port b2b_api_port frontend_port b2b_frontend_port java21
  api_port="$(port_for api)"; ui_port="$(port_for ui)"; front_api_port="$(port_for front-api)"
  b2b_api_port="$(port_for b2b-api)"; frontend_port="$(port_for frontend)"; b2b_frontend_port="$(port_for b2b-frontend)"
  case "$service" in
    api) start_java api api "$api_port" ;;
    ui) start_java ui ui "$ui_port" ;;
    admin) start_java admin admin "$(port_for admin)" ;;
    front-api) start_java front-api front-api "$front_api_port" ;;
    b2b-api)
      java21="$(java_home_21)" || exit 1
      start_process b2b-api "$ROOT/b2b-api" env JAVA_HOME="$java21" \
        SPRING_PROFILES_ACTIVE=local B2B_API_PORT="$b2b_api_port" \
        SPRING_CONFIG_ADDITIONAL_LOCATION="optional:file:$O4G_LOCAL_CONFIG_DIR/b2b-api.yml" \
        mvn --offline spring-boot:run \
          -Dspring-boot.run.jvmArguments=--add-opens=java.base/java.math=ALL-UNNAMED
      ;;
    exposed-docs) start_java exposed-docs services/exposed-docs "$(port_for exposed-docs)" ;;
    geocode) start_java geocode services/geocode "$(port_for geocode)" ;;
    frontend)
      start_process frontend "$ROOT/frontend" env \
        API_URL="http://localhost:$front_api_port" PUBLIC_API_URL="http://localhost:$front_api_port" \
        STATIC_SERVER="http://localhost:$ui_port" \
        SITE_URL="http://localhost:$frontend_port" SITEMAP_BASE_PATH="$O4G_LOCAL_DATA_ROOT/sitemap" \
        NUXT_MACHINE_TOKEN="${FRONT_SECURITY_SHARED_TOKEN:-CHANGE_ME_SHARED_TOKEN}" \
        pnpm dev --host 127.0.0.1 --port "$frontend_port"
      ;;
    b2b-frontend)
      start_process b2b-frontend "$ROOT/b2b-frontend" env \
        NUXT_PUBLIC_BACKEND_BASE_URL="http://localhost:$b2b_api_port" \
        NUXT_PUBLIC_ROUTER_BASE_URL="http://localhost:$b2b_api_port" \
        NUXT_PUBLIC_SITE_URL="http://localhost:$b2b_frontend_port" \
        BACKEND_OPENAPI_URL="http://localhost:$b2b_api_port/v3/api-docs" \
        NUXT_BACKEND_OPEN_API_URL="http://localhost:$b2b_api_port/v3/api-docs" \
        NUXT_OIDC_GOOGLE_REDIRECT_URI="http://localhost:$b2b_frontend_port/auth/callback/google" \
        pnpm dev --host 127.0.0.1 --port "$b2b_frontend_port"
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
  java_home_21 >/dev/null || missing=1
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

cgroup_self_dir() { printf '/sys/fs/cgroup%s\n' "$(awk -F: '{print $3}' /proc/self/cgroup 2>/dev/null)"; }

cgroup_effective_max() {
  # cgroup v2 enforces the minimum numeric limit across the whole ancestor
  # chain; a shared build host sets it above our session scope, not on it.
  # Returns "max" only when every ancestor is unbounded for this file.
  local file="$1" dir value best=""
  dir="$(cgroup_self_dir)"
  while [ -n "$dir" ] && [ "$dir" != "/sys/fs/cgroup" ] && [ "$dir" != "/" ]; do
    if [ -r "$dir/$file" ]; then
      value="$(awk '{print $1}' "$dir/$file" 2>/dev/null)"
      if [ -n "$value" ] && [ "$value" != max ] && { [ -z "$best" ] || [ "$value" -lt "$best" ]; }; then
        best="$value"
      fi
    fi
    dir="$(dirname "$dir")"
  done
  printf '%s\n' "${best:-max}"
}

cgroup_effective_headroom_bytes() {
  # Smallest (max - current) across the ancestor chain, at whichever level
  # is actually bounded; a declared quota means nothing if other tenants
  # under the same ancestor have already consumed it.
  local max_file="$1" current_file="$2" dir max_value cur_value headroom best=""
  dir="$(cgroup_self_dir)"
  while [ -n "$dir" ] && [ "$dir" != "/sys/fs/cgroup" ] && [ "$dir" != "/" ]; do
    if [ -r "$dir/$max_file" ] && [ -r "$dir/$current_file" ]; then
      max_value="$(awk '{print $1}' "$dir/$max_file" 2>/dev/null)"
      cur_value="$(cat "$dir/$current_file" 2>/dev/null)"
      if [ -n "$max_value" ] && [ "$max_value" != max ] && [ -n "$cur_value" ]; then
        headroom=$(( max_value - cur_value ))
        if [ -z "$best" ] || [ "$headroom" -lt "$best" ]; then best="$headroom"; fi
      fi
    fi
    dir="$(dirname "$dir")"
  done
  printf '%s\n' "${best:-max}"
}

cgroup_cpu_millicores_max() {
  # quota/period must be read from the SAME ancestor level: a leaf period
  # paired with a raw min-quota from a different ancestor is not a real
  # ratio. Compute millicores per level, then take the min across levels
  # that are actually bounded.
  local dir quota period millicores best="" any_bounded=0
  dir="$(cgroup_self_dir)"
  while [ -n "$dir" ] && [ "$dir" != "/sys/fs/cgroup" ] && [ "$dir" != "/" ]; do
    if [ -r "$dir/cpu.max" ]; then
      read -r quota period < "$dir/cpu.max"
      if [ "$quota" != max ] && [ -n "${period:-}" ] && [ "$period" -gt 0 ] 2>/dev/null; then
        any_bounded=1
        millicores=$(( quota * 1000 / period ))
        if [ -z "$best" ] || [ "$millicores" -lt "$best" ]; then best="$millicores"; fi
      fi
    fi
    dir="$(dirname "$dir")"
  done
  if [ "$any_bounded" -eq 0 ]; then printf 'max\n'; else printf '%s\n' "$best"; fi
}

# realpath(1) may be absent; readlink -f is POSIX-adjacent and present on
# every host this script targets.
resolved_path() { readlink -f -- "$1" 2>/dev/null || printf '%s\n' "$1"; }

# preflight() is the opt-in shared-build-host gate (O4G_SHARED_HOST=1). It
# never mutates beta/prod state; it only refuses to start the local stack
# when isolation or capacity assumptions do not hold. See
# docs/operations/buildhost-runtime.md for the budget this enforces.
# preflight full-import applies a stricter disk floor: the standard 20GiB
# default only qualifies the native stack idling, not a full backup import.
preflight() {
  load_env
  local mode="${1:-standard}"
  local missing=0 service port mem_max mem_headroom cpu_millicores pids_max
  local port_min="${O4G_SHARED_HOST_PORT_MIN:-4100}" port_max="${O4G_SHARED_HOST_PORT_MAX:-4109}"
  local infra_port_min="${O4G_SHARED_HOST_INFRA_PORT_MIN:-4150}" infra_port_max="${O4G_SHARED_HOST_INFRA_PORT_MAX:-4159}"
  local min_free_gib="${O4G_SHARED_HOST_MIN_FREE_GIB:-20}"
  if [ "$mode" = full-import ]; then
    min_free_gib="${O4G_SHARED_HOST_MIN_FREE_GIB_FULL_IMPORT:-60}"
  fi
  local min_mem_gib="${O4G_SHARED_HOST_MIN_MEM_GIB:-8}" min_cpu="${O4G_SHARED_HOST_MIN_CPU:-2}"
  local min_pids="${O4G_SHARED_HOST_MIN_PIDS:-512}"
  local approved_roots="${O4G_SHARED_HOST_APPROVED_ROOTS:-$LOCAL_ROOT:$HOME/.local/share/open4goods}"

  if ! docker info --format '{{json .SecurityOptions}}' 2>/dev/null | grep -q rootless; then
    echo "rootful or unreachable Docker daemon rejected; DOCKER_HOST must target the rootless daemon" >&2
    missing=1
  fi

  declare -A seen_ports=()
  for service in "${SERVICES[@]}"; do
    port="$(port_for "$service")"
    if [ "$port" -lt "$port_min" ] || [ "$port" -gt "$port_max" ]; then
      echo "application port outside assigned shared-host block ($port_min-$port_max): $service=$port" >&2
      missing=1
    fi
    if [ -n "${seen_ports[$port]:-}" ]; then
      echo "duplicate port $port assigned to both ${seen_ports[$port]} and $service" >&2
      missing=1
    fi
    seen_ports[$port]="$service"
    if ss -Htln "sport = :$port" 2>/dev/null | grep -q LISTEN; then
      echo "application port already in use: $service=$port" >&2
      missing=1
    fi
  done

  local infra_var infra_port
  for infra_var in "${INFRA_PORT_VARS[@]}"; do
    infra_port="${!infra_var:-}"
    [ -n "$infra_port" ] || { echo "incomplete config: $infra_var is unset" >&2; missing=1; continue; }
    if [ "$infra_port" -lt "$infra_port_min" ] || [ "$infra_port" -gt "$infra_port_max" ]; then
      echo "infra port outside assigned shared-host block ($infra_port_min-$infra_port_max): $infra_var=$infra_port" >&2
      missing=1
    fi
    if [ -n "${seen_ports[$infra_port]:-}" ]; then
      echo "duplicate port $infra_port assigned to both ${seen_ports[$infra_port]} and $infra_var" >&2
      missing=1
    fi
    seen_ports[$infra_port]="$infra_var"
    if ss -Htln "sport = :$infra_port" 2>/dev/null | grep -q LISTEN; then
      echo "infra port already in use: $infra_var=$infra_port" >&2
      missing=1
    fi
  done

  local label path real root root_real within
  for label in O4G_LOCAL_DATA_ROOT O4G_LOCAL_CONFIG_DIR PRODUCT_BACKUP_SOURCE_URI; do
    path="${!label}"
    real="$(resolved_path "$path")"
    within=1
    IFS=':' read -ra _approved <<< "$approved_roots"
    for root in "${_approved[@]}"; do
      root_real="$(resolved_path "$root")"
      case "$real" in
        "$root_real"|"$root_real"/*) within=0 ;;
      esac
    done
    if [ "$within" -ne 0 ]; then
      echo "$label resolves outside the approved shared-host roots ($approved_roots): $real" >&2
      missing=1
    fi
  done

  # Container UIDs can make data unreadable to Paperclip's workspace restore.
  # Keep all Docker bind mounts outside the issue worktree on shared hosts.
  real="$(resolved_path "$O4G_LOCAL_DATA_ROOT")"
  root_real="$(resolved_path "$ROOT")"
  case "$real" in
    "$root_real"|"$root_real"/*)
      echo "O4G_LOCAL_DATA_ROOT must be outside the issue worktree: $real" >&2
      missing=1
      ;;
    *GOU-REPLACE*)
      echo "replace GOU-REPLACE with the active issue ID in O4G_LOCAL_DATA_ROOT" >&2
      missing=1
      ;;
  esac

  # A workspace-restore copy of a worktree loses its .git pointer file and
  # silently resolves to this repo: forbid any git worktree registered inside
  # the repo root so one never exists to be copied. Additional worktrees go
  # under the per-issue persistent path (AGENTS.md), never under $ROOT.
  if git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    local wt_path wt_real
    while IFS= read -r wt_path; do
      [ -n "$wt_path" ] || continue
      wt_real="$(resolved_path "$wt_path")"
      case "$wt_real" in
        "$root_real") ;;
        "$root_real"/*)
          echo "git worktree found inside the repo root (forbidden): $wt_real" >&2
          missing=1
          ;;
      esac
    done < <(git -C "$ROOT" worktree list --porcelain 2>/dev/null | awk '/^worktree /{print substr($0,10)}')
  fi

  local variable
  for variable in O4G_LOCAL_POSTGRES_PASSWORD O4G_LOCAL_XWIKI_DB_PASSWORD O4G_LOCAL_XWIKI_ROOT_PASSWORD \
    FRONT_SECURITY_SHARED_TOKEN FRONT_SECURITY_JWT_SECRET B2B_JWT_SECRET O4G_LOCAL_ADMIN_KEY; do
    value="${!variable:-}"
    if [ -z "$value" ] || [[ "$value" == CHANGE_ME* ]]; then
      echo "incomplete config: $variable is unset or still a CHANGE_ME placeholder" >&2
      missing=1
    fi
  done

  local avail_kb
  avail_kb="$(df -Pk "$O4G_LOCAL_DATA_ROOT" 2>/dev/null | awk 'NR==2{print $4}')"
  if [ -z "$avail_kb" ] || [ "$((avail_kb / 1024 / 1024))" -lt "$min_free_gib" ]; then
    echo "insufficient free disk under O4G_LOCAL_DATA_ROOT (need >= ${min_free_gib}GiB, mode=$mode)" >&2
    missing=1
  fi

  mem_max="$(cgroup_effective_max memory.max)"
  if [ "$mem_max" = max ]; then
    echo "cgroup memory limit is unbounded across the whole ancestor chain; refused on a shared host" >&2
    missing=1
  elif [ "$((mem_max / 1024 / 1024 / 1024))" -lt "$min_mem_gib" ]; then
    echo "cgroup memory budget below ${min_mem_gib}GiB" >&2
    missing=1
  else
    mem_headroom="$(cgroup_effective_headroom_bytes memory.max memory.current)"
    if [ "$mem_headroom" != max ] && [ "$((mem_headroom / 1024 / 1024 / 1024))" -lt "$min_mem_gib" ]; then
      echo "insufficient memory headroom under current cgroup usage (need >= ${min_mem_gib}GiB free, not just quota)" >&2
      missing=1
    fi
  fi

  cpu_millicores="$(cgroup_cpu_millicores_max)"
  if [ "$cpu_millicores" = max ]; then
    echo "cgroup CPU limit is unbounded across the whole ancestor chain; refused on a shared host" >&2
    missing=1
  elif [ "$cpu_millicores" -lt "$((min_cpu * 1000))" ]; then
    echo "cgroup CPU budget below ${min_cpu} cores" >&2
    missing=1
  fi

  pids_max="$(cgroup_effective_max pids.max)"
  if [ "$pids_max" = max ]; then
    echo "cgroup pids limit is unbounded across the whole ancestor chain; refused on a shared host" >&2
    missing=1
  elif [ "$pids_max" -lt "$min_pids" ]; then
    echo "cgroup pids budget below ${min_pids} tasks" >&2
    missing=1
  fi

  [ "$missing" -eq 0 ] || return 1
  echo "shared-host preflight ($mode) passed: ports ${port_min}-${port_max}/${infra_port_min}-${infra_port_max}, disk>=${min_free_gib}GiB, mem>=${min_mem_gib}GiB, cpu>=${min_cpu}, pids>=${min_pids}"
}

# Heavy operations (full stack up, full data import) are serialized across
# concurrent agents on the shared host with a non-blocking flock. The lock
# lives under SHARED_LOCK_ROOT (the user's home, not $ROOT/.local) because
# $ROOT is a disposable per-agent worktree: a lock inside it would never be
# seen by a second worktree and would not serialize anything. The importer
# call this guards (POST /backup/products/import) is synchronous on the
# server side -- BackupService.importProducts() blocks the request thread
# until its internal ExecutorService terminates -- so holding the flock for
# the duration of the blocking curl call already covers the job's actual
# completion, not just its submission.
with_full_run_lock() {
  local lock_dir="$SHARED_LOCK_ROOT"
  mkdir -p "$lock_dir"
  exec 9>"$lock_dir/full-run.lock"
  if ! flock -n 9; then
    echo "another full run (up/data full) holds the shared-host lock ($lock_dir/full-run.lock); retry later" >&2
    return 1
  fi
  "$@"
}

full_up() {
  docker compose --env-file "$ENV_FILE" up --detach --wait elasticsearch redis postgres
  local service
  for service in "${SERVICES[@]}"; do start_native "$service"; done
}

status() {
  local service port path
  docker compose ps
  for service in "${SERVICES[@]}"; do
    port="$(port_for "$service")"
    case "$service" in
      frontend|b2b-frontend) path=/ ;;
      *) path=/actuator/health ;;
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
  preflight) preflight "${2:-standard}" ;;
  up)
    init_local
    doctor
    [ "${O4G_SHARED_HOST:-0}" = 1 ] && preflight
    with_full_run_lock full_up
    status
    ;;
  restart)
    load_env
    service="${2:-}"
    is_service "$service" || usage
    [ "${O4G_SHARED_HOST:-0}" = 1 ] && preflight
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
      "http://127.0.0.1:${O4G_PORT_XWIKI:-8080}/xwiki/bin/import/XWiki/XWikiPreferences" >/dev/null
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
        [ "${O4G_SHARED_HOST:-0}" = 1 ] && preflight full-import
        with_full_run_lock curl --fail --show-error --silent --request POST "$O4G_LOCAL_FULL_IMPORT_URL"
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
