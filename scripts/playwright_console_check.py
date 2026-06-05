#!/usr/bin/env python3
"""Fail when sampled pages emit browser errors or Nuxt asset failures."""

from __future__ import annotations

import argparse
import asyncio
import json
from pathlib import Path
from typing import Any

from playwright.async_api import async_playwright


ASSET_HEADER_NAMES = ("cf-ray", "cf-cache-status", "cache-control", "server")


def is_asset_failure(text: str, url: str = "", status: int = 0) -> bool:
    lowered = text.lower()
    return (
        "/_nuxt/" in url
        and status >= 500
        or "failed to fetch dynamically imported module" in lowered
        or "loading chunk" in lowered
        or "importing a module script failed" in lowered
    )


async def response_excerpt(response: Any) -> str:
    try:
        body = await response.body()
    except Exception as error:  # noqa: BLE001 - monitoring should report the failed read, not crash.
        return f"<body unavailable: {error}>"
    return body[:512].decode("utf-8", errors="replace")


async def inspect_url(browser: Any, url: str, timeout_ms: int) -> dict[str, Any]:
    page = await browser.new_page()
    failures: list[dict[str, Any]] = []
    response_tasks: list[asyncio.Task[None]] = []

    page.on(
        "console",
        lambda message: failures.append(
            {
                "type": "console",
                "classification": "frontend_asset_failure"
                if is_asset_failure(message.text)
                else "console_error_or_pageerror",
                "level": message.type,
                "text": message.text,
            }
        )
        if message.type == "error"
        else None,
    )
    page.on(
        "pageerror",
        lambda error: failures.append(
            {
                "type": "pageerror",
                "classification": "frontend_asset_failure"
                if is_asset_failure(str(error))
                else "console_error_or_pageerror",
                "text": str(error),
            }
        ),
    )

    async def on_response(response: Any) -> None:
        status = response.status
        response_url = response.url
        if "/_nuxt/" not in response_url and status < 500:
            return
        if status < 400:
            return
        headers = await response.all_headers()
        failures.append(
            {
                "type": "response",
                "classification": "frontend_asset_failure"
                if is_asset_failure("", response_url, status)
                else "http_failure",
                "status": status,
                "url": response_url,
                "headers": {name: headers.get(name, "") for name in ASSET_HEADER_NAMES},
                "body_excerpt": await response_excerpt(response) if status >= 500 else "",
            }
        )

    def schedule_response_check(response: Any) -> None:
        response_tasks.append(asyncio.create_task(on_response(response)))

    page.on("response", schedule_response_check)

    try:
        response = await page.goto(url, wait_until="networkidle", timeout=timeout_ms)
        if response and response.status >= 500:
            headers = await response.all_headers()
            failures.append(
                {
                    "type": "navigation",
                    "classification": "http_failure",
                    "status": response.status,
                    "url": response.url,
                    "headers": {name: headers.get(name, "") for name in ASSET_HEADER_NAMES},
                    "body_excerpt": await response_excerpt(response),
                }
            )
    except Exception as error:  # noqa: BLE001
        failures.append({"type": "navigation", "classification": "navigation_failure", "text": str(error)})

    if response_tasks:
        await asyncio.gather(*response_tasks, return_exceptions=True)
    await page.close()
    classifications = {failure["classification"] for failure in failures}
    if "frontend_asset_failure" in classifications:
        reason = "frontend_asset_failure"
    elif failures:
        reason = "console_error_or_pageerror"
    else:
        reason = "ok"
    return {"url": url, "reason": reason, "failures": failures}


async def run(args: argparse.Namespace) -> int:
    urls = [line.strip() for line in Path(args.urls).read_text(encoding="utf-8").splitlines() if line.strip()]
    Path(args.out).mkdir(parents=True, exist_ok=True)

    async with async_playwright() as playwright:
        browser = await playwright.chromium.launch()
        try:
            results = [await inspect_url(browser, url, args.timeout_ms) for url in urls]
        finally:
            await browser.close()

    report_path = Path(args.out) / "console-report.json"
    report_path.write_text(json.dumps(results, indent=2, sort_keys=True), encoding="utf-8")

    failed = [result for result in results if result["reason"] != "ok"]
    for result in failed:
        print(f"URL: {result['url']}")
        print(f"Reason: {result['reason']}")
        for failure in result["failures"]:
            print(json.dumps(failure, sort_keys=True))

    return 1 if failed else 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--urls", required=True, help="File containing one URL per line.")
    parser.add_argument("--out", required=True, help="Directory for console-report.json.")
    parser.add_argument("--timeout-ms", type=int, default=20_000)
    return asyncio.run(run(parser.parse_args()))


if __name__ == "__main__":
    raise SystemExit(main())
