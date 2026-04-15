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

    # move — gộp cả reorder và move cross-parent
    # Tạo 3 categories để test
    s1 = call(s, "post", "/api/categories", {"name": "Sort A", "slug": f"{slug}-s1", "active": True})
    s2 = call(s, "post", "/api/categories", {"name": "Sort B", "slug": f"{slug}-s2", "active": True})
    s3 = call(s, "post", "/api/categories", {"name": "Sort C", "slug": f"{slug}-s3", "active": True})
    s1_id = s1["body"].get("data", {}).get("id")
    s2_id = s2["body"].get("data", {}).get("id")
    s3_id = s3["body"].get("data", {}).get("id")

    # Reorder trong cùng root (fromParentId = toParentId = null)
    reorder_same = call(s, "patch", "/api/categories/move", {
        "movedId": s3_id, "fromParentId": None, "toParentId": None,
        "targetOrderedIds": [s3_id, s1_id, s2_id]
    }) if s1_id and s2_id and s3_id else {"skipped": True}

    # Move s2 vào s1 (cross-parent)
    move_cross = call(s, "patch", "/api/categories/move", {
        "movedId": s2_id, "fromParentId": None, "toParentId": s1_id,
        "sourceOrderedIds": [s3_id, s1_id],
        "targetOrderedIds": [s2_id]
    }) if s1_id and s2_id and s3_id else {"skipped": True}

    # Move s2 về root
    move_to_root = call(s, "patch", "/api/categories/move", {
        "movedId": s2_id, "fromParentId": s1_id, "toParentId": None,
        "sourceOrderedIds": [],
        "targetOrderedIds": [s3_id, s1_id, s2_id]
    }) if s1_id and s2_id else {"skipped": True}

    # Circular ref
    move_circular = call(s, "patch", "/api/categories/move", {
        "movedId": s1_id, "fromParentId": None, "toParentId": s2_id,
        "targetOrderedIds": [s1_id]
    }) if s1_id and s2_id else {"skipped": True}

    save("categories", "move", build_doc(
        endpoint="PATCH /api/categories/move",
        description="Di chuyển và/hoặc sắp xếp lại categories. Xử lý cả reorder trong cùng parent lẫn move cross-parent trong 1 request duy nhất.",
        request={
            "body": {
                "movedId": "number (required) — id của category được kéo",
                "fromParentId": "number | null — parent cũ (null = từ root)",
                "toParentId": "number | null — parent mới (null = move lên root)",
                "targetOrderedIds": "number[] (required) — thứ tự mới của tất cả siblings ở parent mới",
                "sourceOrderedIds": "number[] (optional) — thứ tự mới của siblings còn lại ở parent cũ (chỉ cần khi cross-parent)"
            }
        },
        responses={
            "200_reorder_same_parent": reorder_same,
            "200_move_cross_parent": move_cross,
            "200_move_to_root": move_to_root,
            "409_circular_ref": move_circular,
            "404_not_found": call(s, "patch", "/api/categories/move", {
                "movedId": 999999, "toParentId": None, "targetOrderedIds": [999999]
            }),
        },
        notes=[
            "Reorder trong cùng parent: fromParentId = toParentId, không cần sourceOrderedIds",
            "Move cross-parent: cần sourceOrderedIds để reindex siblings ở parent cũ",
            "targetOrderedIds phải chứa toàn bộ siblings của parent mới sau khi drop",
            "Frontend nên dùng optimistic update: đổi state local khi drag, gọi API khi drop"
        ]
    ))

    # cleanup
    for sid in [s1_id, s2_id, s3_id]:
        if sid:
            call(s, "delete", f"/api/categories/{sid}")

    save_index("categories")
