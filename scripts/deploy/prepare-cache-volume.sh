#!/usr/bin/env bash
# Prepare the only allowed beta cache mount source without exposing cache entries.
set -euo pipefail

readonly cache_source='/diskb/open4goods-cache'
readonly cache_target='/opt/open4goods/.cached'

[[ "$(id -u)" == '0' ]] || { echo 'cache-volume preparation must run as root' >&2; exit 1; }
id open4goods >/dev/null 2>&1 || { echo 'open4goods system user is unavailable' >&2; exit 1; }
findmnt --mountpoint /diskb >/dev/null || { echo 'DiskB is not mounted at /diskb' >&2; exit 1; }

install -d -o open4goods -g open4goods -m 0750 "$cache_source"
install -d -o open4goods -g open4goods -m 0750 "$cache_target"

diskb_device="$(findmnt --noheadings --output MAJ:MIN --target /diskb | tr -d '[:space:]')"
source_device="$(findmnt --noheadings --output MAJ:MIN --target "$cache_source" | tr -d '[:space:]')"
[[ -n "$diskb_device" && "$diskb_device" == "$source_device" ]] || {
  echo 'cache source is not backed by DiskB' >&2; exit 1;
}

echo 'prepared DiskB cache source; install and start opt-open4goods-.cached.mount before restarting cache writers'
