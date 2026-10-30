#!/usr/bin/env bash
# Tests scripts/deploy/push-remote-file.sh, push-remote-env.sh and render-service-secrets.sh
# against a fake `ssh` that runs the remote script locally, with /etc/open4goods/ and /opt/open4goods/ mapped to a
# temp dir.
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
script=$(printf '%s' "$last" | sed -e "s#/etc/open4goods/#$FAKE_ROOT/etc/open4goods/#g" \
  -e "s#/opt/open4goods/#$FAKE_ROOT/opt/open4goods/#g")
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
TARGET_PATH='optional:configtree:/etc/open4goods/secrets/x/' run "${DEPLOY}/push-remote-env.sh" /etc/open4goods/x.env TARGET_PATH
if [[ ${status} -eq 0 ]] && grep -qx 'TARGET_PATH=optional:configtree:/etc/open4goods/secrets/x/' "${FAKE_ROOT}/etc/open4goods/x.env"; then
  pass "push-remote-env.sh accepts ':' (Spring config import location)"
else
  ko "push-remote-env.sh ':' (status ${status}): ${out}"
fi

########################################
# push-remote-file.sh --exact: the value as is, no trailing newline
########################################
ANY_VALUE=$'line one "quoted" $HOME #x\nline two: {json}'
ANY_VALUE="${ANY_VALUE}" run "${DEPLOY}/push-remote-file.sh" --exact /etc/open4goods/secrets/x/some.property ANY_VALUE
exact_file="${FAKE_ROOT}/etc/open4goods/secrets/x/some.property"
if [[ ${status} -eq 0 ]] && [[ "$(stat -c '%a' "${exact_file}")" == 600 ]] \
  && cmp -s "${exact_file}" <(printf '%s' "${ANY_VALUE}"); then
  pass "push-remote-file.sh --exact writes the exact value (600), without a trailing newline"
else
  ko "push-remote-file.sh --exact (status ${status}): ${out}"
fi

########################################
# render-service-secrets.sh: arguments
########################################
RENDER="${DEPLOY}/render-service-secrets.sh"
SECRET_NAMES="$(sed -n 's/^[a-z2-]* \([A-Z0-9_]*\) .*/\1/p' "${RENDER}" | sort -u)"
# Runs the script with none of the table's secrets in the environment, whatever the caller exported.
render() {
  local unset_args=()
  local name
  for name in ${SECRET_NAMES}; do
    unset_args+=(-u "${name}")
  done
  env "${unset_args[@]}" "${RENDER}" "$@"
}
# Same, with the given NAME=value pairs first, then the script arguments after `--`.
render_with() {
  local pairs=()
  while [[ "$1" != "--" ]]; do
    pairs+=("$1")
    shift
  done
  shift
  local unset_args=()
  local name
  for name in ${SECRET_NAMES}; do
    unset_args+=(-u "${name}")
  done
  env "${unset_args[@]}" "${pairs[@]}" "${RENDER}" "$@"
}
reset_host() {
  rm -rf "${FAKE_ROOT:?}/etc" "${FAKE_ROOT:?}/opt"
  : > "${FAKE_SSH_LOG}"
  : > "${GITHUB_STEP_SUMMARY}"
}
export GITHUB_STEP_SUMMARY="${WORK}/summary.md"
reset_host

[[ "${SECRET_NAMES}" == *O4G_SBA_PASSWORD* ]] && [[ "${SECRET_NAMES}" == *API_GOOGLE_API_JSON* ]] \
  && pass "render: the table's secret names are read" || ko "render: secret names not read: ${SECRET_NAMES}"

expect_fail "render: unknown service" render check prod xwiki
expect_fail "render: unknown environment" render check staging api
expect_fail "render: unknown action" render render prod api
expect_fail "render: missing environment (old interface)" render check api

########################################
# render-service-secrets.sh: nothing set
########################################
for service in api ui front-api admin b2b-api frontend; do
  for env_name in beta prod; do
    run render --if-present check "${env_name}" "${service}"
    if [[ ${status} -eq 0 ]] && [[ "${out}" == *"::warning::"*"is not set in the ${env_name} GitHub Environment: not rendered for ${service}"* ]] \
      && [[ "${out}" == *"No secret of ${service} is set in the ${env_name} GitHub Environment."* ]] \
      && [[ ! -s "${FAKE_SSH_LOG}" ]]; then
      pass "render: --if-present check ${env_name} ${service} warns, uses no SSH, passes"
    else
      ko "render --if-present check ${env_name} ${service} (status ${status}): ${out}"
    fi
  done
