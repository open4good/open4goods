#!/usr/bin/env bash
# Tests scripts/deploy/push-remote-file.sh, push-remote-env.sh and render-service-secrets.sh
# against a fake `ssh` that runs the remote script locally, with /etc/open4goods/ mapped to a temp dir.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY="${ROOT}/scripts/deploy"
WORK="$(mktemp -d)"
trap 'rm -rf "${WORK}"' EXIT

FAKE_ROOT="${WORK}/host"
mkdir -p "${WORK}/bin" "${FAKE_ROOT}"
cat > "${WORK}/bin/ssh" <<'SH'
#!/bin/sh
# Fake ssh: record the arguments, then run the last one (the remote script) locally.
printf '%s\n' "$@" >> "$FAKE_SSH_LOG"
for arg; do last=$arg; done
script=$(printf '%s' "$last" | sed "s#/etc/open4goods/#$FAKE_ROOT/etc/open4goods/#g")
exec sh -c "$script"
SH
chmod +x "${WORK}/bin/ssh"

export PATH="${WORK}/bin:${PATH}"
export FAKE_ROOT FAKE_SSH_LOG="${WORK}/ssh.log"
export DEPLOY_SSH_HOST=prod.example DEPLOY_SSH_USER=deploy DEPLOY_SSH_KEY="fake-ssh-key"
unset GITHUB_STEP_SUMMARY
: > "${FAKE_SSH_LOG}"

SENTINEL="SENTINELKEYMATERIAL42"
VALID_JSON="$(cat <<JSON
{
  "type": "service_account",
  "project_id": "nudger-test",
  "private_key_id": "0123456789abcdef",
  "private_key": "-----BEGIN PRIVATE KEY-----\\n${SENTINEL}\\n-----END PRIVATE KEY-----\\n",
  "client_email": "indexation@nudger-test.iam.gserviceaccount.com"
}
JSON
)"

failures=0
pass() { echo "ok   - $1"; }
ko() { echo "FAIL - $1"; failures=$((failures + 1)); }

# Runs a command, captures stdout+stderr in $out and its status in $status.
run() {
  set +e
  out="$("$@" 2>&1)"
  status=$?
  set -e
}

no_secret_in() {
  if grep -q "${SENTINEL}" "$@" 2>/dev/null; then
    return 1
  fi
  return 0
}

expect_fail() {
  local label=$1
  shift
  run "$@"
  if [[ ${status} -ne 0 ]] && [[ "${out}" != *"${SENTINEL}"* ]]; then
    pass "${label}"
  else
    ko "${label} (status ${status}): ${out}"
  fi
}

########################################
# push-remote-file.sh: path validation
########################################
export SA_JSON="${VALID_JSON}"
for path in /etc/passwd /etc/open4goods /etc/open4goods/ /etc/open4goods/../passwd \
  "/etc/open4goods/a b" "/etc/open4goods/x;rm" "/etc/open4goods/x'y" /etc/open4goods//x \
  /etc/open4goods/.hidden etc/open4goods/x /etc/open4goodsx/x; do
  expect_fail "rejects path '${path}'" "${DEPLOY}/push-remote-file.sh" --validate-only "${path}" SA_JSON
done
expect_fail "rejects lowercase variable name" "${DEPLOY}/push-remote-file.sh" --validate-only /etc/open4goods/x.json sa_json
expect_fail "rejects unknown option" "${DEPLOY}/push-remote-file.sh" --bogus /etc/open4goods/x.json SA_JSON

########################################
# push-remote-file.sh: service account JSON validation
########################################
check_json() {
  local label=$1 value=$2
  SA_JSON="${value}" expect_fail "${label}" "${DEPLOY}/push-remote-file.sh" --service-account-json --validate-only \
    /etc/open4goods/secrets/x.json SA_JSON
}
check_json "rejects missing value" ""
check_json "rejects invalid JSON" "{\"private_key\": \"${SENTINEL}\""
check_json "rejects non-object JSON" "[\"${SENTINEL}\"]"
check_json "rejects two JSON documents" "${VALID_JSON} ${VALID_JSON}"
check_json "rejects wrong type" "${VALID_JSON//service_account/authorized_user}"
check_json "rejects missing client_email" "$(jq 'del(.client_email)' <<< "${VALID_JSON}")"
check_json "rejects empty private_key" "$(jq '.private_key = ""' <<< "${VALID_JSON}")"
check_json "rejects numeric private_key" "$(jq '.private_key = 1' <<< "${VALID_JSON}")"

run "${DEPLOY}/push-remote-file.sh" --service-account-json --validate-only /etc/open4goods/secrets/x.json SA_JSON
if [[ ${status} -eq 0 ]] && [[ "${out}" != *"${SENTINEL}"* ]] && [[ ! -s "${FAKE_SSH_LOG}" ]]; then
  pass "accepts a valid service account JSON without SSH in --validate-only"
else
  ko "valid JSON in --validate-only (status ${status}): ${out}"
fi
[[ ! -e "${FAKE_ROOT}/etc" ]] && pass "--validate-only writes nothing" || ko "--validate-only wrote files"

########################################
# push-remote-file.sh: write through SSH
########################################
target="${FAKE_ROOT}/etc/open4goods/secrets/x.json"
run "${DEPLOY}/push-remote-file.sh" --service-account-json /etc/open4goods/secrets/x.json SA_JSON
if [[ ${status} -eq 0 ]] && [[ "${out}" == *"Wrote /"*"secrets/x.json (600 "* ]] && [[ "${out}" != *"${SENTINEL}"* ]]; then
  pass "writes the file and reports its mode"
