#!/usr/bin/env python3
"""Validate the name-only runtime input contract used by beta and production."""

from __future__ import annotations

import sys
from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[2]
CONTRACT_PATH = ROOT / "ops" / "config" / "runtime-input-contract.yml"
STORES = {"vars", "secrets"}
ENVIRONMENTS = {"beta", "prod"}


def main() -> int:
    data = yaml.safe_load(CONTRACT_PATH.read_text(encoding="utf-8")) or {}
    problems: list[str] = []
    if data.get("version") != 1:
        problems.append("contract version must be 1")
    services = data.get("services")
    if not isinstance(services, dict) or not services:
        problems.append("services must be a non-empty mapping")
        services = {}
    for service, declaration in services.items():
        if declaration.get("defaultSource") != "packaged":
            problems.append(f"{service}: defaultSource must be packaged")
        if declaration.get("requiredWhen") != "service-enabled":
            problems.append(f"{service}: requiredWhen must be service-enabled")
        properties: set[str] = set()
        inputs: set[str] = set()
        for entry in declaration.get("inputs") or []:
            property_name = entry.get("property")
            input_name = entry.get("input")
            store = entry.get("store")
            environments = set(entry.get("environments") or [])
            if not isinstance(property_name, str) or not property_name:
                problems.append(f"{service}: input property is required")
                continue
            if property_name in properties:
                problems.append(f"{service}: duplicate property {property_name}")
            properties.add(property_name)
            if not isinstance(input_name, str) or not input_name:
                problems.append(f"{service}: {property_name} has no input name")
            elif input_name in inputs:
                problems.append(f"{service}: duplicate input name {input_name}")
            else:
                inputs.add(input_name)
            if store not in STORES:
                problems.append(f"{service}: {property_name} has invalid store {store!r}")
            if not environments or not environments <= ENVIRONMENTS:
                problems.append(f"{service}: {property_name} has invalid environments")
    if problems:
        print("Runtime input contract FAILED:", file=sys.stderr)
        for problem in problems:
            print(f"  {problem}", file=sys.stderr)
        return 1
    print(f"OK: runtime input contract covers {len(services)} services.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
