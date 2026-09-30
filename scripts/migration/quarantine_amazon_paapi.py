#!/usr/bin/env python3
"""Inventory or copy a legacy product index without Amazon PA-API content.

The tool never infers PA-API provenance from an Amazon-looking URL or from a
substring.  A record with the exact legacy ``amazon.fr`` source label is
quarantined as ambiguous.  APPLY writes only independently identified merchant
records to the explicit target index and may retain ASINs in a separate explicit
identity index.  It never changes the source index, aliases or cache files.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import re
import sys
import time
from collections import Counter
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen


class MigrationError(RuntimeError):
    """Raised when a guarded PA-API migration cannot continue safely."""


ASIN = re.compile(r"^[A-Z0-9]{10}$")
PROVEN_PAAPI_LABELS = frozenset({"amazon-paapi", "amazon-paapi-v5", "paapi"})
AMBIGUOUS_AMAZON_LABEL = "amazon.fr"


def call(base_url: str, authorization: str | None, path: str, method: str = "GET",
         body: dict[str, Any] | None = None) -> Any:
    """Call Elasticsearch without exposing credentials or document values in errors."""
    payload = None if body is None else json.dumps(body, separators=(",", ":")).encode("utf-8")
    request = Request(base_url.rstrip("/") + path, data=payload, method=method)
    if authorization:
        request.add_header("Authorization", authorization)
    if payload:
        request.add_header("Content-Type", "application/json")
    try:
        with urlopen(request, timeout=60) as response:  # noqa: S310 - private runtime input
            raw = response.read()
            return json.loads(raw) if raw else {}
    except HTTPError as error:
        raise MigrationError(f"Elasticsearch request failed with HTTP {error.code}") from error
    except (URLError, TimeoutError, json.JSONDecodeError) as error:
        raise MigrationError("Elasticsearch request failed") from error


def labels(value: Any) -> set[str]:
    """Normalize only explicit datasource labels, never URLs or arbitrary text."""
    if isinstance(value, str):
        return {value.casefold()}
    if isinstance(value, list):
        return {item.casefold() for item in value if isinstance(item, str)}
    return set()


def classify(source: dict[str, Any]) -> str:
    """Classify a legacy source record without inventing a provenance claim."""
    sources = labels(source.get("datasourceNames")) | labels(source.get("datasourceCodes"))
    if sources & PROVEN_PAAPI_LABELS:
        return "PROVEN_PAAPI"
    if AMBIGUOUS_AMAZON_LABEL in sources:
        return "AMBIGUOUS_AMAZON"
    return "INDEPENDENT_MERCHANT"


def asins(value: Any, key: str = "") -> set[str]:
    """Extract only syntax-valid ASIN identity values from explicit ASIN fields."""
    found: set[str] = set()
    if isinstance(value, dict):
        for child_key, child_value in value.items():
            found |= asins(child_value, child_key)
    elif isinstance(value, list):
        for item in value:
            found |= asins(item, key)
    elif key.casefold() in {"asin", "amazonasin"} and isinstance(value, str):
        candidate = value.strip().upper()
        if ASIN.fullmatch(candidate):
            found.add(candidate)
    return found


def identity_document(source_id: str, source: dict[str, Any]) -> dict[str, Any] | None:
    """Return the deliberately minimal approved ASIN treatment, if any."""
    values = sorted(asins(source.get("externalIds")))
    if not values:
        return None
    return {"legacySourceId": source_id, "asins": values, "provenance": "AMBIGUOUS_LEGACY_AMAZON"}


def content_types(source: dict[str, Any]) -> set[str]:
    """Describe populated legacy content without retaining its values."""
    fields = set(source)
    types = {"IDENTITY"}
    if fields & {"attributes", "descriptionsByDatasource", "names"}:
        types.add("ATTRIBUTE_OR_TEXT")
    if fields & {"resources", "coverImagePath"}:
        types.add("MEDIA")
    if fields & {"price", "offersCount", "offerNames"}:
        types.add("OFFER_OR_PRICE")
    return types


def identifier(value: str) -> str:
    """Use a stable opaque coordinate in checkpoints and reports."""
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def load_checkpoint(path: Path) -> dict[str, Any] | None:
    """Read a prior checkpoint, rejecting malformed or unrelated state."""
    if not path.exists():
        return None
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise MigrationError("checkpoint is unreadable") from error
    if not isinstance(value, dict) or not isinstance(value.get("searchAfter"), list):
        raise MigrationError("checkpoint has no usable search_after coordinate")
    return value


def write_json(path: Path, value: dict[str, Any]) -> None:
    """Atomically write a private report or checkpoint without document payloads."""
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    os.replace(temporary, path)
    os.chmod(path, 0o600)


def cancelled(path: Path | None) -> bool:
    """Allow a supervisor to stop between bounded batches."""
    return path is not None and path.exists()


def cache_manifest(path: Path | None, cache_root: Path | None, delete: bool) -> dict[str, Any]:
    """Inspect or delete only owner-reviewed, checksummed PA-API cache files."""
    if path is None:
        return {"status": "NOT_PROVIDED", "objectCount": 0, "bytes": 0, "deletedCount": 0}
    if cache_root is None or not cache_root.is_absolute() or not cache_root.is_dir():
        raise MigrationError("cache manifest requires an existing absolute --cache-root")
    try:
        entries = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise MigrationError("proven PA-API cache manifest is unreadable") from error
    if not isinstance(entries, list) or not entries:
        raise MigrationError("proven PA-API cache manifest must be a non-empty list")
    root = cache_root.resolve()
    total = deleted = deleted_bytes = 0
    seen: set[Path] = set()
    for entry in entries:
        if not isinstance(entry, dict) or entry.get("owner") != "amazon-paapi":
            raise MigrationError("cache manifest entry has no exact amazon-paapi owner")
        candidate = entry.get("path")
        digest = entry.get("sha256")
        if not isinstance(candidate, str) or not isinstance(digest, str) or not re.fullmatch(r"[a-f0-9]{64}", digest):
            raise MigrationError("cache manifest entry has no safe exact-object checksum")
        candidate_path = Path(candidate)
        object_path = candidate_path.resolve()
        if (object_path in seen or root not in object_path.parents or candidate_path.is_symlink()
                or not object_path.is_file()):
            raise MigrationError("cache manifest object is unsafe or unavailable")
        seen.add(object_path)
        digest_calculator = hashlib.sha256()
        with object_path.open("rb") as handle:
            for chunk in iter(lambda: handle.read(1024 * 1024), b""):
                digest_calculator.update(chunk)
        actual = digest_calculator.hexdigest()
        if actual != digest:
            raise MigrationError("cache manifest checksum does not match the exact object")
        size = object_path.stat().st_size
        total += size
        if delete:
            if not isinstance(entry.get("recoveryRef"), str) or not entry["recoveryRef"].strip():
                raise MigrationError("cache deletion requires a recovery reference for every exact object")
            object_path.unlink()
            deleted += 1
            deleted_bytes += size
    return {"status": "DELETED" if delete else "INVENTORIED", "objectCount": len(seen), "bytes": total,
            "deletedCount": deleted, "deletedBytes": deleted_bytes}


def upsert(base_url: str, authorization: str | None, index: str, document_id: str,
           document: dict[str, Any]) -> None:
    """Idempotently upsert one target document with optimistic retries."""
    call(base_url, authorization, f"/{quote(index, safe='')}/_update/{quote(document_id, safe='')}?retry_on_conflict=3",
         "POST", {"doc": document, "doc_as_upsert": True})


def require_index(base_url: str, authorization: str | None, index: str) -> None:
    """Require an explicitly provisioned target instead of auto-creating a mapping."""
    mapping = call(base_url, authorization, f"/{quote(index, safe='')}/_mapping")
    if not isinstance(mapping, dict) or not mapping:
        raise MigrationError("explicit migration target has no usable mapping")


def migrate(arguments: argparse.Namespace) -> dict[str, Any]:
    """Run bounded PIT/search_after work and retain only aggregate evidence."""
    authorization = None
    raw_auth = os.environ.get(arguments.basic_auth_env)
    if raw_auth:
        authorization = "Basic " + base64.b64encode(raw_auth.encode()).decode("ascii")
    checkpoint = load_checkpoint(arguments.checkpoint)
    if checkpoint and (checkpoint.get("source") != identifier(arguments.source)
                       or checkpoint.get("mode") != arguments.mode):
        raise MigrationError("checkpoint belongs to a different migration mode or source")
    if arguments.mode == "APPLY" and (not arguments.target or not arguments.identity_target):
        raise MigrationError("APPLY requires explicit --target and --identity-target indexes")
    if (arguments.target and arguments.target == arguments.source) or (
            arguments.identity_target and arguments.identity_target in {arguments.source, arguments.target}):
        raise MigrationError("source, target and identity targets must be distinct")
    if arguments.mode == "APPLY":
        require_index(arguments.elasticsearch_url, authorization, arguments.target)
        require_index(arguments.elasticsearch_url, authorization, arguments.identity_target)
    cache = cache_manifest(arguments.proven_paapi_cache_manifest, arguments.cache_root,
                           arguments.mode == "APPLY" and arguments.delete_proven_paapi_cache)

    pit = call(arguments.elasticsearch_url, authorization, f"/{quote(arguments.source, safe='')}/_pit?keep_alive=5m", "POST")
    pit_id = pit.get("id")
    if not isinstance(pit_id, str):
        raise MigrationError("Elasticsearch did not return a PIT identifier")
    counts: Counter[str] = Counter()
    content_counts: Counter[str] = Counter()
    cursor = checkpoint["searchAfter"] if checkpoint else None
    batches = 0
    cancelled_run = False
    try:
        while True:
            if cancelled(arguments.cancel_file):
                cancelled_run = True
                break
            query: dict[str, Any] = {"size": arguments.batch_size, "pit": {"id": pit_id, "keep_alive": "5m"},
                                     "sort": [{"_shard_doc": "asc"}], "track_total_hits": False}
            if cursor:
                query["search_after"] = cursor
            page = call(arguments.elasticsearch_url, authorization, "/_search", "POST", query)
            hits = page.get("hits", {}).get("hits", [])
            if not isinstance(hits, list):
                raise MigrationError("Elasticsearch search response is malformed")
            if not hits:
                break
            for hit in hits:
                source = hit.get("_source")
                source_id = hit.get("_id")
                if not isinstance(source, dict) or not isinstance(source_id, str):
                    raise MigrationError("legacy search hit has no usable source coordinate")
                category = classify(source)
                counts[category] += 1
                for content_type in content_types(source):
                    content_counts[f"{category}:{content_type}"] += 1
                if arguments.mode == "APPLY":
                    if category == "INDEPENDENT_MERCHANT":
                        upsert(arguments.elasticsearch_url, authorization, arguments.target, source_id, source)
                        counts["TARGET_UPSERTED"] += 1
                    elif category == "AMBIGUOUS_AMAZON":
                        identity = identity_document(source_id, source)
                        if identity:
                            upsert(arguments.elasticsearch_url, authorization, arguments.identity_target, source_id, identity)
                            counts["ASIN_IDENTITIES_UPSERTED"] += 1
            cursor = hits[-1].get("sort")
            if not isinstance(cursor, list):
                raise MigrationError("legacy search hit has no search_after coordinate")
            batches += 1
            write_json(arguments.checkpoint, {"schemaVersion": "open4goods.amazon-paapi-quarantine/v1",
                                              "mode": arguments.mode, "source": identifier(arguments.source), "searchAfter": cursor,
                                              "batches": batches, "counts": dict(sorted(counts.items())),
                                              "contentCounts": dict(sorted(content_counts.items()))})
    finally:
        try:
            call(arguments.elasticsearch_url, authorization, "/_pit", "DELETE", {"id": pit_id})
        except MigrationError:
            pass
    return {"schemaVersion": "open4goods.amazon-paapi-quarantine/v1", "mode": arguments.mode,
            "source": identifier(arguments.source), "target": identifier(arguments.target) if arguments.target else None,
            "identityTarget": identifier(arguments.identity_target) if arguments.identity_target else None,
            "batches": batches, "cancelled": cancelled_run, "counts": dict(sorted(counts.items())),
            "contentCounts": dict(sorted(content_counts.items())), "cache": cache}


def main() -> int:
    """Parse arguments and write a sanitized reconciliation report."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=("INVENTORY", "APPLY"), required=True)
    parser.add_argument("--elasticsearch-url", default=os.environ.get("PAAPI_MIGRATION_ELASTICSEARCH_URL"))
    parser.add_argument("--basic-auth-env", default="PAAPI_MIGRATION_ELASTICSEARCH_BASIC_AUTH")
    parser.add_argument("--source", required=True, help="explicit legacy index or alias")
    parser.add_argument("--target", help="explicit new dataset index; required for APPLY")
    parser.add_argument("--identity-target", help="explicit ASIN identity index; required for APPLY")
    parser.add_argument("--batch-size", type=int, default=500)
    parser.add_argument("--checkpoint", type=Path, required=True)
    parser.add_argument("--cancel-file", type=Path)
    parser.add_argument("--proven-paapi-cache-manifest", type=Path,
                        help="private JSON list of exact, checksummed amazon-paapi cache objects")
    parser.add_argument("--cache-root", type=Path,
                        help="absolute shared cache root; required with a cache manifest")
    parser.add_argument("--delete-proven-paapi-cache", action="store_true",
                        help="APPLY only: delete exact owner-reviewed cache objects after checksum and recovery checks")
    parser.add_argument("--report", type=Path, required=True)
    arguments = parser.parse_args()
    if not arguments.elasticsearch_url:
        parser.error("--elasticsearch-url or PAAPI_MIGRATION_ELASTICSEARCH_URL is required")
    if arguments.batch_size < 1 or arguments.batch_size > 5_000:
        parser.error("--batch-size must be between 1 and 5000")
    if arguments.delete_proven_paapi_cache and arguments.mode != "APPLY":
        parser.error("--delete-proven-paapi-cache requires APPLY mode")
    if arguments.delete_proven_paapi_cache and not arguments.proven_paapi_cache_manifest:
        parser.error("--delete-proven-paapi-cache requires --proven-paapi-cache-manifest")
    try:
        result = migrate(arguments)
        write_json(arguments.report, result)
    except MigrationError as error:
        print(f"amazon PA-API quarantine rejected: {error}", file=sys.stderr)
        return 2
    print("amazon PA-API quarantine completed; report contains aggregate counts only")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
