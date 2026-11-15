#!/bin/sh
# Remote part of the open data upload to R2 (GOU-2245). Run on the prod host through `bash -s`,
# fed over SSH stdin by .github/workflows/opendataR2Upload.yml, after `export` lines for:
# DATASET, MODE, UPLOAD_LIMIT and RCLONE_CONFIG_R2_* (credentials stay out of any command line).
# Commands run with </dev/null so none of them can consume the script from stdin.
set -eu

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
  if [ ! -f "${src}" ]; then echo "fichier absent: ${src}"; exit 1; fi
  local_sha="$(nice -n 19 ionice -c3 sha256sum "${src}" </dev/null | cut -d' ' -f1)"
  echo "[${n}] ${src} $(stat -c %s "${src}") octets sha256=${local_sha}"
  if [ "${MODE}" != "apply" ]; then
    echo "[${n}] mode test: rien n'est écrit. Objet distant actuel:"
    nice -n 19 ionice -c3 "${RCLONE}" lsl "R2:${BUCKET}/${key}" </dev/null || echo "(absent)"
    continue
  fi
  nice -n 19 ionice -c3 "${RCLONE}" copyto "${src}" "R2:${BUCKET}/${key}" \
    --bwlimit "${UPLOAD_LIMIT}" --transfers 1 --s3-upload-concurrency 2 \
    --header-upload "Content-Type: application/zip" \
    --header-upload "Content-Disposition: attachment; filename=\"${key}\"" \
    --header-upload "Cache-Control: public, max-age=86400" \
    --metadata --metadata-set "sha256=${local_sha}" \
    --no-update-modtime </dev/null
  # Read the object back (not the metadata) and compare with the local sum.
  remote_sha="$(nice -n 19 ionice -c3 "${RCLONE}" cat "R2:${BUCKET}/${key}" </dev/null | sha256sum | cut -d' ' -f1)"
  if [ "${remote_sha}" != "${local_sha}" ]; then
    echo "::error::[${n}] sha256 distant ${remote_sha} différent du local ${local_sha}"
    exit 1
  fi
  echo "[${n}] OK sha256 vérifié: ${remote_sha}"
done
