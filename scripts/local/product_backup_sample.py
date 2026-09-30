#!/usr/bin/env python3
"""Create a bounded deterministic local-only JSONL sample from a pinned product backup."""

from __future__ import annotations

import argparse
import gzip
import hashlib
import heapq
import io
import json
import os
import tempfile
from pathlib import Path
from typing import Any

MANIFEST = "products-backup-manifest.json"
VERTICALS = (
    "air-conditioner",
    "dishwasher",
    "oven",
    "refrigerator",
    "smartphones",
    "tv",
    "washing-machine",
)
SPECIAL_BUCKETS = ("source-rich", "legacy-only", "amazon-ambiguous", "known-error")


class SampleError(ValueError):
    """Raised when the immutable source cannot satisfy the sample contract."""


def walk_scalars(value: Any):
    """Yield scalar strings from a legacy record without assuming its historical shape."""
    if isinstance(value, dict):
        for nested in value.values():
            yield from walk_scalars(nested)
    elif isinstance(value, list):
        for nested in value:
            yield from walk_scalars(nested)
    elif value is not None:
        yield str(value)


def normalized_gtin(record: dict[str, Any]) -> str | None:
    """Return the first legacy GTIN representation used only inside the ignored sample."""
    for key in ("gtin", "gtin14", "ean", "barcode"):
        value = record.get(key)
        if isinstance(value, (str, int)):
            return str(value)
    details = record.get("gtinInfos")
    if isinstance(details, dict):
        value = details.get("normalizedGtin14")
        if isinstance(value, (str, int)):
            return str(value)
    return None


def valid_gtin(value: str | None) -> bool:
    """Validate GTIN-8/12/13/14 syntax and check digit."""
    if value is None or not value.isdigit() or len(value) not in (8, 12, 13, 14):
        return False
    digits = [int(digit) for digit in value]
    total = sum(digit * (3 if index % 2 == len(digits) % 2 else 1) for index, digit in enumerate(digits[:-1]))
    return (10 - total % 10) % 10 == digits[-1]


def buckets(record: dict[str, Any]) -> set[str]:
    """Classify one record into required coverage buckets using shape, not provider assumptions."""
    scalar_text = " ".join(walk_scalars(record)).casefold()
    result = {vertical for vertical in VERTICALS if vertical in scalar_text}
    source_evidence_fields = (
        "sources", "datasources", "datasourceNames", "attributes", "offers", "resources",
        "datasourceCodes", "externalIds", "sourceUrls", "categoriesByDatasources", "datasourceCategories",
    )
    if sum(bool(record.get(name)) for name in source_evidence_fields) >= 4:
        result.add("source-rich")
    if not any(record.get(name) for name in ("sources", "datasources", "datasourceNames")):
        result.add("legacy-only")
    if "amazon" in scalar_text and all(token not in scalar_text for token in ("pa-api", "paapi", "product advertising")):
        result.add("amazon-ambiguous")
    if not valid_gtin(normalized_gtin(record)) or any(name in record for name in ("errors", "error", "rejections")):
        result.add("known-error")
    return result


def push_candidate(heap: list[tuple[int, bytes, bytes]], limit: int, digest: bytes, raw: bytes) -> None:
    """Keep the lexicographically smallest content hashes with bounded memory."""
    rank = int.from_bytes(digest, "big")
    item = (-rank, digest, raw)
    if len(heap) < limit:
        heapq.heappush(heap, item)
    elif item > heap[0]:
        heapq.heapreplace(heap, item)


def read_manifest(source: Path) -> tuple[dict[str, Any], bytes, list[str]]:
    """Load the original completed manifest and reject unsafe archive names."""
    path = source / MANIFEST
    raw = path.read_bytes()
    manifest = json.loads(raw)
    files = manifest.get("files")
    if not isinstance(files, list) or not files:
        raise SampleError("original manifest has no archive list")
    if manifest.get("exportedCount", -1) < manifest.get("expectedCount", 0):
        raise SampleError("original manifest reports an incomplete copy")
    if any(not isinstance(name, str) or Path(name).name != name or not name.endswith(".gz") for name in files):
        raise SampleError("original manifest contains an unsafe archive name")
    return manifest, raw, files


