---
title: "Backup qualification and promotion rollback: what the local fixtures prove"
normative: false
audience: PROJECT_SCOPED
---

# Backup qualification and promotion rollback: what the local fixtures prove

ADR-0014 (local-first development and staged promotion) defines a fresh backup as
"qualified" once SHA-256, gzip integrity, line counts and manifest consistency are proven,
and pairs promotion with rollback on failure. Both gates now have offline fixtures.

## Qualifying a fresh backup

`scripts/migration/pin_product_backup.py` reads the private backup volume once and writes an
import-side manifest (schema `open4goods.product-backup-input/v1`, see
[product-backup-input-contract.md](product-backup-input-contract.md)): the legacy manifest's
SHA-256, per-archive checksums, decoded line counts and a value-free field inventory. That
manifest is a claim; `scripts/verify/check_product_backup_qualification.py` re-checks it from
the manifest alone, opening no archive, private volume or remote storage. Qualified means: a
matching pin schema version; a well-formed SHA-256 and line count for exactly the legacy
manifest's archives, in order; a per-file line-count sum equal to the legacy export count; a
field disposition for every observed field; and a scope statement keeping accounts, billing
and keys apart from the backup.

`scripts/verify/test_check_product_backup_qualification.py` pins a synthetic archive with the
real `pin_product_backup.py` path, runs that output through the gate, then breaks each claim
above (wrong schema, malformed digest, inconsistent line count, missing archive, incomplete
field coverage, collapsed account/billing scope) under a `tempfile.TemporaryDirectory()`.

## Forcing promotion failures and rollback

`publish-java-release.sh` / `publish-nuxt-release.sh` already cover health-check rollback via
symlink swap (`scripts/deploy/tests/systemd-runtime.test.sh`).
`scripts/deploy/tests/promotion-failure-fixtures.test.sh` adds the rest, each against a fresh
`mktemp -d` root with `systemctl`/`curl` swapped for local stubs: a stripped `contract=` line
(corrupt manifest), tampered artifact bytes (digest mismatch), an artifact missing from the
bundle, a non-matching health status, and a release directory left mid-stage by a crashed
prior publish (partial write, no `release-manifest`).

Each case leaves `services/<name>/current` at its prior target, skips `systemctl` when
rejection happens before staging, and leaves a partial-write fixture's bytes as found. The
test also checks the fixture tree names no account, billing or credential path, and that
`curl`/`systemctl` resolve to the fixture's own stubs.

Stub isolation is asserted against what a *child* script resolves, not the test's own shell:
the deploy scripts are `#!/usr/bin/env bash`, so an inherited `BASH_ENV` startup file runs
first and can re-export `PATH`, which would hand them the real `systemctl` and restart a live
unit. Every stubbed invocation therefore goes through `env -u BASH_ENV`, and the fixtures fail
up front if the stub is not what a child bash resolves.

```bash
python3 -m unittest scripts.verify.test_check_product_backup_qualification -v
bash scripts/deploy/tests/promotion-failure-fixtures.test.sh
```
