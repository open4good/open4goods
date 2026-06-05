#!/usr/bin/env python3
"""Sample URLs from an XML sitemap."""

from __future__ import annotations

import argparse
import random
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path


def sitemap_urls(sitemap: str, timeout: float) -> list[str]:
    request = urllib.request.Request(sitemap, headers={"User-Agent": "open4goods-sitemap-sampler/1.0"})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        body = response.read()

    root = ET.fromstring(body)
    namespace = ""
    if root.tag.startswith("{"):
        namespace = root.tag.split("}", 1)[0] + "}"

    urls: list[str] = []
    for loc in root.findall(f".//{namespace}loc"):
        if loc.text:
            urls.append(loc.text.strip())
    return urls


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sitemap", required=True)
    parser.add_argument("--n", type=int, default=3)
    parser.add_argument("--out", required=True)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--timeout", type=float, default=20.0)
    args = parser.parse_args()

    urls = sitemap_urls(args.sitemap, args.timeout)
    random.Random(args.seed).shuffle(urls)
    sample = urls[: args.n]

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(sample) + ("\n" if sample else ""), encoding="utf-8")
    print(f"Wrote {len(sample)} URL(s) from {args.sitemap} to {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
