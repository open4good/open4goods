#!/usr/bin/env bash
# Migrate beta's current Java artifacts to the dedicated systemd runtime one service at a time.
set -euo pipefail

usage() {
  echo "usage: $0 --release SHA --bundle DIRECTORY" >&2
  exit 2
}

release=''
bundle=''
while (($#)); do
  case "$1" in
    --release) release="${2:-}"; shift 2 ;;
    --bundle) bundle="${2:-}"; shift 2 ;;
    *) usage ;;
  esac
done
[[ "$release" =~ ^[0-9a-f]{7,64}$ && -d "$bundle" ]] || usage
[[ "$(id -u)" == '0' ]] || { echo 'bootstrap must run as root' >&2; exit 1; }

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
legacy_launcher='/opt/open4goods/bin/publish-jars.sh'
legacy_config='/opt/open4goods/config/beta'
[[ -x "$legacy_launcher" && -d "$legacy_config" ]] || {
  echo 'beta legacy launcher or configuration is unavailable' >&2; exit 1;
}

if ! id open4goods >/dev/null 2>&1; then
  useradd --system --home-dir /srv/open4goods --create-home --shell /usr/sbin/nologin open4goods
fi
install -d -o open4goods -g open4goods -m 0755 /srv/open4goods /srv/open4goods/releases /srv/open4goods/services
install -d -o open4goods -g open4goods -m 0755 /var/log/open4goods
install -d -o root -g open4goods -m 0750 /etc/open4goods

write_java_environment() {
  local service="$1" jvm_options="$2" spring_options="$3"
  printf 'O4G_JAVA_OPTS="%s %s"\n' "$jvm_options" "$spring_options" > "/etc/open4goods/${service}.env"
  chown root:open4goods "/etc/open4goods/${service}.env"
  chmod 0600 "/etc/open4goods/${service}.env"
}

gc_options() {
  local service="$1"
  printf '%s' "-Xlog:gc*,gc+age=trace,safepoint:file=/var/log/open4goods/gc-${service}.log:utctime,level,pid,tags:filecount=10,filesize=32m"
}

write_java_environment sbadmin \
  "-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:MetaspaceSize=256m -XX:+ExitOnOutOfMemoryError $(gc_options sbadmin)" \
  '-Dspring.config.location=file:/opt/open4goods/config/beta/admin/application-active.yml -Dspring.profiles.active=nudger,beta'
write_java_environment api \
  "-Xms8g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=32m -XX:MetaspaceSize=256m -XX:+AlwaysPreTouch -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/open4goods -XX:+ExitOnOutOfMemoryError $(gc_options api)" \
  '-Dspring.config.location=classpath:/application.yml,file:/opt/open4goods/config/beta/api/application-active.yml -Dspring.profiles.active=nudger,beta'
write_java_environment ui \
  "-Xms2048m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -XX:MetaspaceSize=256m -XX:+ExitOnOutOfMemoryError $(gc_options ui)" \
  '-Dspring.config.location=classpath:/application.yml,file:/opt/open4goods/config/beta/ui/application-active.yml -Dspring.profiles.active=nudger,beta'
write_java_environment front-api \
  "-Xms6g -Xmx6g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -XX:MetaspaceSize=256m -XX:+AlwaysPreTouch -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/open4goods -XX:+ExitOnOutOfMemoryError -Djdk.attach.allowAttachSelf=true $(gc_options front-api)" \
  '-Dspring.config.location=classpath:/application.yml,file:/opt/open4goods/config/beta/front-api/application-active.yml -Dspring.profiles.active=nudger,beta'
write_java_environment b2b-api \
  "-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:MetaspaceSize=128m -XX:+ExitOnOutOfMemoryError $(gc_options b2b-api)" \
  '-Dspring.config.location=classpath:/application.yml,file:/opt/open4goods/config/beta/b2b-api/application-active.yml -Dspring.profiles.active=nudger,beta'

install -m 0600 "$legacy_config/infra/.env" /etc/open4goods/infra.env
for service in frontend b2b-frontend; do
  install -m 0600 "$legacy_config/${service}/.env" "/etc/open4goods/${service}.env"
done
printf '%s\n' 'PORT=3000' 'NODE_OPTIONS=--max-old-space-size=4096' 'TZ=Europe/Paris' \
  'METRIKS_DATA_DIR=/opt/open4goods/metriks-data' >> /etc/open4goods/frontend.env
printf '%s\n' 'PORT=3003' 'NODE_OPTIONS=--max-old-space-size=1536' 'TZ=Europe/Paris' \
  >> /etc/open4goods/b2b-frontend.env
"$repo_root/scripts/deploy/install-systemd-runtime.sh"

activate_java_service() {
  local service="$1" port="$2" statuses="$3"
  if ! "$legacy_launcher" beta stop "$service"; then
    echo "could not stop legacy ${service}" >&2
    return 1
  fi
  if "$repo_root/scripts/deploy/publish-java-release.sh" --release "$release" --bundle "$bundle" \
    --service "$service" --health-url "http://127.0.0.1:${port}/actuator/health" --health-status "$statuses"; then
    return 0
  fi
  systemctl stop "open4goods@${service}.service" || true
  "$legacy_launcher" beta restart "$service" || true
  return 1
}

activate_java_service sbadmin 8085 401
activate_java_service api 8081 401
activate_java_service ui 8082 401
activate_java_service front-api 8086 401,302
activate_java_service b2b-api 8087 200

echo "beta Java services run under systemd; Nuxt migration is staged but deliberately not activated by this bootstrap."
