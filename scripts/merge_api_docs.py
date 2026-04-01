"""
API Docs Merger
Gộp các file JSON trong .docs/api/ thành 1 file duy nhất.

Usage:
  # Gộp toàn bộ module auth
  python scripts/merge_api_docs.py auth

  # Gộp nhiều module
  python scripts/merge_api_docs.py auth orders

  # Gộp file chỉ định
  python scripts/merge_api_docs.py auth/register auth/login

  # Gộp tất cả
  python scripts/merge_api_docs.py --all

  # Chỉ định output file
  python scripts/merge_api_docs.py auth -o context/auth.json
"""

import argparse
import json
import os
import sys

DOCS_DIR = os.path.join(os.path.dirname(__file__), "..", ".docs", "api")


def load_file(path: str) -> dict | None:
    if not os.path.exists(path):
        print(f"  [skip] not found: {path}")
        return None
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def collect_module(module: str) -> list[dict]:
    folder = os.path.join(DOCS_DIR, module)
    if not os.path.isdir(folder):
        print(f"  [skip] module not found: {module}")
        return []
    results = []
    for fname in sorted(os.listdir(folder)):
        if fname.endswith(".json") and fname != "index.json":
            doc = load_file(os.path.join(folder, fname))
            if doc:
                results.append(doc)
    return results


def collect_file(spec: str) -> list[dict]:
    # spec có thể là "auth/register" hoặc "auth/register.json"
    if not spec.endswith(".json"):
        spec += ".json"
    path = os.path.join(DOCS_DIR, spec)
    doc = load_file(path)
    return [doc] if doc else []


def collect_all() -> list[dict]:
    results = []
    for entry in sorted(os.listdir(DOCS_DIR)):
        full = os.path.join(DOCS_DIR, entry)
        if os.path.isdir(full):
            results.extend(collect_module(entry))
    return results


def merge(targets: list[str], all_modules: bool) -> list[dict]:
    docs = []
    if all_modules:
        docs = collect_all()
    else:
        for t in targets:
            # Nếu có "/" thì là file cụ thể, không thì là module
            if "/" in t or "\\" in t:
                docs.extend(collect_file(t))
            else:
                docs.extend(collect_module(t))
    return docs


def save(docs: list[dict], output: str):
    os.makedirs(os.path.dirname(os.path.abspath(output)), exist_ok=True)
    with open(output, "w", encoding="utf-8") as f:
        json.dump(docs, f, indent=2, ensure_ascii=False)
    print(f"\nMerged {len(docs)} endpoint(s) → {output}")


def default_output(targets: list[str], all_modules: bool) -> str:
    base = os.path.join(os.path.dirname(__file__), "..", ".docs", "api")
    if all_modules:
        return os.path.join(base, "index.json")
    # Nếu tất cả target cùng module thì đặt vào module/index.json
    modules = set()
    for t in targets:
        modules.add(t.split("/")[0].split("\\")[0])
    if len(modules) == 1:
        return os.path.join(base, modules.pop(), "index.json")
    return os.path.join(base, "index.json")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Merge API doc JSON files")
    parser.add_argument("targets", nargs="*", help="Modules or files to merge (e.g. auth, auth/login)")
    parser.add_argument("--all", action="store_true", help="Merge all modules")
    parser.add_argument("-o", "--output", help="Output file path")
    args = parser.parse_args()

    if not args.all and not args.targets:
        parser.print_help()
        sys.exit(1)

    docs = merge(args.targets, args.all)
    if not docs:
        print("No docs found.")
        sys.exit(1)

    output = args.output or default_output(args.targets, args.all)
    save(docs, output)
