#!/usr/bin/env bash
# Fail closed before a deployment can write to a remote target.
set -euo pipefail

: "${O4G_EXPECTED_TARGET:?O4G_EXPECTED_TARGET is required}"
: "${O4G_EXPECTED_CLUSTER_FINGERPRINT:?O4G_EXPECTED_CLUSTER_FINGERPRINT is required}"

target_marker_file="${O4G_TARGET_MARKER_FILE:-/etc/open4goods/deployment-target}"
cluster_fingerprint_file="${O4G_CLUSTER_FINGERPRINT_FILE:-/etc/open4goods/elasticsearch-cluster-fingerprint}"

if [[ ! "${O4G_EXPECTED_TARGET}" =~ ^[a-z][a-z0-9-]*$ ]]; then
  echo "deployment target guard rejected an invalid expected target" >&2
  exit 1
fi

if [[ ! "${O4G_EXPECTED_CLUSTER_FINGERPRINT}" =~ ^[a-f0-9]{64}$ ]]; then
  echo "deployment target guard rejected an invalid expected cluster fingerprint" >&2
  exit 1
fi

read_marker() {
  local marker_file="$1"
  if [[ ! -r "${marker_file}" ]]; then
    echo "deployment target guard rejected a missing marker" >&2
    exit 1
  fi

  tr -d '\r\n' < "${marker_file}"
}

actual_target="$(read_marker "${target_marker_file}")"
actual_cluster_fingerprint="$(read_marker "${cluster_fingerprint_file}")"

if [[ "${actual_target}" != "${O4G_EXPECTED_TARGET}" ]]; then
  echo "deployment target guard rejected a target mismatch" >&2
  exit 1
fi

if [[ ! "${actual_cluster_fingerprint}" =~ ^[a-f0-9]{64}$ ]] \
  || [[ "${actual_cluster_fingerprint}" != "${O4G_EXPECTED_CLUSTER_FINGERPRINT}" ]]; then
  echo "deployment target guard rejected a cluster mismatch" >&2
  exit 1
fi

echo "deployment target guard accepted ${O4G_EXPECTED_TARGET}"
