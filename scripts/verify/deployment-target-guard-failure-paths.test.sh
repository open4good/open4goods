#!/usr/bin/env bash
# Authorization and TOCTOU failure-path fixtures for deployment-target-guard.sh (GOU-137).
#
# deployment-target-guard.test.sh covers the golden path plus a wrong-target/wrong-cluster
# mismatch. This file covers what happens with missing or malformed authorization inputs, and
# whether the guard can be fooled by state that changes around the moment it is consulted -
# the two properties GOU-94's acceptance criteria calls out by name.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
fixture="$(mktemp -d)"
trap 'rm -rf -- "$fixture"' EXIT

fingerprint_a="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
fingerprint_b="abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
target_marker="$fixture/deployment-target"
cluster_marker="$fixture/elasticsearch-cluster-fingerprint"

printf '%s\n' beta > "$target_marker"
printf '%s\n' "$fingerprint_a" > "$cluster_marker"

run_guard() {
  O4G_EXPECTED_TARGET="${1-}" \
    O4G_EXPECTED_CLUSTER_FINGERPRINT="${2-}" \
    O4G_TARGET_MARKER_FILE="${3-$target_marker}" \
    O4G_CLUSTER_FINGERPRINT_FILE="${4-$cluster_marker}" \
    "$ROOT/scripts/verify/deployment-target-guard.sh"
}

expect_rejected() {
  local description="$1"; shift
  if "$@" >/dev/null 2>&1; then
    echo "expected ${description} to be rejected" >&2
    exit 1
  fi
}

##############################################################################
# Authorization: every required input must be present and correctly shaped, not just non-empty.
##############################################################################
expect_rejected 'a missing expected target' env -u O4G_EXPECTED_TARGET \
  O4G_EXPECTED_CLUSTER_FINGERPRINT="$fingerprint_a" O4G_TARGET_MARKER_FILE="$target_marker" \
  O4G_CLUSTER_FINGERPRINT_FILE="$cluster_marker" "$ROOT/scripts/verify/deployment-target-guard.sh"

expect_rejected 'a missing expected cluster fingerprint' env -u O4G_EXPECTED_CLUSTER_FINGERPRINT \
  O4G_EXPECTED_TARGET=beta O4G_TARGET_MARKER_FILE="$target_marker" \
  O4G_CLUSTER_FINGERPRINT_FILE="$cluster_marker" "$ROOT/scripts/verify/deployment-target-guard.sh"

expect_rejected 'an empty expected target' run_guard '' "$fingerprint_a"
expect_rejected 'an expected target with shell metacharacters' run_guard 'beta; rm -rf /' "$fingerprint_a"
expect_rejected 'a too-short expected cluster fingerprint' run_guard beta 'abc123'
expect_rejected 'an uppercase expected cluster fingerprint' run_guard beta "${fingerprint_a^^}"

##############################################################################
# Authorization: a missing or unreadable marker must fail closed, not be treated as "no target".
##############################################################################
expect_rejected 'a missing target marker file' \
  run_guard beta "$fingerprint_a" "$fixture/does-not-exist" "$cluster_marker"
expect_rejected 'a missing cluster fingerprint marker file' \
  run_guard beta "$fingerprint_a" "$target_marker" "$fixture/does-not-exist"

unreadable_marker="$fixture/unreadable-target"
printf '%s\n' beta > "$unreadable_marker"
chmod 000 "$unreadable_marker"
if [[ "$(id -u)" != '0' ]]; then
  expect_rejected 'an unreadable target marker file' \
    run_guard beta "$fingerprint_a" "$unreadable_marker" "$cluster_marker"
fi
chmod 600 "$unreadable_marker"

##############################################################################
# Authorization: a malformed marker (empty, or a fingerprint that merely matches length/charset
# but not the file's own historical value swapped in below) must not be accepted as a coincidence.
##############################################################################
empty_marker="$fixture/empty-cluster-fingerprint"
: > "$empty_marker"
expect_rejected 'an empty cluster fingerprint marker' \
  run_guard beta "$fingerprint_a" "$target_marker" "$empty_marker"

malformed_marker="$fixture/malformed-cluster-fingerprint"
printf '%s' "not-hex-and-too-short" > "$malformed_marker"
expect_rejected 'a malformed cluster fingerprint marker' \
  run_guard beta "$fingerprint_a" "$target_marker" "$malformed_marker"

##############################################################################
# TOCTOU: the guard must not cache or otherwise remember a prior accept. A caller that re-checks
# immediately before using the authorization (the documented mitigation for a check-then-use gap)
# must see the live marker state, not the state from an earlier successful check.
##############################################################################
live_target="$fixture/live-target"
printf '%s\n' beta > "$live_target"
run_guard beta "$fingerprint_a" "$live_target" "$cluster_marker" | grep -qx 'deployment target guard accepted beta'

printf '%s\n' prod > "$live_target"
expect_rejected 're-checking after the target marker changed underneath an accepted check' \
  run_guard beta "$fingerprint_a" "$live_target" "$cluster_marker"

##############################################################################
# TOCTOU: a marker swapped in the window between the two reads (target, then cluster fingerprint)
# must not let a fingerprint written after the target check slip through unnoticed. Simulate the
# race deterministically: delay only the guard's first marker read (the target) so a background
# writer has a real window to swap the cluster fingerprint marker before the guard reads it.
##############################################################################
race_target="$fixture/race-target"
race_cluster="$fixture/race-cluster"
printf '%s\n' beta > "$race_target"
printf '%s\n' "$fingerprint_a" > "$race_cluster"

race_bin="$fixture/race-bin"
mkdir -p "$race_bin"
first_call_marker="$fixture/tr-first-call-used"
cat > "$race_bin/tr" <<EOF
#!/usr/bin/env bash
if [[ ! -e "$first_call_marker" ]]; then
  : > "$first_call_marker"
  sleep 0.3
fi
exec /usr/bin/tr "\$@"
EOF
chmod +x "$race_bin/tr"

(
  sleep 0.1
  printf '%s\n' "$fingerprint_b" > "$race_cluster"
) &
racer_pid=$!

expect_rejected 'a cluster fingerprint swapped in after the target check started' \
  env PATH="$race_bin:$PATH" \
  O4G_EXPECTED_TARGET=beta O4G_EXPECTED_CLUSTER_FINGERPRINT="$fingerprint_a" \
  O4G_TARGET_MARKER_FILE="$race_target" O4G_CLUSTER_FINGERPRINT_FILE="$race_cluster" \
  "$ROOT/scripts/verify/deployment-target-guard.sh"
wait "$racer_pid"

echo 'OK: deployment target guard authorization and TOCTOU failure paths'
