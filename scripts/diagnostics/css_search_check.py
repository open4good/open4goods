#!/usr/bin/env python3
"""Search quality check for the Google CSS application (nudger.fr/rechercher).

Google's CSS review rejected nudger.fr (June 2026) because the search
"does not offer search functionality" and "does not generally return product
offers relevant to the user's query". This script replays a fixed query set
(`css-search-queries.json`) against a running site and reports, per query:

- **SSR**: the HTML that `/rechercher?q=<query>` sends before any JavaScript
  runs contains the links of the first results the API returns, shows
  prices, and does not say "Aucun resultat". Crawlers and Google's review
  tooling read that HTML.
- **Relevance**: the first results, in display order (each vertical group
  capped at 4 products, then products without a vertical), are the product
  type asked for: right vertical (`expect_vertical`) or a wanted word in the
  name or slug (`expect_tokens`), and no accessory word (`reject_tokens`).
- **Offer quality**: each of those results has an image, a price and a
  merchant; the share with 2+ offers (a real comparison) is reported.

A query passes when SSR is OK, at least 80% of the first results are on
target with no accessory among them, at least 80% have an image and all have
a price and a merchant.

    # Production, markdown report on stdout
    python3 scripts/diagnostics/css_search_check.py

    # Another environment, JSON details kept for comparison
    python3 scripts/diagnostics/css_search_check.py \\
        --base-url https://beta.nudger.fr --json-out /tmp/css-search.json

Read-only: one GET of the search page and one POST to the public search API
per query, as a browser does. Standard library only.
"""

from __future__ import annotations

import argparse
import html
import http.cookiejar
import json
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import asdict, dataclass, field
from pathlib import Path

DEFAULT_BASE_URL = "https://nudger.fr"
DEFAULT_QUERIES = Path(__file__).with_name("css-search-queries.json")
SEARCH_PATH = "/rechercher"
API_PATH = "/api/products/search"
CSRF_COOKIE = "csrf_token"
CSRF_HEADER = "x-csrf-token"
USER_AGENT = "Mozilla/5.0 (compatible; nudger-css-search-check/1.0)"
VERTICAL_RESULTS_LIMIT = 4  # frontend/app/pages/search/index.vue
NO_RESULT_MARKERS = ("Aucun résultat pour", "No results for")
PRODUCT_LINK_RE = re.compile(r'href="(/(?:[a-z0-9-]+/)?\d{8,14}-[^"]*)"')
PRICE_RE = re.compile(r"\d[\d\s.,]*\s?€")

RELEVANT_SHARE_MIN = 0.8
IMAGE_SHARE_MIN = 0.8


@dataclass
class ResultLine:
    """One displayed search result, reduced to what the check scores."""

    name: str
    slug: str
    vertical: str | None
    has_image: bool
    price: float | None
    merchant: str | None
    offers_count: int
    on_target: bool
    rejected: bool


@dataclass
class QueryReport:
    """Outcome of one query of the set."""

    query: str
    scope: str
    ssr_status: int | None = None
    ssr_no_result_message: bool = False
    ssr_product_links: int = 0
    ssr_prices: int = 0
    ssr_top_links_present: float = 0.0
    api_error: str | None = None
    results: list[ResultLine] = field(default_factory=list)
    relevant_share: float = 0.0
    image_share: float = 0.0
    priced_share: float = 0.0
    compared_share: float = 0.0
    passed: bool = False

    @property
    def ssr_ok(self) -> bool:
        return (
            self.ssr_status == 200
            and not self.ssr_no_result_message
            and self.ssr_product_links > 0
            and self.ssr_prices > 0
            and self.ssr_top_links_present == 1.0
        )


def normalize(text: str | None) -> str:
    """Lowercase, strip accents and punctuation, collapse spaces."""
    if not text:
        return ""
    folded = unicodedata.normalize("NFKD", text)
    folded = "".join(c for c in folded if not unicodedata.combining(c))
    return re.sub(r"[^a-z0-9]+", " ", folded.lower()).strip()


def contains_any(haystack: str, needles: list[str]) -> bool:
    """True when one normalized needle starts at a word boundary.

    Prefix matching lets "oled55c3" match "oled55c35la" and "cafe" match
    "cafetiere", without matching "lame" inside "flamme".
    """
    padded = f" {haystack}"
    return any(f" {normalize(n)}" in padded for n in needles if normalize(n))