done
for service in api ui admin b2b-api; do
  run render check prod "${service}"
  [[ ${status} -eq 0 ]] && pass "render: check prod ${service} passes with every optional secret missing" \
    || ko "render check prod ${service} (status ${status}): ${out}"
done
for service in front-api frontend; do
  run render check beta "${service}"
  if [[ ${status} -ne 0 ]] && [[ "${out}" == *"::error::GOOGLE_SSO_CLIENT_ID is empty or missing from the beta GitHub Environment"* ]]; then
    pass "render: check without --if-present fails on the missing required GOOGLE_SSO_CLIENT_ID (${service})"
  else
    ko "render check beta ${service} without SSO (status ${status}): ${out}"
  fi
done

# apply with nothing set: the env file only gains the config tree import (a no-op without a tree).
mkdir -p "${FAKE_ROOT}/etc/open4goods"
printf 'JAVA_OPTS=-Xmx4g\n' > "${FAKE_ROOT}/etc/open4goods/ui.env"
run render --if-present apply beta ui
if [[ ${status} -eq 0 ]] \
  && [[ "$(cat "${FAKE_ROOT}/etc/open4goods/ui.env")" == $'JAVA_OPTS=-Xmx4g\nSPRING_CONFIG_IMPORT=optional:configtree:/etc/open4goods/secrets/ui/' ]] \
  && [[ ! -e "${FAKE_ROOT}/etc/open4goods/secrets" ]]; then
  pass "render: apply with nothing set writes only the config tree import, keeping the other lines"
else
  ko "render apply nothing set (status ${status}): ${out}"
fi
run render --if-present apply beta frontend
if [[ ${status} -eq 0 ]] && [[ ! -e "${FAKE_ROOT}/opt" ]] && [[ "${out}" != *"Updated keys"* ]]; then
  pass "render: frontend apply with nothing set writes nothing"
else
  ko "render frontend apply nothing set (status ${status}): ${out}"
fi

########################################
# render-service-secrets.sh: api, Google indexation JSON (path + line, GOU-933)
########################################
reset_host
API_IMPORT='SPRING_CONFIG_IMPORT=optional:configtree:/etc/open4goods/secrets/api/'
expect_fail "render: a malformed API_GOOGLE_API_JSON fails, even with --if-present" \
  render_with API_GOOGLE_API_JSON='{"type":"service_account"}' -- --if-present check prod api

run render_with API_GOOGLE_API_JSON="${VALID_JSON}" -- check prod api
if [[ ${status} -eq 0 ]] && [[ ! -s "${FAKE_SSH_LOG}" ]] && [[ ! -e "${FAKE_ROOT}/etc" ]] \
  && [[ "${out}" == *"- API_GOOGLE_API_JSON -> GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH (path-sa)"* ]]; then
  pass "render: check lists what would be rendered, uses no SSH and writes nothing"
else
  ko "render check (status ${status}): ${out}"
fi

mkdir -p "${FAKE_ROOT}/etc/open4goods"
printf 'JAVA_OPTS=-Xmx4g\nGOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH=/opt/open4goods/config/google-api.json\n' \
  > "${FAKE_ROOT}/etc/open4goods/api.env"
run render_with API_GOOGLE_API_JSON="${VALID_JSON}" -- apply prod api
json="${FAKE_ROOT}/etc/open4goods/secrets/google-indexation.json"
if [[ ${status} -eq 0 ]] && [[ "$(stat -c '%a' "${json}")" == 600 ]] \
  && jq -e '.type == "service_account"' "${json}" >/dev/null \
  && [[ "$(cat "${FAKE_ROOT}/etc/open4goods/api.env")" == $'JAVA_OPTS=-Xmx4g\nGOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH=/etc/open4goods/secrets/google-indexation.json\n'"${API_IMPORT}" ]]; then
  pass "render: apply writes the JSON (600) and api.env, keeping its other lines"
else
  ko "render apply (status ${status}): ${out} / $(cat "${FAKE_ROOT}/etc/open4goods/api.env")"
fi
if [[ "${out}" != *"89abcdef"* ]] && [[ "${out}" != *"${SENTINEL}"* ]] \
  && grep -q "indexation@nudger-test.iam.gserviceaccount.com (private_key_id 01234567...)" "${GITHUB_STEP_SUMMARY}" \
  && no_secret_in "${GITHUB_STEP_SUMMARY}" "${FAKE_SSH_LOG}"; then
  pass "render: summary shows identifiers only"
