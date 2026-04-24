import time
import requests
from .base import save, save_index, call, build_doc


def capture(s: requests.Session, category_id: int = None, brand_id: int = None):
    print("\n[products]")
    ts = int(time.time())
    slug = f"test-product-{ts}"

    # Tạo category và brand tạm nếu không được truyền vào
    _cat_id = category_id
    _brand_id = brand_id
    _cleanup_cat = False
    _cleanup_brand = False

    if not _cat_id:
        r = call(s, "post", "/api/categories",
                 {"name": f"Tmp Cat {ts}", "slug": f"tmp-cat-{ts}", "active": True})
        _cat_id = r["body"].get("data", {}).get("id")
        _cleanup_cat = True

    if not _brand_id:
        r = call(s, "post", "/api/brands",
                 {"name": f"Tmp Brand {ts}", "slug": f"tmp-brand-{ts}", "active": True})
        _brand_id = r["body"].get("data", {}).get("id")
        _cleanup_brand = True

    # ── list ──────────────────────────────────────────────────────────────────
    save("products", "list", build_doc(
        endpoint="GET /api/products",
        description="Danh sách sản phẩm với filter, search, pagination. Trả priceMin/priceMax/variantCount thay vì full variants[].",
        request={
            "params": {
                "search": "string (optional)",
                "active": "boolean (optional)",
                "deleted": "boolean (optional)",
                "categoryId": "number (optional)",
                "brandId": "number (optional)",
                "page": "number (default: 0)",
                "size": "number (default: 20)",
                "sort": "field,direction — vd: createdAt,desc",
            }
        },
        responses={
            "200_default": call(s, "get", "/api/products"),
            "200_filtered": call(s, "get", "/api/products",
                                 params={"active": "true", "deleted": "false", "size": "5"}),
        },
        notes=[
            "variants luôn là null trong list — dùng GET /api/products/{id} để lấy full variants",
            "priceMin/priceMax tính từ active variants, null nếu chưa có variant nào",
            "variantCount chỉ đếm active variants",
        ]
    ))

    # ── create ────────────────────────────────────────────────────────────────
    create_ok = call(s, "post", "/api/products", {
        "categoryId": _cat_id,
        "brandId": _brand_id,
        "name": "Test Product",
        "slug": slug,
        "description": "A test product",
        "thumbnail": "https://example.com/thumb.jpg",
        "specs": {"weight": "1kg", "color": "black"},
        "active": True,
        "images": ["https://example.com/img1.jpg"],
        "variants": [
            {"sku": f"SKU-{ts}-A", "name": "Size S", "price": 100000,
             "attributes": {"size": "S"}, "active": True},
            {"sku": f"SKU-{ts}-B", "name": "Size M", "price": 120000,
             "attributes": {"size": "M"}, "active": True},
        ]
    })
    product_id = create_ok["body"].get("data", {}).get("id")
    variants = create_ok["body"].get("data", {}).get("variants", [])
    variant_id = variants[0].get("id") if variants else None

    save("products", "create", build_doc(
        endpoint="POST /api/products",
        description="Tạo sản phẩm kèm variants và images. Response trả full variants[]. Slug phải unique trong active records.",
        request={
            "body": {
                "categoryId": "number (required)",
                "brandId": "number (optional)",
                "name": "string (required)",
                "slug": "string (required, unique)",
                "description": "string (optional)",
                "thumbnail": "string (optional, url)",
                "specs": "object (optional) — JSON tự do cho thông số kỹ thuật",
                "active": "boolean (required)",
                "images": "string[] (optional)",
                "variants": [
                    {
                        "sku": "string (required, unique)",
                        "name": "string (required)",
                        "price": "number (required)",
                        "attributes": "object (optional) — vd: {size: S, color: red}",
                        "active": "boolean (required)"
                    }
                ]
            }
        },
        responses={
            "201_created": create_ok,
            "409_slug_exists": call(s, "post", "/api/products", {
                "categoryId": _cat_id, "name": "x", "slug": slug, "active": True
            }),
            "422_validation": call(s, "post", "/api/products", {"name": "", "active": True}),
        }
    ))

    if not product_id:
        print("  [warn] could not get product_id, skipping remaining")
        save_index("products")
        _do_cleanup(s, None, _cat_id if _cleanup_cat else None,
                    _brand_id if _cleanup_brand else None)
        return

    # ── get (detail — trả full variants) ─────────────────────────────────────
    save("products", "get", build_doc(
        endpoint="GET /api/products/{id}",
        description="Lấy chi tiết sản phẩm kèm full variants[] và images. priceMin/priceMax/variantCount cũng có.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "get", f"/api/products/{product_id}"),
            "404_not_found": call(s, "get", "/api/products/999999"),
        }
    ))

    # ── update ────────────────────────────────────────────────────────────────
    save("products", "update", build_doc(
        endpoint="PUT /api/products/{id}",
        description="Cập nhật thông tin sản phẩm. Không cập nhật variants qua endpoint này — dùng /variants.",
        request={
            "path": {"id": "number"},
            "body": {
                "categoryId": "number",
                "brandId": "number",
                "name": "string",
                "slug": "string",
                "description": "string",
                "thumbnail": "string",
                "specs": "object",
                "active": "boolean",
                "images": "string[] — thay thế toàn bộ gallery, null = giữ nguyên"
            }
        },
        responses={
            "200_ok": call(s, "put", f"/api/products/{product_id}",
                           {"name": "Updated Product", "active": False}),
            "404_not_found": call(s, "put", "/api/products/999999", {"name": "x"}),
        }
    ))

    # ── delete ────────────────────────────────────────────────────────────────
    save("products", "delete", build_doc(
        endpoint="DELETE /api/products/{id}",
        description="Soft delete sản phẩm. Có thể restore sau.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "delete", f"/api/products/{product_id}"),
            "404_not_found": call(s, "delete", "/api/products/999999"),
        }
    ))

    # ── restore ───────────────────────────────────────────────────────────────
    save("products", "restore", build_doc(
        endpoint="PATCH /api/products/{id}/restore",
        description="Restore sản phẩm đã soft delete. Trả 409 nếu slug conflict.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "patch", f"/api/products/{product_id}/restore"),
            "404_not_found": call(s, "patch", "/api/products/999999/restore"),
        }
    ))

    # ── hard delete ───────────────────────────────────────────────────────────
    call(s, "delete", f"/api/products/{product_id}")  # soft delete trước
    save("products", "hard-delete", build_doc(
        endpoint="DELETE /api/products/{id}/permanent",
        description="Xóa vĩnh viễn sản phẩm. Chỉ cho phép với sản phẩm đã soft deleted.",
        request={"path": {"id": "number"}},
        responses={
            "200_ok": call(s, "delete", f"/api/products/{product_id}/permanent"),
            "404_not_found": call(s, "delete", "/api/products/999999/permanent"),
        },
        notes=["Phải soft delete trước"]
    ))

    # ── variants ──────────────────────────────────────────────────────────────
    # Tạo product mới để test variants (product cũ đã bị hard delete)
    new_slug = f"{slug}-v2"
    create_v2 = call(s, "post", "/api/products", {
        "categoryId": _cat_id, "name": "Test Product V2",
        "slug": new_slug, "active": True,
        "variants": [
            {"sku": f"SKU-{ts}-V2-A", "name": "Size S", "price": 100000,
             "attributes": {"size": "S"}, "active": True},
        ]
    })
    product_v2_id = create_v2["body"].get("data", {}).get("id")
    variants_v2 = create_v2["body"].get("data", {}).get("variants", [])
    variant_v2_id = variants_v2[0].get("id") if variants_v2 else None

    if product_v2_id:
        create_variant = call(s, "post", f"/api/products/{product_v2_id}/variants", {
            "sku": f"SKU-{ts}-V2-B", "name": "Size M",
            "price": 120000, "attributes": {"size": "M"}, "active": True
        })
        new_variant_id = create_variant["body"].get("data", {}).get("id")

        save("products", "create-variant", build_doc(
            endpoint="POST /api/products/{productId}/variants",
            description="Thêm variant mới vào sản phẩm. SKU phải unique toàn hệ thống.",
            request={
                "path": {"productId": "number"},
                "body": {
                    "sku": "string (required, unique)",
                    "name": "string (required)",
                    "price": "number (required)",
                    "attributes": "object (optional)",
                    "active": "boolean (required)"
                }
            },
            responses={
                "201_created": create_variant,
                "409_sku_exists": call(s, "post", f"/api/products/{product_v2_id}/variants", {
                    "sku": f"SKU-{ts}-V2-A", "name": "Dup", "price": 100000, "active": True
                }),
                "404_product_not_found": call(s, "post", "/api/products/999999/variants", {
                    "sku": f"SKU-{ts}-Z", "name": "x", "price": 1, "active": True
                }),
            }
        ))

        if variant_v2_id:
            save("products", "update-variant", build_doc(
                endpoint="PUT /api/products/{productId}/variants/{variantId}",
                description="Cập nhật variant. SKU phải unique nếu thay đổi.",
                request={
                    "path": {"productId": "number", "variantId": "number"},
                    "body": {
                        "sku": "string",
                        "name": "string",
                        "price": "number",
                        "attributes": "object",
                        "active": "boolean"
                    }
                },
                responses={
                    "200_ok": call(s, "put",
                                   f"/api/products/{product_v2_id}/variants/{variant_v2_id}",
                                   {"price": 110000, "active": False}),
                    "404_not_found": call(s, "put",
                                          f"/api/products/{product_v2_id}/variants/999999",
                                          {"price": 1}),
                }
            ))

            save("products", "delete-variant", build_doc(
                endpoint="DELETE /api/products/{productId}/variants/{variantId}",
                description="Xóa variant khỏi sản phẩm.",
                request={"path": {"productId": "number", "variantId": "number"}},
                responses={
                    "200_ok": call(s, "delete",
                                   f"/api/products/{product_v2_id}/variants/{variant_v2_id}"),
                    "404_not_found": call(s, "delete",
                                          f"/api/products/{product_v2_id}/variants/999999"),
                }
            ))

        # cleanup v2
        call(s, "delete", f"/api/products/{product_v2_id}")
        call(s, "delete", f"/api/products/{product_v2_id}/permanent")

    save_index("products")

    # cleanup category/brand tạm
    _do_cleanup(s, None, _cat_id if _cleanup_cat else None,
                _brand_id if _cleanup_brand else None)


def _do_cleanup(s, product_id, cat_id, brand_id):
    if cat_id:
        call(s, "delete", f"/api/categories/{cat_id}")
        call(s, "delete", f"/api/categories/{cat_id}/permanent")
    if brand_id:
        call(s, "delete", f"/api/brands/{brand_id}")
        call(s, "delete", f"/api/brands/{brand_id}/permanent")
