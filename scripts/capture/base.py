"""
Shared helpers cho tất cả capture modules.
"""

import json
import os
import time
import requests

BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")
DOCS_DIR = os.path.join(os.path.dirname(__file__), "..", "..", ".docs", "api")


def save(module: str, name: str, doc: dict):
    folder = os.path.join(DOCS_DIR, module)
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, f"{name}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(doc, f, indent=2, ensure_ascii=False, default=str)
    print(f"  saved → {path}")


def save_index(module: str):
    folder = os.path.join(DOCS_DIR, module)
    index = {}
    for fname in sorted(os.listdir(folder)):
        if fname.endswith(".json") and fname != f"{module}.json":
            with open(os.path.join(folder, fname), encoding="utf-8") as f:
                index[fname.replace(".json", "")] = json.load(f)
    with open(os.path.join(folder, f"{module}.json"), "w", encoding="utf-8") as f:
        json.dump(index, f, indent=2, ensure_ascii=False, default=str)
    print(f"  index → {module}/{module}.json")


def call(session: requests.Session, method: str, path: str,
         body: dict = None, params: dict = None) -> dict:
    url = f"{BASE_URL}{path}"
    resp = getattr(session, method)(
        url, json=body, params=params,
        headers={"Content-Type": "application/json"}
    )
    try:
        body_out = resp.json()
    except Exception:
        body_out = resp.text
    return {"status": resp.status_code, "body": body_out}


def anon(method: str, path: str, body: dict = None, params: dict = None) -> dict:
    return call(requests.Session(), method, path, body, params)


def build_doc(endpoint: str, description: str, request: dict,
              responses: dict, notes: list = None) -> dict:
    doc = {"endpoint": endpoint, "description": description,
           "request": request, "responses": responses}
    if notes:
        doc["notes"] = notes
    return doc


def unique_email() -> str:
    return f"test_{int(time.time())}@example.com"


def login(email: str, password: str) -> requests.Session:
    s = requests.Session()
    s.post(f"{BASE_URL}/api/auth/login",
           json={"email": email, "password": password},
           headers={"Content-Type": "application/json"})
    return s