else
  ko "render summary: ${out}"
fi

########################################
# render-service-secrets.sh: api env lines (Metriks GOU-957, Effiliation feeds GOU-1014)
########################################
AWIN_SENTINEL="SENTINELAWINVALUE42"
EFFILIATION_SENTINEL="SENTINELEFFILIATIONVALUE42"
FEED_SENTINEL="SENTINELEFFILIATIONFEEDKEY42"
no_env_secret_in() {
  ! grep -q -e "${AWIN_SENTINEL}" -e "${EFFILIATION_SENTINEL}" -e "${FEED_SENTINEL}" "$@" 2>/dev/null
}

reset_host
mkdir -p "${FAKE_ROOT}/etc/open4goods"
printf 'JAVA_OPTS=-Xmx4g\nMETRIKS_AFFILIATION_AWIN_API_TOKEN=old\n' > "${FAKE_ROOT}/etc/open4goods/api.env"
run render_with API_METRIKS_AWIN_PUBLISHER_ID=100000 API_METRIKS_AWIN_API_TOKEN="${AWIN_SENTINEL}" -- --if-present apply beta api
api_env="$(cat "${FAKE_ROOT}/etc/open4goods/api.env")"
expected_env=$'JAVA_OPTS=-Xmx4g\nMETRIKS_AFFILIATION_AWIN_PUBLISHER_ID=100000\nMETRIKS_AFFILIATION_AWIN_API_TOKEN='"${AWIN_SENTINEL}"$'\n'"${API_IMPORT}"
if [[ ${status} -eq 0 ]] && [[ "${api_env}" == "${expected_env}" ]] \
  && [[ ! -e "${FAKE_ROOT}/etc/open4goods/secrets/google-indexation.json" ]]; then
  pass "render: the set Metriks secrets are rendered without the Google JSON, keeping api.env's other lines"
else
  ko "render Metriks only (status ${status}): ${out} / ${api_env}"
fi
for missing in API_GOOGLE_API_JSON API_METRIKS_EFFILIATION_API_KEY API_EFFILIATION_API_KEY API_VERTEX_API_JSON; do
  [[ "${out}" == *"::warning::${missing} is not set in the beta GitHub Environment"* ]] \
    && pass "render: missing ${missing} is a warning" || ko "render: no warning for missing ${missing}: ${out}"
done
if [[ "${out}" != *"::error::"* ]] \
  && grep -q "Not set in beta, not rendered: API_GOOGLE_API_JSON API_METRIKS_EFFILIATION_API_KEY API_EFFILIATION_API_KEY API_VERTEX_API_JSON" "${GITHUB_STEP_SUMMARY}"; then
  pass "render: summary lists the secrets not rendered"
else
  ko "render summary of missing secrets: $(cat "${GITHUB_STEP_SUMMARY}")"
fi
[[ "${out}" != *"${AWIN_SENTINEL}"* ]] && no_env_secret_in "${GITHUB_STEP_SUMMARY}" "${FAKE_SSH_LOG}" \
  && pass "render: env line values absent from the output, the summary and the SSH command line" \
  || ko "render: an env line value leaked"

run render_with API_EFFILIATION_API_KEY="${FEED_SENTINEL}" -- --if-present apply prod api
if [[ ${status} -eq 0 ]] && grep -qx "AFFILIATIONCONFIG_EFFILIATIONAPIKEY=${FEED_SENTINEL}" "${FAKE_ROOT}/etc/open4goods/api.env" \
  && grep -qx "METRIKS_AFFILIATION_AWIN_PUBLISHER_ID=100000" "${FAKE_ROOT}/etc/open4goods/api.env" \
  && [[ "$(grep -c '^SPRING_CONFIG_IMPORT=' "${FAKE_ROOT}/etc/open4goods/api.env")" == 1 ]] \
  && [[ "${out}" != *"${FEED_SENTINEL}"* ]]; then
  pass "render: API_EFFILIATION_API_KEY alone adds its line, keeps the previous render, one import line"
else
  ko "render Effiliation feeds key (status ${status}): ${out}"
fi

