package com.nitrotech.api.application.auth.controller;

import com.nitrotech.api.application.auth.request.*;
import com.nitrotech.api.domain.auth.dto.*;
import com.nitrotech.api.domain.auth.exception.InvalidRefreshTokenException;
import com.nitrotech.api.domain.auth.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import com.nitrotech.api.shared.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationUseCase resendVerificationUseCase;
    private final CookieUtil cookieUtil;

    public AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase, LogoutUseCase logoutUseCase,
                          GetProfileUseCase getProfileUseCase, UpdateProfileUseCase updateProfileUseCase,
                          ChangePasswordUseCase changePasswordUseCase,
                          ForgotPasswordUseCase forgotPasswordUseCase,
                          ResetPasswordUseCase resetPasswordUseCase,
                          VerifyEmailUseCase verifyEmailUseCase,
                          ResendVerificationUseCase resendVerificationUseCase,
                          CookieUtil cookieUtil) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.forgotPasswordUseCase = forgotPasswordUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.resendVerificationUseCase = resendVerificationUseCase;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResult>> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            HttpServletResponse response
    ) {
        AuthResult result = registerUseCase.execute(
                new RegisterCommand(request.name(), request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(toClientResult(result, clientType, response)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResult>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            HttpServletResponse response
    ) {
        AuthResult result = loginUseCase.execute(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok(ApiResponse.ok(toClientResult(result, clientType, response)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenPair>> refresh(
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String refreshToken = isWeb(clientType)
                ? cookieUtil.extractRefreshToken(httpRequest)
                        .orElseThrow(InvalidRefreshTokenException::new)
                : (body != null ? body.refreshToken() : null);

        TokenPair tokens = refreshTokenUseCase.execute(refreshToken);

        if (isWeb(clientType)) {
            cookieUtil.setRefreshTokenCookie(response, tokens.refreshToken(), 30 * 24 * 60 * 60);
            return ResponseEntity.ok(ApiResponse.ok(TokenPair.ofSeconds(tokens.accessToken(), null, tokens.expiresIn())));
        }
        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        String refreshToken = isWeb(clientType)
                ? cookieUtil.extractRefreshToken(httpRequest).orElse(null)
                : (body != null ? body.refreshToken() : null);

        logoutUseCase.execute(refreshToken, extractToken(httpRequest));
        if (isWeb(clientType)) cookieUtil.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal String email,
            @RequestHeader(value = "X-Client-Type", defaultValue = "web") String clientType,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        UserProfileData user = getProfileUseCase.executeByEmail(email);
        logoutUseCase.executeAll(user.id(), extractToken(httpRequest));
        if (isWeb(clientType)) cookieUtil.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out from all devices"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request.email());
        // Luôn trả 200 dù email có tồn tại hay không — tránh email enumeration
        return ResponseEntity.ok(ApiResponse.ok(null, "If the email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok(null, "Password reset successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailUseCase.execute(request.token());
        return ResponseEntity.ok(ApiResponse.ok(null, "Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        resendVerificationUseCase.execute(request.email());
        return ResponseEntity.ok(ApiResponse.ok(null, "Verification email sent"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileData>> me(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.ok(getProfileUseCase.executeByEmail(email)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileData>> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileData current = getProfileUseCase.executeByEmail(email);
        return ResponseEntity.ok(ApiResponse.ok(updateProfileUseCase.execute(
                new UpdateProfileCommand(current.id(), request.name(), request.phone(), request.avatar()))));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UserProfileData current = getProfileUseCase.executeByEmail(email);
        changePasswordUseCase.execute(
                new ChangePasswordCommand(current.id(), request.currentPassword(), request.newPassword()));
        return ResponseEntity.ok(ApiResponse.ok(null, "Password changed successfully"));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

    private boolean isWeb(String clientType) {
        return !"mobile".equalsIgnoreCase(clientType);
    }

    // Web: refresh token vào cookie, bỏ khỏi body. Mobile: giữ nguyên trong body.
    private AuthResult toClientResult(AuthResult result, String clientType, HttpServletResponse response) {
        if (result.accessToken() == null) return result; // register — chưa có token
        if (isWeb(clientType)) {
            cookieUtil.setRefreshTokenCookie(response, result.refreshToken(), 30 * 24 * 60 * 60);
            return AuthResult.of(TokenPair.ofSeconds(result.accessToken(), null, result.expiresIn() != null ? result.expiresIn() : 0), result.user());
        }
        return result;
    }
}
