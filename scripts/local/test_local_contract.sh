#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

bash -n "$ROOT/scripts/local/open4goods.sh"
docker compose --project-directory "$ROOT" --env-file "$ROOT/.env.local.example" config --quiet
docker compose --project-directory "$ROOT" --env-file "$ROOT/.env.buildhost.example" config --quiet
python3 -m unittest "$ROOT/scripts/local/test_product_backup_sample.py"

if rg -n 'devsec|https?://[^ ]*(beta\.)?nudger\.fr' \
  "$ROOT/scripts/local/open4goods.sh" "$ROOT/scripts/local/product_backup_sample.py" "$ROOT/ops/local" \
  "$ROOT/api/src/main/resources/application-local.yml" \
  "$ROOT/admin/src/main/resources/application-local.yml" \
  "$ROOT/front-api/src/main/resources/application-local.yml" \
  "$ROOT/ui/src/main/resources/application-local.yml" \
  "$ROOT/b2b-api/src/main/resources/application-local.yml" \
  "$ROOT/services/exposed-docs/src/main/resources/application-local.yml" \
  "$ROOT/services/geocode/src/main/resources/application-local.yml"; then
  echo "strict-local contract contains a devsec or Nudger runtime dependency" >&2
  exit 1
fi

for port in 3000 3001 8081 8082 8085 8086 8087 8088 8089; do
  rg -q "${port}" "$ROOT/scripts/local/open4goods.sh" || {
    echo "missing local application port: $port" >&2
    exit 1
  }
done

rg -q 'generate:api.*http://localhost:8086/v3/api-docs/front' "$ROOT/frontend/package.json"
rg -q 'BACKEND_OPENAPI_URL="http://localhost:\$b2b_api_port/v3/api-docs"' "$ROOT/scripts/local/open4goods.sh"

# Shared-build-host profile: every buildhost app port stays inside the
# assigned loopback block, and none reuse the runtime's historical defaults.
for port in 4100 4101 4102 4103 4104 4105 4106 4107 4108; do
  rg -q "O4G_PORT_.*=${port}$" "$ROOT/.env.buildhost.example" || {
    echo "missing buildhost application port: $port" >&2
    exit 1
  }
done

# Shared-host preflight gate: rootless-only Docker, assigned port block,
# disk/RAM/CPU/pids budget, and the serialized full-run lock must all exist.
for symbol in 'preflight()' 'O4G_SHARED_HOST' 'cgroup_effective_max' \
  'rootless' 'with_full_run_lock' 'full-run.lock'; do
  rg -q -- "$symbol" "$ROOT/scripts/local/open4goods.sh" || {
    echo "missing shared-host preflight contract: $symbol" >&2
    exit 1
  }
done

echo "OK: strict-local runtime contract"
