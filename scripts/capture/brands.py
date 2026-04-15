import time
import requests
from .base import save, save_index, call, anon, build_doc


def capture(s: requests.Session):
    print("\n[brands]")
    slug = f"test-brand-{int(time.time())}"

    # list
    save("brands", "list", build_doc(
        endpoint="GET /api/brands",
        description="Danh sách brands với filter, search, sort, pagination.",
        request={
            "params": {
                "search": "string (optional)",
                "active": "boolean (optional) — true/false/bỏ qua=tất cả",
                "deleted": "boolean (optional) — true=đã xóa, false=chưa xóa, bỏ qua=tất cả",
                "page": "number (default: 0)",
                "size": "number (default: 20)",
                "sort": "field,direction — vd: name,asc | createdAt,desc",
            }
        },
        responses={
            "200_default": call(s, "get", "/api/brands"),
            "200_filtered": call(s, "get", "/api/brands",
                                 params={"active": "true", "deleted": "false",
                                         "sort": "name,asc", "size": "5"}),
        }
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
        description="Cập nhật brand. Tất cả fields đều optional.",
        request={"path": {"id": "number"},
                 "body": {"name": "string", "slug": "string", "logo": "string",
                          "description": "string", "active": "boolean"}},
        responses={
            "200_ok": call(s, "put", f"/api/brands/{brand_id}",
                           {"name": "Updated Brand", "active": False}),
            "404_not_found": call(s, "put", "/api/brands/999999", {"name": "x"}),
        }
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

    save_index("brands")
