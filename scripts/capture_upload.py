"""
API Documentation Capture — Upload (Cloudinary)
Chạy: python scripts/capture_upload.py
Yêu cầu: server đang chạy, đã login để lấy token

Lưu ý: Các endpoint này require auth.
Set biến môi trường ACCESS_TOKEN trước khi chạy:
  export ACCESS_TOKEN=<your_access_token>
  python scripts/capture_upload.py
"""

import requests
import json
import os

BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")
DOCS_DIR = os.path.join(os.path.dirname(__file__), "..", ".docs", "api")
TOKEN = os.getenv("ACCESS_TOKEN", "")


def save(module: str, name: str, doc: dict):
    folder = os.path.join(DOCS_DIR, module)
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, f"{name}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(doc, f, indent=2, ensure_ascii=False)
    print(f"  saved → {path}")


def call(method: str, path: str, body: dict = None, token: str = None,
         params: dict = None) -> dict:
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    resp = getattr(requests, method)(url, json=body, headers=headers, params=params)
    try:
        resp_body = resp.json()
    except Exception:
        resp_body = resp.text
    return {"status": resp.status_code, "body": resp_body}


def build_doc(endpoint: str, description: str, request: dict, responses: dict,
              notes: list[str] = None) -> dict:
    doc = {"endpoint": endpoint, "description": description,
           "request": request, "responses": responses}
    if notes:
        doc["notes"] = notes
    return doc


