#!/usr/bin/env python3
"""Initialize configured stdio MCP servers and verify that they expose tools."""

from __future__ import annotations

import argparse
import json
import os
import selectors
import signal
import subprocess
import sys
import time
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]


def receive(process: subprocess.Popen[str], request_id: int, timeout: float) -> dict:
    """Read newline-delimited JSON-RPC until the requested response arrives."""
    selector = selectors.DefaultSelector()
    selector.register(process.stdout, selectors.EVENT_READ)
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if process.poll() is not None:
            stderr = process.stderr.read().strip()
            raise RuntimeError(f"server exited {process.returncode}: {stderr[-1000:]}")
        events = selector.select(min(0.5, deadline - time.monotonic()))
        for key, _ in events:
            line = key.fileobj.readline()
            if not line:
                continue
            try:
                message = json.loads(line)
            except json.JSONDecodeError:
                continue
            if message.get("id") == request_id:
                if "error" in message:
                    raise RuntimeError(str(message["error"]))
                return message.get("result") or {}
    raise TimeoutError(f"MCP response {request_id} timed out after {timeout:.0f}s")


def send(process: subprocess.Popen[str], message: dict) -> None:
    process.stdin.write(json.dumps(message, separators=(",", ":")) + "\n")
    process.stdin.flush()


def smoke(name: str, server: dict, timeout: float) -> int:
    environment = os.environ.copy()
    environment.update({str(key): str(value) for key, value in (server.get("env") or {}).items()})
    process = subprocess.Popen(
        [str(server["command"]), *[str(value) for value in server.get("args") or []]],
        cwd=ROOT,
        env=environment,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        start_new_session=True,
        bufsize=1,
    )
    try:
        send(process, {
            "jsonrpc": "2.0", "id": 1, "method": "initialize",
            "params": {
                "protocolVersion": "2025-03-26", "capabilities": {},
                "clientInfo": {"name": "open4goods-mcp-doctor", "version": "1"},
            },
        })
        initialized = receive(process, 1, timeout)
        send(process, {"jsonrpc": "2.0", "method": "notifications/initialized", "params": {}})
        send(process, {"jsonrpc": "2.0", "id": 2, "method": "tools/list", "params": {}})
        tools = receive(process, 2, timeout).get("tools") or []
        if not tools:
            raise RuntimeError("server returned no tools")
        protocol = initialized.get("protocolVersion", "unknown")
        print(f"OK {name}: {len(tools)} tools, protocol {protocol}")
        return 0
    except (BrokenPipeError, OSError, RuntimeError, TimeoutError) as exc:
        print(f"FAIL {name}: {exc}", file=sys.stderr)
        return 1
    finally:
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait(timeout=5)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--core", action="store_true", help="test only required project servers")
    parser.add_argument("--server", action="append", help="test only a named server")
    parser.add_argument("--timeout", type=float, default=60)
    args = parser.parse_args()
    manifest = yaml.safe_load((ROOT / "ops" / "mcp" / "servers.yml").read_text(encoding="utf-8"))
    failures = 0
    for name, server in manifest["servers"].items():
        if server["transport"] != "stdio":
            continue
        if args.server and name not in args.server:
            continue
        if args.core and not server.get("core"):
            continue
        failures += smoke(name, server, args.timeout)
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
