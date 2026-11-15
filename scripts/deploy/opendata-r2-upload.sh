#!/usr/bin/env bash
# Remote part of the open data upload to R2 (GOU-2245). Run on the prod host through `bash -s`,
# fed over SSH stdin by .github/workflows/opendataR2Upload.yml, after `export` lines for:
# DATASET, MODE, UPLOAD_LIMIT and RCLONE_CONFIG_R2_* (credentials stay out of any command line).
# Optional: TOTAL_TIMEOUT_S, overall budget in seconds for every rclone transfer (default 9900 = 165 min, under the
# 180 min job timeout, so a cancelled job leaves no orphan upload). OPENDATA_SRC_DIR and OPENDATA_RCLONE exist for
# the test scripts/deploy/tests/opendata-r2-upload.test.sh.
# Commands run with </dev/null so none of them can consume the script from stdin.
set -euo pipefail

SRC_DIR="${OPENDATA_SRC_DIR:-/opt/open4goods/opendata}"
BUCKET=nudger-opendata
RCLONE="${OPENDATA_RCLONE:-$HOME/bin/rclone}"
TOTAL_TIMEOUT_S="${TOTAL_TIMEOUT_S:-9900}"
START_S="${SECONDS}"
if [ ! -x "${RCLONE}" ]; then
  echo "rclone introuvable: ${RCLONE} (binaire local, voir docs/operations/opendata-r2-upload.md)"
  exit 1
fi
# Entries to send, "local file|public key|content type". Impact datasets (GOU-2250): every impact-*.zip of the
# source folder (7 verticals and impact-all.zip), with the impact manifests, which are JSON.
entries=()
case "${DATASET}" in
  gtin) entries=("gtin.zip|gtin-open-data.zip|application/zip") ;;
  isbn) entries=("isbn.zip|isbn-open-data.zip|application/zip") ;;
  impact|all)
    if [ "${DATASET}" = "all" ]; then
      entries=("gtin.zip|gtin-open-data.zip|application/zip" "isbn.zip|isbn-open-data.zip|application/zip")
    fi
    shopt -s nullglob
    for f in "${SRC_DIR}"/impact-*.zip; do entries+=("$(basename "${f}")|$(basename "${f}")|application/zip"); done
    for f in "${SRC_DIR}"/impact-*-manifest.json; do entries+=("$(basename "${f}")|$(basename "${f}")|application/json"); done
    shopt -u nullglob
    if [ "${DATASET}" = "impact" ] && [ "${#entries[@]}" -eq 0 ]; then
      echo "::error::aucun fichier impact-* dans ${SRC_DIR}"; exit 1
    fi
    ;;
  *) echo "dataset invalide"; exit 1 ;;
esac

# Seconds left in the overall budget; fails when it is spent.
remaining() {
  local left=$(( TOTAL_TIMEOUT_S - (SECONDS - START_S) ))
  if [ "${left}" -le 0 ]; then echo "::error::délai global dépassé" >&2; return 1; fi
  echo "${left}"
}

