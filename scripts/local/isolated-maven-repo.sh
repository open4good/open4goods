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

if [ -z "$RUN_ID" ]; then
  echo "isolated-maven-repo.sh: could not determine a run id" >&2
  exit 1
fi

TARGET="${RUN_ROOT}/${RUN_ID}/repository"

if [ ! -d "$TARGET" ]; then
  mkdir -p "$(dirname "$TARGET")"
  if [ -d "$SHARED_M2" ]; then
    cp -a "$SHARED_M2" "$TARGET"
  else
    mkdir -p "$TARGET"
  fi
fi

echo "$TARGET"
