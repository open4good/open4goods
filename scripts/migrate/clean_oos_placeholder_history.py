#!/usr/bin/env python3
"""Remove out-of-stock placeholder prices (<= 1 EUR) from occasion price histories.

Back Market publishes `search_price = 1` on out-of-stock rows. Until PR #3468,
PriceAggregationService aggregated them, so ~19k products got a 1 EUR occasion
min price and permanent 1 EUR points in `price.occasionPricehistory`.

Two steps:

    # 1. Before or right after deploying #3468: record the affected products.
    #    Selection is impossible later, once the 1 EUR offers have expired.
    python3 scripts/migrate/clean_oos_placeholder_history.py snapshot --out ids.txt

    # 2. At least 2 days after deploy (offers expire after VALID_UNTIL_DURATION):
    python3 scripts/migrate/clean_oos_placeholder_history.py clean --ids ids.txt            # dry run
    python3 scripts/migrate/clean_oos_placeholder_history.py clean --ids ids.txt --apply    # write

Selection (snapshot):
- products whose current min price is a backmarket.fr offer <= 1 EUR;
- vertical products whose occasion history holds a point <= 1 EUR.
Books and other merchants' 1 EUR prices (legitimate used books) are left untouched.

Clean:
- drops occasion history points <= 1 EUR and recomputes the OCCASION trend from
  the last two remaining points, like PriceAggregationService.computePriceHistory;
- skips products whose min price is still a backmarket.fr offer <= 1 EUR (fix not
  deployed yet, or offer not yet expired);
- writes with optimistic concurrency (if_seq_no / if_primary_term); conflicting
  products are reported and can be retried by rerunning the command.

Credentials are read from api/src/main/resources/application-devsec.yml (or
--config) and never printed.
"""

from __future__ import annotations

import argparse
import base64
import json
import re
import sys
import urllib.request
from collections import Counter
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
DEFAULT_CONFIG = REPO / "api/src/main/resources/application-devsec.yml"
DEFAULT_INDEX = "products-moustik"
PLACEHOLDER_MAX = 1.0
MERCHANT = "backmarket.fr"
PAGE = 2000


class Es:
    """Minimal Elasticsearch JSON client."""

    def __init__(self, config: Path):
        text = config.read_text()
        block = text[text.index("\n  elasticsearch:\n    uris:"):]
        get = lambda key: re.search(rf"\n\s+{key}:\s*(\S+)", block).group(1)
        uri = get("uris")
        self.url = uri if uri.startswith("http") else f"http://{uri}"
        token = base64.b64encode(f"{get('username')}:{get('password')}".encode()).decode()
        self.headers = {"Authorization": f"Basic {token}"}

    def call(self, path: str, body=None, method: str | None = None, ndjson: bool = False):
        if ndjson:
            data = body.encode()
            ctype = "application/x-ndjson"
        else:
            data = json.dumps(body).encode() if body is not None else None
            ctype = "application/json"
        req = urllib.request.Request(self.url + path, data=data, method=method or ("POST" if data else "GET"),
                                     headers={**self.headers, "Content-Type": ctype})
        with urllib.request.urlopen(req, timeout=300) as resp:
            return json.loads(resp.read())

    def scroll(self, index: str, query: dict, source: list[str]):
        res = self.call(f"/{index}/_search?scroll=5m", {"size": PAGE, "_source": source, "query": query})
        sid = res["_scroll_id"]
        try:
            while res["hits"]["hits"]:
                yield from res["hits"]["hits"]
                res = self.call("/_search/scroll", {"scroll": "5m", "scroll_id": sid})
                sid = res["_scroll_id"]
        finally:
            self.call("/_search/scroll", {"scroll_id": sid}, method="DELETE")


def is_placeholder_min(src: dict) -> bool:
    """True when the product's min price is still a Back Market placeholder."""
    m = (src.get("price") or {}).get("minPrice") or {}
    return m.get("datasourceName") == MERCHANT and (m.get("price") or PLACEHOLDER_MAX + 1) <= PLACEHOLDER_MAX


