#!/usr/bin/env bash
# Activate a verified Nuxt artifact from an immutable release and roll it back on failed readiness.
set -euo pipefail

usage() {
  echo "usage: $0 --release SHA --service {frontend|b2b-frontend} --health-url URL [--health-status CODES] [--root DIRECTORY]" >&2
  exit 2
}

release=''
service=''
health_url=''
health_status='200'
root='/srv/open4goods'
health_attempts="${O4G_HEALTH_ATTEMPTS:-24}"
health_delay="${O4G_HEALTH_DELAY_SECONDS:-5}"
while (($#)); do
  case "$1" in
    --release) release="${2:-}"; shift 2 ;;
    --service) service="${2:-}"; shift 2 ;;
    --health-url) health_url="${2:-}"; shift 2 ;;
    --health-status) health_status="${2:-}"; shift 2 ;;
    --root) root="${2:-}"; shift 2 ;;
    *) usage ;;
  esac
done

[[ "$release" =~ ^[0-9a-f]{7,64}$ && -n "$health_url" ]] || usage
[[ "$health_attempts" =~ ^[1-9][0-9]*$ && "$health_delay" =~ ^[0-9]+$ ]] || {
  echo 'health retry configuration is invalid' >&2; exit 2;
}
[[ "$health_status" =~ ^[1-5][0-9]{2}(,[1-5][0-9]{2})*$ ]] || {
  echo 'health status configuration is invalid' >&2; exit 2;
}
case "$service" in
  frontend) artifact='frontend-ssr' ;;
  b2b-frontend) artifact='b2b-frontend' ;;
  *) echo "unknown Nuxt service: ${service}" >&2; exit 2 ;;
esac

release_dir="$root/releases/$release"
manifest="$release_dir/release-manifest"
[[ -d "$release_dir/$artifact" && -f "$manifest" ]] || {
  echo "release ${release} does not contain ${artifact}" >&2; exit 1;
}
grep -qx "release=${release}" "$manifest" || { echo 'release does not match manifest' >&2; exit 1; }
expected="$(awk -v name="$artifact" '$1 == "artifact" && $2 == name { print $3 }' "$manifest")"
[[ "$expected" =~ ^[a-f0-9]{64}$ ]] || { echo 'manifest artifact checksum is invalid' >&2; exit 1; }
actual="$(sha256sum "$release_dir/${artifact}.tar.gz" | awk '{print $1}')"
[[ "$actual" == "$expected" ]] || { echo 'release artifact checksum does not match' >&2; exit 1; }

mkdir -p "$root/services/$service"
exec 9>"$root/deploy.lock"
flock -x 9

service_dir="$root/services/$service"
current="$service_dir/current"
previous=''
[[ -L "$current" ]] && previous="$(readlink "$current")"
temporary="$service_dir/.current.${release}.$$"
ln -s "../../releases/${release}/${artifact}" "$temporary"
mv -Tf "$temporary" "$current"

rollback() {
  if [[ -n "$previous" ]]; then
    ln -s "$previous" "$temporary"
    mv -Tf "$temporary" "$current"
    systemctl restart "open4goods-nuxt@${service}.service"
  else
    rm -f -- "$current"
    systemctl stop "open4goods-nuxt@${service}.service" || true
  fi
}

if ! systemctl restart "open4goods-nuxt@${service}.service"; then
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
