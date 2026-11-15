#!/usr/bin/env bash
# Remote part of the open data upload to R2 (GOU-2245). Run on the prod host through `bash -s`,
# fed over SSH stdin by .github/workflows/opendataR2Upload.yml, after `export` lines for:
# DATASET, MODE, UPLOAD_LIMIT and RCLONE_CONFIG_R2_* (credentials stay out of any command line).
# Commands run with </dev/null so none of them can consume the script from stdin.
set -euo pipefail

SRC_DIR=/opt/open4goods/opendata
BUCKET=nudger-opendata
RCLONE="$HOME/bin/rclone"
if [ ! -x "${RCLONE}" ]; then
  echo "rclone introuvable: ${RCLONE} (binaire local, voir docs/operations/opendata-r2-upload.md)"
  exit 1
fi
case "${DATASET}" in
  gtin) names="gtin" ;;
  isbn) names="isbn" ;;
  all)  names="gtin isbn" ;;
  *) echo "dataset invalide"; exit 1 ;;
esac
for n in ${names}; do
  src="${SRC_DIR}/${n}.zip"
  key="${n}-open-data.zip"
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
  nice -n 19 ionice -c3 "${RCLONE}" copyto "${src}" "R2:${BUCKET}/${key}" \
    --bwlimit "${UPLOAD_LIMIT}" --transfers 1 --s3-upload-concurrency 2 \
    --header-upload "Content-Type: application/zip" \
    --header-upload "Content-Disposition: attachment; filename=\"${key}\"" \
    --header-upload "Cache-Control: public, max-age=86400" \
    --metadata --metadata-set "sha256=${local_sha}" \
    --stats 1m --stats-one-line --stats-log-level NOTICE \
    --no-update-modtime </dev/null
  # Read the object back (not the metadata) and compare with the local sum.
  remote_sha="$(nice -n 19 ionice -c3 "${RCLONE}" cat "R2:${BUCKET}/${key}" </dev/null | sha256sum | cut -d' ' -f1)" || {
    echo "::error::[${n}] relecture de l'objet distant impossible"
    exit 1
  }
  if [ "${remote_sha}" != "${local_sha}" ]; then
    echo "::error::[${n}] sha256 distant ${remote_sha} différent du local ${local_sha}"
    exit 1
  fi
  echo "[${n}] OK sha256 vérifié: ${remote_sha}"
done
