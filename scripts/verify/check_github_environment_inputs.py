#!/usr/bin/env python3
"""Verify required GitHub Environment input names without retrieving their values."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[2]
MANIFEST_PATH = ROOT / "ops" / "config" / "deployment-inputs.yml"
RUNTIME_CONTRACT_PATH = ROOT / "ops" / "config" / "runtime-input-contract.yml"


def names_in(store: str, environment: str) -> set[str]:
    """Return Environment input names from GitHub's name-only API response."""
    command = ["gh", store, "list", "--repo", "open4good/open4goods", "--env", environment, "--json", "name"]
    completed = subprocess.run(command, check=False, capture_output=True, text=True)
    if completed.returncode:
        raise RuntimeError(completed.stderr.strip() or f"gh {store} list failed")
    return {entry["name"] for entry in json.loads(completed.stdout)}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--environment", required=True, choices=("beta", "prod"))
    args = parser.parse_args()

    manifest = yaml.safe_load(MANIFEST_PATH.read_text(encoding="utf-8")) or {}
    inputs = manifest.get("inputs") or {}
    stores = {"secret": "secrets", "variable": "vars"}
    required = {
        store: {
            name
            for name, declaration in inputs.items()
            if args.environment in declaration.get("environments", []) and declaration.get("required", True)
            and declaration.get("store") == manifest_store
        }
        for store, manifest_store in stores.items()
    }
    runtime_contract = yaml.safe_load(RUNTIME_CONTRACT_PATH.read_text(encoding="utf-8")) or {}
    for declaration in (runtime_contract.get("services") or {}).values():
        for entry in declaration.get("inputs") or []:
            if args.environment not in entry.get("environments", []) or entry.get("deferredTo"):
                continue
            required["secret" if entry["store"] == "secrets" else "variable"].add(entry["input"])
    available = {store: names_in(store, args.environment) for store in required}
    missing = {store: sorted(required[store] - available[store]) for store in required}
    if any(missing.values()):
        for store, names in missing.items():
            if names:
                print(f"{args.environment}: missing {store} names: {', '.join(names)}", file=sys.stderr)
        return 1
    print(
        f"OK: {args.environment} Environment has {len(required['secret'])} required secret names and "
        f"{len(required['variable'])} required variable names."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