else
  ko "write (status ${status}): ${out}"
fi
[[ "$(stat -c '%a' "${target}")" == 600 ]] && pass "file mode is 600" || ko "file mode is $(stat -c '%a' "${target}")"
[[ "$(stat -c '%a' "$(dirname "${target}")")" == 700 ]] && pass "new directory mode is 700" || ko "directory mode"
if jq -e --arg e indexation@nudger-test.iam.gserviceaccount.com '.client_email == $e' "${target}" >/dev/null; then
  pass "file content is the secret value"
else
  ko "file content differs"
fi
no_secret_in "${FAKE_SSH_LOG}" && pass "secret absent from the SSH command line" || ko "secret found in SSH arguments"
[[ -z "$(find "$(dirname "${target}")" -name 'x.json.*')" ]] && pass "no temp file left" || ko "temp file left"

SA_JSON="${VALID_JSON//0123456789abcdef/fedcba9876543210}" run "${DEPLOY}/push-remote-file.sh" \
  --service-account-json /etc/open4goods/secrets/x.json SA_JSON
if [[ ${status} -eq 0 ]] && jq -e '.private_key_id == "fedcba9876543210"' "${target}" >/dev/null; then
  pass "replaces an existing file"
else
  ko "replace (status ${status}): ${out}"
fi

########################################
# push-remote-env.sh: '/' in values, other lines kept
########################################
mkdir -p "${FAKE_ROOT}/etc/open4goods"
printf 'KEEP_ME=1\nexport TARGET_PATH=/old/path\n' > "${FAKE_ROOT}/etc/open4goods/x.env"
TARGET_PATH=/etc/open4goods/secrets/x.json run "${DEPLOY}/push-remote-env.sh" /etc/open4goods/x.env TARGET_PATH
if [[ ${status} -eq 0 ]] && [[ "$(cat "${FAKE_ROOT}/etc/open4goods/x.env")" == $'KEEP_ME=1\nTARGET_PATH=/etc/open4goods/secrets/x.json' ]]; then
  pass "push-remote-env.sh accepts '/' and keeps other lines"
else
  ko "push-remote-env.sh (status ${status}): ${out}"
fi
TARGET_PATH='/a b' expect_fail "push-remote-env.sh still rejects spaces" "${DEPLOY}/push-remote-env.sh" /etc/open4goods/x.env TARGET_PATH

########################################
# render-service-secrets.sh
########################################
rm -rf "${FAKE_ROOT:?}/etc"
: > "${FAKE_SSH_LOG}"
unset SA_JSON API_GOOGLE_API_JSON

expect_fail "render: unknown service" "${DEPLOY}/render-service-secrets.sh" check ui
expect_fail "render: unknown action" "${DEPLOY}/render-service-secrets.sh" render api
expect_fail "render: check fails when the secret is missing" "${DEPLOY}/render-service-secrets.sh" check api

run "${DEPLOY}/render-service-secrets.sh" --if-present apply api
if [[ ${status} -eq 0 ]] && [[ "${out}" == *"::warning::API_GOOGLE_API_JSON is not set"* ]] && [[ ! -s "${FAKE_SSH_LOG}" ]]; then
  pass "render: --if-present warns and renders nothing when the secret is missing"
else
  ko "render --if-present missing (status ${status}): ${out}"
fi

API_GOOGLE_API_JSON='{"type":"service_account"}' expect_fail "render: --if-present still fails on a malformed secret" \
  "${DEPLOY}/render-service-secrets.sh" --if-present check api

export API_GOOGLE_API_JSON="${VALID_JSON}"
run "${DEPLOY}/render-service-secrets.sh" check api
if [[ ${status} -eq 0 ]] && [[ ! -s "${FAKE_SSH_LOG}" ]] && [[ ! -e "${FAKE_ROOT}/etc" ]]; then
  pass "render: check uses no SSH and writes nothing"
else
  ko "render check (status ${status}): ${out}"
fi

mkdir -p "${FAKE_ROOT}/etc/open4goods"
printf 'JAVA_OPTS=-Xmx4g\nGOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH=/opt/open4goods/config/google-api.json\n' \
  > "${FAKE_ROOT}/etc/open4goods/api.env"
export GITHUB_STEP_SUMMARY="${WORK}/summary.md"
run "${DEPLOY}/render-service-secrets.sh" apply api
json="${FAKE_ROOT}/etc/open4goods/secrets/google-indexation.json"
if [[ ${status} -eq 0 ]] && [[ "$(stat -c '%a' "${json}")" == 600 ]] \
  && jq -e '.type == "service_account"' "${json}" >/dev/null \
  && [[ "$(cat "${FAKE_ROOT}/etc/open4goods/api.env")" == $'JAVA_OPTS=-Xmx4g\nGOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH=/etc/open4goods/secrets/google-indexation.json' ]]; then
  pass "render: apply writes the JSON (600) and api.env, keeping its other lines"
else
  ko "render apply (status ${status}): ${out}"
fi
if [[ "${out}" == *"indexation@nudger-test.iam.gserviceaccount.com (private_key_id 01234567...)"* ]] \
  && [[ "${out}" != *"89abcdef"* ]] && [[ "${out}" != *"${SENTINEL}"* ]] && no_secret_in "${GITHUB_STEP_SUMMARY}" "${FAKE_SSH_LOG}"; then
  pass "render: summary shows identifiers only"
else
  ko "render summary: ${out}"
fi

if [[ ${failures} -gt 0 ]]; then
  echo "${failures} test(s) failed"
  exit 1
fi
echo "push-remote-file test passed"
