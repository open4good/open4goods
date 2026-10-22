#!/bin/sh
# Merge KEY=VALUE lines into an env file on a deploy host (ADR-0006, GOU-767).
#
# Usage: push-remote-env.sh <remote-env-file> <KEY>...
#
# Each KEY is read from this process' environment (a GitHub Environment secret
# mapped through the step `env:`). Values never reach a command line, an
# artifact or a log: they are written to a umask 077 temp file on the runner
# and streamed over SSH stdin. On the host, the other lines of the file are
# kept, the given keys are replaced, and the result is moved into place
# atomically (mode 600, owner of the previous file kept).
#
# Required environment: DEPLOY_SSH_HOST, DEPLOY_SSH_USER, DEPLOY_SSH_KEY.
set -eu

fail() { echo "::error::$*" >&2; exit 1; }

[ "$#" -ge 2 ] || fail "Usage: $0 <remote-env-file> <KEY>..."
remote_file=$1
shift

# The path and key names are interpolated into the remote command: allow only safe characters.
case "$remote_file" in
  /*) ;;
  *) fail "Remote env file must be an absolute path" ;;
esac
case "$remote_file" in
  *[!A-Za-z0-9/._-]* | *..*) fail "Unsupported remote env file path" ;;
esac

: "${DEPLOY_SSH_HOST:?DEPLOY_SSH_HOST is required}"
: "${DEPLOY_SSH_USER:?DEPLOY_SSH_USER is required}"
: "${DEPLOY_SSH_KEY:?DEPLOY_SSH_KEY is required}"

umask 077
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

pattern=
for key in "$@"; do
  case "$key" in
    '' | [!A-Z]* | *[!A-Z0-9_]*) fail "Invalid key name: $key" ;;
  esac
  value=$(printenv "$key" || true)
  [ -n "$value" ] || fail "$key is empty or missing from the job environment"
  # Values are written unquoted (docker compose env_file) and sourced by publish-jars.sh.
  # '/' is allowed for file paths (GOU-933).
  case "$value" in
    *[!A-Za-z0-9/._-]*) fail "$key contains characters outside [A-Za-z0-9/._-]" ;;
  esac
  printf '%s=%s\n' "$key" "$value" >> "$work/keys.env"
  pattern="${pattern:+$pattern|}$key"
done

printf '%s\n' "$DEPLOY_SSH_KEY" > "$work/id"

remote_script="set -eu
umask 077
f='$remote_file'
mkdir -p \"\$(dirname \"\$f\")\"
tmp=\$(mktemp \"\$f.XXXXXX\")
trap 'rm -f \"\$tmp\"' EXIT
if [ -f \"\$f\" ]; then
  grep -v -E '^(export )?($pattern)=' \"\$f\" > \"\$tmp\" || true
  chown --reference=\"\$f\" \"\$tmp\" 2>/dev/null || true
fi
cat >> \"\$tmp\"
mv \"\$tmp\" \"\$f\"
echo \"Updated keys in \$f (\$(stat -c '%a %U:%G' \"\$f\")): $*\""

ssh -i "$work/id" \
  -o BatchMode=yes \
  -o StrictHostKeyChecking=accept-new \
  -o UserKnownHostsFile="$work/known_hosts" \
  "$DEPLOY_SSH_USER@$DEPLOY_SSH_HOST" "$remote_script" < "$work/keys.env"
