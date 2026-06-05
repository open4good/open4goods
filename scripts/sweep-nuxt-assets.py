#!/usr/bin/env python3
"""Validate that pages reference only reachable Nuxt assets."""

from __future__ import annotations

import argparse
import html.parser
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass


ASSET_HEADERS = ("cf-ray", "cf-cache-status", "cache-control", "server")


class AssetParser(html.parser.HTMLParser):
    """Extract Nuxt asset URLs from HTML attributes."""

    def __init__(self) -> None:
        super().__init__()
        self.assets: set[str] = set()

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        for name, value in attrs:
            if name in {"src", "href"} and value and "/_nuxt/" in value:
                self.assets.add(value)


@dataclass
class FetchResult:
    url: str
    status: int
    headers: dict[str, str]
    body_excerpt: str


def fetch(url: str, timeout: float, max_bytes: int = 512) -> FetchResult:
    request = urllib.request.Request(url, headers={"User-Agent": "open4goods-nuxt-asset-sweep/1.0"})
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            status = response.status
            headers = {name: response.headers.get(name, "") for name in ASSET_HEADERS}
            body = response.read(max_bytes)
    except urllib.error.HTTPError as error:
        status = error.code
        headers = {name: error.headers.get(name, "") for name in ASSET_HEADERS}
        body = error.read(512)
    except urllib.error.URLError as error:
        return FetchResult(url, 0, {}, str(error.reason)[:512])

    return FetchResult(url, status, headers, body.decode("utf-8", errors="replace")[:512])


def read_urls(args: argparse.Namespace) -> list[str]:
    urls: list[str] = []
    urls.extend(args.url)

    if args.urls_file:
        with open(args.urls_file, encoding="utf-8") as file:
            urls.extend(line.strip() for line in file if line.strip() and not line.startswith("#"))

    if args.sitemap_url:
        result = fetch(args.sitemap_url, args.timeout, 2_000_000)
        if result.status >= 400 or result.status == 0:
            raise RuntimeError(f"Could not fetch sitemap {args.sitemap_url}: HTTP {result.status}")
        root = ET.fromstring(result.body_excerpt + "")
        namespace = ""
        if root.tag.startswith("{"):
            namespace = root.tag.split("}", 1)[0] + "}"
        for loc in root.findall(f".//{namespace}loc"):
            if loc.text:
                urls.append(loc.text.strip())

    seen: set[str] = set()
    unique_urls: list[str] = []
    for url in urls:
        resolved = urllib.parse.urljoin(args.base_url, url) if args.base_url else url
        if resolved not in seen:
            unique_urls.append(resolved)
            seen.add(resolved)
        if args.limit and len(unique_urls) >= args.limit:
            break
    return unique_urls


def asset_urls(page_url: str, html: str) -> list[str]:
    parser = AssetParser()
    parser.feed(html)
    return sorted(urllib.parse.urljoin(page_url, asset) for asset in parser.assets)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", action="append", default=[], help="Page URL or path to inspect.")
    parser.add_argument("--urls-file", help="File containing page URLs or paths.")
    parser.add_argument("--sitemap-url", help="Sitemap URL to sample page URLs from.")
    parser.add_argument("--base-url", help="Base URL used for relative page paths.")
    parser.add_argument("--limit", type=int, default=0, help="Maximum page count to inspect.")
    parser.add_argument("--timeout", type=float, default=15.0, help="Request timeout in seconds.")
    parser.add_argument("--json", action="store_true", help="Print machine-readable results.")
    args = parser.parse_args()

    pages = read_urls(args)
    if not pages:
        print("No page URLs provided.", file=sys.stderr)
        return 2

    failures: list[FetchResult] = []
    checked_assets = 0
    for page in pages:
        page_result = fetch(page, args.timeout, 2_000_000)
        if page_result.status >= 400 or page_result.status == 0:
            failures.append(page_result)
            continue

        for asset in asset_urls(page, page_result.body_excerpt):
            checked_assets += 1
            result = fetch(asset, args.timeout)
            if result.status >= 400 or result.status == 0:
                failures.append(result)

    report = {
        "classification": "frontend_asset_failure" if failures else "ok",
        "pages_checked": len(pages),
        "assets_checked": checked_assets,
        "failures": [asdict(failure) for failure in failures],
    }

    if args.json:
        print(json.dumps(report, indent=2, sort_keys=True))
    elif failures:
        print("frontend_asset_failure")
        for failure in failures:
            print(f"- {failure.status} {failure.url}")
            if failure.headers:
                print(f"  headers: {failure.headers}")
            if failure.body_excerpt:
                print(f"  body: {failure.body_excerpt[:160]}")
    else:
        print(f"Checked {checked_assets} Nuxt asset(s) from {len(pages)} page(s).")

    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
