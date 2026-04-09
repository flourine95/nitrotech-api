import time
import requests
from .base import save, save_index, call, anon, build_doc


def capture(s: requests.Session):
    print("\n[categories]")
    slug = f"test-cat-{int(time.time())}"

    # list
    save("categories", "list", build_doc(
        endpoint="GET /api/categories",
        description="Danh sách categories. Dùng tree=true để lấy dạng cây.",
        request={
            "params": {
                "search": "string (optional)",
                "active": "boolean (optional)",
                "deleted": "boolean (optional)",
                "parentId": "number (optional)",
                "tree": "boolean (default: false) — trả dạng cây, bỏ qua pagination",
                "page": "number (default: 0)",
                "size": "number (default: 20)",
                "sort": "field,direction",
            }
        },
        responses={
            "200_flat": call(s, "get", "/api/categories",
                             params={"active": "true", "deleted": "false"}),
            "200_tree": call(s, "get", "/api/categories", params={"tree": "true"}),
        }
    ))

    # create parent
    create_parent = call(s, "post", "/api/categories",
                         {"name": "Test Category", "slug": slug,
                          "description": "Test", "active": True})
    parent_id = create_parent["body"].get("data", {}).get("id")

    save("categories", "create", build_doc(
        endpoint="POST /api/categories",
        description="Tạo category. Slug unique trong active records. parentId optional.",
        request={"body": {"name": "string (required)", "slug": "string (required)",
                          "description": "string (optional)", "image": "string (optional)",
                          "parentId": "number (optional)", "active": "boolean (required)"}},
        responses={
            "201_created": create_parent,
            "409_slug_exists": call(s, "post", "/api/categories",
                                    {"name": "x", "slug": slug, "active": True}),
            "404_parent_not_found": call(s, "post", "/api/categories",
                                         {"name": "x", "slug": f"{slug}-x",
                                          "parentId": 999999, "active": True}),
        }
    ))

    if not parent_id:
        print("  [warn] could not get parent_id, skipping remaining")
        save_index("categories")
        return

    # create child
    child_slug = f"{slug}-child"
    create_child = call(s, "post", "/api/categories",
                        {"name": "Child Category", "slug": child_slug,
                         "parentId": parent_id, "active": True})
    child_id = create_child["body"].get("data", {}).get("id")

    # get
    save("categories", "get", build_doc(
        endpoint="GET /api/categories/{id}",
        description="Lấy chi tiết category theo id.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "get", f"/api/categories/{parent_id}"),
            "404_not_found": call(s, "get", "/api/categories/999999"),
        }
    ))

    # update
    save("categories", "update", build_doc(
        endpoint="PUT /api/categories/{id}",
        description="Cập nhật category. Validate circular reference khi đổi parentId.",
        request={"path": {"id": "number"},
                 "body": {"name": "string", "slug": "string", "description": "string",
                          "image": "string", "parentId": "number", "active": "boolean"}},
        responses={
            "200_ok": call(s, "put", f"/api/categories/{parent_id}",
                           {"name": "Updated Category"}),
            "409_circular_ref": call(s, "put", f"/api/categories/{parent_id}",
                                     {"parentId": child_id}) if child_id else {"skipped": True},
        }
    ))

    # delete parent có children → 409
    delete_has_children = call(s, "delete", f"/api/categories/{parent_id}")
    if child_id:
        call(s, "delete", f"/api/categories/{child_id}")
    delete_parent = call(s, "delete", f"/api/categories/{parent_id}")

    save("categories", "delete", build_doc(
        endpoint="DELETE /api/categories/{id}",
        description="Soft delete category. Block nếu còn active children.",
        request={"path": {"id": "number"}},
        responses={
            "409_has_children": delete_has_children,
            "200_ok": delete_parent,
            "404_not_found": call(s, "delete", "/api/categories/999999"),
        },
        notes=["Phải xóa hoặc move children trước khi xóa parent"]
    ))

    # restore
    save("categories", "restore", build_doc(
        endpoint="PATCH /api/categories/{id}/restore",
        description="Restore category đã soft delete. Trả 409 nếu slug conflict.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "patch", f"/api/categories/{parent_id}/restore"),
            "404_not_found": call(s, "patch", "/api/categories/999999/restore"),
        }
    ))

    # hard delete
    call(s, "delete", f"/api/categories/{parent_id}")
    save("categories", "hard-delete", build_doc(
        endpoint="DELETE /api/categories/{id}/permanent",
        description="Xóa vĩnh viễn. Chỉ cho phép với category đã soft deleted. Block nếu có products hoặc children.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "delete", f"/api/categories/{parent_id}/permanent"),
            "404_not_found": call(s, "delete", "/api/categories/999999/permanent"),
        },
        notes=["Phải soft delete trước",
               "Block nếu còn children (kể cả deleted)",
               "Block nếu có products liên kết"]
    ))

    save_index("categories")
