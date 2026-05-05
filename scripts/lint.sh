#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

python3 - <<'PY'
from pathlib import Path
import re
import sys

required = ["scope", "author", "contributors", "tags", "locale"]
errors = []
for path in Path("docs").rglob("*.md") if Path("docs").exists() else []:
    text = path.read_text(encoding="utf-8")
    if not text.startswith("---\n"):
        continue
    end = text.find("\n---\n", 4)
    if end == -1:
        continue
    fm = text[4:end]
    missing = [k for k in required if not re.search(rf"^\s*{re.escape(k)}\s*:", fm, re.M)]
    if missing:
        errors.append(f"{path}: missing metadata keys: {', '.join(missing)}")

if errors:
    print("RAG metadata lint failed:")
    for e in errors:
        print(" -", e)
    sys.exit(1)

print("RAG metadata lint passed")
PY

echo "lint.sh completed"
