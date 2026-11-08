#!/usr/bin/env python3
"""Read-only report on products that lost their offers (GOU-1699).

An offer (`price.offers[]`) expires 2 days after its fragment was last indexed
(`ProductRepository.VALID_UNTIL_DURATION`); front-api hides it at read time,
the api drops it at the next aggregation of the product. Since #3468 an
out-of-stock fragment no longer refreshes its offer, so a merchant that flags
a product out of stock loses the offer 2 days later.

    # One product: offers with their source, condition, price and age.
    python3 scripts/diagnostics/stale_offers_report.py --config <cfg> product 7333394142296

    # Vertical products without a fresh offer of a condition, but with an
    # offer or a price history point of that condition in the last N days,
    # by source of the most recent offer and by day it was last seen.
    python3 scripts/diagnostics/stale_offers_report.py --config <cfg> lost --days 7

`lost` only reads. The source is known while the expired offer is still stored
(product not re-aggregated since); otherwise it is reported as
`<purged>`: the offer was dropped and only the price history remains.

Credentials are read from a YAML holding the
`\\n  elasticsearch:\\n    uris: / username: / password:` block (same shape as
`application-devsec.yml`) and never printed.
"""

from __future__ import annotations

import argparse
import base64
import json
import re
import sys
import time
import urllib.request
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
DEFAULT_CONFIG = REPO / "api/src/main/resources/application-devsec.yml"
DEFAULT_INDEX = "products-moustik"
VALID_UNTIL_MS = 2 * 24 * 3600 * 1000
DAY_MS = 24 * 3600 * 1000
PAGE = 2000
CONDITIONS = ("NEW", "OCCASION")
HISTORY_FIELD = {"NEW": "newPricehistory", "OCCASION": "occasionPricehistory"}


class Es:
    """Minimal read-only Elasticsearch JSON client."""

    def __init__(self, config: Path):
        text = config.read_text()
        block = text[text.index("\n  elasticsearch:\n    uris:"):]
        get = lambda key: re.search(rf"\n\s+{key}:\s*(\S+)", block).group(1)
        uri = get("uris")
        self.url = uri if uri.startswith("http") else f"http://{uri}"
        token = base64.b64encode(f"{get('username')}:{get('password')}".encode()).decode()
        self.headers = {"Authorization": f"Basic {token}"}

    def call(self, path: str, body=None, method: str | None = None):
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(self.url + path, data=data, method=method or ("POST" if data else "GET"),
                                     headers={**self.headers, "Content-Type": "application/json"})
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


def fmt(ts: int | None) -> str:
    if not ts:
        return "-"
    return datetime.fromtimestamp(ts / 1000, timezone.utc).strftime("%Y-%m-%d %H:%M UTC")


def product(es: Es, index: str, gtin: str, now: int) -> None:
    src = es.call(f"/{index}/_doc/{gtin}?_source=lastChange,offersCount,vertical,datasourceCodes,"
                  "price.offers,price.minPrice,price.newPricehistory,price.occasionPricehistory")["_source"]
    price = src.get("price") or {}
    print(f"gtin={gtin} vertical={src.get('vertical')} offersCount(stored)={src.get('offersCount')} "
          f"lastChange={fmt(src.get('lastChange'))}")
    print("datasources:", ", ".join(sorted((src.get("datasourceCodes") or {}).keys())))
    for offer in sorted(price.get("offers") or [], key=lambda o: o.get("timeStamp") or 0, reverse=True):
        ts = offer.get("timeStamp") or 0
        state = "fresh" if now - ts <= VALID_UNTIL_MS else "EXPIRED"
        print(f"  offer {offer.get('datasourceName')!s:28} {offer.get('productState')!s:9} "
              f"{offer.get('price')!s:>9} {offer.get('currency', '')} seen {fmt(ts)} ({state})")
    for condition in CONDITIONS:
        history = price.get(HISTORY_FIELD[condition]) or []
        tail = ", ".join(f"{p.get('price')}@{fmt(p.get('timestamp'))[:10]}" for p in history[-3:])
        print(f"  {condition} history ({len(history)} points), last: {tail or '-'}")


def lost(es: Es, index: str, days: int, now: int) -> None:
    window = now - days * DAY_MS
    totals = Counter()
    by_source = defaultdict(Counter)
    by_day = defaultdict(Counter)
    fresh_by_source = defaultdict(Counter)
    query = {"bool": {"filter": [{"exists": {"field": "vertical"}}]}}
    source = ["price.offers.datasourceName", "price.offers.productState", "price.offers.timeStamp",
              "price.newPricehistory.timestamp", "price.occasionPricehistory.timestamp"]
    for hit in es.scroll(index, query, source):
        price = hit["_source"].get("price") or {}
        offers = price.get("offers") or []
        totals["vertical_products"] += 1
        for condition in CONDITIONS:
            mine = [o for o in offers if o.get("productState") == condition and o.get("timeStamp")]
            fresh = [o for o in mine if now - o["timeStamp"] <= VALID_UNTIL_MS]
            for o in fresh:
                fresh_by_source[condition][o.get("datasourceName")] += 1
            if fresh:
                totals[f"{condition}_with_fresh_offer"] += 1
                continue
            last_offer = max(mine, key=lambda o: o["timeStamp"], default=None)
            history = price.get(HISTORY_FIELD[condition]) or []
            last_history = max((p.get("timestamp") or 0 for p in history), default=0)
            last_seen = max(last_offer["timeStamp"] if last_offer else 0, last_history)
            if last_seen < window:
                continue
            totals[f"{condition}_lost_in_window"] += 1
            src_name = last_offer.get("datasourceName") if last_offer else "<purged>"
            by_source[condition][src_name] += 1
            by_day[condition][fmt(last_seen)[:10]] += 1

    print(f"report at {fmt(now)}, window {days} days: {dict(totals)}")
    for condition in CONDITIONS:
        print(f"\n{condition}: products without fresh offer, last seen in window, by source of last offer")
        for name, count in by_source[condition].most_common():
            print(f"  {name!s:32} {count:>7}   (fresh offers now: {fresh_by_source[condition].get(name, 0)})")
        print(f"{condition}: by day last seen (UTC)")
        for day, count in sorted(by_day[condition].items()):
            print(f"  {day} {count:>7}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument("--index", default=DEFAULT_INDEX)
    sub = parser.add_subparsers(dest="command", required=True)
    one = sub.add_parser("product", help="offers of one product")
    one.add_argument("gtin")
    report = sub.add_parser("lost", help="vertical products that lost their offers")
    report.add_argument("--days", type=int, default=7)
    args = parser.parse_args()

    es = Es(args.config)
    now = int(time.time() * 1000)
    if args.command == "product":
        product(es, args.index, args.gtin, now)
    else:
        lost(es, args.index, args.days, now)
    return 0


if __name__ == "__main__":
    sys.exit(main())
