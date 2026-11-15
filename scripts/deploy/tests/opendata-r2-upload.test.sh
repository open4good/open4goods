#!/usr/bin/env bash
# Test of scripts/deploy/opendata-r2-upload.sh with a fake rclone (GOU-2285). No network, no R2.
# shellcheck disable=SC2016,SC2034 # checks are eval'd strings; GOOD is read by them
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
SCRIPT="${HERE}/../opendata-r2-upload.sh"
T="$(mktemp -d)"
trap 'rm -rf "${T}"' EXIT
mkdir -p "${T}/src" "${T}/bucket/.staging"
printf 'zipdata' > "${T}/src/gtin.zip"
GOOD="$(sha256sum "${T}/src/gtin.zip" | cut -d' ' -f1)"

# Fake rclone: the bucket is a directory; FAKE_CORRUPT alters the read-back; FAKE_SLOW delays copyto.
cat > "${T}/rclone" <<'FAKE'
#!/usr/bin/env bash
cmd="$1"; shift
b="${FAKE_BUCKET}"
key() { echo "${1#R2:nudger-opendata/}"; }
echo "${cmd} $*" >> "${FAKE_LOG}"
case "${cmd}" in
  copyto)
    if [ -n "${FAKE_SLOW:-}" ]; then sleep "${FAKE_SLOW}"; fi
    mkdir -p "${b}/.staging"; cp "$1" "${b}/$(key "$2")"
    for a in "$@"; do case "${a}" in X-Amz-Meta-Sha256*) printf '%s' "${a#*: }" > "${b}/$(key "$2").sha"; esac; done ;;
  cat) cat "${b}/$(key "$1")"; if [ -n "${FAKE_CORRUPT:-}" ]; then printf 'x'; fi ;;
  moveto) mv "${b}/$(key "$1")" "${b}/$(key "$2")"; mv "${b}/$(key "$1").sha" "${b}/$(key "$2").sha" ;;
  deletefile) rm -f "${b}/$(key "$1")" ;;
  lsjson) k="$(key "${@: -1}")"; echo "[{\"MimeType\":\"application/zip\",\"Metadata\":{\"sha256\":\"$(cat "${b}/${k}.sha")\"}}]" ;;
  lsl) exit 3 ;;
esac
FAKE
chmod +x "${T}/rclone"

fails=0
run() { # name expected_rc [env...]
  local name="$1" want="$2"; shift 2
  : > "${T}/log"; rm -rf "${T}/bucket"; mkdir -p "${T}/bucket/.staging"; printf 'OLD' > "${T}/bucket/gtin-open-data.zip"
  env DATASET=gtin MODE=apply UPLOAD_LIMIT=2M OPENDATA_SRC_DIR="${T}/src" OPENDATA_RCLONE="${T}/rclone" \
    FAKE_BUCKET="${T}/bucket" FAKE_LOG="${T}/log" "$@" bash "${SCRIPT}" > "${T}/out" 2>&1
  local rc=$?
  if { [ "${want}" = nz ] && [ "${rc}" -eq 0 ]; } || { [ "${want}" = 0 ] && [ "${rc}" -ne 0 ]; }; then
    echo "FAIL ${name}: rc=${rc}"; cat "${T}/out"; fails=$((fails+1)); return 1
  fi
  return 0
}
check() { if ! eval "$2"; then echo "FAIL $1"; cat "${T}/out" "${T}/log"; fails=$((fails+1)); else echo "ok   $1"; fi; }

run "ok" 0 && {
  check "ok: public replaced" '[ "$(cat "${T}/bucket/gtin-open-data.zip")" = zipdata ]'
  check "ok: moveto once" '[ "$(grep -c "^moveto" "${T}/log")" = 1 ]'
  check "ok: no staging left" '[ ! -e "${T}/bucket/.staging/gtin-open-data.zip" ]'
  check "ok: only sha256 header, no --metadata-set" '! grep -q -- "--metadata-set" "${T}/log" && grep -q "X-Amz-Meta-Sha256: ${GOOD}" "${T}/log"'
}
run "bad sha" nz FAKE_CORRUPT=1 && {
  check "bad sha: public intact" '[ "$(cat "${T}/bucket/gtin-open-data.zip")" = OLD ]'
  check "bad sha: staging deleted" '[ ! -e "${T}/bucket/.staging/gtin-open-data.zip" ]'
  check "bad sha: no moveto" '! grep -q "^moveto" "${T}/log"'
}
run "timeout" nz FAKE_SLOW=20 TOTAL_TIMEOUT_S=2 && {
  check "timeout: public intact" '[ "$(cat "${T}/bucket/gtin-open-data.zip")" = OLD ]'
  check "timeout: no moveto" '! grep -q "^moveto" "${T}/log"'
}
run "test mode" 0 MODE=test && check "test mode: writes nothing" '! grep -qE "^(copyto|moveto|deletefile)" "${T}/log"'
exit "${fails}"
