---
title: "Default-branch commit guard"
normative: false
audience: PROJECT_SCOPED
---

# Default-branch commit guard

## Symptom (GOU-183, 2026-10-01)

Four completed issues (GOU-175, GOU-177, GOU-178, GOU-179) were committed directly onto the
shared workspace's local `main` instead of an issue branch. None of those commits were ever
pushed: no remote branch, no PR, no CI run. GOU-179 was even marked `done`. Lead Tech later
replayed the four commits as real PRs (#3397-#3400); CI immediately found real defects in two
of them (an unregistered control script, an import missing from the runner), because nothing
had ever actually run that code.

This is the upstream half of the branch-contamination family:
[Shared workspace branch-sync guard](shared-workspace-branch-sync.md) (GOU-159) catches a branch
created from a `main` that already carries foreign commits -- contamination flowing *downstream*
into a new branch. This guard catches an agent's own commit landing on `main` in the first place
-- contamination flowing *upstream*, out of the branch workflow entirely.

## Guard

`scripts/verify/check_default_branch_commit.py` checks the current branch against the
repository's default branch (`origin/HEAD`, falling back to `main`) and refuses to proceed when
they match. It is wired as the repository's `pre-commit` hook in `.githooks/pre-commit`, so it
runs before each commit is created, while `HEAD` still points at the branch the commit would land
on:

```sh
git config core.hooksPath .githooks   # see section 6 of AGENTS.md
git commit -m "..."                   # refused outright if HEAD is main
```

A refused commit prints the exact command to branch off first:

```
commit refused: HEAD is on 'main', the default branch -- commits must land on an issue branch,
never on it directly.
'main' must only ever mirror 'origin/main'; branch off before committing:
  git checkout -b <ISSUE-ID>-<slug> main
```

Staged work is not lost: `git checkout -b` carries the index and working tree to the new branch,
so the refused commit can simply be retried there.

The gate is registered in `.o4g/control-invocations.json` under
`scripts/verify/check_default_branch_commit.py`, with `.githooks/pre-commit` as its declared
invocation point (see GOU-178's `check_control_invocations.py`, which fails closed if that
wiring is ever silently dropped).

`scripts/verify/test_check_default_branch_commit.py` demonstrates both directions by execution:
it builds a throwaway fixture repository, installs a hook that calls the real gate script, and
runs real `git commit` attempts -- one on `main` (rejected, no commit lands) and one on an issue
branch (accepted).

## Known limit

The shared build host's workspace provisioning writes its `Paperclip SSH sync merge` commits
onto local `main` outside of any `git commit` call this repository's hooks can observe (see
[Shared workspace branch-sync guard](shared-workspace-branch-sync.md)). This guard does not and
cannot intercept that merge; it only intercepts an agent's own commit landing on `main`, which
is the failure mode GOU-183 describes.