def generate(source: Path, output: Path, per_bucket: int) -> dict[str, Any]:
    """Scan every manifest-listed record and atomically write a deterministic sample."""
    manifest, manifest_raw, files = read_manifest(source)
    heaps: dict[str, list[tuple[int, bytes, bytes]]] = {
        bucket: [] for bucket in (*VERTICALS, *SPECIAL_BUCKETS)
    }
    line_count = 0
    for name in files:
        archive = source / name
        if not archive.is_file():
            raise SampleError(f"manifest-listed archive is absent: {name}")
        with gzip.open(archive, "rb") as handle:
            for line in handle:
                line_count += 1
                raw = line.rstrip(b"\r\n")
                try:
                    record = json.loads(raw)
                except json.JSONDecodeError as error:
                    raise SampleError(f"invalid JSONL in {name} at line {line_count}") from error
                if not isinstance(record, dict):
                    continue
                digest = hashlib.sha256(raw).digest()
                for bucket in buckets(record):
                    push_candidate(heaps[bucket], per_bucket, digest, raw)
    if line_count != manifest.get("exportedCount"):
        raise SampleError("archive line count differs from the original manifest")
    missing = [bucket for bucket, heap in heaps.items() if not heap]
    if missing:
        raise SampleError(f"required coverage buckets are empty: {', '.join(missing)}")

    selected: dict[bytes, bytes] = {}
    bucket_digests: dict[str, list[str]] = {}
    for bucket, heap in heaps.items():
        ordered = sorted((digest, raw) for _, digest, raw in heap)
        bucket_digests[bucket] = [digest.hex() for digest, _ in ordered]
        selected.update(ordered)

    output.mkdir(parents=True, exist_ok=True)
    sample_path = output / "products-sample.jsonl.gz"
    descriptor_path = output / "products-sample-manifest.json"
    fd, temporary_name = tempfile.mkstemp(prefix="products-sample-", suffix=".tmp", dir=output)
    try:
        with os.fdopen(fd, "wb") as raw_output:
            with gzip.GzipFile(fileobj=raw_output, mode="wb", filename="", mtime=0) as compressed:
                for digest in sorted(selected):
                    compressed.write(selected[digest] + b"\n")
        os.replace(temporary_name, sample_path)
    finally:
        if os.path.exists(temporary_name):
            os.unlink(temporary_name)
    sample_sha = hashlib.sha256(sample_path.read_bytes()).hexdigest()
    descriptor = {
        "schemaVersion": "open4goods-local-sample/v1",
        "sourceManifestSha256": hashlib.sha256(manifest_raw).hexdigest(),
        "sourceLineCount": line_count,
        "sampleFile": sample_path.name,
        "sampleSha256": sample_sha,
        "sampleRecordCount": len(selected),
        "recordsPerBucket": per_bucket,
        "buckets": bucket_digests,
    }
    descriptor_path.write_text(json.dumps(descriptor, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return descriptor


def main() -> int:
    """Generate the ignored sample without logging source records or coordinates."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--per-bucket", type=int, default=20)
    args = parser.parse_args()
    if args.per_bucket < 1:
        parser.error("--per-bucket must be positive")
    try:
        result = generate(args.source.resolve(), args.output.resolve(), args.per_bucket)
    except (OSError, UnicodeDecodeError, json.JSONDecodeError, gzip.BadGzipFile, SampleError) as error:
        print(f"local sample rejected: {error}", file=os.sys.stderr)
        return 2
    print(f"deterministic local sample created: records={result['sampleRecordCount']} sha256={result['sampleSha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