reset_host
case_index=0
for bad in "API_METRIKS_AWIN_API_TOKEN=${AWIN_SENTINEL}+x" "API_METRIKS_AWIN_API_TOKEN=${AWIN_SENTINEL} x" \
  "API_METRIKS_EFFILIATION_API_KEY=${EFFILIATION_SENTINEL};x" "API_METRIKS_AWIN_PUBLISHER_ID=1/2" \
  "API_EFFILIATION_API_KEY=${FEED_SENTINEL}\$x" "API_EFFILIATION_API_KEY=${FEED_SENTINEL}:x"; do
  case_index=$((case_index + 1))
  run render_with "${bad}" -- --if-present apply prod api
  if [[ ${status} -ne 0 ]] && no_env_secret_in <(printf '%s' "${out}") \
    && [[ ! -s "${FAKE_SSH_LOG}" ]] && [[ ! -e "${FAKE_ROOT}/etc" ]]; then
    pass "render: rejects malformed ${bad%%=*} (case ${case_index}) without writing or printing it"
  else
    ko "render malformed ${bad%%=*} (case ${case_index}, status ${status}): ${out}"
  fi
done

########################################
# render-service-secrets.sh: config tree (GOU-1041)
########################################
TREE_SENTINEL="SENTINELTREEVALUE42"
# A value no env line can carry: quotes, spaces, '$', '#', ':', '=' and a newline.
TREE_VALUE=$'pa ss"w\'ord $HOME #x: a=b\n'"${TREE_SENTINEL}"

reset_host
mkdir -p "${FAKE_ROOT}/etc/open4goods"
printf 'JAVA_OPTS=-Xmx512m\n' > "${FAKE_ROOT}/etc/open4goods/sbadmin.env"
run render_with O4G_SBA_PASSWORD="${TREE_VALUE}" -- check prod admin
if [[ ${status} -eq 0 ]] && [[ "${out}" == *"- O4G_SBA_PASSWORD -> o4g.sba.registration.password (tree)"* ]] \
  && [[ "${out}" == *"::warning::O4G_MAIL_PASSWORD is not set"* ]] && [[ ! -s "${FAKE_SSH_LOG}" ]]; then
  pass "render: a tree secret accepts any value in check"
else
  ko "render tree check (status ${status}): ${out}"
fi
run render_with O4G_SBA_PASSWORD="${TREE_VALUE}" -- apply prod admin
tree="${FAKE_ROOT}/etc/open4goods/secrets/sbadmin"
if [[ ${status} -eq 0 ]] \
  && cmp -s "${tree}/spring.boot.admin.instance-auth.default-password" <(printf '%s' "${TREE_VALUE}") \
  && cmp -s "${tree}/o4g.sba.registration.password" <(printf '%s' "${TREE_VALUE}") \
  && [[ "$(stat -c '%a' "${tree}/o4g.sba.registration.password")" == 600 ]] \
  && [[ "$(stat -c '%a' "${tree}")" == 700 ]] \
  && [[ ! -e "${tree}/spring.mail.password" ]] \
  && [[ "$(cat "${FAKE_ROOT}/etc/open4goods/sbadmin.env")" == $'JAVA_OPTS=-Xmx512m\nSPRING_CONFIG_IMPORT=optional:configtree:/etc/open4goods/secrets/sbadmin/' ]]; then
  pass "render: admin (sbadmin) tree files hold the exact value (600, directory 700), sbadmin.env imports the tree"
else
  ko "render admin tree (status ${status}): ${out} / $(ls -la "${tree}" 2>&1)"
fi
if [[ "${out}" != *"${TREE_SENTINEL}"* ]] && ! grep -q "${TREE_SENTINEL}" "${GITHUB_STEP_SUMMARY}" "${FAKE_SSH_LOG}"; then
  pass "render: tree values absent from the output, the summary and the SSH command line"
else
  ko "render: a tree value leaked"
fi

reset_host
expect_fail "render: a malformed tree-sa secret fails before anything is written" \
  render_with API_VERTEX_BATCH_JSON="{\"private_key\": \"${SENTINEL}\"" -- --if-present apply prod api
[[ ! -e "${FAKE_ROOT}/etc" ]] && [[ ! -s "${FAKE_SSH_LOG}" ]] && pass "render: nothing written for a malformed tree-sa secret" \
  || ko "render: something was written for a malformed tree-sa secret"
run render_with API_VERTEX_API_JSON="${VALID_JSON}" O4G_INTERNAL_API_KEY="${TREE_VALUE}" API_METRIKS_KEY="${TREE_VALUE}" \
  -- --if-present apply prod api
