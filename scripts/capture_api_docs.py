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


def build_doc(endpoint: str, description: str, request: dict, responses: dict) -> dict:
    return {
        "endpoint": endpoint,
        "description": description,
        "request": request,
        "responses": responses,
    }


# ── helpers ──────────────────────────────────────────────────────────────────

def unique_email():
    return f"test_{int(time.time())}@example.com"


# ── auth module ───────────────────────────────────────────────────────────────

def capture_auth():
    print("\n[auth]")
    email = unique_email()
    password = "password123"
    access_token = None
    refresh_token = None

    # POST /api/auth/register — success
    body = {"name": "Test User", "email": email, "password": password}
    success = call("post", "/api/auth/register", body)

    # POST /api/auth/register — email already exists
    duplicate = call("post", "/api/auth/register", body)

    # POST /api/auth/register — validation error
    invalid = call("post", "/api/auth/register", {"name": "", "email": "not-an-email", "password": "123"})

    save("auth", "register", build_doc(
        endpoint="POST /api/auth/register",
        description="Register a new user account. Account is inactive until email is verified.",
        request={"body": body},
        responses={
            "201": success,
            "409_email_exists": duplicate,
            "422_validation": invalid,
        }
    ))

    # POST /api/auth/verify-email — need a real token, skip runtime capture, document shape only
    save("auth", "verify-email", build_doc(
        endpoint="POST /api/auth/verify-email",
        description="Verify email address using token sent to user's email after registration.",
        request={"body": {"token": "<uuid-token-from-email>"}},
        responses={
            "200_success": {"status": 200, "body": {"message": "Email verified successfully"}},
            "422_invalid_token": call("post", "/api/auth/verify-email", {"token": "invalid-token"}),
        }
    ))

    # POST /api/auth/resend-verification
    resend_success = call("post", "/api/auth/resend-verification", {"email": email})
    resend_not_found = call("post", "/api/auth/resend-verification", {"email": "notfound@example.com"})
    save("auth", "resend-verification", build_doc(
        endpoint="POST /api/auth/resend-verification",
        description="Resend email verification link to the given email address.",
        request={"body": {"email": email}},
        responses={
            "200": resend_success,
            "404_not_found": resend_not_found,
        }
    ))

    # POST /api/auth/login — account not active yet
    login_inactive = call("post", "/api/auth/login", {"email": email, "password": password})
    save("auth", "login", build_doc(
        endpoint="POST /api/auth/login",
        description="Login with email and password. Account must be active (email verified).",
        request={"body": {"email": email, "password": password}},
        responses={
            "200_success": {"status": 200, "body": {"data": {"accessToken": "<jwt>", "refreshToken": "<uuid>", "tokenType": "Bearer", "user": {"id": 1, "name": "Test User", "email": email}}}},
            "422_account_not_active": login_inactive,
            "422_invalid_credentials": call("post", "/api/auth/login", {"email": email, "password": "wrongpass"}),
            "422_validation": call("post", "/api/auth/login", {"email": "bad", "password": ""}),
        }
    ))

    # POST /api/auth/refresh — no valid token available, document shape
    refresh_invalid = call("post", "/api/auth/refresh", {"refreshToken": "invalid-token"})
    save("auth", "refresh-token", build_doc(
        endpoint="POST /api/auth/refresh",
        description="Rotate refresh token. Returns new access token and new refresh token. Old refresh token is revoked.",
        request={"body": {"refreshToken": "<uuid-refresh-token>"}},
        responses={
            "200_success": {"status": 200, "body": {"data": {"accessToken": "<new-jwt>", "refreshToken": "<new-uuid>", "tokenType": "Bearer"}}},
            "422_invalid_token": refresh_invalid,
        }
    ))

    # POST /api/auth/logout — shape only (requires valid tokens)
    save("auth", "logout", build_doc(
        endpoint="POST /api/auth/logout",
        description="Logout from current device. Revokes the provided refresh token and blacklists the access token.",
        request={
            "headers": {"Authorization": "Bearer <access-token>"},
            "body": {"refreshToken": "<uuid-refresh-token>"},
        },
        responses={
            "200": {"status": 200, "body": {"message": "Logged out successfully"}},
        }
    ))

    # POST /api/auth/logout-all — shape only
    save("auth", "logout-all", build_doc(
        endpoint="POST /api/auth/logout-all",
        description="Logout from all devices. Revokes all refresh tokens for the authenticated user.",
        request={"headers": {"Authorization": "Bearer <access-token>"}},
        responses={
            "200": {"status": 200, "body": {"message": "Logged out from all devices"}},
        }
    ))

    # POST /api/auth/forgot-password
    forgot_success = call("post", "/api/auth/forgot-password", {"email": email})
    forgot_invalid_email = call("post", "/api/auth/forgot-password", {"email": "bad-email"})
    save("auth", "forgot-password", build_doc(
        endpoint="POST /api/auth/forgot-password",
        description="Request a password reset link. Always returns 200 regardless of whether email exists (prevents email enumeration).",
        request={"body": {"email": email}},
        responses={
            "200": forgot_success,
            "422_validation": forgot_invalid_email,
        }
    ))

    # POST /api/auth/reset-password
    reset_invalid = call("post", "/api/auth/reset-password", {"token": "invalid-token", "newPassword": "newpass123"})
    reset_validation = call("post", "/api/auth/reset-password", {"token": "", "newPassword": "123"})
    save("auth", "reset-password", build_doc(
        endpoint="POST /api/auth/reset-password",
        description="Reset password using token from email. Revokes all refresh tokens after success.",
        request={"body": {"token": "<uuid-token-from-email>", "newPassword": "newpassword123"}},
        responses={
            "200_success": {"status": 200, "body": {"message": "Password reset successfully"}},
            "422_invalid_token": reset_invalid,
            "422_validation": reset_validation,
        }
    ))

    # GET /api/auth/me — shape only (requires auth)
    save("auth", "me", build_doc(
        endpoint="GET /api/auth/me",
        description="Get current authenticated user's profile.",
        request={"headers": {"Authorization": "Bearer <access-token>"}},
        responses={
            "200": {"status": 200, "body": {"data": {"id": 1, "name": "Test User", "email": email, "phone": None, "avatar": None, "status": "active", "provider": "local"}}},
            "401_unauthorized": {"status": 401, "body": {"status": 401, "code": "UNAUTHORIZED", "message": "Unauthorized"}},
        }
    ))

    # PUT /api/auth/profile — shape only
    save("auth", "update-profile", build_doc(
        endpoint="PUT /api/auth/profile",
        description="Update current user's profile. All fields are optional.",
        request={
            "headers": {"Authorization": "Bearer <access-token>"},
            "body": {"name": "New Name", "phone": "0123456789", "avatar": "https://example.com/avatar.png"},
        },
        responses={
            "200": {"status": 200, "body": {"data": {"id": 1, "name": "New Name", "email": email, "phone": "0123456789", "avatar": "https://example.com/avatar.png", "status": "active", "provider": "local"}}},
            "422_validation": call("put", "/api/auth/profile", {"name": "X"}),
        }
    ))

    # PUT /api/auth/change-password — shape only
    save("auth", "change-password", build_doc(
        endpoint="PUT /api/auth/change-password",
        description="Change password for authenticated user. Requires current password.",
        request={
            "headers": {"Authorization": "Bearer <access-token>"},
            "body": {"currentPassword": "oldpassword", "newPassword": "newpassword123"},
        },
        responses={
            "200_success": {"status": 200, "body": {"message": "Password changed successfully"}},
            "422_wrong_current": {"status": 422, "body": {"status": 422, "code": "INVALID_CREDENTIALS", "message": "Invalid credentials"}},
            "422_validation": call("put", "/api/auth/change-password", {"currentPassword": "", "newPassword": "123"}),
        }
    ))


# ── main ──────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print(f"Capturing API docs from {BASE_URL}")
    capture_auth()
    print("\nDone.")
