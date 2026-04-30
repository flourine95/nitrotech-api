import time
import requests
from .base import save, save_index, call, anon, build_doc


def capture(s: requests.Session):
    print("\n[brands]")
    slug = f"test-brand-{int(time.time())}"

    # list
    save("brands", "list", build_doc(
        endpoint="GET /api/brands",
        description="Danh sách brands với filter, search, sort, pagination. Hỗ trợ multiple sort.",
        request={
            "params": {
                "search": "string (optional)",
                "active": "boolean (optional) — true/false/bỏ qua=tất cả",
                "deleted": "boolean (optional) — true=đã xóa, false=chưa xóa, không truyền=chưa xóa (mặc định). QUAN TRỌNG: Không truyền param này sẽ CHỈ trả brands chưa xóa (deletedAt IS NULL), không phải tất cả.",
                "page": "number (default: 0)",
                "size": "number (default: 20)",
                "sort": "string (optional, repeatable) — format: field,direction (asc|desc). Ví dụ: sort=name,asc hoặc sort=name,asc&sort=createdAt,desc cho multiple sort. Sortable fields: id, name, slug, active, createdAt, updatedAt",
            }
        },
        responses={
            "200_default": call(s, "get", "/api/brands"),
            "200_filtered": call(s, "get", "/api/brands",
                                 params={"active": "true", "deleted": "false",
                                         "sort": "name,asc", "size": "5"}),
            "200_multiple_sort": call(s, "get", "/api/brands",
                                     params={"sort": ["name,asc", "createdAt,desc"], "size": "5"}),
            "200_deleted_only": call(s, "get", "/api/brands",
                                    params={"deleted": "true", "size": "5"}),
        },
        notes=[
            "Multiple sort: Truyền param 'sort' nhiều lần, ví dụ: ?sort=name,asc&sort=createdAt,desc",
            "Default deleted filter: GET /api/brands (không truyền deleted) → CHỈ trả brands chưa xóa (deletedAt IS NULL). Đây là behavior đã được fix để tránh brands đã xóa lẫn vào list mặc định.",
            "Để lấy brands đã xóa (thùng rác): GET /api/brands?deleted=true",
            "Facets trong response: {active: count, inactive: count, deleted: count} — dùng để hiển thị badge số lượng"
        ]
    ))

    # create
    create_ok = call(s, "post", "/api/brands",
                     {"name": "Test Brand", "slug": slug,
                      "logo": "https://example.com/logo.png",
                      "description": "A test brand", "active": True})
    brand_id = create_ok["body"].get("data", {}).get("id")

    save("brands", "create", build_doc(
        endpoint="POST /api/brands",
        description="Tạo brand mới. Slug phải unique trong active records.",
        request={"body": {"name": "string (required)", "slug": "string (required, unique)",
                          "logo": "string (optional, url)", "description": "string (optional)",
                          "active": "boolean (required)"}},
        responses={
            "201_created": create_ok,
            "409_slug_exists": call(s, "post", "/api/brands",
                                    {"name": "Test Brand", "slug": slug, "active": True}),
            "422_validation": call(s, "post", "/api/brands", {"name": "", "active": True}),
        }
    ))

    if not brand_id:
        print("  [warn] could not get brand_id, skipping remaining")
        save_index("brands")
        return

    # get
    save("brands", "get", build_doc(
        endpoint="GET /api/brands/{id}",
        description="Lấy chi tiết brand theo id.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "get", f"/api/brands/{brand_id}"),
            "404_not_found": call(s, "get", "/api/brands/999999"),
        }
    ))

    # update
    save("brands", "update", build_doc(
        endpoint="PUT /api/brands/{id}",
        description="Cập nhật brand. Hỗ trợ partial update - chỉ update fields được truyền.",
        request={"path": {"id": "number"},
                 "body": {"name": "string (optional)", "slug": "string (optional)", "logo": "string (optional)",
                          "description": "string (optional)", "active": "boolean (optional)"}},
        responses={
            "200_ok": call(s, "put", f"/api/brands/{brand_id}",
                           {"name": "Updated Brand", "active": False}),
            "404_not_found": call(s, "put", "/api/brands/999999", {"name": "x"}),
        },
        notes=[
            "Partial update: Fields không truyền → giữ nguyên giá trị cũ",
            "Fields truyền giá trị → update thành giá trị mới",
            "Fields truyền null → bị IGNORE (giữ nguyên giá trị cũ)",
            "KHÔNG HỖ TRỢ clear fields về null. Workaround: dùng empty string \"\" cho text fields (logo, description)"
        ]
    ))

    # delete
    save("brands", "delete", build_doc(
        endpoint="DELETE /api/brands/{id}",
        description="Soft delete brand. Brand vẫn còn trong DB, có thể restore.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "delete", f"/api/brands/{brand_id}"),
            "404_not_found": call(s, "delete", "/api/brands/999999"),
        }
    ))

    # restore
    save("brands", "restore", build_doc(
        endpoint="PATCH /api/brands/{id}/restore",
        description="Restore brand đã soft delete. Trả 409 nếu slug đang bị dùng bởi brand khác.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "patch", f"/api/brands/{brand_id}/restore"),
            "404_not_found": call(s, "patch", "/api/brands/999999/restore"),
        }
    ))

    # hard delete
    call(s, "delete", f"/api/brands/{brand_id}")
    save("brands", "hard-delete", build_doc(
        endpoint="DELETE /api/brands/{id}/permanent",
        description="Xóa vĩnh viễn brand. Chỉ cho phép với brand đã soft deleted. Block nếu có products.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "delete", f"/api/brands/{brand_id}/permanent"),
            "404_not_found": call(s, "delete", "/api/brands/999999/permanent"),
        },
        notes=["Phải soft delete trước",
               "Block nếu brand có products liên kết"]
    ))

    # Tạo thêm brands cho bulk operations
    slug2 = f"test-brand-bulk-{int(time.time())}-2"
    slug3 = f"test-brand-bulk-{int(time.time())}-3"
    slug4 = f"test-brand-bulk-{int(time.time())}-4"
    
    brand2_resp = call(s, "post", "/api/brands",
                       {"name": "Bulk Test 2", "slug": slug2, "active": True})
    brand3_resp = call(s, "post", "/api/brands",
                       {"name": "Bulk Test 3", "slug": slug3, "active": True})
    brand4_resp = call(s, "post", "/api/brands",
                       {"name": "Bulk Test 4", "slug": slug4, "active": True})
    
    brand2_id = brand2_resp["body"].get("data", {}).get("id")
    brand3_id = brand3_resp["body"].get("data", {}).get("id")
    brand4_id = brand4_resp["body"].get("data", {}).get("id")

    # bulk delete
    bulk_delete_ok = call(s, "delete", "/api/brands/bulk",
                          {"ids": [brand2_id, brand3_id]})
    
    save("brands", "bulk-delete", build_doc(
        endpoint="DELETE /api/brands/bulk",
        description="Soft delete nhiều brands cùng lúc. Max 100 ids.",
        request={"body": {"ids": "array<number> (required, max 100)"}},
        responses={
            "200_all_success": bulk_delete_ok,
            "200_partial_success": call(s, "delete", "/api/brands/bulk",
                                        {"ids": [brand4_id, 999998, 999999]}),
            "422_validation": call(s, "delete", "/api/brands/bulk", {"ids": []}),
        },
        notes=["Response có failedReasons: Map<id, reason> cho từng id thất bại",
               "Partial success: succeeded > 0 && failed > 0"]
    ))

    # bulk restore
    bulk_restore_ok = call(s, "patch", "/api/brands/bulk/restore",
                           {"ids": [brand2_id, brand3_id]})
    
    save("brands", "bulk-restore", build_doc(
        endpoint="PATCH /api/brands/bulk/restore",
        description="Restore nhiều brands đã soft delete. Max 100 ids.",
        request={"body": {"ids": "array<number> (required, max 100)"}},
        responses={
            "200_all_success": bulk_restore_ok,
            "200_partial_success": call(s, "patch", "/api/brands/bulk/restore",
                                        {"ids": [brand4_id, 999998, 999999]}),
            "422_validation": call(s, "patch", "/api/brands/bulk/restore", {"ids": []}),
        },
        notes=["Response có failedReasons: Map<id, reason> cho từng id thất bại",
               "Trả 409 nếu slug conflict khi restore",
               "Partial success: succeeded > 0 && failed > 0"]
    ))

    # bulk hard delete - soft delete trước
    call(s, "delete", "/api/brands/bulk", {"ids": [brand2_id, brand3_id, brand4_id]})
    
    bulk_hard_delete_ok = call(s, "delete", "/api/brands/bulk/permanent",
                               {"ids": [brand2_id, brand3_id]})
    
    save("brands", "bulk-hard-delete", build_doc(
        endpoint="DELETE /api/brands/bulk/permanent",
        description="Xóa vĩnh viễn nhiều brands. Chỉ cho phép với brands đã soft deleted. Block nếu có products.",
        request={"body": {"ids": "array<number> (required, max 100)"}},
        responses={
            "200_all_success": bulk_hard_delete_ok,
            "200_partial_success": call(s, "delete", "/api/brands/bulk/permanent",
                                        {"ids": [brand4_id, 999998, 999999]}),
            "422_validation": call(s, "delete", "/api/brands/bulk/permanent", {"ids": []}),
        },
        notes=["Phải soft delete trước",
               "Block nếu brand có products liên kết (chỉ đếm products chưa xóa)",
               "Response có failedReasons: Map<id, reason> cho từng id thất bại",
               "Reasons: 'Brand still has active products' | 'Brand not found or not deleted'",
               "Partial success: succeeded > 0 && failed > 0"]
    ))

    save_index("brands")
