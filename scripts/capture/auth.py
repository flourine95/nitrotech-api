import time
import requests
from .base import save, save_index, call, anon, build_doc, unique_email, login, BASE_URL


def capture(s: requests.Session, admin_email: str, admin_password: str):
    print("\n[auth]")
    email = unique_email()

    # register
    save("auth", "register", build_doc(
        endpoint="POST /api/auth/register",
        description="Đăng ký tài khoản mới. Tài khoản inactive cho đến khi xác thực email.",
        request={"body": {"name": "string", "email": "string", "password": "string (min 6)"}},
        responses={
            "201_created": anon("post", "/api/auth/register",
                                {"name": "Test User", "email": email, "password": "password123"}),
            "409_email_exists": anon("post", "/api/auth/register",
                                     {"name": "Test User", "email": admin_email, "password": "password123"}),
            "422_validation": anon("post", "/api/auth/register",
                                   {"name": "", "email": "bad", "password": "123"}),
        },
        notes=["Không trả token — user phải verify email trước khi login"]
    ))

    # login
    save("auth", "login", build_doc(
        endpoint="POST /api/auth/login",
        description="Đăng nhập. Server set SESSION cookie HttpOnly.",
        request={"body": {"email": "string", "password": "string"}},
        responses={
            "200_ok": anon("post", "/api/auth/login",
                           {"email": admin_email, "password": admin_password}),
            "401_wrong_credentials": anon("post", "/api/auth/login",
                                          {"email": admin_email, "password": "wrongpass"}),
            "422_validation": anon("post", "/api/auth/login", {"email": "bad", "password": ""}),
        },
        notes=["SESSION cookie tự động set — client không cần xử lý thêm",
               "Tài khoản phải active (đã verify email)"]
    ))

    # me
    save("auth", "me", build_doc(
        endpoint="GET /api/auth/me",
        description="Lấy thông tin user đang đăng nhập.",
        request={"cookie": "SESSION=<session-id> (tự động)"},
        responses={
            "200_ok": call(s, "get", "/api/auth/me"),
            "401_unauthorized": anon("get", "/api/auth/me"),
        }
    ))

    # logout
    s_tmp = login(admin_email, admin_password)
    save("auth", "logout", build_doc(
        endpoint="POST /api/auth/logout",
        description="Đăng xuất. Xóa session khỏi Redis, clear cookie.",
        request={"cookie": "SESSION=<session-id> (tự động)"},
        responses={"200_ok": call(s_tmp, "post", "/api/auth/logout")}
    ))

    # logout-all
    s_tmp2 = login(admin_email, admin_password)
    save("auth", "logout-all", build_doc(
        endpoint="POST /api/auth/logout-all",
        description="Đăng xuất tất cả thiết bị. Xóa toàn bộ session của user trong Redis.",
        request={"cookie": "SESSION=<session-id> (tự động)"},
        responses={
            "200_ok": call(s_tmp2, "post", "/api/auth/logout-all"),
            "401_unauthorized": anon("post", "/api/auth/logout-all"),
        }
    ))

    # forgot-password
    save("auth", "forgot-password", build_doc(
        endpoint="POST /api/auth/forgot-password",
        description="Gửi link reset password. Luôn trả 200 dù email có tồn tại hay không.",
        request={"body": {"email": "string"}},
        responses={
            "200": anon("post", "/api/auth/forgot-password", {"email": admin_email}),
            "422_validation": anon("post", "/api/auth/forgot-password", {"email": "bad"}),
        },
        notes=["Luôn trả 200 để tránh email enumeration"]
    ))

    # reset-password
    save("auth", "reset-password", build_doc(
        endpoint="POST /api/auth/reset-password",
        description="Reset password bằng token từ email. Invalidate tất cả session sau khi reset.",
        request={"body": {"token": "string (uuid from email)", "newPassword": "string (min 6)"}},
        responses={
            "200_success": {"status": 200, "body": {"data": None, "message": "Password reset successfully"}},
            "422_invalid_token": anon("post", "/api/auth/reset-password",
                                      {"token": "invalid", "newPassword": "newpass123"}),
        }
    ))

    # verify-email
    save("auth", "verify-email", build_doc(
        endpoint="POST /api/auth/verify-email",
        description="Xác thực email bằng token từ email đăng ký.",
        request={"body": {"token": "string (uuid from email)"}},
        responses={
            "200_success": {"status": 200, "body": {"data": None, "message": "Email verified successfully"}},
            "422_invalid_token": anon("post", "/api/auth/verify-email", {"token": "invalid"}),
        }
    ))

    # resend-verification
    save("auth", "resend-verification", build_doc(
        endpoint="POST /api/auth/resend-verification",
        description="Gửi lại email xác thực. Xóa token cũ, tạo token mới.",
        request={"body": {"email": "string"}},
        responses={
            "200": anon("post", "/api/auth/resend-verification", {"email": email}),
            "404_not_found": anon("post", "/api/auth/resend-verification",
                                  {"email": "notfound@example.com"}),
        }
    ))

    save_index("auth")
