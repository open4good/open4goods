#!/usr/bin/env python3
"""Pin and describe the legacy product gzip JSONL archive without exporting records.

The legacy publisher replaces numbered archive files before it writes its manifest.  This
tool treats that manifest as a generation boundary: it accepts only its listed files,
checks the manifest before and after scanning, and refuses an archive that changes while
it is being pinned.  Its JSON output deliberately contains metadata, counts and field
names only; product records never leave the private archive volume.
"""

from __future__ import annotations

import argparse
import collections
import dataclasses
import gzip
import hashlib
import json
import os
import re
import sys
from pathlib import Path
from typing import Any
from urllib.parse import unquote, urlparse


SCHEMA_VERSION = "open4goods.product-backup-input/v1"
LEGACY_MANIFEST_NAME = "products-backup-manifest.json"
BACKUP_FILE_NAME = re.compile(r"products-backup-\d+\.gz")
GTIN_FIELDS = ("gtin", "ean", "ean13", "barcode")
SOURCE_FIELDS = ("source", "sourceId", "provider", "providerId", "datasourceCodes", "externalIds")
VERTICAL_FIELDS = ("vertical", "verticalId", "category", "categoryId", "categoriesByDatasources", "datasourceCategories")
MEDIA_FIELDS = ("images", "image", "medias", "media", "imageUrl", "imageUrls")
PRICE_FIELDS = ("price", "minPrice", "prices", "offers")
ATTRIBUTE_DISPOSITION_MANIFEST = "services/data-reference/src/main/resources/registry/legacy-attribute-migrations.json"


class InputContractError(ValueError):
    """Raised when the archive cannot be proved to be one immutable generation."""


@dataclasses.dataclass(frozen=True)
class FileFingerprint:
    """Stable identity fields used to detect replacement during a pin."""

    device: int
    inode: int
    size: int
    modified_ns: int


def fingerprint(path: Path) -> FileFingerprint:
    """Return the file identity that must remain stable while it is read."""
    stat = path.stat()
    return FileFingerprint(stat.st_dev, stat.st_ino, stat.st_size, stat.st_mtime_ns)


def sha256_file(path: Path) -> str:
    """Return the SHA-256 digest of compressed archive bytes."""
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def source_path(source_uri: str) -> Path:
    """Resolve an absolute path or file URI, rejecting remote and relative inputs."""
    parsed = urlparse(source_uri.strip())
    if parsed.scheme not in ("", "file") or parsed.netloc not in ("", "localhost"):
        raise InputContractError("PRODUCT_BACKUP_SOURCE_URI must be an absolute local file URI")
    raw_path = Path(unquote(parsed.path if parsed.scheme else source_uri))
    if not raw_path.is_absolute():
        raise InputContractError("PRODUCT_BACKUP_SOURCE_URI must be an absolute local file URI")
    candidate = raw_path.resolve()
    if not candidate.is_dir():
        raise InputContractError("PRODUCT_BACKUP_SOURCE_URI must name an existing directory")
    return candidate


def read_legacy_manifest(source: Path) -> tuple[dict[str, Any], bytes, list[str]]:
    """Read and validate the legacy publication manifest before touching payload files."""
    manifest_path = source / LEGACY_MANIFEST_NAME
    try:
        raw = manifest_path.read_bytes()
        manifest = json.loads(raw)
    except (OSError, json.JSONDecodeError) as error:
        raise InputContractError("legacy manifest is missing or invalid") from error
    if not isinstance(manifest, dict):
        raise InputContractError("legacy manifest must be an object")
    files = manifest.get("files")
    if not isinstance(files, list) or not files or not all(isinstance(name, str) for name in files):
        raise InputContractError("legacy manifest must list one or more archive files")
    if len(files) != len(set(files)) or any(not BACKUP_FILE_NAME.fullmatch(name) for name in files):
        raise InputContractError("legacy manifest contains unsafe or duplicate archive names")
    if not isinstance(manifest.get("completedAt"), str) or not isinstance(manifest.get("completedEpochMillis"), int):
        raise InputContractError("legacy manifest has no usable completion time")
    if not isinstance(manifest.get("exportedCount"), int) or not isinstance(manifest.get("expectedCount"), int):
        raise InputContractError("legacy manifest has no usable export counts")
    if manifest["exportedCount"] < manifest["expectedCount"]:
        raise InputContractError("legacy manifest reports an incomplete export")
    return manifest, raw, files


def json_shape(value: Any) -> str:
    """Describe a JSON value type without retaining its value."""
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "boolean"
    if isinstance(value, str):
        return "string"
    if isinstance(value, int):
        return "integer"
    if isinstance(value, float):
        return "number"
    if isinstance(value, list):
        return "array"
    if isinstance(value, dict):
        return "object"
    return type(value).__name__


