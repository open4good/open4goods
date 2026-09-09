#!/usr/bin/env bash
# Controlled local front-api + Nuxt launcher for autonomous read-only recette.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARTIFACT_ROOT="$ROOT/artifacts/recette"
API_PORT="${NUDGER_RECETTE_API_PORT:-8086}"
FRONT_PORT="${NUDGER_RECETTE_FRONT_PORT:-3000}"

usage() {
  echo "usage: $0 <start|status|offsets|logs|stop> --run <run-id>" >&2
  exit 2
}

command_name="${1:-}"
shift || true
run_id=""
while [ "$#" -gt 0 ]; do
  case "$1" in
    --run) run_id="${2:-}"; shift 2 ;;
    *) usage ;;
  esac
done

case "$run_id" in
  ""|*[!A-Za-z0-9._-]*) echo "invalid --run value" >&2; exit 2 ;;
esac

run_dir="$ARTIFACT_ROOT/$run_id"
api_log="$run_dir/front-api.log"
front_log="$run_dir/frontend-ssr.log"

pid_file() { echo "$run_dir/$1.pid"; }

alive() {
  local service="$1" file pid
  file="$(pid_file "$service")"
  [ -f "$file" ] || return 1
  read -r pid < "$file"
  [[ "$pid" =~ ^[0-9]+$ ]] || return 1
  kill -0 "$pid" 2>/dev/null
}

wait_url() {
  local name="$1" url="$2" log="$3" pid="$4"
  for _ in $(seq 1 120); do
    if curl --silent --show-error --fail --max-time 3 "$url" >/dev/null 2>&1; then
      return 0
    fi
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "$name stopped before readiness; inspect $log" >&2
      return 1
    fi
    sleep 1
  done
  echo "$name did not become ready; inspect $log" >&2
  return 1
}

start_service() {
  local name="$1" directory="$2" log="$3"
  shift 3
  if alive "$name"; then
    echo "$name is already running" >&2
    return 1
  fi
  (
    cd "$directory"
    exec setsid "$@" >>"$log" 2>&1
  ) &
  echo "$!" > "$(pid_file "$name")"
}

stop_service() {
  local name="$1" file pid sid
  file="$(pid_file "$name")"
  [ -f "$file" ] || return 0
  read -r pid < "$file"
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
    sid="$(ps -o sid= -p "$pid" | tr -d ' ')"
    if [ "$sid" = "$pid" ]; then
      kill -- "-$pid"
    else
      kill "$pid"
    fi
    for _ in $(seq 1 20); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 0.25
    done
  fi
  rm -f -- "$file"
}

case "$command_name" in
  start)
    mkdir -p "$run_dir"
    if [ ! -f "$ROOT/front-api/src/main/resources/application-devsec.yml" ]; then
      echo "missing ignored front-api application-devsec.yml; refusing to search for credentials" >&2
      exit 1
    fi
    if [ ! -d "$ROOT/frontend/node_modules" ]; then
      echo "frontend dependencies are absent; run pnpm install explicitly before recette" >&2
      exit 1
    fi
    : > "$api_log"
    : > "$front_log"
    start_service api "$ROOT/front-api" "$api_log" \
      mvn --offline spring-boot:run \
      -Dspring-boot.run.profiles=devsec,local \
      "-Dspring-boot.run.arguments=--server.port=$API_PORT"
    api_pid="$(<"$(pid_file api)")"
    if ! wait_url "front-api" "http://127.0.0.1:$API_PORT/actuator/health" "$api_log" "$api_pid"; then
      stop_service api
      exit 1
    fi
    start_service frontend "$ROOT/frontend" "$front_log" \
      env API_URL="http://127.0.0.1:$API_PORT" PUBLIC_API_URL="http://127.0.0.1:$API_PORT" \
      pnpm dev --host 127.0.0.1 --port "$FRONT_PORT"
    front_pid="$(<"$(pid_file frontend)")"
    if ! wait_url "frontend" "http://127.0.0.1:$FRONT_PORT/" "$front_log" "$front_pid"; then
      stop_service frontend
      stop_service api
      exit 1
    fi
    "$0" offsets --run "$run_id" >/dev/null
    echo "STACK READY: run=$run_id url=http://127.0.0.1:$FRONT_PORT"
    ;;
  status)
    for service in api frontend; do
      if alive "$service"; then
        echo "$service RUNNING pid=$(<"$(pid_file "$service")")"
      else
        echo "$service STOPPED"
      fi
    done
    if curl --silent --show-error --fail --max-time 3 \
      "http://127.0.0.1:$API_PORT/actuator/health" >/dev/null; then
      echo "front-api READY"
    fi
    if curl --silent --show-error --fail --max-time 3 \
      "http://127.0.0.1:$FRONT_PORT/" >/dev/null; then
      echo "frontend READY"
    fi
    ;;
  offsets)
    mkdir -p "$run_dir"
    api_size="$(wc -c < "$api_log" 2>/dev/null || echo 0)"
    front_size="$(wc -c < "$front_log" 2>/dev/null || echo 0)"
    printf '{"front-api.log":%s,"frontend-ssr.log":%s}\n' "$api_size" "$front_size" \
      > "$run_dir/offsets.json"
    cat "$run_dir/offsets.json"
    ;;
  logs)
    tail -n 100 "$api_log" "$front_log"
    ;;
  stop)
    stop_service frontend
    stop_service api
    echo "STACK STOPPED: run=$run_id artifacts=$run_dir"
    ;;
  *) usage ;;
esac
