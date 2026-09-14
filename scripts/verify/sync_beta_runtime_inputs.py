#!/usr/bin/env python3
"""Copy declared beta runtime inputs from the private host to GitHub by name only."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[2]
CONTRACT_PATH = ROOT / "ops" / "config" / "runtime-input-contract.yml"
REPOSITORY = "open4good/open4goods"
REMOTE_LOOKUP = r'''
import json
import sys
from pathlib import Path
import yaml

service, property_name = sys.argv[1:]
config_root = Path("/opt/open4goods/config/beta") / service
path = config_root / (".env" if service in {"frontend", "b2b-frontend", "infra"} else "application-active.yml")
if not path.exists():
    raise SystemExit(3)
if path.name == ".env":
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            values[key] = value
else:
    values = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
current = values
for part in property_name.split("."):
    if not isinstance(current, dict) or part not in current:
        raise SystemExit(3)
    current = current[part]
if not isinstance(current, (str, int, float, bool)):
    raise SystemExit(4)
sys.stdout.write(json.dumps(str(current)))
'''


def read_value(target: str, service: str, property_name: str) -> str | None:
    """Read one scalar configuration value without displaying it locally."""
    completed = subprocess.run(
        ["ssh", "-o", "BatchMode=yes", target, "python3", "-", service, property_name],
        check=False,
        capture_output=True,
        text=True,
        input=REMOTE_LOOKUP,
    )
    if completed.returncode in {3, 4}:
        return None
    if completed.returncode:
        raise RuntimeError(completed.stderr.strip() or f"could not read {service}.{property_name}")
    return json.loads(completed.stdout)


def write_value(store: str, name: str, value: str) -> None:
    """Write a value through stdin so it cannot appear in process arguments or output."""
    command = ["gh", "secret" if store == "secrets" else "variable", "set", name, "--repo", REPOSITORY, "--env", "beta"]
    completed = subprocess.run(command, check=False, input=value, text=True, capture_output=True)
    if completed.returncode:
        raise RuntimeError(completed.stderr.strip() or f"could not set {name}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ssh-target", required=True, help="private beta SSH target")
    parser.add_argument("--apply", action="store_true", help="write beta Environment inputs; default is a name-only dry run")
    args = parser.parse_args()

    data = yaml.safe_load(CONTRACT_PATH.read_text(encoding="utf-8")) or {}
    populated = 0
    unavailable: list[str] = []
    for service, declaration in (data.get("services") or {}).items():
        for entry in declaration.get("inputs") or []:
            if "beta" not in entry["environments"]:
                continue
            label = f"{service}.{entry['property']}"
            value = read_value(args.ssh_target, service, entry["property"])
            if value is None:
                unavailable.append(label)
                continue
            if args.apply:
                write_value(entry["store"], entry["input"], value)
            populated += 1
    mode = "populated" if args.apply else "ready to populate"
    print(f"beta runtime inputs {mode}: {populated}")
    if unavailable:
        print("beta runtime inputs unavailable: " + ", ".join(unavailable))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
