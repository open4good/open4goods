#!/usr/bin/env python3
"""Validate YAML files in a repository."""
from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path
from typing import Iterable, Iterator, Tuple

import yaml

DEFAULT_EXCLUDED_NAMES = {
    ".git",
    ".hg",
    ".svn",
    ".mvn",
    ".gradle",
    ".venv",
    "__pycache__",
    "build",
    "dist",
    "node_modules",
    "target",
    "tmp",
    "vendor",
}

PathPair = Tuple[set[str], set[Path]]

SUPPORTED_EXTENSIONS = {".yml", ".yaml"}


class ValidationError(Exception):
    """Raised when a YAML file fails validation."""


def split_exclusions(root: Path, entries: Iterable[str]) -> PathPair:
    """Separate exclusion entries into directory names and absolute paths."""

    names: set[str] = set()
    paths: set[Path] = set()
    for entry in entries:
        if not entry:
            continue
        if os.sep in entry or (os.altsep and os.altsep in entry):
            candidate = Path(entry)
            if not candidate.is_absolute():
                candidate = (root / candidate).resolve()
            paths.add(candidate)
        else:
            names.add(entry)
    return names, paths


def is_excluded_path(path: Path, excluded_paths: set[Path]) -> bool:
    for excluded in excluded_paths:
        if path == excluded or excluded in path.parents:
            return True
    return False


def iter_yaml_files(root: Path, excluded_names: set[str], excluded_paths: set[Path]) -> Iterator[Path]:
    for current_root, dirs, files in os.walk(root):
        current_root_path = Path(current_root)
        pruned_dirs = []
        for directory in dirs:
            full_path = (current_root_path / directory).resolve()
            if directory in excluded_names or is_excluded_path(full_path, excluded_paths):
                continue
            pruned_dirs.append(directory)
        dirs[:] = pruned_dirs

        if is_excluded_path(current_root_path.resolve(), excluded_paths):
            continue

        for name in files:
            path = current_root_path / name
            if path.suffix.lower() in SUPPORTED_EXTENSIONS:
                yield path


SCALAR_TYPES = (str, int, float, bool, bytes)


def _is_scalar(value: object) -> bool:
    return isinstance(value, SCALAR_TYPES)


def validate_yaml_file(path: Path) -> None:
    try:
        with path.open("r", encoding="utf-8") as handle:
            documents = list(yaml.safe_load_all(handle))
    except UnicodeDecodeError as exc:
        raise ValidationError(f"{path}: unable to decode as UTF-8 ({exc})") from exc
    except yaml.YAMLError as exc:
        raise ValidationError(f"{path}: {exc}") from exc

    if not documents:
        raise ValidationError(f"{path}: contains no YAML documents")

    for index, document in enumerate(documents, start=1):
        if document is None:
            raise ValidationError(f"{path}: document #{index} is empty")
        if _is_scalar(document):
            typename = type(document).__name__
            raise ValidationError(
                f"{path}: document #{index} is a scalar ({typename}); expected mapping or sequence"
            )


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate YAML files in a directory tree.")
    parser.add_argument(
        "--root",
        default=Path("."),
        type=Path,
        help="Root directory to validate (default: current working directory).",
    )
    parser.add_argument(
        "--exclude",
        action="append",
        default=[],
        help=(
            "Additional directories to exclude. Accepts directory names or paths "
            "relative to the root. Can be provided multiple times."
        ),
    )

    args = parser.parse_args(argv)
    root_path = args.root.resolve()
    extra_names, extra_paths = split_exclusions(root_path, args.exclude)
    excluded_names = DEFAULT_EXCLUDED_NAMES.union(extra_names)
    excluded_paths = {path.resolve() for path in extra_paths}

    if not root_path.exists():
        print(f"Root path does not exist: {root_path}", file=sys.stderr)
        return 1

    errors: list[str] = []
    for yaml_path in iter_yaml_files(root_path, excluded_names, excluded_paths):
        try:
            validate_yaml_file(yaml_path)
        except ValidationError as exc:
            errors.append(str(exc))

    if errors:
        print("YAML validation failed for the following files:")
        for error in errors:
            print(f" - {error}")
        return 1

    print("All YAML files validated successfully.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
