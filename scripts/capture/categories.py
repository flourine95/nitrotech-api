import time
import requests
from .base import save, save_index, call, anon, build_doc


def capture(s: requests.Session):
    print("\n[categories]")
    slug = f"test-cat-{int(time.time())}"

    # list - tree with facets
    save("categories", "list", build_doc(
        endpoint="GET /api/categories",
        description="Danh sách categories dạng tree + facets. Dùng deleted=true để lấy deleted list.",
        request={
            "params": {
                "deleted": "boolean (optional) — true: trả deleted list, false/null: trả tree + facets",
            }
        },
        responses={
            "200_tree_with_facets": call(s, "get", "/api/categories"),
            "200_deleted_list": call(s, "get", "/api/categories", params={"deleted": "true"}),
        },
        notes=[
            "Response tree có facets: {data: [...], facets: {active, inactive, deleted, root, withChildren}}",
            "Response deleted list: {data: [...]} - không có facets",
            "path field KHÔNG có trong response (chỉ có trong GET single category)",
            "productCount: số products trong category (chỉ đếm active products)",
            "childrenCount: số children trực tiếp",
            "Performance: 2 queries (1 tree + 1 product counts), không có N+1 query"
        ]
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
        description="Lấy chi tiết category theo id. Response có breadcrumb path và productCount.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "get", f"/api/categories/{parent_id}"),
            "404_not_found": call(s, "get", "/api/categories/999999"),
        },
        notes=[
            "Response có field path: [{id, name, slug, active}] - breadcrumb từ root đến parent",
            "path = [] nếu category ở root (không có parent)",
            "path field CHỈ có trong GET single category, KHÔNG có trong list/tree",
            "productCount: số products trong category (chỉ đếm active products)",
            "Dùng productCount để validate delete: không cho delete nếu productCount > 0"
        ]
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
        description="Soft delete category. Block nếu còn active children hoặc có products.",
        request={"path": {"id": "number"}},
        responses={
            "409_has_children": delete_has_children,
            "200_ok": delete_parent,
            "404_not_found": call(s, "delete", "/api/categories/999999"),
        },
        notes=[
            "Phải xóa hoặc move children trước khi xóa parent",
            "Block nếu category có products (productCount > 0)",
            "Dùng GET /{id} để check productCount trước khi delete"
        ]
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
        notes=[
            "Phải soft delete trước",
            "Block nếu còn children (kể cả deleted)",
            "Block nếu có products liên kết (productCount > 0)"
        ]
    ))

    # Tạo categories để test bulk operations và move
    s1 = call(s, "post", "/api/categories", {"name": "Sort A", "slug": f"{slug}-s1", "active": True})
    s2 = call(s, "post", "/api/categories", {"name": "Sort B", "slug": f"{slug}-s2", "active": False})
    s3 = call(s, "post", "/api/categories", {"name": "Sort C", "slug": f"{slug}-s3", "active": True})
    s1_id = s1["body"].get("data", {}).get("id")
    s2_id = s2["body"].get("data", {}).get("id")
    s3_id = s3["body"].get("data", {}).get("id")

    # bulk-delete
    save("categories", "bulk-delete", build_doc(
        endpoint="DELETE /api/categories/bulk",
        description="Bulk soft delete categories. Chỉ delete categories không có children.",
        request={"body": {"ids": "number[] (required)"}},
        responses={
            "200_partial_success": call(s, "delete", "/api/categories/bulk",
                                        {"ids": [s1_id, s2_id]}) if s1_id and s2_id else {"skipped": True},
            "404_not_found": call(s, "delete", "/api/categories/bulk", {"ids": [999999]}),
        },
        notes=[
            "Response: {succeeded, failed, failedIds, failedReasons}",
            "failedReasons: {'3': 'Has children'}"
        ]
    ))

    # bulk-restore
    save("categories", "bulk-restore", build_doc(
        endpoint="PATCH /api/categories/bulk/restore",
        description="Bulk restore deleted categories.",
        request={"body": {"ids": "number[] (required)"}},
        responses={
            "200_ok": call(s, "patch", "/api/categories/bulk/restore",
                          {"ids": [s1_id, s2_id]}) if s1_id and s2_id else {"skipped": True},
        }
    ))

    # bulk-hard-delete
    call(s, "delete", "/api/categories/bulk", {"ids": [s1_id, s2_id]})
    save("categories", "bulk-hard-delete", build_doc(
        endpoint="DELETE /api/categories/bulk/permanent",
        description="Bulk hard delete. Chỉ delete categories đã soft deleted và không có children.",
        request={"body": {"ids": "number[] (required)"}},
        responses={
            "200_ok": call(s, "delete", "/api/categories/bulk/permanent",
                          {"ids": [s1_id, s2_id]}) if s1_id and s2_id else {"skipped": True},
        }
    ))

    # Tạo lại categories cho bulk activate/deactivate
    s1 = call(s, "post", "/api/categories", {"name": "Test A", "slug": f"{slug}-t1", "active": False})
    s2 = call(s, "post", "/api/categories", {"name": "Test B", "slug": f"{slug}-t2", "active": False})
    s1_id = s1["body"].get("data", {}).get("id")
    s2_id = s2["body"].get("data", {}).get("id")

    # bulk-activate
    save("categories", "bulk-activate", build_doc(
        endpoint="PATCH /api/categories/bulk/activate",
        description="Bulk activate categories. Set active=true cho tất cả ids.",
        request={"body": {"ids": "number[] (required)"}},
        responses={
            "200_ok": call(s, "patch", "/api/categories/bulk/activate",
                          {"ids": [s1_id, s2_id]}) if s1_id and s2_id else {"skipped": True},
        }
    ))

    # bulk-deactivate
    save("categories", "bulk-deactivate", build_doc(
        endpoint="PATCH /api/categories/bulk/deactivate",
        description="Bulk deactivate categories. Set active=false cho tất cả ids.",
        request={"body": {"ids": "number[] (required)"}},
        responses={
            "200_ok": call(s, "patch", "/api/categories/bulk/deactivate",
                          {"ids": [s1_id, s2_id]}) if s1_id and s2_id else {"skipped": True},
        }
    ))

    # validate-delete
    # Tạo parent với children để test
    vp = call(s, "post", "/api/categories", {"name": "Validate Parent", "slug": f"{slug}-vp", "active": True})
    vp_id = vp["body"].get("data", {}).get("id")
    vc = call(s, "post", "/api/categories", {"name": "Validate Child", "slug": f"{slug}-vc", 
                                             "parentId": vp_id, "active": True}) if vp_id else None
    vc_id = vc["body"].get("data", {}).get("id") if vc else None

    save("categories", "validate-delete", build_doc(
        endpoint="POST /api/categories/bulk/validate-delete",
        description="Validate trước khi delete. Check categories nào có thể delete, nào không.",
        request={"body": {"ids": "number[] (required)"}},
        responses={
            "200_with_blocked": call(s, "post", "/api/categories/bulk/validate-delete",
                                    {"ids": [vp_id, s1_id]}) if vp_id and s1_id else {"skipped": True},
        },
        notes=[
            "Response: {canDelete: [], cannotDelete: [], reasons: {'3': 'Has 5 children and 12 products'}}",
            "Frontend dùng để show warning trước khi delete",
            "Reasons bao gồm: has children, has products, hoặc cả hai"
        ]
    ))

    # move-up
    save("categories", "move-up", build_doc(
        endpoint="PATCH /api/categories/{id}/move-up",
        description="Move category lên 1 vị trí (swap với sibling trước đó).",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "patch", f"/api/categories/{s2_id}/move-up") if s2_id else {"skipped": True},
            "409_already_first": call(s, "patch", f"/api/categories/{s1_id}/move-up") if s1_id else {"skipped": True},
        },
        notes=["Chỉ swap trong cùng parent", "Throw ALREADY_FIRST nếu đã ở đầu tiên"]
    ))

    # move-down
    save("categories", "move-down", build_doc(
        endpoint="PATCH /api/categories/{id}/move-down",
        description="Move category xuống 1 vị trí (swap với sibling sau đó).",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "patch", f"/api/categories/{s1_id}/move-down") if s1_id else {"skipped": True},
            "409_already_last": call(s, "patch", f"/api/categories/{s2_id}/move-down") if s2_id else {"skipped": True},
        },
        notes=["Chỉ swap trong cùng parent", "Throw ALREADY_LAST nếu đã ở cuối cùng"]
    ))

    # move (flexible)
    save("categories", "move", build_doc(
        endpoint="PATCH /api/categories/{id}/move",
        description="Flexible move: change parent, reorder, hoặc cả hai. Support drag & drop.",
        request={
            "path": {"id": "number"},
            "body": {
                "newParentId": "number | null (optional) — null: move to root, không truyền: giữ nguyên parent",
                "afterId": "number | null (optional) — null: đặt đầu tiên, không truyền: đặt cuối cùng"
            }
        },
        responses={
            "200_change_parent": call(s, "patch", f"/api/categories/{s2_id}/move",
                                     {"newParentId": s1_id}) if s1_id and s2_id else {"skipped": True},
            "200_move_to_root": call(s, "patch", f"/api/categories/{s2_id}/move",
                                    {"newParentId": None}) if s2_id else {"skipped": True},
            "200_reorder": call(s, "patch", f"/api/categories/{s2_id}/move",
                               {"afterId": s1_id}) if s1_id and s2_id else {"skipped": True},
            "409_circular_ref": call(s, "patch", f"/api/categories/{s1_id}/move",
                                    {"newParentId": s2_id}) if s1_id and s2_id else {"skipped": True},
        },
        notes=[
            "Use cases:",
            "- Move into parent: {newParentId: X}",
            "- Move to root: {newParentId: null}",
            "- Reorder (same parent): {afterId: Y}",
            "- Drag & drop: {newParentId: X, afterId: Y}",
            "Validate circular reference khi change parent"
        ]
    ))

    # toggle
    save("categories", "toggle", build_doc(
        endpoint="PATCH /api/categories/{id}/toggle",
        description="Toggle active status của category (active ↔ inactive).",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "patch", f"/api/categories/{s1_id}/toggle") if s1_id else {"skipped": True},
            "404_not_found": call(s, "patch", "/api/categories/999999/toggle"),
        },
        notes=["Đơn giản hơn update khi chỉ cần toggle active"]
    ))

    # cleanup
    for sid in [s1_id, s2_id, s3_id, vp_id, vc_id]:
        if sid:
            call(s, "delete", f"/api/categories/{sid}")
            call(s, "delete", f"/api/categories/{sid}/permanent")

    save_index("categories")