if [[ ${status} -eq 0 ]] \
  && jq -e '.client_email == "indexation@nudger-test.iam.gserviceaccount.com"' \
    "${FAKE_ROOT}/etc/open4goods/secrets/api/gen-ai-config.google-api-json" >/dev/null \
  && cmp -s "${FAKE_ROOT}/etc/open4goods/secrets/api/admin-key" <(printf '%s' "${TREE_VALUE}") \
  && cmp -s "${FAKE_ROOT}/etc/open4goods/secrets/api/metriks-key" <(printf '%s' "${TREE_VALUE}") \
  && [[ "$(stat -c '%a' "${FAKE_ROOT}/etc/open4goods/secrets/api/metriks-key")" == 600 ]] \
  && [[ "$(cat "${FAKE_ROOT}/etc/open4goods/api.env")" == "${API_IMPORT}" ]] \
  && grep -q "API_VERTEX_API_JSON: service account indexation@nudger-test.iam.gserviceaccount.com (private_key_id 01234567...)" "${GITHUB_STEP_SUMMARY}" \
  && [[ "${out}" != *"${SENTINEL}"* ]] && [[ "${out}" != *"${TREE_SENTINEL}"* ]]; then
  pass "render: api tree-sa and tree files are written, with identifiers only in the summary"
else
  ko "render api tree (status ${status}): ${out}"
fi

# api: Google SSO client pair and Spring Boot Admin monitor on Actuator (GOU-1393).
for half in "GOOGLE_SSO_CLIENT_ID=client-id.apps.example" "GOOGLE_SSO_CLIENT_SECRET=${TREE_VALUE}"; do
  reset_host
  run render_with "${half}" -- --if-present apply prod api
  if [[ ${status} -ne 0 ]] \
    && [[ "${out}" == *"GOOGLE_SSO_CLIENT_ID and GOOGLE_SSO_CLIENT_SECRET go together for api"* ]] \
    && [[ "${out}" != *"${TREE_SENTINEL}"* ]] && [[ ! -s "${FAKE_SSH_LOG}" ]] && [[ ! -e "${FAKE_ROOT}/etc" ]]; then
    pass "render: api refuses half of the Google SSO pair (${half%%=*}) before anything is written"
  else
    ko "render api half SSO pair ${half%%=*} (status ${status}): ${out}"
  fi
done
reset_host
run render_with GOOGLE_SSO_CLIENT_ID=client-id.apps.example GOOGLE_SSO_CLIENT_SECRET="${TREE_VALUE}" \
  O4G_SBA_PASSWORD="${TREE_VALUE}" -- --if-present apply prod api
tree="${FAKE_ROOT}/etc/open4goods/secrets/api"
if [[ ${status} -eq 0 ]] \
  && cmp -s "${tree}/spring.security.oauth2.client.registration.google.client-id" <(printf '%s' client-id.apps.example) \
  && cmp -s "${tree}/spring.security.oauth2.client.registration.google.client-secret" <(printf '%s' "${TREE_VALUE}") \
  && cmp -s "${tree}/o4g.actuator.monitor.password" <(printf '%s' "${TREE_VALUE}") \
  && cmp -s "${tree}/spring.boot.admin.client.password" <(printf '%s' "${TREE_VALUE}") \
  && [[ "$(cat "${FAKE_ROOT}/etc/open4goods/api.env")" == "${API_IMPORT}" ]] \
  && [[ "${out}" != *"${TREE_SENTINEL}"* ]] && ! grep -q "${TREE_SENTINEL}" "${GITHUB_STEP_SUMMARY}" "${FAKE_SSH_LOG}"; then
  pass "render: api receives the Google SSO pair and the Actuator monitor password in its tree"
else
  ko "render api SSO and monitor tree (status ${status}): ${out} / $(ls -la "${tree}" 2>&1)"
fi

########################################
# render-service-secrets.sh: frontend env lines and coupled secrets
########################################
MACHINE_SENTINEL="SENTINELMACHINETOKEN42"
reset_host
frontend_env="${FAKE_ROOT}/opt/open4goods/config/beta/frontend/.env"
mkdir -p "$(dirname "${frontend_env}")"
printf 'API_URL=https://beta.front-api.example\nMACHINE_TOKEN=old\n' > "${frontend_env}"
run render_with GOOGLE_SSO_CLIENT_ID=client-id.apps.example GOOGLE_SSO_CLIENT_SECRET=client-secret \
  O4G_MACHINE_TOKEN="${MACHINE_SENTINEL}" FRONT_API_JWT_SECRET="${MACHINE_SENTINEL}jwt" -- apply beta frontend