def capture_upload():
    print("\n[upload]")

    if not TOKEN:
        print("  [warn] ACCESS_TOKEN not set — auth endpoints will return 401")

    # ── POST /api/upload/sign ─────────────────────────────────────────────────
    sign_success = call("post", "/api/upload/sign", {"folder": "brands"}, token=TOKEN)
    sign_invalid_folder = call("post", "/api/upload/sign", {"folder": "invalid_folder"}, token=TOKEN)
    sign_missing_folder = call("post", "/api/upload/sign", {}, token=TOKEN)
    sign_no_auth = call("post", "/api/upload/sign", {"folder": "brands"})

    save("upload", "sign", build_doc(
        endpoint="POST /api/upload/sign",
        description="Generate a Cloudinary upload signature. Client uses this to upload directly to Cloudinary without going through the backend.",
        request={
            "headers": {"Authorization": "Bearer <access-token>"},
            "body": {"folder": "brands"},
        },
        responses={
            "200": sign_success,
            "422_invalid_folder": sign_invalid_folder,
            "422_missing_folder": sign_missing_folder,
            "401_no_auth": sign_no_auth,
        },
        notes=[
            "Allowed folders: products, brands, categories, avatars, banners",
            "After getting signature, client POSTs to: https://api.cloudinary.com/v1_1/<cloudName>/image/upload",
            "Required form fields: file, api_key, timestamp, signature, folder",
            "Cloudinary returns public_id and secure_url — save public_id to DB",
        ]
    ))

    # ── GET /api/upload/assets ────────────────────────────────────────────────
    assets_brands = call("get", "/api/upload/assets", token=TOKEN,
                         params={"folder": "brands", "maxResults": 10})
    assets_no_auth = call("get", "/api/upload/assets", params={"folder": "brands"})
    assets_with_cursor = {
        "status": 200,
        "body": {
            "data": {
                "resources": [
                    {
                        "public_id": "brands/uuid-example",
                        "secure_url": "https://res.cloudinary.com/<cloud>/image/upload/brands/uuid-example.jpg",
                        "width": 800,
                        "height": 600,
                        "format": "jpg",
                        "created_at": "2026-01-01T00:00:00Z",
                        "bytes": 102400
                    }
                ],
                "nextCursor": "<cursor-string-or-null>"
            }
        }
    }

    save("upload", "assets", build_doc(
        endpoint="GET /api/upload/assets",
        description="List images from a Cloudinary folder. Used for media library/gallery in dashboard.",
        request={
            "headers": {"Authorization": "Bearer <access-token>"},
            "query": {
                "folder": "brands (required)",
                "maxResults": "50 (default, max 100)",
                "cursor": "nextCursor from previous response (for pagination)",
            },
        },
        responses={
            "200_brands": assets_brands,
            "200_shape": assets_with_cursor,
            "401_no_auth": assets_no_auth,
        },
        notes=[
            "Use nextCursor for pagination — null means no more results",
            "Each resource has: public_id, secure_url, width, height, format, created_at, bytes",
            "Build display URL with transformations: https://res.cloudinary.com/<cloud>/image/upload/w_400,h_400,c_fill/<public_id>",
        ]
    ))

    # ── GET /api/upload/folders ───────────────────────────────────────────────
    folders_root = call("get", "/api/upload/folders", token=TOKEN)
    folders_sub = call("get", "/api/upload/folders", token=TOKEN, params={"parent": "brands"})
    folders_no_auth = call("get", "/api/upload/folders")

    save("upload", "folders", build_doc(
        endpoint="GET /api/upload/folders",
        description="List Cloudinary folders. Used to populate folder selector in media library.",
        request={
            "headers": {"Authorization": "Bearer <access-token>"},
            "query": {
                "parent": "(optional) parent folder path — omit for root folders",
            },
        },
        responses={
            "200_root": folders_root,
            "200_subfolders": folders_sub,
            "401_no_auth": folders_no_auth,
        },
        notes=[
            "Root folders: brands, products, categories, avatars, banners",
            "Each folder has: name, path, external_id",
        ]
    ))

    # ── Cloudinary upload flow (client-side) ──────────────────────────────────
    save("upload", "client-flow", {
        "title": "Cloudinary Upload Flow (Client-Side)",
        "description": "Complete flow for uploading an image from client directly to Cloudinary",
        "steps": [
            {
                "step": 1,
                "action": "Get upload signature from backend",
                "request": {
                    "method": "POST",
                    "url": "/api/upload/sign",
                    "headers": {"Authorization": "Bearer <access-token>"},
                    "body": {"folder": "brands"},
                },
                "response": {
                    "signature": "<sha1-hash>",
                    "timestamp": "1234567890",
                    "apiKey": "<cloudinary-api-key>",
                    "cloudName": "<cloud-name>",
                    "folder": "brands",
                }
            },
            {
                "step": 2,
                "action": "Upload file directly to Cloudinary",
                "request": {
                    "method": "POST",
                    "url": "https://api.cloudinary.com/v1_1/<cloudName>/image/upload",
                    "contentType": "multipart/form-data",
                    "fields": {
                        "file": "<binary file>",
                        "api_key": "<from step 1>",
                        "timestamp": "<from step 1>",
                        "signature": "<from step 1>",
                        "folder": "<from step 1>",
                    }
                },
                "response": {
                    "public_id": "brands/uuid-example",
                    "secure_url": "https://res.cloudinary.com/<cloud>/image/upload/brands/uuid-example.jpg",
                    "width": 800,
                    "height": 600,
                    "format": "jpg",
                }
            },
            {
                "step": 3,
                "action": "Save public_id or secure_url to backend",
                "note": "Store public_id (not secure_url) to allow dynamic transformations later",
                "example": {
                    "method": "POST",
                    "url": "/api/brands",
                    "body": {
                        "name": "Nike",
                        "slug": "nike",
                        "logo": "brands/uuid-example",
                    }
                }
            }
        ],
        "transformations": {
            "description": "Build display URLs with on-demand transformations",
            "base_url": "https://res.cloudinary.com/<cloudName>/image/upload/<transformation>/<public_id>",
            "examples": {
                "thumbnail_200x200": "w_200,h_200,c_fill,f_auto,q_auto",
                "product_800x800": "w_800,h_800,c_fill,f_auto,q_auto",
                "banner_1920x600": "w_1920,h_600,c_fill,f_auto,q_auto",
                "avatar_100x100": "w_100,h_100,c_thumb,g_face,f_auto,q_auto",
                "logo_pad_200x200": "w_200,h_200,c_pad,b_white,f_auto,q_auto",
            }
        }
    })


if __name__ == "__main__":
    print(f"Capturing upload docs from {BASE_URL}")
    if not TOKEN:
        print("  Tip: set ACCESS_TOKEN env var to capture authenticated responses")
        print("  export ACCESS_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \\")
        print("    -H 'Content-Type: application/json' -H 'X-Client-Type: mobile' \\")
        print("    -d '{\"email\":\"...\",\"password\":\"...\"}' | python -c \"import sys,json; print(json.load(sys.stdin)['data']['accessToken'])\")")
    capture_upload()
    print("\nDone.")
