"""
Entry point để chạy capture docs.

Cách dùng:
  python scripts/run.py                    # chạy tất cả module
  python scripts/run.py auth               # chỉ chạy auth
  python scripts/run.py brands categories  # chạy nhiều module
"""

import sys
import os

# Thêm scripts/ vào path để import capture package
sys.path.insert(0, os.path.dirname(__file__))

from capture.base import login, BASE_URL
from capture import auth, brands, categories

# ── Config ────────────────────────────────────────────────────────────────────
ADMIN_EMAIL = "hackerisdead1032002@gmail.com"
ADMIN_PASSWORD = "hackerisdead1032002@gmail.com"

# ── Module registry ───────────────────────────────────────────────────────────
MODULES = {
    "auth":       lambda s: auth.capture(s, ADMIN_EMAIL, ADMIN_PASSWORD),
    "brands":     lambda s: brands.capture(s),
    "categories": lambda s: categories.capture(s),
}

# ── Main ──────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    selected = sys.argv[1:] if len(sys.argv) > 1 else list(MODULES.keys())

    # Validate
    invalid = [m for m in selected if m not in MODULES]
    if invalid:
        print(f"Unknown modules: {invalid}")
        print(f"Available: {list(MODULES.keys())}")
        sys.exit(1)

    print(f"Base URL : {BASE_URL}")
    print(f"Account  : {ADMIN_EMAIL}")
    print(f"Modules  : {selected}")

    s = login(ADMIN_EMAIL, ADMIN_PASSWORD)
    if not s.cookies.get("SESSION"):
        print("ERROR: Login failed. Check ADMIN_EMAIL / ADMIN_PASSWORD in scripts/run.py")
        sys.exit(1)

    print(f"Logged in. SESSION={s.cookies.get('SESSION')[:20]}...")

    for module in selected:
        MODULES[module](s)

    print("\nDone. Docs saved to .docs/api/")
