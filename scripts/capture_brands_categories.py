"""
API Documentation Capture — Brands & Categories
Chạy: python scripts/capture_brands_categories.py
Yêu cầu: server đang chạy tại API_BASE_URL (default: http://localhost:8080)
"""

import requests
import json
import os
import time

BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")
DOCS_DIR = os.path.join(os.path.dirname(__file__), "..", ".docs", "api")


def save(module: str, name: str, doc: dict):
    folder = os.path.join(DOCS_DIR, module)
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, f"{name}.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(doc, f, indent=2, ensure_ascii=False)
    print(f"  saved → {path}")


def call(method: str, path: str, body: dict = None, token: str = None) -> dict:
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    resp = getattr(requests, method)(url, json=body, headers=headers)
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


def unique_slug(prefix: str) -> str:
    return f"{prefix}-{int(time.time())}"


# ── brands ────────────────────────────────────────────────────────────────────

def capture_brands():
    print("\n[brands]")
    slug = unique_slug("test-brand")

    # POST — success
    body = {"name": "Test Brand", "slug": slug, "logo": "https://example.com/logo.png",
            "description": "A test brand", "active": True}
    created = call("post", "/api/brands", body)
    brand_id = created["body"].get("data", {}).get("id")

    # POST — duplicate slug
    duplicate = call("post", "/api/brands", body)

    # POST — validation error
    invalid = call("post", "/api/brands", {"name": "", "slug": "INVALID SLUG!!"})

    save("brands", "create", build_doc(
        endpoint="POST /api/brands",
        description="Create a new brand. Slug must be unique and lowercase-hyphenated.",
        request={"body": body},
        responses={
            "201": created,
            "409_slug_exists": duplicate,
            "422_validation": invalid,
        }
    ))

    # GET list
    list_all = call("get", "/api/brands")
    list_active = call("get", "/api/brands?active=true")
    list_inactive = call("get", "/api/brands?active=false")

    save("brands", "list", build_doc(
        endpoint="GET /api/brands",
        description="List all brands. Filter by active status. Returns all records (no pagination — typically small dataset used for filters/dropdowns).",
        request={"query": {"active": "true | false | (omit for all)"}},
        responses={
            "200_all": list_all,
            "200_active_only": list_active,
            "200_inactive_only": list_inactive,
        },
        notes=["No pagination — brands are loaded once and cached client-side for filter/dropdown use"]
    ))

    if brand_id:
        # GET by id
        get_one = call("get", f"/api/brands/{brand_id}")
        get_not_found = call("get", "/api/brands/999999")

        save("brands", "get", build_doc(
            endpoint="GET /api/brands/{id}",
            description="Get a single brand by ID.",
            request={"path": {"id": brand_id}},
            responses={
                "200": get_one,
                "404_not_found": get_not_found,
            }
        ))

        # PUT — update
        update_body = {"name": "Updated Brand", "active": False}
        updated = call("put", f"/api/brands/{brand_id}", update_body)
        update_invalid = call("put", f"/api/brands/{brand_id}", {"slug": "INVALID!!"})

        save("brands", "update", build_doc(
            endpoint="PUT /api/brands/{id}",
            description="Update a brand. All fields are optional (partial update).",
            request={"path": {"id": brand_id}, "body": update_body},
            responses={
                "200": updated,
                "404_not_found": call("put", "/api/brands/999999", update_body),
                "422_validation": update_invalid,
            }
        ))

        # DELETE
        deleted = call("delete", f"/api/brands/{brand_id}")
        delete_not_found = call("delete", f"/api/brands/{brand_id}")  # already deleted

        save("brands", "delete", build_doc(
            endpoint="DELETE /api/brands/{id}",
            description="Soft delete a brand. Deleted brands are excluded from all queries.",
            request={"path": {"id": brand_id}},
            responses={
                "200": deleted,
                "404_not_found": delete_not_found,
            }
        ))
    else:
        print("  [warn] brand_id not found, skipping get/update/delete")


# ── categories ────────────────────────────────────────────────────────────────

