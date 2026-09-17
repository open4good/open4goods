#!/usr/bin/env python3
"""Generate docs/reference/roadmap.md from the WorkOrder contracts under .o4g/work/.

Availability is derived from dependencies, ordered phase gates and explicit blockers.

Pass --check to fail when the committed projection is stale.
"""

from __future__ import annotations

import argparse
import sys
from dataclasses import dataclass
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
PHASES = ("DEVELOPMENT", "BETA_VALIDATION", "PRODUCTION", "POST_PRODUCTION")
OUTPUT = Path("docs/reference/roadmap.md")
FRONT_MATTER = """---
title: "Generated contractual roadmap"
normative: false
audience: PROJECT_SCOPED
---
"""


@dataclass(frozen=True)
class WorkOrder:
    identifier: str
    path: Path
    roadmap_ref: str
    state: str
    dependencies: tuple[str, ...]
    purpose: str
    execution_phase: str = "DEVELOPMENT"
    priority: int = 1000
    external_blockers: tuple[str, ...] = ()
    completion_gates: tuple[str, ...] = ()
    is_ledger: bool = False


def normalized(value: object) -> str:
    return " ".join(str(value or "").split())


def load_work_orders(work_root: Path) -> tuple[list[WorkOrder], dict[str, WorkOrder]]:
    orders: list[WorkOrder] = []
    by_id: dict[str, WorkOrder] = {}
    candidates = sorted(work_root.glob("*.yml")) + sorted((work_root / "ledger").glob("*.yml"))
    for path in candidates:
        data = yaml.safe_load(path.read_text(encoding="utf-8"))
        if not isinstance(data, dict) or data.get("kind") != "WorkOrder":
            continue
        metadata = data.get("metadata") or {}
        spec = data.get("spec") or {}
        identifier = metadata.get("id")
        if not isinstance(identifier, str):
            raise ValueError(f"{path}: WorkOrder metadata.id is missing")
        roadmap_ref = str(spec.get("roadmapRef") or "")
        if not roadmap_ref:
            raise ValueError(
                f"{path}: WorkOrder {identifier!r} has no spec.roadmapRef; it would be "
                "silently excluded from the generated roadmap"
            )
        if identifier in by_id:
            raise ValueError(f"duplicate WorkOrder id: {identifier}")
        is_ledger = path.parent.name == "ledger"
        phase = spec.get("executionPhase", "DEVELOPMENT" if is_ledger else None)
        priority = spec.get("priority", 1000 if is_ledger else None)
        if phase not in PHASES:
            raise ValueError(f"{path}: explicit spec.executionPhase must be one of {PHASES}")
        if type(priority) is not int or priority < 0:
            raise ValueError(f"{path}: explicit spec.priority must be a nonnegative integer")
        external = spec.get("externalBlockers") or []
        if not isinstance(external, list) or any(not isinstance(item, str) or not item.strip() for item in external):
            raise ValueError(f"{path}: externalBlockers must be a list of nonempty strings")
        completion_gates = spec.get("completionGates") or []
        if not isinstance(completion_gates, list) or any(
            gate != "ALL_OTHER_DEVELOPMENT_COMPLETED" for gate in completion_gates
        ):
            raise ValueError(
                f"{path}: completionGates supports only ALL_OTHER_DEVELOPMENT_COMPLETED"
            )
        order = WorkOrder(
            identifier=identifier,
            path=path,
            roadmap_ref=roadmap_ref,
            state=str(spec.get("state") or ""),
            dependencies=tuple(spec.get("dependsOn") or ()),
            purpose=normalized(spec.get("purpose")),
            execution_phase=phase,
            priority=priority,
            external_blockers=tuple(external),
            completion_gates=tuple(completion_gates),
            is_ledger=is_ledger,
        )
        by_id[identifier] = order
        orders.append(order)
    # Validate the complete graph, including cycles crossing milestone boundaries.
    for order in orders:
        for dependency in order.dependencies:
            target = by_id.get(dependency)
            if target is None:
                raise ValueError(f"{order.identifier}: missing dependency {dependency}")
            if not order.is_ledger and PHASES.index(target.execution_phase) > PHASES.index(order.execution_phase):
                raise ValueError(f"{order.identifier}: dependency {dependency} belongs to a later execution phase")
    topological_order(orders)
    return orders, by_id


