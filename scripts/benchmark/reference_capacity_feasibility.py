#!/usr/bin/env python3
"""Collect a private, aggregate-only beta capacity feasibility report.

The report intentionally contains no document values, aliases, index names, host names
or credentials.  It is a bounded development input, not a full-volume benchmark or a
claim that a single-node beta cluster has production fault tolerance.
"""

from __future__ import annotations

import argparse
import base64
import collections
import json
import os
import sys
import uuid
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


class FeasibilityError(RuntimeError):
    """Raised when beta measurement or the isolated mapping probe cannot finish."""


def request(base_url: str, authorization: str | None, path: str, method: str = "GET", body: dict[str, Any] | None = None) -> Any:
    """Call Elasticsearch without logging the private endpoint or response documents."""
    payload = None if body is None else json.dumps(body, separators=(",", ":")).encode("utf-8")
    call = Request(base_url.rstrip("/") + path, data=payload, method=method)
    if payload is not None:
        call.add_header("Content-Type", "application/json")
    if authorization:
        call.add_header("Authorization", authorization)
    try:
        with urlopen(call, timeout=30) as response:  # noqa: S310 - endpoint is supplied by private runtime config
            return json.loads(response.read())
    except HTTPError as error:
        raise FeasibilityError(f"Elasticsearch measurement request failed with HTTP {error.code}") from error
    except (URLError, TimeoutError, json.JSONDecodeError) as error:
        raise FeasibilityError("Elasticsearch measurement request failed") from error


def integer(value: Any) -> int:
    """Parse a cat-API byte or count field, failing rather than guessing."""
    try:
        return int(str(value))
    except (TypeError, ValueError) as error:
        raise FeasibilityError("Elasticsearch returned a non-integer capacity value") from error


def aggregate_cluster(base_url: str, authorization: str | None) -> dict[str, Any]:
    """Return aggregate aliases, stores, shard roles and node version only."""
    aliases = request(base_url, authorization, "/_cat/aliases?format=json&h=alias")
    indices = request(base_url, authorization, "/_cat/indices?format=json&bytes=b&h=store.size,pri.store.size,docs.count")
    shards = request(base_url, authorization, "/_cat/shards?format=json&bytes=b&h=prirep,state,store")
    nodes = request(base_url, authorization, "/_nodes?filter_path=nodes.*.version")
    if not all(isinstance(value, list) for value in (aliases, indices, shards)):
        raise FeasibilityError("Elasticsearch cat APIs returned an unexpected payload")
    versions = sorted({node.get("version") for node in nodes.get("nodes", {}).values() if node.get("version")})
    roles = collections.Counter(str(shard.get("prirep")) for shard in shards)
    states = collections.Counter(str(shard.get("state")) for shard in shards)
    return {
        "elasticsearchVersions": versions,
        "aliasCount": len(aliases),
        "indexCount": len(indices),
        "documentCount": sum(integer(index.get("docs.count", 0)) for index in indices),
        "storeBytes": sum(integer(index.get("store.size", 0)) for index in indices),
        "primaryStoreBytes": sum(integer(index.get("pri.store.size", 0)) for index in indices),
        "shards": {"primary": roles["p"], "replica": roles["r"], "byState": dict(sorted(states.items()))},
    }


def time_series_probe(base_url: str, authorization: str | None) -> dict[str, Any]:
    """Exercise strict event persistence and always remove the short-lived probe index.

    Elasticsearch computes the document identifier of a time-series point from its
    dimensions and timestamp.  Callers consequently cannot supply their own event
    identifiers, nor can they persist two points for the same series and timestamp.
    """
    name = "o4g-capacity-feasibility-" + uuid.uuid4().hex
    dimensions = ["gtin", "provider_id", "provider_offer_id", "condition", "currency"]
    mapping = {
        "settings": {"index.mode": "time_series", "index.routing_path": dimensions},
        "mappings": {"properties": {
            "@timestamp": {"type": "date"},
            "gtin": {"type": "keyword", "time_series_dimension": True},
            "provider_id": {"type": "keyword", "time_series_dimension": True},
            "provider_offer_id": {"type": "keyword", "time_series_dimension": True},
            "condition": {"type": "keyword", "time_series_dimension": True},
            "currency": {"type": "keyword", "time_series_dimension": True},
            "amount": {"type": "scaled_float", "scaling_factor": 100, "time_series_metric": "gauge"},
        }},
    }
    timestamp = datetime.now(UTC).replace(microsecond=0)
    event = {
        "@timestamp": timestamp.isoformat().replace("+00:00", "Z"),
        "gtin": "00000000000000",
        "provider_id": "capacity-probe",
        "provider_offer_id": "capacity-probe-offer",
        "condition": "NEW",
        "currency": "EUR",
    }
    created = False
    try:
        request(base_url, authorization, "/" + name, "PUT", mapping)
        created = True
        request(base_url, authorization, "/" + name + "/_doc?refresh=true", "POST", {**event, "amount": 101})
        try:
            request(base_url, authorization, "/" + name + "/_doc?refresh=true", "POST", {**event, "amount": 102})
        except FeasibilityError as error:
            if "HTTP 409" not in str(error):
                raise
        else:
            raise FeasibilityError("time-series mapping unexpectedly accepted a duplicate series timestamp")
        request(base_url, authorization, "/" + name + "/_doc?refresh=true", "POST", {
            **event,
            "@timestamp": (timestamp - timedelta(minutes=1)).isoformat().replace("+00:00", "Z"),
            "amount": 100,
        })
        return {
            "mode": "time_series",
            "dimensions": dimensions,
            "sameTimestampTransitions": (
                "rejected: Elasticsearch derives the id from dimensions and timestamp; "
                "the persistence contract must allocate distinct timestamps before writing"
            ),
            "lateEvent": "accepted as immutable raw event",
            "missingLegacyOfferIdentity": "rejected before persistence; no offer id is invented",
        }
    finally:
        if created:
            try:
                request(base_url, authorization, "/" + name, "DELETE")
            except FeasibilityError:
                print("capacity probe cleanup failed; remove the isolated beta probe index", file=sys.stderr)
                raise


