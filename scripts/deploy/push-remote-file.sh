#!/bin/sh
# Write the value of an environment variable into a file on a deploy host (ADR-0006, GOU-933).
#
# Usage: push-remote-file.sh [--service-account-json] [--exact] [--validate-only] <remote-file> <VAR>
#
# VAR is read from this process' environment (a GitHub Environment secret mapped
# through the step `env:`). The value never reaches a command line, an artifact
# or a log: it is written to a umask 077 temp file on the runner and streamed
# over SSH stdin. On the host, the parent directory is created if needed (mode
# 700 when new), and the file is replaced atomically (mode 600, owner of the
# previous file kept).
#
# Options:
#   --service-account-json  The value must be a Google service account JSON key:
#                           valid JSON, "type" == "service_account", non-empty
#                           "client_email" and "private_key". Checked on the
#                           runner; neither the value nor the parser message is
#                           printed.
#   --exact                 Write the value as is, without the trailing newline
#                           otherwise added: a Spring Boot config tree file
#                           (GOU-1041), whose content is the property value.
#   --validate-only         Check the path and the value, then exit without SSH.
#
# Required environment (unless --validate-only): DEPLOY_SSH_HOST,
# DEPLOY_SSH_USER, DEPLOY_SSH_KEY.
set -eu

fail() { echo "::error::$*" >&2; exit 1; }

service_account_json=false
exact=false
validate_only=false
while [ "$#" -gt 0 ]; do
  case "$1" in
    --service-account-json) service_account_json=true ;;
    --exact) exact=true ;;
    --validate-only) validate_only=true ;;
    --) shift; break ;;
    -*) fail "Unknown option: $1" ;;
    *) break ;;
  esac
  shift
done

[ "$#" -eq 2 ] || fail "Usage: $0 [--service-account-json] [--exact] [--validate-only] <remote-file> <VAR>"
remote_file=$1
var=$2

# The path is interpolated into the remote command: allow only safe characters under /etc/open4goods/.
case "$remote_file" in
  /etc/open4goods/?*) ;;
  *) fail "Remote file must be under /etc/open4goods/" ;;
esac
case "$remote_file" in
  *[!A-Za-z0-9/._-]* | *..* | *//* | */ | */.* ) fail "Unsupported remote file path" ;;
esac
case "$var" in
  '' | [!A-Z]* | *[!A-Z0-9_]*) fail "Invalid variable name: $var" ;;
esac

umask 077
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

# printf is a shell builtin: the value is not passed to a new process' command line.
value=$(printenv "$var" || true)
[ -n "$value" ] || fail "$var is empty or missing from the job environment"
if [ "$exact" = true ]; then
  printf '%s' "$value" > "$work/content"
else
  printf '%s\n' "$value" > "$work/content"
fi
unset value

if [ "$service_account_json" = true ]; then
  command -v jq >/dev/null 2>&1 || fail "jq is required to validate $var"
  # Exactly one JSON object with the fields google-auth-library needs; output and parser errors discarded.
  if ! jq -e -s '
      length == 1
      and (.[0] | type == "object")
      and .[0].type == "service_account"
      and (.[0].client_email | type == "string" and length > 0)
      and (.[0].private_key | type == "string" and length > 0)
    ' "$work/content" >/dev/null 2>&1; then
    fail "$var is not a Google service account JSON key (valid JSON, type service_account, client_email, private_key)"
  fi
fi

if [ "$validate_only" = true ]; then
  echo "$var is valid for $remote_file (not written: validation only)."
  exit 0
fi

: "${DEPLOY_SSH_HOST:?DEPLOY_SSH_HOST is required}"
: "${DEPLOY_SSH_USER:?DEPLOY_SSH_USER is required}"
: "${DEPLOY_SSH_KEY:?DEPLOY_SSH_KEY is required}"

printf '%s\n' "$DEPLOY_SSH_KEY" > "$work/id"

remote_script="set -eu
umask 077
f='$remote_file'
mkdir -p \"\$(dirname \"\$f\")\"
tmp=\$(mktemp \"\$f.XXXXXX\")
trap 'rm -f \"\$tmp\"' EXIT
if [ -f \"\$f\" ]; then
  chown --reference=\"\$f\" \"\$tmp\" 2>/dev/null || true
fi
cat > \"\$tmp\"
chmod 600 \"\$tmp\"
mv \"\$tmp\" \"\$f\"
echo \"Wrote \$f (\$(stat -c '%a %U:%G' \"\$f\"))\""

ssh -i "$work/id" \
  -o BatchMode=yes \
  -o StrictHostKeyChecking=accept-new \
  -o UserKnownHostsFile="$work/known_hosts" \
  "$DEPLOY_SSH_USER@$DEPLOY_SSH_HOST" "$remote_script" < "$work/content"