expected_frontend=$'API_URL=https://beta.front-api.example\nNUXT_GOOGLE_OAUTH_CLIENT_ID=client-id.apps.example\nNUXT_GOOGLE_OAUTH_CLIENT_SECRET=client-secret\nNUXT_MACHINE_TOKEN='"${MACHINE_SENTINEL}"$'\nMACHINE_TOKEN='"${MACHINE_SENTINEL}"$'\nFRONT_SECURITY_JWT_SECRET='"${MACHINE_SENTINEL}jwt"
if [[ ${status} -eq 0 ]] && [[ "$(cat "${frontend_env}")" == "${expected_frontend}" ]] \
  && [[ "$(stat -c '%a' "${frontend_env}")" == 600 ]] && [[ "${out}" != *"${MACHINE_SENTINEL}"* ]] \
  && ! grep -q "${MACHINE_SENTINEL}" "${GITHUB_STEP_SUMMARY}" "${FAKE_SSH_LOG}" && [[ ! -e "${FAKE_ROOT}/etc" ]]; then
  pass "render: frontend env lines (SSO, machine token twice, JWT secret) replace the old ones, nothing else written"
else
  ko "render frontend (status ${status}): ${out} / $(cat "${frontend_env}")"
fi

reset_host
run render_with GOOGLE_SSO_CLIENT_ID=client-id.apps.example O4G_MACHINE_TOKEN="${MACHINE_SENTINEL}+x" -- check prod front-api
[[ ${status} -eq 0 ]] && pass "render: front-api takes any machine token value (tree)" \
  || ko "render front-api machine token (status ${status}): ${out}"
run render_with GOOGLE_SSO_CLIENT_ID=client-id.apps.example GOOGLE_SSO_CLIENT_SECRET=client-secret \
  O4G_MACHINE_TOKEN="${MACHINE_SENTINEL}+x" -- check prod frontend
if [[ ${status} -ne 0 ]] && [[ "${out}" == *"O4G_MACHINE_TOKEN contains characters outside [A-Za-z0-9/._-]"* ]] \
  && [[ "${out}" != *"${MACHINE_SENTINEL}"* ]]; then
  pass "render: the frontend rejects a machine token an env line cannot carry"
else
  ko "render frontend machine token (status ${status}): ${out}"
fi

########################################
# Workflows: every secret of the table is mapped where its service is rendered
########################################
# Each workflow step that runs the script must map, in its env, every secret of the services it renders.
run python3 - "${RENDER}" "${ROOT}/.github/workflows" <<'PY'
import json, pathlib, re, sys
import yaml

render, workflows = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
table, source = {}, {}
for service, secret, need in re.findall(
        r"(?m)^(api|ui|front-api|admin|b2b-api|frontend|infra) ([A-Z0-9_]+) \S+ \S+ (\S+)$", render.read_text()):
    # `source` rows are only rendered with --source (infraSecrets.yml), which renders nothing else.
    (source if need == "source" else table).setdefault(service, set()).add(secret)
    table.setdefault(service, set())
errors, steps = [], 0
for workflow in sorted(workflows.glob("*.yml")):
    for job in (yaml.safe_load(workflow.read_text()).get("jobs") or {}).values():
        for step in job.get("steps", []):
            script = step.get("run") or ""
            if "render-service-secrets.sh" not in script:
                continue
            steps += 1
            if "${SERVICES}" in script:
                services = set(table)
            else:
                loop = re.search(r"for svc in ([a-z2 -]+); do", script)
                services = set(loop.group(1).split()) if loop else {script.split()[-1]}
            env = step.get("env") or {}
            for service in sorted(services):
                # --source renders the `source` rows only; without it, every other row.
                needed = source.get(service, set()) if "--source" in script else table[service]
                for secret in sorted(needed):
                    value = str(env.get(secret, ""))
                    # serviceSecrets.yml maps a secret only for the services listed in its contains(...).
                    listed = re.search(r"contains\(fromJSON\('(\[[^']*\])'\), inputs\.service\)", value)
                    if f"secrets.{secret}" not in value or (listed and service not in json.loads(listed.group(1))):
                        errors.append(f"{workflow.name} / {step.get('name')}: {secret} ({service}) not mapped")
print("\n".join(errors) or f"{steps} steps checked")
sys.exit(1 if errors or steps < 6 else 0)
PY
[[ ${status} -eq 0 ]] && pass "workflows map every secret of the services they render (${out})" \
  || ko "workflow secret mapping: ${out}"

if [[ ${failures} -gt 0 ]]; then
  echo "${failures} test(s) failed"
  exit 1
fi
echo "push-remote-file test passed"
