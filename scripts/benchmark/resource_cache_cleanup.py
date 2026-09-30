#!/usr/bin/env python3
"""Safely inventory or quarantine obsolete beta product-resource cache files.

The legacy Product index is scanned through a bounded Elasticsearch point in
time (PIT) query.  Live ``resources.cacheKey`` values are kept in an on-disk
SQLite index rather than application heap.  No cache file is examined until
the PIT stream matches the count observed before the scan.  The default mode
only inventories.  ``MOVE`` preserves every candidate below a separate
filesystem, so it releases beta workspace space while retaining a recovery
copy.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import shutil
import sqlite3
import sys
import time
from collections.abc import Iterator
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen


class ResourceCleanupError(RuntimeError):
    """Raised when a bounded resource-cache cleanup cannot safely continue."""


SHARD_DEPTH = 3
UNKNOWN_SHARD = "UNKNOWN"


def call(base_url: str, authorization: str | None, path: str, method: str = "GET",
         body: dict[str, Any] | None = None) -> Any:
    """Call Elasticsearch without including private URLs or documents in errors."""
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
        raise ResourceCleanupError(f"Elasticsearch request failed with HTTP {error.code}") from error
    except (URLError, TimeoutError, json.JSONDecodeError) as error:
        raise ResourceCleanupError("Elasticsearch request failed") from error


def opaque(value: str) -> str:
    """Return a stable coordinate suitable for a private aggregate report."""
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def cancelled(path: Path | None) -> bool:
    """Allow a supervisor to cancel only between bounded database or file batches."""
    return path is not None and path.exists()


def usable_cache_key(value: Any) -> str | None:
    """Accept a cache key only when it can be compared to one cache filename."""
    if not isinstance(value, str) or not value or "/" in value or "\\" in value or "\x00" in value:
        return None
    return value


def resource_keys(source: dict[str, Any]) -> set[str]:
    """Extract exact legacy resource keys without interpreting URLs or media metadata."""
    resources = source.get("resources")
    if not isinstance(resources, list):
        return set()
    return {key for resource in resources if isinstance(resource, dict)
            if (key := usable_cache_key(resource.get("cacheKey"))) is not None}


def free_bytes(path: Path) -> int:
    """Return filesystem space available to the unprivileged application user."""
    stat = os.statvfs(path)
    return stat.f_bavail * stat.f_frsize


def require_free_space(path: Path, minimum_free_bytes: int) -> None:
    """Fail before a batch if the state filesystem reached its configured floor."""
    if free_bytes(path) < minimum_free_bytes:
        raise ResourceCleanupError("state filesystem is below the configured free-space floor")


def open_database(path: Path) -> sqlite3.Connection:
    """Open a durable on-disk live-key set, never an in-memory SQLite database."""
    if not path.is_absolute():
        raise ResourceCleanupError("--database must be an absolute private path")
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and not path.is_file():
        raise ResourceCleanupError("--database must name a regular file")
    connection = sqlite3.connect(path)
    connection.execute("PRAGMA journal_mode=WAL")
    connection.execute("PRAGMA synchronous=FULL")
    connection.execute("CREATE TABLE IF NOT EXISTS active_keys (cache_key TEXT PRIMARY KEY) WITHOUT ROWID")
    connection.execute("CREATE TABLE IF NOT EXISTS cleanup_metadata (name TEXT PRIMARY KEY, value TEXT NOT NULL) WITHOUT ROWID")
    return connection


def reset_database(connection: sqlite3.Connection) -> None:
    """Discard a previous incomplete run before accepting a new PIT stream."""
    connection.execute("DELETE FROM active_keys")
    connection.execute("DELETE FROM cleanup_metadata")
    connection.commit()


def insert_keys(connection: sqlite3.Connection, keys: set[str]) -> None:
    """Insert a bounded page of exact live keys without retaining prior pages in memory."""
    connection.executemany("INSERT OR IGNORE INTO active_keys(cache_key) VALUES (?)", ((key,) for key in keys))
    connection.commit()


def contains_key(connection: sqlite3.Connection, key: str) -> bool:
    """Test one cache filename against the disk-backed exact-key index."""
    return connection.execute("SELECT 1 FROM active_keys WHERE cache_key = ?", (key,)).fetchone() is not None


def mark_validated_database(connection: sqlite3.Connection, source: str, product_count: int,
                            active_key_count: int) -> None:
    """Mark the completed exact scan so MOVE can reuse it only under a maintenance gate."""
    connection.executemany("INSERT INTO cleanup_metadata(name, value) VALUES (?, ?)", (
        ("source", opaque(source)),
        ("productCount", str(product_count)),
        ("activeKeyCount", str(active_key_count)),
    ))
    connection.commit()


def validated_database(connection: sqlite3.Connection, source: str) -> tuple[int, int]:
    """Read only a complete source-bound database; partial scans can never be reused."""
    values = dict(connection.execute("SELECT name, value FROM cleanup_metadata"))
    if values.get("source") != opaque(source):
        raise ResourceCleanupError("database does not belong to this explicit source")
    try:
        product_count = int(values["productCount"])
        active_key_count = int(values["activeKeyCount"])
    except (KeyError, ValueError) as error:
        raise ResourceCleanupError("database has no completed validated scan") from error
    if product_count < 1 or active_key_count < 1:
        raise ResourceCleanupError("database has no usable completed validated scan")
    return product_count, active_key_count


def is_shard_folder(name: str) -> bool:
    """Match only the three-level shard layout produced by Resource.folderHashPrefix."""
    return len(name) == 1 or name == UNKNOWN_SHARD


def cache_files(cache_root: Path) -> Iterator[tuple[Path, Path]]:
    """Yield regular files below exactly three valid shard levels without following links."""
    def descend(folder: Path, depth: int) -> Iterator[tuple[Path, Path]]:
        with os.scandir(folder) as entries:
            for entry in entries:
                candidate = Path(entry.path)
                if entry.is_dir(follow_symlinks=False):
                    if depth < SHARD_DEPTH and is_shard_folder(entry.name):
                        yield from descend(candidate, depth + 1)
                    continue
                if depth == SHARD_DEPTH and entry.is_file(follow_symlinks=False):
                    yield candidate, candidate.relative_to(cache_root)

    yield from descend(cache_root, 0)


def source_count(base_url: str, authorization: str | None, source: str) -> int:
    """Read the initial source count used to reject an incomplete PIT stream."""
    value = call(base_url, authorization, f"/{quote(source, safe='')}/_count")
    count = value.get("count") if isinstance(value, dict) else None
    if not isinstance(count, int) or count < 1:
        raise ResourceCleanupError("source index must have a positive count")
    return count


def populate_live_keys(arguments: argparse.Namespace, connection: sqlite3.Connection,
                       authorization: str | None) -> tuple[int, int, bool]:
    """Fill SQLite from a validated bounded PIT stream and return documents, keys, cancellation."""
    expected_count = source_count(arguments.elasticsearch_url, authorization, arguments.source)
    pit = call(arguments.elasticsearch_url, authorization,
               f"/{quote(arguments.source, safe='')}/_pit?keep_alive=5m", "POST")
    pit_id = pit.get("id") if isinstance(pit, dict) else None
    if not isinstance(pit_id, str):
        raise ResourceCleanupError("Elasticsearch did not return a PIT identifier")
    reset_database(connection)
    cursor: list[Any] | None = None
    documents = 0
    cancelled_run = False
    try:
        while True:
            if cancelled(arguments.cancel_file):
                cancelled_run = True
                break
            require_free_space(arguments.database.parent, arguments.minimum_free_bytes)
            query: dict[str, Any] = {
                "size": arguments.batch_size,
                "pit": {"id": pit_id, "keep_alive": "5m"},
                "sort": [{"_shard_doc": "asc"}],
                "track_total_hits": False,
                "_source": ["resources.cacheKey"],
            }
            if cursor:
                query["search_after"] = cursor
            page = call(arguments.elasticsearch_url, authorization, "/_search", "POST", query)
            hits = page.get("hits", {}).get("hits", []) if isinstance(page, dict) else None
            if not isinstance(hits, list):
                raise ResourceCleanupError("Elasticsearch search response is malformed")
            if not hits:
                break
            keys: set[str] = set()
            for hit in hits:
                source = hit.get("_source") if isinstance(hit, dict) else None
                if not isinstance(source, dict):
                    raise ResourceCleanupError("legacy search hit has no usable source")
                keys.update(resource_keys(source))
            insert_keys(connection, keys)
            documents += len(hits)
            cursor = hits[-1].get("sort") if isinstance(hits[-1], dict) else None
            if not isinstance(cursor, list):
                raise ResourceCleanupError("legacy search hit has no search_after coordinate")
    finally:
        try:
            call(arguments.elasticsearch_url, authorization, "/_pit", "DELETE", {"id": pit_id})
        except ResourceCleanupError:
            pass
    active_keys = connection.execute("SELECT COUNT(*) FROM active_keys").fetchone()[0]
    if not cancelled_run and documents != expected_count:
        raise ResourceCleanupError("PIT stream does not match the source count; no cache file was touched")
    if not cancelled_run and active_keys < 1:
        raise ResourceCleanupError("PIT stream has no usable live cache keys; no cache file was touched")
    if not cancelled_run:
        mark_validated_database(connection, arguments.source, documents, active_keys)
    return documents, active_keys, cancelled_run


def quarantine_file(source: Path, relative_path: Path, quarantine_root: Path) -> None:
    """Copy an exact orphan to another filesystem, then remove the source only on success."""
    destination = quarantine_root / relative_path
    if destination.exists() or destination.is_symlink():
        raise ResourceCleanupError("quarantine destination already exists")
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)
    if source.stat().st_size != destination.stat().st_size:
        destination.unlink(missing_ok=True)
        raise ResourceCleanupError("quarantine copy size validation failed")
    source.unlink()


def sweep(arguments: argparse.Namespace, connection: sqlite3.Connection) -> dict[str, int | bool]:
    """Inventory or move orphan candidates one at a time after the stream is validated."""
    scanned = preserved = within_grace = orphaned = orphan_bytes = moved = moved_bytes = 0
    cancelled_run = move_limit_reached = False
    start_millis = int(time.time() * 1_000)
    for source, relative_path in cache_files(arguments.cache_root):
        if scanned % arguments.file_check_interval == 0 and cancelled(arguments.cancel_file):
            cancelled_run = True
            break
        scanned += 1
        if contains_key(connection, source.name):
            preserved += 1
            continue
        if start_millis - int(source.stat().st_mtime * 1_000) <= arguments.grace_period_ms:
            within_grace += 1
            continue
        orphaned += 1
        size = source.stat().st_size
        orphan_bytes += size
        if arguments.mode == "MOVE":
            if (arguments.max_move_bytes is not None
                    and moved_bytes + size > arguments.max_move_bytes):
                move_limit_reached = True
                break
            quarantine_file(source, relative_path, arguments.quarantine_root)
            moved += 1
            moved_bytes += size
    return {
        "scannedFileCount": scanned,
        "preservedFileCount": preserved,
        "withinGracePeriodFileCount": within_grace,
        "orphanFileCount": orphaned,
        "orphanBytes": orphan_bytes,
        "movedFileCount": moved,
        "movedBytes": moved_bytes,
        "cancelled": cancelled_run,
        "moveLimitReached": move_limit_reached,
    }


def write_json(path: Path, value: dict[str, Any]) -> None:
    """Atomically write a private, aggregate-only report."""
    if not path.is_absolute():
        raise ResourceCleanupError("--report must be an absolute private path")
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    os.replace(temporary, path)
    os.chmod(path, 0o600)


def cleanup(arguments: argparse.Namespace) -> dict[str, Any]:
    """Run one guarded cleanup and return no cache or document coordinates."""
    raw_auth = os.environ.get(arguments.basic_auth_env)
    authorization = None if not raw_auth else "Basic " + base64.b64encode(raw_auth.encode()).decode("ascii")
    with open_database(arguments.database) as connection:
        reused_database = arguments.reuse_validated_database
        if reused_database:
            documents, active_keys = validated_database(connection, arguments.source)
            if source_count(arguments.elasticsearch_url, authorization, arguments.source) != documents:
                raise ResourceCleanupError("source count changed since the validated scan; no cache file was touched")
            cancelled_during_index = False
        else:
            documents, active_keys, cancelled_during_index = populate_live_keys(arguments, connection, authorization)
        result: dict[str, Any] = {
            "schemaVersion": "open4goods.resource-cache-cleanup/v1",
            "mode": arguments.mode,
            "source": opaque(arguments.source),
            "streamedProductCount": documents,
            "activeCacheKeyCount": active_keys,
            "cancelledDuringIndex": cancelled_during_index,
            "reusedValidatedDatabase": reused_database,
        }
        if not cancelled_during_index:
            result["sweep"] = sweep(arguments, connection)
        return result


def main() -> int:
    """Parse guarded runtime inputs and write the reconciliation report."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=("INVENTORY", "MOVE"), default="INVENTORY")
    parser.add_argument("--elasticsearch-url", default=os.environ.get("RESOURCE_CLEANUP_ELASTICSEARCH_URL"))
    parser.add_argument("--basic-auth-env", default="RESOURCE_CLEANUP_ELASTICSEARCH_BASIC_AUTH")
    parser.add_argument("--source", required=True, help="explicit legacy Product index or alias")
    parser.add_argument("--cache-root", type=Path, required=True, help="existing absolute cache directory")
    parser.add_argument("--database", type=Path, required=True, help="absolute private SQLite state path")
    parser.add_argument("--report", type=Path, required=True, help="absolute private aggregate report path")
    parser.add_argument("--quarantine-root", type=Path,
                        help="existing absolute recovery filesystem; required for MOVE")
    parser.add_argument("--recovery-reference", help="private recovery record; required for MOVE")
    parser.add_argument("--reuse-validated-database", action="store_true",
                        help="MOVE only: reuse a completed source-bound SQLite scan under a maintenance gate")
    parser.add_argument("--maintenance-gate-reference",
                        help="private beta maintenance record; required when reusing a validated database")
    parser.add_argument("--batch-size", type=int, default=1_000)
    parser.add_argument("--minimum-free-bytes", type=int, default=10 * 1024 * 1024 * 1024)
    parser.add_argument("--grace-period-ms", type=int, default=86_400_000,
                        help="preserve recently modified cache files while an index writer catches up")
    parser.add_argument("--file-check-interval", type=int, default=1_000)
    parser.add_argument("--max-move-bytes", type=int,
                        help="MOVE only: stop after this much recoverable data has been released")
    parser.add_argument("--cancel-file", type=Path)
    arguments = parser.parse_args()
    if not arguments.elasticsearch_url:
        parser.error("--elasticsearch-url or RESOURCE_CLEANUP_ELASTICSEARCH_URL is required")
    if not arguments.cache_root.is_absolute() or not arguments.cache_root.is_dir():
        parser.error("--cache-root must be an existing absolute directory")
    if arguments.batch_size < 1 or arguments.batch_size > 5_000:
        parser.error("--batch-size must be between 1 and 5000")
    if arguments.minimum_free_bytes < 1 or arguments.file_check_interval < 1 or arguments.grace_period_ms < 0:
        parser.error("free-space, grace-period and file-check limits must be valid")
    if arguments.mode == "MOVE":
        if not arguments.quarantine_root or not arguments.quarantine_root.is_absolute() or not arguments.quarantine_root.is_dir():
            parser.error("MOVE requires an existing absolute --quarantine-root")
        if not arguments.recovery_reference or not arguments.recovery_reference.strip():
            parser.error("MOVE requires a non-empty --recovery-reference")
        if os.stat(arguments.cache_root).st_dev == os.stat(arguments.quarantine_root).st_dev:
            parser.error("MOVE requires a quarantine root on a different filesystem")
    if arguments.reuse_validated_database:
        if arguments.mode != "MOVE":
            parser.error("--reuse-validated-database requires MOVE mode")
        if not arguments.maintenance_gate_reference or not arguments.maintenance_gate_reference.strip():
            parser.error("--reuse-validated-database requires --maintenance-gate-reference")
    if arguments.max_move_bytes is not None:
        if arguments.mode != "MOVE" or arguments.max_move_bytes < 1:
            parser.error("--max-move-bytes requires MOVE mode and a positive value")
    try:
        result = cleanup(arguments)
        write_json(arguments.report, result)
    except ResourceCleanupError as error:
        print(f"resource cache cleanup rejected: {error}", file=sys.stderr)
        return 2
    print("resource cache cleanup completed; report contains aggregate counts only")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
