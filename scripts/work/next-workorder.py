#!/usr/bin/env python3
"""Compatibility entry point for the autonomous development prompt."""

from __future__ import annotations

import sys

from wo import main


if __name__ == "__main__":
    raise SystemExit(main(["next", *sys.argv[1:]]))