def load_milestones(root: Path) -> list[str]:
    manifest = root / ".o4g" / "project.yml"
    if not manifest.is_file():
        return []
    data = yaml.safe_load(manifest.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or data.get("kind") != "Project":
        return []
    milestones = (data.get("spec") or {}).get("milestones") or []
    return [m["id"] for m in sorted(milestones, key=lambda m: m["order"])]


def topological_order(orders: list[WorkOrder]) -> list[WorkOrder]:
    by_id = {order.identifier: order for order in orders}
    emitted: list[WorkOrder] = []
    remaining = set(by_id)
    while remaining:
        ready = sorted(
            identifier
            for identifier in remaining
            if all(
                dependency not in by_id or dependency not in remaining
                for dependency in by_id[identifier].dependencies
            )
        )
        if not ready:
            raise ValueError(f"roadmap dependency cycle among: {', '.join(sorted(remaining))}")
        for identifier in ready:
            emitted.append(by_id[identifier])
            remaining.remove(identifier)
    return emitted


def blockers(order: WorkOrder, by_id: dict[str, WorkOrder]) -> tuple[str, ...]:
    blocked = list(order.external_blockers)
    for dependency in order.dependencies:
        target = by_id.get(dependency)
        if target is None:
            blocked.append(f"{dependency} (MISSING)")
        elif target.state != "COMPLETED":
            blocked.append(dependency)
    if "ALL_OTHER_DEVELOPMENT_COMPLETED" in order.completion_gates:
        remaining = sorted(
            candidate.identifier
            for candidate in by_id.values()
            if not candidate.is_ledger
            and candidate.identifier != order.identifier
            and candidate.execution_phase == "DEVELOPMENT"
            and candidate.state != "COMPLETED"
        )
        if remaining:
            blocked.append(f"all-other-development-workorders ({len(remaining)} open)")
    return tuple(blocked)


def availability(order: WorkOrder, by_id: dict[str, WorkOrder]) -> str:
    if order.state == "COMPLETED":
        return "DONE"
    earlier_phase = {
        "BETA_VALIDATION": ("DEVELOPMENT", "AWAITING_DEVELOPMENT"),
        "PRODUCTION": ("BETA_VALIDATION", "AWAITING_BETA_VALIDATION"),
        "POST_PRODUCTION": ("PRODUCTION", "AWAITING_PRODUCTION"),
    }.get(order.execution_phase)
    if earlier_phase is not None:
        phase, waiting = earlier_phase
        if any(
            not candidate.is_ledger
            and candidate.execution_phase == phase
            and candidate.state != "COMPLETED"
            for candidate in by_id.values()
        ):
            return waiting
    if order.state == "BLOCKED":
        return "BLOCKED"
    if blockers(order, by_id):
        return "BLOCKED"
    if order.state == "IN_PROGRESS":
        return "IN_PROGRESS"
    if order.state == "ACCEPTED":
        return "READY"
    return "PLANNED"


def cell(value: str) -> str:
    return value.replace("|", "\\|")


def render(root: Path) -> str:
    work_root = root / ".o4g" / "work"
    orders, by_id = load_work_orders(work_root)
    declared = load_milestones(root)
    seen = [m for m in declared if any(o.roadmap_ref == m for o in orders)]
    seen += sorted({o.roadmap_ref for o in orders} - set(declared))

    lines = [
        FRONT_MATTER,
        "# Contractual roadmap",
        "",
        "<!-- GENERATED by scripts/generate/generate_roadmap.py from .o4g/work/; do not edit. -->",
        "",
        "Default selection is DEVELOPMENT, ordered by priority after resuming active work. "
        "READY requires completed dependencies, no external blocker and a satisfied earlier-phase gate. "
        "The permanent promotion authority is recorded in the project contract. See ADR-0014.",
        "",
        "| Milestone | Open | READY | BLOCKED | IN_PROGRESS | Awaiting phase | Closed |",
        "|---|---|---|---|---|---|---|",
    ]
    for milestone in seen:
        group = [o for o in orders if o.roadmap_ref == milestone]
        openers = [o for o in group if not o.is_ledger]
        avail = [availability(o, by_id) for o in openers]
        lines.append(
            f"| {milestone} | {len(openers)} | {avail.count('READY')} | {avail.count('BLOCKED')} "
            f"| {sum(1 for o in openers if o.state == 'IN_PROGRESS')} "
            f"| {sum(1 for value in avail if value.startswith('AWAITING_'))} "
            f"| {sum(1 for o in group if o.is_ledger)} |"
        )

    for milestone in seen:
        group = [o for o in orders if o.roadmap_ref == milestone]
        openers = topological_order([o for o in group if not o.is_ledger])
        closed = [o for o in group if o.is_ledger]
        lines += ["", f"## {milestone}", ""]
        if openers:
            lines += [
                "| WorkOrder | Phase / priority | Contract state | Availability | Dependencies | Blockers | Purpose |",
                "|---|---|---|---|---|---|---|",
            ]
            for order in openers:
                relative = Path("../..") / order.path.relative_to(root)
                blocked = blockers(order, by_id)
                lines.append(
                    f"| [{order.identifier}]({relative.as_posix()}) | {order.execution_phase} / {order.priority} | {order.state} "
                    f"| {availability(order, by_id)} "
                    f"| {', '.join(order.dependencies) or '--'} "
                    f"| {cell(', '.join(blocked)) or '--'} | {cell(order.purpose)} |"
                )
        if closed:
            lines += ["", f"*{len(closed)} closed, see [ledger](../../.o4g/work/ledger).*"]
    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    rendered = render(args.root)
    target = args.root / OUTPUT
    if args.check:
        current = target.read_text(encoding="utf-8") if target.is_file() else ""
        if current != rendered:
            print(
                f"{OUTPUT} is stale. Regenerate with scripts/generate/generate_roadmap.py.",
                file=sys.stderr,
            )
            return 1
        print(f"OK: {OUTPUT} matches the WorkOrder contracts.")
        return 0

    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(rendered, encoding="utf-8")
    print(f"Wrote {OUTPUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