def visible_text(page: str) -> str:
    """Text a reader sees in the HTML, without scripts and styles."""
    body = re.sub(r"<script[\s\S]*?</script>", " ", page)
    body = re.sub(r"<style[\s\S]*?</style>", " ", body)
    body = re.sub(r"<[^>]+>", " ", body)
    return html.unescape(re.sub(r"\s+", " ", body))


def build_opener() -> tuple[urllib.request.OpenerDirector, http.cookiejar.CookieJar]:
    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    opener.addheaders = [("User-Agent", USER_AGENT)]
    return opener, jar


def csrf_token(jar: http.cookiejar.CookieJar) -> str | None:
    for cookie in jar:
        if cookie.name == CSRF_COOKIE:
            return urllib.parse.unquote(cookie.value)
    return None


def check_ssr(opener, base_url: str, report: QueryReport, timeout: float) -> str:
    """Fetch the server-rendered search page, inspect its HTML, return it."""
    url = f"{base_url}{SEARCH_PATH}?{urllib.parse.urlencode({'q': report.query})}"
    try:
        with opener.open(url, timeout=timeout) as response:
            report.ssr_status = response.status
            page = response.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as error:
        report.ssr_status = error.code
        return ""
    text = visible_text(page)
    report.ssr_no_result_message = any(m in text for m in NO_RESULT_MARKERS)
    report.ssr_product_links = len(set(PRODUCT_LINK_RE.findall(page)))
    report.ssr_prices = len(PRICE_RE.findall(text))
    return page


def fetch_results(opener, jar, base_url: str, query: str, timeout: float) -> dict:
    """POST the global search like the browser does (CSRF cookie + header)."""
    token = csrf_token(jar)
    if not token:
        raise RuntimeError("no csrf_token cookie after loading the search page")
    body = json.dumps(
        {"query": query, "filters": {"filters": [], "filterGroups": []}}
    ).encode("utf-8")
    request = urllib.request.Request(
        f"{base_url}{API_PATH}",
        data=body,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Accept": "application/json",
            CSRF_HEADER: token,
        },
    )
    with opener.open(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))


def displayed_products(payload: dict) -> list[dict]:
    """Products in the order the search page shows them."""
    products: list[dict] = []
    for group in payload.get("verticalGroups") or []:
        hits = [r.get("product") for r in group.get("results") or []]
        products.extend(p for p in hits[:VERTICAL_RESULTS_LIMIT] if p)
    products.extend(
        r["product"]
        for r in payload.get("missingVerticalResults") or []
        if r.get("product")
    )
    return products


def to_line(product: dict, spec: dict) -> ResultLine:
    names = product.get("names") or {}
    base = product.get("base") or {}
    identity = product.get("identity") or {}
    resources = product.get("resources") or {}
    offers = product.get("offers") or {}
    best = offers.get("bestPrice") or {}
    name = (
        names.get("displayName")
        or base.get("bestName")
        or identity.get("bestName")
        or str(product.get("gtin", ""))
    )
    slug = product.get("fullSlug") or product.get("slug") or ""
    haystack = normalize(f"{name} {slug.replace('-', ' ')}")
    vertical = base.get("vertical")

    on_target = True
    if spec.get("expect_vertical"):
        on_target = vertical == spec["expect_vertical"]
    if on_target and spec.get("expect_tokens"):
        on_target = contains_any(haystack, spec["expect_tokens"])
    rejected = contains_any(haystack, spec.get("reject_tokens") or [])

    return ResultLine(
        name=name,
        slug=slug,
        vertical=vertical,
        has_image=bool(
            resources.get("coverImagePath")
            or resources.get("externalCover")
            or resources.get("images")
        ),
        price=best.get("price"),
        merchant=best.get("datasourceName"),
        offers_count=int(offers.get("offersCount") or 0),
        on_target=on_target and not rejected,
        rejected=rejected,
    )


def score(report: QueryReport, top: int) -> None:
    lines = report.results[:top]
    if not lines:
        report.passed = False
        return
    count = len(lines)
    report.relevant_share = sum(line.on_target for line in lines) / count
    report.image_share = sum(line.has_image for line in lines) / count
    report.priced_share = (
        sum(line.price is not None and bool(line.merchant) for line in lines) / count
    )
    report.compared_share = sum(line.offers_count >= 2 for line in lines) / count
    report.passed = (
        report.ssr_ok
        and report.relevant_share >= RELEVANT_SHARE_MIN
        and not any(line.rejected for line in lines)
        and report.image_share >= IMAGE_SHARE_MIN
        and report.priced_share == 1.0
    )


