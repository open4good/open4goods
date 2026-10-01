#!/usr/bin/env bash
# Bootstrap a per-run Maven local repository on the shared build host, so one
# agent's `mvn install` of a SNAPSHOT artifact cannot overwrite the jar another
# agent is compiling or testing against (see GOU-142 / GOU-168).
#
# Usage:
#   M2_REPO="$(scripts/local/isolated-maven-repo.sh GOU-168)"
#   mvn -Dmaven.repo.local="$M2_REPO" -pl b2b-api -am test
#
# The run/issue identifier is the only argument; it defaults to
# PAPERCLIP_RUN_ID, then PAPERCLIP_TASK_ID, then the current branch name, so a
# bare call still isolates a run that forgets to pass one explicitly.
#
# IMPORTANT: this does a real copy (`cp -a`), not a hard-link bootstrap
# (`cp -al`). A hard-linked artifact still shares one inode with the shared
# cache, and `mvn install` overwrites an existing artifact file in place
# (verified against this host's Maven 3.8.7 / maven-install-plugin: the
# shared ~/.m2 jar's mtime and bytes changed after installing through a
# hard-linked private repo). Hard links therefore reproduce the exact
# corruption this script exists to prevent; do not "optimize" this to -al.
set -euo pipefail

SHARED_M2="${O4G_SHARED_M2_REPOSITORY:-${HOME}/.m2/repository}"
RUN_ROOT="${O4G_LOCAL_M2_ROOT:-${HOME}/.local/state/open4goods-m2}"
RUN_ID="${1:-${PAPERCLIP_RUN_ID:-${PAPERCLIP_TASK_ID:-$(git branch --show-current 2>/dev/null || echo default)}}}"
# Nobody deletes a run's isolated repository when the run dies without
# cleaning up (the normal case, not the rare one: GOU-175). So every
# bootstrap purges run directories whose marker is older than this, and
# refuses outright when the host is too low on space to add another ~1.8GB
# copy, rather than letting a shared beta-serving host fill its partition.
MAX_AGE_HOURS="${O4G_LOCAL_M2_MAX_AGE_HOURS:-24}"
MIN_FREE_GIB="${O4G_LOCAL_M2_MIN_FREE_GIB:-20}"

if [ -z "$RUN_ID" ]; then
  echo "isolated-maven-repo.sh: could not determine a run id" >&2
  exit 1
fi

# Freshness is tracked by a marker file, not the copied tree's own mtimes:
# `cp -a` preserves the shared repository's original timestamps, so the
# repository tree itself is "old" the instant it is created.
purge_stale_repos() {
  local root="$1" max_age_minutes="$2" dir marker ref
  [ -d "$root" ] || return 0
  for dir in "$root"/*/; do
    [ -d "$dir" ] || continue
    dir="${dir%/}"
    marker="$dir/.last-used"
    ref="$marker"
    [ -e "$ref" ] || ref="$dir"
    if [ -n "$(find "$ref" -maxdepth 0 -mmin "+${max_age_minutes}" 2>/dev/null)" ]; then
      rm -rf "$dir"
    fi
  done
}

check_free_space() {
  local path="$1" min_gib="$2" avail_kib avail_gib
  avail_kib="$(df -Pk "$path" | awk 'NR==2 {print $4}')"
  avail_gib=$((avail_kib / 1024 / 1024))
  if [ "$avail_gib" -lt "$min_gib" ]; then
    echo "isolated-maven-repo.sh: only ${avail_gib}GiB free under $path (need >= ${min_gib}GiB free before bootstrapping another isolated repository); refusing to start. Free space under $path or lower O4G_LOCAL_M2_MIN_FREE_GIB if this is expected." >&2
    exit 1
  fi
}

mkdir -p "$RUN_ROOT"
purge_stale_repos "$RUN_ROOT" "$((MAX_AGE_HOURS * 60))"
check_free_space "$RUN_ROOT" "$MIN_FREE_GIB"

TARGET="${RUN_ROOT}/${RUN_ID}/repository"

if [ ! -d "$TARGET" ]; then
  mkdir -p "$(dirname "$TARGET")"
  if [ -d "$SHARED_M2" ]; then
    cp -a "$SHARED_M2" "$TARGET"
  else
    mkdir -p "$TARGET"
  fi
fi

# Mark this run id as active so a later bootstrap's purge pass does not
# reclaim it out from under a still-running build.
touch "${RUN_ROOT}/${RUN_ID}/.last-used"

echo "$TARGET"
