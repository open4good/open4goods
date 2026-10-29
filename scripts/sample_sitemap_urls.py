#!/usr/bin/env python3
"""Sample URLs from an XML sitemap, and optionally check them.

Without ``--check`` the script only writes a random sample of ``<loc>`` URLs to
``--out``. With ``--check`` it also requests every sampled URL, without
following redirects, and fails unless each one answers 200 with a
``rel=canonical`` equal to the URL itself (and, with ``--max-lastmod-age-hours``,
a ``<lastmod>`` younger than the given age). Requests are sequential and spaced
by ``--delay`` seconds; a 403 or 429 stops the run instead of retrying.

A site behind HTTP basic auth (beta) is reached by exporting
``SITEMAP_SAMPLER_BASIC_AUTH=user:password``; the value is never printed.

Example (product sitemap acceptance, GOU-1366)::

    python3 scripts/sample_sitemap_urls.py \\
        --sitemap https://beta.nudger.fr/sitemap/fr/product-pages.xml \\
        --n 200 --check --max-lastmod-age-hours 48
"""

from __future__ import annotations

import argparse
import base64
import os
import random
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path

USER_AGENT = "open4goods-sitemap-sampler/1.1"
BASIC_AUTH_ENV = "SITEMAP_SAMPLER_BASIC_AUTH"


def request_headers(accept: str | None = None) -> dict[str, str]:
    headers = {"User-Agent": USER_AGENT}
    if accept:
        headers["Accept"] = accept
    credentials = os.environ.get(BASIC_AUTH_ENV)
    if credentials:
        headers["Authorization"] = "Basic " + base64.b64encode(credentials.encode("utf-8")).decode("ascii")
    return headers


@dataclass(frozen=True)
class SitemapEntry:
    loc: str
    lastmod: str | None


@dataclass(frozen=True)
class CheckResult:
    url: str
    status: int | None
    location: str | None
    canonical: str | None
    lastmod: str | None
    problems: tuple[str, ...]

    @property
    def ok(self) -> bool:
        return not self.problems


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):  # noqa: D102
        return None


class _CanonicalParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.canonicals: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag != "link":
            return
        values = dict(attrs)
        rel = (values.get("rel") or "").lower().split()
        if "canonical" in rel and values.get("href"):
            self.canonicals.append(values["href"].strip())


def parse_sitemap(body: bytes) -> list[SitemapEntry]:
    root = ET.fromstring(body)
    namespace = ""
    if root.tag.startswith("{"):
        namespace = root.tag.split("}", 1)[0] + "}"

    entries: list[SitemapEntry] = []
    for url in root.iter(f"{namespace}url"):
        loc = url.findtext(f"{namespace}loc")
        if loc and loc.strip():
            lastmod = url.findtext(f"{namespace}lastmod")
            entries.append(SitemapEntry(loc.strip(), lastmod.strip() if lastmod else None))
    if not entries:
        # Sitemap index or non-standard file: keep every <loc>.
        for loc in root.iter(f"{namespace}loc"):
            if loc.text and loc.text.strip():
                entries.append(SitemapEntry(loc.text.strip(), None))
    return entries


def sitemap_entries(sitemap: str, timeout: float) -> list[SitemapEntry]:
    request = urllib.request.Request(sitemap, headers=request_headers())
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return parse_sitemap(response.read())


def sitemap_urls(sitemap: str, timeout: float) -> list[str]:
    return [entry.loc for entry in sitemap_entries(sitemap, timeout)]


def extract_canonicals(html: str) -> list[str]:
    parser = _CanonicalParser()
    parser.feed(html)
    return parser.canonicals


def parse_lastmod(value: str) -> datetime:
    normalized = value.strip().replace("Z", "+00:00")
    parsed = datetime.fromisoformat(normalized)
    if parsed.tzinfo is None:
        parsed = parsed.replace(tzinfo=timezone.utc)
    return parsed


