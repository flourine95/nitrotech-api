package com.nitrotech.api.application.auth.controller;

import com.nitrotech.api.application.auth.service.AuthSessionService;
import com.nitrotech.api.application.auth.service.OAuthAuthorizationService;
import com.nitrotech.api.application.auth.service.OAuthLoginRedirectService;
import com.nitrotech.api.application.auth.request.*;
import com.nitrotech.api.domain.auth.dto.*;
import com.nitrotech.api.domain.auth.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationUseCase resendVerificationUseCase;
    private final OAuthAuthorizationService oauthAuthorizationService;
    private final AuthSessionService authSessionService;
    private final OAuthLoginRedirectService oauthLoginRedirectService;

    @PostMapping("/register")
    public ResponseEntity<ApiResult<AuthResult>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResult result = registerUseCase.execute(
                new RegisterCommand(request.name(), request.email(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(result));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResult<AuthResult>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResult result = loginUseCase.execute(new LoginCommand(request.email(), request.password()));
        authSessionService.create(result, httpRequest);
        return ResponseEntity.ok(ApiResult.ok(result));
    }

    @GetMapping("/oauth/{provider}/authorize")
    public ResponseEntity<ApiResult<Map<String, String>>> authorizeOAuth(
            @PathVariable String provider,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(Map.of("authorizationUrl", oauthAuthorizationService.authorize(provider, request))));
    }

    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> oauthCallback(
            @PathVariable String provider,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletRequest httpRequest
    ) {
        return redirect(oauthLoginRedirectService.handleCallback(provider, code, state, error, errorDescription, httpRequest));
    }

    private ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(HttpServletRequest request) {
        logoutUseCase.execute(request);
        return ResponseEntity.ok(ApiResult.ok("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<ApiResult<Void>> logoutAll(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        logoutUseCase.executeAll(principal.email(), request);
        return ResponseEntity.ok(ApiResult.ok("Logged out from all devices"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResult<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordUseCase.execute(request.email());
        return ResponseEntity.ok(ApiResult.ok("If the email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResult<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResult.ok("Password reset successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResult<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verifyEmailUseCase.execute(request.token());
        return ResponseEntity.ok(ApiResult.ok("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResult<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        resendVerificationUseCase.execute(request.email());
        return ResponseEntity.ok(ApiResult.ok("Verification email sent"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResult<UserProfileData>> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.ok(ApiResult.ok(null, null));
        }
        return ResponseEntity.ok(ApiResult.ok(getProfileUseCase.execute(principal.id())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResult<UserProfileData>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(updateProfileUseCase.execute(
                new UpdateProfileCommand(principal.id(), request.name(), request.phone(), request.avatar()))));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResult<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        changePasswordUseCase.execute(
                new ChangePasswordCommand(principal.id(), request.currentPassword(), request.newPassword()));
        return ResponseEntity.ok(ApiResult.ok("Password changed successfully"));
    }
}
