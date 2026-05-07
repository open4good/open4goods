#!/usr/bin/env python3
"""Repository lint suite used by local development and CI."""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path


EXCLUDED_DIRS = {
    ".git",
    ".husky",
    ".nuxt",
    ".output",
    ".venv",
    "__pycache__",
    "coverage",
    "dist",
    "node_modules",
    "target",
}


class LintSuite:
    """Small command runner that reports all lint failures before exiting."""

    def __init__(self, root: Path, fix: bool) -> None:
        self.root = root
        self.fix = fix
        self.failures: list[str] = []

    def run(self, label: str, command: list[str], cwd: Path | None = None) -> None:
        print(f"\n=== {label} ===", flush=True)
        completed = subprocess.run(command, cwd=cwd or self.root, check=False)
        if completed.returncode != 0:
            self.failures.append(label)

    def skip(self, label: str, hint: str) -> None:
        print(f"\n=== {label} ===")
        print(f"SKIP: {hint}")

    def require_tool(self, name: str, hint: str) -> bool:
        if shutil.which(name):
            return True
        self.skip(name, hint)
        return False


def is_excluded(path: Path) -> bool:
    return any(part in EXCLUDED_DIRS for part in path.parts)


def find_files(root: Path, *suffixes: str) -> list[Path]:
    return sorted(
        path
        for path in root.rglob("*")
        if path.is_file() and path.suffix in suffixes and not is_excluded(path.relative_to(root))
    )


def find_compose_files(root: Path) -> list[Path]:
    files: list[Path] = []
    for path in root.rglob("*"):
        if not path.is_file() or is_excluded(path.relative_to(root)):
            continue
        name = path.name
        if name == "docker-compose.yml" or name.startswith("docker-compose.") and path.suffix in {
            ".yml",
            ".yaml",
        }:
            files.append(path)
        elif "compose" in name and path.suffix in {".yml", ".yaml"}:
            files.append(path)
    return sorted(files)


def lint_text(suite: LintSuite) -> None:
    command = [sys.executable, "scripts/python/text_replacements.py"]
    if suite.fix:
        command.append("--fix")
    suite.run("Markdown/JSON text replacements", command)


def lint_yaml(suite: LintSuite) -> None:
    if suite.require_tool("yamllint", "install with: pip install yamllint"):
        suite.run("yamllint", ["yamllint", "-c", ".yamllint", "."])


def lint_shell(suite: LintSuite) -> None:
    shell_files = find_files(suite.root, ".sh")
    if not shell_files:
        suite.skip("shell", "no shell scripts found")
        return
    syntax_command = [
        "bash",
        "-c",
        'for file in "$@"; do bash -n "$file"; done',
        "_",
        *[str(path.relative_to(suite.root)) for path in shell_files],
    ]
    suite.run("bash syntax", syntax_command)
    if suite.require_tool("shellcheck", "install shellcheck"):
        suite.run(
            "shellcheck",
            ["shellcheck", "--severity=warning", *[str(path.relative_to(suite.root)) for path in shell_files]],
        )


def lint_actions(suite: LintSuite) -> None:
    actionlint_image = "rhysd/actionlint:1.7.12"
    if suite.require_tool("docker", "install Docker for actionlint and compose checks"):
        suite.run(
            "actionlint",
            ["docker", "run", "--rm", "-v", f"{suite.root}:/repo", "-w", "/repo", actionlint_image, "-color"],
        )


def lint_docker(suite: LintSuite) -> None:
    compose_files = find_compose_files(suite.root)
    if suite.require_tool("docker", "install Docker with Compose"):
        for path in compose_files:
            relative = path.relative_to(suite.root)
            suite.run(
                f"docker compose config: {relative}",
                ["docker", "compose", "-f", str(relative), "config", "--no-interpolate", "-q"],
            )

    dockerfiles = sorted(
        path
        for path in suite.root.rglob("Dockerfile*")
        if path.is_file() and not is_excluded(path.relative_to(suite.root))
    )
    if dockerfiles and suite.require_tool("hadolint", "install hadolint for Dockerfile linting"):
        suite.run("hadolint", ["hadolint", *[str(path.relative_to(suite.root)) for path in dockerfiles]])


def lint_frontend(suite: LintSuite) -> None:
    frontend = suite.root / "frontend"
    if not (frontend / "package.json").exists():
        suite.skip("frontend", "frontend/package.json not found")
        return
    if not suite.require_tool("pnpm", "install pnpm"):
        return
    command = ["pnpm", "lint:fix" if suite.fix else "lint"]
    suite.run("frontend lint", command, cwd=frontend)
    suite.run("frontend generate", ["pnpm", "generate"], cwd=frontend)


def main() -> int:
    parser = argparse.ArgumentParser(description="Run the open4goods lint suite.")
    parser.add_argument("--fix", action="store_true", help="Apply supported automatic fixes.")
    args = parser.parse_args()

    root = Path(__file__).resolve().parents[2]
    os.chdir(root)
    suite = LintSuite(root=root, fix=args.fix)

    lint_text(suite)
    lint_yaml(suite)
    lint_shell(suite)
    lint_actions(suite)
    lint_docker(suite)
    lint_frontend(suite)

    if suite.failures:
        print("\nFAILED lint checks:")
        for label in suite.failures:
            print(f"- {label}")
        return 1

    print("\nOK: all lint checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
