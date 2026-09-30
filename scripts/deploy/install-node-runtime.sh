#!/usr/bin/env bash
# Install a checksum-verified Node.js binary for beta's systemd Nuxt services.
set -euo pipefail

usage() {
  echo "usage: $0 --version VERSION [--prefix DIRECTORY]" >&2
  exit 2
}

version=''
prefix='/opt/open4goods'
while (($#)); do
  case "$1" in
    --version) version="${2:-}"; shift 2 ;;
    --prefix) prefix="${2:-}"; shift 2 ;;
    *) usage ;;
  esac
done
[[ "$version" =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]] || usage
[[ "$(id -u)" == '0' ]] || { echo 'Node runtime installation must run as root' >&2; exit 1; }

case "$(uname -m)" in
  x86_64) platform='linux-x64' ;;
  aarch64) platform='linux-arm64' ;;
  *) echo "unsupported Node architecture: $(uname -m)" >&2; exit 1 ;;
esac

archive="node-${version}-${platform}.tar.xz"
base_url="https://nodejs.org/dist/${version}"
destination="${prefix}/node-${version}-${platform}"
if [[ -x "$destination/bin/node" ]]; then
  "$destination/bin/node" --version | grep -qx "$version"
else
  stage="$(mktemp -d)"
  trap 'rm -rf -- "$stage"' EXIT
  curl --fail --silent --show-error --location --proto '=https' --tlsv1.2 \
    "${base_url}/SHASUMS256.txt" -o "$stage/SHASUMS256.txt"
  curl --fail --silent --show-error --location --proto '=https' --tlsv1.2 \
    "${base_url}/${archive}" -o "$stage/${archive}"
  (cd "$stage" && grep -F "  ${archive}" SHASUMS256.txt | sha256sum --check --status -)
  tar -C "$stage" -xf "$stage/${archive}"
  install -d -m 0755 "$prefix"
  mv "$stage/node-${version}-${platform}" "$destination"
  trap - EXIT
fi

for executable in node npm npx corepack; do
  ln -sfn "$destination/bin/${executable}" "/usr/local/bin/${executable}"
done
/usr/local/bin/node --version | grep -qx "$version"
echo "installed Node ${version} at ${destination}"
