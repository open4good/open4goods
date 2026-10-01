#!/usr/bin/env bash
# Fail loud when the local default branch carries commits the remote lacks.
#
# In the shared build-host workspace, a cross-run sync mechanism can leave
# unmerged branch tips merged into the local default branch (see GOU-159).
# Any branch created from a contaminated default branch silently carries
# foreign, unreviewed commits into its PR. Run this before creating a branch.
set -euo pipefail

repo_root="${1:-$(pwd)}"
remote="${O4G_SYNC_REMOTE:-origin}"
branch="${O4G_SYNC_BRANCH:-main}"

if [[ "${O4G_SKIP_FETCH:-0}" != "1" ]]; then
  git -C "${repo_root}" fetch "${remote}" "${branch}" --quiet
fi

ahead="$(git -C "${repo_root}" rev-list --count "${remote}/${branch}..${branch}")"

if [[ "${ahead}" -ne 0 ]]; then
  echo "branch sync guard rejected: local '${branch}' is ${ahead} commit(s) ahead of '${remote}/${branch}'" >&2
  echo "local-only commits:" >&2
  git -C "${repo_root}" log --oneline "${remote}/${branch}..${branch}" >&2
  echo "'${branch}' must never carry unique work; fix with:" >&2
  echo "  git -C '${repo_root}' status --porcelain  # inspect the working tree first" >&2
  echo "  git -C '${repo_root}' stash push -u -m <reason>  # if it holds unrelated uncommitted work" >&2
  echo "  git -C '${repo_root}' reset --hard '${remote}/${branch}'" >&2
  exit 1
fi

echo "branch sync guard accepted: local '${branch}' matches '${remote}/${branch}'"
