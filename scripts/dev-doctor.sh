#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

print_header() {
    printf "\n=== %s ===\n" "$1"
}

check_command() {
    local name=$1
    local cmd=$2

    if command -v "$cmd" >/dev/null 2>&1; then
        printf "[OK] %s: %s\n" "$name" "$($cmd --version 2>/dev/null | head -n1)"
    else
        printf "[MISSING] %s not found in PATH\n" "$name"
        return 1
    fi
}

print_header "Tooling versions"
check_command "Java" "java" || true
check_command "Maven" "mvn" || true
check_command "Node" "node" || true
check_command "pnpm" "pnpm" || true

print_header "Version guards"

java_version_ok=true
if command -v java >/dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | head -n1 | awk -F '"' '{print $2}')
    JAVA_MAJOR=${JAVA_VERSION%%.*}
    if [[ ${JAVA_MAJOR} -lt 21 ]]; then
        echo "[WARN] Java ${JAVA_VERSION} detected. Please install Java 21+."
        java_version_ok=false
    else
        echo "[OK] Java ${JAVA_VERSION} >= 21"
    fi
else
    echo "[WARN] Java missing; install Java 21+"
    java_version_ok=false
fi

node_version_ok=true
if command -v node >/dev/null 2>&1; then
    NODE_VERSION=$(node -v | sed 's/^v//')
    NODE_MAJOR=${NODE_VERSION%%.*}
    if [[ ${NODE_MAJOR} -lt 20 ]]; then
        echo "[WARN] Node ${NODE_VERSION} detected. Please install Node >=20."
        node_version_ok=false
    else
        echo "[OK] Node ${NODE_VERSION} >= 20"
    fi
else
    echo "[WARN] Node missing; install Node >=20"
    node_version_ok=false
fi

PNPM_MINIMUM="10.12.1"
if command -v pnpm >/dev/null 2>&1; then
    PNPM_VERSION=$(pnpm -v)
    if [[ "$(printf '%s\n' "${PNPM_MINIMUM}" "${PNPM_VERSION}" | sort -V | head -n1)" != "${PNPM_MINIMUM}" ]]; then
        echo "[WARN] pnpm ${PNPM_VERSION} detected; expected >= ${PNPM_MINIMUM}." \
             "Run: npm install -g pnpm@${PNPM_MINIMUM}"
    else
        echo "[OK] pnpm ${PNPM_VERSION} >= ${PNPM_MINIMUM}"
    fi
else
    echo "[WARN] pnpm missing; install pnpm@${PNPM_MINIMUM}"
fi

print_header "Environment variables"
missing_env=false
required_env=(API_URL TOKEN_COOKIE_NAME REFRESH_COOKIE_NAME MACHINE_TOKEN GH_TOKEN MCP_GITHUB_TOKEN)
for var in "${required_env[@]}"; do
    if [[ -z "${!var:-}" ]]; then
        echo "[WARN] ${var} is not set"
        missing_env=true
    else
        echo "[OK] ${var} is set"
    fi
done

print_header "Frontend dependencies (presence check)"
if [[ -f "${PROJECT_ROOT}/frontend/pnpm-lock.yaml" ]]; then
    echo "[OK] pnpm-lock.yaml present"
else
    echo "[WARN] pnpm-lock.yaml missing in frontend"
fi

print_header "Summary"
if [[ ${java_version_ok} == true && ${node_version_ok} == true && ${missing_env} == false ]]; then
    echo "Doctor checks passed."
else
    echo "Doctor checks completed with warnings above."
fi