# Stages are removed on any failure; the public key is only touched by the final moveto.
staging=""
cleanup() {
  if [ -n "${staging}" ]; then
    "${RCLONE}" deletefile "R2:${BUCKET}/${staging}" </dev/null >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

for entry in "${entries[@]}"; do
  IFS='|' read -r file key ctype <<<"${entry}"
  n="${file%.*}"
  src="${SRC_DIR}/${file}"
  if [ ! -s "${src}" ]; then echo "::error::[${n}] fichier absent ou vide: ${src}"; exit 1; fi
  local_sha="$(nice -n 19 ionice -c3 sha256sum "${src}" </dev/null | cut -d' ' -f1)"
  if [[ ! "${local_sha}" =~ ^[0-9a-f]{64}$ ]]; then
    echo "::error::[${n}] sha256 local illisible: '${local_sha}'"
    exit 1
  fi
  echo "[${n}] ${src} $(stat -c %s "${src}") octets sha256=${local_sha}"
  if [ "${MODE}" != "apply" ]; then
    echo "[${n}] mode test: rien n'est écrit. Objet distant actuel:"
    # rclone exit codes 3 (directory not found) and 4 (file not found) mean "absent"; any other error
    # (credentials, endpoint, network) fails the dry run instead of passing for an absent object.
    rc=0
    listing="$(nice -n 19 ionice -c3 "${RCLONE}" lsl "R2:${BUCKET}/${key}" </dev/null)" || rc=$?
    case "${rc}" in
      0) echo "${listing:-(absent)}" ;;
      3|4) echo "(absent)" ;;
      *) echo "::error::[${n}] lecture du bucket impossible (rclone code ${rc}): identifiants ou endpoint R2"; exit 1 ;;
    esac
    continue
  fi
  staging=".staging/${key}"
  # The overall budget bounds every transfer: `timeout` kills rclone (and the stage is deleted by the trap).
  left="$(remaining)" || exit 1
  hdrs=(
    --header-upload "Content-Type: ${ctype}"
    --header-upload "Content-Disposition: attachment; filename=\"${key}\""
    --header-upload "Cache-Control: public, max-age=86400"
    --header-upload "X-Amz-Meta-Sha256: ${local_sha}"
  )
  # No --metadata: the prod file's system metadata (uid, gid, mode, dates) must not reach the object.
  rc=0
  timeout -k 30 "${left}" nice -n 19 ionice -c3 "${RCLONE}" copyto "${src}" "R2:${BUCKET}/${staging}" \
    --bwlimit "${UPLOAD_LIMIT}" --transfers 1 --s3-upload-concurrency 2 "${hdrs[@]}" \
    --stats 1m --stats-one-line --stats-log-level NOTICE \
    --no-update-modtime </dev/null || rc=$?
  if [ "${rc}" -ne 0 ]; then
    echo "::error::[${n}] envoi vers ${staging} en échec ou interrompu (code ${rc}); objet public intact"
    exit 1
  fi
  # Read the staged object back (not the metadata) and compare with the local sum, at full speed.
  left="$(remaining)" || exit 1
  remote_sha="$(timeout -k 30 "${left}" nice -n 19 ionice -c3 "${RCLONE}" cat "R2:${BUCKET}/${staging}" </dev/null | sha256sum | cut -d' ' -f1)" || {
    echo "::error::[${n}] relecture de l'objet temporaire impossible ou interrompue; objet public intact"
    exit 1
  }
  if [ "${remote_sha}" != "${local_sha}" ]; then
    echo "::error::[${n}] sha256 distant ${remote_sha} différent du local ${local_sha}; objet public intact"
    exit 1
  fi
  # Server-side copy then delete; the headers are set again in case the copy does not keep them.
  left="$(remaining)" || exit 1
  rc=0
  timeout -k 30 "${left}" "${RCLONE}" moveto "R2:${BUCKET}/${staging}" "R2:${BUCKET}/${key}" "${hdrs[@]}" </dev/null || rc=$?
  if [ "${rc}" -ne 0 ]; then
    echo "::error::[${n}] renommage vers ${key} en échec (code ${rc})"
    exit 1
  fi
  staging=""
  # Final check of the public object: only sha256 as user metadata, and the expected content type.
  meta="$("${RCLONE}" lsjson --metadata "R2:${BUCKET}/${key}" </dev/null)" || {
    echo "::error::[${n}] relecture des métadonnées de ${key} impossible"
    exit 1
  }
  if ! grep -q "${local_sha}" <<<"${meta}" || ! grep -qi "${ctype}" <<<"${meta}"; then
    echo "::error::[${n}] métadonnées de ${key} incomplètes après renommage: ${meta}"
    exit 1
  fi
  echo "[${n}] OK sha256 vérifié: ${remote_sha}"
done