def valid_gtin(value: Any) -> bool:
    """Check GTIN-8/12/13/14 syntax and check digit without preserving the value."""
    if not isinstance(value, str) or not value.isdigit() or len(value) not in (8, 12, 13, 14):
        return False
    digits = [int(digit) for digit in value]
    total = sum(digit * (3 if index % 2 == len(digits) % 2 else 1) for index, digit in enumerate(digits[:-1]))
    return (10 - total % 10) % 10 == digits[-1]


def first_present(record: dict[str, Any], names: tuple[str, ...]) -> Any:
    """Return the first known legacy field present in a record."""
    return next((record[name] for name in names if name in record), None)


def legacy_gtin(record: dict[str, Any]) -> Any:
    """Locate the legacy top-level or nested GTIN representation without retaining it."""
    direct = first_present(record, GTIN_FIELDS)
    if direct is not None:
        return direct
    details = record.get("gtinInfos")
    if not isinstance(details, dict):
        return None
    normalized = details.get("normalizedGtin14")
    if normalized is not None:
        return normalized
    strings = details.get("gtinStrings")
    return strings[0] if isinstance(strings, list) and strings else None


def field_disposition(name: str) -> dict[str, str]:
    """Classify one observed legacy field without inventing a conversion for it."""
    normalized = name.casefold()
    if normalized in {field.casefold() for field in GTIN_FIELDS} | {"id", "uuid", "productid"}:
        return {"classification": "UNATTRIBUTED_IDENTITY", "rule": "preserve only as a legacy correlation key"}
    if "amazon" in normalized:
        return {"classification": "QUARANTINE", "rule": "requires source attribution and rights review"}
    if "price" in normalized or normalized in {"offers", "offer"}:
        return {"classification": "LEGACY_MINIMUM_PRICE", "rule": "retain only as an unattributed historical minimum"}
    if any(token in normalized for token in ("score", "rank", "availability", "available", "stock")):
        return {"classification": "RECOMPUTED", "rule": "derive from eligible imported evidence"}
    if any(token in normalized for token in ("attribute", "feature", "specification", "characteristic")):
        return {
            "classification": "NATIVE_EVIDENCED_CONVERSION",
            "rule": "resolve through the reviewed registry attribute disposition manifest",
        }
    if normalized in {"name", "title", "description", "brand", "model", "manufacturer", "vertical", "category"}:
        return {"classification": "NATIVE_EVIDENCED_CONVERSION", "rule": "retain only with source evidence"}
    return {"classification": "QUARANTINE", "rule": "no conversion is approved for this observed field"}


def add_field_dispositions(result: dict[str, Any]) -> dict[str, Any]:
    """Attach a complete, versioned conversion decision for every observed field name."""
    shapes = result.get("sample", {}).get("fieldShapes")
    if not isinstance(shapes, dict) or not all(isinstance(name, str) for name in shapes):
        raise InputContractError("input manifest has no safe field-shape inventory")
    result["conversionRules"]["fieldDispositions"] = {
        name: field_disposition(name) for name in sorted(shapes)
    }
    result["conversionRules"]["attributeDispositionManifest"] = ATTRIBUTE_DISPOSITION_MANIFEST
    return result


def scan_archive(path: Path, sample_limit: int) -> tuple[dict[str, Any], dict[str, int]]:
    """Count every JSONL line and collect a bounded, value-free field inventory."""
    shapes: dict[str, set[str]] = collections.defaultdict(set)
    counts: collections.Counter[str] = collections.Counter()
    gtins: set[str] = set()
    duplicate_gtins = 0
    with gzip.open(path, "rt", encoding="utf-8", errors="strict") as handle:
        for line_number, line in enumerate(handle, start=1):
            counts["lineCount"] += 1
            if line_number > sample_limit:
                continue
            counts["sampledRecords"] += 1
            try:
                record = json.loads(line)
            except json.JSONDecodeError as error:
                raise InputContractError(f"invalid JSONL in {path.name} at line {line_number}") from error
            if not isinstance(record, dict):
                counts["nonObjectRecords"] += 1
                continue
            for name, value in record.items():
                shapes[name].add(json_shape(value))
            gtin = legacy_gtin(record)
            if valid_gtin(gtin):
                counts["validGtins"] += 1
                if gtin in gtins:
                    duplicate_gtins += 1
                gtins.add(gtin)
            elif gtin is None:
                counts["missingGtins"] += 1
            else:
                counts["invalidGtins"] += 1
            counts["sourcePresent" if first_present(record, SOURCE_FIELDS) is not None else "sourceMissing"] += 1
            vertical = first_present(record, VERTICAL_FIELDS)
            if vertical is None:
                counts["unclassified"] += 1
            else:
                counts["classified"] += 1
            counts["mediaPresent" if first_present(record, MEDIA_FIELDS) is not None else "mediaMissing"] += 1
            price = first_present(record, PRICE_FIELDS)
            counts["legacyPriceMissing" if price is None else f"legacyPrice.{json_shape(price)}"] += 1
    counts["duplicateGtins"] = duplicate_gtins
    return {name: sorted(kinds) for name, kinds in sorted(shapes.items())}, dict(sorted(counts.items()))


