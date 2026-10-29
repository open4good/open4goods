"""Tests for scripts/sample_sitemap_urls.py --check (GOU-1366), against a local HTTP server."""

from __future__ import annotations

import contextlib
import importlib.util
import io
import sys
import threading
import unittest
import unittest.mock
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "sample_sitemap_urls.py"
spec = importlib.util.spec_from_file_location("sample_sitemap_urls", SCRIPT)
sampler = importlib.util.module_from_spec(spec)
assert spec.loader is not None
sys.modules[spec.name] = sampler
spec.loader.exec_module(sampler)


class _Site(BaseHTTPRequestHandler):
    origin = ""
    lastmod = ""
    paths: dict[str, tuple[int, dict[str, str], str]] = {}

    def do_GET(self) -> None:  # noqa: N802
        if self.path == "/sitemap.xml":
            body = '<?xml version="1.0" encoding="UTF-8"?>' \
                '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">' + "".join(
                    f"<url><loc>{self.origin}{path}</loc><lastmod>{self.lastmod}</lastmod></url>"
                    for path in self.paths
                ) + "</urlset>"
            self._send(200, {"Content-Type": "application/xml"}, body)
            return
        status, headers, body = self.paths[self.path]
        self._send(status, headers, body.replace("{origin}", self.origin))

    def _send(self, status: int, headers: dict[str, str], body: str) -> None:
        payload = body.encode("utf-8")
        self.send_response(status)
        for name, value in headers.items():
            self.send_header(name, value.replace("{origin}", self.origin))
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def log_message(self, *args) -> None:  # noqa: D102
        pass


def _page(canonical: str) -> tuple[int, dict[str, str], str]:
    return 200, {"Content-Type": "text/html; charset=utf-8"}, \
        f'<html><head><link rel="canonical" href="{canonical}"></head><body></body></html>'


class SampleSitemapUrlsCheckTest(unittest.TestCase):
    def serve(self, paths: dict[str, tuple[int, dict[str, str], str]], lastmod: str) -> str:
        server = ThreadingHTTPServer(("127.0.0.1", 0), _Site)
        origin = f"http://127.0.0.1:{server.server_address[1]}"
        _Site.origin, _Site.lastmod, _Site.paths = origin, lastmod, paths
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)
        return origin

    def run_check(self, origin: str, *extra: str) -> tuple[int, str]:
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            code = sampler.main(["--sitemap", f"{origin}/sitemap.xml", "--n", "10", "--check", "--delay", "0", *extra])
        return code, output.getvalue()

    def test_canonical_pages_pass(self) -> None:
        recent = datetime.now(timezone.utc).strftime("%Y-%m-%d")
        origin = self.serve({
            "/televiseurs/1-tv": _page("{origin}/televiseurs/1-tv"),
            "/lave-linges/2-ll": _page("{origin}/lave-linges/2-ll"),
        }, recent)

        code, output = self.run_check(origin, "--max-lastmod-age-hours", "48")

        self.assertEqual(code, 0, output)
        self.assertIn("2 OK, 0 failing, 0 redirect(s), 2 self-referencing rel=canonical", output)

    def test_redirect_wrong_canonical_and_stale_lastmod_fail(self) -> None:
        stale = (datetime.now(timezone.utc) - timedelta(days=10)).strftime("%Y-%m-%d")
        origin = self.serve({
            "/lave-linges/1-x": (301, {"Location": "{origin}/lave-vaisselles/1-x"}, ""),
            "/televiseurs/2-y": _page("{origin}/televiseurs/2-renamed"),
            "/televiseurs/3-z": _page("{origin}/televiseurs/3-z"),
        }, stale)

        code, output = self.run_check(origin, "--max-lastmod-age-hours", "48")

        self.assertEqual(code, 1, output)
        self.assertIn("HTTP 301 -> ", output)
        self.assertIn("rel=canonical is ", output)
        self.assertIn("h old", output)
        self.assertIn("0 OK, 3 failing, 1 redirect(s), 1 self-referencing rel=canonical", output)

    def test_rate_limit_stops_the_run(self) -> None:
        origin = self.serve({"/a": (429, {}, "")}, "2026-01-01")

        with contextlib.redirect_stderr(io.StringIO()):
            code, _ = self.run_check(origin)

        self.assertEqual(code, 2)

    def test_basic_auth_header_comes_from_environment(self) -> None:
        with unittest.mock.patch.dict("os.environ", {sampler.BASIC_AUTH_ENV: "qa:secret"}):
            self.assertEqual(sampler.request_headers()["Authorization"], "Basic cWE6c2VjcmV0")
        with unittest.mock.patch.dict("os.environ", {}, clear=True):
            self.assertNotIn("Authorization", sampler.request_headers())


if __name__ == "__main__":
    unittest.main()