def capture_categories():
    print("\n[categories]")
    slug_parent = unique_slug("parent-cat")
    slug_child = unique_slug("child-cat")

    # POST parent
    parent_body = {"name": "Parent Category", "slug": slug_parent,
                   "description": "Top level category", "active": True}
    parent_created = call("post", "/api/categories", parent_body)
    parent_id = parent_created["body"].get("data", {}).get("id")

    # POST — duplicate slug
    duplicate = call("post", "/api/categories", parent_body)

    # POST — validation error
    invalid = call("post", "/api/categories", {"name": "", "slug": "INVALID SLUG!!"})

    save("categories", "create", build_doc(
        endpoint="POST /api/categories",
        description="Create a new category. Supports tree structure via parentId.",
        request={"body": parent_body},
        responses={
            "201": parent_created,
            "409_slug_exists": duplicate,
            "422_validation": invalid,
        }
    ))

    child_id = None
    if parent_id:
        # POST child
        child_body = {"name": "Child Category", "slug": slug_child,
                      "description": "Sub category", "parentId": parent_id, "active": True}
        child_created = call("post", "/api/categories", child_body)
        child_id = child_created["body"].get("data", {}).get("id")

        # POST — circular reference
        if child_id:
            circular = call("post", "/api/categories",
                            {"name": "Circular", "slug": unique_slug("circular"),
                             "parentId": child_id, "active": True})
            # Attempt to set parent's parent to its own child (circular)
            circular_update = call("put", f"/api/categories/{parent_id}",
                                   {"parentId": child_id})

            save("categories", "create-child", build_doc(
                endpoint="POST /api/categories (with parentId)",
                description="Create a child category under a parent.",
                request={"body": child_body},
                responses={
                    "201": child_created,
                    "409_circular_ref": circular_update,
                    "404_parent_not_found": call("post", "/api/categories",
                                                  {"name": "X", "slug": unique_slug("x"),
                                                   "parentId": 999999, "active": True}),
                }
            ))

    # GET list — flat
    list_all = call("get", "/api/categories")
    list_active = call("get", "/api/categories?active=true")
    list_by_parent = call("get", f"/api/categories?parentId={parent_id}") if parent_id else None

    save("categories", "list", build_doc(
        endpoint="GET /api/categories",
        description="List categories flat. Filter by active and parentId. No pagination — used for menus and filters.",
        request={"query": {
            "active": "true | false | (omit for all)",
            "parentId": "filter by parent (omit for all)",
            "tree": "false (default)",
        }},
        responses={
            "200_all": list_all,
            "200_active": list_active,
            "200_by_parent": list_by_parent or {"note": "requires valid parentId"},
        },
        notes=["No pagination — categories are small datasets used for navigation menus"]
    ))

    # GET tree
    tree = call("get", "/api/categories?tree=true")
    tree_active = call("get", "/api/categories?tree=true&active=true")

    save("categories", "list-tree", build_doc(
        endpoint="GET /api/categories?tree=true",
        description="List categories as nested tree. Children are embedded under their parent.",
        request={"query": {"tree": "true", "active": "true | false | (omit for all)"}},
        responses={
            "200_full_tree": tree,
            "200_active_tree": tree_active,
        },
        notes=["Use tree=true for rendering navigation menus with nested structure"]
    ))

    if parent_id:
        # GET by id
        get_one = call("get", f"/api/categories/{parent_id}")
        get_not_found = call("get", "/api/categories/999999")

        save("categories", "get", build_doc(
            endpoint="GET /api/categories/{id}",
            description="Get a single category by ID. Includes parentName if has parent.",
            request={"path": {"id": parent_id}},
            responses={
                "200": get_one,
                "404_not_found": get_not_found,
            }
        ))

        # PUT — update
        update_body = {"name": "Updated Category", "active": False}
        updated = call("put", f"/api/categories/{parent_id}", update_body)

        save("categories", "update", build_doc(
            endpoint="PUT /api/categories/{id}",
            description="Update a category. All fields optional. Validates circular reference when changing parentId.",
            request={"path": {"id": parent_id}, "body": update_body},
            responses={
                "200": updated,
                "404_not_found": call("put", "/api/categories/999999", update_body),
                "409_circular_ref": {"status": 409, "body": {
                    "status": 409, "code": "CATEGORY_CIRCULAR_REF",
                    "message": "Circular reference detected"
                }},
            }
        ))

        # DELETE child first, then parent
        if child_id:
            call("delete", f"/api/categories/{child_id}")

        deleted = call("delete", f"/api/categories/{parent_id}")
        delete_not_found = call("delete", f"/api/categories/{parent_id}")

        save("categories", "delete", build_doc(
            endpoint="DELETE /api/categories/{id}",
            description="Soft delete a category. Children's parentId is set to NULL (ON DELETE SET NULL).",
            request={"path": {"id": parent_id}},
            responses={
                "200": deleted,
                "404_not_found": delete_not_found,
            }
        ))


# ── main ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print(f"Capturing brands & categories docs from {BASE_URL}")
    capture_brands()
    capture_categories()
    print("\nDone.")