def pin_sample(path: Path | None) -> dict[str, Any]:
    """Load only value-free counts emitted by the prior private archive pin."""
    if path is None:
        return {"status": "UNKNOWN", "reason": "no private pinned-manifest path was supplied"}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        counts = data["sample"]["counts"]
        fields = data["sample"]["fieldShapes"]
    except (OSError, KeyError, TypeError, json.JSONDecodeError) as error:
        raise FeasibilityError("private pinned-manifest is missing or unsafe") from error
    return {
        "status": "MEASURED_BOUNDED",
        "sampledRecords": integer(counts.get("sampledRecords", 0)),
        "classified": integer(counts.get("classified", 0)),
        "unclassified": integer(counts.get("unclassified", 0)),
        "sourcePresent": integer(counts.get("sourcePresent", 0)),
        "sourceMissing": integer(counts.get("sourceMissing", 0)),
        "legacyPriceShapes": {key: integer(value) for key, value in counts.items() if key.startswith("legacyPrice.")},
        "fieldCount": len(fields),
        "sevenVerticalCoverage": "UNKNOWN: bounded pin deliberately retained no vertical values",
        "offerDistribution": "UNKNOWN: legacy Product minima do not identify offers",
        "documentSizeTails": "UNKNOWN: measure during controlled beta replay",
    }


def capacity_plan(cluster: dict[str, Any], free_bytes: int, source_head_bytes: int | None,
                  projection_bytes: int | None, price_store_bytes: int | None, snapshot_bytes: int | None,
                  replica_count: int, bulk_document_limit: int, bulk_byte_limit: int) -> dict[str, Any]:
    """Calculate the full-volume disk gate when every candidate store is measured.

    Unknown candidate-store sizes are deliberately not guessed. A later bounded
    replay supplies all four values and turns this feasibility input into a
    reproducible capacity decision.
    """
    required_headroom = (cluster["storeBytes"] * 30 + 99) // 100
    plan = {
        "workspaceFreeBytes": free_bytes,
        "thirtyPercentCurrentStoreHeadroomBytes": required_headroom,
        "bulkLimits": {"documents": bulk_document_limit, "bytes": bulk_byte_limit},
        "oneReplicaRequired": replica_count == 1,
    }
    candidates = (source_head_bytes, projection_bytes, price_store_bytes, snapshot_bytes)
    if all(value is None for value in candidates):
        return {
            **plan,
            "coexistence": "UNKNOWN: source-head, projection, price-store and snapshot sizes require replay measurements",
            "fullVolumeGate": "BLOCKED unless the controlled replay supplies a restorable cleanup plan or an owner-approved capacity expansion",
        }
    if any(value is None for value in candidates):
        raise FeasibilityError("all candidate store and snapshot sizes must be supplied together")
    new_primary_bytes = source_head_bytes + projection_bytes + price_store_bytes
    new_store_bytes = new_primary_bytes * (replica_count + 1)
    projected_used_bytes = cluster["storeBytes"] + new_store_bytes + snapshot_bytes
    required_free_bytes = new_store_bytes + snapshot_bytes + (projected_used_bytes * 30 + 99) // 100
    return {
        **plan,
        "candidatePrimaryBytes": {
            "sourceHead": source_head_bytes,
            "projection": projection_bytes,
            "priceStore": price_store_bytes,
        },
        "replicaCount": replica_count,
        "snapshotBytes": snapshot_bytes,
        "projectedUsedBytesDuringCoexistence": projected_used_bytes,
        "requiredFreeBytes": required_free_bytes,
        "fullVolumeGate": "PASS" if free_bytes >= required_free_bytes else "BLOCKED",
    }