def evaluate(
    entry: SitemapEntry,
    status: int | None,
    location: str | None,
    html: str,
    max_lastmod_age_hours: float | None,
    now: datetime,
) -> CheckResult:
    problems: list[str] = []
    canonical: str | None = None
    if status != 200:
        detail = f" -> {location}" if location else ""
        problems.append(f"HTTP {status}{detail}")
    else:
        canonicals = extract_canonicals(html)
        if not canonicals:
            problems.append("no rel=canonical")
        else:
            canonical = canonicals[0]
            if len(set(canonicals)) > 1:
                problems.append(f"{len(canonicals)} different rel=canonical")
            if canonical != entry.loc:
                problems.append(f"rel=canonical is {canonical}")
    if max_lastmod_age_hours is not None:
        if not entry.lastmod:
            problems.append("no lastmod")
        else:
            try:
                age_hours = (now - parse_lastmod(entry.lastmod)).total_seconds() / 3600
            except ValueError:
                problems.append(f"unparsable lastmod {entry.lastmod}")
            else:
                if age_hours > max_lastmod_age_hours:
                    problems.append(f"lastmod {entry.lastmod} is {age_hours:.0f} h old")
    return CheckResult(entry.loc, status, location, canonical, entry.lastmod, tuple(problems))


class SourceRefused(RuntimeError):
    """The site answered 403 or 429: the run stops instead of insisting."""


def fetch_without_redirect(url: str, timeout: float) -> tuple[int, str | None, str]:
    opener = urllib.request.build_opener(_NoRedirect)
    request = urllib.request.Request(url, headers=request_headers("text/html"))
    try:
        with opener.open(request, timeout=timeout) as response:
            charset = response.headers.get_content_charset() or "utf-8"
            return response.status, None, response.read().decode(charset, errors="replace")
    except urllib.error.HTTPError as error:
        if error.code in (403, 429):
            raise SourceRefused(f"{url} answered {error.code}") from error
        return error.code, error.headers.get("Location"), ""


def check_entries(
    entries: list[SitemapEntry],
    timeout: float,
    delay: float,
    max_lastmod_age_hours: float | None,
) -> list[CheckResult]:
    now = datetime.now(timezone.utc)
    results: list[CheckResult] = []
    for index, entry in enumerate(entries):
        if index and delay > 0:
            time.sleep(delay)
        try:
            status, location, html = fetch_without_redirect(entry.loc, timeout)
        except SourceRefused:
            raise
        except (urllib.error.URLError, TimeoutError) as error:
            results.append(CheckResult(entry.loc, None, None, None, entry.lastmod, (f"request failed: {error}",)))
            continue
        results.append(evaluate(entry, status, location, html, max_lastmod_age_hours, now))
    return results


def print_report(results: list[CheckResult]) -> None:
    failures = [result for result in results if not result.ok]
    redirects = sum(1 for result in results if result.status is not None and 300 <= result.status < 400)
    canonical_ok = sum(1 for result in results if result.canonical == result.url)
    for result in failures:
        print(f"FAIL {result.url}: {'; '.join(result.problems)}")
    print(
        f"Checked {len(results)} URL(s): {len(results) - len(failures)} OK, {len(failures)} failing, "
        f"{redirects} redirect(s), {canonical_ok} self-referencing rel=canonical"
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sitemap", required=True, help="Sitemap URL.")
    parser.add_argument("--n", type=int, default=3, help="Sample size.")
    parser.add_argument("--out", help="File receiving the sampled URLs, one per line.")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--timeout", type=float, default=20.0)
    parser.add_argument(
        "--check",
        action="store_true",
        help="Request each sampled URL and require 200 without redirect and a self-referencing rel=canonical.",
    )
    parser.add_argument(
        "--max-lastmod-age-hours",
        type=float,
        help="With --check, also require a <lastmod> younger than this many hours.",
    )
    parser.add_argument("--delay", type=float, default=1.0, help="Seconds between two checked URLs.")
    args = parser.parse_args(argv)

    if not args.out and not args.check:
        parser.error("give --out, --check, or both")

    entries = sitemap_entries(args.sitemap, args.timeout)
    random.Random(args.seed).shuffle(entries)
    sample = entries[: args.n]
    print(f"Sampled {len(sample)} of {len(entries)} URL(s) from {args.sitemap}")

    if args.out:
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text("".join(f"{entry.loc}\n" for entry in sample), encoding="utf-8")
        print(f"Wrote {len(sample)} URL(s) to {out}")

    if not args.check:
        return 0

    try:
        results = check_entries(sample, args.timeout, args.delay, args.max_lastmod_age_hours)
    except SourceRefused as error:
        print(f"Stopped: {error}", file=sys.stderr)
        return 2
    print_report(results)
    return 0 if all(result.ok for result in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())