def snapshot(es: Es, index: str, out: Path) -> None:
    ids: set[str] = set()
    q_min = {"bool": {"filter": [{"range": {"price.minPrice.price": {"lte": PLACEHOLDER_MAX}}}]}}
    for hit in es.scroll(index, q_min, ["price.minPrice.datasourceName", "price.minPrice.price"]):
        if is_placeholder_min(hit["_source"]):
            ids.add(hit["_id"])
    from_min = len(ids)
    q_vert = {"bool": {"filter": [{"exists": {"field": "vertical"}}]}}
    for hit in es.scroll(index, q_vert, ["price.occasionPricehistory"]):
        history = ((hit["_source"].get("price") or {}).get("occasionPricehistory")) or []
        if any(p["price"] <= PLACEHOLDER_MAX for p in history):
            ids.add(hit["_id"])
    out.write_text("\n".join(sorted(ids)) + "\n")
    print(f"{len(ids)} products written to {out} ({from_min} with a {MERCHANT} min price <= {PLACEHOLDER_MAX}, "
          f"{len(ids) - from_min} more from vertical histories)")


def trend(history: list[dict]) -> int:
    if len(history) < 2:
        return 0
    last, previous = history[-1]["price"], history[-2]["price"]
    return -1 if last < previous else 1 if last > previous else 0


def clean(es: Es, index: str, ids_file: Path, apply: bool, preview_all: bool = False) -> None:
    ids = [line.strip() for line in ids_file.read_text().splitlines() if line.strip()]
    stats, examples = Counter(), []
    for start in range(0, len(ids), 500):
        chunk = ids[start:start + 500]
        res = es.call(f"/{index}/_search", {
            "size": len(chunk), "seq_no_primary_term": True, "query": {"ids": {"values": chunk}},
            "_source": ["price.minPrice.datasourceName", "price.minPrice.price", "price.occasionPricehistory",
                        "price.trends"]})
        stats["missing"] += len(chunk) - len(res["hits"]["hits"])
        actions = []
        for hit in res["hits"]["hits"]:
            src = hit["_source"]
            price = src.get("price") or {}
            history = price.get("occasionPricehistory") or []
            kept = [p for p in history if p["price"] > PLACEHOLDER_MAX]
            if is_placeholder_min(src):
                stats["skipped_still_placeholder_min"] += 1
                if not preview_all:
                    continue
            if len(kept) == len(history):
                stats["already_clean"] += 1
                continue
            trends = dict(price.get("trends") or {})
            trends["OCCASION"] = trend(kept)
            stats["to_clean"] += 1
            stats["points_removed"] += len(history) - len(kept)
            if len(examples) < 5:
                examples.append((hit["_id"], [p["price"] for p in history[-5:]], [p["price"] for p in kept[-5:]]))
            actions.append(json.dumps({"update": {"_index": hit["_index"], "_id": hit["_id"],
                                                  "if_seq_no": hit["_seq_no"],
                                                  "if_primary_term": hit["_primary_term"]}}))
            actions.append(json.dumps({"doc": {"price": {"occasionPricehistory": kept, "trends": trends}}}))
        if apply and actions:
            bulk = es.call("/_bulk", "\n".join(actions) + "\n", ndjson=True)
            for item in bulk["items"]:
                status = item["update"]["status"]
                stats["written" if status < 300 else "conflict" if status == 409 else f"error_{status}"] += 1
    print(("APPLIED" if apply else "DRY RUN") + f" on {len(ids)} products: {dict(stats)}")
    for product_id, before, after in examples:
        print(f"  {product_id}: last points {before} -> {after}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument("--index", default=DEFAULT_INDEX)
    sub = parser.add_subparsers(dest="command", required=True)
    snap = sub.add_parser("snapshot", help="record affected product ids")
    snap.add_argument("--out", type=Path, required=True)
    cl = sub.add_parser("clean", help="remove placeholder points (dry run unless --apply)")
    cl.add_argument("--ids", type=Path, required=True)
    cl.add_argument("--apply", action="store_true")
    cl.add_argument("--preview-all", action="store_true",
                    help="dry run only: also preview products whose min price is still a placeholder")
    args = parser.parse_args()
    if getattr(args, "apply", False) and getattr(args, "preview_all", False):
        parser.error("--preview-all cannot be combined with --apply")

    es = Es(args.config)
    if args.command == "snapshot":
        snapshot(es, args.index, args.out)
    else:
        clean(es, args.index, args.ids, args.apply, args.preview_all)
    return 0


if __name__ == "__main__":
    sys.exit(main())