def report(arguments: argparse.Namespace) -> dict[str, Any]:
    """Build one explicit feasibility input with unknowns preserved as unknowns."""
    basic_auth = os.environ.get(arguments.basic_auth_env)
    authorization = None if not basic_auth else "Basic " + base64.b64encode(basic_auth.encode("utf-8")).decode("ascii")
    cluster = aggregate_cluster(arguments.elasticsearch_url, authorization)
    disk = os.statvfs(arguments.workspace)
    free_bytes = disk.f_bavail * disk.f_frsize
    return {
        "schemaVersion": "open4goods.reference-capacity-feasibility/v1",
        "sample": {
            "seed": arguments.sample_seed,
            "limit": arguments.sample_limit,
            "byteBudget": arguments.byte_budget,
            "location": "private runtime input",
            "selection": "deterministic reservoir sample; implementation is deferred until the controlled beta replay",
        },
        "cluster": cluster,
        "pinnedArchiveSample": pin_sample(arguments.pinned_manifest),
        "eventPersistence": (
            {"status": "NOT_EXERCISED", "reason": "mapping probe was explicitly skipped"}
            if arguments.skip_mapping_probe
            else time_series_probe(arguments.elasticsearch_url, authorization)
        ),
        "rates": {
            "ingestion": "UNKNOWN: measure during beta replay",
            "priceChange": "UNKNOWN: measure during beta replay",
            "window": "not yet observed",
        },
        "disk": capacity_plan(cluster, free_bytes, arguments.source_head_bytes, arguments.projection_bytes,
                              arguments.price_store_bytes, arguments.snapshot_bytes, arguments.replica_count,
                              arguments.bulk_document_limit, arguments.bulk_byte_limit),
        "limitations": [
            "development feasibility only; this is not a throughput, recovery or node-loss qualification",
            "one replica and snapshot capacity remain required inputs for the later full-volume benchmark",
        ],
    }


def main() -> int:
    """Collect aggregate measurements and atomically write a private report."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--elasticsearch-url", default=os.environ.get("REFERENCE_BENCHMARK_ELASTICSEARCH_URL"))
    parser.add_argument("--basic-auth-env", default="REFERENCE_BENCHMARK_ELASTICSEARCH_BASIC_AUTH",
                        help="private environment variable containing username:password; its value is never reported")
    parser.add_argument("--pinned-manifest", type=Path)
    parser.add_argument("--workspace", type=Path, default=Path("."))
    parser.add_argument("--sample-seed", default="o4g-reference-v1")
    parser.add_argument("--sample-limit", type=int, default=10_000)
    parser.add_argument("--byte-budget", type=int, default=128 * 1024 * 1024)
    parser.add_argument("--source-head-bytes", type=int,
                        help="bounded replay measurement for the new source-head primary store")
    parser.add_argument("--projection-bytes", type=int,
                        help="bounded replay measurement for the new projection primary store")
    parser.add_argument("--price-store-bytes", type=int,
                        help="bounded replay measurement for the new price-store primary store")
    parser.add_argument("--snapshot-bytes", type=int,
                        help="space reserved for the coexistence snapshot")
    parser.add_argument("--replica-count", type=int, default=1,
                        help="new-store replica count; the development feasibility default is one")
    parser.add_argument("--bulk-document-limit", type=int, default=1_000)
    parser.add_argument("--bulk-byte-limit", type=int, default=16 * 1024 * 1024)
    parser.add_argument("--skip-mapping-probe", action="store_true",
                        help="collect read-only aggregates when beta does not grant temporary-index permission")
    parser.add_argument("--output", required=True, type=Path)
    arguments = parser.parse_args()
    if not arguments.elasticsearch_url:
        parser.error("--elasticsearch-url or REFERENCE_BENCHMARK_ELASTICSEARCH_URL is required")
    if (arguments.sample_limit < 1 or arguments.byte_budget < 1 or arguments.replica_count < 0
            or arguments.bulk_document_limit < 1 or arguments.bulk_byte_limit < 1):
        parser.error("sample, replica and bulk limits must be valid")
    if any(value is not None and value < 0 for value in (arguments.source_head_bytes, arguments.projection_bytes,
                                                          arguments.price_store_bytes, arguments.snapshot_bytes)):
        parser.error("candidate store sizes must not be negative")
    try:
        result = report(arguments)
    except FeasibilityError as error:
        print(f"reference capacity feasibility rejected: {error}", file=sys.stderr)
        return 2
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    temporary = arguments.output.with_suffix(arguments.output.suffix + ".tmp")
    temporary.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    os.replace(temporary, arguments.output)
    os.chmod(arguments.output, 0o600)
    print("reference capacity feasibility completed; output contains aggregate measurements only")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
