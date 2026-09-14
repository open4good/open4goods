#!/usr/bin/env python3
"""Select and manage open4goods WorkOrders without reimplementing roadmap rules by hand."""

from __future__ import annotations

import argparse
import fnmatch
import importlib.util
import json
import shutil
import subprocess
import sys
from pathlib import Path

import yaml


class IndentedDumper(yaml.SafeDumper):
    """Indents block sequences under their parent key, matching this repo's yamllint config."""

    def increase_indent(self, flow: bool = False, indentless: bool = False) -> None:
        return super().increase_indent(flow, False)


def load_module(path: Path, name: str):
    """Load a repository script as a module."""
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


def run(root: Path, *args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    """Run a command in the repository and capture its text streams."""
    return subprocess.run(args, cwd=root, text=True, capture_output=True, check=check)


def read_order(path: Path) -> dict:
    """Read and minimally validate a WorkOrder document."""
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or data.get("kind") != "WorkOrder":
        raise ValueError(f"{path} is not a WorkOrder")
    return data


class Workspace:
    """Repository paths and WorkOrder operations for one checkout."""

    def __init__(self, root: Path):
        self.root = root.resolve()
        self.work_root = self.root / ".o4g" / "work"
        self.roadmap = load_module(
            self.root / "scripts" / "generate" / "generate_roadmap.py", "o4g_roadmap"
        )

    def all_orders(self):
        return self.roadmap.load_work_orders(self.work_root)

    def open_path(self, identifier: str) -> Path:
        path = self.work_root / f"{identifier}.yml"
        if path.is_file():
            return path
        ledger = self.work_root / "ledger" / f"{identifier}.yml"
        if ledger.is_file():
            raise ValueError(f"{identifier} is already closed in the ledger")
        raise ValueError(f"unknown open WorkOrder: {identifier}")

    def candidates(self, include_blocked: bool = False, phase: str = "DEVELOPMENT") -> list[tuple[object, str, tuple[str, ...]]]:
        orders, by_id = self.all_orders()
        milestones = self.roadmap.load_milestones(self.root)
        milestone_rank = {identifier: index for index, identifier in enumerate(milestones)}
        candidates = []
        for order in orders:
            if order.is_ledger or order.state not in {"ACCEPTED", "IN_PROGRESS", "BLOCKED"}:
                continue
            if phase != "ALL" and order.execution_phase != phase:
                continue
            blocked = self.roadmap.blockers(order, by_id)
            availability = self.roadmap.availability(order, by_id)
            if not include_blocked and (blocked or order.state == "BLOCKED"):
                continue
            candidates.append((order, availability, blocked))
        state_rank = {"IN_PROGRESS": 0, "ACCEPTED": 1, "BLOCKED": 2}
        return sorted(
            candidates,
            key=lambda item: (
                state_rank.get(item[0].state, 9),
                item[0].priority,
                milestone_rank.get(item[0].roadmap_ref, len(milestones)),
                item[0].identifier,
            ),
        )

    def regenerate(self) -> None:
        run(self.root, sys.executable, "scripts/generate/generate_roadmap.py")

    def transition(self, identifier: str, target: str, evidence: list[str], authorization_ref: str | None = None) -> Path:
        path = self.open_path(identifier)
        data = read_order(path)
        spec = data.setdefault("spec", {})
        current = str(spec.get("state") or "")
        allowed = {
            "IN_PROGRESS": {"ACCEPTED", "BLOCKED"},
            "BLOCKED": {"IN_PROGRESS"},
            "COMPLETED": {"IN_PROGRESS"},
        }
        if current not in allowed[target]:
            raise ValueError(f"refusing {current} -> {target} for {identifier}")
        info = self.describe(identifier)
        if target in {"IN_PROGRESS", "COMPLETED"}:
            if info["blockers"]:
                raise ValueError(f"{identifier} has unresolved blockers: {', '.join(info['blockers'])}")
            if info["executionPhase"] != "DEVELOPMENT":
                if not authorization_ref or not authorization_ref.strip():
                    raise ValueError("production phase requires --authorization-ref to an explicit owner order; readiness is not permission")
                evidence = [*evidence, f"owner-order:{authorization_ref.strip()}"]
        if target in {"BLOCKED", "COMPLETED"} and not evidence:
            raise ValueError(f"{target.lower()} requires at least one --evidence entry")
        spec["state"] = target
        if target == "BLOCKED":
            spec["externalBlockers"] = list(dict.fromkeys([*(spec.get("externalBlockers") or []), *evidence]))
        if evidence:
            refs = spec.setdefault("evidenceRefs", [])
            refs.extend(evidence)
        path.write_text(
            yaml.dump(data, Dumper=IndentedDumper, sort_keys=False, width=120),
            encoding="utf-8",
        )
        if target == "COMPLETED":
            destination = self.work_root / "ledger" / path.name
            if destination.exists():
                raise ValueError(f"ledger destination already exists: {destination}")
            shutil.move(path, destination)
            path = destination
        self.regenerate()
        return path

    def describe(self, identifier: str) -> dict:
        orders, by_id = self.all_orders()
        for order in orders:
            if order.identifier == identifier:
                data = read_order(order.path)
                spec = data.get("spec") or {}
                return {
                    "id": identifier,
                    "state": order.state,
                    "availability": self.roadmap.availability(order, by_id),
                    "milestone": order.roadmap_ref,
                    "executionPhase": order.execution_phase,
                    "priority": order.priority,
                    "blockers": list(self.roadmap.blockers(order, by_id)),
                    "pathScope": spec.get("pathScope") or [],
                    "acceptanceCriteria": spec.get("acceptanceCriteria") or [],
                    "evidenceRefs": spec.get("evidenceRefs") or [],
                    "implementationPlan": spec.get("implementationPlan") or [],
                    "path": str(order.path.relative_to(self.root)),
                }
        raise ValueError(f"unknown WorkOrder: {identifier}")

    def validate_paths(self, identifier: str, paths: list[str]) -> None:
        info = self.describe(identifier)
        scopes = list(info["pathScope"])
        governance = [
            f".o4g/work/{identifier}.yml",
            f".o4g/work/ledger/{identifier}.yml",
            "docs/reference/roadmap.md",
            ".o4g/corpus-budget.json",
        ]
        invalid = [
            path
            for path in paths
            if path not in governance and not any(fnmatch.fnmatch(path, scope) for scope in scopes)
        ]
        if invalid:
            raise ValueError(f"paths outside {identifier} pathScope: {', '.join(invalid)}")


def command_next(workspace: Workspace, args: argparse.Namespace) -> int:
    entries = workspace.candidates(args.all, args.phase)
    payload = [
        {
            "id": order.identifier,
            "milestone": order.roadmap_ref,
            "executionPhase": order.execution_phase,
            "priority": order.priority,
            "state": order.state,
            "availability": availability,
            "blockers": list(blockers),
        }
        for order, availability, blockers in entries[: args.limit]
    ]
    if args.json:
        print(json.dumps(payload, indent=2))
    else:
        for item in payload:
            blockers = f" blockers={','.join(item['blockers'])}" if item["blockers"] else ""
            print(f"{item['id']} [{item['executionPhase']} priority={item['priority']}] {item['state']}/{item['availability']}{blockers}")
    return 0


def command_commit(workspace: Workspace, args: argparse.Namespace) -> int:
    paths = [str(Path(path)) for path in args.paths]
    workspace.validate_paths(args.identifier, paths)
    branch = run(workspace.root, "git", "branch", "--show-current").stdout.strip()
    if not branch:
        raise ValueError("refusing to commit or push from detached HEAD")
    revision = run(workspace.root, "git", "rev-parse", "HEAD").stdout.strip()
    run(workspace.root, "git", "add", "--", *paths)
    message = f"{args.message}\n\nOpen4goods-Work-Order: {args.identifier}\nOpen4goods-Contract-Revision: {revision}"
    run(workspace.root, "git", "commit", "--only", "-m", message, "--", *paths)
    if args.push:
        run(workspace.root, "git", "push", "origin", f"HEAD:{branch}")
    print(f"committed {args.identifier} on {branch}" + (" and pushed" if args.push else ""))
    return 0


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    commands = result.add_subparsers(dest="command", required=True)
    next_parser = commands.add_parser("next")
    next_parser.add_argument("--all", action="store_true")
    next_parser.add_argument("--json", action="store_true")
    next_parser.add_argument("--limit", type=int, default=8)
    next_parser.add_argument("--phase", choices=["DEVELOPMENT", "PRODUCTION", "POST_PRODUCTION", "ALL"], default="DEVELOPMENT")
    status = commands.add_parser("status")
    status.add_argument("identifier")
    begin = commands.add_parser("begin")
    begin.add_argument("identifier")
    begin.add_argument("--authorization-ref", help="Reference to an actual explicit owner order; this flag does not grant authority")
    block = commands.add_parser("block")
    block.add_argument("identifier")
    block.add_argument("--evidence", action="append", required=True)
    close = commands.add_parser("close")
    close.add_argument("identifier")
    close.add_argument("--evidence", action="append", required=True)
    close.add_argument("--authorization-ref", help="Owner order covering this production completion")
    commit = commands.add_parser("commit")
    commit.add_argument("identifier")
    commit.add_argument("--message", required=True)
    commit.add_argument("--push", action="store_true")
    commit.add_argument("paths", nargs="+")
    return result


def main(argv: list[str] | None = None) -> int:
    args = parser().parse_args(argv)
    workspace = Workspace(args.root)
    try:
        if args.command == "next":
            return command_next(workspace, args)
        if args.command == "status":
            print(yaml.safe_dump(workspace.describe(args.identifier), sort_keys=False).rstrip())
            return 0
        if args.command == "begin":
            print(workspace.transition(args.identifier, "IN_PROGRESS", [], args.authorization_ref))
            return 0
        if args.command == "block":
            print(workspace.transition(args.identifier, "BLOCKED", args.evidence))
            return 0
        if args.command == "close":
            print(workspace.transition(args.identifier, "COMPLETED", args.evidence, args.authorization_ref))
            return 0
        if args.command == "commit":
            return command_commit(workspace, args)
    except (OSError, ValueError, subprocess.CalledProcessError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        if isinstance(exc, subprocess.CalledProcessError) and exc.stderr:
            print(exc.stderr.rstrip(), file=sys.stderr)
        return 1
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