def run(base_url: str, specs: list[dict], top: int, timeout: float, pause: float) -> list[QueryReport]:
    reports = []
    for spec in specs:
        opener, jar = build_opener()
        report = QueryReport(query=spec["q"], scope=spec.get("scope", ""))
        try:
            page = check_ssr(opener, base_url, report, timeout)
            payload = fetch_results(opener, jar, base_url, spec["q"], timeout)
            report.results = [to_line(p, spec) for p in displayed_products(payload)][
                : max(top, 10)
            ]
            # The first results must be in the served HTML, not only "some"
            # product: closed category panels left them out (brand queries).
            top_slugs = [line.slug for line in report.results[:top] if line.slug]
            if top_slugs:
                report.ssr_top_links_present = sum(
                    f'href="{slug}"' in page for slug in top_slugs
                ) / len(top_slugs)
        except Exception as error:  # report and keep going: one query must not stop the run
            report.api_error = f"{type(error).__name__}: {error}"
        score(report, top)
        reports.append(report)
        time.sleep(pause)
    return reports


def pct(value: float) -> str:
    return f"{round(value * 100)}%"


def markdown(reports: list[QueryReport], base_url: str, top: int) -> str:
    passed = sum(r.passed for r in reports)
    ssr_ok = sum(r.ssr_ok for r in reports)
    out = [
        f"# CSS search check - {base_url}",
        "",
        f"- Queries passed: **{passed}/{len(reports)}**",
        f"- Server-rendered results: **{ssr_ok}/{len(reports)}**",
        f"- Scored on the first {top} displayed results",
        "",
        "| Query | Scope | SSR | On target | Image | Price+merchant | 2+ offers | Pass |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for r in reports:
        if r.ssr_ok:
            ssr = "OK"
        elif r.ssr_no_result_message:
            ssr = "no result"
        elif r.ssr_status == 200:
            ssr = f"top {pct(r.ssr_top_links_present)} in HTML"
        else:
            ssr = f"KO ({r.ssr_status})"
        if r.api_error:
            ssr += " / API error"
        out.append(
            f"| `{r.query}` | {r.scope} | {ssr} | {pct(r.relevant_share)} | "
            f"{pct(r.image_share)} | {pct(r.priced_share)} | "
            f"{pct(r.compared_share)} | {'yes' if r.passed else 'no'} |"
        )
    out += ["", f"## First {top} results per query", ""]
    for r in reports:
        out.append(f"### `{r.query}`")
        if r.api_error:
            out.append(f"- API error: {r.api_error}")
        for line in r.results[:top]:
            flags = []
            if not line.on_target:
                flags.append("off-target")
            if line.rejected:
                flags.append("accessory")
            if not line.has_image:
                flags.append("no image")
            price = f"{line.price} EUR" if line.price is not None else "no price"
            out.append(
                f"- {line.name} ({line.vertical or 'no vertical'}) - {price} "
                f"@ {line.merchant or '?'} - {line.offers_count} offer(s)"
                + (f" - **{', '.join(flags)}**" if flags else "")
            )
        if not r.results:
            out.append("- (no result)")
        out.append("")
    return "\n".join(out)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--queries", type=Path, default=DEFAULT_QUERIES)
    parser.add_argument("--top", type=int, default=5)
    parser.add_argument("--timeout", type=float, default=60.0)
    parser.add_argument("--pause", type=float, default=1.0, help="seconds between queries")
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--only", help="run only queries containing this text")
    args = parser.parse_args(argv)

    specs = json.loads(args.queries.read_text(encoding="utf-8"))["queries"]
    if args.only:
        specs = [s for s in specs if args.only.lower() in s["q"].lower()]
    base_url = args.base_url.rstrip("/")

    reports = run(base_url, specs, args.top, args.timeout, args.pause)
    print(markdown(reports, base_url, args.top))
    if args.json_out:
        args.json_out.write_text(
            json.dumps([asdict(r) | {"ssr_ok": r.ssr_ok} for r in reports], ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
    return 0 if all(r.passed for r in reports) else 1


if __name__ == "__main__":
    sys.exit(main())
