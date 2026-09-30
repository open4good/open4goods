#!/usr/bin/env bash
# Install the Java service units after validating syntax and environment-file modes.
set -euo pipefail

usage() {
  echo "usage: $0 [--unit-dir DIRECTORY] [--environment-dir DIRECTORY]" >&2
  exit 2
}

unit_dir='/etc/systemd/system'
environment_dir='/etc/open4goods'
while (($#)); do
  case "$1" in
    --unit-dir) unit_dir="${2:-}"; shift 2 ;;
    --environment-dir) environment_dir="${2:-}"; shift 2 ;;
    *) usage ;;
  esac
done

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
for service in sbadmin api front-api ui b2b-api frontend b2b-frontend; do
  environment_file="$environment_dir/${service}.env"
  [[ -f "$environment_file" ]] || { echo "missing ${environment_file}" >&2; exit 1; }
  [[ "$(stat -c '%a' "$environment_file")" == '600' ]] || {
    echo "${environment_file} must have mode 0600" >&2; exit 1;
  }
done

install -D -m 0644 "$repo_root/ops/systemd/open4goods@.service" "$unit_dir/open4goods@.service"
install -D -m 0644 "$repo_root/ops/systemd/open4goods-nuxt@.service" "$unit_dir/open4goods-nuxt@.service"
install -D -m 0644 "$repo_root/ops/systemd/open4goods.target" "$unit_dir/open4goods.target"
install -D -m 0644 "$repo_root/ops/systemd/opt-open4goods-.cached.mount" \
  "$unit_dir/opt-open4goods-.cached.mount"
systemd-analyze verify "$unit_dir/open4goods@.service" "$unit_dir/open4goods-nuxt@.service" \
  "$unit_dir/open4goods.target" "$unit_dir/opt-open4goods-.cached.mount"
systemctl daemon-reload
systemctl enable opt-open4goods-.cached.mount
systemctl enable open4goods.target
