---
title: "Shared workspace branch-sync guard"
normative: false
audience: PROJECT_SCOPED
---

# Shared workspace branch-sync guard

## Symptom (GOU-159, 2026-10-01)

On the shared build host, several open PRs (#3371, #3369, #3346) carried commits
that were not their own and failed CI for that reason alone. Each PR branch had
only one commit of its own; the rest came from the local `main` it branched from.

## Cause

The shared build host materializes each run's workspace from the host's prior
git state rather than a clean clone from `origin`. That provisioning step
produced merge commits titled `Paperclip SSH sync merge <hash>` directly on the
local `main` ref, folding in commits from branches that were not yet merged on
`origin/main` (open PRs, duplicates of already-merged work, and the sync merge
commit itself). None of this is visible through the repository's own git hooks:
the merge lands on disk before an agent's git commands run, so a tracked
`.githooks` hook never sees it.

Once local `main` had diverged from `origin/main`, every `git checkout -b
<branch> main` by any agent silently inherited the foreign commits. The
contamination also skewed local-only measurements that diffed against `main`
(for example `check_corpus_budget.py`, which read a false near-saturated budget
against the local branch).

## Guard

`scripts/verify/check-branch-sync.sh` fetches `origin` and fails closed if local
`main` carries any commit `origin/main` lacks:

```sh
./scripts/verify/check-branch-sync.sh "$REPO_ROOT"
```

Run it (or an equivalent `git fetch origin && git rev-list --count
origin/main..main`) before branching for an issue, in the shared workspace.
A non-zero count means `main` must be reset to `origin/main`, after inspecting
`git status` and stashing (not discarding) any unrelated uncommitted work found
on it: `main` itself should never carry commits or working-tree changes of its
own, only mirror `origin/main`.

This guard makes the drift loud instead of silent. It does not stop the host's
provisioning step from writing a sync-merge commit again; that step runs
outside this repository's control (no tracked hook observes it). Treat a
guard failure as the signal to reset `main` and, if it recurs, escalate to
whoever operates the shared build host's workspace provisioning.

This guard catches contamination flowing *downstream*, into a branch cut from
a drifted `main`. See the
[default-branch commit guard](default-branch-commit-guard.md) (GOU-183) for
the complementary *upstream* case: an agent's own commit landing on `main`
directly, which this guard alone does not prevent.