def pin(source_uri: str, sample_limit: int) -> dict[str, Any]:
    """Produce an immutable, manifest-bounded archive contract or reject the input."""
    source = source_path(source_uri)
    manifest_path = source / LEGACY_MANIFEST_NAME
    manifest_before = fingerprint(manifest_path)
    legacy, legacy_raw, files = read_legacy_manifest(source)
    files_out: list[dict[str, Any]] = []
    aggregate_counts: collections.Counter[str] = collections.Counter()
    all_shapes: dict[str, set[str]] = collections.defaultdict(set)
    for name in files:
        path = source / name
        if not path.is_file():
            raise InputContractError(f"manifest-listed archive is missing: {name}")
        before = fingerprint(path)
        shapes, counts = scan_archive(path, sample_limit)
        digest = sha256_file(path)
        if before != fingerprint(path):
            raise InputContractError(f"archive changed during pinning: {name}")
        for field, kinds in shapes.items():
            all_shapes[field].update(kinds)
        aggregate_counts.update(counts)
        files_out.append({"name": name, "sha256": digest, "bytes": before.size, "lineCount": counts["lineCount"]})
    if manifest_before != fingerprint(manifest_path) or legacy_raw != manifest_path.read_bytes():
        raise InputContractError("legacy manifest changed during pinning")
    if aggregate_counts["lineCount"] != legacy["exportedCount"]:
        raise InputContractError("archive line count does not match the legacy exported count")
    result = {
        "schemaVersion": SCHEMA_VERSION,
        "legacyManifest": {
            "sha256": hashlib.sha256(legacy_raw).hexdigest(),
            "completedAt": legacy["completedAt"],
            "completedEpochMillis": legacy["completedEpochMillis"],
            "expectedCount": legacy["expectedCount"],
            "exportedCount": legacy["exportedCount"],
            "files": files,
        },
        "files": files_out,
        "sample": {
            "recordsPerFile": sample_limit,
            "fieldShapes": {name: sorted(kinds) for name, kinds in sorted(all_shapes.items())},
            "counts": dict(sorted(aggregate_counts.items())),
        },
        "conversionRules": {
            "mappingVersion": "legacy-product-fields/v1",
            "attributeDispositionManifest": ATTRIBUTE_DISPOSITION_MANIFEST,
            "attributeDispositionCount": 94,
            "unknownSourceIdentity": "NUDGER_WEB only when required for an otherwise attributable legacy record",
            "amazon": "quarantine when source attribution or redistribution rights are uncertain",
            "derivedScoresAndAvailability": "recompute from eligible imported evidence",
            "prohibitedInventions": ["provider ids", "observations", "languages", "offer identity", "rights"],
        },
        "scope": {
            "validDistinctGtins": "bounded-sample only; full distinct counting is a later capacity-controlled import step",
            "separatePreservation": ["production accounts", "billing", "keys", "non-Product corrections", "media bytes"],
        },
    }
    return add_field_dispositions(result)


def main() -> int:
    """Run the private pin and write its safe machine-readable output."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-uri", help="absolute private path or file URI; never log its value")
    parser.add_argument("--output", required=True, type=Path, help="private destination for the import-side manifest")
    parser.add_argument("--sample-lines", type=int, default=500, help="records sampled per archive for shape inventory")
    parser.add_argument("--enrich-existing", action="store_true", help="add field dispositions to an existing safe pin")
    args = parser.parse_args()
    if args.sample_lines < 1:
        parser.error("--sample-lines must be positive")
    try:
        if args.enrich_existing:
            result = add_field_dispositions(json.loads(args.output.read_text(encoding="utf-8")))
        elif args.source_uri:
            result = pin(args.source_uri, args.sample_lines)
        else:
            parser.error("--source-uri is required unless --enrich-existing is used")
    except (InputContractError, OSError, UnicodeDecodeError, gzip.BadGzipFile) as error:
        print(f"product backup pin rejected: {error}", file=sys.stderr)
        return 2
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = args.output.with_suffix(args.output.suffix + ".tmp")
    temporary.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    os.replace(temporary, args.output)
    print("product backup pin completed; output contains metadata, counts and field names only")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
