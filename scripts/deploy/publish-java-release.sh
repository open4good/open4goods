#!/usr/bin/env bash
# Install one verified Java artifact and restore its prior symlink if health fails.
set -euo pipefail

readonly JAVA_SERVICES=(sbadmin api front-api ui b2b-api)

usage() {
  echo "usage: $0 --release SHA --bundle DIRECTORY --service SERVICE --health-url URL [--health-status CODES] [--root DIRECTORY]" >&2
  exit 2
}

release=''
bundle=''
service=''
health_url=''
health_status='200'
root='/srv/open4goods'
health_attempts="${O4G_HEALTH_ATTEMPTS:-24}"
health_delay="${O4G_HEALTH_DELAY_SECONDS:-5}"
while (($#)); do
  case "$1" in
    --release) release="${2:-}"; shift 2 ;;
    --bundle) bundle="${2:-}"; shift 2 ;;
    --service) service="${2:-}"; shift 2 ;;
    --health-url) health_url="${2:-}"; shift 2 ;;
    --health-status) health_status="${2:-}"; shift 2 ;;
    --root) root="${2:-}"; shift 2 ;;
    *) usage ;;
  esac
done

[[ "$release" =~ ^[0-9a-f]{7,64}$ && -d "$bundle" && -n "$health_url" ]] || usage
[[ "$health_attempts" =~ ^[1-9][0-9]*$ && "$health_delay" =~ ^[0-9]+$ ]] || {
  echo 'health retry configuration is invalid' >&2; exit 2;
}
[[ "$health_status" =~ ^[1-5][0-9]{2}(,[1-5][0-9]{2})*$ ]] || {
  echo 'health status configuration is invalid' >&2; exit 2;
}
case " ${JAVA_SERVICES[*]} " in *" ${service} "*) ;; *) echo "unknown Java service: ${service}" >&2; exit 2 ;; esac

manifest="$bundle/release-manifest"
[[ -f "$manifest" ]] || { echo 'release manifest is missing' >&2; exit 2; }
grep -qx "release=${release}" "$manifest" || { echo 'release does not match manifest' >&2; exit 2; }
grep -q '^contract=[A-Za-z0-9._-]\+$' "$manifest" || { echo 'manifest contract is invalid' >&2; exit 2; }

verify_artifact() {
  local artifact="$1" file="$2" expected actual
  expected="$(awk -v name="$artifact" '$1 == "artifact" && $2 == name { print $3 }' "$manifest")"
  [[ "$expected" =~ ^[a-f0-9]{64}$ && -f "$file" ]] || return 1
  actual="$(sha256sum "$file" | awk '{print $1}')"
  [[ "$actual" == "$expected" ]]
}

for name in "${JAVA_SERVICES[@]}"; do
  verify_artifact "$name" "$bundle/${name}.jar" || {
    echo "invalid Java artifact: ${name}" >&2; exit 2;
  }
done
for name in frontend-ssr b2b-frontend; do
  verify_artifact "$name" "$bundle/${name}.tar.gz" || {
    echo "invalid Nuxt artifact: ${name}" >&2; exit 2;
  }
done

lock="$root/deploy.lock"
mkdir -p "$root/releases" "$root/services/$service"
exec 9>"$lock"
flock -x 9

release_dir="$root/releases/$release"
if [[ -e "$release_dir" ]]; then
  [[ -f "$release_dir/release-manifest" ]] || { echo 'existing release is incomplete' >&2; exit 1; }
  cmp -s "$manifest" "$release_dir/release-manifest" || { echo 'release SHA is not immutable' >&2; exit 1; }
else
  stage_dir="${release_dir}.staging.$$"
  trap 'rm -rf -- "$stage_dir"' EXIT
  mkdir -p "$stage_dir"
  cp -- "$manifest" "$stage_dir/"
  for name in "${JAVA_SERVICES[@]}"; do cp -- "$bundle/${name}.jar" "$stage_dir/${name}.jar"; done
  for name in frontend-ssr b2b-frontend; do
    cp -- "$bundle/${name}.tar.gz" "$stage_dir/${name}.tar.gz"
    mkdir "$stage_dir/${name}"
    tar -xzf "$bundle/${name}.tar.gz" -C "$stage_dir/${name}"
  done
  chmod -R a-w "$stage_dir"
  mv -- "$stage_dir" "$release_dir"
  trap - EXIT
fi

service_dir="$root/services/$service"
current="$service_dir/current"
previous=''
[[ -L "$current" ]] && previous="$(readlink "$current")"
new_target="../../releases/${release}"
temporary="$service_dir/.current.${release}.$$"
ln -s "$new_target" "$temporary"
mv -Tf "$temporary" "$current"

rollback() {
  if [[ -n "$previous" ]]; then
    ln -s "$previous" "$temporary"
    mv -Tf "$temporary" "$current"
    systemctl restart "open4goods@${service}.service"
  else
    rm -f -- "$current"
    systemctl stop "open4goods@${service}.service" || true
  fi
}

if ! systemctl restart "open4goods@${service}.service"; then
  rollback
  exit 1
fi
for _ in $(seq 1 "$health_attempts"); do
  http_status="$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 5 "$health_url" || true)"
  if [[ ",$health_status," == *",$http_status,"* ]]; then
    echo "published ${service} release ${release}"
    exit 0
  fi
  sleep "$health_delay"
done

echo "health check failed for ${service}; restoring prior release" >&2
rollback
exit 1
