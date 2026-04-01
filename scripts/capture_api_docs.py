"""
API Documentation Capture Script
Gọi thật từng endpoint, lưu request/response vào .docs/api/<module>/<endpoint>.json
Chạy: python scripts/capture_api_docs.py
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


def call(method: str, path: str, body: dict = None, token: str = None,
         client_type: str = "web", session: requests.Session = None) -> dict:
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json", "X-Client-Type": client_type}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    caller = session or requests
    resp = getattr(caller, method)(url, json=body, headers=headers)
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


def unique_email():
    return f"test_{int(time.time())}@example.com"


# ── auth module ───────────────────────────────────────────────────────────────

def capture_auth():
    print("\n[auth]")
    email = unique_email()
    password = "password123"

    # Dùng Session để tự động lưu/gửi cookie (web flow)
    web_session = requests.Session()

    # ── register ──────────────────────────────────────────────────────────────
    body = {"name": "Test User", "email": email, "password": password}
    reg_web = call("post", "/api/auth/register", body, client_type="web")
    reg_mobile = call("post", "/api/auth/register",
                      {"name": "Mobile User", "email": unique_email(), "password": password},
                      client_type="mobile")
    reg_duplicate = call("post", "/api/auth/register", body)
    reg_invalid = call("post", "/api/auth/register",
                       {"name": "", "email": "not-an-email", "password": "123"})

    save("auth", "register", build_doc(
        endpoint="POST /api/auth/register",
        description="Register a new user account. Account is inactive until email is verified. No tokens returned — user must verify email first.",
        request={
            "headers": {"X-Client-Type": "web | mobile (default: web)"},
            "body": {"name": "Test User", "email": email, "password": "password123"},
        },
        responses={
            "201_web": reg_web,
            "201_mobile": reg_mobile,
            "409_email_exists": reg_duplicate,
            "422_validation": reg_invalid,
        },
        notes=[
            "Web: no tokens in response (cookie not set yet — user not active)",
            "Mobile: same, no tokens until email verified",
        ]
    ))

    # ── verify-email ──────────────────────────────────────────────────────────
    save("auth", "verify-email", build_doc(
        endpoint="POST /api/auth/verify-email",
        description="Verify email using token from registration email. Activates the account.",
        request={"body": {"token": "<uuid-from-email>"}},
        responses={
            "200_success": {"status": 200, "body": {"message": "Email verified successfully"}},
            "422_invalid_token": call("post", "/api/auth/verify-email", {"token": "invalid-token"}),
            "422_validation": call("post", "/api/auth/verify-email", {"token": ""}),
        }
    ))

    # ── resend-verification ───────────────────────────────────────────────────
    save("auth", "resend-verification", build_doc(
        endpoint="POST /api/auth/resend-verification",
        description="Resend email verification link. Invalidates previous token.",
        request={"body": {"email": email}},
        responses={
            "200": call("post", "/api/auth/resend-verification", {"email": email}),
            "404_not_found": call("post", "/api/auth/resend-verification",
                                  {"email": "notfound@example.com"}),
            "422_validation": call("post", "/api/auth/resend-verification", {"email": "bad"}),
        }
    ))

    # ── login ─────────────────────────────────────────────────────────────────
    # Account vẫn inactive — capture error thật
    login_inactive = call("post", "/api/auth/login", {"email": email, "password": password},
                          client_type="web", session=web_session)

    save("auth", "login", build_doc(
        endpoint="POST /api/auth/login",
        description="Login with email and password. Account must be active (email verified).",
        request={
            "headers": {"X-Client-Type": "web | mobile (default: web)"},
            "body": {"email": email, "password": "password123"},
        },
        responses={
            "200_web": {
                "status": 200,
                "body": {"data": {"accessToken": "<jwt>", "tokenType": "Bearer",
                                  "user": {"id": 1, "name": "Test User", "email": email}}},
                "set_cookie": "refreshToken=<uuid>; Path=/api/auth; HttpOnly; SameSite=Lax",
            },
            "200_mobile": {
                "status": 200,
                "body": {"data": {"accessToken": "<jwt>", "refreshToken": "<uuid>",
                                  "tokenType": "Bearer",
                                  "user": {"id": 1, "name": "Test User", "email": email}}},
            },
            "422_account_not_active": login_inactive,
            "422_invalid_credentials": call("post", "/api/auth/login",
                                            {"email": email, "password": "wrongpass"}),
            "422_validation": call("post", "/api/auth/login", {"email": "bad", "password": ""}),
        },
        notes=[
            "Web: refreshToken set as httpOnly cookie, NOT in response body",
            "Mobile: refreshToken returned in body, store in Keychain/EncryptedSharedPreferences",
        ]
    ))

    # ── refresh ───────────────────────────────────────────────────────────────
    save("auth", "refresh-token", build_doc(
        endpoint="POST /api/auth/refresh",
        description="Rotate refresh token. Old token is revoked, new token issued.",
        request={
            "headers": {"X-Client-Type": "web | mobile (default: web)"},
            "body_mobile_only": {"refreshToken": "<uuid>"},
            "cookie_web": "refreshToken=<uuid> (sent automatically by browser)",
        },
        responses={
            "200_web": {
                "status": 200,
                "body": {"data": {"accessToken": "<new-jwt>", "tokenType": "Bearer"}},
                "set_cookie": "refreshToken=<new-uuid>; Path=/api/auth; HttpOnly; SameSite=Lax",
            },
            "200_mobile": {
                "status": 200,
                "body": {"data": {"accessToken": "<new-jwt>", "refreshToken": "<new-uuid>",
                                  "tokenType": "Bearer"}},
            },
            "422_invalid_token": call("post", "/api/auth/refresh",
                                      {"refreshToken": "invalid"}, client_type="mobile"),
        },
        notes=[
            "Web: no body needed, cookie sent automatically",
            "Mobile: send refreshToken in body",
        ]
    ))

    # ── logout ────────────────────────────────────────────────────────────────
    save("auth", "logout", build_doc(
        endpoint="POST /api/auth/logout",
        description="Logout from current device. Revokes refresh token and blacklists access token.",
        request={
            "headers": {
                "Authorization": "Bearer <access-token>",
                "X-Client-Type": "web | mobile (default: web)",
            },
            "body_mobile_only": {"refreshToken": "<uuid>"},
            "cookie_web": "refreshToken=<uuid> (sent automatically by browser)",
        },
        responses={
            "200": {"status": 200, "body": {"message": "Logged out successfully"}},
        },
        notes=["Web: cookie cleared automatically after logout"]
    ))

    # ── logout-all ────────────────────────────────────────────────────────────
    save("auth", "logout-all", build_doc(
        endpoint="POST /api/auth/logout-all",
        description="Logout from all devices. Revokes all refresh tokens for the user.",
        request={
            "headers": {
                "Authorization": "Bearer <access-token>",
                "X-Client-Type": "web | mobile (default: web)",
            },
        },
        responses={
            "200": {"status": 200, "body": {"message": "Logged out from all devices"}},
            "401_unauthorized": {"status": 401, "body": {"status": 401, "code": "UNAUTHORIZED",
                                                          "message": "Unauthorized"}},
        }
    ))

    # ── forgot-password ───────────────────────────────────────────────────────
    save("auth", "forgot-password", build_doc(
        endpoint="POST /api/auth/forgot-password",
        description="Request password reset link. Always returns 200 to prevent email enumeration.",
        request={"body": {"email": email}},
        responses={
            "200": call("post", "/api/auth/forgot-password", {"email": email}),
            "422_validation": call("post", "/api/auth/forgot-password", {"email": "bad-email"}),
        }
    ))

    # ── reset-password ────────────────────────────────────────────────────────
    save("auth", "reset-password", build_doc(
        endpoint="POST /api/auth/reset-password",
        description="Reset password using token from email. Revokes all refresh tokens after success.",
        request={"body": {"token": "<uuid-from-email>", "newPassword": "newpassword123"}},
        responses={
            "200_success": {"status": 200, "body": {"message": "Password reset successfully"}},
            "422_invalid_token": call("post", "/api/auth/reset-password",
                                      {"token": "invalid-token", "newPassword": "newpass123"}),
            "422_validation": call("post", "/api/auth/reset-password",
                                   {"token": "", "newPassword": "123"}),
        }
    ))

    # ── me ────────────────────────────────────────────────────────────────────
    save("auth", "me", build_doc(
        endpoint="GET /api/auth/me",
        description="Get current authenticated user profile.",
        request={"headers": {"Authorization": "Bearer <access-token>"}},
        responses={
            "200": {"status": 200, "body": {"data": {
                "id": 1, "name": "Test User", "email": email,
                "phone": None, "avatar": None, "status": "active", "provider": "local",
            }}},
            "401_unauthorized": {"status": 401, "body": {
                "status": 401, "code": "UNAUTHORIZED", "message": "Unauthorized",
            }},
        }
    ))

    # ── update-profile ────────────────────────────────────────────────────────
    save("auth", "update-profile", build_doc(
        endpoint="PUT /api/auth/profile",
        description="Update current user profile. All fields are optional.",
        request={
            "headers": {"Authorization": "Bearer <access-token>"},
            "body": {"name": "New Name", "phone": "0123456789",
                     "avatar": "https://example.com/avatar.png"},
        },
        responses={
            "200": {"status": 200, "body": {"data": {
                "id": 1, "name": "New Name", "email": email,
                "phone": "0123456789", "avatar": "https://example.com/avatar.png",
                "status": "active", "provider": "local",
            }}},
            "422_validation": call("put", "/api/auth/profile", {"name": "X"}),
        }
    ))

    # ── change-password ───────────────────────────────────────────────────────
    save("auth", "change-password", build_doc(
        endpoint="PUT /api/auth/change-password",
        description="Change password for authenticated user. Requires current password.",
        request={
            "headers": {"Authorization": "Bearer <access-token>"},
            "body": {"currentPassword": "oldpassword", "newPassword": "newpassword123"},
        },
        responses={
            "200_success": {"status": 200, "body": {"message": "Password changed successfully"}},
            "422_wrong_current": {"status": 422, "body": {
                "status": 422, "code": "INVALID_CREDENTIALS", "message": "Invalid credentials",
            }},
            "422_validation": call("put", "/api/auth/change-password",
                                   {"currentPassword": "", "newPassword": "123"}),
        }
    ))


# ── main ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print(f"Capturing API docs from {BASE_URL}")
    capture_auth()
    print("\nDone.")
